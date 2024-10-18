package com.jbilling.test.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.TextBlock;

import com.jbilling.test.ui.elements.Link;

public class AppPage extends WebPage {

    public enum Page {
        // @formatter:off
        Customers, Agents, Invoices, Payments, Orders, Billing, Mediation, Provisioning,
        Reports, Discounts, Products, Plans, Credits, Configuration;
        // @formatter:on

        private final static By LinkExpandMenuLocator = By.xpath(".//a/span[text()='+']/..");

        public void navigateUsing (HtmlElement divNavigation) {
            Link pageLink = new Link(divNavigation.findElement(By.xpath(String.format(".//a/span[text()='%s']/..",
                    name()))));
            if (!pageLink.isDisplayed()) {
                new Link(divNavigation.findElement(LinkExpandMenuLocator)).click();
            }
            pageLink.click();
        }
    }

    public static class ColumnLeft extends HtmlElement {

        private static final String LinkAddSelector = "./div[@class='btn-box']/a[contains(@class, 'add')]";

        @FindBy(xpath = LinkAddSelector)
        private Link                linkAdd;

        public void clickAdd () {
            linkAdd.click();
        }
    }

    public static class ColumnRight extends HtmlElement {

        private static final String BtnBoxSelector = ".//div[@class='btn-box']";

        @FindBy(xpath = BtnBoxSelector)
        private HtmlElement         divBtnBox;

        public Link button (String title) {
            WebElement element = divBtnBox.findElement(By.xpath(String.format(".//a/span[text()='%s']/..", title)));
            return new Link(element);
        }
    }

    private static final String LEGACY_CURRENT_COMPANY_XPATH   = "(//div[@id='header']/ul[@class='top-nav']/li)[1]";
    private static final By     LEGACY_CURRENT_COMPANY_LOCATOR = By.xpath(LEGACY_CURRENT_COMPANY_XPATH);

    // jBilling
    @FindBy(xpath = LEGACY_CURRENT_COMPANY_XPATH)
    private TextBlock           spanCurrentCompanyLegacy;

    // AppBilling
    @FindBy(xpath = "((//ul[@id='navRight']/li)[2]/ul/li)[1]/span")
    private TextBlock           spanCurrentCompany;

    private static final By     BreadcrumbsLocator             = By.id("breadcrumbs");

    @FindBy(id = "breadcrumbs")
    private HtmlElement         divBreadcrumbs;

    @FindBy(id = "column1")
    private ColumnLeft          divColumnLeft;

    @FindBy(id = "column2")
    protected ColumnRight       divColumnRight;

    @FindBy(id = "navigation")
    private HtmlElement         mainMenu;

    @FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'save')]")
    protected Link              linkSave;

    public AppPage(WebDriver driver) {
        super(driver);
    }

    public void verifyUIComponent () {
        explicitWaitForJavascript();
        Assert.assertTrue(isElementPresent(BreadcrumbsLocator));
    }

    public void navigateTo (Page page) {
        page.navigateUsing(mainMenu);
    }

    public String currentCompanyName () {
        if (isElementPresent(LEGACY_CURRENT_COMPANY_LOCATOR)) {
            return spanCurrentCompanyLegacy.getText();
        }
        // N.B. span is invisible and getText doesn't work
        return spanCurrentCompany.getAttribute("textContent");
    }

    public void clickAdd () {
        divColumnLeft.clickAdd();
    }

    public void clickSave () {
        linkSave.click();
    }
}
