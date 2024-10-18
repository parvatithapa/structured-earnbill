package com.jbilling.framework.utilities.xmlutils;

import java.io.IOException;

import com.jbilling.framework.globals.GlobalConsts;

public class ConfigPropertiesReader extends PropertyFileReaderUpdater {

	public String readConfig(String key) throws IOException {
		String values = this.readPropertyFromFile(key, GlobalConsts.PropertiesFileName);
		return values;
	}

	public String readTestData(String key) throws IOException {
		String values = this.readPropertyFromFile(key, GlobalConsts.TestDataPropertiesFileName);
		return values;
	}
}
