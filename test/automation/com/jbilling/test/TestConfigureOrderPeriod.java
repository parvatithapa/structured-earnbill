package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class TestConfigureOrderPeriod extends BrowserApp {

    @Test(description = "Test Case 2.4 : Verify ability to configure Order Periods")
    public void testConfigureOrderPeriod () throws IOException {

        setTestRailsId("11047245");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.OrderPeriods);
        String orderPeriod1 = confPage.createNewOrderPeriod("firstOrderPeriod", "op");
        msgsPage.verifyDisplayedMessageText("Order Period", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(orderPeriod1);
        confPage.verifyUIComponent();
        String orderPeriod2 = confPage.createNewOrderPeriod("secondOrderPeriod", "op");
        msgsPage.verifyDisplayedMessageText("Order Period", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(orderPeriod2);
        confPage.verifyUIComponent();
        String orderPeriod3 = confPage.createNewOrderPeriod("thirdOrderPeriod", "op");
        msgsPage.verifyDisplayedMessageText("Order Period", "created successfully", TextComparators.contains);
        confPage.validatePeriodsSavedTestData(orderPeriod3);
        confPage.verifyUIComponent();
    }
}
