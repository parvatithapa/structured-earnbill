package com.sapienter.jbilling.server.fupProrating;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.api.automation.orders.OrdersTestHelper;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

@Test(groups = { "fupProrating" }, testName = "AddRemovePlanTest")
public class AddRemovePlanTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    private static final Integer CC_PM_ID = 5;
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private String subScriptionProd01 = "testPlanSubscriptionItem"+ System.currentTimeMillis();;
    private String plan01 = "100 free minute Plan";
    private String usagePoolO1 = "UP with 100 Quantity" + System.currentTimeMillis();
    private String testAccount = "Account Type";
    private String user01 = UUID.randomUUID().toString();
    private String user02 = UUID.randomUUID().toString();
    private String user03 = UUID.randomUUID().toString();
    private static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID= 320104;
    private String testCat1 = UUID.randomUUID().toString();
    private OrdersTestHelper testHelper;
    // Mediated Usage Products
    private static final int INBOUND_USAGE_PRODUCT_ID = 320101;
    private static final int CHAT_USAGE_PRODUCT_ID = 320102;
    private static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;
    private static final String SUBSCRIPTION_ORDER_CODE1 = "subscriptionOrder1"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE2 = "subscriptionOrder2"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE3 = "subscriptionOrder3"+ UUID.randomUUID().toString();
    private static final String USAGE_ORDER_CODE1 = "usageOrder1"+ UUID.randomUUID().toString();
    private static final String USAGE_ORDER_CODE2 = "usageOrder2"+ UUID.randomUUID().toString();
    private static final String ORDER_ASSERT = "Order Should not null";
    private static final String USER_ASSERT = "User Created {}";
    private static final String USER_CREATION_ASSERT = "User Creation Failed";
    private static final String ORDER_CREATION_ASSERT = "Order Creation Failed";
    private static final String USER_INVOICE_ASSERT = "Creating User with next invoice date {}";
    private static final String USAGE_POOL_QUANTITY = "100.0000000000";

    private static final String SUBSCRIPTION_ORDER_CODE4 = "subscriptionOrder4"+ UUID.randomUUID().toString();
    private static final String SUBSCRIPTION_ORDER_CODE5 = "subscriptionOrder5"+ UUID.randomUUID().toString();
    private String user04 = UUID.randomUUID().toString();
    private static final String USAGE_ORDER_CODE3 = "usageOrder3"+ UUID.randomUUID().toString();

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator ->
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi())
        );
    }

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testHelper = OrdersTestHelper.INSTANCE;
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            // Creating account type
            buildAndPersistAccountType(envBuilder, api, testAccount, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, testCat1, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            // Creating usage products
            buildAndPersistFlatProduct(envBuilder, api, subScriptionProd01, false, envBuilder.idForCode(testCat1), "100.00", true);

            // Usage product item ids
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2014);
            pricingDate.set(Calendar.MONTH, 6);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());
            PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, "0", "0.95", pricingDate.getTime());

            // creating Plans, Usage Pools for scenario 01

            // creating usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder, api, usagePoolO1, "100", envBuilder.idForCode(testCat1), items);

            // creating 100 min plan
            buildAndPersistPlan(envBuilder, api, plan01, "100 Free Minutes Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(subScriptionProd01), Arrays.asList(envBuilder.idForCode(usagePoolO1)), planItemProd01WS, planItemProd02WS,
                    planItemProd03WS);

        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(testAccount));
            assertNotNull("MediatedUsage Category Creation Failed", testEnvBuilder.idForCode(testCat1));
            assertNotNull("UsagePool Creation Failed", testEnvBuilder.idForCode(usagePoolO1));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(plan01));
        });
    }

    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    @Test
    public void testAddPlanOnExistingOrder01() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user01, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            AssetWS scenario18Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            Map<Integer, Integer> productAssetMap = buildProductAssetEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario18Asset.getId());

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE1 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE), productAssetMap, user01, false);

            logger.debug("Order Created {}", orderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user01));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE1));
        }).validate((testEnv, testEnvBuilder) -> {
            OrderWS subscriptionOrder = api.getOrder(environment.idForCode(SUBSCRIPTION_ORDER_CODE1));
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            List<OrderLineWS> lines = new ArrayList<>(Arrays.asList(subscriptionOrder.getOrderLines()));
            lines.add(buildOrderLine(environment.idForCode(subScriptionProd01), api));
            subscriptionOrder.setOrderLines(lines.toArray(new OrderLineWS[0]));
            api.updateOrder(subscriptionOrder, OrderChangeBL.buildFromOrder(subscriptionOrder, ORDER_CHANGE_STATUS_APPLY_ID));
            validateCustomerUsagePool(api, environment.idForCode(user01), USAGE_POOL_QUANTITY, FullCreativeUtil.getDate(06, 01, 2016),
                    FullCreativeUtil.getDate(06, 31, 2016));
        });


    }

    @Test
    public void testAddPlanOnExistingOrderAndRerateUsageOrder02() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user02, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            AssetWS scenario18Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            Map<Integer, Integer> productAssetMap = buildProductAssetEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario18Asset.getId());

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE2 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE), productAssetMap, user02, false);

            logger.debug("Order Created {}", orderId);

            Integer usageOrderId = createOrder(USAGE_ORDER_CODE1 , activeSinceDate, null, envHelper.getOrderPeriodOneTime(api),
                    true, buildProductQuantityEntry(INBOUND_USAGE_PRODUCT_ID, new BigDecimal("50")), null, user02, true);

            logger.debug("Usage Order Created {}", usageOrderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user02));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE2));
            assertNotNull("Usage Order Creation Failed", testEnvBuilder.idForCode(USAGE_ORDER_CODE1));
            OrderWS usageOrder = api.getOrder(environment.idForCode(USAGE_ORDER_CODE1));
            assertEquals("Usage Order Amount",  new BigDecimal("125.0000000000"), usageOrder.getTotalAsDecimal());

        }).validate((testEnv, testEnvBuilder) -> {
            OrderWS subscriptionOrder = api.getOrder(environment.idForCode(SUBSCRIPTION_ORDER_CODE2));
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            List<OrderLineWS> lines = new ArrayList<>(Arrays.asList(subscriptionOrder.getOrderLines()));
            lines.add(buildOrderLine(environment.idForCode(subScriptionProd01), api));
            subscriptionOrder.setOrderLines(lines.toArray(new OrderLineWS[0]));
            api.updateOrder(subscriptionOrder, OrderChangeBL.buildFromOrder(subscriptionOrder, ORDER_CHANGE_STATUS_APPLY_ID));
            validateCustomerUsagePool(api, environment.idForCode(user02), USAGE_POOL_QUANTITY, FullCreativeUtil.getDate(06, 01, 2016),
                    FullCreativeUtil.getDate(06, 31, 2016));

            OrderWS usageOrder = api.getOrder(environment.idForCode(USAGE_ORDER_CODE1));
            assertEquals("Free Usage Quantity", "50.0000000000", usageOrder.getFreeUsageQuantity());
        });

    }

    @Test
    public void testRemovePlanOnExistingOrderAndRerateUsageOrder03() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user03, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            AssetWS scenario18Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            Map<Integer, Integer> productAssetMap = buildProductAssetEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario18Asset.getId());

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE3 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE), productAssetMap, user03, false);

            logger.debug("Order Created {}", orderId);

            Integer usageOrderId = createOrder(USAGE_ORDER_CODE2 , activeSinceDate, null, envHelper.getOrderPeriodOneTime(api),
                    true, buildProductQuantityEntry(INBOUND_USAGE_PRODUCT_ID, new BigDecimal("50")), null, user03, true);

            logger.debug("Usage Order Created {}", usageOrderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user03));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE3));
            assertNotNull("Usage Order Creation Failed", testEnvBuilder.idForCode(USAGE_ORDER_CODE2));
            OrderWS usageOrder = api.getOrder(environment.idForCode(USAGE_ORDER_CODE2));
            assertEquals("Usage Order Amount",  new BigDecimal("125.0000000000"), usageOrder.getTotalAsDecimal());

        }).validate((testEnv, testEnvBuilder) -> {
            OrderWS subscriptionOrder = api.getOrder(environment.idForCode(SUBSCRIPTION_ORDER_CODE3));
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            List<OrderLineWS> lines = new ArrayList<>(Arrays.asList(subscriptionOrder.getOrderLines()));
            lines.add(buildOrderLine(environment.idForCode(subScriptionProd01), api));
            subscriptionOrder.setOrderLines(lines.toArray(new OrderLineWS[0]));
            api.updateOrder(subscriptionOrder, OrderChangeBL.buildFromOrder(subscriptionOrder, ORDER_CHANGE_STATUS_APPLY_ID));
            validateCustomerUsagePool(api, environment.idForCode(user02), USAGE_POOL_QUANTITY, FullCreativeUtil.getDate(06, 01, 2016),
                    FullCreativeUtil.getDate(06, 31, 2016));

            OrderWS usageOrder = api.getOrder(environment.idForCode(USAGE_ORDER_CODE2));
            assertEquals("Free Usage Quantity",  "50.0000000000", usageOrder.getFreeUsageQuantity());
        }).validate((testEnv, testEnvBuilder) -> {
            OrderWS subscriptionOrder = api.getOrder(environment.idForCode(SUBSCRIPTION_ORDER_CODE3));
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            List<OrderLineWS> lines = new ArrayList<>(Arrays.asList(subscriptionOrder.getOrderLines()));
            subscriptionOrder.setOrderLines(removeLineByItemId(environment.idForCode(subScriptionProd01), lines));
            api.updateOrder(subscriptionOrder, OrderChangeBL.buildFromOrder(subscriptionOrder, ORDER_CHANGE_STATUS_APPLY_ID));
            OrderWS usageOrder = api.getOrder(environment.idForCode(USAGE_ORDER_CODE2));
            assertEquals("Usage Order Amount",  "125.0000000000", usageOrder.getTotal());
        });
    }

    @Test
    public void testSubScribeMultiplePlans() {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        final JbillingAPI api = environment.getPrancingPonyApi();
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug(USER_INVOICE_ASSERT, nextInvoiceDate);
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, user04, nextInvoiceDate, envHelper.getOrderPeriodMonth(api), api);
            logger.debug(USER_ASSERT, customerId);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            AssetWS scenario18Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            Map<Integer, Integer> productAssetMap = buildProductAssetEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario18Asset.getId());
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            Integer orderId = createOrder(SUBSCRIPTION_ORDER_CODE4 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user04, false);

            logger.debug("Order Created {}", orderId);

            scenario18Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
            productAssetMap = buildProductAssetEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario18Asset.getId());

            productQuantityMap = new HashMap<>();
            productQuantityMap.putAll(buildProductQuantityEntry(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE));
            productQuantityMap.putAll(buildProductQuantityEntry(envBuilder.idForCode(subScriptionProd01), BigDecimal.ONE));

            orderId = createOrder(SUBSCRIPTION_ORDER_CODE5 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api),
                    true, productQuantityMap, productAssetMap, user04, false);

            logger.debug("Order Created {}", orderId);

            Integer usageOrderId = createOrder(USAGE_ORDER_CODE3 , activeSinceDate, null, envHelper.getOrderPeriodOneTime(api),
                    true, buildProductQuantityEntry(INBOUND_USAGE_PRODUCT_ID, new BigDecimal("250")), null, user04, true);

            logger.debug("Usage Order Created {}", usageOrderId);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull(USER_CREATION_ASSERT, testEnvBuilder.idForCode(user04));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE4));
            assertNotNull(ORDER_CREATION_ASSERT, testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_CODE5));
            assertNotNull("Usage Order Creation Failed", testEnvBuilder.idForCode(USAGE_ORDER_CODE3));
            OrderWS usageOrder = api.getOrder(environment.idForCode(USAGE_ORDER_CODE3));

            assertEquals("Usage Order Amount",  new BigDecimal("47.5000000000"), usageOrder.getTotalAsDecimal());
            assertEquals("Free Usage Quantity",  "200.0000000000", usageOrder.getFreeUsageQuantity());

        }).validate((testEnv, testEnvBuilder) -> {
            OrderWS subscriptionOrder = api.getOrder(environment.idForCode(SUBSCRIPTION_ORDER_CODE4));
            assertNotNull(ORDER_ASSERT, subscriptionOrder);
            List<OrderLineWS> lines = new ArrayList<>(Arrays.asList(subscriptionOrder.getOrderLines()));
            subscriptionOrder.setOrderLines(removeLineByItemId(environment.idForCode(subScriptionProd01), lines));
            api.updateOrder(subscriptionOrder, OrderChangeBL.buildFromOrder(subscriptionOrder, ORDER_CHANGE_STATUS_APPLY_ID));
            OrderWS usageOrder = api.getOrder(environment.idForCode(USAGE_ORDER_CODE3));
            assertEquals("Usage Order Amount",  new BigDecimal("142.5000000000"), usageOrder.getTotalAsDecimal());
        });
    }

    private OrderLineWS[] removeLineByItemId(Integer itemId, List<OrderLineWS> lines) {
        return lines.stream()
                .filter(line -> line.getItemId().equals(itemId))
                .peek(line -> line.setDeleted(1))
                .toArray(OrderLineWS[]::new);
    }

    private void validateCustomerUsagePool(JbillingAPI api, Integer userId, String quanity, Date start, Date end) {
        UserWS user = api.getUserWS(userId);
        CustomerUsagePoolWS[] usagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
        assertNotNull("UsagePool creation failed", usagePools);
        assertEquals("Numbe Of Usage pool", 1, usagePools.length);
        assertEquals("Usage pool quantity ", quanity, usagePools[0].getInitialQuantity());
        assertEquals("Expected Cycle start date of customer usage pool: "
                ,TestConstants.DATE_FORMAT.format(start)
                ,TestConstants.DATE_FORMAT.format(usagePools[0].getCycleStartDate()));
        assertEquals("Expected Cycle end date of customer usage pool: "
                ,TestConstants.DATE_FORMAT.format(end)
                ,TestConstants.DATE_FORMAT.format(usagePools[0].getCycleEndDate()));

    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, String flatPrice, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(flatPrice)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    private PlanItemWS buildPlanItem(JbillingAPI api, Integer itemId, Integer periodId, String quantity, String price, Date pricingDate) {
        return PlanBuilder.PlanItemBuilder.getBuilder()
                .withItemId(itemId)
                .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .addModel(pricingDate, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .withBundledPeriodId(periodId)
                .withBundledQuantity(quantity)
                .build();
    }

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
            Integer periodId, Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String quantity,Integer categoryId, List<Integer>  items) {
        return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code)
                .withQuantity(quantity)
                .withResetValue("Reset To Initial Value")
                .withItemIds(items)
                .addItemTypeId(categoryId)
                .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                .withCyclePeriodValue(Integer.valueOf(1)).withName(code)
                .build();
    }

    private Map<Integer, BigDecimal> buildProductQuantityEntry(Integer productId, BigDecimal quantity){
        return Collections.singletonMap(productId, quantity);
    }

    private Map<Integer, Integer> buildProductAssetEntry(Integer productId, Integer assetId){
        return Collections.singletonMap(productId, assetId);
    }

    private AssetWS getAssetIdByProductId(JbillingAPI api, Integer productId) {
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(1);
        criteria.setOffset(0);
        criteria.setSort("id");
        criteria.setTotal(-1);
        criteria.setFilters(new BasicFilter[]{basicFilter});

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(productId, criteria);
        assertNotNull("No available asset found for product "+productId, assetsResult);
        AssetWS[] availableAssets = assetsResult.getObjects();
        assertTrue("No assets found for product .", null != availableAssets && availableAssets.length != 0);
        Integer assetIdProduct = availableAssets[0].getId();
        logger.debug("Asset Available for product {} = {}", productId, assetIdProduct);
        return availableAssets[0];
    }

    private OrderLineWS buildOrderLine(Integer itemId, JbillingAPI api) {
        OrderLineWS line = new OrderLineWS();
        line.setItemId(itemId);
        line.setTypeId(1);
        ItemDTOEx item = api.getItem(itemId, null, null);
        line.setDescription(item.getDescription());
        line.setQuantity(BigDecimal.ONE);
        line.setUseItem(true);
        return line;
    }

    private Integer createOrder(String code,Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate,
            Map<Integer, BigDecimal> productQuantityMap, Map<Integer, Integer> productAssetMap, String userCode, boolean isUsageOrder) {
        this.testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            List<OrderLineWS> lines = productQuantityMap.entrySet()
                    .stream()
                    .map(lineItemQuatityEntry -> {
                        OrderLineWS line = new OrderLineWS();
                        line.setItemId(lineItemQuatityEntry.getKey());
                        line.setTypeId(Integer.valueOf(1));
                        ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                        line.setDescription(item.getDescription());
                        line.setQuantity(lineItemQuatityEntry.getValue());
                        line.setUseItem(true);
                        if(null!=productAssetMap && !productAssetMap.isEmpty()
                                && productAssetMap.containsKey(line.getItemId())) {
                            line.setAssetIds(new Integer[] {productAssetMap.get(line.getItemId())});
                        }
                        return line;
                    }).collect(Collectors.toList());

            envBuilder.orderBuilder(api)
            .withCodeForTests(code)
            .forUser(envBuilder.idForCode(userCode))
            .withActiveSince(activeSince)
            .withActiveUntil(activeUntil)
            .withEffectiveDate(activeSince)
            .withPeriod(orderPeriodId)
            .withProrate(prorate)
            .withOrderLines(lines)
            .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID)
            .withIsMediated(isUsageOrder)
            .build();
        }).test((testEnv, envBuilder) ->
            assertNotNull(ORDER_CREATION_ASSERT, envBuilder.idForCode(code))
        );
        return testBuilder.getTestEnvironment().idForCode(code);
    }
}
