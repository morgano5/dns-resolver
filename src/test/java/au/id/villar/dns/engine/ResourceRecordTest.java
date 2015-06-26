package au.id.villar.dns.engine;

import au.id.villar.dns.engine.DnsEngine;
import au.id.villar.dns.engine.ParseResult;
import au.id.villar.dns.engine.ResourceRecord;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ResourceRecordTest {

	@Test
	public void writeRawDataTest() {

		byte[] data = {
				0, 0, 0, 0, 0,
				6, 'v', 'i', 'l', 'l', 'a', 'r', 2, 'm', 'e', 0,
				0, 1,
				0, 1,
				0, 0, 0, 5,
				0, 4,
				(byte)192, (byte)168, 0, 1
		};
		ParseResult<ResourceRecord> rrResult = new DnsEngine().createResourceRecordFromBuffer(data, 5, null);

		byte[] result = new byte[data.length];

		int usedBytes = rrResult.value.writeRawData(result, 5, 0, new HashMap<>());

		assertTrue(Arrays.equals(data, result));
		assertEquals(25, usedBytes);
	}
}
