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
import reactor.rx.Promise;
import reactor.rx.Streams;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 */
public final class ReactorPromiseConverter implements ReactiveConverter {

	@Override
	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		Set<GenericConverter.ConvertiblePair> convertibleTypes = new LinkedHashSet<>();
		convertibleTypes.add(new GenericConverter.ConvertiblePair(Publisher.class, Promise.class));
		convertibleTypes.add(new GenericConverter.ConvertiblePair(Promise.class, Publisher.class));
		return convertibleTypes;
	}

	@Override
	public Boolean isCollection(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (Promise.class.isAssignableFrom(sourceType.getResolvableType().getRawClass()) ||
				Promise.class.isAssignableFrom(targetType.getResolvableType().getRawClass())) {
			return false;
		}
		return null;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source != null) {
			if (Promise.class.isAssignableFrom(source.getClass())) {
				return ((Promise<?>)source);
			} else  {
				return Streams.wrap((Publisher)source).next();
			}
		}
		return null;
	}

}
