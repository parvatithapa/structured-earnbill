package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyDataPreparationForAllSections extends BrowserApp {

    @Test(description = "TC 04 : Data preparation for all the section")
    public void tc_0004_dataPreprationForAllSections () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Users);
        confPage.addUserApiAccess("pluginAndAgentPermissions", "pap");

        navPage.logoutApplication();
        loginPage.login(baseUrl, credentials);

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Roles);
        confPage.setRolePermission("pluginAndAgentPermissions", "pap");
        msgsPage.verifyDisplayedMessageText("Updated role", "successfully.", TextComparators.contains);
    }
}
