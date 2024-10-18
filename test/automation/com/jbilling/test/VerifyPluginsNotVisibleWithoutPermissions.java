package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyPluginsNotVisibleWithoutPermissions extends BrowserApp {

    @Test(description = "TC 03 : Verify, Plug-ins are not visible for users without permission")
    public void tc_0003_editViewPluginPermissionForUser () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Users);
        confPage.removeUserPluginPermission(getUserName(), "pluginPermissions", "pp");

        navPage.logoutApplication();
        loginPage.login(baseUrl, credentials);

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.verifyDeniedPluginPermissionMessage("pluginPermissions", "pp");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Users);
        confPage.restoreUserPluginPermission(getUserName(), "pluginPermissions", "pp");
    }

    private String getUserName () throws IOException {
        String environment = pr.readConfig("EnvironmentUnderTest");
        return pr.readConfig(environment + "_Username");
    }
}
