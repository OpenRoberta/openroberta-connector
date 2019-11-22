package de.fhg.iais.roberta.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PythonRequireHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PythonRequireHelper.class);

    // When upgrading also upgrade the shipped .exe for Windows!
    private static final String ESPTOOL_REQ = "esptool==2.8";

    private static final String GET_PIP_URL = "https://bootstrap.pypa.io/get-pip.py";

    private static final String DEFAULT_BIN_PATH_MAC = System.getenv("HOME") + "/Library/Python/2.7/bin/";
    private static final String DEFAULT_BIN_PATH_LINUX = System.getenv("HOME") + "/.local/bin/";

    private PythonRequireHelper() {
    }

    /**
     * Returns the base command to run esptool.py. Will check for its availability and install it using pip if necessary.
     *
     * @return the base command to run esptool.py, or an empty string if the requirement could not be fulfilled
     */
    public static String requireEsptool() {
        // For Windows esptool is bundled with the application
        if (SystemUtils.IS_OS_WINDOWS) {
            return PropertyHelper.getInstance().getProperty("espPath");
        }
        String esptoolCmd = "esptool.py";
        Pair<Boolean, String> esptoolOutput = runCommand(esptoolCmd, "version");
        if (esptoolOutput.getFirst()) {
            LOG.info("esptool is in PATH: {}", esptoolOutput.getSecond().trim());
        } else {
            LOG.info("esptool is not in PATH");

            if (SystemUtils.IS_OS_MAC) {
                LOG.info("Checking default install location");
                Pair<Boolean, String> macEsptoolOutput = runCommand(DEFAULT_BIN_PATH_MAC + "esptool.py", "version");
                if (macEsptoolOutput.getFirst()) {
                    return DEFAULT_BIN_PATH_MAC + "esptool.py";
                }
            } else if (SystemUtils.IS_OS_LINUX) {
                LOG.info("Checking default install location");
                Pair<Boolean, String> linuxEsptoolOutput = runCommand(DEFAULT_BIN_PATH_LINUX + "esptool.py", "version");
                if (linuxEsptoolOutput.getFirst()) {
                    return DEFAULT_BIN_PATH_LINUX + "esptool.py";
                }
            }

            Pair<Boolean, String> pythonReq = requirePython();
            if (pythonReq.getFirst()) {
                installWithPip(ESPTOOL_REQ);
            } else {
                LOG.warn("User needs to install Python!");
            }
        }
        return esptoolCmd;
    }

    private static Pair<Boolean, String> runCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        try {
            Process p = processBuilder.start();
            int eCode = p.waitFor();
            String output = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
            String error = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
            return new Pair<>(eCode == 0, output + error);
        } catch ( IOException | InterruptedException e ) {
            return new Pair<>(false, e.getMessage());
        }
    }

    private static Pair<Boolean, String> requirePython() {
        Pair<Boolean, String> pythonOutput = runCommand("python", "--version");
        if (pythonOutput.getFirst()) {
            LOG.info("python is in PATH: {}", pythonOutput.getSecond().trim());
            return new Pair<>(true, "python");
        } else {
            LOG.info("python is not in PATH");
        }
        return new Pair<>(false, "");
    }

    private static boolean installWithPip(String requirement) {
        Pair<Boolean, String> pipReq = requirePip();
        if (pipReq.getFirst()) {
            LOG.info("Installing {} with pip", requirement);
            Pair<Boolean, String> pipOutput = runCommand(pipReq.getSecond(), "install", "--user", requirement);
            if (pipOutput.getFirst()) {
                LOG.info("Successfully installed {}", requirement);
                return true;
            } else {
                LOG.error("Could not install {} via pip: {}", requirement, pipOutput.getSecond());
            }
        }
        return false;
    }

    private static Pair<Boolean, String> requirePip() {
        Pair<Boolean, String> pipOutput = runCommand("pip", "--version");
        if (pipOutput.getFirst()) {
            LOG.info("pip is in PATH: {}", pipOutput.getSecond().trim());
            return new Pair<>(true, "pip");
        } else {
            LOG.info("pip is not in PATH");

            if (SystemUtils.IS_OS_MAC) {
                LOG.info("Checking default install location");
                Pair<Boolean, String> macPipOutput = runCommand(DEFAULT_BIN_PATH_MAC + "pip", "--version");
                if (macPipOutput.getFirst()) {
                    return new Pair<>(true, DEFAULT_BIN_PATH_MAC + "pip");
                }
            }
            if (SystemUtils.IS_OS_LINUX) {
                LOG.info("Checking default install location");
                Pair<Boolean, String> linuxPipOutput = runCommand(DEFAULT_BIN_PATH_LINUX + "pip", "--version");
                if (linuxPipOutput.getFirst()) {
                    return new Pair<>(true, DEFAULT_BIN_PATH_LINUX + "pip");
                }
            }

            LOG.info("Installing pip");
            try {
                URL url = new URL(GET_PIP_URL);

                File getPipFile = Files.createTempFile(null, ".py").toFile();
                ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(getPipFile);
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                Pair<Boolean, String> getPipOutput = runCommand("python", getPipFile.getAbsolutePath());
                if (getPipOutput.getFirst()) {
                    if (getPipOutput.getSecond().contains("which is not on PATH")) {
                        LOG.info("pip install location is not on path");

                        LOG.info("extracting from pip install output");
                        Matcher m = Pattern.compile("are installed in '(.*)' which is not on PATH").matcher(getPipOutput.getSecond());
                        if (m.find()) {
                            return new Pair<>(true, m.group(1));
                        }
                    } else {
                        return new Pair<>(true, "pip");
                    }
                } else {
                    LOG.error("Something went wrong while installing pip");
                }
            } catch (IOException e) {
                LOG.error("Could not download get-pip.py");
            }
        }
        return new Pair<>(false, "");
    }
}
