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

package org.springframework.core.codec.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Decoder;
import org.springframework.util.BytesInputStream;
import org.springframework.core.io.Bytes;
import org.springframework.util.MimeType;


/**
 * Decode from a bytes stream of JSON objects to a stream of {@code Object} (POJO).
 *
 * @author Sebastien Deleuze
 * @see JacksonJsonEncoder
 */
public class JacksonJsonDecoder extends AbstractDecoder<Object> {

	private final ObjectMapper mapper;

	private Decoder<Bytes> preProcessor;


	public JacksonJsonDecoder() {
		this(new ObjectMapper(), null);
	}

	public JacksonJsonDecoder(Decoder<Bytes> preProcessor) {
		this(new ObjectMapper(), preProcessor);
	}

	public JacksonJsonDecoder(ObjectMapper mapper, Decoder<Bytes> preProcessor) {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
		this.mapper = mapper;
		this.preProcessor = preProcessor;
	}


	@Override
	public Publisher<Object> decode(Publisher<Bytes> inputStream, ResolvableType type,
			MimeType mimeType, Object... hints) {

		ObjectReader reader = this.mapper.readerFor(type.getRawClass());

		if (this.preProcessor != null) {
			inputStream = this.preProcessor.decode(inputStream, type, mimeType, hints);
		}

		return Publishers.map(inputStream, content -> {
			try {
				return reader.readValue(new BytesInputStream(content));
			}
			catch (IOException e) {
				throw new CodecException("Error while reading the data", e);
			}
		});
	}

}
