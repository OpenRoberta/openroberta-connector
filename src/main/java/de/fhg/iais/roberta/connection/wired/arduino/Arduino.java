package de.fhg.iais.roberta.connection.wired.arduino;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.AbstractWiredRobot;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;

/**
 * Implementation of the Arduino robot.
 */
public class Arduino extends AbstractWiredRobot {

    /**
     * Constructor for the Arduino robot.
     *
     * @param type the robot type
     * @param port the robot port
     */
    public Arduino(WiredRobotType type, String port) {
        super(type, port);
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new ArduinoConnector(this);
    }
}
