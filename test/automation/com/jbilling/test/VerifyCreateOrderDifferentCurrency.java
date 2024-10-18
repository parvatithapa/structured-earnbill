    package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateOrderDifferentCurrency extends BrowserApp {

    @Test(description = "TC_361  :  Verify correct currency conversion is made while creating order with plan.")
    public void tc_0361_createOrder () throws IOException {

        setTestRailsId("10909926");

        String customername = pr.readTestData("TC_92_CUSTOMER_NAME");
        String planname = pr.readTestData("TC_360_planDescription");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(customername);
        newCustomersPage.createOrderWithDifferentCurrency(planname, "OrderWithDifferentPeriodsCurr", "curr4");
        msgsPage.verifyDisplayedMessageText("Created new order", "successfully", TextComparators.contains);
    }
}
