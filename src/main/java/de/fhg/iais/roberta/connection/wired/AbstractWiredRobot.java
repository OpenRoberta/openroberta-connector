package de.fhg.iais.roberta.connection.wired;

import java.util.Objects;

/**
 * Abstract class for a wired robot.
 */
public abstract class AbstractWiredRobot implements IWiredRobot {
    private final WiredRobotType type;
    private final String port;

    /**
     * Constructor for wired robots.
     *
     * @param type the robot type
     * @param port the robot port
     */
    protected AbstractWiredRobot(WiredRobotType type, String port) {
        this.type = type;
        this.port = port;
    }

    @Override
    public String getName() {
        return this.type.getPrettyText();
    }

    @Override
    public String getPort() {
        return this.port;
    }

    @Override
    public WiredRobotType getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( (obj == null) || (this.getClass() != obj.getClass()) ) {
            return false;
        }
        IWiredRobot robot = (IWiredRobot) obj;
        return (this.type == robot.getType()) && Objects.equals(this.port, robot.getPort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.port);
    }
}
