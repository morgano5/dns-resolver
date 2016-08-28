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

import au.id.villar.dns.cache.DNSCache;
import au.id.villar.dns.engine.*;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

@Deprecated // This is doing things the wrong way
public class Resolver {

    private final DNSEngine engine = new DNSEngine();
    private final List<String> dnsRootServers;
    private final DNSCache cache;
    private final boolean useIPv4;
    private final boolean useIPv6;

    private Resolver(DNSCache cache, List<String> dnsRootServers, boolean useIPv4, boolean useIPv6) {
        this.cache = cache != null? cache: createDummyCache();
        this.dnsRootServers = createDnsRootServers(dnsRootServers, useIPv4, useIPv6);
        this.useIPv4 = useIPv4;
        this.useIPv6 = useIPv6;
    }

    public AnswerProcess lookup(String name, DNSType type) throws DNSException {
        return null;//todo new AnswerProcess(name, type, this, engine, getShuffledRootServers(), cache, useIPv4, useIPv6);
    }

    @SuppressWarnings("unused")
    public static ResolverBuilder usingIPv4(boolean useIPv4) {
        return new ResolverBuilder().usingIPv4(useIPv4);
    }

    @SuppressWarnings("unused")
    public static ResolverBuilder usingIPv6(boolean useIPv6) {
        return new ResolverBuilder().usingIPv6(useIPv6);
    }

    @SuppressWarnings("unused")
    public static ResolverBuilder withCache(DNSCache cache) {
        return new ResolverBuilder().withCache(cache);
    }

    @SuppressWarnings("unused")
    public static ResolverBuilder withRootServers(List<String> rootServers) {
        return new ResolverBuilder().withRootServers(rootServers);
    }

    private List<String> getShuffledRootServers() {
        List<String> rootServers = new ArrayList<>(dnsRootServers);
        Collections.shuffle(rootServers);
        return rootServers;
    }

    private List<String> createDnsRootServers(List<String> rootServers, boolean useIPv4, boolean useIPv6) {
        List<String> sanitizedServers = new ArrayList<>(rootServers.size());
        for(String server: rootServers) {
            if(server == null) throw new InternalException("root server address with null value");
            if(Utils.isValidIPv4(server)) {
                if(!useIPv4)
                    throw new InternalException(
                            server + " is an IPv4 address but this resolver is not configured to use IPv4");
            } else if(Utils.isValidIPv6(server)) {
                if(!useIPv6)
                    throw new InternalException(
                            server + " is an IPv6 address but this resolver is not configured to use IPv6");
            } else {
                throw new InternalException(server + " is not a valid IPv4 or IPv6 address");
            }
            sanitizedServers.add(server);
        }
        return Collections.unmodifiableList(sanitizedServers);
    }

    private DNSCache createDummyCache() {

        return new DNSCache() {

            @Override
            public void addResourceRecord(ResourceRecord resourceRecord) {
            }

            @Override
            public List<ResourceRecord> getResourceRecords(Question question, long timeout) {
                return Collections.emptyList();
            }

            @Override
            public void clear() {
            }

            @Override
            public boolean getResourceRecords(Question question, Selector selector, ResourceRecordHandler handler) {
                return getResourceRecords(question, handler);
            }

            @Override
            public boolean getResourceRecords(Question question, ResourceRecordHandler handler) {
                handler.handleResourceRecord(Collections.emptyList(), null);
                return true;
            }

            @Override
            public void processAttachment(SelectionKey selectionKey) {
            }

            @Override
            public boolean retryGetResourceRecords() {
                return true;
            }
        };

    }



    private class InternalException extends RuntimeException {
        public InternalException(String message) {
            super(message);
        }
    }



    public static class ResolverBuilder {

        private DNSCache cache;
        private List<String> rootServers;
        private boolean useIPv4 = true;
        private boolean useIPv6;

        private ResolverBuilder() {
        }

        public Resolver build() {
            return new Resolver(cache, rootServers, useIPv4, useIPv6);
        }

        public ResolverBuilder usingIPv4(boolean useIPv4) {
            this.useIPv4 = useIPv4;
            return this;
        }

        public ResolverBuilder usingIPv6(boolean useIPv6) {
            this.useIPv6 = useIPv6;
            return this;
        }

        public ResolverBuilder withCache(DNSCache cache) {
            this.cache = cache;
            return this;
        }

        public ResolverBuilder withRootServers(List<String> rootServers) {
            this.rootServers = rootServers;
            return this;
        }
    }


}
