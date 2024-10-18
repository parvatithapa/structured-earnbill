package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestConfigureCollectionPlugins extends BrowserApp {

    @Test(description = "Test Case 2.6 : Verify ability to configure Collections Plugins")
    public void testConfigureCollectionPlugins () throws IOException {

        setTestRailsId("11047247");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Users);
        String userName = pr.readTestData("TC_1.1_CREDENTIALS_USERNAME");
        confPage.configurePluginPermissions(userName, "plugin", "pid");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.selectPluginCategory("selectPluginCategory", "pc");
        confPage.addNewPluginInCategory("addPlugin", "ap");
        msgsPage.verifyDisplayedMessageText("The new plug-in with id", "has been saved.", TextComparators.contains);
        confPage.verifyUIComponent();
    }
}
