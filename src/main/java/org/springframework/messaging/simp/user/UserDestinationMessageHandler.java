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
package org.springframework.messaging.simp.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;


public class UserDestinationMessageHandler implements MessageHandler, Lifecycle {

	private final static Log logger = LogFactory.getLog(UserDestinationMessageHandler.class);


	private final SubscribableChannel clientInChannel;

	private final SubscribableChannel brokerChannel;

	private boolean started;


	public UserDestinationMessageHandler(SubscribableChannel clientInChannel,
			SubscribableChannel brokerChannel) {

		this.clientInChannel = clientInChannel;
		this.brokerChannel = brokerChannel;
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
	public void handleMessage(Message<?> message) {

		// In the real implementation we'd check and transform the "destination" header...

		Object payload = message.getPayload();
		if (payload instanceof Flux) {
			//noinspection unchecked
			Flux<Message<String>> userMessages = ((Flux<Message<?>>) payload)
					.filter(m -> (((String) m.getPayload()).startsWith("user-")))
					.map(m -> {
						String from = (String) m.getPayload();
						String to = from.replaceAll("^user", (String) message.getHeaders().get("session-id"));
						logger.debug("Transformed \"" + from + "\" to \"" + to + "\"");
						return MessageBuilder.createMessage(to, message.getHeaders());
					});
			this.brokerChannel.send(MessageBuilder.createMessage(userMessages, message.getHeaders()));
		}
		else {
			logger.error("Unexpected message: " + message);
		}
	}

}
