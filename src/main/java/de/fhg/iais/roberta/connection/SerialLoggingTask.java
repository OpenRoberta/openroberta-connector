package de.fhg.iais.roberta.connection;

import com.fazecast.jSerialComm.SerialPort;
import de.fhg.iais.roberta.util.IOraListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

// https://github.com/Fazecast/jSerialComm/wiki/Nonblocking-Reading-Usage-Example
public class SerialLoggingTask extends AbstractLoggingTask {
    private static final Logger LOG = LoggerFactory.getLogger(SerialLoggingTask.class);

    private final SerialPort comPort;

    public SerialLoggingTask(IOraListener<byte[]> listener, CharSequence port, int serialRate) {
        registerListener(listener);

        SerialPort[] serialPorts = SerialPort.getCommPorts();
        this.comPort =
            Arrays.stream(serialPorts)
                .filter(serialPort -> serialPort.getSystemPortName().contains(port))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Port is not available!"));
        this.comPort.setBaudRate(serialRate);
        this.comPort.openPort(0);
        LOG.info("SerialPort {} {} {} opened, logging with baud rate of {}",
            this.comPort.getSystemPortName(),
            this.comPort.getDescriptivePortName(),
            this.comPort.getPortDescription(),
            this.comPort.getBaudRate());
    }

    @Override
    protected void log() {
        try {
            byte[] readBuffer = new byte[this.comPort.bytesAvailable()];
            this.comPort.readBytes(readBuffer, readBuffer.length);
            fire(readBuffer);
        } catch (NegativeArraySizeException e) { // gets thrown if cable is disconnected
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void finish() {
        this.comPort.closePort();
    }
}
