package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateAndEditCustomer extends BrowserApp {

    @Test(description = "Test Case 6.1 : Verify if user is able to create/Edit a new customer into the system.")
    public void testCreateAndEditCustomer () throws IOException {

        setTestRailsId("10909907");

        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.addCustomer(pr.readTestData("TC_2.1_ACCOUNT_NAME_ONE"),
                pr.readTestData("TC_2.1.1_METHOD_NAME_ONE"), "new_customer", "nc");
        propReader.updatePropertyInFile("TC_6.1_CUSTOMER_NAME", customerName, "testData");
        msgsPage.verifyDisplayedMessageText("Saved new customer", "successfully", TextComparators.contains);
        newCustomersPage.validateUsersSavedTestData(customerName);
        newCustomersPage.verifyUIComponent();
    }
}
