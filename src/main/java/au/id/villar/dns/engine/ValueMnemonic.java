/*
 * Copyright 2015 Rafael Villar Villar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
