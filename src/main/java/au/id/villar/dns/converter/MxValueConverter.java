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
package au.id.villar.dns.converter;

import au.id.villar.dns.engine.ParseResult;
import au.id.villar.dns.engine.RRValueConverter;
import au.id.villar.dns.engine.Utils;

import java.util.Map;

public class MxValueConverter implements RRValueConverter {

	@Override
	public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
		int preference;
		String mailServer;
		ParseResult<String> result;

		preference = Utils.getInt(data, offset, 2);
		offset += 2;
		result = Utils.getDomainName(data, offset, previousNames);
		mailServer = result.value;
		return new MxData(preference, mailServer);
	}

	@Override
	public Object convertToRawData(Object data) {
		if(!(data instanceof MxData))
			throw new IllegalArgumentException("Only " + MxData.class.getName() + " is supported");
		return data;
	}

	@Override
	public <T> T convertValue(Object value, Class<T> tClass) {
		if(tClass != MxData.class && tClass != Object.class)
			throw new IllegalArgumentException("Only " + MxData.class.getName() + " is supported");
		return tClass.cast(value);
	}

	@Override
	public int writeRawData(Object objValue, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		int start = offset;
		int usedBytes;
		MxData value = (MxData)objValue;

		Utils.writeShort((short)value.getPreference(), array, offset);
		offset += 2;
		usedBytes = Utils.writeDomainNameAndUpdateLinks(value.getMailServer(), array, offset, linkOffset,
				nameLinks);
		offset += usedBytes;
		return offset - start;
	}

	public static final class MxData {

		private final int preference;
		private final String mailServer;

		public MxData(int preference, String mailServer) {
			this.preference = preference;
			this.mailServer = mailServer;
		}

		public int getPreference() {
			return preference;
		}

		public String getMailServer() {
			return mailServer;
		}


		@Override
		public String toString() {
			return "preference: " + preference + ", mail server: " + mailServer;
		}
	}

}
