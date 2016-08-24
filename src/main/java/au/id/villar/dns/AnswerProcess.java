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
package au.id.villar.dns;

import au.id.villar.dns.cache.DNSCache;
import au.id.villar.dns.converter.SoaValueConverter;
import au.id.villar.dns.engine.DNSClass;
import au.id.villar.dns.engine.DNSEngine;
import au.id.villar.dns.engine.DNSMessage;
import au.id.villar.dns.engine.DNSType;
import au.id.villar.dns.engine.Question;
import au.id.villar.dns.engine.ResourceRecord;
import au.id.villar.dns.net.DNSNetClient;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class AnswerProcess implements Closeable {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final DNSEngine engine;
    private final DNSNetClient netClient = new DNSNetClient();
    private DNSCache cache;
    private Deque<SearchingTask> pendingTasks = new LinkedList<>();

    public AnswerProcess(DNSEngine engine, DNSCache cache) {
        this.engine = engine;
        this.cache = cache;
    }

    public List<ResourceRecord> lookUp(String name, DNSType type, long timeout)
            throws DNSException, InterruptedException, TimeoutException {

        pendingTasks.clear();
        pendingTasks.offerFirst(new StartSearch(name, type));
        SearchingTask task;
        TaskMessage lastResult = new TaskMessage(timeout);
        while((task = pendingTasks.pollFirst()) != null) {
            // TODO verify no recursive queries are happening
            // TODO follow CNAME when needed
            lastResult = task.tryToGetRRs(lastResult);
        }
        return lastResult.result;
    }

    @Override
    public void close() throws IOException {
        closeResources();
    }

    /* Queries the cache, if the cache doesn't return any RRs then it delegates the query to other tasks */
    private class StartSearch implements SearchingTask {

        private Question question;

        StartSearch(String name, DNSType type) {
            this.question = engine.createQuestion(name, type, DNSClass.IN);
        }

        @Override
        public TaskMessage tryToGetRRs(TaskMessage message)
                throws DNSException, InterruptedException, TimeoutException {

            message.result = cache.getResourceRecords(question, message.calculateTimeout());
            if(message.result.isEmpty()) {
                pendingTasks.offerFirst(new NameServerSearchThroughOriginalName(question));
            }
            return message;
        }
    }

    /* Traverses the name in the query for a NS, if found, then it delegates the search of the query to other tasks,
     * continues traversing the name until it receives a non-empty set of RRs or the name reaches the root */
    private class NameServerSearchThroughOriginalName implements SearchingTask {

        private Deque<ResourceRecord> sources = new LinkedList<>();
        private Question question;
        private String name;

        NameServerSearchThroughOriginalName(Question question) {
            this.question = question;
            this.name = question.getDnsName();
        }

        @Override
        public TaskMessage tryToGetRRs(TaskMessage message)
                throws DNSException, InterruptedException, TimeoutException {

            if(!message.result.isEmpty()) return message;

            while(name != null || !sources.isEmpty()) {
                if (!sources.isEmpty()) {
                    ResourceRecord source = sources.pollFirst();
                    pendingTasks.offerFirst(this);
                    pendingTasks.offerFirst(new SearchInNameServer(question));
                    pendingTasks.offerFirst(new StartSearch(source.getData(String.class), DNSType.A));
                    return message;
                }
                do {
                    Question question = engine.createQuestion(name, DNSType.NS, DNSClass.IN);
                    int dotPos;
                    name = "".equals(name) ? null : ((dotPos = name.indexOf('.')) != -1) ? name.substring(dotPos + 1) : "";
                    List<ResourceRecord> result = cache.getResourceRecords(question, message.calculateTimeout());
                    Collections.reverse(result);
                    result.forEach(sources::offerFirst);
                } while ((name != null && sources.isEmpty()));
            }

            return message;
        }
    }

    /* Receives IPs of name servers and use them to get results from them. Returns the answer from the servers or
     * delegates to other tasks if it receives no responses but one or more authorities and additionals */
    private class SearchInNameServer implements SearchingTask {

        private Question question;
        private Deque<ResourceRecord> sources = new LinkedList<>();

        SearchInNameServer(Question question) {
            this.question = question;
        }

        @Override
        public TaskMessage tryToGetRRs(TaskMessage message)
                throws DNSException, InterruptedException, TimeoutException {

            Collections.reverse(message.result);
            message.result.forEach(sources::offerFirst);
            message.result = Collections.emptyList();

            while(!sources.isEmpty() && message.result.isEmpty()) {

                ResourceRecord source = sources.pollFirst();
                if(source.getDnsType().equals(DNSType.NS)) {
                    pendingTasks.offerFirst(this);
                    pendingTasks.offerFirst(new StartSearch(source.getData(String.class), DNSType.A));
                    return message;
                }
                if(source.getDnsType().equals(DNSType.SOA)) {
                    pendingTasks.offerFirst(this);
                    pendingTasks.offerFirst(new StartSearch(source.getData(SoaValueConverter.SoaData.class).getDomainName(), DNSType.A));
                    return message;
                }

System.out.println("Query: " + question + ", using: " + source); // TODO remove this debugging line
                ByteBuffer result = netClient.query(
                        createQueryMessage(question), source.getData(String.class), message.calculateTimeout());
                DNSMessage response = engine.createMessageFromBuffer(result.array(), result.position());
                if(response.getNumAnswers() > 0) {
                    message.result = new ArrayList<>();
                    for(int c = 0; c < response.getNumAnswers(); c++) message.result.add(response.getAnswer(c));
                } else {
                    for(int c = 0; c < response.getNumAuthorities(); c++) {
                        ResourceRecord ns = response.getAuthority(c);
                        sources.offerFirst(ns);
                        cache.addResourceRecord(ns);
                    }
                    for(int c = 0; c < response.getNumAdditionals(); c++) {
                        cache.addResourceRecord(response.getAdditional(c));
                    }
                }

            }
            return message;
        }
    }

    private ByteBuffer createQueryMessage(Question question) {
        short id = getNextId();
        DNSMessage dnsMessage = engine.createSimpleQueryMessage(id, question);
        return engine.createBufferFromMessage(dnsMessage);
    }

    private short getNextId() {
        short id = (short)nextId.incrementAndGet();
        if (id == 0) id = (short)nextId.incrementAndGet();
        return id;
    }

    private void closeResources() throws IOException {
        netClient.close();
    }

    private interface SearchingTask {

        TaskMessage tryToGetRRs(TaskMessage taskMessage) throws DNSException, InterruptedException, TimeoutException;

    }

    private class TaskMessage {
        private List<ResourceRecord> result;
        private long timeLimit;

        TaskMessage(long timeout) {
            this.timeLimit = System.currentTimeMillis() + timeout;
            this.result = Collections.emptyList();
        }

        long calculateTimeout() throws TimeoutException {
            long timeout = timeLimit - System.currentTimeMillis();
            if(timeout <= 0) throw new TimeoutException("DNS Query couldn't complete on time");
            return timeout;
        }
    }
}
