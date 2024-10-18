package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestAssignAccountToProduct extends BrowserApp {

    @Test(description = "Test Case 3.5 : Verify user can assign an Account Type price to a Product and edit it")
    public void testAssignAccountToProduct () throws IOException {

        setTestRailsId("10909904");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String accountName = confPage.createAccountType("addAccountType", "aat");
        propReader.updatePropertyInFile("TC_3.5_ACCOUNT_NAME_ONE", accountName, "testData");

        confPage.selectAccountName(accountName);
        confPage.clickAccountTypePrices();
        confPage.addAccountTypePriceToSelectedProduct("Flat 1 Priced Product", "addPrice", "ap");
        confPage.updateAccountTypePriceForProduct("editPrice", "ap");
    }
}
