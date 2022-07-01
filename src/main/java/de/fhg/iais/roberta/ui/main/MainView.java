package de.fhg.iais.roberta.ui.main;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultEditorKit;

import de.fhg.iais.roberta.connection.IRobot.ConnectionType;
import de.fhg.iais.roberta.main.UpdateInfo.Status;
import de.fhg.iais.roberta.ui.OraButton;
import de.fhg.iais.roberta.ui.OraToggleButton;
import de.fhg.iais.roberta.ui.UiState;
import de.fhg.iais.roberta.util.IOraUiListener;
import de.fhg.iais.roberta.util.Pair;

import static de.fhg.iais.roberta.ui.main.ImageHelper.getGif;

public class MainView extends JFrame {
    private static final Logger LOG = LoggerFactory.getLogger(MainView.class);

    private static final long serialVersionUID = 1L;
    private static final int ADDITIONAL_ADVANCED_HEIGHT = 62;

    static final String CMD_EXIT = "exit";
    static final String CMD_CHECK_FOR_UPDATES = "updateConnector";
    static final String CMD_ABOUT = "about";
    static final String CMD_SERIAL = "serial";
    static final String CMD_SCAN = "scan";
    static final String CMD_CUSTOMADDRESS = "customaddress";
    static final String CMD_CONNECT = "connect";
    static final String CMD_DISCONNECT = "disconnect";
    static final String CMD_HELP = "help";
    static final String CMD_ID_EDITOR = "id_editor";
    static final String CMD_COPY = "copy";

    private static final Color BUTTON_FOREGROUND_COLOR = Color.WHITE;
    public static final Color BUTTON_BACKGROUND_COLOR = Color.decode("#b7d032"); // light lime green
    public static final Color HOVER_COLOR = Color.decode("#afca04"); // slightly darker lime green
    public static final Color BACKGROUND_COLOR = Color.WHITE;
    public static final Color TABLE_HEADER_BACKGROUND_COLOR = Color.decode("#dddddd");

    private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final Font MENU_FONT = FONT.deriveFont(12.0f);

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch ( ClassNotFoundException | IllegalAccessException | UnsupportedLookAndFeelException | InstantiationException e ) {
            LOG.error("Error when setting up the look and feel: {}", e.getMessage());
        }
        UIManager.put("MenuBar.background", BACKGROUND_COLOR);
        UIManager.put("Menu.background", BACKGROUND_COLOR);
        UIManager.put("Menu.selectionBackground", BUTTON_BACKGROUND_COLOR);
        UIManager.put("Menu.font", MENU_FONT);
        UIManager.put("MenuItem.background", BACKGROUND_COLOR);
        UIManager.put("MenuItem.selectionBackground", BUTTON_BACKGROUND_COLOR);
        UIManager.put("MenuItem.font", MENU_FONT);
        UIManager.put("Panel.background", BACKGROUND_COLOR);
        UIManager.put("CheckBox.background", BACKGROUND_COLOR);
        UIManager.put("Separator.foreground", Color.decode("#dddddd"));
        UIManager.put("TextField.background", BACKGROUND_COLOR);
        UIManager.put("TextField.font", FONT);
        UIManager.put("TextArea.font", FONT);
        UIManager.put("Label.font", FONT);
        UIManager.put("List.font", FONT);
        UIManager.put("Button.font", FONT);
        UIManager.put("Button.rollover", true);
        UIManager.put("Button.background", BUTTON_BACKGROUND_COLOR);
        UIManager.put("Button.foreground", BUTTON_FOREGROUND_COLOR);
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));
        UIManager.put("ToggleButton.font", FONT);
        UIManager.put("ToggleButton.rollover", true);
        UIManager.put("ToggleButton.background", BUTTON_BACKGROUND_COLOR);
        UIManager.put("ToggleButton.foreground", BUTTON_FOREGROUND_COLOR);
        UIManager.put("ToggleButton.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));
        UIManager.put("OptionPane.background", BACKGROUND_COLOR);
        UIManager.put("OptionPane.messageFont", FONT);
        UIManager.put("ToolTip.background", BACKGROUND_COLOR);
        UIManager.put("ToolTip.font", FONT);
        UIManager.put("EditorPane.font", FONT);
        UIManager.put("Button.font", FONT);
        UIManager.put("ComboBox.font", FONT);
        UIManager.put("ComboBox.background", BACKGROUND_COLOR);
        UIManager.put("ComboBox.disabledBackground", BACKGROUND_COLOR);
        UIManager.put("ComboBox.disabledForeground", BACKGROUND_COLOR);
        UIManager.put("ComboBox.selectionBackground", Color.decode("#dddddd"));
        UIManager.put("Table.font", FONT);
        UIManager.put("Table.alternateRowColor", new Color(240, 240, 240));
        UIManager.put("TableHeader.font", FONT);
        UIManager.put("TableHeader.background", TABLE_HEADER_BACKGROUND_COLOR);
        UIManager.put("ScrollPane.background", BACKGROUND_COLOR);

        // CMD + C support for copying on Mac OS
        if ( SystemUtils.IS_OS_MAC_OSX ) {
            InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
        }
    }

    // ---- Menu ----
    private final JMenuBar menu = new JMenuBar();

    private final JMenu menuFile = new JMenu();
    private final JMenuItem menuItemIdEditor = new JMenuItem();
    private final JMenuItem menuItemClose = new JMenuItem();

    private final JMenu menuArduino = new JMenu();
    private final JMenuItem menuItemSerial = new JMenuItem();

    private final JMenu menuInfo = new JMenu();
    private final JMenuItem menuItemUpdate = new JMenuItem();
    private final JMenuItem menuItemAbout = new JMenuItem();

    private final RobotButton butRobot = new RobotButton();

    // --- Center ---
    private final JPanel pnlCenter = new JPanel();

    private final JSeparator separator = new JSeparator();

    // -- Top --
    private final JPanel pnlTopContainer = new JPanel();

    // - Robot -
    private static final String CARD_ROBOT = "robot";
    private final JPanel pnlRobot = new JPanel();

    private final JLabel lblRobotNameInfo = new JLabel();
    private final JLabel lblRobotName = new JLabel();

    // - Robots -
    private static final String CARD_ROBOTS = "robots";
    private final JPanel pnlRobots = new JPanel();

    private final JPanel pnlAvailable = new JPanel();
    private final JLabel lblAvailable = new JLabel();

    private final JScrollPane scrollPaneRobots = new JScrollPane();
    private final JList<String> listRobots = new JList<>(new String[] { "" });

    // - Token Server -
    private static final String CARD_TOKEN_SERVER = "tokenServer";
    private final JPanel pnlTokenServer = new JPanel();

    private final JPanel pnlToken = new JPanel();
    private final JTextField txtFldPreToken = new JTextField();
    private final JTextField txtFldToken = new JTextField();
    private final OraButton butCopy = new OraButton();

    private final JPanel pnlServer = new JPanel();
    private final JTextField txtFldServer = new JTextField();

    // - Top Empty -
    private static final String CARD_TOP_EMPTY = "topEmpty";

    // -- Gif --
    private final JPanel pnlGif = new JPanel();

    // -- Info --
    private final JTextArea txtAreaInfo = new JTextArea();

    // -- Buttons --
    private final JPanel pnlButtons = new JPanel();
    private final OraToggleButton butConnect = new OraToggleButton();
    private final OraToggleButton butScan = new OraToggleButton();
    private final OraButton butClose = new OraButton();

    // -- Custom --
    private final JPanel pnlCustomContainer = new JPanel();

    // - Address -
    private static final String CARD_ADDRESS = "address";
    private final JPanel pnlAddress = new JPanel();

    private final JPanel pnlCustomInfo = new JPanel();
    private final JButton butCustom = new JButton();

    private final JPanel pnlCustomHeading = new JPanel();
    private final JTextField txtFldCustomHeading = new JTextField();

    private final JPanel pnlCustomAddress = new JPanel();
    private final JLabel lblCustomIp = new JLabel();
    private final JComboBox<String> cmbBoxCustomIp = new JComboBox<>();
    private final JLabel lblCustomPort = new JLabel();
    private final JComboBox<String> cmbBoxCustomPort = new JComboBox<>();

    // - Nao Login -
    private static final String CARD_NAO_LOGIN = "naoLogin";
    private final JPanel pnlNaoLogin = new JPanel();
    private final JLabel lblNaoPassword = new JLabel();
    private final JTextField txtFldRobotPassword = new JTextField();
    // - Custom Empty -
    private static final String CARD_CUSTOM_EMPTY = "customEmpty";

    // Resources
    private static final String FILENAME_UNCHECKED = "input-unchecked.png";
    private static final String FILENAME_CHECKED = "input-checked.png";
    private static final String FILENAME_CLIPBOARD = "clipboard.png";

    private final ResourceBundle messages;

    private boolean toggle = true;
    private boolean customMenuVisible;

    MainView(ResourceBundle messages, IOraUiListener listener) {
        this.messages = messages;

        this.initGUI();
        this.setDiscover();

        this.setWindowListener(listener);
        this.setListSelectionListener(listener);
        this.setActionListener(listener);
    }

    private void initGUI() {
        this.initGeneralGUI();
        this.initMenuGUI();
        this.initCenterGUI();

        this.pack();
        // center on desktop
        this.setLocationRelativeTo(null);

        // hide the advanced options at start
        this.pnlCustomHeading.setVisible(false);
        this.pnlCustomAddress.setVisible(false);
    }

    private void initGeneralGUI() {
        // General
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Titlebar
        this.setIconImage(ImageHelper.getTitleIconImage());
        this.setTitle(this.messages.getString("title"));
    }

    private void initMenuGUI() {
        // General
        this.add(this.menu, BorderLayout.PAGE_START);
        this.menu.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // File
        this.menu.add(this.menuFile);
        this.menuFile.setText(this.messages.getString("file"));
        this.menuFile.add(this.menuItemIdEditor);
        this.menuItemIdEditor.setText(this.messages.getString("idEditor"));
        this.menuItemIdEditor.setActionCommand(CMD_ID_EDITOR);
        this.menuFile.add(this.menuItemClose);
        this.menuItemClose.setText(this.messages.getString("exit"));
        this.menuItemClose.setActionCommand(CMD_EXIT);

        // Arduino
        this.menu.add(this.menuArduino);
        this.menuArduino.setText("Arduino");
        this.menuArduino.add(this.menuItemSerial);
        this.menuItemSerial.setText(this.messages.getString("serialMonitor"));
        this.menuItemSerial.setActionCommand(CMD_SERIAL);

        // Info
        this.menu.add(this.menuInfo);
        this.menuInfo.setText(this.messages.getString("info"));
        this.menuInfo.add(this.menuItemUpdate);
        this.setUpdateButton(Status.SAME_VERSION); // set to check for new initially
        this.menuItemUpdate.setActionCommand(CMD_CHECK_FOR_UPDATES);
        this.menuInfo.add(this.menuItemAbout);
        this.menuItemAbout.setText(this.messages.getString("about"));
        this.menuItemAbout.setActionCommand(CMD_ABOUT);

        this.menu.add(Box.createHorizontalGlue());

        // Icon
        this.menu.add(this.butRobot);
        this.butRobot.setState(UiState.DISCOVERING);
        this.butRobot.setActionCommand(CMD_HELP);
    }

    private void initCenterGUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        this.add(panel);

        // Separator
        panel.add(this.separator);

        // General
        panel.add(this.pnlCenter, BorderLayout.CENTER);
        this.pnlCenter.setLayout(new GridBagLayout());
        this.pnlCenter.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.weightx = 1.0;

        constraints.gridy = 0;
        this.initTopGUI(constraints);

        constraints.gridy = 1;
        this.initMainGifGUI(constraints);

        constraints.gridy = 2;
        this.initTextInfoGUI(constraints);

        constraints.gridy = 3;
        this.initButtonGUI(constraints);

        constraints.gridy = 4;
        this.initCustomGUI(constraints);
    }

    private void initTopGUI(GridBagConstraints constraints) {
        // Top container
        this.pnlCenter.add(this.pnlTopContainer, constraints);
        this.pnlTopContainer.setLayout(new CardLayout());
        this.pnlTopContainer.setPreferredSize(new Dimension(0, 100));

        this.initRobotGUI();
        this.initRobotListGUI();
        this.initTokenServerGUI();

        this.pnlTopContainer.add(new JPanel(), CARD_TOP_EMPTY);
    }

    private void initRobotGUI() {
        this.pnlTopContainer.add(this.pnlRobot, CARD_ROBOT);
        this.pnlRobot.setLayout(new BoxLayout(this.pnlRobot, BoxLayout.PAGE_AXIS));
        this.pnlRobot.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        // Robot name
        this.pnlRobot.add(this.lblRobotNameInfo);
        this.lblRobotNameInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.lblRobotNameInfo.setText(this.messages.getString("foundRobot") + ':');
        this.pnlRobot.add(this.lblRobotName);
        this.lblRobotName.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.lblRobotName.setFont(FONT.deriveFont(18.0f));
    }

    private void initRobotListGUI() {
        this.pnlTopContainer.add(this.pnlRobots, CARD_ROBOTS);
        this.pnlRobots.setLayout(new BoxLayout(this.pnlRobots, BoxLayout.PAGE_AXIS));

        // Info label
        this.pnlRobots.add(this.pnlAvailable);
        this.pnlAvailable.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlAvailable.add(this.lblAvailable);
        this.lblAvailable.setText(this.messages.getString("listInfo"));

        // Robots
        this.pnlRobots.add(this.scrollPaneRobots);
        this.scrollPaneRobots.setViewportView(this.listRobots);
    }

    private void initTokenServerGUI() {
        // Token and Server panel
        this.pnlTopContainer.add(this.pnlTokenServer, CARD_TOKEN_SERVER);
        this.pnlTokenServer.setLayout(new BoxLayout(this.pnlTokenServer, BoxLayout.PAGE_AXIS));

        // Token panel
        this.pnlTokenServer.add(this.pnlToken);

        this.pnlToken.add(this.txtFldPreToken);
        this.txtFldPreToken.setFont(FONT.deriveFont(18.0f));
        this.txtFldPreToken.setBorder(BorderFactory.createEmptyBorder());
        this.txtFldPreToken.setEditable(false);

        this.pnlToken.add(this.txtFldToken);
        this.txtFldToken.setFont(FONT.deriveFont(18.0f));
        this.txtFldToken.setBorder(BorderFactory.createEmptyBorder());
        this.txtFldToken.setEditable(false);

        this.pnlToken.add(this.butCopy);
        this.butCopy.setIcon(ImageHelper.getIcon(FILENAME_CLIPBOARD));
        this.butCopy.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        this.butCopy.setActionCommand(CMD_COPY);

        // Server panel
        this.pnlTokenServer.add(this.pnlServer);

        this.pnlServer.add(this.txtFldServer);
        this.txtFldServer.setBorder(BorderFactory.createEmptyBorder());
        this.txtFldServer.setEditable(false);
    }

    private void initMainGifGUI(GridBagConstraints constraints) {
        // Main gif panel
        this.pnlCenter.add(this.pnlGif, constraints);
        this.pnlGif.setLayout(new CardLayout());
        ImageIcon gif = getGif(UiState.DISCOVERING, ConnectionType.WIRED);
        this.pnlGif.setPreferredSize(new Dimension(gif.getIconWidth(), gif.getIconHeight()));

        for ( UiState state : UiState.values() ) {
            for ( ConnectionType connectionType : ConnectionType.values() ) {
                JLabel label = new JLabel();
                label.setIcon(getGif(state, connectionType));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                this.pnlGif.add(label, state.toString() + connectionType);
            }
        }
    }

    private void initTextInfoGUI(GridBagConstraints constraints) {
        // Info texts
        this.pnlCenter.add(this.txtAreaInfo, constraints);
        this.txtAreaInfo.setLineWrap(true);
        this.txtAreaInfo.setWrapStyleWord(true);
        this.txtAreaInfo.setMargin(new Insets(8, 16, 8, 16));
        this.txtAreaInfo.setEditable(false);
        this.txtAreaInfo.setPreferredSize(new Dimension(0, 100));
    }

    private void initButtonGUI(GridBagConstraints constraints) {
        // Button panel
        this.pnlCenter.add(this.pnlButtons, constraints);
        this.pnlButtons.setLayout(new FlowLayout(FlowLayout.LEADING, 12, 0));

        this.pnlButtons.add(this.butConnect);
        this.butConnect.setText(this.messages.getString("connect"));
        this.butConnect.setActionCommand(CMD_CONNECT);

        this.pnlButtons.add(this.butScan);
        this.butScan.setText(this.messages.getString("scan"));
        this.butScan.setActionCommand(CMD_SCAN);

        this.pnlButtons.add(this.butClose);
        this.butClose.setText(this.messages.getString("exit"));
        this.butClose.setActionCommand(CMD_EXIT);
    }

    private void initCustomGUI(GridBagConstraints constraints) {
        this.pnlCenter.add(this.pnlCustomContainer, constraints);
        this.pnlCustomContainer.setLayout(new CardLayout());

        this.initAddressGUI();
        this.initNaoLogin();

        this.pnlCustomContainer.add(new JPanel(), CARD_CUSTOM_EMPTY);
    }

    private void initNaoLogin() {
        this.pnlCustomContainer.add(this.pnlNaoLogin, CARD_NAO_LOGIN);
        this.pnlNaoLogin.setLayout(new BoxLayout(this.pnlNaoLogin, BoxLayout.PAGE_AXIS));

        this.pnlNaoLogin.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel pnlPassword = new JPanel();
        this.pnlNaoLogin.add(pnlPassword);
        pnlPassword.setLayout(new BoxLayout(pnlPassword, BoxLayout.LINE_AXIS));
        pnlPassword.add(this.lblNaoPassword);
        pnlPassword.add(Box.createRigidArea(new Dimension(6, 0)));
        this.lblNaoPassword.setText(this.messages.getString("password") + ':');
        pnlPassword.add(this.txtFldRobotPassword);
    }

    private void initAddressGUI() {
        // Custom info panel
        this.pnlCustomContainer.add(this.pnlAddress, CARD_ADDRESS);

        this.pnlAddress.setLayout(new BoxLayout(this.pnlAddress, BoxLayout.PAGE_AXIS));

        this.pnlAddress.add(this.pnlCustomInfo);
        this.pnlCustomInfo.setLayout(new FlowLayout(FlowLayout.LEADING));

        this.pnlCustomInfo.add(this.butCustom);
        this.butCustom.setActionCommand("customaddress");
        this.butCustom.setIcon(ImageHelper.getIcon(FILENAME_UNCHECKED));
        this.butCustom.setText(this.messages.getString("checkCustomDesc"));
        this.butCustom.setBorderPainted(false);
        this.butCustom.setBackground(Color.WHITE);
        this.butCustom.setFocusPainted(false);
        this.butCustom.setContentAreaFilled(false);
        this.butCustom.setForeground(this.lblCustomIp.getForeground());
        this.butCustom.setMargin(new Insets(0, 0, 0, 0));

        // Custom heading panel
        this.pnlAddress.add(this.pnlCustomHeading);
        this.pnlCustomHeading.setLayout(new FlowLayout(FlowLayout.LEADING));

        this.pnlCustomHeading.add(this.txtFldCustomHeading);
        this.txtFldCustomHeading.setEditable(false);
        this.txtFldCustomHeading.setBorder(null);
        this.txtFldCustomHeading.setText(this.messages.getString("customDesc"));

        // Custom address panel
        this.pnlAddress.add(this.pnlCustomAddress);
        this.pnlCustomAddress.setLayout(new FlowLayout(FlowLayout.LEADING));

        this.pnlCustomAddress.add(this.lblCustomIp);
        this.lblCustomIp.setBorder(null);
        this.lblCustomIp.setText(this.messages.getString("ip") + ':');

        this.pnlCustomAddress.add(this.cmbBoxCustomIp);
        this.cmbBoxCustomIp.setEditable(true);
        this.cmbBoxCustomIp.setPreferredSize(new Dimension(146, 25));
        this.cmbBoxCustomIp.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        ((JComponent) this.cmbBoxCustomIp.getComponent(0)).setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        this.pnlCustomAddress.add(this.lblCustomPort);
        this.lblCustomPort.setBorder(null);
        this.lblCustomPort.setText(this.messages.getString("port") + ':');

        this.pnlCustomAddress.add(this.cmbBoxCustomPort);
        this.cmbBoxCustomPort.setEditable(true);
        this.cmbBoxCustomPort.setPreferredSize(new Dimension(70, 25));
        this.cmbBoxCustomPort.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        ((JComponent) this.cmbBoxCustomPort.getComponent(0)).setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    }

    // Listeners
    private void setActionListener(ActionListener listener) {
        this.menuItemIdEditor.addActionListener(listener);
        this.menuItemClose.addActionListener(listener);
        this.menuItemUpdate.addActionListener(listener);
        this.menuItemAbout.addActionListener(listener);
        this.menuItemSerial.addActionListener(listener);
        this.butRobot.addActionListener(listener);
        this.butConnect.addActionListener(listener);
        this.butScan.addActionListener(listener);
        this.butClose.addActionListener(listener);
        this.butCustom.addActionListener(listener);
        this.butCopy.addActionListener(listener);
    }

    private void setWindowListener(WindowListener windowListener) {
        this.addWindowListener(windowListener);
    }

    private void setListSelectionListener(ListSelectionListener listener) {
        ListSelectionModel selectionModel = this.listRobots.getSelectionModel();
        selectionModel.addListSelectionListener(listener);
    }

    // States
    void setDiscover() {
        this.txtFldPreToken.setText("");
        this.txtFldToken.setText("");
        this.butCopy.setVisible(false);
        this.txtFldServer.setText("");
        this.butRobot.setState(UiState.DISCOVERING);
        this.butConnect.setText(this.messages.getString("connect"));
        this.butConnect.setSelected(false);
        this.butConnect.setEnabled(false);
        this.butConnect.setActionCommand(CMD_CONNECT);
        this.butScan.setEnabled(false);
        this.butScan.setSelected(false);
        this.txtAreaInfo.setText(this.messages.getString("plugInInfo"));
        ((CardLayout) this.pnlGif.getLayout()).show(this.pnlGif, UiState.DISCOVERING.toString() + ConnectionType.WIRED);
        this.hideArduinoMenu();
        this.showTopRobots(Collections.emptyList());
        this.showCustomAddress();
    }

    void setWaitForConnect(String robotName, ConnectionType connectionType) {
        this.butRobot.setState(UiState.DISCOVERED);
        this.butConnect.setEnabled(true);
        this.butScan.setEnabled(true);
        this.txtAreaInfo.setText(this.messages.getString("connectInfo"));
        ((CardLayout) this.pnlGif.getLayout()).show(this.pnlGif, UiState.DISCOVERED.toString() + connectionType);
        this.showTopRobot(robotName);
    }

    void setWaitForServer() {
        this.butConnect.setSelected(false);
        this.butConnect.setEnabled(false);
        this.showTopTokenServer();
    }

    void setNew(ConnectionType connectionType, String prefix, String token, String serverAddress, boolean showCopy) {
        this.butScan.setEnabled(false);
        this.txtFldPreToken.setText(prefix);
        this.txtFldToken.setText(token);
        // Reset preferred size
        this.txtFldPreToken.setPreferredSize(null);
        this.txtFldToken.setPreferredSize(null);
        // Add one pixel width to remove small scrolling
        this.txtFldPreToken.setPreferredSize(new Dimension((int) this.txtFldPreToken.getPreferredSize().getWidth() + 1,
                                                           (int) this.txtFldPreToken.getPreferredSize().getHeight()));
        this.txtFldToken.setPreferredSize(new Dimension((int) this.txtFldToken.getPreferredSize().getWidth() + 1,
                                                        (int) this.txtFldToken.getPreferredSize().getHeight()));
        this.butCopy.setVisible(showCopy);

        // strip default port from serverAddress
        String servAddress = serverAddress.replace(":443", "");

        this.txtFldServer.setText(this.messages.getString("connectedTo") + ' ' + servAddress);
        this.txtAreaInfo.setText(this.messages.getString("tokenInfo"));
        ((CardLayout) this.pnlGif.getLayout()).show(this.pnlGif, UiState.CONNECTING.toString() + connectionType);
    }

    void setWaitForCmd(ConnectionType connectionType) {
        this.butConnect.setText(this.messages.getString("disconnect"));
        this.butConnect.setEnabled(true);
        this.butConnect.setActionCommand(CMD_DISCONNECT);
        this.butRobot.setState(UiState.CONNECTED);
        this.txtAreaInfo.setText(this.messages.getString("serverInfo"));
        ((CardLayout) this.pnlGif.getLayout()).show(this.pnlGif, UiState.CONNECTED.toString() + connectionType);
    }

    void setWaitExecution() {
        if ( this.toggle ) {
            this.butRobot.setState(UiState.CONNECTED);
        } else {
            this.butRobot.setState(UiState.DISCOVERED);
        }
        this.toggle = !this.toggle;
    }

    // Individual settings and functions

    // Arduino
    void showArduinoMenu() {
        this.menuArduino.setVisible(true);
    }

    private void hideArduinoMenu() {
        this.menuArduino.setVisible(false);
    }

    void setArduinoMenuText(String text) {
        this.menuArduino.setText(text);
    }

    // Top
    void showTopRobots(List<String> robotNames) {
        this.listRobots.setListData(robotNames.toArray(new String[0]));
        CardLayout cl = (CardLayout) this.pnlTopContainer.getLayout();
        cl.show(this.pnlTopContainer, CARD_ROBOTS);
    }

    void showTopRobot(String robotName) {
        this.lblRobotName.setText(robotName);
        CardLayout cl = (CardLayout) this.pnlTopContainer.getLayout();
        cl.show(this.pnlTopContainer, CARD_ROBOT);
    }

    void showTopTokenServer() {
        CardLayout cl = (CardLayout) this.pnlTopContainer.getLayout();
        cl.show(this.pnlTopContainer, CARD_TOKEN_SERVER);
    }

    // Custom
    private void showCustomAddress() {
        CardLayout cl = (CardLayout) this.pnlCustomContainer.getLayout();
        cl.show(this.pnlCustomContainer, CARD_ADDRESS);
    }

    void showCustomNaoLogin() {
        CardLayout cl = (CardLayout) this.pnlCustomContainer.getLayout();
        cl.show(this.pnlCustomContainer, CARD_NAO_LOGIN);
    }

    private void showAdvancedOptions() {
        Dimension size = this.getSize();

        size.height += ADDITIONAL_ADVANCED_HEIGHT;
        this.setSize(size);
        this.pnlCustomHeading.setVisible(true);
        this.pnlCustomAddress.setVisible(true);
        this.butCustom.setIcon(ImageHelper.getIcon(FILENAME_CHECKED));
        this.customMenuVisible = true;
    }

    private void hideAdvancedOptions() {
        if ( this.customMenuVisible ) {
            Dimension size = this.getSize();

            size.height -= ADDITIONAL_ADVANCED_HEIGHT;
            this.setSize(size);
            this.pnlCustomHeading.setVisible(false);
            this.pnlCustomAddress.setVisible(false);
            this.butCustom.setIcon(ImageHelper.getIcon(FILENAME_UNCHECKED));
            this.customMenuVisible = false;
        }
    }

    void toggleAdvancedOptions() {
        if ( this.customMenuVisible ) {
            this.hideAdvancedOptions();
        } else {
            this.showAdvancedOptions();
        }
    }

    boolean isCustomAddressSelected() {
        return this.customMenuVisible;
    }

    Pair<String, String> getCustomAddress() {
        String ip = (String) this.cmbBoxCustomIp.getSelectedItem();
        String port = (String) this.cmbBoxCustomPort.getSelectedItem();

        if ( ip == null ) {
            ip = "";
        }
        if ( port == null ) {
            port = "";
        }

        return new Pair<>(ip, port);
    }

    void setCustomAddresses(Iterable<? extends Pair<String, String>> addresses) {
        this.cmbBoxCustomIp.removeAllItems();
        this.cmbBoxCustomPort.removeAllItems();
        for ( Pair<String, String> address : addresses ) {
            this.cmbBoxCustomIp.addItem(address.getFirst());
            this.cmbBoxCustomPort.addItem(address.getSecond());
        }
    }

    // NAO
    String getRobotPassword() {
        return this.txtFldRobotPassword.getText();
    }

    // Other
    void setConnectButtonText(String text) {
        this.butConnect.setText(text);
    }

    Point getRobotButtonLocation() {
        return new Point(this.butRobot.getLocationOnScreen().x + this.butRobot.getWidth(), this.butRobot.getLocationOnScreen().y);
    }

    /**
     * Changes the update text to the according status message.
     *
     * @param updateStatus the status of the update check
     */
    void setUpdateButton(Status updateStatus) {
        String text = this.messages.getString("update") + ": ";
        switch ( updateStatus ) {
            case NEWER_VERSION:
            case SAME_VERSION:
            case OLDER_VERSION:
                text += this.messages.getString("checkForNew");
                break;
            case NOT_OK:
            case TIMEOUT:
                text += this.messages.getString("noConnection");
                break;
        }
        this.menuItemUpdate.setText(text);
    }
}
