package au.id.villar.dns;

public class DnsException extends Exception {

	public DnsException() {
	}

	public DnsException(String message) {
		super(message);
	}

	public DnsException(String message, Throwable cause) {
		super(message, cause);
	}

	public DnsException(Throwable cause) {
		super(cause);
	}

}
