package au.id.villar.dns.engine;

import java.util.Map;

/**
 * Class representing a DNS Standard question (See RFC-1035)
 */
public final class Question implements DnsItem {

	/* Fields for a query [RFC1035] */
	private final String dnsName;
	private final DnsType dnsType;
	private final DnsClass dnsClass;


	Question(String dnsName, DnsType dnsType, DnsClass dnsClass) {
		this.dnsName = dnsName;
		this.dnsType = dnsType;
		this.dnsClass = dnsClass;
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

	int writeRawData(byte[] buffer, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		int usedBytes;

		usedBytes = Utils.writeDomainNameAndUpdateLinks(dnsName, buffer, offset, linkOffset, nameLinks);
		Utils.writeShort(dnsType.getValue(), buffer, offset + usedBytes);
		usedBytes += 2;
		Utils.writeShort(dnsClass.getValue(), buffer, offset + usedBytes);
		usedBytes += 2;

		return usedBytes;
	}

}
