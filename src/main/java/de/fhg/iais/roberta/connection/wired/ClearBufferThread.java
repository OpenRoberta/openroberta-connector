package de.fhg.iais.roberta.connection.wired;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;


public class ClearBufferThread extends Thread {
    private final Logger LOG = LoggerFactory.getLogger(ClearBufferThread.class);
    private final String portName;
    private Thread thread;
    private SerialPort serialPort;
    private boolean exitThread = false;

    public ClearBufferThread(String portName) {
        this.portName = (SystemUtils.IS_OS_WINDOWS ? "" : "/dev/") + portName;
    }

    public boolean exit() throws InterruptedException {
        if ( thread != null && thread.isAlive() ) {
            exitThread = true;
        }
        if ( serialPort != null && serialPort.isOpen() ) {
            return serialPort.closePort();
        }
        return true;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[4096];
        exitThread = false;
        while ( !exitThread && serialPort.bytesAvailable() >= 0 ) {
            serialPort.readBytes(buffer, Math.min(serialPort.bytesAvailable(), buffer.length));
            try {
                Thread.sleep(100);
            } catch ( InterruptedException e ) {
                LOG.info(e.getMessage());
            }
        }
        LOG.info("Stop clearing buffer");
    }

    public void start(SerialPort serialPortObject) {
        LOG.info("Start clearing buffer until next program upload");
        initSerialPort(serialPortObject);
        thread = new Thread(this, "Clear buffer of " + portName);
        thread.start();
    }

    private void initSerialPort(SerialPort serialPortObject) {
        serialPort = serialPortObject;
        if ( !serialPort.isOpen() ) {
            serialPort.openPort();
        }
    }
}
