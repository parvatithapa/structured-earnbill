package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestAttachPaymentType extends BrowserApp {

    @Test(description = "Test Case 2.1.1 : Verify ability to attach a Payment Method to " + "an Account Type")
    public void testAttachPaymentType () throws IOException {

        setTestRailsId("11047242");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.PaymentMethod);
        confPage.configurePaymentMethod("configurePaymentMethod", "pm");

        String methodName = confPage.addPaymentMethodDetails("debitCardPaymentType", "pt");

        propReader.updatePropertyInFile("TC_2.1.1_METHOD_NAME_ONE", methodName, "testData");
        msgsPage.verifyDisplayedMessageText("Payment Method Type", "created successfully", TextComparators.contains);

        confPage.validatePeriodsSavedTestData(methodName);
        confPage.verifyUIComponent();
    }
}
