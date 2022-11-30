package de.fhg.iais.roberta.connection.wireless.nao;

import de.fhg.iais.roberta.connection.wireless.AbstractWirelessConnector;
import de.fhg.iais.roberta.util.Pair;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
/**
 * Connector class for NAO robots.
 * Handles state and communication between robot, connector and server.
 */
public class NaoConnector extends AbstractWirelessConnector<Nao> {
    private static final Logger LOG = LoggerFactory.getLogger(NaoConnector.class);


    /**
     * Constructor for tha NAO connector.
     *
     * @param nao the NAO that should be connected to
     */
    NaoConnector(Nao nao) {
        super(nao, new NaoCommunicator(nao));
    }
    @Override
    protected void waitUpload() {
        try {
            this.communicator.setPassword(this.password);

            String firmware = this.communicator.checkFirmwareVersion();
            if ( !firmware.isEmpty() ) {
                if ( !this.serverCommunicator.verifyHalChecksum(firmware) ) {
                    this.serverCommunicator.updateHalNAO(firmware);
                }
            }
            Pair<byte[], String> program = getProgram();
            this.communicator.uploadFile(program.getFirst(), program.getSecond());
            this.fire(State.WAIT_EXECUTION);
        } catch ( UserAuthException e ) {
            LOG.error("Could not authorize user: {}", e.getMessage());
            this.reset(State.ERROR_AUTH);
        } catch ( IOException e ) {
            LOG.error("Something went wrong: {}", e.getMessage());
            this.reset(State.ERROR_UPLOAD_TO_ROBOT);
        }
    }

    @Override
    protected void waitStopProgram() {
        LOG.error("Stop program method not implemented");
        this.reset(State.ERROR_STOP_PROGRAM_CALLED);
    }
}
