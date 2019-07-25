package de.fhg.iais.roberta.connection.wireless.nao;

import java.net.InetAddress;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wireless.AbstractWirelessRobot;

/**
 * Implementation of the NAO robot.
 */
public class Nao extends AbstractWirelessRobot {

    /**
     * Constructor for the NAO robot.
     *
     * @param name    the robot name
     * @param address the robot address
     */
    public Nao(String name, InetAddress address) {
        super(name, address);
    }

    @Override
    public String getName() {
        return "NAO: " + super.getName();
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new NaoConnector(this);
    }
}
