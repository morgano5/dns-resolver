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

import au.id.villar.dns.engine.DnsClass;
import au.id.villar.dns.engine.DnsItem;
import au.id.villar.dns.engine.DnsType;
import au.id.villar.dns.engine.ResourceRecord;

public class CachedResourceRecord implements DnsItem {

	private final ResourceRecord wrapped;
	private final long bestBefore;

	public CachedResourceRecord(ResourceRecord wrapped) {
		this.wrapped = wrapped;
		this.bestBefore = wrapped.getSecondsCache() * 1000L + System.currentTimeMillis();
	}

	public <T> T getData(Class<T> tClass) {
		return wrapped.getData(tClass);
	}

	@Override
	public DnsClass getDnsClass() {
		return wrapped.getDnsClass();
	}

	@Override
	public String getDnsName() {
		return wrapped.getDnsName();
	}

	@Override
	public DnsType getDnsType() {
		return wrapped.getDnsType();
	}

	public boolean isExpired() {
		return bestBefore - System.currentTimeMillis() < 0;
	}

	public ResourceRecord getResourceRecord() {
		return wrapped;
	}
}
