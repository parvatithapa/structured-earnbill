package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyUnableToDeleteFUPThatUsed extends BrowserApp {

    @Test(description = "TC 117 : Verify user is unable to delete an FUP that is in use", groups = { "globalRegressionPack" })
    public void tc_0117_userUnableToDeleteFUPThatUsed () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.FreeUsagePools);
        confPage.selectUsagePool(pr.readTestData("TC_113,CATEGORY_NAME"));
        confPage.valdationMessageDisplay("cannot be deleted, it is in use");
    }
}
