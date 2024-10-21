package com.sapienter.jbilling.fc;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;

import static org.testng.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.billing.task.GenerateCancellationInvoiceTask;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.OrderChangeBasedCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.OrderLineBasedCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.cache.MatchType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS;
import com.sapienter.jbilling.server.usagePool.task.CustomerUsagePoolConsumptionActionTask;
import com.sapienter.jbilling.server.usagePool.task.FreeTrialConsumptionTask;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

/**
 * This is Test class for Free Trial Plan
 * JBFC-880
 * 
 * @author swapnil
 *
 */
@Test(groups = { "fullcreative" }, testName = "FreeTrialConsumptionTest")
public class FreeTrialConsumptionTest {

    private Logger logger = LoggerFactory.getLogger(FreeTrialConsumptionTest.class);
    private TestBuilder testBuilder;
    private TestEnvironmentBuilder testEnvironmentBuilder;
    private static final String  CUSTOMER_CODE_1                                = "swapnil"+System.currentTimeMillis();
    private static final String  CUSTOMER_CODE_2                                = "TEST_2_"+System.currentTimeMillis();
    private static final String  CUSTOMER_CODE_3                                = "TEST_3_"+System.currentTimeMillis();
    private static final String  CUSTOMER_CODE_4                                = "TEST_4_"+System.currentTimeMillis();
    private static final String  CUSTOMER_CODE_5                                = "TEST_5_"+System.currentTimeMillis();
    private static final String  CUSTOMER_CODE_6                                = "TEST_6_"+System.currentTimeMillis();
    private static final String  WEEKLY_ORDER                                   = "WeeklyTestFreeTrialOrder";
    private static final String  NON_FREE_WEEKLY_ORDER                          = "WeeklyTestFreeTrialOrder";
    private static final int     ORDER_CHANGE_STATUS_APPLY_ID                   = 3;
    private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID          = 320104;
    private static final String  QUANTITY                                       = "0";
    private static final String  MEDIATED_USAGE_CATEGORY                        = "Full Creative Mediation Usage Category";
    private static final String  CALL_RATE_CARD_ID                              = "12";
    private static final  String  INBOUND_MEDIATION_LAUNCHER                    = "inboundCallsMediationJobLauncher";

    private static final String  WEEKLY_PLAN_CODE                               = "WEEKLY 100 free minute Plan";
    private static final String  WEEKLY_PLAN_CODE2                              = "WEEKLY 100 free minute Plan2";
    private static final String  WEEKLY_PLAN_ITEM_CODE                          = "WEEKLY Free-Trial-Test-Item_01";
    private static final String  WEEKLY_PLAN_ITEM_CODE2                         = "WEEKLY Free-Trial-Test-Item_012";

    private static final String  PRICE                                          = "0";
    private static final String  USAGE_POOL_01                                  = "UP with 100 Quantity"+System.currentTimeMillis();
    public static final int      INBOUND_USAGE_PRODUCT_ID                       = 320101;
    private static final String  ACCOUNT_NAME                                   = "Free Trial Test Account";
    private static final Integer CC_PM_ID                                       = 5;

    private static final String CUSTOMER_CREATION_FAILED                        = "Customer creation failed!";
    private static final String ORDER_CREATION_FAILED                           = "Order creation failed!";
    private static final String INVOICE_GENERATION_FAILED                       = "Invoice generation failed after 100% usage!";
    private static final String INVOICE_AMOUNT_EXPECTED_ZERO                    = "Invoice amount should be $0!";
    private static final String ORDER_STATUS_EXPECTED_FINISHED                  = "Order status should be Finished!";
    private static final String ORDER_DATE_EXPECTED_TODAY                       = "order active until date should be today's date!";
    private static final String USAGE_QUANTITY_EXPECTED_50                      = "usage pool quantity should be 50% remaining!";
    private static final String INVOICE_NOT_EXPECTED                            = "invoice should not be generated for less than 100 % consumption!";
    private static final String AUD_NOT_EXPECTED_AS_ORDER_DATE                  = "Active until date should be as same as created order!";
    private static final BigDecimal BIG_DECIMAL_FIFTY                           = new BigDecimal("50.0000000000");
    private static final BigDecimal BIG_DECIMAL_ZERO                            = new BigDecimal("0E-10");
    private static final String FINISHED                                        = "Finished";

    private boolean ocbctNotPresent;
    private boolean gcitNotPresent;
    private Integer gcitPluginId;
    
    private ConfigurationBuilder configurationBuilder;
    private Integer weeklyOrderPeriod;
    private Integer orderId;

    private Calendar activeSince;
    private Calendar activeUntil;
    private Calendar nextInvoiceDate;
    private Integer accountTypeId;
    private Map<Integer, Integer> productAssetMap = new HashMap<>();
    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

    private static JbillingAPI api;

    @BeforeClass
    public void initializeTests(){
        testBuilder = getTestEnvironment();

        testBuilder.given(envBuilder -> {
            api = envBuilder.getPrancingPonyApi();
            testEnvironmentBuilder = envBuilder;
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull(testEnvBuilder.idForCode(ACCOUNT_NAME),"Account Creation Failed");
            assertNotNull(testEnvBuilder.idForCode(MEDIATED_USAGE_CATEGORY),"Mediated Categroy Creation Failed ");
            assertNotNull(testEnvBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE),"Product Creation Failed");
            assertNotNull(testEnvBuilder.idForCode(USAGE_POOL_01),"Usage pool Creation Failed");
            assertNotNull(testEnvBuilder.idForCode(WEEKLY_PLAN_CODE),"Plan Creation Failed");
            assertTrue(api.getPlanWS(testEnvBuilder.idForCode(WEEKLY_PLAN_CODE)).isFreeTrial(),"Free trial flag should be true!");
        });
    }

    private TestBuilder getTestEnvironment() {
        logger.debug("getting test environment!");
        return TestBuilder.newTest(false).givenForMultiple(envBuilder -> {

            api = envBuilder.getPrancingPonyApi();

            configurationBuilder = envBuilder.configurationBuilder(api);
            if(ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(), FreeTrialConsumptionTask.class.getName()))) {
                configurationBuilder.addPlugin(FreeTrialConsumptionTask.class.getName());
            }
            
            if(ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(), CustomerUsagePoolConsumptionActionTask.class.getName()))){
                configurationBuilder.addPlugin(CustomerUsagePoolConsumptionActionTask.class.getName());
            }

            if(ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(), OrderChangeBasedCompositionTask.class.getName()))){
                ocbctNotPresent = true;
                configurationBuilder.deletePlugin(OrderChangeBasedCompositionTask.class.getName(), api.getCallerId());
            }

            if(ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(), OrderLineBasedCompositionTask.class.getName()))){
                configurationBuilder.addPlugin(OrderLineBasedCompositionTask.class.getName());
            }

            configurationBuilder.build();

            // Creating account type 
            accountTypeId = buildAndPersistAccountType(envBuilder, ACCOUNT_NAME, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, MEDIATED_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            buildRateCardPriceModel(CALL_RATE_CARD_ID, MatchType.COUNTRY_AREA_CODE_MATCH);

            //Create usage products
            buildAndPersistProduct(envBuilder, api, WEEKLY_PLAN_ITEM_CODE, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api.getCallerCurrencyId()), true);

            buildAndPersistProduct(envBuilder, api, WEEKLY_PLAN_ITEM_CODE2, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api.getCallerCurrencyId()), true);

            //Usage product item ids
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2018);
            pricingDate.set(Calendar.MONTH, 0);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            weeklyOrderPeriod = getOrCreateOrderPeriod(PeriodUnitDTO.WEEK);

            PlanItemWS weeklyPlanItem = buildPlanItem(items.get(0), weeklyOrderPeriod, QUANTITY, PRICE, pricingDate.getTime());

            List<UsagePoolConsumptionActionWS> usagePoolConsumptionActions = new ArrayList<>();
            UsagePoolConsumptionActionWS actionWS = new UsagePoolConsumptionActionWS();
            actionWS.setPercentage("100");
            actionWS.setMediumType(NotificationMediumType.EMAIL);
            actionWS.setNotificationId("1");
            actionWS.setType(Constants.FUP_CONSUMPTION_NOTIFICATION);
            usagePoolConsumptionActions.add(actionWS);

            //Create usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder, USAGE_POOL_01, "100", envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), items,
                    usagePoolConsumptionActions);

            buildAndPersistPlan(envBuilder, WEEKLY_PLAN_CODE, WEEKLY_PLAN_CODE, weeklyOrderPeriod,
                    envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)), weeklyPlanItem);

            buildAndPersistPlan(envBuilder, WEEKLY_PLAN_CODE2, WEEKLY_PLAN_CODE2, weeklyOrderPeriod,
                    envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE2), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)), weeklyPlanItem);
        });
    }

    /**
     * Create a customer and subscribe to free trial offer plan.
     * Mediate the order till free usage quantity is exhausted
     **/
    @Test(priority = 1, enabled = true)
    public void testScenario01() {
        logger.debug("test scenario no 1");
        testBuilder.given(envBuilder -> {
            nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.add(Calendar.MONTH, 1);
            nextInvoiceDate.set(Calendar.DATE, nextInvoiceDate.getActualMinimum(Calendar.DAY_OF_MONTH));

            activeSince = Calendar.getInstance();
            activeSince.add(Calendar.DATE, -2);

            activeUntil = (Calendar) activeSince.clone();
            activeUntil.add(Calendar.DATE, 7);

            AssetWS scenario01Asset = getAssetIdByProductId(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);

            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
            productQuantityMap.put(envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE), BigDecimal.ONE);

        }).test(env -> {
            try {
                buildAndPersistCustomer(testEnvironmentBuilder, CUSTOMER_CODE_1, accountTypeId, nextInvoiceDate.getTime(),
                        weeklyOrderPeriod, nextInvoiceDate.get(Calendar.DAY_OF_WEEK));

                orderId = createOrder(testEnvironmentBuilder,WEEKLY_ORDER, activeSince.getTime(),CUSTOMER_CODE_1,activeUntil.getTime(), weeklyOrderPeriod,
                        Constants.ORDER_BILLING_POST_PAID, ORDER_CHANGE_STATUS_APPLY_ID, false,
                        productQuantityMap, productAssetMap, true);

                List<String> cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_1),"115", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);

                assertNotNull(env.idForCode(CUSTOMER_CODE_1),CUSTOMER_CREATION_FAILED);
                assertNotNull(env.idForCode(WEEKLY_ORDER),ORDER_CREATION_FAILED);
                OrderWS order = api.getOrder(orderId);
                assertEquals(order.getActiveUntil(), getStartOfDay(new Date()));

                Integer userId = api.getUserId(CUSTOMER_CODE_1);

                InvoiceWS invoice = api.getLatestInvoice(userId);

                assertNotNull(invoice, INVOICE_GENERATION_FAILED);

                assertEquals(invoice.getBalanceAsDecimal(),BIG_DECIMAL_ZERO,INVOICE_AMOUNT_EXPECTED_ZERO);

                assertEquals(order.getStatusStr(),FINISHED, ORDER_STATUS_EXPECTED_FINISHED);

                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()),ORDER_DATE_EXPECTED_TODAY);
            } finally {
                Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_1), 10, 0))
                    .forEach(invoice -> api.deleteInvoice(invoice.getId()));
                api.deleteUser(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_1));
            }
        });
    }

    /**
     * Create a customer and subscribe to free trial offer plan.
     * Order subscription free usage minutes are more than 100% before the active until date.
     **/
    @Test(priority = 2, enabled = true)
    public void testScenario02() {
        logger.debug("test scenario no 2");
        testBuilder.given(envBuilder ->{
            nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.add(Calendar.MONTH, 1);
            nextInvoiceDate.set(Calendar.DATE, nextInvoiceDate.getActualMinimum(Calendar.DAY_OF_MONTH));

            activeSince = Calendar.getInstance();
            activeSince.add(Calendar.DATE, -2);
            activeUntil = (Calendar) activeSince.clone();
            activeUntil.add(Calendar.DATE, 7);
            
            AssetWS scenario01Asset = getAssetIdByProductId(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
            productQuantityMap.put(envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE), BigDecimal.ONE);

            buildAndPersistCustomer(envBuilder, CUSTOMER_CODE_2, accountTypeId, nextInvoiceDate.getTime(),
                    weeklyOrderPeriod, nextInvoiceDate.get(Calendar.DAY_OF_WEEK));

            orderId = createOrder(envBuilder,WEEKLY_ORDER, activeSince.getTime(),CUSTOMER_CODE_2,activeUntil.getTime(), weeklyOrderPeriod,
                    Constants.ORDER_BILLING_POST_PAID, ORDER_CHANGE_STATUS_APPLY_ID, false,
                    productQuantityMap, productAssetMap, true);
        }).test(env -> {
            try {
                assertNotNull(env.idForCode(CUSTOMER_CODE_2),CUSTOMER_CREATION_FAILED);
                assertNotNull(env.idForCode(WEEKLY_ORDER),ORDER_CREATION_FAILED);

                Integer userId = api.getUserId(CUSTOMER_CODE_2);
                OrderWS order = api.getOrder(orderId);

                List<String> cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_2),"50", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);

                CustomerUsagePoolWS[] customerUsagePoolWS = api.getCustomerUsagePoolsByCustomerId(api.getUserWS(userId).getCustomerId());
                BigDecimal quantity = customerUsagePoolWS[0].getQuantityAsDecimal();
                assertEquals(quantity,BIG_DECIMAL_FIFTY, USAGE_QUANTITY_EXPECTED_50);
                order = api.getOrder(orderId);
                assertEquals(order.getActiveUntil(),getStartOfDay(activeUntil.getTime()), AUD_NOT_EXPECTED_AS_ORDER_DATE);

                InvoiceWS invoice = api.getLatestInvoice(userId);
                assertTrue(invoice==null, INVOICE_NOT_EXPECTED);
                cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_2),"50", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);
                assertTrue(!order.getStatusStr().equalsIgnoreCase(FINISHED), "Order status should not be Finished");

                order = api.getOrder(orderId);
                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()));
                invoice = api.getLatestInvoice(userId);
                assertNotNull(invoice, INVOICE_GENERATION_FAILED);
                assertEquals(invoice.getBalanceAsDecimal(),BIG_DECIMAL_ZERO, INVOICE_AMOUNT_EXPECTED_ZERO);
                assertEquals(order.getStatusStr(),FINISHED, ORDER_STATUS_EXPECTED_FINISHED);
                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()), ORDER_DATE_EXPECTED_TODAY);
            } finally {
                Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_2), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
                api.deleteUser(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_2));
            }
        });
    }

    /**
     * Create a customer and subscribe to free trial offer plan.
     * Order free subscription is expired on active until with free usage minutes still remaining
     **/
    @Test(priority = 3, enabled = true)
    public void testScenario03() {
        logger.debug("test scenario no 3");
        testBuilder.given(envBuilder ->{
            nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.add(Calendar.MONTH, 1);
            nextInvoiceDate.set(Calendar.DATE, nextInvoiceDate.getActualMinimum(Calendar.DAY_OF_MONTH));

            activeSince = Calendar.getInstance();
            activeSince.add(Calendar.DATE, -30);
            activeUntil = (Calendar) activeSince.clone();
            activeUntil.add(Calendar.DATE, 6);

            AssetWS scenario03Asset = getAssetIdByProductId(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario03Asset.getId());

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
            productQuantityMap.put(envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE), BigDecimal.ONE);

            buildAndPersistCustomer(envBuilder, CUSTOMER_CODE_3, accountTypeId, nextInvoiceDate.getTime(),
                    weeklyOrderPeriod, nextInvoiceDate.get(Calendar.DAY_OF_WEEK));

            orderId = createOrder(envBuilder,WEEKLY_ORDER, activeSince.getTime(),CUSTOMER_CODE_3,activeUntil.getTime(), weeklyOrderPeriod,
                    Constants.ORDER_BILLING_POST_PAID, ORDER_CHANGE_STATUS_APPLY_ID, false,
                    productQuantityMap, productAssetMap, true);
        }).test(env -> {
            try {
                assertNotNull(env.idForCode(CUSTOMER_CODE_3),CUSTOMER_CREATION_FAILED);
                assertNotNull(env.idForCode(WEEKLY_ORDER),ORDER_CREATION_FAILED);
                if(ArrayUtils.isEmpty(api.getPluginsWS(api.getCallerId(), GenerateCancellationInvoiceTask.class.getName()))) {
                    gcitNotPresent = true;
                    gcitPluginId = configureGenrateCancellationInvoicePlugin();
                }
                wait(60000);

                Integer userId = api.getUserId(CUSTOMER_CODE_3);

                InvoiceWS invoice2 = api.getLatestInvoice(userId);
                assertNotNull(invoice2, INVOICE_GENERATION_FAILED);
                assertEquals(invoice2.getBalanceAsDecimal(),BIG_DECIMAL_ZERO, INVOICE_AMOUNT_EXPECTED_ZERO);
            }finally {
                Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_3), 10, 0))
                    .forEach(invoice -> api.deleteInvoice(invoice.getId()));
                api.deleteUser(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_3));
            }
        });
    }

    /**
     * scenario 
     */
    @Test(priority = 4, enabled = true)
    public void testScenario04() {
        logger.debug("test scenario no 4");
        testBuilder.given(envBuilder ->{
            nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.add(Calendar.MONTH, 1);
            nextInvoiceDate.set(Calendar.DATE, nextInvoiceDate.getActualMinimum(Calendar.DAY_OF_MONTH));

            activeSince = Calendar.getInstance();
            activeSince.add(Calendar.DATE, -2);
            activeUntil = (Calendar) activeSince.clone();
            activeUntil.add(Calendar.DATE, 6);
            nextInvoiceDate = (Calendar) activeUntil.clone();
            
            AssetWS scenario02Asset = getAssetIdByProductId(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario02Asset.getId());

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
            productQuantityMap.put(envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE), BigDecimal.ONE);

            buildAndPersistCustomer(envBuilder, CUSTOMER_CODE_4, accountTypeId, nextInvoiceDate.getTime(),
                    weeklyOrderPeriod, nextInvoiceDate.get(Calendar.DAY_OF_WEEK));

            orderId = createOrder(envBuilder,WEEKLY_ORDER, activeSince.getTime(),CUSTOMER_CODE_4,activeUntil.getTime(),
                    weeklyOrderPeriod, Constants.ORDER_BILLING_POST_PAID, ORDER_CHANGE_STATUS_APPLY_ID, false,
                    productQuantityMap, productAssetMap, true);
        }).test(env -> {
            try {
                assertNotNull(env.idForCode(CUSTOMER_CODE_4),CUSTOMER_CREATION_FAILED);
                assertNotNull(env.idForCode(WEEKLY_ORDER),ORDER_CREATION_FAILED);

                Integer userId = api.getUserId(CUSTOMER_CODE_4);

                List<String> cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_4),"50",new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);

                OrderWS order = api.getOrder(orderId);
                assertTrue(order.getActiveUntil().compareTo(getStartOfDay(new Date())) != 0, "order active until date should be not today's date!");

                assertEquals(order.getActiveUntil(),getStartOfDay(activeUntil.getTime()), AUD_NOT_EXPECTED_AS_ORDER_DATE);

                InvoiceWS invoice = api.getLatestInvoice(userId);
                assertTrue(invoice==null, INVOICE_NOT_EXPECTED);

                cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_4),"65",new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);

                order = api.getOrder(orderId);
                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()));

                invoice = api.getLatestInvoice(userId);
                assertNotNull(invoice, INVOICE_GENERATION_FAILED);
                assertEquals(invoice.getBalanceAsDecimal(),BIG_DECIMAL_ZERO, INVOICE_AMOUNT_EXPECTED_ZERO);

                assertEquals(order.getStatusStr(),FINISHED, ORDER_STATUS_EXPECTED_FINISHED);

                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()), ORDER_DATE_EXPECTED_TODAY);
            } finally {
                Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_4), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
                api.deleteUser(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_4));
            }
        });
    }

    /**
     * Create a customer and subscribe to free trial offer plan.
     * Order free subscription is expired on active until with free usage minutes still remaining.
     * Subscribe the same user with a new plan other than free subscription plan
     */
    @Test(priority = 5, enabled = true)
    public void testScenario05() {
        logger.debug("test scenario 05");
        Map<Integer, BigDecimal> productQuantityMap2 = new HashMap<>();
        Map<Integer, Integer> productAssetMap2 = new HashMap<>();
        testBuilder.given(envBuilder ->{
            testEnvironmentBuilder = envBuilder;
            Calendar nextInvoiceDate2 = Calendar.getInstance();
            nextInvoiceDate2.add(Calendar.MONTH, 1);
            nextInvoiceDate2.set(Calendar.DATE, nextInvoiceDate2.getActualMinimum(Calendar.DAY_OF_MONTH));

            activeSince = Calendar.getInstance();
            activeSince.add(Calendar.DATE, -2);
            activeUntil = (Calendar) activeSince.clone();
            activeUntil.add(Calendar.DATE, 7);
            AssetWS asset = getAssetIdByProductId(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            productAssetMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, asset.getId());

            productQuantityMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
            productQuantityMap2.put(envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE), BigDecimal.ONE);

            buildAndPersistCustomer(envBuilder, CUSTOMER_CODE_5, accountTypeId, nextInvoiceDate2.getTime(),
                    weeklyOrderPeriod, nextInvoiceDate2.get(Calendar.DAY_OF_WEEK));

            orderId = createOrder(envBuilder,WEEKLY_ORDER, activeSince.getTime(),CUSTOMER_CODE_5,activeUntil.getTime(), weeklyOrderPeriod,
                    Constants.ORDER_BILLING_POST_PAID, ORDER_CHANGE_STATUS_APPLY_ID, false,
                    productQuantityMap2, productAssetMap2, true);
        }).test(env -> {
            try {
                assertNotNull(env.idForCode(CUSTOMER_CODE_5),CUSTOMER_CREATION_FAILED);
                assertNotNull(env.idForCode(WEEKLY_ORDER),ORDER_CREATION_FAILED);

                Integer userId = api.getUserId(CUSTOMER_CODE_5);
                OrderWS order = api.getOrder(orderId);

                List<String> cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_5),"50", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);

                CustomerUsagePoolWS[] customerUsagePoolWS = api.getCustomerUsagePoolsByCustomerId(api.getUserWS(userId).getCustomerId());
                BigDecimal quantity = customerUsagePoolWS[0].getQuantityAsDecimal();
                assertEquals(quantity,BIG_DECIMAL_FIFTY, USAGE_QUANTITY_EXPECTED_50);

                order = api.getOrder(orderId);
                assertEquals(order.getActiveUntil(),getStartOfDay(activeUntil.getTime()), USAGE_QUANTITY_EXPECTED_50);

                InvoiceWS invoice = api.getLatestInvoice(userId);
                assertTrue(invoice==null, INVOICE_NOT_EXPECTED);
                assertTrue(!order.getStatusStr().equalsIgnoreCase(FINISHED), "Order status should not be Finished");

                cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_5),"65", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);
                order = api.getOrder(orderId);
                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()));
                invoice = api.getLatestInvoice(userId);
                assertNotNull(invoice, INVOICE_GENERATION_FAILED);
                assertEquals(invoice.getBalanceAsDecimal(),BIG_DECIMAL_ZERO, INVOICE_AMOUNT_EXPECTED_ZERO);
                assertEquals(order.getStatusStr(),FINISHED, ORDER_STATUS_EXPECTED_FINISHED);
                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()), ORDER_DATE_EXPECTED_TODAY);

                AssetWS asset = getAssetIdByProductId(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
                productAssetMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, asset.getId());

                productQuantityMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap2.put(env.idForCode(WEEKLY_PLAN_ITEM_CODE), BigDecimal.ONE);

                orderId = createOrder(testEnvironmentBuilder,NON_FREE_WEEKLY_ORDER, activeSince.getTime(),CUSTOMER_CODE_5,activeUntil.getTime(),
                        weeklyOrderPeriod, Constants.ORDER_BILLING_POST_PAID, ORDER_CHANGE_STATUS_APPLY_ID, false,
                        productQuantityMap2, productAssetMap2, true);

                cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_5),"65", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);

                order = api.getOrder(orderId);
                customerUsagePoolWS = api.getCustomerUsagePoolsByCustomerId(api.getUserWS(userId).getCustomerId());
                quantity = customerUsagePoolWS[0].getQuantityAsDecimal();
                assertEquals(quantity,new BigDecimal("35.0000000000"),"User still should be able use not free trial plan!");
                logger.debug("order id {}", order.getId());
                assertTrue(order.getActiveUntil().compareTo(getStartOfDay(new Date())) != 0,
                        "Active until date should not be updated for non free trial plan!");
            } finally {
                Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_5), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
                api.deleteUser(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_5));
            }
        });
    }

    /**
     * Create a customer and subscribe to free trial offer plan.
     * Order active until is not provided while creation of Order
     */

    @Test(priority = 6, enabled = true)
    public void testScenario06() {
        logger.debug("test scenario no 6");
        testBuilder.given(envBuilder -> {
            nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.add(Calendar.MONTH, 1);
            nextInvoiceDate.set(Calendar.DATE, nextInvoiceDate.getActualMinimum(Calendar.DAY_OF_MONTH));

            activeSince = Calendar.getInstance();
            activeSince.add(Calendar.DATE, -2);

            AssetWS scenario01Asset = getAssetIdByProductId(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
            productQuantityMap.put(envBuilder.idForCode(WEEKLY_PLAN_ITEM_CODE), BigDecimal.ONE);

            buildAndPersistCustomer(envBuilder, CUSTOMER_CODE_6, accountTypeId, nextInvoiceDate.getTime(),
                    weeklyOrderPeriod, nextInvoiceDate.get(Calendar.DAY_OF_WEEK));

            orderId = createOrder(envBuilder,WEEKLY_ORDER, activeSince.getTime(),CUSTOMER_CODE_6,null, weeklyOrderPeriod,
                    Constants.ORDER_BILLING_POST_PAID, ORDER_CHANGE_STATUS_APPLY_ID, false,
                    productQuantityMap, productAssetMap, true);
        }).test(env -> {
            try {
                assertNotNull(env.idForCode(CUSTOMER_CODE_6),CUSTOMER_CREATION_FAILED);
                assertNotNull(env.idForCode(WEEKLY_ORDER),ORDER_CREATION_FAILED);

                Integer userId = api.getUserId(CUSTOMER_CODE_6);
                OrderWS order = api.getOrder(orderId);

                List<String> cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_6),"50", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);

                CustomerUsagePoolWS[] customerUsagePoolWS = api.getCustomerUsagePoolsByCustomerId(api.getUserWS(userId).getCustomerId());
                BigDecimal quantity = customerUsagePoolWS[0].getQuantityAsDecimal();
                assertEquals(quantity,BIG_DECIMAL_FIFTY, USAGE_QUANTITY_EXPECTED_50);

                InvoiceWS invoice = api.getLatestInvoice(userId);
                assertTrue(invoice==null, INVOICE_NOT_EXPECTED);

                cdrs = buildInboundCDR(getAssetIdentifiers(CUSTOMER_CODE_6),"65", new Date());
                triggerMediation(INBOUND_MEDIATION_LAUNCHER, cdrs);
                assertTrue(!order.getStatusStr().equalsIgnoreCase(FINISHED), "Order status should not be Finished");

                order = api.getOrder(orderId);
                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()));
                invoice = api.getLatestInvoice(userId);
                assertNotNull(invoice, INVOICE_GENERATION_FAILED);
                assertEquals(invoice.getBalanceAsDecimal(),BIG_DECIMAL_ZERO, INVOICE_AMOUNT_EXPECTED_ZERO);
                assertEquals(order.getStatusStr(),FINISHED, ORDER_STATUS_EXPECTED_FINISHED);
                assertEquals(order.getActiveUntil(),getStartOfDay(new Date()), ORDER_DATE_EXPECTED_TODAY);
            } finally {
                Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_6), 10, 0))
                .forEach(invoice -> api.deleteInvoice(invoice.getId()));
                api.deleteUser(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE_6));
            }
        });
    }

    private List<String> getAssetIdentifiers(String userName) {
        logger.debug("getting asset identifiers!");
        if( null == userName || userName.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> identifiers = new ArrayList<>();
        testBuilder.given(envBuilder -> {
            OrderWS [] orders = api.getUserSubscriptions(envBuilder.idForCode(userName));
            Arrays.stream(orders)
            .forEach(order -> {
                Arrays.stream(order.getOrderLines())
                .forEach(line -> {
                    if(null!=line.getAssetIds() && line.getAssetIds().length!= 0 )
                        Arrays.stream(line.getAssetIds())
                        .forEach(assetId ->
                            identifiers.add(api.getAsset(assetId).getIdentifier())
                        );
                });
            });
        });
        return identifiers;
    }
    private List<String> buildInboundCDR(List<String> indentifiers, String quantity, Date eventDate) {
        logger.debug("getting buildInbound CDR!");
        List<String> cdrs = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        indentifiers.forEach(asset ->
            cdrs.add("us-cs-telephony-voice-101108.vdc-070016UTC-" + UUID.randomUUID().toString()
                    + ","+asset+",tressie.johnson,Inbound,"+ asset +","+sdf.format(eventDate)+","
                    + "12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null")
        );

        return cdrs;
    }

    private static Integer getOrCreateOrderPeriod(int periodUnit){
        Integer orderPeriodWS = null;
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(periodUnit == period.getPeriodUnitId() && period.getValue()  == 1){
                orderPeriodWS = period.getId();
            }
        }

        if (orderPeriodWS == null) {
            orderPeriodWS = createPeriod(periodUnit);
        }

        return orderPeriodWS;
    }

    private static Integer createPeriod(int periodUnit) {
        if (PeriodUnitDTO.MONTH == periodUnit) {
            OrderPeriodWS monthly = new OrderPeriodWS();
            monthly.setEntityId(api.getCallerCompanyId());
            monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);
            monthly.setValue(1);
            monthly.setDescriptions(Arrays
                    .asList(new InternationalDescriptionWS(
                            Constants.LANGUAGE_ENGLISH_ID, "MONTHLY")));
            return api.createOrderPeriod(monthly);
        }

        if (PeriodUnitDTO.WEEK == periodUnit) {
            OrderPeriodWS weekly = new OrderPeriodWS();
            weekly.setEntityId(api.getCallerCompanyId());
            weekly.setPeriodUnitId(PeriodUnitDTO.WEEK);
            weekly.setValue(1);
            weekly.setDescriptions(Arrays
                    .asList(new InternationalDescriptionWS(
                            Constants.LANGUAGE_ENGLISH_ID, "WEEKLY")));
            return api.createOrderPeriod(weekly);
        }

        if (PeriodUnitDTO.SEMI_MONTHLY == periodUnit) {
            OrderPeriodWS semiMonthly = new OrderPeriodWS();
            semiMonthly.setEntityId(api.getCallerCompanyId());
            semiMonthly.setPeriodUnitId(PeriodUnitDTO.SEMI_MONTHLY);
            semiMonthly.setValue(1);
            semiMonthly.setDescriptions(Arrays
                    .asList(new InternationalDescriptionWS(
                            Constants.LANGUAGE_ENGLISH_ID,
                            "SEMY_MONTHLY")));
            return api.createOrderPeriod(semiMonthly);
        }

        if (PeriodUnitDTO.DAY == periodUnit) {
            OrderPeriodWS daily = new OrderPeriodWS();
            daily.setEntityId(api.getCallerCompanyId());
            daily.setPeriodUnitId(PeriodUnitDTO.DAY);
            daily.setValue(1);
            daily.setDescriptions(Arrays.asList(new InternationalDescriptionWS(
                    Constants.LANGUAGE_ENGLISH_ID, "DAILY")));
            return api.createOrderPeriod(daily);
        }

        if (PeriodUnitDTO.YEAR == periodUnit) {
            OrderPeriodWS yearly = new OrderPeriodWS();
            yearly.setEntityId(api.getCallerCompanyId());
            yearly.setPeriodUnitId(PeriodUnitDTO.YEAR);
            yearly.setValue(1);
            yearly.setDescriptions(Arrays
                    .asList(new InternationalDescriptionWS(
                            Constants.LANGUAGE_ENGLISH_ID, "YEARLY")));
            return api.createOrderPeriod(yearly);
        }

        return null;
    }

    private Integer buildAndPersistCustomer(TestEnvironmentBuilder envBuilder, String userName,
            Integer accountTypeId, Date nextInvoiceDate, Integer periodId, Integer nextInvoiceDay) {
        logger.debug("getting customer {}", userName);
        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(userName)
                .withAccountTypeId(accountTypeId)
                .addTimeToUsername(false)
                .withNextInvoiceDate(nextInvoiceDate)
                .withMainSubscription(new MainSubscriptionWS(periodId, nextInvoiceDay))
                .build();
        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);
        return userWS.getId();
    }

    private Integer createOrder(TestEnvironmentBuilder envBuilder, String code,Date activeSince,String userName, Date activeUntil,
            Integer orderPeriodId, int billingTypeId, int statusId, boolean prorate, Map<Integer, BigDecimal> productQuantityMap,
            Map<Integer, Integer> productAssetMap, boolean createZeroAmountOrder) {
        logger.debug("creating order {}", code);
        List<OrderLineWS> lines = productQuantityMap.entrySet()
                .stream()
                .map(lineItemQuatityEntry -> {
                    OrderLineWS line = new OrderLineWS();
                    line.setItemId(lineItemQuatityEntry.getKey());
                    line.setTypeId(Integer.valueOf(1));
                    ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                    line.setDescription(item.getDescription());
                    line.setQuantity(lineItemQuatityEntry.getValue());
                    line.setUseItem(true);
                    if(createZeroAmountOrder) {
                        line.setUseItem(false);
                        line.setPrice(BigDecimal.ZERO);
                        line.setAmount(line.getQuantityAsDecimal().multiply(line.getPriceAsDecimal()));
                    }
                    if(null!=productAssetMap && !productAssetMap.isEmpty()
                            && productAssetMap.containsKey(line.getItemId())) {
                        line.setAssetIds(new Integer[] {productAssetMap.get(line.getItemId())});
                    }
                    return line;
                }).collect(Collectors.toList());

        return envBuilder.orderBuilder(api)
        .withCodeForTests(code)
        .forUser(envBuilder.idForCode(userName))
        .withActiveSince(activeSince)
        .withActiveUntil(activeUntil)
        .withEffectiveDate(activeSince)
        .withPeriod(orderPeriodId)
        .withBillingTypeId(billingTypeId)
        .withProrate(prorate)
        .withOrderLines(lines)
        .withOrderChangeStatus(statusId)
        .build();

    }

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, String code, String desc,
            Integer periodId, Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
        logger.debug("creating plan {}", code);
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withFreeTrial(true)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    private void triggerMediation(String jobConfigName, List<String> cdr) {
        api.processCDR(getMediationConfiguration(jobConfigName), cdr);
    }

    private Integer getMediationConfiguration(String mediationJobLauncher) {
        logger.debug("getting mediation configuration");
        MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
        for (MediationConfigurationWS mediationConfigurationWS: allMediationConfigurations) {
            if (null != mediationConfigurationWS.getMediationJobLauncher() &&
                    (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
                return mediationConfigurationWS.getId();
            }
        }
        return null;
    }

    private static AssetWS getAssetIdByProductId(Integer productId) {
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(1);
        criteria.setOffset(0);
        criteria.setSort("id");
        criteria.setTotal(-1);
        criteria.setFilters(new BasicFilter[]{basicFilter});

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(productId, criteria);
        assertNotNull(assetsResult, "No available asset found for product "+productId);
        AssetWS[] availableAssets = null != assetsResult ? assetsResult.getObjects() : null;
        assertTrue(null != availableAssets && availableAssets.length != 0, "No assets found for product .");
        return null != availableAssets ? availableAssets[0] : null;
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        logger.debug("creating category {}", code);
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, PriceModelWS priceModelWS, boolean allowDecimal) {
        logger.debug("creating product {}", code);
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withPriceModel(priceModelWS)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    private PriceModelWS buildRateCardPriceModel(String rateCardId, MatchType matchType){
        logger.debug("creating price model {}", rateCardId);
        PriceModelWS rateCardPrice = new PriceModelWS(PriceModelStrategy.RATE_CARD.name(), null, 1);
        SortedMap<String, String> attributes = new TreeMap<>();
        attributes.put("rate_card_id", rateCardId);
        attributes.put("lookup_field", "Destination Number");
        attributes.put("match_type", matchType.name());
        rateCardPrice.setAttributes(attributes);
        return rateCardPrice;
    }

    private PlanItemWS buildPlanItem(Integer itemId, Integer periodId, String quantity, String price, Date pricingDate) {
        logger.debug("creating plan item {} ", itemId);
        return PlanBuilder.PlanItemBuilder.getBuilder()
                .withItemId(itemId)
                .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .addModel(pricingDate, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .withBundledPeriodId(periodId)
                .withBundledQuantity(quantity)
                .build();
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, String name, Integer ...paymentMethodTypeId) {
        logger.debug("creating account type {} ", name);
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }


    private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, String code, String quantity,Integer categoryId, List<Integer>  items, List<UsagePoolConsumptionActionWS> consumptionActions) {
        logger.debug("creating usage pool {} ", code);
        return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code)
                .withQuantity(quantity)
                .withResetValue("Reset To Initial Value")
                .withItemIds(items)
                .addItemTypeId(categoryId)
                .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS)
                .withCyclePeriodValue(Integer.valueOf(1)).withName(code)
                .withConsumptionActions(consumptionActions)
                .build();
    }

    public static Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @AfterClass
    public void cleanUp() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();

        if(gcitNotPresent) {
            api.deletePlugin(gcitPluginId);
        }

        if(ocbctNotPresent) {
            configurationBuilder.addPlugin(OrderChangeBasedCompositionTask.class.getName());
            configurationBuilder.build();
        }

        testBuilder = null;
        testEnvironmentBuilder = null;
    }

    private Integer configureGenrateCancellationInvoicePlugin() {

        PluggableTaskWS invoiceBillingProcessLinkingTask = new PluggableTaskWS();
        invoiceBillingProcessLinkingTask.setProcessingOrder(10);
        PluggableTaskTypeWS invoiceBillingProcessLinkingTaskType =
                api.getPluginTypeWSByClassName(GenerateCancellationInvoiceTask.class.getName());
        invoiceBillingProcessLinkingTask.setTypeId(invoiceBillingProcessLinkingTaskType.getId());

        invoiceBillingProcessLinkingTask.setParameters(new Hashtable<String, String>(invoiceBillingProcessLinkingTask.getParameters()));
        Hashtable<String, String> parameters = new Hashtable<>();
        parameters.put("cron_exp", "0 0/1 * 1/1 * ? *");
        invoiceBillingProcessLinkingTask.setParameters(parameters);
        return api.createPlugin(invoiceBillingProcessLinkingTask);
    }

    private void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
