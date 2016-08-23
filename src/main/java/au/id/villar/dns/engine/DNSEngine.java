/*
 * Copyright 2015 Rafael Villar Villar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package au.id.villar.dns.engine;

import au.id.villar.dns.converter.*;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DNSEngine {

    private static final int UDP_DATAGRAM_MAX_SIZE = 512;

    private Map<Short, DNSClass> classes = new HashMap<>();
    private Map<Short, DNSType> types = new HashMap<>();
    private Map<DNSClass, Map<DNSType, RRValueConverter>> converters = new HashMap<>();
    private Map<Short, RRValueConverter> internetConverters = new HashMap<>();
    private RRValueConverter defaultConverter = new DefaultConverter();

    public DNSEngine() {

        classes.put(DNSClass.IN_VALUE, DNSClass.IN);
        classes.put(DNSClass.ANY_VALUE, DNSClass.ANY);

        types.put(DNSType.A_VALUE, DNSType.A);
        types.put(DNSType.NS_VALUE, DNSType.NS);
        types.put(DNSType.MD_VALUE, DNSType.MD);
        types.put(DNSType.MF_VALUE, DNSType.MF);
        types.put(DNSType.CNAME_VALUE, DNSType.CNAME);
        types.put(DNSType.SOA_VALUE, DNSType.SOA);
        types.put(DNSType.MB_VALUE, DNSType.MB);
        types.put(DNSType.MG_VALUE, DNSType.MG);
        types.put(DNSType.MR_VALUE, DNSType.MR);
        types.put(DNSType.NULL_VALUE, DNSType.NULL);
        types.put(DNSType.WKS_VALUE, DNSType.WKS);
        types.put(DNSType.PTR_VALUE, DNSType.PTR);
        types.put(DNSType.HINFO_VALUE, DNSType.HINFO);
        types.put(DNSType.MINFO_VALUE, DNSType.MINFO);
        types.put(DNSType.MX_VALUE, DNSType.MX);
        types.put(DNSType.TXT_VALUE, DNSType.TXT);

        types.put(DNSType.AAAA_VALUE, DNSType.AAAA);

        types.put(DNSType.AXFR_VALUE, DNSType.AXFR);
        types.put(DNSType.MAILB_VALUE, DNSType.MAILB);
        types.put(DNSType.MAILA_VALUE, DNSType.MAILA);
        types.put(DNSType.ALL_VALUE, DNSType.ALL);

        internetConverters.put(DNSType.A_VALUE, new AValueConverter());
        internetConverters.put(DNSType.NS_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.MD_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.MF_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.CNAME_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.SOA_VALUE, new SoaValueConverter());
        internetConverters.put(DNSType.MB_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.MG_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.MR_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.NULL_VALUE, new UnknownValueConverter());
        internetConverters.put(DNSType.WKS_VALUE, new WksValueConverter());
        internetConverters.put(DNSType.PTR_VALUE, new DomainNameValueConverter());
        internetConverters.put(DNSType.HINFO_VALUE, new HinfoValueConverter());
        internetConverters.put(DNSType.MINFO_VALUE, new MinfoValueConverter());
        internetConverters.put(DNSType.MX_VALUE, new MxValueConverter());
        internetConverters.put(DNSType.TXT_VALUE, new TxtValueConverter());

        internetConverters.put(DNSType.AAAA_VALUE, new AAAAValueConverter());

    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSClass registerClass(short number, String mnemonic) {
        DNSClass valueMnemonic = new DNSClass(number, mnemonic.toUpperCase());
        return validateAndRegisterInMap(classes, valueMnemonic);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSClass getClass(short classNumber) {
        return classes.get(classNumber);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSClass getClass(String mnemonic) {
        for(DNSClass o: classes.values())
            if(o.getMnemonic().equals(mnemonic))
                return o;
        return null;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSType registerType(short number, String mnemonic) {
        DNSType valueMnemonic = new DNSType(number, mnemonic.toUpperCase());
        return validateAndRegisterInMap(types, valueMnemonic);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSType getType(short typeNumber) {
        return types.get(typeNumber);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSType getType(String mnemonic) {
        for(DNSType o: types.values())
            if(o.getMnemonic().equals(mnemonic))
                return o;
        return null;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void registerConverter(DNSClass dnsClass, DNSType dnsType, RRValueConverter converter) {
        if(dnsClass.equals(DNSClass.IN)) {
            internetConverters.put(dnsType.getValue(), converter);
        } else {
            Map<DNSType, RRValueConverter> converterMap = converters.get(dnsClass);
            if(converterMap == null) {
                converterMap = new HashMap<>();
                converters.put(dnsClass, converterMap);
            }
            converterMap.put(dnsType, converter);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public ResourceRecord createResourceRecord(String name, DNSType dnsType, DNSClass dnsClass, long ttl, Object data) {
        RRValueConverter converter = getConverter(dnsClass, dnsType);
        Object rawData = converter.convertToRawData(data);
        return new ResourceRecord(name, dnsType, dnsClass, ttl, converter, rawData);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public Question createQuestion(String qName, DNSType qType, DNSClass qClass) {
        if(!Utils.isValidDnsName(qName))
            throw new IllegalArgumentException("DNS name not valid");
        return new Question(qName, qType, qClass);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSMessage createSimpleQueryMessage(short id, Question question) {
        return createMessage(id, false, Opcode.QUERY, false, false, true, false,
                (byte)0, ResponseCode.NO_ERROR, new Question[] {question}, null, null, null);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSMessage createMessage(short id, boolean isResponse, Opcode opcode, boolean isAuthoritative,
            boolean wasTruncated, boolean recursionDesired, boolean recursionAvailable, byte reserved,
            ResponseCode responseCode, Question[] questions, ResourceRecord[] answers, ResourceRecord[] authorities,
            ResourceRecord[] additionals) {
        return new DNSMessage(id, isResponse, opcode, isAuthoritative, wasTruncated, recursionDesired,
                recursionAvailable, reserved, responseCode, questions, answers, authorities, additionals);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public DNSMessage createMessageFromBuffer(byte[] buffer, int offset) {

        Map<Integer, String> names = new HashMap<>();
        short id = (short)Utils.getInt(buffer, offset, 2);
        int auxInt = Utils.getInt(buffer, offset + 2, 2);
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

        Question[] questions;
        ResourceRecord[] answers;
        ResourceRecord[] authoritatives;
        ResourceRecord[] additionals;

        offset += DNSMessage.MESSAGE_HEADER_SIZE;
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

        return new DNSMessage(id, qr, opcode, aa, tc, rd, ra, z, rcode, questions, answers, authoritatives,
                additionals);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public ByteBuffer createBufferFromMessage(DNSMessage dnsMessage) {

        int aproxSize = UDP_DATAGRAM_MAX_SIZE;
        ByteBuffer buffer = null;
        while(buffer == null) {
            try {
                byte[] testingBuffer = new byte[aproxSize];
                int actualSize = dnsMessage.writeToBuffer(testingBuffer, 2);
                testingBuffer[0] = (byte)((actualSize >> 8) & 0xFF);
                testingBuffer[1] = (byte)(actualSize & 0xFF);
                actualSize += 2;
                buffer = ByteBuffer.allocate(actualSize);
                buffer.put(testingBuffer, 0, actualSize);
                buffer.position(0);
            } catch(IndexOutOfBoundsException e) {
                aproxSize += UDP_DATAGRAM_MAX_SIZE;
            }
        }
        return buffer;
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
        DNSType type = getRegisterType((short)Utils.getInt(buffer, offset, 2));
        offset += 2;
        DNSClass dnsClass = getRegisterClass((short)Utils.getInt(buffer, offset, 2));
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
        DNSType qType = getType((short) Utils.getInt(buffer, offset + dn.bytesUsed, 2));
        DNSClass qClass = getClass((short) Utils.getInt(buffer, offset + dn.bytesUsed + 2, 2));
        qr.value = new Question(qName, qType, qClass);
        qr.bytesUsed = dn.bytesUsed + 4;
        return qr;
    }

    DNSClass getRegisterClass(short classNumber) {
        DNSClass dnsClass = getClass(classNumber);
        if(dnsClass == null)
            dnsClass = registerClass(classNumber, "UNKNOWN_" + classNumber);
        return dnsClass;
    }

    DNSType getRegisterType(short typeNumber) {
        DNSType dnsType = getType(typeNumber);
        if(dnsType == null)
            dnsType = registerType(typeNumber, "UNKNOWN_" + typeNumber);
        return dnsType;
    }

    RRValueConverter getConverter(DNSClass dnsClass, DNSType dnsType) {
        RRValueConverter converter;
        if(dnsClass.equals(DNSClass.IN)) {
            converter = internetConverters.get(dnsType.getValue());
        } else {
            Map<DNSType, RRValueConverter> converterMap = converters.get(dnsClass);
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

        @Override
        public boolean areEqual(Object rawObject1, Object rawObject2) {
            return Arrays.equals((byte[])rawObject1, (byte[])rawObject2);
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
