package au.id.villar.dns;

import au.id.villar.dns.cache.DnsCache;

import java.util.List;

public interface ResolverBuilder {

	Resolver build();

	ResolverBuilder usingIPv4(boolean useIPv4);

	ResolverBuilder usingIPv6(boolean useIPv6);

	ResolverBuilder withCache(DnsCache cache);

	ResolverBuilder withRootServers(List<String> rootServers);

}
