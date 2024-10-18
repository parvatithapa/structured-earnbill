package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreatePluginData extends BrowserApp {

    @Test(description = "Test Case 285 : Data preparation for provisioning", groups = { "globalRegressionPack" })
    public void tc_0285_creatingPluginData() {

        setTestRailsId("11047241");

        this.navPage.navigateToConfigurationPage();
        this.confPage.selectConfiguration(PageConfigurationItems.Plugins);
        this.productsPage.addPlugin("addPluginOPT", "apOPT");
        this.msgsPage.verifyDisplayedMessageText("The new plug-in with", "has been saved", TextComparators.contains);
    }
}
