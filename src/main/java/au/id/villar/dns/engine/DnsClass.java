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
