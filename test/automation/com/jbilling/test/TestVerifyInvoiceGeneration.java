package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestVerifyInvoiceGeneration extends BrowserApp {

    @Test(description = "Test Case 10.1 : Generating an Invoice (Manually) ")
    public void testVerifyInvoiceGeneration () throws IOException {

        setTestRailsId("10909915");

        String customerName = appendRandomChars("Customer-tc_10_1-");
        String accountTypeName = pr.readTestData("TC_3.5_ACCOUNT_NAME_ONE");

        navPage.navigateToCustomersPage();
        newCustomersPage.addCustomerWithAccountType(customerName, accountTypeName);
        newCustomersPage.selectCustomer(customerName);
        newCustomersPage.createOrderForInvoice("Customer A", "ca");

        navPage.navigateToOrdersPage();
        filtersPage.filterOnLoginNameOrCustomerName(customerName);
        ordersPage.verifyInvoiceGenerationForChoosenCustomer(customerName);
    }
}
