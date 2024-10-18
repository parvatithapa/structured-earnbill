package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.browserutils.BrowserApp;
import com.jbilling.test.ui.Credentials;

public class VerifyLoginUsingValidCredentials extends BrowserApp {

    @Test(description = "Test Case 1.1: Verify that users can login into the JBilling System using valid credential")
    public void tc_0001_1_loginWithInvalidCredentials () {

        setTestRailsId("");

        navPage.logoutApplication();
        loginPage.login(baseUrl, new Credentials.DefaultCredentials("admin", "invalid", "000"));

        msgsPage.assertTextInFirstErrorMessage("Sorry, we were not able to find a user", TextComparators.contains);
    }
}
