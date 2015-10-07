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
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

/**
 * @author Stephane Maldini
 */
public final class RxJava1Converter extends PublisherConverter {

	@Override
	protected boolean canConvertFromPublisher(Class<?> targetType) {
		return
		  Observable.class.isAssignableFrom(targetType) ||
			Single.class.isAssignableFrom(targetType);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> T convertFromPublisher(Publisher<?> source, Class<T> targetType) {
		if(Observable.class.isAssignableFrom(targetType)){
			return (T)RxReactiveStreams.toObservable(source);
		}
		else if (Single.class.isAssignableFrom(targetType)) {
			return (T)RxReactiveStreams.toObservable(source).toSingle();
		}
		throw new IllegalArgumentException("Publisher ["+source+"] cannot be converted to type ["+targetType+"]");
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Publisher<?> convertToPublisher(Object source) {
		if(Observable.class.isAssignableFrom(source.getClass())){
			return RxReactiveStreams.toPublisher((Observable) source);
		}
		else if (Single.class.isAssignableFrom(source.getClass())) {
			return RxReactiveStreams.toPublisher(((Single) source).toObservable());
		}
		throw new IllegalArgumentException("Argument ["+source+"] cannot be converted to Publisher");
	}
}
