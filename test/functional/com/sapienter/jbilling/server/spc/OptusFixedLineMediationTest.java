package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

@Test(testName = "spc.OptusFixedLineMediationTest")
public class OptusFixedLineMediationTest extends BaseMediationTest {

    private static final String MEDIATION_TRIGGERED_SHOULD_RETURN_UUID = "Mediation triggered should return uuid";

    private static final String  BASE_DIR                                      = "base_dir";
    private static final String  CDR_BASE_DIRECTORY = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/cdr";
    private static final String  ROUTE_RATE_CARD_FILE = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/rrc/";
    private static final String  ROUTE_RATE_CARD_FILE_NAME                     = "optus_fixed_plan_rating_1";
    private static final String  ROUTE_RATE_CARD_FILE_EXTN                     = "csv";
    private static final String  CDR_FILE_NAME_STN1                            = "tap_stn1_22202705000137_20181127_008499_a_s.dat";
    private static final String  CDR_FILE_NAME_STN01                           = "tap_stn1_22202705000137_20181127_018499_a_s.dat";
    private static final String  CDR_FILE_NAME_STN001                          = "tap_stn1_22202705000137_20181127_028499_a_s.dat";
    private static final String  CDR_FILE_NAME_STN2                            = "tap_stn2_22205779000148_20181127_005936_a_s.dat";
    private static final String  CDR_FILE_NAME_STN1_MULTILINE                  = "tap_stn1_22202705000137_20190101_008499_a_s.dat";
    private static final String  CDR_FILE_NAME_STN4                            = "tap_stn2_22205779000148_20181126_005936_a_s.dat";

    private static final String  LOCAL_CALLS_ITEM                              = "Local Calls";
    private static final String  LONG_DISTANCE_CALLS_ITEM                      = "Long Distance Calls";
    private static final String  INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED       = "Inbound Mobile to 1300";
    private static final String  TEST_USER_1                                   = "Test-User1-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_01                                  = "Test-User01-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_001                                  = "Test-User001-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                   = "Test-User2-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_3                                   = "Test-User3-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_4                                   = "Test-User4-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_5                                   = "Test-User5-"+ UUID.randomUUID().toString();
    private static final int     MONTHLY_ORDER_PERIOD                          =  2;
    private              UUID    uuid                                          = null;
    private static final int     NEXT_INVOICE_DAY                              =  1;
    private static final String  USER_INVOICE_ASSERT                           = "Creating User with next invoice date {}";
    private static final String  TEST_CAT1                                     = "testCat1";
    private static PriceModelWS  callRateCardPrice;

    private static final String  CUSTOMER_CREATION_FAILED                      = "Customer Creation Failed";
    private static final String  PRODUCT_CREATION_FAILED                       = "Product Creation Failed";
    private static final String  ZERO                                          = "0";
    private static final String  ASSET                                         = "ASSET";

    private String subScriptionProd01 = "testPlanSubscriptionItem"+ System.currentTimeMillis();
    private String serviceNumber1 = "0353836200";
    private String serviceNumber01 = "0353836201";
    private String serviceNumber001 = "0353836202";
    private String serviceNumber2 = "1300887326";
    private String serviceNumber3 = "0740553628";
    private String serviceNumberNotPresent = "1300887311";
    private String serviceNumber5 = "1300887327";
    private Integer enumerationid;
    private Integer planId;
    private Integer routeRateCardId = null;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            //Upload RRC
            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName(ROUTE_RATE_CARD_FILE_NAME);
            routeRateCardWS.setRatingUnitId(spcRatingUnitId);
            routeRateCardWS.setEntityId(api.getCallerCompanyId());

            routeRateCardId = api.createRouteRateCard(routeRateCardWS, new File(ROUTE_RATE_CARD_FILE+ROUTE_RATE_CARD_FILE_NAME+"."+ROUTE_RATE_CARD_FILE_EXTN));
            RouteRateCardWS cardWS = api.getRouteRateCard(routeRateCardId);
            api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, routeRateCardId));

            List<EnumerationValueWS> valueWSs = new ArrayList();
            EnumerationValueWS valueWS = new EnumerationValueWS();
            valueWS.setValue(cardWS.getName());
            valueWSs.add(valueWS);

            EnumerationWS enumeration = new EnumerationWS();
            enumeration.setEntityId(api.getCallerCompanyId());
            enumeration.setName(ENUMERATION_METAFIELD_NAME);
            enumeration.setValues(valueWSs);
            enumerationid = api.createUpdateEnumeration(enumeration);

            // Creating plan level meta-field
            buildAndPersistMetafield(testBuilder, PLAN_LEVEL_METAFIELD, DataType.ENUMERATION, EntityType.PLAN);

            //Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, TEST_CAT1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            Integer planItem = buildAndPersistFlatProduct(envBuilder, api, subScriptionProd01, false, envBuilder.idForCode(TEST_CAT1), "100.00", true,0,true);
            buildAndPersistFlatProduct(envBuilder, api, ASSET, false, envBuilder.idForCode(TEST_CAT1), "20.00", true,1,false);
            buildAndPersistFlatProduct(envBuilder, api, LOCAL_CALLS_ITEM, false, envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "20", true,0,false);
            buildAndPersistFlatProduct(envBuilder, api, INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED, false, envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "30", true,0,false);
            buildAndPersistFlatProduct(envBuilder, api, LONG_DISTANCE_CALLS_ITEM, false, envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "20", true,0,false);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            callRateCardPrice = buildRateCardPriceModel(routeRateCardId);
            PlanItemWS planItemProd01WS1 = buildPlanItem(envBuilder.idForCode(ASSET), MONTHLY_ORDER_PERIOD, ZERO, callRateCardPrice, pricingDate.getTime());
            PlanItemWS planItemProd01WS2 = buildPlanItem(envBuilder.idForCode(LOCAL_CALLS_ITEM), MONTHLY_ORDER_PERIOD, ZERO, callRateCardPrice, pricingDate.getTime());
            PlanItemWS planItemProd01WS3 = buildPlanItem(envBuilder.idForCode(INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED), MONTHLY_ORDER_PERIOD, ZERO, callRateCardPrice, pricingDate.getTime());
            PlanItemWS planItemProd01WS4 = buildPlanItem(envBuilder.idForCode(LONG_DISTANCE_CALLS_ITEM), MONTHLY_ORDER_PERIOD, ZERO, callRateCardPrice, pricingDate.getTime());

            //Create Plan
            PlanWS planWS2 = new PlanWS();
            planWS2.setItemId(planItem);
            planWS2.setDescription("SPC TEST PLAN");
            planWS2.setPeriodId(2);//monthly
            planWS2.addPlanItem(planItemProd01WS1);
            planWS2.addPlanItem(planItemProd01WS2);
            planWS2.addPlanItem(planItemProd01WS3);
            planWS2.addPlanItem(planItemProd01WS4);
            planWS2.getPlanItems().get(0).addModel(pricingDate.getTime(), callRateCardPrice );
            planId = api.createPlan(planWS2);
            logger.debug("Plan created Successfully : {}", planId);
            setPlanLevelMetaField(planId, ROUTE_RATE_CARD_FILE_NAME);

            // configure spc usage manager task.
            Map<String, String> params = new HashMap<>();
            params.put("VOIP_Usage_Field_Name", "SERVICE_NUMBER");
            params.put("Internate_Usage_Field_Name", "USER_NAME");
            updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                    SPCUsageManagerTask.class.getName(), params);
        }).test((testEnv, testEnvBuilder) -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            assertNotNull(PRODUCT_CREATION_FAILED, testEnvBuilder.idForCode(LOCAL_CALLS_ITEM));
            assertNotNull(PRODUCT_CREATION_FAILED, testEnvBuilder.idForCode(INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED));
            assertNotNull(PRODUCT_CREATION_FAILED, testEnvBuilder.idForCode(LONG_DISTANCE_CALLS_ITEM));
        });
    }

    @Override
    @AfterClass
    public void tearDown() {
        // configure again BasicItemManager task.
        updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID,
                BasicItemManager.class.getName(), Collections.emptyMap());
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        try {
            api.deletePlan(planId);
        } catch(Exception ex) {
            logger.error("plan deletion failed", ex);
        }
        if(null != enumerationid) {
            try {
                api.deleteEnumeration(enumerationid);
            } catch(Exception ex) {
                logger.error("enum deletion failed", ex);
            }
        }
        try {
            api.deleteRouteRateCard(routeRateCardId);
        } catch(Exception ex) {
            logger.error("RouteRateCard deletion failed", ex);
        }
        testBuilder = null;
    }

    /**
     * STN1 Valid CDR
     */
    @Test(priority = 1)
    public void test01MediationUpload() {
        List<Integer> assets = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();

        try {
            testBuilder
            .given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user1 = envBuilder.customerBuilder(api).withUsername(TEST_USER_1)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false).withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 1 Creation Failed", user1);
                logger.info("created user 1 {}", user1.getId());
                users.add(user1.getId());
                Integer asset1 = buildAndPersistAsset(envBuilder,
                        testBuilder.getTestEnvironment().idForCode(SPC_MEDIATED_USAGE_CATEGORY), testBuilder
                        .getTestEnvironment().idForCode(ASSET), serviceNumber1, "asset-01"+ System.currentTimeMillis());
                assets.add(asset1);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

                productQuantityMap.putAll(buildProductQuantityEntry(
                        testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01),
                        BigDecimal.ONE));
                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);
                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);

                Integer orderid = createOrder("Test-Order1", activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false, productQuantityMap,
                        productAssetMap, TEST_USER_1);
                orders.add(orderid);

            })
            .validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_1));
            })
            .validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_STN1);
                uuid = api.triggerMediationByConfigurationByFile(
                        getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {

                MediationProcess mediationProcess =
                        api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess); //
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable",Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_1));
                assertNotNull("Mediation Should Create Order", order);
                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                validatePricingFields(viewEvents);
                assertEquals("Invalid original quantity", new BigDecimal("27.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid resolved quantity", new BigDecimal("1.00"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order Amount", new BigDecimal("0.64"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            });
        }

        finally {
            for (Integer userId : users) {
                api.deleteUser(userId);
            }
            for (Integer order : orders) {
                api.deleteOrder(order);
            }
            for (Integer asset : assets) {
                api.deleteAsset(asset);
            }
        }
    }



    @Test(priority = 2)
    public void test02MediationProcessOfStn2FileWithValidCDR() {
        List<Integer> assets = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();

        try{
            testBuilder.given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user2 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_2)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 2 Creation Failed", user2);
                logger.info("created user 2 {}", user2.getId());
                users.add(user2.getId());
                Integer asset2 = buildAndPersistAsset(envBuilder, testBuilder.getTestEnvironment().idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                        testBuilder.getTestEnvironment().idForCode(ASSET), serviceNumber2, "asset-02"+ System.currentTimeMillis());
                assets.add(asset2);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);
                productQuantityMap.putAll(buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);

                Integer orderId = createOrder("Test-Order2", activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap,TEST_USER_2);
                orders.add(orderId);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_2));
            }).validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_STN2);
                uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_2));
                assertNotNull("Mediation Should Create Order", order);
                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                validatePricingFields(viewEvents);
                assertEquals("Invalid original quantity", new BigDecimal("65.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid resolved quantity", new BigDecimal("2.00"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order amount", new BigDecimal("2.39"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            });
        }

        finally {
            for (Integer userId : users) {
                api.deleteUser(userId);
            }
            for (Integer order : orders) {
                api.deleteOrder(order);
            }
            for(Integer asset : assets){
                api.deleteAsset(asset);
            }
        }
    }

    @Test(priority = 3)
    public void test03MediationProcessOfStn1FileWithValidCDRMulipleLinesOfRecord() {
        List<Integer> assets = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();

        try{
            testBuilder.given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user3 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_3)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 3 Creation Failed", user3);
                logger.info("created user 3 {}", user3.getId());
                users.add(user3.getId());
                Integer asset3 = buildAndPersistAsset(envBuilder, testBuilder.getTestEnvironment().idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                        testBuilder.getTestEnvironment().idForCode(ASSET), serviceNumber3, "asset-03"+ System.currentTimeMillis());
                assets.add(asset3);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);
                productQuantityMap.putAll(buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);

                Integer orderId = createOrder("Test-Order3", activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap,TEST_USER_3);
                orders.add(orderId);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_3));
            }).validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_STN1_MULTILINE);
                uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(uuid);
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_3));
                assertNotNull("Mediation Should Create Order", order);
                assertEquals("Invalid order lines", 2, order.getOrderLines().length);
                assertEquals("Invalid order amount", new BigDecimal("3.28"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            });
        }

        finally {
            for (Integer userId : users) {
                api.deleteUser(userId);
            }
            for (Integer order : orders) {
                api.deleteOrder(order);
            }
            for(Integer asset : assets){
                api.deleteAsset(asset);
            }
        }
    }

    @Test(priority = 4)
    public void test04MediationProcessOfStn1FileWithAssetNotPresentInCDR() {
        List<Integer> assets = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();

        try{
            testBuilder.given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user3 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_4)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 4 Creation Failed", user3);
                logger.info("created user 4 {}", user3.getId());
                users.add(user3.getId());
                Integer asset3 = buildAndPersistAsset(envBuilder, testBuilder.getTestEnvironment().idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                        testBuilder.getTestEnvironment().idForCode(ASSET), serviceNumberNotPresent, "asset-04"+ System.currentTimeMillis());
                assets.add(asset3);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);
                productQuantityMap.putAll(buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);

                Integer orderId = createOrder("Test-Order4", activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap,TEST_USER_4);
                orders.add(orderId);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_4));
            }).validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_STN2);
                uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(uuid);
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_4));
                assertNotNull("Mediation Should Create Order", order);
            });
        }

        finally {
            for (Integer userId : users) {
                api.deleteUser(userId);
            }
            for (Integer order : orders) {
                api.deleteOrder(order);
            }
            for(Integer asset : assets){
                api.deleteAsset(asset);
            }
        }
    }

    @Test(priority = 5)
    public void test05MediationProcessOfStn1FileWithInValidCDRDuplicateCDRId() {
        List<Integer> assets = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();

        try{
            testBuilder.given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user5 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_5)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 5 Creation Failed", user5);
                logger.info("created user 5 {}", user5.getId());
                users.add(user5.getId());
                Integer asset5 = buildAndPersistAsset(envBuilder, testBuilder.getTestEnvironment().idForCode(SPC_MEDIATED_USAGE_CATEGORY),
                        testBuilder.getTestEnvironment().idForCode(ASSET), serviceNumber5, "asset-05"+ System.currentTimeMillis());
                assets.add(asset5);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);
                productQuantityMap.putAll(buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);

                Integer orderId = createOrder("Test-Order5", activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap,TEST_USER_5);
                orders.add(orderId);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_5));
            }).validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_STN4);
                uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(uuid);
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Duplicate CDR Id ", Integer.valueOf(1), mediationProcess.getDuplicates());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_5));
                assertNotNull("Mediation Should Create Order", order);
                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                assertEquals("Invalid original quantity", new BigDecimal("65.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid resolved quantity", new BigDecimal("2.00"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order amount", new BigDecimal("2.39"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            });
        }

        finally {
            for (Integer userId : users) {
                api.deleteUser(userId);
            }
            for (Integer order : orders) {
                api.deleteOrder(order);
            }
            for(Integer asset : assets){
                api.deleteAsset(asset);
            }
        }
    }

    @Test(priority = 6, enabled = true)
    public void test001mediationRulesTest() {
        List<Integer> assets = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();

        try {
            testBuilder
            .given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user1 = envBuilder.customerBuilder(api).withUsername(TEST_USER_01)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false).withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();

                assertNotNull("Test User 01 Creation Failed", user1);
                logger.info("created user 01 {}", user1.getId());
                users.add(user1.getId());
                Integer asset1 = buildAndPersistAsset(envBuilder,
                        testBuilder.getTestEnvironment().idForCode(SPC_MEDIATED_USAGE_CATEGORY), testBuilder
                        .getTestEnvironment().idForCode(ASSET), serviceNumber01, "asset-001"+ System.currentTimeMillis());
                assets.add(asset1);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

                productQuantityMap.putAll(buildProductQuantityEntry(
                        testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01),
                        BigDecimal.ONE));

                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.add(Calendar.MONTH, -2);
                activeSinceDate.set(Calendar.DATE, 1);

                Calendar activeUntilDate = Calendar.getInstance();
                activeUntilDate.add(Calendar.MONTH, -2);
                activeUntilDate.set(Calendar.DATE, activeUntilDate.getActualMaximum(Calendar.DAY_OF_MONTH));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);

                Integer orderid = createOrder("Test-Order01", activeSinceDate.getTime(), activeUntilDate.getTime(), MONTHLY_ORDER_PERIOD, false, productQuantityMap,
                        productAssetMap, TEST_USER_01);
                orders.add(orderid);

            })
            .validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_01));
            })
            .validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                String fileName = CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_STN01;
                File cdrFile = new File(fileName);

                // event date within active since and active until dates
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
                Calendar eventDate = Calendar.getInstance();
                eventDate.add(Calendar.MONTH, -2);
                eventDate.set(Calendar.DATE, 10);
                String eventDateStr1 = simpleDateFormat.format(eventDate.getTime());
                updateEventDate(fileName, "eventDateOne", eventDateStr1);

                //event date in past than active since date
                eventDate.add(Calendar.MONTH, -2);
                String eventDateStr2 = simpleDateFormat.format(eventDate.getTime());
                updateEventDate(fileName, "eventDateTwo", eventDateStr2);

                //event date in future of active until date
                eventDate = Calendar.getInstance();
                eventDate.add(Calendar.DATE, -1);
                String eventDateStr3 = simpleDateFormat.format(eventDate.getTime());
                updateEventDate(fileName, "eventDateThree", eventDateStr3);

                uuid = api.triggerMediationByConfigurationByFile(
                        getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {

                MediationProcess mediationProcess =
                        api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess); //
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable",Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_01));
                assertNotNull("Mediation Should Create Order", order);
                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                assertEquals("Invalid original quantity", new BigDecimal("27.00"),
                        viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid resolved quantity", new BigDecimal("1.00"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order Amount", new BigDecimal("0.64"),
                        order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

                JbillingMediationErrorRecord[] errors = api.getMediationErrorRecordsByMediationProcess(uuid, 0);
                Stream.of(errors).forEach(error -> {
                    assertTrue("Mediation errors should be JB-USER-NOT-RESOLVED or ERR-EVENT-DATE-IS-AFTER",
                            error.getErrorCodes().contains("JB-USER-NOT-RESOLVED") || error.getErrorCodes().contains("ERR-EVENT-DATE-IS-AFTER"));
                });
            });
        }

        finally {
            for (Integer userId : users) {
                api.deleteUser(userId);
            }
            for (Integer order : orders) {
                api.deleteOrder(order);
            }
            for (Integer asset : assets) {
                api.deleteAsset(asset);
            }
        }
    }

    @Test(priority = 7, enabled = true)
    public void test002mediationRulesTest() {
        List<Integer> assets = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -4);
        calendar.set(Calendar.DATE, 1);

        testBuilder
        .given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user1 = envBuilder.customerBuilder(api).withUsername(TEST_USER_001)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false).withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .build();

            assertNotNull("Test User 001 Creation Failed", user1);
            logger.info("created user 001 {}", user1.getId());
            users.add(user1.getId());
            Integer asset1 = buildAndPersistAsset(envBuilder,
                    testBuilder.getTestEnvironment().idForCode(SPC_MEDIATED_USAGE_CATEGORY), testBuilder
                    .getTestEnvironment().idForCode(ASSET), serviceNumber001, "asset-0001"+ System.currentTimeMillis());
            assets.add(asset1);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.putAll(buildProductQuantityEntry(
                    testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01),
                    BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);

            Integer orderid = createOrder("Test-Order001", calendar.getTime(), null,
                    MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap, TEST_USER_001,
                    Constants.ORDER_BILLING_PRE_PAID);
            orders.add(orderid);
            api.createInvoiceWithDate(user1.getId(), calendar.getTime(), null, null, false);
            Calendar cal1 = (Calendar) calendar.clone();
            cal1.add(Calendar.MONTH, 1);
            api.createInvoiceWithDate(user1.getId(), cal1.getTime(), null, null, false);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_001));
            createMediationEvaluationStrategyPlugin();
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            String fileName = CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_STN001;
            File cdrFile = new File(fileName);

            // event date within active since and active until dates
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            Calendar eventDate = (Calendar) calendar.clone();
            eventDate.set(Calendar.DATE, 10);
            String eventDateStr1 = simpleDateFormat.format(eventDate.getTime());
            updateEventDate(fileName, "eventDateOne", eventDateStr1);

            uuid = api.triggerMediationByConfigurationByFile(
                    getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
            pauseUntilMediationCompletes(30,api);
            logger.debug("Mediation ProcessId {}", uuid);
            assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
        }).validate((testEnv, testEnvBuilder) -> {

            MediationProcess mediationProcess =
                    api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess); //
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable",Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_001));
            assertNotNull("Mediation Should Create Order", order);
            JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
            assertEquals("Invalid original quantity", new BigDecimal("27.00"),
                    viewEvents[0].getOriginalQuantity().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid resolved quantity", new BigDecimal("1.00"),
                    order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals("Invalid order Amount", new BigDecimal("0.64"),
                    order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
            Calendar cal = (Calendar) calendar.clone();
            cal.add(Calendar.MONTH, 1);
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            assertTrue("Mediated order should be active since current months period", df.format(cal.getTime()).equals(df.format(order.getActiveSince())));
        }).validate((testEnv, testEnvBuilder) -> {
            api.deletePlugin(mediationEvaluationStrategyPluginId);
        });
    }

    private void updateEventDate(String fileName, String wordToReplace, String value) {
        try {
            List<String> newLines = new ArrayList<>();
            for (String line : Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)) {
                if (line.contains("eventDate")) {
                    newLines.add(line.replace(wordToReplace, value));
                } else {
                    newLines.add(line);
                }
            }
            Files.write(Paths.get(fileName), newLines, StandardCharsets.UTF_8);
        } catch(Exception e) {
            logger.error(e.getMessage());
        }
    }
}