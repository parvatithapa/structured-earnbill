package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestUserCanPayInvoiceOnGeneratedInvoice extends BrowserApp {

    // N.B. Depends on TestReportForInvoice.testReportForInvoice

    @Test(description = "Test Case 13.1 : Verify user can pay invoice on a billing process generated invoice")
    public void testUserCanPayInvoiceOnGeneratedInvoice () throws IOException {

        setTestRailsId("10909923");

        navPage.navigateToInvoicesPage();
        String customerName = pr.readTestData("TC_14.1_CUSTOMER_NAME");
        invoicePage.clickOnGeneratedInvoice(customerName);
        invoicePage.clickPayInvoice();
        paymentsPage.makePayment("payInvoice", "aa", pr.readTestData("creditCardPaymentMethod"));

        ordersPage.verifyUIComponent();
    }
}
