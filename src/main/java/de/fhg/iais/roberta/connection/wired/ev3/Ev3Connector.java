package de.fhg.iais.roberta.connection.wired.ev3;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import de.fhg.iais.roberta.connection.AbstractConnector;
import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.ServerCommunicator;
import de.fhg.iais.roberta.util.OraTokenGenerator;
import de.fhg.iais.roberta.util.Pair;
import de.fhg.iais.roberta.util.PropertyHelper;

/**
 * Intended to be used as Singleton(!). This class handles two connections:
 * robot<->Connector: {@link Ev3Communicator}
 * Connector<->Open Roberta server: {@link ServerCommunicator}
 * After setting up an object of this class, you want to run this in a separate thread, because our protocol contains blocking http requests.
 * The state will be changed from the gui in another thread.
 *
 * @author dpyka
 * {@link IConnector}
 */
public class Ev3Connector extends AbstractConnector<Ev3> {
    private static final Logger LOG = LoggerFactory.getLogger(Ev3Connector.class);

    private static final String brickIp = PropertyHelper.getInstance().getProperty("brickIp");

    private final Ev3Communicator ev3comm;

    private final String[] fwfiles = {
        "runtime", "jsonlib", "websocketlib", "ev3menu"
    };

    /**
     * Instantiate the connector with specific properties from the file or use default options defined in this class.
     * Set up a communicator to the EV3 and to the Open Roberta server.
     */
    Ev3Connector(Ev3 robot) {
        super(robot);

        LOG.info("Robot ip {}", brickIp);
        this.ev3comm = new Ev3Communicator(brickIp);
    }

    @Override
    protected void runLoopBody() {
        switch ( this.state ) {
            case DISCOVER:
                try {
                    if ( !this.ev3comm.isRunning() ) { // brick available and no program running
                        this.fire(State.WAIT_FOR_CONNECT_BUTTON_PRESS);
                    }
                } catch ( IOException e ) {
                    // ok
                }
                break;
            case WAIT_EXECUTION:
                try {
                    if ( this.ev3comm.isRunning() ) {
                        // program is running
                        this.fire(State.WAIT_EXECUTION);
                        //fire(this.state);
                    } else {
                        // brick available and no program running
                        LOG.info("{} EV3 plugged in again, no program running, OK", State.WAIT_EXECUTION);
                        this.fire(State.WAIT_FOR_CMD);
                    }
                } catch ( IOException e ) {
                    // ok
                }
                break;
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                try {
                    if ( this.ev3comm.isRunning() ) {
                        this.fire(State.DISCOVER);
                    }  // wait for user
                } catch ( IOException e ) {
                    // ok
                }
                break;
            case CONNECT_BUTTON_IS_PRESSED:
                this.token = OraTokenGenerator.generateToken();
                this.fire(State.WAIT_FOR_SERVER);
                try {
                    this.brickData = this.ev3comm.pushToBrick(CMD_REGISTER);
                    this.brickData.put(KEY_TOKEN, this.token);
                    this.brickData.put(KEY_CMD, CMD_REGISTER);
                } catch ( IOException e ) {
                    LOG.info("{} {}", State.CONNECT_BUTTON_IS_PRESSED, e.getMessage());
                    this.reset(State.ERROR_BRICK);
                    break;
                }
                try {
                    JSONObject serverResponse = this.serverCommunicator.pushRequest(this.brickData);
                    String command = serverResponse.getString("cmd");
                    if ( command.equals(CMD_REPEAT) ) {

                        try {
                            this.brickData = this.ev3comm.pushToBrick(CMD_REPEAT);
                        } catch ( IOException e ) {
                            LOG.info("{} {}", State.CONNECT_BUTTON_IS_PRESSED, e.getMessage());
                            this.reset(State.ERROR_BRICK);
                            break;
                        }
                        this.fire(State.WAIT_FOR_CMD);
                    } else if ( command.equals(CMD_ABORT) ) {
                        this.reset(State.TOKEN_TIMEOUT);
                    } else {
                        LOG.info("{} Command {} unknown", State.CONNECT_BUTTON_IS_PRESSED, command);
                        this.reset(null);
                    }
                } catch ( IOException | JSONException servererror ) {
                    LOG.info("{} {}", State.CONNECT_BUTTON_IS_PRESSED, servererror.getMessage());
                    this.reset(State.ERROR_HTTP);
                }
                break;
            case WAIT_FOR_CMD:
                try {
                    this.brickData = this.ev3comm.pushToBrick(CMD_REPEAT);
                    this.brickData.put(KEY_TOKEN, this.token);
                    this.brickData.put(KEY_CMD, CMD_PUSH);
                } catch ( IOException e ) {
                    LOG.info("{} {}", State.WAIT_FOR_CMD, e.getMessage());
                    this.reset(State.ERROR_BRICK);
                    break;
                }
                String responseCommand;
                try {
                    responseCommand = this.serverCommunicator.pushRequest(this.brickData).getString(KEY_CMD);
                } catch ( IOException | JSONException servererror ) {
                    // continue to default block
                    LOG.info("{} Server response not ok {}", State.WAIT_FOR_CMD, servererror.getMessage());
                    this.reset(State.ERROR_HTTP);
                    break;
                }
                switch ( responseCommand ) {
                    case CMD_REPEAT:
                        break;
                    case CMD_ABORT:
                        this.close();
                        break;
                    case CMD_UPDATE:
                        LOG.info("Execute firmware update");
                        LOG.info(this.brickData.toString());
                        String lejosVersion = "";
                        if ( this.brickData.getString("firmwarename").equals("ev3lejosv1") ) {
                            lejosVersion = "v1/";
                        }
                        try {
                            for ( String fwfile : this.fwfiles ) {
                                Pair<byte[], String> firmware = this.serverCommunicator.downloadFirmwareFile(lejosVersion + fwfile);
                                this.ev3comm.uploadFirmwareFile(firmware.getFirst(), firmware.getSecond());
                            }
                            this.ev3comm.restartBrick();
                            LOG.info("Firmware update successful. Restarting EV3 now!");
                            this.reset(null);
                        } catch ( IOException e ) {
                            LOG.info("{} Brick update failed {}", State.WAIT_FOR_CMD, e.getMessage());
                            this.reset(State.ERROR_UPDATE);
                        }
                        break;
                    case CMD_DOWNLOAD:
                        LOG.info("Download user program");
                        try {
                            Pair<byte[], String> program = this.serverCommunicator.downloadProgram(this.brickData);
                            this.ev3comm.uploadProgram(program.getFirst(), program.getSecond());
                            this.fire(State.WAIT_EXECUTION);
                        } catch ( IOException e ) {
                            // do not give up the brick, try another push request
                            // user has to click on run button again
                            LOG.info("{} Download file failed {}", State.WAIT_FOR_CMD, e.getMessage());
                            this.fire(State.WAIT_FOR_CMD);
                        }
                        break;
                    case CMD_CONFIGURATION:
                        LOG.warn("Command {} unused, ignore and continue push!", responseCommand);
                        break;
                    default:
                        LOG.warn("Command {} unknown", responseCommand);
                        this.reset(null);
                        break;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void close() {
        super.close();
        this.ev3comm.shutdown();
    }
}
