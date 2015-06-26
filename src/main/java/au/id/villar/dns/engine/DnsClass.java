package au.id.villar.dns.engine;

public final class DnsClass extends ValueMnemonic {

	public static final short IN_VALUE = 1;
	public static final short ANY_VALUE = 255;

	/**  1 the Internet */
	public static final DnsClass IN = new DnsClass(IN_VALUE, "IN");

	/** 255 any class */
	public static final DnsClass ANY= new DnsClass(ANY_VALUE, "*");

	DnsClass(short value, String mnemonic) {
		super(value, mnemonic);
	}

}
