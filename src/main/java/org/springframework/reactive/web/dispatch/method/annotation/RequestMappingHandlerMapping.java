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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.reactive.web.dispatch.HandlerMapping;
import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;


/**
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerMapping implements HandlerMapping,
		ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(RequestMappingHandlerMapping.class);


	private final Map<RequestMappingInfo, HandlerMethod> methodMap = new TreeMap<>();

	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		for (Object bean : this.applicationContext.getBeansOfType(Object.class).values()) {
			detectHandlerMethods(bean);
		}
	}

	protected void detectHandlerMethods(final Object bean) {
		final Class<?> beanType = bean.getClass();
		if (AnnotationUtils.findAnnotation(beanType, Controller.class) != null) {
			HandlerMethodSelector.selectMethods(beanType, method -> {
				RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
				if (annotation != null && annotation.value().length > 0) {
					String path = annotation.value()[0];
					RequestMethod[] methods = annotation.method();
					HandlerMethod handlerMethod = new HandlerMethod(bean, method);
					if (logger.isInfoEnabled()) {
						logger.info("Mapped \"" + path + "\" onto " + handlerMethod);
					}
					RequestMappingInfo info = new RequestMappingInfo(path, methods);
					if (this.methodMap.containsKey(info)) {
						throw new IllegalStateException("Duplicate mapping found for " + info);
					}
					methodMap.put(info, handlerMethod);
				}
				return false;
			});
		}
	}

	@Override
	public Object getHandler(ServerHttpRequest request) {
		String path = request.getURI().getPath();
		HttpMethod method = request.getMethod();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : this.methodMap.entrySet()) {
			RequestMappingInfo info = entry.getKey();
			if (path.equals(info.getPath()) && (info.getMethods().isEmpty() || info.getMethods().contains(RequestMethod.valueOf(method.name())))) {
				if (logger.isDebugEnabled()) {
					logger.debug("Mapped " + method + " " + path + " to [" + entry.getValue() + "]");
				}
				return entry.getValue();
			}
		}
		return null;
	}


	private static class RequestMappingInfo implements Comparable {

		private String path;

		private Set<RequestMethod> methods;


		public RequestMappingInfo(String path, RequestMethod... methods) {
			this(path, asList(methods));
		}

		public RequestMappingInfo(String path, Collection<RequestMethod> methods) {
			this.path = path;
			this.methods = new TreeSet<>(methods);
		}


		public String getPath() {
			return path;
		}

		public Set<RequestMethod> getMethods() {
			return methods;
		}

		private static List<RequestMethod> asList(RequestMethod... requestMethods) {
			return (requestMethods != null ? Arrays.asList(requestMethods) : Collections.<RequestMethod>emptyList());
		}

		@Override
		public int compareTo(Object o) {
			RequestMappingInfo other = (RequestMappingInfo)o;
			if (!this.path.equals(other.getPath())) {
				return -1;
			}
			if (this.methods.isEmpty() && !other.methods.isEmpty()) {
				return 1;
			}
			if (!this.methods.isEmpty() && other.methods.isEmpty()) {
				return -1;
			}
			if (this.methods.equals(other.methods)) {
				return 0;
			}
			return -1;
		}
	}

}
