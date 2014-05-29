package ch.bergturbenthal.drone.control;

import java.io.IOException;

public class TestMulticast {
	public static void main(final String args[]) throws IOException {
		new MulticastReceiverImpl().run();
	}
}
