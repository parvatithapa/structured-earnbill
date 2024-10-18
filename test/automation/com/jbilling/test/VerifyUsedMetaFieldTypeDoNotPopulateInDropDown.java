package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyUsedMetaFieldTypeDoNotPopulateInDropDown extends BrowserApp {

    @Test(description = "TC 339 : Verify that used metafield 'type' in an AIT, do not populate in the drop-down list")
    public void tc_0339_addAITMetafieldToAccountType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        String accountTypeName = confPage.addAccountType("addAccountInformationType", "aait");
        confPage.validateAccountTypeSavedTestData(accountTypeName);
        String accountName = confPage
                .addAITMetaFieldToAccountType("addAccountInformationType", "aait", accountTypeName);
        confPage.validateAccountInformationTypeSavedTestData(accountName);
        confPage.verifyUsedMetafieldTypeIsDisplayedInDD("addAccountInformationType", "aait", accountTypeName);
    }
}
