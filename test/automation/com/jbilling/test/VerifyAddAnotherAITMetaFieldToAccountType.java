package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyAddAnotherAITMetaFieldToAccountType extends BrowserApp {

    @Test(description = "TC 341 : Verify that user is able to add another AIT meta-field  to the account type ")
    public void tc_0341_addAnotherAITMetafieldToExistingAccountType () {

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
        confPage.clickOnAccountTypeLink();
        String accountName1 = confPage.addAITMetaFieldToAccountType("addAccountInformationType", "aait",
                accountTypeName);
        confPage.validateAccountInformationTypeSavedTestData(accountName1);
    }
}
