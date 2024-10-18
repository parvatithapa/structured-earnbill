package com.jbilling.test.ui.pages;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.jbilling.test.ui.elements.Link;
import com.jbilling.test.ui.elements.TextInput;

public class FiltersPage extends WebPage {

    @FindBy(id = "filters.ORDER-LIKE_U_userName.stringValue")
    private TextInput        inputLoginName;

    @FindBy(xpath = "//div[@id='filters']/div[@class='btn-hold']/a[contains(@class, 'apply')]")
    private Link             linkApplyFilters;

    @FindBy(xpath = "//div[@id='filters']//div[@class='dropdown']/a[contains(@class, 'add')]")
    Link                     linkAddFilters;

    @FindBy(xpath = "//div[@id='filters']//div[@class='dropdown active']/div[@class='drop']/ul/li")
    private List<WebElement> listAddFiltersOptions;

    public FiltersPage(WebDriver driver) {
        super(driver);
    }

    public void filterOnLoginNameOrCustomerName (String customerName) {
        linkAddFilters.click();
        selectListElementByText(listAddFiltersOptions, "Login Name");
        inputLoginName.setText(customerName);
        linkApplyFilters.click();
    }
}
