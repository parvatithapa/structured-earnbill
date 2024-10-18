package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateCategoryOnePerOrder extends BrowserApp {

    @Test(description = "TC 140 : Verify user can Create categories with One Per Order.")
    public void tc_0140_createCategoryWithOnePerOrder () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String categoryName = productsPage.createCategoryWithOneOrder("addCategoryforOnePerOrder", "acopo");

        String description = productsPage.addProductInOnePerOrderCategory("addProdutcforOnePerOrder", "apopo",
                categoryName);
        navPage.navigateToProductsPage();
        String description1 = productsPage.addProductInOnePerOrderCategory("addProdutcforOnePerOrder", "apopo",
                categoryName);

        navPage.navigateToPlanPage();
        String description2 = plansPage.addProductInPlan("addProductOnePerOrder", "ap", categoryName, description);
        navPage.navigateToPlanPage();
        String description4 = plansPage.addProductInPlan("addProductOnePerOrder", "ap", categoryName, description);

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("debitCardPaymentType", "pt");

        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");

        logger.info("description:"+description);
        logger.info("description2:"+description2);
        logger.info("description4:"+description4);
/*
        String accountType = "TestAccountTYOBK";
        String description = "productYACQA";
        String description1 = "productMFFOE";
        String description2 = "productTDLMQ";
        String description4 = "productWYSXN";
*/

        navPage.navigateToCustomersPage();
        newCustomersPage.addCustomerWithMakePayment("customerCreate", "cc", accountType);
        newCustomersPage.createOrderForOnePerOrder("AddorderPerOrder", "ao", description, description1);

        newCustomersPage.verifyAddedProductInPlan(description, description2, description4);
    }
}
