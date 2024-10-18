package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddPlanField;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateYearlyPostpaidOrder extends BrowserApp {

    @Test(description = "TC 92 : Verify that user is able to Create 'Yearly Post-paid' Order")
    public void tc_0092_verifyCreateMonthlyPostpaidOrder () throws java.io.IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        String orderPeriod = confPage.createOrderPeriod("orderPeriod_Year", "opy");
        navPage.navigateToProductsPage();
        String categoryName = productsPage.addCategory("productCategory", "pcat");
        navPage.navigateToProductsPage();
        String description2 = productsPage.addProducts(AddProductField.FLAT, "addProductThreeToAddDependencies", "ap");
        navPage.navigateToPlanPage();
        String planName = plansPage.addPlanYearly(AddPlanField.BUNDLEDPERIOD, categoryName, description2,
                "yearlyPostPaid", "ypp", orderPeriod);
        msgsPage.verifyDisplayedMessageText("Saved new plan", "successfully", TextComparators.contains);
        navPage.navigateToConfigurationPage();
        confPage.clickOnPaymentMethodLink();
        String methodName = confPage.addPaymentMethod("paymentTypeWithPaymentCard", "pt");
        confPage.clickOnAccountTypeLink();
        String accountType = confPage.createAccount(methodName, "accountCreate", "ac");
        navPage.navigateToCustomersPage();
        String customerName = newCustomersPage.addCustomerWithMakePayment("customerCreate", "cc", accountType);
        propReader.updatePropertyInFile("TC_92_CUSTOMER_NAME", customerName, "testData");
        newCustomersPage.createOrderYearly("createOrderYearlyPostpaid", "coyp", planName);

        // ordersPage.verifyAppliedTotalOnOrder();
    }
}
