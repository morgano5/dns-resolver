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

import java.util.Map;

public class UnknownValueConverter implements RRValueConverter {

	@Override
	public Object convertToRawData(Object data) {
		if(!(data instanceof byte[])) throw new IllegalArgumentException("Only byte[] is allowed");
		byte[] byteData = (byte[])data;
		byte[] value = new byte[byteData.length];
		System.arraycopy(byteData, 0, value, 0, byteData.length);
		return value;
	}

	@Override
	public Object getData(byte[] data, int offset, int length, Map<Integer, String> previousNames) {
		byte[] value = new byte[length];
		System.arraycopy(data, offset, value, 0, length);
		return value;
	}

	@Override
	public <T> T convertValue(Object value, Class<T> tClass) {
		if(tClass != byte[].class && tClass != Object.class)
			throw new IllegalArgumentException("Only byte[] is allowed");
		return tClass.cast(((byte[])value).clone());
	}

	@Override
	public int writeRawData(Object value, byte[] array, int offset, int linkOffset, Map<String, Integer> nameLinks) {
		byte[] arrayValue = (byte[])value;
		System.arraycopy(arrayValue, 0, array, offset, arrayValue.length);
		return arrayValue.length;
	}

}
