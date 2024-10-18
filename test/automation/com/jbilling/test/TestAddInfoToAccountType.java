package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestAddInfoToAccountType extends BrowserApp {

    @Test(description = "Test Case 2.2 : Verify ability to successfully add an Information Type to an Account Type")
    public void testAddInfoToAccountType () throws IOException {

        setTestRailsId("11047243");

        navPage.navigateToConfigurationPage();

        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.AccountType);
        confPage.selectAccountTypeName(pr.readTestData("TC_2.1_ACCOUNT_NAME_ONE"));

        String infoTypeName = confPage.addNewInformationToSelectedAccountType("AddInfoToAccountType", "aitac");

        msgsPage.verifyDisplayedMessageText("Account Information Type", "created successfully",
                TextComparators.contains);

        confPage.validatePeriodsSavedTestData(infoTypeName);
        confPage.verifyUIComponent();
    }
}
