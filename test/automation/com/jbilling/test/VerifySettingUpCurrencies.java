package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifySettingUpCurrencies extends BrowserApp {

    @Test(description = "TC_348 Verify setting up currencies from Configuration/Currencies", groups = { "globalRegressionPack" }, priority = 0)
    public void tc_0348_currencySetup () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Currencies);
        confPage.activeCurrency();
        msgsPage.verifyDisplayedMessageText("Currencies updated successfully.", "successfully",
                TextComparators.contains);
    }

    @Test(description = "TC_357 Verify,Inactive currencies are unavailable at 'New Agent' page and creating an Agent with default currency (USD)", groups = { "globalRegressionPack" }, priority = 1)
    public void tc_0357_checkAgentCurrency () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Currencies);
        confPage.defaultCurrencySetup("setcurrency", "setcurr");
    }

    @Test(description = "TC_353 Verify user is not able to remove currencies which are in use", groups = { "globalRegressionPack" }, priority = 2)
    public void tc_0353_currencyCheckup () {

        setTestRailsId("");

        navPage.navigateToCustomersPage();
        newCustomersPage.addCustomerWithCurrency("addcuswithcurr", "cuswithcurr");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Currencies);
        confPage.deactivateCurrencies();
        msgsPage.isErrorMessageAppeared();
    }

    @Test(description = "TC_354 Verify removing currencies from Configuration/Currencies (not in use)", groups = { "globalRegressionPack" }, priority = 3)
    public void tc_0354_currencyChekupNotInUse () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Currencies);
        confPage.unusedCurrency();
        msgsPage.verifyDisplayedMessageText("Currencies updated successfully.", "successfully",
                TextComparators.contains);
    }
}
