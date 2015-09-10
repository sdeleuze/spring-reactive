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

package org.springframework.reactive.web.http.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import reactor.rx.broadcast.Broadcaster;

import org.springframework.reactive.web.http.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * Conversion between Netty types ({@link HttpRequest}, {@link HttpResponse}, {@link HttpContent} and {@link LastHttpContent})
 * and Spring Reactive types.
 *
 * @author Sebastien Deleuze
 */
public class NettyServerCodecHandlerAdapter extends ChannelDuplexHandler {

	private ServerHttpRequest request;
	private Broadcaster<ByteBuf> requestContent;

	public NettyServerCodecHandlerAdapter() {
		this.requestContent = Broadcaster.create();
	}

	/**
	 * Create a {@link NettyServerHttpRequest} when a {@link HttpRequest} is received, and use
	 * a {@link Broadcaster} to send the content as a request stream.
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Class<?> messageClass = msg.getClass();
		if (HttpRequest.class.isAssignableFrom(messageClass)) {
			this.request = new NettyServerHttpRequest((HttpRequest) msg, this.requestContent);
			super.channelRead(ctx, this.request);
		} else if (HttpContent.class.isAssignableFrom(messageClass)) {
			Assert.notNull(this.request);
			ByteBuf content = ((ByteBufHolder) msg).content();
			this.requestContent.onNext(content);
			if (LastHttpContent.class.isAssignableFrom(messageClass)) {
				this.requestContent.onComplete();
			}
		} else {
			super.channelRead(ctx, msg);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		Class<?> messageClass = msg.getClass();

		if (HttpResponse.class.isAssignableFrom(messageClass)) {
			super.write(ctx, msg, promise);
		} else if (ByteBuf.class.isAssignableFrom(messageClass)) {
			super.write(ctx, new DefaultHttpContent((ByteBuf)msg), promise);
		} else {
			super.write(ctx, msg, promise); // pass through, since we do not understand this message.
		}

	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		super.channelReadComplete(ctx);
		ctx.pipeline().flush(); // If there is nothing to flush, this is a short-circuit in netty.
	}

}
