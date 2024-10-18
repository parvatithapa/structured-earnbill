package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifySearchingSortingForFreeUsagePoolAfterSwitchingConfiguration extends BrowserApp {

    @Test(description = "TC 114 : Verify searching and sorting works as defined for 'Free Usage Pool' after switching configuration 'On' from 'Off'", groups = { "globalRegressionPack" })
    public void tc_0114_configureFreeUsagePoolOnFromOff () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.setConfigurationPreference("addPreferencesvalue1", "ap");
        msgsPage.verifyDisplayedMessageText("Preference 63", "updated successfully", TextComparators.contains);
        confPage.selectConfiguration(PageConfigurationItems.FreeUsagePools);
        navPage.navigateToConfigurationPage();
        confPage.setConfigurationPreferenceJQGrid("addPreferencesvalue0", "ap");
        msgsPage.verifyDisplayedMessageText("Preference 63", "updated successfully", TextComparators.contains);
    }
}
