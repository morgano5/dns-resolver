package au.id.villar.dns.converter;

import au.id.villar.dns.engine.ParseResult;
import au.id.villar.dns.engine.RRValueConverter;
import au.id.villar.dns.engine.Utils;

import java.util.Map;

public class DomainNameValueConverter implements RRValueConverter {

	@Override
	public Object convertToRawData(Object data) {
		if(!(data instanceof String)) throw new IllegalArgumentException("Only String can be converted");
		String str = data.toString();
		if(!Utils.isValidDnsName(str)) throw new IllegalArgumentException("Not a valid domain name");
		return str;
	}

	@Override
	public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
		ParseResult<String> r = Utils.getDomainName(data, offset, previousNames);
		return r.value;
	}

	@Override
	public <T> T convertValue(Object value, Class<T> tClass) {
		if(tClass != String.class && tClass != Object.class)
			throw new IllegalArgumentException("only String conversions are possible");
		return tClass.cast(value);
	}

	@Override
	public int writeRawData(Object value, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		return Utils.writeDomainNameAndUpdateLinks(value.toString(), array, offset, linkOffset, nameLinks);
	}

}
