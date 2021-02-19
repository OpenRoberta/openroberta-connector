package de.fhg.iais.roberta.util;

import de.fhg.iais.roberta.connection.IDetector;

public class ComboItemRobotType {

    private Class<? extends IDetector> id;
    private String description;

    public ComboItemRobotType(Class<? extends IDetector> id, String description) {
        this.id = id;
        this.description = description;
    }

    public Class<? extends IDetector> getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
