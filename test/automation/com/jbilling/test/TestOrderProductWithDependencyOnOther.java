package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestOrderProductWithDependencyOnOther extends BrowserApp {

    @Test(description = "Test Case 15.2 :  Verify that Products with dependencies on other products can be ordered.")
    public void testOrderProductWithDependencyOnOther () throws IOException {

        setTestRailsId("10909926");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.2_CHILD_CUSTOMER_NAME"));
        newCustomersPage.createOrderProductHavingDependency("createOrderSecond", "co");
        ordersPage.verifyUIComponent();
    }
}
