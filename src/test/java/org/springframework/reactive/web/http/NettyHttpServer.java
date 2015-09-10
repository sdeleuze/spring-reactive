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
package org.springframework.reactive.web.http;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.rx.Streams;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.reactive.web.http.netty.NettyServerCodecHandlerAdapter;
import org.springframework.reactive.web.http.netty.NettyServerHttpResponse;
import org.springframework.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class NettyHttpServer extends HttpServerSupport implements InitializingBean, HttpServer {

	private final ServerBootstrap bootstrap;
	private enum ServerState {Starting, Started, Shutdown}
	private final AtomicReference<ServerState> serverStateRef;
	private ChannelFuture bindFuture;

	public NettyHttpServer() {
		this.serverStateRef = new AtomicReference<ServerState>(ServerState.Shutdown);
		this.bootstrap = new ServerBootstrap();
		init();
	}

	private void init() {
		this.bootstrap
					.option(ChannelOption.SO_KEEPALIVE, true)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.group(new NioEventLoopGroup())
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<Channel>() {
						@Override
						protected void initChannel(Channel ch) throws Exception {
							ch.pipeline()
									.addLast(new LoggingHandler())
									.addLast(new HttpServerCodec())
									.addLast(new NettyServerCodecHandlerAdapter())
									.addLast(new ChannelInboundHandlerAdapter() {

										@Override
										public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
											if (msg instanceof ServerHttpRequest) {
												handleRequest(ctx, (ServerHttpRequest) msg);
											}
											else {
												super.channelRead(ctx, msg);
											}
										}

										private void handleRequest(ChannelHandlerContext ctx, ServerHttpRequest request) {
											HttpHandler httpHandler = getHttpHandler();
											Assert.notNull(httpHandler);

											NettyServerHttpResponse response = new NettyServerHttpResponse(ctx);

											Streams.wrap(httpHandler.handle(request, response)).observeError(Exception.class, (o, ex) -> ex.printStackTrace()).observeComplete(v -> {
												if (!response.isHeadersWritten()) {
													response.writeWith(Streams.empty());
												}
											}).consume();
										}
									});
						}
					});
	}


	@Override
	public boolean isRunning() {
		ServerState serverState = serverStateRef.get();
		return (serverState == ServerState.Started) || (serverState == ServerState.Starting);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}


	@Override
	public void start() {
		if (!serverStateRef.compareAndSet(ServerState.Shutdown, ServerState.Starting)) {
			throw new IllegalStateException("Server already started");
		}

		this.bindFuture = this.bootstrap.bind(getPort());
		this.bindFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				if(channelFuture.isSuccess()) {
					serverStateRef.set(ServerState.Started);
				}
			}
		});
		this.bindFuture.awaitUninterruptibly();

	}

	@Override
	public void stop() {
		if (!(serverStateRef.get() == ServerState.Started)) {
			throw new IllegalStateException("The server is already shutdown.");
		}
		ChannelFuture closeFuture = this.bindFuture.channel().close();
		closeFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				if(channelFuture.isSuccess()) {
					serverStateRef.set(ServerState.Shutdown);
				}
			}
		});
		closeFuture.awaitUninterruptibly();
	}

}
