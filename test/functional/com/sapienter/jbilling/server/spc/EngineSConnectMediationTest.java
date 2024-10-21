package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
import com.sapienter.jbilling.server.mediation.JMRPostProcessorTask;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

@Test(testName = "spc.EngineSConnecteMediationTest")
public class EngineSConnectMediationTest  extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ENGINE_SCONNECT_RATE_CARD_NAME           = "engine_sconnect_plan_rating_card";
    private static final String ENGINE_SCONNECT_RATE_CARD_HEADER         = "id,name,surcharge,initial_increment,subsequent_increment,charge,"
            + "markup,capped_charge,capped_increment,minimum_charge,route_id,tariff_code";
    private static final String  OPERATOR_ASSISTED_CALL                  = "Operator Assisted Call";
    private static final String  ENGINE_SCONNECT_PLAN_ITEM               = "testPlanSubscriptionItem" + System.currentTimeMillis();
    private static final int     MONTHLY_ORDER_PERIOD                    =  2;
    private static final String  ENGINE_SCONNECT_PLAN_CODE               =  "engineSconnect-Plan" + System.currentTimeMillis();
    private static final String  USER_01                                 =  "EnginSConnect-TestUser01" + System.currentTimeMillis();
    private static final String  USER_02                                 =  "EnginSConnect-TestUser02" + System.currentTimeMillis();
    private static final String  USER_03                                 =  "EnginSConnect-TestUser03" + System.currentTimeMillis();
    private static final String  USER_04                                 =  "EnginSConnect-TestUser04" + System.currentTimeMillis();
    private static final String  USER_05                                 =  "EnginSConnect-TestUser05" + System.currentTimeMillis();
    private static final String USER_06                                 =    "EnginSConnect-TestUser06" + System.currentTimeMillis();
    private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID   =  320104;
    private static final String  ENGINE_SCONNECT_CDR_FORMAT              = "BW093958429271118568882470@210.50.23.64,0243897012,61243883626,AUSTRALIA,"
            + "2018-11-27 20:40:22,2018-11-27 20:41:33,71,2,Local,Engin National,0.000,0,0,,";
    private static final String  ENGINE_SCONNECT_CDR_FORMAT_FOR_RECYCLE  = "BW233333384261118868588764@210.50.23.64,0266578072,611300362603,AUSTRALIA,"
            + "2018-11-27 10:33:35,2018-11-27 11:02:38,1743,2,Service,engin Super,44.000,1,44,,";
    static final String  ENGINE_SCONNECT_INTERNATIONAL_CDR_FORMAT        = "BW093505326271118-1287708552@210.50.23.64,0244437881,441204305586,UK,"
            + "2018-11-27 20:35:21,2018-11-27 21:24:00,2919,3,International,engin Super,3.500,0,171,,";
    static final String  ENGINE_SCONNECT_PRECISION_CDR_FORMAT            = "BW093505326271118-1287708553@210.50.23.64,0244437882,441204305586,UK,"
            + "2018-11-27 20:35:21,2018-11-27 21:24:00,2919,3,International,engin Super,3.500,0,171,,";
    static final String  ENGINE_SCONNECT_OUT_OF_RANGE_PRECISION_CDR_FORMAT  = "BW093505326271118-1287708554@210.50.23.64,0244437883,341204305586,UK,"
            + "2018-11-27 20:35:21,2018-11-27 21:24:00,2919,3,International,engin Super,3.700,0,171,,";
    static final String  ENGINE_SCONNECT_CUSTOMER_CARE_CALL_CDR_FORMAT  = "BW093505326271118-1287708555@210.50.23.64,0244437884,02244747100,UK,"
            + "2018-11-27 20:35:21,2018-11-27 21:24:00,2919,3,International,engin Super,3.700,0,171,,";
    private static final String ASSET01_NUMBER                           = "0243897012";
    private static final String ASSET02_NUMBER                           = "0266578072";
    private static final String ASSET03_NUMBER                           = "0244437881";
    private static final String ASSET04_NUMBER                           = "0244437882";
    private static final String ASSET05_NUMBER                           = "0244437883";
    private static final String ASSET06_NUMBER                           = "0244437884";
    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                    = "subscription02"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_03                    = "subscription03"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_04                    = "subscription04"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_05                    = "subscription05"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_06                    = "subscription06"+ System.currentTimeMillis();
    private static final String MEDIATION_FILE_PREFIX                    = "ENGINCF";
    private Integer engineSConnectRouteRateCardId ;
    private Integer planRatingEnumId ;
    private static final int PREFERENCE_MEDIATED_ORDER_LINE_AMOUNT_PRECISION = 101;

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            //Uploading Telstra Route Rate card
            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName(ENGINE_SCONNECT_RATE_CARD_NAME);
            routeRateCardWS.setRatingUnitId(spcRatingUnitId);
            routeRateCardWS.setEntityId(api.getCallerCompanyId());

            List<String> rateRecords = Arrays.asList("1,Operator Assisted Call,0.39,60,60,0.25,0,0,0,0,en:2,OP:NAT",
                    "2,Operator Assisted Call,0.89,60,60,0.25,0,0,0,0,en:3:UK,OP:NAT");

            String enginSconnectRouteRateCardFilePath = createFileWithData(ENGINE_SCONNECT_RATE_CARD_NAME, ".csv", ENGINE_SCONNECT_RATE_CARD_HEADER, rateRecords);
            logger.debug("Engine SConnect Route Rate card file path {}", enginSconnectRouteRateCardFilePath);
            engineSConnectRouteRateCardId = api.createRouteRateCard(routeRateCardWS, new File(enginSconnectRouteRateCardFilePath));
            logger.debug("Engine SConnect Route Rate Card id {}", engineSConnectRouteRateCardId);
            Integer matchingFieldId = api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, engineSConnectRouteRateCardId));
            logger.debug("Matching Field id {}", matchingFieldId);

            // Creating Plan Rating Enumeration
            EnumerationValueWS valueWS = new EnumerationValueWS();
            valueWS.setValue(ENGINE_SCONNECT_RATE_CARD_NAME);
            if(api.getEnumerationByName(ENUMERATION_METAFIELD_NAME) == null){
                EnumerationWS enumeration = new EnumerationWS();
                enumeration.setEntityId(api.getCallerCompanyId());
                enumeration.setName(ENUMERATION_METAFIELD_NAME);
                enumeration.setValues(Arrays.asList(valueWS));
                planRatingEnumId = api.createUpdateEnumeration(enumeration);
            }

            // Creating plan level meta-field
            buildAndPersistMetafield(testBuilder, PLAN_LEVEL_METAFIELD, DataType.ENUMERATION, EntityType.PLAN);

            // Creating Usage Product Operator Assisted Call
            buildAndPersistFlatProduct(envBuilder, api, OPERATOR_ASSISTED_CALL, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false);

            // Creating Telstra PlanItem
            buildAndPersistFlatProduct(envBuilder, api, ENGINE_SCONNECT_PLAN_ITEM, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);

            // building Rate Card pricing strategy
            PriceModelWS engineSCOperatorAssistedCallPriceModel = buildRateCardPriceModel(engineSConnectRouteRateCardId, "Quantity");
            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);


            PlanItemWS engineSConnectOperatorAssistedCallPlanItem = buildPlanItem(envBuilder.idForCode(OPERATOR_ASSISTED_CALL),
                    MONTHLY_ORDER_PERIOD, "0.00", engineSCOperatorAssistedCallPriceModel, pricingDate.getTime());
            //Create plan for telstra
            Integer planId =  buildAndPersistPlan(envBuilder,api, ENGINE_SCONNECT_PLAN_CODE, "100 Engine SConnect Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), Collections.emptyList() , engineSConnectOperatorAssistedCallPlanItem);

            setPlanLevelMetaField(planId, ENGINE_SCONNECT_RATE_CARD_NAME);
            // configure spc usage manager task.
            Map<String, String> params = new HashMap<>();
            params.put("VOIP_Usage_Field_Name", "SERVICE_NUMBER");
            params.put("Internate_Usage_Field_Name", "USER_NAME");
            updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                    SPCUsageManagerTask.class.getName(), params);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Operator Assisted Call Product Creation Failed", testEnvBuilder.idForCode(OPERATOR_ASSISTED_CALL));
            assertNotNull("Engine SConnect Plan Creation Failed", testEnvBuilder.idForCode(ENGINE_SCONNECT_PLAN_CODE));
        });
    }

    @Test(priority = 1)
    void test01EngineSConnectMediationUpload() {
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

            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET01_NUMBER,
                    "asset-01"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset1, ASSET01_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset1));
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
            String cdrLine = String.format(ENGINE_SCONNECT_CDR_FORMAT);
            logger.debug("cdr line {}", cdrLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, Arrays.asList(cdrLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", mediationProcessId);
            logger.debug("Mediation ProcessId {}", mediationProcessId);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_01));
            assertNotNull("Mediation Should Create Order", order);
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            validatePricingFields(viewEvents);
            assertEquals("Invalid original quantity", new BigDecimal("71.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("2.00"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("0.89"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    @Test(priority = 2)
    void test02EngineSConnectRecycleMediation() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withUsername(USER_02)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            logger.debug("User created {}", user.getId());

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 02 Creation Failed", testEnvBuilder.idForCode(USER_02));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            String cdrLine = String.format(ENGINE_SCONNECT_CDR_FORMAT_FOR_RECYCLE);
            logger.debug("cdr line {}", cdrLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, Arrays.asList(cdrLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", mediationProcessId);
            logger.debug("Mediation ProcessId {}", mediationProcessId);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record", Integer.valueOf(1), mediationProcess.getErrors());
        }).validate((testEnv, envBuilder) -> {
            // creating subscription order
            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET02_NUMBER,
                    "asset-02"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset, ASSET02_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_02, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_02);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_02));
        }).validate((testEnv, envBuilder) -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
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
            assertEquals("Mediation Error Record", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_02));
            assertNotNull("Mediation Should Create Order", order);
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            assertEquals("Invalid original quantity", new BigDecimal("1743.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("30.00"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("7.89"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    @Test(priority = 3)
    void test03EngineSConnectInternationalCall() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withUsername(USER_03)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            logger.debug("User created {}", user.getId());

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 03 Creation Failed", testEnvBuilder.idForCode(USER_03));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            String cdrLine = String.format(ENGINE_SCONNECT_INTERNATIONAL_CDR_FORMAT);
            logger.debug("cdr line {}", cdrLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, Arrays.asList(cdrLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", mediationProcessId);
            logger.debug("Mediation ProcessId {}", mediationProcessId);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record", Integer.valueOf(1), mediationProcess.getErrors());
        }).validate((testEnv, envBuilder) -> {
            // creating subscription order
            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET03_NUMBER,
                    "asset-03"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset, ASSET03_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_03, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_03);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_03));
        }).validate((testEnv, envBuilder) -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
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
            assertEquals("Mediation Error Record", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_03));
            assertNotNull("Mediation Should Create Order", order);
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            assertEquals("Invalid original quantity", new BigDecimal("2919.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("49.00"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("13.14"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    @Test(priority = 4)
    void test04EngineSConnectPrecisionAmount() {
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        AtomicInteger jmrPostProcessorTaskId = new AtomicInteger(0);
        Hashtable<String, String> parameters = new Hashtable<>();
        parameters.put("rounding mode", "ROUND_HALF_UP");
        parameters.put("rounding scale", "4");
        parameters.put("minimum charge", "0.00");
        parameters.put("tax table name", StringUtils.EMPTY);
        parameters.put("tax date format", "dd-MM-yyyy");
        testBuilder.given(envBuilder -> {
            // configure JMRPostProcessorTask plugin.
            UserWS user = envBuilder.customerBuilder(api)
                    .withUsername(USER_04)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            logger.debug("User created {}", user.getId());

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 04 Creation Failed", testEnvBuilder.idForCode(USER_04));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            String cdrLine = String.format(ENGINE_SCONNECT_PRECISION_CDR_FORMAT);
            logger.debug("cdr line {}", cdrLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, Arrays.asList(cdrLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", mediationProcessId);
            logger.debug("Mediation ProcessId {}", mediationProcessId);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record", Integer.valueOf(1), mediationProcess.getErrors());
        }).validate((testEnv, envBuilder) -> {
            // creating subscription order
            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET04_NUMBER,
                    "asset-04"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset, ASSET04_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_04, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_04);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_04));
        }).validate((testEnv, envBuilder) -> {
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
            assertEquals("Mediation Error Record", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_04));
            assertNotNull("Mediation Should Create Order", order);
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            assertEquals("Invalid original quantity", new BigDecimal("2919.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("49.00"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("13.1400"),
                    order.getTotalAsDecimal().setScale(4, BigDecimal.ROUND_HALF_UP));
            if(jmrPostProcessorTaskId.get()!=0) {
                // delete plugin.
                api.deletePlugin(jmrPostProcessorTaskId.get());
                logger.debug("JMRPostProcessor task {} deleted", jmrPostProcessorTaskId.get());
            }
        });
    }

    @Test(priority = 5)
    void test05EngineSConnectOutOfRangePrecisionAmount() {
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        AtomicInteger jmrPostProcessorTaskId = new AtomicInteger(0);
        Hashtable<String, String> parameters = new Hashtable<>();
        parameters.put("rounding mode", "ROUND_HALF_UP");
        parameters.put("rounding scale", "1");
        parameters.put("minimum charge", "0.00");
        parameters.put("tax table name", StringUtils.EMPTY);
        parameters.put("tax date format", "dd-MM-yyyy");
        testBuilder.given(envBuilder -> {
            // configure JMRPostProcessorTask plugin.
            UserWS user = envBuilder.customerBuilder(api)
                    .withUsername(USER_05)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            logger.debug("User created {}", user.getId());

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 05 Creation Failed", testEnvBuilder.idForCode(USER_05));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            String cdrLine = String.format(ENGINE_SCONNECT_OUT_OF_RANGE_PRECISION_CDR_FORMAT);
            logger.debug("cdr line {}", cdrLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, Arrays.asList(cdrLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", mediationProcessId);
            logger.debug("Mediation ProcessId {}", mediationProcessId);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record", Integer.valueOf(1), mediationProcess.getErrors());
        }).validate((testEnv, envBuilder) -> {
            // creating subscription order
            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET05_NUMBER,
                    "asset-05"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset, ASSET05_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_05, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_05);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_05));
        }).validate((testEnv, envBuilder) -> {
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
            assertEquals("Mediation Error Record", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_05));
            assertNotNull("Mediation Should Create Order", order);
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            assertEquals("Invalid original quantity", new BigDecimal("2919.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("49.0000"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(4, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("13.1400"),
                    order.getTotalAsDecimal().setScale(4, BigDecimal.ROUND_HALF_UP));
            if(jmrPostProcessorTaskId.get()!=0) {
                // delete plugin.
                api.deletePlugin(jmrPostProcessorTaskId.get());
                logger.debug("JMRPostProcessor task {} deleted", jmrPostProcessorTaskId.get());
            }
        });
    }

    @Test(priority = 6)
    void test06EngineSConnectFreeCustomerCareCall() {
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        AtomicInteger jmrPostProcessorTaskId = new AtomicInteger(0);
        Hashtable<String, String> parameters = new Hashtable<>();
        parameters.put("rounding mode", "ROUND_HALF_UP");
        parameters.put("rounding scale", "1");
        parameters.put("minimum charge", "0.00");
        parameters.put("tax table name", StringUtils.EMPTY);
        parameters.put("tax date format", "dd-MM-yyyy");
        testBuilder.given(envBuilder -> {
            // configure JMRPostProcessorTask plugin.
            UserWS user = envBuilder.customerBuilder(api)
                    .withUsername(USER_06)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();
            logger.debug("User created {}", user.getId());

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 06 Creation Failed", testEnvBuilder.idForCode(USER_06));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            String cdrLine = String.format(ENGINE_SCONNECT_CUSTOMER_CARE_CALL_CDR_FORMAT);
            logger.debug("cdr line {}", cdrLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, Arrays.asList(cdrLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", mediationProcessId);
            logger.debug("Mediation ProcessId {}", mediationProcessId);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record", Integer.valueOf(1), mediationProcess.getErrors());
        }).validate((testEnv, envBuilder) -> {
            // creating subscription order
            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET06_NUMBER);
            logger.debug("asset created {} for number {}", asset, ASSET06_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_06, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_06);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_06));
        }).validate((testEnv, envBuilder) -> {
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
            assertEquals("Mediation Error Record", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_06));
            assertNotNull("Mediation Should Create Order", order);
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            assertEquals("Invalid original quantity", new BigDecimal("2919.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("49.0000"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(4, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("0.0000"),
                    order.getTotalAsDecimal().setScale(4, BigDecimal.ROUND_HALF_UP));
            if(jmrPostProcessorTaskId.get()!=0) {
                // delete plugin.
                api.deletePlugin(jmrPostProcessorTaskId.get());
                logger.debug("JMRPostProcessor task {} deleted", jmrPostProcessorTaskId.get());
            }
        });
    }

    @Override
    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        // configure again BasicItemManager task.
        updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                BasicItemManager.class.getName(), Collections.emptyMap());
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        super.tearDown();
        if(null!= planRatingEnumId) {
            try {
                api.deleteEnumeration(planRatingEnumId);
            } catch(Exception ex) {
                logger.error("enum deletion failed", ex);
            }
        }
        if(null!= engineSConnectRouteRateCardId) {
            try {
                api.deleteRouteRateCard(engineSConnectRouteRateCardId);
            } catch(Exception ex) {
                logger.error("route rate card deletion failed", ex);
            }
        }
    }
}