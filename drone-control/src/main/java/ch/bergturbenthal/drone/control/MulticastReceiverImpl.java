package ch.bergturbenthal.drone.control;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.ja4rtor.msg_gps_status;
import org.mavlink.messages.ja4rtor.msg_heartbeat;
import org.mavlink.messages.ja4rtor.msg_mount_status;
import org.mavlink.messages.ja4rtor.msg_servo_output_raw;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MulticastReceiverImpl implements Runnable, Closeable, MavlinkReceiver {
	private class DroneImpl implements Drone {
		@Getter
		private Date lastHeartbeat;

		@Override
		public void setMountTarget(final double pitch, final double yaw, final double roll) {
			// TODO Auto-generated method stub

		}
	}

	private final ConcurrentMap<String, AtomicInteger> countByMessage = new ConcurrentHashMap<String, AtomicInteger>();
	private final Map<DroneAddress, DroneImpl> droneHandlers = new ConcurrentHashMap<DroneAddress, DroneImpl>();
	private final Map<DroneAddress, Date> lastHeartbeat = new ConcurrentHashMap<DroneAddress, Date>();
	private final Map<String, MAVLinkMessage> lastMessageByType = new ConcurrentHashMap<String, MAVLinkMessage>();
	@Value("${mavlink.multicastAddress}")
	private String multicastAddress;
	private Thread receiverThread;
	private boolean running;

	@Override
	public void close() throws IOException {
		running = false;
	}

	@Override
	public Map<String, MAVLinkMessage> getLastMessageByType() {
		return lastMessageByType;
	}

	@Override
	public Collection<DroneAddress> listOnlineDrones() {
		final long timedoutTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10);
		final Iterator<Entry<DroneAddress, Date>> entryIterator = lastHeartbeat.entrySet().iterator();
		while (entryIterator.hasNext()) {
			final Entry<DroneAddress, Date> entry = entryIterator.next();
			if (entry.getValue().getTime() < timedoutTime) {
				entryIterator.remove();
			}
		}
		return lastHeartbeat.keySet();
	}

	@Override
	public Map<String, Number> readMessageCounters() {
		final HashMap<String, Number> ret = new HashMap<String, Number>();
		for (final Entry<String, AtomicInteger> entry : countByMessage.entrySet()) {
			ret.put(entry.getKey(), Integer.valueOf(entry.getValue().intValue()));
		}
		return ret;
	}

	@Override
	public void run() {
		running = true;
		try {
			// multicastAddress = InetAddress.getByName("224.0.0.2");
			@Cleanup
			final MulticastSocket multicastSocket = new MulticastSocket(14550);
			multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));
			final MAVLinkReader reader = new MAVLinkReader(new DataInputStream(new ByteArrayInputStream(new byte[] {})));
			final DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
			log.info("Multicast listener started");
			while (running) {
				multicastSocket.receive(packet);
				// System.out.println(packet.getAddress().getHostAddress() + " " + packet.getOffset() + " " + packet.getLength());

				MAVLinkMessage message = reader.getNextMessage(packet.getData(), packet.getLength());
				while (message != null) {
					if (message instanceof msg_heartbeat) {
						final msg_heartbeat heartbeatMessage = (msg_heartbeat) message;
						// System.out.println(heartbeatMessage.sysId);
						lastHeartbeat.put(new DroneAddress(packet.getAddress(), message.sysId), new Date());
						// System.out.println(lastHeartbeat);
					}
					if (message instanceof msg_gps_status) {
						final msg_gps_status gpsStatus = (msg_gps_status) message;
						log.info(gpsStatus + "");

					}
					if (message instanceof msg_mount_status) {
						log.info("" + message);
					}
					if (message instanceof msg_servo_output_raw) {
						// log.info("Servo Output " + message);
					}

					final String messageName = message.getClass().getSimpleName();
					lastMessageByType.put(messageName, message);
					final AtomicInteger counter = countByMessage.get(messageName);
					if (counter == null) {
						countByMessage.putIfAbsent(messageName, new AtomicInteger(0));
						countByMessage.get(messageName).incrementAndGet();
					} else {
						counter.incrementAndGet();
					}
					// log.info(messageName);
					if (reader.nbUnreadMessages() == 0) {
						break;
					}
					message = reader.getNextMessageWithoutBlocking();
				}
			}
		} catch (final IOException e) {
			log.error("Multicast Listender died", e);
		}
		log.info("Multicast listener ended");
	}

	@PostConstruct
	public void startThread() {
		receiverThread = new Thread(this, "Multicast Receiver");
		receiverThread.start();
	}

	@PreDestroy
	public void stopThread() {
		running = false;
		receiverThread.interrupt();
	}

}
