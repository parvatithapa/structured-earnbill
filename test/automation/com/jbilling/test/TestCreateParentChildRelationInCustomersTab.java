package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateParentChildRelationInCustomersTab extends BrowserApp {

    @Test(description = "Test Case 6.2 : Verify user can create a Parent/Child relationship	within the Customer tab")
    public void testCreateParentChildRelationInCustomersTab () throws IOException {

        setTestRailsId("10909908");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.1_CUSTOMER_NAME"));

        String childCustomer = newCustomersPage.addChildCustomer(pr.readTestData("TC_2.1_ACCOUNT_NAME_ONE"),
                pr.readTestData("TC_2.1.1_METHOD_NAME_ONE"), "addSecondCustomer", "ac");
        propReader.updatePropertyInFile("TC_6.2_CHILD_CUSTOMER_NAME", childCustomer, "testData");
        msgsPage.verifyDisplayedMessageText("Saved new customer", "successfully", TextComparators.contains);
        newCustomersPage.validateUsersSavedTestData(childCustomer);
        newCustomersPage.verifyUIComponent();
    }
}
