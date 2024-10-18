package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyValidityRulesDropDownIsAvailableWhileCreatingMetaFieldToAIT extends BrowserApp {

    @Test(description = "TC 340 : Verify that Validation Rule drop down field is available inplace of Rule type while creating meta-fields to the AIT")
    public void tc_0340_addAITMetafieldToAccountType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        String accountTypeName = confPage.addAccountType("addAccountInformationType", "aait");
        confPage.validateAccountTypeSavedTestData(accountTypeName);
        confPage.verifyVelidityRulesDropDown("addAccountInformationType", "aait", accountTypeName);
    }
}
