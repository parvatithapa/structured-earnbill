package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddPlanField;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateMonthlyPrepaidOrder extends BrowserApp {

    @Test(description = "TC 93 : Verify that user is able to Create 'Monthly Pre-paid' Order")
    public void tc_0093_verifyUserAbleToCreateMonthlyPostpaidOrder () {

        setTestRailsId("");

        navPage.navigateToProductsPage();
        String categoryName = productsPage.addCategory("productCategory", "pcat");
        navPage.navigateToProductsPage();
        String description2 = productsPage.addProducts(AddProductField.FLAT, "addProductThreeToAddDependencies", "ap");
        navPage.navigateToPlanPage();
        String planName = plansPage.addPlanMonthly(AddPlanField.BUNDLEDPERIOD, categoryName, description2,
                "monthlyPrePaid", "mpp");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("paymentTypeWithPaymentCard", "pt");
        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");
        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.addCustomerWithMakePayment("customerCreate", "cc", accountType);
        newCustomersPage.createOrderMonthly("createOrderMonthlyPrepaid", "comp", planName);

        // ordersPage.verifyAppliedTotalOnOrder();
    }
}
