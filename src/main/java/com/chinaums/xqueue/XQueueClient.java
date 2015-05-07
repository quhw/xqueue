package com.chinaums.xqueue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端。
 * 
 * @author 焕文
 *
 */
public class XQueueClient {
	private static Logger log = LoggerFactory.getLogger(XQueueClient.class);
	private SocketAddress[] hostAddr;

	private String subscribeTopic = "default";
	private String clientId;
	private String privateKey;
	private String systemId;
	private int queueSize = 2048;
	private int workerPoolSize = 1;

	private ArrayBlockingQueue<XQueueMessage> queue;

	private List<WorkerThread> workerPool;

	private XQueueListener listener;

	private volatile boolean stop = true;

	private Receiver receiver;

	private int nextHost = 0;

	private Socket sock;

	public XQueueClient() {

	}

	/**
	 * 构造函数
	 * 
	 * @param hosts
	 *            服务器地址列表，host1:port1,host2:port2,...
	 */
	public XQueueClient(String hosts) {
		setHosts(hosts);
	}

	/**
	 * 服务器地址。
	 * 
	 * @param hosts
	 *            服务器地址列表，host1:port1,host2:port2,...
	 */
	public void setHosts(String hosts) {
		String[] hostArr = hosts.split(",");
		hostAddr = new SocketAddress[hostArr.length];
		for (int i = 0; i < hostArr.length; i++) {
			String[] strs = hostArr[i].split(":");
			hostAddr[i] = new InetSocketAddress(strs[0],
					Integer.parseInt(strs[1]));
		}
	}

	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * 接收队列长度，默认2048，超过长度的消息会被丢弃。
	 * 
	 * @param queueSize
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	/**
	 * 工作线程数，默认1，用于并发的回调XQueueListener。
	 * 
	 * @return
	 */
	public int getWorkerPoolSize() {
		return workerPoolSize;
	}

	public void setWorkerPoolSize(int workerPoolSize) {
		this.workerPoolSize = workerPoolSize;
	}

	public String getSubscribeTopic() {
		return subscribeTopic;
	}

	/**
	 * 订阅的TOPIC，不能有*号。
	 * 
	 * @param subscribeTopic
	 */
	public void setSubscribeTopic(String subscribeTopic) {
		this.subscribeTopic = subscribeTopic;
	}

	public String getSystemId() {
		return systemId;
	}

	/**
	 * 客户端系统标识，认证按照systemId进行认证。
	 * 
	 * @param systemId
	 */
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getClientId() {
		return clientId;
	}

	/**
	 * 客户端ID，如果有多个相同的systemId+clientId订阅同一topic， 则只有一个客户端能收到消息，保证集群内消息不会被重复处理。
	 * 
	 * @param clientId
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	/**
	 * 认证私钥，PKCS8格式，hexstring。
	 * 
	 * @param privateKey
	 */
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public XQueueListener getListener() {
		return listener;
	}

	/**
	 * 消息处理接口。
	 * 
	 * @param listener
	 */
	public void setListener(XQueueListener listener) {
		this.listener = listener;
	}

	/**
	 * 开始接收。
	 */
	public void start() {
		if (!stop)
			return;

		stop = false;

		queue = new ArrayBlockingQueue<XQueueMessage>(queueSize);

		workerPool = new ArrayList<XQueueClient.WorkerThread>(workerPoolSize);
		for (int i = 0; i < workerPoolSize; i++) {
			WorkerThread wt = new WorkerThread();
			workerPool.add(wt);
			wt.start();
		}

		receiver = new Receiver();
		receiver.start();
	}

	/**
	 * 停止接收。
	 */
	public void stop() {
		if (stop)
			return;

		stop = true;
		receiver.interrupt();
		disconnect();
		for (WorkerThread wt : workerPool) {
			wt.interrupt();
		}
	}

	private class Receiver extends Thread {
		public void run() {
			while (!stop) {
				connect();
				if (sock == null)
					break;

				try {
					handshake();
					receive();
				} catch (Exception e) {
					log.error("有错误", e);
					disconnect();
					log.info("5秒后重新连接");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
					}
				}
			}
		}
	}

	private void handshake() throws Exception {
		InputStream is = sock.getInputStream();
		XMessage msg = readMessage(is);
		if (!(msg instanceof XQueueChallengeRequest)) {
			throw new Exception("Not receive a challenge");
		}
		String challenge = ((XQueueChallengeRequest) msg).getChallenge();
		byte[] signature = sign(challenge);

		XQueueChallengeResponse resp = new XQueueChallengeResponse();
		resp.setClientId(clientId);
		resp.setSubscribeTopic(subscribeTopic);
		resp.setSystemId(systemId);
		resp.setSignature(signature);

		OutputStream os = sock.getOutputStream();
		writeMessage(os, resp);

		msg = readMessage(is);
		if (!(msg instanceof XQueueChallengeFinish)) {
			throw new Exception("Not receive a challenge finish");
		}
		String status = ((XQueueChallengeFinish) msg).getStatus();

		if (!"OK".equals(status)) {
			throw new Exception("Challenge fail: " + status);
		}
	}

	private void writeMessage(OutputStream os, XMessage msg) throws Exception {
		DataOutputStream dos = new DataOutputStream(os);

		byte type;
		byte[] content;
		StringBuffer buf = new StringBuffer();

		if (msg instanceof XQueueMessageAck) {
			type = XMessage.ACK;
			buf.append("\n");
			content = new byte[0];
		} else if (msg instanceof XQueueChallengeResponse) {
			XQueueChallengeResponse m = (XQueueChallengeResponse) msg;
			type = XMessage.RESP;
			buf.append("systemId:");
			buf.append(m.getSystemId());
			buf.append("\n");
			buf.append("clientId:");
			buf.append(m.getClientId());
			buf.append("\n");
			buf.append("subscribeTopic:");
			buf.append(m.getSubscribeTopic());
			buf.append("\n");
			buf.append("contentLength:");
			buf.append("" + m.getSignature().length);
			buf.append("\n");
			buf.append("\n");
			content = m.getSignature();
		} else {
			throw new Exception("Oops!");
		}

		byte[] head = buf.toString().getBytes("UTF-8");

		dos.writeInt(1 + head.length + content.length);
		dos.write(type);
		dos.write(head);
		dos.write(content);
		dos.flush();
	}

	private byte[] sign(String challenge) throws Exception {
		return RSAUtil.sign(challenge.getBytes(), privateKey);
	}

	private void receive() throws Exception {
		XQueueMessageAck ackMsg = new XQueueMessageAck();

		InputStream is = sock.getInputStream();
		OutputStream os = sock.getOutputStream();
		while (!stop) {
			try {
				XMessage msg = readMessage(is);
				if (!(msg instanceof XQueueMessage)) {
					throw new Exception("Not receive a message");
				}

				XQueueMessage m = (XQueueMessage) msg;
				queue.offer(m);

				writeMessage(os, ackMsg);
			} catch (SocketTimeoutException e) {
				writeMessage(os, ackMsg);
			}
		}
	}

	private void connect() {
		while (!stop) {
			SocketAddress addr = hostAddr[nextHost];
			log.info("连接服务端：" + addr);
			try {
				sock = new Socket();
				sock.connect(addr, 30000);
//				sock.setSoTimeout(10000); // 10秒读取超时，发送心跳。
				log.info("成功连接服务端");
				return;
			} catch (Exception e) {
				sock = null;
				log.warn("无法连接服务端：" + addr);
				nextHost = (nextHost + 1) % hostAddr.length;
				log.info("5秒后重新尝试连接");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
				}
			}
		}
	}

	private void disconnect() {
		try {
			if (sock != null) {
				sock.close();
			}
		} catch (Exception e) {
		}
	}

	private XMessage readMessage(InputStream is) throws Exception {
		DataInputStream dis = new DataInputStream(is);
		int length = dis.readInt();
		if (length > 64 * 1024) {
			throw new Exception("Too large message");
		}

		byte[] data = new byte[length];
		int read = 0;
		while (read < length) {
			read += dis.read(data, read, length - read);
		}

		ByteBuffer buf = ByteBuffer.wrap(data);

		byte type = buf.get();
		switch (type) {
		case XMessage.MSG:
			return parseMessage(buf);
		case XMessage.REQ:
			return parseChallenge(buf);
		case XMessage.FIN:
			return parseFin(buf);
		default:
			throw new Exception("Bad message format");
		}
	}

	private XMessage parseFin(ByteBuffer buf) throws Exception {
		HashMap<String, String> map = parseHeader(buf);
		return new XQueueChallengeFinish(map.get("status"));
	}

	private XMessage parseChallenge(ByteBuffer buf) throws Exception {
		HashMap<String, String> map = parseHeader(buf);
		XQueueChallengeRequest msg = new XQueueChallengeRequest();
		msg.setChallenge(map.get("challenge"));
		return msg;
	}

	private XMessage parseMessage(ByteBuffer buf) throws Exception {
		HashMap<String, String> map = parseHeader(buf);
		XQueueMessage msg = new XQueueMessage();
		String len = map.get("contentLength");
		if (len == null)
			throw new Exception("Bad message");
		byte[] content = new byte[Integer.parseInt(len)];
		buf.get(content);
		msg.setContent(content);
		msg.setTopic(map.get("topic"));
		msg.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
				.parse(map.get("timestamp")));

		for (String key : map.keySet()) {
			if (key.startsWith("p_")) {
				String v = map.get(key);
				String p = key.substring(2);
				msg.addProperty(p, v);
			}
		}

		return msg;
	}

	private String nextLine(ByteBuffer in) throws Exception {
		int start = in.position();
		int current = start;
		int limit = in.limit();
		while (current < limit) {
			if (current - start > 256) {
				throw new Exception("报文头过长");
			}

			byte b = in.get();
			current++;
			if (b == '\n') {
				byte[] a = new byte[current - start];
				in.position(start);
				in.get(a);
				if (a.length >= 2 && a[a.length - 2] == '\r') {
					return new String(a, 0, a.length - 2);
				} else {
					return new String(a, 0, a.length - 1);
				}
			}
		}
		in.position(start);
		return null;
	}

	private HashMap<String, String> parseHeader(ByteBuffer in) throws Exception {
		String line;
		HashMap<String, String> map = new HashMap<String, String>();

		while (true) {
			line = nextLine(in);
			if (line == null)
				throw new Exception("错误的消息格式");

			if (line.length() == 0) {
				return map;
			}

			int pos = line.indexOf(':');
			if (pos > 0 && pos < line.length() - 1) {
				String key = line.substring(0, pos).trim();
				String value = line.substring(pos + 1).trim();
				map.put(key, value);
			}
		}
	}

	private class WorkerThread extends Thread {
		public void run() {
			while (!stop) {
				XQueueMessage msg;
				try {
					msg = queue.take();
					try {
						listener.onMessage(msg);
					} catch (Throwable t) {
						log.error("Uncaught exception: ", t);
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}
}
