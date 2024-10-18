package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.globals.GlobalEnumsPage.setProductCategoryWithAssetMgmt;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateAssetMgmtWithDiffCategories extends BrowserApp {

    @Test(description = "TC 40: Verify, user is able to create asset management categories with "
            + "different category type and belonging to different asset-statuses " + "and having various meta-fields.", groups = { "globalRegressionPack" })
    public void tc_0040_checkCreateAssetMgmtWithDiffCategories () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.selectPluginCategory("selectPluginCategory", "pc");
        confPage.addNewPluginInCategory("addPlugin", "ap");
        navPage.navigateToProductsPage();

        String savedCategoryName = pr.readTestData("TC_3.2_CATEGORY_NAME");
        String strMetaFieldName = pr.readTestData("strMetaField");
        String intMetaFieldName = pr.readTestData("intMetaField");
        String boolMetaFieldName = pr.readTestData("boolMetaField");

        productsPage.addProductCategoryWithAssetMgmt(savedCategoryName, boolMetaFieldName,
                setProductCategoryWithAssetMgmt.PCWAMG2, "CreateProductCategory", "ac");
        productsPage.addProductCategoryWithAssetMgmt(savedCategoryName, intMetaFieldName,
                setProductCategoryWithAssetMgmt.PCWAMG3, "CreateProductCategory", "ac");
        productsPage.addProductCategoryWithAssetMgmt(savedCategoryName, boolMetaFieldName,
                setProductCategoryWithAssetMgmt.PCWAMG4, "CreateProductCategory", "ac");
    }
}
