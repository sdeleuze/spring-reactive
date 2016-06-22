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

package org.springframework.core.codec.hint;

import java.util.Optional;

/**
 * @author Sebastien Deleuze
 */
public abstract class HintUtils {

	public static <T> Optional<T> getHint(Object[] hints, Class<T> clazz) {
		if (hints != null) {
			for (int i = 0; i < hints.length; i++) {
				if (hints[i].getClass().equals(clazz)) {
					return Optional.of((T)hints[i]);
				}
			}
		}
		return Optional.empty();
	}

	public static boolean hasHint(Object[] hints, Object hint) {
		if (hints != null) {
			for (int i = 0; i < hints.length; i++) {
				if (hints[i] == hint) {
					return true;
				}
			}
		}
		return false;
	}

}
