package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

@Test(testName = "spc.ServiceElementsDataMediationTest")
public class ServiceElementsDataMediationTest extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    //@formatter:off
    private static final String MEDIATION_DUPLICATE_RECORDS_STR                 = "Mediation Duplicate Records";
    private static final String MEDIATION_ERROR_RECORDS_STR                     = "Mediation Error Records";
    private static final String MEDIATION_DONE_AND_NOT_BILLABLE_STR             = "Mediation Done And Not Billable";
    private static final String MEDIATION_DONE_AND_BILLABLE_STR                 = "Mediation Done And Billable ";
    private static final String MEDIATION_PROCESS_STR                           = "Mediation Process {}";
    private static final String COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID = "Service Element Internet Item Id";
    private static final String DATA_USAGE              = "se-data-usage";
    private static final String SEDATA_PLAN_ITEM        = "se-data-plan-subscription-item";
    private static final String SEDATA_PLAN_CODE        = "se-data-Plan";

    private static final String USER_01                 = "SE-Data-Test-User-01";
    private static final String USER_02                 = "SE-Data-Test-User-02";
    private static final String SUBSCRIPTION_ORDER_01   = "se-subscription-order-01";
    private static final String MEDIATION_FILE_PREFIX   = "BBS";
    private static final String ORIGINAL_QUANITITY      = "41376014.00";
    private static final String LINE_QUANTITY           = ".04";
    private static final String MEDIATED_AMOUNT         = "0.39";
    private static final int MONTHLY_ORDER_PERIOD       = 2;

    private Integer enumId1;
    private Integer enumId2;
    //@formatter:on

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            RatingConfigurationWS ratingConfiguration = new RatingConfigurationWS(api.getRatingUnit(spcDataUnitId), null);
            // Create usage product for SE Data
            buildAndPersistFlatProductWithRating(envBuilder, api, DATA_USAGE, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "10", true, 0, false,
                    com.sapienter.jbilling.server.util.Util.getEpochDate(), ratingConfiguration);

            // create plan item for SE Data
            buildAndPersistFlatProduct(envBuilder, api, SEDATA_PLAN_ITEM, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);

            // Build flat pricing strategy
            PriceModelWS seDataUsagePriceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(),
                    new BigDecimal("10"), api.getCallerCurrencyId());
            enumId1 = buildAndPersistEnumeration(envBuilder, ENUM_QUANTITY_RESOLUTION_UNIT_VALUES, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT);
            enumId2 = buildAndPersistEnumeration(envBuilder, ENUM_INTERNET_TECHNOLOGY_VALUES, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE);
            if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT)){
                buildAndPersistMetafield(testBuilder, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT, DataType.ENUMERATION, EntityType.PLAN);
            }
            if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE)){
                buildAndPersistMetafield(testBuilder, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE, DataType.ENUMERATION, EntityType.PLAN);
            }
            // create company level metafield for SE Data product id
            if (!isMetaFieldPresent(EntityType.COMPANY, COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID)) {
                buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID, DataType.STRING,
                        EntityType.COMPANY);
            }
            // setting company level metafield value
            setCompanyLevelMetaField(testBuilder.getTestEnvironment(), COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID,
                    envBuilder.idForCode(DATA_USAGE).toString());

            PlanItemWS seDataUsagePlanItem = buildPlanItem(envBuilder.idForCode(DATA_USAGE),
                    MONTHLY_ORDER_PERIOD, "0.00", seDataUsagePriceModel, null);

            PlanItemWS seDataAssetPlanItem = buildPlanItem(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE),
                    MONTHLY_ORDER_PERIOD, "0.00", seDataUsagePriceModel, null);

            // Create plan for SE Data
            Integer planId =  buildAndPersistPlan(envBuilder, api, SEDATA_PLAN_CODE, "100 SE Data Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(SEDATA_PLAN_ITEM), Collections.emptyList(), seDataUsagePlanItem, seDataAssetPlanItem);
            setPlanLevelMetaFieldForInternet(planId, ENUM_INTERNET_TECHNOLOGY_VALUES.get(1).getValue(),ENUM_QUANTITY_RESOLUTION_UNIT_VALUES.get(2).getValue());
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("SE Data Product Creation Failed", testEnvBuilder.idForCode(DATA_USAGE));
            assertNotNull("SE Data Plan Creation Failed", testEnvBuilder.idForCode(SEDATA_PLAN_CODE));
            assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID));
        });
    }

    @Test(priority = 1, enabled = true)
    void test01SEDataMediationUpload() {
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        try {
            testBuilder
            .given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user01 = envBuilder.customerBuilder(api)
                        .withUsername(USER_01)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                        .build();
                assertNotNull("Creation of Test User 01 is failed", user01);
                logger.debug("Test User 01 is created {}", user01.getId());
                users.add(user01.getId());

                Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                        envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                        envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), USER_01);

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SEDATA_PLAN_ITEM), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);

                Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSinceDate.getTime(), null,
                        MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap, USER_01);

                assertNotNull("Creation of subscription order 01 is failed", orderId);
                logger.debug("Subscription order id {} is created for user {}", orderId, envBuilder.idForCode(USER_01));
                orders.add(orderId);

            })
            .validate(
                    (testEnv, testEnvBuilder) -> {
                        logger.debug("Creating mediation CDR file...");
                        List<String> cdrLines = Arrays.asList(new StringJoiner(",").add("2019-03-13").add(USER_01).add("38269483")
                                .add("3106531")
                                .add("41376014").toString());
                        logger.debug("cdr lines {}", cdrLines);
                        String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, cdrLines);
                        logger.debug("Mediation file is created {}", cdrFilePath);

                        final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                        UUID mediationProcessId = api.triggerMediationByConfigurationByFile(
                                getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                        assertNotNull("Mediation trigger is failed", mediationProcessId);
                        logger.debug("Mediation Process Id {}", mediationProcessId);
                        pauseUntilMediationCompletes(20, api);

                    }).validate((testEnv, testEnvBuilder) -> {
                        MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                        logger.debug(MEDIATION_PROCESS_STR, mediationProcess);
                        assertEquals(MEDIATION_DONE_AND_BILLABLE_STR, Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                        assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE_STR, Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                        assertEquals(MEDIATION_ERROR_RECORDS_STR, Integer.valueOf(0), mediationProcess.getErrors());
                        assertEquals(MEDIATION_DUPLICATE_RECORDS_STR, Integer.valueOf(0), mediationProcess.getDuplicates());

                        OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_01));
                        assertNotNull("Mediation Should Create Order", order);
                        assertEquals("Invalid mediated order line,", 1, order.getOrderLines().length);

                        JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                        assertEquals("Invalid original quantity", new BigDecimal(ORIGINAL_QUANITITY),
                                viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));

                        OrderLineWS dataLine = getLineByItemId(order, testEnvBuilder.idForCode(DATA_USAGE));

                        assertNotNull("SE Data usage item not found", dataLine);
                        assertEquals("Invalid item line quantity,", new BigDecimal(LINE_QUANTITY),
                                dataLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Invalid item line amount,", new BigDecimal(MEDIATED_AMOUNT),
                                dataLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Invalid mediated order amount,", new BigDecimal(MEDIATED_AMOUNT),
                                order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                    });
        } finally {
            orders.stream().forEach(api::deleteOrder);
            users.stream().forEach(api::deleteUser);
        }
    }

    @Test(priority = 2, enabled = true)
    void test02SEDataRecycleMediation() {
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        try {
            testBuilder
            .given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user02 = envBuilder.customerBuilder(api)
                        .withUsername(USER_02)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                        .build();
                assertNotNull("Creation of Test User 02 is failed", user02);
                logger.debug("Test User 02 is created {}", user02.getId());
                users.add(user02.getId());

                // set invalid item id in company level metafield
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(), COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID, "123");

                Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                        envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                        envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), USER_02);

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SEDATA_PLAN_ITEM), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);

                Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSinceDate.getTime(), null,
                        MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap, USER_02);
                assertNotNull("Creation of subscription order 01 is failed", orderId);
                logger.debug("Subscription order id {} is created for user {}", orderId, envBuilder.idForCode(USER_02));
                orders.add(orderId);
            })
            .validate(
                    (testEnv, testEnvBuilder) -> {
                        final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                        logger.debug("Creating mediation CDR file...");
                        List<String> cdrLines = Arrays.asList(new StringJoiner(",").add("2019-03-13").add(USER_02).add("38269483")
                                .add("3106531")
                                .add("41376014").toString());
                        logger.debug("cdr lines {}", cdrLines);
                        String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, cdrLines);
                        logger.debug("Mediation file is created {}", cdrFilePath);

                        UUID mediationProcessId = api.triggerMediationByConfigurationByFile(
                                getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                        assertNotNull("Mediation trigger is failed", mediationProcessId);
                        logger.debug("Mediation Process Id {}", mediationProcessId);
                        pauseUntilMediationCompletes(20, api);

                        // Trigger mediation process
                        MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                        logger.debug(MEDIATION_PROCESS_STR, mediationProcess);
                        assertEquals(MEDIATION_DONE_AND_BILLABLE_STR, Integer.valueOf(0), mediationProcess.getDoneAndBillable());
                        assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE_STR, Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                        assertEquals(MEDIATION_DUPLICATE_RECORDS_STR, Integer.valueOf(0), mediationProcess.getDuplicates());
                        assertEquals(MEDIATION_ERROR_RECORDS_STR, Integer.valueOf(1), mediationProcess.getErrors());

                    })
                    .validate((testEnv, testEnvBuilder) -> {
                        // set valid item id in the company level metafield
                        setCompanyLevelMetaField(testBuilder.getTestEnvironment(), COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID,
                                testEnvBuilder.idForCode(DATA_USAGE).toString());

                        // Trigger mediation recycle
                        final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                        UUID mediationProcessId = api.getMediationProcessStatus().getMediationProcessId();
                        logger.debug("trigger recycle process for mediation process {}", mediationProcessId);
                        UUID recycleMediationProcessId = api.runRecycleForProcess(mediationProcessId);
                        logger.debug("Recycle mediation process id is {} for mediation process id {}", recycleMediationProcessId,
                                mediationProcessId);
                        pauseUntilMediationCompletes(20, api);

                    }).validate((testEnv, testEnvBuilder) -> {

                        MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                        logger.debug(MEDIATION_PROCESS_STR, mediationProcess);
                        assertEquals(MEDIATION_DONE_AND_BILLABLE_STR, Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                        assertEquals(MEDIATION_DONE_AND_NOT_BILLABLE_STR, Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                        assertEquals(MEDIATION_ERROR_RECORDS_STR, Integer.valueOf(0), mediationProcess.getErrors());
                        assertEquals(MEDIATION_DUPLICATE_RECORDS_STR, Integer.valueOf(0), mediationProcess.getDuplicates());

                        OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_02));
                        assertNotNull("Mediation should create order", order);
                        assertEquals("Invalid mediated order line,", 1, order.getOrderLines().length);

                        JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                        assertEquals("Invalid original quantity", new BigDecimal(ORIGINAL_QUANITITY),
                                viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));

                        OrderLineWS dataLine = getLineByItemId(order, testEnvBuilder.idForCode(DATA_USAGE));

                        assertNotNull("SE Data usage item not found", dataLine);
                        assertEquals("Invalid item line quantity,", new BigDecimal(LINE_QUANTITY),
                                dataLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Invalid item line amount,", new BigDecimal(MEDIATED_AMOUNT),
                                dataLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                        assertEquals("Invalid mediated order amount,", new BigDecimal(MEDIATED_AMOUNT),
                                order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                    });
        } finally {
            orders.stream().forEach(api::deleteOrder);
            users.stream().forEach(api::deleteUser);
        }
    }

    @Override
    @AfterClass
    public void tearDown() {
        super.tearDown();
        try {
            api.deleteEnumeration(enumId1);
        } catch(Exception ex) {
            logger.error("enum deletion failed", ex);
        }

        try {
            api.deleteEnumeration(enumId2);
        } catch(Exception ex) {
            logger.error("enum deletion failed", ex);
        }
    }

}
