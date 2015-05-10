package com.chinaums.xqueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

/**
 * 处理XQueueAuthResponse, XQueueMessageAck消息
 */
class XMessageDecoder extends ByteToMessageDecoder {
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		if (in.readableBytes() < 4) {
			return;
		}

		in.markReaderIndex();
		int length = in.readInt();
		if (in.readableBytes() < length) {
			in.resetReaderIndex();
			return;
		}

		if (length > 10 * 1024) {
			throw new Exception("报文过长");
		}

		byte[] data = new byte[length];
		in.readBytes(data);

		ByteBuffer buf = ByteBuffer.wrap(data);

		byte type = buf.get();
		switch (type) {
		case XMessage.ACK:
			XQueueMessageAck ack = parseAck(buf);
			out.add(ack);
			return;
		case XMessage.RESP:
			XQueueChallengeResponse resp = parseResponse(buf);
			out.add(resp);
			return;
		default:
			throw new Exception("错误的消息类型");
		}
	}

	private XQueueMessageAck parseAck(ByteBuffer in) throws Exception {
		parseHeader(in);

		XQueueMessageAck msg = new XQueueMessageAck();
		return msg;
	}

	private XQueueChallengeResponse parseResponse(ByteBuffer in)
			throws Exception {
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
