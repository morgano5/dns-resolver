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
package au.id.villar.dns.converter;

import au.id.villar.dns.engine.ParseResult;
import au.id.villar.dns.engine.RRValueConverter;
import au.id.villar.dns.engine.Utils;

import java.util.Map;

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
    public <T> T convertValue(Object value, Class<T> tClass) {
        if(tClass != MinfoData.class && tClass != Object.class)
            throw new IllegalArgumentException("Only " + MinfoData.class.getName() + " is supported");
        return tClass.cast(value);
    }

    @Override
    public int writeRawData(Object objValue, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
        int start = offset;
        int usedBytes;
        MinfoData value = (MinfoData)objValue;

        usedBytes = Utils.writeDomainNameAndUpdateLinks(value.getAdminMaibox(), array, offset, linkOffset, nameLinks);
        offset += usedBytes;
        usedBytes = Utils.writeDomainNameAndUpdateLinks(value.getErrorMailbox(), array, offset, linkOffset, nameLinks);
        offset += usedBytes;
        return offset - start;
    }

    public static final class MinfoData {

        private final String adminMaibox;
        private final String errorMailbox;

        public MinfoData(String adminMaibox, String errorMailbox) {
            this.adminMaibox = adminMaibox;
            this.errorMailbox = errorMailbox;
        }

        public String getAdminMaibox() {
            return adminMaibox;
        }

        public String getErrorMailbox() {
            return errorMailbox;
        }


        @Override
        public String toString() {
            return "admin mailbox: " + adminMaibox + ", error mailbox: " + errorMailbox;
        }
    }

}
