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

package org.springframework.reactive.web.http.reactor;

import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;
import reactor.rx.Stream;
import reactor.rx.Streams;

import org.springframework.reactive.io.Bytes;

/**
 * @author Stephane Maldini
 */
public class ReactorServerHttpRequest extends PublisherReactorServerHttpRequest {

	public ReactorServerHttpRequest(HttpChannel<Buffer, ?> request) {
		super(request);
	}

	@Override
	public Stream<Bytes> getBody() {
		return Streams.wrap(super.getBody());
	}

}
