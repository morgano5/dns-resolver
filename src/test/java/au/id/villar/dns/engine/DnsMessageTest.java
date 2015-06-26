package au.id.villar.dns.engine;

import au.id.villar.dns.engine.DnsEngine;
import au.id.villar.dns.engine.DnsMessage;
import org.junit.Test;

import static org.junit.Assert.*;

public class DnsMessageTest {

	@Test
	public void writeToBufferTest() {

		byte[] rawData = { 1, -128, (byte)0x95, (byte)0xA4, 0, 0, 0, 0, 0, 0, 0, 0 };

		DnsMessage message = new DnsEngine().createMessageFromBuffer(rawData, 0);
		byte[] test = new byte[rawData.length + 5];
		message.writeToBuffer(test, 5);

		for(int index = 0; index < rawData.length; index++) {
			assertEquals(rawData[index], test[index + 5]);
		}
	}
}
