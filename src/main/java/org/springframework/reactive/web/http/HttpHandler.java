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

package org.springframework.reactive.web.http;

import java.util.concurrent.CompletableFuture;


/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public interface HttpHandler {

	/**
	 * Process the given HTTP request, generating a response.
	 *
	 * <p>Implementations should not throw exception but rather returns a future that
	 * completes exceptionally.
	 *
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return a {@link CompletableFuture} that completes (as a success or an Exception
	 * when the request processing is finished.
	 */
	CompletableFuture<Void> handle(ServerHttpRequest request, ServerHttpResponse response);

}
