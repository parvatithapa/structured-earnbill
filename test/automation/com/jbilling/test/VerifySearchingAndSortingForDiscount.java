package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifySearchingAndSortingForDiscount extends BrowserApp {

    @Test(description = "TC 112: Verify searching and sorting works as defined for 'Discounts'.", groups = { "globalRegressionPack" })
    public void tc_0112_verifySearchingandSortingforDiscountTest () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.setConfigurationPreference("setJQLPreferenceValue", "pc");
        navPage.navigateToDiscountsPage();
        discountsPage.verifyDiscountTable();
        navPage.navigateToConfigurationPage();
        confPage.reUpdatePreference("reSetJQLPreferenceValue", "pc");
    }
}
