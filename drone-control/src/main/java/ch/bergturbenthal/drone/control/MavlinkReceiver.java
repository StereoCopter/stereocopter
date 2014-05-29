package ch.bergturbenthal.drone.control;

import java.util.Collection;
import java.util.Map;

import org.mavlink.messages.MAVLinkMessage;

public interface MavlinkReceiver {
	public Collection<DroneAddress> listOnlineDrones();

	public Map<String, Number> readMessageCounters();

	public Map<String, MAVLinkMessage> getLastMessageByType();
}
