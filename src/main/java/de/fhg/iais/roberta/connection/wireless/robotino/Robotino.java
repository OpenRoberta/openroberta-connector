package de.fhg.iais.roberta.connection.wireless.robotino;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wireless.AbstractWirelessRobot;

import java.net.InetAddress;

public class Robotino extends AbstractWirelessRobot {

    public Robotino(String name, InetAddress address) {
        super(name, address);
    }

    @Override
    public String getName() {
        return "Robotino";
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new RobotinoConnector(this);
    }
}
