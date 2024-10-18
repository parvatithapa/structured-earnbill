package com.jbilling.test;

import java.io.IOException;
import java.util.*;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AccountTypeInfo;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.globals.GlobalEnumsPage.MakePayment;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.globals.GlobalEnumsPage.PaymentMethodField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import com.jbilling.framework.utilities.xmlutils.TestData;

public class VerifyAddPaymentMethodAndAccountType extends BrowserApp {

    private HashMap<String, String> runTimeVariables = new HashMap<String, String>();

//    @BeforeClass
    public void loadPropertiesFile() throws IOException {
        Properties props = propReader.readAllProperties("testData.properties");
        for(Map.Entry entry : props.entrySet()) {
            runTimeVariables.put((String)entry.getKey(), (String)entry.getValue());
            logger.info("Adding runtime variable: "+entry.getKey()+"="+entry.getValue());
        }
    }

    @Test(description = "Test Case 07: Verify that user is able to create a payment method using card.", priority = 1, enabled = true)
    public void tc_0007_addPaymentMethodForCard () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        String paymentMethodName = confPage.addPaymentMethodWithoutMetaFields(PaymentMethodField.REECURRING,
                "TC07_AddCardPaymentMethod", "apm");
        runTimeVariables.put("TC_07_PAYMENT_METHOD_NAME", paymentMethodName);
        propReader.updatePropertyInFile("TC_07_PAYMENT_METHOD_NAME", paymentMethodName, "testData");
        msgsPage.verifyDisplayedMessageText("Payment Method Type ", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(paymentMethodName);
    }

    @Test(description = "Test Case 08: Verify that user is able to create a payment method using ACH.", priority = 2, enabled = true)
    public void tc_0008_addPaymentMethodForACH () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        String paymentMethodName = confPage.addPaymentMethodWithoutMetaFields(PaymentMethodField.REECURRING,
                "TC08_CreatePaymentMethodACH", "apm");
        runTimeVariables.put("TC_08_PAYMENT_METHOD_NAME", paymentMethodName);
        propReader.updatePropertyInFile("TC_08_PAYMENT_METHOD_NAME", paymentMethodName, "testData");
        msgsPage.verifyDisplayedMessageText("Payment Method Type ", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(paymentMethodName);
    }

    @Test(description = "TC 09 : Verify that user is able to create a payment method using Cheque", priority = 3, enabled = true)
    public void tc_0009_addPaymentMethodForCheque () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        String paymentMethodName = confPage.addPaymentMethodWithoutMetaFields(PaymentMethodField.REECURRING,
                "TC09_addChequePaymentMethod", "apm");
        runTimeVariables.put("TC_09_PAYMENT_METHOD_NAME", paymentMethodName);
        propReader.updatePropertyInFile("TC_09_PAYMENT_METHOD_NAME", paymentMethodName, "testData");
        msgsPage.verifyDisplayedMessageText("Payment Method Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(paymentMethodName);
    }

    @Test(description = "TC 11.1 : Verify that user is able to create account type with credit limit and notification amount configured", dependsOnMethods = "tc_0009_addPaymentMethodForCheque", priority = 4, enabled = true)
    public void tc_0011_1_addAccountTypeWithCreditForThreePayMethod () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String TC09_paymentMethodName = runTimeVariables.get("TC_09_PAYMENT_METHOD_NAME");
        String TC08_paymentMethodName = runTimeVariables.get("TC_08_PAYMENT_METHOD_NAME");
        String TC07_paymentMethodName = runTimeVariables.get("TC_07_PAYMENT_METHOD_NAME");
        String accountName = confPage.AddAccountTypeWithCreditDetailsForThreePay("TC11_AccountTypeWithCreditLimit",
                "atcl", TC09_paymentMethodName, TC08_paymentMethodName, TC07_paymentMethodName);
        runTimeVariables.put("TC_11_1_ACCOUNT_NAME", accountName);
        propReader.updatePropertyInFile("TC_11_1_ACCOUNT_NAME", accountName, "testData");
        msgsPage.verifyDisplayedMessageText("Account Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(accountName);
    }

    @Test(description = "TC 12.0 : Verify searching and sorting works as defined for 'Account Type'.", dependsOnMethods = "tc_0011_1_addAccountTypeWithCreditForThreePayMethod", priority = 5, enabled = true)
    public void tc_0012_verifySearchingAndSortingForAccountType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();

        // Set Preference for ID 63
        // confPage.updatePreference("set63Preference", "sp");
        logger.debug("Set Preference for ID 63");

        // Select Payment Method from Configuration list
        // confPage.selectConfiguration(PageConfigurationItems.AccountType);
        logger.debug("Select Payment Method from Configuration list");

        // TODO: Method to Sort for Account Name

        // String accountNameToSearch = runTimeVariables.get("TC_11_1_ACCOUNT_NAME");
        // confPage.searchAndSortFunctionalityOfAccType(accountNameToSearch);

        navPage.navigateToConfigurationPage();

        // confPage.ReupdateJQGridPreference("reSet63Preference", "sp");
        logger.debug("Set Preference for ID 63");
    }

    @Test(description = "TC 13 : Verify that user is able to edit account type and edit the payment methods associated with the account type.", dependsOnMethods = "tc_0009_addPaymentMethodForCheque", priority = 6, enabled = true)
    public void tc_0013_addAccountTypeWithCreditForThreePayMethod () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String TC11_1_accountName = runTimeVariables.get("TC_11_1_ACCOUNT_NAME");
        String TC08_paymentMethodName = runTimeVariables.get("TC_08_PAYMENT_METHOD_NAME");
        confPage.editAccountTypeForGivenAccountDeselectPay(TC11_1_accountName, TC08_paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Account Type", "updated successfully", TextComparators.contains);

        String TC09_paymentMethodName = runTimeVariables.get("TC_09_PAYMENT_METHOD_NAME");
        String TC07_paymentMethodName = runTimeVariables.get("TC_07_PAYMENT_METHOD_NAME");
        String accountName = confPage.editAccountTypeForGivenAccountWithThreePay("TC13_EditAccountTypeName", "ea",
                TC11_1_accountName, TC09_paymentMethodName, TC08_paymentMethodName, TC07_paymentMethodName);
        propReader.updatePropertyInFile("TC_13_ACCOUNT_NAME", accountName, "testData");

        msgsPage.verifyDisplayedMessageText("Account Type", "updated successfully", TextComparators.contains);

        // confPage.validatePeriodsSavedTestData(accountName);
        logger.debug("Validate Saved Account Type Test Data");
    }

    @Test(description = "TC 14 : Verify that user can create a new Account type using the Card payment method", dependsOnMethods = "tc_0007_addPaymentMethodForCard", priority = 7, enabled = true)
    public void tc_0014_addAccountTypeUsingCardPayMethod () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        confPage.verifyMandatoryFieldMessages("mandatoryFieldAccountType", "mfat");
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String TC07_paymentMethodName = runTimeVariables.get("TC_07_PAYMENT_METHOD_NAME");
        String accountName = confPage.createAccountTypeWithCreditDetails("TC14_AccountTypeWithCreditLimit", "atcl",
                TC07_paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Account Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(accountName);
    }

    @Test(description = "TC 15 : Verify that user can create a new Account type using the ACH payment method", dependsOnMethods = "tc_0008_addPaymentMethodForACH", priority = 8, enabled = true)
    public void tc_0015_addAccountTypeUsingACHPayMethod () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        String TC08_paymentMethodName = runTimeVariables.get("TC_08_PAYMENT_METHOD_NAME");
        String accountName = confPage.createAccountTypeWithCreditDetails("TC15_AddPaymentMethodwithACH", "apm",
                TC08_paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Account Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(accountName);
    }

    @Test(description = "TC 16 : Verify that user can create a new Account type using the Cheque payment method", dependsOnMethods = "tc_0009_addPaymentMethodForCheque", priority = 9, enabled = true)
    public void tc_0016_addPaymentMethodUsingCheque () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        String TC09_paymentMethodName = runTimeVariables.get("TC_09_PAYMENT_METHOD_NAME");
        String accountName = confPage.createAccountTypeWithCreditDetails("TC16_addPaymentMethodwithCheque", "apm",
                TC09_paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Account Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(accountName);
    }

    @Test(description = "TC 17 :Verify user is able to create payment card/cheque/ach payment method with 'All account Type' check-box checked.", priority = 10, enabled = true)
    public void tc_0017_addPaymentMethodForCheque () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        String paymentMethodName = confPage.addPaymentMethodWithoutMetaFields(PaymentMethodField.ALL,
                "TC17_addChequePaymentMethod", "apm");
        runTimeVariables.put("TC_17_PAYMENT_METHOD_NAME", paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Payment Method Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(paymentMethodName);
    }

    @Test(description = "TC_18  : Verify User able to get mandatory field validation message", dependsOnMethods = "tc_0017_addPaymentMethodForCheque", priority = 11, enabled = true)
    public void tc_0018_verifyPaymentMethodAvailableForAllCust () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        String TC17_PaymentMethodName = runTimeVariables.get("TC_17_PAYMENT_METHOD_NAME");
        String accName = confPage.verifyPayMethodDefaultSelectedForAddingAccountType("TC18_AddAccountType", "aat",
                TC17_PaymentMethodName);
        runTimeVariables.put("TC_18_ACCOUNT_TYPE_NAME", accName);
        navPage.navigateToCustomersPage();
        newCustomersPage.verifyPaymentAvailableForCustomer(accName, TC17_PaymentMethodName);
    }

    @Test(description = "TC 19 : Verify that, this payment method is default selected for all the account types created in future.", dependsOnMethods = "tc_0015_addAccountTypeUsingACHPayMethod", priority = 12, enabled = true)
    public void tc_0019_verifyPayMethodIsDefaultSelectedForAllAccType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        String TC17_PaymentMethodName = runTimeVariables.get("TC_17_PAYMENT_METHOD_NAME");
        confPage.verifyPayMethodDefaultSelectedForAccountType(TC17_PaymentMethodName);
    }

    @Test(description = "TC 20 : Verify this created payment method works correctly for all the account types created in future.", dependsOnMethods = "tc_0009_addPaymentMethodForCheque", priority = 13, enabled = true)
    public void tc_0020_verifyPayMethodWorksForAllAccType () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        String TC09_paymentMethodName = runTimeVariables.get("TC_09_PAYMENT_METHOD_NAME");
        confPage.editPaymentMethodWithAllAccountTypeChecked("TC20_EditPaymentMethodForAllAcount",
                "epmaa", TC09_paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Payment Method Type", "updated successfully", TextComparators.contains);
        navPage.navigateToCustomersPage();
        String TC18_AccountName = runTimeVariables.get("TC_18_ACCOUNT_TYPE_NAME");
        String customerName = newCustomersPage.addCustomerWithMakePayment("TC20_VerifyPayment", "vp", TC18_AccountName);
        msgsPage.verifyDisplayedMessageText("Saved new customer", "successfully.", TextComparators.contains);
        newCustomersPage.validateUsersSavedTestData(customerName);
        newCustomersPage.clickMakePayment();
        paymentsPage.makePayment("TC20_VerifyPayment", "vp", TC09_paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Successfully processed", "new payment", TextComparators.contains);
        paymentsPage.validateSavedTestDataInPaymentsTable(customerName);
    }

    @Test(description = "TC 36 : Verify that user can add account information type meta fields with the account type.", dependsOnMethods = "tc_0009_addPaymentMethodForCheque", priority = 13, enabled = true)
    public void tc_0036_testAddInfoToAccountType () {

        setTestRailsId("11047243");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        confPage.selectAccountName(runTimeVariables.get("TC_18_ACCOUNT_TYPE_NAME"));
        String infoTypeName = confPage.addNewInformationToSelectedAccountType(AccountTypeInfo.DISABLE_CHECKBOX,
                "TC36_AddInfoToAccType", "aiat");
        msgsPage.verifyDisplayedMessageText("Account Information Type", "created successfully",
                TextComparators.contains);
        confPage.validatePeriodsSavedTestData(infoTypeName);
        confPage.verifyUIComponent();

        logger.debug("Verifying if account information type created successfully or not");
    }

    @Test(description = "TC 98: Verify that user is able to create an Order with "
            + "Customer Special Pricing and no special pricing for the product is applied on another customer",
            dependsOnMethods = "tc_0013_addAccountTypeWithCreditForThreePayMethod", enabled = true)
    public void tc_0098_createOrderWithSpecialNonSpecialPricing() throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.addPlugin("TC98_OrderSpecialPricing", "osp");

        confPage.selectConfiguration(PageConfigurationItems.PaymentMethod);
        String paymentMethodName = confPage.addPaymentMethodWithoutMetaFields(PaymentMethodField.ALL, "TC98_OrderSpecialPricing", "osp");
        System.out.println("paymentMethodName:"+paymentMethodName);
        msgsPage.verifyDisplayedMessageText("Payment Method Type ", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(paymentMethodName);
        confPage.selectConfiguration(PageConfigurationItems.AccountType);

        String accName = confPage.verifyPayMethodDefaultSelectedForAddingAccountType("TC98_OrderSpecialPricing", "osp", paymentMethodName);
        System.out.println("accName:"+accName);
        msgsPage.verifyDisplayedMessageText("Account Type", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(accName);
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.MetaFields);
        confPage.addMetaFieldInMetaFieldCategory("TC98_Add_MetaField_Address", "osp");
        confPage.selectConfiguration(PageConfigurationItems.MetaFields);
        confPage.addMetaFieldInMetaFieldCategory("TC98_Add_MetaField_Salary", "osp");
        confPage.selectConfiguration(PageConfigurationItems.MetaFields);
        confPage.addMetaFieldInMetaFieldCategory("TC98_Add_MetaField_Email", "osp");
        confPage.selectConfiguration(PageConfigurationItems.MetaFields);
        confPage.addMetaFieldInMetaFieldCategory("TC98_Add_MetaField_City", "osp");

        navPage.navigateToProductsPage();

        String categoryName = productsPage.addCategory("TC98_OrderSpecialPricing", "osp");
        System.out.println("categoryName:"+categoryName);
        msgsPage.verifyDisplayedMessageText("Saved new product category", "", TextComparators.contains);
        productsPage.validateAddedCategory(categoryName);
        productsPage.selectCategory(categoryName);

        String engDescription = productsPage.addProduct(AddProductField.FLATPRICE, "TC98_OrderSpecialPricing", "osp");
        System.out.println("engDescription:"+engDescription);
        propReader.updatePropertyInFile("TC_98_PRODUCT_NAME", engDescription, "testData");
        msgsPage.verifyDisplayedMessageText("Saved new product", "", TextComparators.contains);
        productsPage.validateAddedProduct(engDescription);
        navPage.navigateToCustomersPage();

        String customerLoginName = newCustomersPage.createCustomerWithMetafileds(
                "TC98_OrderSpecialPricing", "osp", pr.readTestData("TC_13_ACCOUNT_NAME"));
        System.out.println("customerLoginName:"+customerLoginName);
        propReader.updatePropertyInFile("TC_98_CUSTOMER_LOGIN_NAME_ONE", customerLoginName, "testData");
        newCustomersPage.setPaymentMethodAndDetails("TC98_OrderSpecialPricing", "osp", pr.readTestData("TC_07_PAYMENT_METHOD_NAME"));
        msgsPage.verifyDisplayedMessageText("Saved new customer", "successfully.", TextComparators.contains);
        newCustomersPage.validateUsersSavedTestData(customerLoginName);

        String secondCustomerLoginName = newCustomersPage.createCustomerWithMetafileds(
                "TC98_OrderSpecialPricing", "osp", pr.readTestData("TC_13_ACCOUNT_NAME"));
        System.out.println("secondCustomerLoginName:"+secondCustomerLoginName);

        newCustomersPage.setPaymentMethodAndDetails("TC98_OrderSpecialPricing", "osp", pr.readTestData("TC_07_PAYMENT_METHOD_NAME"));
        msgsPage.verifyDisplayedMessageText("Saved new customer", "successfully.", TextComparators.contains);
        newCustomersPage.validateUsersSavedTestData(customerLoginName);
        newCustomersPage.setResetPriceForCustomer("set_Price", "TC98_OrderSpecialPricing", "osp", customerLoginName, categoryName, engDescription);
        msgsPage.verifyDisplayedMessageText("Saved new customer price for product", "successfully.", TextComparators.contains);

        navPage.navigateToCustomersPage();
        newCustomersPage.createOrderAfterPriceSet("Price_Without_Period", "TC98_OrderSpecialPricing", "osp", customerLoginName, categoryName, engDescription);
        msgsPage.verifyDisplayedMessageText("Created new order", "successfully.", TextComparators.contains);

        navPage.navigateToCustomersPage();
        newCustomersPage.createOrderAfterPriceSet("Price_With_Period", "TC98_setPricing", "osp", secondCustomerLoginName, categoryName, engDescription);
        msgsPage.verifyDisplayedMessageText("Created new order", "successfully.", TextComparators.contains);

        navPage.navigateToCustomersPage();
        newCustomersPage.setResetPriceForCustomer("reset_Price", "TC98_OrderSpecialPricing", "osp", customerLoginName, categoryName, engDescription);

        navPage.navigateToCustomersPage();
        newCustomersPage.createOrderAfterPriceSet("Price_With_Period", "TC98_UpdateOrder", "osp", secondCustomerLoginName, categoryName, engDescription);
    }

    @Test(description = "TC 99: Verify no special pricing for product at Inspect customer screen is applied beyond expiry date.",
            dependsOnMethods = "tc_0098_createOrderWithSpecialNonSpecialPricing", enabled = true)
    public void tc_0099_noSpecialPricingBeyondExpiryAtInspectCustomer() throws IOException {

        setTestRailsId("");

        navPage.navigateToCustomersPage();
        newCustomersPage.createOrderAfterPriceSet("Order_With_Date", "TC99_NoSpecialPricing", "osp",
                pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE"), "", pr.readTestData("TC_98_PRODUCT_NAME"));
        ordersPage.verifyAppliedTotalOnOrderFirstLine();
        msgsPage.verifyDisplayedMessageText("Created new order", "successfully.", TextComparators.contains);
    }

    @Test(description = "TC 126: Verify that user is able to generate invoice.", dependsOnMethods="tc_0098_createOrderWithSpecialNonSpecialPricing", enabled = true)
    public void tc_0126_userCanGenerateInvoice() throws IOException {

        setTestRailsId("");

        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE"));
        newCustomersPage.createOrderWithProduct(this.pr.readTestData("TC_98_PRODUCT_NAME"));

        msgsPage.verifyDisplayedMessageText("Created new order", "successfully.", TextComparators.contains);
        ordersPage.selectOrderByCustomerName(this.pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE"));
        ordersPage.clickGenerateInvoice();
        msgsPage.verifyDisplayedMessageText("Successfully generated invoice for Order ", "", TextComparators.contains);
        navPage.navigateToOrdersPage();
        ordersPage.selectOrderByCustomerName(this.pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE"));
        ordersPage.verifyAppliedTotalOnOrderFirstLine();
    }

    @Test(description = "TC 155: Verify Proper validation is displayed if user uses invalid credentials for making Payments.", dependsOnMethods="tc_0126_userCanGenerateInvoice")
    public void tc_0155_validationAppearsForInvalidPaymentAmount() throws IOException {

        setTestRailsId("");

        navPage.navigateToInvoicesPage();
        String customerName = pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE");
        String amount = TestData.read("PageInvoice.xml", "tc_155_PaymentWithInvalid", "paymentAmount", "pwi");
        invoicePage.clickOnGeneratedInvoice(customerName);
        invoicePage.clickPayInvoice();
        paymentsPage.verifyErrorOnInvalidCredentials(customerName, amount);
    }

    @Test(description = "TC 156: Verify that user is able to make Payments through Cheque.", dependsOnMethods = "tc_0126_userCanGenerateInvoice")
    public void tc_0156_makePaymentThroughCheque() throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Plugins);
        confPage.editPlugin("TC156_MakeChequePayment", "cp");
        msgsPage.verifyDisplayedMessageText("The plug-in", "has been updated.", TextComparators.contains);
        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Notification);

        String notificationCategory = TestData.read("PageConfiguration.xml", "TC156_MakeChequePayment", "notificationCategory", "cp");
        confPage.clickPaymentNotification(notificationCategory);

        String notification = TestData.read("PageConfiguration.xml", "TC156_MakeChequePayment", "notification", "cp");
        confPage.selectNotification(notification);
        confPage.clickEditButton();

        String subject = TestData.read("PageConfiguration.xml", "TC156_MakeChequePayment", "subject", "cp");
        confPage.setSubject(subject);

        String bodyText = TestData.read("PageConfiguration.xml", "TC156_MakeChequePayment", "bodyText", "cp");
        confPage.setBodyTextNotification(bodyText);
        confPage.clickSaveChangesButton();
        navPage.navigateToInvoicesPage();
        invoicePage.clickOnGeneratedInvoice(pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE"));
        invoicePage.clickPayInvoice();
        paymentsPage.makePayment(MakePayment.UNCHANGEDPAYEMENT, "TC156_MakeChequePayment", "cp", pr.readTestData("TC_09_PAYMENT_METHOD_NAME"));
        msgsPage.verifyDisplayedMessageText("Successfully processed", "new payment", TextComparators.contains);
    }

    @Test(description = "TC 158: Verify that user is able to make Payments through credit Card.", dependsOnMethods = "tc_0126_userCanGenerateInvoice")
    public void tc_0158_makePaymentThroughCreditCard() throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Notification);

        String notificationCategory = TestData.read("PageConfiguration.xml", "TC158_MakeCreditCardPayment", "notificationCategory", "ccp");
        confPage.clickPaymentNotification(notificationCategory);

        String notification = TestData.read("PageConfiguration.xml", "TC158_MakeCreditCardPayment", "notification", "ccp");
        confPage.selectNotification(notification);
        confPage.clickEditButton();
        confPage.checkActiveCheckBoxChecked();
        confPage.clickSaveChangesButton();
        navPage.navigateToInvoicesPage();
        invoicePage.clickOnGeneratedInvoice(pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE"));
        invoicePage.clickPayInvoice();
        paymentsPage.makePayment(MakePayment.UN_CHANGED_PAYEMENT_CARD, "TC158_MakeCreditCardPayment", "ccp", pr.readTestData("TC_07_PAYMENT_METHOD_NAME"));
        msgsPage.verifyDisplayedMessageText("Successfully processed", "new payment", TextComparators.contains);
    }

    @Test(description = "TC 160: Verify Payment status 'Failed' when user uses invalid credit card credentials", dependsOnMethods = "tc_0126_userCanGenerateInvoice", enabled = true)
    public void tc_0160_checkFailedPaymentForInvalidCardDetails() throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.Notification);

        String notificationCategory = TestData.read("PageConfiguration.xml", "TC160_MakeWrongCreditCardPayment", "notificationCategory", "ccp");
        confPage.clickPaymentNotification(notificationCategory);

        String notification = TestData.read("PageConfiguration.xml", "TC160_MakeWrongCreditCardPayment", "notification", "ccp");
        confPage.selectNotification(notification);
        confPage.clickEditButton();

        boolean active = TextUtilities.compareValue( TestData.read("PageConfiguration.xml", "TC160_MakeWrongCreditCardPayment", "active", "ccp"), "true", true, TextComparators.equals);
        confPage.checkUncheckActiveCheckbox(active);
        confPage.clickSaveChangesButton();
        navPage.navigateToInvoicesPage();
        invoicePage.clickOnGeneratedInvoice(this.pr.readTestData("TC_98_CUSTOMER_LOGIN_NAME_ONE"));
        invoicePage.clickPayInvoice();
        paymentsPage.makePayment(MakePayment.PAYEMENT_CARD_REAL_TIME, "TC160_MakeWrongCreditCardPayment", "ccp", pr.readTestData("TC_07_PAYMENT_METHOD_NAME"));
        msgsPage.assertTextInFirstErrorMessage("Failed to process new payment", TextComparators.contains);
    }
}
