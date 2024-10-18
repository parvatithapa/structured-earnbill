package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateGlobalProductWithAnAsset extends BrowserApp {

    @Test(description = "TC 48 : Verify that user can create a global product with an asset")
    private void tc_0048_createGlobalProductWithAsset () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String globalCategoryWithAssetManagement = productsPage.addNewCategory("assetName", "name");
        productsPage.selectCategory(globalCategoryWithAssetManagement);
        String description = productsPage.addProduct(AddProductField.ASSETMANAGEMENT, "product.tc-48", "ap");
        productsPage.validateAddedProduct(description);
        String identifier = productsPage.addAssetinProduct("assetDetail", "ad");
        productsPage.validateAddedAsset(identifier);
        String childIdentifier = productsPage.addChildAsset("assetDetail", "ad");
        productsPage.validateAddedAsset(childIdentifier);
    }
}
