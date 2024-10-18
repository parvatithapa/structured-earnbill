package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestAgentsLinkedToCustomer extends BrowserApp {

    @Test(description = "Test Case 16.1 : Verify that Agents can be made and linked to a customer")
    public void testAgentsLinkedToCustomer () throws IOException {

        setTestRailsId("10909927");

        navPage.navigateToConfigurationPage();
        confPage.setConfigurationPreference("setPreferenceValue", "pc");
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String accountName = confPage.createAccountType("addAccountType", "aat");

        confPage.verifyUIComponent();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.PaymentMethod);
        confPage.configurePaymentMethod("configurePaymentMethod", "pm");

        String methodName = confPage.addPaymentMethodDetails("debitCardPaymentType", "pt");

        msgsPage.verifyDisplayedMessageText("Payment Method Type", "created successfully", TextComparators.contains);

        String paymentMethod = pr.readTestData("TC_2.1.1_METHOD_NAME_ONE");
        confPage.validatePeriodsSavedTestData(paymentMethod);
        confPage.verifyUIComponent();

        navPage.navigateToAgentsPage();
        String agent = agentsPage.addAgent("addAgent", "aa");

        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.addCustomer(accountName, methodName, "new_customer", "nc");

        newCustomersPage.verifyUIComponent();
    }
}
