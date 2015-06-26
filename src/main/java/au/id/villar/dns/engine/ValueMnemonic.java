package au.id.villar.dns.engine;

abstract class ValueMnemonic {

	private final short value;
	private final String mnemonic;

	public ValueMnemonic(short value, String mnemonic) {
		if(mnemonic == null || mnemonic.length() == 0)
			throw new IllegalArgumentException("mnemonic must not be null or empty");
		if(!mnemonic.equals(mnemonic.trim()))
			throw new IllegalArgumentException("mnemonic must be 'trimmed'");
		this.value = value;
		this.mnemonic = mnemonic;
	}

	public short getValue() {
		return value;
	}

	public String getMnemonic() {
		return mnemonic;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		super.clone();
		throw new CloneNotSupportedException();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValueMnemonic that = (ValueMnemonic) o;
		return value == that.value && mnemonic.equals(that.mnemonic);
	}

	@Override
	public int hashCode() {
		int result = (int) value;
		result = 31 * result + mnemonic.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return mnemonic;
	}
}
