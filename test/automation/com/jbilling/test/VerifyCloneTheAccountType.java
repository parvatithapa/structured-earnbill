package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCloneTheAccountType extends BrowserApp {

    @Test(description = "TC 58 : Verify that user is able to clone the account type.")
    public void tc_0058_verifyCloneTheAccountType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        navPage.navigateToConfigurationPage();
        confPage.setConfigurationPreference("setPreferenceValue", "pc");
        String accountTypeName = confPage.addAccountTypeWithInvoiceDesign("addAccountWithInvoiceDesign", "aaid");
        confPage.validateAccountTypeSavedTestData(accountTypeName);
        navPage.navigateToConfigurationPage();
        confPage.createCloneAccountType("cloneAccountType", "cat", accountTypeName);
    }
}
