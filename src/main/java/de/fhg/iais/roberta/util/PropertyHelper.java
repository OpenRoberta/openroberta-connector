package de.fhg.iais.roberta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public final class PropertyHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyHelper.class);

    private static final String DEFAULT_PROPERTY_NAME = "OpenRobertaConnector.properties";

    private final Properties properties = new Properties();

    private PropertyHelper() {
        try {
            this.properties.load(this.getClass().getClassLoader().getResourceAsStream(DEFAULT_PROPERTY_NAME));
        } catch ( IOException e ) {
            LOG.error("Could not load properties: {}", e.getMessage());
        }
    }

    private static final class InstanceHolder {
        private static final PropertyHelper instance = new PropertyHelper();
    }

    public static PropertyHelper getInstance() {
        return InstanceHolder.instance;
    }

    public String getProperty(String key) {
        return this.properties.getProperty(key);
    }
}
