package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyMetaFieldSelectionNotAvailableWhileAddingNewMetaField extends BrowserApp {

    @Test(description = "TC 344 : Meta field type select is not available while adding the new meta field in expected Account type")
    public void tc_0344_validateMetaFieldTypeSelection () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.validateMetaFieldSelectionForOrder();
        navPage.navigateToConfigurationPage();
        confPage.validateMetaFieldSelectionForCustomer();
        navPage.navigateToConfigurationPage();
        confPage.validateMetaFieldSelectionForPayment();
    }
}
