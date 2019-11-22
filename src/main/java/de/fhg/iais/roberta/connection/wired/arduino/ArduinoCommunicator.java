package de.fhg.iais.roberta.connection.wired.arduino;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import de.fhg.iais.roberta.connection.wired.IWiredRobot;
import de.fhg.iais.roberta.util.Pair;
import de.fhg.iais.roberta.util.PropertyHelper;

class ArduinoCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoCommunicator.class);

    private String avrPath = ""; //path for avrdude bin
    private String avrConfPath = ""; //path for the .conf file
    private final IWiredRobot robot;

    ArduinoCommunicator(IWiredRobot robot) {
        this.robot = robot;
    }

    private void setParameters() {
        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.avrPath = PropertyHelper.getInstance().getProperty("WinPath");
            this.avrConfPath = PropertyHelper.getInstance().getProperty("WinConfPath");
        } else if ( SystemUtils.IS_OS_LINUX ) {
            if ( SystemUtils.OS_ARCH.equals("i386") ) {
                this.avrPath = PropertyHelper.getInstance().getProperty("LinPath32");
            } else if ( SystemUtils.OS_ARCH.equals("arm") ) {
                this.avrPath = PropertyHelper.getInstance().getProperty("LinPathArm32");
            } else {
                this.avrPath = PropertyHelper.getInstance().getProperty("LinPath64");
            }
            this.avrConfPath = PropertyHelper.getInstance().getProperty("LinConfPath");
        } else {
            this.avrPath = PropertyHelper.getInstance().getProperty("OsXPath");
            this.avrConfPath = PropertyHelper.getInstance().getProperty("MacConfPath");
        }
    }

    JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put("firmwarename", this.robot.getType().toString());
        deviceInfo.put("robot", this.robot.getType().toString());
        deviceInfo.put("brickname", this.robot.getName());

        return deviceInfo;
    }

    Pair<Integer, String> uploadFile(String portName, String filePath) {
        this.setParameters();
        String portPath = "/dev/";
        if ( SystemUtils.IS_OS_WINDOWS ) {
            portPath = "";
        }
        try {
            List<String> args = new ArrayList<>();
            args.add(this.avrPath); // path to executable
            args.add("-v"); // verbose output
            args.add("-D"); // disables auto erase for flashing
            args.add("-C" + this.avrConfPath); // specific config file
            args.add("-Uflash:w:" + filePath + ":i"); // the program to flash
            switch ( this.robot.getType() ) {
                case UNO:
                case NANO:
                case BOTNROLL:
                case MBOT:
                    args.add("-patmega328p"); // part number
                    args.add("-carduino"); // programmer
                    args.add("-P" + portPath + portName); // port of the device
                    break;
                case MEGA:
                    args.add("-patmega2560"); // part number
                    args.add("-cwiring"); // programmer
                    args.add("-P" + portPath + portName); // port of the device
                    break;
                case BOB3:
                    args.add("-patmega88"); // part number
                    args.add("-cavrisp2"); // programmer
                    args.add("-e"); // enable erase
                    args.add("-P" + portPath + portName); // port of the device
                    break;
                case UNOWIFIREV2:
                    args.add("-patmega4809"); // part number
                    args.add("-cxplainedmini_updi"); // programmer
                    args.add("-e"); // enable erase
                    args.add("-Pusb"); // port of the device
                    args.add("-Ufuse2:w:0x01:m"); // program fuses
                    args.add("-Ufuse5:w:0xC9:m"); // program fuses
                    args.add("-Ufuse8:w:0x02:m"); // program fuses
                    args.add("-Uflash:w:" + PropertyHelper.getInstance().getProperty("megaavrPath") + "/bootloaders/atmega4809_uart_bl.hex:i"); // additional bootloader
                    break;
                case FESTOBIONIC:
                    // TODO very specific workaround for now: files are zipped serverside and sent to the Connector
                    // here the files are unzipped and esptool is called to flash the two files
                    File zippedFile = new File(filePath);
                    File destDir = new File(zippedFile.getParent());
                    byte[] buffer = new byte[1024];
                    try(ZipInputStream zis = new ZipInputStream(new FileInputStream(zippedFile))) {
                        ZipEntry zipEntry = zis.getNextEntry();
                        while ( zipEntry != null ) {
                            File newFile = new File(destDir, zipEntry.getName());
                            try(FileOutputStream fos = new FileOutputStream(newFile)) {
                                int len;
                                while ( (len = zis.read(buffer)) > 0 ) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                            zipEntry = zis.getNextEntry();
                        }
                        zis.closeEntry();
                    }
                    args.clear(); // reset previous arguments, festo has specific ESP ones
                    args.add("python");
                    args.add(PropertyHelper.getInstance().getProperty("espPath") + "esptool.py");
                    args.add("--chip"); args.add("esp32");
                    args.add("--port"); args.add( portPath + portName);
                    args.add("--baud"); args.add( "921600");
                    args.add("--before"); args.add( "default_reset");
                    args.add("--after"); args.add( "hard_reset");
                    args.add("write_flash");
                    args.add("-z");
                    args.add("--flash_mode"); args.add( "dio");
                    args.add("--flash_freq"); args.add( "80m");
                    args.add("--flash_size"); args.add( "detect");
                    args.add("0xe000"); args.add( PropertyHelper.getInstance().getProperty("espPath") + "partitions/boot_app0.bin");
                    args.add("0x1000"); args.add( PropertyHelper.getInstance().getProperty("espPath") + "sdk/bin/bootloader_qio_80m.bin");
                    args.add("0x10000"); args.add( zippedFile.getParent() + "/" + zippedFile.getName().split("\\.")[0] + ".ino.bin");
                    args.add("0x8000"); args.add( zippedFile.getParent() + "/" + zippedFile.getName().split("\\.")[0] + ".ino.partitions.bin");
                    break;
                default:
                    throw new IllegalStateException("Robot type not supported");
            }

            LOG.info("Starting to upload program {} to {}{}", filePath, portPath, portName);

            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.inheritIO();
            Process p = processBuilder.start();
            int eCode = p.waitFor();
            String errorOutput = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
            if ( eCode == 0 ) {
                LOG.info("Program uploaded successfully");
            } else {
                LOG.error("Program was unable to be uploaded: {}, {}", eCode, errorOutput);
            }
            LOG.debug("Exit code {}", eCode);

            return new Pair<>(eCode, errorOutput);
        } catch ( IOException | InterruptedException e ) {
            LOG.error("Error while uploading to arduino: {}", e.getMessage());
            return new Pair<>(1, "Something went wrong while uploading the file.");
        }
    }
}
