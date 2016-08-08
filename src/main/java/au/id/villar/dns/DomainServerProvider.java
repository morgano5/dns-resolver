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

import au.id.villar.dns.cache.CachedResourceRecord;
import au.id.villar.dns.cache.DnsCache;
import au.id.villar.dns.engine.DnsClass;
import au.id.villar.dns.engine.DnsEngine;
import au.id.villar.dns.engine.DnsType;
import au.id.villar.dns.engine.Question;

import java.util.*;

class DomainServerProvider {

//	private DnsEngine engine;
//	private DnsCache cache;
//	private String targetName;
//	private LinkedList<NameServer> dnsServers;
//	private Set<String> alreadyUsedIps;
//	private NameServer root;
//
//	public DomainServerProvider(DnsEngine engine, DnsCache cache, String targetName, NameServer root) {
//		this.cache = cache;
//		this.targetName = targetName;
//		this.dnsServers = new LinkedList<>();
//		this.alreadyUsedIps = new LinkedHashSet<>();
//		this.root = root;
//		this.engine = engine;
//	}
//
//	public String getNextAddressFromNewServer() {
//		dnsServers.pollFirst();
//		return getNextAddress();
//	}
//
//	public String getNextAddress() {
//
//		String ip = getIpFromDnsServers();
//		if(ip != null) return ip;
//
//		int pos = targetName.indexOf(".");
//		if(pos != -1) {
//			targetName = targetName.substring(pos + 1);
//			Question question = engine.createQuestion(targetName, DnsType.NS, DnsClass.IN);
//			List<CachedResourceRecord> rrs = cache.getResourceRecords(question);
//		}
//
//
//	}
//
//	public void addAddresses(String name, String ... ips) {
//		for(NameServer server: dnsServers) {
//			if(server.getName().equals(name)) {
//				for(String ip: ips) if(!alreadyUsedIps.contains(ip)) server.addIp(ip);
//				return;
//			}
//		}
//		addNameServer(new NameServer(name, ips));
//	}
//
//	private void addNameServer(NameServer nameServer) {
//		int index = 0;
//		for(NameServer item: dnsServers) {
//			if(item.getName().length() < nameServer.getName().length()) {
//				dnsServers.add(index, nameServer);
//				return;
//			}
//			index++;
//		}
//		dnsServers.add(nameServer);
//	}
//
//	private String getIpFromDnsServers() {
//		String ip = null;
//		do {
//			NameServer server = dnsServers.peekFirst();
//			if(server == null) break;
//			ip = server.pollIp();
//			if(ip == null) dnsServers.pollFirst();
//		} while(ip == null);
//		alreadyUsedIps.add(ip);
//		return ip;
//	}

}

