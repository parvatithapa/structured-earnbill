package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCreateAndEditPlan extends BrowserApp {

    @Test(description = "Test Case 4.1 : Verify that user is able to create and edit a plan.")
    public void testCreateAndEditPlan () throws IOException {

        setTestRailsId("10909905");

        navPage.navigateToPlanPage();
        String productCategoryName = pr.readTestData("TC_3.2_CATEGORY_NAME");
        String engDescription = pr.readTestData("TC_3.4_ENGLISH_DESC");
        plansPage.addPlan(productCategoryName, engDescription, "addplan", "ap");
        plansPage.editPlan(engDescription, "addplan", "ap");
    }
}
