package com.jbilling.framework.utilities.browserutils;

import org.testng.annotations.BeforeClass;

import com.jbilling.framework.utilities.xmlutils.ConfigPropertiesReader;
import com.jbilling.framework.utilities.xmlutils.PropertyFileReaderUpdater;
import com.jbilling.test.ui.BrowserTest;

/**
 * Class to initialize all application page objects and manage WebDriver browser object. Each and every test script
 * class must extend this.
 * 
 * @author Aishwarya Dwivedi
 * @since 1.0
 * 
 * @version 1.0
 */
public class BrowserApp extends BrowserTest {

    protected ConfigPropertiesReader    pr         = new ConfigPropertiesReader();
    protected PropertyFileReaderUpdater propReader = new PropertyFileReaderUpdater();

//    protected ConfigurationPage         confPage;
//    protected ProductsPage              productsPage;
//    protected MessagesPage              msgsPage;

    @BeforeClass
    public void initPageObjectsAndLogin () throws IllegalArgumentException, IllegalAccessException {
        System.out.println("initPageObjectsAndLogin "+baseUrl);
//        confPage = GlobalController.brw.initElements(ConfigurationPage.class);
//        productsPage = GlobalController.brw.initElements(ProductsPage.class);
//        msgsPage = GlobalController.brw.initElements(MessagesPage.class);

        loginPage.login(baseUrl, credentials);
    }
}
