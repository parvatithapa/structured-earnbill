package com.jbilling.test.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.qatools.htmlelements.element.HtmlElement;

import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;

public class AccountTypePage extends AppPage {

    static public class AccountTypeForm extends HtmlElement {

        @FindBy(id = "description")
        private TextInput inputDescription;

        @FindBy(id = "mainSubscription.nextInvoiceDayOfPeriod")
        private TextInput inputNextInvoiceDayOfPeriod;

        @FindBy(id = "invoiceDesign")
        private TextInput inputInvoiceDesign;

        @FindBy(id = "creditLimitAsDecimal")
        private TextInput inputCreditLimitAsDecimal;

        @FindBy(id = "creditNotificationLimit1AsDecimal")
        private TextInput inputCreditNotificationLimit1AsDecimal;

        @FindBy(id = "creditNotificationLimit2AsDecimal")
        private TextInput inputCreditNotificationLimit2AsDecimal;

        @FindBy(id = "payment-method-select")
        private Select    selectPaymentMethod;
    }

    @FindBy(name = "account-type-config-form")
    private AccountTypeForm formAccountType;

    public AccountTypePage(WebDriver driver) {
        super(driver);
    }

    public String createAccountTypeWithCreditDetails (String testDataSetName, String category,
            String paymentMethodNameOne) {

        String accountName = TestData.read("AccountType.xml", testDataSetName, "accountName", category);
        String billingCycle = TestData.read("AccountType.xml", testDataSetName, "billingCycle", category);
        String invoiceDesign = TestData.read("AccountType.xml", testDataSetName, "invoiceDesign", category);
        String creditLimit = TestData.read("AccountType.xml", testDataSetName, "creditLimit", category);
        String creditLimitOne = TestData.read("AccountType.xml", testDataSetName, "creditLimitOne", category);
        String creditLimitTwo = TestData.read("AccountType.xml", testDataSetName, "creditLimitTwo", category);

        clickAdd();

        formAccountType.inputDescription.setText(accountName);
        formAccountType.inputNextInvoiceDayOfPeriod.setText(billingCycle);
        formAccountType.inputInvoiceDesign.setText(invoiceDesign);
        formAccountType.inputCreditLimitAsDecimal.setText(creditLimit);
        formAccountType.inputCreditNotificationLimit1AsDecimal.setText(creditLimitOne);
        formAccountType.inputCreditNotificationLimit2AsDecimal.setText(creditLimitTwo);
        formAccountType.selectPaymentMethod.selectByVisibleText(paymentMethodNameOne);

        clickSave();

        return accountName;
    }
}
