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

@Deprecated // To be deleted
public class SingleThreadResolver /* implements Closeable */ {

//    private static final int UDP_DATAGRAM_MAX_SIZE = 512;
//    private static final int DNS_PORT = 53;
//
//    private class NameIpBinding {
//
//        String dnsName;
//        String address;
//
//        public NameIpBinding(String dnsName, String address) {
//            this.dnsName = dnsName;
//            this.address = address;
//        }
//    }
//
//    private enum Status {
//        START,
//        NET_QUERY_NS,
//        NET_QUERY,
//        RESULT,
//        CLOSED,
//        ERROR
//    }
//
//    private final SingleDNSQueryClient netClient;
//    private final AtomicInteger nextId;
//
//    private final DnsEngine engine;
//    private final List<String> dnsRootServers;
//    private final DnsCache cache;
//    private final boolean useIPv4;
//    private final boolean useIPv6;
//
//
//    private Status status;
//    private String targetName;
//    private Deque<NameIpBinding> dnsServers;
//    private Set<String> alreadyUsedIps;
//    private ByteBuffer message;
//
//    private SingleThreadResolver recurringProcess;
//
//    private List<ResourceRecord> result;
//
////            DnsType type = question.getDnsType();
////
/////*
////AXFR            252 A request for a transfer of an entire zone
////MAILB           253 A request for mailbox-related records (MB, MG or MR)
////MAILA           254 A request for mail agent RRs (Obsolete - see MX)
////*               255 A request for all records
////*/
////            // TODO support queries returning more than one resource record type
////            if(type.equals(DnsType.AXFR) || type.equals(DnsType.MAILB) || type.equals(DnsType.MAILA)
////                    || type.equals(DnsType.ALL) || question.getDnsClass().equals(DnsClass.ANY)) {
////            }
//
//
//    SingleThreadResolver(DnsEngine engine,
//                         List<String> dnsRootServers,
//                         DnsCache cache,
//                         boolean useIPv4,
//                         boolean useIPv6
//    ) throws DnsException {
//
//        try {
//            this.netClient = new SingleDNSQueryClient(DNS_PORT);
//        } catch (IOException e) {
//            throw new DnsException(e);
//        }
//
//        this.nextId = new AtomicInteger(1);
//        this.engine = engine;
//        this.dnsRootServers = dnsRootServers;
//        this.cache = cache;
//        this.useIPv4 = useIPv4;
//        this.useIPv6 = useIPv6;
//
//        this.status = Status.ERROR;
//    }
//
//    public void startQuery(Question question) throws DnsException {
//
//        if(status == Status.CLOSED) throw new DnsException("Already closed");
//
//        result = findInCache(question, cache);
//        if(result != null) {
//            this.status = Status.RESULT;
//            return;
//        }
//
//        this.status = Status.START;
//        this.targetName = question.getDnsName();
//        this.dnsServers = getInitialDnsServers(question.getDnsName());
//        this.alreadyUsedIps = new HashSet<>();
//        this.message = createQueryMessage(question);
//    }
//
//    public boolean doIO(int timeoutMillis) throws DnsException {
//
//        switch(status) {
//            case CLOSED: throw new DnsException("Already closed");
//            case ERROR: throw new DnsException("Invalid state");
//            case RESULT: return true;
//        }
//
//        DnsException exception = null;
//        try {
//            boolean finished = internalDoIO(timeoutMillis);
//            if(!finished) return false;
//        } catch(IOException e) {
//            exception = new DnsException(e);
//        } catch(DnsException e) {
//            exception = e;
//        }
//        try {
//            closeResources();
//        } catch (IOException e) {
//            if(exception != null) exception = new DnsException(e);
//        }
//        if(exception != null) throw exception;
//        return true;
//    }
//
//    public List<ResourceRecord> getResult() {
//        return result;
//    }
//
//    @Override
//    public void close() throws IOException {
//        closeResources();
//    }
//
//    private List<ResourceRecord> findInCache(Question question, DnsCache cache) {
//        List<CachedResourceRecord> records = cache.getResourceRecords(question);
//        boolean expired = false;
//        for(CachedResourceRecord record: records) if(record.isExpired()) { expired = true; break; }
//        if(expired || records.size() == 0) return null;
//        return records.stream().map(CachedResourceRecord::getResourceRecord).collect(Collectors.toList());
//    }
//
//    private Deque<NameIpBinding> getInitialDnsServers(String name) {
//        boolean firstTime = true;
//        List<CachedResourceRecord> nsResult;
//        List<CachedResourceRecord> ipResult;
//        Deque<NameIpBinding> result = new LinkedList<>();
//
//        while(!name.isEmpty()) {
//
//            if(firstTime) {
//                firstTime = false;
//            } else {
//                int dotPos = name.indexOf('.');
//                if(dotPos == -1) break;
//                name = name.substring(dotPos + 1);
//            }
//
//            nsResult = cache.getResourceRecords(engine.createQuestion(name, DnsType.NS, DnsClass.IN));
//            for(CachedResourceRecord ns: nsResult) {
//                if(ns.isExpired()) {
//                    cache.removeResourceRecord(ns);
//                    continue;
//                }
//                ipResult = cache.getResourceRecords(engine.createQuestion(ns.getDnsName(), DnsType.A, DnsClass.IN));
//                for(CachedResourceRecord ip: ipResult) {
//                    if(ip.isExpired()) {
//                        cache.removeResourceRecord(ip);
//                        continue;
//                    }
//                    result.add(new NameIpBinding(ns.getDnsName(), ip.getData(String.class)));
//                }
//            }
//        }
//
//        dnsRootServers.stream().map(root -> new NameIpBinding("[ROOT]", root)).forEach(result::add);
//
//        return result;
//    }
//
//    private void closeResources() throws IOException {
//        netClient.close();
//    }
//
//    private IOException close(Closeable closeable) {
//        IOException exception = null;
//        try {
//            if(closeable != null) closeable.close();
//        } catch(IOException e) {
//            exception = e;
//        }
//        return exception;
//    }
//
//    private boolean internalDoIO(int timeoutMillis) throws IOException, DnsException {
//
//        while(result == null) {
//
//            try {
//
//                switch (status) {
//
//                    case START:
//
////                        if (!updateDnsServerIps(timeoutMillis)) return false;
//
//                        NameIpBinding nameServer = dnsServers.peekFirst();
//                        if (nameServer == null) {
//                            result = Collections.emptyList();
//                            return true;
//                        }
//                        if(nameServer.address == null) {
//                            status = Status.NET_QUERY_NS;
//                            if(!netClient.startQuery(message, currentIp, timeoutMillis)) return false;
//
//                            break;
//                        }
//                        status = ? Status.NET_QUERY_NS: Status.NET_QUERY;
//                        break;
//
//                    case NET_QUERY_NS:
//
//
//                        status = Status.NET_QUERY;
//                        if(!netClient.startQuery(message, currentIp, timeoutMillis)) return false;
//
//                    case NET_QUERY:
//
//                        if(!netClient.doIO(timeoutMillis)) return false;
//                        ByteBuffer buffer = netClient.getResult();
//                        DnsMessage response = engine.createMessageFromBuffer(buffer.array(), 0);
//                        grabAnswersIfExist(response);
//                        addExtraInfoToCache(response);
//                        status = Status.RESULT;
//
//                    case RESULT:
//
//
//                }
//
//            } catch (SocketException e) {
//
//                status = Status.START;
//                IOException e1 = close(netClient);
//                IOException e2 = close(recurringProcess);
//                if(e1 != null) throw e1;
//                if(e2 != null) throw e2;
//
//            }
//
//        }
//
//        return true;
//
//    }
//
//    private void addExtraInfoToCache(DnsMessage response) {
//
//        for(int i = 0; i < response.getNumAdditionals(); i++) {
//            cache.addResourceRecord(response.getAdditional(i));
//        }
//
//        if(response.getNumAuthorities() > 0) {
//            Deque<String> serverNames = new LinkedList<>();
//            for(int i = 0; i < response.getNumAuthorities(); i++) {
//                ResourceRecord authority = response.getAuthority(i);
//                cache.addResourceRecord(authority);
//                String name = authority.getDnsName();
//                if(isSelfOrSuperDomain(name, targetName)) {
//                    if(authority.getDnsType().equals(DnsType.NS)) {
//                        serverNames.addFirst(authority.getData(String.class));
//                    } else if(authority.getDnsType().equals(DnsType.SOA)) {
//                        serverNames.addFirst(authority.getData(SoaValueConverter.SoaData.class).getDomainName());
//                    }
//                }
//            }
//            serverNames.forEach(name -> addServerAndIps(dnsServers, name));
//        }
//    }
//
//    private void grabAnswersIfExist(DnsMessage response) {
//        int numAnswers = response.getNumAnswers();
//        if(numAnswers > 0) {
//            result = new ArrayList<>(numAnswers);
//            for (int i = 0; i < numAnswers; i++) {
//                result.add(response.getAnswer(i));
//            }
//        }
//    }
//
//    // TODO refactorize this to optimize and make more clear
//    private boolean updateDnsServerIps(int timeoutMillis) throws IOException, DnsException {
//        if(recurringProcess == null) {
//            if(dnsServers.size() == 0 || dnsServers.peekFirst().getAddresses().size() > 0) return true;
//            recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DnsType.ALL); // TODO optimize ALL to bring only A, AAAA and CNAME
//        }
//        boolean added = false;
//        while(recurringProcess != null) {
//            if (!recurringProcess.doIO(timeoutMillis)) return false;
//            for (ResourceRecord record : recurringProcess.getResult()) {
//                if(record.getDnsType() == DnsType.CNAME) {
//                    addServerAndIps(dnsServers, record.getData(String.class));
//                    recurringProcess.close();
//                    recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DnsType.ALL);
//                    return false;
//                }
//                if (record.getDnsType() == DnsType.A && useIPv4
//                        || record.getDnsType() == DnsType.AAAA && useIPv6) {
//                    String ip = record.getData(String.class);
//                    if (!alreadyUsedIps.contains(ip)) {
//                        dnsServers.peekFirst().getAddresses().add(ip);
//                        added = true;
//                    }
//                }
//            }
//            recurringProcess.close();
//            recurringProcess = null;
//            if(!added) {
//                if(dnsServers.size() == 0) return true;
//                recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DnsType.ALL);
//            }
//        }
//        return true;
//    }
//
//    private String pollNextIp() {
//        NameServer nameServer = dnsServers.peekFirst();
//        List<String> ips = nameServer.getAddresses();
//        String ip = ips.remove(ips.size() - 1);
//        if(ips.size() == 0) dnsServers.pollFirst();
//        if(ip == null) return null;
//        alreadyUsedIps.add(ip);
//        return ip;
//    }
//
//    private ByteBuffer createQueryMessage(Question question) {
//
//        short id = getNextId();
//        DnsMessage dnsMessage = engine.createMessage(id, false, Opcode.QUERY, false, false, true, false,
//                (byte)0, ResponseCode.NO_ERROR, new Question[] {question}, null, null, null);
//
//        int aproxSize = UDP_DATAGRAM_MAX_SIZE;
//        ByteBuffer buffer = null;
//        while(buffer == null) {
//            try {
//                byte[] testingBuffer = new byte[aproxSize];
//                int actualSize = dnsMessage.writeToBuffer(testingBuffer, 0);
//                buffer = ByteBuffer.allocate(actualSize);
//                buffer.put(testingBuffer, 0, actualSize);
//                buffer.position(0);
//            } catch(IndexOutOfBoundsException e) {
//                aproxSize += UDP_DATAGRAM_MAX_SIZE;
//            }
//        }
//        return buffer;
//    }
//
//    private boolean isSelfOrSuperDomain(String superDomain, String subDomain) {
//        return superDomain.length() <= subDomain.length() && subDomain.endsWith(superDomain);
//    }
//
//    private short getNextId() {
//        short id = (short)nextId.incrementAndGet();
//        if (id == 0) id = (short)nextId.incrementAndGet();
//        return id;
//    }

}
