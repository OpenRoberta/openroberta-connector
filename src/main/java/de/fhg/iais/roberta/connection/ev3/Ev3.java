package de.fhg.iais.roberta.connection.ev3;

import java.util.Objects;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;

public class Ev3 implements IRobot {
    private final String name;

    public Ev3(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.WIRED;
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new Ev3Connector(this);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( (obj == null) || (this.getClass() != obj.getClass()) ) {
            return false;
        }
        Ev3 ev3 = (Ev3) obj;
        return Objects.equals(this.name, ev3.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
