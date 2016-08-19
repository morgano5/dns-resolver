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

public final class ResourceRecord implements DNSItem {

    private final String dnsName;
    private final DNSType dnsType;
    private final DNSClass dnsClass;
    private final long ttl;
    private final RRValueConverter converter;
    private final Object data;

    ResourceRecord(String dnsName, DNSType dnsType, DNSClass dnsClass, long ttl, RRValueConverter converter, Object data) {
        this.dnsName = dnsName;
        this.dnsType = dnsType;
        this.dnsClass = dnsClass;
        this.ttl = ttl;
        this.converter = converter;
        this.data = data;
    }

    public String getDnsName() {
        return dnsName;
    }

    public DNSType getDnsType() {
        return dnsType;
    }

    public DNSClass getDnsClass() {
        return dnsClass;
    }

    public long getSecondsCache() {
        return ttl;
    }

    public <T> T getData(Class<T> tClass) {
        return converter.convertValue(data, tClass);
    }

    @Override
    public String toString() {
        return "RR{" +
                "name='" + dnsName + '\'' +
                ", type=" + dnsType +
                ", class=" + dnsClass +
                ", ttl=" + ttl +
                ", value=" + converter.convertValue(data, String.class) +
                '}';
    }

    int writeRawData(byte[] buffer, int offset, int linkOffset, Map<String, Integer> nameLinks) {

        int start = offset;
        int usedBytes;

        usedBytes = Utils.writeDomainNameAndUpdateLinks(dnsName, buffer, offset, linkOffset, nameLinks);
        offset += usedBytes;
        Utils.writeShort(dnsType.getValue(), buffer, offset);
        offset += 2;
        Utils.writeShort(dnsClass.getValue(), buffer, offset);
        offset += 2;
        Utils.writeInt((int) ttl, buffer, offset);
        offset += 6;
        usedBytes = converter.writeRawData(data, buffer, offset, linkOffset + offset, nameLinks);
        if (usedBytes < 0) return -1;
        offset -= 2;
        Utils.writeShort((short) usedBytes, buffer, offset);
        return offset - start + 2 + usedBytes;
    }
}
