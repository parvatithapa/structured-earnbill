package com.jbilling.test.ui.pages;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import com.jbilling.test.ui.Credentials;
import com.jbilling.test.ui.elements.Form;
import com.jbilling.test.ui.elements.Link;

public class LoginPage extends WebPage {

    @FindBy(id = "login-form")
    private Form formLogin;

    @FindBy(id = "submitLink")
    private Link linkLogin;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void login (String jbillingUrl, Credentials credentials) {

        navigateToUrl(jbillingUrl);

        formLogin.fill(loginFormData(credentials));

        linkLogin.click();
    }

    private Map<String, Object> loginFormData (Credentials credentials) {
        Map<String, Object> data = new HashMap<>();
        data.put("j_username", credentials.getLoginId());
        data.put("j_password", credentials.getPassword());
        data.put("j_client_id", credentials.getCompanyId());
        return data;
    }
}
