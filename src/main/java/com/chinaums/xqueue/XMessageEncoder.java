package com.chinaums.xqueue;

import java.text.SimpleDateFormat;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * 
 * @author Huanwen Qu
 * 
 */
class XMessageEncoder implements ProtocolEncoder {

	@Override
	public void encode(IoSession session, Object message,
			ProtocolEncoderOutput out) throws Exception {
		byte type;
		byte[] content;
		StringBuffer buf = new StringBuffer();

		if (message instanceof XQueueChallengeRequest) {
			type = XMessage.REQ;
			buf.append("challenge:");
			buf.append(((XQueueChallengeRequest) message).getChallenge());
			buf.append("\n");
			buf.append("\n");
			content = new byte[0];
		} else if (message instanceof XQueueChallengeFinish) {
			type = XMessage.FIN;
			XQueueChallengeFinish m = (XQueueChallengeFinish) message;
			buf.append("status:");
			buf.append(m.getStatus());
			buf.append("\n");
			buf.append("\n");
			content = new byte[0];
		} else if (message instanceof XQueueMessage) {
			type = XMessage.MSG;
			XQueueMessage m = (XQueueMessage) message;
			buf.append("topic:");
			buf.append(m.getTopic());
			buf.append("\n");
			buf.append("timestamp:");
			buf.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(m
					.getTimestamp()));
			buf.append("\n");
			buf.append("contentLength: ");
			buf.append(m.getContent().length);
			buf.append("\n");
			for (String key : m.getProperties().keySet()) {
				buf.append("p_");
				buf.append(key);
				buf.append(":");
				buf.append(m.getProperty(key));
				buf.append("\n");
			}
			buf.append("\n");
			content = m.getContent();
		} else {
			throw new Exception("Oops!");
		}

		byte[] head = buf.toString().getBytes("UTF-8");

		IoBuffer buffer = IoBuffer.allocate(4 + 1 + head.length
				+ content.length);
		buffer.putInt(1 + head.length + content.length);
		buffer.put(type);
		buffer.put(head);
		buffer.put(content);
		buffer.flip();
		out.write(buffer);
	}

	@Override
	public void dispose(IoSession session) throws Exception {
	}
}
