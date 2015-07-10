package au.id.villar.dns.cache;

import au.id.villar.dns.engine.*;

import java.util.List;

public interface DnsCache {

	void addResourceRecord(ResourceRecord resourceRecord);

	List<CachedResourceRecord> getResourceRecords(Question question);

	void removeResourceRecord(DnsItem resourceRecord);

}
