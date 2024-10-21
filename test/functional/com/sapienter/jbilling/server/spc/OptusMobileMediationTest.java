package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
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
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

@Test(testName = "spc.OptusMobileMediationTest")
public class OptusMobileMediationTest extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String OPTUS_MOBILE_RATE_CARD_NAME              = "optus_rate_card";
    private static final String OPTUS_MOBILE_RATE_CARD_HEADER            = "id,name,surcharge,initial_increment,subsequent_increment,charge,"
            + "markup,capped_charge,capped_increment,minimum_charge,route_id,tariff_code";
    private static final String  OPTUS_MOBILE_CALL                       = "Optus Mobile Call";
    private static final String  OPTUS_MOBILE_SMS                        = "SurePage";
    private static final String  OPTUS_CONTENT_SMS                       = "Content - Adjustment SMS";
    private static final String  OPTUS_MOBILE_PLAN_ITEM                  = "testPlanSubscriptionItem" + System.currentTimeMillis();
    private static final int     MONTHLY_ORDER_PERIOD                    =  2;
    private static final String  OPTUS_MOBILE_PLAN_CODE                  =  "optus-mobile-Plan" + System.currentTimeMillis();
    private static final String  USER_01                                 =  "testUser01" + System.currentTimeMillis();
    private static final String  USER_001                                =  "testUser001" + System.currentTimeMillis();
    private static final String  USER_0001                                =  "testUser0001" + System.currentTimeMillis();
    private static final String  USER_02                                 =  "testUser02" + System.currentTimeMillis();
    private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID   =  320104;
    private static final String TEMP                                     = ("" + System.currentTimeMillis());
    private static final String UNIQUE_ID                                = TEMP.substring(TEMP.length()-5);
    private static final String  CONTENT_SMS_ASSET_NUMBER_01             = "01000" + UNIQUE_ID;
    private static final String  CONTENT_SMS_ASSET_NUMBER_001            = "02000" + UNIQUE_ID;
    private static final String  CONTENT_SMS_ASSET_NUMBER_0001           = "02100" + UNIQUE_ID;
    private static final String  SMS_ASSET_NUMBER_01                     = "04000" + UNIQUE_ID;
    private static final String  SMS_ASSET_NUMBER_001                    = "05000" + UNIQUE_ID;
    private static final String  SMS_ASSET_NUMBER_0001                   = "05100" + UNIQUE_ID;
    private static final String  SMS_ASSET_NUMBER_02                     = "06000" + UNIQUE_ID;
    private static final String ASSET01_NUMBER                           = "07000" + UNIQUE_ID;
    private static final String ASSET001_NUMBER                          = "08000" + UNIQUE_ID;
    private static final String ASSET0001_NUMBER                         = "08100" + UNIQUE_ID;
    private static final String ASSET02_NUMBER                           = "09000" + UNIQUE_ID;
    private static final String  SMS_ASSET_PRODUCT                       = "SMS Asset product";
    private static final String  CONTENT_SMS_ASSET_PRODUCT               = "Content Adjustment SMS Asset product";
    private static final String  OPTUS_MOBILE_FORMAT_10                  = "10%s                       000000000000000616695600005050200050502%s091351%s000100000001201811260411000321           "
            + "1110374900S1445741E1000000000000000000000062A            DEPOSIT        "
            + "Bourke St      1120181126091351000003000000000001000000524%s0904000000000"
            + "00000000000001011                  ";
    private static final String  OPTUS_MOBILE_FORMAT_010                  = "10%s                       000000000000000616695600005050200050502%s091351%s000100000001201811260411000321           "
            + "1110374900S1445741E1000000000000000000000062A            DEPOSIT        "
            + "Bourke St      1120181126091351000003000000000001000000524%s0904000000000"
            + "00000000000001011                  ";
    private static final String  OPTUS_MOBILE_FORMAT_0010                  = "10%s                       000000000000000616695600005050200050502%s091351%s000100000001201811260411000321           "
            + "1110374900S1445741E1000000000000000000000062A            DEPOSIT        "
            + "Bourke St      1120181126091351000003000000000001000000524%s0904000000000"
            + "00000000000001011                  ";
    private static final String OPTUS_MOBILE_FORMAT_30                   = "30%s                       000000000000000616695600005050200050502%s072429201811260724"
            + "290000000000000000012018112601101C0407442220           "
            + "105DDSMS message    P09040000000000000000000000000000000630000000006300000153000%sMSL  0                           ";
    private static final String OPTUS_MOBILE_FORMAT_030                   = "30%s                       000000000000000616695600005050200050502%s072429201811260724"
            + "290000000000000000012018112601101C0407442220           "
            + "105DDSMS message    P09040000000000000000000000000000000630000000006300000153000%sMSL  0                           ";

    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_001                   = "subscription001"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_0001                  = "subscription0001"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                    = "subscription02"+ System.currentTimeMillis();
    private static final String MEDIATION_FILE_PREFIX                    = "RESELL_";
    private Integer optusMobileRateCardId ;
    private Integer planRatingEnumId ;
    private UUID uuid;
    private JbillingAPI api;

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            api = envBuilder.getPrancingPonyApi();
            //Uploading Optus mobile Route Rate card
            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName(OPTUS_MOBILE_RATE_CARD_NAME);
            routeRateCardWS.setRatingUnitId(spcRatingUnitId);
            routeRateCardWS.setEntityId(api.getCallerCompanyId());
            List<String> rateRecords = Arrays.asList("1,Optus Mobile Call,0.39,60,60,10,0,0,0,0,10:S412L:1:1:04,OM:1900",
                    "2,Optus Mobile Call,0.39,60,60,10,0,0,0,0,10:S413L:1:1,OM:1900",
                    "3,SurePage,0.39,60,60,39,0,0,0,0,30:MMSMS:1:0:040,OM:SURE",
                    "4,Content - Adjustment SMS,0.39,60,60,42,0,0,0,0,40:DL:01,OM:CADJ01",
                    "5,Optus Mobile Call,0.39,60,60,10,0,0,0,0,10:S412L:1:1,OM:1900");

            String optusMobileRouteRateCardFilePath = createFileWithData(OPTUS_MOBILE_RATE_CARD_NAME, ".csv", OPTUS_MOBILE_RATE_CARD_HEADER, rateRecords);
            logger.debug("Optus Route Rate card file path {}", optusMobileRouteRateCardFilePath);
            optusMobileRateCardId = api.createRouteRateCard(routeRateCardWS, new File(optusMobileRouteRateCardFilePath));
            logger.debug("Optus Route Rate Card id {}", optusMobileRateCardId);
            Integer matchingFieldId = api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, optusMobileRateCardId));
            logger.debug("Matching Field id {}", matchingFieldId);

            // Creating Plan Rating Enumeration
            EnumerationValueWS valueWS = new EnumerationValueWS();
            valueWS.setValue(OPTUS_MOBILE_RATE_CARD_NAME);
            if(api.getEnumerationByName(ENUMERATION_METAFIELD_NAME) == null) {
                EnumerationWS enumeration = new EnumerationWS();
                enumeration.setEntityId(api.getCallerCompanyId());
                enumeration.setName(ENUMERATION_METAFIELD_NAME);
                enumeration.setValues(Arrays.asList(valueWS));
                planRatingEnumId = api.createUpdateEnumeration(enumeration);
            }

            // Creating plan level meta-field
            buildAndPersistMetafield(testBuilder, PLAN_LEVEL_METAFIELD, DataType.ENUMERATION, EntityType.PLAN);

            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);
            Integer assetEnabledCategory = assetEnabledProduct.getTypes()[0];

            //  Creating sms asset product
            buildAndPersistFlatProduct(envBuilder, api, SMS_ASSET_PRODUCT, false,
                    assetEnabledCategory, "1", true, 1, false);

            //  Creating content adjustment sms asset product
            buildAndPersistFlatProduct(envBuilder, api, CONTENT_SMS_ASSET_PRODUCT, false,
                    assetEnabledCategory, "1", true, 1, false);

            // Creating Usage Product Optus mobile call
            buildAndPersistFlatProduct(envBuilder, api, OPTUS_MOBILE_CALL, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false);

            // Creating Usage Product Optus SMS product
            buildAndPersistFlatProduct(envBuilder, api, OPTUS_MOBILE_SMS, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false);

            // Creating Usage Product Optus Context Adjustment SMS product
            buildAndPersistFlatProduct(envBuilder, api, OPTUS_CONTENT_SMS, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false);

            // Creating optus mobile PlanItem
            buildAndPersistFlatProduct(envBuilder, api, OPTUS_MOBILE_PLAN_ITEM, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);

            // building Rate Card pricing strategy
            PriceModelWS optusMobilePriceModel = buildRateCardPriceModel(optusMobileRateCardId, "DURATION");
            PriceModelWS optusSMSPriceModel = buildRateCardPriceModel(optusMobileRateCardId, "DURATION");
            PriceModelWS optusContentSMSPriceModel = buildRateCardPriceModel(optusMobileRateCardId, "DURATION");

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);


            PlanItemWS optusMobilePlanItem = buildPlanItem(envBuilder.idForCode(OPTUS_MOBILE_CALL),
                    MONTHLY_ORDER_PERIOD, "0.00", optusMobilePriceModel, pricingDate.getTime());

            PlanItemWS optusSMSPlanItem = buildPlanItem(envBuilder.idForCode(OPTUS_MOBILE_SMS),
                    MONTHLY_ORDER_PERIOD, "0.00", optusSMSPriceModel, pricingDate.getTime());

            PlanItemWS optusContentSMSPlanItem = buildPlanItem(envBuilder.idForCode(OPTUS_CONTENT_SMS),
                    MONTHLY_ORDER_PERIOD, "0.00", optusContentSMSPriceModel, pricingDate.getTime());

            //Create plan for optus mobile
            Integer planId =  buildAndPersistPlan(envBuilder,api, OPTUS_MOBILE_PLAN_CODE, "100 Optus Mobile Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(OPTUS_MOBILE_PLAN_ITEM), Collections.emptyList() , optusMobilePlanItem, optusSMSPlanItem, optusContentSMSPlanItem);

            setPlanLevelMetaField(planId, OPTUS_MOBILE_RATE_CARD_NAME);
            buildAndPersistMetafield(testBuilder, "Number of days to back dated events", DataType.STRING, EntityType.COMPANY);
            setCompanyLevelMetaField(testBuilder.getTestEnvironment(), "Number of days to back dated events", "10000");

            // Configure spc usage manager task.
            Map<String, String> params = new HashMap<>();
            params.put("VOIP_Usage_Field_Name", "SERVICE_NUMBER");
            params.put("Internate_Usage_Field_Name", "USER_NAME");
            updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                    SPCUsageManagerTask.class.getName(), params);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Operator Assisted Call Product Creation Failed", testEnvBuilder.idForCode(OPTUS_MOBILE_CALL));
            assertNotNull("SMS Asset Enabled Product Creation Failed", testEnvBuilder.idForCode(SMS_ASSET_PRODUCT));
            assertNotNull("Content Adjustment SMS Asset Enabled Product Creation Failed", testEnvBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT));
            assertNotNull("SMS UsageProduct Creation Failed", testEnvBuilder.idForCode(SMS_ASSET_PRODUCT));
            assertNotNull("Context Adjusment SMS UsageProduct Creation Failed", testEnvBuilder.idForCode(OPTUS_CONTENT_SMS));
            assertNotNull("Optus Mobile Plan Creation Failed", testEnvBuilder.idForCode(OPTUS_MOBILE_SMS));
        });
    }

    @Test(priority = 1)
    void test01OptusMobileMediationUpload() {
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

            Integer asset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID,
                    ASSET01_NUMBER, "asset-01"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset1, ASSET01_NUMBER);

            Integer smsAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], envBuilder.idForCode(SMS_ASSET_PRODUCT),
                    SMS_ASSET_NUMBER_01, "asset-01"+ System.currentTimeMillis());
            logger.debug("sms asset created {} for number {}", smsAsset1, SMS_ASSET_NUMBER_01);

            Integer contentAdjustmentSMSAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0],
                    envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), CONTENT_SMS_ASSET_NUMBER_01, "asset-01"+ System.currentTimeMillis());
            logger.debug("sms asset created {} for number {}", contentAdjustmentSMSAsset1, CONTENT_SMS_ASSET_NUMBER_01);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SMS_ASSET_PRODUCT), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(OPTUS_MOBILE_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(SMS_ASSET_PRODUCT), Arrays.asList(smsAsset1));
            productAssetMap.put(envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), Arrays.asList(contentAdjustmentSMSAsset1));
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
            //Arguments passed to CDR: Asset Number, Event Date, Duration and Product Plan Code
            String cdrLine = String.format(OPTUS_MOBILE_FORMAT_10, ASSET01_NUMBER, "20180202", "000059", "S412L");
            String smsLine = String.format(OPTUS_MOBILE_FORMAT_30, SMS_ASSET_NUMBER_01, "20180202", "MMSMS");
            //String contentAdjustMentsmsLine = String.format(OPTUS_MOBILE_FORMAT_40, CONTENT_SMS_ASSET_NUMBER_01, "20180202", "DL");
            logger.debug("cdr line {}", cdrLine);
            logger.debug("sms cdr line {}", smsLine);
            //logger.debug("Content Adjustment sms cdr line {}", contentAdjustMentsmsLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".dat", null,
                    Arrays.asList(cdrLine, smsLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", mediationProcessId);
            logger.debug("Mediation ProcessId {}", mediationProcessId);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_01));
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            validatePricingFields(viewEvents);
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Mediated Order line shoud be", 2, order.getOrderLines().length);
            assertEquals("Mediated Order Amount shoud be", new BigDecimal("49.78"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            OrderLineWS callLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_CALL));
            assertNotNull("Call Usage Item not found", callLine);
            assertEquals("Call Item Line Quantity ", new BigDecimal("1.00"),
                    callLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Call Item Line Amount ", new BigDecimal("10.39"),
                    callLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

            OrderLineWS smsLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_SMS));
            assertNotNull("SMS Usage Item not found", smsLine);
            assertEquals("SMS Item Line Quantity ", new BigDecimal("1.00"),
                    smsLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("SMS Item Line Amount ", new BigDecimal("39.39"),
                    smsLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    @Test(priority = 2)
    void test02OptusMobileRecycleMediation() {
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
            //Arguments passed to CDR: Asset Number, Event Date, Duration and Product Plan Code
            String cdrLine = String.format(OPTUS_MOBILE_FORMAT_10, ASSET02_NUMBER, "20180202", "000031", "S413L");
            logger.debug("cdr line {}", cdrLine);
            String smsLine = String.format(OPTUS_MOBILE_FORMAT_30, SMS_ASSET_NUMBER_02, "20180202", "MMSMS");
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".csv", null, Arrays.asList(cdrLine, smsLine));
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
            assertEquals("Mediation Error Record", Integer.valueOf(2), mediationProcess.getErrors());
        }).validate((testEnv, envBuilder) -> {
            // creating subscription order
            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID,
                    ASSET02_NUMBER, "asset-02"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset, ASSET02_NUMBER);
            Integer smsAsset2 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], envBuilder.idForCode(SMS_ASSET_PRODUCT),
                    SMS_ASSET_NUMBER_02, "asset-02"+ System.currentTimeMillis());
            logger.debug("sms asset created {} for number {}", smsAsset2, SMS_ASSET_NUMBER_02);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SMS_ASSET_PRODUCT), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(OPTUS_MOBILE_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));
            productAssetMap.put(envBuilder.idForCode(SMS_ASSET_PRODUCT), Arrays.asList(smsAsset2));
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
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Error Record", Integer.valueOf(0), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_02));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Mediated Order line shoud be", 2, order.getOrderLines().length);
            assertEquals("Mediated Order Amount shoud be", new BigDecimal("49.78"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            OrderLineWS callLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_CALL));
            assertNotNull("Call Usage Item not found", callLine);
            assertEquals("Call Item Line Quantity ", new BigDecimal("1.00"),
                    callLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Call Item Line Amount ", new BigDecimal("10.39"),
                    callLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

            OrderLineWS smsLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_SMS));
            assertNotNull("SMS Usage Item not found", smsLine);
            assertEquals("SMS Item Line Quantity ", new BigDecimal("1.00"),
                    smsLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("SMS Item Line Amount ", new BigDecimal("39.39"),
                    smsLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    /**
     * Test case for mediation validation rules
     */
    @Test(priority = 3)
    void test03mediationRulesTest() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user001 = envBuilder.customerBuilder(api)
                    .withUsername(USER_001)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();

            logger.debug("User created {}", user001.getId());

            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID,
                    ASSET001_NUMBER, "asset-001"+ System.currentTimeMillis());
            logger.debug("asset created {} for number {}", asset1, ASSET001_NUMBER);

            Integer smsAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], envBuilder.idForCode(SMS_ASSET_PRODUCT),
                    SMS_ASSET_NUMBER_001, "asset-001"+ System.currentTimeMillis());
            logger.debug("sms asset created {} for number {}", smsAsset1, SMS_ASSET_NUMBER_001);

            Integer contentAdjustmentSMSAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0],
                    envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), CONTENT_SMS_ASSET_NUMBER_001, "asset-001"+ System.currentTimeMillis());
            logger.debug("sms asset created {} for number {}", contentAdjustmentSMSAsset1, CONTENT_SMS_ASSET_NUMBER_001);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SMS_ASSET_PRODUCT), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(OPTUS_MOBILE_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(SMS_ASSET_PRODUCT), Arrays.asList(smsAsset1));
            productAssetMap.put(envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), Arrays.asList(contentAdjustmentSMSAsset1));
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset1));

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.add(Calendar.MONTH, -2);
            activeSinceDate.set(Calendar.DATE, 1);

            Calendar activeUntilDate = Calendar.getInstance();
            activeUntilDate.add(Calendar.MONTH, -2);
            activeUntilDate.set(Calendar.DATE, activeUntilDate.getActualMaximum(Calendar.DAY_OF_MONTH));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_001, activeSinceDate.getTime(), activeUntilDate.getTime(), MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_001);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_001));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 001 Creation Failed", testEnvBuilder.idForCode(USER_001));
            assertNotNull("subscription order 001 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_001));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            //Arguments passed to CDR: Asset Number, Event Date, Duration and Product Plan Code
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

            // event date within active since and active until dates
            Calendar eventDate = Calendar.getInstance();
            eventDate.add(Calendar.MONTH, -2);
            eventDate.set(Calendar.DATE, 10);
            String eventDateStr1 = simpleDateFormat.format(eventDate.getTime());
            String cdrLine1 = String.format(OPTUS_MOBILE_FORMAT_10, ASSET001_NUMBER, eventDateStr1, "000059", "S412L");

            //event date in past than active since date
            eventDate.add(Calendar.MONTH, -2);
            String eventDateStr2 = simpleDateFormat.format(eventDate.getTime());
            String cdrLine2 = String.format(OPTUS_MOBILE_FORMAT_010, ASSET001_NUMBER, eventDateStr2, "000059", "S412L");

            //event date in future of active until date
            eventDate = Calendar.getInstance();
            eventDate.add(Calendar.DATE, -1);
            String eventDateStr3 = simpleDateFormat.format(eventDate.getTime());
            String cdrLine3 = String.format(OPTUS_MOBILE_FORMAT_0010, ASSET001_NUMBER, eventDateStr3, "000059", "S412L");

            String smsLine = String.format(OPTUS_MOBILE_FORMAT_030, SMS_ASSET_NUMBER_001, eventDateStr1, "MMSMS");
            //String contentAdjustMentsmsLine = String.format(OPTUS_MOBILE_FORMAT_40, CONTENT_SMS_ASSET_NUMBER_01, "20180202", "DL");
            logger.debug("cdr lines {}, {}, {}", cdrLine1, cdrLine2, cdrLine3);
            logger.debug("sms cdr line {}", smsLine);
            //logger.debug("Content Adjustment sms cdr line {}", contentAdjustMentsmsLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".dat", null,
                    Arrays.asList(cdrLine1, cdrLine2, cdrLine3, smsLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", uuid);
            logger.debug("Mediation ProcessId {}", uuid);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            JbillingMediationErrorRecord[] errorRecords = api.getMediationErrorRecordsByMediationProcess(api.getMediationProcessStatus().getMediationProcessId(), null);
            if(ArrayUtils.isNotEmpty(errorRecords)) {
                for(JbillingMediationErrorRecord errorRecord : errorRecords) {
                    logger.debug("error codes {} for process id {}", errorRecord.getErrorCodes(), mediationProcess.getId());
                }
            }
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            assertEquals("Mediation Errors detected", Integer.valueOf(2), mediationProcess.getErrors());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_001));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Mediated Order line shoud be", 2, order.getOrderLines().length);
            assertEquals("Mediated Order Amount shoud be", new BigDecimal("49.78"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            OrderLineWS callLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_CALL));
            assertNotNull("Call Usage Item not found", callLine);
            assertEquals("Call Item Line Quantity ", new BigDecimal("1.00"),
                    callLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Call Item Line Amount ", new BigDecimal("10.39"),
                    callLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

            OrderLineWS smsLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_SMS));
            assertNotNull("SMS Usage Item not found", smsLine);
            assertEquals("SMS Item Line Quantity ", new BigDecimal("1.00"),
                    smsLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("SMS Item Line Amount ", new BigDecimal("39.39"),
                    smsLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

            JbillingMediationErrorRecord[] errors = api.getMediationErrorRecordsByMediationProcess(uuid, 0);
            Stream.of(errors).forEach(error -> {
                assertTrue("Mediation errors should be JB-USER-NOT-RESOLVED or ERR-EVENT-DATE-IS-AFTER",
                        error.getErrorCodes().contains("JB-USER-NOT-RESOLVED") || error.getErrorCodes().contains("ERR-EVENT-DATE-IS-AFTER"));
            });
        });
    }

    /**
     * Test case for mediation validation rules
     */
    @Test(priority = 4)
    void test04mediationRulesTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -4);
        calendar.set(Calendar.DATE, 1);
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user0001 = envBuilder.customerBuilder(api)
                    .withUsername(USER_0001)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();

            logger.debug("User created {}", user0001.getId());

            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET0001_NUMBER);
            logger.debug("asset created {} for number {}", asset1, ASSET0001_NUMBER, "asset-0001"+ System.currentTimeMillis());

            Integer smsAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], envBuilder.idForCode(SMS_ASSET_PRODUCT), SMS_ASSET_NUMBER_0001);
            logger.debug("sms asset created {} for number {}", smsAsset1, SMS_ASSET_NUMBER_0001, "asset-0001"+ System.currentTimeMillis());

            Integer contentAdjustmentSMSAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0],
                    envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), CONTENT_SMS_ASSET_NUMBER_0001, "asset-0001"+ System.currentTimeMillis());
            logger.debug("sms asset created {} for number {}", contentAdjustmentSMSAsset1, CONTENT_SMS_ASSET_NUMBER_0001);

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(SMS_ASSET_PRODUCT), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(OPTUS_MOBILE_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(envBuilder.idForCode(SMS_ASSET_PRODUCT), Arrays.asList(smsAsset1));
            productAssetMap.put(envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), Arrays.asList(contentAdjustmentSMSAsset1));
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset1));
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_0001, calendar.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_0001, Constants.ORDER_BILLING_PRE_PAID);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_0001));

            api.createInvoiceWithDate(user0001.getId(), calendar.getTime(), null, null, false);
            Calendar cal1 = (Calendar) calendar.clone();
            cal1.add(Calendar.MONTH, 1);
            api.createInvoiceWithDate(user0001.getId(), cal1.getTime(), null, null, false);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 0001 Creation Failed", testEnvBuilder.idForCode(USER_0001));
            assertNotNull("subscription order 0001 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_0001));
        }).validate((testEnv, testEnvBuilder) -> {
            logger.debug("Creating Mediation file ....");
            //Arguments passed to CDR: Asset Number, Event Date, Duration and Product Plan Code
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            Calendar eventDate = (Calendar) calendar.clone();
            eventDate.set(Calendar.DATE, 10);
            String eventDateStr1 = simpleDateFormat.format(eventDate.getTime());
            String cdrLine = String.format(OPTUS_MOBILE_FORMAT_10, ASSET0001_NUMBER, eventDateStr1, "000059", "S412L");
            String smsLine = String.format(OPTUS_MOBILE_FORMAT_30, SMS_ASSET_NUMBER_0001, eventDateStr1, "MMSMS");
            //String contentAdjustMentsmsLine = String.format(OPTUS_MOBILE_FORMAT_40, CONTENT_SMS_ASSET_NUMBER_01, "20180202", "DL");
            logger.debug("cdr line {}", cdrLine);
            logger.debug("sms cdr line {}", smsLine);
            //logger.debug("Content Adjustment sms cdr line {}", contentAdjustMentsmsLine);
            String cdrFilePath = createFileWithData(MEDIATION_FILE_PREFIX + System.currentTimeMillis(), ".dat", null,
                    Arrays.asList(cdrLine, smsLine));
            logger.debug("Mediation file created {}", cdrFilePath);
            uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), new File(cdrFilePath));
            assertNotNull("Mediation trigger failed", uuid);
            logger.debug("Mediation ProcessId {}", uuid);
            pauseUntilMediationCompletes(20, api);
        }).validate((testEnv, testEnvBuilder) -> {
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(USER_0001));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("Mediated Order line shoud be", 2, order.getOrderLines().length);
            assertEquals("Mediated Order Amount shoud be", new BigDecimal("49.78"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            OrderLineWS callLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_CALL));
            assertNotNull("Call Usage Item not found", callLine);
            assertEquals("Call Item Line Quantity ", new BigDecimal("1.00"),
                    callLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Call Item Line Amount ", new BigDecimal("10.39"),
                    callLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

            OrderLineWS smsLine = getLineByItemId(order, testEnvBuilder.idForCode(OPTUS_MOBILE_SMS));
            assertNotNull("SMS Usage Item not found", smsLine);
            assertEquals("SMS Item Line Quantity ", new BigDecimal("1.00"),
                    smsLine.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("SMS Item Line Amount ", new BigDecimal("39.39"),
                    smsLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            Calendar activeSinceDate = (Calendar) calendar.clone();
            activeSinceDate.set(Calendar.DATE, 1);
            Integer[] invoiceId = api.createInvoiceWithDate(testEnvBuilder.idForCode(USER_0001),activeSinceDate.getTime(),null,null,false);
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            assertTrue("Mediated order should be active since current months period", df.format(activeSinceDate.getTime()).equals(df.format(order.getActiveSince())));
        });
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
        if(null!= optusMobileRateCardId) {
            try {
                api.deleteRouteRateCard(optusMobileRateCardId);
            } catch(Exception ex) {
                logger.error("RouteRateCard deletion failed", ex);
            }
        }
    }
}