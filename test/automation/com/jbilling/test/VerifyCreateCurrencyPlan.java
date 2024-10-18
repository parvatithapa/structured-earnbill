package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateCurrencyPlan extends BrowserApp {

    @Test(description = "TC_360 Verify, correct currency in UI is displayed while creating plan with 'Euro Currency'", groups = { "globalRegressionPack" })
    public void tc_0360_createCurrencyPlan () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Currencies);
        confPage.activeCurrency();

        navPage.navigateToProductsPage();
        String category = productsPage.addCategory("productCategory", "pcat");
        String product = productsPage.addProduct(GlobalEnumsPage.AddProductField.FLATPRICE, "addProductFresh", "prod");
        navPage.navigateToPlanPage();
        String PlanDesc = plansPage.addPlanWithDifferentCurrency(category, product, "addplanwithdiffer", "currplan");
        propReader.updatePropertyInFile("TC_360_planDescription", PlanDesc, "testData");
        msgsPage.verifyDisplayedMessageText("Saved new plan ", "successfully", TextComparators.contains);
    }
}
