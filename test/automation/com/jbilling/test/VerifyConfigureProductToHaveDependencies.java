package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyConfigureProductToHaveDependencies extends BrowserApp {

    @Test(description = "TC 132 : Configure a product to have 1-2 dependencies.", groups = { "globalRegressionPack" })
    public void tc_0132_configure_product_with_dependencies () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String firstCategory = productsPage.addCategory("productCategory", "pcet");
        confPage.validateCategoriesSavedTestData(VerifyAddDependenciesOnProduct.category);
        productsPage.addProducts(AddProductField.FLAT, "addProductTwoToAddDependencies", "kol");

        navPage.navigateToProductsPage();
        String secondCategory = productsPage.addCategory("NewProductCategoryData", "per");
        confPage.validateCategoriesSavedTestData(secondCategory);
        productsPage.addProducts(AddProductField.FLAT, "addProductTwoToAddDependencies", "tgb");
        productsPage.Createanotherproduct(AddProductField.FLAT, "addProductTwoToAddDependencies", "idj");
        productsPage.EditProducts(firstCategory, "OrderPluginPageInfo1", "oi", secondCategory);

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.PaymentMethod);
        confPage.SelectPaymentMethodTemplate("configurePaymentTemplateMethod", "pom");
        String methodName = confPage.addrecurringPaymentMethodDetails("TestPaymentType", "HFG");
        msgsPage.verifyDisplayedMessageText("Payment Method Type", "created successfully", TextComparators.contains);
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        String accountTypename = confPage.accounttype("AccountTypeName", "oii", methodName);

        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.addNewCustomer(accountTypename, methodName, "NewCustomerInfo", "ldr");
        newCustomersPage.createOrderCustomer(customerName, "EditCustomerInfo", "ysk");
    }
}
