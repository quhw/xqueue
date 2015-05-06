package com.chinaums.xqueue;

class XQueueChallengeFinish extends XMessage {
	private String status;

	public XQueueChallengeFinish(String status) {
		super();
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}


}
