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
import au.id.villar.dns.engine.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

class NetTestUtils {

    private NetTestUtils() {}

    static void query(DNSQueryClient client, boolean forTCP, short messageId, String name, DNSType dnsType,
            DNSClass dnsClass, String serverAddress, int port, DNSMessageListener listener)
            throws IOException, InterruptedException {

        DNSEngine engine = new DNSEngine();
        Question question = engine.createQuestion(name, dnsType, dnsClass);
        DNSMessage message = engine.createSimpleQueryMessage(messageId, question);
        ByteBuffer rawMessage = engine.createBufferFromMessage(message);

        try(Selector selector = Selector.open()) {

            boolean done = client.startQuery(rawMessage, serverAddress, port, selector,
                    (b, e) -> listener.result(b != null? engine.createMessageFromBuffer(b.array(), forTCP? 2: 0): null, e));
            while (!done) {
                System.out.println("waiting...");
                Thread.sleep(100);
                if(selector.select() == 0) {
                    System.out.println("...");
                    //selector.
                    //System.out.println("selector was interrupted or woken up");
                    //break;
                }
                done = true;
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    boolean finished = ((DNSQueryClient)key.attachment()).doIO();
                    if(finished) {
                        iterator.remove();
                    } else {
                        done = false;
                    }
                }
            }
        }

    }

    @FunctionalInterface
    interface DNSMessageListener { void result(DNSMessage result, DNSException exception); }

}
