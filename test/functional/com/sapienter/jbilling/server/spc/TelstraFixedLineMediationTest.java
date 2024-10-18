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
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

@Test(testName = "spc.TelstraFixedLineMediationTest")
public class TelstraFixedLineMediationTest extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String TELSTRA_RATE_CARD_NAME                   = "telstra_rate_card";
    private static final String TELSTRA_RATE_CARD_HEADER                 = "id,name,surcharge,initial_increment,subsequent_increment,charge,"
            + "route_id,tariff_code";
    private static final String  OPERATOR_ASSISTED_CALL                  = "Operator Assisted Call";
    private static final String  TELSTRA_PLAN_ITEM                       = "testPlanSubscriptionItem" + System.currentTimeMillis();
    private static final int     MONTHLY_ORDER_PERIOD                    =  2;
    private static final String  TELSTRA_PLAN_CODE                       =  "telstra-Plan" + System.currentTimeMillis();
    private static final String  USER_01                                 =  "testUser01-TelstraFixedLine" + System.currentTimeMillis();
    private static final String  USER_02                                 =  "testUser02-TelstraFixedLine" + System.currentTimeMillis();
    private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID   =  320104;
    private static final String  TELSTRA_CDR_FORMAT                      = "UIRSOU00141477%s016390777%s   38660783000161353850A2%s 0      0000     A20353542589 0      "
            + "0000 A20353393482             20190204  07:05:33Wendouree   SEC  0000003500000000000000US A000000000122500  NM00000";
    private static final String ASSET01_NUMBER                           = "0353542584";
    private static final String ASSET02_NUMBER                           = "0353542585";
    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                    = "subscription02"+ System.currentTimeMillis();
    private static final String MEDIATION_FILE_PREFIX                    = "EBILL";
    private Integer telstraRouteRateCardId ;
    private Integer planRatingEnumId ;

    @Override
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            //Uploading Telstra Route Rate card
            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName(TELSTRA_RATE_CARD_NAME);
            routeRateCardWS.setRatingUnitId(spcRatingUnitId);
            routeRateCardWS.setEntityId(api.getCallerCompanyId());
            List<String> rateRecords = Arrays.asList("1,Operator Assisted Call,0.39,60,60,6,808410W0USAGE,TF:#DIR",
                    "2,Operator Assisted Call,0.39,60,60,6,808411W0USAGE,TF:#DIR");

            String telstraRouteRateCardFilePath = createFileWithData(TELSTRA_RATE_CARD_NAME, ".csv", TELSTRA_RATE_CARD_HEADER, rateRecords);
            logger.debug("Telstra Route Rate card file path {}", telstraRouteRateCardFilePath);
            telstraRouteRateCardId = api.createRouteRateCard(routeRateCardWS, new File(telstraRouteRateCardFilePath));
            logger.debug("Telstra Route Rate Card id {}", telstraRouteRateCardId);
            Integer matchingFieldId = api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, telstraRouteRateCardId));
            logger.debug("Matching Field id {}", matchingFieldId);

            // Creating Plan Rating Enumeration
            EnumerationValueWS valueWS = new EnumerationValueWS();
            valueWS.setValue(TELSTRA_RATE_CARD_NAME);
            if(api.getEnumerationByName(ENUMERATION_METAFIELD_NAME) == null) {
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
            buildAndPersistFlatProduct(envBuilder, api, TELSTRA_PLAN_ITEM, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);

            // building Rate Card pricing strategy
            PriceModelWS telstraOperatorAssistedCallPriceModel = buildRateCardPriceModel(telstraRouteRateCardId, "Quantity");

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);


            PlanItemWS telstraOperatorAssistedCallPlanItem = buildPlanItem(envBuilder.idForCode(OPERATOR_ASSISTED_CALL),
                    MONTHLY_ORDER_PERIOD, "0.00", telstraOperatorAssistedCallPriceModel, pricingDate.getTime());

            //Create plan for telstra
            Integer planId =  buildAndPersistPlan(envBuilder,api, TELSTRA_PLAN_CODE, "100 Telstra Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(TELSTRA_PLAN_ITEM), Collections.emptyList() , telstraOperatorAssistedCallPlanItem);

            setPlanLevelMetaField(planId, TELSTRA_RATE_CARD_NAME);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Operator Assisted Call Product Creation Failed", testEnvBuilder.idForCode(OPERATOR_ASSISTED_CALL));
            assertNotNull("Telstra Plan Creation Failed", testEnvBuilder.idForCode(TELSTRA_PLAN_CODE));
        });
    }


    @Test(priority = 1)
    void test01TelstraMediationUpload() {
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
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(TELSTRA_PLAN_ITEM), BigDecimal.ONE));

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
            String cdrLine = String.format(TELSTRA_CDR_FORMAT, "0000001", "808411W0USAGE", ASSET01_NUMBER);
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
            assertEquals("Invalid original quantity", new BigDecimal("35.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("1.00"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("6.39"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        });
    }

    @Test(priority = 2)
    void test02TelstraRecycleMediation() {
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
            String cdrLine = String.format(TELSTRA_CDR_FORMAT, "0000002", "808411W0USAGE", ASSET02_NUMBER);
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

            Integer asset = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET02_NUMBER);
            logger.debug("asset created {} for number {}", asset, ASSET02_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(TELSTRA_PLAN_ITEM), BigDecimal.ONE));

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
            assertEquals("Invalid original quantity", new BigDecimal("35.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("1.00"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order amount", new BigDecimal("6.39"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
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
        if(null!= telstraRouteRateCardId) {
            try {
                api.deleteRouteRateCard(telstraRouteRateCardId);
            } catch(Exception ex) {
                logger.error("deleteRouteRateCard failed", ex);
            }
        }
    }
}
