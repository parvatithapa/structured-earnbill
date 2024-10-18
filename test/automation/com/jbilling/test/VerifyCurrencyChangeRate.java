package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCurrencyChangeRate extends BrowserApp {

    private String defaultCurrency;

    @Test(description = "TC_363 Verify,user is able to change the default currency", groups = { "globalRegressionPack" }, priority = 0)
    public void tc_0363_verifyChangeDefaultCurrency () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Currencies);
        defaultCurrency = confPage.defaultCurrencySetup("DefaultCurrency", "default_curr_rate");
        msgsPage.verifyDisplayedMessageText("Currencies updated successfully.", "successfully",
                TextComparators.contains);
    }

    @Test(description = "TC_364 Verify, changed default currency is displayed on 'Currency' field at Customer/ Agents creation page", groups = { "globalRegressionPack" }, priority = 1)
    public void tc_0364_verifyAgentCustomerCurrency () throws IOException {

        setTestRailsId("");

        navPage.navigateToCustomersPage();
        newCustomersPage.verifyCurrencyPresentsInList(defaultCurrency);
    }
}
