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

package org.springframework.reactive.web.dispatch;

import org.springframework.core.ResolvableType;

/**
 * Represent the result of the invocation of an handler.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerResult {

	private final Object handler;

	private final Object value;

	private final ResolvableType type;


	public HandlerResult(Object handler, Object value, ResolvableType type) {
		this.handler = handler;
		this.value = value;
		this.type = type;
	}


	public Object getHandler() {
		return this.handler;
	}

	public Object getValue() {
		return this.value;
	}

	public ResolvableType getType() {
		return type;
	}
}
