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

		try(Selector selector = Selector.open()) {
			process.useSelector(selector);

			Set<SelectionKey> keys;

			boolean result = process.doIO();
			MAIN: while (!result) {
				int selectedChannels;
				do {
					selectedChannels = selector.select(1000);
					if(selectedChannels == 0) {
						if(process.doIO()) break MAIN;
					}
				} while (selectedChannels == 0);
				keys = selector.selectedKeys();
				result = process.doIO();
				Iterator iterator = keys.iterator();
				iterator.next();
				iterator.remove();
			}

		} catch(IOException e) {
			e.printStackTrace();
			return;
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
