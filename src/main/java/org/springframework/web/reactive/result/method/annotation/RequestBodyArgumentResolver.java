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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.codec.hint.StreamableHint;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * Resolves method arguments annotated with {@code @RequestBody} by reading and
 * decoding the body of the request through a compatible
 * {@code HttpMessageConverter}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

	private final List<HttpMessageConverter<?>> messageConverters;

	private final ConversionService conversionService;

	private final Validator validator;

	private final List<MediaType> supportedMediaTypes;


	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 */
	public RequestBodyArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service) {

		this(converters, service, null);
	}

	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 * @param validator validator to validate decoded objects with
	 */
	public RequestBodyArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service, Validator validator) {

		Assert.notEmpty(converters, "At least one message converter is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.messageConverters = converters;
		this.conversionService = service;
		this.validator = validator;
		this.supportedMediaTypes = converters.stream()
				.flatMap(converter -> converter.getReadableMediaTypes().stream())
				.collect(Collectors.toList());
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Return the configured {@link ConversionService}.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, ModelMap model,
			ServerWebExchange exchange) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		boolean isAsyncType = isAsyncType(type);
		ResolvableType elementType = (isAsyncType ? type.getGeneric(0) : type);
		StreamableHint streamableHint = (isStreamableType(type) ? StreamableHint.STREAMABLE : StreamableHint.SCALAR);

		MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
		if (mediaType == null) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		for (HttpMessageConverter<?> converter : getMessageConverters()) {
			if (converter.canRead(elementType, mediaType, streamableHint)) {
				Flux<?> elementFlux = converter.read(elementType, exchange.getRequest(), streamableHint);

				if (this.validator != null) {
					elementFlux= applyValidationIfApplicable(elementFlux, parameter);
				}

				if (Mono.class.equals(type.getRawClass())) {
					return Mono.just(Mono.from(elementFlux));
				}
				else if (Flux.class.equals(type.getRawClass())) {
					return Mono.just(elementFlux);
				}
				else if (isAsyncType) {
					return Mono.just(getConversionService().convert(elementFlux, type.getRawClass()));
				}
				else {
					return elementFlux.next().map(o -> o);
				}
			}
		}

		return Mono.error(new UnsupportedMediaTypeStatusException(mediaType, this.supportedMediaTypes));
	}

	private boolean isAsyncType(ResolvableType type) {
		return (Mono.class.equals(type.getRawClass()) || Flux.class.equals(type.getRawClass()) ||
				getConversionService().canConvert(Publisher.class, type.getRawClass()));
	}

	private boolean isStreamableType(ResolvableType type) {
		// TODO Avoid hardcoding types like Observable, should be handled by the GenericConverter or something along these lines
		Class<?> clazz = type.getRawClass();
		return (Flux.class.isAssignableFrom(clazz) || Observable.class.isAssignableFrom(clazz) || (Publisher.class.isAssignableFrom(clazz) && !Mono.class.isAssignableFrom(clazz)));
	}

	protected Flux<?> applyValidationIfApplicable(Flux<?> elementFlux, MethodParameter methodParam) {
		Annotation[] annotations = methodParam.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validAnnot = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validAnnot != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validAnnot != null ? validAnnot.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				return elementFlux.map(element -> {
					validate(element, validationHints, methodParam);
					return element;
				});
			}
		}
		return elementFlux;
	}

	/**
	 * TODO: replace with use of DataBinder
	 */
	private void validate(Object target, Object[] validationHints, MethodParameter methodParam) {
		String name = Conventions.getVariableNameForParameter(methodParam);
		Errors errors = new BeanPropertyBindingResult(target, name);
		if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
			((SmartValidator) this.validator).validate(target, errors, validationHints);
		}
		else if (this.validator != null) {
			this.validator.validate(target, errors);
		}
		if (errors.hasErrors()) {
			throw new ServerWebInputException("Validation failed", methodParam);
		}
	}

}
