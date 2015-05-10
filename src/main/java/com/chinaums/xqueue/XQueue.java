package com.chinaums.xqueue;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XQueue消息服务。
 * 
 * @author 焕文
 *
 */
public class XQueue {
	private static Logger log = LoggerFactory.getLogger(XQueue.class);

	private int port;
	private int queueSize = 2048;
	private int dispatcherThreads = 16;
	private Map<String, String> authKeys = new HashMap<String, String>();

	private XCore core;

	private volatile boolean stop = true;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	public XQueue() {

	}

	/**
	 * 构造服务
	 * 
	 * @param port
	 *            服务监听端口
	 */
	public XQueue(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	/**
	 * 监听端口
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public Map<String, String> getAuthKeys() {
		return authKeys;
	}

	/**
	 * 设置认证公钥，systemId:公钥
	 * 
	 * @param authKeys
	 */
	public void setAuthKeys(Map<String, String> authKeys) {
		this.authKeys = authKeys;
	}

	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * 发送队列长度，默认2048。队列满了后，新消息会被丢弃。
	 * 
	 * @param queueSize
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getDispatcherThreads() {
		return dispatcherThreads;
	}

	/**
	 * 消息分发线程数，默认16。
	 * 
	 * @param dispatcherThreads
	 */
	public void setDispatcherThreads(int dispatcherThreads) {
		this.dispatcherThreads = dispatcherThreads;
	}

	/**
	 * 开始服务。
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		if (!stop)
			return;

		stop = false;

		log.info("开始启动XQueue");
		core = new XCore(queueSize, dispatcherThreads, authKeys);
		core.start();

		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new XMessageEncoder(),
								new XMessageDecoder(), new ClientHandler(core));
					}
				}).option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		b.bind(port).sync();

		log.info("XQueue启动完毕，监听端口：" + port);
	}

	/**
	 * 停止服务。
	 */
	public void stop() {
		stop = true;

		core.stop();

		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
	}

	/**
	 * 发送消息到指定TOPIC。
	 * 
	 * @param topic
	 * @param content
	 * @return 是否发送成功，这里成功指的是进入发送队列，不保证到达。
	 * @throws Exception
	 */
	public boolean send(String topic, byte[] content) throws Exception {
		return send(new XQueueMessage(topic, content));
	}

	/**
	 * 发送消息。
	 * 
	 * @param msg
	 * @return
	 * @throws Exception
	 */
	public boolean send(XQueueMessage msg) throws Exception {
		return core.send(msg);
	}
}
