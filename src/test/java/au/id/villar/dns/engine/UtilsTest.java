package au.id.villar.dns.engine;

import au.id.villar.dns.engine.ParseResult;
import au.id.villar.dns.engine.Utils;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UtilsTest {

	@Test
	public void isValidDnsTest() {

		assertTrue(Utils.isValidDnsName("www.villar.me"));
		assertTrue(Utils.isValidDnsName("www.villar-second.me"));
		assertTrue(Utils.isValidDnsName("www.X-9.me"));

		assertFalse("DNS labels shouldn't finish in dash", Utils.isValidDnsName("www.X-.me"));
		assertFalse("DNS labels shouldn't start with number", Utils.isValidDnsName("www.9-X.me"));
		assertFalse("DNS labels shouldn't start with dash", Utils.isValidDnsName("www.-X.me"));
		assertFalse("DNS labels shouldn't be longer than 63 octects",
				Utils.isValidDnsName("www.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.me"));
		assertFalse("DNS names shouldn't be longer than 253 octects",
				Utils.isValidDnsName("www.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.me"));

	}

	@Test
	public void isValidIPv4Test() {

		assertTrue(Utils.isValidIPv4("192.168.0.1"));
		assertTrue(Utils.isValidIPv4("255.255.255.255"));
		assertTrue(Utils.isValidIPv4("192.168.0.10"));
		assertTrue(Utils.isValidIPv4("192.168.000.001"));

		assertFalse(Utils.isValidIPv4(""));
		assertFalse(Utils.isValidIPv4("300.300.0.1"));
		assertFalse(Utils.isValidIPv4("260.260.0.1"));
		assertFalse(Utils.isValidIPv4("256.256.0.1"));

	}

	@Test
	public void isValidIPv6() {

		assertTrue(Utils.isValidIPv6("[AAAA:BBBB:cccc:dddd:1111:2222:3333:4444]"));
		assertTrue(Utils.isValidIPv6("AAAA:BBBB:cccc:dddd:1111:2222:3333:4444"));
		assertTrue(Utils.isValidIPv6("0AAA:B:ccc:0:0:2222:3333:4444"));
		assertTrue(Utils.isValidIPv6("0AAA:B:ccc::2222:3333:4444"));
		assertTrue(Utils.isValidIPv6("::1"));
		assertTrue(Utils.isValidIPv6("A::"));
		assertTrue(Utils.isValidIPv6("::"));
		assertTrue(Utils.isValidIPv6("[::]"));

		assertFalse(Utils.isValidIPv6(""));
		assertFalse(Utils.isValidIPv6("[AAAA:BBBB:cccc:dddd:1111:2222:3333:4444"));
		assertFalse(Utils.isValidIPv6("AAAA:BBBB:cccc:dddd:1111:2222:3333:4444]"));
		assertFalse(Utils.isValidIPv6("AAAA:BBBB:cccc:dddd:1111:2222:3333:4444:5555"));
		assertFalse(Utils.isValidIPv6("AAAA:BBBB:cccc:dddd:1111:2222:33333:4444"));
		assertFalse(Utils.isValidIPv6("AAAA:BBBB:cccc:dddd:1111:2222:3333"));
		assertFalse(Utils.isValidIPv6("AAAA:BBBB:cccc:dddd::1111:2222:3333:4444:5555"));
		assertFalse(Utils.isValidIPv6("AAAA:BBBB:cccc:dddd::1111:2222:3333:4444"));
		assertFalse(Utils.isValidIPv6("whatever else"));
	}

	@Test
	public void getIntTest() {

		assertEquals(5, Utils.getInt(new byte[]{0, 6, (byte) 255, 0, 5, 9}, 4, 1));
		assertEquals(5, Utils.getInt(new byte[]{0, 6, (byte) 255, 0, 5, 9}, 3, 2));
		assertEquals(16711685, Utils.getInt(new byte[]{0, 6, (byte) 255, 0, 5, 9}, 2, 3));
		assertEquals(117374981, Utils.getInt(new byte[]{0, 6, (byte) 255, 0, 5, 9}, 1, 4));

		assertEquals(16843009, Utils.getInt(new byte[]{1, 1, 1, 1}, 0, 4));

	}

	@Test
	public void basicGetDomainNameTest() {

		byte[] data = {
				0, 0, 0, 0, 0,
				6, 'v', 'i', 'l', 'l', 'a', 'r', 2, 'm', 'e', 0
		};

		ParseResult<String> result = Utils.getDomainName(data, 5, null);
		assertEquals("villar.me", result.value);
		assertEquals(11, result.bytesUsed);
	}

	@Test
	public void getDomainNameWithPointerTest() {

		byte[] data = {
				0, 0, 0, 0, 0,
				6, 'v', 'i', 'l', 'l', 'a', 'r', (byte)0xC0, 25
		};

		Map<Integer, String> pointers = new HashMap<>();
		pointers.put(25, "com.au");

		ParseResult<String> result = Utils.getDomainName(data, 5, pointers);
		assertEquals("villar.com.au", result.value);
		assertEquals(9, result.bytesUsed);
	}

	@Test
	public void writeDomainNameTest() {

		Map<String, Integer> nameLinks = new HashMap<>();

		String domainName = "villar.me";

		byte[] buffer = new byte[domainName.length() + 5];

		byte[] expected = { 0, 0, 0, 6, 'v', 'i', 'l', 'l', 'a', 'r', 2, 'm', 'e', 0 };

		int usedBytes = Utils.writeDomainNameAndUpdateLinks(domainName, buffer, 3, 0, nameLinks);

		assertTrue(Arrays.equals(expected, buffer));
		assertEquals(11, usedBytes);
		assertEquals(2, nameLinks.size());
		assertEquals(new Integer(0), nameLinks.get(domainName));

	}

	@Test
	public void writeDomainNameWithPreviousLinksTest() {

		Map<String, Integer> nameLinks = new HashMap<>();
		nameLinks.put("com.au", 25);

		String domainName = "villar.com.au";

		byte[] buffer = new byte[12];

		byte[] expected = { 0, 0, 0, 6, 'v', 'i', 'l', 'l', 'a', 'r', (byte)0xC0, 25 };

		int usedBytes = Utils.writeDomainNameAndUpdateLinks(domainName, buffer, 3, 8, nameLinks);

		assertTrue(Arrays.equals(expected, buffer));
		assertEquals(9, usedBytes);
		assertEquals(2, nameLinks.size());
		assertEquals(new Integer(8), nameLinks.get(domainName));

	}

	@Test
	public void writeDomainNameUpdatingNamesTest() {

		Map<String, Integer> nameLinks = new HashMap<>();

		String domainName = "villar.com.au";

		byte[] buffer = new byte[2048];

		int usedBytes = Utils.writeDomainNameAndUpdateLinks(domainName, buffer, 3, 5, nameLinks);

		assertEquals(15, usedBytes);
		assertEquals(3, nameLinks.size());
		assertEquals(new Integer(5), nameLinks.get(domainName));
		assertEquals(new Integer(12), nameLinks.get("com.au"));
		assertEquals(new Integer(16), nameLinks.get("au"));

	}

	@Test
	public void getTextTest() {

		String expected = "Hola mundo";
		byte[] bytesFromExpected = expected.getBytes();
		byte[] testingInput = new byte[bytesFromExpected.length + 1];
		System.arraycopy(bytesFromExpected, 0, testingInput, 1, bytesFromExpected.length);
		testingInput[0] = (byte)bytesFromExpected.length;

		ParseResult<String> result = Utils.getText(testingInput, 0);

		assertEquals(bytesFromExpected.length + 1, result.bytesUsed);
		assertEquals(expected, result.value);
	}

}
