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

import java.util.Map;
import java.util.regex.*;

public class Utils {

    private static final Pattern DNS_NAME_PATTERN = getDnsNamePattern();

    private static final Pattern PSEUDO_IPV4_PATTERN = getIPv4Pattern();

    /**
     * Gets an integer from an array, big endian, starting form startIndex and taking the number of
     * bytes specified.
     * @param array array from which the int is obtained.
     * @param startIndex index in the array to which start from
     * @param numBytes number of bytes that should be taken, 1 to 4.
     * @return the calclated integer.
     */
    public static int getInt(byte[] array, int startIndex, int numBytes) {
        int value = 0;
        int endIndex = startIndex + numBytes - 1;
        for(int pos = startIndex; pos <= endIndex; pos++) {
            value |= ((array[pos] & 0xFF) << ((endIndex - pos) * 8));
        }
        return value;
    }

    public static ParseResult<String> getText(byte[] buffer, int offset) {
        // TODO dns i18n
        ParseResult<String> result = new ParseResult<>();
        StringBuilder sBuilder = new StringBuilder();
        int length = buffer[offset++];
        result.bytesUsed = length + 1;
        while(length-- > 0) sBuilder.append((char)buffer[offset++]);
        result.value = sBuilder.toString();
        return result;
    }


    public static ParseResult<String> getDomainName(byte[] buffer, int offset,
                                                    Map<Integer, String> previousNames) {
        // TODO dns i18n
        ParseResult<String> result = new ParseResult<>();
        StringBuilder sBuilder = new StringBuilder(); // domainname
        int length = 0;                               // length used in raw data
        int strOffset = offset;
        boolean isPointer;
        boolean pointerUsed = false;

        while(buffer[strOffset] != 0) {
            // leer label y sumar la longitud empleada en rawData
            isPointer = ((buffer[strOffset] & 0xC000) == 0xC000);
            if(isPointer) {
                strOffset = (getInt(buffer, strOffset, 2) & 0x3FFF);
                if(!pointerUsed) length += 2;
                pointerUsed = true;
                if(previousNames != null && previousNames.containsKey(strOffset)) {
                    result.bytesUsed = length;
                    if(length == 2) {
                        result.value = previousNames.get(strOffset);
                        return result;
                    } else {
                        sBuilder.append(previousNames.get(strOffset));
                        result.value = sBuilder.toString();
                        previousNames.put(strOffset, result.value);
                        return result;
                    }
                }
            } else {
                if(!pointerUsed) length += buffer[strOffset] + 1;
            }
            for(int i = buffer[strOffset++]; i > 0; i--)
                sBuilder.append((char)buffer[strOffset++]);
            sBuilder.append('.');
        }
        if(sBuilder.length() > 0) sBuilder.deleteCharAt(sBuilder.length() - 1);
        result.bytesUsed = length + (pointerUsed? 0: 1);
        result.value = sBuilder.toString();
        if(previousNames != null)
            previousNames.put(offset, result.value);
        return result;
    }

    public static int writeDomainNameAndUpdateLinks(String domainName, byte[] buffer, int offset, int linkOffset,
            Map<String, Integer> nameLinks) {

        if (useLink(domainName, buffer, offset, linkOffset, nameLinks)) return 2;

        byte[] domainNameBytes = domainName.getBytes(); // TODO transform acordingly (DNS i18n)
        int index = offset;
        int pos = offset;
        byte labelLength = 0;

        nameLinks.put(domainName, linkOffset);

        for (byte b : domainNameBytes) {
            index++;
            if (b == '.') {
                buffer[pos] = labelLength;
                labelLength = 0;
                pos = index;

                int dotPos = domainName.indexOf('.');
                domainName = domainName.substring(dotPos + 1);
                if(useLink(domainName, buffer, pos, linkOffset + (pos - offset), nameLinks)) {
                    return pos - offset + 2;
                } else {
                    nameLinks.put(domainName, linkOffset + (pos - offset));
                }

            } else {
                labelLength++;
                buffer[index] = b;
            }
        }
        buffer[pos] = labelLength;
        buffer[++index] = 0;
        return index - offset + 1;

    }

    public static int writeText(String value, byte[] buffer, int offset) {
        // TODO i18n
        byte[] bytes = value.getBytes();
        buffer[offset] = (byte)bytes.length;
        System.arraycopy(bytes, 0, buffer, offset + 1, bytes.length);
        return bytes.length + 1;

    }

    public static void writeShort(short value, byte[] buffer, int offset) {
        buffer[offset] = (byte)(value >> 8);
        buffer[offset + 1] = (byte)value;
    }

    public static void writeInt(int value, byte[] buffer, int offset) {
        buffer[offset++] = (byte)(value >>> 24);
        buffer[offset++] = (byte)(value >>> 16);
        buffer[offset++] = (byte)(value >>> 8);
        buffer[offset] = (byte)value;
    }

    public static boolean isValidDnsName(String name) {
        return name.length() == 0 || name.length() <= 253 && DNS_NAME_PATTERN.matcher(name).matches();
    }

    public static boolean isValidIPv4(String ip) {
        return PSEUDO_IPV4_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidIPv6(String ipv6) {

        if(ipv6 == null || ipv6.isEmpty()) return false;

        int numOfGroups = 0;
        int charsInGroup = 0;
        int pos = 0;
        int limit;
        boolean expectedBracket = ipv6.charAt(0) == '[';
        boolean doubleColonPresent = false;

        if(expectedBracket) {
            if(ipv6.charAt(ipv6.length() - 1) != ']') return false;
            pos++;
            limit = ipv6.length() - 1;
        } else {
            limit = ipv6.length();
        }

        for(; pos < limit; pos++) {
            switch(ipv6.charAt(pos)) {
                case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':case 'a':
                case 'b':case 'c':case 'd':case 'e':case 'f':case 'A':case 'B':case 'C':case 'D':case 'E':case 'F':
                    charsInGroup++;
                    if(charsInGroup > 4) return false;
                    break;
                case ':':
                    if(numOfGroups > 0 && charsInGroup == 0) {
                        if(doubleColonPresent) return false;
                        doubleColonPresent = true;
                    } else {
                        numOfGroups++;
                    }
                    if(numOfGroups == 8 || doubleColonPresent && numOfGroups == 7) return false;
                    charsInGroup = 0;
                    break;
                default:
                    return false;
            }
        }
        return !(numOfGroups != 7 && !doubleColonPresent || numOfGroups > 6 && doubleColonPresent);

    }

    public static String ttlToString(long ttl) {
        long seconds = ttl % 60;
        long minutes = (ttl / 60) % 60;
        long hours = (ttl / 60 / 24) % 24;
        long days = (ttl / 60 / 24);

        return String.format("[%d %02d:%02d:%02d]", days, hours, minutes, seconds);
    }

    private static boolean useLink(String domainName, byte[] buffer, int offset, int linkOffset,
            Map<String, Integer> nameLinks) {
        Integer link = nameLinks.get(domainName);
        if(link == null) return false;
        int newLinkOffset = link | 0xC000;
        buffer[offset] = (byte)(newLinkOffset >> 8);
        buffer[offset + 1] = (byte)newLinkOffset;
        return true;
    }

    private static Pattern getDnsNamePattern() {
        final String LABEL_PATTERN = "(?:[A-Za-z](?:(?:[A-Za-z0-9]|-){0,62}+(?<!-))?)";
        return Pattern.compile("\\A" + LABEL_PATTERN + "(?:\\." + LABEL_PATTERN + ")*+\\z");
    }

    private static Pattern getIPv4Pattern() {
        final String OCTECT = "(?:[3-9][0-9]?+|2(?:[6-9]?+|5[0-5]?+|[0-4][0-9]?+)|[0-1][0-9]{0,2}+)";
        return Pattern.compile("\\A" + OCTECT + "\\." + OCTECT + "\\." + OCTECT + "\\." + OCTECT + "\\z");
    }

    private Utils() {
        throw new AssertionError(Utils.class.getName()
                + " is just a collection of static methods, having instances of it doesn't make sense");
    }
}
