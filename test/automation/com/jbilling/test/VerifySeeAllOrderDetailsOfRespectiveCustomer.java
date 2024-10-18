package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddPlanField;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifySeeAllOrderDetailsOfRespectiveCustomer extends BrowserApp {

    @Test(description = "TC 97 : Verify that user is able to see all Order details of Respective customer")
    public void tc_0097_verifyUserIsAbleToSeeAllOrderOfRespectiveCustomer () {

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
        String planNameWithBundle = plansPage.addPlanMonthly(AddPlanField.BUNDLEDPERIOD, categoryName, description2,
                "withBundle", "wb");
        navPage.navigateToPlanPage();
        String planNameWithoutBundle = plansPage.addPlanMonthly(AddPlanField.BUNDLEDPERIOD, categoryName, description2,
                "withOutBundle", "wob");
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("paymentTypeWithPaymentCard", "pt");
        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");
        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.addCustomerWithMakePayment("customerCreate", "cc", accountType);
        newCustomersPage.createOrderBundleAndWithoutBundle("createOrderMonthlyPrepaid", "comp", planNameWithBundle,
                planNameWithoutBundle);

        ordersPage.verifyAppliedTotalOnOrderFirstLine();
        navPage.navigateToCustomersPage();

        newCustomersPage.verifyShowAllOrder(customerName);

        navPage.navigateToConfigurationPage();
        confPage.ClickOnCurrencyLink();
        confPage.resetCurrecncy("currencyName", "cn", defaultCurrency);
    }
}
