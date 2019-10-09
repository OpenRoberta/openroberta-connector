package de.fhg.iais.roberta.util;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.fhg.iais.roberta.connection.wired.WiredRobotType;

public final class WiredRobotIdFileHelper {
    private static final Logger LOG = LoggerFactory.getLogger(WiredRobotIdFileHelper.class);

    private static final String WIRED_ROBOT_ID_FILENAME = "wired-robot-ids.txt";
    private static final String
        WIRED_ROBOT_ID_FILEPATH =
        SystemUtils.getUserHome().getPath() + File.separator + "OpenRobertaConnector" + File.separator + WIRED_ROBOT_ID_FILENAME;

    private WiredRobotIdFileHelper() {
    }

    public static Pair<Map<SerialDevice, WiredRobotType>, Map<Integer, String>> load() {
        Map<SerialDevice, WiredRobotType> supportedRobots = new HashMap<>(20);
        Map<Integer, String> readIdFileErrors = new HashMap<>(20);

        File file = new File(WIRED_ROBOT_ID_FILEPATH);

        if ( !file.exists() ) {
            LOG.warn("Could not find {}, using default file!", WIRED_ROBOT_ID_FILEPATH);
        }

        try (InputStream inputStream = (file.exists()) ?
                                       new FileInputStream(file) :
                                       WiredRobotIdFileHelper.class.getClassLoader().getResourceAsStream(WIRED_ROBOT_ID_FILENAME);
             BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))) {
            String line;
            int lineNr = 1;
            while ( (line = br.readLine()) != null ) {
                if ( !line.isEmpty() && !line.startsWith("#") ) {
                    List<String> values = Arrays.asList(line.split(","));

                    String error = checkIdEntryFormat(values);
                    if ( error.isEmpty() ) {
                        WiredRobotType wiredRobotType = WiredRobotType.fromString(values.get(2));
                        if (wiredRobotType.isSerial()) {
                            supportedRobots.put(new SerialDevice(values.get(0), values.get(1), "", ""), wiredRobotType);
                        }
                    } else {
                        readIdFileErrors.put(lineNr, error);
                    }
                }
                lineNr++;
            }
        } catch ( FileNotFoundException e ) {
            LOG.error("Could not find file {}: {}", WIRED_ROBOT_ID_FILENAME, e.getMessage());
        } catch ( IOException e ) {
            LOG.error("Something went wrong while loading the {} file: {}", WIRED_ROBOT_ID_FILENAME, e.getMessage());
            readIdFileErrors.put(0, e.getMessage());
        }

        return new Pair<>(supportedRobots, readIdFileErrors);
    }

    public static void save(Iterable<? extends List<String>> entries) {
        File file = new File(WIRED_ROBOT_ID_FILEPATH);

        Map<Integer, String> readIdFileErrors = new HashMap<>(20);

        int lineNr = 0;
        for ( List<String> entry : entries ) {
            String error = checkIdEntryFormat(entry);
            if ( !error.isEmpty() ) {
                readIdFileErrors.put(lineNr, error);
                lineNr++;
            }
        }

        if ( !readIdFileErrors.isEmpty() ) {
            return;
        }

        try (FileOutputStream os = new FileOutputStream(file); OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            for ( List<String> entry : entries ) {
                writer.write(entry.get(0) + ',' + entry.get(1) + ',' + entry.get(2) + System.lineSeparator());
            }
        } catch ( FileNotFoundException e ) {
            LOG.error("Could not find file {}: {}", WIRED_ROBOT_ID_FILENAME, e.getMessage());
        } catch ( IOException e ) {
            LOG.error("Something went wrong while writing the {} file: {}", WIRED_ROBOT_ID_FILENAME, e.getMessage());
        }
    }

    private static String checkIdEntryFormat(List<String> values) {
        if ( values.size() == 3 ) {
            try {
                String.valueOf(Integer.valueOf(values.get(0), 16));
            } catch ( NumberFormatException e ) {
                return "errorConfigVendorId";
            }
            try {
                String.valueOf(Integer.valueOf(values.get(1), 16));
            } catch ( NumberFormatException e ) {
                return "errorConfigProductId";
            }
            try {
                WiredRobotType.fromString(values.get(2));
            } catch ( IllegalArgumentException e ) {
                return "errorConfigArduinoType";
            }
        } else {
            return "errorConfigFormat";
        }
        return "";
    }
}
