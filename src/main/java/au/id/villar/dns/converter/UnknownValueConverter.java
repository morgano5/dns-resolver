package au.id.villar.dns.converter;

import au.id.villar.dns.engine.RRValueConverter;

import java.util.Map;

public class UnknownValueConverter implements RRValueConverter {

	@Override
	public Object convertToRawData(Object data) {
		if(!(data instanceof byte[])) throw new IllegalArgumentException("Only byte[] is allowed");
		byte[] byteData = (byte[])data;
		byte[] value = new byte[byteData.length];
		System.arraycopy(byteData, 0, value, 0, byteData.length);
		return value;
	}

	@Override
	public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
		byte[] value = new byte[length];
		System.arraycopy(data, offset, value, 0, length);
		return value;
	}

	@Override
	public <T> T convertValue(Object value, Class<T> tClass) {
		if(tClass != byte[].class && tClass != Object.class)
			throw new IllegalArgumentException("Only byte[] is allowed");
		return tClass.cast(((byte[])value).clone());
	}

	@Override
	public int writeRawData(Object value, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		byte[] arrayValue = (byte[])value;
		System.arraycopy(arrayValue, 0, array, offset, arrayValue.length);
		return arrayValue.length;
	}

}
