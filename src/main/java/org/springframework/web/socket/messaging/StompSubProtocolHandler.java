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
package org.springframework.web.socket.messaging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.ReactiveMessageChannel;
import org.springframework.messaging.support.ReactiveMessageHandler;

public class StompSubProtocolHandler implements ReactiveMessageHandler, Lifecycle {

	private final static Log logger = LogFactory.getLog(StompSubProtocolHandler.class);


	private final ReactiveMessageChannel clientInboundChannel;

	private final ReactiveMessageChannel clientOutboundChannel;

	private boolean started;


	public StompSubProtocolHandler(ReactiveMessageChannel clientInboundChannel,
			ReactiveMessageChannel clientOutboundChannel) {

		this.clientInboundChannel = clientInboundChannel;
		this.clientOutboundChannel = clientOutboundChannel;
	}


	@Override
	public boolean isRunning() {
		return this.started;
	}

	@Override
	public void start() {
		if (!this.started) {
			this.started = true;
			this.clientOutboundChannel.subscribe(this);
		}
	}

	@Override
	public void stop() {
		if (this.started) {
			this.started = false;
			this.clientOutboundChannel.unsubscribe(this);
		}
	}

	public void handleWebSocketMessages(String sessionId, Flux<String> webSocketFlux) {
		Flux<Message<?>> messageFlux = webSocketFlux.map(message -> createMessage(message, sessionId));
		this.clientInboundChannel.sendWith(messageFlux);
	}

	private Message<?> createMessage(Object payload, String sessionId) {
		return MessageBuilder.withPayload(payload).setHeader("session-id", sessionId).build();
	}

	@Override
	public void handleMessageStream(Publisher<Message<?>> messageStream) {
		Flux.from(messageStream).consume(
				message -> {
					String id = (String) message.getHeaders().get("session-id");
					logger.debug("Message to \"" + id + "\" payload=" + message.getPayload());
				},
				ex -> logger.error("Outbound WebSocket onError", ex),
				() -> logger.debug("Outbound WebSocket onCompleted")
		);
	}

	@Override
	public void handleMessage(Message<?> message) {
		handleMessageStream(Mono.just(message));
	}


}
