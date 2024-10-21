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

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

@Test(testName = "spc.ServiceElementsVoiceMediationTest")
public class ServiceElementsVoiceMediationTest extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SEVOICE_RATE_CARD_HEADER = "id,name,surcharge,initial_increment,subsequent_increment,charge,markup,capped_charge,capped_increment,minimum_charge,tariff_code,route_id";
    private static final String SEVOICE_RATE_CARD_NAME   = "se_voice_rate_card";
    private static final String OPERATOR_ASSISTED_CALL   = "se-voice-call";
    private static final String SEVOICE_PLAN_ITEM        = "se-voice-plan-subscription-item";
    private static final String SEVOICE_PLAN_CODE        = "se-voice-Plan";

    private static final String USER_01                  = "SE-Voice-Test-User-01";
    private static final String USER_02                  = "SE-Voice-Test-User-02";
    private static final String ASSET01_NUMBER           = "8940000600";
    private static final String ASSET02_NUMBER           = "8940000601";
    private static final String SUBSCRIPTION_ORDER_01    = "se-subscription-order-01";
    private static final String SUBSCRIPTION_ORDER_02    = "se-subscription-order-02";
    private static final String MEDIATION_FILE_PREFIX    = "BBS";

    private static final int MONTHLY_ORDER_PERIOD        = 2;
    private static final int NUMBER_ASSET_PRODUCT_ID     = 320104;

    private Integer sevoiceRouteRateCardId;
    private Integer planRatingEnumId;

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            //Uploading route rate card of SE Voice
            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName(SEVOICE_RATE_CARD_NAME);
            routeRateCardWS.setRatingUnitId(spcRatingUnitId);
            routeRateCardWS.setEntityId(api.getCallerCompanyId());
            List<String> rateRecords =
                    Arrays.asList("1,se-voice-call,0.35,60,60,2.25,0,0,0,0,AUS:MOB,AUSTRALIA - MOBILE",
                            "2,se-voice-call,0.45,60,60,1.15,0,0,0,0,AUS:REG,AUSTRALIA - REGIONAL - BUNBURY");

            String sevoiceRouteRateCardFilePath = createFileWithData(SEVOICE_RATE_CARD_NAME, ".csv", SEVOICE_RATE_CARD_HEADER, rateRecords);
            logger.debug("SE Voice Route Rate card file path {}", sevoiceRouteRateCardFilePath);
            sevoiceRouteRateCardId = api.createRouteRateCard(routeRateCardWS, new File(sevoiceRouteRateCardFilePath));
            logger.debug("SE Voice Route Rate Card id {}", sevoiceRouteRateCardId);
            Integer matchingFieldId = api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, sevoiceRouteRateCardId));
            logger.debug("Matching Field id {}", matchingFieldId);

            //Create plan rating enumeration
            EnumerationValueWS valueWS = new EnumerationValueWS();
            valueWS.setValue(SEVOICE_RATE_CARD_NAME);

            EnumerationWS enumeration = new EnumerationWS();
            enumeration.setEntityId(api.getCallerCompanyId());
            enumeration.setName(ENUMERATION_METAFIELD_NAME);
            enumeration.setValues(Arrays.asList(valueWS));
            planRatingEnumId = api.createUpdateEnumeration(enumeration);

            // Creating plan level meta-field
            buildAndPersistMetafield(testBuilder, PLAN_LEVEL_METAFIELD, DataType.ENUMERATION, EntityType.PLAN);
            //Create usage product for SE Voice
            buildAndPersistFlatProduct(envBuilder, api, OPERATOR_ASSISTED_CALL, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false);

            //create plan item for SE Voice
            buildAndPersistFlatProduct(envBuilder, api, SEVOICE_PLAN_ITEM, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);

            //Build rate card pricing strategy
            PriceModelWS sevoiceOperatorAssistedCallPriceModel = buildRateCardPriceModel(sevoiceRouteRateCardId, "DURATION");

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            PlanItemWS sevoiceOperatorAssistedCallPlanItem = buildPlanItem(envBuilder.idForCode(OPERATOR_ASSISTED_CALL),
                    MONTHLY_ORDER_PERIOD, "0.00", sevoiceOperatorAssistedCallPriceModel, pricingDate.getTime());

            //Create plan for SE Voice
            Integer planId =  buildAndPersistPlan(envBuilder,api, SEVOICE_PLAN_CODE, "100 SE Voice Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(SEVOICE_PLAN_ITEM), Collections.emptyList() , sevoiceOperatorAssistedCallPlanItem);

            setPlanLevelMetaField(planId, SEVOICE_RATE_CARD_NAME);
            // configure spc usage manager task.
            Map<String, String> params = new HashMap<>();
            params.put("VOIP_Usage_Field_Name", "SERVICE_NUMBER");
            params.put("Internate_Usage_Field_Name", "USER_NAME");
            updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                    SPCUsageManagerTask.class.getName(), params);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("SE Voice Product Creation Failed", testEnvBuilder.idForCode(OPERATOR_ASSISTED_CALL));
            assertNotNull("SE Voice Plan Creation Failed", testEnvBuilder.idForCode(SEVOICE_PLAN_CODE));
        });
    }

    @Test(priority = 1)
    void test01SEVoiceMediationUpload() {
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        List<Integer> assets = new ArrayList<>();
        try {
            testBuilder.given(envBuilder -> {
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

                ItemDTOEx assetEnabledProduct = api.getItem(NUMBER_ASSET_PRODUCT_ID, null, null);
                Integer assetId = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], NUMBER_ASSET_PRODUCT_ID, ASSET01_NUMBER,
                        "asset-01"+ System.currentTimeMillis());
                assertNotNull("Creation of Asset Number 01 is failed", assetId);
                logger.debug("Asset is created {} for number {}", assetId, ASSET01_NUMBER);
                assets.add(assetId);

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.putAll(buildProductQuantityEntry(NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SEVOICE_PLAN_ITEM), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(NUMBER_ASSET_PRODUCT_ID, assets);

                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);

                Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSinceDate.getTime(), null,
                        MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap, USER_01);
                assertNotNull("Creation of subscription order 01 is failed", orderId);
                logger.debug("Subscription order id {} is created for user {}", orderId, envBuilder.idForCode(USER_01));
                orders.add(orderId);

            }).validate((testEnv, testEnvBuilder) -> {
                logger.debug("Creating mediation CDR file...");
                List<String> cdrLines = Arrays.asList("8940000600,450415177,2019-01-21 10:05:18,322,AUSTRALIA - MOBILE");
                logger.debug("cdr lines {}", cdrLines);
                String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, cdrLines);
                logger.debug("Mediation file is created {}", cdrFilePath);

                final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                assertNotNull("Mediation trigger is failed", mediationProcessId);
                logger.debug("Mediation Process Id {}", mediationProcessId);
                pauseUntilMediationCompletes(20, api);

            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                assertEquals("Mediation Error Records", Integer.valueOf(0), mediationProcess.getErrors());
                assertEquals("Mediation Duplicate Records", Integer.valueOf(0), mediationProcess.getDuplicates());

                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_01));
                assertNotNull("Mediation Should Create Order", order);
                assertEquals("Invalid mediated order line,", 1, order.getOrderLines().length);
                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                assertEquals("Invalid original quantity", new BigDecimal("322.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                validatePricingFields(viewEvents);
                OrderLineWS callLine = getLineByItemId(order, testEnvBuilder.idForCode(OPERATOR_ASSISTED_CALL));
                assertNotNull("SE Voice usage item not found", callLine);
                assertEquals("Invalid item line quantity,", new BigDecimal("6.00"),
                        callLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid item line amount,", new BigDecimal("13.85"),
                        callLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid mediated order amount,", new BigDecimal("13.85"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            });
        } finally {
            orders.stream().forEach(api :: deleteOrder);
            users.stream().forEach(api :: deleteUser);
            assets.stream().forEach(api :: deleteAsset);
        }
    }

    @Test(priority = 2)
    void test02SEVoiceRecycleMediation() {
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        List<Integer> assets = new ArrayList<>();
        try {
            testBuilder.given(envBuilder -> {
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

            }).validate((testEnv, testEnvBuilder) -> {
                final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                logger.debug("Creating mediation CDR file...");
                List<String> cdrLines = Arrays.asList("8940000601,897957002,2019-01-21 19:06:18,59,AUSTRALIA - REGIONAL - BUNBURY");
                logger.debug("cdr lines {}", cdrLines);
                String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, cdrLines);
                logger.debug("Mediation file is created {}", cdrFilePath);

                UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
                assertNotNull("Mediation trigger is failed", mediationProcessId);
                logger.debug("Mediation Process Id {}", mediationProcessId);
                pauseUntilMediationCompletes(20, api);

                //Trigger mediation process
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                assertEquals("Mediation Duplicate Records", Integer.valueOf(0), mediationProcess.getDuplicates());
                assertEquals("Mediation Error Records", Integer.valueOf(1), mediationProcess.getErrors());

            }).validate((testEnv, testEnvBuilder) -> {
                //Creating subscription order with asset
                ItemDTOEx assetEnabledProduct = api.getItem(NUMBER_ASSET_PRODUCT_ID, null, null);
                Integer assetId = buildAndPersistAsset(testEnvBuilder, assetEnabledProduct.getTypes()[0], NUMBER_ASSET_PRODUCT_ID, ASSET02_NUMBER,
                        "asset-02"+ System.currentTimeMillis());
                assertNotNull("Creation of Asset Number 02 is failed", assetId);
                logger.debug("Asset is created {} for number {}", assetId, ASSET02_NUMBER);
                assets.add(assetId);

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.putAll(buildProductQuantityEntry(NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
                productQuantityMap.putAll(buildProductQuantityEntry(testEnvBuilder.idForCode(SEVOICE_PLAN_ITEM), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(NUMBER_ASSET_PRODUCT_ID, assets);

                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);

                Integer orderId = createOrder(SUBSCRIPTION_ORDER_02, activeSinceDate.getTime(), null,
                        MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap, USER_02);
                assertNotNull("Creation of subscription order 02 is failed", orderId);
                logger.debug("Subscription order id {} is created for user {}", orderId, testEnvBuilder.idForCode(USER_02));
                orders.add(orderId);

                //Trigger mediation recycle
                final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                UUID mediationProcessId = api.getMediationProcessStatus().getMediationProcessId();
                logger.debug("trigger recycle process for mediation process {}", mediationProcessId);
                UUID recycleMediationProcessId = api.runRecycleForProcess(mediationProcessId);
                logger.debug("Recycle mediation process id is {} for mediation process id {}", recycleMediationProcessId, mediationProcessId);
                pauseUntilMediationCompletes(20, api);

            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                assertEquals("Mediation Error Records", Integer.valueOf(0), mediationProcess.getErrors());
                assertEquals("Mediation Duplicate Records", Integer.valueOf(0), mediationProcess.getDuplicates());

                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_02));
                assertNotNull("Mediation should create order", order);
                assertEquals("Invalid mediated order line,", 1, order.getOrderLines().length);

                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                assertEquals("Invalid original quantity", new BigDecimal("59.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));

                OrderLineWS callLine = getLineByItemId(order, testEnvBuilder.idForCode(OPERATOR_ASSISTED_CALL));
                assertNotNull("SE voice usage item not found", callLine);
                assertEquals("Invalid item line quantity,", new BigDecimal("1.00"),
                        callLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid item line amount,", new BigDecimal("1.60"),
                        callLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid mediated order amount,", new BigDecimal("1.60"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            });
        } finally {
            orders.stream().forEach(api :: deleteOrder);
            users.stream().forEach(api :: deleteUser);
            assets.stream().forEach(api :: deleteAsset);
        }
    }

    @Override
    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        // configure again BasicItemManager task.
        updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                BasicItemManager.class.getName(), Collections.emptyMap());
        super.tearDown();
        if(null!= planRatingEnumId) {
            try {
                api.deleteEnumeration(planRatingEnumId);
            } catch(Exception ex) {
                logger.error("enum deletion failed", ex);
            }
        }
        if(null!= sevoiceRouteRateCardId) {
            try {
                api.deleteRouteRateCard(sevoiceRouteRateCardId);
            } catch(Exception ex) {
                logger.error("route rate card deletion failed",ex);
            }
        }
    }
}