package ch.bergturbenthal.drone.control;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.tomcat.util.buf.HexUtils;
import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.MAV_AUTOPILOT;
import org.mavlink.messages.MAV_MOUNT_MODE;
import org.mavlink.messages.MAV_TYPE;
import org.mavlink.messages.ja4rtor.msg_gps_status;
import org.mavlink.messages.ja4rtor.msg_heartbeat;
import org.mavlink.messages.ja4rtor.msg_mount_configure;
import org.mavlink.messages.ja4rtor.msg_mount_control;
import org.mavlink.messages.ja4rtor.msg_mount_status;
import org.mavlink.messages.ja4rtor.msg_servo_output_raw;
import org.mavlink.messages.ja4rtor.msg_set_mode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MulticastReceiverImpl implements Runnable, Closeable, MavlinkReceiver {

	@RequiredArgsConstructor
	private class DroneImpl implements Drone {
		private final DroneAddress droneAddress;

		@Getter
		@Setter
		private Date lastHeartbeat;
		private final AtomicInteger sequenceNumber = new AtomicInteger();
		int currentMountMode = -1;

		@Override
		public boolean isAlive() {
			return System.currentTimeMillis() - lastHeartbeat.getTime() < 3000;
		}

		public void sendHeartBeat() {
			final msg_heartbeat heartbeat = new msg_heartbeat();
			heartbeat.type = MAV_TYPE.MAV_TYPE_GCS;
			heartbeat.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
			heartbeat.mavlink_version = 3;
			send(heartbeat);
		}

		@Override
		public void setMode(final int mode) {
			final msg_set_mode set_mode = new msg_set_mode();
			set_mode.base_mode = 1;
			set_mode.custom_mode = mode;
			set_mode.sysId = droneAddress.getSystemId();
			set_mode.target_system = droneAddress.getSystemId();
			send(set_mode);

		}

		@Override
		public void setMountTarget(final double pitch, final double yaw, final double roll) {
			if (currentMountMode != MAV_MOUNT_MODE.MAV_MOUNT_MODE_MAVLINK_TARGETING) {
				final msg_mount_configure mount_configure = new msg_mount_configure();
				mount_configure.mount_mode = MAV_MOUNT_MODE.MAV_MOUNT_MODE_MAVLINK_TARGETING;
				mount_configure.target_component = droneAddress.getSystemId();
				mount_configure.target_system = droneAddress.getSystemId();
				send(mount_configure);
			}
			final msg_mount_control mount_control = new msg_mount_control();
			mount_control.input_a = (long) (pitch * 100);
			mount_control.input_b = (long) (roll * 100);
			mount_control.input_c = (long) (yaw * 100);
			mount_control.target_component = droneAddress.getSystemId();
			mount_control.target_system = droneAddress.getSystemId();
			send(mount_control);
		}

		private void send(final MAVLinkMessage message) {
			try {
				message.sequence = sequenceNumber.incrementAndGet();
				message.sysId = mySystemId;
				message.componentId = myComponentId;
				System.out.println("Send: " + message);
				final byte[] encodedPacket = message.encode();
				System.out.println("Hex: " + HexUtils.toHexString(encodedPacket));
				final DatagramPacket packet = new DatagramPacket(encodedPacket, encodedPacket.length, droneAddress.getGatewayAddress());
				System.out.println("Send Packet: " + packet.getSocketAddress());
				multicastSocket.send(packet);
			} catch (final IOException e) {
				throw new RuntimeException("Cannot send message " + message, e);
			}
		}
	}

	private final ConcurrentMap<String, AtomicInteger> countByMessage = new ConcurrentHashMap<String, AtomicInteger>();
	private final ConcurrentMap<DroneAddress, DroneImpl> droneHandlers = new ConcurrentHashMap<DroneAddress, DroneImpl>();
	private final Map<String, MAVLinkMessage> lastMessageByType = new ConcurrentHashMap<String, MAVLinkMessage>();
	@Value("${mavlink.multicastAddress}")
	private String multicastAddress;
	private MulticastSocket multicastSocket;
	@Value("${mavlink.componentId}")
	private int myComponentId;
	@Value("${mavlink.systemId}")
	private int mySystemId;
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
	public Map<DroneAddress, Drone> listKnownDrones() {
		return new HashMap<DroneAddress, Drone>(droneHandlers);
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
			multicastSocket = new MulticastSocket(14550);
			multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));
			final MAVLinkReader reader = new MAVLinkReader(new DataInputStream(new ByteArrayInputStream(new byte[] {})));
			final DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
			log.info("Multicast listener started");
			while (running) {
				multicastSocket.receive(packet);
				// System.out.println(packet.getAddress().getHostAddress() + " " + packet.getSocketAddress());

				MAVLinkMessage message = reader.getNextMessage(packet.getData(), packet.getLength());
				while (message != null) {
					final DroneAddress droneAddress = new DroneAddress(packet.getSocketAddress(), message.sysId);
					if (message instanceof msg_heartbeat) {
						final msg_heartbeat heartbeatMessage = (msg_heartbeat) message;
						// System.out.println(heartbeatMessage.sysId);
						// System.out.println(lastHeartbeat);
						final DroneImpl existingDrone = droneHandlers.get(droneAddress);
						if (existingDrone != null) {
							existingDrone.setLastHeartbeat(new Date());
						} else {
							droneHandlers.putIfAbsent(droneAddress, new DroneImpl(droneAddress));
							droneHandlers.get(droneAddress).setLastHeartbeat(new Date());
						}
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

	@Scheduled(fixedRate = 1000)
	public void sendHeartBeat() {
		for (final DroneImpl drone : droneHandlers.values()) {
			drone.sendHeartBeat();

		}
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
		if (multicastSocket != null) {
			multicastSocket.close();
		}
	}

}
