# XQueue使用指南

## 说明

XQueue是一个嵌入式的轻量级点到点消息框架，主要用来在系统间进行消息订阅和传递。

### 设计原则

* 简单易用，设计上不提供过多功能，仅满足Server端向Client端发送数据的需求。
* 效率优先，可用性优先，确保消息能快速发送和处理，不会对服务端和客户端造成堵塞。作为代价，
消息不保证到达，没有事物处理。
* 强制对客户端认证，服务端对客户端采用RSA认证，双方需要进行密钥约定。

### 架构设计

![总体架构](https://github.com/quhw/xqueue/raw/master/总体架构.png)

### 依赖包

```maven
		<dependency>
			<groupId>org.slf4j</groupId>
			<artifactId>slf4j-api</artifactId>
			<version>1.7.5</version>
		</dependency>
		<dependency>
			<groupId>io.netty</groupId>
			<artifactId>netty-all</artifactId>
			<version>4.0.28.Final</version>
		</dependency>
```

## 示例

### 服务端

```java
package xqueue;

import java.util.HashMap;

import com.chinaums.xqueue.XQueue;

public class Server {

	public static void main(String[] args) throws Exception {
		XQueue xq = new XQueue(8099);

		HashMap<String, String> keys = new HashMap<String, String>();
		keys.put(
				"test",
				"30819f300d06092a864886f70d010101050003818d0030818902818100d243b2be55f73c741508073225d751b528f1a391d494fe367f88568b20a8ae8edab9932eadba6ab778320cae131229d7eb2574300c4ecb7354cec204dc052a8c4a642922759c97d34c5d7847aebe9f901f15f87c6ea783f43d0928c1df6665bf2464dc0667c5c688e9eb62d7c604809dcb0e402fd5c66802b85f95c2900f08af0203010001");
		xq.setAuthKeys(keys);

		HashMap<String, String> topics = new HashMap<String, String>();
		topics.put("test", "topic0,topic1");
		xq.setAuthTopicsString(topics);

		xq.start();

		while (true) {
			for (int i = 0; i < 10; i++)
				xq.send("topic" + i, ("hello world, " + i).getBytes());
		}
	}

}
```

### 客户端

```java
package xqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chinaums.xqueue.XQueueClient;
import com.chinaums.xqueue.XQueueListener;
import com.chinaums.xqueue.XQueueMessage;

public class Client {
	private static Logger log = LoggerFactory.getLogger(Client.class);

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Client <host:port> <clientId> <topic>");
			System.exit(-1);
		}
		String host = args[0];
		String clientId = args[1];
		String topic = args[2];

		XQueueClient client = new XQueueClient(host);
		client.setClientId(clientId);
		client.setSystemId("test");
		client.setSubscribeTopic(topic);
		client.setWorkerPoolSize(8);
		client.setPrivateKey("30820276020100300d06092a864886f70d0101010500048202603082025c02010002818100d243b2be55f73c741508073225d751b528f1a391d494fe367f88568b20a8ae8edab9932eadba6ab778320cae131229d7eb2574300c4ecb7354cec204dc052a8c4a642922759c97d34c5d7847aebe9f901f15f87c6ea783f43d0928c1df6665bf2464dc0667c5c688e9eb62d7c604809dcb0e402fd5c66802b85f95c2900f08af02030100010281802657c00da3818d5da1c43003be10d0ce9763d12f33b3e3d3ae57ff682991791b85d95774a8ab98f05213d66a1c0230ed35ed438dcb80c6eb06291a0a66d0ee5c083e8cf5ed1c0cbb5efc03bae08850391ae5f858ac2a37757dadc24d08d50f29995ef2b3620c6617c9eea44ad487e56cdc91a22f74df4bb9517e403808b7da19024100eff8a6621a3e2f6e9b46889ee22910adce7b2e4c0d8a6f7a4c1b5e274d5dfa880ad4a7765364739e901271843c2b6010237f48139aeb341a323f0815524fb555024100e04f14b9545264e489846193d80fca5c533ce0321ff9adf0f3c045cc9b48dfc5ec1e3944af0b04361da55051458b0721cd08c5eb5e29bd75a3b7f0bde86e45f302410087a5f0b787ca1b19d4e03c1e440d9e860130146d9d41e1de29e35687b4ee28ce7a00d760c5a704cc5ef86dfea7dc8502da6dfa9a4a7a260ba0d78c8430f129b902400542b588b72a8b8534986f35696ecd5f26f0998d736486a8ccfea864690be77b9bd305a2cfbc2168277fd60761eb25caac255586f49964011b4ad8118c1ef1f302401b4a58478f80331f3c9e4cd0d1ad3debbeb7393a6172e2219a20ecc397c7eaa57d21eb534ee47b03b3f403eaddb6e640942f16fb88503dab8a1112e80ae24e4f");

		client.setListener(new XQueueListener() {

			@Override
			public void onMessage(XQueueMessage msg) throws Exception {
				log.info("Received: {}", new String(msg.getContent()));
			}
		});

		client.start();
	}

}
```
## FAQ

### 认证密钥格式的是什么

私钥是PKCS8格式的十六进制字符串，公钥是X509格式的十六进制字符串，通常由[Java密钥工具](http://144.131.254.48:5446/quhw/RsaKeyUtil)生成。
