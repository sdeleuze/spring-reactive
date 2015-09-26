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
package org.springframework.reactive.web.dispatch.handler;

import java.util.concurrent.CompletableFuture;

import org.springframework.reactive.web.dispatch.HandlerAdapter;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.http.HttpHandler;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.reactive.web.http.ServerHttpResponse;


/**
 * Support use of {@link HttpHandler} with
 * {@link org.springframework.reactive.web.dispatch.DispatcherHandler
 * DispatcherHandler} (which implements the same contract).
 * The use of {@code DispatcherHandler} this way enables routing requests to
 * one of many {@code HttpHandler} instances depending on the configured
 * handler mappings.
 *
 * @author Rossen Stoyanchev
 */
public class HttpHandlerAdapter implements HandlerAdapter {


	@Override
	public boolean supports(Object handler) {
		return HttpHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public CompletableFuture<HandlerResult> handle(ServerHttpRequest request, ServerHttpResponse response, Object handler) {
		HttpHandler httpHandler = (HttpHandler) handler;
		CompletableFuture<Void> handling = httpHandler.handle(request, response);
		return handling.thenApply(aVoid -> null);
	}

}
