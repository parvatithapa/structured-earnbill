package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateCategoryOnePerCustomer extends BrowserApp {

    @Test(description = "TC 141 : Verify user can Create categories with One Per Customer ")
    public void tc_0141_createCategoryWithOnePerOrder () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String categoryName = productsPage.createCategoryWithOneCustomer("addCategoryOnePerCustomer", "acopc");
        System.out.println("categoryName:"+categoryName);
        String productDescription = productsPage.addProductInOnePerCustomerCategory("addFirstProductOnePerCustomer",
                "apopc", categoryName);
        System.out.println("productDescription:"+productDescription);
        navPage.navigateToProductsPage();
        String productDescription1 = productsPage.addProductInOnePerCustomerCategory("addProductOnePerCustomer",
                "apopc", categoryName);
        System.out.println("productDescription1:"+productDescription1);
        navPage.navigateToProductsPage();
        productsPage.addProductInOnePerCustomerCategory("addProductOnePerCustomer", "apopc", categoryName);
        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("debitCardPaymentType", "pt");
        System.out.println("methodName:"+methodName);
        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");
        System.out.println("accountType:"+accountType);
        navPage.navigateToCustomersPage();
        String loginName = newCustomersPage.addCustomerWithMakePayment("customerCreate", "cc", accountType);


        /*String categoryName = "Category One Per CustomerHDOWP";
        String productDescription = "CustomerProductHEIPI";
        String productDescription1 = "CustomerProductTKHDT";
        String methodName = "Debit cardCNZHE";
        String accountType = "TestAccountWWEVQ";
        String loginName = "testLoginZBDAJ";
        */
        System.out.println("loginName:"+loginName);
        newCustomersPage.createOrderForOnePerCustomer("AddorderPerCustomer", "aopc", productDescription);
        navPage.navigateToCustomersPage();
        newCustomersPage.addProductInExistingCustomer("AddorderPerCustomer", "aopc", loginName, productDescription1);
    }
}
