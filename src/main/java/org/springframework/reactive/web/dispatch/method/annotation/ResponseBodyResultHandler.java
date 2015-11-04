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

package org.springframework.reactive.web.dispatch.method.annotation;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.http.server.ReactiveServerHttpResponse;
import org.springframework.reactive.codec.encoder.Encoder;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.reactive.web.dispatch.HandlerResultHandler;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;


/**
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 */
public class ResponseBodyResultHandler implements HandlerResultHandler, Ordered {

	private final List<Encoder<?>> serializers;

	private final ConversionService conversionService;

	private int order = 0;


	public ResponseBodyResultHandler(List<Encoder<?>> serializers, ConversionService service) {
		Assert.notEmpty(serializers, "At least one serializers is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.serializers = serializers;
		this.conversionService = service;
	}


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public boolean supports(HandlerResult result) {
		Object handler = result.getHandler();
		if (handler instanceof HandlerMethod) {
			Method method = ((HandlerMethod) handler).getMethod();
			return AnnotatedElementUtils.isAnnotated(method, ResponseBody.class.getName());
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Publisher<Void> handleResult(ReactiveServerHttpRequest request,
			ReactiveServerHttpResponse response, HandlerResult result) {

		Object value = result.getValue();
		HandlerMethod handlerMethod = (HandlerMethod) result.getHandler();
		MethodParameter returnType = handlerMethod.getReturnValueType(value);

		if (value == null) {
			return Publishers.empty();
		}

		ResolvableType type = ResolvableType.forMethodParameter(returnType);
		MediaType mediaType = resolveMediaType(request);
		Publisher<?> elementStream;
		ResolvableType elementType;
		if (conversionService.canConvert(type.getRawClass(), Publisher.class)) {
			elementStream = conversionService.convert(value, Publisher.class);
			elementType = type.getGeneric(0);
		}
		else {
			elementStream = Publishers.just(value);
			elementType = type;
		}

		Encoder<?> serializer = resolveSerializer(elementType, mediaType);

		if (serializer != null) {
			Publisher<ByteBuffer> outputStream = serializer.encode((Publisher)elementStream, type, mediaType);
			if (mediaType == null || mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
				List<MimeType> mimeTypes = serializer.getSupportedMimeTypes();
				if (!mimeTypes.isEmpty()) {
					MimeType mimeType = mimeTypes.get(0);
					mediaType = new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
				}
			}
			if (mediaType != null && !mediaType.equals(MediaType.ALL)) {
				response.getHeaders().setContentType(mediaType);
			}
			return response.setBody(outputStream);
		}
		String returnTypeName = returnType.getParameterType().getName();
		return Publishers.error(new IllegalStateException("Return value type '" + returnTypeName +
				"' with media type '" + mediaType + "' not supported"));
	}

	private MediaType resolveMediaType(ReactiveServerHttpRequest request) {
		String acceptHeader = request.getHeaders().getFirst(HttpHeaders.ACCEPT);
		List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
		MediaType.sortBySpecificityAndQuality(mediaTypes);
		return ( mediaTypes.size() > 0 ? mediaTypes.get(0) : MediaType.TEXT_PLAIN);
	}

	private Encoder<?> resolveSerializer(ResolvableType type, MediaType mediaType, Object... hints) {
		for (Encoder<?> serializer : this.serializers) {
			if (serializer.canEncode(type, mediaType, hints)) {
				return serializer;
			}
		}
		return null;
	}

}
