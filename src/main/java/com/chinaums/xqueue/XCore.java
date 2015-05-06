package com.chinaums.xqueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XCore {
	private static final String SEP = "|*^*|";

	private static Logger log = LoggerFactory.getLogger(XCore.class);

	private int queueSize;
	private int dispatcherThreads;
	private Map<String, String> authKeys;

	private ArrayBlockingQueue<XQueueMessage> queue;

	private Map<IoSession, Client> clientMap = new ConcurrentHashMap<IoSession, Client>();
	// 由于连接数不会很多，就不用太复杂的数据结构了，每次遍历所有连接。
	private Map<String, Client> topicMap = new ConcurrentHashMap<String, Client>();

	private volatile boolean stop = false;
	private List<Dispatcher> dispatchers;

	private class Client {
		public String topic;
		public String clientId;
		public IoSession session;

		public Client(String topic, String clientId, IoSession session) {
			super();
			this.topic = topic;
			this.clientId = clientId;
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
			Map<String, String> authKeys) {
		this.queueSize = queueSize;
		this.dispatcherThreads = dispatcherThreads;
		this.authKeys = authKeys;

		queue = new ArrayBlockingQueue<XQueueMessage>(this.queueSize);
		dispatchers = new ArrayList<XCore.Dispatcher>(dispatcherThreads);
	}

	public void start() {
		for (int i = 0; i < this.dispatcherThreads; i++) {
			Dispatcher d = new Dispatcher();
			dispatchers.add(d);
			d.start();
		}
	}

	public void stop() {
		stop = true;
		for (Dispatcher d : dispatchers) {
			d.interrupt();
		}
	}

	public boolean authenticate(String challenge, XQueueChallengeResponse msg) {
		String key = authKeys.get(msg.getSystemId());
		if (key == null)
			return false;
		try {
			return RSAUtil.doCheck(challenge.getBytes(), msg.getSignature(),
					key);
		} catch (Exception e) {
			log.error("验签错误", e);
			return false;
		}
	}

	public void addSession(IoSession session, XQueueChallengeResponse m) {
		Client client = new Client(m.getSubscribeTopic(), m.getClientId(),
				session);
		clientMap.put(session, client);

		String key = client.topic + SEP + client.clientId;
		if (!topicMap.containsKey(key)) {
			topicMap.put(key, client);
		}
	}

	public void removeSession(IoSession session) {
		Client client = clientMap.remove(session);
		if (client == null)
			return;

		String key = client.topic + SEP + client.clientId;
		topicMap.remove(key);

		// find other client with same topic and clientid
		for (Client c : clientMap.values()) {
			if (c.topic.equals(client.topic)
					&& c.clientId.equals(client.clientId)) {
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
					if (c != null)
						c.session.write(msg);
				}
			} catch (Exception e) {
				log.warn("发送消息错误: " + e.getMessage());
			}
		}
	}
}
