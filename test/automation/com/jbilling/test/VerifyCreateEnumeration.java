package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddNewEnumeration;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateEnumeration extends BrowserApp {

    @Test(description = "Test Case 84: Verify that user is able to create Enumeration.", priority = 1)
    public void tc_0084_reateEnumerations () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Enumerations);
        String enumerationName = confPage.createEnumeration(AddNewEnumeration.VERIFY_MANDATORY_FIELDS,
                "TC84_EnumValidateMessage", "evm");
        msgsPage.verifyDisplayedMessageText("Enumeration with Id", "saved successfully.", TextComparators.contains);
        confPage.validateEnumerationsSavedData(enumerationName);
        confPage.editConfiguration(enumerationName, "TC84_EnumValidateMessage", "evm");
        confPage.verifyDuplicateValueForEnumeration(enumerationName, "TC84_EnumValidateMessage", "evm");
        confPage.selectEnumerationsFromTable(enumerationName);
        confPage.checkDeleteYesNo("NO");
        confPage.validateEnumerationsSavedData(enumerationName);
    }
}
