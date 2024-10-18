package com.sapienter.jbilling.server.spc;

import com.sapienter.jbilling.resources.CancelOrderInfo;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.commons.lang.time.DateUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

@Test(groups = "agl", testName = "agl.SPCCustomerUsagePoolEvaluationTest")
public class SPCCustomerUsagePoolEvaluationTest extends SPCBaseConfiguration {

    private static final String ASSET01 = "1111111111";
    private static final String ASSET02 = "1111111112";
    private static final String ASSET03 = "1111111113";
    private static final String ASSET04 = "1111111114";
    private static final String TEST_CUSTOMER_OPTUS_1 = "Test-SPC-1";
    private static final String TEST_CUSTOMER_OPTUS_2 = "Test-SPC-2";
    private static final String TEST_CUSTOMER_OPTUS_3 = "Test-SPC-3";
    private static final String TEST_CUSTOMER_OPTUS_4 = "Test-SPC-4";
    private static final String OPTUS_PLAN = "SPCMO-03";
    private static final int BILLING_TYPE_MONTHLY = 1;
    private static final String ORDER_ASSERT = "Order Should not null";
    private static final String[] OPTUS_PLAN_PoolQuantity = {"200.0000000000", "1024.0000000000", "1024.0000000000", "1024.0000000000"};

    List<AssetWS> assetWSs = new ArrayList<>();
    UserWS spcOptusUserWS_1;
    UserWS spcOptusUserWS_2;
    UserWS spcOptusUserWS_3;
    UserWS spcOptusUserWS_4;
    Map<Integer, BigDecimal> productQuantityMapOptus = new HashMap<>();
    LocalDate currentLocalDate = LocalDate.now(ZoneId.systemDefault());

    @BeforeClass
    public void beforeClass() {

        if (null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        testBuilder.given(envBuilder -> {
            logger.debug("SPCCustomerUsagePoolEvaluationTest.beforeClass : {} ", testBuilder);

            String optusPlanDescription = "Green 12M Optus 4G $16 Double Data 6GB";
            String planTypeOptus = "Optus";
            String optusPlanServiceType = "Mobile";
            BigDecimal optusPlanPrice = new BigDecimal("50.000");
            // 1024×1024×1024×(200÷1024) 200 MB
            BigDecimal optusPlanUsagePoolQuantity = new BigDecimal("209715200");
            BigDecimal optusPlanBoostQuantity = new BigDecimal("1024");
            Integer optusPlanBoostCount = new Integer("3");

            String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

            Map<String, String> optusPlanMetaFieldCodeMap = new HashMap<>();
            optusPlanMetaFieldCodeMap.put("USAGE_POOL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("USAGE_POOL_GL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("COST_GL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("REVENUE_GL_CODE", "410026-150");

            logger.debug("************************ Start creating plan");
            Integer optusPlanId = CreatePlanUtility.createPlan(api, OPTUS_PLAN, planTypeOptus, optusPlanServiceType,
                    optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                    optusPlanBoostCount, optusPlanBoostQuantity);
            logger.info("Optus PlanId: {}", optusPlanId);

            buildAndPersistCreditPoolDataTableRecord(optusPlanId.toString(), "OM:NEZ", "50", api.getItemID("MC").toString(), "10",
                    "credit_pool");
        });
    }

    /**
     * - Pre condition, test will be based on dates from 6 Month before from current date. So for example if current date is
     * 1st July 2021, we are going to create customer with active since date equals to Jan 1st 2021
     * <p>
     * 1 - Create Monthly customer PRE PAID type with next invoice date as Jan 1st 2021
     * 2 - Create Monthly Prepaid order with active since Jan 1st 2021
     * 3 - Verify pools with cycle start date equals = Jan 1st 2021 and cycle end date = Jan 31st 2021
     * 4 - Update customer NID to Feb 1st 2021
     * 5 - Run Billing Process one day before customer NID , so run on the Jan 31st 2021
     * 6 - Verify that the customer usage pools from step 3 remains untouched with cycle start date = Jan 1st 2021 and cycle end date = Jan 31st 2021
     */
    @Test(priority = 1)
    public void testPrePaidCustomerUsagePoolNotRenewedOneDayBefore() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Current Local Cate: {}", currentLocalDate);
                Date previousMonthDate = getLocalDateAsDate(currentLocalDate.minusMonths(6));

                logger.debug("Testing Month Date: {}", previousMonthDate);
                Date nextInvoiceDate = previousMonthDate;
                Date activeSinceDate = previousMonthDate;

                //Create Monthly Pre Paid SPC Customer type
                spcOptusUserWS_1 = getSPCTestUserWS(envBuilder, TEST_CUSTOMER_OPTUS_1 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);

                Integer customerId = spcOptusUserWS_1.getCustomerId();
                logger.debug("User created: {}, customerId: {}, NID: {}", spcOptusUserWS_1.getId(), customerId, spcOptusUserWS_1.getNextInvoiceDate());
                assertNotNull("Customer should not be null", customerId);

                Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET01, "asset-01");
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanByInternalNumber(OPTUS_PLAN, api.getCallerCompanyId());
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset("TestOrder_3", spcOptusUserWS_1.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                logger.debug("Plan Order created {}", orderId);

                spcOptusUserWS_1.setNextInvoiceDate(getLocalDateAsDate(currentLocalDate.minusMonths(5)));
                logger.debug("## Updating NID to next period {}", spcOptusUserWS_1.getNextInvoiceDate());
                api.updateUser(spcOptusUserWS_1);

                Date cycleEndDate = getLocalDateAsDate(currentLocalDate.minusMonths(5).minusDays(1));

                OrderWS subscriptionOrder = api.getOrder(orderId);
                assertNotNull(ORDER_ASSERT, subscriptionOrder);

                logger.debug("Before billing Run, CUPs cycleStartDate: {}, cycleEndDate: {}", subscriptionOrder.getActiveSince(), cycleEndDate);

                validateCustomerUsagePoolsByCustomer(customerId, subscriptionOrder.getId(), subscriptionOrder.getActiveSince(), cycleEndDate, OPTUS_PLAN_PoolQuantity, 4, 4);

                Date runDate = getLocalDateAsDate(currentLocalDate.minusMonths(5).minusDays(1));
                updateRunDateBillingProcessConfiguration(runDate);
                logger.debug("Executing billing process one day before customer NID, on date {}", runDate);
                executeBillingProcessByRunDate(runDate);
            }).validate((testEnv, testEnvBuilder) -> {

                OrderWS subscriptionOrder = api.getLatestOrder(spcOptusUserWS_1.getId());
                assertNotNull(ORDER_ASSERT, subscriptionOrder);

                Date cycleStartDate = getLocalDateAsDate(currentLocalDate.minusMonths(6));
                Date cycleEndDate = getLocalDateAsDate(currentLocalDate.minusMonths(5).minusDays(1));
                logger.debug("After billing Run, verify pools remain unchanged with cycleStartDate: {}, cycleEndDate: {}", cycleStartDate, cycleEndDate);
                logger.debug("Validating customer usage pools for customer {}", spcOptusUserWS_1.getCustomerId());

                validateCustomerUsagePoolsByCustomer(spcOptusUserWS_1.getCustomerId(), subscriptionOrder.getId(), cycleStartDate, cycleEndDate, OPTUS_PLAN_PoolQuantity, 4, 4);
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(spcOptusUserWS_1.getId());
        }
    }

    /**
     * - Pre condition, test will be based on dates from 5 Month before from current date. So for example if current date is
     * July 1st 2021, we are going to create customer with active since date equals to Feb 1st 2021
     * <p>
     * 1 - Create Monthly customer POST PAID type with next invoice date as Feb 1st 2021
     * 2 - Create Monthly Prepaid order with active since Feb 1st 2021
     * 3 - Verify pools with cycle start date equals = Feb 1st 2021 and cycle end date = Feb 28th 2021
     * 4 - Update customer NID to March 1st 2021
     * 5 - Run Billing Process one day before customer NID , so run on the Feb 28th 2021
     * 6 - Verify that the customer usage pools from step 3 are renewed with cycle start date = March 1st 2021 to cycle end date = March 31st 2021
     */
    @Test(priority = 2)
    public void testPostPaidCustomerUsagePoolRenewedOneDayBefore() {
        try {
            testBuilder.given(envBuilder -> {

                logger.debug("currentLocalDate: {}", currentLocalDate);
                Date previousMonthDate = getLocalDateAsDate(currentLocalDate.minusMonths(5));

                logger.debug("Testing Month Date: {}", previousMonthDate);
                Date nextInvoiceDate = previousMonthDate;
                Date activeSinceDate = previousMonthDate;

                //Create Monthly Post Paid SPC Customer type
                spcOptusUserWS_2 = getSPCTestUserWS(envBuilder, TEST_CUSTOMER_OPTUS_2 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);

                Integer customerId = spcOptusUserWS_2.getCustomerId();
                logger.debug("User created: {}, customerId: {}, NID: {}", spcOptusUserWS_2.getId(), customerId, spcOptusUserWS_2.getNextInvoiceDate());
                assertNotNull("Customer should not be null", customerId);

                Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02, "asset-01");
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanByInternalNumber(OPTUS_PLAN, api.getCallerCompanyId());
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset("TestOrder_2", spcOptusUserWS_2.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                logger.debug("Plan Order created {}", orderId);

                spcOptusUserWS_2.setNextInvoiceDate(getLocalDateAsDate(currentLocalDate.minusMonths(4)));
                logger.debug("## Updating NID to next period {}", spcOptusUserWS_2.getNextInvoiceDate());
                api.updateUser(spcOptusUserWS_2);

                Date cycleEndDate = getLocalDateAsDate(currentLocalDate.minusMonths(4).minusDays(1));

                OrderWS subscriptionOrder = api.getOrder(orderId);
                assertNotNull(ORDER_ASSERT, subscriptionOrder);

                logger.debug("Before billing Run, CUPs cycleStartDate: {}, cycleEndDate: {}", subscriptionOrder.getActiveSince(), cycleEndDate);

                validateCustomerUsagePoolsByCustomer(spcOptusUserWS_2.getCustomerId(), subscriptionOrder.getId(), subscriptionOrder.getActiveSince(), cycleEndDate, OPTUS_PLAN_PoolQuantity, 4, 4);

                Date runDate = getLocalDateAsDate(currentLocalDate.minusMonths(4).minusDays(1));
                updateRunDateBillingProcessConfiguration(runDate);
                logger.debug("Executing billing process one day before customer NID, on date {}", runDate);
                executeBillingProcessByRunDate(runDate);

            }).validate((testEnv, testEnvBuilder) -> {
                OrderWS subscriptionOrder = api.getLatestOrder(spcOptusUserWS_2.getId());
                assertNotNull(ORDER_ASSERT, subscriptionOrder);
                Date cycleStartDate = getLocalDateAsDate(currentLocalDate.minusMonths(5));
                Date cycleEndDate = getLocalDateAsDate(currentLocalDate.minusMonths(4).minusDays(1));
                logger.debug("After billing Run, verify new pools created with cycleStartDate: {}, cycleEndDate: {}", cycleStartDate, cycleEndDate);
                logger.debug("Validating customer usage pools for customer {}", spcOptusUserWS_2.getCustomerId());

                validateCustomerUsagePoolsByCustomer(spcOptusUserWS_2.getCustomerId(), subscriptionOrder.getId(), cycleStartDate, cycleEndDate, OPTUS_PLAN_PoolQuantity, 4, 8);
            });
        } finally {
            // We do not clea data here as we need it in order to validate duplicate pools are not created when evaluation
            // is executed more than one time in the same day.
        }
    }

    /**
     * - Pre condition, test testPostPaidCustomerUsagePoolRenewedOneDayBefore has to be executed, as the data generated will be used
     *  to evaluate to valuate if evaluation of cups more than one time in the same day does not generate duplicate pools
     *
     */
    @Test(priority = 3)
    public void testPoolsAreNotDuplicatedWhenEvaluatedSameDaySeveralTimes() {
        try {
            testBuilder.given(envBuilder -> {
                CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(spcOptusUserWS_2.getCustomerId());
                logger.debug("Verifying customer usage pool count for customer {} is {} before trigger evaluation task twice", spcOptusUserWS_2.getCustomerId(), 8);
                assertEquals("Customer Usage Pool Map count does not match", customerUsagePools.length, 8);
                Date runDate = getLocalDateAsDate(currentLocalDate.minusMonths(4).minusDays(1));
                logger.debug("Triggering customer usage pool evaluation for date {}", runDate);
                api.triggerCustomerUsagePoolEvaluation(1, runDate);
                sleep(5000);
            }).validate((testEnv, testEnvBuilder) -> {
                logger.debug("Verifying customer usage pool count for customer {} is {} after trigger evaluation task for the second time", spcOptusUserWS_2.getCustomerId(), 8);
                CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(spcOptusUserWS_2.getCustomerId());
                assertEquals("Customer Usage Pool Map count does not match", customerUsagePools.length, 8);
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(spcOptusUserWS_2.getId());
        }
    }

    /**
     * - Pre condition, test will be based on dates from 4 Month before from current date. So for example if current date is
     * 1st July 2021, we are going to create customer with active since date equals to March 1st 2021
     * <p>
     * 1 - Create Monthly customer PRE PAID type with next invoice date as March 1st 2021
     * 2 - Create Monthly Prepaid order with active since March 1st 2021
     * 3 - Verify pools with cycle start date equals = March 1st 2021 and cycle end date = March 31st 2021
     * 4 - Update customer NID to April 1st 2021
     * 5 - Run Billing Process on customer NID, so run on the April 1st 2021
     * 6 - Verify that the customer usage pools from step 3 are updated with cycle start date = April 1st 2021 and cycle end date = April 30th 2021
     */
    @Test(priority = 4)
    public void testPrePaidCustomerUsagePoolRenewedOnRunDate() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Current Local Date: {}", currentLocalDate);
                Date previousMonthDate = getLocalDateAsDate(currentLocalDate.minusMonths(4));

                logger.debug("Testing Month Date: {}", previousMonthDate);
                Date nextInvoiceDate = previousMonthDate;
                Date activeSinceDate = previousMonthDate;

                //Create Monthly Pre Paid SPC Customer type
                spcOptusUserWS_3 = getSPCTestUserWS(envBuilder, TEST_CUSTOMER_OPTUS_3 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);

                Integer customerId = spcOptusUserWS_3.getCustomerId();
                logger.debug("User created: {}, customerId: {}, NID: {}", spcOptusUserWS_3.getId(), customerId, spcOptusUserWS_3.getNextInvoiceDate());
                assertNotNull("Customer should not be null", customerId);


                Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET03, "asset-03");
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanByInternalNumber(OPTUS_PLAN, api.getCallerCompanyId());
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset("TestOrder_3", spcOptusUserWS_3.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                logger.debug("Plan Order created {}", orderId);

                spcOptusUserWS_3.setNextInvoiceDate(getLocalDateAsDate(currentLocalDate.minusMonths(3)));
                logger.debug("## Updating NID to next period {}", spcOptusUserWS_3.getNextInvoiceDate());
                api.updateUser(spcOptusUserWS_3);

                Date cycleEndDate = getLocalDateAsDate(currentLocalDate.minusMonths(3).minusDays(1));

                OrderWS subscriptionOrder = api.getOrder(orderId);
                assertNotNull(ORDER_ASSERT, subscriptionOrder);

                logger.debug("Before billing Run, CUPs cycleStartDate: {}, cycleEndDate: {}", subscriptionOrder.getActiveSince(), cycleEndDate);

                validateCustomerUsagePoolsByCustomer(customerId, subscriptionOrder.getId(), subscriptionOrder.getActiveSince(), cycleEndDate, OPTUS_PLAN_PoolQuantity, 4, 4);

                Date runDate = getLocalDateAsDate(currentLocalDate.minusMonths(3));
                updateRunDateBillingProcessConfiguration(runDate);
                logger.debug("Executing billing process with run date equal to customer next invoice date, on date {}", runDate);
                executeBillingProcessByRunDate(runDate);
            }).validate((testEnv, testEnvBuilder) -> {
                OrderWS subscriptionOrder = api.getLatestOrder(spcOptusUserWS_3.getId());
                assertNotNull(ORDER_ASSERT, subscriptionOrder);
                Date cycleStartDate = getLocalDateAsDate(currentLocalDate.minusMonths(3));
                Date cycleEndDate = getLocalDateAsDate(currentLocalDate.minusMonths(2).minusDays(1));
                logger.debug("After billing Run, CUPs are updated - new cycleStartDate: {}, cycleEndDate: {}", cycleStartDate, cycleEndDate);
                logger.debug("Validating customer usage pools for customer {}", spcOptusUserWS_3.getCustomerId());

                validateCustomerUsagePoolsByCustomer(spcOptusUserWS_3.getCustomerId(), subscriptionOrder.getId(), cycleStartDate, cycleEndDate, OPTUS_PLAN_PoolQuantity, 4, 4);
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(spcOptusUserWS_3.getId());
        }
    }

    /**
     * - Pre condition, test will be based on dates from 3 Month before from current date. So for example if current date is
     * 1st July 2021, we are going to create customer with active since date equals to April 1st 2021
     * <p>
     * 1 - Make sure Billing Process was executed for current date minus 3 months - For example March 30th
     * 2 - Create Monthly customer POST PAID type with next invoice date as April 1st 2021
     * 3 - Create Monthly Prepaid order with active since March 31st 2021
     * 4 - Verify pools for current cycle with start date equals = March 31st 2021 and cycle end date = March 31st 2021
     * 5 - Verify pools for next cycle with start date equals April 1st 2021 and cycle en date = April 30th 2021
     */
    @Test(priority = 5)
    public void testNextCyclePoolsCreatedWhenNewOrderSubscribeOnLastDayAfterBPRun() {

        try {
            testBuilder.given(envBuilder -> {

                logger.debug("Current Local Date: {}", currentLocalDate);
                Date previousMonthDate = getLocalDateAsDate(currentLocalDate.minusMonths(3));

                logger.debug("Testing Month Date: {}", previousMonthDate);
                Date nextInvoiceDate = previousMonthDate;
                Date activeSinceDate = DateUtils.addDays(previousMonthDate, -1);

                //Create Monthly Pre Paid SPC Customer type
                spcOptusUserWS_4 = getSPCTestUserWS(envBuilder, TEST_CUSTOMER_OPTUS_4 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_POST_PAID, AUSTRALIA_POST, CC);

                Integer customerId = spcOptusUserWS_4.getCustomerId();
                logger.debug("User created: {}, customerId: {}, NID: {}", spcOptusUserWS_4.getId(), customerId, spcOptusUserWS_4.getNextInvoiceDate());
                assertNotNull("Customer should not be null", customerId);


                Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET04, "asset-04");
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanByInternalNumber(OPTUS_PLAN, api.getCallerCompanyId());
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);

                Date lastBillingProcessDate = api.getBillingProcess(api.getLastBillingProcess()).getBillingDate();
                logger.debug("Last Billing Process Date is {}", lastBillingProcessDate);

                Integer orderId = createOrderWithAsset("TestOrder_4", spcOptusUserWS_4.getId(), activeSinceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                logger.debug("Plan Order created {} with active since {}", orderId, activeSinceDate);

            }).validate((testEnv, testEnvBuilder) -> {
                logger.debug("Validating that current and next cycle pools are being created");

                CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(spcOptusUserWS_4.getCustomerId());
                logger.debug("Customer Usage Pools length", customerUsagePools.length);

                assertTrue("Customer Usage Pool creation for current and next cycle failed", customerUsagePools.length == 8);

                int currentCyclePoolCount = 0;
                int nextCyclePoolCount = 0;

                Date currentCycleStartDate = DateUtils.addDays(getLocalDateAsDate(currentLocalDate.minusMonths(3)), -1);
                Date currentCycleEndDate = DateUtils.addMilliseconds(getLocalDateAsDate(currentLocalDate.minusMonths(3)), -1);
                logger.debug("Current cycle start date = {} , end date = {}", currentCycleStartDate, currentCycleEndDate);

                Date nextCycleStartDate = getLocalDateAsDate(currentLocalDate.minusMonths(3));
                Date nextCycleEndDate = DateUtils.addMonths(nextCycleStartDate, 1);
                nextCycleEndDate = DateUtils.addMilliseconds(nextCycleEndDate, -1);
                logger.debug("Next cycle start date = {} , end date = {}", nextCycleStartDate, nextCycleEndDate);

                for (CustomerUsagePoolWS cup: customerUsagePools) {
                    logger.debug("Evaluating customer usage pools with cycle start date {} and cycle end date {}", cup.getCycleStartDate(), cup.getCycleEndDate());
                    if (cup.getCycleStartDate().equals(currentCycleStartDate) && cup.getCycleEndDate().equals(currentCycleEndDate)){
                        currentCyclePoolCount ++;
                    }
                    if (cup.getCycleStartDate().equals(nextCycleStartDate) && cup.getCycleEndDate().equals(nextCycleEndDate)){
                        nextCyclePoolCount ++;
                    }
                }
                logger.debug("current pool count {} , next cycle pool count {}", currentCyclePoolCount, nextCyclePoolCount);

                assertEquals("Customer usage pools for the current cycle should be created", 4, currentCyclePoolCount);
                assertEquals("Customer usage pools for the next cycle should be created", 4, nextCyclePoolCount);
            });
        } finally {
            // Do not clean data as it is used in the next test
        }
    }

    /**
     * Use Data created from previous test
     * - Pre condition, test will be based on dates from 3 Month before from current date. So for example if current date is
     * 1st July 2021, we are going to create customer with active since date equals to April 1st 2021
     * Test data used from previous test.
     * <p>
     * 1 - Verify pools for current cycle with start date equals = March 31st 2021 and cycle end date = March 31st 2021
     * 2 - Verify pools for next cycle with start date equals April 1st 2021 and cycle en date = April 30th 2021
     * 6 - Cancel the order with active until date equals to active since date
     * 7 - Verify next cycle pools are expired, current cycle pool remains untouched and no new pools are created
     */
    @Test(priority = 6, dependsOnMethods = "testNextCyclePoolsCreatedWhenNewOrderSubscribeOnLastDayAfterBPRun")
    public void testCancelBackDatedOrderWithActiveUntilSameAsActiveSince() {

        try {
            testBuilder.given(envBuilder -> {
                CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(spcOptusUserWS_4.getCustomerId());
                logger.debug("Customer Usage Pools length");

                logger.debug("Listing existing pools before order cancellation, size {}", customerUsagePools.length);
                for (CustomerUsagePoolWS cup: customerUsagePools) {
                    logger.debug("Existing customer usage pools with cycle start date {} and cycle end date {}", cup.getCycleStartDate(), cup.getCycleEndDate());
                }

                OrderWS order = api.getLatestOrder(spcOptusUserWS_4.getId());
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(order.getId());
                cancelOrderInfo.setActiveUntil(order.getActiveSince());
                logger.debug("Canceling order with active until date equals to order active since");
                api.cancelServiceOrder(cancelOrderInfo);

            }).validate((testEnv, testEnvBuilder) -> {
                // TODO        and then same with active until less
                Date currentCycleStartDate = DateUtils.addDays(getLocalDateAsDate(currentLocalDate.minusMonths(3)), -1);
                Date currentCycleEndDate = DateUtils.addMilliseconds(getLocalDateAsDate(currentLocalDate.minusMonths(3)), -1);
                logger.debug("Current cycle start date = {} , end date = {}", currentCycleStartDate, currentCycleEndDate);

                CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(spcOptusUserWS_4.getCustomerId());
                logger.debug("Customer Usage Pools length", customerUsagePools.length);

                int currentCyclePoolCount = 0;

                for (CustomerUsagePoolWS cup: customerUsagePools) {
                    logger.debug("Evaluating customer usage pools with cycle start date {} and cycle end date {}", cup.getCycleStartDate(), cup.getCycleEndDate());
                    if (cup.getCycleStartDate().equals(currentCycleStartDate) && cup.getCycleEndDate().equals(currentCycleEndDate)){
                        currentCyclePoolCount ++;
                    }
                }
                // Epired pools are not being fetch, but before cancellation there were 8 active customer usage pools and after there are only 4
                logger.debug("current pool count {} , expired pool count {}", currentCyclePoolCount, 4);

                assertEquals("Customer usage pools for the current cycle should not be updated", 4, currentCyclePoolCount);
                assertEquals("Active Customer usage pools count are", 4, customerUsagePools.length);

            });
        } finally {
            // Do not clean data as it is used in the next test
        }
    }


    /**
     * Use Data created from previous test
     * - Pre condition, test will be based on dates from 3 Month before from current date. So for example if current date is
     * 1st July 2021, we are going to create customer with active since date equals to April 1st 2021
     * Test data used from previous test.
     * <p>
     * 1 - Verify pools for current cycle with start date equals = March 31st 2021 and cycle end date = March 31st 2021
     * 6 - Cancel the order with active until date less than active since date
     * 7 - Verify current cycle pools are expired, no active pools should exist after cancellation with active until less than active since
     */
    @Test(priority = 7, dependsOnMethods = "testCancelBackDatedOrderWithActiveUntilSameAsActiveSince")
    public void testCancelBackDatedOrderWithActiveUntilLessThanActiveSince() {

        try {
            testBuilder.given(envBuilder -> {
                CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(spcOptusUserWS_4.getCustomerId());
                logger.debug("Customer Usage Pools length");

                logger.debug("Listing existing pools before order updating order cancelation date, size {}", customerUsagePools.length);
                for (CustomerUsagePoolWS cup: customerUsagePools) {
                    logger.debug("Existing customer usage pools with cycle start date {} and cycle end date {}", cup.getCycleStartDate(), cup.getCycleEndDate());
                }

                OrderWS order = api.getLatestOrder(spcOptusUserWS_4.getId());
                CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
                cancelOrderInfo.setOrderId(order.getId());
                cancelOrderInfo.setActiveUntil(DateUtils.addDays(order.getActiveSince(), -1));
                logger.debug("Canceling order with active until date less than the order active since");
                api.cancelServiceOrder(cancelOrderInfo);

            }).validate((testEnv, testEnvBuilder) -> {

                CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(spcOptusUserWS_4.getCustomerId());
                logger.debug("Customer Usage Pools length", customerUsagePools.length);

                assertEquals("Customer usage pools for the current cycle should be expired", 0, customerUsagePools.length);
                assertEquals("Should not be any active customer usage pools", 0, customerUsagePools.length);

            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(spcOptusUserWS_4.getId());
        }
    }

    private void validateCustomerUsagePoolsByCustomer(Integer customerId, Integer orderId,
                                                      Date cycleStartDate, Date cycleEndDate, String[] poolQuantity, int orderPoolCount, int totalPoolCount) {
        CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(customerId);
        assertNotNull("Customer UsagePool creation failed", customerUsagePools);
        assertTrue("Customer Usage Pool Map count should greater than zero", customerUsagePools.length > 0);
        assertTrue("Customer Usage Pool Map count not matched", customerUsagePools.length == totalPoolCount);

        List<CustomerUsagePoolWS> lessPrecedencePools = Arrays.stream(customerUsagePools)
                .filter(customerUsagePool -> orderId.equals(customerUsagePool.getOrderId()))
                .filter(customerUsagePool -> customerUsagePool.getCycleStartDate().equals(cycleStartDate))
                .collect(Collectors.toList());
        assertTrue("Customer Usage Pool renewal creation failed", lessPrecedencePools.size() == orderPoolCount);
        lessPrecedencePools.sort(Comparator.comparing(p1 -> p1.getUsagePool().getPrecedence()));

        int i = 0;
        for (CustomerUsagePoolWS customerUsagePool : lessPrecedencePools) {
            if (i > orderPoolCount) {
                assertEquals("Customer usage pool count mis-matched: ", i, orderPoolCount);
            }
            if (orderId.equals(customerUsagePool.getOrderId())) {
                validateCustomerUsagePool(customerUsagePool, poolQuantity[i],
                        cycleStartDate, cycleEndDate);
            }
            i = ++i;
        }
    }

    private void validateCustomerUsagePool(CustomerUsagePoolWS usagePool, String quantity, Date start, Date end) {
        assertNotNull("Customer UsagePool creation failed", usagePool);
        assertEquals("Customer Usage pool quantity ", quantity, usagePool.getInitialQuantity());
        assertEquals("Expected Cycle start date of customer usage pool: "
                , TestConstants.DATE_FORMAT.format(start)
                , TestConstants.DATE_FORMAT.format(usagePool.getCycleStartDate()));
        assertEquals("Expected Cycle end date of customer usage pool: "
                , TestConstants.DATE_FORMAT.format(end)
                , TestConstants.DATE_FORMAT.format(usagePool.getCycleEndDate()));
    }

    @AfterClass
    private void teardown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
    }

    private void updateRunDateBillingProcessConfiguration(Date runDate) {
        // set the configuration to something we are sure about
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        logger.debug("Billing runDate: {}", runDate);
        config.setNextRunDate(runDate);
        config.setPeriodUnitId(3); // 3-Daily , 1-monthly
        config.setRetries(null);
        config.setDaysForRetry(1);
        config.setGenerateReport(0);
        config.setAutoPaymentApplication(0);
        config.setDfFm(0);
        config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateValue(1);
        config.setInvoiceDateProcess(0);
        config.setMaximumPeriods(1);
        config.setOnlyRecurring(1);
        config.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());

        api.createUpdateBillingProcessConfiguration(config);
    }

    private void executeBillingProcessByRunDate(Date runDate) {
        try {
            logger.debug("Billing runDate: {}", runDate);
            api.triggerBilling(runDate);

            BillingProcessWS billingProcess = api.getBillingProcess(api.getLastBillingProcess());
            assertEquals("Billing process configuration failed", runDate, billingProcess.getBillingDate());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception:" + e);
        }
    }

}
