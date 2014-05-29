package ch.bergturbenthal.drone.control;

public interface Drone {
	void setMountTarget(final double pitch, final double yaw, final double roll);

	void setMode(final int mode);

	boolean isAlive();
}
