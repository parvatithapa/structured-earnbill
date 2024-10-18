package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyActiveSinceDateOfDiscountSubOrderCreatedIsSameAsActiveSinceOfTheParentOrder extends BrowserApp {

    @Test(description = "TC 111 : Verify that 'Active Since' date of a Discount sub-order created "
            + "(for a manual discount) is same as Active Since of the parent Order.")
    public void tc_0111_verifyActiveDateOfSunOrderSameAsActiveSinceOfParentOrder () {

        navPage.navigateToDiscountsPage();
        String discountName = discountsPage.createNewDiscountWithPercentage("addDiscountWithPercentage", "adwp");

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("debitCardPaymentType", "pt");

        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");

        navPage.navigateToProductsPage();
        String categoryName = productsPage.createCategoryWithOneCustomer("addCategoryForMinimumTimePeriod", "ac");
        String productDescription = productsPage.addProductInOnePerCustomerCategory("addProductForMinimumTime", "ap",
                categoryName);

        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.createCustomerWithPaymentType("AddCustomerWithPaymentCard", "acwpc",
                accountType, methodName);
        newCustomersPage.createOrderWithDiscount("createOrderwithDiscount", "cowd", productDescription, discountName);
        ordersPage.verifySubOrderActiveSinceDate(customerName);
    }
}
