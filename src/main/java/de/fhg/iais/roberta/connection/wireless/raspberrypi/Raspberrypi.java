package de.fhg.iais.roberta.connection.wireless.raspberrypi;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wireless.AbstractWirelessRobot;

import java.net.InetAddress;

/**
 * Implementation of the Raspberrypi robot.
 */
public class Raspberrypi extends AbstractWirelessRobot {

    private final String mac;


    private int port;
    private String userName;
    private String password;

    /**
     * Constructor for the Raspberrypi robot.
     *
     * @param name    the robot name
     * @param address the robot address
     */
    public Raspberrypi(String name, String mac, InetAddress address, int port, String userName, String password) {
        super(name, address);
        this.mac = mac;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public String getName() {
        return "Raspberrypi: " + super.getName();
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new RaspberrypiConnector(this);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getMac() {
        return mac;
    }
}
