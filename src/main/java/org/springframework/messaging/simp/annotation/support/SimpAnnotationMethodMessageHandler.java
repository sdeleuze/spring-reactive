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
package org.springframework.messaging.simp.annotation.support;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;


public class SimpAnnotationMethodMessageHandler implements MessageHandler, Lifecycle {

	private static Log logger = LogFactory.getLog(SimpAnnotationMethodMessageHandler.class);


	private final SubscribableChannel clientInChannel;

	private boolean started;


	public SimpAnnotationMethodMessageHandler(SubscribableChannel clientInChannel) {
		this.clientInChannel = clientInChannel;
	}


	@Override
	public boolean isRunning() {
		return this.started;
	}

	@Override
	public void start() {
		if (!this.started) {
			this.started = true;
			this.clientInChannel.subscribe(this);
		}
	}

	@Override
	public void stop() {
		if (this.started) {
			this.started = false;
			this.clientInChannel.unsubscribe(this);
		}
	}

	@Override
	public void handleMessage(Message<?> messageFlux) {
		String id = (String) messageFlux.getHeaders().get("session-id");
		Object payload = messageFlux.getPayload();
		if (payload instanceof Flux) {
			//noinspection unchecked
			((Flux<Message<?>>) payload).consume(
					message -> logger.debug("Message from \"" + id + "\" payload=" + message.getPayload()),
					ex -> logger.error("onError", ex),
					() -> logger.debug("onCompleted")
			);
		}
		else {
			logger.error("Unexpected message: " + messageFlux);
		}
	}

}
