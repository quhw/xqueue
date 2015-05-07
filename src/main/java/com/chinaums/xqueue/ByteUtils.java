package com.chinaums.xqueue;

import java.io.UnsupportedEncodingException;

class ByteUtils {

	static final String ENCODING_UTF_8 = "UTF-8";
	static final String ENCODING_GBK = "GBK";
	static final byte[] DEFAULT_0 = toByteArray(0);

	public static void main(String[] args) {
		byte[] b = intToByte(100);
		System.out.println(byteArray2HexString(b));
	}

	/**
	 * 把传入的数组转化为整型，数组长度为4
	 * 
	 * @param br
	 * @return
	 */
	public static int tol(byte[] br) {
		// int is = 0;
		// for (int i = 0; i < 4; i++) {
		// is += (br[i] & 0xFF) << (8 * i);
		// }
		// return is;
		return tol(br, 4);
	}

	public static int tol(byte[] br, int len) {
		int is = 0;
		if (br == null || br.length < len) {
			return -1;
		}
		for (int i = 0; i < len; i++) {
			is += (br[i] & 0xFF) << (8 * i);
		}

		return is;
	}

	/**
	 * 根据issource，生成一个长度为4的byte数组 此数组记录isource
	 * 
	 * @param isource
	 * @return
	 */
	public static byte[] toByteArray(int isource) {
		return toByteArray(isource, 4);
	}

	/**
	 * 根据issoirce，生成一个长度为len的字节数组
	 * 
	 * @param isource
	 * @param len
	 * @return
	 */
	public static byte[] toByteArray(int isource, int len) {
		byte[] bl = new byte[len];
		for (int i = 0; i < len; i++) {
			bl[i] = (byte) (isource >> 8 * i & 0xff);
		}
		return bl;
	}

	/**
	 * 拼接两个字符数组
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static byte[] revert(byte[] a, byte[] b) {
		if (a == null) {
			a = new byte[0];
		}
		if (b == null) {
			b = new byte[0];
		}
		byte[] result = new byte[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	/**
	 * @方法说明：根据读取的长度解析出内容
	 * @param pos
	 *            : 相对起始位置
	 * @param readLen
	 *            ：要读取的长度
	 * @param tempByte
	 *            ：原始字符组
	 * @return 显示后面跟随的内容
	 */
	public static byte[] getValue(int pos, int readLen, byte[] srcByte) {
		if (pos < 0) {
			return null;
		}
		if (srcByte == null) {
			return null;
		}
		if (readLen != -1) {
			if (srcByte.length - pos >= readLen) {
				byte[] tempBytes = new byte[readLen];
				System.arraycopy(srcByte, pos, tempBytes, 0, readLen);
				return tempBytes;
			}
		} else if (readLen == 0) {
			return "".getBytes();
		} else {
			if (srcByte.length - pos >= 4) {
				byte[] tempBytes = new byte[4];
				System.arraycopy(srcByte, pos, tempBytes, 0, 4);
				return tempBytes;
			}
		}
		return null;
	}

	public static String getValue2Str(int pos, int readLen, byte[] srcByte,
			String encoding) {
		String result = null;
		byte[] temp = getValue(pos, readLen, srcByte);
		try {
			if (temp != null) {
				if (encoding == null || "".equals(encoding)) {
					encoding = System.getProperty("file.encoding");
				}
				result = new String(temp, encoding);
			}
		} catch (UnsupportedEncodingException e) {
			return null;
		}
		return result;
	}

	/**
	 * 处理字符串,返回符号长度要求的字符串,不满长度的右补“ ” 当要处理字符串长度大于指定长度时,返回null
	 * 
	 * @param s
	 * @param length
	 * @return
	 */
	public static String dealAlpha(String s, int length) {
		try {
			if (s == null || "".equals(s)) {
				s = "";
				for (int j = 0; j < length; j++)
					s = s + " ";
			} else {
				if (s.length() > length) {
					return null;
				} else if (s.length() < length) {
					int rest = length - s.length();
					for (int i = 0; i < rest; i++) {
						s = s.concat(" ");
					}
					return s;
				} else {
					return s;
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return s;
	}

	/**
	 * 处理byte数组,返回符号长度要求的byte数组,不满长度的补0
	 * 
	 * @param by
	 * @param length
	 * @return
	 */
	public static byte[] byteDealAlpha(byte[] by, int length) {
		if (by == null) {
			by = new byte[0];
			return by;
		}
		// 保证数组是8的倍数
		if (length % 8 != 0) {
			int num = 8 - length % 8;
			byte[] a = new byte[num];
			for (int i = 0; i < num; i++) {
				a[i] = (byte) num;
			}
			by = revert(by, a);
		}

		return by;
	}

	public static byte[] getLenAndValue(String value) {
		return getLenAndValue(value, null);
	}

	public static byte[] getLenAndValue(String value, String encoding) {
		try {
			if (encoding == null) {
				encoding = ENCODING_UTF_8;
			}
			if (value == null) {
				value = "";
			}
			byte[] valueByte = value.getBytes(ENCODING_UTF_8);
			byte[] valueByteAll = revert(toByteArray(valueByte.length),
					valueByte);
			return valueByteAll;
		} catch (UnsupportedEncodingException e) {
			// System.out.println("UnsupportedEncodingException");
			e.printStackTrace();
			return DEFAULT_0;
		}
	}

	public static byte[] getLenAndValue(byte[] value) {
		if (value == null) {
			value = new byte[0];
			return value;
		}
		byte[] valueByteAll = revert(toByteArray(value.length), value);
		return valueByteAll;
	}

	/**
	 * 3DES分段
	 * 
	 * byte[]明文cipherText return int part(段数)
	 * */
	public static int getPart(byte[] cipherText) {
		int part = 0; // 密文段数
		if (cipherText.length / 2048 >= 1) {
			if (cipherText.length % 2048 != 0) {
				part = (cipherText.length / 2048) + 1;
			} else {
				part = (cipherText.length / 2048);
			}
		} else {
			part = 1;
		}
		return part;
	}

	public static byte[] hexString2ByteArray(String hexStr) {
		if (hexStr == null)
			return null;
		hexStr = hexStr.replaceAll(" ", "");
		if (hexStr.length() % 2 != 0) {
			return null;
		}
		byte[] data = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++) {
			char hc = hexStr.charAt(2 * i);
			char lc = hexStr.charAt(2 * i + 1);
			byte hb = hexChar2Byte(hc);
			byte lb = hexChar2Byte(lc);
			if (hb < 0 || lb < 0) {
				return null;
			}
			int n = hb << 4;
			data[i] = (byte) (n + lb);
		}
		return data;
	}

	public static byte hexChar2Byte(char c) {
		if (c >= '0' && c <= '9')
			return (byte) (c - '0');
		if (c >= 'a' && c <= 'f')
			return (byte) (c - 'a' + 10);
		if (c >= 'A' && c <= 'F')
			return (byte) (c - 'A' + 10);
		return -1;
	}

	/**
	 * byte[] 转 16进制字符串
	 * 
	 * @param arr
	 * @return
	 */
	public static String byteArray2HexString(byte[] arr) {

		if (arr == null)
			return "";

		StringBuilder sbd = new StringBuilder();
		for (byte b : arr) {
			String tmp = Integer.toHexString(0xFF & b);
			if (tmp.length() < 2)
				tmp = "0" + tmp;
			sbd.append(tmp);
		}
		return sbd.toString();
	}

	/**
	 * 空格分隔的hex string
	 * 
	 * @param arr
	 * @return
	 */
	public static String byteArray2HexStringWithSpace(byte[] arr) {
		StringBuilder sbd = new StringBuilder();
		for (byte b : arr) {
			String tmp = Integer.toHexString(0xFF & b);
			if (tmp.length() < 2)
				tmp = "0" + tmp;
			sbd.append(tmp);
			sbd.append(" ");
		}
		return sbd.toString();
	}

	/**
	 * int以大端序转成字节数组
	 * 
	 * @param i
	 * @return
	 */
	public static byte[] intToByte(int i) {
		byte[] bt = new byte[4];
		bt[3] = (byte) (0xff & i);
		bt[2] = (byte) ((0xff00 & i) >> 8);
		bt[1] = (byte) ((0xff0000 & i) >> 16);
		bt[0] = (byte) ((0xff000000 & i) >> 24);
		return bt;
	}

	/**
	 * 字节数组以大端序转成int
	 * 
	 * @param bytes
	 * @return
	 */
	public static int bytesToInt(byte[] bytes) {
		int num = bytes[3] & 0xFF;
		num |= ((bytes[2] << 8) & 0xFF00);
		num |= ((bytes[1] << 16) & 0xFF0000);
		num |= ((bytes[0] << 24) & 0xFF000000);
		return num;
	}

	public static String stringPadding(String str, char ch, int lenAfterPadding) {
		StringBuffer sb = new StringBuffer();
		int strLen = 0;
		if (str != null) {
			sb.append(str);
			strLen = str.length();
		}
		for (int i = 0; i < lenAfterPadding - strLen; i++) {
			sb.append(ch);
		}
		return sb.toString();
	}

	/**
	 * 取start到end的byte array，包含end。
	 * 
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
	static public byte[] getData(byte[] data, int start, int end) {
		byte[] t = new byte[end - start + 1];
		System.arraycopy(data, start, t, 0, t.length);
		return t;
	}

	/**
	 * 从data取start到end的数据，返回bcd string。end包含在取值范围。
	 * 
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
	static public String getBCDString(byte[] data, int start, int end) {
		byte[] t = new byte[end - start + 1];
		System.arraycopy(data, start, t, 0, t.length);
		return ByteUtils.byteArray2HexString(t);
	}

	/**
	 * 从data取start到end的数据，返回hex string。end包含在取值范围。
	 * 
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
	static public String getHexString(byte[] data, int start, int end) {
		byte[] t = new byte[end - start + 1];
		System.arraycopy(data, start, t, 0, t.length);
		return ByteUtils.byteArray2HexStringWithSpace(t);
	}
	
	/**
	 * 做PKCS5填充
	 * 
	 * @param data
	 * @param blockLen
	 * @return
	 */
	public static byte[] paddingWithPKCS5(byte[] data, int blockLen) {
		if (data == null || data.length == 0)
			return data;
		int paddingLen = blockLen - data.length % blockLen;
		byte[] dataPadded = new byte[data.length + paddingLen];
		System.arraycopy(data, 0, dataPadded, 0, data.length);
		for (int i = data.length; i < dataPadded.length; i++) {
			dataPadded[i] = (byte) paddingLen;
		}
		return dataPadded;
	}
	
	/**
	 * 去除PKCS5填充
	 * 
	 * @param dataPadded
	 * @return
	 */
	public static byte[] removePaddingWithPKCS5(byte[] dataPadded) {
		if (dataPadded == null || dataPadded.length == 0)
			return dataPadded;
		int len = dataPadded.length;
		int paddingLen = dataPadded[len - 1];
		for (int i = len - 1; i > len - paddingLen && i > 0; i--) {
			if (dataPadded[i - 1] != (byte)paddingLen) {
				return dataPadded;
			}
		}
		byte[] data = new byte[len - paddingLen];
		System.arraycopy(dataPadded, 0, data, 0, data.length);
		return data;
	}

}