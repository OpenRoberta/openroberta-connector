package de.fhg.iais.roberta.connection.nao;

import java.net.InetAddress;
import java.util.Objects;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;

/**
 * Implementation of the NAO robot.
 */
public class Nao implements IRobot {
    private final String name;
    private final InetAddress address;

    /**
     * Constructor for the NAO robot.
     *
     * @param name    the robot name
     * @param address the robot address
     */
    public Nao(String name, InetAddress address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.WIRELESS;
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new NaoConnector(this);
    }

    /**
     * Returns the internet address of the NAO.
     *
     * @return the internet address of the NAO
     */
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
        Nao nao = (Nao) obj;
        return Objects.equals(this.name, nao.name) && Objects.equals(this.address, nao.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.address);
    }
}
