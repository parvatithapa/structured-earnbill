package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateProductCategory extends BrowserApp {

    @Test(description = "Test Case 3.1 : Verify user is able to create/edit a Category 'New Test "
            + "Category' is only available to Root Company (jBilling).")
    public void testCreateProductCategory () throws IOException {

        setTestRailsId("11047249");

        navPage.navigateToProductsPage();
        productsPage.addCategory("CreateProductCategoryData", "pcd");
        String categoryName = productsPage.editCategory("NewProductCategoryData", "pcd");
        propReader.updatePropertyInFile("TC_3.1_CATEGORY_NAME", categoryName, "testData");
        confPage.validateCategoriesSavedTestData(categoryName);
        confPage.verifyUIComponent();
    }
}
