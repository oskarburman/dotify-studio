package application.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.print.PrintException;

import org.junit.Test;

public class NetworkDeviceTest {

	@Test
	public void testToDeviceName_01() {
		assertEquals("socket://192.168.1.50:9100", NetworkDevice.toDeviceName("192.168.1.50"));
	}

	@Test
	public void testToDeviceName_02() {
		assertEquals("socket://192.168.1.50:9101", NetworkDevice.toDeviceName(" 192.168.1.50:9101 "));
	}

	@Test
	public void testToDeviceName_03() {
		assertEquals("socket://embosser.local:9100", NetworkDevice.toDeviceName("socket://embosser.local/"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testToDeviceName_noHost() {
		NetworkDevice.toDeviceName(":9100");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testToDeviceName_portNotANumber() {
		NetworkDevice.toDeviceName("192.168.1.50:printer");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testToDeviceName_portOutOfRange() {
		NetworkDevice.toDeviceName("192.168.1.50:70000");
	}

	@Test
	public void testIsNetworkDevice() {
		org.junit.Assert.assertTrue(NetworkDevice.isNetworkDevice("socket://192.168.1.50:9100"));
		org.junit.Assert.assertFalse(NetworkDevice.isNetworkDevice("Index Basic-D"));
		org.junit.Assert.assertFalse(NetworkDevice.isNetworkDevice(null));
	}

	/**
	 * Sends a document to a server that stands in for an embosser, and verifies
	 * that the bytes arrive unmodified.
	 * @throws Exception if the test fails
	 */
	@Test
	public void testTransmit() throws Exception {
		byte[] expected = new byte[512];
		for (int i=0; i<expected.length; i++) {
			expected[i] = (byte)i;
		}
		Path file = Files.createTempFile("emboss", ".tmp");
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try (ServerSocket server = new ServerSocket(0)) {
			Files.write(file, expected);
			Future<byte[]> received = executor.submit(()->readAll(server));
			new NetworkDevice("127.0.0.1", server.getLocalPort()).transmit(file.toFile());
			assertArrayEquals(expected, received.get(10, TimeUnit.SECONDS));
		} finally {
			executor.shutdownNow();
			Files.deleteIfExists(file);
		}
	}

	/**
	 * Verifies that a failure to connect is reported as a print exception
	 * rather than leaving the caller without an explanation.
	 * @throws Exception if the test fails
	 */
	@Test
	public void testTransmit_noServer() throws Exception {
		int port;
		try (ServerSocket server = new ServerSocket(0)) {
			// the port is free once the server is closed, so nothing is listening there
			port = server.getLocalPort();
		}
		Path file = Files.createTempFile("emboss", ".tmp");
		try {
			new NetworkDevice("127.0.0.1", port).transmit(file.toFile());
			fail("Expected a print exception.");
		} catch (PrintException e) {
			// expected
		} finally {
			Files.deleteIfExists(file);
		}
	}

	private static byte[] readAll(ServerSocket server) throws IOException {
		try (Socket socket = server.accept(); InputStream in = socket.getInputStream()) {
			return in.readAllBytes();
		}
	}

}
