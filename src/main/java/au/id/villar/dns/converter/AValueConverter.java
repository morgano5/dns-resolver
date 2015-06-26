package au.id.villar.dns.converter;

import au.id.villar.dns.engine.RRValueConverter;
import au.id.villar.dns.engine.Utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

public class AValueConverter implements RRValueConverter {

	@Override
	public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
		byte[] value = new byte[4];
		System.arraycopy(data, offset, value, 0, 4);
		return value;
	}

	@Override
	public Object convertToRawData(Object data) {
		if(data instanceof String) return createFromString(data.toString());
		if(data instanceof Inet4Address) return createFromInet4Address((Inet4Address)data);
		if(data instanceof byte[]) return cloneFromArrayIfValid((byte[])data);
		throw new IllegalArgumentException("Object can't be converted to an IPv4 address: " + data);
	}

	@Override
	public <T> T convertValue(Object value, Class<T> tClass) {
		byte[] arrayValue = (byte[])value;
		Object result = null;
		if(tClass == String.class || tClass == Object.class) {
			int ip = Utils.getInt(arrayValue, 0, 4);
			result = String.valueOf((ip >> 24) & 0xff) + '.' + ((ip >> 16) & 0xff) + '.'
					+ ((ip >> 8) & 0xff) + '.' + (ip & 0xff);
		} else if(tClass == InetAddress.class || tClass == Inet4Address.class) {
			try {
				result = Inet4Address.getByAddress(arrayValue);
			} catch (UnknownHostException e) {
				throw new RuntimeException("internal error: " + e.getMessage(), e);
			}
		} else if(tClass == byte[].class) {
			result = arrayValue.clone();
		}

		if(result == null)
			throw new IllegalArgumentException("conversion not supported for type A: " + tClass.getName());

		return tClass.cast(result);
	}

	@Override
	public int writeRawData(Object value, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		byte[] arrayValue = (byte[])value;
		System.arraycopy(arrayValue, 0, array, offset, 4);
		return 4;
	}

	private byte[] createFromString(String str) {

		if(!Utils.isValidIPv4(str))
			throw new IllegalArgumentException("Invalid IPv4 address: " + str);

		byte[] value = new byte[4];
		int pos = 0;
		for(int index = 0; index < 4; index++) {
			int octectValue = 0;
			char ch;
			while(pos < str.length() && (ch = str.charAt(pos)) != '.')
				octectValue = octectValue * 10 + (ch - '0');
			value[pos] = (byte)octectValue;
		}
		return value;

	}

	private byte[] createFromInet4Address(Inet4Address inet4Address) {
		byte[] value = new byte[4];
		System.arraycopy(inet4Address.getAddress(), 0, value, 0, 4);
		return value;
	}

	private byte[] cloneFromArrayIfValid(byte[] array) {

		if(array.length != 4)
			throw new IllegalArgumentException("Invalid IPv4 address: " + Arrays.toString(array));

		return array.clone();
	}
}
