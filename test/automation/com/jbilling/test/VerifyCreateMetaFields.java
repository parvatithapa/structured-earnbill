package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateMetaFields extends BrowserApp {

    @Test(description="TC66: Verify that User is able to create Meta-fields")
    public void tc_0066_VerifyCreateMetafields () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.MetaFields);
        confPage.createMetafield();
    }
}
