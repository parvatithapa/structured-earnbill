package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestAddAccountType extends BrowserApp {

    @Test(description = "Test Case 2.1 : Verify ability to successfully configure and edit Account Types "
            + "\"Direct Customer\" & \"Distributor Account\" >> Verify the ability to set Jasper"
            + " invoice design as default for invoice download")
    public void testAddAccountType () throws IOException {

        setTestRailsId("11047241");

        navPage.navigateToConfigurationPage();
        confPage.setConfigurationPreference("setPreferenceValue", "pc");
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String accountName = confPage.createAccountType("addAccountType", "aat");
        propReader.updatePropertyInFile("TC_2.1_ACCOUNT_NAME_ONE", accountName, "testData");

        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        accountName = confPage.createAccountType("addSecondAccountType", "aat");
        propReader.updatePropertyInFile("TC_2.1_ACCOUNT_NAME_TWO", accountName, "testData");

        accountName = confPage.editAccountTypeName("addAccountType", "aat");

        msgsPage.verifyDisplayedMessageText("Account Type", "updated successfully", TextComparators.contains);

        confPage.validatePeriodsSavedTestData(accountName);
        confPage.verifyUIComponent();
    }
}
