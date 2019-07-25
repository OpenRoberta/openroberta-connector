package de.fhg.iais.roberta.connection;

/**
 * Connector class for robots that do not need server connection.
 * Automatically sets robots to connected.
 */
public class AutoConnector extends AbstractConnector<IRobot> {
    public AutoConnector(IRobot robot) {
        super(robot);
    }

    @Override
    protected void runLoopBody() {
        if ( this.state == State.DISCOVER ) {
            this.fire(State.WAIT_FOR_CONNECT_BUTTON_PRESS);
            this.fire(State.WAIT_FOR_CMD);
        }
    }
}
