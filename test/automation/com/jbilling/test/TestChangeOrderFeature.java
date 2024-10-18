package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestChangeOrderFeature extends BrowserApp {

    @Test(description = "Test Case 7.1 : Verify Order Changes feature works correctly")
    public void testChangeOrderFeature () throws IOException {

        setTestRailsId("10909910");

        navPage.navigateToCustomersPage();
        newCustomersPage.addCustomer(pr.readTestData("TC_2.1_ACCOUNT_NAME_ONE"),
                pr.readTestData("TC_2.1.1_METHOD_NAME_ONE"), "new_customer", "nc");

        newCustomersPage.createOrderForCustomer("Customer A", "ca");

        ordersPage.verifyUIComponent();
    }
}
