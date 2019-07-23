package de.fhg.iais.roberta.connection;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.util.PropertyHelper;

public abstract class AbstractConnector<T extends IRobot> implements IConnector<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConnector.class);

    private final Collection<IOraListener<State>> listeners = new ArrayList<>();

    private final String defaultServerAddress;
    protected final ServerCommunicator serverCommunicator;

    protected JSONObject brickData = null;

    protected State state = State.DISCOVER; // First state when program starts
    protected String token = "";
    protected T robot;

    protected AbstractConnector(T robot) {
        String serverIp = PropertyHelper.getInstance().getProperty("serverIp");
        String serverPort = PropertyHelper.getInstance().getProperty("serverPort");
        this.defaultServerAddress = serverIp + ':' + serverPort;
        this.serverCommunicator = new ServerCommunicator(this.defaultServerAddress);
        this.robot = robot;
    }

    private boolean running = false;

    @Override
    public void run() {
        this.running = true;
        LOG.info("Starting {} connector with server address {}", this.robot.getName(), this.defaultServerAddress);
        while ( this.running ) {
            this.runLoopBody();

            // Needed, otherwise the state change does not get through
            try {
                Thread.sleep(1L);
            } catch ( InterruptedException e ) {
                LOG.error("Interrupt triggered inside run: {}", e.getMessage());
            }
        }
    }

    protected abstract void runLoopBody();

    @Override
    public void connect() {
        this.state = State.CONNECT_BUTTON_IS_PRESSED;
    }

    @Override
    public void close() {
        this.running = false;
        this.fire(State.DISCOVER);
    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public T getRobot() {
        return this.robot;
    }

    @Override
    public String getCurrentServerAddress() {
        return this.serverCommunicator.getServerAddress();
    }

    @Override
    public void updateCustomServerAddress(String customServerAddress) {
        this.serverCommunicator.setServerAddress(customServerAddress);
        LOG.info("Now using custom address {}", customServerAddress);
    }

    @Override
    public void resetToDefaultServerAddress() {
        this.serverCommunicator.setServerAddress(this.defaultServerAddress);
        LOG.info("Now using default address {}", this.defaultServerAddress);
    }

    /**
     * Reset the Connector to the start state (discover).
     *
     * @param additionalErrorMessage Display a popup with error message. If this is null, we do not want to display the tooltip.
     */
    protected void reset(State additionalErrorMessage) {
        if ( additionalErrorMessage != null ) {
            this.fire(additionalErrorMessage);
        }
        this.fire(State.DISCOVER);
    }

    @Override
    public void registerListener(IOraListener<State> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<State> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(State object) {
        this.state = object;
        for ( IOraListener<State> listener : this.listeners ) {
            listener.update(object);
        }
    }
}
