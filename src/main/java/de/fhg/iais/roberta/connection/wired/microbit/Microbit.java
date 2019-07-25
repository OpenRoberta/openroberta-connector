package de.fhg.iais.roberta.connection.wired.microbit;

import de.fhg.iais.roberta.connection.AutoConnector;
import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.AbstractWiredRobot;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;

/**
 * Implementation of the Micro:bit/Calliope mini robot.
 */
public class Microbit extends AbstractWiredRobot {

    /**
     * Constructor for the Micro:bit/Calliope mini robot.
     *
     * @param port the robot port
     */
    public Microbit(String port) {
        super(WiredRobotType.MICROBIT, port);
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new AutoConnector(this);
    }
}