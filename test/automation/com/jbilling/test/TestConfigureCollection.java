package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.CollectionAgeingStep;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestConfigureCollection extends BrowserApp {

    @Test(description = "Test Case 2.5 : Verify ability to configure Collections")
    public void testConfigureCollection () throws IOException {

        setTestRailsId("11047246");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Collections);
        confPage.addCollectionsAgeingStep(CollectionAgeingStep.FIRST, "collectionsStepOne", "ccd");
        confPage.addCollectionsAgeingStep(CollectionAgeingStep.SECOND, "collectionsStepTwo", "ccd");
        confPage.addCollectionsAgeingStep(CollectionAgeingStep.THIRD, "collectionsStepThree", "ccd");
        confPage.addCollectionsAgeingStep(CollectionAgeingStep.FOURTH, "collectionsStepFour", "ccd");
        confPage.clickSaveChangesToCollections();
        navPage.navigateToConfigurationPage();
    }
}
