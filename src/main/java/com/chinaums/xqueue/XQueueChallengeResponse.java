package com.chinaums.xqueue;

class XQueueChallengeResponse extends XMessage {
	private String systemId;
	private byte[] signature;
	private String clientId;
	private String subscribeTopic;

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getSubscribeTopic() {
		return subscribeTopic;
	}

	public void setSubscribeTopic(String subscribeTopic) {
		this.subscribeTopic = subscribeTopic;
	}
}
