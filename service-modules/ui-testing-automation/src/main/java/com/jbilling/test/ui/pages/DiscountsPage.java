package com.jbilling.test.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.qatools.htmlelements.element.Button;
import ru.yandex.qatools.htmlelements.element.CheckBox;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;

public class DiscountsPage extends AppPage {

    @FindBy(id = "jqgh_data_grid_column1_description")
    Link              linkDiscountDescription;

    @FindBy(id = "jqgh_data_grid_column1_type")
    Link              linkDiscountType;

    @FindBy(id = "discount.code")
    private TextInput inputDiscountCode;

    @FindBy(xpath = "//div[@id='descriptions']/div/div/input")
    private TextInput inputAddDescription;

    @FindBy(id = "discount.rate")
    private TextInput inputDiscountRate;

    @FindBy(xpath = "//div[@id='addDescription']/div/div/select")
    private Select    selectAddDescription;

    @FindBy(xpath = "//div[@id='addDescription']/div/a")
    private Button    buttonAddDescription;

    @FindBy(xpath = "//label[text()='Discount Type']/../div/select")
    private Select    selectDiscountType;

    @FindBy(id = "discount.attribute.2.value")
    private TextInput inputDiscountPeriod;

    @FindBy(id = "discount.startDate")
    private TextInput inputDiscountStartDate;

    @FindBy(id = "discount.endDate")
    private TextInput inputDiscountEndDate;

    @FindBy(id = "discount.attribute.3.value")
    private CheckBox  checkboxDiscountPercentage;

    public DiscountsPage(WebDriver driver) {
        super(driver);
    }

    public String createNewDiscount (String testDataSetName, String category) {

        String discountCode = TestData.read("PageDiscounts.xml", testDataSetName, "discountCode", category);
        String descriptionLanguage = TestData.read("PageDiscounts.xml", testDataSetName, "descriptionLanguage",
                category);
        String description = TestData.read("PageDiscounts.xml", testDataSetName, "description", category);
        String discountType = TestData.read("PageDiscounts.xml", testDataSetName, "discountType", category);
        String discountRate = TestData.read("PageDiscounts.xml", testDataSetName, "discountRate", category);

        inputDiscountCode.setText(discountCode);
        selectAddDescription.selectByVisibleText(descriptionLanguage);
        buttonAddDescription.click();
        inputAddDescription.setText(description);
        selectDiscountType.selectByVisibleText(discountType);
        inputDiscountRate.setText(discountRate);
        clickSave();

        return discountCode + " - " + description;
    }

    public String createNewDiscountWithPercentage (String testDataSetName, String category) {

        String discountCode = TestData.read("PageDiscounts.xml", testDataSetName, "discountCode", category);
        String descriptionLanguage = TestData.read("PageDiscounts.xml", testDataSetName, "descriptionLanguage",
                category);
        String description = TestData.read("PageDiscounts.xml", testDataSetName, "description", category);
        String discountType = TestData.read("PageDiscounts.xml", testDataSetName, "discountType", category);
        String discountRate = TestData.read("PageDiscounts.xml", testDataSetName, "discountRate", category);
        String periodValue = TestData.read("PageDiscounts.xml", testDataSetName, "periodValue", category);
        String startDate = TestData.read("PageDiscounts.xml", testDataSetName, "startDate", category);
        String endDate = TestData.read("PageDiscounts.xml", testDataSetName, "endDate", category);
        boolean isPercentage = TextUtilities.compareValue(
                TestData.read("PageDiscounts.xml", testDataSetName, "isPercentage", category), "true", true,
                TextComparators.equals);

        clickAdd();
        inputDiscountCode.setText(discountCode);
        selectAddDescription.selectByVisibleText(descriptionLanguage);
        buttonAddDescription.click();
        inputAddDescription.setText(description);
        selectDiscountType.selectByVisibleText(discountType);
        inputDiscountRate.setText(discountRate);
        inputDiscountPeriod.setText(periodValue);
        inputDiscountStartDate.setText(startDate);
        inputDiscountEndDate.setText(endDate);
        checkboxDiscountPercentage.set(isPercentage);
        clickSave();

        return discountCode + " - " + description;
    }

    public void verifyDiscountTable () {
        isElementPresent(By.id("gs_code"));
        isElementPresent(By.xpath("//div[@class='ui-jqgrid-sortable']/span[@class='s-ico']/span[1]"));
        isElementPresent(By.xpath("//div[@class='ui-jqgrid-sortable']/span[@class='s-ico']/span[2]"));
        linkDiscountDescription.click();
        linkDiscountType.click();
        isChildElementPresent(linkDiscountType.getWrappedElement(), By.xpath("./span[@class='s-ico']/span[1]"));
        isChildElementPresent(linkDiscountType.getWrappedElement(), By.xpath("./span[@class='s-ico']/span[2]"));
    }

    public void isValidationErrorAppeared () {
        MessagesPage.isErrorMessageAppeared();
    }

    public void isDiscountCreatedSuccessfully () {
        String msg = MessagesPage.isOperationSuccessfulOnMessage("Discount", "created successfully",
                TextComparators.contains);
        if (msg != null) {
            throw new RuntimeException(msg);
        }
    }
}
