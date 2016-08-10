/*
 * Copyright 2015 Rafael Villar Villar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package au.id.villar.dns;

import au.id.villar.dns.cache.CachedResourceRecord;
import au.id.villar.dns.cache.DNSCache;
import au.id.villar.dns.converter.SoaValueConverter;
import au.id.villar.dns.engine.*;
import au.id.villar.dns.net.SingleDNSQueryClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AnswerProcess implements Closeable {

    private static final int UDP_DATAGRAM_MAX_SIZE = 512;
    private static final int DNS_PORT = 53;

    private enum Status {
        START,
        NET_QUERY,
        RESULT
    }

    private final SingleDNSQueryClient netClient;

    private final AtomicInteger nextId = new AtomicInteger(1);

    private Resolver resolver;
    private DNSEngine engine;
    private List<String> dnsRootServers;
    private DNSCache cache;
    private boolean useIPv4;
    private boolean useIPv6;

    private String targetName;


    private Status status;
    private Deque<NameServer> dnsServers;
    private Set<String> alreadyUsedIps;
    private ByteBuffer message;

    private ByteBuffer buffer;

    private AnswerProcess recurringProcess;

    private List<ResourceRecord> result;

    @Deprecated
    AnswerProcess(String name,
            DNSType type,
            Resolver resolver,
            DNSEngine engine,
            List<String> dnsRootServers,
            DNSCache cache,
            boolean useIPv4,
            boolean useIPv6
    ) throws DNSException {

        Question question = engine.createQuestion(name, type, DNSClass.IN);

        status = Status.START;

        try {
            this.netClient = new SingleDNSQueryClient(DNS_PORT);
        } catch (IOException e) {
            throw new DNSException(e);
        }

        result = findInCache(question, cache);
        if(result != null) {
            return;
        }

        this.resolver = resolver;
        this.engine = engine;
        this.dnsRootServers = dnsRootServers;
        this.cache = cache;
        this.useIPv4 = useIPv4;
        this.useIPv6 = useIPv6;

        this.targetName = question.getDnsName();


//            DnsType type = question.getDnsType();
//
///*
//AXFR            252 A request for a transfer of an entire zone
//MAILB           253 A request for mailbox-related records (MB, MG or MR)
//MAILA           254 A request for mail agent RRs (Obsolete - see MX)
//*               255 A request for all records
//*/
//            // TODO support queries returning more than one resource record type
//            if(type.equals(DnsType.AXFR) || type.equals(DnsType.MAILB) || type.equals(DnsType.MAILA)
//                    || type.equals(DnsType.ALL) || question.getDnsClass().equals(DnsClass.ANY)) {
//            }


        this.status = Status.START;
        this.dnsServers = getInitialDnsServers(question.getDnsName());
        this.alreadyUsedIps = new HashSet<>();
        this.message = createQueryMessage(question);
    }

    public boolean doIO(int timeoutMillis) throws DNSException {
        DNSException exception = null;
        try {
            boolean finished = internalDoIO(timeoutMillis);
            if(!finished) return false;
        } catch(IOException e) {
            exception = new DNSException(e);
        } catch(DNSException e) {
            exception = e;
        }
        try {
            closeResources();
        } catch (IOException e) {
            if(exception != null) exception = new DNSException(e);
        }
        if(exception != null) throw exception;
        return true;
    }

    public List<ResourceRecord> getResult() {
        return result;
    }

    @Override
    public void close() throws IOException {
        closeResources();
    }

    private List<ResourceRecord> findInCache(Question question, DNSCache cache) {
        List<CachedResourceRecord> records = cache.getResourceRecords(question);
        boolean expired = false;
        for(CachedResourceRecord record: records) if(record.isExpired()) { expired = true; break; }
        if(expired || records.size() == 0) return null;
        return records.stream().map(CachedResourceRecord::getResourceRecord).collect(Collectors.toList());
    }

    private Deque<NameServer> getInitialDnsServers(String name) {
        boolean firstTime = true;
        List<CachedResourceRecord> nsResult;
        List<CachedResourceRecord> ipResult;
        Deque<NameServer> result =
                dnsRootServers.stream()
                        .map(root -> new NameServer("[ROOT]", root))
                        .collect(Collectors.toCollection(LinkedList::new));

        while(!name.isEmpty()) {

            if(firstTime) {
                firstTime = false;
            } else {
                int dotPos = name.indexOf('.');
                if(dotPos == -1) break;
                name = name.substring(dotPos + 1);
            }

            nsResult = cache.getResourceRecords(engine.createQuestion(name, DNSType.NS, DNSClass.IN));
            for(CachedResourceRecord ns: nsResult) {
                if(ns.isExpired()) {
                    cache.removeResourceRecord(ns);
                    continue;
                }
                ipResult = cache.getResourceRecords(engine.createQuestion(ns.getDnsName(), DNSType.A, DNSClass.IN));
                for(CachedResourceRecord ip: ipResult) {
                    if(ip.isExpired()) {
                        cache.removeResourceRecord(ip);
                        continue;
                    }
                    addServerAndIps(result, name, ip.getData(String.class));
                }
            }
        }
        return result;
    }

    private void addServerAndIps(Deque<NameServer> list, String name, String... ips) {
        for(NameServer server: list) {
            if(server.getName().equals(name)) {
                server.getAddresses().addAll(Arrays.asList(ips));
                return;
            }
        }
        list.addFirst(new NameServer(name, ips));
    }

    private void closeResources() throws IOException {
        netClient.close();
    }

    private IOException close(Closeable closeable) {
        IOException exception = null;
        try {
            if(closeable != null) closeable.close();
        } catch(IOException e) {
            exception = e;
        }
        return exception;
    }

    private boolean internalDoIO(int timeoutMillis) throws IOException, DNSException {

        while(result == null) {

            try {

                switch (status) {

                    case START:

                        if (!updateDnsServerIps(timeoutMillis)) return false;

                        String currentIp = pollNextIp();
                        if (currentIp == null) {
                            result = Collections.emptyList();
                            return true;
                        }

                        status = Status.NET_QUERY;
                        if(!netClient.startQuery(message, currentIp, timeoutMillis)) return false;

                    case NET_QUERY:

                        if(!netClient.doIO(timeoutMillis)) return false;
                        buffer = netClient.getResult();
                        status = Status.RESULT;

                    case RESULT:

                        DNSMessage response = engine.createMessageFromBuffer(buffer.array(), 0);
                        grabAnswersIfExist(response);
                        addExtraInfoToCache(response);
                        status = Status.START;

                }

            } catch (SocketException e) {

                status = Status.START;
                IOException e1 = close(netClient);
                IOException e2 = close(recurringProcess);
                if(e1 != null) throw e1;
                if(e2 != null) throw e2;

            }

        }

        return true;

    }

    private void addExtraInfoToCache(DNSMessage response) {

        for(int i = 0; i < response.getNumAdditionals(); i++) {
            cache.addResourceRecord(response.getAdditional(i));
        }

        if(response.getNumAuthorities() > 0) {
            Deque<String> serverNames = new LinkedList<>();
            for(int i = 0; i < response.getNumAuthorities(); i++) {
                ResourceRecord authority = response.getAuthority(i);
                cache.addResourceRecord(authority);
                String name = authority.getDnsName();
                if(isSelfOrSuperDomain(name, targetName)) {
                    if(authority.getDnsType().equals(DNSType.NS)) {
                        serverNames.addFirst(authority.getData(String.class));
                    } else if(authority.getDnsType().equals(DNSType.SOA)) {
                        serverNames.addFirst(authority.getData(SoaValueConverter.SoaData.class).getDomainName());
                    }
                }
            }
            serverNames.forEach(name -> addServerAndIps(dnsServers, name));
        }
    }

    private void grabAnswersIfExist(DNSMessage response) {
        int numAnswers = response.getNumAnswers();
        if(numAnswers > 0) {
            result = new ArrayList<>(numAnswers);
            for (int i = 0; i < numAnswers; i++) {
                result.add(response.getAnswer(i));
            }
        }
    }

    // TODO refactorize this to optimize and make more clear
    private boolean updateDnsServerIps(int timeoutMillis) throws IOException, DNSException {
        if(recurringProcess == null) {
            if(dnsServers.size() == 0 || dnsServers.peekFirst().getAddresses().size() > 0) return true;
            recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DNSType.ALL); // TODO optimize ALL to bring only A, AAAA and CNAME
        }
        boolean added = false;
        while(recurringProcess != null) {
            if (!recurringProcess.doIO(timeoutMillis)) return false;
            for (ResourceRecord record : recurringProcess.getResult()) {
                if(record.getDnsType() == DNSType.CNAME) {
                    addServerAndIps(dnsServers, record.getData(String.class));
                    recurringProcess.close();
                    recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DNSType.ALL);
                    return false;
                }
                if (record.getDnsType() == DNSType.A && useIPv4
                        || record.getDnsType() == DNSType.AAAA && useIPv6) {
                    String ip = record.getData(String.class);
                    if (!alreadyUsedIps.contains(ip)) {
                        dnsServers.peekFirst().getAddresses().add(ip);
                        added = true;
                    }
                }
            }
            recurringProcess.close();
            recurringProcess = null;
            if(!added) {
                if(dnsServers.size() == 0) return true;
                recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DNSType.ALL);
            }
        }
        return true;
    }

    private String pollNextIp() {
        NameServer nameServer = dnsServers.peekFirst();
        List<String> ips = nameServer.getAddresses();
        String ip = ips.remove(ips.size() - 1);
        if(ips.size() == 0) dnsServers.pollFirst();
        if(ip == null) return null;
        alreadyUsedIps.add(ip);
        return ip;
    }

    private ByteBuffer createQueryMessage(Question question) {
        short id = getNextId();
        DNSMessage dnsMessage = engine.createSimpleQueryMessage(id, question);
        return engine.createBufferFromMessage(dnsMessage);
    }

    private boolean isSelfOrSuperDomain(String superDomain, String subDomain) {
        return superDomain.length() <= subDomain.length() && subDomain.endsWith(superDomain);
    }

    private short getNextId() {
        short id = (short)nextId.incrementAndGet();
        if (id == 0) id = (short)nextId.incrementAndGet();
        return id;
    }

}
