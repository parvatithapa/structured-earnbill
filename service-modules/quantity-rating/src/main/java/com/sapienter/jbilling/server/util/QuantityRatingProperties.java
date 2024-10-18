package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.exception.QuantityRatingException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;


public final class QuantityRatingProperties {

    private static final FormatLogger logger = new FormatLogger(Logger.getLogger(MethodHandles.lookup().lookupClass()));

    private static final String RESOURCE_PROPS_FILE = "properties/jbilling_quantity_rating.properties";
    private static final String OVERRIDE_PROPS_FILE_SYSPROP_NAME = "overrideRatingPropertyFile";

    private static final Properties props = new Properties();

    static {
        loadDefaultProperties();
        loadPropertiesFromFile(System.getProperty(OVERRIDE_PROPS_FILE_SYSPROP_NAME));
        loadSystemProperties();
    }

    private static void loadDefaultProperties() {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try (InputStream is = loader.getResourceAsStream(RESOURCE_PROPS_FILE)) {
            props.load(is);

        } catch(IOException ie) {
            throw new QuantityRatingException("Failed to load resource properties file " +
                    RESOURCE_PROPS_FILE);
        }
    }

    private static void loadPropertiesFromFile(String filePath) {
        try {
            if (StringUtils.isEmpty(filePath) || !Files.exists(Paths.get(filePath))) {
                logger.info("No external properties files specified, moving ahead !");
                return;
            }
        } catch (InvalidPathException e) {
            throw new QuantityRatingException("Invalid external properties file path");
        }

        try (InputStream is = new FileInputStream(RESOURCE_PROPS_FILE)) {
            props.load(is);

        } catch(IOException ie) {
            throw new QuantityRatingException("Failed to load properties file " + filePath);
        }
    }

    private static void loadSystemProperties() {
        props.entrySet().stream()
                .forEach(e -> {
                    String val = System.getProperty((String)e.getKey());
                    if (val != null) {
                        e.setValue(val);
                    }
                });
    }

    public static String get(final String key) {
        return Optional.ofNullable(props.getProperty(key))
                .orElseThrow(() ->
                    new QuantityRatingException("Missing property: " + key)
                );
    }

    public static String get(final String key, final String defaultValue) {
        return Optional.ofNullable(props.getProperty(key))
                .orElse(defaultValue);
    }
}
