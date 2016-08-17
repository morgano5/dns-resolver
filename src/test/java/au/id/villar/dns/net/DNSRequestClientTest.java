/*
 * Copyright 2016 Rafael Villar Villar
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
package au.id.villar.dns.net;

import au.id.villar.dns.DNSException;
import au.id.villar.dns.TestUtils;
import au.id.villar.dns.engine.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class DNSRequestClientTest {

    public static void main(String[] args) throws IOException, DNSException, InterruptedException {

        DNSEngine engine = new DNSEngine();
        Question question = engine.createQuestion("id.au", DNSType.ALL, DNSClass.IN);
        DNSMessage message = engine.createSimpleQueryMessage((short)15, question);
        ByteBuffer rawMessage = engine.createBufferFromMessage(message);
        ByteBuffer result = new DNSRequestClient().query(rawMessage, "8.8.8.8", 10_000);

        System.out.println(TestUtils.messageToString(engine.createMessageFromBuffer(result.array(),
                result.position())));
    }

    public static void main3(String[] args) throws IOException, InterruptedException {

        class BooleanHolder { volatile boolean value; }
        BooleanHolder holder = new BooleanHolder();

        DNSEngine engine = new DNSEngine();
        Question question = engine.createQuestion("id.au", DNSType.ALL, DNSClass.IN);
        // DNSMessage message = engine.createSimpleQueryMessage((short)15, question);
        DNSMessage message = engine.createMessage((short)15, false, Opcode.QUERY, false, false, true, false,
                (byte)0, ResponseCode.NO_ERROR, new Question[] {question}, new ResourceRecord[0], new ResourceRecord[0],
                new ResourceRecord[0]);
        ByteBuffer rawMessage = engine.createBufferFromMessage(message);

        try(Selector selector = Selector.open()) {
            DNSRequestClient client = new DNSRequestClient();
            client.startQuery(rawMessage, "8.8.8.8", selector, (r, e) -> {
                System.out.println(r != null? TestUtils.messageToString(
                        engine.createMessageFromBuffer(r.array(), r.position())): e.getMessage());
                holder.value = true;
            });

            while (!holder.value) {
                System.out.println("waiting...");
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()) {
                    DNSRequestClient.processAttachment(iterator.next());
                    iterator.remove();
                }
            }
        }
    }
}
