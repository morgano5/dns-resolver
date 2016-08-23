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

import au.id.villar.dns.engine.*;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO do cleaning up periodically
/////*
////AXFR            252 A request for a transfer of an entire zone
////MAILB           253 A request for mailbox-related records (MB, MG or MR)
////MAILA           254 A request for mail agent RRs (Obsolete - see MX)
////*               255 A request for all records
////*/
////            // TODO support queries returning more than one resource record type
public class SimpleDNSCache implements DNSCache {


    private int numEntries;

    private List<CachedResourceRecord> cachedRecords = new ArrayList<>(numEntries);

    public SimpleDNSCache(int numEntries) {
        this.numEntries = numEntries;
    }

    @Override
    public void addResourceRecord(ResourceRecord resourceRecord) {
        CachedResourceRecord wrapper = new CachedResourceRecord(resourceRecord, System.currentTimeMillis());
        int position = Collections.binarySearch(cachedRecords, wrapper, this::compareRecordsForAdding);
        if(position < 0) {
            cachedRecords.add(-position - 1, wrapper);
            if(cachedRecords.size() > numEntries) cachedRecords.remove(numEntries - 1);
        } else if (resourceRecord.dataIsEqual(cachedRecords.get(position).getResourceRecord())) {
            cachedRecords.set(position, wrapper);
        } else {
            cachedRecords.add(position, wrapper);
        }
    }

    @Override
    public List<ResourceRecord> getResourceRecords(Question question, long timeout) {
        List<ResourceRecord> list = new ArrayList<>();
        int position = Collections.binarySearch(cachedRecords, question, this::compareRecordsForSearching);
        if(position < 0) return list;
        while(position >= 0 && compareRecordsForSearching(cachedRecords.get(position), question) == 0) position--;
        while(++position < cachedRecords.size()
                && compareRecordsForSearching(cachedRecords.get(position), question) == 0) {
            list.add(cachedRecords.get(position).getResourceRecord());
        }
        return list;
    }

    @Override
    public void clear() {
        cachedRecords.clear();
    }

    @Override
    public boolean getResourceRecords(Question question, Selector selector, ResourceRecordHandler handler) {
        handler.handleResourceRecord(getResourceRecords(question, 0), null);
        return true;
    }

    @Override
    public void processAttachment(SelectionKey selectionKey) {
    }

    private int compareRecordsForAdding(CachedResourceRecord item1, CachedResourceRecord item2) {
        long comp;
        if((comp = item1.getDnsName().compareTo(item2.getDnsName())) != 0)
            return comp > 0? 1: comp < 0? -1: 0;
        if((comp = item1.getDnsClass().getMnemonic().compareTo(item2.getDnsClass().getMnemonic())) != 0)
            return comp > 0? 1: comp < 0? -1: 0;
        if((comp = item1.getDnsType().getMnemonic().compareTo(item2.getDnsType().getMnemonic())) != 0)
            return comp > 0? 1: comp < 0? -1: 0;
        if((comp = item1.getTimeAdded() - item2.getTimeAdded()) != 0)
            return comp > 0? 1: comp < 0? -1: 0;
        if((comp = item1.getTimeAccessed() - item2.getTimeAccessed()) != 0)
            return comp > 0? 1: comp < 0? -1: 0;
        return 0;
    }

    private int compareRecordsForSearching(DNSItem item1, DNSItem item2) {
        int comp;
        if((comp = item1.getDnsName().compareTo(item2.getDnsName())) != 0) {
            return comp;
        }
        if((comp = item1.getDnsClass().getMnemonic().compareTo(item2.getDnsClass().getMnemonic())) != 0
                && item1.getDnsClass() != DNSClass.ANY && item2.getDnsClass() != DNSClass.ANY) {
            return comp;
        }
        if((comp = item1.getDnsType().getMnemonic().compareTo(item2.getDnsType().getMnemonic())) != 0
                && item1.getDnsType() != DNSType.ALL && item2.getDnsType() != DNSType.ALL)
            return comp;
        return 0;
    }

}
