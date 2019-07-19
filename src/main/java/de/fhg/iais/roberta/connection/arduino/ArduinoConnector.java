package de.fhg.iais.roberta.connection.arduino;

import de.fhg.iais.roberta.connection.AbstractConnector;
import de.fhg.iais.roberta.main.Robot;
import de.fhg.iais.roberta.util.OraTokenGenerator;
import de.fhg.iais.roberta.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ArduinoConnector extends AbstractConnector {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoConnector.class);

    private ArduinoCommunicator arduinoCommunicator = null;

    private final ArduinoType type;
    private final String portName;

    public ArduinoConnector(Arduino arduino) {
        super(arduino.getName());

        this.type = arduino.getType();
        this.portName = arduino.getPort();
    }

    @Override
    protected void runLoopBody() {
        switch ( this.state ) {
            case DISCOVER:
                this.arduinoCommunicator = new ArduinoCommunicator(this.brickName, this.type);
                this.fire(State.WAIT_FOR_CONNECT_BUTTON_PRESS);
                break;
            case CONNECT_BUTTON_IS_PRESSED:
                this.token = OraTokenGenerator.generateToken();
                this.fire(State.WAIT_FOR_SERVER);
                this.brickData = this.arduinoCommunicator.getDeviceInfo();
                this.brickData.put(KEY_TOKEN, this.token);
                this.brickData.put(KEY_CMD, CMD_REGISTER);
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
                } catch ( IOException | JSONException e ) {
                    LOG.error("CONNECT {}", e.getMessage());
                    this.reset(State.ERROR_HTTP);
                }
                break;
            case WAIT_FOR_CMD:
                this.brickData = this.arduinoCommunicator.getDeviceInfo();
                this.brickData.put(KEY_TOKEN, this.token);
                this.brickData.put(KEY_CMD, CMD_PUSH);
                try {
                    JSONObject response = this.serverCommunicator.pushRequest(this.brickData);
                    String cmdKey = response.getString(KEY_CMD);
                    if ( cmdKey.equals(CMD_REPEAT) ) {
                        break;
                    } else if ( cmdKey.equals(CMD_DOWNLOAD) ) {
                        LOG.info("Download user program");
                        try {
                            Pair<byte[], String> program = this.serverCommunicator.downloadProgram(this.brickData);
                            File temp = File.createTempFile(program.getSecond(), "");
                            temp.deleteOnExit();

                            if ( !temp.exists() ) {
                                throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
                            }

                            try (FileOutputStream os = new FileOutputStream(temp)) {
                                os.write(program.getFirst());
                            }

                            this.fire(State.WAIT_UPLOAD);
                            this.arduinoCommunicator.uploadFile(this.portName, temp.getAbsolutePath());
                        } catch ( FileNotFoundException e ) {
                            LOG.info("File not found: {}", e.getMessage());
                            this.fire(State.WAIT_FOR_CMD);
                        } catch ( IOException io ) {
                            LOG.info("Download and run failed: {}", io.getMessage());
                            LOG.info("Do not give up yet - make the next push request");
                            this.fire(State.WAIT_FOR_CMD);
                        }
                    } else if ( cmdKey.equals(CMD_CONFIGURATION) ) {
                        LOG.info("Configuration");
                    } else if ( cmdKey.equals(CMD_UPDATE) ) {
                        LOG.info("Firmware updated not necessary and not supported!");// LOG and go to abort
                    } else if ( cmdKey.equals(CMD_ABORT) ) {
                        LOG.error("Unexpected response from server: {}", cmdKey);
                        this.reset(State.ERROR_HTTP);
                    }
                } catch ( JSONException | IOException r ) {
                    LOG.error("WAIT_FOR_CMD {}", r.getMessage());
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

    public String getPortName() {
        return this.portName;
    }

    @Override
    public Class<? extends Robot> getRobotClass() {
        return Arduino.class;
    }
}
