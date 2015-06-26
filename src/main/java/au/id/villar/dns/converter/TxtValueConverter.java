package au.id.villar.dns.converter;

import au.id.villar.dns.engine.ParseResult;
import au.id.villar.dns.engine.RRValueConverter;
import au.id.villar.dns.engine.Utils;

import java.util.Map;

public class TxtValueConverter implements RRValueConverter {

	@Override
	public Object convertToRawData(Object data) {
		if(!(data instanceof String))
			throw new IllegalArgumentException("Only String type supported");
		return data.toString();
	}

	@Override
	public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
		StringBuilder strData = new StringBuilder(data.length - 1);
		int index = offset;
		int total = offset + length;
		while(index < total) {
			ParseResult<String> result = Utils.getText(data, index);
			strData.append(result.value);
			index += result.bytesUsed;
			if(index < total) strData.append(' ');
		}
		return strData.toString();
	}

	@Override
	public <T> T convertValue(Object value, Class<T> tClass) {
		if(tClass != String.class && tClass != Object.class)
			throw new IllegalArgumentException("Only String type supported");
		return tClass.cast(value);
	}

	@Override
	public int writeRawData(Object value, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		return Utils.writeText(value.toString(), array, offset);
	}

}
