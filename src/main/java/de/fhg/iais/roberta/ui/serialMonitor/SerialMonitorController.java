package de.fhg.iais.roberta.ui.serialMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.SerialLoggingTask;
import de.fhg.iais.roberta.connection.wired.IWiredRobot;
import de.fhg.iais.roberta.ui.IController;
import de.fhg.iais.roberta.util.IOraUiListener;

import static de.fhg.iais.roberta.ui.serialMonitor.SerialMonitorView.CMD_CLEAR;
import static de.fhg.iais.roberta.ui.serialMonitor.SerialMonitorView.CMD_RESTART;

public class SerialMonitorController implements IController {
    private static final Logger LOG = LoggerFactory.getLogger(SerialMonitorController.class);

    private final SerialMonitorView serialMonitorView;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<Void> serialLoggingFuture = null;

    private String portName = null;

    public SerialMonitorController(ResourceBundle rb) {
        this.serialMonitorView = new SerialMonitorView(rb, new SerialMonitorViewListener());

        this.serialMonitorView.setVisible(false);
    }

    @Override
    public void setConnector(IConnector<?> connector) {
        LOG.debug("setConnector: {}", connector.getClass().getSimpleName());
        connector.registerListener(this::setState);
        this.portName = ((IWiredRobot) connector.getRobot()).getPort();
    }

    @Override
    public void setState(State state) {
        LOG.debug("setState: {}", state);
        switch ( state ) {
            case DISCOVER:
                this.stopSerialLogging();
                break;
            case WAIT_UPLOAD:
                this.stopSerialLogging();
                break;
            case WAIT_EXECUTION:
                if ( this.serialMonitorView.isVisible() ) {
                    this.restartSerialLogging();
                }
                break;
            default:
                break;
        }
    }

    public void showSerialMonitor() {
        LOG.debug("showSerialMonitor");

        this.serialMonitorView.setVisible(true);

        this.restartSerialLogging();
    }

    private void restartSerialLogging() {
        LOG.debug("restartSerialLogging");
        this.stopSerialLogging();

        this.serialLoggingFuture =
            this.executorService.submit(new SerialLoggingTask(this::appendSerial, this.portName, this.serialMonitorView.getSerialRate()));
    }

    private void appendSerial(byte[] readBuffer) {
        SwingUtilities.invokeLater(() -> this.serialMonitorView.appendText(readBuffer));
    }

    private void stopSerialLogging() {
        if ( this.serialLoggingFuture != null ) {
            this.serialLoggingFuture.cancel(true);
        }
    }

    private class SerialMonitorViewListener implements IOraUiListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("actionPerformed: {}", e.getActionCommand());

            switch ( e.getActionCommand() ) {
                case "comboBoxChanged":
                    SerialMonitorController.this.restartSerialLogging();
                    SerialMonitorController.this.serialMonitorView.clearText();
                    break;
                case CMD_CLEAR:
                    SerialMonitorController.this.serialMonitorView.clearText();
                    break;
                case CMD_RESTART:
                    SerialMonitorController.this.restartSerialLogging();
                    break;
                default:
                    throw new UnsupportedOperationException("Action " + e.getActionCommand() + " is not implemented!");
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            LOG.info("User closed serial window");
            SerialMonitorController.this.stopSerialLogging();
        }
    }
}
