package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyAccountTypeNotification extends BrowserApp {

    @Test(description = "TC 346: Verify that when user check Use In Notification checkbox at AIT metafield page then it is automatically selected at account type", groups = { "globalRegressionPack" })
    public void tc_0346_addAITMetafieldToAccountType () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        String accountTypeName = confPage.addAccountType("SingleaddAccountInformationType", "aait");

        confPage.validateAccountTypeSavedTestData(accountTypeName);
        String accountName = confPage.addAITMetaFieldToAccountType("addAccountInformationType", "aait", accountTypeName);
        confPage.validateAccountInformationTypeSavedTestData(accountName);
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        confPage.clickRecentlyCreatedAccountType();
        confPage.clickOnMetafieldID();
        confPage.clickEditAIT();
        confPage.clickUserNotification(true);
        confPage.popupYesAit();
        confPage.clickSaveChangesButton();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        confPage.clickRecentlyCreatedAccountType();
        confPage.clickEditAccountTypeButton();
    }
}
