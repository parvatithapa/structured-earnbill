package com.jbilling.test.ui.pages;

import java.util.List;

import com.jbilling.framework.globals.GlobalController;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.Table;

import com.jbilling.framework.globals.GlobalEnumsPage.AddPlanField;
import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;

public class PlansPage extends AppPage {

    @FindBy(id = "product.number")
    private TextInput        inputProductCode;

    @FindBy(id = "newDescriptionLanguage")
    private Select           selectDescriptionLanguage;

    @FindBy(xpath = "//a[@onclick='addNewProductDescription()']")
    private Link             linkAddNewProductDescription;

    @FindBy(id = "product.descriptions[0].content")
    private TextInput        inputDescription;

    @FindBy(id = "price.rateAsDecimal")
    private TextInput        inputPriceAsDecimal;

    @FindBy(id = "product.types")
    private Select           selectProductTypes;

    @FindBy(id = "company-select")
    private Select           selectCompany;

    @FindBy(xpath = "//li[@aria-controls='ui-tabs-products']/a")
    private Link             linkTabProducts;

    @FindBy(xpath = "//table[@id='products']/tbody//td/a/strong")
    private List<WebElement> listProducts;

    @FindBy(xpath = "//table[@id='plans']/tbody//td/a/strong")
    private List<WebElement> listPlans;

    @FindBy(xpath = "//table[@id='plans']/tbody/tr/td/a[1]")
    private Link             linkFirstPlan;

    @FindBy(id = "bundle.quantityAsDecimal")
    private TextInput        inputBundleQuantity;

    @FindBy(id = "model.0.rateAsDecimal")
    private TextInput        inputProductPriceAsDecimal;

    @FindBy(id = "price.precedence")
    private TextInput        inputProductPricePrecedence;

    @FindBy(id = "plan.description")
    private TextInput        inputPlanDescription;

    @FindBy(id = "bundle.periodId")
    private Select           selectBundlePeriod;

    @FindBy(id = "plan.periodId")
    private Select           selectPlanPeriod;

    @FindBy(id = "plan.usagePoolIds")
    private Select           selectFreeUsagePools;

    @FindBy(id = "price.currencyId")
    private Select           selectCurrencies;

    @FindBy(xpath = "//span[text()='Edit']")
    private Link             linkEdit;

    @FindBy(xpath = "//span[text()='Update']")
    private Link             linkUpdate;

    @FindBy(xpath = "//span[text()='Save Changes']")
    private Link             linkSaveChanges;

    @FindBy(xpath = "//span[contains(text(),'-')]/parent::li")
    private HtmlElement      liMetafieldTabActivator;

    @FindBy(xpath = "//span[contains(text(),'-')]/parent::li")
    private HtmlElement      spanMetafieldTab;

    @FindBy(xpath = "//div[@class='box-card-hold']/div/ul/li[1]")
    private HtmlElement      liCategoryName1;

    @FindBy(xpath = "//div[@class='box-card-hold']/div/ul/li[2]")
    private HtmlElement      liCategoryName2;

    @FindBy(id = "plans")
    private Table            tablePlans;

    // TODO. remove duplication (OrdersPage)
    @FindBy(id = "filterBy")
    private TextInput        inputFilterProductsBy;

    public PlansPage(WebDriver driver) {
        super(driver);
    }

    public void addPlan (AddPlanField addPlanField, String userCategory, String productCategory,
            String testDataSetName, String category) {

        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);
        String description = TestData.read("PagePlans.xml", testDataSetName, "description", category);
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);
        String rate = TestData.read("PagePlans.xml", testDataSetName, "rate", category);
        String bundleQuantity = TestData.read("PagePlans.xml", testDataSetName, "bundleQuantity", category);
        String rateProduct = TestData.read("PagePlans.xml", testDataSetName, "rateProduct", category);
        String bundledPeriod = TestData.read("PagePlans.xml", testDataSetName, "bundledPeriod", category);
        String note = TestData.read("PagePlans.xml", testDataSetName, "note", category);
        String precedence = TestData.read("PagePlans.xml", testDataSetName, "precedence", category);

        clickAdd();
        inputProductCode.setText(productCode);
        selectDescriptionLanguage.selectByVisibleText(description);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);
        inputPriceAsDecimal.setText(rate);
        selectProductTypes.selectByVisibleText(userCategory);

        selectCompany.selectByVisibleText(currentCompanyName());

        switch (addPlanField) {
        case ALL:
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            inputProductPriceAsDecimal.setText(rateProduct);
            break;

        case PRODUCT:
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            break;

        case BUNDLEDPERIOD:
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            selectBundlePeriod.selectByVisibleText(bundledPeriod);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            break;

        case WITHNOTE:
            inputPlanDescription.setText(note);
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            verifyMetaField();
            inputProductPricePrecedence.setText(precedence);
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            selectBundlePeriod.selectByVisibleText(bundledPeriod);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            break;
        default:
            throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");
        }
        linkUpdate.click();
        clickSaveChanges();
    }

    public void addPlan (String userCategory, String productCategory, String testDataSetName, String category) {

        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);
        String description = TestData.read("PagePlans.xml", testDataSetName, "description", category);
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);
        String rate = TestData.read("PagePlans.xml", testDataSetName, "rate", category);
        String bundleQuantity = TestData.read("PagePlans.xml", testDataSetName, "bundleQuantity", category);
        String rateProduct = TestData.read("PagePlans.xml", testDataSetName, "rateProduct", category);

        clickAdd();
        inputProductCode.setText(productCode);
        selectDescriptionLanguage.selectByVisibleText(description);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);
        selectProductTypes.selectByVisibleText(userCategory);
        inputPriceAsDecimal.setText(rate);
        selectCompany.selectByVisibleText(currentCompanyName());
        linkTabProducts.click();
        filterProducts(productCategory);
        selectListElementByText(listProducts, productCategory);
        verifyMetaField();
        inputBundleQuantity.setText(bundleQuantity);
        verifyMetaField();
        inputProductPriceAsDecimal.setText(rateProduct);
        linkUpdate.click();
        clickSaveChanges();
    }

    public void addPlan (AddPlanField addPlanField, String userCategory, String productCategory, String planPeriod,
            String testDataSetName, String category) {

        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);
        String description = TestData.read("PagePlans.xml", testDataSetName, "description", category);
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);
        String rate = TestData.read("PagePlans.xml", testDataSetName, "rate", category);
        String bundleQuantity = TestData.read("PagePlans.xml", testDataSetName, "bundleQuantity", category);
        String rateProduct = TestData.read("PagePlans.xml", testDataSetName, "rateProduct", category);
        String bundleQuantity2 = TestData.read("PagePlans.xml", testDataSetName, "bundleQuantity2", category);
        String rateProduct2 = TestData.read("PagePlans.xml", testDataSetName, "rateProduct2", category);
        String note = TestData.read("PagePlans.xml", testDataSetName, "note", category);

        clickAdd();
        inputProductCode.setText(productCode);
        selectDescriptionLanguage.selectByVisibleText(description);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);

        selectPlanPeriod.selectByVisibleText(planPeriod);

        inputPriceAsDecimal.setText(rate);
        selectProductTypes.selectByVisibleText(userCategory);
        selectCompany.selectByVisibleText(currentCompanyName());

        switch (addPlanField) {

        case PLANPERIOD:
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            verifyMetaField();
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            break;

        case MULTIPLEPLAN:
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            verifyMetaField();
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            linkUpdate.click();

            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            verifyMetaField();
            inputBundleQuantity.setText(bundleQuantity2);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct2);
            break;

        case WITHNOTE:
            inputPlanDescription.setText(note);
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            verifyMetaField();
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            break;
        default:
            throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");
        }
        linkUpdate.click();
        clickSaveChanges();
    }

    public String addPlanForMobileCalls (String freeUsagePoolName, String productCategory, String products,
            String testDataSetName, String category) {

        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);
        String description = TestData.read("PagePlans.xml", testDataSetName, "description", category);
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);

        clickAdd();
        inputProductCode.setText(productCode);
        selectDescriptionLanguage.selectByVisibleText(description);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);
        selectProductTypes.selectByVisibleText(productCategory);
        selectFreeUsagePools.selectByVisibleText(freeUsagePoolName);

        selectCompany.selectByVisibleText(currentCompanyName());
        linkTabProducts.click();
        explicitWaitForJavascript();
        selectListElementByText(listProducts, products);
        explicitWaitForJavascript();
        linkUpdate.click();
        clickSaveChanges();

        return englishDescription;
    }

    public String addProductInPlan (String testDataSetName, String category, String productCategory, String productName) {
        String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
        String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);

        clickAdd();
        inputProductCode.setText(productCode);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);
        selectProductTypes.selectByVisibleText(productCategory);
        selectCompany.selectByVisibleText(currentCompanyName());
        linkTabProducts.click();
        selectListElementByText(listProducts, productName);
        explicitWaitForJavascript();
        clickSaveChanges();

        return englishDescription;
    }

    public void addProductInMultipleCategoryInPlan (String testDataSetName, String category, String categoryName1,
            String categoryName2, String productName) {
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);
        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);

        clickAdd();
        inputProductCode.setText(productCode);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);
        selectProductTypes.selectByVisibleText(categoryName2);
        selectProductTypes.selectByVisibleText(categoryName1);

        selectCompany.selectByVisibleText(currentCompanyName());
        linkTabProducts.click();
        selectListElementByText(listProducts, productName);
        explicitWaitForJavascript();
        clickSaveChanges();
        validateTextPresentInTable(englishDescription, tablePlans);

        selectPlan(englishDescription);

        Assert.assertEquals(liCategoryName1.getText(), categoryName1);
        Assert.assertEquals(liCategoryName2.getText(), categoryName2);
    }

    protected void selectPlan(String description) {
        selectListElementByText(listPlans, description);
        explicitWaitForJavascript ();
    }

    //This must be removed after refactoring.
    protected boolean explicitWaitForJavascript () {
        return WebPage.DriverUtils.explicitWaitForJavascript(GlobalController.brw.getCurrentWebDriver(), 20);
    }

    public String addPlanWithDifferentCurrency (String userCategory, String productCategory, String testDataSetName,
            String category) {
        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);
        String description = TestData.read("PagePlans.xml", testDataSetName, "description", category);
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);
        String differentcurrency = TestData.read("PagePlans.xml", testDataSetName, "currencydiffer", category);
        String rate = TestData.read("PagePlans.xml", testDataSetName, "rate", category);

        clickAdd();
        inputProductCode.setText(productCode);
        selectDescriptionLanguage.selectByVisibleText(description);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);
        inputPriceAsDecimal.setText(rate);
        selectProductTypes.selectByVisibleText(userCategory);
        selectCompany.selectByVisibleText(currentCompanyName());
        selectCurrencies.selectByVisibleText(differentcurrency);
        linkTabProducts.click();
        selectListElementByText(listProducts, productCategory);
        explicitWaitForJavascript();
        clickSaveChanges();

        return englishDescription;
    }

    public String addPlanYearly (AddPlanField addPlanField, String userCategory, String productCategory,
            String testDataSetName, String category, String planPeriod) {

        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);
        String description = TestData.read("PagePlans.xml", testDataSetName, "description", category);
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);
        String rate = TestData.read("PagePlans.xml", testDataSetName, "rate", category);
        String bundleQuantity = TestData.read("PagePlans.xml", testDataSetName, "bundleQuantity", category);
        String rateProduct = TestData.read("PagePlans.xml", testDataSetName, "rateProduct", category);
        String bundledPeriod = TestData.read("PagePlans.xml", testDataSetName, "bundledPeriodOneTime", category);
        String note = TestData.read("PagePlans.xml", testDataSetName, "note", category);
        String precedence = TestData.read("PagePlans.xml", testDataSetName, "precedence", category);
        String bundledPeriodOneTime = TestData.read("PagePlans.xml", testDataSetName, "bundledPeriodOneTime", category);

        clickAdd();
        inputProductCode.setText(productCode);
        selectDescriptionLanguage.selectByVisibleText(description);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);

        switch (addPlanField) {
        case ALL:
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            inputProductPriceAsDecimal.setText(rateProduct);
            break;
        case PRODUCT:
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            break;
        case BUNDLEDPERIOD:
            selectPlanPeriod.selectByVisibleText(planPeriod);
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            selectBundlePeriod.selectByVisibleText(bundledPeriodOneTime);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            break;
        case WITHNOTE:
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            inputPlanDescription.setText(note);
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            verifyMetaField();
            inputProductPricePrecedence.setText(precedence);
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            selectBundlePeriod.selectByVisibleText(bundledPeriod);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);

            break;

        default:
            throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");
        }

        linkUpdate.click();
        clickSaveChanges();

        return englishDescription;
    }

    public String addPlanMonthly (AddPlanField addPlanField, String userCategory, String productCategory,
            String testDataSetName, String category) {

        String productCode = TestData.read("PagePlans.xml", testDataSetName, "productCode", category);
        String description = TestData.read("PagePlans.xml", testDataSetName, "description", category);
        String englishDescription = TestData.read("PagePlans.xml", testDataSetName, "englishDescription", category);
        String rate = TestData.read("PagePlans.xml", testDataSetName, "rate", category);
        String bundleQuantity = TestData.read("PagePlans.xml", testDataSetName, "bundleQuantity", category);
        String rateProduct = TestData.read("PagePlans.xml", testDataSetName, "rateProduct", category);
        String bundledPeriod = TestData.read("PagePlans.xml", testDataSetName, "bundledPeriodOneTime", category);
        String note = TestData.read("PagePlans.xml", testDataSetName, "note", category);
        String precedence = TestData.read("PagePlans.xml", testDataSetName, "precedence", category);

        clickAdd();
        inputProductCode.setText(productCode);
        selectDescriptionLanguage.selectByVisibleText(description);
        linkAddNewProductDescription.click();
        inputDescription.setText(englishDescription);

        switch (addPlanField) {
        case ALL:
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            inputProductPriceAsDecimal.setText(rateProduct);
            break;

        case PRODUCT:
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            break;

        case BUNDLEDPERIOD:
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            selectBundlePeriod.selectByVisibleText(bundledPeriod);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            break;

        case WITHNOTE:
            inputPriceAsDecimal.setText(rate);
            selectProductTypes.selectByVisibleText(userCategory);
            selectCompany.selectByVisibleText(currentCompanyName());
            inputPlanDescription.setText(note);
            linkTabProducts.click();
            selectListElementByText(listProducts, productCategory);
            explicitWaitForJavascript();
            verifyMetaField();
            inputProductPricePrecedence.setText(precedence);
            inputBundleQuantity.setText(bundleQuantity);
            verifyMetaField();
            selectBundlePeriod.selectByVisibleText(bundledPeriod);
            verifyMetaField();
            inputProductPriceAsDecimal.setText(rateProduct);
            break;

        default:
            throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");
        }

        linkUpdate.click();
        clickSaveChanges();

        return englishDescription;
    }

    public void editPlan (String productCategory, String testDataSetName, String category) {
        String bundleQuantity = TestData.read("PagePlans.xml", testDataSetName, "bundleQuantity", category);
        String rateProduct = TestData.read("PagePlans.xml", testDataSetName, "rateProduct", category);

        linkFirstPlan.click();
        linkEdit.click();
        linkTabProducts.click();
        selectListElementByText(listProducts, productCategory);
        explicitWaitForJavascript();
        inputBundleQuantity.setText(bundleQuantity);
        inputProductPriceAsDecimal.setText(rateProduct);
        linkUpdate.click();
        clickSaveChanges();
    }

    private void clickSaveChanges () {

        // TODO. verify if waits still needed
        // GlobalController.brw.waitForAjaxElement(LT_SAVECHANGES);
        // GlobalController.brw.waitUntilElementStopMoving(LT_SAVECHANGES);

        linkSaveChanges.click();
    }

    private void verifyMetaField () {
        if (!liMetafieldTabActivator.getAttribute("class").contains("active")) {
            spanMetafieldTab.click();
        }
    }

    // TODO. remove duplication (OrderBuilderPage)
    private void filterProducts (String filter) {
        inputFilterProductsBy.setText(filter);
        inputFilterProductsBy.sendKeys(Keys.TAB);
        // wait for listProducts
        explicitWaitForJavascript();
    }

    // TODO. check if ajax wait is still required
    /*
     * 1. after linkTabProducts.click() wait for inputFilterProductsBy 2. after selectListElementByText(listProducts,
     * ...) wait for inputBundleQuantity
     */
}
