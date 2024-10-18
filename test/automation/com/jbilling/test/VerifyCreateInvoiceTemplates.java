package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateInvoiceTemplates extends BrowserApp {

    @Test(description = "TC 75 : Verify that user is able to create new invoice templates.", groups = { "globalRegressionPack" })
    public void tc_0075_addInvoiceTemplate () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.InvoiceTemplates);
        String AddinvoiceTemplates1 = confPage.addInvoiceTemplate("addInvoiceTemplateName", "additn");
        String AddinvoiceTemplates2 = confPage.addInvoiceTemplate("addInvoiceTemplateName", "additn");
        msgsPage.verifyDisplayedMessageText("Invoice Template ", "Created Invoice Template", TextComparators.contains);
        confPage.validateInvoiceSavedTestData(AddinvoiceTemplates1);
        confPage.validateInvoiceSavedTestData(AddinvoiceTemplates2);
    }
}
