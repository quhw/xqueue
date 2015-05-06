package com.chinaums.xqueue;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class XQueueMessage extends XMessage implements Serializable{
	private static final long serialVersionUID = -1265844461078088526L;

	private String topic = "default";
	private Date timestamp = new Date();
	private Map<String, String> properties = new HashMap<String, String>();
	private byte[] content = new byte[0];

	public XQueueMessage() {
	}

	public XQueueMessage(String topic, byte[] content) {
		this.topic = topic;
		this.content = content;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public void addProperty(String key, String value) {
		properties.put(key, value);
	}

	public String getProperty(String key){
		return properties.get(key);
	}
}
