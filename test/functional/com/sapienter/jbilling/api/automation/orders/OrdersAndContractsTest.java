package com.sapienter.jbilling.api.automation.orders;

import com.gurock.testrail.TestRailClass;
import com.gurock.testrail.TestRailMethod;
import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Created by hristijan on 6/8/16.
 */
@Test(groups = {"api-automation"}, testName = "OrdersAndContractsTest")
@TestRailClass(description="Orders and contracts tests")
public class OrdersAndContractsTest {

    private static final Logger logger = LoggerFactory.getLogger(OrdersAndContractsTest.class);
    private EnvironmentHelper environmentHelper;
    private OrdersTestHelper testHelper;
    private TestBuilder testBuilder;

    @BeforeClass
    public void initializeTests(){
        testBuilder = getTestEnvironment();
    }


    @AfterClass
    public void tearDown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != environmentHelper){
            environmentHelper = null;
        }
        if (null != testBuilder){
            testBuilder = null;
        }
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest().givenForMultiple(testEnvironmentCreator -> {
            environmentHelper = EnvironmentHelper.getInstance(testEnvironmentCreator.getPrancingPonyApi());
            testHelper = OrdersTestHelper.INSTANCE;
        });
    }

    @Test(priority = 0)
    @TestRailMethod(
            title = "Test 001 - Mandatory and optional dependency products",
            summary = "Verify jBilling API can create products with mandatory and optional dependencies correctly.",
            refs = "JBQA-14")
    public void test001MandatoryAndOptionalDependencyProducts(){

        final Integer[] productIds = new Integer[3];

       testBuilder.given(envBuilder -> {

           JbillingAPI api = envBuilder.getPrancingPonyApi();
           
           String categoryCode1 = "Calls";
           Integer categoryId1 = envBuilder.itemBuilder(api).itemType().withCode(categoryCode1).global(true)
                   .build();

           String productCode1 = "LocalCallsIncluded";
           productIds[0] = createGraduatedProduct(productCode1, categoryId1, true, "18", "100", envBuilder, api);

           String assetCategoryCode = "AllowAssetMgmt1";
           Integer assetCategoryId = createCategory(assetCategoryCode, false, Integer.valueOf(1), envBuilder, api);

           String assetProductCode = "PhoneLineInstalation";
           productIds[1] = createProduct(assetProductCode, assetCategoryId, "33", false,
                   Integer.valueOf(1), envBuilder, api, api.getCallerCompanyId());

           String assetProductCodeTwo = "PhoneLine";

           ItemBuilder itemBuilder = envBuilder.itemBuilder(api);
           productIds[2] = itemBuilder
                   .item()
                   .withCode(assetProductCodeTwo)
                   .global(false)
                   .withType(assetCategoryId)
                   .withEntities(api.getCallerCompanyId())
                   .withAssetManagementEnabled(Integer.valueOf(1))
                   .withFlatPrice("12")
                   .withDependencies(
                           itemBuilder.itemDependency()
                                   .withDependentId(productIds[1])
                                   .withMinimum(1)
                                   .withItemDependencyType(ItemDependencyType.ITEM)
                                   .build()
                           ,
                           itemBuilder.itemDependency()
                                   .withDependentId(productIds[0])
                                   .withMinimum(0)
                                   .withItemDependencyType(ItemDependencyType.ITEM)
                                   .build()
                   )
                   .build();
       }).test((env) -> {

           JbillingAPI api = env.getPrancingPonyApi();
           ItemDTOEx itemDTOEx = api.getItem(productIds[2], null, null);
           assertNotNull(itemDTOEx, "Item can not be null!!");
           assertEquals(itemDTOEx.getDependencies().length, 2, "Invalid number of item dependencies");
           assertEquals(itemDTOEx.getDependenciesOfType(ItemDependencyType.ITEM).length, 2,
                   "Invalid number of item dependencies type!");
           assertEquals(itemDTOEx.getDependenciesOfType(ItemDependencyType.ITEM_TYPE).length, 0,
                   "Invalid number of item type dependencies type!");
           ItemDependencyDTOEx[] itemDependencyDTOExes = itemDTOEx.getDependencies();
           sortItemDependencyByItemId(itemDependencyDTOExes);
           for (ItemDependencyDTOEx itemDependency : itemDependencyDTOExes) {
               assertEquals(itemDependency.getItemId(), productIds[2], "Invalid item id!");
           }
           assertEquals(itemDependencyDTOExes[0].getDependentId(), productIds[0]);
           assertEquals(itemDependencyDTOExes[0].getMinimum(), Integer.valueOf(0));
           assertEquals(itemDependencyDTOExes[1].getDependentId(), productIds[1]);
           assertEquals(itemDependencyDTOExes[1].getMinimum(), Integer.valueOf(1));
       });
    }

    @Test(priority = 1)
    @TestRailMethod(
            title = "Test 002 - Order with mandatory and optional dependency products",
            summary = "Verify jBilling API can create order with dependency products correctly.",
            refs = "JBQA-14")
    public void test002OrderWithMandatoryAndOptionalDependencyProducts(){

        final Integer[] productIds = new Integer[3];
        final Integer[] orderIds = new Integer[1];
        final Integer[] userIds = new Integer[1];

        TestBuilder.newTest().given(envBuilder -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String categoryCode1 = "Calls";
            Integer categoryId1 = createCategory(categoryCode1, true, Integer.valueOf(0), envBuilder, api);

            String productCode1 = "LocalCallsIncluded";
            productIds[0] = createGraduatedProduct(productCode1, categoryId1, true, "18", "100", envBuilder, api);

            String assetCategoryCode = "AllowAssetMgmt1";

            Integer assetCategoryId = createCategory(assetCategoryCode, false, Integer.valueOf(0), envBuilder, api);

            String assetProductCode = "PhoneLineInstalation";

            productIds[1] = createProduct(assetProductCode, assetCategoryId, "33", false,
                    Integer.valueOf(0), envBuilder, api, api.getCallerCompanyId());

            String assetProductCodeTwo = "PhoneLine";

            ItemBuilder itemBuilder = envBuilder.itemBuilder(api);
            productIds[2] = itemBuilder
                    .item()
                    .withCode(assetProductCodeTwo)
                    .global(false)
                    .withType(assetCategoryId)
                    .withEntities(api.getCallerCompanyId())
                    .withFlatPrice("12")
                    .withDependencies(
                            itemBuilder.itemDependency()
                                    .withDependentId(productIds[1])
                                    .withMinimum(1)
                                    .withItemDependencyType(ItemDependencyType.ITEM)
                                    .build()
                            ,
                            itemBuilder.itemDependency()
                                    .withDependentId(productIds[0])
                                    .withMinimum(0)
                                    .withItemDependencyType(ItemDependencyType.ITEM)
                                    .build()
                    )
                    .build();

            envBuilder.accountTypeBuilder(api)
                    .withName("Account Type 1")
                    .withInvoiceDesign("Simple Design")
                    .withCreditLimit("200")
                    .withCreditNotificationLimit1("10")
                    .withCreditNotificationLimit2("20")
                    .build();

            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();

            userIds[0] = userWS.getId();

        }).test((env, envBuilder) -> {

            JbillingAPI jbillingAPI = envBuilder.getPrancingPonyApi();

            // Parent Line
            OrderLineWS parentOrderLine = envBuilder.orderBuilder(jbillingAPI).orderLine()
                    .withItemId(productIds[2]) // default quantity 1
                    .build();
            // Child Line
            OrderLineWS childOrderLine = envBuilder.orderBuilder(jbillingAPI).orderLine()
                    .withItemId(productIds[1]) // default quantity 1
                    .withParentLine(parentOrderLine)
                    .build();

            parentOrderLine.setChildLines(new OrderLineWS[]{childOrderLine});

            orderIds[0] = envBuilder.orderBuilder(jbillingAPI)
                    .forUser(userIds[0])
                    .withPeriod(environmentHelper.getOrderPeriodOneTime(jbillingAPI))
                    .withOrderLine(parentOrderLine)
                    .withOrderLine(childOrderLine)
                    .withCodeForTests("codefortest")
                    .build();

            OrderWS orderWS = jbillingAPI.getOrder(orderIds[0]);
            assertNotNull(orderWS);
            assertEquals(orderWS.getOrderLines().length, 2, "Invalid number order lines");
            assertEquals(orderWS.getPeriod(), environmentHelper.getOrderPeriodOneTime(jbillingAPI), "Invalid Order Period");
            assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("45").setScale(2, BigDecimal.ROUND_CEILING));


        });
    }

    @Test(priority = 2,expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*OrderWS,hierarchy,error.order.hierarchy.product.mandatory.dependency.not.meet.*")
    @TestRailMethod(
            title = "Test 003 - Order with mandatory and optional dependency products not met dependencies",
            summary = "Verify jBilling API throws expected exception after creating order with products with not met dependencies.",
            refs = "JBQA-14")
    public void test003OrderWithMandatoryAndOptionalDependencyProductsNotMetDependencies(){

        final Integer[] productIds = new Integer[3];
        final Integer[] orderIds = new Integer[1];
        final Integer[] userIds= new Integer[1];

        TestBuilder.newTest().given(envBuilder -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String categoryCode1 = "Calls";
            Integer categoryId1 = createCategory(categoryCode1, true, Integer.valueOf(0), envBuilder, api);

            String productCode1 = "LocalCallsIncluded";
            productIds[0] = createGraduatedProduct(productCode1, categoryId1, true, "18", "100", envBuilder, api);

            String assetCategoryCode = "AllowAssetMgmt1";

            Integer assetCategoryId = createCategory(assetCategoryCode, false, Integer.valueOf(0), envBuilder, api);

            String assetProductCode = "PhoneLineInstalation";

            productIds[1] = createProduct(assetProductCode, assetCategoryId, "33", false,
                    Integer.valueOf(0), envBuilder, api, api.getCallerCompanyId());

            String assetProductCodeTwo = "PhoneLine";

            ItemBuilder itemBuilder = envBuilder.itemBuilder(api);
            productIds[2] = itemBuilder
                    .item()
                    .withCode(assetProductCodeTwo)
                    .global(false)
                    .withType(assetCategoryId)
                    .withEntities(api.getCallerCompanyId())
                    .withFlatPrice("12")
                    .withDependencies(
                            itemBuilder.itemDependency()
                                    .withDependentId(productIds[1])
                                    .withMinimum(1)
                                    .withItemDependencyType(ItemDependencyType.ITEM)
                                    .build()
                            ,
                            itemBuilder.itemDependency()
                                    .withDependentId(productIds[0])
                                    .withMinimum(0)
                                    .withItemDependencyType(ItemDependencyType.ITEM)
                                    .build()
                    )
                    .build();

            AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                    .withName("Account Type 1")
                    .withInvoiceDesign("Simple Design")
                    .withCreditLimit("200")
                    .withCreditNotificationLimit1("10")
                    .withCreditNotificationLimit2("20")
                    .build();

            UserWS userWS = envBuilder.customerBuilder(api)
                    .withAccountTypeId(accountTypeWS.getId())
                    .withUsername("TestCustomer")
                    .build();

            userIds[0] = userWS.getId();

        }).test((env, envBuilder) -> {

            JbillingAPI jbillingAPI = envBuilder.getPrancingPonyApi();
            OrderBuilder orderBuilder = envBuilder.orderBuilder(jbillingAPI);

            // Parent Line
            OrderLineWS parentOrderLine = orderBuilder.orderLine()
                    .withItemId(productIds[2]) // default quantity 1
                    .build();

            orderIds[0] = orderBuilder
                    .forUser(userIds[0])
                    .withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests("codefortest")
                    .build();

        });

    }

     @Test(priority = 3)
     @TestRailMethod(
             title = "Test 004 - Order with product status pending",
             summary = "Verify jBilling API can change order status to pending and no order lines are created.",
             refs = "JBQA-14")
     public void test004OrderWithProductStatusPending(){

         final Integer[] userIds = new Integer[1];
         final Integer[] productIds = new Integer[1];
         final Integer[] orderIds = new Integer[1];

         TestBuilder.newTest().given(envBuilder -> {

             JbillingAPI api = envBuilder.getPrancingPonyApi();

             String categoryCode = "Test Category" + System.currentTimeMillis();
             Integer categoryId = createCategory(categoryCode, false, Integer.valueOf(0), envBuilder, api);

             String productCode = "TestProduct";
             productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                     envBuilder, api, api.getCallerCompanyId());

             AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                     .withName("Account Type 1")
                     .withInvoiceDesign("Simple Design")
                     .withCreditLimit("200")
                     .withCreditNotificationLimit1("10")
                     .withCreditNotificationLimit2("20")
                     .build();

             UserWS userWS = envBuilder.customerBuilder(api)
                     .withAccountTypeId(accountTypeWS.getId())
                     .withUsername("TestCustomer")
                     .build();

             userIds[0] = userWS.getId();

         }).test((env, envBuilder) -> {

             JbillingAPI api = env.getPrancingPonyApi();
             Integer orderStatusPendingId = getOrCreateOrderChangeStatus(api, ApplyToOrder.NO, Integer.valueOf(2));

             try {
                 OrderBuilder orderBuilder = envBuilder.orderBuilder(api);
                 // Parent Line
                 OrderLineWS parentOrderLine = orderBuilder.orderLine()
                         .withItemId(productIds[0])// default quantity 1
                         .build();

                 orderIds[0] = orderBuilder
                         .forUser(userIds[0])
                         .withPeriod(Constants.ORDER_PERIOD_ONCE)
                         .withOrderLine(parentOrderLine)
                         .withCodeForTests("codefortest")
                         .withOrderChangeStatus(orderStatusPendingId)
                         .build();

                 OrderWS orderWS = api.getOrder(orderIds[0]);
                 assertNotNull(orderWS);
                 assertEquals(orderWS.getOrderLines().length, 0, "Invalid number Order Lines");
                 assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                         new BigDecimal("0").setScale(2, BigDecimal.ROUND_CEILING));

             } finally {

                 if (orderStatusPendingId != null) {
                     api.deleteOrderChangeStatus(orderStatusPendingId);
                 }
             }
         });
     }


    @Test(priority = 4)
    @TestRailMethod(
            title = "Test 005 - Create order with effective date set in future",
            summary = "Verify jBilling API can create order with effective date set in future and no order lines are created.",
            refs = "JBQA-14")
    public void test005CreateOrderWithEffectiveDateSetInFuture(){

        final Integer[] userId = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] categoryIds = new Integer[1];
        
        testBuilder.given(envBuilder -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api).withUsername("TestCustomer" + System.currentTimeMillis())
                    .build();
            userId[0] = userWS.getUserId();

            String categoryCode = "Testcategory" + System.currentTimeMillis();
            categoryIds[0] = envBuilder.itemBuilder(api).itemType().withCode(categoryCode).global(true).build();

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createFlatProduct(productCode, categoryIds[0], "3", true, envBuilder, api);

        }).test((env, envBuilder) -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer statusId = getOrCreateOrderChangeStatus(api, ApplyToOrder.YES, Integer.valueOf(1));

            Integer orderId = null;
            try {
                OrderBuilder orderBuilder = envBuilder.orderBuilder(api);
                // Parent Line
                OrderLineWS parentOrderLine = orderBuilder.orderLine()
                        .withItemId(productIds[0])// default quantity 1
                        .build();

                OrderWS orderWS = orderBuilder
                        .forUser(userId[0])
                        .withPeriod(Constants.ORDER_PERIOD_ONCE)
                        .withOrderLine(parentOrderLine)
                        .withCodeForTests("codefortest")
                        .buildOrder();

                OrderChangeWS[] orderChangeWS = new OrderChangeWS[1];
                orderChangeWS[0] = OrderChangeBL.buildFromLine(parentOrderLine, orderWS, statusId);

                orderChangeWS[0].setStartDate(new LocalDate().plusMonths(Integer.valueOf(3)).toDate());
                orderId = api.createOrder(orderWS, orderChangeWS);

                orderWS = api.getOrder(orderId);
                assertNotNull(orderWS);
                assertEquals(orderWS.getOrderLines().length, 0, "Invalid number Order Lines");
                assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("0").setScale(2, BigDecimal.ROUND_CEILING));

            } finally {
                {
                    if (orderId != null) {
                        api.deleteOrder(orderId);
                    }
                }
            }
        });
    }


    @Test(priority = 5,expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*OrderChangeWS,startDate,validation.error.incorrect.start.date.*")
    @TestRailMethod(
            title = "Test 006 - Create order with effective date set in past",
            summary = "Verify jBilling API throws expected exception after creating order with effective date set in past.",
            refs = "JBQA-14")
    public void test006CreateOrderWithEffectiveDateSetInPast(){

        final Integer[] userId = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] categoryIds = new Integer[1];

        testBuilder.given(envBuilder -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api).withUsername("TestCustomer" + System.currentTimeMillis())
                    .build();
            userId[0] = userWS.getUserId();

            String categoryCode = "Testcategory" + System.currentTimeMillis();
            categoryIds[0] = envBuilder.itemBuilder(api).itemType().withCode(categoryCode).global(true).build();

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createFlatProduct(productCode, categoryIds[0], "3", true, envBuilder, api);

        }).test((env, envBuilder) -> {


            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer statusId = getOrCreateOrderChangeStatus(api, ApplyToOrder.YES, Integer.valueOf(1));

            Integer orderId = null;
            try {
                OrderBuilder orderBuilder = envBuilder.orderBuilder(api);
                // Parent Line
                OrderLineWS parentOrderLine = orderBuilder.orderLine()
                        .withItemId(productIds[0])// default quantity 1
                        .build();

                OrderWS orderWS = orderBuilder
                        .forUser(userId[0])
                        .withPeriod(Constants.ORDER_PERIOD_ONCE)
                        .withOrderLine(parentOrderLine)
                        .withCodeForTests("codefortest")
                        .buildOrder();

                OrderChangeWS[] orderChangeWS = new OrderChangeWS[1];
                orderChangeWS[0] = OrderChangeBL.buildFromLine(parentOrderLine, orderWS, statusId);

                orderChangeWS[0].setStartDate(new LocalDate().minusMonths(Integer.valueOf(3)).toDate());
                orderId = api.createOrder(orderWS, orderChangeWS);


            } finally {
                {
                    if (orderId != null) {
                        api.deleteOrder(orderId);
                    }
                }
            }
        });
    }

    @Test(priority = 6)
    @TestRailMethod(
            title = "Test 007 - Plan creation with pending status",
            summary = "Verify jBilling API can create order with plan and pending status and no order lines are created.",
            refs = "JBQA-14")
    public void test007PlanCreationWithPendingStatus(){

        final Integer[] userId = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] categoryIds = new Integer[1];
        final Integer[] planIds = new Integer[1];

        testBuilder.given(envBuilder -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer" + System.currentTimeMillis())
                    .build();
            userId[0] = userWS.getUserId();

            String categoryCode = "Testcategory" + System.currentTimeMillis();
            categoryIds[0] = envBuilder.itemBuilder(api).itemType().withCode(categoryCode).global(true).build();

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createFlatProduct(productCode, categoryIds[0], "3", true, envBuilder, api);

            String subscriptionItemTestCode = "Test Subscription Item" + System.currentTimeMillis();

            Integer subscriptionItemId = createItem(envBuilder, subscriptionItemTestCode,
                    categoryIds[0], "1", api);

            String planTestCode = "Plan Test Code" + System.currentTimeMillis();
            PlanWS planWS = envBuilder.planBuilder(api, planTestCode)
                    .withDescription(planTestCode)
                    .withItemId(subscriptionItemId)
                    .withPeriodId(Constants.ORDER_PERIOD_ONCE)
                    .addPlanItem(
                            PlanBuilder.PlanItemBuilder.getBuilder()
                                    .withItemId(productIds[0])
                                    .withBundledQuantity("1")
                                    .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                    .build()
                    )
                    .build();
            planIds[0] = planWS.getId();

        }).test((env, envBuilder) -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer orderStatusPendingId = getOrCreateOrderChangeStatus(api, ApplyToOrder.NO, Integer.valueOf(2));

            try {
                OrderBuilder orderBuilder = envBuilder.orderBuilder(api);
                // Parent Line

                PlanWS planWS = api.getPlanWS(planIds[0]);


                OrderLineWS parentOrderLine = orderBuilder.orderLine()
                        .withItemId(planWS.getPlanSubscriptionItemId())// default quantity 1
                        .build();

                Integer orderId = orderBuilder
                        .forUser(userId[0])
                        .withPeriod(Constants.ORDER_PERIOD_ONCE)
                        .withOrderLine(parentOrderLine)
                        .withCodeForTests("codefortest")
                        .withOrderChangeStatus(orderStatusPendingId)
                        .build();

                OrderWS orderWS = api.getOrder(orderId);
                assertNotNull(orderWS);
                assertEquals(orderWS.getOrderLines().length, 0, "Invalid number Order Lines");
                assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("0").setScale(2, BigDecimal.ROUND_CEILING));

            } finally {

                if (orderStatusPendingId != null) {
                    api.deleteOrderChangeStatus(orderStatusPendingId);
                }
            }


        });
    }



    @Test(priority = 7)
    @TestRailMethod(
            title = "Test 008 - Plan creation with apply status",
            summary = "Verify jBilling API can create order with plan and apply status and order line is created.",
            refs = "JBQA-14")
    public void test008PlanCreationWithApplyStatus(){

        final Integer[] userId = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] categoryIds = new Integer[1];
        final Integer[] planIds = new Integer[1];

        testBuilder.given(envBuilder -> {


            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer" + System.currentTimeMillis())
                    .build();
            userId[0] = userWS.getUserId();

            String categoryCode = "Testcategory" + System.currentTimeMillis();
            categoryIds[0] = envBuilder.itemBuilder(api).itemType().withCode(categoryCode).global(true).build();

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createFlatProduct(productCode, categoryIds[0], "3", true, envBuilder, api);

            String subscriptionItemTestCode = "Test Subscription Item" + System.currentTimeMillis();

            Integer subscriptionItemId = createItem(envBuilder, subscriptionItemTestCode,
                    categoryIds[0], "1", api);

            PriceModelWS priceModelWS = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("1"),
                    api.getCallerCurrencyId());

            String planTestCode = "Plan Test Code" + System.currentTimeMillis();
            PlanWS planWS = envBuilder.planBuilder(api, planTestCode)
                    .withDescription(planTestCode)
                    .withItemId(subscriptionItemId)
                    .withPeriodId(Constants.ORDER_PERIOD_ONCE)
                    .addPlanItem(
                            PlanBuilder.PlanItemBuilder.getBuilder()
                                    .withItemId(productIds[0])
                                    .withBundledQuantity("1")
                                    .withModel(priceModelWS)
                                    .addModel(new Date(), priceModelWS)
                                    .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                    .build()
                    )
                    .build();
            planIds[0] = planWS.getId();

        }).test((env, envBuilder) -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer orderStatusApllyId = getOrCreateOrderChangeStatus(api, ApplyToOrder.YES, Integer.valueOf(1));

            try {
                OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

                PlanWS planWS = api.getPlanWS(planIds[0]);
                // Parent Line
                OrderLineWS parentOrderLine = orderBuilder.orderLine()
                        .withItemId(planWS.getPlanSubscriptionItemId())// default quantity 1
                        .build();

                Integer orderId = orderBuilder
                        .forUser(userId[0])
                        .withPeriod(Constants.ORDER_PERIOD_ONCE)
                        .withOrderLine(parentOrderLine)
                        .withOrderChangeStatus(orderStatusApllyId)
                        .withCodeForTests("codefortests")
                        .build();

                OrderWS orderWS = api.getOrder(orderId);
                assertNotNull(orderWS);
                assertEquals(orderWS.getOrderLines().length, 1, "Invalid number Order Lines");
                assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("1").setScale(2, BigDecimal.ROUND_CEILING));

            } finally {

                if (orderStatusApllyId != null) {
                    api.deleteOrderChangeStatus(orderStatusApllyId);
                }
            }
        });
    }



    @Test(priority = 8)
    @TestRailMethod(
            title = "Test 009 - Plan creation with effective date in future",
            summary = "Verify jBilling API can create order with plan with future effective date and no order lines are created.",
            refs = "JBQA-14")
    public void test009PlanCreationWithEffectiveDateInFuture(){

        final Integer[] userId = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] categoryIds = new Integer[1];
        final Integer[] planIds = new Integer[1];

        testBuilder.given(envBuilder -> {


            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer" + System.currentTimeMillis())
                    .build();
            userId[0] = userWS.getUserId();

            String categoryCode = "Testcategory" + System.currentTimeMillis();
            categoryIds[0] = envBuilder.itemBuilder(api).itemType().withCode(categoryCode).global(true).build();

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createFlatProduct(productCode, categoryIds[0], "3", true, envBuilder, api);

            String subscriptionItemTestCode = "Test Subscription Item" + System.currentTimeMillis();

            Integer subscriptionItemId = createItem(envBuilder, subscriptionItemTestCode,
                    categoryIds[0], "1", api);

            PriceModelWS priceModelWS = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("1"),
                    api.getCallerCurrencyId());

            String planTestCode = "Plan Test Code" + System.currentTimeMillis();
            PlanWS planWS = envBuilder.planBuilder(api, planTestCode)
                    .withDescription(planTestCode)
                    .withItemId(subscriptionItemId)
                    .withPeriodId(Constants.ORDER_PERIOD_ONCE)
                    .addPlanItem(
                            PlanBuilder.PlanItemBuilder.getBuilder()
                                    .withItemId(productIds[0])
                                    .withBundledQuantity("1")
                                    .withModel(priceModelWS)
                                    .addModel(new Date(), priceModelWS)
                                    .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                    .build()
                    )
                    .build();
            planIds[0] = planWS.getId();

        }).test((env, envBuilder) -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer orderChangeStatusId = getOrCreateOrderChangeStatus(api, ApplyToOrder.YES, Integer.valueOf(1));

            Integer orderId = null;

            try {
                OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

                PlanWS planWS = api.getPlanWS(planIds[0]);
                // Parent Line
                OrderLineWS parentOrderLine = orderBuilder.orderLine()
                        .withItemId(planWS.getPlanSubscriptionItemId())// default quantity 1
                        .build();

                OrderWS orderWS = orderBuilder
                        .forUser(userId[0])
                        .withPeriod(Constants.ORDER_PERIOD_ONCE)
                        .withOrderLine(parentOrderLine)
                        .withCodeForTests("codefortests")
                        .buildOrder();

                OrderChangeWS[] orderChangeWS = new OrderChangeWS[1];
                orderChangeWS[0] = OrderChangeBL.buildFromLine(parentOrderLine, orderWS, orderChangeStatusId);

                orderChangeWS[0].setStartDate(new LocalDate().plusMonths(Integer.valueOf(3)).toDate());
                orderId = api.createOrder(orderWS, orderChangeWS);

                orderWS = api.getOrder(orderId);

                orderWS = api.getOrder(orderId);
                assertNotNull(orderWS);
                assertEquals(orderWS.getOrderLines().length, 0, "Invalid number Order Lines");
                assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("0").setScale(2, BigDecimal.ROUND_CEILING));

            } finally {

                if (orderId != null) {
                    api.deleteOrder(orderId);
                }
                if (orderChangeStatusId != null) {
                    api.deleteOrderChangeStatus(orderChangeStatusId);
                }

            }
        });
    }


    @Test(priority = 9,expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = "(?s).*OrderChangeWS,startDate,validation.error.incorrect.start.date.*")
    @TestRailMethod(
            title = "Test 010 - Plan creation with effective date in past",
            summary = "Verify jBilling API throws expected exception after creating order with plan with effective date set in past.",
            refs = "JBQA-14")
    public void test010PlanCreationWithEffectiveDateInPast(){

        final Integer[] userId = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] categoryIds = new Integer[1];
        final Integer[] planIds = new Integer[1];

        testBuilder.given(envBuilder -> {


            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer" + System.currentTimeMillis())
                    .build();
            userId[0] = userWS.getUserId();

            String categoryCode = "Testcategory" + System.currentTimeMillis();
            categoryIds[0] = envBuilder.itemBuilder(api).itemType().withCode(categoryCode).global(true).build();

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createFlatProduct(productCode, categoryIds[0], "3", true, envBuilder, api);

            String subscriptionItemTestCode = "Test Subscription Item" + System.currentTimeMillis();

            Integer subscriptionItemId = createItem(envBuilder, subscriptionItemTestCode,
                    categoryIds[0], "1", api);

            PriceModelWS priceModelWS = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("1"),
                    api.getCallerCurrencyId());

            String planTestCode = "Plan Test Code" + System.currentTimeMillis();
            PlanWS planWS = envBuilder.planBuilder(api, planTestCode)
                    .withDescription(planTestCode)
                    .withItemId(subscriptionItemId)
                    .withPeriodId(Constants.ORDER_PERIOD_ONCE)
                    .addPlanItem(
                            PlanBuilder.PlanItemBuilder.getBuilder()
                                    .withItemId(productIds[0])
                                    .withBundledQuantity("1")
                                    .withModel(priceModelWS)
                                    .addModel(new Date(), priceModelWS)
                                    .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                    .build()
                    )
                    .build();
            planIds[0] = planWS.getId();

        }).test((env, envBuilder) -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer orderChangeStatusId = getOrCreateOrderChangeStatus(api, ApplyToOrder.YES, Integer.valueOf(1));

            Integer orderId = null;

            try {
                OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

                PlanWS planWS = api.getPlanWS(planIds[0]);
                // Parent Line
                OrderLineWS parentOrderLine = orderBuilder.orderLine()
                        .withItemId(planWS.getPlanSubscriptionItemId())// default quantity 1
                        .build();

                OrderWS orderWS = orderBuilder
                        .forUser(userId[0])
                        .withPeriod(Constants.ORDER_PERIOD_ONCE)
                        .withOrderLine(parentOrderLine)
                        .withCodeForTests("codefortests")
                        .buildOrder();

                OrderChangeWS[] orderChangeWS = new OrderChangeWS[1];
                orderChangeWS[0] = OrderChangeBL.buildFromLine(parentOrderLine, orderWS, orderChangeStatusId);

                orderChangeWS[0].setStartDate(new LocalDate().minusMonths(Integer.valueOf(3)).toDate());
                orderId = api.createOrder(orderWS, orderChangeWS);

            } finally {

                if (orderChangeStatusId != null) {
                    api.deleteOrderChangeStatus(orderChangeStatusId);
                }

            }
        });
    }


    @Test(priority = 10)
    @TestRailMethod(
            title = "Test 011 - Order with cancellation fee",
            summary = "Verify jBilling API can create order with cancellation fee product.",
            refs = "JBQA-14")
    public void test011OrderWithCancelationFee(){

        final Integer[] userIds = new Integer[1];
        final Integer[] productIds = new Integer[3];
        final Integer[] orderIds = new Integer[1];
        final String plugin = "com.sapienter.jbilling.server.order.task.OrderCancellationTask";

        TestBuilder.newTest().given(envBuilder -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String categoryCode = "Test Category" + System.currentTimeMillis();
            Integer categoryId = createCategory(categoryCode, true, Integer.valueOf(0), envBuilder, api);

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());

            String feeProductCode = "FeeProduct" + System.currentTimeMillis();
            productIds[1] = createProduct(productCode, categoryId, "1", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());


            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();

            ConfigurationBuilder configurationBuilder = envBuilder.configurationBuilder(api);

            Hashtable<String, String> pluginMetafields = new Hashtable<String, String>();
            pluginMetafields.put("fee_item_id", productIds[1].toString());
            configurationBuilder.addPluginWithParameters(plugin, pluginMetafields)
                    .build();

        }).test((env, envBuilder) -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();

            try {
                OrderBuilder orderBuilder = envBuilder.orderBuilder(api);
                // Parent Line
                OrderLineWS parentOrderLine = orderBuilder.orderLine()
                        .withItemId(productIds[0])// default quantity 1
                        .build();

                OrderWS orderWS = orderBuilder
                        .forUser(userIds[0])
                        .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                        .withOrderLine(parentOrderLine)
                        .withActiveSince(new LocalDate(2010, 1, 1).toDate())
                        .withActiveUntil(new LocalDate(2010, 7, 1).toDate())
                        .withCodeForTests("codefortest")
                        .withCancelationMinimumPeriod(Integer.valueOf(6))
                        .withCancellationFee(Integer.valueOf(10))
                        .withCancellationFeeType(PriceModelStrategy.FLAT.name())
                        .buildOrder();

                OrderChangeWS orderChangeWS = OrderChangeBL.buildFromLine(parentOrderLine, orderWS,
                        environmentHelper.getOrderChangeStatusApply(api));
                orderChangeWS.setStartDate(new LocalDate(2010, 1, 1).toDate());

                orderIds[0] = api.createOrder(orderWS, new OrderChangeWS[]{orderChangeWS});


                orderWS = api.getOrder(orderIds[0]);
                assertNotNull(orderWS);
                assertEquals(orderWS.getOrderLines().length, 1, "Invalid number Order Lines");
                assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("12").setScale(2, BigDecimal.ROUND_CEILING));

                orderWS.setActiveUntil(new LocalDate(2010, 3, 1).toDate());
                api.updateOrder(orderWS, null);

                orderWS = api.getLatestOrder(userIds[0]);
                assertNotNull(orderWS);
                productIds[2] = orderWS.getId();
                assertEquals(orderWS.getOrderLines().length, 1, "Invalid number Order Lines");
                OrderLineWS orderLineWS = orderWS.getOrderLines()[0];
                assertNotNull(orderLineWS);
                assertEquals(orderLineWS.getItemId(), productIds[1], "Invalid Item");
                assertEquals(orderLineWS.getQuantityAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("1").setScale(2, BigDecimal.ROUND_CEILING), "Invalid quantity");
                assertEquals(orderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                        new BigDecimal("10").setScale(2, BigDecimal.ROUND_CEILING));

            } finally {

                if (productIds[2] != null) {
                    api.deleteOrder(productIds[2]);
                }
            }
        });
    }

    @Test(priority = 11)
    @TestRailMethod(
            title = "Test 012 - Adding order status",
            summary = "Verify jBilling API can update order status",
            refs = "JBQA-14")
    public void test012AddingOrderStatus() {

        final Integer[] userIds = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] orderIds = new Integer[1];


        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();


            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();

            String categoryCode = "Test Category" + System.currentTimeMillis();
            Integer categoryId = createCategory(categoryCode, true, Integer.valueOf(0), envBuilder, api);

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());


        }).test((env, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            Integer orderStatusInvoice = api.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, api.getCallerCompanyId());
            assertNotNull(orderStatusInvoice);

            Integer orderStatusNotInvoice = api.getDefaultOrderStatusId(OrderStatusFlag.NOT_INVOICE, api.getCallerCompanyId());
            assertNotNull(orderStatusNotInvoice);

            // Parent Line
            OrderLineWS parentOrderLine = orderBuilder.orderLine()
                    .withItemId(productIds[0])// default quantity 1
                    .build();

            Integer orderId = orderBuilder
                    .forUser(userIds[0])
                    .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests("codefortests")
                    .withOrderStatus(orderStatusInvoice)
                    .build();

            OrderWS orderWS = api.getOrder(orderId);

            assertEquals(orderWS.getStatusId(), orderStatusInvoice, "Invalid Order Status");

            OrderStatusWS orderStatusWS = new OrderStatusWS();
            orderStatusWS.setId(orderStatusNotInvoice);
            orderStatusWS.setEntity(api.getCompany());
            orderStatusWS.setDescription("Suspended");

            orderWS.setOrderStatusWS(orderStatusWS);

            api.updateOrder(orderWS, null);

            orderWS = api.getOrder(orderId);
            assertEquals(orderWS.getStatusId(), orderStatusNotInvoice, "Invalid Order Status");

        });


    }
        @Test(priority = 12, enabled = false)
        @TestRailMethod(
                title = "Test 013 - Adding order status",
                summary = "",
                refs = "JBQA-14")
        public void test013AddingOrderStatus(){

            TestBuilder.newTest().given(envBuilder -> {
                JbillingAPI api = envBuilder.getPrancingPonyApi();


            }).test((env, envBuilder) -> {
                JbillingAPI api = envBuilder.getPrancingPonyApi();
                assertNotNull(api.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, api.getCallerCompanyId()));
                assertNotNull(api.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, api.getCallerCompanyId()));
                assertNotNull(api.getDefaultOrderStatusId(OrderStatusFlag.NOT_INVOICE, api.getCallerCompanyId()));
                assertNotNull(api.getDefaultOrderStatusId(OrderStatusFlag.SUSPENDED_AGEING, api.getCallerCompanyId()));

                OrderStatusWS orderStatusWS = new OrderStatusWS();
                orderStatusWS.setDescription("Active");
                orderStatusWS.setEntity(api.getCompany());
                orderStatusWS.setOrderStatusFlag(OrderStatusFlag.INVOICE);

                Integer orderStatusId = api.createUpdateOrderStatus(orderStatusWS);

                //TODO

                logger.debug("orderStatusId = {}", orderStatusId);

                api.deleteOrderStatus(orderStatusWS);

            });

}
    @Test(priority = 13)
    @TestRailMethod(
            title = "Test 014 - Discount on order",
            summary = "Verify jBilling API can add discount to an order and discount is created as a child order correctly.",
            refs = "JBQA-14")
    public void test014DiscountOnOrder(){
        final Integer[] userIds = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] orderIds = new Integer[1];


        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();


            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();

            String categoryCode = "Test Category" + System.currentTimeMillis();
            Integer categoryId = createCategory(categoryCode, true, Integer.valueOf(0), envBuilder, api);

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());


        }).test((env, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String code = "TD" + System.currentTimeMillis();
            Integer discountId = envBuilder.discountBuilder(api)
                    .withCodeForTests(code)
                    .withRate("10")
                    .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                    .withDescription("t" + System.currentTimeMillis())
                    .build();

            DiscountLineWS discountLineWS = envBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription("TestDescription" + System.currentTimeMillis())
                    .build();

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderLineWS parentOrderLine = orderBuilder.orderLine()
                    .withItemId(productIds[0])// default quantity 1
                    .build();

            orderIds[0] = orderBuilder
                    .forUser(userIds[0])
                    .withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests("codefortests")
                    .withDiscountLine(discountLineWS)
                    .build();

            assertNotNull(orderIds[0]);
            OrderWS orderWS = api.getOrder(orderIds[0]);
            assertNotNull(orderWS);
            assertEquals(orderWS.getChildOrders().length, 1, "Invalid number of child orders");
            OrderWS[] orderWSes = orderWS.getChildOrders();
            OrderWS childOrderWS = orderWSes[0];
            assertNotNull(childOrderWS);
            assertEquals(childOrderWS.getOrderLines().length, 1, "Invalid number order lines");
            assertEquals(childOrderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("-10").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Total");
        });
    }


    @Test(priority = 14)
    @TestRailMethod(
            title = "Test 015 - Discount on product in order",
            summary = "Verify jBilling API can add discount to a product in an order and discount is created as a child order correctly.",
            refs = "JBQA-14")
    public void test015DiscountOnProductInOrder(){
        final Integer[] userIds = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] orderIds = new Integer[1];

        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();

            String categoryCode = "Test Category" + System.currentTimeMillis();
            Integer categoryId = createCategory(categoryCode, true, Integer.valueOf(0), envBuilder, api);

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());


        }).test((env, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String code = "TD" + System.currentTimeMillis();
            Integer discountId = envBuilder.discountBuilder(api)
                    .withCodeForTests(code)
                    .withRate("10")
                    .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                    .withDescription("t" + System.currentTimeMillis())
                    .build();

            DiscountLineWS discountLineWS = envBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription("TestDescription" + System.currentTimeMillis())
                    .withItemId(productIds[0])
                    .build();

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderLineWS parentOrderLine = orderBuilder.orderLine()
                    .withItemId(productIds[0])// default quantity 1
                    .build();

            orderIds[0] = orderBuilder
                    .forUser(userIds[0])
                    .withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests("codefortests")
                    .withDiscountLine(discountLineWS)
                    .build();

            assertNotNull(orderIds[0]);
            OrderWS orderWS = api.getOrder(orderIds[0]);
            assertNotNull(orderWS);
            assertEquals(orderWS.getChildOrders().length, 1, "Invalid number of child orders");
            OrderWS[] orderWSes = orderWS.getChildOrders();
            OrderWS childOrderWS = orderWSes[0];
            assertNotNull(childOrderWS);
            assertEquals(childOrderWS.getOrderLines().length, 1, "Invalid number order lines");
            assertEquals(childOrderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("-10").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Total");
        });
    }

    @Test(priority = 15)
    @TestRailMethod(
            title = "Test 016 - Metafields in order",
            summary = "Verify jBilling API can create metafields in orderlines correctly.",
            refs = "JBQA-14")
    public void test016MetafieldsInOrder(){

        final String META_FIELD_NAME = "Preferred Time";
        final String META_FIELD_NAME_2 = "Contact person";
        final Integer[] categoryIds = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] userIds = new Integer[1];


        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            UserWS userWS = envBuilder.customerBuilder(api).withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();

            MetaFieldWS metaFieldWS1 = ApiBuilderHelper.getMetaFieldWS(META_FIELD_NAME, DataType.STRING,
                    EntityType.ORDER_LINE, api.getCallerCompanyId());
            metaFieldWS1.setMandatory(true);

            MetaFieldWS metaFieldWS2 = ApiBuilderHelper.getMetaFieldWS(META_FIELD_NAME_2, DataType.STRING,
                    EntityType.ORDER_LINE, api.getCallerCompanyId());
            metaFieldWS2.setMandatory(false);

            String categoryCode = "Testcategory" + System.currentTimeMillis();
            categoryIds[0] = envBuilder.itemBuilder(api).itemType().withCode(categoryCode).global(true).build();

            String code = "Product" + System.currentTimeMillis();
            productIds[0] = createFlatProduct(code, categoryIds[0], "10", true, envBuilder, api, metaFieldWS1, metaFieldWS2);

        }).test((env, envBuilder) -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            String orderCode = "TO" + System.currentTimeMillis();


            OrderLineWS orderLineWS = envBuilder.orderBuilder(api).orderLine()
                    .withItemId(productIds[0])
                    .withQuantity(new BigDecimal(Integer.valueOf(1)))
                    .withMetaField(ApiBuilderHelper.getMetaFieldValueWS(META_FIELD_NAME, "20-20"))
                    .withMetaField(ApiBuilderHelper.getMetaFieldValueWS(META_FIELD_NAME_2, "TestPerson"))
                    .build();

            Integer orderId = envBuilder.orderBuilder(api).withCodeForTests(orderCode).forUser(userIds[0])
                    .withActiveSince(new Date()).withPeriod(environmentHelper.getOrderPeriodMonth(api)).withOrderLine(orderLineWS)
                    .build();

            OrderWS orderWS = api.getOrder(orderId);
            assertNotNull(orderWS);
            OrderLineWS[] orderLines = orderWS.getOrderLines();
            assertNotNull(orderLines, "Order lines expected!");
            assertEquals(Integer.valueOf(orderLines.length), Integer.valueOf(1), "Invalid number of order lines!");
            MetaFieldValueWS[] metaFieldValues = orderLines[0].getMetaFields();
            assertNotNull(metaFieldValues, "Meta-fields expected!");
            assertEquals(Integer.valueOf(metaFieldValues.length), Integer.valueOf(2), "Invalid number of meta-fields!");
            Arrays.sort(metaFieldValues, (o1, o2) -> o1.getFieldName().compareTo(o2.getFieldName()));
            validateMetaField(metaFieldValues[0], META_FIELD_NAME_2, DataType.STRING, "TestPerson");
            validateMetaField(metaFieldValues[1], META_FIELD_NAME, DataType.STRING, "20-20");
        });
    }


    private void validateMetaField(MetaFieldValueWS metaFieldValue, String name, DataType dataType, Object value){

        assertNotNull(metaFieldValue, "Meta-Field value can not be null!");
        assertEquals(metaFieldValue.getFieldName(), name, "Invalid meta-field name!");
        assertEquals(metaFieldValue.getMetaField().getDataType(), dataType, "Invalid meta-field data type!");
        if (metaFieldValue.getValue() instanceof BigDecimal){
            assertEquals(((BigDecimal) metaFieldValue.getValue()).setScale(2, RoundingMode.CEILING),
                    value, "Invalid meta-field value!");
        } else
            assertEquals(metaFieldValue.getValue(), value, "Invalid meta-field value!");
    }

    private Integer createItem(TestEnvironmentBuilder testEnvironmentBuilder, String code,
                               Integer categoryId, String price , JbillingAPI api){

        return testEnvironmentBuilder.itemBuilder(api).item()
                .withCode(code)
                .withEntities(api.getCallerCompanyId())
                .withType(categoryId)
                .withFlatPrice(price)
                .build();
    }

    private Integer createGraduatedProduct(String productCode, Integer categoryId, Boolean global, String price,
                                           String includedUnits, TestEnvironmentBuilder testEnvironmentBuilder,
                                           JbillingAPI api){
        return testEnvironmentBuilder.itemBuilder(api)
                .item()
                .withType(categoryId)
                .withCode(productCode)
                .withGraduatedPrice(price, includedUnits)
                .global(global)
                .build();
    }

    private Integer createFlatProduct(String productCode , Integer categoryId, String price, boolean global,
                                      TestEnvironmentBuilder testEnvironmentBuilder, JbillingAPI api, MetaFieldWS... orderLineMetaFields){
        return testEnvironmentBuilder.itemBuilder(api)
                .item()
                .withCode(productCode)
                .withFlatPrice(price)
                .withType(categoryId)
                .withOrderLineMetaFields(Arrays.asList(orderLineMetaFields))
                .global(global)
                .build();
    }

    private Integer createProduct(String productCode,Integer category,String price,Boolean global,
                                  Integer assetmanagment,TestEnvironmentBuilder testEnvironmentBuilder,
                                  JbillingAPI api, Integer... entityId ){

        return testEnvironmentBuilder.itemBuilder(api)
                .item()
                .withCode(productCode)
                .global(global)
                .withType(category)
                .withEntities(entityId)
                .withAssetManagementEnabled(assetmanagment)
                .withFlatPrice(price)
                .build();
    }

    private Integer createCategory(String categoryCode, boolean global, Integer allowAssetManagement,
                                   TestEnvironmentBuilder testEnvironmentBuilder, JbillingAPI api){


        return testEnvironmentBuilder.itemBuilder(api)
                .itemType()
                .global(global)
                .withCode(categoryCode)
                .allowAssetManagement(allowAssetManagement)
                .build();
    }


    private void sortItemDependencyByItemId(ItemDependencyDTOEx[] itemDependencies){

        Arrays.sort(itemDependencies, (o1, o2) -> o1.getDependentId().compareTo(o2.getDependentId()));
    }



    private Integer getOrCreateOrderChangeStatus(JbillingAPI api , ApplyToOrder applyToOrder , Integer order){
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for(OrderChangeStatusWS orderChangeStatus : list){
            if(orderChangeStatus.getApplyToOrder().equals(applyToOrder) &&
                    orderChangeStatus.getEntityId()!=null){
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if(statusId != null){
            return statusId;
        }else{
            OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
            newStatus.setApplyToOrder(applyToOrder);
            newStatus.setDeleted(0);
            newStatus.setEntityId(api.getCallerCompanyId());
            newStatus.setOrder(order);
            newStatus.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status"+order));
            return api.createOrderChangeStatus(newStatus);
        }
    }


}
