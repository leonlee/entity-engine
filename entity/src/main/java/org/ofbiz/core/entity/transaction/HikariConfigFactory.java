package org.ofbiz.core.entity.transaction;

import com.zaxxer.hikari.HikariConfig;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class HikariConfigFactory {

    private static final Logger log = Logger.getLogger(HikariConfigFactory.class);

    /**
     * Get a hikari config object. If a filename is provided, we will attempt to load it and initially configure
     * with any properties present. Failing that, a default config will be returned.
     * @param filename
     * @return HikariConfig
     */
    public HikariConfig getHikariConfig(final String filename) {
        Properties properties = loadPropertiesFile(filename);
        return properties.isEmpty() ? new HikariConfig() : new HikariConfig(properties);

    }

    private static Properties loadPropertiesFile(final String filename) {
        Properties properties = new Properties();
        InputStream inputStream = AbstractConnectionFactory.class.getResourceAsStream("/" + filename);
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                log.error("Error loading properties file: " + filename + ", " + properties, e);
            }
        }

        return properties;
    }
}
