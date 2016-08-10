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
package au.id.villar.dns;

import au.id.villar.dns.engine.*;

public class TestUtils {

    private TestUtils() {}

    public static String messageToString(DNSMessage message) {
        StringBuilder builder = new StringBuilder();
        builder.append("DNS MESSAGE\nID:").append(message.getId()).append(", ")
                .append(message.isResponse()? "RESPONSE": "QUERY")
                .append(", OPCODE: ").append(message.getOpcode());
        if(message.isResponse()) {
            builder.append(", ").append(!message.isAuthoritative()? "NOT ": "")
                    .append("AUTHORITATIVE, RECURSION ").append(!message.isRecursionAvailable()? "NOT ": "")
                    .append("AVAILABLE").append(message.isTruncated()? ", TRUNCATED": "");
        } else {
            builder.append(", RECURSION ").append(!message.isRecursionDesired()? "NOT ": "").append("DESIRED");
        }
        builder.append("\nRESPONSE CODE: ").append(message.getResponseCode()).append('\n')
                .append("\nQUESTIONS: ").append(message.getNumQuestions()).append('\n');
        for(int n = 0; n <message.getNumQuestions(); n++) {
            builder.append(questionToString(message.getQuestion(n))).append('\n');
        }
        builder.append("\nANSWERS: ").append(message.getNumAnswers()).append('\n');
        for(int n = 0; n <message.getNumAnswers(); n++) {
            builder.append(resourceRecordToString(message.getAnswer(n))).append('\n');
        }
        builder.append("\nAUTHORITIES: ").append(message.getNumAuthorities()).append('\n');
        for(int n = 0; n <message.getNumAuthorities(); n++) {
            builder.append(resourceRecordToString(message.getAuthority(n))).append('\n');
        }
        builder.append("\nADDITIONALS: ").append(message.getNumAdditionals()).append('\n');
        for(int n = 0; n <message.getNumAdditionals(); n++) {
            builder.append(resourceRecordToString(message.getAdditional(n))).append('\n');
        }

        return builder.toString();
    }

    public static String questionToString(Question question) {
        return "NAME: " + question.getDnsName() + ", TYPE: "
                + question.getDnsType() + ", CLASS: " + question.getDnsClass();
    }

    public static String resourceRecordToString(ResourceRecord rr) {
        return "NAME: " + rr.getDnsName() + ", TYPE: " + rr.getDnsType()
                + ", CLASS: " + rr.getDnsClass() + ", seconds to cache: " + rr.getSecondsCache()
                + ", data: " + rr.getData(Object.class);
    }
}
