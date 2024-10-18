package com.jbilling.framework.utilities.xmlutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.jbilling.framework.globals.GlobalConsts;

public class PropertyFileReaderUpdater {

	public String readPropertyFromFile(final String key, String fileName) throws IOException {
		String value = "";
        final Properties prop = readAllProperties(fileName);
        value = prop.getProperty(key);
		return value;
	}

    public Properties readAllProperties(String fileName) throws IOException {
        final Properties prop = new Properties();
        final File f = new File(GlobalConsts.getProjectDir() + "/test/automation/resources/" + fileName);
        if (f.exists()) {
            prop.load(new FileInputStream(f));
        }
        return prop;
    }

    public void updatePropertyInFile(final String key, final String value, String fileName) throws IOException {
		final Properties props = new Properties();
		final String propsFileName = GlobalConsts.getProjectDir() + "/test/automation/resources/" + fileName + ".properties";

		// first load old property file:
		final FileInputStream configStream = new FileInputStream(propsFileName);
		props.load(configStream);
		configStream.close();

		// modifies new property
		props.setProperty(key, value);

		// save modified property file
		final FileOutputStream output = new FileOutputStream(propsFileName);
		props.store(output, "");
		output.close();
	}
}
