package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateFUP extends BrowserApp {

    @Test(description = "TC 113: Verify user is able to create FUP.", groups = { "globalRegressionPack" })
    public void tc_0113_verifyCreateFUP () throws IOException {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String productCategory = productsPage
                .addCategoryNationalMobileCalls("productCategoryNationMobileCalls", "pnmc");
        productsPage.validateCategoriesSavedTestData(productCategory);
        String products = productsPage.addProductNationalRomingcall("addProductRomingCallRates", "aprcr");
        productsPage.validateProductSavedTestData(products);
        String id = productsPage.getIDOfAddedProduct();
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.FreeUsagePools);
        String freeUsagePoolName = confPage.AddFreeUsagePool("addFreeUsagePool", "afup", id, productCategory, products);
        propReader.updatePropertyInFile("TC_113,CATEGORY_NAME", freeUsagePoolName, "testData");
        navPage.navigateToPlanPage();
        String addPlan = plansPage.addPlanForMobileCalls(freeUsagePoolName, productCategory, products,
                "addPlanForMobileCall", "apfmc");
        propReader.updatePropertyInFile("TC_113,PLAN", addPlan, "testData");
        msgsPage.verifyDisplayedMessageText("Saved New plan", "successfully", TextComparators.contains);
    }
}
