package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyAddAITMetaFieldToAccountType extends BrowserApp {

    @Test(description = "TC 338 : Verify User Is Able To add AIT meta-field to the account type")
    public void tc_0338_addAITMetafieldToAccountType () {

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        String accountTypeName = confPage.addAccountType("addAccountInformationType", "aait");
        confPage.validateAccountTypeSavedTestData(accountTypeName);
        String accountName = confPage
                .addAITMetaFieldToAccountType("addAccountInformationType", "aait", accountTypeName);
        confPage.validateAccountInformationTypeSavedTestData(accountName);
    }
}
