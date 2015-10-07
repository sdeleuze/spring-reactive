/*
 * Copyright (c) 2011-2015 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.reactive.web.dispatch.method.convert;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Stephane Maldini
 */
public class DefaultCompositionConverter implements ConversionService {

	static public final DefaultCompositionConverter INSTANCE = new DefaultCompositionConverter();

	static final ConversionService reactorStreamConverter;
	static final ConversionService rxJava1Converter;

	static {
		if (CompositionDependencyUtils.hasRxJava1() && CompositionDependencyUtils.hasRxReactiveStream()) {
			rxJava1Converter = new RxJava1Converter();
		} else {
			rxJava1Converter = null;
		}
		if (CompositionDependencyUtils.hasReactorStream()) {
			reactorStreamConverter = new ReactorStreamConverter();
		} else {
			reactorStreamConverter = null;
		}

	}

	private DefaultCompositionConverter(){
		//IGNORE
	}

	@Override
	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return
		  targetType.isAssignableFrom(sourceType) ||
			(reactorStreamConverter != null && reactorStreamConverter.canConvert(sourceType, targetType)) ||
			(rxJava1Converter != null && rxJava1Converter.canConvert(sourceType, targetType));
	}

	@Override
	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return canConvert(sourceType.getType(), targetType.getType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Class<T> targetType) {
		Class<?> sourceType = source == null ? Void.class : source.getClass();

		if (targetType.isAssignableFrom(sourceType)) {
			return (T) source;
		}
		else if (reactorStreamConverter != null && reactorStreamConverter.canConvert(sourceType, targetType)) {
			return reactorStreamConverter.convert(source, targetType);
		}
		else if (rxJava1Converter != null && rxJava1Converter.canConvert(sourceType, targetType)) {
			return rxJava1Converter.convert(source, targetType);
		}
		else {
			throw new IllegalArgumentException("Source ["+source+"] of type ["+sourceType+"] cannot be converted to type ["+targetType);
		}
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return convert(source, targetType.getType());
	}
}
