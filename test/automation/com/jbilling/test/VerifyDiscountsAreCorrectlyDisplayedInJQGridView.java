package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyDiscountsAreCorrectlyDisplayedInJQGridView extends BrowserApp {

    @Test(description = "TC 124 : Verify that discounts are correctly displayed in JQGrid view.", groups = { "globalRegressionPack" })
    public void tc_0124_displayInJQGridView () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
    }
}
