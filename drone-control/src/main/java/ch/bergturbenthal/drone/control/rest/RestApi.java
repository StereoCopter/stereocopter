package ch.bergturbenthal.drone.control.rest;

import java.util.Collection;
import java.util.Map;

import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.MAV_MODE_STATE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.drone.control.Drone;
import ch.bergturbenthal.drone.control.DroneAddress;
import ch.bergturbenthal.drone.control.MavlinkReceiver;

@RestController()
@RequestMapping("api")
public class RestApi {
	@Autowired
	private MavlinkReceiver receiver;

	@ResponseBody
	@RequestMapping("lastMessage")
	public Map<String, MAVLinkMessage> getLastMessageByType() {
		return receiver.getLastMessageByType();
	}

	@ResponseBody
	@RequestMapping("drones")
	public Collection<DroneAddress> listAvailableDrones() {
		return receiver.listKnownDrones().keySet();
	}

	@ResponseBody
	@RequestMapping("counters")
	public Map<String, Number> listCounters() {
		return receiver.readMessageCounters();
	}

	@RequestMapping("setCameraDirection")
	public void setCameraDirection() {
		for (final Drone drone : receiver.listKnownDrones().values()) {
			drone.setMountTarget(45, 45, 45);
		}
	}

	@RequestMapping("setMode")
	public void setMode() {
		for (final Drone drone : receiver.listKnownDrones().values()) {
			drone.setMode(MAV_MODE_STATE.MAV_MODE_MANUAL);
		}
	}
}
