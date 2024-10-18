package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateProductWithDependencyOnOther extends BrowserApp {

    @Test(description = "Test Case 15.1 :  Verify that Products with dependencies on other "
            + "products can be created.")
    public void testCreateProductWithDependencyOnOther () throws IOException {

        setTestRailsId("10909925");

        navPage.navigateToProductsPage();
        String assetCategory = productsPage.addProductCategoryWithAssetMgmt("CreateProductCategory", "ac");
        propReader.updatePropertyInFile("TC_3.2_CATEGORY_NAME", assetCategory, "testData");
        msgsPage.verifyDisplayedMessageText("Saved new product category", "successfully", TextComparators.contains);
        confPage.validateCategoriesSavedTestData(assetCategory);
        confPage.verifyUIComponent();
        navPage.navigateToProductsPage();
        productsPage.addCategory("productCategory", "pcat");
        productsPage.addProductOnDependency("addProductTwo", "ap", assetCategory);
        ordersPage.verifyUIComponent();
    }
}
