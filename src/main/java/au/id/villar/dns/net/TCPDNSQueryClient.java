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

class TCPDNSQueryClient extends AbstractDNSQueryClient {

    private ByteBuffer buffer;

    TCPDNSQueryClient(int dnsPort, Selector selector) throws IOException {
        super(dnsPort, selector);
    }

    protected boolean internalDoIO(int timeoutMillis) throws IOException, DNSException {

        SocketChannel tcpChannel = (SocketChannel)channel;

        switch (status) {

            case OPENING:

                channel = tcpChannel = SocketChannel.open();
                tcpChannel.configureBlocking(false);
                tcpChannel.register(selector, SelectionKey.OP_CONNECT);
                status = Status.CONNECTING;
                tcpChannel.connect(new InetSocketAddress(address, dnsPort));

            case CONNECTING:

                if(selector.select(timeoutMillis) == 0) {
                    status = Status.CONNECTING;
                    return false;
                }
                tcpChannel.register(selector, SelectionKey.OP_WRITE);
                query.position(0);

            case SENDING:

                if(!sendDataAndPrepareForReceiving(timeoutMillis, tcpChannel)) {
                    status = Status.SENDING;
                    return false;
                }

            case RECEIVING:

                if(selector.select(timeoutMillis) == 0) {
                    status = Status.RECEIVING;
                    return false;
                }
                receiveTcp();
                tcpChannel.close();
                result = buffer;
                status = Status.RESULT;

        }

        return true;
    }

    private void receiveTcp() throws IOException {
        int received;
        buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE * 2);
        do {
            received = ((ReadableByteChannel)channel).read(buffer);
            if(received > 0) enlargeBuffer();
        } while(received > 0);
    }

    private void enlargeBuffer() {
        ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + UDP_DATAGRAM_MAX_SIZE);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }

}
