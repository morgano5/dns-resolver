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
import au.id.villar.dns.engine.DNSClass;
import au.id.villar.dns.engine.DNSEngine;
import au.id.villar.dns.engine.DNSMessage;
import au.id.villar.dns.engine.DNSType;
import au.id.villar.dns.engine.Question;
import au.id.villar.dns.engine.ResourceRecord;
import au.id.villar.dns.net.DNSRequestClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/*
From RFC-1034:

SERVER

4.3.2. Algorithm

The actual algorithm used by the name server will depend on the local OS
and data structures used to store RRs.  The following algorithm assumes
that the RRs are organized in several tree structures, one for each
zone, and another for the cache:

   1. Set or clear the value of recursion available in the response
      depending on whether the name server is willing to provide
      recursive service.  If recursive service is available and
      requested via the RD bit in the query, go to step 5,
      otherwise step 2.

   2. Search the available zones for the zone which is the nearest
      ancestor to QNAME.  If such a zone is found, go to step 3,
      otherwise step 4.

   3. Start matching down, label by label, in the zone.  The
      matching process can terminate several ways:

         a. If the whole of QNAME is matched, we have found the
            node.

            If the data at the node is a CNAME, and QTYPE doesn't
            match CNAME, copy the CNAME RR into the answer section
            of the response, change QNAME to the canonical name in
            the CNAME RR, and go back to step 1.

            Otherwise, copy all RRs which match QTYPE into the
            answer section and go to step 6.

         b. If a match would take us out of the authoritative data,
            we have a referral.  This happens when we encounter a
            node with NS RRs marking cuts along the bottom of a
            zone.

            Copy the NS RRs for the subzone into the authority
            section of the reply.  Put whatever addresses are
            available into the additional section, using glue RRs
            if the addresses are not available from authoritative
            data or the cache.  Go to step 4.

         c. If at some label, a match is impossible (i.e., the
            corresponding label does not exist), look to see if a
            the "*" label exists.

            If the "*" label does not exist, check whether the name
            we are looking for is the original QNAME in the query

            or a name we have followed due to a CNAME.  If the name
            is original, set an authoritative name error in the
            response and exit.  Otherwise just exit.

            If the "*" label does exist, match RRs at that node
            against QTYPE.  If any match, copy them into the answer
            section, but set the owner of the RR to be QNAME, and
            not the node with the "*" label.  Go to step 6.

   4. Start matching down in the cache.  If QNAME is found in the
      cache, copy all RRs attached to it that match QTYPE into the
      answer section.  If there was no delegation from
      authoritative data, look for the best one from the cache, and
      put it in the authority section.  Go to step 6.

   5. Using the local resolver or a copy of its algorithm (see
      resolver section of this memo) to answer the query.  Store
      the results, including any intermediate CNAMEs, in the answer
      section of the response.

   6. Using local data only, attempt to add other RRs which may be
      useful to the additional section of the query.  Exit.


RESOLVER

5.3.3. Algorithm

The top level algorithm has four steps:

   1. See if the answer is in local information, and if so return
      it to the client.

   2. Find the best servers to ask.

   3. Send them queries until one returns a response.

   4. Analyze the response, either:

         a. if the response answers the question or contains a name
            error, cache the data as well as returning it back to
            the client.

         b. if the response contains a better delegation to other
            servers, cache the delegation information, and go to
            step 2.

         c. if the response shows a CNAME and that is not the
            answer itself, cache the CNAME, change the SNAME to the
            canonical name in the CNAME RR and go to step 1.

         d. if the response shows a servers failure or other
            bizarre contents, delete the server from the SLIST and
            go back to step 3.

Step 1 searches the cache for the desired data. If the data is in the
cache, it is assumed to be good enough for normal use.  Some resolvers
have an option at the user interface which will force the resolver to
ignore the cached data and consult with an authoritative server.  This
is not recommended as the default.  If the resolver has direct access to
a name server's zones, it should check to see if the desired data is
present in authoritative form, and if so, use the authoritative data in
preference to cached data.

Step 2 looks for a name server to ask for the required data.  The
general strategy is to look for locally-available name server RRs,
starting at SNAME, then the parent domain name of SNAME, the
grandparent, and so on toward the root.  Thus if SNAME were
Mockapetris.ISI.EDU, this step would look for NS RRs for
Mockapetris.ISI.EDU, then ISI.EDU, then EDU, and then . (the root).
These NS RRs list the names of hosts for a zone at or above SNAME.  Copy
the names into SLIST.  Set up their addresses using local data.  It may
be the case that the addresses are not available.  The resolver has many
choices here; the best is to start parallel resolver processes looking
for the addresses while continuing onward with the addresses which are
available.  Obviously, the design choices and options are complicated
and a function of the local host's capabilities.  The recommended
priorities for the resolver designer are:

   1. Bound the amount of work (packets sent, parallel processes
      started) so that a request can't get into an infinite loop or
      start off a chain reaction of requests or queries with other
      implementations EVEN IF SOMEONE HAS INCORRECTLY CONFIGURED
      SOME DATA.

   2. Get back an answer if at all possible.

   3. Avoid unnecessary transmissions.

   4. Get the answer as quickly as possible.

If the search for NS RRs fails, then the resolver initializes SLIST from
the safety belt SBELT.  The basic idea is that when the resolver has no
idea what servers to ask, it should use information from a configuration
file that lists several servers which are expected to be helpful.
Although there are special situations, the usual choice is two of the
root servers and two of the servers for the host's domain.  The reason
for two of each is for redundancy.  The root servers will provide
eventual access to all of the domain space.  The two local servers will
allow the resolver to continue to resolve local names if the local
network becomes isolated from the internet due to gateway or link
failure.

In addition to the names and addresses of the servers, the SLIST data
structure can be sorted to use the best servers first, and to insure
that all addresses of all servers are used in a round-robin manner.  The
sorting can be a simple function of preferring addresses on the local
network over others, or may involve statistics from past events, such as
previous response times and batting averages.

Step 3 sends out queries until a response is received.  The strategy is
to cycle around all of the addresses for all of the servers with a
timeout between each transmission.  In practice it is important to use
all addresses of a multihomed host, and too aggressive a retransmission
policy actually slows response when used by multiple resolvers
contending for the same name server and even occasionally for a single
resolver.  SLIST typically contains data values to control the timeouts
and keep track of previous transmissions.

Step 4 involves analyzing responses.  The resolver should be highly
paranoid in its parsing of responses.  It should also check that the
response matches the query it sent using the ID field in the response.

The ideal answer is one from a server authoritative for the query which
either gives the required data or a name error.  The data is passed back
to the user and entered in the cache for future use if its TTL is
greater than zero.

If the response shows a delegation, the resolver should check to see
that the delegation is "closer" to the answer than the servers in SLIST
are.  This can be done by comparing the match count in SLIST with that
computed from SNAME and the NS RRs in the delegation.  If not, the reply
is bogus and should be ignored.  If the delegation is valid the NS
delegation RRs and any address RRs for the servers should be cached.
The name servers are entered in the SLIST, and the search is restarted.

If the response contains a CNAME, the search is restarted at the CNAME
unless the response has the data for the canonical name or if the CNAME
is the answer itself.
*/
@Deprecated
public class AnswerProcess implements Closeable {

    private enum Status {
        START,
        NET_QUERY,
        RESULT
    }

    private final DNSRequestClient netClient;

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
            this.netClient = new DNSRequestClient();
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
            boolean finished = internalDoIO(timeoutMillis, null); // TODO
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

    private boolean internalDoIO(int timeoutMillis, Selector selector) throws IOException, DNSException {

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
//                        if(!netClient.startQuery(message, currentIp, selector, null /*TODO*/)) return false;

                    case NET_QUERY:

//                        if(!netClient.doIO()) return false; TODO
//                        buffer = netClient.getResult();     TODO
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
