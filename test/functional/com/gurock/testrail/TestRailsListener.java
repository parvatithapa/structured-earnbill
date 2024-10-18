package com.gurock.testrail;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

//import com.gurock.testrails.APIClient;
//import com.jbilling.framework.globals.GlobalController;
//import com.jbilling.framework.globals.Logger;
//import com.jbilling.framework.utilities.fileutils.FileUtilities;
//import com.jbilling.framework.utilities.textutilities.TextUtilities;
//import com.jbilling.framework.utilities.xmlutils.PropertiesReader;

public class TestRailsListener extends TestListenerAdapter {
	// Initialize private logger object
//	private static Logger logger = new Logger().getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
//	PropertiesReader pr = new PropertiesReader();

	private APIClient client;
	private boolean _postResultsToTestRail = false;
	private String _resultsFileName = "";
	private final String _csvSeparator = "|";

	public TestRailsListener() throws Exception {
//		this.client = new APIClient(this.pr.readConfig("UrlTestRails"));
//		this.client.setUser(this.pr.readConfig("UserNameTestRails"));
//		this.client.setPassword(this.pr.readConfig("PasswordTestRails"));
//		this.client.setTestRunId(this.pr.readConfig("TestRunId"));
//		this._postResultsToTestRails = Boolean.valueOf(this.pr.readConfig("RecordTestResultsToTestRails"));
//
//		this._resultsFileName = "./results/results_" + System.currentTimeMillis() + ".csv";
//		FileUtilities.FileWrite(this._resultsFileName, "Test Case Id, Test Case Method Name, Test Case Objective, RESULT, Error Message");
	}

	Map<String, String> tests = new HashMap<>();
	Map<String, String> section = new HashMap<>();
	Map<String, String> test = new HashMap<>();
	String returnedSuite;
	String returnedSection;
	String returnedParent;
	String returnedCase;

	@Override
	public void onStart(ITestContext testContext) {
		super.onStart(testContext);


//		Annotation anns = testContext.getName().

			//	section = fetchSection();
	}


	@Override
	public void onTestStart(ITestResult result) {

		//test = fetchTest();
	}

	@Override
	public void onTestFailure(ITestResult result) {

		//checkUpdateTestCase(result);

//		String tcid = (String) result.getAttribute("tcid");
//		TestRailsListener.logger.info(result.getMethod().getMethodName() + " Test method failed having id " + tcid);
//
//		HashMap<String, Object> data = new HashMap<String, Object>();
//		// 5 is the id for Failed state in TestRails
//		data.put("status_id", new Integer(5));
//		data.put("comment", "FAILED due to " + result.getThrowable().getMessage());
//		try {
//			if (this._postResultsToTestRails) {
//				this.client.sendPost("add_result_for_case/" + this.client.getTestRunId() + "/" + tcid, data);
//			}
//			String errorMsg = result.getThrowable().getMessage();
//			errorMsg = TextUtilities.substring(errorMsg, 0, TextUtilities.indexOf(errorMsg, ","));
//			errorMsg = errorMsg.replaceAll("\r\n", "");
//			String resultMsg = "\n" + tcid + this._csvSeparator + result.getMethod().getMethodName() + this._csvSeparator
//					+ result.getMethod().getDescription() + this._csvSeparator + "FAIL" + this._csvSeparator + errorMsg;
//			FileUtilities.FileAppend(this._resultsFileName, resultMsg);
//
//			// Capture Screenshot on failure
//			GlobalController.brw.takeScreenShot(tcid + "_" + result.getMethod().getMethodName());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {

		//checkUpdateTestCase(result);
//		String tcid = (String) result.getAttribute("tcid");
//		TestRailsListener.logger.info(result.getTestName() + " Test method SKIPPED having id " + tcid);
//
//		HashMap<String, Object> data = new HashMap<String, Object>();
//		// 2 is the id for Blocked state in TestRails
//		data.put("status_id", new Integer(2));
//		data.put("comment", "Test blocked due to its dependant test case failure");
//		try {
//			if (this._postResultsToTestRails) {
//				this.client.sendPost("add_result_for_case/" + this.client.getTestRunId() + "/" + tcid, data);
//			}
//			String errorMsg = result.getThrowable().getMessage();
//			errorMsg = TextUtilities.substring(errorMsg, 0, TextUtilities.indexOf(errorMsg, ","));
//			String resultMsg = "\n" + tcid + this._csvSeparator + result.getMethod().getMethodName() + this._csvSeparator
//					+ result.getMethod().getDescription() + this._csvSeparator + "SKIPPED" + this._csvSeparator + errorMsg;
//			FileUtilities.FileAppend(this._resultsFileName, resultMsg);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {

		//checkUpdateTestCase(result);
//		String tcid = (String) result.getAttribute("tcid");
//		TestRailsListener.logger.info(result.getTestName() + " Test method success\n");
//
//		HashMap<String, Object> data = new HashMap<String, Object>();
//		// 1 is the id for Failed state in TestRails
//		data.put("status_id", new Integer(1));
//		data.put("comment", "Test executed successfully!");
//		try {
//			if (this._postResultsToTestRails) {
//				this.client.sendPost("add_result_for_case/" + this.client.getTestRunId() + "/" + tcid, data);
//			}
//			String resultMsg = "\n" + tcid + this._csvSeparator + result.getMethod().getMethodName() + this._csvSeparator
//					+ result.getMethod().getDescription() + this._csvSeparator + "PASS";
//			FileUtilities.FileAppend(this._resultsFileName, resultMsg);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

//	protected String apiToAddTestResultForCase(String tcid) {
//		String api = "add_result_for_case/" + this.client.getTestRunId() + "/" + tcid;
//
//		return api;
//	}


	/*private void checkUpdateTestCase(ITestResult result) throws IOException, APIException {
		Annotation anns = result.getTestClass().getRealClass().getAnnotation(TestRailClass.class);
		Annotation anes = result.getMethod().getRealClass().getAnnotation(TestRailCase.class);

		APIClient client = new APIClient("https://appdirect.testrail.com/");
		client.setUser("branko.stevanovic@jbilling.com");
		client.setPassword("SOVVcmNnU0tau.nkyzpn-FpSOS3E.rwCSopMwA2Eg");


		if (null != anns) {
			String suite = ((TestRailClass) anns).suite();
			String parent = ((TestRailClass) anns).parent();
			String section = ((TestRailClass) anns).section();


			TestRailUpload a = new TestRailUpload();
			returnedSuite = a.getSuite(client, suite);
			System.out.println("\n");
			returnedParent = a.getParent(client, parent);
			System.out.println("\n");
			returnedSection = a.getSection(client, section);
			System.out.println("\n");


			if (null != anes) {

				String title = ((TestRailCase) anes).title();
				String steps = ((TestRailCase) anes).steps();
				String refs = ((TestRailCase) anes).refs();


				String methodName = result.getMethod().getMethodName();
				String testName = result.getTestClass().getTestName();

//				System.out.println("suite: " + suite);
//				System.out.println("parent: " + parent);
//				System.out.println("section: " + section);
//				System.out.println("methodName: " + methodName);
//				System.out.println("testName: " + testName);

				String testKey = testName + "." + methodName;

				returnedCase = a.getCase(client, title);
				System.out.println("\n");


				if (!test.containsKey(testKey)) {
					//create


				} else {
					String testId = test.get(testKey);
					//Test test = fetch(testId);


					//update
				}
			} //else TestRailsListener.logger.info(result.getTestName() + " TestRail case didn't update, missing details.\n");
		}

	}*/


	private Map<String, String> fetchTest() {


		//Map suites = (JSONObject) client.sendGet("get_suites/12");

		return test;
	}

//	protected String apiToAddTestRunForCases(String projectId,
//											 ArrayList<String> tcids) {
//		//String api = "add_result_for_case/" + this.client.getTestRunId() + "/" + tcid;
//
//		return api;
//	}

//	private Map<String, String> fetchSuite() {
//
//		APIClient client = new APIClient("https://appdirect.testrail.com/");
//		client.setUser("branko.stevanovic@jbilling.com");
//		client.setPassword("SOVVcmNnU0tau.nkyzpn-FpSOS3E.rwCSopMwA2Eg");
//
//
//	//	JSONObject d = (JSONObject) client.sendGet("get_suite/9575");
//
//	}

}
