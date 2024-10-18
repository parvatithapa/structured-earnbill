package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateOrderWithDifferentTypedandPeriods extends BrowserApp {

    @Test(description = "Test Case 11.1 :  Verify user is able to create orders belonging "
            + "to different periods/type to be processed in billing process later.")
    public void testCreateOrderWithDifferentTypedandPeriods () throws IOException {

        setTestRailsId("10909918");

        navPage.navigateToCustomersPage();
        newCustomersPage.addCustomer(pr.readTestData("TC_2.1_ACCOUNT_NAME_ONE"),
                pr.readTestData("TC_2.1.1_METHOD_NAME_ONE"), "new_customer", "nc");
        newCustomersPage.addCustomer(pr.readTestData("TC_2.1_ACCOUNT_NAME_ONE"),
                pr.readTestData("TC_2.1.1_METHOD_NAME_ONE"), "new_customer", "nc");
        navPage.navigateToProductsPage();
        productsPage.addCategory("productCategoryWithDifferentPeriods", "pcat");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.2_CHILD_CUSTOMER_NAME"));
        newCustomersPage.createOrderForCustomer("OrderWithDifferentPeriods", "co");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.2_CHILD_CUSTOMER_NAME"));
        newCustomersPage.createOrderForCustomer("OrderTwoWithDifferentPeriods", "co");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.2_CHILD_CUSTOMER_NAME"));
        newCustomersPage.createOrderForCustomer("OrderThreeWithDifferentPeriods", "co");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.2_CHILD_CUSTOMER_NAME"));
        newCustomersPage.createOrderForCustomer("OrderfourWithDifferentPeriods", "co");

        ordersPage.verifyUIComponent();
    }
}
