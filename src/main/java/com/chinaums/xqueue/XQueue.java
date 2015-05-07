package com.chinaums.xqueue;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
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
	private ExecutorService executor;
	
	private XCore core;

	private volatile boolean stop = true;

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

		acceptor = new NioSocketAcceptor(Runtime.getRuntime()
				.availableProcessors() + 1);
		acceptor.setReuseAddress(false);

		acceptor.getFilterChain().addLast(
				"protocol",
				new ProtocolCodecFilter(new XMessageEncoder(),
						new XMessageDecoder()));

		// 用ExecutorFilter来实现线程
		executor = new OrderedThreadPoolExecutor(Runtime.getRuntime()
				.availableProcessors() + 1);
		// 由于codecFilter内部有锁，无法并发，所以要把executor放到codec后面
		acceptor.getFilterChain().addLast("ThreadPool",
				new ExecutorFilter(executor));

		acceptor.setHandler(new ConnHandler(core));

		// 心跳，30秒
		acceptor.getSessionConfig().setIdleTime(IdleStatus.WRITER_IDLE, 30);

		acceptor.bind(new InetSocketAddress(port));
		log.info("XQueue启动完毕，监听端口：" + port);
	}

	/**
	 * 停止服务。
	 */
	public void stop() {
		stop = true;

		core.stop();
		acceptor.unbind();
		acceptor.dispose();
		executor.shutdownNow();
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
