package com.chinaums.xqueue;

abstract class XMessage {
	public static final byte REQ = 1;
	public static final byte RESP = 2;
	public static final byte FIN = 3;
	public static final byte MSG = 4;
	public static final byte ACK = 5;

}
