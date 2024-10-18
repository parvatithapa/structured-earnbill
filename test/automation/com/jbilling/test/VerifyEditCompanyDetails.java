package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyEditCompanyDetails extends BrowserApp {

    @Test(description = "TC 02: Verify that users can Edit Company details")
    public void tc_0002_editCompanyDetails () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Company);
        confPage.editCompanyDetails("TC02_EditCompanyDetails", "cd");
        msgsPage.verifyDisplayedMessageText("Successfully saved", "Company information.", TextComparators.contains);
    }
}
