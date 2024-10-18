package com.sapienter.jbilling.api.automation.mediation;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

/**
 *
 * @author Krunal Bhavsar
 *
 */
@Test(groups = { "api-automation" }, testName = "FutureDatedPlanWithPreUsageTest")
public class FutureDatedPlanWithPreUsageTest {

    private static final Logger logger = LoggerFactory.getLogger(FutureDatedPlanWithPreUsageTest.class);
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;

    private final static Integer CC_PM_ID = 5;

    private static final Integer nextInvoiceDay = 1;
    private static final Integer postPaidOrderTypeId = Constants.ORDER_BILLING_POST_PAID;
    public final static int ONE_TIME_ORDER_PERIOD = 1;
    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private String testCat1 = "MediatedUsageCategory";
    private String testAccount = "Account Type";

    public static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID = 320104;
    public static final int TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID = 320105;
    public static final int LOCAL_ECF_NUMBER_ASSET_PRODUCT_ID = 320106;

    public static final String INBOUND_MEDIATION_LAUNCHER = "inboundCallsMediationJobLauncher";
    // Mediated Usage Products
    public static final int INBOUND_USAGE_PRODUCT_ID = 320101;
    public static final int CHAT_USAGE_PRODUCT_ID = 320102;
    public static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;

    // Test data for scenario 18
    private static final String MONTHLY_PLAN_PRODUCT = "testMonthlyPlanItem";
    private static final String DAILY_PLAN_PRODUCT = "testDailyPlanItem";
    private static final String WEEKLY_PLAN_PRODUCT = "testWeeklyPlanItem";
    private static final String YEARLY_PLAN_PRODUCT = "testYearlyPlanItem";
    private static final String USAGE_POOL_01 = "testScenario1FUP100Min"+System.currentTimeMillis();
    private static final String PLAN = "testPlan100";

    // different periods
    private static Integer DAILY_PERIOD;
    private static Integer WEEKLY_PERIOD;
    private static Integer MONTHLY_PERIOD;
    private static Integer YEARLY_PERIOD;

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        DAILY_PERIOD = envHelper.getOrderPeriodDay(api);
        WEEKLY_PERIOD = envHelper.getOrderPeriodWeek(api);
        MONTHLY_PERIOD = envHelper.getOrderPeriodMonth(api);

        testBuilder.given(envBuilder -> {
            // Creating account type
            buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            YEARLY_PERIOD = buildAndPersistOrderPeriod(envBuilder, api, "testYearlyPeriod", 1, PeriodUnitDTO.YEAR);

            // creating plan
            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2017);
            pricingDate.set(Calendar.MONTH, 1);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
            PlanItemWS planItemProd1WS = buildPlanItem(api, items.get(0), MONTHLY_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd2WS = buildPlanItem(api, items.get(1), MONTHLY_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd3WS = buildPlanItem(api, items.get(2), MONTHLY_PERIOD, "0", "0.95", pricingDate.getTime());

            buildAndPersistUsagePool(envBuilder, api, USAGE_POOL_01, "300", envBuilder.idForCode(testCat1), items);
            buildAndPersistFlatProduct(envBuilder, api, MONTHLY_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "100.00", true);
            buildAndPersistPlan(envBuilder,api, PLAN, "100 Min Plan - $100.00 / Month", MONTHLY_PERIOD,
                    envBuilder.idForCode(MONTHLY_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),
                    planItemProd1WS, planItemProd2WS, planItemProd3WS);

            buildAndPersistFlatProduct(envBuilder, api, DAILY_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "100.00", true);
            buildAndPersistPlan(envBuilder,api, PLAN, "100 Min Plan - $100.00 / Month", DAILY_PERIOD,
                    envBuilder.idForCode(DAILY_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),
                    planItemProd1WS, planItemProd2WS, planItemProd3WS);

            buildAndPersistFlatProduct(envBuilder, api, WEEKLY_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "100.00", true);
            buildAndPersistPlan(envBuilder,api, PLAN, "100 Min Plan - $100.00 / Month", WEEKLY_PERIOD,
                    envBuilder.idForCode(WEEKLY_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),
                    planItemProd1WS, planItemProd2WS, planItemProd3WS);

            buildAndPersistFlatProduct(envBuilder, api, YEARLY_PLAN_PRODUCT, false, envBuilder.idForCode(testCat1), "100.00", true);
            buildAndPersistPlan(envBuilder,api, PLAN, "100 Min Plan - $100.00 / Month", YEARLY_PERIOD,
                    envBuilder.idForCode(YEARLY_PLAN_PRODUCT), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),
                    planItemProd1WS, planItemProd2WS, planItemProd3WS);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("MediatedUsage Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("DAILY PERIOD Creation Failed", DAILY_PERIOD);
            assertNotNull("WEEKLY PERIOD Creation Failed", WEEKLY_PERIOD);
            assertNotNull("Monthly PERIOD Creation Failed", MONTHLY_PERIOD);
            assertNotNull("Monthly PERIOD Creation Failed", YEARLY_PERIOD);
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(MONTHLY_PLAN_PRODUCT));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(DAILY_PLAN_PRODUCT));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(WEEKLY_PLAN_PRODUCT));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(YEARLY_PLAN_PRODUCT));
        });
    }

    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        if (null != testBuilder) {
            testBuilder = null;
        }
    }

    private TestBuilder getTestEnvironment() {

        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {

            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }

    /**
     * Scenarios #1
     * CDR Event Date Same as Monthly Orders Active Since Date
     */
    @Test
    public void testScenario01() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        String user01 = "test-User-01"+System.currentTimeMillis();

        Calendar activeSince = Calendar.getInstance();
        activeSince.set(Calendar.YEAR, 2017);
        activeSince.set(Calendar.MONTH, 2);
        activeSince.set(Calendar.DAY_OF_MONTH, 17);
        testBuilder.given(envBuilder -> {

            logger.debug("CDR with Effective Date Same as Monthly Orders Active Since Date #1");

            Calendar nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.set(Calendar.YEAR, 2017);
            nextInvoiceDate.set(Calendar.MONTH, 3);
            nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer asset01 = buildAndPersistAsset(envBuilder, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, RandomStringUtils.random(10, true, true));
            AssetWS scenario01Asset = api.getAsset(asset01);
            logger.debug("asset Identifier {} fetched for product {}", scenario01Asset.getIdentifier(), TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            Map<Integer, Integer> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
            productQuantityMap.put(environment.idForCode(MONTHLY_PLAN_PRODUCT), BigDecimal.ONE);

            List<String> inboundCdrs = buildInboundCDR(Arrays.asList(scenario01Asset.getIdentifier()), "150", "03/17/2017");

            InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
            // Next Invoice Date 1st of April 2017
            scenario01.createUser(user01, environment.idForCode(testAccount), nextInvoiceDate.getTime(), MONTHLY_PERIOD, nextInvoiceDay)
            // creating subscription order on 17th of March 2017
            .createOrder("testSubScriptionOrderO1", activeSince.getTime(),null, MONTHLY_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                    productQuantityMap, productAssetMap, false);

            Integer orderId = environment.idForCode("testSubScriptionOrderO1");
            logger.debug("testScenario01 checking asset {} on order {}", asset01, orderId);
            if(!checkAssetOnOrder(api, orderId, new Integer[] { asset01 })) {
                logger.debug("asset {} not found on order {}", scenario01Asset, orderId);
            }
            //  trigger Inbound Mediation
            UUID mediationProcessId = triggerMediationAndLogErrorRecords(api, INBOUND_MEDIATION_LAUNCHER, inboundCdrs);
            logger.debug("testScenario01 mediation processid {}", mediationProcessId);

        }).validate((testEnv, envBuilder) -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            final Integer customerId = testEnv.idForCode(user01);
            logger.debug("## Customer Id:: {}",customerId);

            Integer [] mediationOrder = api.getOrderByPeriod(customerId,ONE_TIME_ORDER_PERIOD);

            logger.debug("## mediationOrder: {}", Arrays.toString(mediationOrder));

            List<OrderWS> mediatedOrders = Arrays.stream(mediationOrder)
                    .map(api::getOrder)
                    .collect(Collectors.toList());

            mediatedOrders.forEach(order -> logger.debug("## mediationOrder: {}", order));


            assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(activeSince.getTime()), 1);

        });
        logger.debug("Scenario #1 has been passed successfully");
    }

    private UUID triggerMediationAndLogErrorRecords(JbillingAPI api, String jobConfigName, List<String> cdr) {
        UUID mediationProcessId = api.processCDR(getMediationConfiguration(api, jobConfigName), cdr);
        JbillingMediationErrorRecord[] errors = api.getErrorsByMediationProcess(mediationProcessId.toString(), 0, 10000);
        MediationProcess mediationProcess = api.getMediationProcess(mediationProcessId);
        if(null == mediationProcess.getEndDate()) {
            logger.debug("mediation process {} is still running", mediationProcessId);
        }
        if(ArrayUtils.isNotEmpty(errors)) {
            for(JbillingMediationErrorRecord  error : errors) {
                logger.debug("error cause {}", error.getErrorCodes());
            }
        } else {
            logger.debug("no error record found for process id {}", mediationProcessId);
        }
        return mediationProcessId;
    }

    private Integer getMediationConfiguration(JbillingAPI api, String mediationJobLauncher) {

        MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
        for (MediationConfigurationWS mediationConfigurationWS: allMediationConfigurations) {
            if (null != mediationConfigurationWS.getMediationJobLauncher() &&
                    (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
                return mediationConfigurationWS.getId();
            }
        }
        return null;
    }

    /**
     * Scenarios #2
     * Uploads 2 CRDS one CDR EventDate is before ActiveSince date and other CDR EventDate is after ActiveSince date.
     */
    @Test
    public void testScenario02() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        String user02 = "test-User-02"+System.currentTimeMillis();

        Calendar activeSince = Calendar.getInstance();
        activeSince.set(Calendar.YEAR, 2017);
        activeSince.set(Calendar.MONTH, 2);
        activeSince.set(Calendar.DAY_OF_MONTH, 17);

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Uploads 2 CRDS one CDR EventDate is before ActiveSince date and other CDR EventDate is after ActiveSince date #2");

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2017);
                nextInvoiceDate.set(Calendar.MONTH, 3);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Integer asset02 = buildAndPersistAsset(envBuilder, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, RandomStringUtils.random(10, true, true));
                AssetWS scenario02Asset = api.getAsset(asset02);

                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario02Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(MONTHLY_PLAN_PRODUCT), BigDecimal.ONE);

                List<String> inboundCdrs = new ArrayList<>();

                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario02Asset.getIdentifier()), "150", "03/16/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario02Asset.getIdentifier()), "150", "03/17/2017"));

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                // Next Invoice Date 1st of March 2016
                scenario01.createUser(user02,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_PERIOD, nextInvoiceDay)
                // creating subscription order on 17th of Feb 2016
                .createOrder("testSubScriptionOrderO2", activeSince.getTime(),null, MONTHLY_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        productQuantityMap, productAssetMap, false);

                Integer orderId = environment.idForCode("testSubScriptionOrderO2");
                logger.debug("testScenario02 checking asset {} on order {}", asset02, orderId);
                if(!checkAssetOnOrder(api, orderId, new Integer[] { asset02 })) {
                    logger.debug("asset {} not found on order {}", scenario02Asset, orderId);
                }
                //  trigger Inbound Mediation
                scenario01.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final Integer customerId = testEnv.idForCode(user02);
                logger.debug("## Customer Id:: {}",customerId);

                Integer [] mediationOrder = api.getOrderByPeriod(customerId,ONE_TIME_ORDER_PERIOD);

                logger.debug("## mediationOrder: {}",Arrays.toString(mediationOrder));

                List<OrderWS> mediatedOrders = Arrays.stream(mediationOrder)
                        .map(api::getOrder)
                        .collect(Collectors.toList());

                mediatedOrders.forEach(order -> logger.debug("## mediationOrder: {}",order));


                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017"), activeSince.getTime()), 1);
                // Pre Usage One time order should be charged with product level price
                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017")), "2.5000000000");

            });
            logger.debug("Scenario #2 has been passed successfully");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Scenarios #3
     *Customer Main Subscription 1 Monthly
     *Event Date	Monthly order 	Expected mediated order	Result
					Active since	Active since Date
	03-15-2017		03-17-2017		03-01-2017				Create new order
	03-16-2017		03-17-2017		03-01-2017				Append usage on exiting 03-01-2017 active since order
	03-17-2017		03-17-2017		03-17-2017				Create new order
	03-18-2017		03-17-2017		03-17-2017				Append usage on exiting 03-17-2017 active since order
	03-31-2017		03-17-2017		03-17-2017				Append usage on exiting 03-17-2017 active since order
     *
     */
    @Test
    public void testScenario03() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        String user03 = "test-User-03"+System.currentTimeMillis();

        Calendar activeSince = Calendar.getInstance();
        activeSince.set(Calendar.YEAR, 2017);
        activeSince.set(Calendar.MONTH, 2);
        activeSince.set(Calendar.DAY_OF_MONTH, 17);

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Customer Main Subscription 1 Monthly #3");

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2017);
                nextInvoiceDate.set(Calendar.MONTH, 3);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Integer asset03 = buildAndPersistAsset(envBuilder, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, RandomStringUtils.random(10, true, true));
                AssetWS scenario03Asset = api.getAsset(asset03);

                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario03Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(MONTHLY_PLAN_PRODUCT), BigDecimal.ONE);

                List<String> inboundCdrs = new ArrayList<>();

                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario03Asset.getIdentifier()), "150", "03/15/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario03Asset.getIdentifier()), "150", "03/16/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario03Asset.getIdentifier()), "150", "03/17/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario03Asset.getIdentifier()), "150", "03/18/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario03Asset.getIdentifier()), "150", "03/31/2017"));

                InvoiceSummaryScenarioBuilder scenario03 = new InvoiceSummaryScenarioBuilder(testBuilder);
                // Next Invoice Date 1st of April 2017
                scenario03.createUser(user03,environment.idForCode(testAccount),nextInvoiceDate.getTime(), MONTHLY_PERIOD, nextInvoiceDay)
                // creating subscription order on 17th of March 2017
                .createOrder("testSubScriptionOrderO3", activeSince.getTime(),null, MONTHLY_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        productQuantityMap, productAssetMap, false);

                Integer orderId = environment.idForCode("testSubScriptionOrderO3");
                logger.debug("testScenario03 checking asset {} on order {}", asset03, orderId);
                if(!checkAssetOnOrder(api, orderId, new Integer[] { asset03 })) {
                    logger.debug("asset {} not found on order {}", scenario03Asset, orderId);
                }
                //  trigger Inbound Mediation
                scenario03.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final Integer customerId = testEnv.idForCode(user03);
                logger.debug("## Customer Id:: "+customerId);

                Integer [] mediationOrder = api.getOrderByPeriod(customerId,ONE_TIME_ORDER_PERIOD);

                logger.debug("## mediationOrder: "+ Arrays.toString(mediationOrder));

                List<OrderWS> mediatedOrders = Arrays.stream(mediationOrder)
                        .map(api::getOrder)
                        .collect(Collectors.toList());

                mediatedOrders.forEach(order -> logger.debug("## mediationOrder: "+ order));


                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017"),activeSince.getTime()), 1);
                // Pre Usage One time order should be charged with product level price
                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017")), "2.5000000000");
            });
            logger.debug("Scenario #3 has been passed successfully");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Scenarios #4
     *Customer Main Subscription Daily
     *Event Date	Monthly order 	Expected mediated order	Result
					Active since	Active since Date
     *03-15-2017	03-22-2017		03-01-2017				Create new order
     *03-16-2017	03-22-2017		03-01-2017				Append usage on exiting 03-01-2017 active since order
     *03-22-2017	03-22-2017		03-22-2017				Create new order
     *03-18-2017	03-22-2017		03-01-2017				Append usage on exiting 03-01-2017 active since order
     *03-31-2017	03-22-2017		03-22-2017				Append usage on exiting 03-22-2017 active since order
     */
    @Test
    public void testScenario04() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        String user04 = "test-User-04"+System.currentTimeMillis();

        Calendar activeSince = Calendar.getInstance();
        activeSince.set(Calendar.YEAR, 2017);
        activeSince.set(Calendar.MONTH, 2);
        activeSince.set(Calendar.DAY_OF_MONTH, 22);

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Customer Main Subscription Daily #4");

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2017);
                nextInvoiceDate.set(Calendar.MONTH, 3);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Integer asset04 = buildAndPersistAsset(envBuilder, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, RandomStringUtils.random(10, true, true));
                AssetWS scenario04Asset = api.getAsset(asset04);

                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario04Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(DAILY_PLAN_PRODUCT), BigDecimal.ONE);

                List<String> inboundCdrs = new ArrayList<>();

                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario04Asset.getIdentifier()), "150", "03/15/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario04Asset.getIdentifier()), "150", "03/16/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario04Asset.getIdentifier()), "150", "03/22/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario04Asset.getIdentifier()), "150", "03/18/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario04Asset.getIdentifier()), "150", "03/31/2017"));

                InvoiceSummaryScenarioBuilder scenario04 = new InvoiceSummaryScenarioBuilder(testBuilder);
                // Next Invoice Date 1st of April 2017
                scenario04.createUser(user04,environment.idForCode(testAccount),nextInvoiceDate.getTime(), DAILY_PERIOD, nextInvoiceDay)
                // creating subscription order on 17th of March 2017
                .createOrder("testSubScriptionOrderO4", activeSince.getTime(),null, DAILY_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        productQuantityMap, productAssetMap, false);
                Integer orderId = environment.idForCode("testSubScriptionOrderO4");
                logger.debug("testScenario04 checking asset {} on order {}", asset04, orderId);
                if(!checkAssetOnOrder(api, orderId, new Integer[] { asset04 })) {
                    logger.debug("asset {} not found on order {}", scenario04Asset, orderId);
                }
                //  trigger Inbound Mediation
                scenario04.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final Integer customerId = testEnv.idForCode(user04);
                logger.debug("## Customer Id:: "+customerId);

                Integer [] mediationOrder = api.getOrderByPeriod(customerId,ONE_TIME_ORDER_PERIOD);

                logger.debug("## mediationOrder: "+ Arrays.toString(mediationOrder));

                List<OrderWS> mediatedOrders = Arrays.stream(mediationOrder)
                        .map(api::getOrder)
                        .collect(Collectors.toList());

                mediatedOrders.forEach(order -> logger.debug("## mediationOrder: "+ order));


                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017"),activeSince.getTime()), 1);
                // Pre Usage One time order should be charged with product level price
                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017")), "2.5000000000");
            });
            logger.debug("Scenario #4 has been passed successfully");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Scenarios #5
     *Customer Main Subscription Weekly
     *Event Date	Monthly order 	Expected mediated order	Result
					Active since	Active since Date
     *03-02-2017	03-15-2017		03-01-2017				Create new order
     *03-03-2017	03-15-2017		03-01-2017				Append usage on exiting 03-01-2017 active since order
     *03-14-2017	03-15-2017		03-01-2017				Append usage on exiting 03-01-2017 active since order
     *03-29-2017	03-15-2017		03-15-2017				Append usage on exiting 03-15-2017 active since order
     *04-01-2017	03-15-2017		04-01-2017				Create new order for future period
     */
    @Test
    public void testScenario05() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        String user05 = "test-User-05"+System.currentTimeMillis();

        Calendar activeSince = Calendar.getInstance();
        activeSince.set(Calendar.YEAR, 2017);
        activeSince.set(Calendar.MONTH, 2);
        activeSince.set(Calendar.DAY_OF_MONTH, 15);

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Customer Main Subscription Weekly #5");

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2017);
                nextInvoiceDate.set(Calendar.MONTH, 2);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 19);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Integer asset05 = buildAndPersistAsset(envBuilder, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, RandomStringUtils.random(10, true, true));
                AssetWS scenario05Asset = api.getAsset(asset05);

                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario05Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(WEEKLY_PLAN_PRODUCT), BigDecimal.ONE);

                List<String> inboundCdrs = new ArrayList<>();

                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario05Asset.getIdentifier()), "150", "03/02/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario05Asset.getIdentifier()), "150", "03/03/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario05Asset.getIdentifier()), "150", "03/14/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario05Asset.getIdentifier()), "150", "03/29/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario05Asset.getIdentifier()), "150", "04/01/2017"));

                InvoiceSummaryScenarioBuilder scenario05 = new InvoiceSummaryScenarioBuilder(testBuilder);
                // Next Invoice Date 19th of April 2017
                scenario05.createUser(user05,environment.idForCode(testAccount),nextInvoiceDate.getTime(), WEEKLY_PERIOD, nextInvoiceDay)
                // creating subscription order on 17th of March 2017
                .createOrder("testSubScriptionOrderO5", activeSince.getTime(),null, WEEKLY_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        productQuantityMap, productAssetMap, false);
                Integer orderId = environment.idForCode("testSubScriptionOrderO5");
                logger.debug("testScenario05 checking asset {} on order {}", asset05, orderId);
                if(!checkAssetOnOrder(api, orderId, new Integer[] { asset05 })) {
                    logger.debug("asset {} not found on order {}", scenario05Asset, orderId);
                }
                //  trigger Inbound Mediation
                scenario05.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final Integer customerId = testEnv.idForCode(user05);
                logger.debug("## Customer Id:: "+customerId);

                Integer [] mediationOrder = api.getOrderByPeriod(customerId,ONE_TIME_ORDER_PERIOD);

                logger.debug("## mediationOrder: "+ Arrays.toString(mediationOrder));

                List<OrderWS> mediatedOrders = Arrays.stream(mediationOrder)
                        .map(api::getOrder)
                        .collect(Collectors.toList());

                mediatedOrders.forEach(order -> logger.debug("## mediationOrder: "+ order));


                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017"),activeSince.getTime(),parseStringToDate("04/01/2017")), 1);
                // Pre Usage One time order should be charged with product level price
                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017")), "2.5000000000");
                // Usage order after Plan orders active since date should be charged with Plan level price
                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("04/01/2017")), "0.9500000000");
            });
            logger.debug("Scenario #5 has been passed successfully");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Scenarios #6
     *Customer Main Subscription Yearly
     *Event Date	Monthly order 	Expected mediated order	Result
					Active since	Active since Date
     *01-01-2017	03-15-2017		03-01-2017				Create new order
     *03-03-2017	03-15-2017		03-01-2017				Append usage on exiting 03-01-2017 active since order
     *03-14-2017	03-15-2017		03-01-2017				Append usage on exiting 03-01-2017 active since order
     *03-29-2017	03-15-2017		03-15-2017				Append usage on exiting 03-15-2017 active since order
     *04-01-2017	03-15-2017		04-01-2017				Create new order for future period
     */
    @Test
    public void testScenario06() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        String user06 = "test-User-06"+System.currentTimeMillis();

        Calendar activeSince = Calendar.getInstance();
        activeSince.set(Calendar.YEAR, 2017);
        activeSince.set(Calendar.MONTH, 2);
        activeSince.set(Calendar.DAY_OF_MONTH, 15);

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Customer Main Subscription Yearly #6");

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 01);

                final JbillingAPI api = envBuilder.getPrancingPonyApi();

                Integer asset06 = buildAndPersistAsset(envBuilder, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, RandomStringUtils.random(10, true, true));
                AssetWS scenario06Asset = api.getAsset(asset06);

                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario06Asset.getId());

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
                productQuantityMap.put(environment.idForCode(YEARLY_PLAN_PRODUCT), BigDecimal.ONE);

                List<String> inboundCdrs = new ArrayList<>();

                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario06Asset.getIdentifier()), "150", "01/01/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario06Asset.getIdentifier()), "150", "03/03/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario06Asset.getIdentifier()), "150", "03/14/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario06Asset.getIdentifier()), "150", "03/29/2017"));
                inboundCdrs.addAll(buildInboundCDR(Arrays.asList(scenario06Asset.getIdentifier()), "150", "04/01/2017"));

                InvoiceSummaryScenarioBuilder scenario06 = new InvoiceSummaryScenarioBuilder(testBuilder);
                // Next Invoice Date 19th of April 2017
                scenario06.createUser(user06,environment.idForCode(testAccount),nextInvoiceDate.getTime(), YEARLY_PERIOD, nextInvoiceDay)
                // creating subscription order on 17th of March 2017
                .createOrder("testSubScriptionOrderO6", activeSince.getTime(),null, YEARLY_PERIOD, postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, true,
                        productQuantityMap, productAssetMap, false);
                Integer orderId = environment.idForCode("testSubScriptionOrderO6");
                logger.debug("testScenario05 checking asset {} on order {}", asset06, orderId);
                if(!checkAssetOnOrder(api, orderId, new Integer[] { asset06 })) {
                    logger.debug("asset {} not found on order {}", scenario06Asset, orderId);
                }
                //  trigger Inbound Mediation
                scenario06.triggerMediation(INBOUND_MEDIATION_LAUNCHER, inboundCdrs);

            }).validate((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final Integer customerId = testEnv.idForCode(user06);
                logger.debug("## Customer Id:: "+customerId);

                Integer [] mediationOrder = api.getOrderByPeriod(customerId,ONE_TIME_ORDER_PERIOD);

                logger.debug("## mediationOrder: "+ Arrays.toString(mediationOrder));

                List<OrderWS> mediatedOrders = Arrays.stream(mediationOrder)
                        .map(api::getOrder)
                        .collect(Collectors.toList());

                mediatedOrders.forEach(order -> logger.debug("## mediationOrder: "+ order));


                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("01/01/2017"),parseStringToDate("03/01/2017"),activeSince.getTime(),parseStringToDate("04/01/2017")), 1);
                // Pre Usage One time order should be charged with product level price
                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("01/01/2017")), "2.5000000000");
                assertMediatedOrderByCDREventDate(mediatedOrders, Arrays.asList(parseStringToDate("03/01/2017")), "2.5000000000");
            });
            logger.debug("Scenario #6 has been passed successfully");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Asserts Order for given ActiveSince Date
     * @param orders
     * @param eventDate
     * @param expectedOrderQuantity
     */
    private void assertMediatedOrderByCDREventDate(List<OrderWS> orders, List<Date> activeSinceDates, int expectedOrderQuantity) {
        logger.debug("Validating mediated Order for "+ activeSinceDates);

        Map<Date, List<OrderWS>> mediatedOrderMapByEventDate = orders.stream()
                .peek(order -> order.setActiveSince(trunCateDate(order.getActiveSince())))
                .collect(Collectors.groupingBy(OrderWS::getActiveSince, Collectors.toList()));
        logger.debug(mediatedOrderMapByEventDate.toString());
        activeSinceDates.stream()
        .forEach(date -> {
            logger.debug("Fetching Mediated Order for Date "+ date);
            List<OrderWS> mediatedOrderForDate = mediatedOrderMapByEventDate.get(trunCateDate(date));
            assertNotNull("MediatedOrder is not created ", mediatedOrderForDate);
            assertEquals(expectedOrderQuantity, mediatedOrderForDate.size(), "Only One Mediated Order Should be Created For Date" + date);
            assertEquals(trunCateDate(date), mediatedOrderForDate.get(0).getActiveSince(), "Invalid active since!!");
        });
    }

    /**
     * Asserts Pre Usage Order price
     * @param orders
     * @param eventDate
     * @param price
     */
    private void assertMediatedOrderByCDREventDate(List<OrderWS> orders, List<Date> activeSinceDates, String price) {
        logger.debug("Validating mediated Order for "+ activeSinceDates);

        Map<Date, List<OrderWS>> mediatedOrderMapByEventDate = orders.stream()
                .peek(order -> order.setActiveSince(trunCateDate(order.getActiveSince())))
                .collect(Collectors.groupingBy(OrderWS::getActiveSince, Collectors.toList()));
        logger.debug(mediatedOrderMapByEventDate.toString());
        activeSinceDates.stream()
        .forEach(date -> {
            List<OrderWS> mediatedOrderForDate = mediatedOrderMapByEventDate.get(trunCateDate(date));
            logger.debug("Fetching Mediated Order Price "+ mediatedOrderForDate.get(0).getOrderLines()[0].getPrice());
            assertEquals(price, mediatedOrderForDate.get(0).getOrderLines()[0].getPrice(), "Incorrect price!!");
        });
    }

    private Date trunCateDate(Date date) {
        return DateUtils.truncate(date, Calendar.DATE);
    }

    public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {

        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();

        return accountTypeWS.getId();
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

    public Integer buildAndPersistLinePercentageProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, String percentage, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withLinePercentage(percentage)
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

    private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String quantity,Integer categoryId, List<Integer>  items) {
        return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code)
                .withQuantity(quantity)
                .withResetValue("Reset To Initial Value")
                .withItemIds(items)
                .addItemTypeId(categoryId)
                .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                .withCyclePeriodValue(Integer.valueOf(1)).withName(code)
                .build();
    }


    public static Integer buildAndPersistPlugIn(TestEnvironmentBuilder envBuilder, JbillingAPI api, String pluginClassName, Hashtable<String, String> parameters) {
        envBuilder.configurationBuilder(api)
        .addPluginWithParameters(pluginClassName, parameters)
        .build();
        return envBuilder.idForCode(pluginClassName);

    }

    private Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer itemId, String phoneNumber) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        ItemDTOEx item = api.getItem(itemId, null, null);
        ItemTypeWS itemTypeWS = api.getItemCategoryById(item.getTypes()[0]);
        Integer assetStatusId = itemTypeWS.getAssetStatuses().stream().
                filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription()
                .equals("Available")).collect(Collectors.toList()).get(0).getId();
        return envBuilder.assetBuilder(api)
                .withItemId(itemId)
                .withAssetStatusId(assetStatusId)
                .global(true)
                .withIdentifier(phoneNumber)
                .withCode(phoneNumber)
                .build();
    }


    private Date parseStringToDate(String date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy");
            return format.parse(date);
        } catch (ParseException e) {
            logger.debug("Exception Occur During String To Date Conversion" + e.getLocalizedMessage());
            return new Date(0);
        }
    }

    private List<String> buildInboundCDR(List<String> indentifiers, String quantity, String eventDate) {
        List<String> cdrs = new ArrayList<String>();
        indentifiers.forEach(asset -> {
            cdrs.add("us-cs-telephony-voice-101108.vdc-070016UTC-" + UUID.randomUUID().toString()+",6165042651,tressie.johnson,Inbound,"+ asset +","+eventDate+","+"12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null");
        });

        return cdrs;
    }

    public Integer buildAndPersistOrderPeriod(TestEnvironmentBuilder envBuilder, JbillingAPI api,
            String description, Integer value, Integer unitId) {

        return envBuilder.orderPeriodBuilder(api)
                .withDescription(description)
                .withValue(value)
                .withUnitId(unitId)
                .build();
    }

    private boolean checkAssetOnOrder(JbillingAPI api, Integer orderId, Integer[] assets) {
        OrderWS order = api.getOrder(orderId);
        for(OrderLineWS orderLineWS : order.getOrderLines()) {
            Integer[] lineAssets = orderLineWS.getAssetIds();
            for(Integer assetId : assets) {
                if(ArrayUtils.contains(lineAssets, assetId)) {
                    AssetWS asset = api.getAsset(assetId);
                    logger.debug("asset id {} with identifier {} found on order {}", assetId, asset.getIdentifier(), orderId);
                    return true;
                }
            }
        }
        return false;
    }
}