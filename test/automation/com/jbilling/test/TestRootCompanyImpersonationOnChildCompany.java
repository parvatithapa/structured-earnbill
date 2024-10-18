package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestRootCompanyImpersonationOnChildCompany extends BrowserApp {

    @Test(description = "Test Case 5.1: Verify that Root Company has ability to "
            + "impersonate Child Company and view all & only information assigned to Child Company.")
    public void testRootCompanyImpersonationOnChildCompany () {

        setTestRailsId("10909906");

        // TODO : why was it commented out
        // navPage.switchToChildCompany(runTimeVariables.get("TC_1.3_CHILD_COMPANY_COMPANYNAME"));

        navPage.navigateToProductsPage();
        // navPage.switchToParentCompany();

        navPage.navigateToCustomersPage();
        newCustomersPage.verifyUIComponent();
    }
}
