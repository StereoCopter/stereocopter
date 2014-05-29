package ch.bergturbenthal.drone.control.rest;

import java.util.Collection;
import java.util.Map;

import org.mavlink.messages.MAVLinkMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
		return receiver.listOnlineDrones();
	}

	@ResponseBody
	@RequestMapping("counters")
	public Map<String, Number> listCounters() {
		return receiver.readMessageCounters();
	}
}
