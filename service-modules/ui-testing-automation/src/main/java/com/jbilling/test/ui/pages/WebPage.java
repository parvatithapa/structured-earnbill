package com.jbilling.test.ui.pages;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testng.Assert;

import ru.yandex.qatools.htmlelements.element.Table;
import ru.yandex.qatools.htmlelements.element.TextBlock;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;
import ru.yandex.qatools.htmlelements.loader.HtmlElementLoader;

public class WebPage {

    /**
     * Logger available to subclasses
     */
    protected final Log       logger = LogFactory.getLog(getClass());

    protected final WebDriver driver;

    @Autowired
    @Qualifier("implicitlyWaitSeconds")
    private long              implicitlyWaitSeconds;

    public static class DriverUtils {

        public static final By ThisLocator = By.xpath(".");

        // @formatter:off
        private static final ExpectedCondition<Boolean> isJQueryLoaded = new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply (WebDriver theDriver) {
                try {
                    return ((Long) ((JavascriptExecutor) theDriver).executeScript("return jQuery.active") == 0);
                } catch (Exception e) {
                    return true;
                }
            }
        };
        private static final ExpectedCondition<Boolean> isJavascriptLoaded = new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply (WebDriver theDriver) {
                Object readyState = ((JavascriptExecutor) theDriver).executeScript("return document.readyState");
                return "complete".equals(readyState) || "loaded".equals(readyState);
            }
        };
        // @formatter:on

        private DriverUtils() {
        }

        public static WebDriver asWebDriver (WebElement element) {
            WrapsDriver wrapsDriver = (WrapsDriver) element;
            return wrapsDriver.getWrappedDriver();
        }

        public static WebDriverWait getWait (WebElement element, long timeOutInSeconds) {
            return new WebDriverWait(asWebDriver(element), timeOutInSeconds);
        }

        public static WebElement waitUntilClickable (WebElement element, long timeOutInSeconds) {
            return getWait(element, timeOutInSeconds).until(ExpectedConditions.elementToBeClickable(element));
        }

        public static WebElement asWebElement (TypifiedElement element) {
            return element.findElement(ThisLocator);
        }

        public static WebDriver asWebDriver (TypifiedElement element) {
            return asWebDriver(asWebElement(element));
        }

        public static WebDriverWait getWait (TypifiedElement element, long timeOutInSeconds) {
            return new WebDriverWait(asWebDriver(element), timeOutInSeconds);
        }

        public static WebElement waitUntilClickable (TypifiedElement element, long timeOutInSeconds) {
            return waitUntilClickable(asWebElement(element), timeOutInSeconds);
        }

        public static Object executeScript (WebDriver driver, String script, Object... args) {
            return ((JavascriptExecutor) driver).executeScript(script, args);
        }

        public static Object executeScript (TypifiedElement element, String script, Object... args) {
            return ((JavascriptExecutor) asWebDriver(element)).executeScript(script, args);
        }

        public static boolean explicitWaitForJavascript (WebDriver driver, long timeOutInSeconds) {
            WebDriverWait explicitWait = new WebDriverWait(driver, timeOutInSeconds);
            return explicitWait.until(isJQueryLoaded) && explicitWait.until(isJavascriptLoaded);
        }

        public static boolean explicitWaitForJavascript (TypifiedElement element, long timeOutInSeconds) {
            return explicitWaitForJavascript(asWebDriver(element), timeOutInSeconds);
        }
    }

    public WebPage(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver);
        HtmlElementLoader.populatePageObject(this, driver);
    }

    protected void navigateToUrl (String url) {
        driver.get(url);
    }

    protected boolean isElementPresent (By locator) {
        driver.manage().timeouts().implicitlyWait(0l, TimeUnit.SECONDS);
        try {
            return driver.findElements(locator).size() > 0;
        } finally {
            driver.manage().timeouts().implicitlyWait(implicitlyWaitSeconds, TimeUnit.SECONDS);
        }
    }

    protected boolean isChildElementPresent (WebElement parent, By locator) {
        driver.manage().timeouts().implicitlyWait(0l, TimeUnit.SECONDS);
        try {
            return parent.findElements(locator).size() > 0;
        } finally {
            driver.manage().timeouts().implicitlyWait(implicitlyWaitSeconds, TimeUnit.SECONDS);
        }
    }

    private WebElement getListElementByText (List<WebElement> elements, String text) {
        String target = text.trim();
        for (WebElement element : elements) {
            if (target.equals(element.getText().trim())) {
                return element;
            }
        }
        throw new RuntimeException("No list item found with value '" + text + "'");
    }

    protected void selectListElementByText (List<WebElement> elements, String text) {
        getListElementByText(elements, text).click();
    }

    protected boolean isElementInList (List<WebElement> elements, String text) {
        String target = text.trim();
        for (WebElement element : elements) {
            if (target.equals(element.getText().trim())) {
                return true;
            }
        }
        return false;
    }

    protected BigDecimal asDecimal (TextBlock text) {
        return new BigDecimal(text.getText().replaceAll("[^\\d.,]+", "")).setScale(3);
    }

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    protected String formatCurrentDate () {
        return dateFormatter.format(LocalDate.now());
    }

    protected void validateTextPresentInTable (String text, Table table) {
        Assert.assertTrue(table.isDisplayed());
        Assert.assertTrue(table.isEnabled());

        List<WebElement> cells = table.findElements(By.tagName("td"));
        for (WebElement cell : cells) {
            if (cell.getText().contains(text)) {
                break;
            } else {
                List<WebElement> elementsWithText = cell.findElements(By.xpath(".//*[contains(text(), '" + text + "')]"));
                if (!elementsWithText.isEmpty()) {
                    Assert.assertTrue(elementsWithText.get(0).isDisplayed());
                    break;
                }
            }
        }
    }

    protected boolean isTextPresentInTable (String text, Table table) {
        Assert.assertTrue(table.isDisplayed());
        Assert.assertTrue(table.isEnabled());

        List<WebElement> cells = table.findElements(By.tagName("td"));
        for (WebElement cell : cells) {
            if (cell.getText().contains(text)) {
                return true;
            } else {
                List<WebElement> elementsWithText = cell.findElements(By.xpath(".//*[contains(text(), '" + text + "')]"));
                if (!elementsWithText.isEmpty()) {
                    Assert.assertTrue(elementsWithText.get(0).isDisplayed());
                    break;
                }
            }
        }
        return false;
    }

    protected boolean explicitWaitForJavascript (long timeOutInSeconds) {
        return DriverUtils.explicitWaitForJavascript(driver, timeOutInSeconds);
    }

    protected boolean explicitWaitForJavascript () {
        return explicitWaitForJavascript(implicitlyWaitSeconds);
    }
}
