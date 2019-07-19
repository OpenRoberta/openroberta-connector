package de.fhg.iais.roberta.connection.arduino;

import java.util.Objects;

import de.fhg.iais.roberta.main.Robot;

public class Arduino implements Robot {
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
