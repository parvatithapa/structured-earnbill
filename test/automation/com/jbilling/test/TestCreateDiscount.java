package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateDiscount extends BrowserApp {

    @Test(description = "Test Case 6.3 : Verify user is able to create discounts to be availed while making purchase.")
    public void testCreateDiscount () throws IOException {

        setTestRailsId("10909909");

        navPage.navigateToDiscountsPage();

        discountsPage.clickAdd();
        discountsPage.clickSave();
        discountsPage.isValidationErrorAppeared();

        String discountCodeDescription = discountsPage.createNewDiscount("addDiscount", "ad");
        discountsPage.isDiscountCreatedSuccessfully();

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_6.2_CHILD_CUSTOMER_NAME"));
        try {Thread.sleep(1000);} catch (Exception e) {}
        newCustomersPage.createOrder("createOrder", "co", discountCodeDescription);
        try {Thread.sleep(1000);} catch (Exception e) {}
        ordersPage.verifyUIComponent();
    }
}
