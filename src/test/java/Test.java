import au.id.villar.dns.DnsException;
import au.id.villar.dns.Resolver;
import au.id.villar.dns.engine.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class Test {

	public static void main(String[] args) {
		Path rootDir = Paths.get("H:\\stuff\\03_code_and_projects\\03_java\\dns_code\\dns-resolver");
		processNode(rootDir);
	}

	private static void processNode(Path node) {
		try {
			if (Files.isDirectory(node)) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(node)) {
					for (Path child : stream) processNode(child);
				}
			} else {
				try (InputStream stream = Files.newInputStream(node)) {
					boolean previousWasCR = false;
					int ch;
					while((ch = stream.read()) != -1) {
						if(previousWasCR && ch == 10) {
							System.out.format("HIT: %s%n", node);
							break;
						}
						previousWasCR = ch == 13;
					}
				}
			}
		} catch (IOException e) {
			System.out.format("ERROR processing %s: %s%n", node, e.getMessage());
		}
	}


	public static void main2(String[] args) {

		Resolver resolver = new Resolver();

		Resolver.AnswerProcess process = resolver.lookup("eluniversal.mx", DnsType.ALL);

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
