package au.id.villar.dns;

import au.id.villar.dns.cache.CachedResourceRecord;
import au.id.villar.dns.cache.DnsCache;
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
import java.util.stream.Collectors;

public class Resolver {

	private static final int DNS_PORT = 53;
	private static final int UDP_DATAGRAM_MAX_SIZE = 512;
	private static final int UDP_RETRY_MILLISECONDS = 1000;

	private AtomicInteger nextId = new AtomicInteger(1);

	private final DnsEngine engine = new DnsEngine();
	private final List<String> dnsRootServers;
	private final DnsCache cache;
	private final boolean useIPv4;
	private final boolean useIPv6;

	private Resolver(DnsCache cache, List<String> dnsRootServers, boolean useIPv4, boolean useIPv6) {
		this.cache = cache != null? cache: createDummyCache();
		this.dnsRootServers = createDnsRootServers(dnsRootServers, useIPv4, useIPv6);
		this.useIPv4 = useIPv4;
		this.useIPv6 = useIPv6;
	}

	public AnswerProcess lookup(String name, DnsType type) {
		Question question = engine.createQuestion(name, type, DnsClass.IN);
		return new AnswerProcess(question);
	}

	@SuppressWarnings("unused")
	public static ResolverBuilder usingIPv4(boolean useIPv4) {
		return new InternalResolverBuilder().usingIPv4(useIPv4);
	}

	@SuppressWarnings("unused")
	public static ResolverBuilder usingIPv6(boolean useIPv6) {
		return new InternalResolverBuilder().usingIPv6(useIPv6);
	}

	@SuppressWarnings("unused")
	public static ResolverBuilder withCache(DnsCache cache) {
		return new InternalResolverBuilder().withCache(cache);
	}

	@SuppressWarnings("unused")
	public static ResolverBuilder withRootServers(List<String> rootServers) {
		return new InternalResolverBuilder().withRootServers(rootServers);
	}

	private short getNextId() {
		short id = (short)nextId.incrementAndGet();
		if (id == 0) id = (short)nextId.incrementAndGet();
		return id;
	}

	private Deque<NameServer> getInitialDnsServers(String name) {
		boolean firstTime = true;
		List<CachedResourceRecord> nsResult;
		List<CachedResourceRecord> ipResult;
		Deque<NameServer> result =
				getShuffledRootServers().stream()
						.map(root -> new NameServer("[ROOT]", root))
						.collect(Collectors.toCollection(LinkedList::new));

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
					addServerAndIps(result, name, ip.getData(String.class));
				}
			}
		}
		return result;
	}

	private void addServerAndIps(Deque<NameServer> list, String name, String... ips) {
		for(NameServer server: list) {
			if(server.name.equals(name)) {
				server.addresses.addAll(Arrays.asList(ips));
				return;
			}
		}
		list.addFirst(new NameServer(name, ips));
	}

	private List<String> getShuffledRootServers() {
		List<String> rootServers = new ArrayList<>(dnsRootServers);
		Collections.shuffle(rootServers);
		return rootServers;
	}

	private List<String> createDnsRootServers(List<String> rootServers, boolean useIPv4, boolean useIPv6) {
		List<String> sanitizedServers = new ArrayList<>(rootServers.size());
		for(String server: rootServers) {
			if(server == null) throw new InternalException("root server address with null value");
			if(Utils.isValidIPv4(server)) {
				if(!useIPv4)
					throw new InternalException(
							server + " is an IPv4 address but this resolver is not configured to use IPv4");
			} else if(Utils.isValidIPv6(server)) {
				if(!useIPv6)
					throw new InternalException(
							server + " is an IPv6 address but this resolver is not configured to use IPv6");
			} else {
				throw new InternalException(server + " is not a valid IPv4 or IPv6 address");
			}
			sanitizedServers.add(server);
		}
		return Collections.unmodifiableList(sanitizedServers);
	}

	private DnsCache createDummyCache() {

		return new DnsCache() {

			@Override
			public void addResourceRecord(ResourceRecord resourceRecord) {
			}

			@Override
			public List<CachedResourceRecord> getResourceRecords(Question question) {
				return Collections.emptyList();
			}

			@Override
			public void removeResourceRecord(DnsItem resourceRecord) {
			}
		};

	}



	private class InternalException extends RuntimeException {
		public InternalException(String message) {
			super(message);
		}
	}



	private enum AnswerProcessStatus {
		START,
		OPENING_TCP, CONNECTING_TCP, SENDING_TCP, RECEIVING_TCP,
		OPENING_UDP, SENDING_UDP, RECEIVING_UDP,
		RESULT
	}



	private static class InternalResolverBuilder implements ResolverBuilder {

		private DnsCache cache;
		private List<String> rootServers;
		private boolean useIPv4 = true;
		private boolean useIPv6;

		private InternalResolverBuilder() {
		}

		@Override
		public Resolver build() {
			return new Resolver(cache, rootServers, useIPv4, useIPv6);
		}

		@Override
		public ResolverBuilder usingIPv4(boolean useIPv4) {
			this.useIPv4 = useIPv4;
			return this;
		}

		@Override
		public ResolverBuilder usingIPv6(boolean useIPv6) {
			this.useIPv6 = useIPv6;
			return this;
		}

		@Override
		public ResolverBuilder withCache(DnsCache cache) {
			this.cache = cache;
			return this;
		}

		@Override
		public ResolverBuilder withRootServers(List<String> rootServers) {
			this.rootServers = rootServers;
			return this;
		}
	}



	private class NameServer {
		String name;
		List<String> addresses = new LinkedList<>();

		public NameServer(String name, String ... addresses) {
			this.name = name;
			this.addresses.addAll(Arrays.asList(addresses));
		}

		@Override
		public String toString() {
			return "NameServer{name='" + name + "', addresses=" + addresses + '}';
		}
	}



	public class AnswerProcess implements Closeable {

		private final String targetName;

		private AnswerProcessStatus processStatus;
		private Deque<NameServer> dnsServers;
		private Set<String> alreadyUsedIps;
		private String currentIp;
		private ByteBuffer message;

		private Selector selector;
		private SocketChannel tcpChannel;
		private DatagramChannel udpChannel;
		private ByteBuffer buffer;
		private long udpTimestamp;

		private AnswerProcess recurringProcess;

		private List<ResourceRecord> result;
		private DnsException exception;

		public AnswerProcess(Question question) {

			this.targetName = question.getDnsName();
			try {
				this.selector = Selector.open();
			} catch (IOException e) {
				this.exception = new DnsException(e);
				return;
			}

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
			this.dnsServers = getInitialDnsServers(question.getDnsName());
			this.alreadyUsedIps = new HashSet<>();
			this.message = createQueryMessage(question);
		}

		public boolean doIO(int timeoutMillis) {
			try {
				boolean finished = internalDoIO(timeoutMillis);
				if(!finished) return false;
			} catch(IOException e) {
				exception = new DnsException(e);
			} catch(DnsException e) {
				exception = e;
			}
			closeResources();
			return true;
		}

		public List<ResourceRecord> getResult() throws DnsException {
			if(exception != null) throw exception;
			return result;
		}

		@Override
		public void close() throws IOException {
			closeResources();
		}

		private void closeResources() {
			close(selector);
			close(tcpChannel);
			close(udpChannel);
		}

		private void close(Closeable channel) {
			try {
				if(channel != null) channel.close();
			} catch(IOException e) {
				if(!thereIsResult()) exception = new DnsException(e);
			}
		}

		private boolean internalDoIO(int timeoutMillis) throws IOException, DnsException {

			while(!thereIsResult()) {

				try {

					switch (processStatus) {

						case START:

							if (!updateDnsServerIps(timeoutMillis)) return false;

							currentIp = pollNextIp();
							if (currentIp == null) {
								result = Collections.emptyList();
								return true;
							}

							processStatus = message.limit() > UDP_DATAGRAM_MAX_SIZE ?
									AnswerProcessStatus.OPENING_TCP :
									AnswerProcessStatus.OPENING_UDP;
							break;

						case OPENING_TCP: case CONNECTING_TCP: case SENDING_TCP: case RECEIVING_TCP:

							if (!doTcpQuery(timeoutMillis)) return false;
							break;

						case OPENING_UDP: case SENDING_UDP: case RECEIVING_UDP:

							if (!doUdpQuery(timeoutMillis)) return false;
							break;

						case RESULT:

							DnsMessage response = engine.createMessageFromBuffer(buffer.array(), 0);

							grabAnswersIfExist(response);

							if (!thereIsResult() && response.isTruncated()) {
								processStatus = AnswerProcessStatus.OPENING_TCP;
								break;
							}

							addExtraInfoToCache(response);
							processStatus = AnswerProcessStatus.START;

					}

				} catch (SocketException e) {

					processStatus = AnswerProcessStatus.START;
					close(tcpChannel);
					close(udpChannel);
					close(recurringProcess);

				}

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
					ResourceRecord authority = response.getAuthority(i);
					cache.addResourceRecord(authority);
					String name = authority.getDnsName();
					if(isSelfOrSuperDomain(name, targetName)) {
						if(authority.getDnsType().equals(DnsType.NS)) {
							serverNames.addFirst(authority.getData(String.class));
						} else if(authority.getDnsType().equals(DnsType.SOA)) {
							serverNames.addFirst(authority.getData(SoaValueConverter.SoaData.class).getDomainName());
						}
					}
				}
				serverNames.forEach(name -> addServerAndIps(dnsServers, name));
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

		private boolean doTcpQuery(int timeoutMillis) throws IOException {
			switch(processStatus) {

				case OPENING_TCP:

					tcpChannel = SocketChannel.open();
					tcpChannel.configureBlocking(false);
					tcpChannel.register(selector, SelectionKey.OP_CONNECT);
					processStatus = AnswerProcessStatus.CONNECTING_TCP;
					tcpChannel.connect(new InetSocketAddress(currentIp, DNS_PORT));

				case CONNECTING_TCP:

					if(selector.select(timeoutMillis) == 0) {
						processStatus = AnswerProcessStatus.CONNECTING_TCP;
						return false;
					}
					tcpChannel.register(selector, SelectionKey.OP_WRITE);
					message.position(0);

				case SENDING_TCP:

					if(!sendDataAndPrepareForReceiving(timeoutMillis, tcpChannel)) {
						processStatus = AnswerProcessStatus.SENDING_TCP;
						return false;
					}

				case RECEIVING_TCP:

					if(selector.select(timeoutMillis) == 0) {
						processStatus = AnswerProcessStatus.RECEIVING_TCP;
						return false;
					}
					receiveTcp();
					tcpChannel.close();
					processStatus = AnswerProcessStatus.RESULT;

			}
			return true;
		}

		private void receiveTcp() throws IOException {
			int received;
			buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE * 2);
			do {
				received = tcpChannel.read(buffer);
				if(received > 0) enlargeBuffer();
			} while(received > 0);
		}

		private void enlargeBuffer() {
			ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + UDP_DATAGRAM_MAX_SIZE);
			buffer.flip();
			newBuffer.put(buffer);
			buffer = newBuffer;
		}

		private boolean doUdpQuery(int timeoutMillis) throws IOException {

			switch(processStatus) {

				case OPENING_UDP:

					udpTimestamp = System.currentTimeMillis();
					udpChannel = DatagramChannel.open();
					udpChannel.configureBlocking(false);
					udpChannel.socket().connect(new InetSocketAddress(currentIp, DNS_PORT));
					message.position(0);
					udpChannel.register(selector, SelectionKey.OP_WRITE);

				case SENDING_UDP:

					if(!sendDataAndPrepareForReceiving(timeoutMillis, udpChannel)) {
						processStatus = AnswerProcessStatus.SENDING_UDP;
						return false;
					}

				case RECEIVING_UDP:

					if(selector.select(timeoutMillis) == 0) {
						if(System.currentTimeMillis() - udpTimestamp > UDP_RETRY_MILLISECONDS) {
							message.position(0);
							udpChannel.register(selector, SelectionKey.OP_WRITE);
							processStatus = AnswerProcessStatus.SENDING_UDP;
							return false;
						}
						processStatus = AnswerProcessStatus.RECEIVING_UDP;
						return false;
					}
					buffer = ByteBuffer.allocate(UDP_DATAGRAM_MAX_SIZE);
					udpChannel.receive(buffer);
					udpChannel.close();
					processStatus = AnswerProcessStatus.RESULT;

			}
			return true;
		}

		private boolean sendDataAndPrepareForReceiving(int timeoutMillis, Channel channel) throws IOException {
			if(selector.select(timeoutMillis) == 0) return false;
			Iterator iterator = selector.selectedKeys().iterator();
			iterator.next();
			iterator.remove();
			((SelectableChannel)channel).register(selector, SelectionKey.OP_READ);
			((WritableByteChannel)channel).write(message);
			return true;
		}

		private boolean thereIsResult() {
			return result != null || exception != null;
		}

		// TODO refactorize this to optimize and make more clear
		private boolean updateDnsServerIps(int timeoutMillis) throws IOException, DnsException {
			if(recurringProcess == null) {
				if(dnsServers.size() == 0 || dnsServers.peekFirst().addresses.size() > 0) return true;
				recurringProcess = Resolver.this.lookup(dnsServers.peekFirst().name, DnsType.ALL); // TODO optimize ALL to bring only A, AAAA and CNAME
			}
			boolean added = false;
			while(recurringProcess != null) {
				if (!recurringProcess.doIO(timeoutMillis)) return false;
				for (ResourceRecord record : recurringProcess.getResult()) {
					if(record.getDnsType() == DnsType.CNAME) {
						addServerAndIps(dnsServers, record.getData(String.class));
						recurringProcess.close();
						recurringProcess = Resolver.this.lookup(dnsServers.peekFirst().name, DnsType.ALL);
						return false;
					}
					if (record.getDnsType() == DnsType.A && useIPv4
							|| record.getDnsType() == DnsType.AAAA && useIPv6) {
						String ip = record.getData(String.class);
						if (!alreadyUsedIps.contains(ip)) {
							dnsServers.peekFirst().addresses.add(ip);
							added = true;
						}
					}
				}
				recurringProcess.close();
				recurringProcess = null;
				if(!added) {
					if(dnsServers.size() == 0) return true;
					recurringProcess = Resolver.this.lookup(dnsServers.peekFirst().name, DnsType.ALL);
				}
			}
			return true;
		}

		private String pollNextIp() {
			NameServer nameServer = dnsServers.peekFirst();
			List<String> ips = nameServer.addresses;
			String ip = ips.remove(ips.size() - 1);
			if(ips.size() == 0) dnsServers.pollFirst();
			if(ip == null) return null;
			alreadyUsedIps.add(ip);
			return ip;
		}

		private ByteBuffer createQueryMessage(Question question) {

			short id = getNextId();
			DnsMessage dnsMessage = engine.createMessage(id, false, Opcode.QUERY, false, false, true, false,
					(byte)0, ResponseCode.NO_ERROR, new Question[] {question}, null, null, null);

			int aproxSize = UDP_DATAGRAM_MAX_SIZE;
			ByteBuffer buffer = null;
			while(buffer == null) {
				try {
					byte[] testingBuffer = new byte[aproxSize];
					int actualSize = dnsMessage.writeToBuffer(testingBuffer, 0);
					buffer = ByteBuffer.allocate(actualSize);
					buffer.put(testingBuffer, 0, actualSize);
					buffer.position(0);
				} catch(IndexOutOfBoundsException e) {
					aproxSize += UDP_DATAGRAM_MAX_SIZE;
				}
			}
			return buffer;
		}

		private boolean isSelfOrSuperDomain(String superDomain, String subDomain) {
			return superDomain.length() <= subDomain.length() && subDomain.endsWith(superDomain);
		}

	}
}
