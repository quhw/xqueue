package com.chinaums.xqueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XMessageEncoder extends MessageToByteEncoder<XMessage> {
	private static Logger log = LoggerFactory.getLogger(XMessageEncoder.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, XMessage message,
			ByteBuf out) throws Exception {

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

		out.writeInt(1 + head.length + content.length);
		out.writeByte(type);
		out.writeBytes(head);
		out.writeBytes(content);
	}

}
