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
package au.id.villar.dns.cache;

import au.id.villar.dns.DNSException;
import au.id.villar.dns.engine.*;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.concurrent.TimeoutException;

public interface DNSCache {

    void addResourceRecord(ResourceRecord resourceRecord);

    List<ResourceRecord> getResourceRecords(Question question, long timeout)
            throws DNSException, InterruptedException, TimeoutException;

    boolean getResourceRecords(Question question, Selector selector, ResourceRecordHandler handler);

    void processAttachment(SelectionKey selectionKey);

    void clear();

}
