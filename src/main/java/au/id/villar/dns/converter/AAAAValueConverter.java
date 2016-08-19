/*
 * Copyright 2015-2016 Rafael Villar Villar
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
package au.id.villar.dns.converter;

import au.id.villar.dns.engine.RRValueConverter;
import au.id.villar.dns.engine.Utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converter for ResourceRecords of type AAAA (IPv6 address)
 */
public class AAAAValueConverter implements RRValueConverter {

    private static final Pattern IPv6_GROUP = Pattern.compile("[A-Fa-f0-9]++");
    private static final Pattern IPv6_ZERO_ABBREV = Pattern.compile("0(?::0)++");

    @Override
    public Object convertToRawData(Object data) {
        if(data instanceof String) return createFromString(data.toString());
        if(data instanceof Inet6Address) return createFromInet6Address((Inet6Address) data);
        if(data instanceof byte[]) return cloneFromArrayIfValid((byte[])data);
        throw new IllegalArgumentException("Object can't be converted to an IPv4 address: " + data);
    }

    @Override
    public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
        byte[] value = new byte[16];
        System.arraycopy(data, offset, value, 0, 16);
        return value;
    }

    @Override
    public <T> T convertValue(Object rawObject, Class<T> tClass) {

        byte[] arrayValue = (byte[])rawObject;
        Object result = null;
        if(tClass == String.class || tClass == Object.class) {
            result = convertToSring(arrayValue);
        } else if(tClass == InetAddress.class || tClass == Inet6Address.class) {
            try {
                result = Inet6Address.getByAddress(arrayValue);
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
    public int writeRawData(Object rawObject, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
        byte[] arrayValue = (byte[])rawObject;
        System.arraycopy(arrayValue, 0, array, offset, 16);
        return 16;
    }

    private byte[] createFromString(String str) {

        if(!Utils.isValidIPv6(str))
            throw new IllegalArgumentException("Invalid IPv6 address: " + str);

        if(str.startsWith("[") && str.endsWith("]")) str = str.substring(1, str.length() - 1);

        byte[] ipv6 = new byte[16];
        int abbrevPos = str.indexOf("::");
        int index = 0;
        Matcher matcher = IPv6_GROUP.matcher(str);
        while (matcher.find()) {
            if(abbrevPos > -1 && matcher.start() > abbrevPos) {
                abbrevPos = -1;
                index = 16 - index;
            }
            int group = Integer.valueOf(matcher.group(), 16);
            ipv6[index++] = (byte)(group >> 8);
            ipv6[index++] = (byte)group;
        }
        return ipv6;
    }

    private byte[] createFromInet6Address(Inet6Address inet6Address) {
        byte[] value = new byte[16];
        System.arraycopy(inet6Address.getAddress(), 0, value, 0, 16);
        return value;
    }

    private byte[] cloneFromArrayIfValid(byte[] array) {

        if(array.length != 16)
            throw new IllegalArgumentException("Invalid IPv6 address: " + Arrays.toString(array));

        return array.clone();
    }

    private String convertToSring(byte[] rawData) {
        StringBuilder builder = new StringBuilder(40);
        for(int index = 0; index < 16; index += 2) {
            if(builder.length() > 0) builder.append(':');
            int group = ((rawData[index] << 8) & 0xFF00) | (rawData[index + 1] & 0xFF);
            builder.append(Integer.toHexString(group));
        }
        int pos = -1;
        int length = 0;
        Matcher matcher = IPv6_ZERO_ABBREV.matcher(builder);
        while(matcher.find()) {
            int currentLength = matcher.end() - matcher.start();
            if(currentLength > length) {
                pos = matcher.start();
                length = currentLength;
            }
        }
        if(pos >= 0) {
            builder.replace(pos, pos + length, "");
            if(builder.length() == 0) {
                builder.append("::");
            } else if(builder.charAt(0) == ':') {
                builder.insert(0, ':');
            } else if(builder.charAt(builder.length() - 1) == ':') {
                builder.append(':');
            }
        }
        return builder.toString();
    }
}
