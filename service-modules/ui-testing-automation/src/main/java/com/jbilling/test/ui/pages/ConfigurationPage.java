package com.jbilling.test.ui.pages;

import com.jbilling.framework.globals.GlobalConsts;
import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.globals.GlobalEnumsPage.*;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.*;
import ru.yandex.qatools.htmlelements.element.Button;
import ru.yandex.qatools.htmlelements.element.TextBlock;

import java.util.List;


public class ConfigurationPage extends AppPage {
	@FindBy(xpath = "//div[@class='menu-items']/ul/li")
	private List<WebElement> listConfigurationItems;

	@FindBy(xpath = "//div[@id='column1']/div[@class='table-box']/table/tbody//td/a/strong")
	private List<WebElement> listPluginCategories;

	@FindBy(xpath = "//span[contains(text(),'Add New')]/..")
	private Link linkAddNewButton;

	@FindBy(xpath = "//a/span[text()='Save Changes']/..")
	private Link linkSaveChanges;

	@FindBy(xpath = "//span[text()='Cancel']")
	private Link linkCancel;

	@FindBy(xpath = "//span[contains(text(),'Edit')]")
	private Link linkEdit;

	@FindBy(xpath = "//a[contains(@class,'submit save')]")
	private Link linkSaveChangesTemplate;

	@FindBy(xpath = "//select[@id='typeId']")
	private Select selectPluginType;

	@FindBy(xpath = "//input[@id='processingOrder']")
	private TextInput inputProcessingOrder;

	@FindBy(xpath = "//input[@id='name']")
	private TextInput inputAccountTypeName;

	@FindBy(xpath = "//a[contains(@onclick,'addOrderChangeStatus')]")
	private Link linkAddOrderChangeStatus;

    @FindBy(xpath = "//a[contains(@onclick,'addModelAttribute')]")
	private Link linkAddModelAttribute;

	@FindBy(xpath = "//span[text()='Save Plug-in']/..")
	private Select linkSavePlugin;

	@FindBy(xpath = "//input[@id='nextRunDate']")
	private TextInput inputNewRunDate;

	@FindBy(xpath = "//select[@id='periodUnitId']")
	private Select selectBillingPeriodUnit;

	@FindBy(xpath = "//select[@id='usagePool.consumptionActions.1.type']")
	private Select selectUsagePoolConsumptionNotification;

	@FindBy(xpath = "//select[@id='usagePool.consumptionActions.1.notificationId']")
	private Select selectUsagePoolConsumptionNotification_Invoice;

	@FindBy(xpath = "//select[@id='usagePool.consumptionActions.1.mediumType']")
	private Select selectUsagePoolConsumptionNotification_Invoice_mail;

	@FindBy(xpath = "//select[@id='usagePool.consumptionActions.2.type']")
	private Select selectUsagePoolConsumptionFee;

	@FindBy(xpath = "//select[@id = 'usagePool.itemTypes']")
	private Select selectUsagePoolProductCategory;

	@FindBy(xpath = "//select[@id = 'usagePool.items']")
	private Select selectUsagePoolProduct;

	@FindBy(xpath = "//span[text()='Run Commission Process']")
	private Link linkRunCommission;

	@FindBy(xpath = "//input[@id='obj[0].statusStr']")
	private TextInput textStep1Status;

	@FindBy(xpath = "//input[@id='obj[0].days']")
	private TextInput textStep1Days;

	@FindBy(xpath = "//input[@id='obj[0].sendNotification']")
	private CheckBox cbStep1Notification;

	@FindBy(xpath = "//input[@id='obj[0].paymentRetry']")
	private CheckBox cbStep1PaymentRetry;

	@FindBy(xpath = "//input[@id='obj[0].suspended']")
	private CheckBox cbStep1Syspend;

	@FindBy(xpath = "//input[@id='obj[1].statusStr']")
	private TextInput textStep2Status;

	@FindBy(xpath = "//input[@id='obj[1].days']")
	private TextInput textStep2Days;

	@FindBy(xpath = "//input[@id='obj[1].sendNotification']")
	private CheckBox cbStep2Notification;

	@FindBy(xpath = "//input[@id='obj[1].paymentRetry']")
	private CheckBox cbStep2PaymentRetry;

	@FindBy(xpath = "//input[@id='obj[1].suspended']")
	private CheckBox cbStep2Suspend;

	@FindBy(xpath = "//input[@id='obj[2].statusStr']")
	private TextInput textStep3Status;

	@FindBy(xpath = "//input[@id='obj[2].days']")
	private TextInput textStep3Days;

	@FindBy(xpath = "//input[@id='obj[2].sendNotification']")
	private CheckBox cbStep3Notification;

    @FindBy(xpath = "//input[@id='obj[2].paymentRetry']")
    private CheckBox cbStep3PaymentRetry;

	@FindBy(xpath = "//input[@id='obj[3].sendNotification']")
	private CheckBox cbStep4Notification;

	@FindBy(xpath = "//input[@id='obj[2].suspended']")
	private CheckBox cbStep3Suspend;

	@FindBy(xpath = "//input[@id='obj[3].statusStr']")
	private TextInput textStep4Status;

	@FindBy(xpath = "//input[@id='obj[3].days']")
	private TextInput textStep4Days;

	@FindBy(xpath = "//input[@id='obj[3].paymentRetry']")
	private CheckBox cbStep4PaymentRetry;

	@FindBy(xpath = "//input[@name='obj[1].description_1']")
	private TextInput textProcessingOrderStatus;

	@FindBy(xpath = "//input[@name='obj[2].description_1']")
	private TextInput textFinishedOrderStatus;

	@FindBy(xpath = "//input[@name='obj[3].description_1']")
	private TextInput textSuspendedOrderStatus;

	@FindBy(xpath = "//input[@id='obj[3].suspended']")
	private CheckBox cbStep4Suspended;

	@FindBy(xpath = "//input[@id='collectionsRunDate']")
	private TextInput textCollectionRunDate;

	@FindBy(xpath = "//a[@id='runCollections']")
	private Link linkRunCollection;

	@FindBy(xpath = "//button[@id='runCollectionsConfirm']")
	private Link linkPopupRunCollections;

	@FindBy(xpath = "//table[@id='users']/tbody//td/a/strong")
	private List<WebElement> listConfigurationPreferences;

	@FindBy(xpath = "//table[@id='usagePools']/tbody//td/a/strong")
	private List<WebElement> listUsagePoolNames;

	@FindBy(xpath = "//table[@id='data_grid_column1']/tbody//td/div")
	private List<WebElement> listConfigurationPreferencesJQGrid;

    @FindBy(xpath = "//input[@id='gs_preferenceId']")
    private TextInput textJqGridNameFilter;

	@FindBy(xpath = "//table[@id='users']/tbody//td/a/strong")
	private List<WebElement> listUsersAnchors;

	@FindBy(xpath = "//span[text()='Permissions']/..")
	private Link linkUserPermissions;

	@FindBy(xpath = "//input[@id = 'permission.1902']")
	private CheckBox linkPermissionConfPlugin;

	@FindBy(xpath = "//span[contains(text(),'Copy Company')]/..")
	private Link linkCopyCompany;

	@FindBy(xpath = "//input[@id='adminEmail']")
	private TextInput textAdminEmail;

	@FindBy(xpath = "//input[@id='isCompanyChild']")
	private CheckBox cbChildCompany;

	@FindBy(xpath = "//input[@id='childCompany']")
    private TextInput textTemplateCompanyName;

	@FindBy(xpath = "//button/span[text()='Yes']/..")
	private Button btnConfirmYes;

	@FindBy(xpath = "//button/span[text()='No']/..")
	private Button btnConfirmNo;

	@FindBy(xpath = "//p[contains(text(),'New Company has been created')]")
	private TextBlock textCompanyCredentials;

	@FindBy(xpath = "//input[@id='description']")
	private TextInput textOrderPeriodDescription;

	@FindBy(xpath = "//select[@id='periodUnitId']")
	private Select selectOrderPeriodUnit;

	@FindBy(xpath = "//input[@id='value']")
	private TextInput textOrderPeriodValue;

	@FindBy(xpath = "//strong[text()='Should use JQGrid for tables']/parent::a")
	private HtmlElement htmlUseJqGridForTables;

	@FindBy(xpath = "//div[text()='Should use JQGrid for tables']/parent::td")
	private HtmlElement htmlIsJqGridForTables;

	@FindBy(xpath = "//strong[text()='ITG invoice notification']/parent::a")
	private HtmlElement htmlTgiNotification;

	@FindBy(xpath = "//input[@id='preference.value']")
	private TextInput textConfigPreferenceValue;

	@FindBy(xpath = "//input[@id='description']")
	private TextInput textAccountTypeName;

	@FindBy(xpath = "//input[@id='mainSubscription.nextInvoiceDayOfPeriod']")
	private TextInput textAccountTypeBillingCycle;

	@FindBy(xpath = "//input[@id='invoiceDesign']")
	private TextInput textAccountTypeInvoiceDesign;

	@FindBy(xpath = "//table[@id='periods']")
	private Table tableAccountTypeNames;

	@FindBy(xpath = "//input[@class='cb checkbox']")
	private CheckBox cbApplied;

	@FindBy(xpath = "//table[@id='periods']/tbody//td/a/strong")
	private List<WebElement> listAccountTypeNames;

	@FindBy(xpath = "//table[@id='categories']")
	private Table tableCategories;

	@FindBy(xpath = "(//table[@id='periods']/tbody/tr/td/a)[1]")
	private Link linkRecentAccountType;

	@FindBy(xpath = "//span[text()='Add Information Type']")
	private Link linkAddNewInformation;

	@FindBy(xpath = "//input[@id='name']")
	private TextInput textInformationTypeName;

	@FindBy(xpath = "//span[text()='Add New Metafield']")
	private Link linkAddNewMetafield;

	@FindBy(xpath = "//span[text()='-']/parent::li")
	private HtmlElement htmlMetaFieldsTabActivator;

	@FindBy(xpath = "//span[text()='-']")
	private Link linkSpanNewMetafieldTab;

	@FindBy(xpath = "//input[@id='metaField0.name']")
	private TextInput textMetaFieldName;

	@FindBy(xpath = "//select[@id='fieldType0']")
	private Select selectMetaFieldType;

	@FindBy(xpath = "//li[contains(@class,'active')]/following::li//a/span[contains(text(),'Update')]")
	private Link linkUpdateMetaField;

	@FindBy(xpath = "//div[@id='review-box']//span[text()='Save Changes']")
	private Link linkSaveChangesToInformationType;

	@FindBy(xpath = "//span[text()='Prices']")
	private Link linkAccountTypePrices;

	@FindBy(xpath = "//select[@id='typeId']")
	private Select selectAccountTypeProductCategory;

	@FindBy(xpath = "//table[@id='products']/tbody//td/a/strong")
	private List<WebElement> listProductPrices;

	@FindBy(xpath = "//span[text()='Add price']/..")
	private Link linkAddPrice;

	@FindBy(xpath = "//input[@id = 'model.0.rateAsDecimal']")
	private TextInput inputRate;

	@FindBy(xpath = "//a[contains(@href,'editAccountTypePrice')]")
	private Link linkIbtEdit;

	@FindBy(xpath = "//select[@id='templateId']")
	private Select selectPaymentTemplate;

	@FindBy(id = "methodName")
	private TextInput textMethodName;

	@FindBy(id = "isRecurring")
	private CheckBox cbIsRecurring;

	@FindBy(id = "allAccount")
	private CheckBox cbAllAccountTypes;

	@FindBy(xpath = "//span[text()='cc.number']/parent::li")
	private HtmlElement htmlCcNumberTabActivatorLink;

	@FindBy(xpath = "//span[text() = 'cc.number']")
	private Link linkCcNumberTab;

	@FindBy(xpath = "//span[text()='cc.number']/parent::li/following::li[contains(@id,'editor')]//select[contains(@id,'newDescriptionLanguage')]")
	private Select selectCcNrErrorLang;

	@FindBy(xpath = "//span[text()='cc.number']/parent::li/following::li[contains(@id,'editor')]//a[contains(@onclick,'addNewDescription')]")
	private Link linkAddCcNrErrMsg;

	@FindBy(xpath = "//span[text()='cc.number']/../..//label[text()='English Error message']/../div/input[1]")
	private TextInput textCardExpiryMsg;

	@FindBy(xpath = "//span[text()='cc.number']/parent::li/following::li//div[contains(@id,'descriptions')]//a[contains(@onclick,'removeDescription')]")
	private Link linkRemoveCcNrErrMsg;

	@FindBy(xpath = "//span[text()='cc.number']/parent::li/following::li[contains(@id,'editor')]//a//span[text()='Update']")
	private Link linkUpdateCard;

	@FindBy(xpath = "//span[text()='cc.expiry.date']/parent::li")
	private HtmlElement htmlExpiryTabActivatorLink;

	@FindBy(xpath = "//span[text()='cc.expiry.date']")
	private Link linkExpiryDateTab;

	@FindBy(xpath = "//span[text()='cc.expiry.date']/parent::li/following::li[contains(@id,'editor')]//select[contains(@id,'newDescriptionLanguage')]")
	private Select selectExpiryDateErrorLanguage;

	@FindBy(xpath = "//span[text()='cc.expiry.date']/parent::li/following::li[contains(@id,'editor')]//a[contains(@onclick,'addNewDescription')]")
	private Link linkIbtAddExpiryDateErrMsg;

	@FindBy(xpath = "//span[text()='cc.expiry.date']/../..//label[text()='English Error message']/../div/input[1]")
	private TextInput textExpiryDateMessage;

	@FindBy(xpath = "//span[text()='cc.expiry.date']/parent::li/following::li//div[contains(@id,'descriptions')]//a[contains(@onclick,'removeDescription')]")
	private Link linkIbtRemoveExpiryDateErrMsg;

	@FindBy(xpath = "//span[text()='cc.expiry.date']/parent::li/following::li[contains(@id,'editor')]//a//span[text()='Update']")
	private Link linkUpdateDate;

	@FindBy(xpath = "//div[@class = 'buttons']//a[contains(@onclick,'submit')]")
	private Link linkSelect;

	@FindBy(xpath = "//div[@id='breadcrumbs']")
	private HtmlElement htmlBreadcrumbs;

	@FindBy(id = "description")
	private TextInput textCompanyDescription;

	@FindBy(id = "address1")
	private TextInput textCompanyAddress;

	@FindBy(id = "city")
	private TextInput textCompanyCity;

	@FindBy(id = "stateProvince")
	private TextInput textCompanyState;

	@FindBy(id = "failedEmailNotification")
    private TextInput textCompanyFailedNotificationEmail;

    @FindBy(xpath = "//span[contains(text(),'API')]/..")
    private Link linkPermissionsApi;

    @FindBy(id = "permission.120")
    private CheckBox cbApiAccess;

	@FindBy(id = "permission.1901")
	private CheckBox cbViewPlugin;

	@FindBy(id = "permission.1902")
	private CheckBox cbEditPlugin;

	@FindBy(id = "permission.1903")
	private CheckBox cbEditRole;

	@FindBy(xpath = "//div[@id='error-messages']/following-sibling::div//p")
	private HtmlElement htmlPluginDenied;

	@FindBy(xpath = "//table[@id='roles']//strong")
	private List<WebElement> listRoles;

    @FindBy(xpath = "//span[contains(text(),'Menu')]/..")
    private Link linkMenuApi;

	@FindBy(xpath = "//label[text()='Show agent menu']/../input[2]")
	private CheckBox cbShowAgentMenu;

	@FindBy(xpath = "//label[text()='Edit agent']/../input[2]")
	private CheckBox cbEditAgent;

	@FindBy(xpath = "//label[text()='View agent details']/../input[2]")
	private CheckBox cbViewAgentDetails;

	@FindBy(xpath = "//a[@class='submit delete']")
	private Link linkDelete;

	@FindBy(id = "creditLimitAsDecimal")
	private TextInput textCreditLimit;

	@FindBy(id = "creditNotificationLimit1AsDecimal")
	private TextInput textCreditLimitNotOne;

	@FindBy(id = "creditNotificationLimit2AsDecimal")
	private TextInput textCreditLimitNotTwo;

	@FindBy(xpath = "//div[@id='messages']//ul/li")
	private HtmlElement textValidationMessage;

	@FindBy(id = "payment-method-select")
	private Select selectPaymentMethodType;

	@FindBy(xpath = "//div[@class='table-box']//tr//td//a")
	private List<WebElement> listAssetField;

	@FindBy(id = "metaField.name")
	private TextInput textMdMetaFieldName;

	@FindBy(id = "metaField.dataType")
	private Select selectMdMetaFieldDataType;

	@FindBy(id = "mandatoryCheck")
	private CheckBox cbNewMandatory;

	@FindBy(id = "defaultValue")
	private TextInput textMdMetaFieldDefault;

	@FindBy(id = "metaField.validationRule.ruleType")
	private Select selectMdMetaFieldValidationRuleType;

	@FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'save')]/span[text()='Save Changes']/..")
	private Link linkMdMetaFieldSaveChanges;

	@FindBy(xpath = "//table[@id='roles']")
	private Table tableVerifyCreatedMetaData;

	@FindBy(xpath = "//select[@id='vis-cols-multi-sel-group-left']")
	private Select selectMetaDataGroup;

	@FindBy(xpath = "//a[@id='vis-cols-multi-sel-to-right']")
	private Link linkArrowToAdd;

	@FindBy(xpath = "//input[@id='name']")
	private TextInput textGroupMetaDataName;

	@FindBy(id = "disableCheck0")
	private CheckBox cbDisabled;

	@FindBy(xpath = "//select[contains(@id,'dataType')]")
	private Select selectInfoMetaDataType;

	@FindBy(xpath = "//div[@class='lang_description_1']//input[@id='obj_1_description_1']")
	private TextInput inputNameEnglish;

	@FindBy(xpath = "//*[@id='obj[1].order']")
	private TextInput inputOrderText;

	@FindBy(xpath = "//*[@id='obj[1].applyToOrder']")
	private CheckBox cbSecondRow;

	@FindBy(xpath = "//*[@id='obj[0].applyToOrder']")
	private HtmlElement cbFirstRow;

	@FindBy(xpath = "//table[@id='invoiceTemplates']")
	private Table tableInvoiceTemplateNames;

	@FindBy(xpath = "//*[@id='17']/em")
	private Link linkPluginRowNr17;

	@FindBy(xpath = "//*[@id='processingOrder']")
	private TextInput textPluginProcessingOrder;

	@FindBy(xpath = "//*[@id='typeId']")
	private Select selectAddNewPlugin;

	@FindBy(xpath = "//div[@class='heading']//strong")
	private TextBlock textPluginHeader;

	@FindBy(xpath = "//*[@id='orderChangeStatusesTable']/tbody/tr[2]/td[5]/a/img/..")
    private Link linkSecondRowApplyIndicator;

	@FindBy(xpath = "//*[@id='orderChangeStatusesTable']/tbody/tr[3]/td[5]/a/img")
	private HtmlElement thirdRowApplyIndicator;

	@FindBy(xpath = "//*[@id='column1']/div/table/thead/tr/th[2]")
	private TextBlock textPluginCategoriesHeader;

	@FindBy(xpath = "//*[@id='CUSTOMER']/strong")
	private HtmlElement linkCustomerMetaField;

	@FindBy(xpath = "//*[@id='column1']/div[1]/table/thead/tr/th")
	private TextBlock textMetaFieldHeader;

	@FindBy(xpath = "//*[@id='metaField.name']")
	private TextInput textMfName;

	@FindBy(xpath = "//*[@id='metaField.dataType']")
	private Select selectMfDataType;

	@FindBy(xpath = "//*[@id='mandatoryCheck']")
	private CheckBox cbMfMandatory;

	@FindBy(xpath = "//*[@class='table-box']//strong[contains(text(),'Salary')]")
	private HtmlElement linkSalarySearch;

	@FindBy(xpath = "(//button[@type='button'])[3]")
	private Button buttonYesButtonForDeleteButton;

	@FindBy(xpath = "//a[contains(text(),'Payment Method')]")
	private Link linkPaymentMethod;

	@FindBy(xpath = "//div[@class='menu-items']/ul/li/a[contains(text(),'Account Type')]")
	private Link linkAccountType;

	@FindBy(xpath = "//div[@id='messages']/div[@class='msg-box error']/strong")
	private HtmlElement htmlStrongErrorMessage;

	@FindBy(xpath = "//div[@id='messages']/div[@class='msg-box error']/ul/li")
	private HtmlElement htmlStrongErrorMessage1;

	@FindBy(xpath = "//a[contains(.,'Order Statuses')]")
	private Link linkOrderStatuses;

	@FindBy(xpath = "//a[contains(.,'INVOICE')]")
	private Link linkInvoiceFlag;

	@FindBy(xpath = "//a[contains(.,'FINISHED')]")
	private Link linkFinishedFlag;

	@FindBy(xpath = "//a[contains(.,'NOT_INVOICE')]")
	private Link linkNotInvoiceFlag;

	@FindBy(xpath = "//a[contains(.,'SUSPENDED_AGEING')]")
	private Link linkSuspendAgeingFlag;

	@FindBy(xpath = "//a[contains(.,'Active')]")
	private Link linkInvoiceDesc;

    @FindBy(xpath = "//a[contains(.,'Finished')]")
    private Link linkFinishedDesc;

	@FindBy(xpath = "//a[contains(.,'Suspended')]")
	private Link linkNotInvoiceDesc;

	@FindBy(xpath = "//a[contains(.,'Suspended ageing(auto)')]")
	private Link linkSuspendAgeingAutoDesc;

	@FindBy(xpath = "//input[@id='description']")
	private TextInput textNewDescription;

	@FindBy(xpath = "//select[contains(@id,'orderStatusFlag')]")
	private Select selectOrderStatusFlagType;

	@FindBy(xpath = "//div[@id='error-messages']/ul/li")
	private HtmlElement htmlEnumerationMessage;

	@FindBy(xpath = "//div[@id='messages']//div[@class='msg-box error'][1]/ul/li")
	private HtmlElement htmlErrorMessageEnumeration;

	@FindBy(xpath = "(//div[@id='error-messages']/ul/li)[1]")
	private HtmlElement htmlEnumerationMessageName;

	@FindBy(xpath = "(//div[@id='error-messages']/ul/li)[2]")
	private HtmlElement textEnumerationMsgValue;

	@FindBy(id = "name")
	private TextInput textEnumerationName;

	@FindBy(id = "usagePool.cyclePeriodValue")
	private TextInput textCyclePeriod;

	@FindBy(id = "usagePool.quantity")
	private TextInput textFupQuantity;

	@FindBy(id = "usagePool.consumptionActions.1.percentage")
	private TextInput textFupConsumptionPercentage;

	@FindBy(id = "usagePool.consumptionActions.2.percentage")
	private TextInput textFupConsumptionPercentage2;

	@FindBy(id = "usagePool.consumptionActions.2.productId")
	private TextInput textFupProductId;

	@FindBy(xpath = "//input[contains(@id,'.value')]")
	private TextInput textZeroEnumValue;
	
	@FindBy(xpath = "//tbody[@id='enum-body']//td[text()=1]/..//input[contains(@id,'.value')]")
	private TextInput textFirstEnumValue;

	@FindBy(xpath = "//table[@id='enumerations']/tbody/tr/td//strong")
	private List<WebElement> listEnumFirstCell;

    @FindBy(xpath = "//table[@id='enumerations']")
    private Table tableEnumerations;

    @FindBy(xpath = "//input[@checked='checked' and contains(@id,'inUse')]/following::input[1]")
    private List<TextInput> currentSelectedCurrencies;

	@FindBy(xpath = "//tbody[@id='enum-body']//a[contains(@onclick, 'addEnumerationValue')]")
	private Link linkAddEnumerationValue;

    @FindBy(xpath = "//tbody[@id='enum-body']//a[contains(@onclick, 'removeEnumerationValue')]")
	private Link linkRemoveEnumerationValue;

	@FindBy(xpath = "//li[contains(@class,'editor')]//input[contains(@name, '.name')]")
	private TextInput textMetaFieldName2;

	@FindBy(xpath = "//label[text()='MetaField Type']/following::select[1]")
	private Select selectMetaFieldType2;

	@FindBy(xpath = "//a[contains(text(),'Meta Fields')]")
	private Link linkMetaFields;

/*	@FindBy(xpath = "//ul[@id='metafield-ait']/form[1]//span[text()='Not Mandatory']")
	private ElementField METAFIELD_BAR;*/

	@FindBy(xpath = "//label[text()='Validation Rule']/following::select[contains(@id,'validationRule')]")
	private Select selectValidationRule;

	@FindBy(xpath = "//label[text()='Include in Notifications']/following::select")
	private Select selectIncludeNotification;

	@FindBy(xpath = "(//li[contains(@class,'editor')]//input[contains(@name, '.name')])[2]")
	private TextInput textMetaFieldNameIdx2;

	@FindBy(xpath = "(//label[text()='MetaField Type']/following::select[1])[2]")
	private Select selectMetaFieldTypeIdx2;

	@FindBy(xpath = "(//li[contains(@class,'active')]/following::li//a/span[contains(text(),'Update')])[2]")
	private Link linkUpdateMetaFieldIdx2;
	
	@FindBy(xpath = "//*[@id='ORDER']/strong")
	private Link linkSelectOrderInMetaField;

	@FindBy(xpath = "//*[@id='PAYMENT']/strong")
	private Link linkSelectPaymentInMetaField;
	
	@FindBy(xpath = "//span[text()='Clone']")
	private Link linkClone;
	
	@FindBy(xpath = "//table[@id='data_grid_column1']/tbody/tr[@id='63']/td/div")
	private HtmlElement htmlReconfigure;
	
	@FindBy(xpath = "//li/a[contains(text(), 'Order Change Statuses')]")
	private Link linkChangeOrderStatuses;

	@FindBy(xpath = "//ul[@class='list']//li/a[contains(text(), 'Order Change Types')]")
	private Link linkChangeOrderType;

	@FindBy(xpath = "//a/span[contains(text(), 'Save Changes')]")
	private Link linkSaveChanges2;

	@FindBy(xpath = "//input[@name='allowOrderStatusChange']")
	private CheckBox cbAllowOrderStatusChange;
	
	@FindBy(xpath = "//tr[2]//input[@id='obj_1_description_1']")
	private TextInput textOrderStatusChangeName;

	@FindBy(xpath = "//tr[2]//input[contains(@name,'.order')]")
	private TextInput textOrderStatusOrder;
	
	@FindBy(xpath = "//tr[2]//input[contains(@name,'.applyToOrder')]")
	private CheckBox cbApplyToOrder;
	
	@FindBy(xpath = "//div[@class='form-columns']//div[@class='inp-bg ']/input[@id='name']")
	private TextInput textOrderEnglishName;

	@FindBy(xpath = "//select[@id='itemTypes_selector']")
	private Select selectProductCategory;
	
	@FindBy(xpath = "//a[contains(text(),'Order Periods')]")
	private Link linkOrderPeriod;
	
	@FindBy(xpath = "//td[contains(text(),'Euro')]/following::input[@type='checkbox'][1]")
	private CheckBox cbEuroCurrency;

    @FindBy(xpath = "//td[contains(text(),'Euro')]/following::input[@type='text'][1]")
    private TextInput textEuroCurrencyRate;

    @FindBy(xpath = "//td[contains(text(),'United States Dollar')]/following::input[@type='text'][1]")
    private TextInput textDollarCurrencyRate;

    @FindBy(xpath = "//a[contains(text(),'Currencies')]")
	private Link linkCurrencies;

	@FindBy(xpath = "//select[@id='defaultCurrencyId']")
	private Select selectDefaultCurrency;

    @FindBy(xpath = "//table[@class='dataTable']/tbody/tr[1]/td[2]/a")
    private Link linkMetaFieldId;

    @FindBy(xpath = "/html/body/div/div[2]/div[3]/div[3]/div[2]/div/div/div[3]/a/span[text()='Edit']")
    private Link linkEditAitType;

    @FindBy(xpath = "//input[@id='useForNotifications']")
    private CheckBox cbUserNotificationAccountType;

    @FindBy(xpath = "//div[@id='infoTypeName-change-dialog']/following::button[contains(span/text(),'Yes')]")
    private Button btnYesPopupAit;

    @FindBy(xpath = "//select[@id='aitName']")
    private Select selectAitIncludeNotifications;

    @FindBy(xpath = "//input[@id='user.userName']")
    private TextInput txtLoginName;

    @FindBy(xpath = "//input[@id='contact.firstName']")
    private TextInput txtFirstName;

    @FindBy(xpath = "//input[@id='contact.lastName']")
    private TextInput txtLastName;

    @FindBy(xpath = "//input[@id='contact.phoneCountryCode1']")
    private TextInput txtFirstPhone;

    @FindBy(xpath = "//input[@id='contact.phoneAreaCode']")
    private TextInput txtPhoneAreaCode;

    @FindBy(xpath = "//input[@id='contact.phoneNumber']")
    private TextInput txtContactPhoneNumber;

    @FindBy(xpath = "//input[@id='contact.email']")
    private TextInput txtEmail;

    @FindBy(xpath = "//input[@id='contact.organizationName']")
    private TextInput txtOrganization;

    @FindBy(xpath = "//input[@id='contact.address1']")
    private TextInput txtAddress1;

    @FindBy(xpath = "//input[@id='contact.stateProvince']")
    private TextInput txtStateProvince;

    @FindBy(id = "contact.countryCode")
    private Select selectConuntryCode;

    @FindBy(xpath = "//input[@id='contact.postalCode']")
    private TextInput txtZipPostalCode;

    @FindBy(xpath = " //span[text()='Save Changes']")
    private Link linkSaveChangesSeller;

    @FindBy(xpath = "//span[text()='Create Reseller']")
    private Link linkCreateSeller;

    @FindBy(id = "defaultCurrencyId")
    private Select selectCurrencyDefault;

//    @FindBy(xpath = "//span[text()='Save Changes']")
//    private Link linkSaveChanges;

    @FindBy(xpath = "//span[text()='Delete']")
    private Link linkDeleteAitType;

    @FindBy(xpath = "//span[text() = 'Yes']")
    private Button buttonDeleteAitTypeYes;

    @FindBy(xpath = "//table[@id='categories']/tbody/tr/td/a/strong")
    private List<WebElement> listPayments;

    @FindBy(xpath = "//span[text()='Add Notification']")
    private Link linkAddPaymentNotification;

    @FindBy(xpath = "//select[@name='newDescriptionLanguage']")
    private Select selectAddPaymentNotificationDescLang;

    @FindBy(xpath = "//a[contains(@onclick,'addNewDescription')]")
    private Link linkAddPaymentNotificationDesc;

    @FindBy(xpath = "//input[contains(@name, 'notification.description[0].content')]")
    private TextInput textAddPaymentNotificationDesc;

    @FindBy(xpath = "//span[text()='Save Changes']")
    private Link linkNotificationSaveChanges;

    @FindBy(xpath = "//div[@id='notification-box']/div[@class='table-box']/table/tbody/tr[1]/td[1]/a//em")
    private Link linkRecentNotification;

    @FindBy(xpath = "//span[text()='Edit']")
    private Link linkEditPaymentNotification;

	@FindBy(xpath = "//div[@class='menu-items']/ul/li/a[contains(text(),'Entity Logos')]")
	private Link linkEntityLogos;

    @FindBy(id = "mediumTypes")
    private Select selectPaymentValue;

    @FindBy(id = "currencies.3.inUse")
    private CheckBox cbEuro;

    @FindBy(id = "currencies.4.inUse")
    private CheckBox cbYen;

    @FindBy(xpath = "//td[contains(text(),'Yen')]/following::input[@type='text'][1]")
    private TextInput textYenCurrencyRate;

    @FindBy(id = "currencies.5.inUse")
    private CheckBox cbPoundSterling;

    @FindBy(xpath = "//td[contains(text(),'Sterling')]/following::input[@type='text'][1]")
    private TextInput textSterlingCurrencyRate;

    @FindBy(id = "currencies.6.inUse")
    private CheckBox cbWon;

    @FindBy(xpath = "//td[contains(text(),'Won')]/following::input[@type='text'][1]")
    private TextInput textWonCurrencyRate;

    @FindBy(xpath = "//th[text()='Plug-ins categories']/parent::tr/parent::thead/parent::table/tbody/tr/td[2]//em")
    private List<WebElement> listPlugins;

    @FindBy(xpath = "//th[contains(text(),'Meta Field Categories')]/parent::tr/parent::thead/parent::table/tbody/tr/td/a/strong")
    private List<WebElement> listMetaFieldsCategories;

    @FindBy(xpath = "//th[text()='Meta Fields']/parent::tr/parent::thead/parent::table")
    private Table tableMetaFields;

    @FindBy(xpath = "//input[@name= 'messageSections[1].notificationMessageLines.content']")
    private TextInput textPaymentNotificationSubject;

    @FindBy(xpath = "//a[@class='submit save button-primary']/span[text()='Save Changes']")
    private Link linkPaymentNotificationSave;

    @FindBy(xpath = "//td[contains(text(),'Subject')]/following-sibling::td[1]")
    private HtmlElement htmlVerifyTableData;

    @FindBy(xpath = "//th[text()='Notification']/../../following-sibling::tbody/tr[1]/td/a/em")
    private Link linkNotification;

    @FindBy(xpath = "//input[@id='notifyParent']")
    private CheckBox cbNotifyParent;

    @FindBy(xpath = "//input[@id='notifyAdmin']")
    private CheckBox cbNotifyAdmin;

    @FindBy(xpath = "//input[@id='notifyPartner']")
    private CheckBox cbNotifyAgent;

    @FindBy(xpath = "//input[@id='notifyAllParents']")
    private CheckBox cbNotifyAllParents;

    @FindBy(xpath = "//td[contains(text(),'Notify Admin?:')]/following-sibling::td[1]")
    private HtmlElement htmlVerifyCbAdmin;

    @FindBy(xpath = "//td[contains(text(),'Notify Agent?:')]/following-sibling::td[1]")
    private HtmlElement htmlVerifyCbAgent;

    @FindBy(xpath = "//td[contains(text(),'Notify Parent?:')]/following-sibling::td[1]")
    private HtmlElement htmlVerifyCbNotifyParent;

    @FindBy(xpath = "//td[contains(text(),'Notify All Parent')]/following-sibling::td[1]")
    private HtmlElement htmlVerifyCbNotifyAllParent;

    @FindBy(xpath = "(//td/a)[1]")
    private Link linkVerifyBlackListedCustomer;

    @FindBy(xpath = "//td/a[contains(text(), '************1152')]")
    private Link linkCreditCard;

    @FindBy(xpath = "//li/a[contains(text(), 'Blacklist')]")
    private Link linkBlacklist;

    @FindBy(xpath = "//div[@class='inp-bg ']/input[@id='filterBy']")
    private TextInput textSearchCustomer;

    @FindBy(xpath = "//table[@id='periods']//tbody//td/a/strong")
    private List<WebElement> listAccountTypes;

    @FindBy(xpath = "//span[contains(text(), 'Delete')]")
    private Link linkDeleteAccountType;

    @FindBy(xpath = "//table/tbody/tr/td[2]/p[starts-with(@id,'confirm-dialog-delete')]")
    private HtmlElement htmlConfMsgVerify;

    @FindBy(xpath = "//span[contains(text(), 'No')]")
    private Link linkConfirmNo;

    @FindBy(xpath = "//tr/td/a/strong[contains(text(), 'ACCOUNT_TYPE')]")
    private Link linkMfAccountType;

    @FindBy(xpath = "//a[contains(text(), 'Import Metafields')]")
    private Link linkImportMetafields;

    @FindBy(xpath = "//table[@id='metafields']//tbody//td/a/span")
    private List<WebElement> listImportMetafields;

    @FindBy(xpath = "//li[@class='mf  active']")
    private HtmlElement htmlAddNewMeta;

    @FindBy(xpath = "//a[@class='submit']/span")
    private Link linkEditPlugin;

    @FindBy(xpath = "//input[@id='plg-parm-port']")
    private TextInput textPluginPortNumber;

    @FindBy(xpath = "//div[@class='pager-box']/div[2]/a")
    private List<WebElement> htmlPagerBox;

    @FindBy(xpath = "//div[@id='notification-box']//div[@class='table-box']//tr//td//a/strong")
    private List<WebElement> listNotificationNames;

    @FindBy(xpath = "//th[text()='Notification']/ancestor::table/tbody/tr//a/strong")
    private List<WebElement> linkNotificationId;

    @FindBy(xpath = "//label[text()='Subject:']/../div/input[@type='text']")
    private TextInput textSubject;

    @FindBy(xpath = "//label[text()='Body (Text):']/../div/textarea")
    private TextInput textEmailBody;

    @FindBy(xpath = "//label[text()='Active?:']/../div/input[@class='cb checkbox']")
    private CheckBox cbActive;

    @FindBy(xpath = "//th[text()='Type']/ancestor::table/tbody/tr/td[2]/a/em")
    private List<WebElement> listPluginTypes;

    @FindBy(xpath = "//select[@name='newDescriptionLanguage']")
    private Select selectFupDescLang;

    @FindBy(xpath = "//a[contains(@onclick,'addNewDescription')]")
    private Link linkAddFupDesc;

    @FindBy(xpath = "//label[contains(text(), 'English Name')]/following::input[contains(@name,'usagePool.names')][1]")
    private TextInput textFupDesc;

	@FindBy(xpath = "//div[contains(@id,\"file-upload-cont-logo\")]//span[@class='filebutton']")
	private Button btnNavigationBarLogoUpload;

	@FindBy(xpath = "//div[contains(@id,\"file-upload-cont-favicon\")]//span[@class='filebutton']")
	private Button btnFaviconLogoUpload;

	@FindBy(xpath = ".//*[@id='flash-info']/ul/li")
	private TextBlock logoUploadSuccessMessage;

	@FindBy(xpath = ".//*[@id='flash-errormsg']/ul/li")
	private TextBlock logoUploadFailMessage;

    @Autowired
    protected MessagesPage messagesPage;

    public ConfigurationPage(WebDriver driver) {
        super(driver);
    }

	public void verifyAddPluginPageHeader() {
		String headerText = textPluginHeader.getText();

		if (! headerText.contains("ADD NEW PLUG-IN")) {
			throw new RuntimeException("Test Case failed: " + headerText);
		}
	}

	public void verifyPluginscategoriesPageHeader() {
		String headerText = textPluginCategoriesHeader.getText();

		if (! headerText.contains("Plug-ins categories")) {
			throw new RuntimeException("Test Case failed: " + headerText);
		}
	}

	public void setNumberOfRowsToTwo() {
		boolean flag = false;
		flag = thirdRowApplyIndicator.exists();
		if (flag) {
            linkSecondRowApplyIndicator.click();
			this.clickSaveChangesButton();
			this.enterTestDataInBox2("secondRowInfo", "ai");
		} else {
            inputNameEnglish.setText("");
			this.enterTestDataInBox2("secondRowInfo", "ai");
		}
	}

	private void enterTestDataInBox2(String testDataSetName, String category) {
		String secondRowNameTestData = TestData.read("PageConfiguration.xml", testDataSetName, "secondRowNameTestData", category);
		String secondRowOrderTestData = TestData.read("PageConfiguration.xml", testDataSetName, "secondRowOrderTestData", category);

		this.setSecondRowTestData(secondRowNameTestData, secondRowOrderTestData);
		this.clickPluginAddMoreParametersIcon();
		this.clickSaveChangesButton();
	}

	private void setSecondRowTestData(String secondRowNameTestData, String secondRowOrderTestData) {
        inputNameEnglish.setText(secondRowNameTestData);
        inputOrderText.setText(secondRowOrderTestData);
        cbSecondRow.deselect();
	}

	public void checkboxOrderChangeStatuses() {
		boolean flagRowOne = cbFirstRow.exists();
		boolean flagRowTwo = cbSecondRow.exists();

		if ((! flagRowOne) || (! flagRowTwo)) {
			throw new RuntimeException("Test Case failed: " + flagRowOne + flagRowTwo);
		}
		this.clickSaveChangesButton();
	}

	public void clickOnEventListner() {
        linkPluginRowNr17.click();
		this.clickAddNewButton();
	}

	public void enterTestDataInOnPlugnin(String testdataset, String category) {

		String dropDownValueToBeSelected = "OrderChangeApplyOrderStatusTask";
        selectAddNewPlugin.selectByVisibleText(dropDownValueToBeSelected);

		String addNewPluginOrderValue = TestData.read("PageConfiguration.xml", testdataset, "AddNewPluginOrderValue", category);

		this.setOrderTestData(addNewPluginOrderValue);
		this.clickSavePlugin();
	}

	private void setOrderTestData(String orderNo) {
        textPluginProcessingOrder.setText(orderNo);
	}

	public void addAccountTypePriceToSelectedProduct(String productCode, String testDataSetName, String category) {
		String productCategory = TestData.read("PageConfiguration.xml", testDataSetName, "productCategory", category);
		String productRate = TestData.read("PageConfiguration.xml", testDataSetName, "rate", category);

		this.selectProductCategoryInAccountTypePrices(productCategory);
		this.selectProductInAccountTypePricesProductsTable(productCode);
		this.clickAddPrice();
		this.setAccountTypePriceRate(productRate);
		this.clickSaveChangesButton();
	}

	public void addBillingProcess(String testDataSetName, String category) {
		String nextRunDate = TestData.read("PageConfiguration.xml", testDataSetName, "nextRunDate", category);
		String billingPeriod = TestData.read("PageConfiguration.xml", testDataSetName, "billingPeriod", category);

		this.setBillingProcessNextRunDate(nextRunDate);
		this.selectBillingProcessPeriod(billingPeriod);
		this.clickSaveChangesToBillingProcess();
	}

	private void addCCNumberErrorMsg(String cardErrorMsg) {
		try {
			// click on remove button for error message if any message is
			// already there
            linkRemoveCcNrErrMsg.click();
		} catch (Exception e) {
			// eat exception
		}

        selectCcNrErrorLang.selectByVisibleText("English");
        linkAddCcNrErrMsg.click();
        textCardExpiryMsg.setText(cardErrorMsg);
        linkUpdateCard.click();
	}

	public void addCollectionsAgeingStep(CollectionAgeingStep ageingStep, String testDataSetName, String category) {
		String step = TestData.read("PageConfiguration.xml", testDataSetName, "step", category);
		String days = TestData.read("PageConfiguration.xml", testDataSetName, "days", category);

		boolean notification = TextUtilities.compareValue(
				TestData.read("PageConfiguration.xml", testDataSetName, "notification", category), "true", true, TextComparators.equals);
		boolean payment = TextUtilities.compareValue(TestData.read("PageConfiguration.xml", testDataSetName, "payment", category), "true",
				true, TextComparators.equals);
		boolean suspend = TextUtilities.compareValue(TestData.read("PageConfiguration.xml", testDataSetName, "suspend", category), "true",
				true, TextComparators.equals);

		switch (ageingStep) {
		case FIRST:
			this.setStepOne(step);
			this.setForDaysOne(days);
			this.checkNotificationsOne(notification);
			this.checkPaymentOne(payment);
			this.checkSuspendOne(suspend);
			break;
		case SECOND:
			this.setStepTwo(step);
			this.setForDaysTwo(days);
			this.checkNotificationsTwo(notification);
			this.checkPaymentTwo(payment);
			this.checkSuspendTwo(suspend);
			break;
		case THIRD:
			this.setStepThree(step);
			this.setForDaysThree(days);
			this.checkNotificationsThree(notification);
			this.checkPaymentThree(payment);
			this.checkSuspendThree(suspend);
			break;
		case FOURTH:
			this.setStepFour(step);
			this.setForDaysFour(days);
			this.checkNotificationsFour(notification);
			this.checkPaymentFour(payment);
			this.checkSuspendFour(suspend);
			break;
		default:
			throw new RuntimeException("Invalid Ageing Step provided to work on. Not defined in enumeration");
		}
	}

	private void addExpiryDateErrorMsg(String dateErrorMsg) {
		try {
			// click on remove button for error message if any message is
			// already there
            linkIbtRemoveExpiryDateErrMsg.click();
		} catch (Exception e) {
			// eat exception
		}
        selectExpiryDateErrorLanguage.selectByVisibleText("English");
        linkIbtAddExpiryDateErrMsg.click();
        textExpiryDateMessage.setText(dateErrorMsg);
        linkUpdateDate.click();
	}

	private void addMetaField(AccountTypeInfo accountTypeInfo, String testDataSetName, String category) {
        activateMetaFieldEditor();
		this.updateMetaFieldDetails(accountTypeInfo, testDataSetName, category);
	}

	public String addNewInformationToSelectedAccountType(AccountTypeInfo accountTypeInfo, String testDataSetName, String category) {
        linkAddNewInformation.click();

		String infoTypeName = TestData.read("PageConfiguration.xml", testDataSetName, "name", category);

		this.setAccountTypeInformationTypeName(infoTypeName);
		this.clickAddNewMetaField();
		this.addMetaField(accountTypeInfo, testDataSetName, category);
		this.clickSaveChangesToInformationType();
		return infoTypeName;
	}

	public void addNewPluginInCategory(String testDataSetName, String category) {
		this.clickAddNewButton();

		String pluginType = TestData.read("PageConfiguration.xml", testDataSetName, "pluginType", category);
		String order = TestData.read("PageConfiguration.xml", testDataSetName, "order", category);

		this.selectPluginType(pluginType);
		this.setPluginOrder(order);
		this.clickSavePlugin();
	}

	public String addPaymentMethodDetails(String testDataSetName, String category) {
		String methodName = TestData.read("PageConfiguration.xml", testDataSetName, "methodName", category);
		String cardErrorMsg = TestData.read("PageConfiguration.xml", testDataSetName, "cardErrorMsg", category);
		String dateErrorMsg = TestData.read("PageConfiguration.xml", testDataSetName, "dateErrorMsg", category);
		boolean isRecurring = TestData.read("PageConfiguration.xml", testDataSetName, "isRecurring", category).equals("true");
		boolean allAccountTypes = TestData.read("PageConfiguration.xml", testDataSetName, "allAccountTypes", category).equals("true");

        explicitWaitForJavascript();
		this.setMethodName(methodName);
		this.checkIsRecurring(isRecurring);
        explicitWaitForJavascript();
		this.checkAllAccountTypes(allAccountTypes);
        explicitWaitForJavascript();
		this.createValRulOnCCNumberMF(cardErrorMsg);
		this.createValRulOnExpiryDateMF(dateErrorMsg);
		this.clickSaveChangesButton();

		return methodName;
	}

	private void changeUserPermissionForEditPlugin(boolean editPlugin) {
		if (editPlugin) {
            linkPermissionConfPlugin.select();
		} else {
            linkPermissionConfPlugin.deselect();
		}
	}

	private void checkAllAccountTypes(boolean allAccountTypes) {
		if (allAccountTypes) {
            cbAllAccountTypes.select();
		} else {
            cbAllAccountTypes.deselect();
		}
        explicitWaitForJavascript();
	}

	private void checkIsRecurring(boolean isRecurring) {
		if (isRecurring) {
            cbIsRecurring.select();
		} else {
            cbIsRecurring.deselect();
		}
	}

	private void checkNotificationsFour(java.lang.Boolean notification) {
		if (notification) {
            cbStep4Notification.select();
		} else {
            cbStep4Notification.deselect();
		}
	}

	private void checkNotificationsOne(java.lang.Boolean notification) {
		if (notification) {
            cbStep1Notification.select();
		} else {
            cbStep1Notification.deselect();
		}
	}

	private void clickOrderStatuses() {
        linkOrderStatuses.click();
	}

	private void checkNotificationsThree(java.lang.Boolean notification) {
		if (notification) {
            cbStep1Notification.select();
		} else {
            cbStep1Notification.deselect();
		}
	}

	private void checkNotificationsTwo(java.lang.Boolean notification) {
		if (notification) {
            cbStep2Notification.select();
		} else {
            cbStep2Notification.deselect();
		}
	}

	private void checkPaymentFour(boolean payment) {
		if (payment) {
            cbStep4PaymentRetry.select();
		} else {
            cbStep4PaymentRetry.deselect();
		}
	}

	private void checkPaymentOne(boolean payment) {
		if (payment) {
            cbStep1PaymentRetry.select();
		} else {
            cbStep1PaymentRetry.deselect();
		}
	}

	private void checkPaymentThree(boolean payment) {
		if (payment) {
            cbStep3PaymentRetry.select();
		} else {
            cbStep3PaymentRetry.deselect();
		}
	}

	private void checkPaymentTwo(boolean payment) {
		if (payment) {
            cbStep2PaymentRetry.select();
		} else {
            cbStep2PaymentRetry.deselect();
		}
	}

	private void checkSuspendFour(boolean suspend) {
		if (suspend) {
            cbStep4Suspended.select();
		} else {
            cbStep4Suspended.deselect();
		}
	}

	private void checkSuspendOne(boolean suspend) {
		if (suspend) {
            cbStep1Syspend.select();
		} else {
            cbStep1Syspend.deselect();
		}
	}

	private void checkSuspendThree(boolean suspend) {
		if (suspend) {
            cbStep3Suspend.select();
		} else {
            cbStep3Suspend.deselect();
		}
	}

	private void checkSuspendTwo(boolean suspend) {
		if (suspend) {
            cbStep2Suspend.select();
		} else {
            cbStep2Suspend.deselect();
		}
	}

	public void clickAccountTypePrices() {
        linkAccountTypePrices.click();
	}

	public void clickAddNewButton() {
        linkAddNewButton.click();
	}

	private void clickAddNewMetaField() {
        linkAddNewMetafield.click();
        explicitWaitForJavascript();
/*
        GlobalController.brw.waitForAjaxElement(this.LT_ADD_NEW_METAFIELD);
		GlobalController.brw.clickLinkText(this.LT_ADD_NEW_METAFIELD);
        GlobalController.brw.waitForAjaxElement(this.METAFIELD_BAR);
*/
        activateMetaFieldEditor();
	}

	private void clickAddPrice() {
        linkAddPrice.click();
	}

	public void clickConfirmPopupYesButton() {
        btnConfirmYes.click();
	}

	public void clickCopyCompanyButton() {
        linkCopyCompany.click();
	}

	public void clickEditAccountTypeButton() {
        linkEdit.click();
	}

	private void clickEditImageAccountTypePrice() {
        linkIbtEdit.click();
	}

	private void clickPluginAddMoreParametersIcon() {
        linkAddOrderChangeStatus.click();
	}

	private void clickFupAction() {
        linkAddModelAttribute.click();
	}

	private void clickPopupRunCollections() {
        linkPopupRunCollections.click();
	}

	public void clickRecentlyCreatedAccountType() {
        linkRecentAccountType.click();
	}

	private void clickRunCollections() {
        explicitWaitForJavascript(21);
        linkRunCollection.click();
	}

	public void clickRunCommmisionToBillingProcess() {
        linkRunCommission.click();
	}

	public void clickSaveChangesButton() {
        linkSaveChanges.click();
	}

	private void clickSaveChangesToBillingProcess() {
        linkSaveChanges.click();
	}

	private void clickSaveChangesTemplatebutton() {
        linkSaveChangesTemplate.click();
	}

	public void clickSaveChangesToCollections() {
        linkSaveChanges.click();
	}

	private void clickSaveChangesToInformationType() {
        linkSaveChangesToInformationType.click();
	}

	private void clickSaveChangesToOrderPeriod() {
        linkSaveChanges.click();
    }

	private void clickSaveChangesToUsersPermissions() {
        linkSaveChanges.click();
	}

	private void clickSavePlugin() {
        linkSavePlugin.click();
	}

	private void clickSelectButton() {
        linkSelect.click();
	}

	private void clickSpecifiedUserInUsersTable(String userName) {
        selectListElementByText(listUsersAnchors, userName);
	}

	private void clickUpdateButton() {
        linkUpdateMetaField.click();
	}

	private void clickUsersPermissionsButton() {
        linkUserPermissions.click();
	}

	public String createAccountType(String testDataSetName, String category) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);
		String billingCycle = TestData.read("PageConfiguration.xml", testDataSetName, "billingCycle", category);
		String invoiceDesign = TestData.read("PageConfiguration.xml", testDataSetName, "invoiceDesign", category);

		this.clickAddNewButton();
		this.setAccountTypeName(accountName);
		this.setAccountTypeBillingCycle(billingCycle);
		this.setAccountTypeInvoiceDesign(invoiceDesign);
		this.clickSaveChangesButton();
        explicitWaitForJavascript();
		return accountName;
	}

	public String editAccountTypeName(String testDataSetName, String category) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);

		this.clickRecentlyCreatedAccountType();
		this.clickEditAccountTypeButton();
		this.setAccountTypeName(accountName);
		this.clickSaveChangesButton();
		return accountName;

	}

	public String createNewOrderPeriod(String testDataSetName, String category) {
		String description = TestData.read("PageConfiguration.xml", testDataSetName, "description", category);
		String unit = TestData.read("PageConfiguration.xml", testDataSetName, "unit", category);
		String value = TestData.read("PageConfiguration.xml", testDataSetName, "value", category);

		this.createNewOrderPeriod(description, unit, value);
		return description;
	}

	private void createNewOrderPeriod(String description, String unit, String value) {
		this.clickAddNewButton();
		this.setOrderPeriodDescription(description);
		this.selectOrderPeriodUnit(unit);
		this.setOrderPeriodValue(value);
		this.clickSaveChangesToOrderPeriod();
	}

	private void createValRulOnCCNumberMF(String cardErrorMsg) {
		String attrVal = htmlCcNumberTabActivatorLink.getAttribute("class");
		if (TextUtilities.compareValue("active", attrVal, true, TextComparators.notContains)) {
            linkCcNumberTab.click();
		}
		this.addCCNumberErrorMsg(cardErrorMsg);
	}

	private void createValRulOnExpiryDateMF(String dateErrorMsg) {
		String attrVal = htmlExpiryTabActivatorLink.getAttribute("class");
		if (TextUtilities.compareValue("active", attrVal, true, TextComparators.notContains)) {
            linkExpiryDateTab.click();
		}
		this.addExpiryDateErrorMsg(dateErrorMsg);
	}

	public String extractCompanyIdFromCompanyCreationMessage() {
		String companyIdStartString = ", ID: ";

		String credentialsMessage = this.getNewCompanyCredentials();

		int companyIdStartIndex = TextUtilities.indexOf(credentialsMessage, companyIdStartString) + companyIdStartString.length();
		String companyId = TextUtilities.trim(TextUtilities.substring(credentialsMessage, companyIdStartIndex).replace(".", ""));

		return companyId;
	}

	public String extractCompanyNameFromCompanyCreationMessage() {
		String companyNameStartString = ", Company Name:";
		String companyIdStartString = ", ID: ";

		String credentialsMessage = this.getNewCompanyCredentials();

		int companyNameStartIndex = TextUtilities.indexOf(credentialsMessage, companyNameStartString) + companyNameStartString.length();
		int companyNameEndIndex = TextUtilities.indexOf(credentialsMessage, companyIdStartString);
		String companyName = TextUtilities.trim(TextUtilities.substring(credentialsMessage, companyNameStartIndex, companyNameEndIndex));

		return companyName;
	}

	public String extractPasswordFromCompanyCreationMessage() {
		String passwordStartString = ", password:";
		String companyNameStartString = ", Company Name:";

		String credentialsMessage = this.getNewCompanyCredentials();

		int passwordStartIndex = TextUtilities.indexOf(credentialsMessage, passwordStartString) + passwordStartString.length();
		int passwordEndIndex = TextUtilities.indexOf(credentialsMessage, companyNameStartString);
		String password = TextUtilities.trim(TextUtilities.substring(credentialsMessage, passwordStartIndex, passwordEndIndex));

		return password;
	}

	public String extractUserNameFromCompanyCreationMessage() {
		String userNameStartString = "User name:";
		String passwordStartString = ", password:";

		String credentialsMessage = this.getNewCompanyCredentials();

		int userNameStartIndex = TextUtilities.indexOf(credentialsMessage, userNameStartString) + userNameStartString.length();
		int userNameEndIndex = TextUtilities.indexOf(credentialsMessage, passwordStartString);
		String userName = TextUtilities.trim(TextUtilities.substring(credentialsMessage, userNameStartIndex, userNameEndIndex));

		return userName;
	}

	private String getNewCompanyCredentials() {
		String credentials = textCompanyCredentials.getText();
		return credentials;
	}

	private boolean isItgInvoiceNotificationPageLoaded() {
        return htmlTgiNotification.exists();
	}

	private boolean isUseJQGridPageLoaded() {
        return htmlUseJqGridForTables.exists();
	}

	private boolean isJQGridPageLoaded() {
        return htmlIsJqGridForTables.exists();
	}

	public void markCompanyAsChildCompany(boolean isChildCompany) {
		if (isChildCompany) {
            cbChildCompany.select();
		} else {
            cbChildCompany.deselect();
		}
	}

	public void setTemplateCompanyName() {
		String templateCompanyName = this.getCompanyDescription();
        textTemplateCompanyName.setText(templateCompanyName);
	}

	public void runCollectionsForDate(String collectionDate) {
		this.setRunCollectionDate(collectionDate);
		this.clickRunCollections();
		this.clickPopupRunCollections();
	}

	public void selectAccountTypeName(String accountTypeName) {
        selectListElementByText(listAccountTypeNames, accountTypeName);
	}

	private void selectProductCategory(String category) {
        selectUsagePoolProductCategory.selectByVisibleText(category);
	}

	private void selectProduct(String product) {
        selectUsagePoolProduct.selectByVisibleText(product);
	}

	private void selectBillingProcessPeriod(String billingPeriod) {
        selectBillingPeriodUnit.selectByVisibleText(billingPeriod);
	}

	public void selectConfiguration(GlobalEnumsPage.PageConfigurationItems configName) {
        selectListElementByText(listConfigurationItems, configName.GetValue());
        explicitWaitForJavascript();
	}

	private void selectConfigurationPreference(String configPreferenceName) {
        selectListElementByText(listConfigurationPreferences, configPreferenceName);
	}

	private void selectConfigurationPreferenceJQGrid(String configPreferenceName) {
        textJqGridNameFilter.setText(configPreferenceName);
        textJqGridNameFilter.sendKeys(Keys.ENTER.toString());
        explicitWaitForJavascript();
        selectListElementByText(listConfigurationPreferencesJQGrid, configPreferenceName);
	}

	private void selectMetafieldType(String metaFieldType) {
        explicitWaitForJavascript();
        selectMetaFieldType.selectByVisibleText(metaFieldType);
	}

	private void selectOrderPeriodUnit(String unit) {
        selectOrderPeriodUnit.selectByVisibleText(unit);
	}

	private void selectPaymentTemplate(String paymentCard) {
        selectPaymentTemplate.selectByVisibleText(paymentCard);
	}

	public void selectPluginCategory(String testDataSetName, String category) {
		String pluginCategory = TestData.read("PageConfiguration.xml", testDataSetName, "pluginCategory", category);
        selectListElementByText(listPluginCategories, pluginCategory);
	}

	private void selectPluginType(String pluginType) {
        selectPluginType.selectByVisibleText(pluginType);
	}

	private void selectProductCategoryInAccountTypePrices(String productCategory) {
        selectAccountTypeProductCategory.selectByVisibleText(productCategory);
	}

	private void selectProductInAccountTypePricesProductsTable(String productCodeName) {
        selectListElementByText(listProductPrices, productCodeName);
        explicitWaitForJavascript();
	}

	private void setAccountTypeBillingCycle(String billingCycle) {
        textAccountTypeBillingCycle.setText(billingCycle);
	}

	private void setAccountTypeInformationTypeName(String name) {
        textInformationTypeName.setText(name);
	}

	private void setAccountTypeInvoiceDesign(String invoiceDesign) {
        textAccountTypeInvoiceDesign.setText(invoiceDesign);
	}

	private void setInvoiceName(String invoiceName) {
        inputAccountTypeName.setText(invoiceName);
	}

	private void setAccountTypeName(String accountName) {
        textAccountTypeName.setText(accountName);
	}

	private void setAccountTypePriceRate(String rate) {
        inputRate.setText(rate);
	}

	private void setBillingProcessNextRunDate(String nextRunDate) {
        inputNewRunDate.setText(nextRunDate);
	}

	private void setConfigurationalPreferenceValue(String preferenceValue) {
        textConfigPreferenceValue.setText(preferenceValue);
	}

	private void setForDaysFour(String days) {
        textStep4Days.setText(days);
	}

	private void setForDaysOne(String days) {
        textStep1Days.setText(days);
	}

	private void setForDaysThree(String days) {
        textStep3Days.setText(days);
	}

	private void setForDaysTwo(String days) {
        textStep2Days.setText(days);
	}

	private void setMetaFieldName(String name) {
        textMetaFieldName.setText(name);
	}

	private void setMethodName(String methodName) {
        textMethodName.setText(methodName);
        explicitWaitForJavascript();
	}

	private void setOrderPeriodDescription(String description) {
        textOrderPeriodDescription.setText(description);
	}

	private void setOrderPeriodValue(String value) {
        textOrderPeriodValue.setText(value);
	}

	private void setPluginOrder(String order) {
        inputProcessingOrder.setText(order);
	}

	private void setRunCollectionDate(String collectionDate) {
        textCollectionRunDate.setText(collectionDate);
        textCollectionRunDate.sendKeys(Keys.TAB.toString());
        explicitWaitForJavascript();
	}

	private void setStepFour(String step) {
        textStep4Status.setText(step);
        textStep4Status.sendKeys(Keys.TAB.toString());
        explicitWaitForJavascript();
	}

	private void setStepOne(String step) {
        textStep1Status.setText(step);
        textStep1Status.sendKeys(Keys.TAB.toString());
        explicitWaitForJavascript();
	}

	private void setStepThree(String step) {
        textStep3Status.setText(step);
        textStep3Status.sendKeys(Keys.TAB.toString());
        explicitWaitForJavascript();
	}

	private void setStepTwo(String step) {
        textStep2Status.setText(step);
        textStep2Status.sendKeys(Keys.TAB.toString());
        explicitWaitForJavascript();
	}

	public void updateAccountTypePriceForProduct(String testDataSetName, String category) {
		String productRate = TestData.read("PageConfiguration.xml", testDataSetName, "rate", category);

		this.clickEditImageAccountTypePrice();
		this.setAccountTypePriceRate(productRate);
		this.clickSaveChangesButton();
	}

	private void updateMetaFieldDetails(AccountTypeInfo accountTypeInfo, String testDataSetName, String category) {

		String metaFieldName = TestData.read("PageConfiguration.xml", testDataSetName, "metaFieldName", category);
		String metaFieldType = TestData.read("PageConfiguration.xml", testDataSetName, "metaFieldType", category);
		String metaFieldDayaType = TestData.read("PageConfiguration.xml", testDataSetName, "metaFieldDayaType", category);

		switch (accountTypeInfo) {
		case SIMPLE:
			this.setMetaFieldName(metaFieldName);
			this.selectMetafieldType(metaFieldType);
			this.clickUpdateButton();

			break;
		case DISABLE_CHECKBOX:
			this.setMetaFieldName(metaFieldName);
			this.selectInfoMetaDataType(metaFieldDayaType);
			this.checkboxDisableInMetaFields(true);
			this.clickUpdateButton();
			break;
		default:
			throw new RuntimeException("Invalid Metafield Step provided to work on. Not defined in enumeration");
		}
	}

	private void verifyConfigurationalPreferenceValue(String expectedValue) {
		String actualValue = textConfigPreferenceValue.getAttribute("value");

		if (expectedValue.equals(actualValue) == false) {
			throw new RuntimeException("Preference value " + actualValue + " is not matching with provided value " + expectedValue);
		}
	}

	public void verifySavedCommision() {
        messagesPage.isIntermediateSuccessMessageAppeared();
	}

	private void verifyUpdatedPreference() {
        messagesPage.isIntermediateSuccessMessageAppeared();
	}

	public void setConfigurationPreference(String testDataSetName, String category) {
		String notification = TestData.read("PageConfiguration.xml", testDataSetName, "notification", category);
		String preferenceValue = TestData.read("PageConfiguration.xml", testDataSetName, "preferenceValue", category);
		this.isItgInvoiceNotificationPageLoaded();
		this.isUseJQGridPageLoaded();
		this.selectConfigurationPreference(notification);
		this.setConfigurationalPreferenceValue(preferenceValue);
		this.clickSaveChangesButton();
	}

	public void configurePaymentMethod(String testDataSetName, String category) {
		String paymentTemplate = TestData.read("PageConfiguration.xml", testDataSetName, "paymentTemplate", category);

		this.clickAddNewButton();
		this.selectPaymentTemplate(paymentTemplate);
		this.clickSelectButton();
	}

	public void updatePreference(String testDataSetName, String category) {
		String preference = TestData.read("PageConfiguration.xml", testDataSetName, "preference", category);
		String preferenceVal = TestData.read("PageConfiguration.xml", testDataSetName, "preferenceVal", category);

		this.selectConfigurationPreference(preference);
		this.setConfigurationalPreferenceValue(preferenceVal);
		this.verifyConfigurationalPreferenceValue(preferenceVal);
		this.clickSaveChangesButton();
		this.verifyUpdatedPreference();
	}

	public void validatePeriodsSavedTestData(String data) {
        validateTextPresentInTable(data, tableAccountTypeNames);
	}

	public void validateInvoiceSavedTestData(String data) {
        validateTextPresentInTable(data, tableInvoiceTemplateNames);
	}

	public void validateCategoriesSavedTestData(String data) {
        validateTextPresentInTable(data, tableCategories);
	}

	public void verifyUIComponent() {
        explicitWaitForJavascript();
        Assert.assertTrue(htmlBreadcrumbs.isDisplayed());
	}

	private String getCompanyDescription() {
		String companyName = textCompanyDescription.getAttribute("Value");
		return companyName;
	}

	private void setCompanyDescription(String companyDescription) {
        textCompanyDescription.setText(companyDescription);
	}

	private void setCompanyAddress(String companyAddress) {
        textCompanyAddress.setText(companyAddress);
	}

	private void setCompanyCity(String companyCity) {
        textCompanyCity.setText(companyCity);
	}

	private void setCompanyState(String companyState) {
        textCompanyState.setText(companyState);
	}

	private void setCompanyFailedNotificationEmail(String companyFailedNotificationEmail) {
        textCompanyFailedNotificationEmail.setText(companyFailedNotificationEmail);
    }

	public void editCompanyDetails(String testDataSetName, String category) {
		String companyDescription = TestData.read("PageConfiguration.xml", testDataSetName, "companyDescription", category);
		String companyAddress = TestData.read("PageConfiguration.xml", testDataSetName, "companyAddress", category);
		String companyCity = TestData.read("PageConfiguration.xml", testDataSetName, "companyCity", category);
		String companyState = TestData.read("PageConfiguration.xml", testDataSetName, "companyState", category);
		String companyFailedNotificationEmail = TestData.read("PageConfiguration.xml", testDataSetName, "companyFailedNotificationEmail", category);
		this.setCompanyDescription(companyDescription);
		this.setCompanyAddress(companyAddress);
		this.setCompanyCity(companyCity);
		this.setCompanyState(companyState);
		this.setCompanyFailedNotificationEmail(companyFailedNotificationEmail);
		this.clickSaveChangesButton();
	}

	private void viewPluginCheckbox(java.lang.Boolean viewPlugin) {
		if (viewPlugin) {
            cbViewPlugin.select();
		} else {
            cbViewPlugin.deselect();
		}
	}

    private void apiAccessCheckbox(boolean viewPlugin) {
        linkPermissionsApi.click();
        if (viewPlugin) {
            cbApiAccess.select();
        } else {
            cbApiAccess.deselect();
        }
    }

	private void editPluginCheckbox(java.lang.Boolean editPlugin) {
		if (editPlugin) {
            cbEditPlugin.select();
		} else {
            cbEditPlugin.deselect();
		}
	}

	private void editRoleCheckbox(java.lang.Boolean editPlugin) {
		if (editPlugin) {
            cbEditRole.select();
		} else {
            cbEditRole.deselect();
		}
	}

	public void removeUserPluginPermission(String userName, String testDataSetName, String category) {
		this.clickSpecifiedUserInUsersTable(userName);
		this.clickUsersPermissionsButton();
		this.viewPluginCheckbox(false);
		this.editPluginCheckbox(false);
		this.clickSaveChangesButton();
	}

    public void restoreUserPluginPermission(String userName, String testDataSetName, String category) {
        this.clickSpecifiedUserInUsersTable(userName);
        this.clickUsersPermissionsButton();
        this.viewPluginCheckbox(true);
        this.editPluginCheckbox(true);
        this.clickSaveChangesButton();
    }

	public void verifyDeniedPluginPermissionMessage(String testDataSetName, String category) {
		String pluginMessage = TestData.read("PageConfiguration.xml", testDataSetName, "pluginMessage", category);
		this.verifyPermissionDeniedDisplayedMessageText(pluginMessage, TextComparators.contains);
	}

	private void verifyFlagField(String FlagField1) {
		String msg = linkInvoiceFlag.getText();
		if (TextUtilities.contains(msg, FlagField1)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void verifyFlagField2(String FlagField2) {
		String msg = linkFinishedFlag.getText();
		if (TextUtilities.contains(msg, FlagField2)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void verifyFlagField3(String FlagField3) {
		String msg = linkNotInvoiceFlag.getText();
		if (TextUtilities.contains(msg, FlagField3)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void verifyFlagField4(String FlagField4) {
		String msg = linkSuspendAgeingFlag.getText();
		if (TextUtilities.contains(msg, FlagField4)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void verifyINVOICEDESC(String Description1) {
		String msg = linkInvoiceDesc.getText();
		if (TextUtilities.contains(msg, Description1)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void verifyFINISHEDDESC(String Description2) {
		String msg = linkFinishedDesc.getText();
		if (TextUtilities.contains(msg, Description2)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void verifyNOTINVOICEDESC(String Description3) {
		String msg = linkNotInvoiceDesc.getText();
		if (TextUtilities.contains(msg, Description3)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void verifySUSPENDEDDESC(String Description4) {
		String msg = linkSuspendAgeingAutoDesc.getText();
		if (TextUtilities.contains(msg, Description4)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void setNewDescription(String newDescription) {
        textNewDescription.setText(newDescription);
	}

	private void selectFlag(String Flag) {
        selectOrderStatusFlagType.selectByVisibleText(Flag);
	}

	private String isOperationSuccessfulOnMessage(String messageToVerify, TextComparators comparator) {
		String msg = htmlPluginDenied.getText();
		msg = TextUtilities.nullToBlank(msg, true);
		boolean result = TextUtilities.compareValue(messageToVerify, msg, true, comparator);

		return (result ? null : msg);
	}

	private void verifyPermissionDeniedDisplayedMessageText(String messageToVerify, TextComparators comparator) {

		String rsltMsg = isOperationSuccessfulOnMessage(messageToVerify, TextComparators.contains);
		if (rsltMsg != null) {
			throw new RuntimeException("Test Case failed: " + rsltMsg);
		}
	}

	public void addUserApiAccess(String testDataSetName, String category) {
		String userName = TestData.read("PageConfiguration.xml", testDataSetName, "userName", category);
		this.clickSpecifiedUserInUsersTable(userName);
		this.clickUsersPermissionsButton();
		this.apiAccessCheckbox(true);
		this.clickSaveChangesButton();
	}

	private void selectRoleFromTable(String role) {
        selectListElementByText(listRoles, role);
	}

	private void showAgentMenuCheckbox(java.lang.Boolean cb) {
        linkMenuApi.click();
		if (cb) {
            cbShowAgentMenu.select();
		} else {
            cbShowAgentMenu.deselect();
		}
	}

	private void editAgentCheckbox(java.lang.Boolean cb) {
		if (cb) {
            cbEditAgent.select();
		} else {
            cbEditAgent.deselect();
		}
	}

	private void viewAgentDetailsCheckbox(java.lang.Boolean cb) {
		if (cb) {
            cbViewAgentDetails.select();
		} else {
            cbViewAgentDetails.deselect();
		}
	}

	public void setRolePermission(String testDataSetName, String category) {
		String role = TestData.read("PageConfiguration.xml", testDataSetName, "role", category);
		this.selectRoleFromTable(role);
		this.clickEditAccountTypeButton();
		this.showAgentMenuCheckbox(false);
		this.clickSaveChangesButton();
	}

	public String addPaymentMethod(String testDataSetName, String category) {
		String paymentCard = TestData.read("PageConfiguration.xml", testDataSetName, "paymentCard", category);
		String methodName = TestData.read("PageConfiguration.xml", testDataSetName, "methodName", category);
		boolean isRecurring = TestData.read("PageConfiguration.xml", testDataSetName, "isRecurring", category).equals("true");
		boolean allAccountType = TestData.read("PageConfiguration.xml", testDataSetName, "allAccountTypes", category).equals("true");
		this.clickAddNewButton();
		this.selectPaymentTemplate(paymentCard);
		this.clickSelectButton();
		this.setMethodName(methodName);
		this.checkIsRecurring(isRecurring);
		this.checkAllAccountTypes(allAccountType);
        explicitWaitForJavascript();
		this.clickSaveChangesButton();
		return methodName;
	}

	public String addACHPaymentMethod(String testDataSetName, String category) {
		String paymentTemplateACH = TestData.read("PageConfiguration.xml", testDataSetName, "paymentTemplateACH", category);
		String methodName = TestData.read("PageConfiguration.xml", testDataSetName, "methodName", category);
		boolean isRecurring = TestData.read("PageConfiguration.xml", testDataSetName, "isRecurring", category).equals("true");
		boolean allAccountType = TestData.read("PageConfiguration.xml", testDataSetName, "allAccountTypes", category).equals("true");
		this.clickAddNewButton();
		this.selectPaymentTemplate(paymentTemplateACH);
		this.clickSelectButton();
		this.setMethodName(methodName);
		this.checkIsRecurring(isRecurring);
		this.checkAllAccountTypes(allAccountType);
        explicitWaitForJavascript();
		this.clickSaveChangesButton();
		return methodName;
	}

	public void addEditDeletePaymentMethod(String testDataSetName, String category) {
		String newMethodName = TestData.read("PageConfiguration.xml", testDataSetName, "newMethodName", category);
		this.addPaymentMethodWithoutMetaFields(PaymentMethodField.REECURRING, testDataSetName, category);
		this.clickEditAccountTypeButton();
		this.setMethodName(newMethodName);
        explicitWaitForJavascript();
		this.clickSaveChangesButton();
		this.clickDeleteButton();
		this.clickConfirmPopupYesButton();
	}

	private void clickDeleteButton() {
        linkDelete.click();
	}

	public String createAccountTypeWithCreditDetails(String testDataSetName, String category, String paymentMethodNameOne) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);
		String billingCycle = TestData.read("PageConfiguration.xml", testDataSetName, "billingCycle", category);
		String invoiceDesign = TestData.read("PageConfiguration.xml", testDataSetName, "invoiceDesign", category);
		String creditLimit = TestData.read("PageConfiguration.xml", testDataSetName, "creditLimit", category);
		String creditLimitOne = TestData.read("PageConfiguration.xml", testDataSetName, "creditLimitOne", category);
		String creditLimitTwo = TestData.read("PageConfiguration.xml", testDataSetName, "creditLimitTwo", category);

		this.clickAddNewButton();
		this.setAccountTypeName(accountName);
		this.setAccountTypeBillingCycle(billingCycle);
		this.setAccountTypeInvoiceDesign(invoiceDesign);
		this.setCreditLimitForAccountType(creditLimit);
		this.setCreditLimitNotificationOneForAccountType(creditLimitOne);
		this.setCreditLimitNotificationTwoForAccountType(creditLimitTwo);
		this.selectPaymentMethodTypes(paymentMethodNameOne);
        explicitWaitForJavascript();
		this.clickSaveChangesButton();
		return accountName;
	}

	public String AddAccountTypeWithCreditDetailsForThreePay(String testDataSetName, String category, String paymentMethodNameOne,
			String paymentMethodNameTwo, String paymentMethodNameThree) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);
		String billingCycle = TestData.read("PageConfiguration.xml", testDataSetName, "billingCycle", category);
		String invoiceDesign = TestData.read("PageConfiguration.xml", testDataSetName, "invoiceDesign", category);
		String creditLimit = TestData.read("PageConfiguration.xml", testDataSetName, "creditLimit", category);
		String creditLimitOne = TestData.read("PageConfiguration.xml", testDataSetName, "creditLimitOne", category);
		String creditLimitTwo = TestData.read("PageConfiguration.xml", testDataSetName, "creditLimitTwo", category);

		this.clickAddNewButton();
		this.setAccountTypeName(accountName);
		this.setAccountTypeBillingCycle(billingCycle);
		this.setAccountTypeInvoiceDesign(invoiceDesign);
		this.setCreditLimitForAccountType(creditLimit);
		this.setCreditLimitNotificationOneForAccountType(creditLimitOne);
		this.setCreditLimitNotificationTwoForAccountType(creditLimitTwo);
		this.selectPaymentMethodTypes(paymentMethodNameOne);
		this.selectPaymentMethodTypes(paymentMethodNameTwo);
		this.selectPaymentMethodTypes(paymentMethodNameThree);
		this.clickSaveChangesButton();
		return accountName;
	}

	public void setConfigurationPreferenceForAnyPreference(String testDataSetName, String category) {
		String userName = TestData.read("PageConfiguration.xml", testDataSetName, "userName", category);
		String preferenceValue = TestData.read("PageConfiguration.xml", testDataSetName, "preferenceValue", category);
		this.clickSpecifiedUserInUsersTable(userName);
		this.setConfigurationalPreferenceValue(preferenceValue);
		this.clickSaveChangesButton();
		this.verifyConfigurationalPreferenceValue(preferenceValue);
	}

	public void verifyMandatoryFieldMessages(String testDataSetName, String category) {
		String message = TestData.read("PageConfiguration.xml", testDataSetName, "message", category);
		this.clickAddNewButton();
		this.clickSaveChangesButton();
		this.verifyMandatoryFieldMessage(message);
	}

	private void setCreditLimitForAccountType(String creditLimit) {
        textCreditLimit.setText(creditLimit);
	}

	private void setCreditLimitNotificationOneForAccountType(String creditLimitOne) {
        textCreditLimitNotOne.setText(creditLimitOne);
	}

	private void setCreditLimitNotificationTwoForAccountType(String creditLimitTwo) {
        textCreditLimitNotTwo.setText(creditLimitTwo);
	}

	private void verifyMandatoryFieldMessage(String message) {
		String msg = textValidationMessage.getText();
		if (TextUtilities.contains(msg, message)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	public String addPaymentMethodWithoutMetaFields(PaymentMethodField paymentMethodFields, String testDataSetName, String category) {
		String paymentTemplate = TestData.read("PageConfiguration.xml", testDataSetName, "paymentTemplate", category);
		String methodName = TestData.read("PageConfiguration.xml", testDataSetName, "methodName", category);
		boolean allAccountTypes = TestData.read("PageConfiguration.xml", testDataSetName, "allAccountTypes", category).equals("true");
		boolean isRecurring = TestData.read("PageConfiguration.xml", testDataSetName, "isRecurring", category).equals("true");

		switch (paymentMethodFields) {
		case ALL:
			this.clickAddNewButton();
			this.selectPaymentTemplate(paymentTemplate);
			this.clickSelectButton();
			this.setMethodName(methodName);
			this.checkAllAccountTypes(allAccountTypes);
			this.checkIsRecurring(isRecurring);
			break;

		case REECURRING:
			this.clickAddNewButton();
			this.selectPaymentTemplate(paymentTemplate);
			this.clickSelectButton();
			this.setMethodName(methodName);
			this.checkIsRecurring(isRecurring);
			break;

		case ALL_ACCOUNTS:
			this.clickAddNewButton();
			this.selectPaymentTemplate(paymentTemplate);
			this.clickSelectButton();
			this.setMethodName(methodName);
			this.checkAllAccountTypes(allAccountTypes);
			break;
		default:
			throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");

		}
        explicitWaitForJavascript();
		this.clickSaveChangesButton();
		return methodName;
	}

	private void selectPaymentMethodTypes(String paymentMethod) {
        selectPaymentMethodType.selectByVisibleText(paymentMethod);
	}

	private void deSelectPaymentMethodTypes(String paymentMethod) {
        selectPaymentMethodType.deselectByVisibleText(paymentMethod);
	}

	public String verifyPayMethodDefaultSelectedForAddingAccountType(String testDataSetName, String category, String paymentMethodName) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);

		this.clickAddNewButton();
		this.setAccountTypeName(accountName);
		this.VerifyPaymentMethodItemIsSelectedInDropdown(paymentMethodName);
        explicitWaitForJavascript();
		this.clickSaveChangesButton();
		return accountName;
	}

	public void editPaymentMethodWithAllAccountTypeChecked(String testDataSetName, String category, String paymentMethodName) {
		boolean allAccountTypes = TestData.read("PageConfiguration.xml", testDataSetName, "allAccountTypes", category).equals("true");

		this.selectAccountTypeName(paymentMethodName);
		this.clickEditAccountTypeButton();
		this.checkAllAccountTypes(allAccountTypes);
        explicitWaitForJavascript();
		this.clickSaveChangesButton();
	}

	public void clickOnPaymentMethodLink() {
        linkPaymentMethod.click();
	}

	public void clickOnAccountTypeLink() {
        linkAccountType.click();
	}

	public void clickMetaDataFieldValue(String metaFieldName) {
        selectListElementByText(listAssetField, metaFieldName);
	}
	
	private void clickOnCloneButton() {
        linkClone.click();
	}

	private void enterNewMetaDataName(String Name) {
        textMdMetaFieldName.setText(Name);
	}

	private void enterNewMetaDataGroupName(String Name) {
        textGroupMetaDataName.setText(Name);
	}

	private void enterNewMetaDataDefaultValue(String Value) {
        textMdMetaFieldDefault.setText(Value);
	}

	private void selectDataTypeForNewMataData(String dataTypeValue) {
        selectMdMetaFieldDataType.selectByVisibleText(dataTypeValue);
	}

	private void changeUserPermissionForMandatory(boolean editManadatory) {
		if (editManadatory) {
            cbNewMandatory.select();
		} else {
            cbNewMandatory.deselect();
		}
	}

	public String setNewMetaData(AddMetaDataFields addMetaField, String testDataSetName, String category) {
		String Name = TestData.read("PageConfiguration.xml", testDataSetName, "Name", category);
		String Value = TestData.read("PageConfiguration.xml", testDataSetName, "Value", category);
		String dataTypeValue = TestData.read("PageConfiguration.xml", testDataSetName, "dataTypeValue", category);
		boolean permission = TestData.read("PageConfiguration.xml", testDataSetName, "permission", category).equals("true");

		switch (addMetaField) {
		case DATA_FIELD:
			this.enterNewMetaDataName(Name);
			this.changeUserPermissionForMandatory(permission);
			this.enterNewMetaDataDefaultValue(Value);
			break;
		case DATA_TYPE:
			this.enterNewMetaDataName(Name);
			this.enterNewMetaDataDefaultValue(Value);
			this.selectDataTypeForNewMataData(dataTypeValue);
			break;
		case DATA_DEFAULT_VALUE:
			this.enterNewMetaDataName(Name);
			this.enterNewMetaDataDefaultValue(Value);
			break;
		default:
			throw new RuntimeException("Bad enum value: "+ addMetaField);
		}
        explicitWaitForJavascript();
        clickSaveChangesButton();
		return Name;
	}

	public void validateMetaSavedTestData(String data) {
        validateTextPresentInTable(data, tableVerifyCreatedMetaData);
	}

	private void selectMetaDataGroupValue(String metaGroupValue) {
        selectMetaDataGroup.selectByVisibleText(metaGroupValue);
	}

	private void clickArrowButton() {
        linkArrowToAdd.click();
	}

	public void setNewMetaDataGroup(AddMetaDataGroupFields addMetaField, String testDataSetName, String category,
			String valueOne, String valueTwo) {

		String GroupName1 = TestData.read("PageConfiguration.xml", testDataSetName, "Name1", category);
		String GroupName2 = TestData.read("PageConfiguration.xml", testDataSetName, "Name2", category);
		switch (addMetaField) {
		case GROUP_DATA_FIELD:
			this.enterNewMetaDataGroupName(GroupName1);
			this.selectMetaDataGroupValue(valueOne);
			this.clickArrowButton();
			this.selectMetaDataGroupValue(valueTwo);
			this.clickArrowButton();
			this.clickSaveChangesButton();
			this.clickEditAccountTypeButton();
			this.enterNewMetaDataGroupName(GroupName2);
			break;

		default:
			throw new RuntimeException("Bad enum value: "+ addMetaField);
		}
	}

	public String editAccountTypeForGivenAccountWithThreePay(String testDataSetName, String category, String accName,
			String paymentMethodOne, String paymentMethodTwo, String paymentMethodThree) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);

		this.selectAccountTypeName(accName);
		this.clickEditAccountTypeButton();
		this.setAccountTypeName(accountName);
		this.selectPaymentMethodTypes(paymentMethodOne);
		this.selectPaymentMethodTypes(paymentMethodTwo);
		this.selectPaymentMethodTypes(paymentMethodThree);
		this.clickSaveChangesButton();
		return accountName;
	}

	public void editAccountTypeForGivenAccountDeselectPay(String accName, String paymentMethod) {

		this.selectAccountTypeName(accName);
		this.clickEditAccountTypeButton();
		this.deSelectPaymentMethodTypes(paymentMethod);
		this.clickSaveChangesButton();
	}

	private void VerifyPaymentMethodItemIsSelectedInDropdown(String targetValue) {
        selectPaymentMethodType.isDropDownOptionSelected(targetValue);
	}

	public void verifyPayMethodDefaultSelectedForAccountType(String paymentMethodName) {
		this.clickAddNewButton();
		this.VerifyPaymentMethodItemIsSelectedInDropdown(paymentMethodName);
	}

	private void checkboxDisableInMetaFields(java.lang.Boolean cb) {
		if (cb) {
            cbDisabled.select();
		} else {
            cbDisabled.deselect();
		}
	}

	private void selectInfoMetaDataType(String targetValue) {
        selectInfoMetaDataType.selectByVisibleText(targetValue);
	}

	public void createMetafield() {

		String displayedText = textMetaFieldHeader.getText();
		String expectedText = "Meta Field Categories";
		if (! displayedText.contains(expectedText)) {
			throw new RuntimeException();
		}
        linkCustomerMetaField.click();
		boolean flag = false;
		flag = linkSalarySearch.exists();
		if (flag == true) {
            linkSalarySearch.click();
			this.clickDeleteButton();
            buttonYesButtonForDeleteButton.click();
		}
		this.clickAddNewButton();
		this.clickSaveChangesButton();
        messagesPage.isErrorMessageAppeared();
		this.addMetaData();
        messagesPage.isIntermediateSuccessMessageAppeared();
	}

	public void VerifyOrderStatus(String testDataSetName, String category) {
		String FlagField1 = TestData.read("PageConfiguration.xml", testDataSetName, "FlagField1", category);
		String FlagField2 = TestData.read("PageConfiguration.xml", testDataSetName, "FlagField2", category);
		String FlagField3 = TestData.read("PageConfiguration.xml", testDataSetName, "FlagField3", category);
		String FlagField4 = TestData.read("PageConfiguration.xml", testDataSetName, "FlagField4", category);
		String Description1 = TestData.read("PageConfiguration.xml", testDataSetName, "Description1", category);
		String Description2 = TestData.read("PageConfiguration.xml", testDataSetName, "Description2", category);
		String Description3 = TestData.read("PageConfiguration.xml", testDataSetName, "Description3", category);
		String Description4 = TestData.read("PageConfiguration.xml", testDataSetName, "Description4", category);
		String newDescription = TestData.read("PageConfiguration.xml", testDataSetName, "newDescription", category);
		String newDescription1 = TestData.read("PageConfiguration.xml", testDataSetName, "newDescription1", category);
		String Flag = TestData.read("PageConfiguration.xml", testDataSetName, "Flag", category);

		this.clickOrderStatuses();
		this.verifyFlagField(FlagField1);
		this.verifyFlagField2(FlagField2);
		this.verifyFlagField3(FlagField3);
		this.verifyFlagField4(FlagField4);
		this.verifyINVOICEDESC(Description1);
		this.verifyFINISHEDDESC(Description2);
		this.verifyNOTINVOICEDESC(Description3);
		this.verifySUSPENDEDDESC(Description4);
		this.clickAddNewButton();
		this.setNewDescription(newDescription);
		this.clickSaveChangesButton();
		this.clickAddNewButton();
		this.selectFlag(Flag);
		this.setNewDescription(newDescription1);
		this.clickSaveChangesButton();
	}

	private void addMetaData() {
		String name = TestData.read("PageConfiguration.xml", "MetadataName", "Metadata", "MDN");
		String data_type = TestData.read("PageConfiguration.xml", "MetadataName", "MetDataType", "MDN");
        textMfName.setText(name);
        selectMfDataType.selectByVisibleText(data_type);
        cbMfMandatory.select();
		this.clickSaveChangesButton();
	}

	public String addInvoiceTemplate(String testDataSetName, String category) {
		String templateName = TestData.read("PageConfiguration.xml", testDataSetName, "Name", category);
		this.clickAddNewButton();
		this.setInvoiceName(templateName);
		this.clickSaveChangesTemplatebutton();

		return templateName;
	}

	public String enterDataStatus(String testDataSetName, String category) {

		String enterProcessingOrderStatus = TestData.read("PageConfiguration.xml", testDataSetName, "enterProcessingOrderStatus", category);
		String enterFinishedOrderStatus = TestData.read("PageConfiguration.xml", testDataSetName, "enterFinishedOrderStatus", category);

		String enterSuspendedOrderStatus = TestData.read("PageConfiguration.xml", testDataSetName, "enterSuspendedOrderStatus", category);

		this.setOrderStatusProcessing(enterProcessingOrderStatus);
		this.clickPluginAddMoreParametersIcon();

		this.setFinishedOrderStatus(enterFinishedOrderStatus);
		this.clickPluginAddMoreParametersIcon();

		this.setSuspendedOrderStatus(enterSuspendedOrderStatus);
		this.clickPluginAddMoreParametersIcon();
		this.unCheckAppliedbox();
		this.CheckAppliedbox();

		return enterSuspendedOrderStatus;
	}

	public String createAccount(String paymentMethod, String testDataSetName, String category) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);
		this.clickAddNewButton();
		this.setAccountTypeName(accountName);
		this.selectPaymentMethodTypes(paymentMethod);
		this.clickSaveChangesButton();
		return accountName;

	}

	private void setOrderStatusProcessing(String enterProcessingOrderStatus) {
        textProcessingOrderStatus.setText(enterProcessingOrderStatus);
	}

	private void setFinishedOrderStatus(String enterFinishedOrderStatus) {
        textFinishedOrderStatus.setText(enterFinishedOrderStatus);
	}

	private void setSuspendedOrderStatus(String enterSuspendedOrderStatus) {
        textSuspendedOrderStatus.setText(enterSuspendedOrderStatus);
	}

	private String unCheckAppliedbox() {
        cbApplied.deselect();
		this.clickSaveChangesButton();
		return htmlStrongErrorMessage.getText();
	}

	private void CheckAppliedbox() {
        cbApplied.select();
		this.clickSaveChangesButton();
	}

	public String addNewInformationToSelectedAccountType(String testDataSetName, String category) {
        linkAddNewInformation.click();

		String infoTypeName = TestData.read("PageConfiguration.xml", testDataSetName, "name", category);
		String metaFieldName = TestData.read("PageConfiguration.xml", testDataSetName, "name", category);
		String metaFieldType = TestData.read("PageConfiguration.xml", testDataSetName, "metaFieldType", category);

        this.setAccountTypeInformationTypeName(infoTypeName);
		this.clickAddNewMetaField();
		this.addMetaField(metaFieldName, metaFieldType);
		this.clickSaveChangesToInformationType();

		return infoTypeName;
	}

	private void addMetaField(String name, String metaFieldType) {
        activateMetaFieldEditor();
		this.updateMetaFieldDetails(name, metaFieldType);
	}

	private void updateMetaFieldDetails(String name, String metaFieldType) {
		this.setMetaFieldName(name);
		this.selectMetafieldType(metaFieldType);
		this.clickUpdateButton();
	}

	public void selectAccountName(String accountTypeName) {
        selectListElementByText(listAccountTypeNames, accountTypeName);
	}

	public void configurePluginPermissions(String userName, String testDataSetName, String category) {
		boolean plugin = TextUtilities.compareValue(TestData.read("PageConfiguration.xml", testDataSetName, "plugin", category), "true",
				true, TextComparators.equals);
		this.clickSpecifiedUserInUsersTable(userName);
		this.clickUsersPermissionsButton();
		this.changeUserPermissionForEditPlugin(plugin);
		this.clickSaveChangesToUsersPermissions();
	}

	public void setConfigurationPreferenceJQGrid(String testDataSetName, String category) {
		String notification = TestData.read("PageConfiguration.xml", testDataSetName, "notification", category);
		String preferenceValue = TestData.read("PageConfiguration.xml", testDataSetName, "preferenceValue", category);

		this.isJQGridPageLoaded();
		this.selectConfigurationPreferenceJQGrid(notification);
		this.setConfigurationalPreferenceValue(preferenceValue);
		this.clickSaveChangesButton();
	}

	private void verifyMandatoryFieldMessageForNewEnumeration(NewEnumerationMsg newEnumerationMsg, String testDataSetName,
			String category) {
		String NameMessage = TestData.read("PageConfiguration.xml", testDataSetName, "NameMessage", category);
		String ValueMessage = TestData.read("PageConfiguration.xml", testDataSetName, "ValueMessage", category);
        System.out.println("NameMessage="+NameMessage+"=");
        System.out.println("ValueMessage="+ValueMessage+"=");
		switch (newEnumerationMsg) {
		case ALL:
			String NameMsg = htmlEnumerationMessageName.getText();
			String ValueMsg = textEnumerationMsgValue.getText();
            System.out.println("NameMsg="+NameMsg+"=");
            System.out.println("ValueMsg="+ValueMsg+"=");
			if (TextUtilities.contains(NameMsg, NameMessage) && TextUtilities.contains(ValueMsg, ValueMessage)) {
				Assert.assertTrue(true);
			} else {
				Assert.assertTrue(false);
			}
			break;

		case NAME:
			String onlyNameMsg = htmlEnumerationMessage.getText();
			if (TextUtilities.contains(onlyNameMsg, NameMessage)) {
				Assert.assertTrue(true);
			} else {
				Assert.assertTrue(false);
			}
			break;

		case VALUE:
			String onlyValueMsg = htmlEnumerationMessage.getText();
			if (TextUtilities.contains(onlyValueMsg, ValueMessage)) {
				Assert.assertTrue(true);
			} else {
				Assert.assertTrue(false);
			}
			break;

		default:
			throw new RuntimeException("Invalid Message Enum provided to work on. Not defined in enumeration");
		}
	}

	private void setEnumerationName(String enumerationName) {
        textEnumerationName.setText(enumerationName);
	}

	private void clickOnCustomerInMetaField() {
        linkCustomerMetaField.click();
	}

	private void setEnglishName(String englishNameValue) {
        selectFupDescLang.selectByVisibleText("English");
        linkAddFupDesc.click();
        textFupDesc.setText(englishNameValue);
//		GlobalController.brw.setText(this.TB_ENGLISH_NAME, englishNameValue);
	}

	private void setQuantity(String quantity) {
        textFupQuantity.setText(quantity);
	}

	private void setCyclePeriod(String cyclePeriod) {
        textCyclePeriod.setText(cyclePeriod);
	}

	private void setConsumptionFirstbox(String consumption) {
        textFupConsumptionPercentage.setText(consumption);
	}

	private void setConsumptionSecondbox(String consumption) {
        textFupConsumptionPercentage2.setText(consumption);
	}

	private void setProductID(String Id) {
        textFupProductId.setText(Id);
	}

	private void setEnumerationValue(SetEnumValues setEnumValues, String enumerationValue) {

		switch (setEnumValues) {
		case ZERO:
            textZeroEnumValue.setText(enumerationValue);
			break;
		case ONE:
            textFirstEnumValue.setText(enumerationValue);
			break;
		default:
			throw new RuntimeException("Invalid Message Enum provided to work on. Not defined in enumeration");
		}
	}

	public void validateEnumerationsSavedData(String enumerationName) {
        validateTextPresentInTable(enumerationName, tableEnumerations);
	}

	public void selectEnumerationsFromTable(String enumerationName) {
        selectListElementByText(listEnumFirstCell, enumerationName);
	}

	public void clickEditButton() {
        explicitWaitForJavascript();
        try {Thread.sleep(1000);} catch (Exception e) {}
        linkEdit.click();
        explicitWaitForJavascript();
	}

	private void clickImageAddMoreEnumerationValueField() {
        linkAddEnumerationValue.click();
        explicitWaitForJavascript();
	}

	private void clickCancelButton() {
        linkCancel.click();
	}

	private void verifyErrorMsgForDuplicateEnumValue(String errorMessage) {
		String NameMsg = htmlErrorMessageEnumeration.getText();
		if (TextUtilities.contains(NameMsg, errorMessage)) {
			Assert.assertTrue(true);
		} else {
			Assert.assertTrue(false);
		}
	}

	private void clickImageRemoveEnumerationFirstValueField() {
        linkRemoveEnumerationValue.click();
	}

	public String createEnumeration(AddNewEnumeration addNewEnumeration, String testDataSetName, String category) {
		String enumerationName = TestData.read("PageConfiguration.xml", testDataSetName, "enumerationName", category);
		String enumerationValue = TestData.read("PageConfiguration.xml", testDataSetName, "enumerationValue", category);

		switch (addNewEnumeration) {
		case VERIFY_MANDATORY_FIELDS:
			this.clickAddNewButton();
			this.clickSaveChangesButton();
			this.verifyMandatoryFieldMessageForNewEnumeration(NewEnumerationMsg.ALL, testDataSetName, category);
			this.setEnumerationName(enumerationName);
			this.clickSaveChangesButton();
			this.verifyMandatoryFieldMessageForNewEnumeration(NewEnumerationMsg.VALUE, testDataSetName, category);
			this.setEnumerationValue(SetEnumValues.ZERO, enumerationValue);
			this.clickSaveChangesButton();
			break;

		default:
			throw new RuntimeException("Invalid Message Enum provided to work on. Not defined in enumeration");
		}
		return enumerationName;
	}

	public String editConfiguration(String existEnumName, String testDataSetName, String category) {
		String enumerationValue = TestData.read("PageConfiguration.xml", testDataSetName, "editEnumerationValue", category);
		this.selectEnumerationsFromTable(existEnumName);
		this.clickEditButton();
		this.clickImageAddMoreEnumerationValueField();
		this.clickImageRemoveEnumerationFirstValueField();
		this.setEnumerationValue(SetEnumValues.ZERO, enumerationValue);
		this.clickSaveChangesButton();
		return enumerationValue;
	}

	public void verifyDuplicateValueForEnumeration(String existEnumName, String testDataSetName, String category) {
		String enumerationValue = TestData.read("PageConfiguration.xml", testDataSetName, "editEnumerationValue", category);
		String errorMessage = TestData.read("PageConfiguration.xml", testDataSetName, "errorMessage", category);

		this.selectEnumerationsFromTable(existEnumName);
		this.clickEditButton();
		this.clickImageAddMoreEnumerationValueField();
		this.setEnumerationValue(SetEnumValues.ONE, enumerationValue);
		this.clickSaveChangesButton();
		this.verifyErrorMsgForDuplicateEnumValue(errorMessage);
		this.clickCancelButton();
	}

	private void clickConfirmPopupNOButton() {
        btnConfirmNo.click();
	}

	public String AddFreeUsagePool(String testDataSetName, String category, String ID, String productCategory, String products) {

		String englishName = TestData.read("PageConfiguration.xml", testDataSetName, "englishName", category);
		String quantity = TestData.read("PageConfiguration.xml", testDataSetName, "quantity", category);
		String cyclePeriod = TestData.read("PageConfiguration.xml", testDataSetName, "cyclePeriod", category);
		String consumption1 = TestData.read("PageConfiguration.xml", testDataSetName, "consumption1", category);
		String consumption2 = TestData.read("PageConfiguration.xml", testDataSetName, "consumption2", category);
		String usagePoolConsumptionNotification = TestData.read("PageConfiguration.xml", testDataSetName,
				"usagePoolConsumptionNotification", category);
		String usagePoolConsumptionFee = TestData.read("PageConfiguration.xml", testDataSetName, "usagePoolConsumptionFee", category);
		String usagePoolConsumptionNotification_Invoice = TestData.read("PageConfiguration.xml", testDataSetName,
				"usagePoolConsumptionNotification_Invoice", category);
		String usagePoolConsumptionNotification_Invoice_mail = TestData.read("PageConfiguration.xml", testDataSetName,
				"usagePoolConsumptionNotification_Invoice_mail", category);

		this.clickAddNewButton();
//		this.clickPluginAddMoreNameParametersIcon();
		this.setEnglishName(englishName);
		this.setQuantity(quantity);
		this.setCyclePeriod(cyclePeriod);
		this.selectProductCategory(productCategory);
		this.selectProduct(products);

		this.selectUsagePoolConsumptionNotification(usagePoolConsumptionNotification);
		this.selectusagePoolConsumptionNotification_Invoice(usagePoolConsumptionNotification_Invoice);
		this.selectusagePoolConsumptionNotification_Invoice_mail(usagePoolConsumptionNotification_Invoice_mail);

		this.setConsumptionFirstbox(consumption1);
		this.clickFupAction();
		this.selectUsagePoolConsumptionFee(usagePoolConsumptionFee);
		this.setConsumptionSecondbox(consumption2);
		this.setProductID(ID);
		this.clickFupAction();
		this.clickSaveChangesButton();

		return englishName;
	}

	public void checkDeleteYesNo(String delete) {
		switch (delete) {
		case "YES":
			this.clickDeleteButton();
			this.clickConfirmPopupYesButton();
			break;
		case "NO":
			this.clickDeleteButton();
			this.clickConfirmPopupNOButton();
			break;
		}
	}

	private void selectUsagePoolName(String usagePoolName) {
        selectListElementByText(listUsagePoolNames, usagePoolName);
	}

	private void selectUsagePoolConsumptionNotification(String usagePoolConsumptionNotification) {
        selectUsagePoolConsumptionNotification.selectByVisibleText(usagePoolConsumptionNotification);
	}

	private void selectusagePoolConsumptionNotification_Invoice(String usagePoolConsumptionNotification_Invoice) {
        selectUsagePoolConsumptionNotification_Invoice.selectByVisibleText(usagePoolConsumptionNotification_Invoice);
	}

	private void selectusagePoolConsumptionNotification_Invoice_mail(String usagePoolConsumptionNotification_Invoice_mail) {
        selectUsagePoolConsumptionNotification_Invoice_mail.selectByVisibleText(usagePoolConsumptionNotification_Invoice_mail);
	}

	private void selectUsagePoolConsumptionFee(String usagePoolConsumptionFee) {
        selectUsagePoolConsumptionFee.selectByVisibleText(usagePoolConsumptionFee);
	}

	public void selectUsagePool(String usagePoolName) {

		this.selectUsagePoolName(usagePoolName);
		this.checkDeleteYesNo("NO");
		this.checkDeleteYesNo("YES");
	}

	public void valdationMessageDisplay(String message) {
		String msg = htmlStrongErrorMessage1.getText();
		if (TextUtilities.contains(msg, message)) {
			Assert.assertTrue(true);
		} else {
			throw new RuntimeException("Test Case failed: ");
		}
	}

	private void clickOnAddInformationType() {
        linkAddNewInformation.click();
	}

	private void setAccountName(String OrderTestData) {
        textMetaFieldName2.setText(OrderTestData);
	}

	public void setAdminEmail(String email) {
	    if (textAdminEmail.exists()) {
            textAdminEmail.setText(email);
	    }
	}

	public void validateAccountInformationTypeSavedTestData(String data) {
        validateTextPresentInTable(data, tableAccountTypeNames);
	}

	public void validateAccountTypeSavedTestData(String data) {
        validateTextPresentInTable(data, tableAccountTypeNames);
	}

	private void clickOnMetaFieldLink() {
        linkMetaFields.click();
	}

	private void validateValidationRules() {
        selectMdMetaFieldValidationRuleType.isDropDownOptionSelected("--");
	}

	public String addAccountType(String testDataSetName, String category) {
		String accountTypeName = TestData.read("PageConfiguration.xml", testDataSetName, "AccountTypeName", category);
		this.clickOnAccountTypeLink();
		this.clickAddNewButton();
		this.setAccountTypeName(accountTypeName);
		this.clickSaveChangesButton();
		this.verifySavedAccountType();
		return accountTypeName;
	}

	public String addACHAccountType(String testDataSetName, String category, String TC_115_PAYMENT_METHOD_ACH) {
		String accountTypeName = TestData.read("PageConfiguration.xml", testDataSetName, "accountTypeName", category);
		this.clickAddNewButton();
		this.setAccountTypeName(accountTypeName);
		this.selectPaymentMethodTypes(TC_115_PAYMENT_METHOD_ACH);
		this.clickSaveChangesButton();
		this.verifySavedAccountType();
		return accountTypeName;
	}

	private void activateMetaFieldEditor () {
        String cssClass = htmlMetaFieldsTabActivator.getAttribute("class");
        if (! cssClass.contains("active")) {
            linkSpanNewMetafieldTab.click();
        }
        explicitWaitForJavascript();
    }

	public String addAITMetaFieldToAccountType(String testDataSetName, String category, String accountType) {

		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "AccountName", category);
		String nameEmail = TestData.read("PageConfiguration.xml", testDataSetName, "NameEmail", category);
		String metaFieldSelection = TestData.read("PageConfiguration.xml", testDataSetName, "MetafieldSelection", category);
		this.selectAccountTypeName(accountType);
		this.clickOnAddInformationType();
        this.setAccountTypeInformationTypeName(accountName);
		this.clickAddNewMetaField();
		this.selectMetaFieldType(metaFieldSelection);
		this.setAccountName(nameEmail);
		this.clickUpdateButton();
		this.clickSaveChangesButton();
		this.verifySavedAccountType();
		return accountName;
	}

	public String addrecurringPaymentMethodDetails(String testDataSetName, String category) {
		String methodName = TestData.read("PageConfiguration.xml", testDataSetName, "methodName", category);
		String cardErrorMsg = TestData.read("PageConfiguration.xml", testDataSetName, "cardErrorMsg", category);
		String dateErrorMsg = TestData.read("PageConfiguration.xml", testDataSetName, "dateErrorMsg", category);

		this.setMethodName(methodName);
        cbIsRecurring.select();
		this.createValRulOnCCNumberMF(cardErrorMsg);
		this.createValRulOnExpiryDateMF(dateErrorMsg);
		this.clickSaveChangesButton();

		return methodName;
	}

	public String accounttype(String testDataSetName, String category, String method_types) {

		String accountTypeName = TestData.read("PageConfiguration.xml", testDataSetName, "accountTypeName", category);
		this.clickAddNewButton();
		this.setAccountTypeName(accountTypeName);
		this.selectPaymentMethodTypes(method_types);
        linkMdMetaFieldSaveChanges.click();
		return accountTypeName;
	}

	public void SelectPaymentMethodTemplate(String testDataSetName, String category) {
		String paymentTemplate = TestData.read("PageConfiguration.xml", testDataSetName, "paymentTemplate", category);

		this.clickAddNewButton();
		this.selectPaymentTemplate(paymentTemplate);
		this.clickSelectButton();
	}

	private void validateValidationRuleDropDown() {
		if (selectValidationRule.exists()) {
			Assert.assertTrue(true);
		} else {
			Assert.assertTrue(false);
		}
	}

	private void verifyValueFromDropDown(String metfieldAccountName) {
        Assert.assertTrue(selectIncludeNotification.hasOption(metfieldAccountName));
	}

	private void setAccountNameOnSecondForm(String OrderTestData) {
        textMetaFieldNameIdx2.setText(OrderTestData);
	}

	private void selectMetaFieldType(String metaFieldType) {
        selectMetaFieldType2.selectByVisibleText(metaFieldType);
	}

	private void selectMetaFieldTypeOnSecondForm(String metaFieldType) {
        selectMetaFieldTypeIdx2.selectByVisibleText(metaFieldType);
	}

	private void verifySavedAccountType() {
        messagesPage.isIntermediateSuccessMessageAppeared();
	}

	private void clickUpdateButtonOnSecondForm() {
        linkUpdateMetaFieldIdx2.click();
	}

	private void verifySuccessfulErrorMessage() {
        messagesPage.isErrorMessageAppeared();
	}

	public void verifyVelidityRulesDropDown(String testDataSetName, String category, String accountType) {
		String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "AccountName", category);
		this.clickOnAccountTypeLink();
		this.selectAccountTypeName(accountType);
		this.clickOnAddInformationType();
        this.setAccountTypeInformationTypeName(accountName);
		this.clickAddNewMetaField();
		this.validateValidationRuleDropDown();
	}

	public void verifyAddedMetafieldIsDisplayingIncludeNotificationDropDown(String accountType, String metafieldAccountName) {
		this.clickOnAccountTypeLink();
		this.selectAccountTypeName(accountType);
		this.clickEditButton();
		this.verifyValueFromDropDown(metafieldAccountName);
	}

	public void verifyUsedMetafieldTypeIsDisplayedInDD(String testDataSetName, String category, String accountType) {
		String nameEmail = TestData.read("PageConfiguration.xml", testDataSetName, "NameEmail", category);
		String metaFieldSelection = TestData.read("PageConfiguration.xml", testDataSetName, "MetafieldSelection", category);
		this.clickEditButton();
		this.clickAddNewMetaField();
		this.selectMetaFieldTypeOnSecondForm(metaFieldSelection);
		this.setAccountNameOnSecondForm(nameEmail);
		this.selectMetaFieldTypeOnSecondForm(metaFieldSelection);
		this.clickUpdateButtonOnSecondForm();
		this.clickSaveChangesButton();
		this.verifySuccessfulErrorMessage();
	}
	
	public void validateMetaFieldSelectionForCustomer() {
		this.clickOnMetaFieldLink();
		this.clickOnCustomerInMetaField();
		this.clickAddNewButton();
		this.validateValidationRules();
	}

	public void validateMetaFieldSelectionForOrder() {
		this.clickOnMetaFieldLink();
		this.clickOnPaymentInMetaField();
		this.clickAddNewButton();
		this.validateValidationRules();
	}

	public void validateMetaFieldSelectionForPayment() {
		this.clickOnMetaFieldLink();
		this.clickOnOrderInMetaField();
		this.clickAddNewButton();
		this.validateValidationRules();
	}
	
	private void clickOnPaymentInMetaField() {
        linkSelectPaymentInMetaField.click();
	}
	
	private void clickOnOrderInMetaField() {
        linkSelectOrderInMetaField.click();
	}
	
	public void verifyUserCannotSelectBothPaymentMethod(String accountType, String paymentAddress1, String paymentAddress2) {
		this.clickOnAccountTypeLink();
		this.selectAccountTypeName(accountType);
		this.clickEditButton();
		this.selectPaymentAddress(paymentAddress1);
		this.clickConfirmPopupYesButton();
		this.selectPaymentAddress(paymentAddress2);
		this.clickConfirmPopupYesButton();
		this.verifySelectedPaymentAddressOption(paymentAddress2);
		this.clickSaveChangesButton();
	}

	private void selectPaymentAddress(String paymentAddress) {
        selectIncludeNotification.selectByVisibleText(paymentAddress);
	}
	
	private void verifySelectedPaymentAddressOption(String paymentAddress) {
        selectIncludeNotification.isDropDownOptionSelected(paymentAddress);
	}
	
	public void createCloneAccountType(String testDataSetName, String category, String accountTypeName) {
		String cloneAccountName = TestData.read("PageConfiguration.xml", testDataSetName, "cloneAccountName", category);
		this.clickOnAccountTypeLink();
		this.selectAccountTypeName(accountTypeName);
		this.clickOnCloneButton();
		this.clickSaveChangesButton();
		this.verifySuccessfulErrorMessage();
		this.setAccountTypeName(cloneAccountName);
		this.clickSaveChangesButton();
        this.verifySavedAccountType();
	}
	
	public String addAccountTypeWithInvoiceDesign(String testDataSetName, String category) {
		String accountTypeName = TestData.read("PageConfiguration.xml", testDataSetName, "AccountTypeName", category);
		String InvoiceDesign = TestData.read("PageConfiguration.xml", testDataSetName, "InvoiceDesign", category);

		this.clickOnAccountTypeLink();
		this.clickAddNewButton();
		this.setAccountTypeName(accountTypeName);
		this.setAccountTypeInvoiceDesign(InvoiceDesign);
		this.clickSaveChangesButton();
		this.verifySavedAccountType();
		return accountTypeName;
	}
	
	public void reUpdatePreference(String testDataSetName, String category) {
		String preference = TestData.read("PageConfiguration.xml", testDataSetName, "preference", category);
		String preferenceVal = TestData.read("PageConfiguration.xml", testDataSetName, "preferenceValue", category);
        textJqGridNameFilter.setText(preference);
        textJqGridNameFilter.sendKeys(Keys.ENTER.toString());
        explicitWaitForJavascript();

        htmlReconfigure.click();
		this.setConfigurationalPreferenceValue(preferenceVal);
		this.clickSaveChangesButton();
		this.verifyUpdatedPreference();
	}
	
	public void adddNewOrderStatus(String testDataSetName, String category, boolean appliedCheckBox) {
		String OrderName = TestData.read("PageConfiguration.xml", testDataSetName, "OrderName", category);
		String OrderQauntity = TestData.read("PageConfiguration.xml", testDataSetName, "OrderQauntity", category);
		this.clickOnOrderChangeStatuses();
		this.setOrderStatusName(OrderName);
		this.setOrderStatusOrder(OrderQauntity);
		this.checkApplieOrderdCheckBox(appliedCheckBox);
		this.clickOnSaveChanges();
	}
	
	private void clickOnOrderChangeStatuses() {
        linkChangeOrderStatuses.click();
	}

	private void clickOnOrderChangeType() {
        linkChangeOrderType.click();
	}
	
	private void setOrderStatusName(String OrderName) {
        textOrderStatusChangeName.setText(OrderName);
	}
	
	private void setOrderStatusOrder(String OrderQauntity) {
        textOrderStatusOrder.setText(OrderQauntity);
	}
	
	private void checkApplieOrderdCheckBox(boolean appliedCheckBox) {
        if(appliedCheckBox) {
            cbApplyToOrder.select();
        } else {
            cbApplyToOrder.deselect();
        }
	}
	
	private void clickOnSaveChanges() {
        linkSaveChanges2.click();
	}

	public void configureOrderChangeTypeWithMulitpleProduct(String testdataset, String category, boolean allowOrder,
			String selectProduct1, String selectProduct2) {
		String orderChangeName = TestData.read("PageConfiguration.xml", testdataset, "OrderName", category);
		this.clickOnOrderChangeType();
		this.clickAddNewButton();
		this.setOrderChangeName(orderChangeName);
		this.allowOrderStatusChangeCheckbox(allowOrder);
		this.selectProductCategory_OrderChangeType(selectProduct1);
        selectProductCategory.sendKeys(Keys.CONTROL.toString());
		this.selectProductCategory_OrderChangeType(selectProduct2);
		this.clickSaveChangesButton();
	}
	
	private void setOrderChangeName(String orderChangeName) {
        textOrderEnglishName.setText(orderChangeName);
	}
	
	private void allowOrderStatusChangeCheckbox(java.lang.Boolean allowOrder) {
		if (allowOrder) {
            cbAllowOrderStatusChange.select();
		} else {
            cbAllowOrderStatusChange.deselect();
		}
	}
	
	private void selectProductCategory_OrderChangeType(String selectProduct) {
        selectProductCategory.selectByVisibleText(selectProduct);
	}
	
	public void configureOrderChangeTypeWithoutAllowStatusChange(String testdataset, String category, String selectProduct1,
			String selectProduct2) {
		String orderChangeName = TestData.read("PageConfiguration.xml", testdataset, "OrderName", category);
		this.clickOnOrderChangeType();
		this.clickAddNewButton();
		this.setOrderChangeName(orderChangeName);
		this.selectProductCategory_OrderChangeType(selectProduct1);
        selectProductCategory.sendKeys(Keys.CONTROL.toString());
		this.selectProductCategory_OrderChangeType(selectProduct2);
		this.clickSaveChangesButton();
	}

	private void ClickOnOrderPeriod() {
        linkOrderPeriod.click();
	}

	public String createOrderPeriod(String testDataSetName, String category) {
		String description = TestData.read("PageConfiguration.xml", testDataSetName, "description", category);
		String value = TestData.read("PageConfiguration.xml", testDataSetName, "value", category);
		String billingPeriod = TestData.read("PageConfiguration.xml", testDataSetName, "billingPeriod", category);

		this.ClickOnOrderPeriod();
		this.clickAddNewButton();
		this.setOrderPeriodDescription(description);
		this.selectBillingProcessPeriod(billingPeriod);
		this.setOrderPeriodValue(value);
		this.clickSaveChangesButton();
		return description;
	}
	
	public String getDefaultCurrencyValue() {
		String currentCurrency = selectDefaultCurrency.getFirstSelectedValue();            // GlobalController.brw.getDropDownSelectedValue(this.DD_DEFAULT_CURRENCY);
		return currentCurrency;
	}

	private void selectEuroCurrenyCheckBox(boolean currencyName) {
		if (currencyName) {
            cbEuroCurrency.select();
            textEuroCurrencyRate.setText("1");
            textDollarCurrencyRate.setText("1");
		} else {
            cbEuroCurrency.deselect();
            textDollarCurrencyRate.setText("1");
		}
	}

	private void setDefaultCurrencyValue(String targetValue) {
        selectDefaultCurrency.selectByVisibleText(targetValue);
	}

	public void setCurrency(String testDataSetName, String category) {
		String currencyName = TestData.read("PageConfiguration.xml", testDataSetName, "currencyName", category);
		boolean CurrencyNameCheckBox = TextUtilities.compareValue(
				TestData.read("PageConfiguration.xml", testDataSetName, "EnableCurrencyName", category), "true", true,
				TextComparators.equals);
		this.selectEuroCurrenyCheckBox(CurrencyNameCheckBox);
		this.clickSaveChangesButton();
		this.setDefaultCurrencyValue(currencyName);
		this.clickSaveChangesButton();
	}

	public void ClickOnCurrencyLink() {
        linkCurrencies.click();
	}

	public void resetCurrecncy(String testDataSetName, String category, String lastCurrencyName) {
		this.setDefaultCurrencyValue(lastCurrencyName);
		this.clickSaveChangesButton();
	}

    public void clickOnMetafieldID() {
        linkMetaFieldId.click();
    }

    public void clickEditAIT() {
        linkEditAitType.click();
    }

    public void clickUserNotification(boolean checkbox) {
        cbUserNotificationAccountType.select();
    }

    public void popupYesAit() {
        btnYesPopupAit.click();
    }

    public void verifyDropdownIncludeinNotifications(String value) {
        Assert.assertTrue(selectAitIncludeNotifications.hasOption(value));
    }

    private void clickCreateSeller() {
        linkCreateSeller.click();
    }

    private void setSellerLoginName(String loginName) {
        txtLoginName.setText(loginName);
    }

    private void setSellerFirstName(String firstname) {
        txtFirstName.setText(firstname);
    }

    private void setSellerLastName(String lastname) {
        txtLastName.setText(lastname);
    }

    private void setSellerFirstNumber(String number) {
        txtFirstPhone.setText(number);
    }

    private void setsSllerConuntryCode(String conuntrycode) {
        selectConuntryCode.selectByVisibleText(conuntrycode);
    }

    private void setSellerContactPhoneNumber(String number) {
        txtContactPhoneNumber.setText(number);
    }

    private void setSellerPhoneAreaCode(String number) {
        txtPhoneAreaCode.setText(number);
    }

    private void setSellerEmail(String email) {
        txtEmail.setText(email);
    }

    private void setSellerOrganization(String Organization) {
        txtOrganization.setText(Organization);
    }

    private void setsellerAddress1(String address1) {
        txtAddress1.setText(address1);
    }

    private void setSellerStateProvince(String StateProvince) {
        txtStateProvince.setText(StateProvince);
    }

    private void setSellerZipPostalCode(String ZipPostalCode) {
        txtZipPostalCode.setText(ZipPostalCode);
    }

    private void setSellerSave() {
        linkSaveChangesSeller.click();
    }

    public void createSellerCompany(String testDataSetName, String category) {
        String login = TestData.read("PageConfiguration.xml", testDataSetName, "loginname", category);
        String FirstName = TestData.read("PageConfiguration.xml", testDataSetName, "FirstNa", category);
        String LastName = TestData.read("PageConfiguration.xml", testDataSetName, "lastNa", category);
        String PhoneNumber = TestData.read("PageConfiguration.xml", testDataSetName, "firstNu", category);
        String SecondNumer = TestData.read("PageConfiguration.xml", testDataSetName, "SectNu", category);
        String FinalNumber = TestData.read("PageConfiguration.xml", testDataSetName, "finalnumber", category);
        String Email = TestData.read("PageConfiguration.xml", testDataSetName, "email", category);
        String OrganizationName = TestData.read("PageConfiguration.xml", testDataSetName, "org", category);
        String Address = TestData.read("PageConfiguration.xml", testDataSetName, "add", category);
        String State_Province = TestData.read("PageConfiguration.xml", testDataSetName, "stat", category);
        String Country = TestData.read("PageConfiguration.xml", testDataSetName, "county", category);
        String Zip_Postal_Code = TestData.read("PageConfiguration.xml", testDataSetName, "zip", category);

        this.clickCreateSeller();
        this.setSellerLoginName(login);
        this.setSellerFirstName(FirstName);
        this.setSellerLastName(LastName);
        this.setSellerFirstNumber(PhoneNumber);
        this.setSellerContactPhoneNumber(FinalNumber);
        this.setSellerPhoneAreaCode(SecondNumer);
        this.setSellerEmail(Email);
        this.setSellerOrganization(OrganizationName);
        this.setsellerAddress1(Address);
        this.setSellerStateProvince(State_Province);
        this.setsSllerConuntryCode(Country);
        this.setSellerZipPostalCode(Zip_Postal_Code);
        this.setSellerSave();
    }

    private void defaultCurrency(String defaultvalue) {
        selectCurrencyDefault.selectByVisibleText(defaultvalue);
    }

    private void clickSaveCurrencies() {
        linkSaveChanges.click();
    }

    public String defaultCurrencySetup(String testDataSetName, String category) {
        String defaultCurr = TestData.read("PageConfiguration.xml", testDataSetName, "defaultCurry", category);
        this.defaultCurrency(defaultCurr);
        this.clickSaveCurrencies();
        return defaultCurr;
    }

    public void clickDeleteAit() {
        linkDeleteAitType.click();
    }

    public void clickPopupYes() {
        buttonDeleteAitTypeYes.click();
    }

    public void verifyErrorMessage() {
        messagesPage.isErrorMessageAppeared();
    }

    public void clickPaymentNotification(String metaFieldName) {
        explicitWaitForJavascript();
        selectListElementByText(listPayments, metaFieldName);
    }

    private void addPaymentnotification() {
        linkAddPaymentNotification.click();
        explicitWaitForJavascript();
    }

    private void setPaymentNotification(String desc) {
        selectAddPaymentNotificationDescLang.selectByVisibleText("English");
        linkAddPaymentNotificationDesc.click();
        textAddPaymentNotificationDesc.setText(desc);
    }

    private void setPaymentNotificationSave() {
        linkNotificationSaveChanges.click();
    }

    public void createPaymentNotification(String testdata, String category) {
        String setdesc = TestData.read("PageConfiguration.xml", testdata, "Descrip", category);
        this.clickPaymentNotification("Payments");
        this.addPaymentnotification();
        this.setPaymentNotification(setdesc);
        this.setPaymentNotificationSave();
    }

    private void clickRecentPaymentNotification() {
        linkRecentNotification.click();
    }

    public void clickEditPaymentNotification() {
        linkEditPaymentNotification.click();
    }

	public void clickentityLogosLink(){linkEntityLogos.click();}

    private void verifyEmail(String value) {
        Assert.assertTrue(selectPaymentValue.hasOption(value));
    }

    public void setEditAndVerifyValuePresent(String testDataSetName, String category) {
        String emavalue = TestData.read("PageConfiguration.xml", testDataSetName, "verifyemail", category);
        this.clickRecentPaymentNotification();
        this.clickEditPaymentNotification();
        this.verifyEmail(emavalue);
    }

    private void currencyActiveEuro(boolean checkbox) {
        if (checkbox) {
            cbEuro.select();
            textEuroCurrencyRate.setText("0.8118");
        } else {
            cbEuro.deselect();
        }
    }

    private void currencyActivePoundSterling(boolean checkbox) {
        if (checkbox) {
            cbPoundSterling.select();
            textSterlingCurrencyRate.setText("0.5479");
        } else {
            cbPoundSterling.deselect();
        }
    }

    private void currencyWon(boolean checkbox) {
        if (checkbox) {
            cbWon.select();
            textWonCurrencyRate.setText("1171.0000");
        } else {
            cbWon.deselect();
        }
    }

    private void currencyActiveYen(boolean checkbox) {
        if (checkbox) {
            cbYen.select();
            textYenCurrencyRate.setText("111.4000");
        } else {
            cbYen.deselect();
        }
    }

    private void setRateForCurrentCurrencies() {
        for(TextInput textCurrency : currentSelectedCurrencies) {
            textCurrency.setText("1");
        }
    }

    public void activeCurrency() {
        setRateForCurrentCurrencies();
        this.currencyActiveEuro(true);
        this.currencyActivePoundSterling(true);
        this.currencyWon(true);
        this.currencyActiveYen(true);
        this.clickSaveCurrencies();
    }

    public void deactivateCurrencies() {
        this.currencyActiveEuro(false);
        this.clickSaveCurrencies();
    }

    public void unusedCurrency() {
        this.currencyActiveYen(false);
        this.clickSaveCurrencies();
    }

    private void selectPluginFromTable (String pluginName) {
        selectListElementByText(listPlugins, pluginName);
    }

    public void addPlugin (String testDataSetName, String category) {
        String pluginType = TestData.read("PageConfiguration.xml", testDataSetName, "pluginType", category);
        String pluginName = TestData.read("PageConfiguration.xml", testDataSetName, "pluginName", category);
        String order = TestData.read("PageConfiguration.xml", testDataSetName, "order", category);

        this.selectPluginFromTable(pluginName);

        if (!  isElementInList(listPluginTypes, pluginType)) {
            this.clickAddNewButton();
            this.selectPluginType(pluginType);
            this.setOrderTestData(order);
            this.clickSavePlugin();

            messagesPage.verifyDisplayedMessageText("The new plug-in with id", "has been saved", TextComparators.contains);
        }
    }

    private void selectMetaFieldCategory (String category) {
        selectListElementByText(listMetaFieldsCategories, category);
    }

    public void addMetaFieldInMetaFieldCategory (String testDataSetName, String category) {
        String metaFieldCategory = TestData.read("PageConfiguration.xml", testDataSetName, "metaFieldCategory", category);
        String metaFieldName = TestData.read("PageConfiguration.xml", testDataSetName, "metaFieldName", category);
        this.selectMetaFieldCategory(metaFieldCategory);
        explicitWaitForJavascript();
        if (! isTextPresentInTable(metaFieldName, tableVerifyCreatedMetaData)) {
            this.clickAddNewButton();
            this.enterNewMetaDataName(metaFieldName);
            this.clickSaveChangesButton();
        }
    }

    public String setEditAndPaymentSave (String testDataSetName, String category) {

        String subj = TestData.read("PageConfiguration.xml", testDataSetName, "sub", category);
        this.clickEditPaymentNotification();
        this.setEditSubject(subj);
        this.setEditSubjectSave();
        return subj;
    }

    private void setEditSubject(String sub) {
        textPaymentNotificationSubject.setText(sub);
    }

    private void setEditSubjectSave () {
        linkPaymentNotificationSave.click();
    }

    public void validateEditPaymentSavedTestData (String data) {
        Assert.assertTrue(htmlVerifyTableData.getText().contains(data));
//        GlobalController.brw.validateSavedTestData(this.tab_VERIFYTABDATA, data);
    }

    public void clickNewCreatedPayment () {
        linkNotification.click();
    }

    public void notificationCheckBox (String testDataSetName, String category) {
        boolean value1 = TextUtilities.compareValue(TestData.read("PageConfiguration.xml", testDataSetName, "value1", category), "true",
                true, TextComparators.equals);
        boolean value2 = TextUtilities.compareValue(TestData.read("PageConfiguration.xml", testDataSetName, "value2", category), "true",
                true, TextComparators.equals);
        boolean value3 = TextUtilities.compareValue(TestData.read("PageConfiguration.xml", testDataSetName, "value3", category), "true",
                true, TextComparators.equals);
        boolean value4 = TextUtilities.compareValue(TestData.read("PageConfiguration.xml", testDataSetName, "value4", category), "true",
                true, TextComparators.equals);

        this.clickNotifyParent(value1);
        this.clickNotifyAdmin(value2);
        this.clickNotifyAgent(value3);
        this.clickAllNotifyParent(value4);
        this.setEditSubjectSave();
    }

    private void clickNotifyParent (boolean checkbox) {
        if (checkbox) {
            cbNotifyParent.select();
        } else {
            cbNotifyParent.deselect();
        }
    }

    private void clickNotifyAdmin (boolean checkbox) {
        if (checkbox) {
            cbNotifyAdmin.select();
        } else {
            cbNotifyAdmin.deselect();
        }
    }

    private void clickNotifyAgent (boolean checkbox) {
        if (checkbox) {
            cbNotifyAgent.select();
        } else {
            cbNotifyAgent.deselect();
        }
    }

    private void clickAllNotifyParent(boolean checkbox) {
        if (checkbox) {
            cbNotifyAllParents.select();
        } else {
            cbNotifyAllParents.deselect();
        }
    }

    public void validateValueNotification (String testDataSetName, String category) {

        String value_one = TestData.read("PageConfiguration.xml", testDataSetName, "one", category);
        String value_twoth = TestData.read("PageConfiguration.xml", testDataSetName, "two", category);
        String value_three = TestData.read("PageConfiguration.xml", testDataSetName, "three", category);
        String value_fourth = TestData.read("PageConfiguration.xml", testDataSetName, "fourth", category);

        String val_one = htmlVerifyCbAdmin.getText();
        String value_tw = htmlVerifyCbAgent.getText();
        String val_three = htmlVerifyCbNotifyAllParent.getText();
        String val_four = htmlVerifyCbNotifyParent.getText();

        Assert.assertEquals(value_one, val_one);
        Assert.assertEquals(value_twoth, value_tw);
        Assert.assertEquals(value_three, val_three);
        Assert.assertEquals(value_fourth, val_four);
    }

    public String newAccountTypeMethod (String testDataSetName, String category, String paymentMethodNameOne) {
        String accountName = TestData.read("PageConfiguration.xml", testDataSetName, "accountName", category);
        String invoiceDesign = TestData.read("PageConfiguration.xml", testDataSetName, "invoiceDesign", category);

        this.clickAccountTypeLink();
        this.clickAddNewButton();
        this.setAccountTypeName(accountName);
        this.setAccountTypeInvoiceDesign(invoiceDesign);
        this.selectPaymentMethodTypes(paymentMethodNameOne);
        this.clickOnSaveChanges();
        return accountName;
    }

    private void clickAccountTypeLink () {
        linkAccountType.click();
    }

    public void verifyBlackListedCustomerWithDetail (String CustomerName) {
        this.clickOnBlackList();
        this.setNewFilterInBlackListCustomer(CustomerName);
        this.verifyCustomerName(CustomerName);
        this.verifyCustomerCreditCard();
    }

    private void clickOnBlackList () {
        linkBlacklist.click();
    }

    private void setNewFilterInBlackListCustomer (String CustomerName) {
        textSearchCustomer.setText(CustomerName);
        textSearchCustomer.sendKeys(Keys.ENTER.toString());
        explicitWaitForJavascript();
    }

    private void verifyCustomerName(String CustomerName) {
        String name = linkVerifyBlackListedCustomer.getText();
        if (TextUtilities.contains(name, CustomerName)) {
            Assert.assertTrue(true);
        } else {
            throw new RuntimeException("Test Case failed: ");
        }
    }

    private void verifyCustomerCreditCard () {
        linkCreditCard.isDisplayed();
    }

    public void deleteAccountTypeUsedMethod(String accountName) {
        this.clickAccountTypeLink();
        this.selectAccountTypeFromTable(accountName);
        this.clickOnAccountTypeDelete();
        this.verifyValidationMessageOnDeleteConfirmation();
        this.clickNoLinkOnConfirmationMessage();
    }

    private void selectAccountTypeFromTable(String accountType) {
        selectListElementByText(listAccountTypes, accountType);
    }

    private void clickOnAccountTypeDelete() {
        linkDeleteAccountType.click();
    }

    private void clickNoLinkOnConfirmationMessage() {
        linkConfirmNo.click();
    }

    private void verifyValidationMessageOnDeleteConfirmation() {
        htmlConfMsgVerify.exists();
    }

    public String addNewAccountTypeInMetaField (String testdataset, String category) {
        String Name = TestData.read("PageConfiguration.xml", testdataset, "Name", category);

        this.clickOnMetaFieldLink();
        this.clickOnAccountTypeInMetaField();
        this.clickAddNewButton();
        this.enterNewMetaDataName(Name);
        this.clickOnSaveChanges();

        return Name;
    }

    private void clickOnAccountTypeInMetaField () {
        linkMfAccountType.click();
    }

    public void importMetafieldsUsingAITWithACCTYPE (String accountType, String metaFeild) {
        this.clickAccountTypeLink();
        this.selectAccountTypeName(accountType);
        this.clickOnAddInformationType();
        this.clickOnImportMetafields();
        this.selectMetaFieldUnderImport(metaFeild);
        this.verifyAddNewMetaFeilds();
    }

    private void clickOnImportMetafields () {
        linkImportMetafields.click();
    }

    private void selectMetaFieldUnderImport (String metaFeild) {
        selectListElementByText(listImportMetafields, metaFeild);
    }

    private void verifyAddNewMetaFeilds () {
        htmlAddNewMeta.exists();
    }

    public String addPaymentMethod (String testDataSetName, String category, boolean allAccountTypes, boolean isRecurring) {
        String methodName = TestData.read("PageConfiguration.xml", testDataSetName, "methodName", category);
        String paymentMethod = TestData.read("PageConfiguration.xml", testDataSetName, "PaymentMethod", category);

        this.clickOnPaymentMethod();
        this.clickAddNewButton();
        this.selectPaymentTemplate(paymentMethod);
        this.clickSelectButton();
        this.setMethodName(methodName);
        this.checkAllAccountTypes(allAccountTypes);
        this.checkIsRecurring(isRecurring);
        this.clickOnSaveChanges();

        return methodName;
    }

    private void clickOnPaymentMethod() {
        linkPaymentMethod.click();
    }

    private void selectPluginTypeFromTable (String pluginType) {
        selectListElementByText(listPluginTypes, pluginType);
    }

    private void clickEditPlugin () {
        linkEditPlugin.click();
    }

    private void setPortForPlugin (String portNumber) {
        textPluginPortNumber.setText(portNumber);
    }

    public void editPlugin (String testDataSetName, String category) {
        String pluginType = TestData.read("PageConfiguration.xml", testDataSetName, "pluginType", category);
        String pluginName = TestData.read("PageConfiguration.xml", testDataSetName, "pluginName", category);
        String portNumber = TestData.read("PageConfiguration.xml", testDataSetName, "portNumber", category);

        this.selectPluginFromTable(pluginName);
        this.selectPluginTypeFromTable(pluginType);
        this.clickEditPlugin();
        this.setPortForPlugin(portNumber);
        this.clickSavePlugin();
    }

    public void selectNotification (String text) {
        selectListElementByText(listNotificationNames, text);
        explicitWaitForJavascript();
    }

    public void selectDataInTable(List<WebElement> cells, List<WebElement> ef2, String text) {
        logger.debug("Finding index: " + text + " :in given Pager box");
        int targetValueIndex = 0;
        int i = cells.size();
        logger.debug("cells size is :::::::::" + i);
        if (isElementInList(ef2, text)) {
            selectListElementByText(ef2, text);
        } else {
            for (int j = 1; j <= i; j++) {
                String loc = "//div[@class='pager-box']/div[2]/a[" + j + "]";
                WebElement we = this.driver.findElement(By.xpath(loc));
                explicitWaitForJavascript();
                we.click();
//                this._clickUsingJavaScript(we);
                explicitWaitForJavascript();
                if (isElementInList(ef2, text)) {
                    selectListElementByText(ef2, text);
                    break;
                } else {
                    targetValueIndex++;
                    continue;
                }
            }
        }
        if (targetValueIndex >= cells.size()) {
            throw new RuntimeException("Unable to Find Given text: " + text);
        }
    }

    public void setSubject (String subject) {
        textSubject.setText(subject);
    }

    public void setBodyTextNotification (String bodyText) {
        textEmailBody.setText(bodyText);
    }

    public void checkActiveCheckBoxChecked () {
        Assert.assertTrue(cbActive.isSelected());
    }

    public void checkUncheckActiveCheckbox (boolean checkbox) {
        if (checkbox) {
            cbActive.select();
        } else {
            cbActive.deselect();
        }
    }

	public void uploadNavigationBarLogo(String path){
		driver.findElement(By.id("logo")).sendKeys(GlobalConsts.getProjectDir() + path);
	}

	public void uploadFaviconLogo(String path){
		driver.findElement(By.id("favicon")).sendKeys(GlobalConsts.getProjectDir() + path);
	}

	public String isNavigationLogoUploadSuccesful () {
		return messagesPage.isOperationSuccessfulOnMessage("The Navigation Bar logo has been updated successfully", TextComparators.contains);
	}

	public String isFaviconLogoUploadSuccesful () {
		return messagesPage.isOperationSuccessfulOnMessage("The Favicon Logo has been updated successfully", TextComparators.contains);
	}
}
