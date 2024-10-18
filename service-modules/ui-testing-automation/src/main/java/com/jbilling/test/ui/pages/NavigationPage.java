package com.jbilling.test.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import ru.yandex.qatools.htmlelements.element.Button;

import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;

public class NavigationPage extends AppPage {

    @FindBy(id = "header")
    WebElement     divHeader;

    @FindBy(id = "navRight")
    WebElement     ulNavRight;

    @FindBy(id = "impersonate")
    private Link   linkImpersonate;

    @FindBy(id = "impersonation-select")
    private Select selectChildEntity;

    @FindBy(id = "impersonation-button")
    private Button buttonImpersonate;

    @FindBy(xpath = "//a[@class='dissimulate']")
    private Link   linkNewChildCompanyName;

    @FindBy(xpath = "//div[@id='header']//a[@class='logout']")
    private Link   linkLogout;

    public NavigationPage(WebDriver driver) {
        super(driver);
    }

    public void logoutApplication () {
        if (isChildElementPresent(divHeader, By.id("navRight"))) {
            new Link(ulNavRight.findElement(By.xpath("./li/a[contains(@class, 'menuClick')]"))).click();
        }
        linkLogout.click();
    }

    public void navigateToCustomersPage () {
        navigateTo(Page.Customers);
    }

    public void navigateToAgentsPage () {
        navigateTo(Page.Agents);
    }

    public void navigateToInvoicesPage () {
        navigateTo(Page.Invoices);
    }

    public void navigateToOrdersPage () {
        navigateTo(Page.Orders);
    }

    //
    public void navigateToReportsPage () {
        navigateTo(Page.Reports);
    }

    public void navigateToDiscountsPage () {
        navigateTo(Page.Discounts);
    }

    public void navigateToProductsPage () {
        navigateTo(Page.Products);
        setPageSize50();
    }

    public void navigateToPlanPage () {
        navigateTo(Page.Plans);
    }

    public void navigateToConfigurationPage () {
        navigateTo(Page.Configuration);
    }

    // @formatter:off
    private final static By PageSize50Locator = By.xpath("//div[@id='column1']/div[@class='pager-box']/div[@class='row left']/a[text()='50']");
    // @formatter:on

    private void setPageSize50 () {
        if (isElementPresent(PageSize50Locator)) {
            driver.findElement(PageSize50Locator).click();
        }
    }

    public void switchToChildCompany (String companyName) {
        linkImpersonate.click();
        selectChildEntity.selectByVisibleText(companyName);
        buttonImpersonate.click();
    }

    public void verifySwitchedToChildCompany (String companyName) {
        if (!linkNewChildCompanyName.getText().contains(companyName)) {
            throw new RuntimeException("Verification failed for company switched to child company [" + companyName
                    + "]");
        }
    }

    public boolean isChildCompanyImpersonated (String companyName) {
        linkImpersonate.click();
        return selectChildEntity.hasOption(companyName);
    }

    public void switchToParentCompany () {
        linkNewChildCompanyName.click();
    }
}
