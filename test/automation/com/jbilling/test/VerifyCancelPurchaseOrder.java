package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCancelPurchaseOrder extends BrowserApp {

    @Test(description = "TC 97 : Verify that user is able to see all Order details of Respective customer")
    public void tc_0097_verifyUserIsAbleToCancelPurchasingOrder () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("paymentTypeWithPaymentCard", "pt");
        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");
        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.addCustomerWithMakePayment("customerCreate", "cc", accountType);
        newCustomersPage.createOrderForCancel("createOrderMonthlyPrepaid", "comp");
        ordersPage.verifyOrdersTableIsDisplayed();
    }
}
