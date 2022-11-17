package de.fhg.iais.roberta.connection.wired.spike;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.AbstractWiredRobot;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;

public class Spike extends AbstractWiredRobot {
    /**
     * Constructor for wired robots.
     *
     * @param type the robot type
     * @param port the robot port
     */
    public Spike(WiredRobotType type, String port) {
        super(type, port);
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new SpikeConnector(this);
    }
}
