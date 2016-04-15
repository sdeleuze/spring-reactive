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
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.subscriber.SignalEmitter;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.ReactiveMessageChannel;
import org.springframework.messaging.support.ReactiveMessageHandler;

public class SimpleBrokerMessageHandler implements ReactiveMessageHandler, Lifecycle {

	private static Log logger = LogFactory.getLog(SimpleBrokerMessageHandler.class);


	private final ReactiveMessageChannel brokerChannel;

	private final ReactiveMessageChannel clientInChannel;

	private final ReactiveMessageChannel clientOutChannel;

	private boolean started;

	/* Emitters for each  */
	private final Map<String, SignalEmitter<Message<?>>> emitters = new ConcurrentHashMap<>();


	public SimpleBrokerMessageHandler(ReactiveMessageChannel brokerChannel,
			ReactiveMessageChannel clientInChannel, ReactiveMessageChannel clientOutChannel) {

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
	public void handleMessageStream(Publisher<Message<?>> messageStream) {
		Flux.from(messageStream).consume(message -> {

			// 1. Create SignalEmitter for outbound client messages
			String id = (String) message.getHeaders().get("session-id");
			if (id != null && !this.emitters.containsKey(id)) {
				logger.debug("Adding emitter for session \"" + id + "\"");
				EmitterProcessor<Message<?>> processor = EmitterProcessor.create();
				SignalEmitter<Message<?>> emitter = processor.connectEmitter();
				this.emitters.put(id, emitter);
				this.clientOutChannel.sendWith(processor);
			}

			// 2. For now broadcast every message
			broadcastMessage(message);
		},
		ex -> logger.error("Simple broker onError", ex),
		() -> logger.debug("Simple broker onCompleted"));
	}

	@Override
	public void handleMessage(Message<?> message) {
		handleMessageStream(Mono.just(message));
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
