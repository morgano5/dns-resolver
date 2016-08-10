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
package au.id.villar.dns.net;

import au.id.villar.dns.DNSException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

class UDPDNSQueryClient extends AbstractDNSQueryClient {

    private static final int UDP_RETRY_MILLISECONDS = 5000;

    private long udpTimestamp;

    UDPDNSQueryClient(int dnsPort, Selector selector) throws IOException {
        super(dnsPort, selector);
    }

    protected boolean internalDoIO(int timeoutMillis) throws IOException, DNSException {

        DatagramChannel udpChannel = (DatagramChannel)channel;

        switch (status) {

            case OPENING:

                udpTimestamp = System.currentTimeMillis();
                channel = udpChannel = DatagramChannel.open();
                udpChannel.configureBlocking(false);
                udpChannel.socket().connect(new InetSocketAddress(address, dnsPort));
                query.position(0);
                udpChannel.register(selector, SelectionKey.OP_WRITE);

            case SENDING:

                if(!sendDataAndPrepareForReceiving(timeoutMillis, udpChannel)) {
                    status = Status.SENDING;
                    return false;
                }

            case RECEIVING:

                if(selector.select(timeoutMillis) == 0) {
                    if(System.currentTimeMillis() - udpTimestamp > UDP_RETRY_MILLISECONDS) {
                        query.position(0);
                        udpChannel.register(selector, SelectionKey.OP_WRITE);
                        status = Status.SENDING;
                        return false;
                    }
                    status = Status.RECEIVING;
                    return false;
                }
                ByteBuffer buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE);
                udpChannel.receive(buffer);
                udpChannel.close();
                result = buffer;
                status = Status.RESULT;

        }

        return true;
    }

}
