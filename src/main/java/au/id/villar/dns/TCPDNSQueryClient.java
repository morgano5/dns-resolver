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

import au.id.villar.dns.converter.SoaValueConverter;
import au.id.villar.dns.engine.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class TCPDNSQueryClient /*implements DNSQueryClient*/ {

//	private static final int DNS_PORT = 53;
//	private static final int UDP_DATAGRAM_MAX_SIZE = 512;
//	private static final int UDP_RETRY_MILLISECONDS = 1000;
//
//	private enum AnswerProcessStatus {
//		START,
//		OPENING_TCP, CONNECTING_TCP, SENDING_TCP, RECEIVING_TCP,
//		OPENING_UDP, SENDING_UDP, RECEIVING_UDP,
//		RESULT
//	}
//
//	private final AtomicInteger nextId = new AtomicInteger(1);
//
//	private final DnsEngine engine;
//
//	private String targetName;
//
//	private AnswerProcessStatus processStatus;
//	private String address;
//	private ByteBuffer message;
//
//	private Selector selector;
//	private SocketChannel tcpChannel;
//	private DatagramChannel udpChannel;
//	private ByteBuffer buffer;
//	private long udpTimestamp;
//
//	private DnsMessage result;
//	private DnsException exception;
//
//	TCPDNSQueryClient(DnsEngine engine) {
//		this.engine = engine;
//	}
//
//	public boolean startQuery(Question question, String address, int timeoutMillis) {
//		closeResources();
//		this.address = address;
//		this.targetName = question.getDnsName();
//		try {
//			this.selector = Selector.open();
//		} catch (IOException e) {
//			this.exception = new DnsException(e);
//			return true;
//		}
//		this.processStatus = AnswerProcessStatus.START;
//		this.message = createQueryMessage(question);
//		return doIO(timeoutMillis);
//	}
//
//	public boolean doIO(int timeoutMillis) {
//		try {
//			boolean finished = internalDoIO(timeoutMillis);
//			if(!finished) return false;
//		} catch(IOException e) {
//			exception = new DnsException(e);
//		} catch(DnsException e) {
//			exception = e;
//		}
//		closeResources();
//		return true;
//	}
//
//	public DnsMessage getResult() throws DnsException {
//		if(exception != null) throw exception;
//		return result;
//	}
//
//	@Override
//	public void close() throws IOException {
//		closeResources();
//	}
//
//	private void closeResources() {
//		close(selector);
//		close(tcpChannel);
//		close(udpChannel);
//	}
//
//	private void close(Closeable channel) {
//		try {
//			if(channel != null) channel.close();
//		} catch(IOException e) {
//			if(!thereIsResult()) exception = new DnsException(e);
//		}
//	}
//
//	private boolean internalDoIO(int timeoutMillis) throws IOException, DnsException {
//
//		while(!thereIsResult()) {
//
//			try {
//
//				switch (processStatus) {
//
//					case START:
//
//						processStatus = message.limit() > UDP_DATAGRAM_MAX_SIZE ?
//								AnswerProcessStatus.OPENING_TCP :
//								AnswerProcessStatus.OPENING_UDP;
//						break;
//
//					case OPENING_TCP: case CONNECTING_TCP: case SENDING_TCP: case RECEIVING_TCP:
//
//						if (!doTcpQuery(timeoutMillis)) return false;
//						break;
//
//					case OPENING_UDP: case SENDING_UDP: case RECEIVING_UDP:
//
//						if (!doUdpQuery(timeoutMillis)) return false;
//						break;
//
//					case RESULT:
//
//						DnsMessage response = engine.createMessageFromBuffer(buffer.array(), 0);
//
//						grabAnswersIfExist(response);
//
//						if (!thereIsResult() && response.isTruncated()) {
//							processStatus = AnswerProcessStatus.OPENING_TCP;
//							break;
//						}
//
//						addExtraInfoToCache(response);
//						processStatus = AnswerProcessStatus.START;
//
//				}
//
//			} catch (SocketException e) {
//				exception = new DnsException(e);
//				close(tcpChannel);
//				close(udpChannel);
//			}
//		}
//		return true;
//	}
//
//	private void addExtraInfoToCache(DnsMessage response) {
//
//		for(int i = 0; i < response.getNumAdditionals(); i++) {
//			cache.addResourceRecord(response.getAdditional(i));
//		}
//
//		if(response.getNumAuthorities() > 0) {
//			Deque<String> serverNames = new LinkedList<>();
//			for(int i = 0; i < response.getNumAuthorities(); i++) {
//				ResourceRecord authority = response.getAuthority(i);
//				cache.addResourceRecord(authority);
//				String name = authority.getDnsName();
//				if(isSelfOrSuperDomain(name, targetName)) {
//					if(authority.getDnsType().equals(DnsType.NS)) {
//						serverNames.addFirst(authority.getData(String.class));
//					} else if(authority.getDnsType().equals(DnsType.SOA)) {
//						serverNames.addFirst(authority.getData(SoaValueConverter.SoaData.class).getDomainName());
//					}
//				}
//			}
//			serverNames.forEach(name -> addServerAndIps(dnsServers, name));
//		}
//	}
//
//	private void grabAnswersIfExist(DnsMessage response) {
//		int numAnswers = response.getNumAnswers();
//		if(numAnswers > 0) {
//			result = new ArrayList<>(numAnswers);
//			for (int i = 0; i < numAnswers; i++) {
//				result.add(response.getAnswer(i));
//			}
//		}
//	}
//
//	private boolean doTcpQuery(int timeoutMillis) throws IOException {
//		switch(processStatus) {
//
//			case OPENING_TCP:
//
//				tcpChannel = SocketChannel.open();
//				tcpChannel.configureBlocking(false);
//				tcpChannel.register(selector, SelectionKey.OP_CONNECT);
//				processStatus = AnswerProcessStatus.CONNECTING_TCP;
//				tcpChannel.connect(new InetSocketAddress(currentIp, DNS_PORT));
//
//			case CONNECTING_TCP:
//
//				if(selector.select(timeoutMillis) == 0) {
//					processStatus = AnswerProcessStatus.CONNECTING_TCP;
//					return false;
//				}
//				tcpChannel.register(selector, SelectionKey.OP_WRITE);
//				message.position(0);
//
//			case SENDING_TCP:
//
//				if(!sendDataAndPrepareForReceiving(timeoutMillis, tcpChannel)) {
//					processStatus = AnswerProcessStatus.SENDING_TCP;
//					return false;
//				}
//
//			case RECEIVING_TCP:
//
//				if(selector.select(timeoutMillis) == 0) {
//					processStatus = AnswerProcessStatus.RECEIVING_TCP;
//					return false;
//				}
//				receiveTcp();
//				tcpChannel.close();
//				processStatus = AnswerProcessStatus.RESULT;
//
//		}
//		return true;
//	}
//
//	private void receiveTcp() throws IOException {
//		int received;
//		buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE * 2);
//		do {
//			received = tcpChannel.read(buffer);
//			if(received > 0) enlargeBuffer();
//		} while(received > 0);
//	}
//
//	private void enlargeBuffer() {
//		ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + UDP_DATAGRAM_MAX_SIZE);
//		buffer.flip();
//		newBuffer.put(buffer);
//		buffer = newBuffer;
//	}
//
//	private boolean doUdpQuery(int timeoutMillis) throws IOException {
//
//		switch(processStatus) {
//
//			case OPENING_UDP:
//
//				udpTimestamp = System.currentTimeMillis();
//				udpChannel = DatagramChannel.open();
//				udpChannel.configureBlocking(false);
//				udpChannel.socket().connect(new InetSocketAddress(currentIp, DNS_PORT));
//				message.position(0);
//				udpChannel.register(selector, SelectionKey.OP_WRITE);
//
//			case SENDING_UDP:
//
//				if(!sendDataAndPrepareForReceiving(timeoutMillis, udpChannel)) {
//					processStatus = AnswerProcessStatus.SENDING_UDP;
//					return false;
//				}
//
//			case RECEIVING_UDP:
//
//				if(selector.select(timeoutMillis) == 0) {
//					if(System.currentTimeMillis() - udpTimestamp > UDP_RETRY_MILLISECONDS) {
//						message.position(0);
//						udpChannel.register(selector, SelectionKey.OP_WRITE);
//						processStatus = AnswerProcessStatus.SENDING_UDP;
//						return false;
//					}
//					processStatus = AnswerProcessStatus.RECEIVING_UDP;
//					return false;
//				}
//				buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE);
//				udpChannel.receive(buffer);
//				udpChannel.close();
//				processStatus = AnswerProcessStatus.RESULT;
//
//		}
//		return true;
//	}
//
//	private boolean sendDataAndPrepareForReceiving(int timeoutMillis, Channel channel) throws IOException {
//		if(selector.select(timeoutMillis) == 0) return false;
//		Iterator iterator = selector.selectedKeys().iterator();
//		iterator.next();
//		iterator.remove();
//		((SelectableChannel)channel).register(selector, SelectionKey.OP_READ);
//		((WritableByteChannel)channel).write(message);
//		return true;
//	}
//
//	private boolean thereIsResult() {
//		return result != null || exception != null;
//	}
//
//	// TODO refactorize this to optimize and make more clear
//	private boolean updateDnsServerIps(int timeoutMillis) throws IOException, DnsException {
//		if(recurringProcess == null) {
//			if(dnsServers.size() == 0 || dnsServers.peekFirst().getAddresses().size() > 0) return true;
//			recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DnsType.ALL); // TODO optimize ALL to bring only A, AAAA and CNAME
//		}
//		boolean added = false;
//		while(recurringProcess != null) {
//			if (!recurringProcess.doIO(timeoutMillis)) return false;
//			for (ResourceRecord record : recurringProcess.getResult()) {
//				if(record.getDnsType() == DnsType.CNAME) {
//					addServerAndIps(dnsServers, record.getData(String.class));
//					recurringProcess.close();
//					recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DnsType.ALL);
//					return false;
//				}
//				if (record.getDnsType() == DnsType.A && useIPv4
//						|| record.getDnsType() == DnsType.AAAA && useIPv6) {
//					String ip = record.getData(String.class);
//					if (!alreadyUsedIps.contains(ip)) {
//						dnsServers.peekFirst().getAddresses().add(ip);
//						added = true;
//					}
//				}
//			}
//			recurringProcess.close();
//			recurringProcess = null;
//			if(!added) {
//				if(dnsServers.size() == 0) return true;
//				recurringProcess = resolver.lookup(dnsServers.peekFirst().getName(), DnsType.ALL);
//			}
//		}
//		return true;
//	}
//
//	private String pollNextIp() {
//		NameServer nameServer = dnsServers.peekFirst();
//		List<String> ips = nameServer.getAddresses();
//		String ip = ips.remove(ips.size() - 1);
//		if(ips.size() == 0) dnsServers.pollFirst();
//		if(ip == null) return null;
//		alreadyUsedIps.add(ip);
//		return ip;
//	}
//
//	private ByteBuffer createQueryMessage(Question question) {
//
//		short id = getNextId();
//		DnsMessage dnsMessage = engine.createMessage(id, false, Opcode.QUERY, false, false, true, false,
//				(byte)0, ResponseCode.NO_ERROR, new Question[] {question}, null, null, null);
//
//		int aproxSize = UDP_DATAGRAM_MAX_SIZE;
//		ByteBuffer buffer = null;
//		while(buffer == null) {
//			try {
//				byte[] testingBuffer = new byte[aproxSize];
//				int actualSize = dnsMessage.writeToBuffer(testingBuffer, 0);
//				buffer = ByteBuffer.allocate(actualSize);
//				buffer.put(testingBuffer, 0, actualSize);
//				buffer.position(0);
//			} catch(IndexOutOfBoundsException e) {
//				aproxSize += UDP_DATAGRAM_MAX_SIZE;
//			}
//		}
//		return buffer;
//	}
//
//	private boolean isSelfOrSuperDomain(String superDomain, String subDomain) {
//		return superDomain.length() <= subDomain.length() && subDomain.endsWith(superDomain);
//	}
//
//	private short getNextId() {
//		short id = (short)nextId.incrementAndGet();
//		if (id == 0) id = (short)nextId.incrementAndGet();
//		return id;
//	}

}
