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

import org.reactivestreams.Publisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Stephane Maldini
 */
public abstract class PublisherConverter implements ConversionService {

	@Override
	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return canConvert(sourceType.getType(), targetType.getType());
	}

	@Override
	public final boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return
		  (Publisher.class.isAssignableFrom(sourceType) && canConvertFromPublisher(targetType)) ||
			(Publisher.class.isAssignableFrom(targetType) && canConvertToPublisher(sourceType));
	}

	protected abstract boolean canConvertFromPublisher(Class<?> targetType);

	protected boolean canConvertToPublisher(Class<?> sourceType) {
		return canConvertFromPublisher(sourceType);
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return convert(source, targetType.getType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Class<T> targetType) {
		if (source != null) {
			if (Publisher.class.isAssignableFrom(source.getClass())) {
				return convertFromPublisher((Publisher<?>) source, targetType);
			} else if (Publisher.class.isAssignableFrom(targetType)) {
				return (T) convertToPublisher(source);
			}
		}
		return null;
	}

	protected abstract <T> T convertFromPublisher(Publisher<?> source, Class<T> targetType);

	protected abstract Publisher<?> convertToPublisher(Object source);
}
