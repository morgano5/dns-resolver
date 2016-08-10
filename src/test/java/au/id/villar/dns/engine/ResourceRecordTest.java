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

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ResourceRecordTest {

    @Test
    public void writeRawDataTest() {

        byte[] data = {
                0, 0, 0, 0, 0,
                6, 'v', 'i', 'l', 'l', 'a', 'r', 2, 'm', 'e', 0,
                0, 1,
                0, 1,
                0, 0, 0, 5,
                0, 4,
                (byte)192, (byte)168, 0, 1
        };
        ParseResult<ResourceRecord> rrResult = new DNSEngine().createResourceRecordFromBuffer(data, 5, null);

        byte[] result = new byte[data.length];

        int usedBytes = rrResult.value.writeRawData(result, 5, 0, new HashMap<>());

        assertTrue(Arrays.equals(data, result));
        assertEquals(25, usedBytes);
    }
}
