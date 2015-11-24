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

package org.springframework.http.server.undertow;

import static org.xnio.IoUtils.safeClose;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.springframework.core.io.Bytes;
import org.springframework.util.Assert;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.SameThreadExecutor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSourceChannel;
import reactor.core.error.SpecificationExceptions;
import reactor.core.support.BackpressureUtils;

/**
 * @author Marek Hawrylczak
 */
class RequestBodyPublisher implements Publisher<Bytes> {

	private static final AtomicLongFieldUpdater<RequestBodySubscription> DEMAND =
			AtomicLongFieldUpdater.newUpdater(RequestBodySubscription.class, "demand");


	private final HttpServerExchange exchange;

	private Subscriber<? super Bytes> subscriber;


	public RequestBodyPublisher(HttpServerExchange exchange) {
		Assert.notNull(exchange, "'exchange' is required.");
		this.exchange = exchange;
	}


	@Override
	public void subscribe(Subscriber<? super Bytes> subscriber) {
		if (subscriber == null) {
			throw SpecificationExceptions.spec_2_13_exception();
		}
		if (this.subscriber != null) {
			subscriber.onError(new IllegalStateException("Only one subscriber allowed"));
		}

		this.subscriber = subscriber;
		this.subscriber.onSubscribe(new RequestBodySubscription());
	}


	private class RequestBodySubscription implements Subscription, Runnable,
			ChannelListener<StreamSourceChannel> {

		volatile long demand;

		private PooledByteBuffer pooledBuffer;

		private StreamSourceChannel channel;

		private boolean subscriptionClosed;

		private boolean draining;


		@Override
		public void request(long n) {
			BackpressureUtils.checkRequest(n, subscriber);
			if (this.subscriptionClosed) {
				return;
			}
			BackpressureUtils.getAndAdd(DEMAND, this, n);
			scheduleNextMessage();
		}

		private void scheduleNextMessage() {
			exchange.dispatch(exchange.isInIoThread() ? SameThreadExecutor.INSTANCE :
					exchange.getIoThread(), this);
		}

		@Override
		public void cancel() {
			this.subscriptionClosed = true;
			close();
		}

		private void close() {
			if (this.pooledBuffer != null) {
				safeClose(this.pooledBuffer);
				this.pooledBuffer = null;
			}
			if (this.channel != null) {
				safeClose(this.channel);
				this.channel = null;
			}
		}

		@Override
		public void run() {
			if (this.subscriptionClosed || this.draining) {
				return;
			}
			if (0 == BackpressureUtils.getAndSub(DEMAND, this, 1)) {
				return;
			}

			this.draining = true;

			if (this.channel == null) {
				this.channel = exchange.getRequestChannel();

				if (this.channel == null) {
					if (exchange.isRequestComplete()) {
						return;
					}
					else {
						throw new IllegalStateException("Failed to acquire channel!");
					}
				}
			}
			if (this.pooledBuffer == null) {
				this.pooledBuffer = exchange.getConnection().getByteBufferPool().allocate();
			}
			else {
				this.pooledBuffer.getBuffer().clear();
			}

			try {
				ByteBuffer buffer = this.pooledBuffer.getBuffer();
				int count;
				do {
					count = this.channel.read(buffer);
					if (count == 0) {
						this.channel.getReadSetter().set(this);
						this.channel.resumeReads();
					}
					else if (count == -1) {
						if (buffer.position() > 0) {
							doOnNext(Bytes.from(buffer));
						}
						doOnComplete();
					}
					else {
						if (buffer.remaining() == 0) {
							if (this.demand == 0) {
								this.channel.suspendReads();
							}
							doOnNext(Bytes.from(buffer));
							if (this.demand > 0) {
								scheduleNextMessage();
							}
							break;
						}
					}
				} while (count > 0);
			}
			catch (IOException e) {
				doOnError(e);
			}
		}

		private void doOnNext(Bytes buffer) {
			this.draining = false;
			buffer.flip();
			subscriber.onNext(buffer);
		}

		private void doOnComplete() {
			this.subscriptionClosed = true;
			try {
				subscriber.onComplete();
			}
			finally {
				close();
			}
		}

		private void doOnError(Throwable t) {
			this.subscriptionClosed = true;
			try {
				subscriber.onError(t);
			}
			finally {
				close();
			}
		}

		@Override
		public void handleEvent(StreamSourceChannel channel) {
			if (this.subscriptionClosed) {
				return;
			}

			try {
				ByteBuffer buffer = this.pooledBuffer.getBuffer();
				int count;
				do {
					count = channel.read(buffer);
					if (count == 0) {
						return;
					}
					else if (count == -1) {
						if (buffer.position() > 0) {
							doOnNext(Bytes.from(buffer));
						}
						doOnComplete();
					}
					else {
						if (buffer.remaining() == 0) {
							if (this.demand == 0) {
								channel.suspendReads();
							}
							doOnNext(Bytes.from(buffer));
							if (this.demand > 0) {
								scheduleNextMessage();
							}
							break;
						}
					}
				} while (count > 0);
			}
			catch (IOException e) {
				doOnError(e);
			}
		}
	}

}
