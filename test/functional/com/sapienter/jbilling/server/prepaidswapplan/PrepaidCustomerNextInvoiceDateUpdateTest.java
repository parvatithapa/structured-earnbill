package com.sapienter.jbilling.server.prepaidswapplan;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.Assert.assertEquals;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;

/**
 * 
 * @author Dipak Kardel
 * Date:14-March-2018
 *
 */

@Test(groups = { "prepaid-swapPlan" }, testName = "PrepaidCustomerNextInvoiceDateUpdateTest")
public class PrepaidCustomerNextInvoiceDateUpdateTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    private String testAccount = "Account Type";
    private Integer accTypeId;
    private final static Integer CC_PM_ID = 5;
    private Integer nextInvoiceDay = 15;
    private static final  int MONTHLY_ORDER_PERIOD = 2;
    private static final String USER_01 = "Test-PP-User-NID-1";
    private static final String USER_02 = "Test-PP-User-NID-2";
    private static final String USER_03 = "Test-PP-User-NID-3";

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            // Creating account type
            accTypeId = buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);
        });
    }

    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    @Test
    public void test001NextInvoiceDateTestScenarioA1(){
        testBuilder.getTestEnvironment();
        Calendar nextInvoiceDate = Calendar.getInstance();
        int day = 1;
        try {
            testBuilder.given(envBuilder -> {
                nextInvoiceDate.add(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, day);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                nextInvoiceDay = day;
                //creating user with billing cycle monthly
                buildAndPersistCustomer(envBuilder, api, USER_01, accTypeId, null, MONTHLY_ORDER_PERIOD, nextInvoiceDay);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS userWS = api.getUserWS(envBuilder.idForCode(USER_01));
                assertNotNull("User creation Failed",  envBuilder.idForCode(USER_01));
                logger.debug("Next invoice date: {}", parseDate(userWS.getNextInvoiceDate()));
                if(Calendar.getInstance().get(Calendar.DATE) < day){
                    nextInvoiceDate.add(Calendar.MONTH, -1);
                }
                assertEquals(parseDate(userWS.getNextInvoiceDate()),parseDate(nextInvoiceDate.getTime()));

            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_01));
        }
    }

    @Test
    public void test002NextInvoiceDateTestScenarioA2(){
        testBuilder.getTestEnvironment();
        final Calendar nextInvoiceDate = Calendar.getInstance();
        int day = 15;
        try {
            testBuilder.given(envBuilder -> {
                nextInvoiceDate.setTime(new Date());
                nextInvoiceDate.add(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, day);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                nextInvoiceDay = day;
                //creating user with billing cycle monthly
                buildAndPersistCustomer(envBuilder, api, USER_02, accTypeId, null, MONTHLY_ORDER_PERIOD, nextInvoiceDay);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS userWS = api.getUserWS(envBuilder.idForCode(USER_02));
                assertNotNull("User creation Failed",  envBuilder.idForCode(USER_02));
                logger.debug("Next invoice date: {}", parseDate(userWS.getNextInvoiceDate()));
                if(Calendar.getInstance().get(Calendar.DATE) < day){
                    nextInvoiceDate.add(Calendar.MONTH, -1);
                }
                assertEquals(parseDate(userWS.getNextInvoiceDate()),parseDate(nextInvoiceDate.getTime()));

            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_02));
        }
    }

    @Test
    public void test003NextInvoiceDateTestScenarioA3(){
        testBuilder.getTestEnvironment();
        final Calendar nextInvoiceDate = Calendar.getInstance();
        int day = 28;
        try {
            testBuilder.given(envBuilder -> {
                nextInvoiceDate.setTime(new Date());
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, day);
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                nextInvoiceDay = day;
                //creating user with billing cycle monthly
                buildAndPersistCustomer(envBuilder, api, USER_03, accTypeId, null, MONTHLY_ORDER_PERIOD, nextInvoiceDay);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS userWS = api.getUserWS(envBuilder.idForCode(USER_03));
                assertNotNull("User creation Failed",  envBuilder.idForCode(USER_03));
                if(Calendar.getInstance().get(Calendar.DATE) < day){
                    nextInvoiceDate.add(Calendar.MONTH, -1);
                }
                logger.debug("Next invoice date: {}", parseDate(userWS.getNextInvoiceDate()));
                assertEquals(parseDate(userWS.getNextInvoiceDate()),parseDate(nextInvoiceDate.getTime()));

            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_03));
        }
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator ->
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi()));
    }

    public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }
    public Integer buildAndPersistCustomer(TestEnvironmentBuilder envBuilder, JbillingAPI api, String username,
            Integer accountTypeId, Date nextInvoiceDate, Integer periodId, Integer nextInvoiceDay) {

        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(username)
                .withAccountTypeId(accountTypeId)
                .addTimeToUsername(false)
                .withNextInvoiceDate(nextInvoiceDate)
                .withMainSubscription(new MainSubscriptionWS(periodId, nextInvoiceDay))
                .build();
        return userWS.getId();
    }

    private String parseDate(Date date) {
    	return date==null ? null : new SimpleDateFormat("MM/dd/yyyy").format(date);
    }

}
