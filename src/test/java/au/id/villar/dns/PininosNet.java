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

import au.id.villar.dns.engine.*;

import java.io.*;import java.net.*;


public class PininosNet {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		DatagramSocket dSocket = new DatagramSocket();
		InetSocketAddress dnsServerSocket = new InetSocketAddress(/*"127.0.1.1"*//*"8.8.8.8"*/"198.41.0.4"/*"216.239.34.10"*/, 53);
		byte[] rawData = {0x41, 0x60, 0x1, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x5, 0x73,
				0x74, 0x61, 0x72, 0x74, 0x6, 0x75, 0x62, 0x75, 0x6e, 0x74, 0x75, 0x3, 0x63, 0x6f,
				0x6d, 0x0, 0x0, 0x1, 0x0, 0x1};
		byte[] rawDataReceived = new byte[2048];

////aqeunomamess.coma //浙江大学.cn    nonasdasdqweer.mx
//DnsMessage m = DnsMessage.createMsgQuery((short)999, "nonasdasdqweer.mx", DnsType.getType("A"), DnsClass.IN);
//rawData = new byte[m.getRawSize()];
//m.writeRawData(rawData, 0);

		DnsEngine dnsEngine = new DnsEngine();

		DnsMessage m = dnsEngine.createMessage((short) 999, false, Opcode.QUERY, false, false, true, false, (byte)0,
				ResponseCode.NO_ERROR, new Question[] {
//						dnsEngine.createQuestion("google.com", DnsType.AAAA, DnsClass.IN),
//						dnsEngine.createQuestion("yahoo.com", DnsType.A, DnsClass.IN),//"179.105.0.101.in-addr.arpa"
						dnsEngine.createQuestion("villar.me", DnsType.A, DnsClass.IN),
				},
				new ResourceRecord[0], new ResourceRecord[0], new ResourceRecord[0]);

		rawData = new byte[2048];
		int usedBytes = m.writeToBuffer(rawData, 0);



		DatagramPacket datagram = new DatagramPacket(rawData, 0, usedBytes);
		DatagramPacket receivingDatagram = new DatagramPacket(rawDataReceived, rawDataReceived.length);

		int index;
		int printLength = 32;
		int mod = 0;

		for(index = 0; index < usedBytes; index++) {
			System.out.format(" %2x ", rawData[index]);
			if(++mod % printLength == 0) System.out.format("%n");
		} System.out.println("\n\n"); mod = 0;

System.out.println("1");
		dSocket.connect(dnsServerSocket);
		dSocket.send(datagram);
		dSocket.receive(receivingDatagram);
System.out.println("2");

		byte[] data = receivingDatagram.getData();

		for(index = 0; index < receivingDatagram.getLength(); index++) {
			System.out.format(" %2x ", data[index]);
			if(++mod % printLength == 0) System.out.format("%n");
		} System.out.println("\n\n");

		DnsMessage message = dnsEngine.createMessageFromBuffer(data, 0);

		System.out.println("\n>>" + message.isResponse()
				+ "\n>>" + message.getOpcode()
				+ "\n>>" + message.getResponseCode()
				+ "\n>>Questions: " + message.getNumQuestions()
				+ "\n>>Answers: " + message.getNumAnswers()
				+ "\n>>Nameservers: " + message.getNumAuthorities()
				+ "\n>>Additionals: " + message.getNumAdditionals());
		for(int i = 0; i < message.getNumQuestions(); i++) {
			System.out.println("\nQUESTION -------------------------");
			System.out.println("QNAME: " + message.getQuestion(i).getDnsName());
			System.out.println("QTYPE: " + message.getQuestion(i).getDnsType());
			System.out.println("QCLASS: " + message.getQuestion(i).getDnsClass());
		}
		for(int i = 0; i < message.getNumAnswers(); i++) {
			System.out.println("\nANSWER -------------------------");
			System.out.println("NAME: " + message.getAnswer(i).getDnsName());
			System.out.println("TYPE: " + message.getAnswer(i).getDnsType());
			System.out.println("CLASS: " + message.getAnswer(i).getDnsClass());
			System.out.println("TTL: " + Utils.ttlToString(message.getAnswer(i).getSecondsCache()));
			System.out.println("DATA: " + message.getAnswer(i).getData(Object.class));
		}
		for(int i = 0; i < message.getNumAuthorities(); i++) {
			System.out.println("\nNAMESERVER -------------------------");
			System.out.println("NAME: " + message.getAuthority(i).getDnsName());
			System.out.println("TYPE: " + message.getAuthority(i).getDnsType());
			System.out.println("CLASS: " + message.getAuthority(i).getDnsClass());
			System.out.println("TTL: " + Utils.ttlToString(message.getAuthority(i).getSecondsCache()));
			System.out.println("DATA: " + message.getAuthority(i).getData(Object.class));
		}
		for(int i = 0; i < message.getNumAdditionals(); i++) {
			System.out.println("\nADDITIONAL -------------------------");
			System.out.println("NAME: " + message.getAdditional(i).getDnsName());
			System.out.println("TYPE: " + message.getAdditional(i).getDnsType());
			System.out.println("CLASS: " + message.getAdditional(i).getDnsClass());
			System.out.println("TTL: " + Utils.ttlToString( message.getAdditional(i).getSecondsCache()));
			System.out.println("DATA: " + message.getAdditional(i).getData(Object.class));
		}
	}


}
//2a00:1450:400c:c01::8b
//2a00:1450:4001:80b::100e
//2404:6800:4004:80b::1005
