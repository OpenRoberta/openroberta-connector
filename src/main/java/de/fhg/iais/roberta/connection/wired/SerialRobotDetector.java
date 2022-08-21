package de.fhg.iais.roberta.connection.wired;

import de.fhg.iais.roberta.connection.wired.mBot2.Mbot2;
import static de.fhg.iais.roberta.util.WiredRobotIdFileHelper.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.connection.wired.arduino.Arduino;
import de.fhg.iais.roberta.connection.wired.microbit.Microbit;
import de.fhg.iais.roberta.util.Pair;
import de.fhg.iais.roberta.util.SerialDevice;

public class SerialRobotDetector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(SerialRobotDetector.class);

    private Map<SerialDevice, WiredRobotType> supportedRobots;
    private Map<Integer, String> readIdFileErrors;

    public SerialRobotDetector() {
        Pair<Map<SerialDevice, WiredRobotType>, Map<Integer, String>> loadIdsResult = load();
        this.supportedRobots = loadIdsResult.getFirst();
        this.readIdFileErrors = loadIdsResult.getSecond();
    }

    public Map<Integer, String> getReadIdFileErrors() {
        return new HashMap<>(this.readIdFileErrors);
    }

    @Override
    public List<IRobot> detectRobots() {
        List<IRobot> detectedRobots = new ArrayList<>(20);

        Pair<Map<SerialDevice, WiredRobotType>, Map<Integer, String>> loadIdsResult = load();
        this.supportedRobots = loadIdsResult.getFirst();
        this.readIdFileErrors = loadIdsResult.getSecond();

        List<SerialDevice> devices = getUsbDevices();

        for ( SerialDevice device : devices ) {
            WiredRobotType wiredRobotType = this.supportedRobots.get(device);
            if ( wiredRobotType != null ) {
                switch ( wiredRobotType ) {
                    case UNO:
                    case MEGA:
                    case NANO:
                    case BOB3:
                    case BOTNROLL:
                    case MBOT:
                    case UNOWIFIREV2:
                    case FESTOBIONIC:
                    case FESTOBIONICFLOWER:
                    case NANO33BLE:
                    case ROB3RTA:
                        detectedRobots.add(new Arduino(wiredRobotType, device.port));
                        break;
                    case MICROBIT:
                        detectedRobots.add(new Microbit(device.port));
                        break;
                    case MBOT2:
                        detectedRobots.add(new Mbot2(wiredRobotType, device.port));
                        break;
                    case EV3:
                    case NONE:
                        throw new IllegalStateException("Robot type not supported");
                    default:
                        throw new IllegalStateException("Robot type not implemented");
                }
            }
        }
        return detectedRobots;
    }

    public static String getPortOfConnectedRobotType(WiredRobotType wiredRobotTypeToLookup) {
        Pair<Map<SerialDevice, WiredRobotType>, Map<Integer, String>> loadIdsResult = load();
        Map<SerialDevice, WiredRobotType> supportedRobots = loadIdsResult.getFirst();
        List<SerialDevice> devices = getUsbDevices();
        for ( SerialDevice device : devices ) {
            WiredRobotType wiredRobotType = supportedRobots.get(device);
            if ( wiredRobotType == wiredRobotTypeToLookup ) {
                return device.port;
            }
        }
        return null;
    }

    public static List<SerialDevice> getUsbDevices() {
        if ( SystemUtils.IS_OS_LINUX ) {
            LOG.debug("Linux detected");
            return getUsbDevicesLinux();
        } else if ( SystemUtils.IS_OS_WINDOWS ) {
            LOG.debug("Windows detected");
            return getUsbDevicesWindows();
        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            LOG.debug("OSX detected");
            return getUsbDevicesMacOSX();
        }
        throw new UnsupportedOperationException("Operating system not supported!");
    }

    private static List<SerialDevice> getUsbDevicesLinux() {
        List<SerialDevice> devices = new ArrayList<>(5);
        File devicesDir = new File("/sys/bus/usb/devices");

        // check every usb device
        for ( File deviceDir : Objects.requireNonNull(devicesDir.listFiles()) ) {
            File idVendorFile = new File(deviceDir, "idVendor");
            File idProductFile = new File(deviceDir, "idProduct");

            // if the id files exist check the content
            if ( idVendorFile.exists() && idProductFile.exists() ) {
                try (Stream<String> vendorLines = Files.lines(idVendorFile.toPath()); Stream<String> productLines = Files.lines(idProductFile.toPath())) {

                    String idVendor = vendorLines.findFirst().orElseThrow(() -> new IllegalStateException("Vendor id could not be read"));
                    String idProduct = productLines.findFirst().orElseThrow(() -> new IllegalStateException("Product id could not be read"));

                    // recover the tty portname of the device
                    // it can be found in the subdirectory with the same name as the device
                    String[] port = new String[1];
                    for ( File subDir : deviceDir.listFiles() ) {
                        if ( subDir.getName().contains(deviceDir.getName()) ) {
                            List<File> subSubDirs = Arrays.asList(subDir.listFiles());

                            // look for a directory containing tty, in case its only called tty look into it to find the real name
                            subSubDirs
                                .stream()
                                .filter(file -> file.getName().contains("tty"))
                                .findFirst()
                                .ifPresent(file -> port[0] = file.getName().equals("tty") ? file.list()[0] : file.getName());
                        }
                    }

                    if ( port[0] != null ) {
                        devices.add(new SerialDevice(idVendor, idProduct, port[0], ""));
                    }

                } catch ( IOException e ) {
                    // continue if id files do not exist
                }
            }
        }
        return devices;
    }

    private static List<SerialDevice> getUsbDevicesWindows() {
        List<SerialDevice> devices = new ArrayList<>(5);

        ProcessBuilder processBuilder =
            new ProcessBuilder("powershell.exe", "-Command", "Get-WmiObject -Query \\\"SELECT Name, DeviceID FROM Win32_PnPEntity\\\"");
        try {
            Process pr = processBuilder.start();
            // Output stream has to be closed to work around process buffer size hanging
            pr.getOutputStream().close();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream(), Charsets.UTF_8));
                BufferedReader errReader = new BufferedReader(new InputStreamReader(pr.getErrorStream(), Charsets.UTF_8))) {
                String result = reader.lines().collect(Collectors.joining("\n"));
                // Also read the error stream to avoid hanging
                String errors = errReader.lines().collect(Collectors.joining("\n"));

                Matcher matcher = Pattern.compile("DeviceID\\s*:.*\\\\VID_(\\w{4}).PID_(\\w{4}).*\\nName\\s*: (.*)\\((COM\\d*)\\)").matcher(result);

                while ( matcher.find() ) {
                    String idVendor = matcher.group(1);
                    String idProduct = matcher.group(2);
                    String name = matcher.group(3);
                    String port = matcher.group(4);

                    devices.add(new SerialDevice(idVendor, idProduct, port, name));
                }
            }
            pr.waitFor();
        } catch ( InterruptedException | IOException e ) {
            LOG.error("Something went wrong while trying to query for Win32_PnPEntities: {}", e.getMessage());
        }
        return devices;
    }

    private static List<SerialDevice> getUsbDevicesMacOSX() {
        List<SerialDevice> devices = new ArrayList<>(5);
        try {
            Runtime rt = Runtime.getRuntime();
            String[] commands =
                {
                    "/bin/sh",
                    "-c",
                    "ioreg -r -c IOUSBHostDevice -l | grep -B30 IOTTYDevice"
                };
            Process pr = rt.exec(commands);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream(), Charsets.UTF_8))) {
                String result = reader.lines().collect(Collectors.joining("\n"));

                Matcher idVendorMatcher = Pattern.compile("\"idVendor\" = (\\d*)").matcher(result);
                Matcher idProductMatcher = Pattern.compile("\"idProduct\" = (\\d*)").matcher(result);
                Matcher ttyMatcher = Pattern.compile("\"IOTTYDevice\" = \"(.*)\"").matcher(result);
                Matcher nameMatcher = Pattern.compile("\"Product Name\" = \"(.*)\"").matcher(result);

                while ( idVendorMatcher.find() && idProductMatcher.find() && ttyMatcher.find() ) {
                    String idVendorDec = idVendorMatcher.group(1);
                    String idProductDec = idProductMatcher.group(1);
                    String tty = "tty." + ttyMatcher.group(1);

                    String name = "";
                    if ( nameMatcher.find() ) {
                        name = nameMatcher.group(1);
                    }
                    String idVendor = String.format("%04X", Integer.valueOf(idVendorDec));
                    String idProduct = String.format("%04X", Integer.valueOf(idProductDec));

                    devices.add(new SerialDevice(idVendor, idProduct, tty, name));
                }
            }
        } catch ( IOException e ) {
            LOG.error("Something went wrong while trying to get ioreg output: {}", e.getMessage());
        }
        return devices;
    }
}
