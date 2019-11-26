package de.fhg.iais.roberta.connection.wired.arduino;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;

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
            String pArg;
            String cArg;
            String eArg = "";
            switch ( this.robot.getType() ) {
                // specify if different
                case MEGA:
                    pArg = "-patmega2560";
                    cArg = "-cwiring";
                    break;
                case BOB3:
                    pArg = "-patmega88";
                    cArg = "-cavrisp2";
                    eArg = "-e";
                    break;
                default: // take uno config as default, this is used by Uno, Nano, Bot'n Roll and Mbot
                    pArg = "-patmega328p";
                    cArg = "-carduino";
                    break;
            }

            LOG.info("Starting to upload program {} to {}{}", filePath, portPath, portName);
            ProcessBuilder
                processBuilder =
                new ProcessBuilder(this.avrPath,
                                   "-v",
                                   "-D",
                                   pArg,
                                   cArg,
                                   "-Uflash:w:" + filePath + ":i",
                                   "-C" + this.avrConfPath,
                                   "-P" + portPath + portName,
                                   eArg);

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
