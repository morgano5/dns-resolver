import au.id.villar.dns.DnsException;
import au.id.villar.dns.Resolver;
import au.id.villar.dns.engine.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Test {

	public static void main(String[] args) {

		Resolver resolver = new Resolver();

		Resolver.AnswerProcess process = resolver.lookup("villar.id.au", DnsType.MX);

			boolean done = process.doIO(100);
			while (!done) {
				done = process.doIO(100);
			}

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
