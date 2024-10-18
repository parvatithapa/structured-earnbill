/*package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.task.SpcCreditOrderCreationTask;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

@Test(testName = "spc.CreditAmountUsagePoolTest")
public class CreditAmountUsagePoolTest extends BaseMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PLAN_LEVEL_META_FIELD                       = "Credit Amount";
    protected static final String PLAN_LEVEL_META_FIELD_VALUE_1             = "10";
    protected static final String PLAN_LEVEL_META_FIELD_VALUE_2             = "20";
    private static final String OPERATOR_ASSISTED_CALL                      = "Operator Assisted Call";
    private static final String ENGINE_SCONNECT_PLAN_ITEM                   = "testPlanSubscriptionItem" + System.currentTimeMillis();
    private static final int MONTHLY_ORDER_PERIOD                           =  2;
    private static final String TEST_PLAN_CODE                              =  "Test-Plan" + System.currentTimeMillis();
    private static final String USER_01                                     =  "TestUser01" + System.currentTimeMillis();
    private static final String USER_02                                     =  "TestUser02" + System.currentTimeMillis();
    private static final String USER_03                                     =  "TestUser03" + System.currentTimeMillis();
    private static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID          =  320104;
    private static final String ASSET01_NUMBER                              = RandomStringUtils.randomNumeric(10);
    private static final String ASSET02_NUMBER                              = RandomStringUtils.randomNumeric(10);
    private static final String ASSET03_NUMBER                              = RandomStringUtils.randomNumeric(10);
    private static final String SUBSCRIPTION_ORDER_01                       = "subscription01"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02                       = "subscription02"+ System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_03                       = "subscription03"+ System.currentTimeMillis();
    private static final String RATE_CARD_NAME                              = "engine_sconnect_plan_rating_card";
    private static final String RATE_CARD_HEADER                            = "id,name,surcharge,initial_increment,subsequent_increment,charge,"
            + "route_id,tariff_code";
    private static final String CREDIT_ADJUSTABLE_ITEM_ID                   = "240";

    private Integer engineSConnectRouteRateCardId ;
    private Integer creditAmountUsagePoolTaskPluginId;

    @BeforeClass
    public void initializeTests() {
        super.initializeTests();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            buildAndPersistMetafield(testBuilder, PLAN_LEVEL_META_FIELD, DataType.STRING, EntityType.PLAN);

            buildAndPersistFlatProduct(envBuilder, api, OPERATOR_ASSISTED_CALL, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false);

            buildAndPersistFlatProduct(envBuilder, api, OPERATOR_ASSISTED_CALL, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "1", true, 0, false);

            buildAndPersistFlatProduct(envBuilder, api, ENGINE_SCONNECT_PLAN_ITEM, false,
                    envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "100.00", false, 0, true);

            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName(RATE_CARD_NAME);
            routeRateCardWS.setRatingUnitId(spcRatingUnitId);
            routeRateCardWS.setEntityId(api.getCallerCompanyId());

            List<String> rateRecords = Arrays.asList("1,Operator Assisted Call,0.39,60,60,0.25,2,OP:NAT",
                    "2,Operator Assisted Call,0.89,60,60,0.25,3:UK,OP:NAT");
            String enginSconnectRouteRateCardFilePath = createFileWithData(RATE_CARD_NAME, ".csv", RATE_CARD_HEADER, rateRecords);
            engineSConnectRouteRateCardId = api.createRouteRateCard(routeRateCardWS, new File(enginSconnectRouteRateCardFilePath));
            logger.debug("Engine SConnect Route Rate Card id {}", engineSConnectRouteRateCardId);
            Integer matchingFieldId = api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, engineSConnectRouteRateCardId));
            logger.debug("Matching Field id {}", matchingFieldId);

            // building Rate Card pricing strategy
            PriceModelWS engineSCOperatorAssistedCallPriceModel = buildRateCardPriceModel(engineSConnectRouteRateCardId, "Quantity");

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);


            PlanItemWS testPlanItem = buildPlanItem(envBuilder.idForCode(OPERATOR_ASSISTED_CALL),
                    MONTHLY_ORDER_PERIOD, "0.00", engineSCOperatorAssistedCallPriceModel, pricingDate.getTime());

            Integer planId =  buildAndPersistPlan(envBuilder,api, TEST_PLAN_CODE, "100 Engine SConnect Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), Collections.emptyList() , testPlanItem);

            setPlanLevelMetaField(planId, PLAN_LEVEL_META_FIELD_VALUE_1);

            creditAmountUsagePoolTaskPluginId = configureCreditAmountUsagePoolPlugin(CREDIT_ADJUSTABLE_ITEM_ID);
        });
    }

    @Override
    @AfterClass
    public void tearDown() {
        super.tearDown();
        try {
            if(api.getPluginWS(creditAmountUsagePoolTaskPluginId) != null)
                api.deletePlugin(creditAmountUsagePoolTaskPluginId);
        } catch(Exception e) {
            logger.error("Credit amount usage pool task plugin not found ");
        }
    }


 * Test for plan level meta-field value = 10
 * Required plugin - CreditAmountUsagePoolTask is configured

    @Test(priority = 1)
    void testScenario01() {
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
            Integer[] createdInvoiceIds = api.createInvoiceWithDate(testEnv.idForCode(USER_01), null, null, null, true);
            InvoiceWS invoice = api.getInvoiceWS(createdInvoiceIds[0]);
            assertNotNull("Invoice should be created", createdInvoiceIds);
            OrderWS[] userSubscriptions = api.getUsersAllSubscriptions(testEnv.idForCode(USER_01));
            String subscriptionTotal = userSubscriptions[0].getTotal();

            assertEquals("Subscription order should be of total", BigDecimal.valueOf(104.99).setScale(2, RoundingMode.CEILING),
                    new BigDecimal(userSubscriptions[0].getTotal()).setScale(2, RoundingMode.CEILING));

            assertEquals("Invoice amount should be subscription total - credit amount",
                    new BigDecimal(subscriptionTotal).add(BigDecimal.TEN.negate()).setScale(2, RoundingMode.CEILING),
                    new BigDecimal(invoice.getTotal()).setScale(2, RoundingMode.CEILING));

            Integer[] orderIds = api.getInvoiceWS(Arrays.stream(createdInvoiceIds).iterator().next()).getOrders();
            int counter = 0;
            BigDecimal creditAmount = BigDecimal.ZERO;
            for (Integer oid : orderIds) {
                for (OrderLineWS line : api.getOrder(oid).getOrderLines()) {
                    if(line.getTypeId() == 7) {
                        counter ++;
                        creditAmount = new BigDecimal(line.getAmount());
                    }
                }
            }
            assertEquals("Credit order should not be generated", 1, counter);
            assertEquals("Credit order should be created of the amount set in the plan metafield = -10 ",
                    BigDecimal.valueOf(-10).setScale(2, RoundingMode.CEILING),
                    creditAmount.setScale(2, RoundingMode.CEILING));

        });
    }


 * Test for plan level meta-field value = 10
 * Required plugin - CreditAmountUsagePoolTask is not configured

    @Test(priority = 2)
    void testScenario02() {
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

            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET02_NUMBER);
            logger.debug("asset created {} for number {}", asset1, ASSET02_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset1));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_02, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_02);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_02));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 02 Creation Failed", testEnvBuilder.idForCode(USER_02));
            assertNotNull("subscription order 02 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_01));
            api.deletePlugin(creditAmountUsagePoolTaskPluginId);
        }).validate((testEnv, testEnvBuilder) -> {
            Integer[] createdInvoiceIds = api.createInvoiceWithDate(testEnv.idForCode(USER_02), null, null, null, true);
            InvoiceWS invoice = api.getInvoiceWS(createdInvoiceIds[0]);
            assertNotNull("Invoice should be created", createdInvoiceIds);
            OrderWS[] userSubscriptions = api.getUsersAllSubscriptions(testEnv.idForCode(USER_02));
            String subscriptionTotal = userSubscriptions[0].getTotal();

            assertEquals("Subscription order should be of total", BigDecimal.valueOf(104.99).setScale(2, RoundingMode.CEILING),
                    new BigDecimal(userSubscriptions[0].getTotal()).setScale(2, RoundingMode.CEILING));

            assertEquals("Invoice amount should be subscription total only",
                    new BigDecimal(subscriptionTotal).setScale(2, RoundingMode.CEILING),
                    new BigDecimal(invoice.getTotal()).setScale(2, RoundingMode.CEILING));

            Integer[] orderIds = api.getInvoiceWS(Arrays.stream(createdInvoiceIds).iterator().next()).getOrders();

            int counter = 0;
            for (Integer oid : orderIds) {
                for (OrderLineWS line : api.getOrder(oid).getOrderLines()) {
                    if(line.getTypeId() == 7) {
                        counter ++;
                    }
                }
            }
            assertEquals("Credit order should not be generated", 0, counter);

        }).validate((testEnv, testEnvBuilder) -> {
            configureCreditAmountUsagePoolPlugin(CREDIT_ADJUSTABLE_ITEM_ID);
        });
    }


 * Test for plan level meta-field value = 20
 * Required plugin - CreditAmountUsagePoolTask is configured

    @Test(priority = 3)
    void testScenario03() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user03 = envBuilder.customerBuilder(api)
                    .withUsername(USER_03)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(new Date())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, 1))
                    .build();

            logger.debug("User created {}", user03.getId());

            ItemDTOEx assetEnabledProduct = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);

            Integer asset1 = buildAndPersistAsset(envBuilder, assetEnabledProduct.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, ASSET03_NUMBER);
            logger.debug("asset created {} for number {}", asset1, ASSET03_NUMBER);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(ENGINE_SCONNECT_PLAN_ITEM), BigDecimal.ONE));

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset1));
            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.set(2018, 01, 01);
            Integer orderId = createOrder(SUBSCRIPTION_ORDER_03, activeSinceDate.getTime(), null, MONTHLY_ORDER_PERIOD, false,
                    productQuantityMap, productAssetMap, USER_03);
            logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(USER_03));

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("user 03 Creation Failed", testEnvBuilder.idForCode(USER_03));
            assertNotNull("subscription order 03 Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_03));
            setPlanLevelMetaField(testEnv.idForCode(TEST_PLAN_CODE), PLAN_LEVEL_META_FIELD_VALUE_2);
        }).validate((testEnv, testEnvBuilder) -> {
            Integer[] createdInvoiceIds = api.createInvoiceWithDate(testEnv.idForCode(USER_03), null, null, null, true);
            InvoiceWS invoice = api.getInvoiceWS(createdInvoiceIds[0]);
            assertNotNull("Invoice should be created", createdInvoiceIds);
            OrderWS[] userSubscriptions = api.getUsersAllSubscriptions(testEnv.idForCode(USER_03));
            String subscriptionTotal = userSubscriptions[0].getTotal();

            assertEquals("Subscription order should be of total", BigDecimal.valueOf(104.99).setScale(2, RoundingMode.CEILING),
                    new BigDecimal(userSubscriptions[0].getTotal()).setScale(2, RoundingMode.CEILING));

            assertEquals("Invoice amount should be subscription total - credit amount",
                    new BigDecimal(subscriptionTotal).add(new BigDecimal(20).negate()).setScale(2, RoundingMode.CEILING),
                    new BigDecimal(invoice.getTotal()).setScale(2, RoundingMode.CEILING));

            Integer[] orderIds = api.getInvoiceWS(Arrays.stream(createdInvoiceIds).iterator().next()).getOrders();

            int counter = 0;
            BigDecimal creditAmount = BigDecimal.ZERO;
            for (Integer oid : orderIds) {
                for (OrderLineWS line : api.getOrder(oid).getOrderLines()) {
                    if(line.getTypeId() == 7) {
                        counter ++;
                        creditAmount = new BigDecimal(line.getAmount());
                    }
                }
            }
            assertEquals("Credit order should not be generated", 1, counter);
            assertEquals("Credit order should be created of the amount set in the plan metafield = -20 ",
                    BigDecimal.valueOf(-20).setScale(2, RoundingMode.CEILING),
                    creditAmount.setScale(2, RoundingMode.CEILING));

        });
    }

    protected void setPlanLevelMetaField(Integer planId, String name) {
        logger.debug("setting the plan level metafields for plan {}", planId);
        PlanWS plan = api.getPlanWS(planId);
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
        Arrays.asList(plan.getMetaFields()).forEach(mf -> {
            if (mf.getFieldName().equals(PLAN_LEVEL_META_FIELD)) {
                mf.setValue(name);
                values.add(mf);
            }
        });
        values.forEach(value ->
        value.setEntityId(api.getCallerCompanyId())
                );
        plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updatePlan(plan);
    }

    private Integer configureCreditAmountUsagePoolPlugin(String adjustableItemId) {

        PluggableTaskWS creditAmountUsagePoolTask = new PluggableTaskWS();
        creditAmountUsagePoolTask.setProcessingOrder(101);
        PluggableTaskTypeWS creditAmountUsagePoolTaskType =
                api.getPluginTypeWSByClassName(SpcCreditOrderCreationTask.class.getName());
        creditAmountUsagePoolTask.setTypeId(creditAmountUsagePoolTaskType.getId());

        creditAmountUsagePoolTask.setParameters(new Hashtable<String, String>(creditAmountUsagePoolTask.getParameters()));
        Hashtable<String, String> parameters = new Hashtable<>();
        parameters.put("credit_item_id", adjustableItemId);
        creditAmountUsagePoolTask.setParameters(parameters);
        return api.createPlugin(creditAmountUsagePoolTask);
    }
}
 */