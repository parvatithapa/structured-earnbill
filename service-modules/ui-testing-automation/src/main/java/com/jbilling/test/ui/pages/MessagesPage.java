package com.jbilling.test.ui.pages;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.utilities.textutilities.TextUtilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;
import ru.yandex.qatools.htmlelements.element.TextBlock;

public class MessagesPage extends AppPage {

	@FindBy(xpath = "//div[@id='messages']/div/p")
	private static TextBlock textOperationMessage;

	@FindBy(xpath = "//div[@class = 'msg-box successfully']")
	private static TextBlock textIntermediateSuccessMessage;

	@FindBy(xpath = "//div[@id='messages']/div[@class='msg-box error']/strong")
	private static TextBlock textStrongErrorMessage;

    @FindBy(xpath = "//div[@id='messages']/div[@class='msg-box error']/ul")
    private static TextBlock textErrorMessageList;

    @FindBy(xpath = "(//div[@id='messages']/div[@class='msg-box error']/ul/li)[1]")
    private static TextBlock textErrorMessagesListFirstMsg;

	@FindBy(xpath = "//div[@id='flash-info']/ul/li")
	private static TextBlock textOperationFlashInfo;
	
    public MessagesPage(WebDriver driver) {
        super(driver);
    }

	public static String isOperationSuccessfulOnMessage(String messageToVerify, String additionalMessage, TextComparators comparator) {
		String msg = isOperationSuccessfulOnMessage(messageToVerify, comparator);
		String msg2 = isOperationSuccessfulOnMessage(additionalMessage, comparator);

		if (msg == null) {
			if (msg2 == null) {
				return null;
			}
			return msg2;
		}

		return msg;
	}

	public static String isOperationSuccessfulOnMessage(String messageToVerify, TextComparators comparator) {
		String msg = textOperationMessage.exists() ? textOperationMessage.getText() : textOperationFlashInfo.getText();

		boolean result = TextUtilities.compareValue(messageToVerify, msg, true, comparator);

		return (result ? null : msg);
	}

	public static boolean isIntermediateSuccessMessageAppeared() {
		return textIntermediateSuccessMessage.exists();
	}

	public static boolean isErrorMessageAppeared() {
		return textStrongErrorMessage.exists();
	}

    public static boolean isErrorMessagesListAppeared() {
        return textErrorMessageList.exists();
    }

	public static void verifyDisplayedMessageText(String messageToVerify, String additionalMessage, TextComparators comparator) {

		String rsltMsg = isOperationSuccessfulOnMessage(messageToVerify, additionalMessage, TextComparators.contains);
		if (rsltMsg != null) {
			throw new RuntimeException("Test Case failed: " + rsltMsg);
		}
	}

    public static void assertTextInFirstErrorMessage (String expectedText) {
        String actualText = textErrorMessageList.getText();
        Assert.assertEquals(actualText, expectedText);
    }

    public static void assertTextInFirstErrorMessage (String expectedText, TextComparators comparator) {
        String actualText = textErrorMessagesListFirstMsg.getText();

        actualText = TextUtilities.nullToBlank(actualText, true);
        if(!TextUtilities.compareValue(expectedText, actualText, true, comparator)) {
            Assert.fail("Expected '"+expectedText+"' found '"+actualText+"'");
        }
    }
}
