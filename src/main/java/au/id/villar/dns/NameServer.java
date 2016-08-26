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
package au.id.villar.dns;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Deprecated // apparently not in use
class NameServer {

    private final String name;
    private LinkedList<String> addresses = new LinkedList<>();

    public NameServer(String name, String... addresses) {
        this.name = name;
        this.addresses.addAll(Arrays.asList(addresses));
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public List<String> getAddresses() {
        return addresses;
    }

    public void addIp(String ip) {
        addresses.add(ip);
    }

    public String pollIp() {
        return addresses.pollFirst();
    }

    @Override
    public String toString() {
        return "NameServer{name='" + name + "', addresses=" + addresses + '}';
    }
}
