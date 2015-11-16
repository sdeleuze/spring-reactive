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

package org.springframework.core.io.support;

import java.nio.ByteBuffer;

import reactor.io.buffer.Buffer;

import org.springframework.core.io.Bytes;

/**
 * @author Sebastien Deleuze
 */
public class ReactorBufferBackedBytes implements Bytes {

	private final Buffer buffer;


	public ReactorBufferBackedBytes() {
		this.buffer = new Buffer();
	}

	public ReactorBufferBackedBytes(ByteBuffer byteBuffer) {
		this.buffer = new Buffer(byteBuffer);
	}

	public ReactorBufferBackedBytes(byte[] bytes) {
		this.buffer = Buffer.wrap(bytes);
	}


	@Override
	public int get() {
		return this.buffer.byteBuffer().get();
	}

	@Override
	public void get(byte[] dst, int offset, int length) {
		this.buffer.byteBuffer().get(dst, offset, length);
	}

	@Override
	public void get(byte[] b) {
		this.buffer.byteBuffer().get(b);
	}

	@Override
	public byte[] asBytes() {
		return this.buffer.asBytes();
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return this.buffer.byteBuffer();
	}

	@Override
	public boolean hasRemaining() {
		return this.buffer.byteBuffer().hasRemaining();
	}

	@Override
	public int remaining() {
		return this.buffer.byteBuffer().remaining();
	}

	@Override
	public int limit() {
		return this.buffer.limit();
	}

	@Override
	public Bytes append(byte[] bytes) {
		this.buffer.append(bytes);
		return this;
	}

	@Override
	public Bytes append(Bytes bytes) {
		this.append(bytes.asBytes());
		return this;
	}

	@Override
	public void flip() {
		this.buffer.flip();
	}

}
