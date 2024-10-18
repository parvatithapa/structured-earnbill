package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestCustomerStatusChangeAsCollection extends BrowserApp {

    @Test(description = "Test Case 12.1: Verify customer status changed as per the collection")
    public void testCustomerStatusChangeAsCollection () {

        setTestRailsId("10909922");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Collections);
        confPage.runCollectionsForDate("03/01/2001");
        navPage.navigateToCustomersPage();
        newCustomersPage.statusCycle("customerInformationForCollectionCycleOne_One",
                "customerInformationForCollectionCycleOne_Two", "ci");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Collections);
        confPage.runCollectionsForDate("03/20/2001");
        navPage.navigateToCustomersPage();
        newCustomersPage.statusCycle("customerInformationForCollectionCycleTwo_One",
                "customerInformationForCollectionTwo_Two", "ci");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Collections);
        confPage.runCollectionsForDate("03/25/2001");
        navPage.navigateToCustomersPage();
        newCustomersPage.statusCycle("customerInformationForCollectionCycleThree_One",
                "customerInformationForCollectionCycleThree_Two", "ci");
    }
}
