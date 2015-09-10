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

import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.rx.Streams;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class NettyServerHttpResponse implements ServerHttpResponse {

	private final HttpResponse response;

	private final ChannelHandlerContext ctx;

	private final HttpHeaders headers;

	private boolean headersWritten = false;

	private AtomicBoolean responseWritten = new AtomicBoolean(false);



	public NettyServerHttpResponse(ChannelHandlerContext ctx) {
		Assert.notNull(ctx, "ctx must not be null.");
		this.ctx = ctx;
		this.response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
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

	@Override
	public Publisher<Void> writeWith(Publisher<byte[]> contentPublisher) {
		writeHeaders();
		return new Publisher<Void>() {
			@Override
			public void subscribe(Subscriber<? super Void> subscriber) {
				subscriber.onSubscribe(new Subscription() {
					@Override
					public void request(long l) {
						Streams.wrap(contentPublisher)
								.observeError(Exception.class, (o, ex) -> ex.printStackTrace())
								.observeComplete(z -> {
									if (responseWritten.compareAndSet(false, true)) {
										ctx.writeAndFlush(response);
									}
									ctx.writeAndFlush(new DefaultLastHttpContent());
									ctx.close();
								})
								.consume(bytes -> {
									if (responseWritten.compareAndSet(false, true)) {
										ctx.writeAndFlush(response);
									}
									ctx.writeAndFlush(Unpooled.copiedBuffer(bytes));
								});
					}

					@Override
					public void cancel() {
						response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
						ctx.writeAndFlush(response);
						ctx.writeAndFlush(new DefaultLastHttpContent());
						ctx.close();
					}
				});
			}
		};
	}

	private void writeHeaders() {
		if (!this.headersWritten) {
			for (String name : this.headers.keySet()) {
				for (String value : this.headers.get(name)) {
					this.response.headers().add(name, value);
				}
			}
			this.headersWritten = true;
		}
	}

	public boolean isHeadersWritten() {
		return headersWritten;
	}
}
