package com.jbilling.test.ui.elements;

import com.jbilling.framework.globals.GlobalEnumerations;
import com.jbilling.framework.interfaces.ElementField;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import org.openqa.selenium.WebElement;

import com.jbilling.test.ui.pages.WebPage;
import org.testng.Assert;

import java.util.List;

public class Select extends ru.yandex.qatools.htmlelements.element.Select {

    private static final long DEFAULT_WAIT = 5L;

    public Select(WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public void selectByVisibleText (String value) {
        super.selectByVisibleText(value);
        WebPage.DriverUtils.explicitWaitForJavascript(this, DEFAULT_WAIT);
    }

    public void isDropDownOptionSelected(String targetValue) {
        String cells = "";
        int i;
        List<WebElement> options = getAllSelectedOptions();
        int optionSize = options.size();
        for (i = 0; i <= optionSize; i++) {
            cells = options.get(i).getText();

            if (TextUtilities.compareValue(cells, targetValue, true, GlobalEnumerations.TextComparators.equals)) {
                Assert.assertTrue(true);
                break;
            } else if (i == optionSize) {
                Assert.assertTrue(TextUtilities.compareValue(cells, targetValue, true, GlobalEnumerations.TextComparators.equals), "Given Target Value"
                        + targetValue + " Is Not Default Selected");
            }
        }
    }

    public String getFirstSelectedValue() {
        return getFirstSelectedOption().getText();
    }

    public boolean hasOption (String optionText) {
        for (WebElement option : getOptions()) {
            if (option.getText().toUpperCase().contains(optionText.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
