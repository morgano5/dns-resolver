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

public class DomainNameValueConverter implements RRValueConverter {

    @Override
    public Object convertToRawData(Object data) {
        if(!(data instanceof String)) throw new IllegalArgumentException("Only String can be converted");
        String str = data.toString();
        if(!Utils.isValidDnsName(str)) throw new IllegalArgumentException("Not a valid domain name: " + str);
        return str;
    }

    @Override
    public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
        ParseResult<String> r = Utils.getDomainName(data, offset, previousNames);
        return r.value;
    }

    @Override
    public <T> T convertValue(Object value, Class<T> tClass) {
        if(tClass != String.class && tClass != Object.class)
            throw new IllegalArgumentException("only String conversions are possible");
        return tClass.cast(value);
    }

    @Override
    public int writeRawData(Object value, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
        return Utils.writeDomainNameAndUpdateLinks(value.toString(), array, offset, linkOffset, nameLinks);
    }

}
