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

import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import org.springframework.reactive.util.CompletableFutureUtils;
import org.springframework.reactive.web.http.HttpHandler;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class RequestHandlerAdapter implements RequestHandler<ByteBuf, ByteBuf> {

	private final HttpHandler httpHandler;


	public RequestHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required.");
		this.httpHandler = httpHandler;
	}

	@Override
	public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
		RxNettyServerHttpRequest adaptedRequest = new RxNettyServerHttpRequest(request);
		RxNettyServerHttpResponse adaptedResponse = new RxNettyServerHttpResponse(response);
		CompletableFuture<Void> result = this.httpHandler.handle(adaptedRequest, adaptedResponse);
		return CompletableFutureUtils.toObservable(result);
	}

}
