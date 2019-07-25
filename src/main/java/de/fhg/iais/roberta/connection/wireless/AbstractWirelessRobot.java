package de.fhg.iais.roberta.connection.wireless;

import java.net.InetAddress;
import java.util.Objects;

/**
 * Abstract class for a wireless robot.
 */
public abstract class AbstractWirelessRobot implements IWirelessRobot {
    private final String name;
    private final InetAddress address;

    /**
     * Constructor for wireless robots.
     *
     * @param name    the robot name
     * @param address the robot address
     */
    protected AbstractWirelessRobot(String name, InetAddress address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public InetAddress getAddress() {
        return this.address;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( (obj == null) || (this.getClass() != obj.getClass()) ) {
            return false;
        }
        IWirelessRobot robot = (IWirelessRobot) obj;
        return Objects.equals(this.name, robot.getName()) && Objects.equals(this.address, robot.getAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.address);
    }
}
