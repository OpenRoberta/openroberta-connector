package de.fhg.iais.roberta.ui.deviceIdEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JTable;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.wired.SerialRobotDetector;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;
import de.fhg.iais.roberta.ui.IController;
import de.fhg.iais.roberta.ui.OraPopup;
import de.fhg.iais.roberta.ui.OraTable.FixedTableModel;
import de.fhg.iais.roberta.util.IOraUiListener;
import de.fhg.iais.roberta.util.SerialDevice;
import de.fhg.iais.roberta.util.WiredRobotIdFileHelper;

import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorView.CMD_ADD_ENTRY;
import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorView.CMD_CANCEL;
import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorView.CMD_DELIMITER;
import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorView.CMD_REMOVE_ENTRY;
import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorView.CMD_SAVE_AND_CLOSE;
import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorView.createDeviceEntry;
import static de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorView.createIdEntry;

public class DeviceIdEditorController implements IController {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceIdEditorController.class);

    private final DeviceIdEditorView deviceIdEditorView;

    private final ResourceBundle messages;

    private ScheduledExecutorService executorService = null;

    public DeviceIdEditorController(ResourceBundle rb) {
        this.deviceIdEditorView = new DeviceIdEditorView(rb, new DeviceIdEditorViewListener());
        this.messages = rb;
    }

    @Override
    public void setConnector(IConnector<?> connector) {
        // does not need the connector
    }

    @Override
    public void setState(State state) {
        // does not need the state
    }

    static List<List<Object>> getDevicesTableData() {
        List<List<Object>> data = new ArrayList<>(20);
        int number = 1;
        for ( SerialDevice usbDevice : SerialRobotDetector.getUsbDevices() ) {
            data.add(createDeviceEntry(number, usbDevice.vendorId, usbDevice.productId, usbDevice.port));
            number++;
        }
        return data;
    }

    public void showEditor() {
        // Also update the connected devices
        this.executorService = Executors.newSingleThreadScheduledExecutor();

        this.executorService.scheduleAtFixedRate(() -> {
            List<List<Object>> devicesTableData = getDevicesTableData();
            this.deviceIdEditorView.getTblDevices().updateTable(devicesTableData, Arrays.asList(2, 3));
        }, 0L, 1L, TimeUnit.SECONDS);

        this.deviceIdEditorView.setVisible(true);
    }

    private class DeviceIdEditorViewListener implements IOraUiListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("actionPerformed: {}", e.getActionCommand());

            switch ( e.getActionCommand().split(CMD_DELIMITER)[0] ) {
                case CMD_ADD_ENTRY:
                    this.addEntry(e);
                    break;
                case CMD_REMOVE_ENTRY:
                    this.removeEntry(e);
                    break;
                case CMD_SAVE_AND_CLOSE:
                    this.saveAndClose();
                    break;
                case CMD_CANCEL:
                    this.close();
                    break;
                default:
                    throw new UnsupportedOperationException("Action " + e.getActionCommand() + " is not implemented!");
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            this.close();
        }

        private void addEntry(ActionEvent e) {
            JTable table = (JTable) e.getSource();
            int modelRow = Integer.parseInt(e.getActionCommand().split(CMD_DELIMITER)[1]);

            List<Object>
                rowData =
                createIdEntry((String) table.getModel().getValueAt(modelRow, 1), (String) table.getModel().getValueAt(modelRow, 2), WiredRobotType.NONE);

            int result = DeviceIdEditorController.this.deviceIdEditorView.getTblIds().addRow(rowData, Arrays.asList(0, 1));

            if ( result == -1 ) {
                DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                .setRowSelectionInterval(DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                                                                                         .getRowCount() - 1,
                                                                                         DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                                                                                         .getRowCount() - 1);
                DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                .scrollRectToVisible(DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                                                                                     .getCellRect(
                                                                                                                                         DeviceIdEditorController.this.deviceIdEditorView
                                                                                                                                             .getTblIds()
                                                                                                                                             .getRowCount() - 1,
                                                                                                                                         0,
                                                                                                                                         false));
            } else {
                DeviceIdEditorController.this.deviceIdEditorView.getTblIds().setRowSelectionInterval(result, result);
                DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                .scrollRectToVisible(new Rectangle(DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                                                                                                   .getCellRect(
                                                                                                                                                       result,
                                                                                                                                                       0,
                                                                                                                                                       true)));

                OraPopup.showPopup(
                    DeviceIdEditorController.this.deviceIdEditorView,
                    "attention",
                    "alreadyExists",
                    DeviceIdEditorController.this.messages,
                    null,
                    new String[] { "yes" });
            }
        }

        private void removeEntry(ActionEvent e) {
            JTable table = (JTable) e.getSource();
            int modelRow = Integer.parseInt(e.getActionCommand().split(CMD_DELIMITER)[1]);

            int decision = OraPopup.showPopup(DeviceIdEditorController.this.deviceIdEditorView,
                                              "attention",
                                              "confirmDeletion",
                                              DeviceIdEditorController.this.messages,
                                              null,
                                              new String[] { "yes", "no" });
            if ( decision == 0 ) { // clicked yes
                ((FixedTableModel) table.getModel()).removeRow(modelRow);
                DeviceIdEditorController.this.deviceIdEditorView.pack();
            }
        }

        private void saveAndClose() {
            Collection<List<String>> wiredRobotIdEntries = new ArrayList<>(20);

            int noneRowIndex = -1;
            for ( int row = 0; row < DeviceIdEditorController.this.deviceIdEditorView.getTblIds().getRowCount(); ++row ) {
                List<String> entry = new ArrayList<>(3);
                entry.add((String) DeviceIdEditorController.this.deviceIdEditorView.getTblIds().getModel().getValueAt(row, 0));
                entry.add((String) DeviceIdEditorController.this.deviceIdEditorView.getTblIds().getModel().getValueAt(row, 1));

                WiredRobotType wiredRobotType = (WiredRobotType) DeviceIdEditorController.this.deviceIdEditorView.getTblIds().getModel().getValueAt(row, 2);
                if ( wiredRobotType == WiredRobotType.NONE ) {
                    noneRowIndex = row;
                }

                entry.add(wiredRobotType.toString());

                wiredRobotIdEntries.add(entry);
            }

            if ( noneRowIndex == -1 ) {
                WiredRobotIdFileHelper.save(wiredRobotIdEntries);

                this.close();
            } else {
                DeviceIdEditorController.this.deviceIdEditorView.getTblIds().setRowSelectionInterval(noneRowIndex, noneRowIndex);
                DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                .scrollRectToVisible(new Rectangle(DeviceIdEditorController.this.deviceIdEditorView.getTblIds()
                                                                                                                                                   .getCellRect(
                                                                                                                                                       noneRowIndex,
                                                                                                                                                       0,
                                                                                                                                                       true)));

                OraPopup.showPopup(DeviceIdEditorController.this.deviceIdEditorView,
                                   "attention",
                                   "noneTypeRemaining",
                                   DeviceIdEditorController.this.messages,
                                   null,
                                   new String[] { "ok" });
            }
        }

        private void close() {
            LOG.info("close");
            DeviceIdEditorController.this.executorService.shutdownNow();
            DeviceIdEditorController.this.deviceIdEditorView.dispose();
        }
    }
}
