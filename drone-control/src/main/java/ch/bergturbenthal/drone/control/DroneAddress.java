package ch.bergturbenthal.drone.control;

import java.net.SocketAddress;

import lombok.Value;

@Value
public class DroneAddress {
	private SocketAddress gatewayAddress;
	private int systemId;
}
