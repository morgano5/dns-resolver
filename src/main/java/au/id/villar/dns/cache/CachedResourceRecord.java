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

import au.id.villar.dns.engine.DNSClass;
import au.id.villar.dns.engine.DNSItem;
import au.id.villar.dns.engine.DNSType;
import au.id.villar.dns.engine.ResourceRecord;

class CachedResourceRecord implements DNSItem {

    private final ResourceRecord wrapped;
    private final long bestBefore;
    private final long timeAdded;
    private long timeAccessed;

    public CachedResourceRecord(ResourceRecord wrapped, long timeAdded) {
        this.wrapped = wrapped;
        this.timeAdded = timeAdded;
        this.timeAccessed = timeAdded;
        this.bestBefore = wrapped.getSecondsCache() * 1000L + this.timeAdded;
    }

    public <T> T getData(Class<T> tClass) {
        return wrapped.getData(tClass);
    }

    @Override
    public DNSClass getDnsClass() {
        return wrapped.getDnsClass();
    }

    @Override
    public String getDnsName() {
        return wrapped.getDnsName();
    }

    @Override
    public DNSType getDnsType() {
        return wrapped.getDnsType();
    }

    public boolean isExpired() {
        return bestBefore - System.currentTimeMillis() < 0;
    }

    public long getTimeAdded() {
        return timeAdded;
    }

    public long getTimeAccessed() {
        return timeAccessed;
    }

    public void setTimeAccessed(long timeAccessed) {
        this.timeAccessed = timeAccessed;
    }

    public ResourceRecord getResourceRecord() {
        return wrapped;
    }
}
