package de.fhg.iais.roberta.util;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RobotPropertyHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RobotPropertyHelper.class);

    private static final String ROBOT_PROPERTY_NAME = "robots.properties";

    private final Properties properties = new Properties();

    private RobotPropertyHelper() {
        try {
            this.properties.load(this.getClass().getClassLoader().getResourceAsStream(ROBOT_PROPERTY_NAME));
        } catch ( IOException e ) {
            LOG.error("Could not load properties: {}", e.getMessage());
        }
    }

    private static final class InstanceHolder {
        private static final RobotPropertyHelper instance = new RobotPropertyHelper();
    }

    public static RobotPropertyHelper getInstance() {
        return InstanceHolder.instance;
    }

    public Boolean hasSerial(String robot) {
        return this.properties.getProperty(robot + ".serial") != null;
    }

    public String getBaudRate(String robot){
        return this.properties.getProperty(robot + ".serial.baudrate");
    }
}
