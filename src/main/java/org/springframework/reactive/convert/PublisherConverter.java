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

package org.springframework.reactive.convert;

import java.util.LinkedHashSet;
import java.util.Set;

import org.reactivestreams.Publisher;

import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Sebastien Deleuze
 */
public final class PublisherConverter implements ReactiveConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		Set<ConvertiblePair> convertibleTypes = new LinkedHashSet<>();
		convertibleTypes.add(new ConvertiblePair(Publisher.class, Publisher.class));
		return convertibleTypes;
	}

	@Override
	public Boolean isCollection(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (Publisher.class.isAssignableFrom(sourceType.getResolvableType().getRawClass()) ||
				Publisher.class.isAssignableFrom(targetType.getResolvableType().getRawClass())) {
			return true;
		}
		return null;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return source;
	}

}
