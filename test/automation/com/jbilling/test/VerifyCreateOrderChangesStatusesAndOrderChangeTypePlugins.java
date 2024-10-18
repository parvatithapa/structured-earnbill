package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateOrderChangesStatusesAndOrderChangeTypePlugins extends BrowserApp {

    @Test(description = "TC103 : Verify User Able To Create Order Changes Statuses And Order Change Type Plugins", groups = { "globalRegressionPack" })
    public void tc_0103_createOrderChangesStatusesAndOrderChangeTypePlugins () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.OrderChangeStatuses);
        confPage.setNumberOfRowsToTwo();
        msgsPage.verifyDisplayedMessageText("Order Change Statuses updated", "", TextComparators.contains);
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.clickOnEventListner();
        confPage.verifyAddPluginPageHeader();
        confPage.enterTestDataInOnPlugnin("OrderPluginPageInfo", "oi");
    }
}
