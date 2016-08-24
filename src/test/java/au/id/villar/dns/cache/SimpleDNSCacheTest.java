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

package au.id.villar.dns.cache;

import au.id.villar.dns.DNSException;
import au.id.villar.dns.engine.DNSClass;
import au.id.villar.dns.engine.DNSEngine;
import au.id.villar.dns.engine.DNSType;
import au.id.villar.dns.engine.ResourceRecord;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class SimpleDNSCacheTest {

    @Test
    public void addAndGetRRs() throws InterruptedException, DNSException, TimeoutException {
        List<ResourceRecord> records;
        DNSCache cache = new SimpleDNSCache(1000);
        DNSEngine engine = new DNSEngine();
        cache.addResourceRecord(engine.createResourceRecord("test", DNSType.A, DNSClass.IN, 100000, "192.168.0.1"));
        cache.addResourceRecord(engine.createResourceRecord("another", DNSType.A, DNSClass.IN, 100000, "192.168.0.10"));
        cache.addResourceRecord(engine.createResourceRecord("another", DNSType.A, DNSClass.IN, 100000, "192.168.0.20"));

        records = cache.getResourceRecords(engine.createQuestion("whatever", DNSType.A, DNSClass.IN), 100_000L);
        assertNotNull("records shouldn't be null", records);
        assertEquals("no record must be found for 'whatever'", 0, records.size());

        records = cache.getResourceRecords(engine.createQuestion("test", DNSType.NS, DNSClass.IN), 100_000L);
        assertNotNull("records shouldn't be null", records);
        assertEquals(0, records.size());

        records = cache.getResourceRecords(engine.createQuestion("test", DNSType.A, DNSClass.ANY), 100_000L);
        assertNotNull("records shouldn't be null", records);
        assertEquals(1, records.size());

        records = cache.getResourceRecords(engine.createQuestion("test", DNSType.A, DNSClass.IN), 100_000L);
        assertNotNull("records shouldn't be null", records);
        assertEquals(1, records.size());
        assertEquals("192.168.0.1", records.get(0).getData(String.class));


        records = cache.getResourceRecords(engine.createQuestion("another", DNSType.A, DNSClass.IN), 100_000L);
        assertNotNull("records shouldn't be null", records);
        assertEquals(2, records.size());
        assertEquals("192.168.0.10", records.get(1).getData(String.class));
        assertEquals("192.168.0.20", records.get(0).getData(String.class));
    }

}
