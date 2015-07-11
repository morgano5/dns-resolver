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

import au.id.villar.dns.engine.RRValueConverter;

import java.util.Arrays;
import java.util.Map;

public class WksValueConverter implements RRValueConverter {

	@Override
	public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
		byte[] ipv4 = new byte[4];
		int protocol;
		byte[] bitmap = new byte[length - 5];

		System.arraycopy(data, offset, ipv4, 0, 4);
		protocol = data[offset + 4] & 0xFF;
		System.arraycopy(data, offset + 5, bitmap, 0, length - 5);
		return new WksData(ipv4, protocol, bitmap);
	}

	@Override
	public Object convertToRawData(Object data) {
		if(!(data instanceof WksData))
			throw new IllegalArgumentException("Only " + WksData.class.getName() + " is supported");
		return data;
	}

	@Override
	public <T> T convertValue(Object value,Class<T> tClass) {
		if(tClass != WksData.class && tClass != Object.class)
			throw new IllegalArgumentException("Only " + WksData.class.getName() + " is supported");
		return tClass.cast(value);
	}

	@Override
	public int writeRawData(Object objValue, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		WksData value = (WksData)objValue;
		System.arraycopy(value.getIpv4(), 0, array, offset, 4);
		array[offset + 4] = (byte)value.getProtocol();
		System.arraycopy(value.getBitmap(), 0, array, offset + 5, value.getBitmap().length);
		return value.getBitmap().length + 5;
	}

	public static final class WksData {

		private final byte[] ipv4;
		private final int protocol;
		private final byte[] bitmap;

		public WksData(byte[] ipv4, int protocol, byte[] bitmap) {
			this.ipv4 = ipv4.clone();
			this.protocol = protocol;
			this.bitmap = bitmap.clone();
		}

		public byte[] getIpv4() {
			return ipv4.clone();
		}

		public int getProtocol() {
			return protocol;
		}

		public byte[] getBitmap() {
			return bitmap.clone();
		}

		@Override
		public String toString() {
			return "ipv4: " + Arrays.toString(ipv4) + ", protocol: " + protocol
					+ ", bitmap: " + Arrays.toString(bitmap);
		}
	}

}
