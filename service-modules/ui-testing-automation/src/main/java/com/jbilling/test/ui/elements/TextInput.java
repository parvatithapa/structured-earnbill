package com.jbilling.test.ui.elements;

import org.openqa.selenium.WebElement;

public class TextInput extends ru.yandex.qatools.htmlelements.element.TextInput {

    public TextInput(WebElement wrappedElement) {
        super(wrappedElement);
    }

    public void setText (String value) {
        sendKeys(getClearCharSequence());
        sendKeys(value);
    }
}
