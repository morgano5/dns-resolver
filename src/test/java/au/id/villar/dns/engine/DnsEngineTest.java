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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DnsEngineTest {

    @Test
    public void createResourceRecordFromBufferTest() {

/*
                                    1  1  1  1  1  1
      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                                               |
    /                                               /
    /                      NAME                     /
    |                                               |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                      TYPE                     |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                     CLASS                     |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                      TTL                      |
    |                                               |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                   RDLENGTH                    |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
    /                     RDATA                     /
    /                                               /
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

*/

        byte[] data = {
                0, 0, 0, 0, 0,
                6, 'v', 'i', 'l', 'l', 'a', 'r', 2, 'm', 'e', 0,
                0, 1,
                0, 1,
                0, 0, 0, 5,
                0, 4,
                (byte)192, (byte)168, 0, 1
        };

        ParseResult<ResourceRecord> result = new DnsEngine().createResourceRecordFromBuffer(data, 5, null);
        assertEquals("villar.me", result.value.getDnsName());
        assertEquals(DnsType.A, result.value.getDnsType());
        assertEquals(DnsClass.IN, result.value.getDnsClass());
        assertEquals(5, result.value.getSecondsCache());
        assertEquals("192.168.0.1", result.value.getData(String.class));
        assertEquals(25, result.bytesUsed);
    }

    @Test
    public void createFromBufferTest() {

/*
                                    1  1  1  1  1  1
      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                      ID                       |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |QR|   Opcode  |AA|TC|RD|RA|   Z    |   RCODE   |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    QDCOUNT                    |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    ANCOUNT                    |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    NSCOUNT                    |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
    |                    ARCOUNT                    |
    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

*/

        byte[] rawData = {
                0, 0, 0, 0, 0, // some testing ofset
                1, -128, (byte)0x95, (byte)0xA4,
                0, 0, 0, 0, 0, 0, 0, 0
        };

        DnsMessage message = new DnsEngine().createMessageFromBuffer(rawData, 5);

        assertEquals(384, message.getId());
        assertTrue(message.isResponse());
        assertEquals(Opcode.STATUS, message.getOpcode());
        assertTrue(message.isAuthoritative());
        assertFalse(message.isTruncated());
        assertTrue(message.isRecursionDesired());
        assertTrue(message.isRecursionAvailable());
        assertEquals(2, message.getZ());
        assertEquals(ResponseCode.NOT_IMPLEMENTED, message.getResponseCode());
        assertEquals(0, message.getNumQuestions());
        assertEquals(0, message.getNumAnswers());
        assertEquals(0, message.getNumAuthorities());
        assertEquals(0, message.getNumAdditionals());
    }

}
