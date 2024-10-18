package com.jbilling.test;

import java.io.IOException;
import java.lang.AssertionError;
import java.lang.RuntimeException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;
import junit.framework.Assert;

public class TestEntityLogoConfiguration extends BrowserApp {

    private static final String VALID_IMAGE = "/test/automation/resources/testdata/logos/logoForAutomationTest.png";

    @Test(description = "Verify correct navigation bar logo upload")
    public void TestNavigationBarLogoUpload () throws IOException {
        setTestRailsId("34318837");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.EntityLogos);
        confPage.uploadNavigationBarLogo(VALID_IMAGE);
        confPage.clickSave();
        String msg = confPage.isNavigationLogoUploadSuccesful();
        if (msg != null) {
            throw new RuntimeException("Navigation Bar Logo was not updated successfully. Message: " + msg);
        }
    }

    @Test(description = "Verify correct favicon logo upload")
    public void TestFaviconLogoUpload () throws IOException {
        setTestRailsId("34347058");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.EntityLogos);
        confPage.uploadFaviconLogo(VALID_IMAGE);
        confPage.clickSave();
        String msg = confPage.isFaviconLogoUploadSuccesful();
        if (msg != null) {
            throw new RuntimeException("Favicon Logo was not updated successfully. Message: " + msg);
        }
    }
}