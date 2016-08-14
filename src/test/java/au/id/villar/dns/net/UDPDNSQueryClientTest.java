/*
 * Copyright 2016 Rafael Villar Villar
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
package au.id.villar.dns.net;

import au.id.villar.dns.DNSException;
import au.id.villar.dns.TestUtils;
import au.id.villar.dns.engine.*;

import java.io.IOException;

public class UDPDNSQueryClientTest {

    public static void main(String[] args) throws IOException, DNSException, InterruptedException {
        try(DNSQueryClient client = new UDPDNSQueryClient()) {

            NetTestUtils.query(client, false, (short)15, "id.au", DNSType.ALL, DNSClass.IN,
                    /*"37.209.192.5"*/"8.8.8.8", 53, (r, e) -> System.out.println("\n\n" + (r != null? TestUtils.messageToString(r): e.getMessage()) + "\n\n"));

        }
    }

}
