package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyConfigurePluginsForProvisioning extends BrowserApp {

    @Test(description = "TC 44: Verify that user is able to configure Plugins for provisioning", groups = { "globalRegressionPack" })
    public void tc_0044_configurePluginProvisionForUser () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        productsPage.addPlugin("addPluginOPT", "apOPT");
        productsPage.addPluginWithProvisionID("addPluginOPTL", "apOPTL");
        productsPage.addPluginInsidePlugin("addPluginAPT", "apAPT");
        productsPage.addPluginInsidePlugin("addPluginPPT", "apPPT");
        productsPage.addPluginInsidePlugin("addPluginPCT", "apPCT");
    }
}
