package de.fhg.iais.roberta.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.fhg.iais.roberta.util.IOraListenable;

/**
 * Defines a set of states, keywords and methods for handling the USB connection of a robot to the Open Roberta server. This interface is intended to be
 * implemented by all connector classes for different robot types.
 *
 * @author dpyka
 */
public interface IConnector<T extends IRobot> extends IOraListenable<IConnector.State> {

    enum State {
        DISCOVER,
        RECONNECT,
        WAIT_FOR_CONNECT_BUTTON_PRESS,
        CONNECT_BUTTON_IS_PRESSED,
        WAIT_FOR_CMD,
        WAIT_UPLOAD,
        WAIT_EXECUTION,
        DISCONNECT,
        WAIT_FOR_SERVER,
        UPDATE_SUCCESS,
        UPDATE_FAIL,
        ERROR_HTTP,
        ERROR_UPDATE,
        ERROR_BRICK,
        ERROR_DOWNLOAD,
        ERROR_AUTH,
        ERROR_UPLOAD_TO_ROBOT,
        ERROR_MISSING_PASSWORD,
        ERROR_PYTHON_REQUIRE,
        TOKEN_TIMEOUT;

        private List<String> additionalInfo = new ArrayList<>();

        /**
         * Optional info that may be needed by users of the State enum.
         * @param additionalInfo a variable number of strings to be added
         * @return the State object for chaining
         */
        public State setAdditionalInfo(String... additionalInfo) {
            this.additionalInfo = Collections.unmodifiableList(Arrays.asList(additionalInfo));
            return this;
        }

        /**
         * Returns all additional info. May be empty.
         * @return an unmodifiable list of additional info
         */
        public List<String> getAdditionalInfo() {
            return additionalInfo;
        }
    }

    String KEY_TOKEN = "token";
    String KEY_CMD = "cmd";

    String CMD_REGISTER = "register";
    String CMD_PUSH = "push";
    String CMD_ISRUNNING = "isrunning";

    String CMD_REPEAT = "repeat";
    String CMD_ABORT = "abort";
    String CMD_UPDATE = "update";
    String CMD_DOWNLOAD = "download";
    String CMD_CONFIGURATION = "configuration";

    /**
     * Runs this connector.
     * Starts the main loop to communicate with the server and the robot.
     */
    void run();

    /**
     * Tell the connector to collect necessary data from the robot and initialise a registration to Open Roberta.
     */
    void connect();

    /**
     * Shut down the connector for closing the Connector.
     */
    void close();

    /**
     * Get the token to display in the gui.
     *
     * @return the token
     */
    String getToken();

    /**
     * Returns the robot the connector is currently connected with.
     *
     * @return the current robot
     */
    T getRobot();

    String getCurrentServerAddress();

    /**
     * Update the server communicator's address to which it will connect.
     *
     * @param customServerAddress the specified server address
     */
    void updateCustomServerAddress(String customServerAddress);

    /**
     * If gui fields are empty but advanced options is checked, use the default server address.
     */
    void resetToDefaultServerAddress();
}
