package com.jbilling.test.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.Button;
import ru.yandex.qatools.htmlelements.element.CheckBox;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage;
import com.jbilling.framework.globals.GlobalEnumsPage.PaymentMethodField;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.CreditCard;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;
import com.jbilling.test.ui.parts.DivPaymentFields;
import ru.yandex.qatools.htmlelements.element.Table;

public class PaymentsPage extends AppPage {

    @FindBy(name = "payment.amountAsDecimal")
    private TextInput        inputPaymentAmount;

    @FindBy(id = "payment-method-fields-0")
    private DivPaymentFields divPaymentFields;

    @FindBy(id = "paymentMethod_0.paymentMethodTypeId")
    private Select           selectPaymentMethodType;

    @FindBy(id = "paymentMethod_0.processingOrder")
    private TextInput        inputProcessingOrder;

    @FindBy(xpath = "//div[@class='buttons']/ul/li/a[contains(@class, 'submit') and contains(@class, 'payment')]/span[text()='Review Payment']/..")
    private Link             linkReviewPayment;

    @FindBy(xpath = "//div[@class='buttons']/ul/li/a[contains(@class, 'submit') and contains(@class, 'payment')]/span[text()='Make Payment']/..")
    private Link             linkMakePayment;

    @FindBy(id = "submitToPaymentGateway")
    private CheckBox         checkboxProcessPaymentNow;

    @FindBy(id = "templateId")
    private Select           selectPaymentTemplate;

    @FindBy(xpath = "//div[@class = 'buttons']//a[contains(@onclick,'submit')]")
    private Link             linkSelect;

    @FindBy(id = "methodName")
    private TextInput        inputMethodName;

    @FindBy(id = "allAccount")
    private CheckBox         checkboxAllAccountsTypes;

    @FindBy(id = "isRecurring")
    private CheckBox         checkboxIsRecurring;

    @FindBy(xpath = "//span[text()='Save Changes']")
    private Button           buttonSaveChanges;

    @FindBy(id = "payments")
    private Table            tablePayments;

    public PaymentsPage(WebDriver driver) {
        super(driver);
    }

    public void verifyErrorOnInvalidCredentials (String customerName, String amount) {

        inputPaymentAmount.setText(amount);
        linkReviewPayment.click();

        Assert.assertTrue(isElementPresent(By.xpath("//div[@id='messages']/div/ul/li")));
    }

    public void makePayment (String testDataSetName, String category, String paymentMethodType) {
        String chequeHolderName = TestData.read("PaymentsPage.xml", testDataSetName, "chequeHolderName", category);
        String chequeNumber = TestData.read("PaymentsPage.xml", testDataSetName, "chequeNumber", category);
        String chequeDate = TestData.read("PaymentsPage.xml", testDataSetName, "chequeDate", category);
        String paymentAmount = TestData.read("PaymentsPage.xml", testDataSetName, "paymentAmount", category);
        String paymentOrder = TestData.read("PaymentsPage.xml", testDataSetName, "PaymentOrder", category);

        explicitWaitForJavascript();
        inputPaymentAmount.setText(paymentAmount);

        CreditCard creditCard = new CreditCard.DefaultCreditCard(chequeHolderName, chequeNumber, chequeDate);
        selectPaymentMethodType.selectByVisibleText(paymentMethodType);
        divPaymentFields.setCardData(creditCard);

        inputProcessingOrder.setText(paymentOrder);

//        reviewAndMakePayment();
        linkReviewPayment.click();
        divPaymentFields.assertEquals(creditCard);
        linkMakePayment.click();

        Assert.assertTrue(isElementPresent(By.xpath("//div[@class = 'msg-box successfully']")));
    }

    public String addPaymentMethodWithoutMetaFields (PaymentMethodField paymentMethodFields, String testDataSetName,
            String category) {
        String paymentTemplate = TestData.read("PaymentsPage.xml", testDataSetName, "paymentTemplate", category);
        String methodName = TestData.read("PaymentsPage.xml", testDataSetName, "methodName", category);
        boolean allAccountTypes = TestData.read("PaymentsPage.xml", testDataSetName, "allAccountTypes", category)
                .equals("true");
        boolean isRecurring = TestData.read("PaymentsPage.xml", testDataSetName, "isRecurring", category)
                .equals("true");

        clickAdd();
        selectPaymentTemplate.selectByVisibleText(paymentTemplate);
        linkSelect.click();
        inputMethodName.setText(methodName);

        switch (paymentMethodFields) {
        case ALL:
            checkboxAllAccountsTypes.set(allAccountTypes);
            checkboxIsRecurring.set(isRecurring);
            break;

        case REECURRING:
            checkboxIsRecurring.set(isRecurring);
            break;

        case ALL_ACCOUNTS:
            checkboxAllAccountsTypes.set(allAccountTypes);
            break;
        default:
            throw new RuntimeException("Invalid Step Provided. Not defined in enumeration");
        }
        explicitWaitForJavascript(30);
        buttonSaveChanges.click();

        return methodName;
    }

    public void makePayment (GlobalEnumsPage.MakePayment payment, String testDataSetName, String category,
            String paymentMethodType) {
        String chequeHolderName = TestData.read("PaymentsPage.xml", testDataSetName, "chequeHolderName", category);
        String chequeNumber = TestData.read("PaymentsPage.xml", testDataSetName, "chequeNumber", category);
        String chequeDate = TestData.read("PaymentsPage.xml", testDataSetName, "chequeDate", category);
        String paymentAmount = TestData.read("PaymentsPage.xml", testDataSetName, "paymentAmount", category);
        String paymentOrder = TestData.read("PaymentsPage.xml", testDataSetName, "paymentOrder", category);
        boolean realTime = TextUtilities.compareValue(
                TestData.read("PaymentsPage.xml", testDataSetName, "realTime", category), "true", true,
                TextComparators.equals);

        CreditCard creditCard = new CreditCard.DefaultCreditCard(chequeHolderName, chequeNumber, chequeDate);

        if (payment != GlobalEnumsPage.MakePayment.UNCHANGEDPAYEMENT) {
            inputPaymentAmount.setText(paymentAmount);
        }
        selectPaymentMethodType.selectByVisibleText(paymentMethodType);
        divPaymentFields.setCardData(creditCard);

        inputProcessingOrder.setText(paymentOrder);

        if (payment == GlobalEnumsPage.MakePayment.PAYEMENT_CARD_REAL_TIME) {
            checkboxProcessPaymentNow.set(realTime);
        }
        reviewAndMakePayment();
    }

    public void validateSavedTestDataInPaymentsTable (String data) {
        validateTextPresentInTable(data, tablePayments);
    }

    private void reviewAndMakePayment () {
        linkReviewPayment.click();
        linkMakePayment.click();
    }
}
