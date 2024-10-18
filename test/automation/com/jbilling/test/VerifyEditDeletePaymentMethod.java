package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyEditDeletePaymentMethod extends BrowserApp {

    @Test(description = "TC 10 : Verify that user can edit and delete the created payment method")
    public void tc_0010_editDeletePaymentMethodForCheque () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        confPage.addEditDeletePaymentMethod("TC10_AddEditDeletePaymentMethod", "aedpm");
        msgsPage.verifyDisplayedMessageText("Deleted Payment Method Type", "successfully", TextComparators.contains);
    }
}
