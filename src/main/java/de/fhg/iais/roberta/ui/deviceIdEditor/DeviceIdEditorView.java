package de.fhg.iais.roberta.ui.deviceIdEditor;

import de.fhg.iais.roberta.connection.arduino.ArduinoType;
import de.fhg.iais.roberta.ui.OraButtonColumn;
import de.fhg.iais.roberta.ui.OraTable;
import de.fhg.iais.roberta.util.ArduinoIdFileHelper;
import de.fhg.iais.roberta.util.IOraUiListener;
import de.fhg.iais.roberta.util.SerialDevice;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;

import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorController.getDevicesTableData;
import static de.fhg.iais.roberta.ui.main.MainView.ICON_TITLE;
import static de.fhg.iais.roberta.ui.main.MainView.IMAGES_PATH;

class DeviceIdEditorView extends JDialog {
    private static final Icon PLUS = new ImageIcon(Objects.requireNonNull(DeviceIdEditorView.class.getClassLoader().getResource(IMAGES_PATH + "plus.png")));
    private static final Icon MINUS = new ImageIcon(Objects.requireNonNull(DeviceIdEditorView.class.getClassLoader().getResource(IMAGES_PATH + "minus.png")));

    static final String CMD_ADD_ENTRY = "add_entry";
    static final String CMD_REMOVE_ENTRY = "remove_entry";
    static final String CMD_DELIMITER = ":";
    static final String CMD_SAVE_AND_CLOSE = "save_and_close";
    static final String CMD_CANCEL = "cancel";

    private final ResourceBundle messages;

    private final JPanel pnlInfo = new JPanel();
    private final JTextArea txtAreaInfo = new JTextArea();

    private final JPanel pnlConnectedLabel = new JPanel();
    private final JLabel lblConnected = new JLabel();

    private final JPanel pnlDevicesTableHeader = new JPanel();
    private final JScrollPane scrDevicesTable = new JScrollPane();
    private final JPanel pnlDevicesTable = new JPanel();
    private final OraTable tblDevices;

    private final JPanel pnlRegisteredLabel = new JPanel();
    private final JLabel lblRegistered = new JLabel();

    private final JPanel pnlIdTableHeader = new JPanel();
    private final JScrollPane scrIdTable = new JScrollPane();
    private final JPanel pnlIdTable = new JPanel();
    private final OraTable tblIds;

    private final JPanel pnlButton = new JPanel();
    private final JButton btnSave = new JButton();
    private final JButton btnCancel = new JButton();

    DeviceIdEditorView(ResourceBundle messages, IOraUiListener listener) {
        this.messages = messages;

        // General
        this.setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        this.setResizable(false);
        this.addWindowListener(listener);
        this.setModal(true);

        // Titlebar
        this.setTitle(messages.getString("idEditor"));
        this.setIconImage(ICON_TITLE.getImage());

        this.add(this.pnlInfo);
        this.pnlInfo.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlInfo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        this.pnlInfo.add(this.txtAreaInfo);
        this.txtAreaInfo.setText(this.messages.getString("idEditorInfo"));
        this.txtAreaInfo.setWrapStyleWord(true);
        this.txtAreaInfo.setLineWrap(true);
        this.txtAreaInfo.setColumns(30);
        this.txtAreaInfo.setEditable(false);

        this.add(this.pnlConnectedLabel);
        this.pnlConnectedLabel.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlConnectedLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        this.pnlConnectedLabel.add(this.lblConnected);
        this.lblConnected.setText(this.messages.getString("connectedDevices"));

        this.tblDevices = new OraTable(getDevicesTableData(),
            Arrays.asList(this.messages.getString("numberAbbrev"),
                this.messages.getString("vendorId"),
                this.messages.getString("productId"),
                this.messages.getString("port"),
                ""),
            new HashSet<>(Collections.singletonList(4)));
        // Devices table header
        this.add(this.pnlDevicesTableHeader);
        this.pnlDevicesTableHeader.setLayout(new BorderLayout());
        this.pnlDevicesTableHeader.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        this.pnlDevicesTableHeader.add(this.tblDevices.getTableHeader(), BorderLayout.CENTER);

        // Devices table
        this.add(this.scrDevicesTable);
        this.scrDevicesTable.setViewportView(this.pnlDevicesTable);
        this.scrDevicesTable.setPreferredSize(new Dimension(430, 150));
        this.scrDevicesTable.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        this.pnlDevicesTable.setLayout(new BorderLayout());

        this.pnlDevicesTable.add(this.tblDevices, BorderLayout.CENTER);
        this.tblDevices.getColumnModel().getColumn(0).setMaxWidth(30);
        this.tblDevices.getColumnModel().getColumn(4).setMaxWidth(30);
        new OraButtonColumn(this.tblDevices, listener, CMD_ADD_ENTRY + CMD_DELIMITER,4);

        this.add(Box.createRigidArea(new Dimension(0, 20)));

        this.add(this.pnlRegisteredLabel);
        this.pnlRegisteredLabel.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlRegisteredLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        this.pnlRegisteredLabel.add(this.lblRegistered);
        this.lblRegistered.setText(this.messages.getString("registeredDevices"));

        this.tblIds = new OraTable(getIdTableData(),
            Arrays.asList(this.messages.getString("vendorId"), this.messages.getString("productId"), this.messages.getString("deviceType"), ""),
            new HashSet<>(Arrays.asList(2, 3)));
        // Device ID table header
        this.add(this.pnlIdTableHeader);
        this.pnlIdTableHeader.setLayout(new BorderLayout());
        this.pnlIdTableHeader.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        this.pnlIdTableHeader.add(this.tblIds.getTableHeader(), BorderLayout.CENTER);

        // Device ID table
        this.add(this.scrIdTable);
        this.scrIdTable.setViewportView(this.pnlIdTable);
        this.scrIdTable.setPreferredSize(new Dimension(430, 150));
        this.scrIdTable.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        this.pnlIdTable.setLayout(new BorderLayout());

        this.pnlIdTable.add(this.tblIds, BorderLayout.CENTER);
        this.tblIds.getColumnModel().getColumn(3).setMaxWidth(30);
        initTypeColumn(this.tblIds.getColumnModel().getColumn(2));
        new OraButtonColumn(this.tblIds, listener, CMD_REMOVE_ENTRY + CMD_DELIMITER, 3);

        this.add(Box.createRigidArea(new Dimension(0, 20)));

        // Buttons
        this.add(this.pnlButton);
        this.pnlButton.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlButton.setBorder(BorderFactory.createEmptyBorder(0, 11, 0, 11));

        this.pnlButton.add(this.btnSave);
        this.btnSave.setText(this.messages.getString("saveAndClose"));
        this.btnSave.setActionCommand(CMD_SAVE_AND_CLOSE);
        this.btnSave.addActionListener(listener);

        this.pnlButton.add(this.btnCancel);
        this.btnCancel.setText(this.messages.getString("cancel"));
        this.btnCancel.setActionCommand(CMD_CANCEL);
        this.btnCancel.addActionListener(listener);

        this.add(Box.createRigidArea(new Dimension(0, 20)));

        this.pack();

        this.setLocation(this.getWidth(), this.getHeight());
        this.setLocationRelativeTo(this.getParent());
    }

    private static List<List<Object>> getIdTableData() {
        List<List<Object>> data = new ArrayList<>();
        for ( Entry<SerialDevice, ArduinoType> entry : ArduinoIdFileHelper.load().getFirst().entrySet() ) {
            data.add(createIdEntry(entry.getKey().vendorId, entry.getKey().productId, entry.getValue()));
        }
        return data;
    }


    private void initTypeColumn(TableColumn typeColumn) {
        JComboBox<ArduinoType> comboBox = new JComboBox<>();

        List<ArduinoType> arduinoTypes = new ArrayList<>(Arrays.asList(ArduinoType.values()));
        arduinoTypes.remove(ArduinoType.NONE);

        for ( ArduinoType type : arduinoTypes ) {
            comboBox.addItem(type);
        }

        typeColumn.setCellEditor(new DefaultCellEditor(comboBox));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setToolTipText(this.messages.getString("selectDeviceType"));
        typeColumn.setCellRenderer(renderer);
    }

    static List<Object> createDeviceEntry(int number, String vendorId, String productId, String port) {
        List<Object> entry = new ArrayList<>(5);
        entry.add(number);
        entry.add(vendorId);
        entry.add(productId);
        entry.add(port);
        entry.add(PLUS);
        return entry;
    }

    static List<Object> createIdEntry(String vendorId, String productId, ArduinoType arduinoType) {
        List<Object> entry = new ArrayList<>(4);
        entry.add(vendorId);
        entry.add(productId);
        entry.add((arduinoType == ArduinoType.NONE) ? "" : arduinoType);
        entry.add(MINUS);
        return entry;
    }

    OraTable getTblDevices() {
        return this.tblDevices;
    }

    OraTable getTblIds() {
        return this.tblIds;
    }
}
