package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyStatusOfPreExistingAndNewOrder extends BrowserApp {

    @Test(description = "107: Verify pre-existing Order Statuses are available and new order statuses can be customized.", groups = { "globalRegressionPack" })
    public void tc_0107_verifyStatusOfPreExistingandNewOrder () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.VerifyOrderStatus("VerifyOrderStatuses", "OS");
        msgsPage.isErrorMessageAppeared();
    }
}
