package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyDeleteAITType extends BrowserApp {

    @Test(description = "TC 347 : Verify that user is not able to delete used AIT metafields.", groups = { "globalRegressionPack" })
    public void tc_0347_deleteAIT () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.PaymentMethod);
        confPage.addPaymentMethod("debitCardPaymentType", "pt");
        String accountTypeName = confPage.addAccountType("SingleaddAccountInformationType", "aait");
        confPage.validateAccountTypeSavedTestData(accountTypeName);
        String accountName = confPage.addAITMetaFieldToAccountType("addAccountInformationType", "aait", accountTypeName);
        confPage.validateAccountInformationTypeSavedTestData(accountName);
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.AccountType);
        confPage.clickRecentlyCreatedAccountType();
        confPage.clickOnMetafieldID();
        confPage.clickEditAIT();
        confPage.clickUserNotification(true);
        confPage.popupYesAit();
        confPage.clickSaveChangesButton();
        confPage.clickDeleteAit();
        confPage.clickPopupYes();
        confPage.verifyErrorMessage();
    }
}
