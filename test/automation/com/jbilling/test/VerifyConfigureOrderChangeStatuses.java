package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyConfigureOrderChangeStatuses extends BrowserApp {

    @Test(description = "TC 85 : Verify that user is able to Configure Order Change Statuses", groups = { "globalRegressionPack" })
    public void tc_0085_configureOrderChangeStatus () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.OrderChangeStatuses);
        confPage.enterDataStatus("DataStatusInEnglishBox", "dsieb");
        msgsPage.verifyDisplayedMessageText("Order Change Statuses updated", "Statuses updated",
                TextComparators.contains);
    }
}
