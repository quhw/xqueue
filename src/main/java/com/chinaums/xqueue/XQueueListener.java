package com.chinaums.xqueue;

public interface XQueueListener {
	public void onMessage(XQueueMessage msg) throws Exception;
	/**
	 * 构造服务
	 * @param hosts 格式：host1:port1,host2:port2,host3,port3
	 */
}
