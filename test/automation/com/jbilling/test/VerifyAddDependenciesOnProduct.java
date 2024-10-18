package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.globals.GlobalEnumsPage.AddPlanField;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.globals.GlobalEnumsPage.PaymentMethodField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

import java.io.IOException;
import java.util.*;

public class VerifyAddDependenciesOnProduct extends BrowserApp {

    static String category          = "";
    static String description2      = "";
    static String paymentMethodName = "";
    static String accountName       = "";
    static String customerName      = "";
    static String description1      = "";
    static String secondCategory    = "";
    static String orderPeriod1      = "";
    static String description       = "";
    static String id                = "";
    static String id1               = "";

    private static final String TEST_DATA_FILE = "testData";
    private static final String CATEGORY = "VerifyAddDependenciesOnProduct_category";
    private static final String DESCRIPTION2 = "VerifyAddDependenciesOnProduct_description2";
    private static final String PAYMENT_METHOD_NAME = "VerifyAddDependenciesOnProduct_paymentMethodName";
    private static final String ACCOUNT_NAME = "VerifyAddDependenciesOnProduct_accountName";
    private static final String CUSTOMER_NAME = "VerifyAddDependenciesOnProduct_customerName";
    private static final String DESCRIPTION1 = "VerifyAddDependenciesOnProduct_description1";
    private static final String SECOND_CATEGORY = "VerifyAddDependenciesOnProduct_secondCategory";
    private static final String ORDER_PERIOD1 = "VerifyAddDependenciesOnProduct_orderPeriod1";
    private static final String DESCRIPTION = "VerifyAddDependenciesOnProduct_description";
    private static final String ID = "VerifyAddDependenciesOnProduct_id";
    private static final String ID1 = "VerifyAddDependenciesOnProduct_id1";

    /*
    Usefull when testing locally
     */
    //@BeforeClass
    public void loadPropertiesFile() throws IOException {
        Properties props = propReader.readAllProperties(TEST_DATA_FILE+".properties");
        category = props.getProperty(CATEGORY);
        description2 = props.getProperty(DESCRIPTION2);
        paymentMethodName = props.getProperty(PAYMENT_METHOD_NAME);
        accountName = props.getProperty(ACCOUNT_NAME);
        customerName = props.getProperty(CUSTOMER_NAME);
        description1 = props.getProperty(DESCRIPTION1);
        secondCategory = props.getProperty(SECOND_CATEGORY);
        orderPeriod1 = props.getProperty(ORDER_PERIOD1);
        id = props.getProperty(ID);
        id1 = props.getProperty(ID1);
        description = props.getProperty(DESCRIPTION);
    }

    @Test(description = "TC 43 : Verify user is able to add mandatory and optional dependencies on product.", priority = 1, enabled = true)
    public void tc_0043_verifyUserAbleToAddDependencies () throws IOException {

        setTestRailsId("");


        navPage.navigateToProductsPage();
        category = productsPage.addCategory("productCategory", "pcat");
        propReader.updatePropertyInFile(CATEGORY, category, TEST_DATA_FILE);
        confPage.validateCategoriesSavedTestData(category);
        description = productsPage.addProduct(AddProductField.GRADUATED, "addProductOneToAddDependencies", "ap");
        propReader.updatePropertyInFile(DESCRIPTION, description, TEST_DATA_FILE);
        id = productsPage.getIDOfAddedProduct();
        propReader.updatePropertyInFile(ID, id, TEST_DATA_FILE);
        navPage.navigateToProductsPage();
        secondCategory = productsPage.addCategory("NewProductCategoryData", "pcd");
        propReader.updatePropertyInFile(SECOND_CATEGORY, secondCategory, TEST_DATA_FILE);
        confPage.validateCategoriesSavedTestData(secondCategory);
        description1 = productsPage.addProduct(AddProductField.FLAT, "addProductTwoToAddDependencies", "ap");
        propReader.updatePropertyInFile(DESCRIPTION1, description1, TEST_DATA_FILE);
        id1 = productsPage.getIDOfAddedProduct();
        propReader.updatePropertyInFile(ID1, id1, TEST_DATA_FILE);
        navPage.navigateToProductsPage();
        description2 = productsPage.addProduct(AddProductField.FLAT, "addProductThreeToAddDependencies", "ap");
        propReader.updatePropertyInFile(DESCRIPTION2, description2, TEST_DATA_FILE);
        productsPage.getIDOfAddedProduct();

        navPage.navigateToProductsPage();

        String productToSelect1 = id1 + " : " + description1;
        String productToSelect = id + " : " + description;

        productsPage.editDependencyInProduct("editFirstDependency", "efd", secondCategory, description2,
                secondCategory, productToSelect1, category, productToSelect);

        productsPage.validateDependencySavedTestData(description1);
        productsPage.validateDependencySavedTestData(description);

        if (productsPage.isProductPresentInTheDropdownForDependency(secondCategory, productToSelect1) == true) {
            throw new RuntimeException("Test 43 failed as Product on which dependency is applied is still Present ");
        }

        if (productsPage.isProductPresentInTheDropdownForDependency(category, productToSelect) == true) {
            throw new RuntimeException("Test 43 failed as Product on which dependency is applied is still Present ");
        }
    }

    @Test(description = "TC 62.1 : Create/Edit Plans in System", priority = 2)
    public void tc_0062_1_addOrderPeriods () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.OrderPeriods);
        orderPeriod1 = confPage.createNewOrderPeriod("yearlyOrderPeriod", "op");
        propReader.updatePropertyInFile(ORDER_PERIOD1, orderPeriod1, TEST_DATA_FILE);
        msgsPage.verifyDisplayedMessageText("Order Period", "created successfully", TextComparators.contains);
    }

    @Test(description = "TC 62.2 : Create/Edit Plans in System", dependsOnMethods = {
            "tc_0043_verifyUserAbleToAddDependencies", "tc_0062_1_addOrderPeriods" }, priority = 3)
    public void tc_0062_2_addPlans () {

        setTestRailsId("");

        navPage.navigateToPlanPage();
        plansPage.addPlan(AddPlanField.ALL, category, description2, "addplan62.2", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
    }

    @Test(description = "TC 62.3 : Create/Edit Plans in System", dependsOnMethods = {
            "tc_0043_verifyUserAbleToAddDependencies", "tc_0062_1_addOrderPeriods" }, priority = 4)
    public void tc_0062_3_addPlans () {

        setTestRailsId("");

        navPage.navigateToPlanPage();
        plansPage.addPlan(AddPlanField.PRODUCT, category, description2, "addplan62.3", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
    }

    @Test(description = "TC 62.3.i : Create/Edit Plans in System", dependsOnMethods = {
            "tc_0043_verifyUserAbleToAddDependencies", "tc_0062_1_addOrderPeriods" }, priority = 5)
    public void tc_0062_3_1_addPlans () {

        setTestRailsId("");

        navPage.navigateToPlanPage();
        plansPage.addPlan(AddPlanField.BUNDLEDPERIOD, category, description2, "addplan62.3.1", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
    }

    @Test(description = "TC 62.4 : Create/Edit Plans in System", dependsOnMethods = {
            "tc_0043_verifyUserAbleToAddDependencies", "tc_0062_1_addOrderPeriods" }, priority = 6)
    public void tc_0062_4_addPlans () {

        setTestRailsId("");

        navPage.navigateToPlanPage();
        plansPage.addPlan(AddPlanField.PLANPERIOD, category, description2, orderPeriod1, "TC_62.4_addplan", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
    }

    @Test(description = "TC 62.5 : Create/Edit Plans in System", dependsOnMethods = {
            "tc_0043_verifyUserAbleToAddDependencies", "tc_0062_1_addOrderPeriods" }, priority = 7)
    public void tc_0062_5_addPlans () {

        setTestRailsId("");

        navPage.navigateToPlanPage();
        plansPage.addPlan(AddPlanField.MULTIPLEPLAN, category, description2, orderPeriod1, "TC_62.5_addplan", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
    }

    @Test(description = "TC 62.6 : Create/Edit Plans in System", dependsOnMethods = {
            "tc_0043_verifyUserAbleToAddDependencies", "tc_0062_1_addOrderPeriods" }, priority = 8)
    public void tc_62_6_addPlans () {

        setTestRailsId("");

        navPage.navigateToPlanPage();
        plansPage.addPlan(AddPlanField.WITHNOTE, category, description2, orderPeriod1, "TC_62.6_addplan_a", "ap");
        navPage.navigateToPlanPage();
        plansPage.addPlan(AddPlanField.WITHNOTE, category, description2, "TC_62.6_addplan_b", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
    }

    @Test(description = "TC 102 : Verify that orders with mandatory and optional dependency gets created.", dependsOnMethods = { "tc_0043_verifyUserAbleToAddDependencies" }, priority = 6)
    public void tc_102_verifyOrderesWithMandatoryAndOptionalDependency () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.PaymentMethod);
        paymentMethodName = paymentsPage.addPaymentMethodWithoutMetaFields(PaymentMethodField.ALL,
                "TC102_addPaymentMethod", "apm");
        msgsPage.verifyDisplayedMessageText("Payment Method Type ", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(paymentMethodName);
        confPage.selectConfiguration(PageConfigurationItems.AccountType);
        accountName = accountTypePage.createAccountTypeWithCreditDetails("TC102_AddAccountType", "aat",
                paymentMethodName);


/*
        String accountName = "Account TypeOPBON";
        String paymentMethodName = "Card PayPZZOE";
        String description1 = "Billing Flat First";
        String description2 = "Billing Flat Second";
*/

        navPage.navigateToCustomersPage();
        System.out.println("accountName:"+accountName);
        System.out.println("paymentMethodName:"+paymentMethodName);
        System.out.println("description1:"+description1);
        System.out.println("description2:"+description2);
        customerName = newCustomersPage.addCustomer(accountName, paymentMethodName, "TC_102_NewCustomer", "nc");
        System.out.println("customerName:"+customerName);
        newCustomersPage.selectCustomer(customerName);
        newCustomersPage.addOrder(description2, description1, "TC_102_AddOrder", "ao");
    }
}
