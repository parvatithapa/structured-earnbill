package com.jbilling.test.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.qatools.htmlelements.element.Button;

import com.jbilling.framework.utilities.xmlutils.TestData;
import com.jbilling.test.ui.elements.Select;
import com.jbilling.test.ui.elements.TextInput;

public class AgentsPage extends AppPage {

    @FindBy(id = "user.userName")
    private TextInput inputLoginName;

    @FindBy(id = "contact.email")
    private TextInput inputEmail;

    @FindBy(id = "type")
    private Select    selectAgentType;

    @FindBy(id = "commissionType")
    private Select    selectComissionType;

    @FindBy(xpath = "//div[@class = 'buttons']/ul/li/a")
    private Button    buttonSaveChanges;

    public AgentsPage(WebDriver driver) {
        super(driver);
    }

    public String addAgent (String testDataSetName, String category) {
        String login = TestData.read("PageAgents.xml", testDataSetName, "login", category);
        String email = TestData.read("PageAgents.xml", testDataSetName, "email", category);
        String agent = TestData.read("PageAgents.xml", testDataSetName, "agent", category);
        String commission = TestData.read("PageAgents.xml", testDataSetName, "commission", category);

        clickAdd();
        inputLoginName.setText(login);
        inputEmail.setText(email);
        selectAgentType.selectByVisibleText(agent);
        selectComissionType.selectByVisibleText(commission);
        buttonSaveChanges.click();

        return agent;
    }
}
