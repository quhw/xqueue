package com.chinaums.xqueue;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
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

	private SocketAcceptor acceptor;
	
	private XCore core;

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

	public void setPort(int port) {
		this.port = port;
	}

	public Map<String, String> getAuthKeys() {
		return authKeys;
	}

	public void setAuthKeys(Map<String, String> authKeys) {
		this.authKeys = authKeys;
	}

	public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getDispatcherThreads() {
		return dispatcherThreads;
	}

	public void setDispatcherThreads(int dispatcherThreads) {
		this.dispatcherThreads = dispatcherThreads;
	}

	public void start() throws Exception {
		log.info("开始启动XQueue");
		core = new XCore(queueSize, dispatcherThreads, authKeys);
		core.start();

		acceptor = new NioSocketAcceptor(Runtime.getRuntime()
				.availableProcessors() + 1);
		acceptor.setReuseAddress(true);

		acceptor.getFilterChain().addLast(
				"protocol",
				new ProtocolCodecFilter(new XMessageEncoder(),
						new XMessageDecoder()));

		// 这里不用executor thread来接受客户端连接，没有必要。

		acceptor.setHandler(new ConnHandler(core));

		acceptor.bind(new InetSocketAddress(port));
		log.info("XQueue启动完毕，监听端口：" + port);
	}

	public void stop() {
		core.stop();
		acceptor.unbind();
		acceptor.dispose();
	}

	public boolean send(String topic, byte[] content) throws Exception {
		return send(new XQueueMessage(topic, content));
	}

	public boolean send(XQueueMessage msg) throws Exception {
		return core.send(msg);
	}
}
