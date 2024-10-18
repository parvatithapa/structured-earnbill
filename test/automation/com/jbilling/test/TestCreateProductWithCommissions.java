package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateProductWithCommissions extends BrowserApp {

    @Test(description = "Test Case 16.2 : Verify that products with commisions can be made")
    public void testCreateProductWithCommissions () {

        setTestRailsId("10909928");

        navPage.navigateToProductsPage();
        productsPage.addCategory("NewProductCategoryData", "pcd");
        productsPage.addProductWithCommission("addProductTwo", "ap");
    }
}
