package com.jbilling.test.ui;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.jbilling.test.ui.pages.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import ru.stqa.selenium.factory.WebDriverPool;

import com.jbilling.framework.globals.GlobalConsts;
import com.jbilling.framework.globals.GlobalController;
import com.jbilling.framework.interfaces.IBrowser;
import com.jbilling.framework.interfaces.impl.selenium.BrowserSelenium;
import com.jbilling.framework.testrails.TestRailsListener;
import com.jbilling.framework.utilities.textutilities.TextUtilities;

/**
 * <p>
 * To switch between 2 modes, apply correspondent annotations to this class.
 * </p>
 * <p>
 * 1. Single browser per test suite.
 * </p>
 * <p>
 * 2. Single browser per test class.
 * </p>
 */

@Test(groups = { "automation" })
@Listeners({ TestRailsListener.class })
@ContextConfiguration(classes = { AopConfiguration.class, BrowserTest.Config.class })
@BrowserTest.BrowserPerTestClass
public class BrowserTest extends AbstractTestNGSpringContextTests {

    @Target(value = ElementType.TYPE)
    @Retention(value = RetentionPolicy.RUNTIME)
    @Documented
    @TestExecutionListeners(inheritListeners = false, listeners = {
            DirtiesContextBeforeModesTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
            DirtiesContextTestExecutionListener.class })
    @DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
    public @interface BrowserPerTestClass {
    }

    @Target(value = ElementType.TYPE)
    @Retention(value = RetentionPolicy.RUNTIME)
    @Documented
    @TestExecutionListeners(inheritListeners = false, listeners = { DependencyInjectionTestExecutionListener.class })
    public @interface BrowserPerTestSuite {
    }

    public enum BrowsersTypes {
        InternetExplorer {

            @Override
            DesiredCapabilities buildDesiredCapabilities () {
                DesiredCapabilities result = DesiredCapabilities.internetExplorer();
                result.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                result.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
                result.setCapability(InternetExplorerDriver.IE_ENSURE_CLEAN_SESSION, true);
                // System.setProperty("webdriver.ie.driver",
                // "./resources/brwDrivers/IEDriverServer.exe");
                return result;
            }
        },
        Chrome {

            @Override
            DesiredCapabilities buildDesiredCapabilities () {
                DesiredCapabilities result = DesiredCapabilities.chrome();
                result.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                System.setProperty("webdriver.chrome.driver", "/usr/bin/google-chrome");
                return result;
            }
        },
        Firefox {

            @Override
            DesiredCapabilities buildDesiredCapabilities () {
                DesiredCapabilities result = DesiredCapabilities.firefox();
                result.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
                return result;
            }
        };

        abstract DesiredCapabilities buildDesiredCapabilities ();
    };

    @Configuration
    @EnableAspectJAutoProxy
    @PropertySource("classpath:config.properties")
    static class Config {

        protected final Log logger = LogFactory.getLog(getClass()); ;

        @Autowired
        private Environment env;

        @Bean
        public String baseUrl () {
            String environmentName = env.getProperty("EnvironmentUnderTest");
            String url = env.getProperty("jbillingUrl", env.getProperty(environmentName + "_URL"));
            System.out.println("baseUrl = "+url);
            logger.warn("baseUrl = "+url);
            return url;
        }

        @Bean
        public Credentials credentials () {
            String environmentName = env.getProperty("EnvironmentUnderTest");
            // @formatter:off
            return new Credentials.DefaultCredentials(
                    env.getProperty("loginId", env.getProperty(environmentName + "_Username")),
                    env.getProperty("password", env.getProperty(environmentName + "_Password")),
                    env.getProperty("companyId", env.getProperty(environmentName + "_CompanyID")));
            // @formatter:on
        }

        @Bean
        public BrowsersTypes browserType () {
            return BrowsersTypes.Firefox;
        }

        @Bean(destroyMethod = "")
        public WebDriver webDriver (BrowsersTypes browserType) {
            DesiredCapabilities desiredCapabilities = browserType.buildDesiredCapabilities();
            WebDriver result = WebDriverPool.DEFAULT.getDriver(desiredCapabilities);
            setupDriverOptions(result);
            return result;
        }

        private static final long   DEFAULT_TIMEOUT_IMPLICIT_WAIT = 30L;
        private static final long   DEFAULT_TIMEOUT_PAGE_LOAD     = 30L;
        private static final long   DEFAULT_TIMEOUT_SCRIPT_EXEC   = 30L;

        // http://stackoverflow.com/questions/31860117/how-use-waits-in-html-elements-framework
        private static final String IMPLICIT_WAIT_PROPERTY_NAME   = "webdriver.timeouts.implicitWait";
        private static final String PAGE_LOAD_PROPERTY_NAME       = "webdriver.timeouts.pageLoad";
        private static final String SCRIPT_EXEC_PROPERTY_NAME     = "webdriver.timeouts.scriptTimeout";

        @Bean
        public long implicitlyWaitSeconds () {
            if (!env.containsProperty(IMPLICIT_WAIT_PROPERTY_NAME)) {
                System.setProperty(IMPLICIT_WAIT_PROPERTY_NAME, Long.toString(DEFAULT_TIMEOUT_IMPLICIT_WAIT));
            }
            return env.getProperty(IMPLICIT_WAIT_PROPERTY_NAME, Long.class, DEFAULT_TIMEOUT_IMPLICIT_WAIT);
        }

        @Bean
        public long pageLoadSeconds () {
            return env.getProperty(PAGE_LOAD_PROPERTY_NAME, Long.class, DEFAULT_TIMEOUT_PAGE_LOAD);
        }

        @Bean
        public long scriptTimeoutSeconds () {
            return env.getProperty(SCRIPT_EXEC_PROPERTY_NAME, Long.class, DEFAULT_TIMEOUT_SCRIPT_EXEC);
        }

        private void setupDriverOptions (WebDriver driver) {
            Options options = driver.manage();

            options.deleteAllCookies();
            options.window().maximize();
            options.window().setSize(new org.openqa.selenium.Dimension(1366, 768));

            Timeouts timeouts = options.timeouts();
            timeouts.pageLoadTimeout(pageLoadSeconds(), TimeUnit.SECONDS);
            timeouts.setScriptTimeout(scriptTimeoutSeconds(), TimeUnit.SECONDS);
            timeouts.implicitlyWait(implicitlyWaitSeconds(), TimeUnit.SECONDS);
            GlobalConsts.IMPLICIT_TIME_LIMIT = implicitlyWaitSeconds();
        }

        @Bean
        public IBrowser browser (WebDriver driver) {
            logger.debug("WebDriver driver = " + driver);
            return GlobalController.brw = new BrowserSelenium(driver);
        }

        @Bean
        public LoginPage loginPage (WebDriver driver) {
            return new LoginPage(driver);
        }

        @Bean
        public NavigationPage navigationPage (WebDriver driver) {
            return new NavigationPage(driver);
        }

        @Bean
        public ConfigurationPage configurationPage (WebDriver driver) {
            return new ConfigurationPage(driver);
        }

        @Bean
        public ProductsPage productsPage (WebDriver driver) {
            return new ProductsPage(driver);
        }

        @Bean
        public MessagesPage messagesPage(WebDriver driver) {
            return new MessagesPage(driver);
        }

        @Bean
        public AccountTypePage accountTypePagePage (WebDriver driver) {
            return new AccountTypePage(driver);
        }

        @Bean
        public DiscountsPage discountPage (WebDriver driver) {
            return new DiscountsPage(driver);
        }

        @Bean
        public FiltersPage filtersPage (WebDriver driver) {
            return new FiltersPage(driver);
        }

        @Bean
        public AgentsPage agentsPage (WebDriver driver) {
            return new AgentsPage(driver);
        }

        @Bean
        public InvoicePage invoicePage (WebDriver driver) {
            return new InvoicePage(driver);
        }

        @Bean
        public PaymentsPage paymentsPage (WebDriver driver) {
            return new PaymentsPage(driver);
        }

        @Bean
        public OrdersPage ordersPage (WebDriver driver) {
            return new OrdersPage(driver);
        }

        @Bean
        public PlansPage plansPage (WebDriver driver) {
            return new PlansPage(driver);
        }

        @Bean
        public CustomersPage customersPage (WebDriver driver) {
            return new CustomersPage(driver);
        }

        @Bean
        public ReportsPage reportsPage (WebDriver driver) {
            return new ReportsPage(driver);
        }

        @Bean
        public OrderBuilderPage orderBuilderPage (WebDriver driver) {
            return new OrderBuilderPage(driver);
        }
    }

    @Autowired
    private WebDriver         driver;
    @Autowired
    private IBrowser          browser;

    @Autowired
    protected String          baseUrl;

    @Autowired
    protected Credentials     credentials;

    @Autowired
    protected LoginPage       loginPage;
    @Autowired
    protected NavigationPage  navPage;
    @Autowired
    protected ConfigurationPage confPage;
    @Autowired
    protected ProductsPage productsPage;
    @Autowired
    protected MessagesPage    msgsPage;
    @Autowired
    protected AccountTypePage accountTypePage;
    @Autowired
    protected DiscountsPage   discountsPage;
    @Autowired
    protected FiltersPage     filtersPage;
    @Autowired
    protected AgentsPage      agentsPage;
    @Autowired
    protected InvoicePage     invoicePage;
    @Autowired
    protected PaymentsPage    paymentsPage;
    @Autowired
    protected OrdersPage      ordersPage;
    @Autowired
    protected PlansPage       plansPage;
    @Autowired
    protected CustomersPage   newCustomersPage;
    @Autowired
    protected ReportsPage     reportsPage;

    @AfterSuite(alwaysRun = true)
    public void dismissBrowser () {
        WebDriverPool.DEFAULT.dismissAll();
    }

    @AfterClass(alwaysRun = true)
    public void quitBrowserIfNeeded () {
        if (BrowserTest.class.isAnnotationPresent(BrowserPerTestClass.class)) {
            WebDriverPool.DEFAULT.dismissDriver(driver);
        }
        if (BrowserTest.class.isAnnotationPresent(BrowserPerTestSuite.class)) {
            navPage.logoutApplication();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeTestMethod (Method method, ITestContext context) {
        Reporter.log("<br> Test Begins");
        logger.info("Starting  test: " + method.getName());
    }

    @AfterMethod(alwaysRun = true)
    public void afterTestMethod (Method method, ITestContext context) {
        logger.info("Finishing test: " + method.getName());
        Reporter.log("<br> Test Passed");
    }

    protected String appendRandomChars (String inputString) {
        return inputString + TextUtilities.getRandomString(5);
    }

    protected String appendRandomDigits (String inputString) {
        return inputString + TextUtilities.getRandomNumber(5);
    }

    protected ITestResult result;

    protected void setTestRailsId (final String tcid) {
        result = Reporter.getCurrentTestResult();
        result.setAttribute("tcid", tcid);
    }
}
