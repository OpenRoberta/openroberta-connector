package de.fhg.iais.roberta.connection.arduino;

import java.util.Objects;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IRobot;

public class Arduino implements IRobot {
    private final ArduinoType type;
    private final String port;

    public Arduino(ArduinoType type, String port) {
        this.type = type;
        this.port = port;
    }

    @Override
    public String getName() {
        return this.type.getPrettyText();
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.WIRED;
    }

    @Override
    public IConnector<? extends IRobot> createConnector() {
        return new ArduinoConnector(this);
    }

    public ArduinoType getType() {
        return this.type;
    }

    public String getPort() {
        return this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( (obj == null) || (this.getClass() != obj.getClass()) ) {
            return false;
        }
        Arduino arduino = (Arduino) obj;
        return (this.type == arduino.type) && Objects.equals(this.port, arduino.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.port);
    }
}
