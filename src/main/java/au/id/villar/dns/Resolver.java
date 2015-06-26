package au.id.villar.dns;

import au.id.villar.dns.cache.CachedResourceRecord;
import au.id.villar.dns.cache.DnsCache;
import au.id.villar.dns.cache.SimpleDnsCache;
import au.id.villar.dns.engine.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Resolver {

	private static final int DNS_PORT = 53;

	private AtomicInteger nextId = new AtomicInteger(1);

	private DnsEngine engine = new DnsEngine();
	private List<String> dnsRootServers = new ArrayList<>();
	private DnsCache cache = new SimpleDnsCache(); // TODO customize

	public Resolver() {

		// TODO get info from somewhere else
		dnsRootServers.add("198.41.0.4");
		dnsRootServers.add("192.228.79.201");
		dnsRootServers.add("192.33.4.12");
		dnsRootServers.add("199.7.91.13");
		dnsRootServers.add("192.203.230.10");
		dnsRootServers.add("192.5.5.241");
		dnsRootServers.add("192.112.36.4");
		dnsRootServers.add("128.63.2.53");
		dnsRootServers.add("192.36.148.17");
		dnsRootServers.add("192.58.128.30");
		dnsRootServers.add("193.0.14.129");
		dnsRootServers.add("199.7.83.42");
		dnsRootServers.add("202.12.27.33");

	}

	public AnswerProcess lookup(String name, DnsType type) {
		Question question = engine.createQuestion(name, type, DnsClass.IN);
		return new AnswerProcess(question);
	}

	private short getNextId() {
		short id = (short)nextId.incrementAndGet();
		if (id == 0) id = (short)nextId.incrementAndGet();
		return id;
	}

	private Deque<String> getInitialDnsServers(String name) {
		boolean firstTime = true;
		List<CachedResourceRecord> nsResult;
		List<CachedResourceRecord> ipResult;
		Deque<String> result = new LinkedList<>();

		result.addAll(getShuffledRootServers());

		while(!name.isEmpty()) {

			if(firstTime) {
				firstTime = false;
			} else {
				int dotPos = name.indexOf('.');
				if(dotPos == -1) break;
				name = name.substring(dotPos + 1);
			}

			nsResult = cache.getResourceRecords(engine.createQuestion(name, DnsType.NS, DnsClass.IN));
			for(CachedResourceRecord ns: nsResult) {
				if(ns.isExpired()) {
					cache.removeResourceRecord(ns);
					continue;
				}
				ipResult = cache.getResourceRecords(engine.createQuestion(ns.getDnsName(), DnsType.A, DnsClass.IN));
				for(CachedResourceRecord ip: ipResult) {
					if(ip.isExpired()) {
						cache.removeResourceRecord(ip);
						continue;
					}
					result.addFirst(ip.getData(String.class));
				}
			}
		}
		return result;
	}

	private List<String> getShuffledRootServers() {
		List<String> rootServers = new ArrayList<>(dnsRootServers);
		Collections.shuffle(rootServers);
		return rootServers;
	}

	private enum AnswerProcessStatus {
		START,
		OPENING_TCP, CONNECTING_TCP, SENDING_TCP, RECEIVING_TCP,
		SENDING_UDP, RECEIVING_UDP
	}






	public class AnswerProcess {

		private final String targetName;

		private AnswerProcessStatus processStatus;
		private Deque<String> dnsServerNames;
		private Deque<String> dnsServerIps;
		private Set<String> alreadyUsedNames;
		private Set<String> alreadyUsedIps;
		private String currentIp;
		private ByteBuffer message;

		private Selector selector;
		private SocketChannel tcpChannel;
		private DatagramChannel udpChannel;
		private ByteBuffer buffer;

		private AnswerProcess recurringProcess;

		private List<ResourceRecord> result;
		private DnsException exception;

		public AnswerProcess(Question question) {

			this.targetName = question.getDnsName();

//			DnsType type = question.getDnsType();
//
///*
//AXFR            252 A request for a transfer of an entire zone
//MAILB           253 A request for mailbox-related records (MB, MG or MR)
//MAILA           254 A request for mail agent RRs (Obsolete - see MX)
//*               255 A request for all records
//*/
//			// TODO support queries returning more than one resource record type
//			if(type.equals(DnsType.AXFR) || type.equals(DnsType.MAILB) || type.equals(DnsType.MAILA)
//					|| type.equals(DnsType.ALL) || question.getDnsClass().equals(DnsClass.ANY)) {
//			}

			List<CachedResourceRecord> records = cache.getResourceRecords(question);
			boolean expired = false;
			for(CachedResourceRecord record: records) if(record.isExpired()) { expired = true; break; }

			if(!expired && records.size() > 0) {
				result = records.stream().map(CachedResourceRecord::getResourceRecord).collect(Collectors.toList());
				return;
			}

			this.processStatus = AnswerProcessStatus.START;
			this.dnsServerNames = new LinkedList<>();
			this.dnsServerIps = getInitialDnsServers(question.getDnsName());
			this.alreadyUsedNames = new HashSet<>();
			this.alreadyUsedIps = new HashSet<>();
			this.message = createQueryMessage(question);
		}

		public boolean doIO() {
			try {
				boolean finished = internalDoIO();
				if(!finished) return false;
			} catch(IOException e) {
				exception = new DnsException(e);
			} catch(DnsException e) {
				exception = e;
			}
			closeChannel(tcpChannel);
			closeChannel(udpChannel);
			return true;
		}

		private void closeChannel(SelectableChannel channel) {
			try {
				if(channel != null) channel.close();
			} catch(IOException ignore) {}
		}

		private boolean internalDoIO() throws IOException, DnsException {

			while(!thereIsResult()) {

				switch(processStatus) {

					case START:

						if(!updateDnsServerIps()) return false;

						currentIp = pollNextIp();
						if(currentIp == null) {
							result = Collections.emptyList();
							return true;
						}

						if(message.limit() > 512) {
							processStatus = AnswerProcessStatus.OPENING_TCP;
							if(!doTcpQuery()) return false;
						} else {
							processStatus = AnswerProcessStatus.SENDING_UDP;
							if(!doUdpQuery()) return false;
						}
						break;

					case OPENING_TCP: case CONNECTING_TCP: case SENDING_TCP: case RECEIVING_TCP:

						if(!doTcpQuery()) return false;
						break;

					case SENDING_UDP: case RECEIVING_UDP:

						if(!doUdpQuery()) return false;
						break;

				}

				DnsMessage response = engine.createMessageFromBuffer(buffer.array(), 0);

				grabAnswersIfExist(response);

				if(!thereIsResult() && response.isTruncated()) {
					processStatus = AnswerProcessStatus.OPENING_TCP;
					return false;
				}

				addExtraInfoToCache(response);
			}

			return true;

		}

		private void addExtraInfoToCache(DnsMessage response) {

			for(int i = 0; i < response.getNumAdditionals(); i++) {
				cache.addResourceRecord(response.getAdditional(i));
			}

			if(response.getNumAuthorities() > 0) {
				Deque<String> serverNames = new LinkedList<>();
				for(int i = 0; i < response.getNumAuthorities(); i++) {
					cache.addResourceRecord(response.getAuthority(i));
					String name = response.getAuthority(i).getDnsName();
					if(isSelfOrSuperDomain(name, targetName)) {
						serverNames.addFirst(response.getAuthority(i).getData(String.class));
					}
				}
				serverNames.forEach(this::addServerName);
			}
		}

		private void grabAnswersIfExist(DnsMessage response) {
			int numAnswers = response.getNumAnswers();
			if(numAnswers > 0) {
				result = new ArrayList<>(numAnswers);
				for (int i = 0; i < numAnswers; i++) {
					result.add(response.getAnswer(i));
				}
			}
		}

		public List<ResourceRecord> getResult() throws DnsException {
			if(exception != null) throw exception;
			return result;
		}

		public void useSelector(Selector selector) {
			this.selector = selector;
		}

		private boolean doTcpQuery() throws IOException {
			switch(processStatus) {

				case OPENING_TCP:

					buffer = ByteBuffer.allocate(5120);

					tcpChannel = SocketChannel.open();
					tcpChannel.configureBlocking(false);
					useSelectIfAvailable(tcpChannel);
					processStatus = AnswerProcessStatus.CONNECTING_TCP;
					tcpChannel.connect(new InetSocketAddress(currentIp, DNS_PORT));

				case CONNECTING_TCP:

					if(!tcpChannel.isConnected()) return false;
					message.position(0);

				case SENDING_TCP:

					tcpChannel.write(message);
					if(message.remaining() > 0) {
						processStatus = AnswerProcessStatus.SENDING_TCP;
						return false;
					}

				case RECEIVING_TCP:

					// TODO: how to read in non-blocking mode for TCP and make the buffer bigger if needed
					try {
						int received;
						do {
							received = tcpChannel.read(buffer);
							if(received == 0) Thread.sleep(1000);
						} while (received == 0);
					} catch (InterruptedException ignore) {}

					tcpChannel.close();
					processStatus = AnswerProcessStatus.START;

			}
			return true;
		}

		private void enlargeBuffer() {
			ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + 512);
			buffer.flip();
			newBuffer.put(buffer);
			buffer = newBuffer;
		}

		private void useSelectIfAvailable(SelectableChannel channel) throws IOException {
			if(selector == null) return;
			int ops = channel.validOps();
			SelectionKey key = channel.register(selector, ops);
			key.attach(this);
		}

		private boolean doUdpQuery() throws IOException {

			switch(processStatus) {

				case SENDING_UDP:

					udpChannel = DatagramChannel.open();
					udpChannel.configureBlocking(false);
					useSelectIfAvailable(udpChannel);
					udpChannel.socket().connect(new InetSocketAddress(currentIp, DNS_PORT));

					message.position(0);
					if(udpChannel.write(message) == 0) {
						udpChannel.close();
						return false;
					}
					buffer = ByteBuffer.allocate(512);

				case RECEIVING_UDP:

					if(udpChannel.receive(buffer) == null) {
						processStatus = AnswerProcessStatus.RECEIVING_UDP;
						return false;
					}

					udpChannel.close();
					processStatus = AnswerProcessStatus.START;

			}
			return true;
		}

		private boolean thereIsResult() {
			return result != null || exception != null;
		}

		private void addServerName(String name) {
			if(alreadyUsedNames.contains(name)) return;
			alreadyUsedNames.add(name);
			dnsServerNames.addFirst(name);
		}

		private boolean updateDnsServerIps() throws IOException, DnsException {
			if(recurringProcess == null) {
				if(dnsServerNames.size() == 0) return true;
				recurringProcess = Resolver.this.lookup(dnsServerNames.pollFirst(), DnsType.A);
			}
			boolean added = false;
			while(recurringProcess != null) {
				if (!recurringProcess.doIO()) return false;
				for (ResourceRecord record : recurringProcess.getResult()) {
					String ip = record.getData(String.class);
					if (!alreadyUsedIps.contains(ip)) {
						dnsServerIps.addFirst(ip);
						added = true;
					}
				}
				recurringProcess = null;
				if(!added) {
					if(dnsServerNames.size() == 0) return true;
					recurringProcess = Resolver.this.lookup(dnsServerNames.pollFirst(), DnsType.A);
				}
			}
			return true;
		}

		private String pollNextIp() {
			String ip = dnsServerIps.pollFirst();
			if(ip == null) return null;
			alreadyUsedIps.add(ip);
			return ip;
		}

		private ByteBuffer createQueryMessage(Question question) {

			short id = getNextId();
			DnsMessage dnsMessage = engine.createMessage(id, false, Opcode.QUERY, false, false, true, false,
					(byte)0, ResponseCode.NO_ERROR, new Question[] {question}, null, null, null);

			int aproxSize = 512;
			ByteBuffer buffer = null;
			while(buffer == null) {
				try {
					byte[] testingBuffer = new byte[aproxSize];
					int actualSize = dnsMessage.writeToBuffer(testingBuffer, 0);
					buffer = ByteBuffer.allocate(actualSize);
					buffer.put(testingBuffer, 0, actualSize);
					buffer.position(0);
				} catch(IndexOutOfBoundsException e) {
					aproxSize += 512;
				}
			}
			return buffer;
		}

		private boolean isSelfOrSuperDomain(String superDomain, String subDomain) {
			return superDomain.length() <= subDomain.length() && subDomain.endsWith(superDomain);
		}

	}
}
