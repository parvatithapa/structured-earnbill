package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateNestedPlanForDataPreparation extends BrowserApp {

    @Test(description = "TC 145: Data preparation (Nested Plan)", groups = { "globalRegressionPack" })
    public void tc_0145_createNestedPlanForDataPreparationTest () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.adddNewOrderStatus("OrderChangeStatus", "ocs", true);
        navPage.navigateToProductsPage();
        String AddedCategory = productsPage.addCategory("CreateProductCategoryServices", "pcd");
        String CategoryID = productsPage.getIDAddedCategory(AddedCategory);
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameSMS", "ap");
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameGPRS", "ap");
        navPage.navigateToProductsPage();
        String AddedCategory1 = productsPage.addCategory("CreateProductCategoryMSISDN", "pcd");
        String CategoryID1 = productsPage.getIDAddedCategory(AddedCategory1);
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameMSISDN", "ap");
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameMSISDN_2", "ap");
        navPage.navigateToProductsPage();
        String AddedCategory2 = productsPage.addCategory("CreateProductCategoryTarrifPlans", "pcd");
        String CategoryID2 = productsPage.getIDAddedCategory(AddedCategory2);
        String AddedCategory3 = productsPage.addCategory("CreateProductCategoryRates", "pcd");
        String CategoryID3 = productsPage.getIDAddedCategory(AddedCategory3);
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameSMS2", "ap");
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameEU", "ap");
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameConnectionFee", "ap");
        String productCategory9 = productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameSMS3", "ap");
        String ID = productsPage.getIDAddedProduct(productCategory9);
        productsPage.addProduct(AddProductField.FLATPRICE, "addProductWithNameSMS1", "ap");
        productsPage.editProductWithDependency("editProductWithDependency", "ap", AddedCategory3, ID);
        navPage.navigateToConfigurationPage();
        confPage.configureOrderChangeTypeWithMulitpleProduct("NewOrderChangeStatus", "ocs", true, CategoryID1,
                CategoryID2);
        confPage.configureOrderChangeTypeWithoutAllowStatusChange("NewOrderChangeStatusWithoutAllowOrder", "ocs",
                CategoryID, CategoryID3);
        msgsPage.verifyDisplayedMessageText("Order Change Type created", "successfully", TextComparators.contains);
    }
}
