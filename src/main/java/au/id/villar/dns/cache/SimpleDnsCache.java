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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleDnsCache implements DnsCache {


    private int numEntries = 1000; // todo customize

    private List<CachedResourceRecord> cachedRecords = new ArrayList<>(numEntries);

    @Override
    public void addResourceRecord(ResourceRecord resourceRecord) {
        int position = Collections.binarySearch(cachedRecords, resourceRecord, this::compareRecordsForAdding);
        CachedResourceRecord wrapper = new CachedResourceRecord(resourceRecord);
        if(position < 0) {
            cachedRecords.add(-position - 1, wrapper);
            if(cachedRecords.size() > numEntries) cachedRecords.remove(numEntries - 1);
        } else {
            cachedRecords.set(position, wrapper);
        }
    }

    @Override
    public List<CachedResourceRecord> getResourceRecords(Question question) {
        int position = Collections.binarySearch(cachedRecords, question, this::compareRecordsForSearching);
        while(position >= 0 && compareRecordsForSearching(cachedRecords.get(position), question) == 0) position--;
        List<CachedResourceRecord> list = new ArrayList<>();
        if(position >= 0) while(++position < cachedRecords.size()
                && compareRecordsForSearching(cachedRecords.get(position), question) == 0) {
            list.add(cachedRecords.get(position));
        }
        return list;
    }

    @Override
    public void removeResourceRecord(DnsItem resourceRecord) {
        int position = Collections.binarySearch(cachedRecords, resourceRecord, this::compareRecordsForAdding);
        if(position >= 0) cachedRecords.remove(position);
    }

    private int compareRecordsForAdding(DnsItem item1, DnsItem item2) {
        int comp;
        if((comp = item1.getDnsName().compareTo(item2.getDnsName())) != 0)
            return comp;
        if((comp = item1.getDnsClass().getMnemonic().compareTo(item2.getDnsClass().getMnemonic())) != 0)
            return comp;
        if((comp = item1.getDnsType().getMnemonic().compareTo(item2.getDnsType().getMnemonic())) != 0)
            return comp;
        return 0;
    }

    private int compareRecordsForSearching(DnsItem item1, DnsItem item2) {
        int comp;
        if((comp = item1.getDnsName().compareTo(item2.getDnsName())) != 0) {
            return comp;
        }
        if((comp = item1.getDnsClass().getMnemonic().compareTo(item2.getDnsClass().getMnemonic())) != 0
                && item1.getDnsClass() != DnsClass.ANY && item2.getDnsClass() != DnsClass.ANY) {
            return comp;
        }
        if((comp = item1.getDnsType().getMnemonic().compareTo(item2.getDnsType().getMnemonic())) != 0
                && item1.getDnsType() != DnsType.ALL && item2.getDnsType() != DnsType.ALL)
            return comp;
        return 0;
    }

}
