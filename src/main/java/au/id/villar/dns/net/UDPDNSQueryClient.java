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
package au.id.villar.dns.net;

import au.id.villar.dns.DNSException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

class UDPDNSQueryClient extends AbstractDNSQueryClient {

    private static final int UDP_CLIENT_OPS = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    private static final int UDP_RETRY_MILLISECONDS = 5000;

    private long udpTimestamp;
    private ByteBuffer buffer;

    @Override
    protected boolean internalDoIO(Selector selector, String address, int port) throws IOException, DNSException {

        DatagramChannel udpChannel = (DatagramChannel)channel;

        switch (status) {

            case OPENING:

                udpTimestamp = System.currentTimeMillis();
                query.position(2);
                channel = udpChannel = DatagramChannel.open();
                udpChannel.configureBlocking(false);
                udpChannel.connect(new InetSocketAddress(address, port));

            case SENDING:

                ((WritableByteChannel)channel).write(query);
                if(query.remaining() > 0) {
                    registerAndAttach(selector, UDP_CLIENT_OPS);
                    status = Status.SENDING;
                    return false;
                }
                buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE);

            case RECEIVING:

                if(udpChannel.receive(buffer) == null) {
                    registerAndAttach(selector, UDP_CLIENT_OPS);
                    if(System.currentTimeMillis() - udpTimestamp > UDP_RETRY_MILLISECONDS) {
                        udpTimestamp = System.currentTimeMillis();
                        query.position(2);
                        status = Status.SENDING;
                    } else {
                        status = Status.RECEIVING;
                    }
                    return false;
                }
                udpChannel.close();
                buffer.flip();
                result = buffer;
                status = Status.RESULT;
                checkIdMatch(0);

        }

        return true;
    }

}
