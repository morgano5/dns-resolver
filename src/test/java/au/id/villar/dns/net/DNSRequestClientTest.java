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
import au.id.villar.dns.engine.DNSClass;
import au.id.villar.dns.engine.DNSEngine;
import au.id.villar.dns.engine.DNSMessage;
import au.id.villar.dns.engine.DNSType;
import au.id.villar.dns.engine.Question;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class DNSRequestClientTest {

    public static void main3(String[] args) throws IOException, DNSException, InterruptedException {

        DNSEngine engine = new DNSEngine();
        Question question = engine.createQuestion("id.au", DNSType.ALL, DNSClass.IN);
        DNSMessage message = engine.createSimpleQueryMessage((short)15, question);
        ByteBuffer rawMessage = engine.createBufferFromMessage(message);
        ByteBuffer result = new DNSRequestClient().query(rawMessage, "8.8.8.8", 10_000);

        System.out.println(TestUtils.messageToString(engine.createMessageFromBuffer(result.array(),
                result.position())));
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        class BooleanHolder { volatile boolean value; }
        BooleanHolder holder = new BooleanHolder();

        DNSEngine engine = new DNSEngine();
        Question question = engine.createQuestion("id.au", DNSType.ALL, DNSClass.IN);
        DNSMessage message = engine.createSimpleQueryMessage((short)15, question);
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
                Thread.sleep(100);
                if(selector.select() == 0) System.out.println("...");
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()) {
                    System.out.println("iterating...");
                    SelectionKey key = iterator.next();
                    if(client.processAttachement(key)) iterator.remove();
                }
            }
        }
    }
}
