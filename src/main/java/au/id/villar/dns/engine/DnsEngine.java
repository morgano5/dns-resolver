package au.id.villar.dns.engine;

import au.id.villar.dns.converter.*;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class DnsEngine {

	private Map<Short, DnsClass> classes = new HashMap<>();
	private Map<Short, DnsType> types = new HashMap<>();
	private Map<DnsClass, Map<DnsType, RRValueConverter>> converters = new HashMap<>();
	private Map<Short, RRValueConverter> internetConverters = new HashMap<>();
	private RRValueConverter defaultConverter = new DefaultConverter();

	public DnsEngine() {

		classes.put(DnsClass.IN_VALUE, DnsClass.IN);
		classes.put(DnsClass.ANY_VALUE, DnsClass.ANY);

		types.put(DnsType.A_VALUE, DnsType.A);
		types.put(DnsType.NS_VALUE, DnsType.NS);
		types.put(DnsType.MD_VALUE, DnsType.MD);
		types.put(DnsType.MF_VALUE, DnsType.MF);
		types.put(DnsType.CNAME_VALUE, DnsType.CNAME);
		types.put(DnsType.SOA_VALUE, DnsType.SOA);
		types.put(DnsType.MB_VALUE, DnsType.MB);
		types.put(DnsType.MG_VALUE, DnsType.MG);
		types.put(DnsType.MR_VALUE, DnsType.MR);
		types.put(DnsType.NULL_VALUE, DnsType.NULL);
		types.put(DnsType.WKS_VALUE, DnsType.WKS);
		types.put(DnsType.PTR_VALUE, DnsType.PTR);
		types.put(DnsType.HINFO_VALUE, DnsType.HINFO);
		types.put(DnsType.MINFO_VALUE, DnsType.MINFO);
		types.put(DnsType.MX_VALUE, DnsType.MX);
		types.put(DnsType.TXT_VALUE, DnsType.TXT);

		types.put(DnsType.AAAA_VALUE, DnsType.AAAA);

		types.put(DnsType.AXFR_VALUE, DnsType.AXFR);
		types.put(DnsType.MAILB_VALUE, DnsType.MAILB);
		types.put(DnsType.MAILA_VALUE, DnsType.MAILA);
		types.put(DnsType.ALL_VALUE, DnsType.ALL);

		internetConverters.put(DnsType.A_VALUE, new AValueConverter());
		internetConverters.put(DnsType.NS_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.MD_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.MF_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.CNAME_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.SOA_VALUE, new SoaValueConverter());
		internetConverters.put(DnsType.MB_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.MG_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.MR_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.NULL_VALUE, new UnknownValueConverter());
		internetConverters.put(DnsType.WKS_VALUE, new WksValueConverter());
		internetConverters.put(DnsType.PTR_VALUE, new DomainNameValueConverter());
		internetConverters.put(DnsType.HINFO_VALUE, new HinfoValueConverter());
		internetConverters.put(DnsType.MINFO_VALUE, new MinfoValueConverter());
		internetConverters.put(DnsType.MX_VALUE, new MxValueConverter());
		internetConverters.put(DnsType.TXT_VALUE, new TxtValueConverter());

		internetConverters.put(DnsType.AAAA_VALUE, new AAAAValueConverter());

	}

	public DnsClass registerClass(short number, String mnemonic) {
		DnsClass valueMnemonic = new DnsClass(number, mnemonic.toUpperCase());
		return validateAndRegisterInMap(classes, valueMnemonic);
	}

	public DnsClass getClass(short classNumber) {
		return classes.get(classNumber);
	}

	public DnsClass getClass(String mnemonic) {
		for(DnsClass o: classes.values())
			if(o.getMnemonic().equals(mnemonic))
				return o;
		return null;
	}

	public DnsType registerType(short number, String mnemonic) {
		DnsType valueMnemonic = new DnsType(number, mnemonic.toUpperCase());
		return validateAndRegisterInMap(types, valueMnemonic);
	}

	public DnsType getType(short typeNumber) {
		return types.get(typeNumber);
	}

	public DnsType getType(String mnemonic) {
		for(DnsType o: types.values())
			if(o.getMnemonic().equals(mnemonic))
				return o;
		return null;
	}

	public void registerConverter(DnsClass dnsClass, DnsType dnsType, RRValueConverter converter) {
		if(dnsClass.equals(DnsClass.IN)) {
			internetConverters.put(dnsType.getValue(), converter);
		} else {
			Map<DnsType, RRValueConverter> converterMap = converters.get(dnsClass);
			if(converterMap == null) {
				converterMap = new HashMap<>();
				converters.put(dnsClass, converterMap);
			}
			converterMap.put(dnsType, converter);
		}
	}

	public ResourceRecord createResourceRecord(String name, DnsType dnsType, DnsClass dnsClass, long ttl, Object data) {
		RRValueConverter converter = getConverter(dnsClass, dnsType);
		Object rawData = converter.convertToRawData(data);
		return new ResourceRecord(name, dnsType, dnsClass, ttl, converter, rawData);
	}

	public Question createQuestion(String qName, DnsType qType, DnsClass qClass) {
		if(!Utils.isValidDnsName(qName))
			throw new IllegalArgumentException("DNS name not valid");
		return new Question(qName, qType, qClass);
	}

	public DnsMessage createMessage(short id, boolean isResponse, Opcode opcode, boolean isAuthoritative,
			boolean wasTruncated, boolean recursionDesired, boolean recursionAvailable, byte reserved,
			ResponseCode responseCode, Question[] questions, ResourceRecord[] answers, ResourceRecord[] authorities,
			ResourceRecord[] additionals) {
		return new DnsMessage(id, isResponse, opcode, isAuthoritative, wasTruncated, recursionDesired,
				recursionAvailable, reserved, responseCode, questions, answers, authorities, additionals);
	}

	public DnsMessage createMessageFromBuffer(byte [] buffer, int offset) {

		Map<Integer, String> names = new HashMap<>();
		int auxInt = Utils.getInt(buffer, offset + 2, 2);
		short id = (short)Utils.getInt(buffer, offset, 2);
		boolean qr = ((auxInt & 0x8000) != 0);
		Opcode opcode = Opcode.getOpcode((byte)((auxInt >> 11) & 0xF));
		boolean aa = ((auxInt & 0x0400) != 0);
		boolean tc = ((auxInt & 0x0200) != 0);
		boolean rd = ((auxInt & 0x0100) != 0);
		boolean ra = ((auxInt & 0x0080) != 0);
		byte z = (byte)((auxInt >> 4) & 0x7);
		ResponseCode rcode = ResponseCode.getResponseCode((byte)(auxInt & 0xF));
		int qdcount = Utils.getInt(buffer, offset + 4, 2);
		int ancount = Utils.getInt(buffer, offset + 6, 2);
		int nscount = Utils.getInt(buffer, offset + 8, 2);
		int arcount = Utils.getInt(buffer, offset + 10, 2);

		Question[] questions = null;
		ResourceRecord[] answers = null;
		ResourceRecord[] authoritatives = null;
		ResourceRecord[] additionals = null;

		offset += DnsMessage.MESSAGE_HEADER_SIZE;
		ParseResult<Question[]> resultQuestions = readResourceRecordGroup(qdcount, buffer, offset, names,
				this::createQuestionFromBuffer, Question.class);
		questions = resultQuestions.value;
		offset += resultQuestions.bytesUsed;
		ParseResult<ResourceRecord[]> resultRr;
		resultRr = readResourceRecordGroup(ancount, buffer, offset, names, this::createResourceRecordFromBuffer,
				ResourceRecord.class);
		answers = resultRr.value;
		offset += resultRr.bytesUsed;
		resultRr = readResourceRecordGroup(nscount, buffer, offset, names, this::createResourceRecordFromBuffer,
				ResourceRecord.class);
		authoritatives = resultRr.value;
		offset += resultRr.bytesUsed;
		resultRr = readResourceRecordGroup(arcount, buffer, offset, names, this::createResourceRecordFromBuffer,
				ResourceRecord.class);
		additionals = resultRr.value;

		if(additionals.length < arcount) tc = true;

		return new DnsMessage(id, qr, opcode, aa, tc, rd, ra, z, rcode, questions, answers, authoritatives,
				additionals);
	}

	private interface MessageItemReader<T> {
		ParseResult<T> readData(byte[] buffer, int offset, Map<Integer, String> names);
	}

	ParseResult<ResourceRecord> createResourceRecordFromBuffer(byte[] buffer, int offset,
															   Map<Integer, String> previousNames) {
		ParseResult<ResourceRecord> r = new ParseResult<>();
		int length;
		ParseResult<String> dn = Utils.getDomainName(buffer, offset, previousNames);
		offset += dn.bytesUsed;
		DnsType type = getRegisterType((short)Utils.getInt(buffer, offset, 2));
		offset += 2;
		DnsClass dnsClass = getRegisterClass((short)Utils.getInt(buffer, offset, 2));
		offset += 2;
		long ttl = Utils.getInt(buffer, offset, 4);
		offset += 4;
		length = Utils.getInt(buffer, offset, 2);
		offset += 2;
		RRValueConverter converter = getConverter(dnsClass, type);
		Object data = converter.getData(buffer, offset, length, previousNames);
		r.value = new ResourceRecord(dn.value, type, dnsClass, ttl, converter, data);
		r.bytesUsed = dn.bytesUsed + length + 10;
		return r;
	}

	ParseResult<Question> createQuestionFromBuffer(byte[] buffer, int offset, Map<Integer, String> previousNames) {
		ParseResult<Question> qr = new ParseResult<>();
		ParseResult<String> dn = Utils.getDomainName(buffer, offset, previousNames);
		String qName = dn.value;
		DnsType qType = getType((short) Utils.getInt(buffer, offset + dn.bytesUsed, 2));
		DnsClass qClass = getClass((short) Utils.getInt(buffer, offset + dn.bytesUsed + 2, 2));
		qr.value = new Question(qName, qType, qClass);
		qr.bytesUsed = dn.bytesUsed + 4;
		return qr;
	}

	DnsClass getRegisterClass(short classNumber) {
		DnsClass dnsClass = getClass(classNumber);
		if(dnsClass == null)
			dnsClass = registerClass(classNumber, "UNKNOWN_" + classNumber);
		return dnsClass;
	}

	DnsType getRegisterType(short typeNumber) {
		DnsType dnsType = getType(typeNumber);
		if(dnsType == null)
			dnsType = registerType(typeNumber, "UNKNOWN_" + typeNumber);
		return dnsType;
	}

	RRValueConverter getConverter(DnsClass dnsClass, DnsType dnsType) {
		RRValueConverter converter;
		if(dnsClass.equals(DnsClass.IN)) {
			converter = internetConverters.get(dnsType.getValue());
		} else {
			Map<DnsType, RRValueConverter> converterMap = converters.get(dnsClass);
			converter = converterMap == null? null: converterMap.get(dnsType);
		}
		return converter != null? converter: defaultConverter;
	}

	private class DefaultConverter implements RRValueConverter {

		@Override
		public Object convertToRawData(Object data) {
			if(!(data instanceof byte[])) throw new IllegalArgumentException("only byte[] can be converted");
			byte[] arrayData = (byte[])data;
			byte[] rawData = new byte[arrayData.length];
			System.arraycopy(arrayData, 0, rawData, 0, arrayData.length);
			return rawData;
		}

		@Override
		public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
			byte[] rawData = new byte[length];
			System.arraycopy(data, offset, rawData, 0, length);
			return rawData;
		}

		@Override
		public <T> T convertValue(Object rawObject, Class<T> tClass) {
			if(tClass != byte[].class && tClass != Object.class)
				throw new IllegalArgumentException("only byte[] can be converted");
			return tClass.cast(((byte[])rawObject).clone());
		}

		@Override
		public int writeRawData(Object rawObject, byte[] array, int offset, int linkOffset,
				Map<String, Integer> nameLinks) {
			byte[] arrayObject = (byte[])rawObject;
			System.arraycopy(arrayObject, 0, array, offset, arrayObject.length);
			return arrayObject.length;
		}

	}

	@SuppressWarnings("unchecked")
	private <T> ParseResult<T[]> readResourceRecordGroup(int count, byte[] buffer, int offset,
			Map<Integer, String> names, MessageItemReader<T> reader, Class<T> itemClass) {
		int start = offset;
		ParseResult<T> rr;
		T[] items = (T[])Array.newInstance(itemClass, count);
		int index = 0;
		try {
			for (index = 0; index < count; index++) {
				rr = reader.readData(buffer, offset, names);
				items[index] = rr.value;
				offset += rr.bytesUsed;
			}
		} catch(IndexOutOfBoundsException e) {
			T[] actualResult = (T[])Array.newInstance(itemClass, index);
			System.arraycopy(items, 0, actualResult, 0, index);
			items = actualResult;
		}
		ParseResult<T[]> result = new ParseResult<>();
		result.value = items;
		result.bytesUsed = offset - start;
		return result;
	}

	private <T extends ValueMnemonic> T validateAndRegisterInMap(Map<Short, T> predefinedList, T valueMnemonic) {
		for(Map.Entry<Short, T> entry: predefinedList.entrySet()) {
			if(entry.getKey().equals(valueMnemonic.getValue())) {
				if(entry.getValue().getMnemonic().equals(valueMnemonic.getMnemonic())) {
					return entry.getValue();
				} else {
					throw new IllegalArgumentException("type already registered with other mnemonic: "
							+ entry.getValue());
				}
			} else if(entry.getValue().getMnemonic().equals(valueMnemonic.getMnemonic())) {
				throw new IllegalArgumentException("type already registered with other number: " + entry.getValue());
			}
		}
		predefinedList.put(valueMnemonic.getValue(), valueMnemonic);
		return valueMnemonic;
	}
}
