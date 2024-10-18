package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestConfigurePaymentMethod extends BrowserApp {

    @Test(description = "Test Case 2.3 : Verify ability to configure a Payment Method Configure 'Credit Card' payment method for Account Type: Direct Customer")
    public void testConfigurePaymentMethod () throws IOException {

        setTestRailsId("11047244");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.PaymentMethod);
        confPage.configurePaymentMethod("configurePaymentMethod", "pm");
        String paymentMethodName = confPage.addPaymentMethodDetails("creditCardPaymentMethod", "pt");
        propReader.updatePropertyInFile("creditCardPaymentMethod", paymentMethodName, "testData");
        msgsPage.verifyDisplayedMessageText("Payment Method Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(paymentMethodName);
        confPage.verifyUIComponent();
    }
}
