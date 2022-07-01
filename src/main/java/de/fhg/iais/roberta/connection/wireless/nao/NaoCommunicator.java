package de.fhg.iais.roberta.connection.wireless.nao;

import de.fhg.iais.roberta.connection.wireless.IWirelessCommunicator;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;

import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

import de.fhg.iais.roberta.util.PropertyHelper;
import de.fhg.iais.roberta.util.SshConnection;

/**
 * Communicator class for the NAO robot. Handles network communication between the NAO and the connector.
 */
public class NaoCommunicator implements IWirelessCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(NaoCommunicator.class);

    private static final String USERNAME = "nao";

    private final String name;
    private final InetAddress address;

    private String password = "";

    private final String workingDirectory;
    private String firmwareVersion = "";

    /**
     * Constructor for the NAO communicator.
     *
     * @param nao the robot to communicate with
     */
    public NaoCommunicator(Nao nao) {
        this.name = nao.getName();
        this.address = nao.getAddress();

        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.workingDirectory = System.getenv("APPDATA") + '/' + PropertyHelper.getInstance().getProperty("artifactId") + '/';
        } else {
            this.workingDirectory = System.getProperty("user.home") + '/' + PropertyHelper.getInstance().getProperty("artifactId") + '/';
        }
    }

    /**
     * Uploads a binary file to the NAO robot.
     *
     * @param binaryFile the content of the file
     * @param fileName   the desired file name
     * @throws UserAuthException if the user is not correctly authorized
     * @throws IOException if something with the ssh connection or file transfer went wrong
     */
    public void uploadFile(byte[] binaryFile, String fileName) throws UserAuthException, IOException {
        Collection<String> fileNames = new ArrayList<>(5);
        fileNames.add("__init__.py");
        fileNames.add("blockly_methods.py");
        fileNames.add("original_hal.py");
        fileNames.add("speech_recognition_module.py");
        fileNames.add("face_recognition_module.py");

        try (SshConnection ssh = new SshConnection(this.address, USERNAME, this.password)) {
            ssh.command("rm -rf /home/" + USERNAME + "/roberta");
            ssh.command("mkdir -p /home/" + USERNAME + "/roberta");
            for ( String fname : fileNames ) {
                ssh.copyLocalToRemote(this.workingDirectory + "/roberta", "roberta", fname);
            }
            ssh.copyLocalToRemote(binaryFile, ".", fileName);
            String runCommand = this.firmwareVersion.equals("2-8") ? "eval \"export $(xargs < /etc/conf.d/naoqi)\"; python " : "python ";
            ssh.command(runCommand + fileName);
        } catch ( FileNotFoundException | TransportException | ConnectionException e ) {
            throw new IOException(e);
        }
    }

    /**
     * Checks the NAO firmware version using the naoqi-bin installed on the NAO.
     * @return the NAO firmware version
     * @throws UserAuthException if the user is not correctly authorized
     * @throws IOException if something with the ssh connection went wrong
     */
    public String checkFirmwareVersion() throws UserAuthException, IOException {
        try (SshConnection ssh = new SshConnection(this.address, USERNAME, this.password)) {
            String msg = ssh.command("naoqi-bin --version");
            String version = msg.split("\n")[0].split(":")[1].trim();
            this.firmwareVersion = version.replace(".", "-");
            return this.firmwareVersion;
        } catch ( TransportException | ConnectionException e ) {
            throw new IOException(e);
        }
    }

    /**
     * Sets the password for SSH communication with the NAO.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the JSON device info needed for the server.
     *
     * @return the device info as a json object
     */
    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("firmwarename", "Nao");
        deviceInfo.put("robot", "nao");
        deviceInfo.put("firmwareversion", this.firmwareVersion);
        deviceInfo.put("macaddr", "usb");
        deviceInfo.put("brickname", this.name);
        deviceInfo.put("battery", "1.0");
        return deviceInfo;
    }
}
