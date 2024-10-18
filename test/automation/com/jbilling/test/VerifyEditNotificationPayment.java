package com.jbilling.test;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyEditNotificationPayment extends BrowserApp {

    @Test(description = "TC_168 Verify that email medium type option is displayed on Edit notification page", priority = 1)
    public void tc_0168_editNotification () {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Notification);
        confPage.clickPaymentNotification("Payments");
        confPage.createPaymentNotification("NotificationPayment", "notpay");
        msgsPage.verifyDisplayedMessageText("Notification Saved", "Successfully", TextComparators.contains);
        confPage.setEditAndVerifyValuePresent("NotificationEditPayment", "notf");
    }
    
    @Test(description = "TC_169 Verify that user is able to edit the notification subject.", priority = 2)
    public void tc_0169_editNotificationSubject() {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Notification);
        confPage.clickPaymentNotification("Payments");
        confPage.clickNewCreatedPayment();

        String subvalue = confPage.setEditAndPaymentSave("notificationEditPaymentTwo", "notTwo");

        confPage.validateEditPaymentSavedTestData(subvalue);
    }
    
    @Test(description = "TC_170 Verify that notification sent to the user, configured to sent notification.", priority = 3)
    public void tc_0170_verifyCheckbox() {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(GlobalEnumsPage.PageConfigurationItems.Notification);
        confPage.clickPaymentNotification("Payments");
        confPage.clickNewCreatedPayment();
        confPage.clickEditPaymentNotification();
        confPage.notificationCheckBox("notificationCheckbox", "chk_Box");
        confPage.validateValueNotification("verifyCheckbox", "chk_Value");
    }
}
