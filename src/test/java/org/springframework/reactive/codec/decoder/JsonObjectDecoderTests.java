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

package org.springframework.reactive.codec.decoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import reactor.rx.Stream;
import reactor.rx.Streams;

import org.springframework.core.codec.support.JsonObjectDecoder;
import org.springframework.core.io.Bytes;

/**
 * @author Sebastien Deleuze
 */
public class JsonObjectDecoderTests {

	@Test
	public void decodeSingleChunkToJsonObject() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Stream<Bytes> source = Streams.just(Bytes.from("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}".getBytes()));
		List<String> results = Streams.wrap(decoder.decode(source, null, null)).map(chunk -> {
					byte[] b = new byte[chunk.remaining()];
					chunk.get(b);
					return new String(b, StandardCharsets.UTF_8);
				}).toList().await();
		assertEquals(1, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
	}

	@Test
	public void decodeMultipleChunksToJsonObject() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Stream<Bytes> source = Streams.just(Bytes.from("{\"foo\": \"foofoo\"".getBytes()), Bytes.from(", \"bar\": \"barbar\"}".getBytes()));
		List<String> results = Streams.wrap(decoder.decode(source, null, null)).map(chunk -> {
					byte[] b = new byte[chunk.remaining()];
					chunk.get(b);
					return new String(b, StandardCharsets.UTF_8);
				}).toList().await();
		assertEquals(1, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
	}

	@Test
	public void decodeSingleChunkToArray() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Stream<Bytes> source = Streams.just(Bytes.from("[{\"foo\": \"foofoo\", \"bar\": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]".getBytes()));
		List<String> results = Streams.wrap(decoder.decode(source, null, null)).map(chunk -> {
					byte[] b = new byte[chunk.remaining()];
					chunk.get(b);
					return new String(b, StandardCharsets.UTF_8);
				}).toList().await();
		assertEquals(2, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
		assertEquals("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}", results.get(1));
	}

	@Test
	public void decodeMultipleChunksToArray() throws InterruptedException {
		JsonObjectDecoder decoder = new JsonObjectDecoder();
		Stream<Bytes> source = Streams.just(Bytes.from("[{\"foo\": \"foofoo\", \"bar\"".getBytes()), Bytes.from(": \"barbar\"},{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}]".getBytes()));
		List<String> results = Streams.wrap(decoder.decode(source, null, null)).map(chunk -> {
					byte[] b = new byte[chunk.remaining()];
					chunk.get(b);
					return new String(b, StandardCharsets.UTF_8);
				}).toList().await();
		assertEquals(2, results.size());
		assertEquals("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}", results.get(0));
		assertEquals("{\"foo\": \"foofoofoo\", \"bar\": \"barbarbar\"}", results.get(1));
	}

}
