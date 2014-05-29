package ch.bergturbenthal.drone.control;

import java.net.InetAddress;

import lombok.Value;

@Value
public class DroneAddress {
	private InetAddress gatewayAddress;
	private int systemId;
}
