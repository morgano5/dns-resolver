package au.id.villar.dns.cache;

import au.id.villar.dns.engine.DnsClass;
import au.id.villar.dns.engine.DnsItem;
import au.id.villar.dns.engine.DnsType;
import au.id.villar.dns.engine.ResourceRecord;

public class CachedResourceRecord implements DnsItem {

	private final ResourceRecord wrapped;
	private final long bestBefore;

	public CachedResourceRecord(ResourceRecord wrapped) {
		this.wrapped = wrapped;
		this.bestBefore = wrapped.getSecondsCache() * 1000L + System.currentTimeMillis();
	}

	public <T> T getData(Class<T> tClass) {
		return wrapped.getData(tClass);
	}

	@Override
	public DnsClass getDnsClass() {
		return wrapped.getDnsClass();
	}

	@Override
	public String getDnsName() {
		return wrapped.getDnsName();
	}

	@Override
	public DnsType getDnsType() {
		return wrapped.getDnsType();
	}

	public boolean isExpired() {
		return bestBefore - System.currentTimeMillis() < 0;
	}

	public ResourceRecord getResourceRecord() {
		return wrapped;
	}
}
