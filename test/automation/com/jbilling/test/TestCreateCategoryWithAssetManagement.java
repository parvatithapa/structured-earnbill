package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateCategoryWithAssetManagement extends BrowserApp {

    @Test(description = "Test Case 3.2 : Verify user is able to create a Category that uses "
            + "Asset Management (and meta fields) 'Asset Category 1' is available to all Companies")
    public void testCreateCategoryWithAssetManagement () throws IOException {

        setTestRailsId("11047250");

        navPage.navigateToProductsPage();
        String assetCategory = productsPage.addProductCategoryWithAssetMgmt("CreateProductCategory", "ac");
        propReader.updatePropertyInFile("TC_3.2_CATEGORY_NAME", assetCategory, "testData");
        msgsPage.verifyDisplayedMessageText("Saved new product category", "successfully", TextComparators.contains);
        confPage.validateCategoriesSavedTestData(assetCategory);
        confPage.verifyUIComponent();
    }
}
