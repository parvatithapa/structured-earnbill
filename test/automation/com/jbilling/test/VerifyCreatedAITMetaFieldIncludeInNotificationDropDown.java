package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreatedAITMetaFieldIncludeInNotificationDropDown extends BrowserApp {

    @Test(description = "TC 342 :Verify that created AIT meta fields are displayed in 'Include in notifications' droplist after editing.")
    public void tc_0342_addAITMetafieldToAccountType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        String accountTypeName = confPage.addAccountType("addAccountInformationType", "aait");
        confPage.validateAccountTypeSavedTestData(accountTypeName);
        String accountName = confPage
                .addAITMetaFieldToAccountType("addAccountInformationType", "aait", accountTypeName);
        confPage.validateAccountInformationTypeSavedTestData(accountName);
        navPage.navigateToConfigurationPage();
    }
}
