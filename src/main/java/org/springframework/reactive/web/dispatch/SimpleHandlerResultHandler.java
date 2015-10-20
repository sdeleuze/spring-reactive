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

package org.springframework.reactive.web.dispatch;

import java.util.Arrays;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.core.Ordered;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.reactive.web.http.ServerHttpResponse;

/**
 * Supports {@link HandlerResult} with a {@code Publisher<Void>} value.
 *
 * @author Sebastien Deleuze
 */
public class SimpleHandlerResultHandler implements Ordered, HandlerResultHandler {

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public boolean supports(HandlerResult result) {
		Object value = result.getValue();
		return value != null && Publisher.class.isAssignableFrom(value.getClass());
	}

	@Override
	public Publisher<Void> handleResult(ServerHttpRequest request, ServerHttpResponse response, HandlerResult result) {
		Publisher<Void> handleComplete = Publishers.completable((Publisher<?>)result.getValue());
		return Publishers.concat(Publishers.from(Arrays.asList(handleComplete, response.writeHeaders())));
	}

}
