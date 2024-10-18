package com.jbilling.framework.globals;

//TODO: Move consts to configurable xml / properties file
public class GlobalConsts {

	public static final String PropertiesFileName = "config.properties";

	public static final String TestDataPropertiesFileName = "testData.properties";

	public static final String DirectoryPathTestData = "./test/automation/resources/testdata/";

	public static long         IMPLICIT_TIME_LIMIT = 10l;

	public static final String TestEnvironment = "develop";

	public static final String getProjectDir() {
		return System.getProperty("user.dir");
	}

	public static final String getScreenShotsFolderPath() {
		return GlobalConsts.getProjectDir() + "/target/test-results/screenshots/";
	}
}
