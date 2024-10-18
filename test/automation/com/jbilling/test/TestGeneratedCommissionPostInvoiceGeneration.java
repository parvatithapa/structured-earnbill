package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestGeneratedCommissionPostInvoiceGeneration extends BrowserApp {

    @Test(description = "Test Case 16.4 : Verify that correct commission is generated for the "
            + "Agent after order invoice is generated")
    public void testGeneratedCommissionPostInvoiceGeneration () throws IOException {

        setTestRailsId("10909930");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.2_CHILD_CUSTOMER_NAME"));
        newCustomersPage.generateInvoice("Customer A", "ca");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AgentCommissionProcess);
        confPage.addBillingProcess("BillingProcess", "cbp");
        confPage.clickRunCommmisionToBillingProcess();
        confPage.verifySavedCommision();
    }
}
