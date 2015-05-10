package com.chinaums.xqueue;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends ChannelInboundHandlerAdapter {
	private static Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private XCore core;

	private String challenge;

	public ClientHandler(XCore core) {
		this.core = core;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// 握手
		log.info("收到连接请求：{}", ctx.channel().remoteAddress());
		XQueueChallengeRequest msg = new XQueueChallengeRequest();
		challenge = UUID.randomUUID().toString();
		msg.setChallenge(challenge);
		ctx.writeAndFlush(msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("连接已关闭：{}", ctx.channel().remoteAddress());
		core.removeSession(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object message)
			throws Exception {
		if (message instanceof XQueueMessageAck) {
		} else if (message instanceof XQueueChallengeResponse) {
			log.info("收到认证应答：" + ctx.channel().remoteAddress());
			XQueueChallengeResponse m = (XQueueChallengeResponse) message;
			if (core.authenticate(challenge, m)) {
				ctx.writeAndFlush(new XQueueChallengeFinish("OK"));
				core.addSession(ctx, m);
				log.info("认证通过：{}", ctx.channel().remoteAddress());
			} else {
				ctx.writeAndFlush(
						new XQueueChallengeFinish("Authentication fail."))
						.addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future)
									throws Exception {
								future.channel().close();
							}
						});
				log.warn("认证失败：{}", ctx.channel().remoteAddress());
			}
		} else {
			log.warn("收到非法请求报文：" + ctx.channel().remoteAddress());
			ctx.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.error("异常: " + ctx.channel().remoteAddress(), cause);
		ctx.close();
	}

}
