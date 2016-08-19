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

import au.id.villar.dns.engine.ParseResult;
import au.id.villar.dns.engine.RRValueConverter;
import au.id.villar.dns.engine.Utils;

import java.util.Map;

/**
 * Converter for ResourceRecords of type SOA (Start of authority)
 */
public class SoaValueConverter implements RRValueConverter {

    @Override
    public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
        String mname;
        String rname;
        long serial;
        long refreshInterval;
        long retryInterval;
        long expire;
        long minimum;
        ParseResult<String> result;

        result = Utils.getDomainName(data, offset, previousNames);
        mname = result.value;
        offset += result.bytesUsed;
        result = Utils.getDomainName(data, offset, previousNames);
        rname = result.value;
        offset += result.bytesUsed;
        serial = Utils.getInt(data, offset, 4) & 0xFFFFFFFFL;
        offset += 4;
        refreshInterval = Utils.getInt(data, offset, 4) & 0xFFFFFFFFL;
        offset += 4;
        retryInterval = Utils.getInt(data, offset, 4) & 0xFFFFFFFFL;
        offset += 4;
        expire = Utils.getInt(data, offset, 4) & 0xFFFFFFFFL;
        offset += 4;
        minimum = Utils.getInt(data, offset, 4) & 0xFFFFFFFFL;
        return new SoaData(mname, rname, serial, refreshInterval, retryInterval, expire, minimum);
    }

    @Override
    public Object convertToRawData(Object data) {
        if(!(data instanceof SoaData))
            throw new IllegalArgumentException("Only " + SoaData.class.getName() + " is supported");
        return data;
    }

    @Override
    public <T> T convertValue(Object rawObject, Class<T> tClass) {
        if(tClass == String.class)
            return tClass.cast(rawObject.toString());
        if(tClass != SoaData.class && tClass != Object.class)
            throw new IllegalArgumentException("Only " + SoaData.class.getName() + " is supported");
        return tClass.cast(rawObject);
    }

    @Override
    public int writeRawData(Object rawObject, byte[] array, int offset, int linkOffset,
            Map<String, Integer> nameLinks) {
        SoaData value = (SoaData)rawObject;
        int start = offset;
        int usedBytes;

        usedBytes = Utils.writeDomainNameAndUpdateLinks(value.getDomainName(), array, offset, linkOffset, nameLinks);
        offset += usedBytes;
        usedBytes = Utils.writeDomainNameAndUpdateLinks(value.getMailbox(), array, offset, linkOffset, nameLinks);
        offset += usedBytes;
        Utils.writeInt((int)value.getSerial(), array, offset);
        offset += 4;
        Utils.writeInt((int)value.getRefreshInterval(), array, offset);
        offset += 4;
        Utils.writeInt((int)value.getRetryInterval(), array, offset);
        offset += 4;
        Utils.writeInt((int)value.getExpire(), array, offset);
        offset += 4;
        Utils.writeInt((int)value.getMinimum(), array, offset);
        offset += 4;
        return offset - start;
    }

    /**
     * Holds data related to a SOA (tart of authority) Resource Record
     */
    @SuppressWarnings("WeakerAccess")
    public static final class SoaData {

        private final String domainName;
        private final String mailbox;
        private final long serial;
        private final long refreshInterval;
        private final long retryInterval;
        private final long expire;
        private final long minimum;

        public SoaData(String domainName, String mailbox, long serial, long refreshInterval, long retryInterval,
                long expire, long minimum) {
            this.domainName = domainName;
            this.mailbox = mailbox;
            this.serial = serial;
            this.refreshInterval = refreshInterval;
            this.retryInterval = retryInterval;
            this.expire = expire;
            this.minimum = minimum;
        }

        public String getDomainName() {
            return domainName;
        }

        public String getMailbox() {
            return mailbox;
        }

        public long getSerial() {
            return serial;
        }

        public long getRefreshInterval() {
            return refreshInterval;
        }

        public long getRetryInterval() {
            return retryInterval;
        }

        public long getExpire() {
            return expire;
        }

        public long getMinimum() {
            return minimum;
        }

        @Override
        public String toString() {
            return "MNAME: " + domainName + ", RNAME: " + mailbox + ", SERIAL: " + serial
                    + ", REFRESH: " + refreshInterval + ", RETRY: " + retryInterval + ", EXPIRE: " + expire
                    + ", MINIMUM: " + minimum;
        }
    }

}
