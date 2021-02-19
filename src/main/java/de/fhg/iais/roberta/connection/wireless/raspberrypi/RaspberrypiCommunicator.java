package de.fhg.iais.roberta.connection.wireless.raspberrypi;

import de.fhg.iais.roberta.util.SshConnection;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Communicator class for the NAO robot. Handles network communication between the NAO and the connector.
 */
public class RaspberrypiCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(RaspberrypiCommunicator.class);
    private final Raspberrypi robot;


    /**
     * Constructor for the RaspberryPi communicator.
     *
     * @param rasp the robot to communicate with
     */
    public RaspberrypiCommunicator(Raspberrypi rasp) {
        this.robot = rasp;
    }

    /**
     * Uploads a binary file to the RaspberryPi robot.
     *
     * @param binaryFile the content of the file
     * @param fileName   the desired file name
     * @throws UserAuthException if the user is not correctly authorized
     * @throws IOException       if something with the ssh connection or file transfer went wrong
     */
    public void uploadFile(byte[] binaryFile, String fileName) throws UserAuthException, IOException {
        try (SshConnection ssh = new SshConnection(robot.getAddress(), robot.getUserName(), robot.getPassword())) {
            ssh.copyLocalToRemote(binaryFile, ".", fileName);
            ssh.command("python " + fileName);
        } catch (FileNotFoundException | TransportException | ConnectionException e) {
            throw new IOException(e);
        }
    }


    /**
     * Returns the JSON device info needed for the server.
     *
     * @return the device info as a json object
     */
    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("firmwarename", "RaspberryPi");
        deviceInfo.put("robot", "raspberrypi");
        deviceInfo.put("macaddr", "usb");
        deviceInfo.put("brickname", this.robot.getName());
        deviceInfo.put("battery", "1.0");
        return deviceInfo;
    }
}
