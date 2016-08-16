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

import java.nio.ByteBuffer;

/**
 * Interface used to report when a result from a name server is ready.
 * @see DNSRequestClient
 */
@FunctionalInterface
public interface ResultListener {

    /**
     * Method to be called when a result from a name server is ready. If the operation was successful then
     * {@code result} will contain the raw response from the server and {@code exception} will be {@literal null};
     * otherwise {@code result} will be {@literal null} and {@code exception} will contain the exception thrown
     * with information of the error.
     * @param result The raw response from the name server, it can be {@literal null} if an error happened.
     * @param exception The exception thrown if there was an error. It will be {@literal null} if the operation was
     *                  successful.
     */
    void result(ByteBuffer result, DNSException exception);
}
