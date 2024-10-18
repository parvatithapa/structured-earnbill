package com.jbilling.framework.utilities.xmlutils;

import java.util.Map;
import com.jbilling.framework.globals.GlobalConsts;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class XmlDataReader {
	DomParser dp;

	public XmlDataReader(final String xmlFileName) {
        this.dp = new DomParser(GlobalConsts.DirectoryPathTestData, xmlFileName);
	}

	public String readData(String dataSetName, String keyName, String category) {
		Map<String, String> ln = this.readData(dataSetName, category);

		String value = "";
		for (Map.Entry<String, String> entry : ln.entrySet()) {
			String key = entry.getKey();
			if (TextUtilities.equalsIgnoreCase(key, keyName)) {
				value = entry.getValue();
				if (value.contains("{RANDOM}")) {
					value = value.replace("{RANDOM}", "") + TextUtilities.getRandomString(5);
				}
				if (value.contains("{RANDOMNUM}")) {
					value = value.replace("{RANDOMNUM}", "") + TextUtilities.getRandomNumber(5);
				}
				if (value.contains("{CURRENTDATE}")) {
					value = value.replace("{CURRENTDATE}", "") + DateTimeFormatter.ofPattern("MM/dd/yyyy").format(LocalDate.now());
				}
				if (value.contains("{NEXTYEAR}")) {
					value = value.replace("{NEXTYEAR}", "") + LocalDate.now().plusYears(1).getYear();
				}
			}
		}

		return value.trim();
	}

	private Map<String, String> readData(String dataSetName, String category) {
		Map<String, String> ln = this.dp.getTestDataSetNodesWithValues(GlobalConsts.TestEnvironment, dataSetName, category);
		return ln;
	}
}