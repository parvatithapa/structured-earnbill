package com.jbilling.test.ui.pages;

import java.math.BigDecimal;
import java.util.List;

import com.jbilling.framework.globals.GlobalController;
import com.jbilling.framework.interfaces.ElementField;
import com.jbilling.framework.interfaces.LocateBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.CheckBox;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.ClearingData;
import com.jbilling.test.ui.CreditCard;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;
import com.jbilling.test.ui.parts.DivPaymentFields;
import ru.yandex.qatools.htmlelements.element.Table;
import ru.yandex.qatools.htmlelements.element.TextBlock;

public class CustomersPage extends AppPage {

    static public class UserEditForm extends HtmlElement {

        @FindBy(id = "user.userName")
        private TextInput        inputUserName;

        @FindBy(id = "user.partnerIdList")
        private TextInput        inputAgentIds;

        @FindBy(id = "user.currencyId")
        private Select           selectCurrency;

        @FindBy(name = "mainSubscription.periodId")
        private Select           selectBillingCycleUnit;

        @FindBy(name = "mainSubscription.nextInvoiceDayOfPeriod")
        private Select           selectBillingCycleDay;

        @FindBy(id = "user.isParent")
        private CheckBox         checkboxAllowSubAccounts;

        @FindBy(id = "user.nextInvoiceDate")
        private TextInput        inputNextInvoiceDate;

        @FindBy(id = "companyBillingCycle")
        private Link             linkUseCompanyBillingCycle;

        @FindBy(id = "payment-method-fields-0")
        private DivPaymentFields divPaymentFields;

        @FindBy(name = "paymentMethod_0.paymentMethodTypeId")
        private Select           selectPaymentMethodType;

        // inputs for metafields added for test case 98
        @FindBy(xpath = "//label[contains(text(),'Address')]/../div/input")
        private TextInput        inputAddress;

        @FindBy(xpath = "//label[contains(text(),'Salary')]/../div/input")
        private TextInput        inputSalary;

        @FindBy(xpath = "//label[contains(text(),'Email')]/../div/input")
        private TextInput        inputEmail;

        @FindBy(xpath = "//label[contains(text(),'City')]/../div/input")
        private TextInput        inputCity;

        private void setCardData (String paymentMethodType, CreditCard cardData) {
            selectPaymentMethodType.selectByVisibleText(paymentMethodType);
            divPaymentFields.setCardData(cardData);
        }

        private void setClearingData (String paymentMethodType, ClearingData clearingData) {
            selectPaymentMethodType.selectByVisibleText(paymentMethodType);
            divPaymentFields.setClearingData(clearingData);
        }
    }

    static public class SelectAccountTypeForm extends HtmlElement {

        @FindBy(id = "user.entityId")
        private Select selectCompany;

        @FindBy(id = "accountTypeId")
        private Select selectAccountType;

        @FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'save')]/span[text()='Select']/..")
        private Link   linkSelectAccountType;

        void setAccountType (String accountType) {
            selectAccountType.selectByVisibleText(accountType);
            linkSelectAccountType.click();
        }
    }

    @FindBy(id = "user-account-select-form")
    private SelectAccountTypeForm formSelectAccountType;

    @FindBy(id = "user-edit-form")
    private UserEditForm          formUserEdit;

    @FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'save')]/span[text()='Save Changes']/..")
    private Link                  linkSaveChanges;

    @FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'cancel')]/span[text()='Cancel']/..")
    private Link                  linkCancel;

    @FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'add')]/span[text()='Blacklist']/..")
    private Link                  linkBlacklist;

    @FindBy(xpath = "//a[contains(@class, 'submit') and contains(@class, 'payment')]/span[text()='Make Payment']/..")
    private Link                  linkMakePayment;

    @FindBy(xpath = "//a[@title='Inspect this customer']")
    private Link                  linkInspect;

    @FindBy(xpath = "//table[@id='users']/tbody//td/a/strong")
    private List<WebElement>      listCustomers;

    @FindBy(xpath = "//table[@id='products']/tbody//td/a/strong")
    private List<WebElement>      listProducts;

    @FindBy(id = "users")
    private Table                 tableCustomers;

    @FindBy(xpath = "//a[contains(text(),'Show all orders')]")
    private Link                  linkShowAllOrders;

    @FindBy(xpath = "//a/span[text()= 'Generate Invoice']/..")
    private Link                  linkGenerateInvoice;

    @FindBy(xpath = "//a/span[text()= 'Prices']/..")
    private Link                  linkPricesHeader;

    @FindBy(xpath = "//select[@id='typeId']")
    private Select                selectProductCategory;

    @FindBy(xpath = "//input[@id='filterBy']")
    private TextInput             filterProducts;

    @FindBy(xpath = "//a[@class='submit add']/span[text()='Add Price']/..")
    private Link                  linkAddPrice;

    @FindBy(xpath = "//a[contains(@onclick,'deleteCustomerPrice')]")
    private Link                  linkDeleteCustomerPrice;

    @FindBy(xpath = "//div[@class='form-edit']//strong[contains(text(),'Add Customer Price')]")
    private TextBlock             txtAddCustomerPrice;

    @FindBy(xpath = "//input[@id='model.0.rateAsDecimal']")
    private TextInput             inputRate;

    @FindBy(xpath = "//input[@id='startDate']")
    private TextInput             inputStartDate;

    @FindBy(xpath = "//input[@id='priceExpiryDate']")
    private TextInput             inputPriceExpiry;

    @Autowired
    protected OrderBuilderPage    orderBuilderPage;
    @Autowired
    protected OrdersPage          ordersPage;
    @Autowired
    protected MessagesPage messagesPage;

    public CustomersPage(WebDriver driver) {
        super(driver);
    }

    public void statusCycle (String testDataSetNameForCustomerOne, String testDataSetNameForCustomerTwo, String category) {
    }

    public void verifyShowAllOrder (String customerName) {
        selectCustomer(customerName);
        linkShowAllOrders.click();
        explicitWaitForJavascript();
        ordersPage.selectOrderByCustomerName(customerName);
    }

    public void setPaymentMethodAndDetails (String testDataSetName, String category, String payment) {
        String cardHolderName = TestData.read("PageCustomers.xml", testDataSetName, "cardHolderName", category);
        String cardNumber = TestData.read("PageCustomers.xml", testDataSetName, "cardNumber", category);
        String expiryDate = TestData.read("PageCustomers.xml", testDataSetName, "expiryDate", category);
        String paymentMethod = TestData.read("PageCustomers.xml", testDataSetName, "paymentMethod", category);

        if ((payment != null) || (payment == "")) {
            formUserEdit.setCardData(payment,
                    new CreditCard.DefaultCreditCard(cardHolderName, cardNumber, expiryDate));
        } else {
            formUserEdit.setCardData(paymentMethod,
                    new CreditCard.DefaultCreditCard(cardHolderName, cardNumber, expiryDate));
        }
        linkSaveChanges.click();
    }

    public void setResetPriceForCustomer (String condition, String testDataSetName, String category,
                                          String customerName, String productCategory, String product) {
        String rate = TestData.read("PageCustomers.xml", testDataSetName, "rate", category);
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String priceExpiryDate = TestData.read("PageCustomers.xml", testDataSetName, "priceExpiryDate", category);

        switch (condition) {
            case "set_Price":
                selectCustomer(customerName);
                linkInspect.click();
                linkPricesHeader.click();
                selectProductCategory.selectByVisibleText(productCategory);
                explicitWaitForJavascript();
                selectProduct(product);
                linkAddPrice.click();
                verifyAddCustomerPricePage();
                inputRate.setText(rate);
                inputStartDate.setText(startDate);
                inputPriceExpiry.setText(priceExpiryDate);
                linkSaveChanges.click();
                break;
            case "reset_Price":
                this.selectCustomer(customerName);
                linkInspect.click();
                linkPricesHeader.click();
                selectProductCategory.selectByVisibleText(productCategory);
                explicitWaitForJavascript();
                selectProduct(product);
                linkDeleteCustomerPrice.click();
                break;
        }
    }

    private void verifyAddCustomerPricePage () {
        Assert.assertTrue(txtAddCustomerPrice.exists());
    }

    private void filterProducts (String textForProductFiltering) {
        filterProducts.setText(textForProductFiltering);
        filterProducts.sendKeys(Keys.TAB);
        explicitWaitForJavascript();
    }

    private void selectProduct (String product) {
        filterProducts(product);
        selectListElementByText(listProducts, product);
        explicitWaitForJavascript();
    }

    public String addCustomer (String accountType, String paymentMethod, String testDataSetName, String category) {
        String cardHolderName = TestData.read("PageCustomers.xml", testDataSetName, "cardHolderName", category);
        String cardNumber = TestData.read("PageCustomers.xml", testDataSetName, "cardNumber", category);
        String expiryDate = TestData.read("PageCustomers.xml", testDataSetName, "expiryDate", category);
        String login = TestData.read("PageCustomers.xml", testDataSetName, "loginName", category);
        String allowSubAccount = TestData.read("PageCustomers.xml", testDataSetName, "allowSubAccount", category);

        clickAdd();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);
        formUserEdit.checkboxAllowSubAccounts.set(Boolean.parseBoolean(allowSubAccount));

        formUserEdit.setCardData(paymentMethod,
                new CreditCard.DefaultCreditCard(cardHolderName, cardNumber, expiryDate));

        linkSaveChanges.click();

        return login;
    }

    public String addNewCustomer (String accountType, String paymentMethod, String testdataset, String category) {

        String cardHolderName = TestData.read("PageCustomers.xml", testdataset, "cardHolderName", category);
        String cardNumber = TestData.read("PageCustomers.xml", testdataset, "cardNumber", category);
        String expiryDate = TestData.read("PageCustomers.xml", testdataset, "expiryDate", category);
        String login = TestData.read("PageCustomers.xml", testdataset, "loginName", category);

        clickAdd();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);

        formUserEdit.setCardData(paymentMethod,
                new CreditCard.DefaultCreditCard(cardHolderName, cardNumber, expiryDate));

        linkSaveChanges.click();

        return login;
    }

    public String addChildCustomer (String accountType, String paymentMethod, String testDataSetName, String category) {
        String billingCycleDay = TestData.read("PageCustomers.xml", testDataSetName, "billingCycleDay", category);
        String cardHolderName = TestData.read("PageCustomers.xml", testDataSetName, "cardHolderName", category);
        String cardNumber = TestData.read("PageCustomers.xml", testDataSetName, "cardNumber", category);
        String expiryDate = TestData.read("PageCustomers.xml", testDataSetName, "expiryDate", category);
        String login = TestData.read("PageCustomers.xml", testDataSetName, "loginName", category);
        String agent = TestData.read("PageCustomers.xml", testDataSetName, "agent", category);
        String billingCycleUnit = TestData.read("PageCustomers.xml", testDataSetName, "billingCycleUnit", category);
        String allowSubAccount = TestData.read("PageCustomers.xml", testDataSetName, "allowSubAccount", category);

        divColumnRight.button("Add Sub-Account").click();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);
        formUserEdit.checkboxAllowSubAccounts.set(Boolean.parseBoolean(allowSubAccount));
        formUserEdit.inputAgentIds.setText(agent);

        formUserEdit.linkUseCompanyBillingCycle.click();

        formUserEdit.selectBillingCycleUnit.selectByVisibleText(billingCycleUnit);
        formUserEdit.selectBillingCycleDay.selectByVisibleText(billingCycleDay);

        formUserEdit.setCardData(paymentMethod,
                new CreditCard.DefaultCreditCard(cardHolderName, cardNumber, expiryDate));

        linkSaveChanges.click();

        return login;
    }

    public String addACHCustomerType (String accountType, String paymentMethod, String testDataSetName, String category) {
        String billingCycleDay = TestData.read("PageCustomers.xml", testDataSetName, "billingCycleDay", category);
        String login = TestData.read("PageCustomers.xml", testDataSetName, "loginName", category);
        String billingCycleUnit = TestData.read("PageCustomers.xml", testDataSetName, "billingCycleUnit", category);
        String achRoutingNumber = TestData.read("PageCustomers.xml", testDataSetName, "achroutingnumber", category);
        String achCustomerName = TestData.read("PageCustomers.xml", testDataSetName, "achcustomername", category);
        String achAccountNumber = TestData.read("PageCustomers.xml", testDataSetName, "achaccounnumber", category);
        String achBankName = TestData.read("PageCustomers.xml", testDataSetName, "achbankname", category);
        String achAccountType = TestData.read("PageCustomers.xml", testDataSetName, "achaccounttype", category);

        clickAdd();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);

        formUserEdit.selectBillingCycleUnit.selectByVisibleText(billingCycleUnit);
        formUserEdit.selectBillingCycleDay.selectByVisibleText(billingCycleDay);

        formUserEdit.setClearingData(paymentMethod, new ClearingData.DefaultClearingData(achRoutingNumber,
                achCustomerName, achAccountNumber, achBankName, achAccountType));

        linkSaveChanges.click();

        return login;
    }

    public void addCustomerWithAccountType (String login, String accountType) {

        clickAdd();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);

        linkSaveChanges.click();
    }

    public String addCustomerWithMakePayment (String testDataSetName, String category, String accountType) {
        String login = TestData.read("PageCustomers.xml", testDataSetName, "name", category);

        clickAdd();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);

        linkSaveChanges.click();
        return login;
    }

    public String addCustomerWithCurrency (String testDataSetName, String category) {
        String login = TestData.read("PageCustomers.xml", testDataSetName, "loginname", category);
        String currency = TestData.read("PageCustomers.xml", testDataSetName, "CrrencySelection", category);

        clickAdd();
        formSelectAccountType.linkSelectAccountType.click();

        formUserEdit.inputUserName.setText(login);
        formUserEdit.selectCurrency.selectByVisibleText(currency);

        linkSaveChanges.click();

        return login;
    }

    public String createCustomerWithPaymentType (String testDataSetName, String category, String accountType,
            String paymentMethod) {
        String login = TestData.read("PageCustomers.xml", testDataSetName, "customerName", category);
        String cardHolderName = TestData.read("PageCustomers.xml", testDataSetName, "name", category);
        String cardNumber = TestData.read("PageCustomers.xml", testDataSetName, "number", category);
        String expiryDate = TestData.read("PageCustomers.xml", testDataSetName, "date", category);
        String nextInvoiceDate = TestData.read("PageCustomers.xml", testDataSetName, "nextInvoiceDate", category);

        clickAdd();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);

        formUserEdit.setCardData(paymentMethod,
                new CreditCard.DefaultCreditCard(cardHolderName, cardNumber, expiryDate));

        linkSaveChanges.click();

        divColumnRight.button("Edit").click();

        formUserEdit.inputNextInvoiceDate.setText(nextInvoiceDate);

        linkSaveChanges.click();

        return login;
    }

    public String createCustomerWithMetafileds (String testDataSetName, String category, String accountType) {
        String login = TestData.read("PageCustomers.xml", testDataSetName, "loginName", category);
        String email = TestData.read("PageCustomers.xml", testDataSetName, "email", category);
        String address = TestData.read("PageCustomers.xml", testDataSetName, "address", category);
        String city = TestData.read("PageCustomers.xml", testDataSetName, "city", category);
        String salary = TestData.read("PageCustomers.xml", testDataSetName, "salary", category);

        clickAdd();
        selectAccountType(accountType);

        formUserEdit.inputUserName.setText(login);

        formUserEdit.inputAddress.setText(address);
        formUserEdit.inputSalary.setText(salary);
        formUserEdit.inputEmail.setText(email);
        formUserEdit.inputCity.setText(city);

        return login;
    }

    public void editCustomerWithPaymentMethod (String testDataSetName, String category, String customerName,
            String paymentMethod) {

        String cardHolderName = TestData.read("PageCustomers.xml", testDataSetName, "cardHolderName", category);
        String cardNumber = TestData.read("PageCustomers.xml", testDataSetName, "cardNumber", category);
        String expiryDate = TestData.read("PageCustomers.xml", testDataSetName, "expiryDate", category);

        CreditCard creditCard = new CreditCard.DefaultCreditCard(cardHolderName, cardNumber, expiryDate);

        selectCustomer(customerName);

        divColumnRight.button("Edit").click();
        formUserEdit.setCardData(paymentMethod, creditCard);
        linkSaveChanges.click();

        divColumnRight.button("Edit").click();
        formUserEdit.divPaymentFields.assertEquals(creditCard);

        linkCancel.click();

        selectCustomer(customerName);

        linkInspect.click();
        linkBlacklist.click();
    }

    public void verifyPaymentAvailableForCustomer (String accountType, String paymentMethodType) {
        clickAdd();
        selectAccountType(accountType);
        formUserEdit.selectPaymentMethodType.selectByVisibleText(paymentMethodType);
    }

    public void clickCreateOrder () {
        divColumnRight.button("Create Order").click();
    }

    public void selectCustomer (String customerName) {
        selectListElementByText(listCustomers, customerName);
        explicitWaitForJavascript();
    }

    public void createOrder (String testDataSetName, String category, String discount) {
        String period = TestData.read("PageOrders.xml", testDataSetName, "period", category);
        String product = TestData.read("PageOrders.xml", testDataSetName, "product", category);

        explicitWaitForJavascript(40);
        clickCreateOrder();

        orderBuilderPage.withActiveSince(formatCurrentDate())
                .withOrderPeriod(period)
                .withProduct(product)
                .update()
                .withDiscount(discount)
                .gotoReviewTab()
                .saveChanges();
    }

    public void addOrder (String product, String dependencyProduct, String testDataSetName, String category) {
        String message = TestData.read("PageOrders.xml", testDataSetName, "message", category);

        clickCreateOrder();

        orderBuilderPage.withProduct(product)
                .update()
                .gotoReviewTab()
                .saveChanges()
                .verifyErrorMessage(message)
                .gotoLineChanges()
                .withDependentProduct(dependencyProduct)
                .verifyApplyNowCheckBox()
                .update()
                .saveChanges();
    }

    public void createOrderForCustomer (String testDataSetName, String category) {
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String product = TestData.read("PageCustomers.xml", testDataSetName, "product", category);
        String effectiveDate = TestData.read("PageCustomers.xml", testDataSetName, "setDate", category);

        clickCreateOrder();

        orderBuilderPage.withActiveSince(formatCurrentDate())
                .withOrderPeriod(period)
                .withProduct(product)
                .withEffectiveDate(effectiveDate)
                .update()
                .saveChanges();
    }

    public void createOrderForCancel (String testDataSetName, String category) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);

        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period).withBillingType(type).withActiveSince(startDate).cancel();
    }

    public void createOrderForInvoice (String testDataSetName, String category) {
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);
        String product = TestData.read("PageCustomers.xml", testDataSetName, "product", category);

        clickCreateOrder();

        orderBuilderPage.withActiveSince(formatCurrentDate())
                .withOrderPeriod(period)
                .withBillingType(type)
                .saveChanges()
                .withProduct(product)
                .update()
                .saveChanges();
    }

    public void createOrderWithInvalidData (String addedProduct) {

        clickCreateOrder();

        orderBuilderPage.withDueDate("<html><head><body><script>This is for test</script></body></head></html>")
                .withMinimumPeriod("<html><head><body><script>This is for test</script></body></head></html>")
                .withProduct(addedProduct)
                .withQuantity("<html><head><body><script>This is for test</script></body></head></html>")
                .saveChanges();
    }

    public void createOrderForFUPCustomer (String tc_115_customer_name, String testDataSetName, String category, String plan) {
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);

        selectCustomer(tc_115_customer_name);
        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period).withBillingType(type).withPlan(plan).update().saveChanges();
    }

    // TODO: real edit wasn't implemented by 360logica. add it ?
    public void editOrderForFUPCustomer (String tc_115_customer_name, String testDataSetName, String category) {

        selectCustomer(tc_115_customer_name);
        linkInspect.click();
    }

    public void addOrderWithAssetToCustomer (String testDataSetName, String category, String customer, String product) {
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String order = TestData.read("PageCustomers.xml", testDataSetName, "order", category);

        selectCustomer(customer);
        clickCreateOrder();

        orderBuilderPage.withActiveSince(formatCurrentDate())
                .withOrderPeriod(period)
                .withBillingType(order)
                .withProductAndFirstAsset(product)
                .saveChanges();
    }

    public void addProductInExistingCustomer (String testDataSetName, String category, String customerName,
            String produtDescription) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String effectiveDate = TestData.read("PageCustomers.xml", testDataSetName, "EffectiveDate", category);

        selectCustomer(customerName);
        clickCreateOrder();

        orderBuilderPage.withActiveSince(startDate)
                .withProduct(produtDescription)
                .withEffectiveDate(effectiveDate)
                .update();

        messagesPage.isIntermediateSuccessMessageAppeared();
    }

    private static final String TC_140_ERROR_MESSAGE = "User has already subscribed to a product/plan from the given category.";

    public void createOrderForOnePerOrder (String testDataSetName, String category, String productDescription,
            String productDescription1) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String endDate = TestData.read("PageCustomers.xml", testDataSetName, "endDate", category);
        String effectiveDate = TestData.read("PageCustomers.xml", testDataSetName, "effectiveDate", category);

        clickCreateOrder();

        orderBuilderPage.withActiveSince(startDate)
                .withActiveUntil(endDate)
                .withProduct(productDescription)
                .withEffectiveDate(effectiveDate)
                .update();

        orderBuilderPage.withProduct(productDescription1);
        messagesPage.assertTextInFirstErrorMessage(TC_140_ERROR_MESSAGE);
    }

    public void verifyAddedProductInPlan (String productDescription, String productDescription3,
            String productDescription4) {

        orderBuilderPage.withPlan(productDescription3);
        messagesPage.assertTextInFirstErrorMessage(TC_140_ERROR_MESSAGE);
        orderBuilderPage.remove().withPlan(productDescription3);
        orderBuilderPage.withPlan(productDescription4);
        messagesPage.assertTextInFirstErrorMessage(TC_140_ERROR_MESSAGE);
        orderBuilderPage.withProduct(productDescription);
        messagesPage.assertTextInFirstErrorMessage(TC_140_ERROR_MESSAGE);
    }

    public void createOrderForOnePerCustomer (String testDataSetName, String category, String productDescription) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String endDate = TestData.read("PageCustomers.xml", testDataSetName, "endDate", category);
        String effectiveDate = TestData.read("PageCustomers.xml", testDataSetName, "effectiveDate", category);
        String quantity = TestData.read("PageCustomers.xml", testDataSetName, "quantity", category);
        String quantity1 = TestData.read("PageCustomers.xml", testDataSetName, "quantity1", category);

        clickCreateOrder();

        orderBuilderPage.withActiveSince(startDate)
                .withActiveUntil(endDate)
                .withProduct(productDescription)
                .withQuantity(quantity)
                .withEffectiveDate(effectiveDate)
                .update();

        messagesPage.isErrorMessagesListAppeared();

        orderBuilderPage.withQuantity(quantity1);

        messagesPage.isIntermediateSuccessMessageAppeared();

        orderBuilderPage.saveChanges();
    }

    public void createOrderMonthly (String testDataSetName, String category, String planDescription) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);

        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period)
                .withBillingType(type)
                .withActiveSince(startDate)
                .withPlan(planDescription)
                .update()
                .saveChanges();

        verifyItemInMonthlyPostpaidOrder();
    }

    // TODO: remove duplication. 'createOrderMonthly' has exactly same code. difference in period ?
    public void createOrderYearly (String testDataSetName, String category, String planDescription) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);

        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period)
                .withBillingType(type)
                .withActiveSince(startDate)
                .withPlan(planDescription)
                .update()
                .saveChanges();

        verifyItemInMonthlyPostpaidOrder();
    }

    public void createOrderAfterPriceSet (String condition, String testDataSetName, String category,
            String customerName, String accountType, String product) {
        String rate = TestData.read("PageCustomers.xml", testDataSetName, "rate", category);
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String endDate = TestData.read("PageCustomers.xml", testDataSetName, "endDate", category);
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String order = TestData.read("PageCustomers.xml", testDataSetName, "order", category);

        selectCustomer(customerName);
        clickCreateOrder();

        switch (condition) {
        case "Price_Without_Period":

            orderBuilderPage.withActiveSince(startDate)
                    .withActiveUntil(endDate)
                    .withProduct(product)
                    .withEffectiveDate(startDate)
                    .update()
                    .gotoReviewTab();

            orderBuilderPage.assertTotalEquals(rate);
            break;

        case "Price_With_Period":

            orderBuilderPage.withOrderPeriod(period)
                    .withBillingType(order)
                    .withActiveSince(startDate)
                    .withProduct(product)
                    .update();
            break;

        case "Order_With_Date":

            orderBuilderPage.withActiveSince(startDate)
                    .withActiveUntil(endDate)
                    .withProduct(product)
                    .withEffectiveDate(startDate)
                    .update();

            break;
        }
        orderBuilderPage.saveChanges();
    }

    public void createOrderWithDifferentCurrency (String planvalue, String testDataSetName, String category) {
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);

        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period).withBillingType(type).withPlan(planvalue).update().saveChanges();
    }

    public void createOrderCustomer (String customerName, String testDataSetName, String category) {

        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String product = TestData.read("PageCustomers.xml", testDataSetName, "product", category);
        String effectiveDate = TestData.read("PageCustomers.xml", testDataSetName, "effectiveDate", category);
        String order = TestData.read("PageCustomers.xml", testDataSetName, "order", category);
        String secondProduct = TestData.read("PageCustomers.xml", testDataSetName, "secondProduct", category);
        String activeDate = TestData.read("PageCustomers.xml", testDataSetName, "activeDate", category);

        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period)
                .withBillingType(order)
                .withActiveSince(activeDate)
                .withProduct(product)
                .withEffectiveDate(effectiveDate);
        explicitWaitForJavascript();
        orderBuilderPage.withDependentProduct("Lemonade Plan Setup Fee");
        explicitWaitForJavascript();
        orderBuilderPage.withOrderPeriod(period).withBillingType(order).withActiveSince(activeDate).saveChanges();

        ordersPage.editFirstOrder(customerName);

        orderBuilderPage.withProduct(secondProduct).withEffectiveDate(effectiveDate).update().saveChanges();
    }

    public void createOrderBundleAndWithoutBundle (String testDataSetName, String category,
            String firstPlanDescription, String planDescription2) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);

        System.out.println("startDate:"+startDate);
        System.out.println("type:"+type);
        System.out.println("period:"+period);
        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period)
                .withBillingType(type)
                .withActiveSince(startDate)
                .withPlan(firstPlanDescription)
                .remove()
                .withPlan(planDescription2)
                .update()
                .saveChanges();

    }

    private BigDecimal calculatedDiscount (String testDataSetName, String category) {
        String discountRate = TestData.read("PageCustomers.xml", testDataSetName, "DiscountRate", category);
        String productRate = TestData.read("PageCustomers.xml", testDataSetName, "productRate", category);
        BigDecimal percentageRate1 = new BigDecimal(discountRate);
        BigDecimal productRate1 = new BigDecimal(productRate);
        return percentageRate1.multiply(productRate1).divide(BigDecimal.valueOf(-100));
    }

    public void createOrderWithDiscount (String testDataSetName, String category, String productDescription,
            String discountName) {
        String startDate = TestData.read("PageCustomers.xml", testDataSetName, "startDate", category);
        String endDate = TestData.read("PageCustomers.xml", testDataSetName, "endDate", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);
        String effectiveDate = TestData.read("PageCustomers.xml", testDataSetName, "effectiveDate", category);
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String discountableItem = TestData.read("PageCustomers.xml", testDataSetName, "discountableItem", category);

        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period)
                .withBillingType(type)
                .withActiveSince(startDate)
                .withActiveUntil(endDate)
                .withProduct(productDescription)
                .withDiscount(discountableItem, discountName)
                .withEffectiveDate(effectiveDate)
                .update()
                .saveChanges();
    }

    public void createOrderWithProduct (String product) {
        String startDate = TestData.read("PageCustomers.xml", "TC_126_GenerateInvoice", "startDate", "gi");

        clickCreateOrder();

        orderBuilderPage.withActiveSince(startDate)
                .withProduct(product)
                .withEffectiveDate(startDate)
                .update()
                .saveChanges();
    }

    public void createOrderProductHavingDependency (String testDataSetName, String category) {
        String period = TestData.read("PageCustomers.xml", testDataSetName, "period", category);
        String type = TestData.read("PageCustomers.xml", testDataSetName, "type", category);
        String product2 = TestData.read("PageCustomers.xml", testDataSetName, "product2", category);
        String effectiveDate = TestData.read("PageCustomers.xml", testDataSetName, "effectiveDate", category);

        clickCreateOrder();

        orderBuilderPage.withOrderPeriod(period)
                .withBillingType(type)
                .withProduct(product2)
                .withEffectiveDate(effectiveDate)
                .update()
                .saveChanges();
    }

    public void generateInvoice (String testDataSetName, String category) {
        createOrderForInvoice(testDataSetName, category);
        linkGenerateInvoice.click();
        Assert.assertTrue(messagesPage.isIntermediateSuccessMessageAppeared());
    }

    public void verifyCurrencyPresentsInList (String currency) {
        clickAdd();
        formSelectAccountType.linkSelectAccountType.click();
        Assert.assertTrue(formUserEdit.selectCurrency.hasOption(currency));
    }

    private void selectAccountType (String accountType) {
        formSelectAccountType.setAccountType(accountType);
    }


    public void clickMakePayment () {
        linkMakePayment.click();
    }

    public void validateUsersSavedTestData (String data) {
        validateTextPresentInTable(data, tableCustomers);
    }

    // TODO: 360logica nonsense, ha ?
    private void verifyItemInMonthlyPostpaidOrder () {
        Assert.assertTrue(isElementPresent(By.xpath("(//table[@class='innerTable'])[1]")));
        Assert.assertTrue(isElementPresent(By.xpath("(//table[@class='innerTable'])[2]")));
    }
}
