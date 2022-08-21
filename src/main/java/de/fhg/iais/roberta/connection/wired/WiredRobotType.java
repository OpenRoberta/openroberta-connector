package de.fhg.iais.roberta.connection.wired;

public enum WiredRobotType {
    UNO ("uno", "Arduino Uno", true),
    MEGA ("mega", "Arduino Mega", true),
    NANO ("nano", "Arduino Nano", true),
    BOB3 ("bob3", "BOB3", true),
    BOTNROLL ("ardu", "Bot'n Roll", true),
    MBOT ("mbot", "mBot", true),
    MBOT2 ("mbot2", "mbot2", true),
    FESTOBIONIC("festobionic", "Bionics4Education", true),
    FESTOBIONICFLOWER("festobionicflower", "BionicFlower", true),
    MICROBIT("microbit", "Micro:bit/Calliope mini", true),
    EV3("ev3", "LEGO EV3", false),
    UNOWIFIREV2("unowifirev2", "Arduino Uno Wifi Rev2", true),
    NANO33BLE("nano33ble", "Arduino Nano 33 BLE", true),
    ROB3RTA ("rob3rta", "ROB3RTA", true),
    NONE ("none", "none", false);

    private final String text;
    private final String prettyText;
    private final boolean serial;

    /**
     * Creates a new wired robot type.
     * @param text the internal name of the robot type
     * @param prettyText the pretty text to display
     * @param serial whether the robot communicates over serial
     */
    WiredRobotType(String text, String prettyText, boolean serial) {
        this.text = text;
        this.prettyText = prettyText;
        this.serial = serial;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public String getPrettyText() {
        return this.prettyText;
    }

    public boolean isSerial() {
        return this.serial;
    }

    public static WiredRobotType fromString(String text) {
        for ( WiredRobotType type : WiredRobotType.values() ) {
            if (text.equalsIgnoreCase(type.toString())) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
