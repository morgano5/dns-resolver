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

import java.util.Arrays;
import java.util.Map;

/**
 * Converter for ResourceRecords of unknown type
 */
public class UnknownValueConverter implements RRValueConverter {

    @Override
    public Object convertToRawData(Object data) {
        if(!(data instanceof byte[])) throw new IllegalArgumentException("Only byte[] is allowed");
        byte[] byteData = (byte[])data;
        byte[] value = new byte[byteData.length];
        System.arraycopy(byteData, 0, value, 0, byteData.length);
        return value;
    }

    @Override
    public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
        byte[] value = new byte[length];
        System.arraycopy(data, offset, value, 0, length);
        return value;
    }

    @Override
    public <T> T convertValue(Object rawObject, Class<T> tClass) {
        if(tClass == String.class)
            return tClass.cast(byteToString((byte[])rawObject));
        if(tClass != byte[].class && tClass != Object.class)
            throw new IllegalArgumentException("Only byte[] is allowed");
        return tClass.cast(((byte[])rawObject).clone());
    }

    @Override
    public int writeRawData(Object rawObject, byte[] array, int offset, int linkOffset,
            Map<String, Integer> nameLinks) {
        byte[] arrayValue = (byte[])rawObject;
        System.arraycopy(arrayValue, 0, array, offset, arrayValue.length);
        return arrayValue.length;
    }

    @Override
    public boolean areEqual(Object rawObject1, Object rawObject2) {
        return Arrays.equals((byte[])rawObject1, (byte[])rawObject2);
    }

    private String byteToString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder(bytes.length);
        for(byte b: bytes) {
            if(b > 31 && b < 127) {
                buffer.append('\'').append((char)b).append('\'');
            } else {
                buffer.append('[').append(b & 0xFF).append(']');
            }
        }
        return buffer.toString();
    }
}
