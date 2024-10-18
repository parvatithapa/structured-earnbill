package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestOrderProvisioningTaskTriggersEvent extends BrowserApp {

    @Test(description = "Test Case 8.1 : Verify Order Provisioning Task Triggers Event")
    public void tc_0008_orderProvisioningTaskTriggersEvent () {

        setTestRailsId("10909911");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        productsPage.addPlugin("addPlugin", "ap");
        navPage.navigateToProductsPage();

        String category = productsPage.addProductCategoryWithAssetMgmt("CreateProductCategory", "ac");

        String productName = productsPage.addProduct(AddProductField.ADDPRODUCTWITHASSETMANAGEMENT, "addProduct", "ap");
        productsPage.addAsset();

        String assetIdentifier1 = productsPage.addAsset("addAssetOne", "ap");
        productsPage.clickAddNew();

        String assetIdentifier2 = productsPage.addAsset("addAssetTwo", "ap");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String accountName = confPage.createAccountType("addAccountType", "aat");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        confPage.configurePaymentMethod("configurePaymentMethod", "pm");

        String methodName = confPage.addPaymentMethodDetails("debitCardPaymentType", "pt");

        navPage.navigateToCustomersPage();

        String customerName = newCustomersPage.addCustomer(accountName, methodName, "new_customer", "nc");
        newCustomersPage.addOrderWithAssetToCustomer("createOrder", "co", customerName, productName);
        ordersPage.clickProvisioningButton();
    }
}
