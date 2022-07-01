package de.fhg.iais.roberta.connection.wireless.robotino;

import de.fhg.iais.roberta.connection.wireless.AbstractWirelessConnector;
import de.fhg.iais.roberta.connection.wireless.IWirelessConnector;
import de.fhg.iais.roberta.util.Pair;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static de.fhg.iais.roberta.connection.IConnector.State.ERROR_AUTH;
import static de.fhg.iais.roberta.connection.IConnector.State.ERROR_UPLOAD_TO_ROBOT;

/**
 * Connector class for Robotino robots.
 * Handles state and communication between robot, connector and server.
 */
public class RobotinoConnector extends AbstractWirelessConnector<Robotino> implements IWirelessConnector<Robotino> {
    private static final Logger LOG = LoggerFactory.getLogger(RobotinoConnector.class);
    RobotinoConnector(Robotino robotino) {
        super(robotino, new RobotinoViewCommunicator(robotino));
    }
    @Override
    protected void waitUpload() {
        try {
            this.communicator.setPassword(this.password);
            Pair<byte[], String> program = getProgram();
            this.communicator.uploadFile(program.getFirst(), "NEPOprog.py");
            this.fire(State.WAIT_EXECUTION);
        } catch ( UserAuthException e ) {
            LOG.error("Could not authorize user: {}", e.getMessage());
            this.reset(ERROR_AUTH);
        } catch ( IOException e ) {
            LOG.error("Something went wrong: {}", e.getMessage());
            this.reset(ERROR_UPLOAD_TO_ROBOT);
        }
    }


}
