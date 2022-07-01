package de.fhg.iais.roberta.connection.wireless;

import org.json.JSONObject;

import java.io.IOException;

public interface IWirelessCommunicator {
    void setPassword(String password);

    JSONObject getDeviceInfo();

    void uploadFile(byte[] binaryFile, String fileName) throws IOException;

    String checkFirmwareVersion() throws IOException;
}
