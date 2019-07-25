package de.fhg.iais.roberta.connection.wired;

import de.fhg.iais.roberta.connection.IRobot;

/**
 * Interface for wired robots.
 */
public interface IWiredRobot extends IRobot {
    @Override
    default ConnectionType getConnectionType() {
        return ConnectionType.WIRED;
    }

    /**
     * Returns the port of the wired robot.
     *
     * @return the port of the wired robot
     */
    String getPort();

    /**
     * Returns the type of the wired robot.
     *
     * @return the type of the wired robot
     */
    WiredRobotType getType();
}
