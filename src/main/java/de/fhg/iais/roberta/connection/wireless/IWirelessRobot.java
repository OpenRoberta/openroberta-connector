package de.fhg.iais.roberta.connection.wireless;

import java.net.InetAddress;

import de.fhg.iais.roberta.connection.IRobot;

/**
 * Interface for wireless robots.
 */
public interface IWirelessRobot extends IRobot {
    @Override
    default ConnectionType getConnectionType() {
        return ConnectionType.WIRELESS;
    }

    /**
     * Returns the internet address of the wireless robot.
     *
     * @return the internet address of the wireless robot
     */
    InetAddress getAddress();
}
