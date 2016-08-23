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

    @Override
    public boolean areEqual(Object rawObject1, Object rawObject2) {
        return rawObject1.equals(rawObject2);
    }

    /**
     * Holds data related to a SOA (start of authority) Resource Record. For more information see the FRC-1035:
     * https://tools.ietf.org/html/rfc1035
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

        /**
         * Creates an object that contains data value of a SOA Resource record.
         * @param domainName The domain name of the name server that is the source of data for this zone.
         * @param mailbox A domain name which specifies the mailbox of the person responsible for this zone.
         * @param serial The version number of the original copy of the zone.
         * @param refreshInterval A time interval before the zone should be refreshed.
         * @param retryInterval A time interval that should elapse before a failed refresh should be retried.
         * @param expire The upper limit on the time interval that can elapse before the zone is no longer
         *               authoritative.
         * @param minimum The minimum TTL field that should be exported with any resource record from this zone.
         */
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

        /**
         * The domain name of the name server that is the source of data for this zone.
         * @return The domain name of the name server that is the source of data for this zone.
         */
        public String getDomainName() {
            return domainName;
        }

        /**
         * A domain name which specifies the mailbox of the person responsible for this zone.
         * @return A domain name which specifies the mailbox of the person responsible for this zone.
         */
        public String getMailbox() {
            return mailbox;
        }

        /**
         * The version number of the original copy of the zone.
         * @return The version number of the original copy of the zone.
         */
        public long getSerial() {
            return serial;
        }

        /**
         * A time interval before the zone should be refreshed.
         * @return A time interval before the zone should be refreshed.
         */
        public long getRefreshInterval() {
            return refreshInterval;
        }

        /**
         * A time interval that should elapse before a failed refresh should be retried.
         * @return A time interval that should elapse before a failed refresh should be retried.
         */
        public long getRetryInterval() {
            return retryInterval;
        }

        /**
         * The upper limit on the time interval that can elapse before the zone is no longer authoritative.
         * @return The upper limit on the time interval that can elapse before the zone is no longer authoritative.
         */
        public long getExpire() {
            return expire;
        }

        /**
         * The minimum TTL field that should be exported with any resource record from this zone.
         * @return The minimum TTL field that should be exported with any resource record from this zone.
         */
        public long getMinimum() {
            return minimum;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SoaData soaData = (SoaData) o;

            if (serial != soaData.serial) return false;
            if (refreshInterval != soaData.refreshInterval) return false;
            if (retryInterval != soaData.retryInterval) return false;
            if (expire != soaData.expire) return false;
            if (minimum != soaData.minimum) return false;
            if (!domainName.equals(soaData.domainName)) return false;
            return mailbox.equals(soaData.mailbox);

        }

        @Override
        public int hashCode() {
            int result = domainName.hashCode();
            result = 31 * result + mailbox.hashCode();
            result = 31 * result + (int) (serial ^ (serial >>> 32));
            result = 31 * result + (int) (refreshInterval ^ (refreshInterval >>> 32));
            result = 31 * result + (int) (retryInterval ^ (retryInterval >>> 32));
            result = 31 * result + (int) (expire ^ (expire >>> 32));
            result = 31 * result + (int) (minimum ^ (minimum >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "MNAME: " + domainName + ", RNAME: " + mailbox + ", SERIAL: " + serial
                    + ", REFRESH: " + refreshInterval + ", RETRY: " + retryInterval + ", EXPIRE: " + expire
                    + ", MINIMUM: " + minimum;
        }
    }

}
