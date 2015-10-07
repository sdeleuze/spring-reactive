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

/**
 * @author Stephane Maldini
 */
public final class CompositionDependencyUtils {

	static private final boolean HAS_RXJAVA_1;
	static private final boolean HAS_RXJAVA_1_RS_BRIDGE;
	static private final boolean HAS_RXJAVA_2;
	static private final boolean HAS_REACTOR_STREAM;

	static {
		boolean hasRxjava1 = false;
		boolean hasRxjava1_bridge = false;
		boolean hasRxjava2 = false;
		boolean hasRxReactor = false;
		try {
			Class.forName("rx.Observable");
			hasRxjava1 = true;
			Class.forName("rx.RxReactiveStreams");
			hasRxjava1_bridge = true;
		}
		catch (ClassNotFoundException cnfe) {
			//IGNORE
		}
		try {
			Class.forName("io.reactivex.Observable");
			hasRxjava2 = true;
		}
		catch (ClassNotFoundException cnfe) {
			//IGNORE
		}
		try {
			Class.forName("reactor.rx.Stream");
			hasRxReactor = true;
		}
		catch (ClassNotFoundException cnfe) {
			//IGNORE
		}

		HAS_RXJAVA_1 = hasRxjava1;
		HAS_RXJAVA_1_RS_BRIDGE = hasRxjava1_bridge;
		HAS_RXJAVA_2 = hasRxjava2;
		HAS_REACTOR_STREAM = hasRxReactor;
	}

	public static boolean hasRxJava1() {
		return HAS_RXJAVA_1;
	}


	public static boolean hasRxReactiveStream() {
		return HAS_RXJAVA_1_RS_BRIDGE;
	}

	public static boolean hasRxJava2() {
		return HAS_RXJAVA_2;
	}

	public static boolean hasReactorStream() {
		return HAS_REACTOR_STREAM;
	}
}
