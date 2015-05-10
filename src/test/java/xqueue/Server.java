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

		xq.setDispatcherThreads(1);

		xq.start();

		while (true) {
			for (int i = 0; i < 10; i++)
				xq.send("topic" + i, ("hello world, " + i).getBytes());
		}
	}

}
