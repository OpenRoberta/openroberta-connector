package de.fhg.iais.roberta.connection.wired.spike;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

import de.fhg.iais.roberta.connection.wired.ClearBufferThread;
import de.fhg.iais.roberta.connection.wired.IWiredRobot;
import de.fhg.iais.roberta.util.Pair;

public class SpikeCommunicator {

    private static final Logger LOG = LoggerFactory.getLogger(SpikeCommunicator.class);

    private final IWiredRobot robot;
    private final ClearBufferThread clearBufferThread;
    private final int slotId = 0;

    private SerialPort serialPort;
    private List<JSONObject> payloads;
    private byte[] fileContentEncoded;

    private boolean transferIdAdded = false;
    private boolean resendStarWriteProgramPayload = true;

    SpikeCommunicator(IWiredRobot robot) {
        this.robot = robot;
        this.clearBufferThread = new ClearBufferThread(robot.getPort());
    }

    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put("firmwarename", this.robot.getType().toString());
        deviceInfo.put("robot", this.robot.getType().toString());
        deviceInfo.put("brickname", this.robot.getType().getPrettyText());
        return deviceInfo;
    }

    public Pair<Integer, String> handleUpload(String absolutePath) {
        Pair<Integer, String> result = new Pair<>(1, "errorRobotUpload");
        try {
            stopClearBufferThread();
            initSerialPort(this.robot.getPort());

            extractFileInformation(absolutePath);
            createJsonPayloads();

            result = sendPayloads();
            LOG.info(result.getSecond());
        } catch ( Exception e ) {
            LOG.info(e.getMessage());
        }

        transferIdAdded = false;
        resendStarWriteProgramPayload = true;
        startClearBufferThread();
        return result;
    }

    private void initSerialPort(String portName) {
        portName = (SystemUtils.IS_OS_WINDOWS ? "" : "/dev/") + portName; // to hide the parameter, which should not be used
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(115200);
        LOG.info("Serial Communication is initialized: {} {} {}",
            serialPort.getSystemPortName(),
            serialPort.getDescriptivePortName(),
            serialPort.getPortDescription());
    }

    private void extractFileInformation(String filePath) throws IOException {
        File file = new File(filePath);
        fileContentEncoded = FileUtils.readFileToByteArray(file);
    }

    private void createJsonPayloads() {
        payloads = new ArrayList<>();

        createProgramTerminatePayload();
        createStartWriteProgramPayload();
        createWritePackagePayload();
        createExecuteProgramPayload();

        LOG.info("Created " + payloads.size() + " payloads");
    }

    private void createProgramTerminatePayload() {
        assemblePayload("program_terminate", new JSONObject());
    }

    private void createStartWriteProgramPayload() {
        JSONObject params = new JSONObject();
        JSONObject meta = new JSONObject();
        long nowTime = System.currentTimeMillis() / 1000;

        meta.put("created", nowTime);
        meta.put("modified", nowTime);
        meta.put("name", "NepoProg.py");
        meta.put("type", "python");
        meta.put("project_id", "OpenRoberta");

        params.put("slotid", slotId);
        params.put("size", fileContentEncoded.length);
        params.put("meta", meta);

        assemblePayload("start_write_program", params);
    }

    private void createWritePackagePayload() {
        List<JSONObject> paramList = new ArrayList<>();
        int maxDataSize = 512;
        int rest = 0;
        int end;
        for ( int i = 0; i < fileContentEncoded.length; i += end ) {
            JSONObject param = new JSONObject();
            end = Math.min(maxDataSize, fileContentEncoded.length - rest);
            rest += end;
            param.put("data", Base64.getEncoder().encodeToString(Arrays.copyOfRange(fileContentEncoded, i, i + end)));

            paramList.add(param);
        }
        for ( JSONObject params : paramList ) {
            assemblePayload("write_package", params);
        }
    }

    private void createExecuteProgramPayload() {
        JSONObject params = new JSONObject();
        params.put("slotid", slotId);
        assemblePayload("program_execute", params);
    }

    private void assemblePayload(String mode, JSONObject params) {
        JSONObject payload = new JSONObject();

        payload.put("m", mode);
        payload.put("p", params);
        payload.put("i", RandomStringUtils.randomAlphanumeric(4));

        payloads.add(payload);
    }

    private void addTransferIdToWritePackage(String transferId) {
        for ( JSONObject payload : payloads ) {
            if ( payload.getString("m").equals("write_package") ) {
                payload.getJSONObject("p").put("transferid", transferId);
            }
        }
        transferIdAdded = true;
    }

    private Pair<Integer, String> sendPayloads() throws InterruptedException, JSONException {
        Pair<Integer, String> result = new Pair<>(1, "errorRobotUpload");
        int payloadSize = payloads.size();
        if ( !serialPort.isOpen() ) {
            serialPort.openPort();
        }
        LOG.info("Program upload starts");
        for ( int i = 0; i < payloadSize; i++ ) {
            LOG.info("sending payload " + (i + 1) + " of " + payloadSize);
            JSONObject payload = payloads.get(i);
            String payloadAsString = payload + "\r";
            byte[] payloadAsBytes = payloadAsString.getBytes(StandardCharsets.UTF_8);
            int payloadLength = payloadAsBytes.length;
            int bytesWritten = serialPort.writeBytes(payloadAsBytes, payloadLength);

            if ( bytesWritten == payloadLength ) {
                Thread.sleep(1);
                result = receiveResponse(payload);
                if ( result.getFirst() == 1 ) {
                    break;
                } else if ( result.getFirst() == 2 ) {
                    resendStarWriteProgramPayload = false;
                    i--;
                }
            } else {
                break;
            }
        }
        return result;
    }

    private Pair<Integer, String> receiveResponse(JSONObject payload) throws InterruptedException, JSONException {
        String id = payload.getString("i");
        Pattern findResponsePattern = Pattern.compile("(\\{.*(" + id + "|\"i\"|\"r\"|transferid|checksum|next_ptr){1}.*}?)");
        short bufSize = 2048;
        byte[] buffer = new byte[bufSize];
        long time = System.currentTimeMillis();

        while ( (System.currentTimeMillis()) - time < 12000 ) {
            int bytesAvailable = serialPort.bytesAvailable();
            if ( bytesAvailable < 0 ) {
                LOG.error("Error: Robot seems disconnected");
                return new Pair<>(1, "errorRobotUpload");
            }
            serialPort.readBytes(buffer, Math.min(bytesAvailable, bufSize));
            String answer = new String(buffer, StandardCharsets.UTF_8);
            Matcher responseMatcher = findResponsePattern.matcher(answer);
            if ( responseMatcher.find() ) {
                return checkResponse(responseMatcher.group(), id, payload.getString("m"));
            }
            Thread.sleep(10);
        }
        LOG.error("Error: No response received from the robot");
        return new Pair<>(1, "errorRobotUpload");
    }

    private Pair<Integer, String> checkResponse(String response, String id, String mode) throws JSONException {
        try {
            JSONObject jsonAnswer = new JSONObject(response);
            if ( jsonAnswer.has("e") ) {
                String error = new String(Base64.getDecoder().decode(jsonAnswer.getString("e")), StandardCharsets.UTF_8);
                LOG.error("Error from the robot: {}", error);
                return new Pair<>(1, "errorRobotUpload");
            }
            if ( !transferIdAdded && !jsonAnswer.get("r").equals(JSONObject.NULL) && jsonAnswer.getJSONObject("r").has("transferid") ) {
                addTransferIdToWritePackage(jsonAnswer.getJSONObject("r").getString("transferid"));
            }
        } catch ( JSONException e ) {
            if ( mode.equals("start_write_program") ) {
                if ( !resendStarWriteProgramPayload ) {
                    return new Pair<>(1, "errorRobotUpload");
                }
                LOG.info("Response with transferId is broken. Sending payload again");
                return new Pair<>(2, "");
            }
            LOG.info("Broken response detected. Ignoring and continuing the upload. Response: " + response);
        }
        return new Pair<>(0, "");
    }

    private void startClearBufferThread() {
        clearBufferThread.start(serialPort);
    }

    private void stopClearBufferThread() {
        clearBufferThread.exit();
    }
}