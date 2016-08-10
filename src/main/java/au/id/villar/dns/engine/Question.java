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

/**
 * Class representing a DNS Standard question (See RFC-1035)
 */
public final class Question implements DNSItem {

    /* Fields for a query [RFC1035] */
    private final String dnsName;
    private final DNSType dnsType;
    private final DNSClass dnsClass;


    Question(String dnsName, DNSType dnsType, DNSClass dnsClass) {
        this.dnsName = dnsName;
        this.dnsType = dnsType;
        this.dnsClass = dnsClass;
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

    int writeRawData(byte[] buffer, int offset, int linkOffset, Map<String, Integer> nameLinks) {
        int usedBytes;

        usedBytes = Utils.writeDomainNameAndUpdateLinks(dnsName, buffer, offset, linkOffset, nameLinks);
        Utils.writeShort(dnsType.getValue(), buffer, offset + usedBytes);
        usedBytes += 2;
        Utils.writeShort(dnsClass.getValue(), buffer, offset + usedBytes);
        usedBytes += 2;

        return usedBytes;
    }

}
