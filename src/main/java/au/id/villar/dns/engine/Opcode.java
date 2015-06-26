package au.id.villar.dns.engine;

/** Kind of DNS query */
public enum Opcode {

	/** Query */
	QUERY((byte)         0),

	/** Inverse Query */
	IQUERY((byte)        1),

	/** Server status request */
	STATUS((byte)        2),

	RESERVED_3((byte)    3),
	RESERVED_4((byte)    4),
	RESERVED_5((byte)    5),
	RESERVED_6((byte)    6),
	RESERVED_7((byte)    7),
	RESERVED_8((byte)    8),
	RESERVED_9((byte)    9),
	RESERVED_10((byte)  10),
	RESERVED_11((byte)  11),
	RESERVED_12((byte)  12),
	RESERVED_13((byte)  13),
	RESERVED_14((byte)  14),
	RESERVED_15((byte)  15);

	private byte opcode;

	Opcode(byte opcode) {
		this.opcode = opcode;
	}

	public byte getOpcode() {
		return opcode;
	}

	public static Opcode getOpcode(byte opcode) {
		switch(opcode) {
		case 0: return QUERY;
		case 1: return IQUERY;
		case 2: return STATUS;
		case 3: return RESERVED_3;
		case 4: return RESERVED_4;
		case 5: return RESERVED_5;
		case 6: return RESERVED_6;
		case 7: return RESERVED_7;
		case 8: return RESERVED_8;
		case 9: return RESERVED_9;
		case 10: return RESERVED_10;
		case 11: return RESERVED_11;
		case 12: return RESERVED_12;
		case 13: return RESERVED_13;
		case 14: return RESERVED_14;
		case 15: return RESERVED_15;
		default: throw new RuntimeException("opcode not valid");
		}
	}

}
