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

import java.nio.ByteBuffer;
import java.util.List;

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.reactive.codec.decoder.Decoder;
import org.springframework.util.MimeType;

/**
 * Encode from a stream of {@code T} to a bytes stream.
 *
 * @author Sebastien Deleuze
 * @see Decoder
 */
public interface Encoder<T> {

	/**
	 * Indicate whether the given type and mime type can be processed by this encoder.
	 * @param type the stream element type to process.
	 * @param mimeType the mime type to process.
	 * @param hints Additional information about how to do decode, optional.
	 * @return {@code true} if can process; {@code false} otherwise
	 */
	boolean canEncode(ResolvableType type, MimeType mimeType, Object... hints);

	/**
	 * Encode an input stream of {@code T} to an output {@link ByteBuffer} stream.
	 * @param inputStream the input stream to process.
	 * @param type the stream element type to process.
	 * @param mimeType the mime type to process.
	 * @param hints Additional information about how to do decode, optional.
	 * @return the output stream
	 */
	Publisher<ByteBuffer> encode(Publisher<? extends T> inputStream, ResolvableType type, MimeType mimeType, Object... hints);

	/**
	 * Return the list of {@link MimeType} objects supported by this codec.
	 * @return the list of supported mime types
	 */
	List<MimeType> getSupportedMimeTypes();

}
