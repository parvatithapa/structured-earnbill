package com.sapienter.jbilling.server.prepaidswapplan;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

/**
 * 
 * @author Pranay Raherkar
 * Date:14-March-2018
 *
 */
@Test(groups = { "prepaid-swapPlan" }, testName = "PrepaidCustomerUsagepoolCreationAndEvaluationTest")
public class PrepaidCustomerUsagepoolCreationAndEvaluationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String USER_01 = "Test-PP-FUP-Eval-User-1";
    private static final String ORDER_01 = "subscriptionOrder_01";
    private static final String USER_02 = "Test-PP-FUP-Eval-User-2";
    private static final String ORDER_02 = "subscriptionOrder_02";
    private static final String USER_03 = "Test-PP-FUP-Eval-User-3";
    private static final String ORDER_03 = "subscriptionOrder_03";
    private static final String USER_04 = "Test-PP-FUP-Eval-User-4";
    private static final String ORDER_04 = "subscriptionOrder_04";
    private static final String SUBSCRIPTION_PROD_01 = "testPlanSubscriptionItemForCUP_01";
    private static final String USAGE_POOL_01 = "UP with 100 Quantity For CUP"+System.currentTimeMillis();
    private static final String PLAN_01 = "100 free minute Plan For CUP";
    private static final String USAGEPOOL_LOGGER_MSG = "Cycle start date: {} Cycle end date: {}";
    private static final String ORDER_ASSERT = "Order creation Failed";
    private static final String USER_ASSERT = "User creation Failed";
    private static final Integer CC_PM_ID = 5;
    private static final Integer NEXT_INVOICE_DAY = 1;
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private static final int INBOUND_USAGE_PRODUCT_ID = 320101;
    private static final int CHAT_USAGE_PRODUCT_ID = 320102;
    private static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static Integer customerUsagePoolEvalutionPluginId;
    private String testCat1 = "MediatedUsageCategoryForCUP";
    private BillingProcessConfigurationWS oldBillingProcessConfig;
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    private String testAccount = "Account Type";
    private Integer accTypeId;

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            //To configure Customer usagepool evalution task we need to set prorate type as "Manual".
            oldBillingProcessConfig = api.getBillingProcessConfiguration();
            BillingProcessConfigurationWS newBillingProcessConfig = api.getBillingProcessConfiguration();
            newBillingProcessConfig.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());
            api.createUpdateBillingProcessConfiguration(newBillingProcessConfig);

            //Configure Customer usagepool evalution task.
            PluggableTaskWS customerUsagePoolEvaluationTask= new PluggableTaskWS();
            customerUsagePoolEvaluationTask.setProcessingOrder(523);
            customerUsagePoolEvaluationTask.setTypeId(118);
            customerUsagePoolEvaluationTask.setNotes("customerUsagePoolEvaluationTask");
            customerUsagePoolEvalutionPluginId = api.createPlugin(customerUsagePoolEvaluationTask);

            accTypeId = buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
            //Creating usage products
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_01, false, envBuilder.idForCode(testCat1), "99", true);
            //Usage product item ids 
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2018);
            pricingDate.set(Calendar.MONTH, 0);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);
            PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());
            PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());
            PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "1.50", pricingDate.getTime());

            //Creating usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder,api,USAGE_POOL_01, "100", envBuilder.idForCode(testCat1), items);

            //Creating 100 min plan
            buildAndPersistPlan(envBuilder,api, PLAN_01, "100 Free Minutes Plan", MONTHLY_ORDER_PERIOD, 
                        envBuilder.idForCode(SUBSCRIPTION_PROD_01), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),planItemProd01WS, planItemProd02WS, planItemProd03WS);
            
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("Usage pool Creation Failed", testEnvBuilder.idForCode(USAGE_POOL_01));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_01));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(PLAN_01));
        });
    }

    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        api.createUpdateBillingProcessConfiguration(oldBillingProcessConfig);
        api.deletePlugin(customerUsagePoolEvalutionPluginId);
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        envHelper = null;
        testBuilder = null;
    }

    /**
     * Customer Usage Pool Creation with Pre-Paid order
     * 
     */
    @Test
    public void test001CustomerUsagePoolCreationWithPrePaid(){

        try {
                testBuilder.given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Calendar nextInvoiceDate = Calendar.getInstance();
                    nextInvoiceDate.set(Calendar.YEAR, 2018);
                    nextInvoiceDate.set(Calendar.MONTH, 0);
                    nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                    //Create User with NID 1st Jan 2018
                    buildAndPersistCustomer(envBuilder, api, USER_01, accTypeId, nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY);
                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.put(envBuilder.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                    //Create Order with Active since 1st Jan 2018
                    buildAndPersistOrder(envBuilder, api, ORDER_01, envBuilder.idForCode(USER_01), nextInvoiceDate.getTime(), null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_PRE_PAID,
                            true, productQuantityMap);
                }).validate((testEnv, envBuilder) -> {
                    assertNotNull(USER_ASSERT, envBuilder.idForCode(USER_01));
                    assertNotNull(ORDER_ASSERT,  envBuilder.idForCode(ORDER_01));
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    UserWS user = api.getUserWS(envBuilder.idForCode(USER_01));

                    Calendar cycleStartdate = Calendar.getInstance();
                    cycleStartdate.set(Calendar.YEAR, 2018);
                    cycleStartdate.set(Calendar.MONTH, 0);
                    cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                    Calendar cycleEnddate = Calendar.getInstance();
                    cycleEnddate.set(Calendar.YEAR, 2018);
                    cycleEnddate.set(Calendar.MONTH, 0);
                    cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                    CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                    //Check CUSTOMER USAGE POOLS cycle start date, cycle end date and usage quantity.
                    logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                    assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                    assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                    assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("100.00"));

                    //create one time usage order with active since date 10 Jan 2018
                    cycleStartdate.set(Calendar.DAY_OF_MONTH, 10);
                    try {
                        Integer oneTimeOrderId = createOneTimeOrder(envBuilder.idForCode(USER_01), cycleStartdate.getTime(), "200");
                        OrderWS oneTimeOrder =  api.getOrder(oneTimeOrderId);
                        assertEquals(oneTimeOrder.getOrderLines()[0].getAmountAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("150.00"));
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(USER_01), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_01));
        }
    }


    /**
     * Customer Usage Pool Creation with Pre-Paid order.
     * 
     */
    @Test
    public void test002CustomerUsagePoolCreationwithPrepaidAndInvoiceGeneration(){

        try {
                testBuilder.given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Calendar nextInvoiceDate = Calendar.getInstance();
                    nextInvoiceDate.set(Calendar.YEAR, 2018);
                    nextInvoiceDate.set(Calendar.MONTH, 0);
                    nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                    //Create User with NID 1st Jan 2018
                    buildAndPersistCustomer(envBuilder, api, USER_02, accTypeId, nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY);
                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.put(envBuilder.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                    //Create Order with Active since 1st Jan 2018
                    buildAndPersistOrder(envBuilder, api, ORDER_02, envBuilder.idForCode(USER_02), nextInvoiceDate.getTime(), null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_PRE_PAID,
                            true, productQuantityMap);
                }).validate((testEnv, envBuilder) -> {
                    assertNotNull(USER_ASSERT, envBuilder.idForCode(USER_02));
                    assertNotNull(ORDER_ASSERT,  envBuilder.idForCode(ORDER_02));
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    UserWS user = api.getUserWS(envBuilder.idForCode(USER_02));

                    Calendar cycleStartdate = Calendar.getInstance();
                    cycleStartdate.set(Calendar.YEAR, 2018);
                    cycleStartdate.set(Calendar.MONTH, 0);
                    cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                    api.createInvoiceWithDate(user.getId(), cycleStartdate.getTime(), PeriodUnitDTO.MONTH, 21, false);
                    assertNotNull("Invoice creation Failed", api.getLatestInvoice(user.getId()));
                    Calendar cycleEnddate = Calendar.getInstance();
                    cycleEnddate.set(Calendar.YEAR, 2018);
                    cycleEnddate.set(Calendar.MONTH, 0);
                    cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);
                    
                    CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                    //Check CUSTOMER USAGE POOLS cycle start date, cycle end date and usage quantity.
                    logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                    assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                    assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                    assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("100.00"));
                });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(USER_02), 10, 0))
                 .forEach(invoice -> api.deleteInvoice(invoice.getId()));
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_02));
        }
    }


    /**
     * Customer Usage Pool Creation with post-Paid order.
     * 
     */
    @Test
    public void test003CustomerUsagePoolCreationWithPostPaidAndInvoiceGeneration(){

        try {
                testBuilder.given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Calendar nextInvoiceDate = Calendar.getInstance();
                    nextInvoiceDate.set(Calendar.YEAR, 2018);
                    nextInvoiceDate.set(Calendar.MONTH, 0);
                    nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                    //Create User with NID 1st Jan 2018
                    buildAndPersistCustomer(envBuilder, api, USER_03, accTypeId, nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY);
                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.put(envBuilder.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                    //Create Order with Active since 1st Jan 2018
                    buildAndPersistOrder(envBuilder, api, ORDER_03, envBuilder.idForCode(USER_03), nextInvoiceDate.getTime(), null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_POST_PAID,
                            true, productQuantityMap);
                }).validate((testEnv, envBuilder) -> {
                    assertNotNull(USER_ASSERT, envBuilder.idForCode(USER_03));
                    assertNotNull(ORDER_ASSERT,  envBuilder.idForCode(ORDER_03));
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    UserWS user = api.getUserWS(envBuilder.idForCode(USER_03));

                    Calendar cycleStartdate = Calendar.getInstance();
                    cycleStartdate.set(Calendar.YEAR, 2018);
                    cycleStartdate.set(Calendar.MONTH, 0);
                    cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                    api.createInvoiceWithDate(user.getId(), cycleStartdate.getTime(), PeriodUnitDTO.MONTH, 21, false);
                    if(null != api.getLatestInvoice(user.getId())){
                        //Here invoice should no be generated for this user.
                        assertEquals(1,2);
                    }

                    Calendar cycleEnddate = Calendar.getInstance();
                    cycleEnddate.set(Calendar.YEAR, 2018);
                    cycleEnddate.set(Calendar.MONTH, 0);
                    cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                    CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                    //Check CUSTOMER USAGE POOLS cycle start date, cycle end date and usage quantity.
                    assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                    assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                    assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("100.00"));

                    cycleStartdate.set(Calendar.MONTH, 1);
                    cycleEnddate.set(Calendar.MONTH, 1);
                    cycleEnddate.set(Calendar.DAY_OF_MONTH, 28);

                    Calendar invoiceDate = Calendar.getInstance();
                    invoiceDate.set(Calendar.YEAR, 2018);
                    invoiceDate.set(Calendar.MONTH, 1);
                    invoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                    //Update customer's NID to 1st Feb 2018
                    user.setNextInvoiceDate(invoiceDate.getTime());
                    api.updateUser(user);
                    user = api.getUserWS(user.getId());
                    //Create invoice for post paid order on date - 1st Feb 2018
                    api.createInvoiceWithDate(user.getId(), invoiceDate.getTime(), PeriodUnitDTO.MONTH, 21, false);

                    //Update customer's NID to 1st March 2018
                    invoiceDate.set(Calendar.MONTH, 2);
                    user.setNextInvoiceDate(invoiceDate.getTime());
                    api.updateUser(user);
                    user = api.getUserWS(user.getId());

                    //trigger customerUsagePoolEvalutionPlugin for updating cycle start date, cycle end date
                    api.triggerScheduledTask(customerUsagePoolEvalutionPluginId , new Date());
                    try {
                        Thread.sleep(30000);
                    } catch (Exception e) {
                	logger.error(e.getMessage());
                    }
                    fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                    //Check customer usage pool cycle start date, cycle end date and usage quantity.
                    logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                    assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                    assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                    assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("100.00"));

                });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(USER_03), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_03));
        }
    }


    /**
     * Customer Usage Pool Creation with post-Paid order.
     * 
     */
    @Test
    public void test004CustomerUsagePoolCreationWithPostPaidwithActiveUntilDate(){

        try {
                testBuilder.given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();

                    Calendar nextInvoiceDate = Calendar.getInstance();
                    nextInvoiceDate.set(Calendar.YEAR, 2018);
                    nextInvoiceDate.set(Calendar.MONTH, 0);
                    nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                    //Create User with NID 1st Jan 2018
                    buildAndPersistCustomer(envBuilder, api, USER_04, accTypeId, nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY);
                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.put(envBuilder.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);
                    //Create Order with Active since 1st Jan 2018
                    buildAndPersistOrder(envBuilder, api, ORDER_04, envBuilder.idForCode(USER_04), nextInvoiceDate.getTime(), null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_POST_PAID,
                            true, productQuantityMap);
                }).validate((testEnv, envBuilder) -> {
                    assertNotNull(USER_ASSERT, envBuilder.idForCode(USER_04));
                    assertNotNull(ORDER_ASSERT,  envBuilder.idForCode(ORDER_04));
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    UserWS user = api.getUserWS(envBuilder.idForCode(USER_04));

                    Calendar cycleStartdate = Calendar.getInstance();
                    cycleStartdate.set(Calendar.YEAR, 2018);
                    cycleStartdate.set(Calendar.MONTH, 0);
                    cycleStartdate.set(Calendar.DAY_OF_MONTH, 1);

                    api.createInvoiceWithDate(user.getId(), cycleStartdate.getTime(), PeriodUnitDTO.MONTH, 21, false);
                    if(null != api.getLatestInvoice(user.getId())){
                        //Here invoice should no be generated for this user.
                        assertEquals(1,2);
                    }

                    Calendar cycleEnddate = Calendar.getInstance();
                    cycleEnddate.set(Calendar.YEAR, 2018);
                    cycleEnddate.set(Calendar.MONTH, 0);
                    cycleEnddate.set(Calendar.DAY_OF_MONTH, 31);

                    CustomerUsagePoolWS[] fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                    //Check CUSTOMER USAGE POOLS cycle start date, cycle end date and usage quantity.
                    logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                    assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                    assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                    assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("100.00"));

                    Calendar activeUntil = Calendar.getInstance();
                    activeUntil.set(Calendar.YEAR, 2018);
                    activeUntil.set(Calendar.MONTH, 0);
                    activeUntil.set(Calendar.DAY_OF_MONTH, 24);

                    OrderWS order = api.getOrder(envBuilder.idForCode(ORDER_04));
                    order.setActiveUntil(activeUntil.getTime());
                    api.updateOrder(order, null);
                    cycleEnddate.set(Calendar.DAY_OF_MONTH, 24);
                    fups = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
                    //Check customer usage pool cycle start date, cycle end date and usage quantity.
                    logger.debug(USAGEPOOL_LOGGER_MSG, parseDate(fups[0].getCycleStartDate()),parseDate(fups[0].getCycleEndDate()));
                    assertEquals(parseDate(fups[0].getCycleStartDate()),parseDate(cycleStartdate.getTime()));
                    assertEquals(parseDate(fups[0].getCycleEndDate()), parseDate(cycleEnddate.getTime()));
                    assertEquals(fups[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP), new BigDecimal("77.42"));

                });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(USER_04), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_04));
        }
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi()));
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
        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);
        return userWS.getId();
    }

    public Integer buildAndPersistOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, Integer userId,
            Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId,
            boolean prorate, Map<Integer, BigDecimal> productQuantityMap) {

        OrderBuilder orderBuilder = envBuilder.orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withEffectiveDate(activeSince)
                .withPeriod(orderPeriodId)
                .withBillingTypeId(billingTypeId)
                .withProrate(prorate);

        for (Map.Entry<Integer, BigDecimal> entry : productQuantityMap.entrySet()) {
            orderBuilder.withOrderLine(
                   orderBuilder.orderLine()
                          .withItemId(entry.getKey())
                          .withQuantity(entry.getValue())
                          .build());
        }
        return orderBuilder.build();
    }

    public Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                 .itemType()
                 .withCode(code)
                 .withCategoryType(categoryType)
                 .global(global)
                 .build();
    }

    public Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
        boolean global, Integer categoryId, String flatPrice, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                 .item()
                 .withCode(code)
                 .withType(categoryId)
                 .withFlatPrice(flatPrice)
                 .global(global)
                 .allowDecimal(allowDecimal)
                 .build();
    }

    private PlanItemWS buildPlanItem(JbillingAPI api, Integer itemId, Integer periodId, String quantity, String price, Date pricingDate) {
        return PlanBuilder.PlanItemBuilder.getBuilder()
                 .withItemId(itemId)
                 .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                 .addModel(pricingDate, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                 .withBundledPeriodId(periodId)
                 .withBundledQuantity(quantity)
                 .build();
    }

    private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String quantity,Integer categoryId, List<Integer>  items) {
        return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code)
                .withQuantity(quantity)
                .withResetValue("Reset To Initial Value")
                .withItemIds(items)
                .addItemTypeId(categoryId)
                .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                .withCyclePeriodValue(1).withName(code)
                .build();
    }

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
        Integer periodId, Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    public static Integer createOneTimeOrder(Integer userId, Date activeSinceDate, String inboundProductQuantity) throws JbillingAPIException, IOException {

        JbillingAPI api = JbillingAPIFactory.getAPI();

        logger.debug("Creating One time usage order...");
        OrderWS oTOrder = new OrderWS();
        oTOrder.setUserId(userId);
        oTOrder.setActiveSince(activeSinceDate);
        oTOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        oTOrder.setPeriod(1); // Onetime
        oTOrder.setCurrencyId(1);

        OrderLineWS oTline1 = new OrderLineWS();
        oTline1.setItemId(TestConstants.INBOUND_USAGE_PRODUCT_ID);
        oTline1.setDescription("Inbound");
        oTline1.setQuantity(inboundProductQuantity);
        oTline1.setTypeId(1);
        oTline1.setPrice("0.00");
        oTline1.setAmount("0.00");
        oTline1.setUseItem(true);

        oTOrder.setOrderLines(new OrderLineWS[]{oTline1});
        Integer oneTimeOrderId = api.createOrder(oTOrder, OrderChangeBL.buildFromOrder(oTOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        logger.debug("Created one time usage order with Id: {}", oneTimeOrderId);
        assertNotNull("one time usage order creation failed", oneTimeOrderId);

        return oneTimeOrderId;
    }
 
    private String parseDate(Date date) {
        if(date == null)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }
}
