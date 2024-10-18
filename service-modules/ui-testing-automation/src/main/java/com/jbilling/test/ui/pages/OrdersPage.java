package com.jbilling.test.ui.pages;

import java.math.BigDecimal;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.TextBlock;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.TextInput;

public class OrdersPage extends AppPage {

    @FindBy(xpath = "//span[text()='Edit this Order']")
    private Link               linkEditOrder;

    @FindBy(xpath = "//table[@id='orders']/tbody//td[2]/a/strong")
    private List<WebElement>   listOrdersCustomerColumn;

    @FindBy(xpath = "//a[text()='Line Changes']")
    private Link               linkLineChanges;

    @FindBy(xpath = "//a[contains(@class,'submit')]//span[text()='Provisioning']")
    private Link               linkProvisioning;

    @FindBy(xpath = "//a[@class='submit order']/span[text()='Generate Invoice']")
    private Link               linkGenerateInvoice;

    // 'Orders' -> (when line selected in left column [order list]) -> right column -> first order -> 'Order ddd'
    @FindBy(xpath = "(//td[text()='Active Since:']/following::td[1])[1]")
    private TextBlock          inputMainOrderActiveSince;

    @FindBy(xpath = "(//td[text()='Active Since:']/following::td[1])[2]")
    private TextBlock          inputSubOrderActiveSince;

    // 'Orders' -> (when line selected in left column [order list]) -> right column -> first order -> 'Lines' -> first
    // line
    @FindBy(xpath = "(//table[@class='innerTable'])/tbody/tr/td[1]")
    private TextBlock          textOrderFirstLineProductId;

    @FindBy(xpath = "(//table[@class='innerTable'])/tbody/tr/td[2]")
    private TextBlock          textOrderFirstLineDescription;

    @FindBy(xpath = "(//table[@class='innerTable'])/tbody/tr/td[3]")
    private TextBlock          textOrderFirstLineQuantity;

    @FindBy(xpath = "(//table[@class='innerTable'])/tbody/tr/td[4]")
    private TextBlock          textOrderFirstLinePrice;

    @FindBy(xpath = "(//table[@class='innerTable'])/tbody/tr/td[5]")
    private TextBlock          textOrderFirstLineTotal;

    @Autowired
    protected OrderBuilderPage orderBuilderPage;
    @Autowired
    protected MessagesPage messagesPage;

    public OrdersPage(WebDriver driver) {
        super(driver);
    }

    public void editCreatedOrder (String testDataSetName, String category, String customerName) {
        String period = TestData.read("PageOrders.xml", testDataSetName, "period", category);
        String type = TestData.read("PageOrders.xml", testDataSetName, "type", category);

        editFirstOrder(customerName);

        orderBuilderPage.withOrderPeriod(period).withBillingType(type).saveChanges();

        messagesPage.isIntermediateSuccessMessageAppeared();
    }

    public void editFirstOrder (String customerName) {
        selectListElementByText(listOrdersCustomerColumn, customerName); // selects first order for customer
        linkEditOrder.click();
    }

    public void verifyAppliedTotalOnOrderFirstLine () {
        BigDecimal quantity = getFirstLineQuantity();
        BigDecimal price = getFirstLinePrice();

        Assert.assertEquals(quantity.multiply(price).setScale(3), getFirstLineTotal());
    }

    public BigDecimal getFirstLineQuantity () {
        return asDecimal(textOrderFirstLineQuantity);
    }

    public BigDecimal getFirstLinePrice () {
        return asDecimal(textOrderFirstLinePrice);
    }

    public BigDecimal getFirstLineTotal () {
        return asDecimal(textOrderFirstLineTotal);
    }

    public void verifySubOrderActiveSinceDate (String customerName) {
        String parentOrderActiveDate = inputMainOrderActiveSince.getText();
//        selectListElementByText(listOrdersCustomerColumn, customerName);
        String childOrderActiveDate = inputSubOrderActiveSince.getText();
        Assert.assertTrue(parentOrderActiveDate.equals(childOrderActiveDate));
    }

    public void clickProvisioningButton () {
        linkProvisioning.click();
    }

    public void clickGenerateInvoice () {
        linkGenerateInvoice.click();
    }

    public void verifyInvoiceGenerationForChoosenCustomer (String customerName) {
        selectOrderByCustomerName(customerName);
        clickGenerateInvoice();
        String msg = isInvoiceGenerationSuccessful();
        if (msg != null) {
            throw new RuntimeException("Invoice has not been generated successfully. Message: " + msg);
        }
    }

    public void selectOrderByCustomerName (String name) {
        selectListElementByText(listOrdersCustomerColumn, name);
    }

    public void verifyOrdersTableIsDisplayed () {
        Assert.assertTrue(driver.findElement(By.id("orders")).isDisplayed());
    }

    private String isInvoiceGenerationSuccessful () {
        String msgToVerify = "successfully generated invoice for order";
        return messagesPage.isOperationSuccessfulOnMessage(msgToVerify, TextComparators.contains);
    }
}
