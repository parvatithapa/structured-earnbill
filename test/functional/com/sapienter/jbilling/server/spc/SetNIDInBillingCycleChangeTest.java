package com.sapienter.jbilling.server.spc;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserResourceHelperService;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.testng.AssertJUnit.assertEquals;

@Test(groups = "agl", testName = "agl.SetNIDInBillingCycleChangeTest")
public class SetNIDInBillingCycleChangeTest extends SPCBaseConfiguration {

    private static final String optusPlan = "SPCMO-1393";
    private static final int BILLIING_TYPE_MONTHLY = 1;
    private static final int BILLIING_TYPE_MONTHLY_POST_PAID = 2;
    private static final String TEST_CUSTOMER1 = "Test1-1393";
    private static final String TEST_CUSTOMER2 = "Test2-1393";
    private static final String TEST_CUSTOMER3 = "Test3-1393";
    private static final String TEST_CUSTOMER4 = "Test4-1393";
    private static final String TEST_CUSTOMER5 = "Test5-1393";
    private static final String TEST_CUSTOMER6 = "Test6-1393";
    private static final String TEST_CUSTOMER7 = "Test7-1393";
    private static final String TEST_CUSTOMER8 = "Test8-1393";
    private static final String TEST_CUSTOMER9 = "Test9-1393";
    private static final String TEST_CUSTOMER10 = "Test10-1393";
    private static final String TEST_CUSTOMER11 = "Test11-1393";
    private static final String TEST_CUSTOMER12 = "Test12-1393";
    private static final String TEST_CUSTOMER13 = "Test13-1393";
    private static final String TEST_CUSTOMER14 = "Test14-1393";
    private static final String USER_CREATION_ASSERT = "User Creation Failed";
    private static final String PLAN_CREATION_ASSERT = "Plan Creation Failed";
    private static final String ORDER_CREATION_ASSERT = "Order Creation Failed";
    private static final String ORDER_ASSERT = "Order Should not null";
    private static final String USER_ASSERT = "User Created {}";
    String assetIdentifier1 = "04"+randomLong(10000000L, 99999999L);
    String assetIdentifier2 = "04"+randomLong(10000000L, 99999999L);
    String assetIdentifier3 = "04"+randomLong(10000000L, 99999999L);
    String assetIdentifier4 = "04"+randomLong(10000000L, 99999999L);
    String assetIdentifier5 = "04"+randomLong(10000000L, 99999999L);
    UserWS spcTestUserWS;
    Integer orderId;
    Integer optusPlanId;

    @BeforeClass
    public void beforeClass () {
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        testBuilder.given(envBuilder -> {
            ConfigurationBuilder configurationBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
            if (!configurationBuilder.pluginExists(SPCUpdateCustomerNextInvoiceDateTask.class.getName(), api.getCallerCompanyId())) {
                configurationBuilder.addPlugin(SPCUpdateCustomerNextInvoiceDateTask.class.getName()).withProcessingOrder(
                        SPCUpdateCustomerNextInvoiceDateTask.class.getName(), 1138);
            }
            configurationBuilder.build();
            logger.debug("SetNIDInBillingCycleChangeTest.beforeClass : {} " + testBuilder);

            String optusPlanDescription = "Optus Budget - $10";
            String planTypeOptus = "Optus";
            String optusPlanServiceType = "Mobile";
            BigDecimal optusPlanPrice = new BigDecimal("9.0909");
            BigDecimal optusPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024)
            // 200
            // MB
            BigDecimal optusPlanBoostQuantity = new BigDecimal("1024");
            Integer optusPlanBoostCount = new Integer("3");

            String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

            Map<String, String> optusPlanMetaFieldCodeMap = new HashMap<>();
            optusPlanMetaFieldCodeMap.put("USAGE_POOL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("USAGE_POOL_GL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("COST_GL_CODE", "410026-150");
            optusPlanMetaFieldCodeMap.put("REVENUE_GL_CODE", "410026-150");

            logger.debug("************************ Start creating plan : " + optusPlan + ", " + optusPlanDescription);
            optusPlanId = CreatePlanUtility.createPlan(api, optusPlan, planTypeOptus, optusPlanServiceType,
                    optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                    optusPlanBoostCount, optusPlanBoostQuantity, optusPlanMetaFieldCodeMap);
            logger.info("Optus PlanId: {}", optusPlanId);
        });
    }



    /**
     *
     * 1.Run Bill run for 15 July
     * 2.Create a Postpaid customer with 12 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 12 Aug)
     * 4.Create a Postpaid order with Active Since Date = 12 July
     * 5.Check NID again.(Expected NID = 12 Oct)*/
    @Test(enabled = true, priority = 1)
    public void testCustomerCreationBillingCycleDatePlus3DayDelayDateBefore3Months () {
        try {
            Date nextInvoiceDate = getDate(-4, 15, true).getTime();
            Date nextInvoiceDateNew = nextInvoiceDate;

            Date billingCycleDate = getDate(-4, 12,true).getTime();
            Date billingCycleDateNew = billingCycleDate;

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);

                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate,lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-4, 15,true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER1,
                        billingCycleDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(-3, 12, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648563",
                        "asset-16", "5123648563");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), billingCycleDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     *
     * 1.Run Bill run for 16 Nov
     * 2.Create a Prepaid customer with 16 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 16 Dec)
     * 4.Create a Prepaid order with Active Since Date = 16 Nov
     * 5.Check NID again.(Expected NID = 16 Dec)
     */
    @Test(enabled = true, priority = 6)
    public void testPrepaidCustomerCreationBillingCycleDateSameAsBillRun () {
        try {
            Date nextInvoiceDateNew = getDate(-1, 16, true).getTime();

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);

                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate, lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);

                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-1, 16, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER2,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(0, 16, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648564",
                        "asset-17", "5123648564");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());

        }
    }

    /**
     *
     * 1.Run Bill run for 17 Nov
     * 2.Create a Prepaid customer with 17 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 17 Dec)
     * 4.Create a Postpaid order with Active Since Date = 17 Nov
     * 5.Check NID again.(Expected NID = 17 Dec)
     */
    @Test(enabled = true, priority = 7)
    public void testPrepaidCustomerCreationBillingCycleDateSameAsBillRun1 () {
        try {
            Date nextInvoiceDateNew = getDate(-1, 17, true).getTime();

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);

                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate, lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-1, 17, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER3,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(0, 17, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648565",
                        "asset-18", "5123648565");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), nextInvoiceDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     *
     * 1.Run Bill run for 18 Nov
     * 2.Create a Postpaid customer with 10 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 10 Dec)
     * 4.Create a Prepaid order with Active Since Date = 10 Nov
     * 5.Check NID again.(Expected NID = 10 Dec)
     */
    @Test(enabled = true ,priority = 8)
    public void testCustomerCreationBillingCyclePlus3DayDelayDateLessThanBillRun () {
        try {
            Date nextInvoiceDate = getDate(-1, 18, true).getTime();
            //LocalDate nextInvoiceDate = LocalDate.of(2021, 11, 18);
            Date nextInvoiceDateNew = nextInvoiceDate;

            Date billingCycleDate = getDate(-1, 10, true).getTime();
            //LocalDate billingCycleDate = LocalDate.of(2021, 11, 10);
            Date billingCycleDateNew = billingCycleDate;

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate, lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-1, 18, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER4,
                        billingCycleDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(0, 10, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648566",
                        "asset-19", "5123648566");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), billingCycleDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());
                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     *
     * 1.Run Bill run for 27 Nov
     * 2.Create a Postpaid customer with 28 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 28 Nov)
     * 4.Create a Prepaid order with Active Since Date = 28 Nov
     * 5.Check NID again.(Expected NID = 28 Nov)
     */
    @Test(enabled = true ,priority = 15)
    public void testCustomerCreationBillingCyclePlus3DayDelayDateMoreThanBillRun () {
        try {
            Date nextInvoiceDate = getDate(-1, 27, true).getTime();
            //LocalDate nextInvoiceDate = LocalDate.of(2021, 11, 27);
            Date nextInvoiceDateNew = nextInvoiceDate;

            Date billingCycleDate = getDate(-1, 28, true).getTime();
            //LocalDate billingCycleDate = LocalDate.of(2021, 11, 28);
            Date billingCycleDateNew = billingCycleDate;

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate, lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-1, 27, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER5,
                        billingCycleDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(-1, 28, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648567",
                        "asset-19", "5123648567");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), billingCycleDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     *
     * 1.Run Bill run for 20 Nov
     * 2.Create a Prepaid customer with 9 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 9 Dec)
     * 4.Create a Prepaid order with Active Since Date = 9 Nov
     * 5.Check NID again.(Expected NID = 9 Dec)
     */
    @Test(enabled = true ,priority = 10)
    public void testPrepaidCustomerCreationBillingCycleDateLessThanBillRun () {
        try {
            Date nextInvoiceDateNew = getDate(-1, 20, true).getTime();

            Date billingCycleDateNew = getDate(-1, 9, true).getTime();

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate, lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-1, 20, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER6,
                        billingCycleDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(0, 9, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648568",
                        "asset-20", "5123648568");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), billingCycleDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     *
     * 1.Run Bill run for 21 Nov
     * 2.Create a Prepaid customer with 23 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 23 Dec)
     * 4.Create a Prepaid order with Active Since Date = 23 Nov
     * 5.Check NID again.(Expected NID = 23 DEc)
     */
    @Test(enabled = true ,priority = 11)
    public void testPrepaidCustomerCreationBillingCycleDateMoreThanBillRun () {
        try {
            Date billingCycleDateNew = getDate(-1, 21, true).getTime();

            Date nextInvoiceDateNew = getDate(-1, 23, true).getTime();

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(billingCycleDateNew);
                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate, lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-1, 21, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER7,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(-1, 23, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648569",
                        "asset-21", "5123648569");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), billingCycleDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     *
     * 1.Run Bill run for 22 Nov
     * 2.Create a Prepaid customer with 8 Monthly billing cycle.
     * 3.Check what is the NID of the customer.(Expected NID = 8 Dec)
     * 4.Create a Postpaid order with Active Since Date = 8 Nov
     * 5.Check NID again.(Expected NID = 8 Dec)
     */
    @Test(enabled = true ,priority = 12)
    public void testPrepaidCustomerCreationBillingCycleDateLessThanBillRun1 () {
        try {
            Date nextInvoiceDateNew = getDate(-1, 22, true).getTime();

            Date billingCycleDateNew = getDate(-1, 8, true).getTime();

            testBuilder.given(envBuilder -> {
                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(nextInvoiceDateNew);
                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(runDate, lastBillingProcessId);

                Date billingDate = getBillingProcessDate(lastBillingProcessId);
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-1, 22, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER8,
                        billingCycleDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID,false,
                        AUSTRALIA_POST,CC);

                logger.debug("## customer nextInvoiceDate {}",spcTestUserWS.getNextInvoiceDate());

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(0, 8, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        "5123648570",
                        "asset-22", "5123648570");

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder", spcTestUserWS.getId(), billingCycleDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY_POST_PAID, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        }finally{
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     * 1. The bill run has been executed for 1 Dec.
     * 2. Check from API the last Bill run date 1 Dec.
     * 3. Create a postpaid customer with billing cycle 1 Dec.
     * 4. Expected result :NID of customer is 1 Dec.
     * 5. Change Billing Cycle via API to 3 Dec.
     * 6. Expected result :NID of customer is 3 Dec.
     * 7. Create Prepaid order with Active since = 1 Dec.
     * 8.Expected result :NID of customer is 3 Dec.
     */
    @Test(enabled = true,priority = 16)
    public void testUpdateNIDInBillingCycleChangeForCurrentMonth() {

        try {
            //Bill run process
            Date runDate = getDate(0, 1, true).getTime();

            Integer lastBillingProcessId = api.getLastBillingProcess();
            logger.debug("## lastBillingProcess {}",lastBillingProcessId);

            updateBillingProcessDate(runDate, lastBillingProcessId);

            Date billingDate = getBillingProcessDate(lastBillingProcessId);
            logger.debug("## lastbillingDate {}",billingDate);

            Date lastBillDate = getDate(0, 1, true).getTime();
            assertEquals("Last bill run date should be match", lastBillDate,billingDate);

            // Create customer with billing cycle 1st Monthly
            Date nextInvoiceDateNew = getDate(0, 1, true).getTime();
            testBuilder.given(envBuilder -> {

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER10,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,
                        false,
                        AUSTRALIA_POST, CC);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(0, 1, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Change billing cycle to 16th Monthly
                MainSubscriptionWS mainSubscription = spcTestUserWS.getMainSubscription();
                mainSubscription.setNextInvoiceDayOfPeriod(3);
                spcTestUserWS.setMainSubscription(mainSubscription);
                api.updateUser(spcTestUserWS);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate1 = getDate(0, 3, true).getTime();
                assertEquals("Customer NID should be match", NIDDate1,spcTestUserWS.getNextInvoiceDate());


                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        assetIdentifier1,
                        "asset-15", assetIdentifier1);

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder-1393", spcTestUserWS.getId(), nextInvoiceDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate2 = getDate(0, 3, true).getTime();
                assertEquals("Customer NID should be match", NIDDate2,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }


    /**
     * 1. The bill run has been executed for 19th nov.
     * 2. Check from API the last Bill run date 19th Nov
     * 3. Create a postpaid customer with NID 14 Nov (14 Monthly)
     * 4. Expected result :NID of customer is 14th Nov
     * 5. Change Billing Cycle via API to 16 Nov (16 Monthly)
     * 6. Expected result :NID of customer is 16 Dec
     * 7. Create Prepaid order with Active since = 14 Nov.
     * 8.Expected result :NID of customer is 16 Dec
     */
    @Test(enabled = true,priority = 3)
    public void testUpdateNIDInBillingCycleChangeForNextMonth() {

        try {
            testBuilder.given(envBuilder -> {

                //Bill run process
                Date runDate = getDate(-2, 19, true).getTime();

                updateRunDateBillingProcessConfiguration(runDate);
                executeBillingProcessByRunDate(runDate);

                Integer lastBillingProcess = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcess);

                // Check last bill run date
                BillingProcessWS billingProcessWS = api.getBillingProcess(lastBillingProcess);
                Date billingDate =  billingProcessWS.getBillingDate() ;
                logger.debug("## lastbillingDate {}",billingDate);

                Date lastBillDate = getDate(-2, 19, true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                // Create customer with billing cycle 14th Monthly
                Date nextInvoiceDateNew = getDate(-2, 14, true).getTime();

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER11,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,
                        true,
                        AUSTRALIA_POST, CC);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(-2, 14,true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Change billing cycle to 16th Monthly
                MainSubscriptionWS mainSubscription = spcTestUserWS.getMainSubscription();
                mainSubscription.setNextInvoiceDayOfPeriod(16);
                spcTestUserWS.setMainSubscription(mainSubscription);
                api.updateUser(spcTestUserWS);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate1 = getDate(-1, 16, true).getTime();
                assertEquals("Customer NID should be match", NIDDate1,spcTestUserWS.getNextInvoiceDate());


                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        assetIdentifier2,
                        "asset-15", assetIdentifier2);

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder-1393", spcTestUserWS.getId(), nextInvoiceDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate2 = getDate(-1, 16, true).getTime();
                assertEquals("Customer NID should be match", NIDDate2,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     * 1. The bill run has been executed for 14th Oct.
     * 2. Check from API the last Bill run date.
     * 3. Create a postpaid customer with NID = 10 Monthly
     * 4. Check NID of customer is 10 Oct
     * 5. Change Billing Cycle via API to NID = 12 Monthly
     * 6. Check NID of customer is 12th Oct
     * 7. Create Prepaid order with Active since = 12 Oct.
     * 8. Check NID of customer 12th Oct
     * 9. Execute The bill run 15th Oct.
     */
    @Test(enabled = true,priority = 4)
    public void testUpdateNIDInBillingCycleChangeIn3DaysDelay() {

        try {
            testBuilder.given(envBuilder -> {

                //Bill run process
                Date runDate = com.sapienter.jbilling.server.util.Util.getStartOfDay(getDate(-2, 20, true).getTime());

                updateRunDateBillingProcessConfiguration(runDate);
                executeBillingProcessByRunDate(runDate);
                logger.debug("## runDate {}",runDate);

                Integer lastBillingProcess1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcess1);

                BillingProcessWS billingProcessWS1 = api.getBillingProcess(lastBillingProcess1);
                Date billingDate1 =  billingProcessWS1.getBillingDate() ;
                logger.debug("## lastbillingDate {}",billingDate1);

                Date lastBillDate = getDate(-2, 20,true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate1);

                // Create customer with billing cycle 10th Monthly
                Date nextInvoiceDateNew = getDate(-2, 16,true).getTime();

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER12,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,
                        false,
                        AUSTRALIA_POST, CC);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(-1, 16,true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Change billing cycle to 12th Monthly
                MainSubscriptionWS mainSubscription = spcTestUserWS.getMainSubscription();
                mainSubscription.setNextInvoiceDayOfPeriod(18);
                spcTestUserWS.setMainSubscription(mainSubscription);
                api.updateUser(spcTestUserWS);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate1 = getDate(-2, 18,true).getTime();
                assertEquals("Customer NID should be match", NIDDate1,spcTestUserWS.getNextInvoiceDate());

                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        assetIdentifier3,
                        "asset-15", assetIdentifier3);

                assetWSs.add(api.getAsset(asset1));

                Date activeSinceDate = getDate(-2, 18,true).getTime();
                orderId = createOrderWithAsset("TestOrder-1393", spcTestUserWS.getId(), activeSinceDate, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate2 = getDate(-2, 18,true).getTime();
                assertEquals("Customer NID should be match", NIDDate2,spcTestUserWS.getNextInvoiceDate());

                //Bill run process
                Date runDateNew1 = getDate(-2, 21,true).getTime();

                updateRunDateBillingProcessConfiguration(runDateNew1);
                executeBillingProcessByRunDate(runDateNew1);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate3 = getDate(-1, 18,true).getTime();
                assertEquals("Customer NID should be match", NIDDate3,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     *1. The bill run has been executed for 15th Oct.
     * 2. Check from API the last Bill run date 15th Oct
     * 3. Create a postpaid customer with NID = 10 Monthly
     * 4. Check NID of customer is 10 Nov
     * 5. Change Billing Cycle via API to NID = 11 Monthly
     * 6. Check NID of customer is 11
     * 7. Create Prepaid order with Active since = 11 Nov.
     * 8. Check NID of customer is 11th Dec
     */
    @Test(enabled = true,priority = 2)
    public void testUpdateNIDInBillingCycleChangeIn3DaysDelayNIDBeforeLBD() {

        try {
            testBuilder.given(envBuilder -> {

                //Update run date
                Date updateRunDate1 = getDate(-2, 15, true).getTime();
                Date updateRunDateOriginal1 = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate1);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal1,lastBillingProcessId1);

                BillingProcessWS billingProcessWS1 = api.getBillingProcess(lastBillingProcessId1);
                Date billingDate =  billingProcessWS1.getBillingDate() ;
                logger.debug("## lastbillingDate {}",billingDate);


                Date lastBillDate = getDate(-2, 15,true).getTime();
                assertEquals("Last bill run date should be match", lastBillDate,billingDate);

                // Create customer with billing cycle 10th Monthly
                Date nextInvoiceDateNew = getDate(-1, 10,true).getTime();

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER13,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,
                        false,
                        AUSTRALIA_POST, CC);


                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(-1, 10,true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Change billing cycle to 11th Monthly
                MainSubscriptionWS mainSubscription = spcTestUserWS.getMainSubscription();
                mainSubscription.setNextInvoiceDayOfPeriod(11);
                spcTestUserWS.setMainSubscription(mainSubscription);
                api.updateUser(spcTestUserWS);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate1 = getDate(-1, 11,true).getTime();
                assertEquals("Customer NID should be match", NIDDate1,spcTestUserWS.getNextInvoiceDate());


                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        assetIdentifier4,
                        "asset-15", assetIdentifier4);

                assetWSs.add(api.getAsset(asset1));
                Date activeSinceDate = getDate(-1, 11,true).getTime();
                orderId = createOrderWithAsset("TestOrder-1393", spcTestUserWS.getId(), activeSinceDate, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate2 = getDate(-1, 11, true).getTime();
                assertEquals("Customer NID should be match", NIDDate2,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    /**
     * 1. The bill run has been executed for 10 Dec.
     * 2. Check from API the last Bill run date 10 Dec.
     * 3. Create a postpaid customer with billing cycle 8 Dec.
     * 4. Expected result :NID of customer is 8 Dec.
     * 5. Change Billing Cycle via API to 11 Dec.
     * 6. Expected result :NID of customer is 11 Dec.
     * 7. Create Prepaid order with Active since = 8 Dec.
     * 8.Expected result :NID of customer is 11 Dec.
     */
    @Test(enabled = true,priority = 18)
    public void testUpdateNIDInBillingCycleChange() {

        try {
            //Bill run process
            Date runDate = getDate(-1, 10, true).getTime();

            Integer lastBillingProcessId = api.getLastBillingProcess();
            logger.debug("## lastBillingProcess {}",lastBillingProcessId);

            updateBillingProcessDate(runDate, lastBillingProcessId);

            Date billingDate = getBillingProcessDate(lastBillingProcessId);
            logger.debug("## lastbillingDate {}",billingDate);

            Date lastBillDate = getDate(-1, 10, true).getTime();
            assertEquals("Last bill run date should be match", lastBillDate,billingDate);

            // Create customer with billing cycle 1st Monthly
            Date nextInvoiceDateNew = getDate(-1, 8, true).getTime();
            testBuilder.given(envBuilder -> {

                spcTestUserWS = getSPCTestUserWS(
                        envBuilder,
                        TEST_CUSTOMER14,
                        nextInvoiceDateNew,
                        "",
                        CUSTOMER_TYPE_VALUE_POST_PAID,
                        false,
                        AUSTRALIA_POST, CC);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate = getDate(-1, 8, true).getTime();
                assertEquals("Customer NID should be match", NIDDate,spcTestUserWS.getNextInvoiceDate());

                //Change billing cycle to 16th Monthly
                MainSubscriptionWS mainSubscription = spcTestUserWS.getMainSubscription();
                mainSubscription.setNextInvoiceDayOfPeriod(11);
                spcTestUserWS.setMainSubscription(mainSubscription);
                api.updateUser(spcTestUserWS);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate1 = getDate(-1, 11, true).getTime();
                assertEquals("Customer NID should be match", NIDDate1,spcTestUserWS.getNextInvoiceDate());


                //optus
                PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                List<AssetWS> assetWSs = new ArrayList<>();
                Integer asset1 = buildAndPersistAssetWithServiceId(envBuilder,
                        getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS),
                        assetIdentifier5,
                        "asset-15", assetIdentifier5);

                assetWSs.add(api.getAsset(asset1));

                orderId = createOrderWithAsset("TestOrder-1393", spcTestUserWS.getId(), nextInvoiceDateNew, null,
                        MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap , assetWSs, optusPlanId);

                //Validate Customer NID
                spcTestUserWS = api.getUserWS(spcTestUserWS.getId());
                Date NIDDate2 = getDate(-1, 11, true).getTime();
                assertEquals("Customer NID should be match", NIDDate2,spcTestUserWS.getNextInvoiceDate());

                //Update run date
                Date updateRunDate = getDate(-2, 16, true).getTime();
                Date updateRunDateOriginal = com.sapienter.jbilling.server.util.Util.getStartOfDay(updateRunDate);

                Integer lastBillingProcessId1 = api.getLastBillingProcess();
                logger.debug("## lastBillingProcess {}",lastBillingProcessId1);
                updateBillingProcessDate(updateRunDateOriginal,lastBillingProcessId1);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(USER_CREATION_ASSERT, spcTestUserWS);
                assertNotNull(ORDER_CREATION_ASSERT, orderId);

                UserWS user = api.getUserWS(spcTestUserWS.getId());
                logger.debug("## Customer Id {}", user.getCustomerId());

                OrderWS subscriptionOrder = api.getOrder(orderId);

                assertNotNull(ORDER_ASSERT, subscriptionOrder);
            });
        } finally {
            clearTestDataForUser(spcTestUserWS.getId());
        }
    }

    private void updateRunDateBillingProcessConfiguration(Date runDate) {
        // set the configuration to something we are sure about
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        logger.debug("Billing runDate: {}", runDate);
        config.setNextRunDate(runDate);
        config.setPeriodUnitId(3); // 3-Daily , 1-monthly
        config.setRetries(null);
        config.setDaysForRetry(new Integer(1));
        config.setGenerateReport(new Integer(0));
        config.setAutoPaymentApplication(new Integer(0));
        config.setDfFm(new Integer(0));
        config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateValue(new Integer(1));
        config.setInvoiceDateProcess(new Integer(0));
        config.setMaximumPeriods(new Integer(1));
        config.setOnlyRecurring(new Integer(1));
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
        }
    }

    private void resetBillingProcessConfiguration() {
        // set the configuration to something we are sure about
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setGenerateReport(new Integer(1));
        config.setPeriodUnitId(1); // 3-Daily , 1-monthly
        config.setRetries(new Integer(0));
        config.setDaysForRetry(new Integer(1));
        config.setGenerateReport(new Integer(1));
        config.setAutoPaymentApplication(new Integer(1));
        config.setDfFm(new Integer(0));
        config.setDueDateUnitId(Constants.PERIOD_UNIT_MONTH);
        config.setDueDateValue(new Integer(1));
        config.setInvoiceDateProcess(new Integer(0));
        config.setMaximumPeriods(new Integer(1));
        config.setOnlyRecurring(new Integer(1));
        config.setProratingType(ProratingType.PRORATING_AUTO_OFF.getProratingType());
        api.createUpdateBillingProcessConfiguration(config);
    }

    @AfterClass
    public void afterTests() {
        ConfigurationBuilder configurationBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
        if (configurationBuilder.pluginExists(SPCUpdateCustomerNextInvoiceDateTask .class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.deletePlugin(SPCUpdateCustomerNextInvoiceDateTask.class.getName(), api.getCallerCompanyId());
        }
        configurationBuilder.build();
    }
}
