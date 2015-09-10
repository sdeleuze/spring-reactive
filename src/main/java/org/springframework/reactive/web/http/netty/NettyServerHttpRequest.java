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
package org.springframework.reactive.web.http.netty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpRequest;
import org.reactivestreams.Publisher;
import reactor.rx.Stream;
import reactor.rx.Streams;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class NettyServerHttpRequest implements ServerHttpRequest {

	private final HttpRequest request;

	private final Stream<ByteBuf> content;

	private HttpHeaders headers;


	public NettyServerHttpRequest(HttpRequest request, Publisher<ByteBuf> content) {
		Assert.notNull("'request', request must not be null.");
		this.request = request;
		this.content = Streams.wrap(content);
	}


	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (Map.Entry<String, String> header : this.request.headers()) {
				this.headers.add(header.getKey(), header.getValue());
			}
		}
		return this.headers;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.request.method().name());
	}

	@Override
	public URI getURI() {
		try {
			return new URI(this.request.uri());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
		}

	}

	@Override
	public Publisher<byte[]> getBody() {
		return this.content.map(byteBuf -> {
			byte[] copy = new byte[byteBuf.readableBytes()];
			byteBuf.readBytes(copy);
			return copy;
		});
	}

}
