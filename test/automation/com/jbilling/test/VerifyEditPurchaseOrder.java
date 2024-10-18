package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddPlanField;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyEditPurchaseOrder extends BrowserApp {

    @Test(description = "TC 95 : Verify that user is able to Edit Purchase Order")
    public void tc_0095_verifyUserIsAbleToEditPurchasedOrder () {

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

/*

        String defaultCurrency = "Euro";
        String categoryName = "Dependent ProductsOYCRS";
        String description2 = "Billing Flat Second";
        String planNameWithBundle = "Plan 4DWHKO";
        String planNameWithoutBundle = "Plan 4VMGRH";
        String methodName = "ForAll_RVFZL";
        String accountType = "TestAccountICUQU";
        String customerName = "testLoginYZVJX";
*/

        System.out.println("defaultCurrency:"+defaultCurrency);
        System.out.println("categoryName:"+categoryName);
        System.out.println("description2:"+description2);
        System.out.println("planNameWithBundle:"+planNameWithBundle);
        System.out.println("planNameWithoutBundle:"+planNameWithoutBundle);
        System.out.println("methodName:"+methodName);
        System.out.println("accountType:"+accountType);
        System.out.println("customerName:"+customerName);

        newCustomersPage.createOrderBundleAndWithoutBundle("createOrderMonthlyPrepaid", "comp", planNameWithBundle,
                planNameWithoutBundle);

        ordersPage.verifyAppliedTotalOnOrderFirstLine();
        ordersPage.editCreatedOrder("editOrder", "eo", customerName);

        navPage.navigateToConfigurationPage();
        confPage.ClickOnCurrencyLink();
        confPage.resetCurrecncy("currencyName", "cn", defaultCurrency);
    }
}
