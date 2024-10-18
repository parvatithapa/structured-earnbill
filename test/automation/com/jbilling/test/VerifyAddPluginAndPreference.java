package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyAddPluginAndPreference extends BrowserApp {

    @Test(description = "TC 271: Verify user is able to set up plug-in for agents and commission")
    public void tc_0271_addPluginAndPreference () throws Exception {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.addPlugin("TC_271_addPluginAndPreference", "app");
        confPage.selectConfiguration(PageConfigurationItems.All);
        confPage.setConfigurationPreferenceForAnyPreference("TC_271_addPluginAndPreference", "app");
        msgsPage.verifyDisplayedMessageText("Preference 61", "updated successfully", TextComparators.contains);
    }
}
