package com.chinaums.xqueue;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XCore {
	private static final String SEP = "|*^*|";

	private static Logger log = LoggerFactory.getLogger(XCore.class);

	private int queueSize;
	private int dispatcherThreads;
	private Map<String, String> authKeys;
	private Map<String, Set<String>> authTopics;

	private ArrayBlockingQueue<XQueueMessage> queue;

	private Map<ChannelHandlerContext, Client> clientMap = new ConcurrentHashMap<ChannelHandlerContext, Client>();
	// 由于连接数不会很多，就不用太复杂的数据结构了，每次遍历所有连接。
	private Map<String, Client> topicMap = new ConcurrentHashMap<String, Client>();

	private volatile boolean stop = true;
	private List<Dispatcher> dispatchers;

	private class Client {
		public String topic;
		public String systemId;
		public String clientId;
		public ChannelHandlerContext session;

		public Client(String topic, String clientId, String systemId,
				ChannelHandlerContext session) {
			super();
			this.topic = topic;
			this.clientId = clientId;
			this.systemId = systemId;
			this.session = session;
		}

	}

	private class Dispatcher extends Thread {
		public void run() {
			while (!stop) {
				try {
					XQueueMessage msg = queue.take();
					dispatchMessage(msg);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public XCore(int queueSize, int dispatcherThreads,
			Map<String, String> authKeys, Map<String, Set<String>> authTopics) {
		this.queueSize = queueSize;
		this.dispatcherThreads = dispatcherThreads;
		this.authKeys = authKeys;
		this.authTopics = authTopics;

		queue = new ArrayBlockingQueue<XQueueMessage>(this.queueSize);
		dispatchers = new ArrayList<XCore.Dispatcher>(dispatcherThreads);
	}

	public void start() {
		if (!stop)
			return;

		stop = false;

		for (int i = 0; i < this.dispatcherThreads; i++) {
			Dispatcher d = new Dispatcher();
			d.setName("XQueue Dispatcher " + i);
			dispatchers.add(d);
			d.start();
		}
	}

	public void stop() {
		if (stop)
			return;

		stop = true;
		for (Dispatcher d : dispatchers) {
			d.interrupt();
		}
	}

	public boolean authenticate(String systemId, String challenge,
			byte[] signature) {
		String key = authKeys.get(systemId);
		if (key == null)
			return false;
		try {
			return RSAUtil.doCheck(challenge.getBytes(), signature, key);
		} catch (Exception e) {
			log.error("验签错误", e);
			return false;
		}
	}

	public boolean authorize(String systemId, String topic) {
		Set<String> topics = authTopics.get(systemId);
		if (topics != null) {
			return topics.contains(topic);
		}
		return false;
	}

	public void addSession(ChannelHandlerContext session,
			XQueueChallengeResponse m) {
		Client client = new Client(m.getSubscribeTopic(), m.getClientId(),
				m.getSystemId(), session);
		clientMap.put(session, client);

		String key = client.topic + SEP + client.clientId + SEP
				+ client.systemId;
		if (!topicMap.containsKey(key)) {
			topicMap.put(key, client);
		}
	}

	public void removeSession(ChannelHandlerContext session) {
		Client client = clientMap.remove(session);
		if (client == null)
			return;

		String key = client.topic + SEP + client.clientId + SEP
				+ client.systemId;
		topicMap.remove(key);

		// find other client with same topic and clientid+systemId
		for (Client c : clientMap.values()) {
			if (c.topic.equals(client.topic)
					&& c.clientId.equals(client.clientId)
					&& c.systemId.equals(client.systemId)) {
				topicMap.put(key, c);
				break;
			}
		}
	}

	public boolean send(XQueueMessage msg) throws Exception {
		return queue.offer(msg);
	}

	private void dispatchMessage(XQueueMessage msg) {
		String topic = msg.getTopic();
		String p = topic + SEP;
		for (String key : topicMap.keySet()) {
			try {
				if (key.startsWith(p)) {
					Client c = topicMap.get(key);
					if (c != null && c.session.channel().isWritable()) {
						c.session.writeAndFlush(msg);
					}
				}
			} catch (Exception e) {
				log.warn("发送消息错误: " + e.getMessage());
			}
		}
	}
}
