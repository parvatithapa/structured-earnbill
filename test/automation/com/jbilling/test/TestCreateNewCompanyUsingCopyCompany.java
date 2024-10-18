package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;
import com.jbilling.framework.utilities.textutilities.TextUtilities;

public class TestCreateNewCompanyUsingCopyCompany extends BrowserApp {

    @Test(description = "Test Case 1.1:  testCreateNewCompanyUsingCopyCompany")
    public void testCreateNewCompanyUsingCopyCompany () throws IOException {

        setTestRailsId("11047236");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Company);
        confPage.clickCopyCompanyButton();
        confPage.setAdminEmail("admin@jbilling.com");
        logger.info("Starting 'Copy Company'");
        try {
            confPage.clickConfirmPopupYesButton();
        } catch (Exception e) {
            try {
                Thread.sleep(30000l);
            } catch (InterruptedException e1) {

            }
            logger.info("Copy company took longer than default timeout");
            logger.info("Swallowing Exception", e);
        }
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

        propReader.updatePropertyInFile("TC_1.1_CREDENTIALS_USERNAME", un, "testData");
        propReader.updatePropertyInFile("TC_1.1_CREDENTIALS_PASSWORD", pwd, "testData");
        propReader.updatePropertyInFile("TC_1.1_CREDENTIALS_COMPANYNAME", cn, "testData");
        propReader.updatePropertyInFile("TC_1.1_CREDENTIALS_COMPANYID", cid, "testData");

        confPage.verifyUIComponent();
    }
}
