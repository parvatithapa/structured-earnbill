package com.jbilling.framework.interfaces;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * IBrowser interface is a parent interface of all the classes/interfaces in
 * Automation tool independence layer. This interface has all methods
 * declarations which could be valid for any automation tool. E.g. setText,
 * selectText, etc. All automation tool related objects would be created using
 * IBrowser interface only.
 * 
 * @author Aishwarya Dwivedi
 * @since 1.0
 * 
 * @version 1.0
 */
public interface IBrowser {
	<T> T initElements(Class<T> pageClassToProxy) throws IllegalArgumentException, IllegalAccessException;

	void check(ElementField ef);

    void uncheck(ElementField ef);

	void clearTextBox(ElementField ef);

	void click(ElementField ef);

	void clickLinkText(ElementField ef);

	void clickLinkText(ElementField ef, boolean attemptJsIfFail);

	void clickTableCellWithText(ElementField efTable, String text);

	String getAttribute(ElementField ef, String attributeName);

	String getText(ElementField ef);

    void setText(ElementField ef, String text);

    public boolean isElementLoaded(ElementField ef);

	boolean isElementPresent(ElementField element);

	boolean isElementPresentNoWait(ElementField ef);

	void pressEnter(ElementField ef);

	void pressTab(ElementField ef);

    void pressControlKey(ElementField ef);

	void selectDropDown(ElementField ef, String targetValue);

    void deSelectDropDown(ElementField ef, String targetValue);

	boolean isValuePresentInDropDown(ElementField ef, String targetValue);

    void getDropDownOptionIsSelected(ElementField ef, String targetValue);

    String getDropDownSelectedValue(ElementField ef);

    void selectListItem(ElementField ef, String targetValue);

    boolean isDataPresentInTable(ElementField efTable, String text);

    void selectDataInTable (ElementField ef1, ElementField ef2, String text);

    void validateSavedTestData(ElementField efTable, String text);

    void verifyUIComponent(ElementField ef);

    WebDriver getCurrentWebDriver();
    
    By getByLocator(ElementField ef);

	void takeScreenShot(String methodName);

    void waitForElementLoad(ElementField element, long timeoutInSeconds);

	void waitForElement(ElementField element, long timeoutInSeconds);

	boolean waitUntilElementStopMoving (ElementField ef);

	void waitForAjaxElement (ElementField ef);
}
