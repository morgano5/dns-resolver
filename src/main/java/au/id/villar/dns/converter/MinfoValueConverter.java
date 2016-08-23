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
 * Converter for ResourceRecords of type MINFO (Mailbox info)
 */
public class MinfoValueConverter implements RRValueConverter {

    @Override
    public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
        String adminMailbox;
        String errorMailbox;
        ParseResult<String> result;

        result = Utils.getDomainName(data, offset, previousNames);
        adminMailbox = result.value;
        offset += result.bytesUsed;
        result = Utils.getDomainName(data, offset, previousNames);
        errorMailbox = result.value;
        return new MinfoData(adminMailbox, errorMailbox);
    }

    @Override
    public Object convertToRawData(Object data) {
        if(!(data instanceof MinfoData))
            throw new IllegalArgumentException("Only " + MinfoData.class.getName() + " is supported");
        return data;
    }

    @Override
    public <T> T convertValue(Object rawObject, Class<T> tClass) {
        if(tClass == String.class)
            return tClass.cast(rawObject.toString());
        if(tClass != MinfoData.class && tClass != Object.class)
            throw new IllegalArgumentException("Only " + MinfoData.class.getName() + " is supported");
        return tClass.cast(rawObject);
    }

    @Override
    public int writeRawData(Object rawObject, byte[] array, int offset, int linkOffset,
            Map<String, Integer> nameLinks) {
        int start = offset;
        int usedBytes;
        MinfoData value = (MinfoData)rawObject;

        usedBytes = Utils.writeDomainNameAndUpdateLinks(value.getAdminMaibox(), array, offset, linkOffset, nameLinks);
        offset += usedBytes;
        usedBytes = Utils.writeDomainNameAndUpdateLinks(value.getErrorMailbox(), array, offset, linkOffset, nameLinks);
        offset += usedBytes;
        return offset - start;
    }

    @Override
    public boolean areEqual(Object rawObject1, Object rawObject2) {
        return rawObject1.equals(rawObject2);
    }

    /**
     * Holds data related to a MINFO (Mailbox info) Resource Record
     */
    @SuppressWarnings("WeakerAccess")
    public static final class MinfoData {

        private final String adminMaibox;
        private final String errorMailbox;

        /**
         * Creates an object to contain the data value for a MINFO Resource record.
         * @param adminMaibox A domain name specifying a mailbox of the responsible for the mailing list or mailbox.
         * @param errorMailbox A domain name specifying a mailbox used to receive error messages related to the mailing
         *                     list or mailbox.
         */
        public MinfoData(String adminMaibox, String errorMailbox) {
            this.adminMaibox = adminMaibox;
            this.errorMailbox = errorMailbox;
        }

        /**
         * A domain name specifying a mailbox of the responsible for the mailing list or mailbox.
         * @return A domain name specifying a mailbox of the responsible for the mailing list or mailbox.
         */
        public String getAdminMaibox() {
            return adminMaibox;
        }

        /**
         * A domain name specifying a mailbox used to receive error messages related to the mailing list or
         * mailbox.
         * @return A domain name specifying a mailbox used to receive error messages related to the mailing list or
         * mailbox.
         */
        public String getErrorMailbox() {
            return errorMailbox;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MinfoData minfoData = (MinfoData) o;

            return adminMaibox.equals(minfoData.adminMaibox) && errorMailbox.equals(minfoData.errorMailbox);
        }

        @Override
        public int hashCode() {
            int result = adminMaibox.hashCode();
            result = 31 * result + errorMailbox.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "admin mailbox: " + adminMaibox + ", error mailbox: " + errorMailbox;
        }
    }

}
