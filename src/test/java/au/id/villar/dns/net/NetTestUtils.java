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

public class NetTestUtils {

    private NetTestUtils() {}

    public static DNSMessage query(DNSQueryClient client, short messageId, String name, DNSType dnsType, DNSClass dnsClass, String serverAddress, int timeout) throws IOException, DNSException, InterruptedException {

        DNSEngine engine = new DNSEngine();
        Question question = engine.createQuestion(name, dnsType, dnsClass);
        DNSMessage message = engine.createSimpleQueryMessage(messageId, question);
        ByteBuffer rawMessage = engine.createBufferFromMessage(message);

        boolean done = client.startQuery(rawMessage, serverAddress, timeout);
        while(!done) {
            System.out.println("waiting...");
            Thread.sleep(100);
            done = client.doIO(timeout);
        }

        return engine.createMessageFromBuffer(client.getResult().array(), 0);
    }


}
