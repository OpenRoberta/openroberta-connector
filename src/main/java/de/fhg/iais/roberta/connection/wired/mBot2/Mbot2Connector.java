package de.fhg.iais.roberta.connection.wired.mBot2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.connection.AbstractConnector;
import de.fhg.iais.roberta.util.OraTokenGenerator;
import de.fhg.iais.roberta.util.Pair;

public class Mbot2Connector extends AbstractConnector<Mbot2> {
    private static final Logger LOG = LoggerFactory.getLogger(Mbot2Connector.class);

    private final Mbot2Communicator mbot2comm;

    Mbot2Connector(Mbot2 robot) {
        super(robot);

        this.mbot2comm = new Mbot2Communicator(robot);
    }

    @Override
    protected void runLoopBody() {
        switch ( this.state ) {
            case DISCOVER:
                this.fire(State.WAIT_FOR_CONNECT_BUTTON_PRESS);
                break;
            case CONNECT_BUTTON_IS_PRESSED:
                this.token = OraTokenGenerator.generateToken();
                this.fire(State.WAIT_FOR_SERVER);
                fillBrickData(CMD_REGISTER);
                try {
                    JSONObject serverResponse = this.serverCommunicator.pushRequest(this.brickData);
                    String command = serverResponse.getString("cmd");
                    switch ( command ) {
                        case CMD_REPEAT:
                            this.fire(State.WAIT_FOR_CMD);
                            LOG.info("Robot successfully registered with token {}, waiting for commands", this.token);
                            break;
                        case CMD_ABORT:
                            LOG.info("registration timeout");
                            this.fire(State.TOKEN_TIMEOUT);
                            this.fire(State.DISCOVER);
                            break;
                        default:
                            LOG.error("Unexpected command {} from server", command);
                            this.reset(State.ERROR_HTTP);
                    }
                } catch ( IOException e ) {
                    LOG.error("CONNECT {}", e.getMessage());
                }
                break;
            case WAIT_FOR_CMD:
                fillBrickData(CMD_PUSH);
                try {
                    JSONObject response = this.serverCommunicator.pushRequest(this.brickData);
                    String cmdKey = response.getString(KEY_CMD);
                    switch ( cmdKey ) {
                        case CMD_REPEAT:
                            break;
                        case CMD_DOWNLOAD:
                            LOG.info("Download user program");
                            try {
                                Pair<byte[], String> program = this.serverCommunicator.downloadProgram(this.brickData);
                                if ( program.getFirst().length > 65534 ) {
                                    this.fire(State.ERROR_UPLOAD_TO_ROBOT.setAdditionalInfo("Program too large for robot. Must be less than 65534 bytes"));
                                    this.fire(State.WAIT_FOR_CMD);
                                    break;
                                }
                                File tmp = File.createTempFile("mbot2_", program.getSecond());
                                tmp.deleteOnExit();

                                if ( !tmp.exists() ) {
                                    throw new FileNotFoundException("FIle " + tmp.getAbsolutePath() + " does not exist");
                                }

                                try (FileOutputStream os = new FileOutputStream(tmp)) {
                                    os.write(program.getFirst());
                                }

                                this.fire(State.WAIT_UPLOAD);
                                Pair<Integer, String> result = this.mbot2comm.uploadFile(this.robot.getPort(), tmp.getAbsolutePath());
                                if ( result.getFirst() != 0 ) {
                                    this.fire(State.ERROR_UPLOAD_TO_ROBOT.setAdditionalInfo(result.getSecond()));
                                    this.fire(State.WAIT_FOR_CMD);
                                }
                            } catch ( FileNotFoundException e ) {
                                LOG.info("File not found: {}", e.getMessage());
                                this.fire(State.ERROR_UPLOAD_TO_ROBOT);
                                this.fire(State.WAIT_FOR_CMD);
                            }
                            break;
                        case CMD_CONFIGURATION:
                            LOG.info("Configuration");
                            break;
                        case CMD_UPDATE:
                            LOG.info("Firmware update not necessary and not supported!");
                            break;
                        case CMD_ABORT:
                            LOG.error("Unexpected response from server: {}", cmdKey);
                            this.reset(State.ERROR_HTTP);
                            break;
                    }
                } catch ( JSONException | IOException e ) {
                    LOG.error("WAIT_FOR_CMD {}", e.getMessage());
                    this.reset(State.ERROR_HTTP);
                }
                break;
            case WAIT_UPLOAD:
                this.fire(State.WAIT_FOR_CMD);
                break;
            default:
                break;

        }
    }

    private void fillBrickData(String cmd) {
        this.brickData = this.mbot2comm.getDeviceInfo();
        this.brickData.put(KEY_TOKEN, this.token);
        this.brickData.put(KEY_CMD, cmd);
    }
}
