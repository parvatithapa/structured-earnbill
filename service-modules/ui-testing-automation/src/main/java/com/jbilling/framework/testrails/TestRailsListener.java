package com.jbilling.framework.testrails;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import com.gurock.testrails.APIClient;
import com.gurock.testrails.APIException;
import com.jbilling.framework.globals.GlobalController;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import com.jbilling.framework.utilities.xmlutils.ConfigPropertiesReader;

public class TestRailsListener extends TestListenerAdapter {

    protected final Log logger = LogFactory.getLog(getClass());

	ConfigPropertiesReader pr = new ConfigPropertiesReader();

	private final APIClient client;
	private boolean _postResultsToTestRails = false;
	private String _resultsFileName = "";
	private final String _csvSeparator = "|";

	public TestRailsListener() throws IOException {
		this.client = new APIClient(this.pr.readConfig("UrlTestRails"));
		this.client.setUser(this.pr.readConfig("UserNameTestRails"));
		this.client.setPassword(this.pr.readConfig("PasswordTestRails"));
		// this.client.setTestRunId(this.pr.readConfig("TestRunId"));
		this.client.setTestProjectId(this.pr.readConfig("ProjectId"));
		this.client.setTestSuiteId(this.pr.readConfig("TestSuiteId"));

		this._postResultsToTestRails = Boolean.valueOf(this.pr.readConfig("RecordTestResultsToTestRails"));

		this._resultsFileName = "./results/results_" + System.currentTimeMillis() + ".csv";
		// FileUtilities.FileWrite(this._resultsFileName,
		// "Test Case Id, Test Case Method Name, Test Case Objective, RESULT, Error Message");

	}

	public void getProjects() throws MalformedURLException, IOException, APIException {
		System.out.println(this.client.sendGet("get_projects/"));
	}

	@Override
	public void onTestFailure(ITestResult result) {
		String tcid = (String) result.getAttribute("tcid");
		logger.info(result.getMethod().getMethodName() + " Test method failed having id " + tcid);

		HashMap<String, Object> data = new HashMap<String, Object>();
		// 5 is the id for Failed state in TestRails
		data.put("status_id", new Integer(5));
		data.put("comment", "FAILED due to " + result.getThrowable().getMessage());
		try {
			if (this._postResultsToTestRails) {
				this.client.sendPost("add_result_for_case/" + this.client.getTestRunId() + "/" + tcid, data);
			}
			String errorMsg = result.getThrowable().getMessage();
			errorMsg = TextUtilities.substring(errorMsg, 0, TextUtilities.indexOf(errorMsg, ","));
			errorMsg = errorMsg.replaceAll("\r\n", "");
			String resultMsg = "\n" + tcid + this._csvSeparator + result.getMethod().getMethodName() + this._csvSeparator
					+ result.getMethod().getDescription() + this._csvSeparator + "FAIL" + this._csvSeparator + errorMsg;
			//FileUtilities.FileAppend(this._resultsFileName, resultMsg);

			// Capture Screenshot on failure
			GlobalController.brw.takeScreenShot(tcid + "_" + result.getMethod().getMethodName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		String tcid = (String) result.getAttribute("tcid");
		logger.info(result.getTestName() + " Test method SKIPPED having id " + tcid);

		HashMap<String, Object> data = new HashMap<String, Object>();
		// 2 is the id for Blocked state in TestRails
		data.put("status_id", new Integer(2));
		data.put("comment", "Test blocked due to its dependant test case failure");
		try {
			if (this._postResultsToTestRails) {
				this.client.sendPost("add_result_for_case/" + this.client.getTestRunId() + "/" + tcid, data);
			}
			String errorMsg = result.getThrowable().getMessage();
			errorMsg = TextUtilities.substring(errorMsg, 0, TextUtilities.indexOf(errorMsg, ","));
			String resultMsg = "\n" + tcid + this._csvSeparator + result.getMethod().getMethodName() + this._csvSeparator
					+ result.getMethod().getDescription() + this._csvSeparator + "SKIPPED" + this._csvSeparator + errorMsg;
			//FileUtilities.FileAppend(this._resultsFileName, resultMsg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		String tcid = (String) result.getAttribute("tcid");
		logger.info(result.getTestName() + " Test method success");

		HashMap<String, Object> data = new HashMap<String, Object>();
		// 1 is the id for Failed state in TestRails
		data.put("status_id", new Integer(1));
		data.put("comment", "Test executed successfully!");
		try {
			if (this._postResultsToTestRails) {
				this.client.sendPost("add_result_for_case/" + this.client.getTestRunId() + "/" + tcid, data);
			}
			String resultMsg = "\n" + tcid + this._csvSeparator + result.getMethod().getMethodName() + this._csvSeparator
					+ result.getMethod().getDescription() + this._csvSeparator + "PASS";
			//FileUtilities.FileAppend(this._resultsFileName, resultMsg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected String apiToAddTestResultForCase(String tcid) {
		String api = "add_result_for_case/" + this.client.getTestRunId() + "/" + tcid;

		return api;
	}
}
