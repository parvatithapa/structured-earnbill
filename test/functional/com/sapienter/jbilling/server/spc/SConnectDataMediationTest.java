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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

@Test(testName = "spc.SConnectDataMediationTest")
public class SConnectDataMediationTest  extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String USER_01                                  = "AVC000058969034";
    private static final String USER_02                                  = "AVC000058969044";
    private static final String USER_03                                  = "AVC000060345751";
    private static final String SCONNECT_DATA_PLAN_ITEM                  = "sconnect-data-plan-subscription-item";
    private static final String SCONNECT_DATA_PLAN_ITEM_DOWNUSG          = "sconnect-data-plan-subscription-item-dwnlUsg";
    private static final String SCONNECT_DATA_PLAN_CODE                  = "sconnect-data-Plan";
    private static final String SCONNECT_DATA_PLAN_CODE_FORDWNUSG        = "sconnect-data-Plan-For-Dwnld-Usg";
    private static final String SCONNECT_DATA_PRODUCT                    = "sconnect-data_product";
    private static final String SCONNECT_DATA_PRODUCT_DOWNLOAD_USG       = "sconnect_data_product_download_usage";
    private static final String SCONNECT_DATA_CDR_FORMAT                 = "AVC000058969034,26,13329468,23/05/2018 11:10,23/05/2018 11:12,134343421,565644534535,565778877956";
    private static final String SCONNECT_DATA_CDR_FORMAT_DU              = "AVC000060345751,26,13328987,23/05/2018 10:59,23/05/2018 11:03,205,234346160,234346365";
    private static final String SCONNECT_DATA_CDR_FORMAT_DUPLICATE       = "AVC000058969044,26,13329468,23/05/2018 11:10,23/05/2018 11:12,1234343441,765644534535,766878877976";
    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                    = "subscription02"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_03                    = "subscription03"+ System.currentTimeMillis();
    private static final String MEDIATION_FILE_PREFIX                    = "SConnect_Data";
    private static final int    MONTHLY_ORDER_PERIOD                    = 2;
    public static final String  COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID = "SConnect Internet Item Id";

    int productId;
    int dUsgproductId;
    private UUID uuid;
    private Integer enumId1;
    private Integer enumId2;
    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder
                .given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    RatingConfigurationWS ratingConfiguration = new RatingConfigurationWS(api.getRatingUnit(spcDataUnitId), null);
                    // Creating Vocus Internet Product
                    productId = buildAndPersistFlatProductWithRating(envBuilder, api, SCONNECT_DATA_PRODUCT, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false,
                            com.sapienter.jbilling.server.util.Util.getEpochDate(), ratingConfiguration);
                    dUsgproductId = buildAndPersistFlatProductWithRating(envBuilder, api, SCONNECT_DATA_PRODUCT_DOWNLOAD_USG, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "2", true, 0, false,
                            com.sapienter.jbilling.server.util.Util.getEpochDate(), ratingConfiguration);
                 // create plan item for Vocus Internet
                    buildAndPersistFlatProduct(envBuilder, api, SCONNECT_DATA_PLAN_ITEM, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);
                    buildAndPersistFlatProduct(envBuilder, api, SCONNECT_DATA_PLAN_ITEM_DOWNUSG, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "180.00", false, 0, true);
                 // Build flat pricing strategy
                    PriceModelWS vocusInternetUsagePriceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(),
                            new BigDecimal("10"), api.getCallerCurrencyId());
                    
                    enumId1 = buildAndPersistEnumeration(envBuilder, ENUM_QUANTITY_RESOLUTION_UNIT_VALUES, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT);
                    enumId2 = buildAndPersistEnumeration(envBuilder, ENUM_INTERNET_TECHNOLOGY_VALUES, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE);
                    if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT)){
                    buildAndPersistMetafield(testBuilder, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT, DataType.ENUMERATION, EntityType.PLAN);
                    }
                    if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE)){
                    buildAndPersistMetafield(testBuilder, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE, DataType.ENUMERATION, EntityType.PLAN);
                    }

                    if (!isMetaFieldPresent(EntityType.COMPANY, COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID)){
                    buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID, DataType.STRING,
                            EntityType.COMPANY);
                    }

                    // Setting Company Level Meta Fields
                    setCompanyLevelMetaField(testBuilder.getTestEnvironment(), COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID,
                            envBuilder.idForCode(SCONNECT_DATA_PRODUCT).toString());
                    PlanItemWS vocusInternetUsagePlanItem = buildPlanItem(envBuilder.idForCode(SCONNECT_DATA_PRODUCT),
                            MONTHLY_ORDER_PERIOD, "0.00", vocusInternetUsagePriceModel, null);
                    
                    PlanItemWS vocusInternetDownloadUsagePlanItem = buildPlanItem(envBuilder.idForCode(SCONNECT_DATA_PRODUCT),
                            MONTHLY_ORDER_PERIOD, "0.00", vocusInternetUsagePriceModel, null);

                    PlanItemWS vocusAssetPlanItemUPL = buildPlanItem(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE),
                            MONTHLY_ORDER_PERIOD, "0.00", vocusInternetUsagePriceModel, null);

                    PlanItemWS vocusAssetPlanItemDWL = buildPlanItem(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE),
                            MONTHLY_ORDER_PERIOD, "0.00", vocusInternetUsagePriceModel, null);

                    // Create plan for Vocus Internet 
                    // Integer planId =
                    Integer planId = buildAndPersistPlan(envBuilder, api, SCONNECT_DATA_PLAN_CODE, "100 Vocus Interent Plan", MONTHLY_ORDER_PERIOD,
                            envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), Collections.emptyList(), vocusInternetUsagePlanItem, vocusAssetPlanItemUPL);
                    setPlanLevelMetaFieldForInternet(planId, ENUM_INTERNET_TECHNOLOGY_VALUES.get(1).getValue(),ENUM_QUANTITY_RESOLUTION_UNIT_VALUES.get(2).getValue());

                    Integer downLoadUsageplanId =  buildAndPersistPlan(envBuilder, api, SCONNECT_DATA_PLAN_CODE_FORDWNUSG, "100 Vocus Interent Plan for DUsg", MONTHLY_ORDER_PERIOD,
                            envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM_DOWNUSG), Collections.emptyList(), vocusInternetDownloadUsagePlanItem, vocusAssetPlanItemDWL);
                    setPlanLevelMetaFieldForInternet(downLoadUsageplanId, ENUM_INTERNET_TECHNOLOGY_VALUES.get(1).getValue(),ENUM_QUANTITY_RESOLUTION_UNIT_VALUES.get(0).getValue());

                }).test((testEnv, testEnvBuilder) -> {
                    assertNotNull("SConnect Data Product Creation Failed",
                            testEnvBuilder.idForCode(SCONNECT_DATA_PRODUCT));
                    assertNotNull("SConnect Data Plan Creation Failed", testEnvBuilder.idForCode(SCONNECT_DATA_PLAN_CODE));
                });
    }


    @Test(priority = 1)
    void test01VocusInternetMediationUpload() {
        testBuilder
                .given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    UserWS user01 = envBuilder.customerBuilder(api).withUsername(USER_01)
                            .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                            .addTimeToUsername(false).withNextInvoiceDate(new Date())
                            .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1)).build();
                    logger.debug("User created {}", user01.getId());

                    Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                            envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), "AVC000058969034");

                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.putAll(buildProductQuantityEntry(
                            envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
                    productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM),
                            BigDecimal.ONE));

                    Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                    productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE),
                            Arrays.asList(userNameAsAsset));

                    Calendar activeSinceDate = Calendar.getInstance();
                    activeSinceDate.set(2018, 01, 01);

                    Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSinceDate.getTime(), null,
                            MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap, USER_01);
                    logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_01));

                })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            assertNotNull("user 01 Creation Failed", testEnvBuilder.idForCode(USER_01));
                            assertNotNull("subscription order 01 Creation Failed",
                                    testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_01));
                        })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            logger.debug("Creating Mediation file ....");
                            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                            List<String> cdrLine = new ArrayList<>();
                            cdrLine.addAll(Arrays.asList(String.format(SCONNECT_DATA_CDR_FORMAT)));
                            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(),
                                    ".txt", null, cdrLine);
                            logger.debug("Mediation file created {}", cdrFilePath);
                            uuid = api.triggerMediationByConfigurationByFile(
                                    getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                            assertNotNull("Mediation trigger failed", uuid);
                            logger.debug("Mediation ProcessId {}", uuid);
                            pauseUntilMediationCompletes(20, api);
                        })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            MediationProcess mediationProcess = api.getMediationProcess(uuid);
                            logger.debug("Mediation Process {}", mediationProcess);
                            assertEquals("Mediation Done And Billable ", Integer.valueOf(1),
                                    mediationProcess.getDoneAndBillable());
                            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0),
                                    mediationProcess.getDoneAndNotBillable());
                            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_01));
                            assertNotNull("Mediation Should Create Order", order);
                            assertEquals(
                                    "Invalid resolved quantity",
                                    new BigDecimal("565778877956").divide(new BigDecimal(1024 * 1024 * 1024)).setScale(
                                            2, BigDecimal.ROUND_HALF_UP), order.getOrderLines()[0]
                                            .getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                            assertEquals("Invalid order amount", new BigDecimal("5269.23"), order.getTotalAsDecimal()
                                    .setScale(2, BigDecimal.ROUND_HALF_UP));
                        });
    }

    @Test(priority = 2)
    void test02DuplicateRecord() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user02 = envBuilder.customerBuilder(api)
                    .withUsername(USER_02)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            logger.debug("User created {}", user02.getId());

            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), "AVC000058969035");

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_02, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_02);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_02));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 01 Creation Failed", testEnvBuilder.idForCode(USER_02));
            assertNotNull("subscription order 01 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_02));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            List<String> cdrLine = new ArrayList<>();
            cdrLine.addAll(Arrays.asList(String.format(SCONNECT_DATA_CDR_FORMAT_DUPLICATE)));
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".txt", null, cdrLine);
            logger.debug("Mediation file created {}", cdrFilePath);
            uuid = api.triggerMediationByConfigurationByFile(
                    getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", uuid);
            logger.debug("Mediation ProcessId {}", uuid);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(uuid);
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            //1 duplicate as recorde accntSessionId and  event date is same
            assertEquals("Mediation Duplicate", Integer.valueOf(1), mediationProcess.getDuplicates());
        });
    }


    @Test(priority = 3)
    void test03VocusInternetDownloadUsageProduct() {
        testBuilder
                .given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    UserWS user03 = envBuilder.customerBuilder(api).withUsername(USER_03)
                            .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                            .addTimeToUsername(false).withNextInvoiceDate(new Date())
                            .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1)).build();
                    logger.debug("User created {}", user03.getId());

                    Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                            envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), "AVC000060345751");

                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.putAll(buildProductQuantityEntry(
                            envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
                    productQuantityMap.putAll(buildProductQuantityEntry(
                            envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM_DOWNUSG), BigDecimal.ONE));

                    Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                    productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE),
                            Arrays.asList(userNameAsAsset));

                    Calendar activeSinceDate = Calendar.getInstance();
                    activeSinceDate.set(2018, 01, 01);

                    Integer orderId = createOrder(SUBSCRIPTION_ORDER_03, activeSinceDate.getTime(), null,
                            MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap, USER_03);
                    logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_03));

                })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            assertNotNull("user 03 Creation Failed", testEnvBuilder.idForCode(USER_03));
                            assertNotNull("subscription order 03 Creation Failed",
                                    testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_03));
                        })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            logger.debug("Creating Mediation file ....");
                            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                            List<String> cdrLine = new ArrayList<>();
                            cdrLine.addAll(Arrays.asList(String.format(SCONNECT_DATA_CDR_FORMAT_DU)));
                            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(),
                                    ".txt", null, cdrLine);
                            logger.debug("Mediation file created {}", cdrFilePath);
                            uuid = api.triggerMediationByConfigurationByFile(
                                    getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                            assertNotNull("Mediation trigger failed", uuid);
                            logger.debug("Mediation ProcessId {}", uuid);
                            pauseUntilMediationCompletes(20, api);
                        })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            MediationProcess mediationProcess = api.getMediationProcess(uuid);
                            logger.debug("Mediation Process {}", mediationProcess);
                            assertEquals("Mediation Done And Billable ", Integer.valueOf(1),
                                    mediationProcess.getDoneAndBillable());
                            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0),
                                    mediationProcess.getDoneAndNotBillable());
                            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_03));
                            assertNotNull("Mediation Should Create Order", order);
                            assertEquals(
                                    "Invalid resolved quantity",
                                    new BigDecimal("234346365").divide(new BigDecimal(1024 * 1024 * 1024)).setScale(2,
                                            BigDecimal.ROUND_HALF_UP), order.getOrderLines()[0].getQuantityAsDecimal()
                                            .setScale(2, BigDecimal.ROUND_HALF_UP));
                            assertEquals("Invalid order amount", new BigDecimal("2.18"), order.getTotalAsDecimal()
                                    .setScale(2, BigDecimal.ROUND_HALF_UP));
                        });
    }


    public static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
            String fieldName) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
                return ws;
            }
        }
        return null;
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
