package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestConfigurePluginPreferencesForCommission extends BrowserApp {

    @Test(description = "Test Case 16.3 : Verify that user can configure the plug-in "
            + "and preference required for running a comission process")
    public void testConfigurePluginPreferencesForCommission () {

        setTestRailsId("10909929");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        productsPage.addPlugin("addPlugin", "ap");
        confPage.selectConfiguration(PageConfigurationItems.All);
        confPage.updatePreference("updatePreference", "up");
        productsPage.verifyUIComponent();
    }
}
