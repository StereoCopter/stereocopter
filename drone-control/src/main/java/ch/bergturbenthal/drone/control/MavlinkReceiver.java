package ch.bergturbenthal.drone.control;

import java.util.Map;

import org.mavlink.messages.MAVLinkMessage;

public interface MavlinkReceiver {
	public Map<DroneAddress, Drone> listKnownDrones();

	public Map<String, Number> readMessageCounters();

	public Map<String, MAVLinkMessage> getLastMessageByType();
}
