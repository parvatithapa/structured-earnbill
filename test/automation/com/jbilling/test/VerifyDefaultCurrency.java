package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyDefaultCurrency extends BrowserApp {

    @Test(description = "TC_358 Verify that user is able to set exchange rate for currencies", groups = { "globalRegressionPack" })
    public void tc_0358_currencyCreation () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Currencies);
        confPage.defaultCurrencySetup("DefaultCurrency", "default_curr_rate");
        msgsPage.verifyDisplayedMessageText("Currencies updated successfully.", "successfully",
                TextComparators.contains);
    }
}
