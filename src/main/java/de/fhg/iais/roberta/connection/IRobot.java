package de.fhg.iais.roberta.connection;

/**
 * The general interface for every robot that should be supported by the connector.
 */
public interface IRobot {
    /**
     * Different types of connection.
     */
    enum ConnectionType {
        WIRELESS,
        WIRED
    }

    /**
     * Returns the correct name of the robot.
     *
     * @return the name of the robot
     */
    String getPrettyName();

    /**
     * Returns the shortened name of the robot
     *
     * @return
     */
    String getName();

    /**
     * Returns the connection type of the robot.
     *
     * @return the connection type of the robot
     */
    ConnectionType getConnectionType();

    /**
     * Returns an appropriate connector for this robot.
     *
     * @return the associated connector
     */
    IConnector<? extends IRobot> createConnector();
}
