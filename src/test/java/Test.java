import au.id.villar.dns.DnsException;
import au.id.villar.dns.Resolver;
import au.id.villar.dns.engine.*;

import java.util.Collections;
import java.util.List;

public class Test {

	public static void main(String[] args) {

		Resolver resolver = new Resolver();

		Resolver.AnswerProcess process = resolver.lookup("villar.me", DnsType.ALL);

		int timeout = 100;
		boolean done;
		do {

			done = process.doIO(timeout);

			// do something else...

		} while (!done);

		List<ResourceRecord> result = Collections.emptyList();
		try {
			result = process.getResult();
		} catch (DnsException e) {
			e.printStackTrace();
		}

		for(ResourceRecord rr: result) {
			System.out.println("Result: " + rr.getDnsName() + " - " + rr.getDnsType() + " - " + rr.getData(Object.class));
		}

	}

}
