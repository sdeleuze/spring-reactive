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

package org.springframework.reactive.web.http.servlet;

import java.io.IOException;
import java.util.function.BiConsumer;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatus;
import org.springframework.reactive.web.http.HttpHandler;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
@WebServlet(asyncSupported = true)
public class HttpHandlerServlet extends HttpServlet {

	private static final int BUFFER_SIZE = 8192;

	private static Log logger = LogFactory.getLog(HttpHandlerServlet.class);


	private HttpHandler handler;


	public void setHandler(HttpHandler handler) {
		this.handler = handler;
	}


	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		AsyncContext context = request.startAsync();
		AsyncContextSynchronizer contextSynchronizer = new AsyncContextSynchronizer(context);

		RequestBodyPublisher requestPublisher = new RequestBodyPublisher(contextSynchronizer, BUFFER_SIZE);
		request.getInputStream().setReadListener(requestPublisher);
		ServletServerHttpRequest httpRequest = new ServletServerHttpRequest(request, requestPublisher);

		ResponseBodySubscriber responseSubscriber = new ResponseBodySubscriber(contextSynchronizer);
		response.getOutputStream().setWriteListener(responseSubscriber);
		ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response, responseSubscriber);

		HandlerResultConsumer resultConsumer = new HandlerResultConsumer(contextSynchronizer, httpResponse);
		this.handler.handle(httpRequest, httpResponse).whenComplete(resultConsumer);
	}


	private static class HandlerResultConsumer implements BiConsumer<Void, Throwable> {

		private final AsyncContextSynchronizer synchronizer;

		private final ServletServerHttpResponse response;


		public HandlerResultConsumer(AsyncContextSynchronizer synchronizer, ServletServerHttpResponse response) {
			this.synchronizer = synchronizer;
			this.response = response;
		}

		@Override
		public void accept(Void aVoid, Throwable ex) {
			if (ex != null) {
				logger.error("Error from request handling. Completing the request.", ex);
				this.response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
				this.synchronizer.complete();
			}
			else {
				this.synchronizer.complete();
			}
		}
	}
}
