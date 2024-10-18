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

@Test(testName = "spc.OptusMobileMediationTest")
public class OptusMobileMediationTest extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String OPTUS_MOBILE_RATE_CARD_NAME              = "optus_rate_card";
    private static final String OPTUS_MOBILE_RATE_CARD_HEADER            = "id,name,surcharge,initial_increment,subsequent_increment,charge,"
            + "route_id,tariff_code";
    private static final String  OPTUS_MOBILE_CALL                       = "Optus Mobile Call";
    private static final String  OPTUS_MOBILE_SMS                        = "SurePage";
    private static final String  OPTUS_CONTENT_SMS                       = "Content - Adjustment SMS";
    private static final String  OPTUS_MOBILE_PLAN_ITEM                  = "testPlanSubscriptionItem" + System.currentTimeMillis();
    private static final int     MONTHLY_ORDER_PERIOD                    =  2;
    private static final String  OPTUS_MOBILE_PLAN_CODE                  =  "optus-mobile-Plan" + System.currentTimeMillis();
    private static final String  USER_01                                 =  "testUser01" + System.currentTimeMillis();
    private static final String  USER_02                                 =  "testUser02" + System.currentTimeMillis();
    private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID   =  320104;
    private static final String  CONTENT_SMS_ASSET_NUMBER_01             = "0200000001";
    private static final String  CONTENT_SMS_ASSET_NUMBER_02             = "0200000002";
    private static final String  SMS_ASSET_NUMBER_01                     = "0300000001";
    private static final String  SMS_ASSET_NUMBER_02                     = "0300000002";
    private static final String  SMS_ASSET_PRODUCT                       = "SMS Asset product";
    private static final String  CONTENT_SMS_ASSET_PRODUCT               = "Content Adjustment SMS Asset product";
    private static final String  OPTUS_MOBILE_FORMAT_10                  = "10%s                       000000000000000616695600005050200050502%s091351%s000100000001201811260411000321           "
            + "1110374900S1445741E1000000000000000000000062A            DEPOSIT        "
            + "Bourke St      1120181126091351000003000000000001000000524%s0904000000000"
            + "00000000000001011                  ";
    private static final String OPTUS_MOBILE_FORMAT_30                   = "30%s                       000000000000000616695600005050200050502%s072429201811260724"
            + "290000000000000000012018112601101C0407442220           "
            + "105DDSMS message    P09040000000000000000000000000000000630000000006300000153000%sMSL  0                           ";

    private static final String OPTUS_MOBILE_FORMAT_40                   = "40%s                       000000000000000616695600005050200050502%s15073901000003212727211559                           "
            + "mnetcorporationM.Net Corporation       19955901_MO_Mnt9811ShortCodeProduct        "
            + "%s0001U00000000500DR00000000050DRY000000                                       0            "
            + "                                                                       ";

    private static final String ASSET01_NUMBER                           = "0400000001";
    private static final String ASSET02_NUMBER                           = "0400000002";
    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                    = "subscription02"+ System.currentTimeMillis();
    private static final String MEDIATION_FILE_PREFIX                    = "RESELL_";
    private Integer optusMobileRateCardId ;
    private Integer planRatingEnumId ;

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            //Uploading Optus mobile Route Rate card
            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName(OPTUS_MOBILE_RATE_CARD_NAME);
            routeRateCardWS.setRatingUnitId(spcRatingUnitId);
            routeRateCardWS.setEntityId(api.getCallerCompanyId());
            List<String> rateRecords = Arrays.asList("1,Optus Mobile Call,0.39,60,60,10,10:1:DEPOSIT:S412L:1,OM:1900",
                    "2,Optus Mobile Call,0.39,60,60,10,10:1:DEPOSIT:S413L:1,OM:1900",
                    "3,SurePage,0.39,60,60,39,30:MMSMS:1:0:040,OM:SURE",
                    "4,Content - Adjustment SMS,0.39,60,60,42,40:DL:01,OM:CADJ01");

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

            Integer asset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET01_NUMBER);
            logger.debug("asset created {} for number {}", asset1, ASSET01_NUMBER);

            Integer smsAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], envBuilder.idForCode(SMS_ASSET_PRODUCT), SMS_ASSET_NUMBER_01);
            logger.debug("sms asset created {} for number {}", smsAsset1, SMS_ASSET_NUMBER_01);

            Integer contentAdjustmentSMSAsset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0],
                    envBuilder.idForCode(CONTENT_SMS_ASSET_PRODUCT), CONTENT_SMS_ASSET_NUMBER_01);
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

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET02_NUMBER);
            logger.debug("asset created {} for number {}", asset, ASSET02_NUMBER);
            Integer smsAsset2 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], envBuilder.idForCode(SMS_ASSET_PRODUCT), SMS_ASSET_NUMBER_02);
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

    @Override
    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
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