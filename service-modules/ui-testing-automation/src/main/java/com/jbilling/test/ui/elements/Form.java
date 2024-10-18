package com.jbilling.test.ui.elements;

import org.openqa.selenium.WebElement;

public class Form extends ru.yandex.qatools.htmlelements.element.Form {

    public Form(WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    protected void fillSelect (WebElement element, String value) {
        new Select(element).selectByVisibleText(value);
    }

    public void fillSelectByVisibleText (String selectName, String value) {
        fillSelect(findElementByKey(selectName), value);
    }
}
