/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.nio.ByteBuffer;

import org.springframework.core.io.support.ReactorBufferBackedBytes;

/**
 * WIP byte buffer interface
 *
 * @author Sebastien Deleuze
 */
public interface Bytes {

	static Bytes create() {
		return new ReactorBufferBackedBytes();
	}

	static Bytes from(ByteBuffer byteBuffer) {
		return new ReactorBufferBackedBytes(byteBuffer);
	}

	static Bytes from(byte[] bytes) {
		return new ReactorBufferBackedBytes(bytes);
	}


	int get();

	void get(byte[] b);

	void get(byte[] dst, int offset, int length);

	byte[] asBytes();

	ByteBuffer asByteBuffer();

	boolean hasRemaining();

	int remaining();

	int limit();

	Bytes append(byte[] bytes);

	Bytes append(Bytes bytes);

	// TODO Remove this horrible flip() method, manage reader + writer indexes to avoid this like in Netty ByteBuf
	void flip();

}
