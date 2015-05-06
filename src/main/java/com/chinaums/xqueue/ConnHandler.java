package com.chinaums.xqueue;

import java.util.UUID;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConnHandler implements IoHandler {
	private static Logger log = LoggerFactory.getLogger(ConnHandler.class);
	private XCore core;

	public ConnHandler(XCore core) {
		this.core = core;
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		// 握手
		log.info("收到连接请求：" + session.getRemoteAddress());
		XQueueChallengeRequest msg = new XQueueChallengeRequest();
		String challenge = UUID.randomUUID().toString();
		msg.setChallenge(challenge);
		session.setAttribute("CHALLENGE", challenge);
		session.write(msg);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		core.removeSession(session);
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
			throws Exception {
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		log.error("Uncaught exception", cause);
		session.close(false);
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		if (message instanceof XQueueChallengeResponse) {
			log.info("收到认证应答：" + session.getRemoteAddress());
			XQueueChallengeResponse m = (XQueueChallengeResponse) message;
			String challenge = (String) session.getAttribute("CHALLENGE");
			if (core.authenticate(challenge, m)) {
				log.info("认证通过");
				session.write(new XQueueChallengeFinish("OK"));
				core.addSession(session, m);
			} else {
				log.warn("认证失败");
				session.write(new XQueueChallengeFinish("Authentication fail."));
				session.close(false);
			}
		} else {
			log.warn("收到非法请求报文：" + session.getRemoteAddress());
			session.close(false);
		}
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
	}

	@Override
	public void inputClosed(IoSession session) throws Exception {
	}

}
