package de.fhg.iais.roberta.connection.wired.mBot2;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import de.fhg.iais.roberta.connection.wired.IWiredRobot;
import de.fhg.iais.roberta.util.Pair;

public class Mbot2Communicator {

    private static final Logger LOG = LoggerFactory.getLogger(Mbot2Communicator.class);

    private final IWiredRobot robot;

    private SerialPort serialPort;
    private final List<byte[]> payloads = new ArrayList<>();
    private final List<Byte> fileContent = new ArrayList<>();

    private final Pattern responsePattern = Pattern.compile("f3(fa070001005ef001000((0)|(1))50|(f603000d00000d))f4");

    public Mbot2Communicator(IWiredRobot robot) {
        this.robot = robot;
    }

    public JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put("firmwarename", this.robot.getType().toString());
        deviceInfo.put("robot", this.robot.getType().toString());
        deviceInfo.put("brickname", this.robot.getName());

        return deviceInfo;
    }

    public Pair<Integer, String> uploadFile(String portName, String filePath) {
        portName = (SystemUtils.IS_OS_WINDOWS ? "" : "/dev/") + portName; // to hide the parameter, which should not be used
        try {
            initSerialPort(portName);
            extractFileInformation(filePath);
            generatePayloads();
            return sendPayload();
        } catch ( Exception e ) {
            LOG.info(e.getMessage());
            return new Pair<>(1, "Error while uploading file");
        }
    }

    private void initSerialPort(String portName) throws SerialPortInvalidPortException {
        if ( serialPort != null && serialPort.isOpen() ) {
            serialPort.closePort();
        }
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(115200);
    }

    private void extractFileInformation(String filePath) throws IOException {
        File file = new File(filePath);
        Path path = Paths.get(file.getAbsolutePath());
        byte[] tmp = Files.readAllBytes(path);
        fileContent.clear();
        for ( byte value : tmp ) {
            this.fileContent.add(value);
        }
    }

    private void generatePayloads() {
        List<Byte> dataFrame = new ArrayList<>();
        List<Byte> uploadFrame = new ArrayList<>();
        List<List<Byte>> fileDataFrame = new ArrayList<>();
        byte[] modeUpload = new byte[] {(byte) 0xF3, (byte) 0xF6, 0x03, 0x00, 0x0D, 0x00, 0x00, 0x0D, (byte) 0xF4};
        byte frameHeader = (byte) 0xF3;
        byte frameFooter = (byte) 0xF4;
        byte protocolId = 0x01;
        byte deviceId = 0x00;
        byte serviceId = 0x5E;
        byte len1;
        byte len2;
        byte headerChecksum;
        byte fileDataChecksum;
        int frameSize;

        payloads.clear();
        payloads.add(modeUpload);

        fileDataFrame.add(generateHeader());
        fileDataFrame.addAll(generateBody());

        for ( List<Byte> frame : fileDataFrame ) {
            uploadFrame.add(protocolId);
            uploadFrame.add(deviceId);
            uploadFrame.add(serviceId);
            uploadFrame.addAll(frame);

            frameSize = uploadFrame.size();
            len1 = (byte) (frameSize % 256);
            len2 = (byte) (frameSize / 256);
            headerChecksum = (byte) (frameHeader + len1 + len2);
            fileDataChecksum = calculateChecksum(uploadFrame);

            dataFrame.add(frameHeader);
            dataFrame.add(headerChecksum);
            dataFrame.add(len1);
            dataFrame.add(len2);
            dataFrame.addAll(uploadFrame);
            dataFrame.add(fileDataChecksum);
            dataFrame.add(frameFooter);

            payloads.add(convertArrayListToByteArray(dataFrame));

            dataFrame.clear();
            uploadFrame.clear();
        }
        LOG.info("Generated " + payloads.size() + " payloads");
    }

    private List<Byte> generateHeader() {
        List<Byte> frame = new ArrayList<>();
        List<Byte> data = new ArrayList<>();
        String fileName = "/flash/main.py";
        int fileSize = this.fileContent.size();
        byte instructionId = 0x01;
        byte fileType = 0x00;
        byte[] sizeByte = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(fileSize).array();

        data.add(fileType);
        for ( byte value : sizeByte ) {
            data.add(value);
        }
        data.addAll(xor32BitChecksum());
        for ( byte value : fileName.getBytes() ) {
            data.add(value);
        }
        frame.add(instructionId);
        frame.add((byte) data.size());
        frame.add((byte) 0x00);
        frame.addAll(data);
        return frame;
    }

    private List<List<Byte>> generateBody() {
        List<List<Byte>> bodyArr = new ArrayList<>();
        List<Byte> frame;
        List<Byte> data;
        byte[] sentDataArray;
        int dataSizeToSend;
        byte instructionID = 0x02;
        int maxSize = 0x40;

        for ( int sentData = 0x00; sentData < fileContent.size(); sentData += dataSizeToSend ) {
            frame = new ArrayList<>();
            data = new ArrayList<>();

            dataSizeToSend = Math.min(maxSize, this.fileContent.size()-sentData);
            sentDataArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sentData).array();
            for ( byte value : sentDataArray ) {
                data.add(value);
            }
            data.addAll(this.fileContent.subList(sentData, dataSizeToSend + sentData));
            frame.add(instructionID);
            frame.add((byte) data.size());
            frame.add((byte) 0x00);
            frame.addAll(data);
            bodyArr.add(frame);
        }
        return bodyArr;
    }

    private List<Byte> xor32BitChecksum() {
        int fileSize = fileContent.size();
        byte[] checksum = new byte[] {0x00, 0x00, 0x00, 0x00};
        byte padding = (byte) (fileSize % 4);
        for ( int i = 0; i < fileSize / 4; i++ ) {
            checksum[0] ^= fileContent.get(i * 4);
            checksum[1] ^= fileContent.get(i * 4 + 1);
            checksum[2] ^= fileContent.get(i * 4 + 2);
            checksum[3] ^= fileContent.get(i * 4 + 3);
        }
        if ( padding != 0 ) {
            for ( int i = 0; i < padding; i++ ) {
                checksum[i] ^= fileContent.get(4 * (fileSize / 4) + i);
            }
        }
        return Arrays.asList(ArrayUtils.toObject(checksum));
    }

    private byte calculateChecksum(List<Byte> values) {
        byte checksum = 0x00;
        for ( int value : values ) {
            checksum += value;
        }
        return checksum;
    }

    private Pair<Integer, String> sendPayload() {
        Pair<Integer, String> result = new Pair<>(0, "Program successfully uploaded");
        int payloadLength;
        int writtenBytes;
        if ( !serialPort.isOpen() ) {
            serialPort.openPort();
        }
        for ( byte[] payload : payloads ) {
            payloadLength = payload.length;
            writtenBytes = serialPort.writeBytes(payload, payloadLength);
            if ( writtenBytes != payloadLength || !receiveAnswer() ) {
                result = new Pair<>(1, "Something went wrong while uploading the program. If this happens again, please reconnect the robot with the computer and try again");
                break;
            }
        }
        if ( result.getFirst() == 0 ) {
            LOG.info("Program successfully uploaded");
        }
        clearAndCloseAll();
        return result;
    }

    private boolean receiveAnswer() {
        short bufSize = 128;
        byte[] buf = new byte[bufSize];
        String bufAsHexString;
        Matcher responseMatcher;
        long time = System.currentTimeMillis();
        while ( (System.currentTimeMillis()) - time < 3000 ) {
            serialPort.readBytes(buf, bufSize);
            bufAsHexString = Hex.encodeHexString(buf);
            responseMatcher = responsePattern.matcher(bufAsHexString);
            if ( responseMatcher.find() ) {
                if ( responseMatcher.group(4) != null ) { //4 == regex error group
                    LOG.error("A package could not be delivered");
                    return false;
                }
                return true;
            }
        }
        LOG.error("Timeout: No response received");
        return false;
    }

    private byte[] convertArrayListToByteArray(List<Byte> arrayList) {
        byte[] result = new byte[arrayList.size()];
        for ( int i = 0; i < arrayList.size(); i++ ) {
            result[i] = arrayList.get(i);
        }
        return result;
    }

    private void clearAndCloseAll() {
        serialPort.closePort();
        payloads.clear();
        fileContent.clear();
    }
}