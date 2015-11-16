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

package org.springframework.reactive.codec.encoder;

import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Stream;
import reactor.rx.Streams;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.BytesEncoder;
import org.springframework.http.MediaType;
import org.springframework.core.io.Bytes;

/**
 * @author Sebastien Deleuze
 */
public class ByteBufferEncoderTests {

	private final BytesEncoder encoder = new BytesEncoder();

	@Test
	public void canDecode() {
		assertTrue(encoder.canEncode(ResolvableType.forClass(Bytes.class), MediaType.TEXT_PLAIN));
		assertFalse(encoder.canEncode(ResolvableType.forClass(Integer.class), MediaType.TEXT_PLAIN));
		assertTrue(encoder.canEncode(ResolvableType.forClass(Bytes.class), MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() throws InterruptedException {
		Bytes fooBuffer = Bytes.from("foo".getBytes());
		Bytes barBuffer = Bytes.from("bar".getBytes());
		Stream<Bytes> source = Streams.just(fooBuffer, barBuffer);
		List<Bytes> results = Streams.wrap(encoder.encode(source,
				ResolvableType.forClassWithGenerics(Publisher.class, Bytes.class), null)).toList().await();
		assertEquals(2, results.size());
		assertEquals(fooBuffer, results.get(0));
		assertEquals(barBuffer, results.get(1));
	}

}
