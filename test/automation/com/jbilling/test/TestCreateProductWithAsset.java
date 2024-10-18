package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateProductWithAsset extends BrowserApp {

    @Test(description = "Test Case 3.4 : Verify that a user can create a product with an asset")
    public void testCreateProductWithAsset () throws IOException {

        setTestRailsId("11047252");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        productsPage.addPlugin("addPlugin", "ap");
        navPage.navigateToProductsPage();
        productsPage.selectAssetCategory1();
        String englishDescription = productsPage.addProduct(AddProductField.ASSETMANAGEMENT, "product.tc-3.4", "ap");
        propReader.updatePropertyInFile("TC_3.4_ENGLISH_DESC", englishDescription, "testData");
        productsPage.addAsset();
        productsPage.addAsset("addAssetOne", "ap");
        productsPage.clickAddNew();
        productsPage.addAsset("addAssetTwo", "ap");
    }
}
