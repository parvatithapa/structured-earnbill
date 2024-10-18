package com.sapienter.jbilling.fc.mediation;

import static org.junit.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

@Test(groups = { "fullcreative" }, testName = "FullCreativeMediationWith30Under30Test")
public class FullCreativeMediationWith30Under30Test {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    private static final Integer CC_PM_ID                           = 5;
    private static final int MONTHLY_ORDER_PERIOD                   = 2;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID           = 3;
    private String subScriptionProd01                               = "testPlanSubscriptionItem1_"+ System.currentTimeMillis();
    private String subScriptionProd02                               = "testPlanSubscriptionItem2"+ System.currentTimeMillis();
    private String subScriptionProd03                              = "testPlanSubscriptionItem01_"+ System.currentTimeMillis();
    private String plan01                                           = "100 free minute Plan with free call limit";
    private String plan02                                           = "100 free minute Plan without free call limit";
    private String plan03                                           = "50 free minute Plan without free call limit";
    private String usagePoolO1                                      = "UP with 100 Quantity" + System.currentTimeMillis();
    private String usagePoolO2                                      = "UP with 50 Quantity" + System.currentTimeMillis();
    private String testAccount                                      = "Account Type";
    private String user01                                           = "TEST_USER_01_" + UUID.randomUUID().toString();
    private String user02                                           = "TEST_USER_02_" + UUID.randomUUID().toString();
    private String user03                                           = "TEST_USER_03_" + UUID.randomUUID().toString();
    private String user04                                           = "TEST_USER_04_" + UUID.randomUUID().toString();
    private String user05                                           = "TEST_USER_05_" + UUID.randomUUID().toString();
    private String user06                                           = "TEST_USER_06_" + UUID.randomUUID().toString();
    private static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID  = 320104;
    private String testCat1                                         = "TEST_CATEGORY_1_" + UUID.randomUUID().toString();
    private String testCat2                                         = "TEST_CATEGORY_2_" + UUID.randomUUID().toString();
    // Mediated Usage Products
    private static final int INBOUND_USAGE_PRODUCT_ID               = 320101;
    private static final int CHAT_USAGE_PRODUCT_ID                  = 320102;
    private static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID       = 320103;
    private static final String SUBSCRIPTION_ORDER_CODE1            = "subscriptionOrder1"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE2            = "subscriptionOrder2"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE3            = "subscriptionOrder3"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE4            = "subscriptionOrder4"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE5            = "subscriptionOrder5"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE6            = "subscriptionOrder6"+ UUID.randomUUID().toString();
    private static final String USER_ASSERT                         = "User Created {}";
    private static final String USER_CREATION_ASSERT                = "User Creation Failed";
    private static final String ORDER_CREATION_ASSERT               = "Order Creation Failed";
    private static final String USER_INVOICE_ASSERT                 = "Creating User with next invoice date {}";
    private static final String LIVE_ANSWER_META_FIELD_NAME         = "Set ItemId For Live Answer";
    private static final String  FC_MEDIATION_CONFIG_NAME           = "fcMediationJob";
    private static final String  FC_MEDIATION_JOB_NAME              = "fcMediationJobLauncher";
    private static final String MEDIATED_ORDER_SHOULD_HAVE          = "Mediated Order Should have ";
    private static final String MEDIATED_ORDER_SHOULD_HAVE_LINES    = "Mediated Order Should have lines";
    private static final String MEDIATION_SHOULD_CREATE_ORDER       = "Mediation Should Create Order";
    private static final String FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE = "freeCallCounter for the order lines should be ";
    private static final String MEDIATION_ERROR_RECORD_COUNT        = "Mediation Error Record Count";
    private static final String MEDIATION_DONE_AND_NOT_BILLABLE     = "Mediation Done And Not Billable";
    private static final String MEDIATION_DONE_AND_BILLABLE         = "Mediation Done And Billable ";
    private static final String MEDIATION_PROCESS                   = "Mediation Process {}";
    private static final String BUILD_CRDS                          = "build crds {}";
    private static final String ORDER_CREATED                       = "Order Created {}";
    private static final String CDR_FORMAT                          = "%s,%s,tressie.johnson,%s,%s,07/05/2016,12:00:16 AM,4,3,407,2,0,%s,47,0,null";

    private static int CUSTOMER_LEVEL_FREE_CALLS                    = 10;
    private static int PLAN_LEVEL_FREE_CALLS_1                      = 5;
    private static int PLAN_LEVEL_FREE_CALLS_2                      = 3;
    private static int COMPANY_LEVEL_FREE_CALLS                     = 15;

    private Integer ratingSchemeId;
    private int freeCallsLessThan30;
    private int freeCallsGreaterOrEqualThan30;
    private int totalCdrCount = 0;
    private JbillingAPI api;

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator ->
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi())
        );
    }

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            api = envBuilder.getPrancingPonyApi();

            // Creating account type
            buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
            buildAndPersistCategory(envBuilder, api, testCat2, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            // Creating usage products
            buildAndPersistFlatProduct(envBuilder, api, subScriptionProd01, false, envBuilder.idForCode(testCat1), "100.00", true);
            buildAndPersistFlatProduct(envBuilder, api, subScriptionProd02, false, envBuilder.idForCode(testCat2), "50.00", true);
            buildAndPersistFlatProduct(envBuilder, api, subScriptionProd03, false, envBuilder.idForCode(testCat2), "50.00", true);

            // Usage product item ids
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

            PlanItemWS planItemProd07WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd08WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd09WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

            PlanItemWS planItemProd04WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd05WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd06WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

            buildAndPersistMetafield(testBuilder, LIVE_ANSWER_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY);
            // creating Plans, Usage Pools for scenario 01

            // creating usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder, api, usagePoolO1, "100", envBuilder.idForCode(testCat1), items);
            buildAndPersistUsagePool(envBuilder, api, usagePoolO2, "50", envBuilder.idForCode(testCat2), items);

            setCompanyLevelNumberOfFreeCalls(api, COMPANY_LEVEL_FREE_CALLS);

            // creating 100 min plan
            buildAndPersistPlan(envBuilder, api, plan01, "100 Free Minutes Plan with free call limit", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(subScriptionProd01), Arrays.asList(envBuilder.idForCode(usagePoolO1)), PLAN_LEVEL_FREE_CALLS_1,
                    planItemProd01WS, planItemProd02WS, planItemProd03WS);
            // creating 100 min plan
            buildAndPersistPlan(envBuilder, api, plan02, "100 Free Minutes Plan without free call limit", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(subScriptionProd02), Arrays.asList(envBuilder.idForCode(usagePoolO1)), 0,
                    planItemProd04WS, planItemProd05WS, planItemProd06WS);
            // creating 50 min plan
            buildAndPersistPlan(envBuilder, api, plan03, "50 Free Minutes Plan with free call limit", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(subScriptionProd03), Arrays.asList(envBuilder.idForCode(usagePoolO2)), PLAN_LEVEL_FREE_CALLS_2,
                    planItemProd07WS, planItemProd08WS, planItemProd09WS);

            // Setting Company Level Meta Fields
            setCompanyLevelMetaField(testBuilder.getTestEnvironment());

            // Creating Job Launcher
            buildAndPersistMediationConfiguration(envBuilder, api, FC_MEDIATION_CONFIG_NAME, FC_MEDIATION_JOB_NAME);

            // Configuring Usage Manager Task
            FullCreativeUtil.updatePlugin(FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);

            if (api.getRatingSchemesForEntity().length == 0) {
                MediationRatingSchemeWS ratingScheme = FullCreativeUtil.getRatingSchemeWithHalfRoundUp("Prancing Pony Rating Scheme");
                logger.debug("Creating Rating Scheme with initial increment 30 and main increment 6.");
                ratingSchemeId = api.createRatingScheme(ratingScheme);
                assertNotNull("Rating Scheme should not be null", ratingSchemeId);
                logger.debug("Rating Scheme created with Id :::: {}", ratingSchemeId);
            } else {
                ratingSchemeId = -1;
            }

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("MediatedUsage Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("UsagePool Creation Failed", testEnvBuilder.idForCode(usagePoolO1));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(plan01));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(plan02));
        });
    }

    /*
     * Test for customer level call limit
     */
    @Test
    public void test01customerLevelCallLimit() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = buildAndPersistCustomer(envBuilder, user01, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api,
                    CUSTOMER_LEVEL_FREE_CALLS);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE1 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user01);

            logger.debug(ORDER_CREATED, orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user01));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE1));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user01, testEnv));
            logger.debug(BUILD_CRDS, cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug(MEDIATION_PROCESS, mediationProcess);
            assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(resolveDoneAndBillable(CUSTOMER_LEVEL_FREE_CALLS)), mediationProcess.getDoneAndBillable());
            assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(resolveDoneAndNotBillable(CUSTOMER_LEVEL_FREE_CALLS)), mediationProcess.getDoneAndNotBillable());
            assertEquals(MEDIATION_ERROR_RECORD_COUNT, Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user01));
            long freeCallCounter = Stream.of(order.getOrderLines()).mapToLong(OrderLineWS :: getFreeCallCounter).reduce(0L, Long::sum);
            assertEquals(FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE, CUSTOMER_LEVEL_FREE_CALLS, freeCallCounter);
            assertNotNull(MEDIATION_SHOULD_CREATE_ORDER, order);
            assertNotNull(MEDIATED_ORDER_SHOULD_HAVE_LINES, order.getOrderLines());
            assertEquals(MEDIATED_ORDER_SHOULD_HAVE, 3, order.getOrderLines().length);

        });
    }

    /*
     * Test for plan level call limit
     */
    @Test
    public void test02planLevelCallLimit() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = buildAndPersistCustomer(envBuilder, user02, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api,
                    0);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE2 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user02);

            logger.debug(ORDER_CREATED, orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user02));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE2));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user02, testEnv));
            logger.debug(BUILD_CRDS, cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug(MEDIATION_PROCESS, mediationProcess);
            assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(resolveDoneAndBillable(PLAN_LEVEL_FREE_CALLS_1)), mediationProcess.getDoneAndBillable());
            assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(resolveDoneAndNotBillable(PLAN_LEVEL_FREE_CALLS_1)), mediationProcess.getDoneAndNotBillable());
            assertEquals(MEDIATION_ERROR_RECORD_COUNT, Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user02));
            long freeCallCounter = Stream.of(order.getOrderLines()).mapToLong(OrderLineWS :: getFreeCallCounter).reduce(0L, Long::sum);
            assertEquals(FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE, PLAN_LEVEL_FREE_CALLS_1, freeCallCounter);
            assertNotNull(MEDIATION_SHOULD_CREATE_ORDER, order);
            assertNotNull(MEDIATED_ORDER_SHOULD_HAVE_LINES, order.getOrderLines());
            assertEquals(MEDIATED_ORDER_SHOULD_HAVE, 3, order.getOrderLines().length);

        });
    }

    /*
     * Test for plan level call limit with 2 plans
     */
    @Test
    public void test002planLevelCallLimit() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = buildAndPersistCustomer(envBuilder, user05, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api,
                    0);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap1 = new HashMap<>();
            List<Integer> assets1 = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap1.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets1);

            Map<Integer, BigDecimal> productQuantityMap1 = new HashMap<>();
            productQuantityMap1.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets1.size())));
            productQuantityMap1.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));
            Integer orderId1 = createOrder(SUBSCRIPTION_ORDER_CODE5 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap1, productAssetMap1, user05);
            logger.debug(ORDER_CREATED, orderId1);

            Map<Integer, List<Integer>> productAssetMap2 = new HashMap<>();
            List<Integer> assets2 = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap2.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets2);

            Map<Integer, BigDecimal> productQuantityMap2 = new HashMap<>();
            productQuantityMap2.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets2.size())));
            productQuantityMap2.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd03), BigDecimal.ONE));

            Integer orderId2 = createOrder(SUBSCRIPTION_ORDER_CODE6 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap2, productAssetMap2, user05);

            logger.debug(ORDER_CREATED, orderId2);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user05));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE5));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE6));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user05, testEnv));
            logger.debug(BUILD_CRDS, cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug(MEDIATION_PROCESS, mediationProcess);
            int totalFreeCalls = PLAN_LEVEL_FREE_CALLS_1 + PLAN_LEVEL_FREE_CALLS_2;
            assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(resolveDoneAndBillable(totalFreeCalls)), mediationProcess.getDoneAndBillable());
            assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(resolveDoneAndNotBillable(totalFreeCalls)), mediationProcess.getDoneAndNotBillable());
            assertEquals(MEDIATION_ERROR_RECORD_COUNT, Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user05));
            long freeCallCounter = Stream.of(order.getOrderLines()).mapToLong(OrderLineWS :: getFreeCallCounter).reduce(0L, Long::sum);
            assertEquals(FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE, totalFreeCalls, freeCallCounter);
            assertNotNull(MEDIATION_SHOULD_CREATE_ORDER, order);
            assertNotNull(MEDIATED_ORDER_SHOULD_HAVE_LINES, order.getOrderLines());
            assertEquals(MEDIATED_ORDER_SHOULD_HAVE, 6, order.getOrderLines().length);

        });
    }

    /*
     * Test for company level call limit
     */
    @Test
    public void test03companyLevelCallLimit() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = buildAndPersistCustomer(envBuilder, user03, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api,0);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd02), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE3 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user03);

            logger.debug(ORDER_CREATED, orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user03));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE3));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user03, testEnv));
            logger.debug(BUILD_CRDS, cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug(MEDIATION_PROCESS, mediationProcess);
            assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(resolveDoneAndBillable(COMPANY_LEVEL_FREE_CALLS)), mediationProcess.getDoneAndBillable());
            assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(resolveDoneAndNotBillable(COMPANY_LEVEL_FREE_CALLS)), mediationProcess.getDoneAndNotBillable());
            assertEquals(MEDIATION_ERROR_RECORD_COUNT, Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user03));
            long freeCallCounter = Stream.of(order.getOrderLines()).mapToLong(OrderLineWS :: getFreeCallCounter).reduce(0L, Long::sum);
            assertEquals(FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE, COMPANY_LEVEL_FREE_CALLS, freeCallCounter);
            assertNotNull(MEDIATION_SHOULD_CREATE_ORDER, order);
            assertNotNull(MEDIATED_ORDER_SHOULD_HAVE_LINES, order.getOrderLines());
            assertEquals(MEDIATED_ORDER_SHOULD_HAVE, 3, order.getOrderLines().length);

        });
    }

    /*
     * Test for free call limit as zero for all, customer level = 0, plan level = 0 and company level = 0
     */
    @Test
    public void test04allLevelCallLimitZero() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = buildAndPersistCustomer(envBuilder, user04, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api, 0);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd02), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE4 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user04);

            logger.debug(ORDER_CREATED, orderId);

            setCompanyLevelNumberOfFreeCalls(api, 0);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user04));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE4));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user04, testEnv));
            logger.debug(BUILD_CRDS, cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug(MEDIATION_PROCESS, mediationProcess);
            assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(freeCallsGreaterOrEqualThan30), mediationProcess.getDoneAndBillable());
            assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(freeCallsLessThan30), mediationProcess.getDoneAndNotBillable());
            assertEquals(MEDIATION_ERROR_RECORD_COUNT, Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user04));
            long freeCallCounter = Stream.of(order.getOrderLines()).mapToLong(OrderLineWS :: getFreeCallCounter).reduce(0L, Long::sum);
            assertEquals(FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE, 0, freeCallCounter);
            assertNotNull(MEDIATION_SHOULD_CREATE_ORDER, order);
            assertNotNull(MEDIATED_ORDER_SHOULD_HAVE_LINES, order.getOrderLines());
            assertEquals(MEDIATED_ORDER_SHOULD_HAVE, 3, order.getOrderLines().length);
        }).validate((testEnv, testEnvBuilder) -> {
            setCompanyLevelNumberOfFreeCalls(api, CUSTOMER_LEVEL_FREE_CALLS);
        });
    }

    /*
     * Test for customer level call limit and then plan level after finishing the order
     */
    @Test
    public void test05customerAndPlanLevelCallLimit() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = buildAndPersistCustomer(envBuilder, user06, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api,
                    CUSTOMER_LEVEL_FREE_CALLS);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            List<Integer> assets = getAssetIdByProductId(api, TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, 3);
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, assets);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE1 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user06);

            logger.debug(ORDER_CREATED, orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user06));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE1));
        }).validate((testEnv, testEnvBuilder) -> {
            List<String> cdrs = buildCDR(createCDRAssetMap(user06, testEnv));
            logger.debug(BUILD_CRDS, cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug(MEDIATION_PROCESS, mediationProcess);
            assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(resolveDoneAndBillable(CUSTOMER_LEVEL_FREE_CALLS)), mediationProcess.getDoneAndBillable());
            assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(resolveDoneAndNotBillable(CUSTOMER_LEVEL_FREE_CALLS)), mediationProcess.getDoneAndNotBillable());
            assertEquals(MEDIATION_ERROR_RECORD_COUNT, Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user06));
            long freeCallCounter = Stream.of(order.getOrderLines()).mapToLong(OrderLineWS :: getFreeCallCounter).reduce(0L, Long::sum);
            assertEquals(FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE, CUSTOMER_LEVEL_FREE_CALLS, freeCallCounter);
            assertNotNull(MEDIATION_SHOULD_CREATE_ORDER, order);
            assertNotNull(MEDIATED_ORDER_SHOULD_HAVE_LINES, order.getOrderLines());
            assertEquals(MEDIATED_ORDER_SHOULD_HAVE, 3, order.getOrderLines().length);

        }).validate((testEnv, testEnvBuilder) -> {
            UserWS user = api.getUserWS(testEnvBuilder.idForCode(user06));
            user.setNumberOfFreeCalls(0);
            api.updateUser(user);
            Date billingDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Integer[] invoiceWithDate = api.createInvoiceWithDate(testEnvBuilder.idForCode(user06), billingDate, null, null, false);
            Arrays.stream(invoiceWithDate).forEach( id -> {
                testBuilder.getTestEnvironment().add(api.getInvoiceWS(id).getNumber(), id, id.toString(), api, TestEntityType.INVOICE);
            });
            List<String> cdrs = buildCDR(createCDRAssetMap(user06, testEnv));
            logger.debug(BUILD_CRDS, cdrs);
            triggerMediation(testEnvBuilder,FC_MEDIATION_JOB_NAME, cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug(MEDIATION_PROCESS, mediationProcess);
            assertEquals(MEDIATION_DONE_AND_BILLABLE, Integer.valueOf(resolveDoneAndBillable(PLAN_LEVEL_FREE_CALLS_1)), mediationProcess.getDoneAndBillable());
            assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE, Integer.valueOf(resolveDoneAndNotBillable(PLAN_LEVEL_FREE_CALLS_1)), mediationProcess.getDoneAndNotBillable());
            assertEquals(MEDIATION_ERROR_RECORD_COUNT, Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(user06));
            long freeCallCounter = Stream.of(order.getOrderLines()).mapToLong(OrderLineWS :: getFreeCallCounter).reduce(0L, Long::sum);
            assertEquals(FREE_CALL_COUNTER_FOR_THE_ORDER_LINES_SHOULD_BE, PLAN_LEVEL_FREE_CALLS_1, freeCallCounter);
            assertNotNull(MEDIATION_SHOULD_CREATE_ORDER, order);
            assertNotNull(MEDIATED_ORDER_SHOULD_HAVE_LINES, order.getOrderLines());
            assertEquals(MEDIATED_ORDER_SHOULD_HAVE, 3, order.getOrderLines().length);

        });
    }

    @AfterClass
    public void tearDown() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        FullCreativeUtil.updatePlugin(FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        try {
            if(ratingSchemeId!=-1) {
                logger.debug("Deleting persisted rating scheme.");
                assertTrue(api.deleteRatingScheme(ratingSchemeId));
            }
        } catch(Exception e) {
            fail("Rating Scheme deletion failed.");
        }
        setCompanyLevelNumberOfFreeCalls(api, 0);
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    private int resolveDoneAndNotBillable(int callLimit) {
        return freeCallsLessThan30 >= callLimit ? callLimit : freeCallsLessThan30;
    }

    private int resolveDoneAndBillable(int callLimit) {
        return totalCdrCount - (freeCallsLessThan30 >= callLimit ? callLimit : freeCallsLessThan30);
    }

    private Integer buildAndPersistCustomer(TestEnvironmentBuilder environmentBuilder, String username, Date nextInvoiceDate,
            Integer orderPeriodId, JbillingAPI api, Integer numberOfFreeCalls) {

        UserWS userWS = environmentBuilder.customerBuilder(api)
                    .withUsername(username).addTimeToUsername(false)
                    .build();

        if (null != nextInvoiceDate) {
            DateTime nid = new DateTime(nextInvoiceDate);
            userWS.setMainSubscription(new MainSubscriptionWS(orderPeriodId, nid.getDayOfMonth()));
            userWS.setNextInvoiceDate(nextInvoiceDate);
            userWS.setNumberOfFreeCalls(numberOfFreeCalls);
            api.updateUser(userWS);
        }

        return userWS.getId();
    }

    private void triggerMediation(TestEnvironmentBuilder envBuilder, String jobConfigName, List<String> cdr) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer configId = getMediationConfiguration(api, jobConfigName);
        api.processCDR(configId, cdr);
    }

    private Map<String, List<String>> createCDRAssetMap(String userName, TestEnvironment testEnvironment) {
        List<String> identifiers = getAssetIdentifiers(userName, testEnvironment);
        Map<String, List<String>> assetCDRMap = new HashMap<>();
        assetCDRMap.put("Inbound", identifiers);
        return assetCDRMap;
    }

    private List<String> buildCDR(Map<String,List<String>> assetCDRMap) {
        List<String> cdrs = new ArrayList<>();
        // 16 records < 30
        // 14 records >= 30
        List<Integer> callDurationList = Arrays.asList(1, 5, 5, 7, 10, 11, 12, 13, 15, 16, 20, 25, 27, 29, 29, 29,
                30, 35, 40, 120, 50, 55, 180, 45, 60, 55, 35, 36, 39, 40);
        freeCallsLessThan30 = 0;
        freeCallsGreaterOrEqualThan30 = 0;
        totalCdrCount = 0;
        assetCDRMap.forEach((k, v) -> {
            v.stream().forEach(identifier -> {
                callDurationList.stream().forEach(callDuration -> {
                    cdrs.add(String.format(CDR_FORMAT, UUID.randomUUID().toString(), identifier,
                            k, identifier, callDuration));
                    if(callDuration < 30) {
                        freeCallsLessThan30++;
                    } else {
                        freeCallsGreaterOrEqualThan30++;
                    }
                });
            });
        });
        totalCdrCount = cdrs.size();
        return cdrs;
    }

    private List<String> getAssetIdentifiers(String userName, TestEnvironment envBuilder) {
        if( null == userName || userName.isEmpty()) {
            return Collections.emptyList();
        }

        OrderWS [] orders = api.getUserSubscriptions(envBuilder.idForCode(userName));
        List<String> identifiers = new ArrayList<>();
        for (OrderWS order : orders) {
            for (OrderLineWS line : order.getOrderLines()) {
                for (Integer id : line.getAssetIds()) {
                    identifiers.add(api.getAsset(id).getIdentifier());
                }
            }
        }
        return identifiers;
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
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

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
            Integer periodId, Integer itemId, List<Integer> usagePools, Integer numberOfFreeCalls, PlanItemWS... planItems) {
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withNumberOfFreeCalls(numberOfFreeCalls)
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

    private Map<Integer, BigDecimal> buildProductQuantityEntry(Integer productId, BigDecimal quantity){
        return Collections.singletonMap(productId, quantity);
    }

    private List<Integer> getAssetIdByProductId(JbillingAPI api, Integer productId, int noOfAsset) {
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(noOfAsset);
        criteria.setOffset(0);
        criteria.setSort("id");
        criteria.setTotal(-1);
        criteria.setFilters(new BasicFilter[]{basicFilter});

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(productId, criteria);
        assertNotNull("No available asset found for product "+productId, assetsResult);
        AssetWS[] availableAssets = assetsResult.getObjects();
        assertTrue("No assets found for product .", null != availableAssets && availableAssets.length != 0);
        return Arrays.stream(availableAssets)
                .map(AssetWS::getId)
                .collect(Collectors.toList());
    }

    private Integer createOrder(String code,Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode) {
        this.testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
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
                        if(null!=productAssetMap && !productAssetMap.isEmpty()
                                && productAssetMap.containsKey(line.getItemId())) {
                            List<Integer> assets = productAssetMap.get(line.getItemId());
                            line.setAssetIds(assets.toArray(new Integer[0]));
                            line.setQuantity(assets.size());
                        }
                        return line;
                    }).collect(Collectors.toList());

            envBuilder.orderBuilder(api)
                .withCodeForTests(code)
                .forUser(envBuilder.idForCode(userCode))
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withEffectiveDate(activeSince)
                .withPeriod(orderPeriodId)
                .withProrate(prorate)
                .withOrderLines(lines)
                .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID)
                .build();
        }).test((testEnv, envBuilder) ->
            assertNotNull(ORDER_CREATION_ASSERT, envBuilder.idForCode(code))
        );
        return testBuilder.getTestEnvironment().idForCode(code);
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value =  new MetaFieldBuilder()
                                .name(name)
                                .dataType(dataType)
                                .entityType(entityType)
                                .primary(true)
                                .build();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);

    }

    private void setCompanyLevelMetaField(TestEnvironment environment) {
        JbillingAPI api = environment.getPrancingPonyApi();
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));

        values.add(new MetaFieldValueWS(LIVE_ANSWER_META_FIELD_NAME, null, DataType.STRING, true,
                String.valueOf(FullCreativeTestConstants.INBOUND_USAGE_PRODUCT_ID)));
        int entityId = api.getCallerCompanyId();
        values.forEach(value -> {
            value.setEntityId(entityId);
        });

        company.setTimezone(company.getTimezone());
        company.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updateCompany(company);

    }

    private void setCompanyLevelNumberOfFreeCalls(JbillingAPI api, int numberOfFreeCalls) {
        CompanyWS company = api.getCompany();
        company.setNumberOfFreeCalls(numberOfFreeCalls);
        company.setTimezone(company.getTimezone());
        api.updateCompany(company);
    }

    private Integer buildAndPersistMediationConfiguration(TestEnvironmentBuilder envBuilder, JbillingAPI api, String configName, String jobLauncherName) {
        return envBuilder.mediationConfigBuilder(api)
                  .withName(configName)
                  .withLauncher(jobLauncherName)
                  .build();
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

}
