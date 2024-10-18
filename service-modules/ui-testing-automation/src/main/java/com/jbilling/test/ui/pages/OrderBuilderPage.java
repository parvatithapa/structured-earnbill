package com.jbilling.test.ui.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import ru.yandex.qatools.htmlelements.element.CheckBox;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.TextBlock;
import ru.yandex.qatools.htmlelements.exceptions.HtmlElementsException;

import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;

public class OrderBuilderPage extends WebPage {

    static public class OrderDetailsForm extends HtmlElement {

        @FindBy(id = "orderPeriod")
        private Select           selectOrderPeriod;

        @FindBy(id = "billingTypeId")
        private Select           selectBillingType;

        @FindBy(id = "activeSince")
        private TextInput        inputActiveSince;

        @FindBy(id = "activeUntil")
        private TextInput        inputActiveUntil;

        @FindBy(id = "dueDateValue")
        private TextInput        inputDueDate;

        @FindBy(id = "cancellationMinimumPeriod")
        private TextInput        inputMinimumPeriod;

        @FindBy(xpath = "//li[@aria-controls='ui-tabs-products']/a")
        private Link             linkTabProducts;

        @FindBy(xpath = "//table[@id='products']/tbody//td/a/strong")
        private List<WebElement> listProducts;

        @FindBy(xpath = "//li[@aria-controls='ui-tabs-discounts']/a")
        private Link             linkTabDiscounts;

        @FindBy(xpath = "//li[@aria-controls='ui-tabs-details']/a")
        private Link             linkTabDetails;

        @FindBy(xpath = "//li[@aria-controls='ui-tabs-plans']/a")
        private Link             linkTabPlans;

        @FindBy(xpath = "//table[@id='plans']/tbody//td/a/strong")
        private List<WebElement> listPlans;
    }

    static public class DiscountLinesForm extends HtmlElement {
        @FindBy(id = "discountableItem.0.lineLevelDetails")
        private Select selectDiscountableItem;

        @FindBy(id = "discount.0.id")
        private Select selectDiscount;
    }

    static public class ReviewTabs extends HtmlElement {

        @FindBy(xpath = "//a[text()='Review']")
        private Link            linkReviewTab;

        @FindBy(xpath = "//a[text()='Line Changes']")
        private Link            linkLineChanges;

        @FindBy(xpath = "//div[@id = 'review-messages']/div/ul/li")
        HtmlElement             elementReviewMessages;

        @FindBy(xpath = "//label[text()='Apply Now']/preceding-sibling::input[@class='cb check']")
        private CheckBox        checkboxApplyNow;

        // @FindBy(xpath = "//label[text()='Effective Date']/../div/input")
        // private TextInput inputEffectiveDate;

        static private final By InputEffectiveDateLocator = By.xpath("//label[text()='Effective Date']/../div/input");

        private TextInput inputEffectiveDate () {
            return new TextInput(findElement(InputEffectiveDateLocator));
        }

        @FindBy(xpath = "//label[text()='Quantity']/../div/input")
        private TextInput inputQuantity;

        @FindBy(xpath = "//div[@class='total']")
        private TextBlock textboxTotal;

        @FindBy(xpath = "//a/span[text()='Save Changes']/..")
        private Link      linkSaveChanges;

        @FindBy(xpath = "//table[@id='editOrderChanges']//a/span[text()='Update']/..")
        private Link      linkUpdate;

        @FindBy(xpath = "//a/span[text()='Cancel']/..")
        private Link      linkCancel;

        @FindBy(xpath = "//a/span[text()='Remove']/..")
        private Link      linkRemove;

        @FindBy(xpath = "//a/span[text()='Dependency']/..")
        private Link      linkDependency;
    }

    static public class AddAssetsForm extends HtmlElement {

        @FindBy(name = "asset.select.0")
        private CheckBox checkboxFirstAsset;

        @FindBy(xpath = "//a/span[text()='Add Selected']/..")
        private Link     linkAddSelected;

        @FindBy(xpath = "//a/span[text()='Add to Order']/..")
        private Link     linkAddToOrder;
    }

    static public class DependencyDialog extends HtmlElement {

        @FindBy(xpath = "//table[contains(@id, 'dependencies-products-change_-')]/tbody/tr/td/a/strong")
        private List<WebElement> listDependentProducts;

        @FindBy(xpath = "//button/span[text()='New SubOrder']/..")
        private Link             linkSubOrder;
    }

    @FindBy(id = "order-details-form")
    private OrderDetailsForm  formOrderDetails;

    @FindBy(id = "discount-lines-form")
    private DiscountLinesForm formDiscountLines;

    @FindBy(xpath = "//form[@id='products-filter-form']//input[@id='filterBy']")
    private TextInput         inputFilterProductsBy;

    @FindBy(xpath = "//form[@id='plans-filter-form']//input[@id='filterBy']")
    private TextInput         inputFilterPlansBy;

    @FindBy(id = "review-tabs")
    private ReviewTabs        divReviewTabs;

    @FindBy(id = "add-assets-form-add")
    private AddAssetsForm     formAddAssets;

    @FindBy(xpath = "//div[contains(@class, 'ui-dialog') and contains(@aria-describedby, 'dependencies-dialog-change_-')]")
    private DependencyDialog  dialogDependency;

    public OrderBuilderPage(WebDriver driver) {
        super(driver);
    }

    public OrderBuilderPage withOrderPeriod (String period) {
        formOrderDetails.selectOrderPeriod.selectByVisibleText(period);
        return this;
    }

    public OrderBuilderPage withActiveSince (String activeSince) {
        formOrderDetails.inputActiveSince.setText(activeSince);
        return this;
    }

    public OrderBuilderPage withActiveUntil (String activeUntil) {
        formOrderDetails.inputActiveUntil.setText(activeUntil);
        return this;
    }

    public OrderBuilderPage withDetails () {
        formOrderDetails.linkTabDetails.click();
        return this;
    }

    public OrderBuilderPage withBillingType (String type) {
        formOrderDetails.selectBillingType.selectByVisibleText(type);
        return this;
    }

    public OrderBuilderPage withDueDate (String dueDate) {
        formOrderDetails.inputDueDate.setText(dueDate);
        return this;
    }

    public OrderBuilderPage withMinimumPeriod (String minimumPeriod) {
        formOrderDetails.inputMinimumPeriod.setText(minimumPeriod);
        return this;
    }

    public OrderBuilderPage withProduct (String product) {
        formOrderDetails.linkTabProducts.click();
        filterProducts(product);
        selectListElementByText(formOrderDetails.listProducts, product);
        explicitWaitForJavascript();
        return this;
    }

    public OrderBuilderPage withPlan (String plan) {
        formOrderDetails.linkTabPlans.click();
        filterPlans(plan);
        selectListElementByText(formOrderDetails.listPlans, plan);
        explicitWaitForJavascript();
        return this;
    }

    public OrderBuilderPage withEffectiveDate (String date) {
        explicitWaitForJavascript();
        TextInput inputEffectiveDate = divReviewTabs.inputEffectiveDate();
        inputEffectiveDate.setText(date);
        inputEffectiveDate.sendKeys(Keys.TAB);
        explicitWaitForJavascript();
        return this;
    }

    public OrderBuilderPage withQuantity (String quantity) {
        divReviewTabs.inputQuantity.setText(quantity);
        return update();
    }

    public OrderBuilderPage withProductAndFirstAsset (String product) {
        withProduct(product);
        formAddAssets.checkboxFirstAsset.set(true);
        formAddAssets.linkAddSelected.click();
        formAddAssets.linkAddToOrder.click();
        return update();
    }

    public OrderBuilderPage withDiscount (String discountableItem, String discount) {
        formOrderDetails.linkTabDiscounts.click();
        logger.info("discountableItem: " + discountableItem);
        formDiscountLines.selectDiscountableItem.selectByVisibleText(discountableItem);
        logger.info("discount: " + discount);
        formDiscountLines.selectDiscount.selectByVisibleText(discount);
        return this;
    }

    public OrderBuilderPage withDiscount (String discount) {
        formOrderDetails.linkTabDiscounts.click();
        formDiscountLines.selectDiscount.selectByVisibleText(discount);
        return this;
    }

    public OrderBuilderPage gotoReviewTab () {
        divReviewTabs.linkReviewTab.click();
        return this;
    }

    public OrderBuilderPage gotoLineChanges () {
        divReviewTabs.linkLineChanges.click();
        return this;
    }

    public OrderBuilderPage assertTotalEquals (String expected) {
        String actual = divReviewTabs.textboxTotal.getText();
        if (!actual.contains(expected)) {
            throw new AssertionError("Total value error. expected: " + expected + ", but actual: " + actual);
        }
        return this;
    }

    public OrderBuilderPage saveChanges () {
        divReviewTabs.linkSaveChanges.click();
        return this;
    }

    public OrderBuilderPage update () {
        divReviewTabs.linkUpdate.click();
        return this;
    }

    public OrderBuilderPage cancel () {
        divReviewTabs.linkCancel.click();
        return this;
    }

    public OrderBuilderPage remove () {
        divReviewTabs.linkRemove.click();
        return this;
    }

    public OrderBuilderPage withDependentProduct (String product) {
        divReviewTabs.linkDependency.click();
        explicitWaitForJavascript();
        try { Thread.sleep(1000); } catch (Exception e) {}
        String xpathLocation = "//table[contains(@id, 'dependencies-products-change_-')]//a/strong[contains(text(),'"+product+"')]/..";
        driver.findElement(By.xpath(xpathLocation)).click();

//        selectListElementByText(dialogDependency.listDependentProducts, product);
        dialogDependency.linkSubOrder.click();
        explicitWaitForJavascript();
        return this;
    }

    public OrderBuilderPage verifyErrorMessage (String expected) {
        String actual = divReviewTabs.elementReviewMessages.getText();
        if (!actual.contains(expected)) {
            throw new RuntimeException("Test Case failed. expected message: " + expected + " not in actual: " + actual);
        }
        return this;
    }

    public OrderBuilderPage verifyApplyNowCheckBox () {
        String applyNowType = divReviewTabs.checkboxApplyNow.getAttribute("type");
        if (!"checkbox".equals(applyNowType)) {
            throw new HtmlElementsException("Apply Now is " + applyNowType + ". It is not a check box");
        }
        return this;
    }

    private void filterProducts (String filter) {
        inputFilterProductsBy.setText(filter);
        inputFilterProductsBy.sendKeys(Keys.TAB);
        // wait for listProducts
        explicitWaitForJavascript();
    }

    private void filterPlans (String filter) {
        inputFilterPlansBy.setText(filter);
        inputFilterPlansBy.sendKeys(Keys.TAB);
        // wait for listProducts
        explicitWaitForJavascript();
    }
}
