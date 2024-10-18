package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestLoginIntoCompany extends BrowserApp {

    @Test(description = "Test Case 1.2:  Verify that users are able to login into the JBilling System using valid credential.")
    public void testLoginIntoCompany () {

        setTestRailsId("11047237");

        navPage.logoutApplication();
        loginPage.login(baseUrl, credentials);
    }
}
