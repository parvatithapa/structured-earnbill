package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifySubscribeToTheFUP extends BrowserApp {

    private String achPaymentMethod;
    private String achAccountName;
    private String achCustomerName;

    @Test(description = " TC 115: Verify user is able to subscribe to the FUP plan ", priority = 1)
    public void tc_0115_addPaymentMethodForACH () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        achPaymentMethod = confPage.addACHPaymentMethod("TC115_CreatePaymentMethodACH", "apm");
        logger.info("achPaymentMethod:"+achPaymentMethod);
        msgsPage.verifyDisplayedMessageText("Payment Method Type ", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(achPaymentMethod);
    }

    @Test(description = "TC 115: Verify user is able to subscribe to the FUP plan", dependsOnMethods = "tc_0115_addPaymentMethodForACH", priority = 2)
    public void tc_0115_addAccountType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        achAccountName = confPage.addACHAccountType("TC115_AccountTypeACH", "atach", achPaymentMethod);
        logger.info("achAccountName:"+achAccountName);
        msgsPage.verifyDisplayedMessageText("Account Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(achAccountName);
    }

    @Test(description = "TC 115: Verify user is able to subscribe to the FUP plan", dependsOnMethods = "tc_0115_addAccountType", priority = 3)
    public void tc_0115_addCustomer () {

        setTestRailsId("");

        navPage.navigateToCustomersPage();
        achCustomerName = newCustomersPage.addACHCustomerType(achAccountName, achPaymentMethod,
                "TC115_ACH_Cusotmer_Type", "achct");
        logger.info("achCustomerName:"+achCustomerName);
        msgsPage.verifyDisplayedMessageText("Saved new customer", "successfully", TextComparators.contains);
        newCustomersPage.validateUsersSavedTestData(achCustomerName);
    }

    @Test(description = "TC 115: Verify user is able to subscribe to the FUP plan", dependsOnMethods = "tc_0115_addCustomer", priority = 4)
    public void tc_0115_createOrdere () throws java.io.IOException {

        setTestRailsId("");
        logger.info("achCustomerName:"+achCustomerName);
        navPage.navigateToCustomersPage();
        String plan = propReader.readPropertyFromFile("TC_113,PLAN", "testData.properties");
        newCustomersPage.createOrderForFUPCustomer(achCustomerName, "TC115_Create_Order", "co", plan);
        msgsPage.verifyDisplayedMessageText("Created new order", "successfully", TextComparators.contains);
    }

    @Test(description = "TC 115: Verify user is able to subscribe to the FUP plan", dependsOnMethods = "tc_0115_addCustomer", priority = 4)
    public void tc_0115_editOrder () {

        setTestRailsId("");

        navPage.navigateToCustomersPage();
        newCustomersPage.editOrderForFUPCustomer(achCustomerName, "TC115_Edit_Order", "eco");
    }
}
