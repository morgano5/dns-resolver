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

import java.util.HashMap;
import java.util.Map;


public class DNSMessage {

    public static final int MESSAGE_HEADER_SIZE = 12;

    /* Fields for header message [RFC1035] */
    private final short id;         // message ID
    private final boolean qr;       // query ( 0 or false) o response (1 or true)
    private final Opcode opcode;    // kind of query
    private final boolean aa;
    private final boolean tc;
    private final boolean rd;
    private final boolean ra;
    private final byte z;
    private final ResponseCode rcode; // response code

    private final Question[] questions;
    private final ResourceRecord[] answers;
    private final ResourceRecord[] authorities;
    private final ResourceRecord[] additionals;

    DNSMessage(short id, boolean isResponse, Opcode opcode, boolean isAuthoritatve,
            boolean wasTruncated, boolean recursionDesired, boolean recursionAvailable,
            byte reserved, ResponseCode rcode, Question[] questions, ResourceRecord[] answers,
            ResourceRecord[] authorities, ResourceRecord[] additionals) {
        this.id = id;
        this.qr = isResponse;
        this.opcode = opcode;
        this.aa = isAuthoritatve;
        this.tc = wasTruncated;
        this.rd = recursionDesired;
        this.ra = recursionAvailable;
        this.z = reserved;
        this.rcode = rcode;
        this.questions = questions != null? questions.clone(): new Question[0];
        this.answers = answers != null? answers.clone(): new ResourceRecord[0];
        this.authorities = authorities != null? authorities.clone(): new ResourceRecord[0];
        this.additionals = additionals  != null? additionals.clone(): new ResourceRecord[0];
    }

    public short getId() {
        return id;
    }

    public boolean isResponse() {
        return qr;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public boolean isAuthoritative() {
        return aa;
    }

    public boolean isTruncated() {
        return tc;
    }

    public boolean isRecursionDesired() {
        return rd;
    }

    public boolean isRecursionAvailable() {
        return ra;
    }

    public byte getZ() {
        return z;
    }

    public ResponseCode getResponseCode() {
        return rcode;
    }

    public int getNumQuestions() {
        return questions.length;
    }

    public int getNumAnswers() {
        return answers.length;
    }

    public int getNumAuthorities() {
        return authorities.length;
    }

    public int getNumAdditionals() {
        return additionals.length;
    }

    public Question getQuestion(int queryNum) {
        return questions[queryNum];
    }

    public ResourceRecord getAnswer(int answerNum) {
        return answers[answerNum];
    }

    public ResourceRecord getAuthority(int nameserverNum) {
        return authorities[nameserverNum];
    }

    public ResourceRecord getAdditional(int additionalNum) {
        return additionals[additionalNum];
    }

    public int writeToBuffer(byte[] buffer, int offset) {
        short aux;
        int usedBytes;
        int start = offset;
        Map<String, Integer> nameLinks = new HashMap<>();

        Utils.writeShort(id, buffer, offset++);
        aux = (short) ((qr ? 1 : 0) << 15);
        aux |= (short) (opcode.getOpcode() << 11);
        aux |= (short) ((aa ? 1 : 0) << 10);
        aux |= (short) ((tc ? 1 : 0) << 9);
        aux |= (short) ((rd ? 1 : 0) << 8);
        aux |= (short) ((ra ? 1 : 0) << 7);
        aux |= (short) (z << 4);
        aux |= rcode.getRcode();
        Utils.writeShort(aux, buffer, ++offset);
        offset += 2;
        Utils.writeShort((short) questions.length, buffer, offset);
        offset += 2;
        Utils.writeShort((short) answers.length, buffer, offset);
        offset += 2;
        Utils.writeShort((short) authorities.length, buffer, offset);
        offset += 2;
        Utils.writeShort((short) additionals.length, buffer, offset);
        offset += 2;
        for (Question q : questions) {
            usedBytes = q.writeRawData(buffer, offset, offset - start, nameLinks);
            offset += usedBytes;
        }
        for (ResourceRecord rr : answers) {
            usedBytes = rr.writeRawData(buffer, offset, offset - start, nameLinks);
            offset += usedBytes;
        }
        for (ResourceRecord rr : authorities) {
            usedBytes = rr.writeRawData(buffer, offset, offset - start, nameLinks);
            offset += usedBytes;
        }
        for (ResourceRecord rr : additionals) {
            usedBytes = rr.writeRawData(buffer, offset, offset - start, nameLinks);
            offset += usedBytes;
        }
        return offset;
    }
}
