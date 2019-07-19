package de.fhg.iais.roberta.connection.ev3;

import java.util.Objects;

import de.fhg.iais.roberta.main.Robot;

public class Ev3 implements Robot {
    private final String name;

    public Ev3(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
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
