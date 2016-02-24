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

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;

/**
 * Operations for sending messages to and receiving the reply from a destination.
 *
 * @author Sebastien Deleuze
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @param <D> the type of destination
 */
public interface ReactiveMessageRequestReplyOperations<D> {

	/**
	 * Send a request message and receive the reply from a default destination.
	 * @param requestMessage the message to send
	 * @return a {@code Mono} emitting a complete signal with the message when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	Mono<Message<?>> sendAndReceive(Publisher<Message<?>> requestMessage);

	/**
	 * Send a request message and receive the reply from the given destination.
	 * @param destination the target destination
	 * @param requestMessage the message to send
	 * @return a {@code Mono} emitting a complete signal with the message when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	Mono<Message<?>> sendAndReceive(D destination, Publisher<Message<?>> requestMessage);

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter}, send
	 * it as a {@link Message} to a default destination, receive the reply and convert
	 * its body of the specified target class.
	 * @param request payload for the request message to send
	 * @param targetType the target type to convert the payload of the reply to
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> convertSendAndReceive(Publisher<Object> request, ResolvableType targetType);

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter}, send
	 * it as a {@link Message} to the given destination, receive the reply and convert
	 * its body of the specified target class.
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param targetType the target type to convert the payload of the reply to
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> convertSendAndReceive(D destination, Publisher<Object> request, ResolvableType targetType);

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter}, send
	 * it as a {@link Message} with the given headers, to the specified destination,
	 * receive the reply and convert its body of the specified target class.
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param headers headers for the request message to send
	 * @param targetType the target type to convert the payload of the reply to
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> convertSendAndReceive(D destination, Publisher<Object> request, Map<String, Object> headers, ResolvableType targetType);

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * apply the given post processor and send the resulting {@link Message} to a
	 * default destination, receive the reply and convert its body of the given
	 * target class.
	 * @param request payload for the request message to send
	 * @param targetType the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> convertSendAndReceive(Publisher<Object> request, ResolvableType targetType, MessagePostProcessor requestPostProcessor);

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * apply the given post processor and send the resulting {@link Message} to the
	 * given destination, receive the reply and convert its body of the given
	 * target class.
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param targetType the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> convertSendAndReceive(D destination, Publisher<Object> request, ResolvableType targetType, MessagePostProcessor requestPostProcessor);

	/**
	 * Convert the given request Object to serialized form, possibly using a
	 * {@link org.springframework.messaging.converter.MessageConverter},
	 * wrap it as a message with the given headers, apply the given post processor
	 * and send the resulting {@link Message} to the specified destination, receive
	 * the reply and convert its body of the given target class.
	 * @param destination the target destination
	 * @param request payload for the request message to send
	 * @param targetType the target type to convert the payload of the reply to
	 * @param requestPostProcessor post process to apply to the request message
	 * @return a {@code Mono} emitting a complete signal with the payload when the message
	 * has been effectively sent, or an error signal with a {link MessagingException}
	 */
	<T> Mono<T> convertSendAndReceive(D destination, Publisher<Object> request, Map<String, Object> headers, ResolvableType targetType, MessagePostProcessor requestPostProcessor);

}
