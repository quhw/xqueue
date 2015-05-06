package com.chinaums.xqueue;

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
	 * 处理XQueueAuthResponse消息
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

			byte type = in.get();
			if (type != XMessage.RESP) {
				throw new Exception("错误的消息类型");
			}

			XQueueChallengeResponse msg = new XQueueChallengeResponse();

			String line;
			int contentLength = 0;

			while (true) {
				line = nextLine(in);
				if (line == null)
					throw new Exception("错误的消息格式");

				if (line.length() == 0) {
					byte[] content = new byte[contentLength];
					in.get(content);
					msg.setSignature(content);
					out.write(msg);
					return true;
				}

				int pos = line.indexOf(':');
				if (pos > 0 && pos < line.length() - 1) {
					String key = line.substring(0, pos).trim();
					String value = line.substring(pos + 1).trim();
					if ("systemId".equalsIgnoreCase(key)) {
						msg.setSystemId(value);
					} else if ("clientId".equalsIgnoreCase(key)) {
						msg.setClientId(value);
					} else if ("subscribeTopic".equalsIgnoreCase(key)) {
						msg.setSubscribeTopic(value);
					} else if ("contentLength".equalsIgnoreCase(key)) {
						contentLength = Integer.parseInt(value);
					}
				}
			}
		}
	}

	private String nextLine(IoBuffer in) throws Exception {
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

}
