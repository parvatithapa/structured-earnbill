package com.jbilling.test.ui.pages;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;

public class ReportsPage extends AppPage {

    @FindBy(xpath = "//strong[text()='Invoice Reports']")
    private Link   linkReportType;

    @FindBy(xpath = "//strong[text() = 'Total Amount Invoiced']")
    private Link   linkReportName;

    @FindBy(xpath = "//div[@class = 'btn-box']/a/span[text()='Run Report']")
    private Link   linkRunReport;

    @FindBy(id = "period")
    private Select selectPeriodBreakdown;

    @FindBy(id = "format")
    private Select selectReportView;

    public ReportsPage(WebDriver driver) {
        super(driver);
    }

    public void getReportsView (String testDataSetName, String category) throws InterruptedException, AWTException {

        String period = TestData.read("PageReports.xml", testDataSetName, "period", category);
        String view = TestData.read("PageReports.xml", testDataSetName, "view", category);
        String view2 = TestData.read("PageReports.xml", testDataSetName, "view2", category);
        String view3 = TestData.read("PageReports.xml", testDataSetName, "view3", category);

        linkReportType.click();
        linkReportName.click();
        selectPeriodBreakdown.selectByVisibleText(period);
        selectReportView.selectByVisibleText(view);

        /*
         * TODO 2016-09-03 igor.poteryaev@jbilling.com: implement normal work with report windows
         */
        /*
         * switchToAndVerifyWindow(); selectReportView.selectByVisibleText(view2); closeWindowPopup();
         * selectReportView.selectByVisibleText(view3); closeWindowPopup();
         */
    }

    private void switchToAndVerifyWindow () throws InterruptedException {

        String savedWindowHandle = driver.getWindowHandle();
        try {
            // Perform the click operation that opens new window
            linkRunReport.click();
            Thread.sleep(5000);

            // Switch to new window opened
            Object[] handles = driver.getWindowHandles().toArray();
            if (handles.length > 1) {
                driver.switchTo().window(handles[handles.length - 1].toString());

                // Perform the actions on new window

                // Close the new window, if that window no more required
                // driver.close();
                Thread.sleep(5000);
            }
        } finally {
            driver.switchTo().window(savedWindowHandle);
        }

        // TODO: Continue with original browser (first window)
    }

    // TODO: refactor to remove dependency on AWT
    private void closeWindowPopup () throws AWTException {
        // Perform the click operation that opens window popup
        linkRunReport.click();

        // creating instance of Robot class (A java based utility)
        Robot rb = new Robot();
        // pressing keys with the help of keyPress and keyRelease events
        rb.keyPress(KeyEvent.VK_ENTER);
        rb.keyRelease(KeyEvent.VK_ENTER);
    }

}
