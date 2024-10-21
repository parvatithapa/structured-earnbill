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
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
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

@Test(testName = "spc.AaptInternetMediationTest")
public class AaptInternetMediationTest  extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String  USER_01                                 =  "spc0249388477@southernphone.com.au";
    private static final String  USER_02                                 =  "spc0354332313@southernphone.com.au";
    private static final String  AAPT_INTERNET_PLAN_ITEM                 =  "aapt-internet-plan-subscription-item";
    private static final String  AAPT_INTERNET_PLAN_CODE                 = "aapt-internet-Plan";
    private static final String  AAPT_INTERNET_PRODUCT                   = "aapt_internet_product";
    private static final String  DOMAIN_TIME_COMMENT                     = "*Domain: southernphone.com.au";
    private static final String  DAILY_USAGE_DATE_COMMENT                = "*Daily usage report on 2019-01-08";
    private static final String  PROCESSING_TIME_COMMENT                 = "*Generating at: Wed Jan 09 00:55:39 EST 2019";
    private static final String  AAPT_INTERNET_CDR_FORMAT                = "spc0249388477@southernphone.com.au,53712209,116939256,0354332313";
    private static final String  AAPT_INTERNET_CDR_FORMAT_DUPLICATE      = "spc0249388478@southernphone.com.au,23712209,816939256,0354332313";
    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                    = "subscription02"+ System.currentTimeMillis();
    private static final String MEDIATION_FILE_PREFIX                    = "southernphone.com.au_Daily_";
    private static final int     MONTHLY_ORDER_PERIOD                    =  2;
    public static final String COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID = "AAPT Internet Item Id";

    private Integer enumId1;
    private Integer enumId2;
    int productId;
    private UUID uuid;

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            RatingConfigurationWS ratingConfiguration = new RatingConfigurationWS(api.getRatingUnit(spcDataUnitId), null);
            // Creating AApt Internet Product
            productId = buildAndPersistFlatProductWithRating(envBuilder, api, AAPT_INTERNET_PRODUCT, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false,
                    com.sapienter.jbilling.server.util.Util.getEpochDate(), ratingConfiguration);
            // create plan item for Aapt Internet
            buildAndPersistFlatProduct(envBuilder, api, AAPT_INTERNET_PLAN_ITEM, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);

            // Build flat pricing strategy
            PriceModelWS aaptInternetUsagePriceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(),
                    new BigDecimal("10"), api.getCallerCurrencyId());

            enumId1 = buildAndPersistEnumeration(envBuilder, ENUM_QUANTITY_RESOLUTION_UNIT_VALUES, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT);
            enumId2 = buildAndPersistEnumeration(envBuilder, ENUM_INTERNET_TECHNOLOGY_VALUES, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE);
            if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT)){
                buildAndPersistMetafield(testBuilder, PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT, DataType.ENUMERATION, EntityType.PLAN);
            }
            if (!isMetaFieldPresent(EntityType.PLAN, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE)){
                buildAndPersistMetafield(testBuilder, PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE, DataType.ENUMERATION, EntityType.PLAN);
            }

            if (!isMetaFieldPresent(EntityType.COMPANY, COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID)){
                buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID,
                        DataType.STRING, EntityType.COMPANY);
            }

            // Setting Company Level Meta Fields
            setCompanyLevelMetaField(testBuilder.getTestEnvironment(), COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID,
                    envBuilder.idForCode(AAPT_INTERNET_PRODUCT).toString());

            PlanItemWS aaptInternetUsagePlanItem = buildPlanItem(envBuilder.idForCode(AAPT_INTERNET_PRODUCT),
                    MONTHLY_ORDER_PERIOD, "0.00", aaptInternetUsagePriceModel, null);

            PlanItemWS aaptAssetPlanItem = buildPlanItem(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE),
                    MONTHLY_ORDER_PERIOD, "0.00", aaptInternetUsagePriceModel, null);

            // Create plan for Aapt Internet
            // Integer planId =
            Integer planId = buildAndPersistPlan(envBuilder, api, AAPT_INTERNET_PLAN_CODE, "100 Aapt Interent Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(AAPT_INTERNET_PLAN_ITEM), Collections.emptyList(), aaptInternetUsagePlanItem, aaptAssetPlanItem);
            setPlanLevelMetaFieldForInternet(planId, ENUM_INTERNET_TECHNOLOGY_VALUES.get(1).getValue(),ENUM_QUANTITY_RESOLUTION_UNIT_VALUES.get(2).getValue());
            // configure spc usage manager task.
            Map<String, String> params = new HashMap<>();
            params.put("VOIP_Usage_Field_Name", "SERVICE_NUMBER");
            params.put("Internate_Usage_Field_Name", "USER_NAME");
            updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                    SPCUsageManagerTask.class.getName(), params);
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Aapt Internet Product Creation Failed", testEnvBuilder.idForCode(AAPT_INTERNET_PRODUCT));
            assertNotNull("Aapt Internet Plan Creation Failed", testEnvBuilder.idForCode(AAPT_INTERNET_PLAN_CODE));
        });
    }


    @Test(priority = 1)
    void test01AaptInternetMediationUpload() {
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

            Integer userNameAsAsset = buildAndPersistAsset(envBuilder,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), "spc0249388477@southernphone.com.au", "asset-01"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(AAPT_INTERNET_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), Arrays.asList(userNameAsAsset));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_01);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_01));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 01 Creation Failed", testEnvBuilder.idForCode(USER_01));
            assertNotNull("subscription order 01 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_01));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            List<String> cdrLine = new ArrayList<>();
            cdrLine.addAll(Arrays.asList(String.format(DOMAIN_TIME_COMMENT),
                    String.format(DAILY_USAGE_DATE_COMMENT),
                    String.format(PROCESSING_TIME_COMMENT),
                    String.format(AAPT_INTERNET_CDR_FORMAT)));

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
            assertEquals("Invalid resolved quantity", new BigDecimal("0.16"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("1.59"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    @Test(priority = 2, enabled=true)
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
                    envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), "spc0249388478@southernphone.com.au", "asset-02"+ System.currentTimeMillis());

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(AAPT_INTERNET_PLAN_ITEM), BigDecimal.ONE));

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
            cdrLine.addAll(Arrays.asList(String.format(DOMAIN_TIME_COMMENT),
                    String.format(DAILY_USAGE_DATE_COMMENT),
                    String.format(PROCESSING_TIME_COMMENT),
                    String.format(AAPT_INTERNET_CDR_FORMAT_DUPLICATE),
                    String.format(AAPT_INTERNET_CDR_FORMAT_DUPLICATE)));

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
            //1 duplicate as recorde username and  event date is same
            assertEquals("Mediation Duplicate", Integer.valueOf(1), mediationProcess.getDuplicates());
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