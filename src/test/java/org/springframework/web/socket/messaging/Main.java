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

import java.io.IOException;

import reactor.core.publisher.Flux;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.support.ReactiveMessageChannel;


public class Main {

	public static void main(String[] a) throws IOException {

		AnnotationConfigApplicationContext cxt = new AnnotationConfigApplicationContext();
		cxt.register(Config.class);
		cxt.refresh();
		cxt.start();

		Flux<String> webSocketStream1 = Flux.just("one", "two", "user-three");
		Flux<String> webSocketStream2 = Flux.just("uno", "dos", "user-tres");

		StompSubProtocolHandler subProtocolHandler = cxt.getBean(StompSubProtocolHandler.class);
		subProtocolHandler.handleWebSocketMessages("sess1", webSocketStream1);
		subProtocolHandler.handleWebSocketMessages("sess2", webSocketStream2);

		//noinspection ResultOfMethodCallIgnored
		System.in.read();
	}


	@Configuration @SuppressWarnings("unused")
	static class Config {

		@Bean
		public StompSubProtocolHandler stompSubProtocolHandler() {
			return new StompSubProtocolHandler(clientInChannel(), clientOutChannel());
		}

		@Bean
		public ReactiveMessageChannel clientInChannel() {
			return new ReactiveMessageChannel();
		}

		@Bean
		public ReactiveMessageChannel clientOutChannel() {
			return new ReactiveMessageChannel();
		}

		@Bean
		public ReactiveMessageChannel brokerChannel() {
			return new ReactiveMessageChannel();
		}

		@Bean
		public SimpAnnotationMethodMessageHandler annotatedMethodMessageHandler() {
			return new SimpAnnotationMethodMessageHandler(clientInChannel());
		}

		@Bean
		public SimpleBrokerMessageHandler simpleBrokerMessageHandler() {
			return new SimpleBrokerMessageHandler(brokerChannel(), clientInChannel(), clientOutChannel());
		}

		@Bean
		public UserDestinationMessageHandler userDestinationMessageHandler() {
			return new UserDestinationMessageHandler(clientInChannel(), brokerChannel());
		}
	}

}
