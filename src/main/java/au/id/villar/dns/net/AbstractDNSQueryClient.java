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
import java.nio.ByteBuffer;
import java.nio.channels.*;

abstract class AbstractDNSQueryClient implements DNSQueryClient {

    enum Status {
        OPENING,
        CONNECTING,
        SENDING,
        RECEIVING,
        RESULT,
        ERROR,
        CLOSED
    }

    SelectableChannel channel;
    ByteBuffer query;
    ByteBuffer result;
    Status status;

    private ResultHandler resultHandler;

    @Override
    public boolean startQuery(ByteBuffer query, String address, int port, Selector selector,
            ResultHandler resultHandler) {

        if(query.remaining() == 0) {
            resultHandler.result(null, new DNSException("Empty query"));
            return true;
        }

        try {

            IOException exception = close(channel);
            if(exception != null) throw exception;

            this.resultHandler = resultHandler;
            this.result = null;
            this.status = Status.OPENING;
            this.query = query;
            return checkIfResultAndNotify(selector, address, port) == NO_OP;
        } catch (IOException | DNSException e) {
            resultHandler.result(null, e instanceof DNSException? (DNSException)e: new DNSException(e));
            return true;
        }
    }

    @Override
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public int doIO() {
        try {

            return checkIfStatusValidAndResultReady()? 0: checkIfResultAndNotify(null, null, 0);

        } catch(IOException | DNSException e) {
            close(channel);
            status = Status.ERROR;
            resultHandler.result(null, e instanceof DNSException? (DNSException)e: new DNSException(e));
            return 0;
        }
    }

    @Override
    public void close() throws IOException {
        IOException exChannel = close(channel);
        channel = null;
        status = Status.CLOSED;
        if(exChannel != null) throw exChannel;
    }

    abstract int internalDoIO(Selector selector, String address, int port)
            throws IOException, DNSException;

    void registerAndAttach(Selector selector, int ops) throws ClosedChannelException {
        if(selector != null) {
            channel.register(selector, ops).attach(this);
        }
    }

    void checkIdMatch(int responseOffset) throws DNSException {
        byte[] query = this.query.array();
        byte[] response = this.result.array();
        if(query[2] != response[responseOffset] || query[3] != response[responseOffset + 1]) {
            throw new DNSException("Query and response IDs don't match");
        }
    }

    private int checkIfResultAndNotify(Selector selector, String address, int port)
            throws IOException, DNSException {
        int ops = internalDoIO(selector, address, port);
        if(ops == NO_OP) resultHandler.result(result, null);
        return ops;
    }

    private IOException close(Channel channel) {
        try {
            if(channel != null && channel.isOpen()) channel.close();
            return null;
        } catch(IOException e) {
            return e;
        }
    }

    private boolean checkIfStatusValidAndResultReady() throws DNSException {
        switch(status) {
            case CLOSED: throw new DNSException("Already closed");
            case ERROR: throw new DNSException("Invalid state");
            case RESULT: return true;
            default: return false;
        }
    }

}
