package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyMandatoryFieldMessageForAccountType extends BrowserApp {

    @Test(description = "TC 11.2 : Verify User able to get mandatory field validation message")
    public void tc_0011_2_verifyMandatoryFieldMessageForAccountType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        confPage.verifyMandatoryFieldMessages("mandatoryFieldAccountType", "mfat");
    }
}
