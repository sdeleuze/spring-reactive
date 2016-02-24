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

package org.springframework.messaging.core;

import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;

/**
 * Operations for receiving messages from a destination.
 *
 * @author Sebastien Deleuze
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @param <D> the type of destination to receive messages from
 */
public interface ReactiveMessageReceivingOperations<D> {

	/**
	 * Receive a message from a default destination.
	 * @return a {@code Mono} emitting a complete signal with the message when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	Mono<Message<?>> receive();

	/**
	 * Receive a message from the given destination.
	 * @param destination the target destination
	 * @return a {@code Mono} emitting a complete signal with the message when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	Mono<Message<?>> receive(D destination);

	/**
	 * Receive a message from a default destination and convert its payload to the
	 * specified target class.
	 * @param targetType the target type to convert the payload to
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> receiveAndConvert(ResolvableType targetType);

	/**
	 * Receive a message from the given destination and convert its payload to the
	 * specified target class.
	 * @param destination the target destination
	 * @param targetType the target type to convert the payload to
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> receiveAndConvert(D destination, ResolvableType targetType);

}
