package de.fhg.iais.roberta.connection.wired.mBot2;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.AbstractWiredRobot;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;

public class Mbot2 extends AbstractWiredRobot {
    /**
     * Constructor for wired robots.
     *
     * @param type the robot type
     * @param port the robot port
     */
    public Mbot2(WiredRobotType type, String port) {
        super(type, port);
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new Mbot2Connector(this);
    }
}
