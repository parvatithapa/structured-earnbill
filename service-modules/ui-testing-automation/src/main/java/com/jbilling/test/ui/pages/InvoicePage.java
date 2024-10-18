package com.jbilling.test.ui.pages;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.jbilling.test.ui.elements.Link;

public class InvoicePage extends WebPage {

    @FindBy(xpath = "//div[@class='btn-box']/div/a[contains(@class, 'submit') and contains(@class, 'payment')]/span[text()='Pay Invoice']/..")
    private Link             linkPayInvoice;

    @FindBy(xpath = "//table[@id='invoices']/tbody/tr/td/a/strong")
    private List<WebElement> listInvoicesData;

    public InvoicePage(WebDriver driver) {
        super(driver);
    }

    public void clickPayInvoice () {
        linkPayInvoice.click();
    }

    public void clickOnGeneratedInvoice (String customerName) {
        selectListElementByText(listInvoicesData, customerName);
        explicitWaitForJavascript();
    }
}
