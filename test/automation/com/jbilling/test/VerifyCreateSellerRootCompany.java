package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateSellerRootCompany extends BrowserApp {

    @Test(description = "Verify that user can create Child Company (Invoice as reseller = Checked) within Root Company", groups = { "globalRegressionPack" })
    public void tc_0364_addSellerCompany () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Company);
        confPage.createSellerCompany("sellerCompany", "seller");
        msgsPage.isIntermediateSuccessMessageAppeared();
    }
}
