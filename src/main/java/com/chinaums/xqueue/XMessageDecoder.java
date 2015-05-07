package com.chinaums.xqueue;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * 
 * @author 焕文
 * 
 */
class XMessageDecoder extends CumulativeProtocolDecoder {

	/**
	 * 处理XQueueAuthResponse, XQueueMessageAck消息
	 */
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		Integer length = (Integer) session.getAttribute("MSG_LEN");

		if (length == null) {
			if (in.remaining() < 4) {
				return false;
			} else {
				length = in.getInt();
				if (length > 10 * 1024) {
					throw new Exception("报文过长");
				}
			}
		}
		if (in.remaining() < length) {
			session.setAttribute("MSG_LEN", length);
			return false;
		} else {
			session.removeAttribute("MSG_LEN");
			
			byte[] data = new byte[length];
			in.get(data);
			
			ByteBuffer buf = ByteBuffer.wrap(data);

			byte type = buf.get();
			switch (type) {
			case XMessage.ACK:
				XQueueMessageAck ack = parseAck(buf);
				out.write(ack);
				return true;
			case XMessage.RESP:
				XQueueChallengeResponse resp = parseResponse(buf);
				out.write(resp);
				return true;
			default:
				throw new Exception("错误的消息类型");
			}
		}
	}

	private XQueueMessageAck parseAck(ByteBuffer in) throws Exception {
		parseHeader(in);

		XQueueMessageAck msg = new XQueueMessageAck();
		return msg;
	}

	private XQueueChallengeResponse parseResponse(ByteBuffer in) throws Exception {
		HashMap<String, String> header = parseHeader(in);

		XQueueChallengeResponse msg = new XQueueChallengeResponse();
		msg.setSystemId(header.get("systemId"));
		msg.setClientId(header.get("clientId"));
		msg.setSubscribeTopic(header.get("subscribeTopic"));
		int contentLength = Integer.parseInt(header.get("contentLength"));

		byte[] content = new byte[contentLength];
		in.get(content);
		msg.setSignature(content);
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

}
