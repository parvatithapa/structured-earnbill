package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddPlanField;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateMonthlyPostpaidOrder extends BrowserApp {

    @Test(description = "TC 90 : Verify that user is able to Create 'Monthly Post-paid' Order")
    public void tc_0090_verifyUserAbleToCreateMonthlyPostpaidOrder () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.ClickOnCurrencyLink();
        String defaultCurrency = confPage.getDefaultCurrencyValue();
        confPage.setCurrency("currencyName", "cn");

        navPage.navigateToProductsPage();
        String categoryName = productsPage.addCategory("productCategory", "pcat");
        navPage.navigateToProductsPage();
        String description2 = productsPage.addProducts(AddProductField.FLAT, "addProductThreeToAddDependencies", "ap");
        navPage.navigateToPlanPage();
        String planName = plansPage.addPlanMonthly(AddPlanField.BUNDLEDPERIOD, categoryName, description2,
                "addplan62.3.1", "ap");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("paymentTypeWithPaymentCard", "pt");
        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");
        navPage.navigateToCustomersPage();
        newCustomersPage.addCustomerWithMakePayment("customerCreate", "cc", accountType);
        newCustomersPage.createOrderMonthly("createOrderMonthlyPostpaid", "comp", planName);

        navPage.navigateToConfigurationPage();
        confPage.ClickOnCurrencyLink();
        confPage.resetCurrecncy("currencyName", "cn", defaultCurrency);
    }
}
