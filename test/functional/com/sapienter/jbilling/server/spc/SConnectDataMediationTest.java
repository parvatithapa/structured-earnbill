package com.sapienter.jbilling.server.spc;

import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
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
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

@Test(testName = "spc.SConnectDataMediationTest")
public class SConnectDataMediationTest  extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String USER_01                                  = "AVC01" + ("" + System.currentTimeMillis()).substring(4);
    private static final String USER_02                                  = "AVC02" + ("" + System.currentTimeMillis()).substring(4);
    private static final String USER_03                                  = "AVC03" + ("" + System.currentTimeMillis()).substring(4);
    private static final String USER_001                                 = "AVC001" + ("" + System.currentTimeMillis()).substring(4);
    private static final String USER_002                                 = "AVC002" + ("" + System.currentTimeMillis()).substring(4);
    private static final String USER_003                                 = "AVC003" + ("" + System.currentTimeMillis()).substring(4);
    private static final String SCONNECT_DATA_PLAN_ITEM                  = "sconnect-data-plan-subscription-item";
    private static final String SCONNECT_DATA_PLAN_ITEM_DOWNUSG          = "sconnect-data-plan-subscription-item-dwnlUsg";
    private static final String SCONNECT_DATA_PLAN_CODE                  = "sconnect-data-Plan";
    private static final String SCONNECT_DATA_PLAN_CODE_FORDWNUSG        = "sconnect-data-Plan-For-Dwnld-Usg";
    private static final String SCONNECT_DATA_PRODUCT                    = "sconnect-data_product";
    private static final String SCONNECT_DATA_PRODUCT_DOWNLOAD_USG       = "sconnect_data_product_download_usage";
    private static final String SCONNECT_DATA_CDR_FORMAT                 = USER_01 + ",26,13329468,23/05/2018 11:10,23/05/2018 11:12,121,565644534535,8843";
    private static final String SCONNECT_DATA_CDR_FORMAT_DU              = USER_03 + ",26,13328987,23/05/2018 10:59,23/05/2018 11:03,205,234346160,765644534535";
    private static final String SCONNECT_DATA_CDR_FORMAT_DUPLICATE       = USER_02 + ",26,13329468,23/05/2018 11:10,23/05/2018 11:12,121,765644534535,9843";
    private static final String SCONNECT_DATA_CDR_FORMAT_1               = USER_001 + ",26,23329468,eventStartDate 11:10,eventEndDate 11:12,121,565644534535,8843";
    private static final String SCONNECT_DATA_CDR_FORMAT_2               = USER_001 + ",26,23328987,eventStartDate 10:59,eventEndDate 11:03,205,234346160,765644534535";
    private static final String SCONNECT_DATA_CDR_FORMAT_3               = USER_001 + ",26,23329469,eventStartDate 11:10,eventEndDate 11:12,121,765644533333,9843";
    private static final String SCONNECT_DATA_CDR_FORMAT_4               = USER_002 + ",26,23329470,eventStartDate 11:10,eventEndDate 11:12,121,765644533333,9843";
    private static final String SCONNECT_DATA_CDR_FORMAT_5               = USER_003 + ",26,23329471,eventStartDate 11:10,eventEndDate 11:12,121,765644533333,9843";
    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                    = "subscription02"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_03                    = "subscription03"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_06                    = "subscription06"+ System.currentTimeMillis();
    private static final String MEDIATION_FILE_PREFIX                    = "SConnect_Data";
    private static final int    MONTHLY_ORDER_PERIOD                    = 2;
    public static final String  COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID = "SConnect Internet Item Id";

    int productId;
    int dUsgproductId;
    private UUID uuid;
    private Integer enumId1;
    private Integer enumId2;
    private List<Integer> invoicesToDelete = new ArrayList<>();
    private List<Integer> ordersToDelete = new ArrayList<>();
    private List<Integer> usersToDelete = new ArrayList<>();
    private List<Integer> itemsToDelete = new ArrayList<>();

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
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
            if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT)){
            enumId1 = buildAndPersistEnumeration(envBuilder, ENUM_QUANTITY_RESOLUTION_UNIT_VALUES, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT);
            }
            if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE)){
            enumId2 = buildAndPersistEnumeration(envBuilder, ENUM_INTERNET_TECHNOLOGY_VALUES, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE);
            }
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

            // Integer planId =
            Integer planId = buildAndPersistPlan(envBuilder, api, SCONNECT_DATA_PLAN_CODE, "100 Vocus Interent Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), Collections.emptyList(), vocusInternetUsagePlanItem, vocusAssetPlanItemUPL);
            setPlanLevelMetaFieldForInternet(planId, ENUM_INTERNET_TECHNOLOGY_VALUES.get(1).getValue(),ENUM_QUANTITY_RESOLUTION_UNIT_VALUES.get(2).getValue());

            Integer downLoadUsageplanId =  buildAndPersistPlan(envBuilder, api, SCONNECT_DATA_PLAN_CODE_FORDWNUSG, "100 Vocus Interent Plan for DUsg", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM_DOWNUSG), Collections.emptyList(), vocusInternetDownloadUsagePlanItem, vocusAssetPlanItemDWL);
            setPlanLevelMetaFieldForInternet(downLoadUsageplanId, ENUM_INTERNET_TECHNOLOGY_VALUES.get(1).getValue(),ENUM_QUANTITY_RESOLUTION_UNIT_VALUES.get(0).getValue());
            // configure spc usage manager task.
            Map<String, String> params = new HashMap<>();
            params.put("VOIP_Usage_Field_Name", "SERVICE_NUMBER");
            params.put("Internate_Usage_Field_Name", "USER_NAME");
            updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                    SPCUsageManagerTask.class.getName(), params);
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("SConnect Data Product Creation Failed",
                    testEnvBuilder.idForCode(SCONNECT_DATA_PRODUCT));
            assertNotNull("SConnect Data Plan Creation Failed", testEnvBuilder.idForCode(SCONNECT_DATA_PLAN_CODE));
        });
    }


    @Test(priority = 1)
    void test01VocusInternetMediationUpload() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user01 = envBuilder.customerBuilder(api)
                    .withUsername(USER_01)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            logger.debug("User created {}", user01.getId());
            usersToDelete.add(user01.getId());
            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), USER_01, "asset-01"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_01);
            ordersToDelete.add(orderId);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_01));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 01 Creation Failed", testEnvBuilder.idForCode(USER_01));
            assertNotNull("subscription order 01 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_01));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            List<String> cdrLine = new ArrayList<>();
            cdrLine.addAll(Arrays.asList(String.format(SCONNECT_DATA_CDR_FORMAT)));
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
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_01));
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            validatePricingFields(viewEvents);
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Invalid resolved quantity", new BigDecimal("526.80"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("5267.98"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
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
            usersToDelete.add(user02.getId());
            logger.debug("User created {}", user02.getId());

            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), "AVC000058969035", "asset-02"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_02, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_02);
            ordersToDelete.add(orderId);
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
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user03 = envBuilder.customerBuilder(api)
                    .withUsername(USER_03)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            usersToDelete.add(user03.getId());
            logger.debug("User created {}", user03.getId());

            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), USER_03, "asset-03"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM_DOWNUSG), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_03, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_03);
            ordersToDelete.add(orderId);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_03));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 03 Creation Failed", testEnvBuilder.idForCode(USER_03));
            assertNotNull("subscription order 03 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_03));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            List<String> cdrLine = new ArrayList<>();
            cdrLine.addAll(Arrays.asList(String.format(SCONNECT_DATA_CDR_FORMAT_DU)));
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
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_03));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Invalid resolved quantity", new BigDecimal("0.22"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("2.18"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    @Test(priority = 4)
    void test04mediationRulesTest() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user01 = envBuilder.customerBuilder(api)
                    .withUsername(USER_001)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            usersToDelete.add(user01.getId());
            logger.debug("User created {}", user01.getId());

            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), USER_001, "asset-001"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.add(Calendar.MONTH, -2);
            activeSinceDate.set(Calendar.DATE, 1);

            Calendar activeUntilDate = Calendar.getInstance();
            activeUntilDate.add(Calendar.MONTH, -2);
            activeUntilDate.set(Calendar.DATE, activeUntilDate.getActualMaximum(Calendar.DAY_OF_MONTH));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSinceDate.getTime(), activeUntilDate.getTime(), MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_001);
            ordersToDelete.add(orderId);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_001));


        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 01 Creation Failed", testEnvBuilder.idForCode(USER_001));
            assertNotNull("subscription order 01 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_01));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            List<String> cdrLine = new ArrayList<>();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Calendar eventDate = Calendar.getInstance();

            // event date within active since and active until dates
            eventDate.add(Calendar.MONTH, -2);
            eventDate.set(Calendar.DATE, 10);
            String eventDateStr = simpleDateFormat.format(eventDate.getTime());
            String sConnectDataCdrFormat1= SCONNECT_DATA_CDR_FORMAT_1.replace("eventStartDate", eventDateStr);
            sConnectDataCdrFormat1 = sConnectDataCdrFormat1.replace("eventEndDate", eventDateStr);

            //event date in past than active since date
            eventDate.add(Calendar.MONTH, -2);
            eventDateStr = simpleDateFormat.format(eventDate.getTime());
            String sConnectDataCdrFormat2= SCONNECT_DATA_CDR_FORMAT_2.replace("eventStartDate", eventDateStr);
            sConnectDataCdrFormat2 = sConnectDataCdrFormat2.replace("eventEndDate", eventDateStr);

            //event date in future of active until date
            eventDate = Calendar.getInstance();
            eventDate.add(Calendar.DATE, -1);
            eventDateStr = simpleDateFormat.format(eventDate.getTime());
            String sConnectDataCdrFormat3= SCONNECT_DATA_CDR_FORMAT_3.replace("eventStartDate", eventDateStr);
            sConnectDataCdrFormat3 = sConnectDataCdrFormat3.replace("eventEndDate", eventDateStr);

            cdrLine.addAll(Arrays.asList(String.format(sConnectDataCdrFormat1), String.format(sConnectDataCdrFormat2),
                    String.format(sConnectDataCdrFormat3)));
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".txt", null, cdrLine);
            logger.debug("Mediation file created {}", cdrFilePath);
            uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", uuid);
            logger.debug("Mediation ProcessId {}", uuid);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(uuid);
            logger.debug("Mediation Process {}", mediationProcess);
            JbillingMediationErrorRecord[] errors = api.getMediationErrorRecordsByMediationProcess(uuid, 0);
            Stream.of(errors).forEach(error -> {
                assertTrue("Mediation errors should be JB-USER-NOT-RESOLVED or ERR-EVENT-DATE-IS-AFTER",
                        error.getErrorCodes().contains("JB-USER-NOT-RESOLVED") || error.getErrorCodes().contains("ERR-EVENT-DATE-IS-AFTER"));
            });
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Errors detected", Integer.valueOf(2), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_001));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Invalid resolved quantity", new BigDecimal("526.80"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("5267.98"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

        });
    }

    @Test(priority = 5)
    void test05mediationRulesTest() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user01 = envBuilder.customerBuilder(api)
                    .withUsername(USER_002)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            usersToDelete.add(user01.getId());
            logger.debug("User created {}", user01.getId());

            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), USER_002, "asset-002"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.add(Calendar.MONTH, -1);
            activeSinceDate.set(Calendar.DATE, 1);

            Calendar activeUntilDate = Calendar.getInstance();
            activeUntilDate.add(Calendar.MONTH, -1);
            activeUntilDate.set(Calendar.DATE, activeUntilDate.getActualMaximum(Calendar.DAY_OF_MONTH));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_02, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_002);
            ordersToDelete.add(orderId);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_002));
            Integer[] invoices = api.createInvoiceWithDate(user01.getId(),
                    activeSinceDate.getTime(), null, null, false);
            invoicesToDelete.add(invoices[0]);
            logger.debug("invoice created {} for user {} for date {}", invoices, user01.getId(), activeSinceDate.getTime());
            assertNotNull("invoice Creation Failed for user "+ user01.getId(), invoices);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 02 Creation Failed", testEnvBuilder.idForCode(USER_002));
            assertNotNull("subscription order 02 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_02));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            List<String> cdrLine = new ArrayList<>();

            Calendar eventDate = Calendar.getInstance();
            eventDate.add(Calendar.MONTH, -1);
            eventDate.set(Calendar.DATE, 10);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String eventDateStr = simpleDateFormat.format(eventDate.getTime());
            String sConnectDataCdrFormat= SCONNECT_DATA_CDR_FORMAT_4.replace("eventStartDate", eventDateStr);
            sConnectDataCdrFormat = sConnectDataCdrFormat.replace("eventEndDate", eventDateStr);
            cdrLine.addAll(Arrays.asList(String.format(sConnectDataCdrFormat)));
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".txt", null, cdrLine);
            logger.debug("Mediation file created {}", cdrFilePath);
            uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME),
                    new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", uuid);
            logger.debug("Mediation ProcessId {}", uuid);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(uuid);
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_002));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Invalid resolved quantity", new BigDecimal("713.06"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("7130.62"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            cal.set(Calendar.DATE, 1);
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            assertTrue("Mediated order should be active since current months period", df.format(cal.getTime()).equals(df.format(order.getActiveSince())));
        });
    }
    
    @Test(priority = 6)
    void test06mediationThreeDayBillingDelayOrderExcludeTest() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user01 = envBuilder.customerBuilder(api)
                    .withUsername(USER_003)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            usersToDelete.add(user01.getId());
            logger.debug("User created {}", user01.getId());

            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), USER_003, "asset-003"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SCONNECT_DATA_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.add(Calendar.MONTH, -1);
            activeSinceDate.set(Calendar.DATE, 1);

            Calendar activeUntilDate = Calendar.getInstance();
            activeUntilDate.add(Calendar.MONTH, -1);
            activeUntilDate.set(Calendar.DATE, activeUntilDate.getActualMaximum(Calendar.DAY_OF_MONTH));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_06, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_003);
            ordersToDelete.add(orderId);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_003));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 003 Creation Failed", testEnvBuilder.idForCode(USER_003));
            assertNotNull("subscription order 04 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_06));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            List<String> cdrLine = new ArrayList<>();

            Calendar eventDate = Calendar.getInstance();
            eventDate.add(Calendar.MONTH, -1);
            eventDate.set(Calendar.DATE, 10);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String eventDateStr = simpleDateFormat.format(eventDate.getTime());
            String sConnectDataCdrFormat= SCONNECT_DATA_CDR_FORMAT_5.replace("eventStartDate", eventDateStr);
            sConnectDataCdrFormat = sConnectDataCdrFormat.replace("eventEndDate", eventDateStr);
            cdrLine.addAll(Arrays.asList(String.format(sConnectDataCdrFormat)));
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".txt", null, cdrLine);
            logger.debug("Mediation file created {}", cdrFilePath);
            uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME),
                    new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", uuid);
            logger.debug("Mediation ProcessId {}", uuid);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(uuid);
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_003));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Invalid resolved quantity", new BigDecimal("713.06"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("7130.62"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.add(Calendar.MONTH, -1);
            activeSinceDate.set(Calendar.DATE, 1);
            Integer[] invoiceId = api.createInvoiceWithDate(testEnvBuilder.idForCode(USER_003),activeSinceDate.getTime(),null,null,false);
            invoicesToDelete.add(invoiceId[0]);
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            assertTrue("Mediated order should be active since current months period", df.format(activeSinceDate.getTime()).equals(df.format(order.getActiveSince())));
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
        // configure again BasicItemManager task.
        updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                BasicItemManager.class.getName(), Collections.emptyMap());
        invoicesToDelete.forEach(api :: deleteInvoice);

        ordersToDelete.forEach(api :: deleteOrder);

        usersToDelete.forEach(api :: deleteUser);
        itemsToDelete.stream().forEach(api :: deleteItem);
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
        super.tearDown();
    }
}