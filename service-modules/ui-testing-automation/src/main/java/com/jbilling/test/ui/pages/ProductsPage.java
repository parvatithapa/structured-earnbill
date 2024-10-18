package com.jbilling.test.ui.pages;

import com.jbilling.framework.globals.GlobalController;
import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddProductField;
import com.jbilling.framework.globals.GlobalEnumsPage.setProductCategoryWithAssetMgmt;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import ru.yandex.qatools.htmlelements.element.CheckBox;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.element.Table;

import java.util.List;

public class ProductsPage extends AppPage {

	@FindBy(xpath = "//table[@id='categories']/tbody//strong[contains(text(),'New Test category')]/parent::a")
	private Link linkProductCategory;

	@FindBy(xpath = "//table[@id='categories']/tbody//strong[contains(text(),'Asset Category 1')]/parent::a")
	private Link linkAssetCategory1;

	@FindBy(xpath = "//span[text()='Add Product']")
	private Link linkAddProduct;

	@FindBy(xpath = "//select[@id='newDescriptionLanguage']")
	private Select selectAddDescription;

	@FindBy(xpath = "//a[@onclick='addNewProductDescription()']")
	private Link linkAddDescription;

	@FindBy(xpath = "//label[contains(text(),'English Description')]/../div/input[1]")
	private TextInput textEnglishDescription;

	@FindBy(xpath = "//input[@id='product.number']")
	private TextInput textProductCode;

	@FindBy(xpath = "//input[@id='product.standardAvailability']")
	private CheckBox cbStandardAvailability;

	@FindBy(id = "model.0.type")
	private Select selectPriceModel;

	@FindBy(xpath = "//input[@id='model.0.rateAsDecimal']")
	private TextInput textRate;

	@FindBy(xpath = "//select[@id='model.0.currencyId']")
	private Select selectCurrency;

    @FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'save')]/span[text()='Save Changes']/..")
	private Link linkSaveChanges;

	@FindBy(xpath = "//table[@id='products']/tbody//strong[contains(text(),'Flat 1 Priced Product')]")
	private HtmlElement htmlFlatProduct;

	@FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'edit')]")
	private Link linkEditProduct;

	@FindBy(xpath = "//input[@id='product.standardPartnerPercentageAsDecimal']")
	private TextInput textStandardPartnerPercentageAsDecimal;

	@FindBy(xpath = "//input[@id='product.masterPartnerPercentageAsDecimal']")
	private TextInput textMasterPartnerPercentageAsDecimal;

	@FindBy(xpath = "//input[@id='plg-parm-provisionable_order_change_status_id']")
	private TextInput textProvisionable;

	@FindBy(xpath = "//select[@id='company-select']")
	private Select selectCompany;

	@FindBy(xpath = "//span[text()='Add Category']")
	private Link linkAddProductCategory;

	@FindBy(xpath = "//select[@id='orderLineTypeId']")
	private Select selectAddProductCategory;

	@FindBy(xpath = "//input[@id='description']")
	private TextInput textProductCategoryName;

	@FindBy(xpath = "//span[text()='Edit']")
	private Link linkEdit;

	@FindBy(xpath = "//input[@id='lastStatusName']")
	private TextInput textAssetStatusName;

	@FindBy(xpath = "//th[contains(text(),'Available')]/following::tbody/tr[1]/td[2]/input[@type='checkbox']")
	private CheckBox cbAvailable;

	@FindBy(xpath = "//th[contains(text(),'Available')]/following::tbody/tr[1]/td[3]/input[@type='checkbox']")
	private CheckBox cbDefault;

	@FindBy(xpath = "//th[contains(text(),'Available')]/following::tbody/tr[2]/td[4]/input[@type='checkbox']")
    private CheckBox cbOrderSave;

	@FindBy(xpath = "//th[contains(text(),'Available')]/following::tbody/tr[2]/td[5]/input[@type='checkbox']")
	private CheckBox cbActive;

	@FindBy(xpath = "//th[contains(text(),'Available')]/following::tbody/tr[3]/td[4]/input[@type='checkbox']")
	private CheckBox cbOrderSavePending;

	@FindBy(xpath = "//th[contains(text(),'Available')]/following::tbody/tr[3]/td[6]/input[@type='checkbox']")
	private CheckBox cbPending;

	@FindBy(xpath = "//tr[@id='lastStatus']/td[8]/a")
	private Link linkHiddenAssetStatus;

	@FindBy(xpath = "//div[@id='assetMetaFields']//a[contains(@onclick, 'addMetaField')]")
	private Link linkAddMetaField;

    @FindBy(xpath = "//div[@id='assetMetaFields']//a[contains(@onclick, 'mfId')]")
    private Link linkImportMetaField;

	@FindBy(xpath = "//input[@id='metaField2.name']")
	private TextInput textMetaFieldName;

	@FindBy(xpath = "//table[@id='categories']/tbody/tr[1]/td/a/strong")
	private Link linkCreatedProductCategory;

    // jBilling
	@FindBy(xpath = "//ul[@class='top-nav']/li[1]")
    private HtmlElement htmlCompanyLegacy;
    // AppBilling
    @FindBy(xpath = "((//ul[@id='navRight']/li)[2]/ul/li)[1]/span")
	private HtmlElement htmlCompany;

	@FindBy(xpath = "//select[@id = 'parentItemTypeId']")
	private Select selectParentCategory;

	@FindBy(xpath = "//a/strong[contains(text(),'Generic internal events listener')]/..")
	private Link linkGenericListener;

	@FindBy(xpath = "//span[text()='Add New']")
	private Link linkAddNew;

	@FindBy(xpath = "//select[@id='typeId']")
	private Select selectPluginType;

	@FindBy(xpath = "//input[@id='processingOrder']")
	private TextInput textProcessingOrder;

	@FindBy(xpath = "//span[text()='Save Plug-in']")
	private Link linkSavePlugIn;

	@FindBy(xpath = "//span[text()='Add Asset']")
	private Link linkAddAsset;

	@FindBy(xpath = "//input[@id='identifier']")
	private TextInput textAssetIdentifier;

	@FindBy(xpath = "//label[text()='Included Quantity']/../div/input[2]")
	private TextInput textIncludedQuantity;

	@FindBy(xpath = "//a/span[text()='Dependencies']/..")
	private Link linkDependencies;

	@FindBy(xpath = "//select[@name='product.dependencyItemTypes']")
	private Select selectProductDepencyCategories;

	@FindBy(xpath = "//select[@name='product.dependencyItems']")
	private Select selectProductDepencyItems;

	@FindBy(xpath = "//a[@onclick='addDependency(); return false;']")
	private Link linkAddDependency;

	@FindBy(xpath = "//span[contains(text(),'Add Category')]")
	private Link linkAddCategory;

	@FindBy(xpath = "//div[@id='breadcrumbs']")
	private HtmlElement htmlBreadcrumbs;

	@FindBy(id = "assetManagementEnabled")
	private CheckBox cbAssetMgmtEnabled;

    @FindBy(id = "allowAssetManagement")
    private CheckBox cbAllowAssetMgmt;

	@FindBy(xpath = "//*[@id='lastStatus']/td[8]/a")
	private Link linkAddStatus;

	@FindBy(id = "lastStatusOrderSaved")
	private CheckBox cbOrderSaved;

	@FindBy(id = "lastStatusActive")
	private CheckBox cbActiveStatus;

	@FindBy(id = "lastStatusPending")
	private CheckBox cbPendingStatus;

	@FindBy(xpath = "//span[contains(text(),'Add New')]")
	private Link linkAddNewAsset;

	@FindBy(xpath = "//tbody[@id='statusTBody']/tr[1]/td//input[@type='text']")
	private TextInput textAssetName1;

	@FindBy(xpath = "//tbody[@id='statusTBody']/tr[contains(@id,'lastStatus')]//td//input[@type='text']")
	private TextInput textAssetName2;

	@FindBy(xpath = "//table[@id='categories']")
	private Table tableCategory;

	@FindBy(xpath = "//table[@id='products']")
	private Table tableProduct;

    // N.B. fix in jbilling. table id should be changed to 'assets'
	@FindBy(xpath = "//table[@id='users']")
	private Table tableAsset;

	@FindBy(id = "product.dependencyMin")
	private TextInput textDependencyMinimum;

	@FindBy(id = "product.dependencyMax")
	private TextInput textDependencyMaximum;

	@FindBy(xpath = "//table[@id='categories']/tbody//td/a/strong")
	private List<WebElement> listProductCategories;

	@FindBy(xpath = "//table[@id='products']/tbody//td/a/strong")
	private List<WebElement> listProductNames;

	@FindBy(xpath = "//div[@class='form-columns']/div/div/span")
	private HtmlElement htmlProductId;

	@FindBy(xpath = "//div[@class = 'buttons']//span[text()='Save Changes']")
	private Link linkEditSaveChanges;

	@FindBy(xpath = "//span[text()='Dependencies']/parent::div[@class = 'box-cards-title']/following-sibling::div[@class = 'box-card-hold']/div/table[@class = 'dataTable']")
	private Table tableAddedDependency;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'The cap on the maximum amount on the total')]")
	private TextInput textMaximum;

	@FindBy(id = "assetIdentifierLabel")
	private TextInput textAssetIdLabel;

	@FindBy(id = "mandatoryCheck2")
	private CheckBox cbNewMandatory;

	@FindBy(xpath = "//label[text()='Import Meta Field']/following::select")
	private Select selectAssetMetaDataOption;

	@FindBy(xpath = "//label[text()='Default value']/following::div/input")
	private TextInput textMetaDataDefaultValue;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'If mediation is used this field is required.')]")
	private TextInput textDate;

	@FindBy(id = "model.0.attribute.2.value")
	private TextInput textTimeSet;

	@FindBy(xpath = "//input[@id='model.0.attribute.1.value']")
	private TextInput textRange;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'This represents the range starting value')]")
	private TextInput textFrom;

	@FindBy(xpath = "//input[@data-customtooltip='This is the rate for the range starting from this value(inclusive)']")
	private TextInput textRate1;

	@FindBy(xpath = "//a[@onclick ='addModelAttribute(this, 0, 2)']")
	private Link linkNewAttribute;

	@FindBy(name = "model.0.attribute.3.name")
	private TextInput textFrom1;

	@FindBy(xpath = "//input[contains(@name,'model.0.attribute.3.value')]")
	private TextInput textRate2;

	@FindBy(xpath = "//a[@onclick ='addModelAttribute(this, 0, 3)']")
	private Link linkNewAttribute1;

	@FindBy(name = "model.0.attribute.4.name")
	private TextInput textFrom2;

	@FindBy(xpath = "//input[contains(@name,'model.0.attribute.4.value')]")
	private TextInput textRate3;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'Number of pulled products to be purchased, to be able to get this product as a bonus')]")
	private TextInput textMultiplier;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'The calculation to determine the percentage is Selection Category/Category Percent x 100')]")
	private TextInput textCategoryPercantage;

	@FindBy(xpath = "//input[@data-customtooltip='Pair range between percentage calculated and the ID the product added, starting from 0 percent']")
	private TextInput textZeroField;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'Pair range between quantity of products in selected category and the ID the product added, starting from 1')]")
	private TextInput textOneField;

	@FindBy(name = "model.0.attribute.1.name")
	private TextInput textProductId;

	@FindBy(name = "model.0.attribute.1.value")
	private TextInput textPriceQuantity;

	@FindBy(xpath = "//span[contains(.,'Add Chain')]")
	private Link linkAddChain;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'The rate of the product after the included quantity')]")
	private TextInput textGraduateRate;

	@FindBy(xpath = "//input[contains(@data-customtooltip,'Number of included products for free')]")
	private TextInput textGraduateIncludedQuantity;

	@FindBy(xpath = "//strong[text()='Lemonade']")
	private HtmlElement htmlProdLemonade;

	@FindBy(xpath = "//strong[text()='Coffee']")
	private HtmlElement htmlProductCoffee;

	@FindBy(xpath = "//*[@id='product.dependencyItems']/option[2]")
	private HtmlElement productDepencySecondItem;

	@FindBy(xpath = "//*[@id='save-product-form']/fieldset/div[7]/ul/li[1]/a")
	private Link linkProdSaveButton;

	@FindBy(xpath = "//table[@id='categories']/tbody/tr/td/a/strong")
	private List<WebElement> listCreatedCategory;

	@FindBy(id = "onePerOrder")
	private CheckBox cbOnePerOrder;

	@FindBy(id = "onePerCustomer")
	private CheckBox cbOneItemPerCustomer;

	@FindBy(id = "product.activeSince")
	private TextInput textProdStartDate;

	@FindBy(id = "product.activeUntil")
	private TextInput textProdActiveUntil;

	@FindBy(id = "product.dependencyMax")
	private TextInput textProdDependencyMax;

    @FindBy(id = "global-checkbox")
    private CheckBox cbGlobal;

    @FindBy(id = "onePerCustomer")
    private CheckBox cbOnePerCustomer;

    @FindBy(xpath = "//a[contains(@class,'save')]//span[contains(text(),'Save Changes')]")
    private Link linkSaveProductCategory;

    @FindBy(id = "assetManagementEnabled")
    private CheckBox cbAllowAssetManagement;

    @Autowired
    protected MessagesPage messagesPage;

    public ProductsPage(WebDriver driver) {
        super(driver);
    }

	public void selectCategory(String category) {
        selectListElementByText(listCreatedCategory, category);
	}

	private void clickAddCategory() {
        linkAddCategory.click();
	}

	private void addProductCategory() {
        linkProductCategory.click();
        explicitWaitForJavascript();
//		GlobalController.brw.waitForAjaxElement(LT_AddProduct);
	}

	public void selectAssetCategory1() {
        linkAssetCategory1.click();
	}

	private void checkStandardAvailability(boolean standardAvailability) {
		if (standardAvailability) {
            cbStandardAvailability.select();
		} else {
            cbStandardAvailability.deselect();
		}
	}

	private void selectPricingModel(String pricingModel) {
        selectPriceModel.selectByVisibleText(pricingModel);
	}

	private void clickProductToEdit() {
        htmlFlatProduct.click();
	}

	private void clickEditProduct() {
        explicitWaitForJavascript();
        linkEditProduct.click();
/*
        GlobalController.brw.waitForAjaxElement(this.LT_EDITPRODUCT);
        GlobalController.brw.waitUntilElementStopMoving(this.LT_EDITPRODUCT);
		GlobalController.brw.clickLinkText(this.LT_EDITPRODUCT);
*/
	}

	private void setAgentCommission(String agent) {
        textStandardPartnerPercentageAsDecimal.setText(agent);
	}

	private void setMasterCommission(String master) {
        textMasterPartnerPercentageAsDecimal.setText(master);
	}

	private void verifySavedProduct() {
		messagesPage.isIntermediateSuccessMessageAppeared();
	}

	private void setCategoryName(String productCategoryName) {
        textProductCategoryName.setText(productCategoryName);
	}

	private void clickRecentCreatedcategory() {
        linkCreatedProductCategory.click();
	}

	private void clickEdit() {
        linkEdit.click();
	}

	private void selectCurrentCompany() {
        String company;
	    if (htmlCompanyLegacy.exists()) {
	        company = htmlCompanyLegacy.getText();
	    } else {
	        company = htmlCompany.getAttribute("textContent");
	    }
        selectCompany.selectByVisibleText(company);
	}

	private void selectsecondoption() {
		String product = productDepencySecondItem.getText();
        selectProductDepencyItems.selectByVisibleText(product);
	}

	private void selectParentCategory(String parent) {
        selectParentCategory.selectByVisibleText(parent);
	}

	private void clickPluginGeneric() {
        linkGenericListener.click();
	}

	public void clickAddNew() {
        linkAddNew.click();
	}

	private void selectPluginTypeDropdown(String pluginType) {
        selectPluginType.selectByVisibleText(pluginType);
	}

	private void setOrder(String order) {
        textProcessingOrder.setText(order);
	}

	private void setProvisionableField(String provision_ID) {
        textProvisionable.setText(provision_ID);
	}

	private void clickSavePluginButton() {
        linkSavePlugIn.click();
	}

	private void clickAddProduct() {
        linkAddProduct.click();
	}

	private void checkAssetManagement(Boolean assetManage) {
		if (assetManage) {
            cbAssetMgmtEnabled.select();
		} else {
            cbAssetMgmtEnabled.deselect();
		}
	}

	public void addAsset() {
        linkAddAsset.click();
	}

	private void addChildAsset() {
        linkAddNewAsset.click();
	}

	private void assetDetails(String identifier) {
        textAssetIdentifier.setText(identifier);
	}

	private void clickAddProductCategory() {
        linkAddProductCategory.click();
	}


	private void checkAllowAssetManagement() {
        cbAllowAssetMgmt.select();
	}

	private void setAssetStatusName(String assetStatusName) {
        textAssetStatusName.setText(assetStatusName);
	}

	private void checkAvailable() {
        cbAvailable.select();
	}

	private void checkDefault() {
        cbDefault.select();
	}

	private void clickHiddenStatus() {
        linkHiddenAssetStatus.click();
	}

	private void checkOrderSave() {
        cbOrderSave.select();
	}

	private void checkActive() {
		cbActive.select();
	}

	private void checkOrderSavePending() {
		cbOrderSavePending.select();
	}

	private void checkPending() {
		cbPending.select();
	}

	private void clickMetaField() {
        linkAddMetaField.click();
        explicitWaitForJavascript();
	}

	private void clickImportMetaField() {
        explicitWaitForJavascript();
        linkImportMetaField.click();
        WebPage.DriverUtils.explicitWaitForJavascript(GlobalController.brw.getCurrentWebDriver(), 20);
    }

	private void setMetafieldName(String assetStatusName) {
        textMetaFieldName.setText(assetStatusName);
	}

	private void selectAddDescription(String description) {
        selectAddDescription.selectByVisibleText(description);
	}

	private void clickAddDescription() {
        linkAddDescription.click();
	}

	private void setGraduateRate(String GraduateRate) {
        textGraduateRate.setText(GraduateRate);
	}

	private void setDescription(String englishDescription) {
        textEnglishDescription.setText(englishDescription);
	}

	private void setGraduateIncludedQuantity(String GraduateQuantity) {
        textGraduateIncludedQuantity.setText(GraduateQuantity);
	}

	private void setProductCode(String productCode) {
        textProductCode.setText(productCode);
	}

	private void setIncludedQuantity(String includedQuantity) {
        textIncludedQuantity.setText(includedQuantity);
	}

	private void selectCurrency(String currency) {
        selectCurrency.selectByVisibleText(currency);
	}

	private void selectProductCategory(String category) {
        selectProductDepencyCategories.selectByVisibleText(category);
	}

	private void setMaximum(String maximum) {
        textMaximum.setText(maximum);
	}

	private void setDate(String date) {
        textDate.setText(date);
	}

	private void setTimeField(String TimeValue) {
        textTimeSet.setText(TimeValue);
	}

	private void setRange(String Range) {
        textRange.setText(Range);
	}

	private void setFrom(String From) {
        textFrom.setText(From);
	}

	private void setToRate(String Rate1) {
        textRate1.setText(Rate1);
	}

	private void setFrom1(String From1) {
        textFrom1.setText(From1);
	}

	private void setToRate2(String Rate2) {
        textRate2.setText(Rate2);
	}

	private void setFrom2(String From2) {
        textFrom2.setText(From2);
	}

	private void setToRate3(String Rate3) {
        textRate3.setText(Rate3);
	}

	private void setMultiplier(String Multiplier) {
        textMultiplier.setText( Multiplier);
	}

	private void clickAddNewAttribute() {
        linkNewAttribute.click();
	}

	private void clickAddNewAttribute1() {
        linkNewAttribute1.click();
	}

	private void setRate(String rate) {
        textRate.setText(rate);
	}

	private void setProductID(String ProductID) {
        textProductId.setText(ProductID);
	}

	private void setQuantity(String Quantity) {
        textPriceQuantity.setText(Quantity);
	}

	private void clickAddChain() {
        linkAddChain.click();
	}

	private void setPercentageCategory(String Percentage) {
        textCategoryPercantage.setText(Percentage);
	}

	private void setZero(String Zero) {
        textZeroField.setText(Zero);
	}

	private void setOne(String One) {
        textOneField.setText(One);
	}

	private void selectProduct(String product) {
        selectProductDepencyItems.selectByVisibleText(product);
	}

	private void clickImagePlusButton() {
        linkAddDependency.click();
	}

	private void clickDependencies() {
        linkDependencies.click();
	}

	private void clickSaveChanges() {
        linkSaveChanges.click();
	}

	private void setMimimumValue(String minimum) {
		textDependencyMinimum.setText(minimum);
	}

	private void setMaximimValue(String maximum) {
		textDependencyMaximum.setText( maximum);
	}

	private void clickSpecifiedCategoryInTheTable(String categoryName) {
        selectListElementByText(listProductCategories, categoryName);
	}

	private void clickSpecifiedProductInTheTable(String product) {
        selectListElementByText(listProductNames, product);
	}

	private String getProductID() {
		String text = htmlProductId.getText();
		return text;
	}

	private boolean isProductPresent(String productName) {
        return selectProductDepencyCategories.hasOption(productName);
	}

	private void clickEditSaveChanges() {
        linkEditSaveChanges.click();
	}

	public String addProduct(AddProductField addProductField, String testDataSetName, String category) {
		String description = TestData.read("PageProducts.xml", testDataSetName, "description", category);
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		String pricingModel = TestData.read("PageProducts.xml", testDataSetName, "pricingModel", category);
		String rate = TestData.read("PageProducts.xml", testDataSetName, "rate", category);
		String currency = TestData.read("PageProducts.xml", testDataSetName, "currency", category);
		String includedQuantity = TestData.read("PageProducts.xml", testDataSetName, "includedQuantity", category);
		String Maximum = TestData.read("PageProducts.xml", testDataSetName, "maximum", category);
		String Date = TestData.read("PageProducts.xml", testDataSetName, "Date", category);
		String TimeFormat = TestData.read("PageProducts.xml", testDataSetName, "Time", category);
		String Range = TestData.read("PageProducts.xml", testDataSetName, "Range", category);
		String From = TestData.read("PageProducts.xml", testDataSetName, "From", category);
		String Rate1 = TestData.read("PageProducts.xml", testDataSetName, "Rate", category);
		String From1 = TestData.read("PageProducts.xml", testDataSetName, "From1", category);
		String Rate2 = TestData.read("PageProducts.xml", testDataSetName, "Rate2", category);
		String From2 = TestData.read("PageProducts.xml", testDataSetName, "From2", category);
		String Rate3 = TestData.read("PageProducts.xml", testDataSetName, "Rate3", category);
		String Multiplier = TestData.read("PageProducts.xml", testDataSetName, "Multiplier", category);
		String Percentage = TestData.read("PageProducts.xml", testDataSetName, "CategoryPercentage", category);
		String Zero = TestData.read("PageProducts.xml", testDataSetName, "Zero", category);
		String One = TestData.read("PageProducts.xml", testDataSetName, "One", category);
		String ProductID = TestData.read("PageProducts.xml", testDataSetName, "ProductID", category);
		String Quantity = TestData.read("PageProducts.xml", testDataSetName, "Quantity", category);
		String GraduateRate = TestData.read("PageProducts.xml", testDataSetName, "GraduateRate", category);
		String GraduateQuantity = TestData.read("PageProducts.xml", testDataSetName, "GraduateQuantity", category);

		boolean standardAvailability = TextUtilities.compareValue(
				TestData.read("PageProducts.xml", testDataSetName, "standardAvailability", category), "true", true, TextComparators.equals);
		boolean assetManage = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "assetManage", category),
				"true", true, TextComparators.equals);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);

		switch (addProductField) {
		case FLAT:
			this.addProductCategory();
			this.clickAddProduct();
			this.selectAddDescription(description);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.setProductCode(productCode);
			this.selectCurrentCompany();
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			this.selectCurrency(currency);

			break;
		case LINEPERCENTAGE:
			this.addProductCategory();
			this.clickAddProduct();
			this.selectAddDescription(description);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.setProductCode(productCode);
			this.checkStandardAvailability(standardAvailability);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			break;

        case LINEPERCENTAGEWITHCOMPANY:
            this.clickAddProduct();
            this.selectAddDescription(description);
            this.clickAddDescription();
            this.setDescription(englishDescription);
            this.setProductCode(productCode);
            this.selectCurrentCompany();
            this.selectPricingModel(pricingModel);
            this.setRate(rate);
            break;

		case ASSETMANAGEMENT: // "product.tc-3.4", "ap"
			this.clickAddProduct();
			this.selectAddDescription(description);
			this.clickAddDescription();

			this.setDescription(englishDescription);
			this.setProductCode(productCode);
			this.checkAssetManagement(assetManage);
            if (global) {
                this.enableGlobleCheckBox(true);
            }
			this.checkStandardAvailability(standardAvailability);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			this.selectCurrency(currency);
			break;
		case DESCRIPTION:
			// this.addProductCategory();
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			break;
		case FLATPRICE:
			// this.addProductCategory();
			// this.selectRecentCategory();
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			break;
		case GRADUATEDPRICE:
			// this.addProductCategory();
			// this.selectProductCategory(category);
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			this.setIncludedQuantity(includedQuantity);
			break;

		case GRADUATECAPPRICE:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			this.setIncludedQuantity(includedQuantity);
			this.setMaximum(Maximum);
			break;

		case TIMEOFDAY:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setDate(Date);
			this.setTimeField(TimeFormat);
			break;

		case TIERED:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setRange(Range);
			this.setFrom(From);
			this.setToRate(Rate1);
			this.clickAddNewAttribute();
			this.setFrom1(From1);
			this.setToRate2(Rate2);
			break;

		case VOLUME:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setRange(Range);
			this.setFrom(From);
			this.setToRate(Rate1);
			this.clickAddNewAttribute();
			this.setFrom1(From1);
			this.setToRate2(Rate2);
			this.clickAddNewAttribute1();
			this.setFrom2(From2);
			this.setToRate3(Rate3);
			break;

		case POOLED:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			this.setMultiplier(Multiplier);
			break;
		case ITEMPAGESELECTOR:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			this.setPercentageCategory(Percentage);
			this.setZero(Zero);
			break;

		case ITEMSELECTOR:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setOne(One);
			break;

		case QUANTITYADON:
			this.clickAddProduct();
			this.setProductCode(productCode);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.selectPricingModel(pricingModel);
			this.setProductID(ProductID);
			this.setQuantity(Quantity);
			this.clickAddChain();
			this.setGraduateRate(GraduateRate);
			this.setGraduateIncludedQuantity(GraduateQuantity);
			break;
		case GRADUATED:
			this.clickRecentCreatedcategory();
			this.clickAddProduct();
			this.selectAddDescription(description);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.setProductCode(productCode);
			this.selectPricingModel(pricingModel);
			this.setIncludedQuantity(includedQuantity);
			this.setRate(rate);
			break;

        case ADDPRODUCTWITHASSETMANAGEMENT:
            this.clickAddProduct();
            this.setProductCode(productCode);
            this.clickAddDescription();
            this.setDescription(englishDescription);
            this.checkAllowAssetManagementChkBox(assetManage);
            break;
        case TEASERPRICING:
            this.clickAddProduct();
            this.setProductCode(productCode);
            this.clickAddDescription();
            this.setDescription(englishDescription);
            this.selectPricingModel(pricingModel);
            this.setToRate2(Rate2);
            this.setFrom2(From2);
            this.setToRate3(Rate3);
            break;
		default:
			throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");
		}
		this.clickSaveChanges();

		return englishDescription;
	}

	public void editProduct(String testDataSetName, String category) {
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		String rate = TestData.read("PageProducts.xml", testDataSetName, "rate", category);

		this.clickProductToEdit();
		this.clickEditProduct();

		this.setDescription(englishDescription);
		this.setProductCode(productCode);

		this.setRate(rate);
		this.clickSaveChanges();
	}

	public String addCategory(String testDataSetName, String category) {
		String name = TestData.read("PageProducts.xml", testDataSetName, "name", category);
		this.clickAddCategory();
		this.setCategoryName(name);
		this.selectCurrentCompany();
		this.clickSaveChanges();
		return name;

	}

	public String addCategoryNationalMobileCalls(String testDataSetName, String category) {
		String name = TestData.read("PageProducts.xml", testDataSetName, "name", category);
		this.clickAddCategory();
		this.setCategoryName(name);
		this.selectCurrentCompany();
		this.clickSaveChanges();
		return name;

	}

	public void addPlugin(String testDataSetName, String category) {
		String pluginType = TestData.read("PageProducts.xml", testDataSetName, "pluginType", category);
		String order4 = TestData.read("PageProducts.xml", testDataSetName, "order", category);

		this.clickPluginGeneric();
		this.clickAddNew();
		this.selectPluginTypeDropdown(pluginType);

		this.setOrder(order4);
		this.clickSavePluginButton();
	}

	public String addAsset(String testDataSetName, String category) {
		String identifier = TestData.read("PageProducts.xml", testDataSetName, "identifier", category);
		this.assetDetails(identifier);
		this.clickSaveChanges();
        explicitWaitForJavascript();
		return identifier;

	}

	public void addProductCategoryWithAssetMgmt(String parentCategory, String importedMetaFieldName, setProductCategoryWithAssetMgmt addProductCategory, String testDataSetName,
			String category) {
		String assetCategory = TestData.read("PageProducts.xml", testDataSetName, "assetCategory", category);
		String assetCategory2 = TestData.read("PageProducts.xml", testDataSetName, "assetCategory2", category);
		String metaFieldName = TestData.read("PageProducts.xml", testDataSetName, "metaFieldName", category);
		String assetStatusName = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName", category);
		String assetStatusName2 = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName2", category);
		String assetStatusName3 = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName3", category);
		String assetStatusName4 = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName4", category);
		String assetStatusName5 = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName5", category);
		String assetStatusName6 = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName6", category);
		String addAssetIdentifier = TestData.read("PageProducts.xml", testDataSetName, "addAssetIdentifier", category);
		String addAssetIdentifier2 = TestData.read("PageProducts.xml", testDataSetName, "addAssetIdentifier2", category);
		String addAssetIdentifier3 = TestData.read("PageProducts.xml", testDataSetName, "addAssetIdentifier3", category);
		String metaFieldName2 = TestData.read("PageProducts.xml", testDataSetName, "metaFieldName2", category);
		String Value = TestData.read("PageProducts.xml", testDataSetName, "Value", category);
		String productCategoryType = TestData.read("PageProducts.xml", testDataSetName, "productCategoryType", category);
		String productCategoryType2 = TestData.read("PageProducts.xml", testDataSetName, "productCategoryType2", category);
		String taxCategory = TestData.read("PageProducts.xml", testDataSetName, "taxCategory", category);

		switch (addProductCategory) {
		case PCWAMG1:
			this.clickAddProductCategory();
			this.setCategoryName(assetCategory);
			this.selectCurrentCompany();
			this.checkAllowAssetManagement();
			this.setAssetStatusName(assetStatusName);
			this.checkAvailable();
			this.checkDefault();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName2);
			this.checkOrderSave();
			this.checkActive();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName6);
			this.checkOrderSavePending();
			this.checkPending();
			this.clickMetaField();
			this.setMetafieldName(metaFieldName);
			this.clickSaveChanges();
			break;

		case PCWAMG2:
			this.clickAddProductCategory();
			this.setCategoryName(assetCategory);
            this.selectParentCategory(parentCategory);
			this.selectCurrentCompany();
			this.checkAllowAssetManagement();
			this.addAssetIdentifier(addAssetIdentifier);
			this.setAssetStatusName(assetStatusName);
			this.checkAvailable();
			this.checkDefault();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName3);
			this.checkOrderSave();
			this.checkActive();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName6);
			this.checkOrderSavePending();
			this.checkPending();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName4);
			this.selectImportMetaField(importedMetaFieldName);
			this.clickMetaField();
			this.setMetafieldName(metaFieldName2);
			this.changeUserPermissionForMandatory(true);
			this.enterNewMetaDataDefaultValue(Value);
			this.clickSaveChanges();
			break;

		case PCWAMG3:
			this.clickAddProductCategory();
			this.setCategoryName(taxCategory);
			this.selectProductCategoryType(productCategoryType);
            this.selectCurrentCompany();
			this.checkAllowAssetManagement();
			this.addAssetIdentifier(addAssetIdentifier2);
			this.setAssetStatusName(assetStatusName5);
			this.checkDefault();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName3);
			this.checkOrderSave();
			this.checkActive();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName6);
			this.checkOrderSavePending();
			this.checkPending();
			this.selectImportMetaField(importedMetaFieldName);
			this.clickImportMetaField();
			this.clickSaveChanges();
			break;

		case PCWAMG4:
			this.clickAddProductCategory();
			this.setCategoryName(assetCategory2);
			this.selectProductCategoryType(productCategoryType2);
            this.selectCurrentCompany();
			this.checkAllowAssetManagement();
			this.addAssetIdentifier(addAssetIdentifier3);
			this.setAssetStatusName(assetStatusName5);
			this.checkDefault();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName3);
			this.checkOrderSave();
			this.checkActive();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName6);
			this.checkOrderSavePending();
			this.checkPending();
			this.clickHiddenStatus();
			this.setAssetStatusName(assetStatusName4);
			this.selectImportMetaField(importedMetaFieldName);
            this.clickImportMetaField();
			this.clickSaveChanges();
			break;

		default:
			throw new RuntimeException("Bad enum value: "+ addProductCategory);
		}
	}

	public String Createanotherproduct(AddProductField addProductField, String testDataSetName, String category) {
		String description = TestData.read("PageProducts.xml", testDataSetName, "description", category);
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		String pricingModel = TestData.read("PageProducts.xml", testDataSetName, "pricingModel", category);
		String rate = TestData.read("PageProducts.xml", testDataSetName, "rate", category);
		String currency = TestData.read("PageProducts.xml", testDataSetName, "currency", category);

		boolean standardAvailability = TextUtilities.compareValue(
				TestData.read("PageProducts.xml", testDataSetName, "standardAvailability", category), "true", true, TextComparators.equals);

		this.clickAddProduct();
		this.selectAddDescription(description);
		this.clickAddDescription();

		this.setDescription(englishDescription);
		this.setProductCode(productCode);
		this.checkStandardAvailability(standardAvailability);
		this.selectPricingModel(pricingModel);
		this.setRate(rate);
		this.selectCurrency(currency);
		this.clickSaveChanges();
		return englishDescription;

	}

	public String editCategory(String testDataSetName, String category) {
		String productCategoryName = TestData.read("PageProducts.xml", testDataSetName, "name", category);

		this.clickRecentCreatedcategory();
		this.clickEdit();
		this.setCategoryName(productCategoryName);
		this.clickSaveChanges();
		return productCategoryName;
	}

	public void addProductOnDependency(String testDataSetName, String category, String Category) {
		String description = TestData.read("PageProducts.xml", testDataSetName, "description", category);
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		String pricingModel = TestData.read("PageProducts.xml", testDataSetName, "pricingModel", category);

		boolean standardAvailability = TextUtilities.compareValue(
				TestData.read("PageProducts.xml", testDataSetName, "standardAvailability", category), "true", true, TextComparators.equals);

		this.addProductCategory();
		this.clickAddProduct();
		this.selectAddDescription(description);
		this.clickAddDescription();

		this.setDescription(englishDescription);
		this.setProductCode(productCode);
		this.checkStandardAvailability(standardAvailability);

		this.selectPricingModel(pricingModel);
		this.clickDependencies();
		this.selectProductCategory(Category);
		this.clickSaveChanges();
	}

	public void addProductWithCommission(String testDataSetName, String category) {
		String description = TestData.read("PageProducts.xml", testDataSetName, "description", category);
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		String pricingModel = TestData.read("PageProducts.xml", testDataSetName, "pricingModel", category);
		String rate = TestData.read("PageProducts.xml", testDataSetName, "rate", category);
		String agent = TestData.read("PageProducts.xml", testDataSetName, "agent", category);
		String master = TestData.read("PageProducts.xml", testDataSetName, "master", category);

		this.addProductCategory();
		this.clickAddProduct();
		this.selectAddDescription(description);
		this.clickAddDescription();

		this.setDescription(englishDescription);
		this.setProductCode(productCode);
		this.setAgentCommission(agent);
		this.setMasterCommission(master);
		this.selectPricingModel(pricingModel);
		this.setRate(rate);
		this.clickSaveChanges();
		this.verifySavedProduct();
	}

	public void verifyUIComponent() {
        explicitWaitForJavascript();
        Assert.assertTrue(htmlBreadcrumbs.isDisplayed());
	}

	private void enableGlobleCheckBox(boolean global) {
		if (global) {
            cbGlobal.select();
		} else {
            cbGlobal.deselect();
		}
	}

	private void clickPlusImageButton() {
        linkAddStatus.click();
	}

	private void clickonOrderSavedCheckbox() {
        cbOrderSaved.select();
	}

	private void clickonActiveCheckbox() {
		cbActiveStatus.select();
	}

	private void clickonPendingCheckbox() {
		cbPendingStatus.select();
	}

	private void enterAssetName(String assetName) {
        textAssetName1.setText(assetName);
	}

	private void enterAssetName1(String assetName) {
        textAssetName2.setText(assetName);
	}

	public String addNewCategory(String testDataSetName, String category) {
		String assetName1 = TestData.read("PageProducts.xml", testDataSetName, "asserName1", category);
		String assetName2 = TestData.read("PageProducts.xml", testDataSetName, "asserName2", category);
		String assetName3 = TestData.read("PageProducts.xml", testDataSetName, "asserName3", category);
		String categoryName = TestData.read("PageProducts.xml", testDataSetName, "categoryName", category);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);

		this.clickAddCategory();
		this.setCategoryName(categoryName);
		this.checkAllowAssetManagement();
		this.enableGlobleCheckBox(global);
		this.checkAvailable();
		this.checkDefault();
		this.setAssetStatusName(assetName1);
		this.clickPlusImageButton();
		this.setAssetStatusName(assetName2);
		this.checkOrderSave();
		this.checkActive();
		this.clickPlusImageButton();
		this.setAssetStatusName(assetName3);
		this.checkOrderSavePending();
		this.checkPending();
		this.clickSaveChanges();
		this.verifySavedProduct();
		this.validateAddedCategory(categoryName);

		return categoryName;
	}

	public String addAssetinProduct(String testDataSetName, String category) {
		String identifier = TestData.read("PageProducts.xml", testDataSetName, "identifier1", category);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);

		this.addAsset();
		this.assetDetails(identifier);
		this.enableGlobleCheckBox(global);
		this.clickSaveChanges();
		return identifier;
	}

	public String addChildAsset(String testDataSetName, String category) {
		String identifier = TestData.read("PageProducts.xml", testDataSetName, "identifier2", category);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);
		this.addChildAsset();
		this.assetDetails(identifier);
		this.enableGlobleCheckBox(global);
		this.selectCurrentCompany();
		this.clickSaveChanges();
		return identifier;
	}

	public void validateAddedCategory(String data) {
        validateTextPresentInTable(data, tableCategory);
	}

	public void validateAddedProduct(String data) {
        validateTextPresentInTable(data, tableProduct);
	}

	public void validateAddedAsset(String data) {
        validateTextPresentInTable(data, tableAsset);
	}

	public void editDependencyInProduct(String testDataSetName, String category, String categoryName, String product,
			String productCategory, String productName, String productCategory2, String productName2) {
		String min1 = TestData.read("PageProducts.xml", testDataSetName, "Min1", category);
		String min0 = TestData.read("PageProducts.xml", testDataSetName, "Min0", category);

        System.out.println("1");
		this.clickSpecifiedCategoryInTheTable(categoryName);
        System.out.println("2");
        try {Thread.sleep(2000);} catch (Exception e) {}
		this.clickSpecifiedProductInTheTable(product);
        System.out.println("3");
        try {Thread.sleep(2000);} catch (Exception e) {}
		this.clickEditProduct();
        System.out.println("4");
        try {Thread.sleep(2000);} catch (Exception e) {}

		this.clickDependencies();
        System.out.println("5");
        try {Thread.sleep(2000);} catch (Exception e) {}
		this.selectProductCategory(productCategory);
		this.selectProduct(productName);
		this.setMimimumValue(min1);
		this.clickImagePlusButton();

		this.selectProductCategory(productCategory2);
		this.selectProduct(productName2);
		this.setMimimumValue(min0);
		this.clickImagePlusButton();

		this.clickEditSaveChanges();
	}

	public boolean isProductPresentInTheDropdownForDependency(String productCategory, String productName) {

		this.clickEditProduct();
		this.clickDependencies();
		this.selectProductCategory(productCategory);
		boolean result = this.isProductPresent(productName);
		this.clickEditSaveChanges();
		return result;
	}

	public String getIDOfAddedProduct() {
		this.clickEditProduct();
		String id = this.getProductID();
		return id;
	}

	public void validateDependencySavedTestData(String data) {
        validateTextPresentInTable(data, tableAddedDependency);
	}

	public String addProductNationalRomingcall(String testDataSetName, String category) {

		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		String pricingModel = TestData.read("PageProducts.xml", testDataSetName, "pricingModel", category);
		String rate = TestData.read("PageProducts.xml", testDataSetName, "rate", category);

		this.clickAddProduct();
		this.setProductCode(productCode);
		this.clickAddDescription();
		this.setDescription(englishDescription);
		this.selectPricingModel(pricingModel);
		this.setRate(rate);
		this.clickSaveChanges();
		return englishDescription;
	}

	public void validateCategoriesSavedTestData(String data) {
        validateTextPresentInTable(data, tableCategory);
	}

	public void validateProductSavedTestData(String data) {
        validateTextPresentInTable(data, tableProduct);
	}

	public void EditProducts(String product_category, String testDataSetName1, String category, String product_category2) {

		String Minimum = TestData.read("PageProducts.xml", testDataSetName1, "Minimum", category);
		String Maximum = TestData.read("PageProducts.xml", testDataSetName1, "Maximum", category);
		String Minimumboxes = TestData.read("PageProducts.xml", testDataSetName1, "Minimumboxes", category);
		String Maximumboxes = TestData.read("PageProducts.xml", testDataSetName1, "Maximumboxes", category);

		boolean flag = false;
		flag = htmlProdLemonade.exists();
		if (flag == true) {
            htmlProdLemonade.click();
			this.clickEditProduct();
			this.clickDependencies();
			this.selectCategoryLemonade(product_category);
			this.selectsecondoption();
            textDependencyMinimum.setText("");
			this.setMimimumValue(Minimum);
			this.setMaximimValue(Maximum);
			this.clickImagePlusButton();
            linkProdSaveButton.click();

		} else {
			throw new RuntimeException("Test Case failed: ");
		}

		// This will edit COFFEE
		flag = htmlProductCoffee.exists();
		if (flag == true) {
            htmlProductCoffee.click();
			this.clickEditProduct();
			this.clickDependencies();
			this.selectCategorCoffee(product_category2);
			this.selectsecondoption();
			this.setMimimumValue(Minimumboxes);
			this.setMaximimValue(Maximumboxes);
			this.clickImagePlusButton();
            linkProdSaveButton.click();

		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void selectCategorCoffee(String testdataset) {
        selectProductDepencyCategories.selectByVisibleText(testdataset);
	}

	private void selectCategoryLemonade(String testdataset) {
        selectProductDepencyCategories.selectByVisibleText(testdataset);
	}

	private void addAssetIdentifier(String assetIdentifierValue) {
        textAssetIdLabel.setText(assetIdentifierValue);
	}

	private void changeUserPermissionForMandatory(boolean editManadatory) {
		if (editManadatory) {
            cbNewMandatory.select();
		} else {
            cbNewMandatory.deselect();
		}
	}

	private void selectImportMetaField(String ImportMetaFieldValue) {
        selectAssetMetaDataOption.selectByVisibleText(ImportMetaFieldValue);
	}

	private void enterNewMetaDataDefaultValue(String Value) {
        textMetaDataDefaultValue.setText(Value);
	}

	private void selectProductCategoryType(String productCategoryType) {
        selectAddProductCategory.selectByVisibleText(productCategoryType);
	}

	public void addPluginWithProvisionID(String testDataSetName, String category) {
		String pluginType = TestData.read("PageProducts.xml", testDataSetName, "pluginType", category);
		String order = TestData.read("PageProducts.xml", testDataSetName, "order", category);

		this.clickAddNew();
		this.selectPluginTypeDropdown(pluginType);

		this.setOrder(order);
		this.setProvisionableField("5");
		this.clickSavePluginButton();
	}

	public void addPluginInsidePlugin(String testDataSetName, String category) {
		String pluginType = TestData.read("PageProducts.xml", testDataSetName, "pluginType", category);
		String order = TestData.read("PageProducts.xml", testDataSetName, "order", category);

		this.clickAddNew();
		this.selectPluginTypeDropdown(pluginType);

		this.setOrder(order);
		this.clickSavePluginButton();
	}

	public String addProductCategoryWithAssetMgmt(String testDataSetName, String category) {
		String assetCategory = TestData.read("PageProducts.xml", testDataSetName, "assetCategory", category);
		String metaFieldName = TestData.read("PageProducts.xml", testDataSetName, "metaFieldName", category);
		String assetStatusName = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName", category);
		String assetStatusName2 = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName2", category);
		String assetStatusName6 = TestData.read("PageProducts.xml", testDataSetName, "assetStatusName6", category);

		this.clickAddProductCategory();
		this.setCategoryName(assetCategory);
		this.selectCurrentCompany();
		this.checkAllowAssetManagement();
		this.setAssetStatusName(assetStatusName);
		this.checkAvailable();
		this.checkDefault();
		this.clickHiddenStatus();
		this.setAssetStatusName(assetStatusName2);
		this.checkOrderSave();
		this.checkActive();
		this.clickHiddenStatus();
		this.setAssetStatusName(assetStatusName6);
		this.checkOrderSavePending();
		this.checkPending();
		this.clickMetaField();
		this.setMetafieldName(metaFieldName);
		this.clickSaveChanges();
		return assetCategory;

	}

	private void clickOneItemPerOrderChkBox(boolean value) {
        if(value) {
            cbOnePerOrder.select();
        } else {
            cbOnePerOrder.deselect();
        }
	}

	private void clickOneItemPerCustomerChkBox(boolean value) {
        if(value) {
            cbOneItemPerCustomer.select();
        } else {
            cbOneItemPerCustomer.deselect();
        }
	}

	private void verifyOneItemPerCustomerChkBoxIsDisabled() {
		Assert.assertFalse(cbOneItemPerCustomer.isEnabled());
	}

	private void setStartDate(String startDate) {
        textProdStartDate.setText(startDate);
	}

	private void setEndDate(String startDate) {
        textProdActiveUntil.setText(startDate);
	}

	private void verifyOneItemPerOrderChkBoxIsDisabled() {
        Assert.assertTrue(!cbOnePerOrder.isEnabled());
	}

	public String createCategoryWithOneOrder(String testDataSetName, String category) {
		String categoryName = TestData.read("PageProducts.xml", testDataSetName, "categoryName", category);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);
		boolean customerCheckBox = TextUtilities.compareValue(
				TestData.read("PageProducts.xml", testDataSetName, "customerCheckBox", category), "true", true, TextComparators.equals);
		boolean orderCheckBox = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "orderCheckBox", category),
				"true", true, TextComparators.equals);
		this.clickAddCategory();
		this.clickOneItemPerOrderChkBox(orderCheckBox);
        explicitWaitForJavascript();
		this.verifyOneItemPerCustomerChkBoxIsDisabled();
		this.clickOneItemPerOrderChkBox(!orderCheckBox);
		this.clickOneItemPerCustomerChkBox(customerCheckBox);
        explicitWaitForJavascript();
		this.verifyOneItemPerOrderChkBoxIsDisabled();
		this.setCategoryName(categoryName);
		this.enableGlobleCheckBox(global);
		this.clickOneItemPerCustomerChkBox(!customerCheckBox);
		this.clickOneItemPerOrderChkBox(orderCheckBox);
		this.clickSaveChanges();
		this.validateAddedCategory(categoryName);
		return categoryName;
	}

	public String addProductInOnePerOrderCategory(String testDataSetName, String category, String categoryName) {
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String startDate = TestData.read("PageProducts.xml", testDataSetName, "startDate", category);
		String endDate = TestData.read("PageProducts.xml", testDataSetName, "endDate", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);
		this.selectCategory(categoryName);
		this.clickAddProduct();
		this.clickAddDescription();
		this.setDescription(englishDescription);
		this.setProductCode(productCode);
		this.selectCurrentCompany();
		this.enableGlobleCheckBox(global);
		this.setStartDate(startDate);
		this.setEndDate(endDate);
		this.clickSaveChanges();
		return englishDescription;
	}

	public String addProducts(AddProductField addProductField, String testDataSetName, String category) {
		String description = TestData.read("PageProducts.xml", testDataSetName, "description", category);
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		String pricingModel = TestData.read("PageProducts.xml", testDataSetName, "pricingModel", category);
		String rate = TestData.read("PageProducts.xml", testDataSetName, "rate", category);

		switch (addProductField) {
		case FLAT:
			this.clickRecentCreatedcategory();
			this.clickAddProduct();
			this.selectAddDescription(description);
			this.clickAddDescription();
			this.setDescription(englishDescription);
			this.setProductCode(productCode);
			this.selectPricingModel(pricingModel);
			this.setRate(rate);
			break;

		default:
			throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");
		}
		this.clickSaveChanges();
		return englishDescription;

	}

	public String createCategoryWithOneCustomer(String testDataSetName, String category) {
		String categoryName = TestData.read("PageProducts.xml", testDataSetName, "categoryName", category);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);
		boolean customerCheckBox = TextUtilities.compareValue(
				TestData.read("PageProducts.xml", testDataSetName, "customerCheckBox", category), "true", true, TextComparators.equals);
		this.clickAddCategory();
		this.setCategoryName(categoryName);
		this.clickOneItemPerCustomerChkBox(customerCheckBox);
		this.enableGlobleCheckBox(global);
		this.clickSaveChanges();
		this.validateAddedCategory(categoryName);
		return categoryName;
	}

	public String addProductInOnePerCustomerCategory(String testDataSetName, String category, String categoryName) {
		String englishDescription = TestData.read("PageProducts.xml", testDataSetName, "englishDescription", category);
		String startDate = TestData.read("PageProducts.xml", testDataSetName, "startDate", category);
		String pricingModel = TestData.read("PageProducts.xml", testDataSetName, "pricingModel", category);
		String rate = TestData.read("PageProducts.xml", testDataSetName, "rate", category);
		String currency = TestData.read("PageProducts.xml", testDataSetName, "currency", category);
		String endDate = TestData.read("PageProducts.xml", testDataSetName, "endDate", category);
		String productCode = TestData.read("PageProducts.xml", testDataSetName, "productCode", category);
		boolean global = TextUtilities.compareValue(TestData.read("PageProducts.xml", testDataSetName, "global", category), "true", true,
				TextComparators.equals);
		this.selectCategory(categoryName);
		this.clickAddProduct();
		this.clickAddDescription();
		this.setDescription(englishDescription);
		this.setProductCode(productCode);
		this.enableGlobleCheckBox(global);
		this.setStartDate(startDate);
		this.setEndDate(endDate);
		this.selectPricingModel(pricingModel);
		this.setRate(rate);
		this.selectCurrency(currency);
		this.clickSaveChanges();
		return englishDescription;
	}

	public String getIDAddedCategory(String categoryName) {
		this.clickEdit();
		String id = this.getCategoryID();

		this.clickEditSaveChanges();
		String product = categoryName.concat(" (Id: ");
		String productName1 = product.concat(id);
		String category = productName1.concat(")");
		return category;
	}
	
	private String getCategoryID() {
		String text = htmlProductId.getText();
		return text;

	}
	
	public String getIDAddedProduct(String productName) {
		this.clickEditProduct();
		String id = this.getProductID();

		this.clickEditSaveChanges();
		String product = id.concat(" : ");
		String productName1 = product.concat(productName);
		return productName1;
	}
	
	public void editProductWithDependency(String testDataSetName, String category, String productCategory, String productName) {
		String minimum = TestData.read("PageProducts.xml", testDataSetName, "minimum", category);
		String maximum = TestData.read("PageProducts.xml", testDataSetName, "maximum", category);

		this.clickEditProduct();

		this.clickDependencies();
		this.selectProductCategory(productCategory);
		this.selectProduct(productName);
		this.setMimimumValue(minimum);
		this.setMaximumValue(maximum);
		this.clickImagePlusButton();
		this.clickEditSaveChanges();
	}

	private void setMaximumValue(String maximum) {
        textProdDependencyMax.setText(maximum);
	}

    public String addCategorywithGlobal (String testDataSetName, String category, boolean oneItem) {
        String name = TestData.read("PageProducts.xml", testDataSetName, "categoryName", category);

        this.clickAddCategory();
        this.setCategoryName(name);
        this.checkGlobal(oneItem);
        this.checkOneItemPerCustomer(oneItem);
        this.clickSaveCategory();
        return name;
    }

    private void checkGlobal (Boolean on) {
        if (on) {
            cbGlobal.select();
        } else {
            cbGlobal.deselect();
        }
    }

    private void checkOneItemPerCustomer (Boolean on) {
        if (on) {
            cbOnePerCustomer.select();
        } else {
            cbOnePerCustomer.deselect();
        }
    }

    private void clickSaveCategory () {
        linkSaveProductCategory.click();
    }

    private void checkAllowAssetManagementChkBox (boolean allowAsset) {
        if (allowAsset) {
            cbAllowAssetManagement.select();
        } else {
            cbAllowAssetManagement.deselect();
        }
    }
}
