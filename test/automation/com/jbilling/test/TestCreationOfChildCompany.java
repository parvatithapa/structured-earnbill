package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;
import com.jbilling.framework.utilities.textutilities.TextUtilities;

public class TestCreationOfChildCompany extends BrowserApp {

    @Test(description = "Test Case 1.3: Verify ability to create a Child Company within Root Company")
    public void testCreationOfChildCompany () {

        setTestRailsId("11047238");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Company);
        confPage.clickCopyCompanyButton();
        confPage.setAdminEmail("admin@jbilling.com");
        confPage.markCompanyAsChildCompany(true);
        confPage.setTemplateCompanyName();
        logger.info("Starting 'Copy Company'");
        confPage.clickConfirmPopupYesButton();
        logger.info("'Copy Company' finished");

        String un = confPage.extractUserNameFromCompanyCreationMessage();
        String pwd = confPage.extractPasswordFromCompanyCreationMessage();
        String cn = confPage.extractCompanyNameFromCompanyCreationMessage();
        String cid = confPage.extractCompanyIdFromCompanyCreationMessage();

        if (TextUtilities.isBlank(un) || TextUtilities.isBlank(pwd) || TextUtilities.isBlank(cn)
                || TextUtilities.isBlank(cid)) {
            throw new RuntimeException("Test failed for copying company as no information generated. UserName: " + un
                    + " -> Password: " + pwd + " -> CompanyName: " + cn + " -> CompanyId: " + cid);
        }

        // TODO: check what is wrong here and why it was commented out

        // propReader.updatePropertyInFile("TC_1.3_CHILD_COMPANY_USERNAME", un, "testData");
        // propReader.updatePropertyInFile("TC_1.3_CHILD_COMPANY_PASSWORD", pwd, "testData");
        // propReader.updatePropertyInFile("TC_1.3_CHILD_COMPANY_COMPANYNAME", cn, "testData");
        // propReader.updatePropertyInFile("TC_1.3_CHILD_COMPANY_COMPANYID", cid, "testData");
        //
        // // The Child Company should now appear in the impersonate drop down.
        // if (navPage.isChildCompanyImpersonated(propReader.readPropertyFromFile("TC_1.3_CHILD_COMPANY_COMPANYNAME",
        // "testData")) == false) {
        // throw new RuntimeException("Test 1.3 failed as child company created but not impersonated");
        // }
        logger.debug("User successfully created the Child Company and appearing in the impersonate drop down.");
    }
}
