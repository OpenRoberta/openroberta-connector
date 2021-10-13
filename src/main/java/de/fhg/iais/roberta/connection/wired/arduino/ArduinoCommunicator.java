package de.fhg.iais.roberta.connection.wired.arduino;

import static de.fhg.iais.roberta.util.PythonRequireHelper.requireEsptool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.connection.wired.IWiredRobot;
import de.fhg.iais.roberta.connection.wired.SerialRobotDetector;
import de.fhg.iais.roberta.connection.wired.WiredRobotType;
import de.fhg.iais.roberta.util.Pair;
import de.fhg.iais.roberta.util.PropertyHelper;
import de.fhg.iais.roberta.util.ZipHelper;

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
            this.bossacPath = PropertyHelper.getInstance().getProperty("bossacLinPath");
        } else if ( SystemUtils.IS_OS_MAC ) {
            this.avrdudePath = PropertyHelper.getInstance().getProperty("avrdudeOsXPath");
            this.avrdudeConfPath = PropertyHelper.getInstance().getProperty("avrdudeMacConfPath");
            this.bossacPath = null; // TODO add mac bossac
        } else {
            throw new UnsupportedOperationException("Operating system not supported!");
        }
        if (( this.robot.getType() == WiredRobotType.FESTOBIONIC )||( this.robot.getType() == WiredRobotType.FESTOBIONICFLOWER )) {
            this.esptoolPath = requireEsptool();
        }
    }

    /**
     * Check whether the esptool is initialized. Always returns true if it is not required.
     *
     * @return whether esptool is initialized, or true if it is not required
     */
    boolean isEspInitialized() {
        return ((this.robot.getType() != WiredRobotType.FESTOBIONIC)&&(this.robot.getType() != WiredRobotType.FESTOBIONICFLOWER)) || !this.esptoolPath.isEmpty();
    }

    JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put("firmwarename", this.robot.getType().toString());
        deviceInfo.put("robot", this.robot.getType().toString());
        deviceInfo.put("brickname", this.robot.getName());

        return deviceInfo;
    }

    Pair<Integer, String> uploadFile(String portName, String filePath) {
        portName = (SystemUtils.IS_OS_WINDOWS ? "" : "/dev/") + portName; // to hide the parameter, which should not be used
        ArgsAdder args = new ArgsAdder();
        try {
            switch ( this.robot.getType() ) {
                case UNO:
                case NANO:
                case BOTNROLL:
                case MBOT:
                    addAvrDudeStdParams(args, avrdudePath, avrdudeConfPath, filePath, portName);
                    args.add("-patmega328p", "-carduino");
                    return runProcessUntilTermination(args, true);
                case MEGA:
                    addAvrDudeStdParams(args, avrdudePath, avrdudeConfPath, filePath, portName);
                    args.add("-patmega2560", "-cwiring");
                    return runProcessUntilTermination(args, true);
                case BOB3:
                    addAvrDudeStdParams(args, avrdudePath, avrdudeConfPath, filePath, portName);
                    args.add("-patmega88", "-cavrisp2", "-e");
                    return runProcessUntilTermination(args, true);
                case UNOWIFIREV2:
                    addAvrDudeStdParams(args, avrdudePath, avrdudeConfPath, filePath, "usb");
                    args.add("-patmega4809", "-cxplainedmini_updi", "-e");
                    args.add("-Ufuse2:w:0x01:m", "-Ufuse5:w:0xC9:m", "-Ufuse8:w:0x02:m"); // program fuses
                    args.add("-Uflash:w:" + PropertyHelper.getInstance().getProperty("megaavrPath") + "/bootloaders/atmega4809_uart_bl.hex:i"); // additional bootloader
                    return runProcessUntilTermination(args, true);
                case FESTOBIONICFLOWER:
                case FESTOBIONIC:
                    LOG.info("Starting to upload program {} to {}", filePath, portName);
                    // files are zipped serverside and sent to the Connector, unzipped here and flashed by the esptool
                    File zipFile = Paths.get(filePath).toFile();
                    Path tempDirectory = Files.createTempDirectory(null);
                    ZipHelper.unzipFiles(Paths.get(filePath), tempDirectory);

                    args.add(this.esptoolPath);
                    args.add("--chip", "esp32");
                    args.add("--port", portName);
                    args.add("--baud", SystemUtils.IS_OS_MAC ? "115200" : "921600");
                    args.add("--before", "default_reset");
                    args.add("--after", "hard_reset");
                    args.add("write_flash");
                    args.add("-z");
                    args.add("--flash_mode", "dio");
                    args.add("--flash_freq", "80m");
                    args.add("--flash_size", "detect");
                    Path tempDirN = tempDirectory.normalize();
                    String zipFilePrefix = zipFile.getName().split("\\.")[0];
                    Path zipFileSpiffs = tempDirN.resolve(zipFilePrefix + ".spiffs.bin");
                    args.add("0xe000", tempDirN.resolve("boot_app0.bin").toString());
                    args.add("0x1000", tempDirN.resolve("bootloader_qio_80m.bin").toString());
                    args.add("0x10000", tempDirN.resolve(zipFilePrefix + ".bin").toString());
                    args.add("0x8000", tempDirN.resolve(zipFilePrefix + ".partitions.bin").toString());
                    if ( Files.exists(zipFileSpiffs) ) {
                        args.add("0x291000", zipFileSpiffs.toString());
                    }
                    return runProcessUntilTermination(args, true);
                case NANO33BLE:
                    LOG.info("Starting to upload program {}, port of running mode {} ...", filePath, portName);
                    ArgsAdder gotoFlash = new ArgsAdder();
                    if ( SystemUtils.IS_OS_WINDOWS ) {
                        gotoFlash.add("mode.com", portName, "1200,o,8,1");
                    } else {
                        gotoFlash.add("stty", "-F", portName, "1200");
                    }
                    runProcessUntilTermination(gotoFlash, true);
                    Thread.sleep(500);
                    String portForFlashing =
                        (SystemUtils.IS_OS_WINDOWS ? "" : "/dev/") + SerialRobotDetector.getPortOfConnectedRobotType(WiredRobotType.NANO33BLE);
                    LOG.info("... port of flashing mode {} ", portForFlashing);
                    args.add(this.bossacPath);
                    args.add("-d", "--port=" + portForFlashing, "-U", "-i", "-e", "-w", filePath, "-R");
                    return runProcessUntilTermination(args, true);
                case ROB3RTA:
                    addAvrDudeStdParams(args, avrdudePath, avrdudeConfPath, filePath, portName);
                    args.add("-patmega328pb", "-cavrisp2", "-e");
                    return runProcessUntilTermination(args, true);
                default:
                    throw new IllegalStateException("Robot type not supported");
            }
        } catch ( Exception e ) {
            LOG.error("Error while uploading to arduino: {}", e.getMessage());
            return new Pair<>(1, "Something went wrong while uploading the file.");
        }
    }

    /**
     * add the avrdudePath and some standard parameter to the arg list
     *
     * @param args to this list the arguments are appended
     * @param avrdudePath path to the avrdude binary
     * @param avrdudeConfPath path to the configuration file of avrdude
     * @param filePath path to the binary generated by the lab
     */
    private static void addAvrDudeStdParams(ArgsAdder args, String avrdudePath, String avrdudeConfPath, String filePath, String portName) {
        LOG.info("Starting to upload program {} to {}", filePath, portName);
        args.add(avrdudePath); // path to executable
        args.add("-v"); // verbose output
        args.add("-D"); // disables auto erase for flashing
        args.add("-C" + avrdudeConfPath); // specific config file
        args.add("-Uflash:w:" + filePath + ":i"); // the program to flash
        args.add("-P" + portName);
    }

    private static Pair<Integer, String> runProcessUntilTermination(ArgsAdder args, boolean log) {
        try {
            if ( log ) {
                LOG.info("command to be executed: {}", args.toString());
            }

            ProcessBuilder processBuilder = new ProcessBuilder(args.getArgs());
            processBuilder.inheritIO();
            Process p = processBuilder.start();
            int eCode = p.waitFor();
            String errorOutput = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
            if ( log ) {
                if ( eCode > 0 ) {
                    LOG.error("process to flash an arduino failed: {}, {}", eCode, errorOutput);
                } else if ( log ) {
                    LOG.info("command execution was successful");
                }
            }
            return new Pair<>(eCode, errorOutput);
        } catch ( IOException | InterruptedException e ) {
            String msg = "Error while running a process to flash an arduino: " + e.getMessage();
            LOG.error(msg);
            return new Pair<>(1, msg);
        }
    }

    private static class ArgsAdder {
        private final List<String> args = new ArrayList<>();
        public ArgsAdder add(String... args) {
            for ( String arg : args ) {
                this.args.add(arg);
            }
            return this;
        }

        public List<String> getArgs() {
            return args;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for ( String arg : args ) {
                sb.append(arg).append(' ');
            }
            return sb.toString();
        }
    }
}
