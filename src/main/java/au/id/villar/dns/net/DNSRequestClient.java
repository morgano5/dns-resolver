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
import au.id.villar.dns.engine.Utils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class DNSRequestClient implements Closeable {

    private final UDPDNSQueryClient udpClient;
    private final TCPDNSQueryClient tcpClient;

    public DNSRequestClient() throws IOException {
        this.udpClient = new UDPDNSQueryClient();
        this.tcpClient = new TCPDNSQueryClient();
    }

    public void startQuery(ByteBuffer question, String address, int port, Selector selector,
            ResultListener resultListener) {

        udpClient.startQuery(question, address, port, selector,
                (r, e) -> checkAndDoTCPIfNeeded(r, e, question, address, port, selector, resultListener));

    }

    public boolean processAttachement(SelectionKey selectionKey) {
        Object attachment = selectionKey.attachment();
        return attachment instanceof DNSQueryClient && ((DNSQueryClient)attachment).doIO();
    }

    public ByteBuffer query(ByteBuffer question, String address, int port, long timeout)
            throws DNSException, InterruptedException {

        @SuppressWarnings("WeakerAccess")
        class ResultHolder { ByteBuffer result; DNSException exception; }
        ResultHolder holder = new ResultHolder();

        long start = System.currentTimeMillis();
        boolean udpDone = false;
        boolean done = udpClient.startQuery(question, address, port, null, (r, e) -> {
            holder.result = r;
            holder.exception = e;
        });
        while(!done) {
            if(System.currentTimeMillis() - start > timeout) break;
            Thread.sleep(10);
            if(!udpDone) {
                udpDone = udpClient.doIO();
                if(udpDone) {
                    if(!udpIsTruncated(holder.result)) break;
                    question.position(0);
                    done = tcpClient.startQuery(question, address, port, null, (r, e) -> {
                        if(r != null) r.position(2);
                        holder.result = r;
                        holder.exception = e;
                    });
                }
            } else {
                done = tcpClient.doIO();
            }
        }
        if(holder.exception != null) throw holder.exception;
        return holder.result;
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;
        try { udpClient.close(); } catch (IOException e) { exception = e; }
        try { tcpClient.close(); } catch (IOException e) { if(exception == null) exception = e; }
        if(exception != null) throw exception;
    }

    private void checkAndDoTCPIfNeeded(ByteBuffer result, DNSException exception, ByteBuffer question, String address,
            int port, Selector selector, ResultListener resultListener) {
        if(udpIsTruncated(result)) {
            question.position(0);
            tcpClient.startQuery(question, address, port, selector, (r, e) -> {
                if(r != null) r.position(2);
                resultListener.result(r, e);
            });
        } else {
            resultListener.result(result, exception);
        }
    }

    private boolean udpIsTruncated(ByteBuffer udpResult) {
        return udpResult != null && (Utils.getInt(udpResult.array(), 2, 2) & 0x0200) != 0;
    }
}
