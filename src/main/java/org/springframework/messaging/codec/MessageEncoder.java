/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.codec;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;

/**
 * TODO
 *
 * @author Sebastien Deleuze
 */
public interface MessageEncoder {

	/**
	 * TODO
	 *
	 * @param payload the payload to encode
	 * @param type the stream element type to process.
	 * @param hints Additional information about how to do encode, optional.
	 * @return will produce ONE message in most case, but it seems interesting to keep the possibility to produce many
	 */
	Flux<Message<?>> encode(Publisher<?> payload, ResolvableType type, Object... hints);

}
