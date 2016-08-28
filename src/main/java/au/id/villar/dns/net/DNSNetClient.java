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
import java.util.concurrent.TimeoutException;

import static au.id.villar.dns.net.DNSQueryClient.NO_OP;

/**
 * Handles queries to a name server. There are three way of using an object of this class:
 * <ol>
 *     <li>Synchronized query using {@link #query(ByteBuffer, String, long)}.</li>
 *     <li>Asynchronous query with a {@link Selector} using {@link #startQuery(ByteBuffer, String, Selector,
 *     ResultHandler)} and {@link #processAttachment(SelectionKey)}.</li>
 *     <li>Asynchronous query using {@link #startQuery(ByteBuffer, String, ResultHandler)} and {@link #retryQuery()}.
 *     </li>
 * </ol>
 * The query process is a follows: First, a query is done using UPD, port 53; if there is an answer then it is returned
 * to the calling code and the process finishes, unless the response if truncated in which case another query
 * is performed using TCP, port 53.
 */
public class DNSNetClient implements Closeable {

    private static final int DNS_PORT = 53;

    private final UDPDNSQueryClient udpClient;
    private final TCPDNSQueryClient tcpClient;
    private boolean needsTCP;

    /** Constructor */
    public DNSNetClient() {
        this.udpClient = new UDPDNSQueryClient();
        this.tcpClient = new TCPDNSQueryClient();
    }

    /**
     * Executes a query synchronously.
     * @param question A buffer containing the raw DNS query message, prefixed with a two-byte number specifying the
     *                 size of the query.
     * @param dnsServerAddress The address (IPv4, IPv6) of the name server.
     * @param timeout The approximate amount of time in milliseconds to wait for a response to be ready.
     * @return A buffer with the raw response from the name server.
     * @throws DNSException If a DNS related error happened during the process.
     * @throws InterruptedException If the executing thread was interrupted.
     * @throws TimeoutException if the call to this method timeout and there is no response available.
     */
    public ByteBuffer query(ByteBuffer question, String dnsServerAddress, long timeout)
            throws DNSException, InterruptedException, TimeoutException {

        ResultHolder holder = new ResultHolder();

        long start = System.currentTimeMillis();
        boolean udpDone = false;
        boolean done = udpClient.startQuery(question, dnsServerAddress, DNS_PORT, null, (r, e) -> {
            holder.result = r;
            holder.exception = e;
        });
        while(!done && !Thread.interrupted()) {
            if(System.currentTimeMillis() - start > timeout) break;
            Thread.sleep(1);
            if(!udpDone) {
                udpDone = udpClient.doIO() == NO_OP;
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
                done = tcpClient.doIO() == NO_OP;
            }
        }
        if(holder.exception != null) throw holder.exception;
        if(holder.result == null) throw new TimeoutException("DNS query couldn't be completed in the given time");
        return holder.result;
    }

    /**
     * Starts a query asynchronously using a {@link Selector}. The specified selector will be used to monitor when the
     * communication channel is ready, so the thread unblocked by selector can continue by invoking
     * {@link #processAttachment(SelectionKey)}.
     * @param question A buffer containing the raw DNS query message, prefixed with a two-byte number specifying the
     *                 size of the query.
     * @param dnsServerAddress The address (IPv4, IPv6) of the name server.
     * @param selector The selector used to monitor whenever the communications channel is ready for an operation.
     * @param resultHandler A listener to be invoked when there is a result from the server or when there is an error.
     * @return {@literal true} if the operation was already finished and the given {@link Selector} was not used.
     * {@literal false} otherwise.
     * @see #processAttachment(SelectionKey)
     * @see Selector
     * @see SelectionKey
     */
    public boolean startQuery(ByteBuffer question, String dnsServerAddress, Selector selector,
            ResultHandler resultHandler) {

        if(resultHandler == null) throw new NullPointerException("resultHandler cannot be null");

        needsTCP = false;
        return udpClient.startQuery(question, dnsServerAddress, DNS_PORT, selector,
                (r, e) -> checkAndDoTCPIfNeeded(r, e, question, dnsServerAddress, selector, resultHandler));

    }

    /**
     * Utility method to check after a {@link Selector#select()} if the resulting {@link SelectionKey}s contain
     * a pending task coming from a DNS query initiated by a {@link DNSNetClient} and execute it if this is the
     * case. If the {@link SelectionKey}'s attachment is not related to any {@link DNSNetClient} then it just does
     * nothing.
     * @param selectionKey Any key to test, taken from {@link Selector#selectedKeys()} after a
     * {@link Selector#select()}.
     * @see #startQuery(ByteBuffer, String, Selector, ResultHandler)
     * @see Selector
     * @see SelectionKey
     */
    public static void processAttachment(SelectionKey selectionKey) {
        Object attachment = selectionKey.attachment();
        if(selectionKey.isValid() && attachment instanceof DNSQueryClient) {
            int ops = ((DNSQueryClient)attachment).doIO();
            if(ops != NO_OP) selectionKey.interestOps(ops);
        }
    }

    /**
     * Starts a query asynchronously. If it was possible to get a response during the call to this method then
     * {@literal true} is returned, otherwise teh continuation of the query must be retried with successive calls to
     * {@link #retryQuery()} until {@literal true} is returned.
     * @param question A buffer containing the raw DNS query message, prefixed with a two-byte number specifying the
     *                 size of the query.
     * @param dnsServerAddress The address (IPv4, IPv6) of the name server.
     * @param resultHandler A listener to be invoked when there is a result from the server or when there is an error.
     * @return {@literal true} if the operation was able to return a result (by invoking the {@code resulthandler}).
     * {@literal false} otherwise.
     * @see #retryQuery()
     * @see Selector
     * @see SelectionKey
     */
    public boolean startQuery(ByteBuffer question, String dnsServerAddress, ResultHandler resultHandler) {
        return startQuery(question, dnsServerAddress, null, resultHandler);
    }

    /**
     * Tries to finish a query started with {@link #startQuery(ByteBuffer, String, ResultHandler)}. This method must
     * be executed preiodically until it returns {@literal true}.
     * @return {@literal true} if the query was finalised and the {@link ResultHandler} was invoked with a result.
     * {@literal false} otherwise.
     * @see #startQuery(ByteBuffer, String, Selector, ResultHandler)
     * @see Selector
     * @see SelectionKey
     */
    public boolean retryQuery() {
        return udpClient.doIO() == NO_OP && (!needsTCP || tcpClient.doIO() == NO_OP);
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;
        try { udpClient.close(); } catch (IOException e) { exception = e; }
        try { tcpClient.close(); } catch (IOException e) { if(exception == null) exception = e; }
        if(exception != null) throw exception;
    }

    private class ResultHolder { ByteBuffer result; DNSException exception; }

    private void checkAndDoTCPIfNeeded(ByteBuffer result, DNSException exception, ByteBuffer question, String address,
            Selector selector, ResultHandler resultHandler) {
        if(udpIsTruncated(result)) {
            needsTCP = true;
            question.position(0);
            tcpClient.startQuery(question, address, DNS_PORT, selector, (r, e) -> {
                if(r != null) r.position(2);
                resultHandler.result(r, e);
            });
        } else {
            resultHandler.result(result, exception);
        }
    }

    private static boolean udpIsTruncated(ByteBuffer udpResult) {
        return udpResult != null && (Utils.getInt(udpResult.array(), 2, 2) & 0x0200) != 0;
    }
}
