package au.id.villar.dns.net;

import au.id.villar.dns.DNSException;
import au.id.villar.dns.engine.DNSClass;
import au.id.villar.dns.engine.DNSEngine;
import au.id.villar.dns.engine.DNSMessage;
import au.id.villar.dns.engine.DNSType;
import au.id.villar.dns.engine.Opcode;
import au.id.villar.dns.engine.Question;
import au.id.villar.dns.engine.ResponseCode;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SingleDNSQueryClientTest {

    private static final int DNS_PORT = 53;

    private static ByteBuffer createQueryMessage(Question question) {

        short id = 15;
        DNSEngine engine = new DNSEngine();
        DNSMessage dnsMessage = engine.createMessage(id, false, Opcode.QUERY, false, false, true, false,
                (byte)0, ResponseCode.NO_ERROR, new Question[] {question}, null, null, null);
        return engine.createBufferFromMessage(dnsMessage);
    }

    public static void main(String[] args) throws IOException, DNSException {
        SingleDNSQueryClient client = new SingleDNSQueryClient(DNS_PORT);
        if(client.startQuery(createQueryMessage(new DNSEngine().createQuestion("id.au", DNSType.ALL, DNSClass.IN)), "8.8.8.8"/*"216.69.185.14"*/, 10000)) {
            DNSMessage message = new DNSEngine().createMessageFromBuffer(client.getResult().array(), 0);
            for(int x = 0; x < message.getNumAnswers(); x++) {
                System.out.println("ANSWER: " + message.getAnswer(x).getDnsType() + ' ' + message.getAnswer(x).getData(Object.class));
            }
            for(int x = 0; x < message.getNumAuthorities(); x++) {
                System.out.println("AUTHORITY: " + message.getAnswer(x).getDnsType() + ' ' + message.getAuthority(x).getData(Object.class));
            }
            for(int x = 0; x < message.getNumAdditionals(); x++) {
                System.out.println("ADDITIONAL: " + message.getAnswer(x).getDnsType() + ' ' + message.getAdditional(x).getData(Object.class));
            }
        } else {
            System.out.println("necesita mas tiempo");
        }
    }

}
