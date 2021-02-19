package de.fhg.iais.roberta.ui.main;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.IWiredRobot;
import de.fhg.iais.roberta.connection.wired.arduino.Arduino;
import de.fhg.iais.roberta.connection.wired.microbit.Microbit;
import de.fhg.iais.roberta.connection.wireless.RaspberrypiDetector;
import de.fhg.iais.roberta.connection.wireless.nao.Nao;
import de.fhg.iais.roberta.connection.wireless.nao.NaoConnector;
import de.fhg.iais.roberta.connection.wireless.raspberrypi.Raspberrypi;
import de.fhg.iais.roberta.main.UpdateHelper;
import de.fhg.iais.roberta.main.UpdateInfo;
import de.fhg.iais.roberta.main.UpdateInfo.Status;
import de.fhg.iais.roberta.ui.IController;
import de.fhg.iais.roberta.ui.OraPopup;
import de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorController;
import de.fhg.iais.roberta.ui.serialMonitor.SerialMonitorController;
import de.fhg.iais.roberta.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static de.fhg.iais.roberta.ui.main.HelpDialog.*;
import static de.fhg.iais.roberta.ui.main.MainView.*;
import static java.awt.Image.SCALE_AREA_AVERAGING;

public class MainController implements IController, IOraListenable<IRobot> {
    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private static final String FILENAME_ROBERTA = "Roberta.png";
    private static final int UPLOAD_ERROR_LINES = 4;

    private final Collection<IOraListener<IRobot>> listeners = new ArrayList<>(5);

    // View related
    private final ResourceBundle rb;
    private final MainView mainView;

    private final CustomAddressHelper addresses = new CustomAddressHelper();
    // Child controllers of the main controller, this includes other windows/JFrames that are launched from the main controller
    private final SerialMonitorController serialMonitorController;
    private final DeviceIdEditorController deviceIdEditorController;
    private final HelpDialog helpDialog;
    // For the robot selection if there is more than one robot available
    private List<IRobot> robotList = null;
    private boolean connected;
    private boolean scan;
    private IConnector<?> connector = null;
    private SshBean sshCredentials = null;

    public MainController(ResourceBundle rb) {
        MainViewListener mainViewListener = new MainViewListener();
        this.mainView = new MainView(rb, mainViewListener);
        this.mainView.setVisible(true);
        this.rb = rb;
        this.connected = false;
        this.scan = false;


        this.mainView.setCustomAddresses(this.addresses.get());

        this.serialMonitorController = new SerialMonitorController(this.rb);

        this.helpDialog = new HelpDialog(this.mainView, rb, mainViewListener);
        // Update location of help dialog when moving the main window
        this.mainView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                helpDialog.setLocation(mainView.getRobotButtonLocation());
            }
        });
        // Check for updates on startup
        this.checkForUpdates();

        this.deviceIdEditorController = new DeviceIdEditorController(rb);
    }

    private void checkForUpdates() {
        UpdateInfo updateInfo = UpdateHelper.checkForUpdates();
        if (updateInfo.getStatus() == Status.NEWER_VERSION) {
            this.showAttentionPopup("connectorUpdateAvailable", updateInfo.getName(), updateInfo.getUrl());
        }
        this.mainView.setUpdateButton(updateInfo.getStatus());
    }

    public void setRobotList(Set<? extends IRobot> robotList) {
        this.robotList = new ArrayList<>(robotList);
        this.mainView.showTopRobots(this.robotList.stream().map(IRobot::getName).collect(Collectors.toList()));
    }

    public Class<? extends IDetector> getRobotType() {
        return this.mainView.getSelectedRobotType();
    }

    @Override
    public void setState(State state) {
        LOG.info("Setting state to {}", state);
        switch (state) {
            case DISCOVER:
                this.setDiscover();
                break;
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                this.connected = false;
                this.scan = false;
                this.mainView.toggleScan();

                this.mainView.setWaitForConnect(this.connector.getRobot().getName(), this.connector.getRobot().getConnectionType());

                if (this.connector.getRobot() instanceof IWiredRobot) {
                    this.mainView.showArduinoMenu();
                    this.mainView.setArduinoMenuText(this.connector.getRobot().getName());
                }
                break;
            case WAIT_FOR_SERVER:
                this.mainView.setNew(
                        this.connector.getRobot().getConnectionType(),
                        this.rb.getString("token"),
                        this.connector.getToken(),
                        this.connector.getCurrentServerAddress(),
                        true);
                this.mainView.setWaitForServer();
                break;
            case RECONNECT:
                this.mainView.setConnectButtonText(this.rb.getString("disconnect"));
            case WAIT_FOR_CMD:
                this.connected = true;
                this.mainView.setNew(
                        this.connector.getRobot().getConnectionType(),
                        this.rb.getString("name"),
                        this.connector.getRobot().getName(),
                        this.connector.getCurrentServerAddress(),
                        false);
                this.mainView.setWaitForCmd(this.connector.getRobot().getConnectionType());
                break;
            case WAIT_UPLOAD:
                if (this.connector.getRobot() instanceof Nao) {
                    String password = this.mainView.getSshPassword();
                    ((NaoConnector) this.connector).setPassword(password);
                } else if (this.connector.getRobot() instanceof Raspberrypi) {
                    String userName = this.mainView.getSshUserName();
                    String password = this.mainView.getSshPassword();
                    int port = this.mainView.getSshPort();
                    ((Raspberrypi) this.connector.getRobot()).setUserName(userName);
                    ((Raspberrypi) this.connector.getRobot()).setPassword(password);
                    ((Raspberrypi) this.connector.getRobot()).setPort(port);
                }
                break;
            case WAIT_EXECUTION:
                this.mainView.setWaitExecution();
                break;
            case UPDATE_SUCCESS:
                this.showAttentionPopup("restartInfo");
                break;
            case UPDATE_FAIL:
                this.showAttentionPopup("updateFail");
                break;
            case ERROR_HTTP:
                this.showAttentionPopup("httpErrorInfo");
                break;
            case ERROR_DOWNLOAD:
                this.showAttentionPopup("downloadFail");
                break;
            case ERROR_BRICK:
                this.showAttentionPopup("httpBrickInfo");
                break;
            case ERROR_AUTH:
                this.showAttentionPopup("errorAuth");
                break;
            case ERROR_UPLOAD_TO_ROBOT:
                List<String> additionalInfo = new ArrayList<>(state.getAdditionalInfo());
                if (additionalInfo.isEmpty()) {
                    this.showAttentionPopup("errorUploadToRobot", "");
                } else {
                    String errorOutput = additionalInfo.get(0);
                    this.showAttentionPopup("errorUploadToRobot", getLastNLines(errorOutput, UPLOAD_ERROR_LINES));
                }
                break;
            case ERROR_PYTHON_REQUIRE:
                this.showAttentionPopup("errorPythonRequire");
                break;
            case TOKEN_TIMEOUT:
                this.showAttentionPopup("tokenTimeout");
                break;
            default:
                break;
        }
    }

    private String getLastNLines(String s, int n) {
        List<String> lines = Arrays.asList(s.split("\n"));
        List<String> lastNLines = lines.subList(Math.max(lines.size() - n, 0), lines.size());
        StringBuilder sb = new StringBuilder();
        for (String line : lastNLines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void setConnector(IConnector<?> connector) {
        LOG.debug("setConnector: {}", connector.getClass().getSimpleName());
        this.connector = connector;
        this.connector.registerListener(this::setState);

        this.mainView.showTopTokenServer();

        // Serial monitor is only needed for serial supporting robots
        if ((connector.getRobot() instanceof Arduino) || (connector.getRobot() instanceof Microbit)) {
            this.serialMonitorController.setConnector(connector);
        }
    }

    public void showConfigErrorPopup(Map<Integer, String> errors) {
        StringBuilder sb = new StringBuilder(200);
        sb.append(System.lineSeparator());
        for (Entry<Integer, String> entry : errors.entrySet()) {
            sb.append("Line ").append(entry.getKey()).append(": ").append(this.rb.getString(entry.getValue())).append(System.lineSeparator());
        }
        LOG.error("Errors in config file:{}", sb);

        this.showAttentionPopup("errorReadConfig", sb.toString());
    }

    public void showHelp() {
        this.helpDialog.setLocation(this.mainView.getRobotButtonLocation());
        this.helpDialog.setVisible(true);
    }

    private void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.scan = true;
        this.mainView.setDiscover();
        this.serialMonitorController.setState(State.DISCOVER);
    }

    private void setIdle() {
        LOG.debug("setIdle");
        this.mainView.setIdle();
        this.scan = false;
        this.serialMonitorController.setState(State.IDLE);
    }

    private void showAttentionPopup(String key, String... entries) {
        OraPopup.showPopup(this.mainView, "attention", key, this.rb, null, new String[]{"ok"}, entries);
    }

    @Override
    public void registerListener(IOraListener<IRobot> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<IRobot> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(IRobot object) {
        for (IOraListener<IRobot> listener : this.listeners) {
            listener.update(object);
        }
    }

    public boolean isScan() {
        return this.scan;
    }

    private class MainViewListener implements IOraUiListener {


        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("User performed action {}", e.getActionCommand());
            switch (e.getActionCommand()) {
                case CMD_EXIT:
                    this.closeApplication();
                    break;
                case CMD_ABOUT:
                    this.showAboutPopup();
                    break;
                case CMD_SELECT_ROBOTTYPE:
                    MainController.this.mainView.selectRobotType();
                    break;
                case CMD_SCAN:
                    MainController.this.setDiscover();
                    if (MainController.this.getRobotType().isNestmateOf(RaspberrypiDetector.class)) {
                        String address = MainController.this.mainView.getSshAddress();
                        String userName = MainController.this.mainView.getSshUserName();
                        String password = MainController.this.mainView.getSshPassword();
                        int port = MainController.this.mainView.getSshPort();
                        try {
                            MainController.this.sshCredentials = new SshBean(InetAddress.getByName(address),
                                    userName, password, port);
                        } catch (UnknownHostException unknownHostException) {
                            unknownHostException.printStackTrace();
                        }
                        LOG.info(String.valueOf(MainController.this.sshCredentials));
                    }
                    break;
                case CMD_STOP_SCAN:
                    MainController.this.setIdle();
                    break;
                case CMD_SERIAL:
                    MainController.this.serialMonitorController.showSerialMonitor();
                    break;
                case CMD_CONNECT:
                    this.checkForValidCustomServerAddressAndUpdate();
                    MainController.this.connector.connect();
                    break;
                case CMD_DISCONNECT:
                    setDiscover(); // first!, order is important, otherwise ui thread does into deadlock
                    MainController.this.connector.close();
                    break;
                case CMD_HELP:
                    this.toggleHelp();
                    break;
                case CMD_ID_EDITOR:
                    MainController.this.deviceIdEditorController.showEditor();
                    if (MainController.this.connector != null) {
                        MainController.this.connector.close();
                    }
                    MainController.this.setDiscover();
                    break;
                case CMD_SELECT_EV3:
                    MainController.this.helpDialog.dispose();
                    try {
                        Desktop.getDesktop().browse(new URI(MainController.this.rb.getString("linkEv3UsbWiki")));
                    } catch (IOException | URISyntaxException e1) {
                        LOG.error("Could not open browser: {}", e1.getMessage());
                    }
                    break;
                case CMD_SELECT_NAO:
                    MainController.this.helpDialog.dispose();
                    try {
                        Desktop.getDesktop().browse(new URI(MainController.this.rb.getString("linkNaoWiki")));
                    } catch (IOException | URISyntaxException e1) {
                        LOG.error("Could not open browser: {}", e1.getMessage());
                    }
                    break;
                case CMD_SELECT_OTHER:
                    MainController.this.helpDialog.dispose();
                    MainController.this.deviceIdEditorController.showEditor();
                    if (MainController.this.connector != null) {
                        MainController.this.connector.close();
                    }
                    MainController.this.setDiscover();
                    break;
                case CMD_CLOSE_HELP:
                    MainController.this.helpDialog.dispose();
                    break;
                case CMD_COPY:
                    Transferable stringSelection = new StringSelection(MainController.this.connector.getToken());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                    break;
                case CMD_CHECK_FOR_UPDATES:
                    MainController.this.checkForUpdates();
                    break;
                default:
                    throw new UnsupportedOperationException("Action " + e.getActionCommand() + " is not implemented!");
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            LOG.info("User closed main window");
            this.closeApplication();
        }

        // Sends event to all listeners waiting for the robot selection event when a robot was selected
        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();

            if (!lsm.isSelectionEmpty()) {
                // Only one can be selected
                MainController.this.fire(MainController.this.robotList.get(lsm.getMinSelectionIndex()));
            }
        }

        private void showAboutPopup() {
            OraPopup.showPopup(MainController.this.mainView,
                    "about",
                    "aboutInfo",
                    MainController.this.rb,
                    new ImageIcon(ImageHelper.getIcon("iais_logo.gif").getImage().getScaledInstance(100, 27, SCALE_AREA_AVERAGING)),
                    new String[]{"ok"},
                    PropertyHelper.getInstance().getProperty("version"));
        }

        private void toggleHelp() {
            MainController.this.helpDialog.setLocation(MainController.this.mainView.getRobotButtonLocation());
            MainController.this.helpDialog.setVisible(!MainController.this.helpDialog.isVisible());
        }

        private void checkForValidCustomServerAddressAndUpdate() {
            LOG.debug("checkForValidCustomServerAddressAndUpdate");

            Pair<String, String> address = MainController.this.mainView.getCustomAddress();
            String ip = address.getFirst();
            String port = address.getSecond();

            if (ip.isEmpty()) {
                LOG.warn("Invalid custom address - Using default address");
                MainController.this.connector.resetToDefaultServerAddress();
            } else {
                if (port.isEmpty()) {
                    LOG.info("Valid custom ip {}, using default ports", ip);
                    MainController.this.connector.updateCustomServerAddress(ip);
                    MainController.this.addresses.add(address);
                    MainController.this.mainView.setCustomAddresses(MainController.this.addresses.get());
                } else {
                    if (CustomAddressHelper.validatePort(port)) {
                        String formattedAddress = ip + ':' + port;
                        LOG.info("Valid custom address {}", formattedAddress);
                        MainController.this.connector.updateCustomServerAddress(formattedAddress);
                        MainController.this.addresses.add(address);
                        MainController.this.mainView.setCustomAddresses(MainController.this.addresses.get());
                    } else {
                        LOG.warn("Invalid port {}", port);
                    }
                }
            }

        }

        private void closeApplication() {
            LOG.debug("closeApplication");
            MainController.this.addresses.save();
            if (MainController.this.connected) {
                String[] buttons = {
                        "exit", "cancel"
                };
                int
                        n =
                        OraPopup.showPopup(MainController.this.mainView,
                                "attention",
                                "confirmCloseInfo",
                                MainController.this.rb,
                                ImageHelper.getIcon(FILENAME_ROBERTA),
                                buttons);
                if (n == 0) {
                    if (MainController.this.connector != null) {
                        MainController.this.connector.close();
                    }
                    System.exit(0);
                }
            } else {
                System.exit(0);
            }
        }
    }
}
