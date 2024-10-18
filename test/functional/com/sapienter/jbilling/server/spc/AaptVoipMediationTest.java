package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.math.BigDecimal;
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
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
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

@Test(testName = "spc.AaptVoipMediationTest")
public class AaptVoipMediationTest extends BaseMediationTest {

    private static final String MEDIATION_TRIGGERED_SHOULD_RETURN_UUID = "Mediation triggered should return uuid";

    private static final String  BASE_DIR                                      = "base_dir";
    private static final String  CDR_BASE_DIRECTORY = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/cdr";
    private static final String  ROUTE_RATE_CARD_FILE = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/rrc/";
    private static final String  ROUTE_RATE_CARD_FILE_NAME                     = "aaptVoipCtop_fixed_plan_rating";
    private static final String  ROUTE_RATE_CARD_FILE_EXTN                     = "csv";
    private static final String  CDR_FILE_NAME_CTOP_VALID                      = "CTOP_2000024909_0000003967_20181126203813.txt";
    private static final String  CDR_FILE_NAME_CTOP_VALID_2                      = "CTOP_2000024909_0000003967_20181127203813.txt";
    private static final String  CDR_FILE_NAME_CTOP_VALID_3                      = "CTOP_2000024909_0000003967_20181128203813.txt";
    private static final String  INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED       = "13/1300 Call";
    private static final String  TEST_USER_1                                   = "Test-User1-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                   = "Test-User2-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_3                                   = "Test-User3-"+ UUID.randomUUID().toString();
    private static final int     MONTHLY_ORDER_PERIOD                          =  2;
    private static final int     NEXT_INVOICE_DAY                              =  1;
    private static final String  USER_INVOICE_ASSERT                           = "Creating User with next invoice date {}";
    private static final String  TEST_CAT1                                     = "testCat1";

    private static final String  CUSTOMER_CREATION_FAILED                      = "Customer Creation Failed";
    private static final String  PRODUCT_CREATION_FAILED                       = "Product Creation Failed";
    private static final String  ZERO                                          = "0";
    private static final String  ASSET                                         = "ASSET";
    private static final String  AAPT_VOIP_PLAN_CODE                           =  "aaptVoip-Plan" + System.currentTimeMillis();
    private String subScriptionProd01 = "testPlanSubscriptionItem"+ System.currentTimeMillis();
    private String serviceNumber1 = "123456789";
    private String serviceNumber2 = "123456799";
    private String serviceNumber3 = "123456798";

    private Integer aaptVoipRouteRateCardId;
    private Integer planRatingEnumId;
    private Integer planId;
    private PriceModelWS  callRateCardPrice;
    private UUID uuid;

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

            aaptVoipRouteRateCardId = api.createRouteRateCard(routeRateCardWS, new File(ROUTE_RATE_CARD_FILE+ROUTE_RATE_CARD_FILE_NAME+"."+ROUTE_RATE_CARD_FILE_EXTN));
            RouteRateCardWS cardWS = api.getRouteRateCard(aaptVoipRouteRateCardId);
            api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, aaptVoipRouteRateCardId));

            List<EnumerationValueWS> valueWSs = new ArrayList<>();
            EnumerationValueWS valueWS = new EnumerationValueWS();
            valueWS.setValue(cardWS.getName());
            valueWSs.add(valueWS);

            EnumerationWS enumeration = new EnumerationWS();
            enumeration.setEntityId(api.getCallerCompanyId());
            enumeration.setName(ENUMERATION_METAFIELD_NAME);
            enumeration.setValues(valueWSs);
            planRatingEnumId = api.createUpdateEnumeration(enumeration);

            // Creating plan level meta-field
            buildAndPersistMetafield(testBuilder, PLAN_LEVEL_METAFIELD, DataType.ENUMERATION, EntityType.PLAN);
            //Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, TEST_CAT1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            buildAndPersistFlatProduct(envBuilder, api, subScriptionProd01, false, envBuilder.idForCode(TEST_CAT1), "100.00", true,0,true);
            buildAndPersistFlatProduct(envBuilder, api, ASSET, false, envBuilder.idForCode(TEST_CAT1), "20.00", true,1,false);
            buildAndPersistFlatProduct(envBuilder, api, INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED, false, envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "30", true,0,false);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            callRateCardPrice = buildRateCardPriceModel(aaptVoipRouteRateCardId);
            PlanItemWS planItemProd01WS1 = buildPlanItem(envBuilder.idForCode(ASSET), MONTHLY_ORDER_PERIOD, ZERO, callRateCardPrice, pricingDate.getTime());
            PlanItemWS planItemProd01WS2 = buildPlanItem(envBuilder.idForCode(INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED), MONTHLY_ORDER_PERIOD, ZERO, callRateCardPrice, pricingDate.getTime());
            //Create plan for telstra
            planId =  buildAndPersistPlan(envBuilder,api, AAPT_VOIP_PLAN_CODE, "100 Aapt Voip Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(subScriptionProd01), Collections.emptyList() , planItemProd01WS2);
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
            assertNotNull(PRODUCT_CREATION_FAILED, testEnvBuilder.idForCode(INBOUND_MOBILE_ONE_TO_THIRTEEN_HUNDRFED));
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
        if(null!= aaptVoipRouteRateCardId) {
            try {
                api.deleteRouteRateCard(aaptVoipRouteRateCardId);
            } catch(Exception ex) {
                logger.error("RouteRateCard deletion failed", ex);
            }

        }
    }

    /**
     * STN1 Valid CDR
     */
    @Test(priority = 1, enabled=true)
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

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);

                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);
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
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_CTOP_VALID);
                uuid = api.triggerMediationByConfigurationByFile(
                        getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {

                MediationProcess mediationProcess =
                        api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess); //
                assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable",Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_1));
                assertNotNull("Mediation Should Create Order", order);
                assertEquals("Invalid resolved quantity", new BigDecimal("3.00"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order amount", new BigDecimal("3.98"),
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

    @Test(priority = 2, enabled=true)
    public void test02MediationProcessOfCTOPFileWithValidCDR() {
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
                        testBuilder.getTestEnvironment().idForCode(ASSET), serviceNumber2, "asset-01"+ System.currentTimeMillis());
                assets.add(asset2);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

                productQuantityMap.putAll(buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);
                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);
                Integer orderId = createOrder("Test-Order2", activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap,TEST_USER_2);
                orders.add(orderId);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_2));
            }).validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_CTOP_VALID_2);
                uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_2));
                JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(order.getId());
                validatePricingFields(viewEvents);
                assertNotNull("Mediation Should Create Order", order);
                assertEquals("Invalid resolved quantity", new BigDecimal("3.00"),
                        order.getOrderLines()[0].getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                assertEquals("Invalid order amount", new BigDecimal("3.98"),
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
    //both records have same CDR_ID
    @Test(priority = 3, enabled=true)
    public void test03MediationProcessOfCTOPFileWithInValidCDRWithDuplicate() {
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
                        testBuilder.getTestEnvironment().idForCode(ASSET), serviceNumber3, "asset-01"+ System.currentTimeMillis());
                assets.add(asset3);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

                productQuantityMap.putAll(buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(ASSET), new BigDecimal(assets.size())));
                productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

                Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                productAssetMap.put(testBuilder.getTestEnvironment().idForCode(ASSET), assets);
                Calendar activeSinceDate = Calendar.getInstance();
                activeSinceDate.set(2018, 01, 01);
                Integer orderId = createOrder("Test-Order3", activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false, productQuantityMap, productAssetMap,TEST_USER_3);
                orders.add(orderId);

            }).validate((testEnv, testEnvBuilder) -> {
                assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_3));
            }).validate((testEnv, testEnvBuilder) -> {
                // trigger mediation
                JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                File cdrFile = new File(CDR_BASE_DIRECTORY + File.separator + CDR_FILE_NAME_CTOP_VALID_3);
                uuid = api.triggerMediationByConfigurationByFile(getMediationConfiguration(api, SPC_MEDIATION_JOB_NAME), cdrFile);
                pauseUntilMediationCompletes(30,api);
                logger.debug("Mediation ProcessId {}", uuid);
                assertNotNull(MEDIATION_TRIGGERED_SHOULD_RETURN_UUID, uuid);
            }).validate((testEnv, testEnvBuilder) -> {
                MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
                logger.debug("Mediation Process {}", mediationProcess);
                assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
                assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());
                assertEquals("Mediation Failed with Duplicates", Integer.valueOf(1), mediationProcess.getDuplicates());
                OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_3));
                assertNotNull("Mediation Should Create Order", order);
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


}