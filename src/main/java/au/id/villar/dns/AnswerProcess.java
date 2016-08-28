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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/*
https://tools.ietf.org/html/rfc1033
https://tools.ietf.org/html/rfc1034
https://tools.ietf.org/html/rfc1035
*/
public class AnswerProcess implements Closeable {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final DNSEngine engine;
    private final DNSNetClient netClient = new DNSNetClient();
    private DNSCache cache;
    private Deque<SearchingTask> pendingTasks = new LinkedList<>();

    private TaskMessage lastResult;

    public AnswerProcess(DNSEngine engine, DNSCache cache) {
        this.engine = engine;
        this.cache = cache;
    }

    // TODO throw TimeoutException when time is up
    public List<ResourceRecord> lookUp(String name, DNSType type, long timeout)
            throws DNSException, InterruptedException, TimeoutException {

        TaskMessage message = new TaskMessage(null, null);
        boolean done = startLookUp(name, type, (rr, e) -> {
            message.error = e;
            message.result = rr;
        });
        while(!done) {
            Thread.sleep(10);
            done = retryLookUp();
        }
        if(message.error != null) throw new DNSException(message.error);
        return message.result;
    }

    public boolean startLookUp(String name, DNSType type, Selector selector, ResourceRecordHandler handler) {
        pendingTasks.clear();
        pendingTasks.offerFirst(new StartSearch(name, type));
        lastResult = new TaskMessage(selector, handler);
        return retryLookUp();
    }

// TODO review this code
//    public static void processAttachment(SelectionKey selectionKey) {
//        Object attachment = selectionKey.attachment();
//        if(selectionKey.isValid() && attachment instanceof AnswerProcess) {
//            ((AnswerProcess)attachment).retryLookUp();
//        }
//    }

    public boolean startLookUp(String name, DNSType type, ResourceRecordHandler handler) {
        return startLookUp(name, type, null, handler);
    }

    public boolean retryLookUp() {

        SearchingTask task;
        while((task = pendingTasks.pollFirst()) != null) {
            // TODO verify no recursive queries are happening
            // TODO follow CNAME when needed
            // TODO check lastResult.error
            lastResult = task.tryToGetRRs(lastResult);
            if(lastResult.waitingIO) return false;
        }
        lastResult.handler.handleResourceRecord(lastResult.result, lastResult.error);
        return true;
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
        public TaskMessage tryToGetRRs(TaskMessage message) {

            if(!message.waitingIO) {
                message.waitingIO = !cache.getResourceRecords(question, message.selector, (rr, e) -> {
                    message.waitingIO = false;
                    message.error = e;
                    message.result = rr;
                });
            } else {
                message.waitingIO = !cache.retryGetResourceRecords();
            }
            if(message.waitingIO) {
                pendingTasks.offerFirst(this);
                return message;
            }
            if(message.result != null && message.result.isEmpty()) {
                pendingTasks.offerFirst(new NameServerSearchThroughOriginalName(question));
            }
            return message;
        }
    }

    /* Traverses the name in the query for a NS, if found, then it delegates the search of the query to other tasks,
     * continues traversing the name until it receives a non-empty set of RRs or the name reaches the root */
    private class NameServerSearchThroughOriginalName implements SearchingTask {

        private Question question;
        private Deque<ResourceRecord> sources = new LinkedList<>();
        private String name;

        NameServerSearchThroughOriginalName(Question question) {
            this.question = question;
            this.name = question.getDnsName();
        }

        @Override
        public TaskMessage tryToGetRRs(TaskMessage message) {

            if(message.error != null || !message.result.isEmpty()) return message;

            while(name != null || !sources.isEmpty()) {
                if (!sources.isEmpty()) {
                    ResourceRecord source = sources.pollFirst();
                    pendingTasks.offerFirst(this);
                    pendingTasks.offerFirst(new SearchInNameServer(question));
                    pendingTasks.offerFirst(new StartSearch(source.getData(String.class), DNSType.A));
                    return message;
                }
                do {
                    if(!message.waitingIO) {
                        Question question = engine.createQuestion(name, DNSType.NS, DNSClass.IN);
                        int dotPos;
                        name = "".equals(name)? null:
                                ((dotPos = name.indexOf('.')) != -1)? name.substring(dotPos + 1): "";
                        message.waitingIO = !cache.getResourceRecords(question, message.selector, (rr, e) -> {
                            message.waitingIO = false;
                            message.error = e;
                            if(rr != null) {
                                Collections.reverse(rr);
                                rr.forEach(sources::offerFirst);
                            }
                        });
                    } else {
                        message.waitingIO = !cache.retryGetResourceRecords();
                    }
                    if(message.waitingIO) {
                        pendingTasks.offerFirst(this);
                        return message;
                    }
                    if(message.error != null) return message;
                } while (name != null && sources.isEmpty() && !message.waitingIO);
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
        public TaskMessage tryToGetRRs(TaskMessage message) {

            if(message.error != null) return message;

            if(message.waitingIO) {
                message.waitingIO = !netClient.retryQuery();
            } else {
                Collections.reverse(message.result);
                message.result.forEach(sources::offerFirst);
                message.result = Collections.emptyList();
            }

            while(!sources.isEmpty() && message.result.isEmpty() && !message.waitingIO) {

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
                message.waitingIO = !netClient.startQuery(createQueryMessage(question), source.getData(String.class), (b, e) -> {
                    message.waitingIO = false;
                    message.error = e;
                    // if(e != null) return; // TODO what to do with 'e' if != null (show warning that the server was unable? just return the exception?)
                    DNSMessage response = engine.createMessageFromBuffer(b.array(), b.position());
                    if(response.getNumAnswers() > 0) {
                        message.result = new ArrayList<>();
                        for(int c = 0; c < response.getNumAnswers(); c++) message.result.add(response.getAnswer(c));
                    }
                    for(int c = 0; c < response.getNumAuthorities(); c++) {
                        ResourceRecord ns = response.getAuthority(c);
                        sources.offerFirst(ns);
                        cache.addResourceRecord(ns);
                    }
                    for(int c = 0; c < response.getNumAdditionals(); c++) {
                        cache.addResourceRecord(response.getAdditional(c));
                    }
                });
            }
            if(message.waitingIO) {
                pendingTasks.offerFirst(this);
                return message;
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
        TaskMessage tryToGetRRs(TaskMessage taskMessage);
    }

    private class TaskMessage {
        private List<ResourceRecord> result = Collections.emptyList();
        private Exception error;
        private Selector selector;
        private ResourceRecordHandler handler;
        private boolean waitingIO;

        TaskMessage(Selector selector, ResourceRecordHandler handler) {
            this.selector = selector;
            this.handler = handler;
            this.result = Collections.emptyList();
        }

    }
}
