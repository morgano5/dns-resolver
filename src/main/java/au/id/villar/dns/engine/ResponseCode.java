package au.id.villar.dns.engine;

public enum ResponseCode {

	NO_ERROR((byte)        0),
	FORMAT_ERROR((byte)    1),
	SERVER_FAILURE((byte)  2),
	NAME_ERROR((byte)      3),
	NOT_IMPLEMENTED((byte) 4),
	REFUSED((byte)         5);

	private byte rcode;

	ResponseCode(byte rcode) {
		this.rcode = rcode;
	}

	public byte getRcode() {
		return rcode;
	}

	public static ResponseCode getResponseCode(byte rcode) {
		switch(rcode) {
		case 0: return NO_ERROR;
		case 1: return FORMAT_ERROR;
		case 2: return SERVER_FAILURE;
		case 3: return NAME_ERROR;
		case 4: return NOT_IMPLEMENTED;
		case 5: return REFUSED;
		default: throw new RuntimeException("rcode not implemented");
		}
	}

}
