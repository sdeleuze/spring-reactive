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

package org.springframework.reactive.codec.encoder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.subscriber.SubscriberBarrier;
import reactor.core.support.BackpressureUtils;

import org.springframework.core.ResolvableType;
import org.springframework.reactive.codec.decoder.JsonObjectDecoder;
import org.springframework.reactive.io.Bytes;
import org.springframework.util.MimeType;

import static reactor.Publishers.lift;

/**
 * Encode a byte stream of individual JSON element to a byte stream representing
 * a single JSON array when if it contains more than one element.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 *
 * @see JsonObjectDecoder
 */
public class JsonObjectEncoder extends AbstractEncoder<Bytes> {

	public JsonObjectEncoder() {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
	}

	@Override
	public Publisher<Bytes> encode(Publisher<? extends Bytes> messageStream,
			ResolvableType type, MimeType mimeType, Object... hints) {

		//noinspection Convert2MethodRef
		return lift(messageStream, bbs -> new JsonEncoderBarrier(bbs));
	}


	private static class JsonEncoderBarrier extends SubscriberBarrier<Bytes, Bytes> {

		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<JsonEncoderBarrier> REQUESTED =
				AtomicLongFieldUpdater.newUpdater(JsonEncoderBarrier.class, "requested");

		static final AtomicIntegerFieldUpdater<JsonEncoderBarrier> TERMINATED =
				AtomicIntegerFieldUpdater.newUpdater(JsonEncoderBarrier.class, "terminated");


		private Bytes prev = null;

		private long count = 0;

		private volatile long requested;

		private volatile int terminated;


		public JsonEncoderBarrier(Subscriber<? super Bytes> subscriber) {
			super(subscriber);
		}


		@Override
		protected void doRequest(long n) {
			BackpressureUtils.getAndAdd(REQUESTED, this, n);
			if(TERMINATED.compareAndSet(this, 1, 2)){
				drainLast();
			}
			else {
				super.doRequest(n);
			}
		}

		@Override
		protected void doNext(Bytes next) {
			this.count++;
			if (this.count == 1) {
				this.prev = next;
				super.doRequest(1);
				return;
			}

			Bytes tmp = this.prev;
			this.prev = next;
			Bytes buffer = Bytes.create();
			if (this.count == 2) {
				buffer.append("[".getBytes());
			}
			buffer.append(tmp);
			buffer.append(",".getBytes());
			buffer.flip();

			BackpressureUtils.getAndSub(REQUESTED, this, 1L);
			downstream().onNext(buffer);
		}

		protected void drainLast(){
			if(BackpressureUtils.getAndSub(REQUESTED, this, 1L) > 0) {
				Bytes buffer = Bytes.create();
				buffer.append(this.prev);
				if (this.count > 1) {
					buffer.append("]".getBytes());
				}
				buffer.flip();
				downstream().onNext(buffer);
				super.doComplete();
			}
		}

		@Override
		protected void doComplete() {
			if(TERMINATED.compareAndSet(this, 0, 1)) {
				drainLast();
			}
		}
	}

}
