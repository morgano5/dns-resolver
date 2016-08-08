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
package au.id.villar.dns.net;

import au.id.villar.dns.DnsException;
import au.id.villar.dns.engine.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

public class SingleDNSQueryClient implements DNSQueryClient {

	private enum Status {
		START_QUERY_UDP,
		QUERY_UDP,
		EVAL_UDP,
		START_QUERY_TCP,
		QUERY_TCP,
		RESULT,
		ERROR,
		CLOSED
	}

	private final Selector selector;
	private final UDPDNSQueryClient udpClient;
	private final TCPDNSQueryClient tcpClient;

	private String address;
	private ByteBuffer query;
	private ByteBuffer result;
	private Status status;

	public SingleDNSQueryClient(int dnsPort) throws IOException {
		this.selector = Selector.open();
		this.udpClient = new UDPDNSQueryClient(dnsPort, selector);
		this.tcpClient = new TCPDNSQueryClient(dnsPort, selector);
	}

	@Override
	public boolean startQuery(ByteBuffer question, String address, int timeoutMillis) throws DnsException {

		if(status == Status.CLOSED) throw new DnsException("Already closed");

		this.query = question;
		this.address = address;

		status = (question.position() > UDP_DATAGRAM_MAX_SIZE)? Status.START_QUERY_TCP: Status.START_QUERY_UDP;

		return doIO(timeoutMillis);

	}

	@Override
	public boolean doIO(int timeoutMillis) throws DnsException {
		try {

			switch(status) {
				case CLOSED: throw new DnsException("Already closed");
				case ERROR: throw new DnsException("Invalid state");
				case RESULT: return true;
			}

			return internalDoIO(timeoutMillis);
		} catch(DnsException e) {
			status = Status.ERROR;
			throw e;
		}

	}

	private boolean internalDoIO(int timeoutMillis) throws DnsException {

		switch (status) {

			case START_QUERY_UDP:

				status = Status.QUERY_UDP;
				if (!udpClient.startQuery(query, address, timeoutMillis)) return false;
				status = Status.EVAL_UDP;

			case QUERY_UDP:

				if (status == Status.QUERY_UDP && !udpClient.doIO(timeoutMillis)) return false;

			case EVAL_UDP:

				if (!udpIsTruncated()) {
					status = Status.RESULT;
					result = udpClient.getResult();
					return true;
				}

			case START_QUERY_TCP:

				status = Status.QUERY_TCP;
				if(!tcpClient.startQuery(query, address, timeoutMillis)) return false;
				status = Status.RESULT;
				result = tcpClient.getResult();
				return true;

			case QUERY_TCP:

				if (!tcpClient.doIO(timeoutMillis)) return false;
				status = Status.RESULT;
				result = tcpClient.getResult();

		}

		return true;
	}

	@Override
	public ByteBuffer getResult() {
		return result;
	}

	@Override
	public void close() throws IOException {
		IOException exception = null;
		try { udpClient.close(); } catch (IOException e) { exception = e; }
		try { tcpClient.close(); } catch (IOException e) { if(exception == null) exception = e; }
		try { selector.close(); } catch (IOException e) { if(exception == null) exception = e; }
		if(exception != null) throw exception;
	}

	private boolean udpIsTruncated() {
		return (Utils.getInt(udpClient.getResult().array(), 2, 2) & 0x0200) != 0;
	}
}
