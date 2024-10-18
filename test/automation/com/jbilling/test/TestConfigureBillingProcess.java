package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestConfigureBillingProcess extends BrowserApp {

    @Test(description = "Test Case 2.7 : Test Data Preparation for Billing Process")
    public void testConfigureBillingProcess () {

        setTestRailsId("11047248");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.BillingProcess);
        confPage.addBillingProcess("BillingProcess", "cbp");
        msgsPage.verifyDisplayedMessageText("Billing configuration", "saved successfully", TextComparators.contains);
    }
}
