package xqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chinaums.xqueue.XQueueClient;
import com.chinaums.xqueue.XQueueListener;
import com.chinaums.xqueue.XQueueMessage;

public class Client {
	private static Logger log = LoggerFactory.getLogger(Client.class);

	public static void main(String[] args) {
		XQueueClient client = new XQueueClient("127.0.0.1:8099");
		client.setClientId("test");
		client.setSystemId("test");
		client.setSubscribeTopic("test");
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
