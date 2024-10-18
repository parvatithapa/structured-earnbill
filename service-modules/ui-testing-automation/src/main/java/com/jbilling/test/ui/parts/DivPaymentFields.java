package com.jbilling.test.ui.parts;

import org.openqa.selenium.By;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.HtmlElement;

import com.jbilling.test.ui.ClearingData;
import com.jbilling.test.ui.CreditCard;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;

public class DivPaymentFields extends HtmlElement {

    public void setCardData (CreditCard cardData) {
        TextInput inputCardHolder = new TextInput(findElement(By.xpath("(.//input[@type='text'])[1]")));
        TextInput inputCardNumber = new TextInput(findElement(By.xpath("(.//input[@type='text'])[2]")));
        TextInput inputCardExpiryDate = new TextInput(findElement(By.xpath("(.//input[@type='text'])[3]")));

        inputCardHolder.setText(cardData.getHolder());
        inputCardNumber.setText(cardData.getNumber());
        inputCardExpiryDate.setText(cardData.getExpiryDate());
    }

    public void assertEquals (CreditCard cardData) {
        TextInput inputCardHolder = new TextInput(findElement(By.xpath("(.//input[@type='text'])[1]")));
        TextInput inputCardNumber = new TextInput(findElement(By.xpath("(.//input[@type='text'])[2]")));
        TextInput inputCardExpiryDate = new TextInput(findElement(By.xpath("(.//input[@type='text'])[3]")));

        Assert.assertEquals(inputCardHolder.getText(), cardData.getHolder());
        if(inputCardNumber.getText().length() > 4 && cardData.getNumber().length() > 4) {
            Assert.assertEquals(inputCardNumber.getText().substring(inputCardNumber.getText().length()-4), cardData.getNumber().substring(cardData.getNumber().length()-4));
        }
        String[] expected = inputCardExpiryDate.getText().split("/");
        String[] data = cardData.getExpiryDate().split("/");
        Assert.assertEquals(expected.length, data.length);
        for(int i=0; i<expected.length; i++) {
            Assert.assertEquals(expected[i].replaceFirst("^0*", ""), data[i].replaceFirst("^0*", ""));
        }
    }

    public void setClearingData (ClearingData achData) {
        TextInput inputRoutingNumber = new TextInput(findElement(By.xpath("(.//input[@type='text'])[1]")));
        TextInput inputCustomerName = new TextInput(findElement(By.xpath("(.//input[@type='text'])[2]")));
        TextInput inputAccountNumber = new TextInput(findElement(By.xpath("(.//input[@type='text'])[3]")));
        TextInput inputBankName = new TextInput(findElement(By.xpath("(.//input[@type='text'])[4]")));
        Select selectAccountType = new Select(findElement(By.xpath("(.//select)[1]")));

        inputRoutingNumber.setText(achData.getRoutingNumber());
        inputCustomerName.setText(achData.getCustomerName());
        inputAccountNumber.setText(achData.getAccountNumber());
        inputBankName.setText(achData.getBankName());
        selectAccountType.selectByVisibleText(achData.getAccountType());
    }
}
