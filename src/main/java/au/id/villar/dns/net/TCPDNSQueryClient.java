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

// TODO revisit this implementation and check https://tools.ietf.org/html/rfc7766
class TCPDNSQueryClient extends AbstractDNSQueryClient {

    private static final int TCP_CLIENT_OPS = SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE;

    private ByteBuffer buffer;

    @Override
    protected boolean internalDoIO(Selector selector, String address, int port) throws IOException, DNSException {
        SocketChannel tcpChannel = (SocketChannel)channel;

        switch (status) {

            case OPENING:

                query.position(0);
                channel = tcpChannel = SocketChannel.open();
                tcpChannel.configureBlocking(false);
                if(!tcpChannel.connect(new InetSocketAddress(address, port))) {
                    registerAndAttach(selector, TCP_CLIENT_OPS);
                    status = Status.CONNECTING;
                    return false;
                }

            case CONNECTING:

                if(!tcpChannel.finishConnect()) {
                    registerAndAttach(selector, TCP_CLIENT_OPS);
                    status = Status.CONNECTING;
                    return false;
                }
                query.position(0);

            case SENDING:

                tcpChannel.write(query);
                if(query.remaining() > 0) {
                    registerAndAttach(selector, TCP_CLIENT_OPS);
                    status = Status.SENDING;
                    return false;
                }
                buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE * 2);

            case RECEIVING:

                if(!receiveToEnd()) {
                    registerAndAttach(selector, TCP_CLIENT_OPS);
                    status = Status.RECEIVING;
                    return false;
                }
                tcpChannel.close();
                buffer.flip();
                result = buffer;
                status = Status.RESULT;

        }

        return true;
    }

    private boolean receiveToEnd() throws IOException {
        int received;
        do {
            received = ((ReadableByteChannel)channel).read(buffer);
            if(buffer.remaining() == 0) {
                enlargeBuffer();
            } else {
                break;
            }
        } while(received > 0);
        return received == -1;
    }

    private void enlargeBuffer() {
        ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + UDP_DATAGRAM_MAX_SIZE);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }

}
