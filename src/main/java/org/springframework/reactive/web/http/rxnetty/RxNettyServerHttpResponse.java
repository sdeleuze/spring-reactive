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
package org.springframework.reactive.web.http.rxnetty;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.util.Assert;
import reactor.io.buffer.Buffer;
import rx.Observable;
import rx.RxReactiveStreams;

import java.nio.ByteBuffer;

/**
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class RxNettyServerHttpResponse implements ServerHttpResponse {

	private final HttpServerResponse<?> response;

	private final HttpHeaders headers;

	private boolean headersWritten = false;


	public RxNettyServerHttpResponse(HttpServerResponse<?> response) {
		Assert.notNull("'response', response must not be null.");
		this.response = response;
		this.headers = new HttpHeaders();
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		this.response.setStatus(HttpResponseStatus.valueOf(status.value()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	public Observable<Void> writeWith(Observable<ByteBuffer> contentPublisher) {
		return this.response.writeBytes(contentPublisher.map(content -> new Buffer(content).asBytes()));
	}

	@Override
	public Publisher<Void> writeWith(Publisher<ByteBuffer> contentPublisher) {
		writeHeaders();
		Observable<byte[]> contentObservable = RxReactiveStreams.toObservable(contentPublisher).map(content -> new Buffer(content).asBytes());
		return RxReactiveStreams.toPublisher(this.response.writeBytes(contentObservable));
	}

	private void writeHeaders() {
		if (!this.headersWritten) {
			for (String name : this.headers.keySet()) {
				for (String value : this.headers.get(name)) {
					this.response.addHeader(name, value);
				}
			}
		}
	}
}
