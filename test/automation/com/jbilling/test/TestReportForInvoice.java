package com.jbilling.test;

import java.awt.AWTException;
import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestReportForInvoice extends BrowserApp {

    @Test(description = "Test Case 14.1 : Verify correct report is displayed.")
    public void testReportForInvoice () throws IOException, InterruptedException, AWTException {

        setTestRailsId("10909924");

        navPage.navigateToCustomersPage();

        String customerName = newCustomersPage.addCustomerWithMakePayment("S_TC14.1_ReportForInvoices", "rfi",
                pr.readTestData("TC_3.5_ACCOUNT_NAME_ONE"));
        propReader.updatePropertyInFile("TC_14.1_CUSTOMER_NAME", customerName, "testData");

        newCustomersPage.selectCustomer(customerName);
        newCustomersPage.generateInvoice("Customer A", "ca");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(customerName);
        newCustomersPage.clickMakePayment();
        paymentsPage.makePayment("payInvoice", "aa", pr.readTestData("creditCardPaymentMethod"));
        navPage.navigateToReportsPage();
        reportsPage.getReportsView("selectReportType", "sra");
        ordersPage.verifyUIComponent();
    }
}
