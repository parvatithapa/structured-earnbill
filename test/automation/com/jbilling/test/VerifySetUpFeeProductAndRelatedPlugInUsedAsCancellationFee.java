package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifySetUpFeeProductAndRelatedPlugInUsedAsCancellationFee extends BrowserApp {

    @Test(description = "TC 53 : Verify User Is Able To Set Up Fee Product And Related Plug In To Be Used As CancellationFee", groups = { "globalRegressionPack" })
    public void tc_0053_setUpFeeProductAndRelatedPlugInToBeUsedAsCancellationFee () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String category = productsPage.addCategory("productCategory", "pcat");
        confPage.validateCategoriesSavedTestData(category);
        productsPage.addProducts(AddProductField.FLAT, "addProductOneToAddDependencies", "pou");
        productsPage.Createanotherproduct(AddProductField.FLAT, "addProductTwoToAddDependencies", "pok");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.verifyPluginscategoriesPageHeader();
        confPage.clickOnEventListner();
        confPage.verifyAddPluginPageHeader();
        confPage.enterTestDataInOnPlugnin("OrderPluginPageInfo", "oi");
        confPage.selectConfiguration(PageConfigurationItems.OrderChangeStatuses);
        confPage.checkboxOrderChangeStatuses();
    }
}
