package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyAssociatePlanWithDifferentCategories extends BrowserApp {

    @Test(description = "TC 143: Verify user can associate plan with different (OPO/OPC) caegories.")
    public void tc_0143_validateMetaFiledtypeSelection () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String categoryName1 = productsPage.createCategoryWithOneCustomer("addCategoryOnePerCustomer", "acopc");
        navPage.navigateToProductsPage();
        String categoryName2 = productsPage.createCategoryWithOneOrder("addCategoryforOnePerOrder", "acopo");
        String description = productsPage.addProductInOnePerOrderCategory("addProdutcforOnePerOrder", "apopo",
                categoryName1);
        navPage.navigateToPlanPage();
        plansPage.addProductInMultipleCategoryInPlan("addProductOnePerOrder", "ap", categoryName1, categoryName2, description);
    }
}
