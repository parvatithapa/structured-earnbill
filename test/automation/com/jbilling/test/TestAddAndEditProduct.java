package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestAddAndEditProduct extends BrowserApp {

    @Test(description = "Test Case 3.3 : Verify user is able to add and edit a Product.")
    public void testAddAndEditProduct () {

        setTestRailsId("11047251");

        navPage.navigateToProductsPage();
        productsPage.addProduct(AddProductField.FLAT, "addProductOne", "ap");
        navPage.navigateToProductsPage();
        productsPage.addProduct(AddProductField.LINEPERCENTAGE, "addProductTwo", "ap");
        productsPage.editProduct("editProduct", "ap");
        msgsPage.verifyDisplayedMessageText("Updated product", "successfully.", TextComparators.contains);
        confPage.verifyUIComponent();
    }
}
