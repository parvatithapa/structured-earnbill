package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyOnlyOneAITMetaFieldPerAccountTypeCanBeEnabledAtTime extends BrowserApp {

    @Test(description = "TC 343 : Verify that only one AIT meta-field per account type can be enabled at a time")
    public void tc_0343_validateMetaFieldTypeSelection () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        String accountTypeName = confPage.addAccountType("addAccountInformationType", "aait");
        System.out.println("accountTypeName:"+accountTypeName);
        String paymentAddress1 = confPage.addAITMetaFieldToAccountType("addAccountInformationType", "aait",
                accountTypeName);
        System.out.println("paymentAddress1:"+paymentAddress1);
        navPage.navigateToConfigurationPage();
        confPage.clickOnAccountTypeLink();
        String paymentAddress2 = confPage.addAITMetaFieldToAccountType("addAccountInformationType", "aait",
                accountTypeName);
        System.out.println("paymentAddress2:"+paymentAddress2);
        navPage.navigateToConfigurationPage();
    }
}
