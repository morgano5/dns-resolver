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

public class HinfoValueConverter implements RRValueConverter {

    @Override
    public Object convertToRawData(Object data) {
        if(!(data instanceof HinfoData))
            throw new IllegalArgumentException("Only String type supported");
        return data;
    }

    @Override
    public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
        String cpu;
        String operatingSystem;
        ParseResult<String> result;

        result = Utils.getText(data, offset);
        offset += result.bytesUsed;
        cpu = result.value;
        result = Utils.getText(data, offset);
        operatingSystem = result.value;
        return new HinfoData(cpu, operatingSystem);
    }

    @Override
    public <T> T convertValue(Object rawObject, Class<T> tClass) {
        if(tClass != HinfoData.class && tClass != Object.class)
            throw new IllegalArgumentException("Only " + HinfoData.class.getName() + " is supported");
        return tClass.cast(rawObject);
    }

    @Override
    public int writeRawData(Object rawObject, byte[] array, int offset, int linkOffset,
            Map<String, Integer> nameLinks) {
        HinfoData value = (HinfoData)rawObject;
        int start = offset;
        offset += Utils.writeText(value.getCpu(), array, offset);
        offset += Utils.writeText(value.getOperatingSystem(), array, offset);
        return offset - start;
    }

    public static final class HinfoData {

        private final String cpu;
        private final String operatingSystem;

        public HinfoData(String cpu, String operatingSystem) {
            this.cpu = cpu;
            this.operatingSystem = operatingSystem;
        }

        public String getCpu() {
            return cpu;
        }

        public String getOperatingSystem() {
            return operatingSystem;
        }

        @Override
        public String toString() {
            return "CPU: " + cpu + ", Operating System: " + operatingSystem;
        }
    }
}
