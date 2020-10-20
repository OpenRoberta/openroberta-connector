package de.fhg.iais.roberta.connection.wired.arduino;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.fhg.iais.roberta.connection.wired.IWiredRobot;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;
import de.fhg.iais.roberta.util.Pair;
import de.fhg.iais.roberta.util.PropertyHelper;
import de.fhg.iais.roberta.util.ZipHelper;

import static de.fhg.iais.roberta.util.PythonRequireHelper.requireEsptool;

class ArduinoCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoCommunicator.class);

    private final String avrdudePath;
    private final String avrdudeConfPath;
    private final String bossacPath;
    private String esptoolPath = "";

    private final IWiredRobot robot;

    ArduinoCommunicator(IWiredRobot robot) {
        this.robot = robot;
        if ( SystemUtils.IS_OS_WINDOWS ) {
            this.avrdudePath = PropertyHelper.getInstance().getProperty("avrdudeWinPath");
            this.avrdudeConfPath = PropertyHelper.getInstance().getProperty("avrdudeWinConfPath");
            this.bossacPath = PropertyHelper.getInstance().getProperty("bossacWinPath");
        } else if ( SystemUtils.IS_OS_LINUX ) {
            if ( SystemUtils.OS_ARCH.equals("i386") ) {
                this.avrdudePath = PropertyHelper.getInstance().getProperty("avrdudeLinPath32");
            } else if ( SystemUtils.OS_ARCH.equals("arm") ) {
                this.avrdudePath = PropertyHelper.getInstance().getProperty("avrdudeLinPathArm32");
            } else {
                this.avrdudePath = PropertyHelper.getInstance().getProperty("avrdudeLinPath64");
            }
            this.avrdudeConfPath = PropertyHelper.getInstance().getProperty("avrdudeLinConfPath");
            this.bossacPath = null; // TODO add linux bossac
        } else if ( SystemUtils.IS_OS_MAC ) {
            this.avrdudePath = PropertyHelper.getInstance().getProperty("avrdudeOsXPath");
            this.avrdudeConfPath = PropertyHelper.getInstance().getProperty("avrdudeMacConfPath");
            this.bossacPath = null; // TODO add mac bossac
        } else {
            throw new UnsupportedOperationException("Operating system not supported!");
        }
        if ( this.robot.getType() == WiredRobotType.FESTOBIONIC) {
            this.esptoolPath = requireEsptool();
        }
    }

    /**
     * Check whether the esptool is initialized. Always returns true if it is not required.
     *
     * @return whether esptool is initialized, or true if it is not required
     */
    boolean isEspInitialized() {
        return this.robot.getType() != WiredRobotType.FESTOBIONIC || !this.esptoolPath.isEmpty();
    }

    JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put("firmwarename", this.robot.getType().toString());
        deviceInfo.put("robot", this.robot.getType().toString());
        deviceInfo.put("brickname", this.robot.getName());

        return deviceInfo;
    }

    Pair<Integer, String> uploadFile(String portName, String filePath) {
        String portPath = "/dev/";
        if ( SystemUtils.IS_OS_WINDOWS ) {
            portPath = "";
        }
        try {
            List<String> args = new ArrayList<>();
            args.add(this.avrdudePath); // path to executable
            args.add("-v"); // verbose output
            args.add("-D"); // disables auto erase for flashing
            args.add("-C" + this.avrdudeConfPath); // specific config file
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
                    // files are zipped serverside and sent to the Connector
                    // here the files are unzipped and esptool is called to flash the four files
                    File zipFile = Paths.get(filePath).toFile();
                    Path tempDirectory = Files.createTempDirectory(null);
                    ZipHelper.unzipFiles(Paths.get(filePath), tempDirectory);
                    args.clear(); // reset previous arguments, festo has specific ESP ones
                    args.add(this.esptoolPath);
                    args.add("--chip"); args.add("esp32");
                    args.add("--port"); args.add( portPath + portName);
                    if (SystemUtils.IS_OS_MAC) {
                        args.add("--baud"); args.add( "115200");
                    } else {
                        args.add("--baud"); args.add( "921600");
                    }
                    args.add("--before"); args.add( "default_reset");
                    args.add("--after"); args.add( "hard_reset");
                    args.add("write_flash");
                    args.add("-z");
                    args.add("--flash_mode"); args.add( "dio");
                    args.add("--flash_freq"); args.add( "80m");
                    args.add("--flash_size"); args.add( "detect");
                    args.add("0xe000"); args.add(tempDirectory.normalize() + "/boot_app0.bin");
                    args.add("0x1000"); args.add(tempDirectory.normalize() + "/bootloader_qio_80m.bin");
                    args.add("0x10000"); args.add(tempDirectory.normalize() + "/" + zipFile.getName().split("\\.")[0] + ".bin");
                    args.add("0x8000"); args.add(tempDirectory.normalize() + "/" + zipFile.getName().split("\\.")[0] + ".partitions.bin");
                    if (Files.exists(tempDirectory.resolve(zipFile.getName().split("\\.")[0] + ".spiffs.bin"))) {
                        args.add("0x291000"); args.add(tempDirectory.normalize() + "/" + zipFile.getName().split("\\.")[0] + ".spiffs.bin");
                    }
                    break;
                case NANO33BLE:
                    // TODO add reset & com rediscovery -> helper function in SerialRobotDetector?
                    args.clear();
                    args.add(this.bossacPath);
                    // bossac.exe -d --port=COM5 -U -i -e -w C:\Users\boonw\AppData\Local\Temp\arduino_build_94650/Blink.ino.bin -R
                    args.add("-d");
                    args.add("--port=" + portPath + portName);
                    args.add("-U");
                    args.add("-i");
                    args.add("-e");
                    args.add("-w");
                    args.add(filePath);
                    args.add("-R");
                    break;
                default:
                    throw new IllegalStateException("Robot type not supported");
            }

            LOG.info("Starting to upload program {} to {}{}", filePath, portPath, portName);
            LOG.info("Upload command used: {}", args);

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
