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

public final class DnsType extends ValueMnemonic {

    public static final short A_VALUE = 1;
    public static final short NS_VALUE = 2;
    public static final short MD_VALUE = 3;
    public static final short MF_VALUE = 4;
    public static final short CNAME_VALUE = 5;
    public static final short SOA_VALUE = 6;
    public static final short MB_VALUE = 7;
    public static final short MG_VALUE = 8;
    public static final short MR_VALUE = 9;
    public static final short NULL_VALUE = 10;
    public static final short WKS_VALUE = 11;
    public static final short PTR_VALUE = 12;
    public static final short HINFO_VALUE = 13;
    public static final short MINFO_VALUE = 14;
    public static final short MX_VALUE = 15;
    public static final short TXT_VALUE = 16;

    public static final short AAAA_VALUE = 28;

    public static final short AXFR_VALUE = 252;
    public static final short MAILB_VALUE = 253;
    public static final short MAILA_VALUE = 254;
    public static final short ALL_VALUE = 255;

    /** 1 a host address */
    public static final DnsType A = new DnsType(A_VALUE, "A");

    /** 2 an authoritative name server */
    public static final DnsType NS = new DnsType(NS_VALUE, "NS");

    /** 3 a mail destination (Obsolete - use MX) */
    public static final DnsType MD = new DnsType(MD_VALUE, "MD");

    /** 4 a mail forwarder (Obsolete - use MX) */
    public static final DnsType MF = new DnsType(MF_VALUE, "MF");

    /** 5 the canonical name for an alias */
    public static final DnsType CNAME = new DnsType(CNAME_VALUE, "CNAME");

    /** 6 marks the start of a zone of authority */
    public static final DnsType SOA = new DnsType(SOA_VALUE, "SOA");

    /** 7 a mailbox domain name (EXPERIMENTAL) */
    public static final DnsType MB = new DnsType(MB_VALUE, "MB");

    /** 8 a mail group member (EXPERIMENTAL) */
    public static final DnsType MG = new DnsType(MG_VALUE, "MG");

    /** 9 a mail rename domain name (EXPERIMENTAL) */
    public static final DnsType MR = new DnsType(MR_VALUE, "MR");

    /** 10 a null RR (EXPERIMENTAL) */
    public static final DnsType NULL = new DnsType(NULL_VALUE, "NULL");

    /** 11 a well known service description */
    public static final DnsType WKS = new DnsType(WKS_VALUE, "WKS");

    /** 12 a domain name pointer */
    public static final DnsType PTR = new DnsType(PTR_VALUE, "PTR");

    /** 13 host information */
    public static final DnsType HINFO = new DnsType(HINFO_VALUE, "HINFO");

    /** 14 mailbox or mail list information */
    public static final DnsType MINFO = new DnsType(MINFO_VALUE, "MINFO");

    /** 15 mail exchange */
    public static final DnsType MX = new DnsType(MX_VALUE, "MX");

    /** 16 text strings */
    public static final DnsType TXT = new DnsType(TXT_VALUE, "TXT");



    /** 28 IPv6 address */
    public static final DnsType AAAA = new DnsType(AAAA_VALUE, "AAAA");



    /** A request for a transfer of an entire zone */
    public static final DnsType AXFR = new DnsType(AXFR_VALUE, "AXFR");

    /** A request for mailbox-related records (MB, MG or MR) */
    public static final DnsType MAILB = new DnsType(MAILB_VALUE, "MAILB");

    /** A request for mail agent RRs (Obsolete - see MX) */
    public static final DnsType MAILA = new DnsType(MAILA_VALUE, "MAILA");

    /** A request for all records */
    public static final DnsType ALL = new DnsType(ALL_VALUE, "*");



    DnsType(short value, String mnemonic) {
        super(value, mnemonic);
    }

}
