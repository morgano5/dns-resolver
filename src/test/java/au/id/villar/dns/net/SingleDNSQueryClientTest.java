package au.id.villar.dns.net;

import au.id.villar.dns.DnsException;
import au.id.villar.dns.converter.SoaValueConverter;
import au.id.villar.dns.engine.DnsClass;
import au.id.villar.dns.engine.DnsEngine;
import au.id.villar.dns.engine.DnsMessage;
import au.id.villar.dns.engine.DnsType;
import au.id.villar.dns.engine.Opcode;
import au.id.villar.dns.engine.Question;
import au.id.villar.dns.engine.ResponseCode;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SingleDNSQueryClientTest {

    private static final int UDP_DATAGRAM_MAX_SIZE = 512;
    private static final int DNS_PORT = 53;

    private static ByteBuffer createQueryMessage(Question question) {

        short id = 15;
        DnsMessage dnsMessage = new DnsEngine().createMessage(id, false, Opcode.QUERY, false, false, true, false,
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

    public static void main(String[] args) throws IOException, DnsException {
        SingleDNSQueryClient client = new SingleDNSQueryClient(DNS_PORT);
        if(client.startQuery(createQueryMessage(new DnsEngine().createQuestion("villar.me", DnsType.A, DnsClass.IN)), "8.8.8.8", 10000)) {
            DnsMessage message = new DnsEngine().createMessageFromBuffer(client.getResult().array(), 0);
            for(int x = 0; x < message.getNumAnswers(); x++) {
                System.out.println("ANSWER: " + message.getAnswer(x).getData(String.class));
            }
            for(int x = 0; x < message.getNumAuthorities(); x++) {
                System.out.println("AUTHORITY: " + message.getAuthority(x).getData(SoaValueConverter.SoaData.class));
            }
            for(int x = 0; x < message.getNumAdditionals(); x++) {
                System.out.println("AUTHORITY: " + message.getAdditional(x).getData(String.class));
            }
        } else {
            System.out.println("necesita mas tiempo");
        }
    }

}
