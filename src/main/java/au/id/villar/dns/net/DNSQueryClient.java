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

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

/**
 * Interface implemented by UDP and TCP clients to perform a query. The process is as follows:
 * <ol>
 *     <li>{@link #startQuery(ByteBuffer, String, int, Selector, ResultHandler)} is executed, if it returns
 *     {@literal true} then there is a result and the process has finished. Otherwise, wait of a reasonable amount of
 *     time (if not using a selector) of wait for the selector to unblock to continue the process.
 *     </li>
 *     <li>{@link #doIO()} is executed. If it returns false then wait of a reasonable amount of time (if not using a
 *     selector) of wait for the selector to unblock again to continue the process and repeat this same step until it
 *     returns {@literal true}.</li>
 * </ol>
 */
interface DNSQueryClient extends Closeable {

    int UDP_DATAGRAM_MAX_SIZE = 512;
    int NO_OP = 0;

    /**
     * Starts a query process. If {@code selector} is {@literal null} then the specified selector will be used to
     * monitor when the communication channel is ready, so the thread unblocked by selector can continue by invoking
     * {@link #doIO()} on this same object, which is attached to the {@link java.nio.channels.SelectionKey} taken
     * from the selector.
     * @param question A buffer containing the raw DNS query message, prefixed with a two-byte number specifying the
     *                 size of the query.
     * @param address The address (IPv4, IPv6) of the name server.
     * @param port The port where the name server is listening for connections.
     * @param selector The selector used to monitor whenever the communications channel is ready for an operation. It
     *                 can be null
     * @param resultHandler A handler to be invoked when there is a result from the server or when there is an error.
     * @return {@literal true} if the operation could be completely done and {@code resultHandler} was invoked with
     * the result. {@literal false otherwise}
     */
    boolean startQuery(ByteBuffer question, String address, int port, Selector selector, ResultHandler resultHandler);

    /**
     * Tries to continue the query process. If the client finishes then {@literal 0} is returned, otherwise a non-zero
     * value returns; if a {@link Selector} is being used, this value represents the
     * {@link java.nio.channels.SelectionKey} operation flags to be passed to
     * {@link java.nio.channels.SelectionKey#interestOps(int)}.
     * a value with the {@link java.nio.channels.SelectionKey} operations that could be used to update
     * @return the flags of interested operations to pass to {@link java.nio.channels.SelectionKey#interestOps(int)},
     * or {@literal 0} if this client has already finished.
     */
    int doIO();

}
