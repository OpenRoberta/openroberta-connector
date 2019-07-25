package de.fhg.iais.roberta.connection.wired;

public enum WiredRobotType {
    UNO ("uno", "Arduino Uno"),
    MEGA ("mega", "Arduino Mega"),
    NANO ("nano", "Arduino Nano"),
    BOB3 ("bob3", "BOB3"),
    BOTNROLL ("ardu", "Bot'n Roll"),
    MBOT ("mbot", "mBot"),
    MICROBIT("microbit", "Micro:bit/Calliope mini"),
    EV3("ev3", "LEGO EV3"),
    NONE ("none", "none");

    private final String text;
    private final String prettyText;

    WiredRobotType(String text, String prettyText) {
        this.text = text;
        this.prettyText = prettyText;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public String getPrettyText() {
        return this.prettyText;
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
