package com.chinaums.xqueue;

/**
 * 消息回调接口
 * @author 焕文
 *
 */
public interface XQueueListener {
	/**
	 * 收到消息后被调用。
	 * @param msg
	 * @throws Exception
	 */
	public void onMessage(XQueueMessage msg) throws Exception;
}
