package com.jbilling.test.ui.elements;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.jbilling.test.ui.pages.WebPage;

public class Link extends ru.yandex.qatools.htmlelements.element.Link {

    protected final Log logger = LogFactory.getLog(getClass());

    public Link(WebElement wrappedElement) {
        super(wrappedElement);
    }

    private static final long DEFAULT_WAIT = 5L;

    @Override
    public void click () {
        WebDriver driver = WebPage.DriverUtils.asWebDriver(this);
        String logThis = toString();
        logger.trace("Enter: click on " + logThis);
        WebPage.DriverUtils.explicitWaitForJavascript(driver, DEFAULT_WAIT);
        WebPage.DriverUtils.waitUntilClickable(this, DEFAULT_WAIT);
        try {
            new Actions(driver).moveToElement(getWrappedElement()).perform();
            super.click();
            logger.info("  Clicked on " + logThis);
        } catch (WebDriverException e) { // MoveTargetOutOfBoundsException |
                                         // Element is not clickable at point
            logger.error("Exception while clicking on " + logThis + ". failback to javascript clicking.", e);
            WebPage.DriverUtils.executeScript(driver, "arguments[0].click();", getWrappedElement());
        }
        WebPage.DriverUtils.explicitWaitForJavascript(driver, DEFAULT_WAIT);
    }

    @Override
    public String toString () {
        String result = super.toString();
        if (result == null) {
            try {
                result = getText();
            } catch (StaleElementReferenceException e) {
                result = "<a>???</a>";
            }
        }
        return result;
    }
}
