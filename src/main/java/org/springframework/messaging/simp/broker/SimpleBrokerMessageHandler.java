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
package org.springframework.messaging.simp.broker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.subscriber.SignalEmitter;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;


public class SimpleBrokerMessageHandler implements MessageHandler, Lifecycle {

	private static Log logger = LogFactory.getLog(SimpleBrokerMessageHandler.class);


	private final SubscribableChannel brokerChannel;

	private final SubscribableChannel clientInChannel;

	private final SubscribableChannel clientOutChannel;

	private boolean started;

	/* Emitters for each  */
	private final Map<String, SignalEmitter<Message<?>>> emitters = new ConcurrentHashMap<>();


	public SimpleBrokerMessageHandler(SubscribableChannel brokerChannel,
			SubscribableChannel clientInChannel, SubscribableChannel clientOutChannel) {

		this.brokerChannel = brokerChannel;
		this.clientInChannel = clientInChannel;
		this.clientOutChannel = clientOutChannel;
	}


	@Override
	public boolean isRunning() {
		return this.started;
	}

	@Override
	public void start() {
		if (!this.started) {
			this.started = true;
			this.brokerChannel.subscribe(this);
			this.clientInChannel.subscribe(this);
		}
	}

	@Override
	public void stop() {
		if (this.started) {
			this.started = false;
			this.brokerChannel.unsubscribe(this);
			this.clientInChannel.unsubscribe(this);
		}
	}

	@Override
	public void handleMessage(Message<?> message) {

		Object payload = message.getPayload();
		if (payload instanceof Flux) {

			// 1. Create SignalEmitter for outbound client messages

			String id = (String) message.getHeaders().get("session-id");
			if (id != null && !this.emitters.containsKey(id)) {
				logger.debug("Adding emitter for session \"" + id + "\"");
				EmitterProcessor<Message<?>> processor = EmitterProcessor.create();
				SignalEmitter<Message<?>> emitter = processor.connectEmitter();
				this.emitters.put(id, emitter);
				this.clientOutChannel.send(createMessage(processor, id));
			}

			// 2. For now broadcast every message

			//noinspection unchecked
			((Flux<Message<?>>) payload).consume(
					this::broadcastMessage,
					ex -> logger.error("Simple broker onError", ex),
					() -> logger.debug("Simple broker onCompleted")
			);
		}
		else {
			logger.error("Unexpected message: " + message);
		}
	}

	private void broadcastMessage(Message<?> message) {
		this.emitters.entrySet().forEach(
				entry -> {
					String sessionId = entry.getKey();
					SignalEmitter<Message<?>> emitter = entry.getValue();
					logger.debug("Broadcast to \"" + sessionId + "\" payload=" + message.getPayload());
					emitter.submit(createMessage(message.getPayload(), sessionId));
				});
	}

	private Message<?> createMessage(Object payload, String sessionId) {
		return MessageBuilder.withPayload(payload).setHeader("session-id", sessionId).build();
	}

}
