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

		Resolver.AnswerProcess process = resolver.lookup("gmail.com", DnsType.MX);

		try(Selector selector = Selector.open()) {
			process.useSelector(selector);

			Set<SelectionKey> keys;

			boolean result = process.doIO();
			while (!result) {
				int selectedChannels;
				do {
					selectedChannels = selector.select();
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
