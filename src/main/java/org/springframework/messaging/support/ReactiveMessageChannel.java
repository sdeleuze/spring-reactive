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
package org.springframework.messaging.support;

import java.util.ArrayList;
import java.util.List;

import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;


public class ReactiveMessageChannel implements SubscribableChannel {

	private final List<MessageHandler> handlers = new ArrayList<>();


	@Override
	public boolean subscribe(MessageHandler handler) {
		return this.handlers.add(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		return this.handlers.remove(handler);
	}

	@Override
	public boolean send(Message<?> message) {

		Object payload = message.getPayload();
		if (payload instanceof Flux) {
			//noinspection unchecked
			ConnectableFlux<Message<?>> flux = ((Flux<Message<?>>) payload).publish();
			final Message<Flux<Message<?>>> wrapped = MessageBuilder.createMessage(flux, message.getHeaders());
			this.handlers.forEach(handler -> handler.handleMessage(wrapped));
			flux.connect();
		}
		else {
			this.handlers.forEach(handler -> handler.handleMessage(message));
		}

		return true;
	}


	@Override
	public boolean send(Message<?> message, long timeout) {
		return false;
	}

}
