/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging;

import reactor.core.publisher.Mono;

/**
 * A {@link ReactiveMessageChannel} from which messages may be actively received through polling.
 *
 * @author Sebastien Deleuze
 * @author Mark Fisher
 */
public interface ReactivePollableChannel extends ReactiveMessageChannel {

	/**
	 * Receive a message from this channel.
	 * @return a {@code Mono} emitting a complete signal when the message has been
	 * effectively received
	 * TODO timeout() operator will be available in Reactor Core 2.5 M2, see https://github.com/reactor/reactor-core/issues/29
	 */
	Mono<Message<?>> receive();

}
