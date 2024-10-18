package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCustomerCCNumberMasked extends BrowserApp {

    private String newAccountName;
    private String newCustomerName;

    @Test(description = "TC 12.13: Verify a Customer CC number is masked on customer 'Details page' , 'Edit Customer' page and 'Blacklist' page.", priority = 1)
    public void tc_0030_verifyCustomerCCNumberMasked () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();

        String paymentName = confPage.addPaymentMethod("NewPaymentTypeMethod", "ocs", true, true);
        navPage.navigateToConfigurationPage();
        confPage.setConfigurationPreference("setPreferenceValue", "pc");

        newAccountName = confPage.newAccountTypeMethod("CreateNewAccountType", "atcl", paymentName);
        navPage.navigateToCustomersPage();
        newCustomerName = newCustomersPage.addCustomerWithMakePayment("CreateCustomer_C", "vp", newAccountName);

//        String paymentName = "Credit cardBICLV";
//        newAccountName = "BASIC02QUONG";
//        newCustomerName = "Cust_COUBWE";
        System.out.println("paymentName:"+paymentName);
        System.out.println("newAccountName:"+newAccountName);
        System.out.println("newCustomerName:"+newCustomerName);
        newCustomersPage.editCustomerWithPaymentMethod("EditCustomerWithPaymentMethod", "vp", newCustomerName, paymentName);
        navPage.navigateToConfigurationPage();
        confPage.verifyBlackListedCustomerWithDetail(newCustomerName);
    }

    @Test(description = " TC 33: Verify that Proper validation is displayed on deleting the Account type used in any customer.", dependsOnMethods = "tc_0030_verifyCustomerCCNumberMasked", priority = 2)
    public void tc_0059_verifyValidationDisplayedOnDeletingAccountType () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.deleteAccountTypeUsedMethod(newAccountName);
    }

    @Test(description = " TC 28.1: Verify that user is able to import the meta field as AIT which is created under account type at meta field.", dependsOnMethods = "tc_0059_verifyValidationDisplayedOnDeletingAccountType", priority = 3)
    public void tc_0073_testUserAbleToImportMetaFieldAsAITUnderAccountTypeMeta () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        String accountType = confPage.addNewAccountTypeInMetaField("addNewAccountTypeInMetaField", "aitc");
        confPage.importMetafieldsUsingAITWithACCTYPE(newAccountName, accountType);
    }

    @Test(description = " TC 37: Verify Correct validation should be displayed if user uses invalid input.", dependsOnMethods = "tc_0073_testUserAbleToImportMetaFieldAsAITUnderAccountTypeMeta", priority = 4)
    public void tc_0086_testCorrectValidationUserUsesInvalidInput () throws IOException {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String addedCategory = productsPage.addCategorywithGlobal("CreateProductCategoryGlobal", "pcd", true);

        String Product1 = productsPage.addProduct(AddProductField.LINEPERCENTAGEWITHCOMPANY,
                "addProductWithLinePercentage", "ap");
        navPage.navigateToCustomersPage();
        newCustomersPage.selectCustomer(newCustomerName);
        newCustomersPage.createOrderWithInvalidData(Product1);
    }
}
