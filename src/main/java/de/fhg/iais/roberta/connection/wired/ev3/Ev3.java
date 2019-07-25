package de.fhg.iais.roberta.connection.wired.ev3;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.AbstractWiredRobot;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;

/**
 * Implementation of the EV3 robot.
 */
public class Ev3 extends AbstractWiredRobot {

    /**
     * Constructor for the EV3 robot.
     *
     * @param port the robot port
     */
    public Ev3(String port) {
        super(WiredRobotType.EV3, port);
    }

    @Override
    public String getName() {
        return super.getName() + ": " + this.getPort();
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new Ev3Connector(this);
    }
}
