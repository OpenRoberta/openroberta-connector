package de.fhg.iais.roberta.main;

/**
 * The general interface for every robot that should be supported by the connector.
 */
@FunctionalInterface
public interface Robot {
    /**
     * Returns the name of the robot.
     * @return the name of the robot.
     */
    String getName();
}
