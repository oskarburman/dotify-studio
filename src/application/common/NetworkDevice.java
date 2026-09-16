package application.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;

import javax.print.PrintException;

import org.daisy.dotify.api.embosser.Device;

/**
 * Provides a device that sends the braille data to an embosser on the network,
 * using the printing protocol commonly known as AppSocket, JetDirect or port 9100.
 *
 * <p>This is needed because a print queue that passes the data through untouched
 * (a raw queue) cannot be created on all systems. Notably, macOS removed support
 * for raw queues, and any other queue would translate the data and destroy the braille.</p>
 */
public class NetworkDevice implements Device {
	/**
	 * The prefix identifying a device name as a network address.
	 */
	public static final String PREFIX = "socket://";
	/**
	 * The port used when the device name doesn't specify one.
	 */
	public static final int DEFAULT_PORT = 9100;
	private static final int CONNECT_TIMEOUT = 10000;
	private final String host;
	private final int port;

	/**
	 * Creates a new network device.
	 * @param host the host name or IP address of the embosser
	 * @param port the port to connect to
	 */
	public NetworkDevice(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Returns true if the specified device name is a network address.
	 * @param deviceName the device name
	 * @return true if the name is a network address, false otherwise
	 */
	public static boolean isNetworkDevice(String deviceName) {
		return deviceName!=null && deviceName.startsWith(PREFIX);
	}

	/**
	 * Creates a device from a name on the form <code>socket://host:port</code>.
	 * The port is optional.
	 * @param deviceName the device name
	 * @return a new network device
	 * @throws IllegalArgumentException if the name cannot be parsed
	 */
	public static NetworkDevice parse(String deviceName) {
		if (!isNetworkDevice(deviceName)) {
			throw new IllegalArgumentException("Not a network address: " + deviceName);
		}
		String address = deviceName.substring(PREFIX.length());
		while (address.endsWith("/")) {
			address = address.substring(0, address.length()-1);
		}
		int separator = address.lastIndexOf(':');
		if (separator<0) {
			return new NetworkDevice(requireHost(address, deviceName), DEFAULT_PORT);
		}
		String host = requireHost(address.substring(0, separator), deviceName);
		try {
			int port = Integer.parseInt(address.substring(separator+1));
			if (port<1 || port>65535) {
				throw new IllegalArgumentException("Port out of range in: " + deviceName);
			}
			return new NetworkDevice(host, port);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Failed to parse the port in: " + deviceName, e);
		}
	}

	private static String requireHost(String host, String deviceName) {
		if (host.isEmpty()) {
			throw new IllegalArgumentException("No host in: " + deviceName);
		}
		return host;
	}

	/**
	 * Gets a device name for the specified address, on the form <code>socket://host:port</code>.
	 * @param address a host name or IP address, optionally followed by a colon and a port
	 * @return a device name
	 * @throws IllegalArgumentException if the address cannot be parsed
	 */
	public static String toDeviceName(String address) {
		String value = address.trim();
		if (!value.startsWith(PREFIX)) {
			value = PREFIX + value;
		}
		NetworkDevice device = parse(value);
		return PREFIX + device.host + ":" + device.port;
	}

	@Override
	public void transmit(File file) throws PrintException {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
			try (OutputStream out = socket.getOutputStream(); InputStream in = Files.newInputStream(file.toPath())) {
				in.transferTo(out);
				out.flush();
			}
		} catch (IOException e) {
			throw new PrintException("Failed to send the document to " + host + ":" + port + ". " + e.getMessage());
		}
	}

}
