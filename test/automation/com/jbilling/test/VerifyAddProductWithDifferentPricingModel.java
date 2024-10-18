package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyAddProductWithDifferentPricingModel extends BrowserApp {

    @Test(description = "60.1: Verify User is able to Add Product with different Pricing Models.", groups = { "globalRegressionPack" })
    public void tc_0060_1_verifyAddProductwithDifferentPricing () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        productsPage.addCategory("CreateProductCategoryData", "pcd");
        productsPage.addProduct(AddProductField.DESCRIPTION, "addProductCategoryOne", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.2: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_2_verifyAddProductwithFlatPrice () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithFlatPricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.3: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_3_verifyAddProductwithGraduatePrice () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.GRADUATEDPRICE, "addProductWithGraduatePricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.4: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_4_verifyAddProductwithGraduatedCapPrice () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.GRADUATECAPPRICE, "addProductWithGraduateCapPricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.5: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_5_verifyAddProductwithTimeOfDayPrice () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.TIMEOFDAY, "addProductWithTimeOfDayPricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.6: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_6_verifyAddProductwithTieredPricing () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.TIERED, "addProductWithTieredPricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.7: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_7_verifyAddProductwithVolumePricing () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.VOLUME, "addProductWithVolumePricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.8: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_8_verifyAddwithPooledPricing () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.POOLED, "addProductWithPooledPricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.10: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_10_verifyAddwithItemPercantageSelector () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.ITEMPAGESELECTOR, "addProductWithItemPercantageSelector", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.9: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_9_verifyAddItemItemSelector () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.ITEMSELECTOR, "addProductWithItemSelector", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.11: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_11_verifyAddItemQuantityAdd () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.QUANTITYADON, "addProductWithQuantityAdOn", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }

    @Test(description = "60.12: Verify User is able to Add Product with different Pricing Models.", dependsOnMethods = "tc_0060_1_verifyAddProductwithDifferentPricing")
    public void tc_0060_12_verifyAddItemWithTeaserPricing () {

        setTestRailsId("");

        productsPage.addProduct(AddProductField.TEASERPRICING, "addProductWithTeaserPricing", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new product", "successfully", TextComparators.contains);
    }
}
