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

import static org.junit.Assert.*;

public class DnsMessageTest {

    @Test
    public void writeToBufferTest() {

        byte[] rawData = { 1, -128, (byte)0x95, (byte)0xA4, 0, 0, 0, 0, 0, 0, 0, 0 };

        DNSMessage message = new DNSEngine().createMessageFromBuffer(rawData, 0);
        byte[] test = new byte[rawData.length + 5];
        message.writeToBuffer(test, 5);

        for(int index = 0; index < rawData.length; index++) {
            assertEquals(rawData[index], test[index + 5]);
        }
    }
}
