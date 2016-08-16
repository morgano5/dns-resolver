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

/**
 * Handles queries to a name server. First, a query is done using UPD, port 53; if there is an answer it is reported
 * to the specified {@link ResultListener} and the process finishes, unless the response if truncated in which case
 * another query is performed using TCP, port 53.
 */
public class DNSRequestClient implements Closeable {

    private static final int DNS_PORT = 53;

    private final UDPDNSQueryClient udpClient;
    private final TCPDNSQueryClient tcpClient;

    /** Constructor */
    public DNSRequestClient() throws IOException {
        this.udpClient = new UDPDNSQueryClient();
        this.tcpClient = new TCPDNSQueryClient();
    }

    /**
     * Starts a query using a {@link Selector}. The specified selector will be used to monitor when the communication
     * channel is ready, so the thread unblocked by selector can continue by invoking
     * {@link #processAttachement(SelectionKey)}.
     * @param question A buffer containing the raw DNS query message, prefixed with a two-byte number specifying the
     *                 size of the query.
     * @param dnsServerAddress The address (IPv4, IPv6) of the name server.
     * @param selector The selector used to monitor whenever the communications channel is ready for an operation.
     * @param resultListener A listener to be invoked when there is a result from the server or when there is an error.
     * @return {@literal true} if the operation was already finished and the given {@link Selector} was not used.
     * {@literal false} otherwise.
     * @see #checkAndDoTCPIfNeeded(ByteBuffer, DNSException, ByteBuffer, String, Selector, ResultListener)
     * @see Selector
     * @see SelectionKey
     */
    public boolean startQuery(ByteBuffer question, String dnsServerAddress, Selector selector,
            ResultListener resultListener) {

        if(selector == null) throw new NullPointerException("selector cannot be null");
        if(resultListener == null) throw new NullPointerException("resultListener cannot be null");

        return udpClient.startQuery(question, dnsServerAddress, DNS_PORT, selector,
                (r, e) -> checkAndDoTCPIfNeeded(r, e, question, dnsServerAddress, selector, resultListener));

    }

    /**
     * Utility method to check after a {@link Selector#select()} if the resulting {@link SelectionKey}s contain
     * a pending task from this task and execute it if this is the case.
     * @param selectionKey Any key to test, taken from {@link Selector#selectedKeys()} after a
     * {@link Selector#select()}.
     * @return {@literal true} if the attachment contained a task from this object and was successfully finished (i. e.
     * this {@link SelectionKey} can be removed from the {@link Selector}). {@literal false} otherwise.
     * @see #startQuery(ByteBuffer, String, Selector, ResultListener)
     * @see Selector
     * @see SelectionKey
     */
    public boolean processAttachement(SelectionKey selectionKey) {
        Object attachment = selectionKey.attachment();
        return attachment instanceof DNSQueryClient && ((DNSQueryClient)attachment).doIO();
    }

    /**
     * Executes a query asynchronously.
     * @param question A buffer containing the raw DNS query message, prefixed with a two-byte number specifying the
     *                 size of the query.
     * @param dnsServerAddress The address (IPv4, IPv6) of the name server.
     * @param timeout The approximate amount of time in milliseconds to wait for a response to be ready.
     * @return A buffer with the raw response from the name server, or null if the call to this method timeout and there
     * is no response available.
     * @throws DNSException If a DNS related error happened during the process.
     * @throws InterruptedException If the executing thread was interrupted.
     */
    public ByteBuffer query(ByteBuffer question, String dnsServerAddress, long timeout)
            throws DNSException, InterruptedException {

        @SuppressWarnings("WeakerAccess")
        class ResultHolder { ByteBuffer result; DNSException exception; }
        ResultHolder holder = new ResultHolder();

        long start = System.currentTimeMillis();
        boolean udpDone = false;
        boolean done = udpClient.startQuery(question, dnsServerAddress, DNS_PORT, null, (r, e) -> {
            holder.result = r;
            holder.exception = e;
        });
        while(!done && !Thread.interrupted()) {
            if(System.currentTimeMillis() - start > timeout) break;
            Thread.sleep(10);
            if(!udpDone) {
                udpDone = udpClient.doIO();
                if(udpDone) {
                    if(!udpIsTruncated(holder.result)) break;
                    question.position(0);
                    done = tcpClient.startQuery(question, dnsServerAddress, DNS_PORT, null, (r, e) -> {
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
            Selector selector, ResultListener resultListener) {
        if(udpIsTruncated(result)) {
            question.position(0);
            tcpClient.startQuery(question, address, DNS_PORT, selector, (r, e) -> {
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
