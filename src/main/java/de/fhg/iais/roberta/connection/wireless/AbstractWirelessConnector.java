package de.fhg.iais.roberta.connection.wireless;

import de.fhg.iais.roberta.connection.AbstractConnector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.util.OraTokenGenerator;
import de.fhg.iais.roberta.util.Pair;
import net.schmizz.sshj.userauth.UserAuthException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static de.fhg.iais.roberta.connection.IConnector.State.ERROR_AUTH;
import static de.fhg.iais.roberta.connection.IConnector.State.ERROR_UPLOAD_TO_ROBOT;

/**
 * Connector class for Wireless robots.
 * Handles state and communication between robot, connector and server.
 */
public abstract class AbstractWirelessConnector<T extends IRobot> extends AbstractConnector<T> implements IWirelessConnector<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWirelessConnector.class);

    protected IWirelessCommunicator communicator;
    protected String password = "";

    protected AbstractWirelessConnector(T robot, IWirelessCommunicator communicator) {
        super(robot);
        this.communicator = communicator;
    }

    /**
     * Sets the password used in the SSH connection.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    protected void runLoopBody() {
        switch (this.state) {
            case DISCOVER:
                this.discover();
                break;
            case CONNECT_BUTTON_IS_PRESSED:
                this.connectButtonIsPressed();
                break;
            case WAIT_FOR_CMD:
                this.waitForCmd();
                break;
            case WAIT_UPLOAD:
                this.waitUpload();
                break;
            case WAIT_EXECUTION:
                LOG.info("Program execution finished - enter WAIT_FOR_CMD state again");
                this.fire(State.WAIT_FOR_CMD);
                break;
            default:
                break;
        }
    }

    protected void discover() {
        if (this.token.isEmpty()) { // not connected yet
            this.fire(State.WAIT_FOR_CONNECT_BUTTON_PRESS);
        } else { // robot is already connected
            this.fire(State.WAIT_FOR_CMD);
        }
    }

    protected void connectButtonIsPressed() {
        this.token = OraTokenGenerator.generateToken();
        this.fire(State.WAIT_FOR_SERVER);

        JSONObject deviceInfo = this.communicator.getDeviceInfo();
        deviceInfo.put(KEY_TOKEN, this.token);
        deviceInfo.put(KEY_CMD, CMD_REGISTER);
        try {
            //Blocks until the server returns command in its response
            JSONObject serverResponse = this.serverCommunicator.pushRequest(deviceInfo);
            String command = serverResponse.getString("cmd");
            if (command.equals(CMD_REPEAT)) {
                LOG.info("registration successful");
                this.fire(State.WAIT_FOR_CMD);
            } else if (command.equals(CMD_ABORT)) {
                LOG.info("registration timeout");
                this.fire(State.TOKEN_TIMEOUT);
                this.fire(State.DISCOVER);
            } else {
                LOG.error("Unexpected command {} from server", command);
                this.reset(State.ERROR_HTTP);
                this.resetLastConnectionData();
            }
        } catch (IOException | UnsupportedOperationException | JSONException e) {
            LOG.info("SERVER COMMUNICATION ERROR {}", e.getMessage());
            this.reset(State.ERROR_HTTP);
            this.resetLastConnectionData();
        }
    }

    protected void waitForCmd() {
        JSONObject deviceInfoWaitCMD = this.communicator.getDeviceInfo();
        deviceInfoWaitCMD.put(KEY_TOKEN, this.token);
        deviceInfoWaitCMD.put(KEY_CMD, CMD_PUSH);

        try {
            JSONObject pushRequestResponse = this.serverCommunicator.pushRequest(deviceInfoWaitCMD);
            String serverCommand = pushRequestResponse.getString(KEY_CMD);

            if (serverCommand.equals(CMD_REPEAT)) {
                // do nothing
            } else if (serverCommand.equals(CMD_DOWNLOAD)) {
                this.fire(State.WAIT_UPLOAD);
            } else {
                LOG.info("WAIT_FOR_CMD {}", "Unexpected response from server");
                this.resetLastConnectionData();
                this.reset(State.ERROR_HTTP);
            }
        } catch (IOException e) {
            LOG.info("WAIT_FOR_CMD {}", e.getMessage());
            this.resetLastConnectionData();
            this.reset(State.ERROR_HTTP);
        }
    }

    protected void waitUpload() {
        try {
            this.communicator.setPassword(this.password);
            Pair<byte[], String> program = getProgram();
            this.communicator.uploadFile(program.getFirst(), program.getSecond());
            this.fire(State.WAIT_EXECUTION);
        } catch (UserAuthException e) {
            LOG.error("Could not authorize user: {}", e.getMessage());
            this.reset(ERROR_AUTH);
        } catch (IOException e) {
            LOG.error("Something went wrong: {}", e.getMessage());
            this.reset(ERROR_UPLOAD_TO_ROBOT);
        }
    }

    protected Pair<byte[], String> getProgram() throws IOException {
        JSONObject deviceInfo = this.communicator.getDeviceInfo();
        deviceInfo.put(KEY_TOKEN, this.token);
        deviceInfo.put(KEY_CMD, CMD_REGISTER); // TODO why is the command register

        return this.serverCommunicator.downloadProgram(deviceInfo);
    }

    private void resetLastConnectionData() {
        LOG.info("resetting");
        this.token = "";
    }
}
