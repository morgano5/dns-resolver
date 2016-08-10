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
package au.id.villar.dns.engine;

import java.util.Map;

/**
 * Object to contain the RDATA part of a DNS resource record. It depends on the DNS type.
 */
public interface RRValueConverter {

    /**
     *
     * @param data
     * @return
     */
    Object convertToRawData(Object data);

    /**
     * Reads a "raw" RR data from the specified array.
     * @param data the array from which the "raw" RR data is going to be parsed
     * @param offset the position on the array where the read operation starts.
     * @param length the maximum number of bytes that can be used to parse the "raw" RR data
     * @param previousNames
     * @return
     */
    Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames);

    /**
     * Converts a "raw" RR data into an instance of the specified type. This method should create
     * a new instance on every call or return the same instance provided it is immutable.
     * @param rawObject "raw" RR data to be converted
     * @param tClass The type of the object to be returned.
     * @return The created object.
     * @throws IllegalArgumentException if the internal data can't be represented by an instance of the specified type.
     */
    <T> T convertValue(Object rawObject, Class<T> tClass);

    /**
     * Writes the "raw" RR data to an array in the format expected in a DNS message.
     * @param rawObject  "raw" RR data to be "serialized"
     * @param array array where the internal data is to be written.
     * @param offset the array's offset where the write operation starts.
     * @param linkOffset for DNS name compression purposes (see RFC-1035, section 4.1.4), the message offset
     *                   value where the write operation starts.
     * @param nameLinks for DNS name compression purposes (see RFC-1035, section 4.1.4), a map of names to link
     *                  offsets already in the message. This is expected to both utilize already found names and to
     *                  register new ones, the key is the name and the integer is the message offset were the name
     *                  appears.
     * @return the number of bytes written.
     */
    int writeRawData(Object rawObject, byte[] array, int offset, int linkOffset,
            Map<String, Integer> nameLinks);

}
