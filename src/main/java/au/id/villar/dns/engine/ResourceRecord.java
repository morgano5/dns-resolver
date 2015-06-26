package au.id.villar.dns.engine;

import java.util.Map;

public final class ResourceRecord implements DnsItem {

	private final String dnsName;
	private final DnsType dnsType;
	private final DnsClass dnsClass;
	private final long ttl;
	private final RRValueConverter converter;
	private final Object data;

	ResourceRecord(String dnsName, DnsType dnsType, DnsClass dnsClass, long ttl, RRValueConverter converter, Object data) {
		this.dnsName = dnsName;
		this.dnsType = dnsType;
		this.dnsClass = dnsClass;
		this.ttl = ttl;
		this.converter = converter;
		this.data = data;
	}

	public String getDnsName() {
		return dnsName;
	}

	public DnsType getDnsType() {
		return dnsType;
	}

	public DnsClass getDnsClass() {
		return dnsClass;
	}

	public long getSecondsCache() {
		return ttl;
	}

	public <T> T getData(Class<T> tClass) {
		return converter.convertValue(data, tClass);
	}

	int writeRawData(byte[] buffer, int offset, int linkOffset, Map<String, Integer> nameLinks) {

		int start = offset;
		int usedBytes;

		usedBytes = Utils.writeDomainNameAndUpdateLinks(dnsName, buffer, offset, linkOffset, nameLinks);
		offset += usedBytes;
		Utils.writeShort(dnsType.getValue(), buffer, offset);
		offset += 2;
		Utils.writeShort(dnsClass.getValue(), buffer, offset);
		offset += 2;
		Utils.writeInt((int) ttl, buffer, offset);
		offset += 6;
		usedBytes = converter.writeRawData(data, buffer, offset, linkOffset + offset, nameLinks);
		if (usedBytes < 0) return -1;
		offset -= 2;
		Utils.writeShort((short) usedBytes, buffer, offset);
		return offset - start + 2 + usedBytes;
	}
}
