package com.sapienter.jbilling.api.automation.plans;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

import static org.testng.Assert.*;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by hristijan on 6/8/16.
 */
@Test(groups = {"api-automation"}, testName = "PlansProductPricingTest")
public class PlansProductPricingTest {

    private EnvironmentHelper envHelper;
    private PlansProductPricingTestHelper plansHelper;
    private TestBuilder testBuilder;

    @BeforeClass
    public void initializeTests () {
        testBuilder = getTestEnvironment();
    }

    @AfterClass
    public void tearDown () {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != envHelper) {
            envHelper = null;
        }
        if (null != testBuilder) {
            testBuilder = null;
        }
    }

    private TestBuilder getTestEnvironment () {
        return TestBuilder.newTest().givenForMultiple(envBuilder -> {
            envHelper = EnvironmentHelper.getInstance(envBuilder.getPrancingPonyApi());
            plansHelper = new PlansProductPricingTestHelper();
        });
    }

    @Test(priority = 0, description = "Create products with dependencies; Create order for products with dependency")
    public void test001AddProductDependency () {
        final Integer[] productIds = new Integer[3];
        final Integer[] categoryIds = new Integer[2];
        initAccountTypeAndCustomer("Dependency Account Type", "Dependencies Customer")
                .given(envBuilder -> {

                    JbillingAPI api = envBuilder.getPrancingPonyApi();

                    categoryIds[0] = plansHelper.buildAndPersistCategory("Fees", true, api, envBuilder);
                    categoryIds[1] = plansHelper.buildAndPersistCategory("Drink Passes", true, api, envBuilder);

                    productIds[0] = plansHelper.buildAndPersistFlatProduct("Lemonade Plan Setup Fee", true, categoryIds[0], new BigDecimal("10"), api, envBuilder);

                    productIds[1] = plansHelper.buildAndPersistProductWithDependency("Lemonade", true, categoryIds[1], productIds[0], 1, 1, new BigDecimal("10"), api, envBuilder);
                    productIds[2] = plansHelper.buildAndPersistProductWithDependency("Coffee", true, categoryIds[1], productIds[1], 0, 0, new BigDecimal("10"), api, envBuilder);

                }).test((env, envBuilder) -> {

            JbillingAPI api = env.getPrancingPonyApi();

            Integer userId = env.idForCode("Dependencies Customer");
            Integer accountTypeId = env.idForCode("Dependency Account Type");

            UserWS user = api.getUserWS(userId);

            OrderLineWS parentOrderLine = envBuilder.orderBuilder(api)
                    .orderLine()
                    .withItemId(productIds[1])
                    .build();

            OrderLineWS childOrderLine = envBuilder.orderBuilder(api)
                    .orderLine()
                    .withItemId(productIds[0])
                    .build();

            childOrderLine.setParentLine(parentOrderLine);

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderWS orderChild = orderBuilder.forUser(userId)
                    .withCodeForTests("Test Order Child")
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withOrderLine(childOrderLine)
                    .withActiveSince(new DateTime().withTimeAtStartOfDay().toDate())
                    .buildOrder();

            OrderWS orderParent = orderBuilder.forUser(userId)
                    .withCodeForTests("Test Order Parent")
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withOrderLine(parentOrderLine)
                    .withActiveSince(new DateTime().withTimeAtStartOfDay().toDate())
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withChildOrder(orderChild)
                    .buildOrder();

            orderChild.setParentOrder(orderParent);

            Integer orderId = orderBuilder.persistOrder(orderParent);
            OrderWS retOrder = api.getOrder(orderId);

            OrderLineWS updateOrderLine = orderBuilder.orderLine().withItemId(productIds[2]).build();

            OrderLineWS[] orderLines = retOrder.getOrderLines();
            OrderLineWS[] newOrderLines = new  OrderLineWS[orderLines.length + 1];
            for (int i = 0; i < orderLines.length; i++) {
                if(retOrder.getOrderLines()[i].getItemId().equals(childOrderLine.getItemId())) {
                    orderLines[i].setDeleted(1);
                }

                newOrderLines[i] = orderLines[i];
            }

            newOrderLines[newOrderLines.length - 1] = updateOrderLine;

            retOrder.setOrderLines(newOrderLines);
            api.createUpdateOrder(retOrder, OrderChangeBL.buildFromOrder(retOrder, envHelper.getOrderChangeStatusApply(api)));

            ItemDTOEx item2 = api.getItem(productIds[1], null, null);
            ItemDTOEx item3 = api.getItem(productIds[2], null, null);

            assertNotNull(item2, "Item should not be null!");
            assertNotNull(item3, "Item should not be null!");

            assertEquals(item2.getDependencies().length, 1, "This item should have one dependency");
            assertEquals(item3.getDependencies().length, 1, "This item should have one dependency");

            assertNotNull(user.getAccountTypeId());
            assertEquals(user.getAccountTypeId(), accountTypeId, "Values should match!");

            assertEquals(retOrder.getChildOrders().length, 1, "There should be one child order!");
            assertEquals(retOrder.getOrderLines().length, 3, "Order should have 3 order lines!");
            assertEquals(retOrder.getChildOrders()[0].getOrderLines().length, 1, "Child order should have one order line!");
            assertEquals(retOrder.getId(), retOrder.getChildOrders()[0].getParentOrder().getId(), "Values should match!");
        });
    }

    @Test(description = "Create products with dependencies; Create order for products with dependency in the current order")
    public void test001OrderCreationDependencyCurrentOrder () {
        final Integer[] categoryIds = new Integer[2];
        final Integer[] productIds = new Integer[3];
        initAccountTypeAndCustomer("TestAccountType", "DependenciesCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            categoryIds[0] = plansHelper.buildAndPersistCategory("Mobile", true, api, envBuilder);
            categoryIds[1] = plansHelper.buildAndPersistCategory("Laptop", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("Nokia", true, categoryIds[0], new BigDecimal("10"), api, envBuilder);
            productIds[2] = plansHelper.buildAndPersistFlatProduct("Dell", true, categoryIds[1], new BigDecimal("10"), api, envBuilder);
            productIds[1] = createProductWithMultipleDependencies("Accer", true, categoryIds[1], 1, 1, new BigDecimal("10"), api, envBuilder, productIds[0], productIds[2]);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("DependenciesCustomer");

            OrderLineWS parentOrderLine = envBuilder.orderBuilder(api).orderLine().withItemId(productIds[1]).build();

            Integer orderId = envBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withOrderLine(parentOrderLine)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .withParentLine(parentOrderLine)
                                           .build())
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[2])
                                           .withParentLine(parentOrderLine)
                                           .build())
                    .withCodeForTests("Current Order Dependency")
                    .build();

            OrderWS orderWS = api.getOrder(orderId);
            ItemDTOEx dependencyProduct = api.getItem(productIds[1], userId, null);

            assertNotNull(orderWS, "There should be one order");
            assertEquals(orderWS.getOrderLines().length, 3, "There should be 3 order lines");

            assertTrue(dependencyProduct.getDependencies().length == 2);
            assertEquals(dependencyProduct.getDependencies().length, 2, "This product should have 2 dependencies!");

            api.deleteOrder(orderWS.getId());
        });
    }

    @Test(description = "Create product with multiple dependencies and creating order using that product.")
    public void test001OrderCreationMultipleDependencies () {
        final Integer[] categoryIds = new Integer[2];
        final Integer[] productIds = new Integer[3];
        initAccountTypeAndCustomer("TestAccountType", "DependenciesCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            categoryIds[0] = plansHelper.buildAndPersistCategory("Mobile", true, api, envBuilder);
            categoryIds[1] = plansHelper.buildAndPersistCategory("Laptop", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("Nokia", true, categoryIds[0], new BigDecimal("10"), api, envBuilder);
            productIds[2] = plansHelper.buildAndPersistFlatProduct("Dell", true, categoryIds[1], new BigDecimal("10"), api, envBuilder);

            productIds[1] = createProductWithMultipleDependencies("Accer", true, categoryIds[1], 1, 1, new BigDecimal("10"), api, envBuilder, productIds[0], productIds[2]);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("DependenciesCustomer");

            OrderLineWS parentOrderLine = envBuilder.orderBuilder(api).orderLine().withItemId(productIds[1]).build();

            OrderLineWS childOrderLine = envBuilder.orderBuilder(api)
                    .orderLine()
                    .withItemId(productIds[0])
                    .build();

            OrderLineWS childLine = envBuilder.orderBuilder(api)
                    .orderLine()
                    .withItemId(productIds[2])
                    .build();

            childOrderLine.setParentLine(parentOrderLine);
            childLine.setParentLine(parentOrderLine);

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderWS orderChild = orderBuilder.forUser(userId)
                    .withCodeForTests("Test Order Child")
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withOrderLine(childOrderLine)
                    .withActiveSince(new DateTime().withTimeAtStartOfDay().toDate())
                    .buildOrder();

            OrderWS orderParent = orderBuilder.forUser(userId)
                    .withCodeForTests("Test Order Parent")
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withActiveSince(new DateTime().withTimeAtStartOfDay().toDate())
                    .withOrderLine(parentOrderLine)
                    .withOrderLine(childLine)
                    .withChildOrder(orderChild)
                    .buildOrder();

            orderChild.setParentOrder(orderParent);
            Integer orderId = orderBuilder.persistOrder(orderParent);

            OrderWS retOrder = api.getOrder(orderId);

            OrderLineWS[] orderLines = retOrder.getOrderLines();
            OrderLineWS[] newOrderLines = new OrderLineWS[orderLines.length];

            for (int i = 0; i < orderLines.length; i++) {
                if(childOrderLine.getItemId().equals(retOrder.getOrderLines()[i].getItemId())) {
                    orderLines[i].setDeleted(1);
                }
                newOrderLines[i] = orderLines[i];
            }

            retOrder.setOrderLines(newOrderLines);

            orderId = api.createUpdateOrder(retOrder, OrderChangeBL.buildFromOrder(retOrder, envHelper.getOrderChangeStatusApply(api)));

            retOrder = api.getOrder(orderId);

            ItemDTOEx dependencyProduct = api.getItem(productIds[1], userId, null);

            assertEquals(dependencyProduct.getDependencies().length, 2, "This product should have two dependencies");

            assertNotNull(retOrder, "There should be one order");
            assertEquals(retOrder.getChildOrders().length, 1, "Order should have one child order!");
            assertEquals(retOrder.getChildOrders()[0].getOrderLines().length, 1, "Child order should have one order line!");
            assertEquals(retOrder.getOrderLines().length, 2, "There should be 3 order lines");
            assertTrue(retOrder.getId().equals(retOrder.getChildOrders()[0].getParentOrder().getId()));
        });
    }

    @Test(expectedExceptions = SessionInternalError.class,
            description = "Failed to create order with product that have multiple dependencies.")
    public void test001OrderCreationMultipleDependenciesFailed () {
        final Integer[] categoryIds = new Integer[2];
        final Integer[] productIds = new Integer[3];
        initAccountTypeAndCustomer("TestAccountType", "DependenciesCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            categoryIds[0] = plansHelper.buildAndPersistCategory("Mobile", true, api, envBuilder);
            categoryIds[1] = plansHelper.buildAndPersistCategory("Laptop", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("Nokia", true, categoryIds[0], new BigDecimal("10"), api, envBuilder);

            productIds[2] = plansHelper.buildAndPersistFlatProduct("Dell", true, categoryIds[1], new BigDecimal("10"), api, envBuilder);
            productIds[1] = createProductWithMultipleDependencies("Accer", true, categoryIds[1], 1, 1, new BigDecimal("10"), api, envBuilder, productIds[0], productIds[2]);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("DependenciesCustomer");

            OrderLineWS parentOrderLine = envBuilder.orderBuilder(api).orderLine().withItemId(productIds[1]).build();

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderWS orderChild = orderBuilder.forUser(userId)
                    .withCodeForTests("Test Order Child")
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .withParentLine(parentOrderLine)
                                           .build())
                    .buildOrder();

            orderBuilder.forUser(userId)
                    .withCodeForTests("Test Order Parent")
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withOrderLine(parentOrderLine)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[2])
                                           .withParentLine(parentOrderLine)
                                           .build())
                    .withChildOrder(orderChild)
                    .build();
        });
    }

    @Test
    public void test004PlanWithValidPeriod () {
        final Integer[] productIds = new Integer[2];
        initAccountTypeAndCustomer("TestAccountType", "ValidityCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer category = plansHelper.buildAndPersistCategory("New Category", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProductWithDate("VP-01", true, category, new BigDecimal("500"),
                                                                           new LocalDate(2000, 3, 1).toDate(), new LocalDate(2000, 3, 31).toDate(), api, envBuilder);

            productIds[1] = plansHelper.buildAndPersistFlatProductWithDate("VP-02", true, category, new BigDecimal("400"),
                                                                           new LocalDate(2000, 3, 15).toDate(), new LocalDate(2000, 3, 20).toDate(), api, envBuilder);

            envBuilder.planBuilder(api, "TestPlan")
                    .withItemId(productIds[1])
                    .withDescription("Validity Plan")
                    .withPeriodId(envHelper.getOrderPeriodMonth(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[0])
                                         .withBundledQuantity("1")
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("400"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("400"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .build();

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("ValidityCustomer");

            OrderWS order = envBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withCodeForTests("Test Order Parent")
                    .withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withActiveSince(new LocalDate(2000, 3, 15).toDate())
                    .withActiveUntil(new LocalDate(2000, 3, 20).toDate())
                    .withOrderLine(envBuilder.orderBuilder(api).orderLine().withItemId(productIds[0]).build())
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[1])
                                           .build())
                    .buildOrder();

            OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, envHelper.getOrderChangeStatusApply(api));

            for (OrderChangeWS orderChange : orderChanges) {
                orderChange.setStartDate(new LocalDate(2000, 3, 15).toDate());
            }

            api.createOrder(order, orderChanges);

            assertNotNull(order);
            assertEquals(order.getOrderLines().length, 2, "There should be two order lines!");
            assertEquals(order.getActiveSince(), orderChanges[0].getStartDate(), "Effective date should be in range of active since and active until date");
            assertEquals(order.getActiveSince(), orderChanges[1].getStartDate(), "Effective date should be in range of active since and active until date");
        });
    }

    @Test(expectedExceptions = SessionInternalError.class)
    public void test004PlanWithInvalidPeriod () {
        final Integer[] productIds = new Integer[2];
        initAccountTypeAndCustomer("TestAccountType", "ValidityCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer category = plansHelper.buildAndPersistCategory("New Category", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProductWithDate("VP-01", true, category, new BigDecimal("500"),
                                                                           new LocalDate(2000, 3, 1).toDate(), new LocalDate(2000, 3, 31).toDate(), api, envBuilder);

            productIds[1] = plansHelper.buildAndPersistFlatProductWithDate("VP-02", true, category, new BigDecimal("400"),
                                                                           new LocalDate(2000, 3, 15).toDate(), new LocalDate(2000, 3, 20).toDate(), api, envBuilder);

            envBuilder.planBuilder(api, "TestPlan")
                    .withItemId(productIds[1])
                    .withDescription("Validity Plan")
                    .withPeriodId(envHelper.getOrderPeriodMonth(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[0])
                                         .withBundledQuantity("1")
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("400"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("400"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .build();

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("ValidityCustomer");

            envBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withCodeForTests("Test Order Parent")
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withActiveSince(new LocalDate(2000, 3, 15).toDate())
                    .withActiveUntil(new LocalDate(2000, 3, 20).toDate())
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .build())
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[1])
                                           .build())
                    .build();
        });
    }

    @Test(description = "Create product with price chain")
    public void test005PercentagePricingStrategy () {
        final Integer[] productIds = new Integer[3];
        initAccountTypeAndCustomer("TestAccountType", "ValidityCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Integer categoryId = plansHelper.buildAndPersistCategory("Test Category", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProductWithChain("Headphones", true, categoryId,
                                                                                BigDecimal.TEN, api, envBuilder);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("ValidityCustomer");

            Integer orderId = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .build())
                    .withCodeForTests("Pricing Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();

            OrderWS order = api.getOrder(orderId);

            OrderLineWS[] orderLineWS = order.getOrderLines();
            orderLineWS[0].setQuantity(new BigDecimal("2"));

            api.updateOrderLine(orderLineWS[0]);
            api.updateOrder(order, null);

            order = api.getOrder(order.getId());

            assertNotNull(orderId);
            assertEquals(order.getOrderLines().length, 1, "There should be one order line!");
            assertEquals(orderLineWS[0].getQuantityAsDecimal(), new BigDecimal("2"), "Order line quantity should be two!");
            assertEquals(order.getTotalAsDecimal()
                                 .setScale(BigDecimal.ROUND_CEILING, 2),
                         new BigDecimal("2").setScale(BigDecimal.ROUND_CEILING, 2), "Order total should be $2.00!");
        });
    }

    @Test(description = "Test pooled pricing strategy")
    public void test005PooledPricingStrategy () {
        final Integer[] productIds = new Integer[3];
        initAccountTypeAndCustomer("TestAccountType", "PooledPricingCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer categoryId = plansHelper.buildAndPersistCategory("Test Category", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("Phone", true, categoryId, new BigDecimal("20"), api, envBuilder);
            productIds[1] = plansHelper.buildAndPersistPooledProduct("Phone Case", true, categoryId, new BigDecimal("10"), productIds[0], 1, api, envBuilder);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("PooledPricingCustomer");
            Integer orderId = null;

            OrderWS order = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .build())
                    .withCodeForTests("Pooled Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withActiveSince(new LocalDate(2000, 1, 2).toDate())
                    .buildOrder();

            OrderChangeWS orderChange = OrderChangeBL.buildFromLine(order.getOrderLines()[0], order, envHelper.getOrderChangeStatusApply(api));
            orderChange.setStartDate(new LocalDate(2000, 1, 2).toDate());

            orderId = api.createOrder(order, new OrderChangeWS[] {orderChange});
            order = api.getOrder(orderId);

            assertNotNull(orderId, "There should be one order!");
            assertEquals(order.getTotalAsDecimal()
                                 .setScale(BigDecimal.ROUND_CEILING, 2), new BigDecimal("20").setScale(BigDecimal.ROUND_CEILING, 2), "Values should match!");

            order = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[1])
                                           .build())
                    .withCodeForTests("Pooled Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .withActiveSince(new LocalDate(2000, 1, 2).toDate())
                    .buildOrder();

            orderChange = OrderChangeBL.buildFromLine(order.getOrderLines()[0], order, envHelper.getOrderChangeStatusApply(api));
            orderChange.setStartDate(new LocalDate(2000, 1, 2).toDate());

            orderId = api.createOrder(order, new OrderChangeWS[] {orderChange});

            order = api.getOrder(orderId);

            assertNotNull(order);
            assertEquals(order.getTotalAsDecimal().setScale(BigDecimal.ROUND_CEILING, 0),
                         new BigDecimal("0").setScale(BigDecimal.ROUND_CEILING, 0), "Order's total amount should be zero!");
        });
    }

    @Test(description = "Test company pooled pricing strategy")
    public void test005CompanyPooledPricingStrategy() {
        final Integer[] productIds = new Integer[4];
        final Integer[] categoryIds = new Integer[2];
        final UserWS[] users = new UserWS[3];
        final PriceModelWS[] priceModelIds = new PriceModelWS[1];
        testBuilder.given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            categoryIds[0] = plansHelper.buildAndPersistCategory("Local Minute Pool", true, api, envBuilder);
            categoryIds[1] = plansHelper.buildAndPersistCategory("NewCategory", true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProductWithCompany("Free Minutes Pool", false, categoryIds[0], api.getCallerCompanyId(), new BigDecimal("0"), api, envBuilder);
            productIds[1] = plansHelper.buildAndPersistCompanyPooledProduct("Local Minute 1", false, categoryIds[0], new BigDecimal("1"), categoryIds[0], 0, api, envBuilder);
            productIds[2] = plansHelper.buildAndPersistCompanyPooledProduct("Local Minute 2", false, categoryIds[0], new BigDecimal("2"), categoryIds[0], 0, api, envBuilder);

            productIds[3] = plansHelper.buildAndPersistFlatProduct("Plan", false, categoryIds[1], new BigDecimal("10"), api, envBuilder);

            AccountTypeWS accountType = envBuilder.accountTypeBuilder(api)
                    .withName("Test Account Type")
                    .withCreditLimit("10000")
                    .withEntityId(1)
                    .build();

            users[0] = envBuilder.customerBuilder(api)
                    .withUsername("ParentCustomer")
                    .withAccountTypeId(accountType.getId())
                    .build();

            users[0].setIsParent(true);

            users[1] = envBuilder.customerBuilder(api)
                    .withUsername("Child1")
                    .withAccountTypeId(accountType.getId())
                    .build();

            users[2] = envBuilder.customerBuilder(api)
                    .withUsername("Child2")
                    .withAccountTypeId(accountType.getId())
                    .build();

            users[1].setParentId(users[0].getId());
            users[2].setParentId(users[0].getId());

            priceModelIds[0] = new PriceModelWS();
            priceModelIds[0].setCurrencyId(api.getCallerCurrencyId());
            priceModelIds[0].setType(PriceModelStrategy.COMPANY_POOLED.name());
            priceModelIds[0].setRate("0");
            priceModelIds[0].addAttribute("pool_item_category_id", String.valueOf(categoryIds[0]));
            priceModelIds[0].addAttribute("included_quantity", "1000");

            api.updateUser(users[0]);
            api.updateUser(users[1]);
            api.updateUser(users[2]);

            envBuilder.planBuilder(api, "Premium Plan - 1000 free minutes")
                    .withItemId(productIds[3])
                    .withDescription("Test Plan")
                    .withPeriodId(envHelper.getOrderPeriodMonth(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[0])
                                         .withBundledQuantity("0")
                                         .withModel(priceModelIds[0])
                                         .addModel(priceModelIds[0])
                                         .withBundledPeriodId(envHelper.getOrderPeriodOneTime(api))
                                         .build())
                    .build();

        }).test((env, envBuilder) -> {

            JbillingAPI api = env.getPrancingPonyApi();

            Integer planId = env.idForCode("Premium Plan - 1000 free minutes");
            Integer orderId = null;
            PlanWS plan = api.getPlanWS(planId);

            orderId = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withQuantity(BigDecimal.ONE)
                                           .withItemId(plan.getItemId())
                                           .build())
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[1])
                                           .withQuantity(new BigDecimal("500"))
                                           .build())
                    .withCodeForTests("Company Pooled Order")
                    .forUser(users[1].getId())
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();

            OrderWS order = api.getOrder(orderId);

            assertNotNull(orderId);
            assertEquals(order.getTotalAsDecimal().setScale(BigDecimal.ROUND_CEILING, 2),
                         BigDecimal.TEN.setScale(BigDecimal.ROUND_CEILING, 2), "This order should have total price of $20");

            Integer orderChildId = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[2])
                                           .withQuantity(new BigDecimal("300"))
                                           .build())
                    .withCodeForTests("Company Pooled Order Child2")
                    .forUser(users[2].getId())
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();

            assertNotNull(orderChildId);

            Integer orderChildId2 = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[1])
                                           .withQuantity(new BigDecimal("500"))
                                           .build())
                    .withCodeForTests("Company Pooled Second Order Child2")
                    .forUser(users[2].getId())
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();

            OrderWS orderChild = api.getOrder(orderChildId);
            OrderWS orderChild2 = api.getOrder(orderChildId2);

            assertNotNull(orderChild);
            assertEquals(orderChild.getTotalAsDecimal()
                                 .setScale(BigDecimal.ROUND_CEILING, 0), BigDecimal.ZERO.setScale(BigDecimal.ROUND_CEILING, 0), "Expected total value should be zero");

            assertNotNull(orderChild2);
            assertEquals(orderChild2.getTotalAsDecimal()
                                 .setScale(BigDecimal.ROUND_CEILING, 2), new BigDecimal("300").setScale(BigDecimal.ROUND_CEILING, 2), "Expected total value should be 300");
        });
    }

    // TODO When we want to update order using one per item category we should use api.createUpdateOrder() method
    @Test(expectedExceptions = SessionInternalError.class,
            description = "Updating order with more than one item from the same category should throw exception because the category is one item per order")
    public void test006OneItemPerCategoryFailedOnOrderLineUpdate () {
        final Integer[] productIds = new Integer[4];
        final Integer[] categoryIds = new Integer[1];
        initAccountTypeAndCustomer("OPOType", "OPC OPO Customer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            categoryIds[0] = plansHelper.buildAndPersistCategoryOnePerOrder("OPO Category", true, true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("VD-01", true, categoryIds[0], new BigDecimal("0"), api, envBuilder);
            productIds[1] = plansHelper.buildAndPersistFlatProduct("VD-02", true, categoryIds[0], new BigDecimal("0"), api, envBuilder);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("OPC OPO Customer");
            Integer orderId = null;

            OrderWS order = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .withQuantity(new BigDecimal("1"))
                                           .build())
                    .withCodeForTests("OPO Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .buildOrder();

            OrderChangeWS orderChange = OrderChangeBL.buildFromLine(order.getOrderLines()[0], order, envHelper.getOrderChangeStatusApply(api));

            orderId = api.createOrder(order, new OrderChangeWS[] {orderChange});

            OrderWS retOrder = api.getOrder(orderId);

            OrderLineWS newOrderLine = envBuilder.orderBuilder(api)
                    .orderLine()
                    .withItemId(productIds[1])
                    .withQuantity(new BigDecimal("1"))
                    .build();

            OrderLineWS[] previousOrderLines = retOrder.getOrderLines();
            OrderLineWS[] newOrderLines = new OrderLineWS[previousOrderLines.length + 1];

            System.arraycopy(previousOrderLines, 0, newOrderLines, 0, newOrderLines.length - 1);

            newOrderLine.setOrderId(orderId);
            newOrderLines[newOrderLines.length - 1] = newOrderLine;

            retOrder.setOrderLines(newOrderLines);
            OrderChangeWS[] orderChangesWS = OrderChangeBL.buildFromOrder(retOrder, envHelper.getOrderChangeStatusApply(api));

            api.createUpdateOrder(retOrder, orderChangesWS);
        });
    }

    @Test(expectedExceptions = SessionInternalError.class,
            description = "Order creation failed on creation with multiple products from same category that is one per item category")
    public void test006OneItemPerCategoryFailedOnCreation () {
        final Integer[] productIds = new Integer[4];
        initAccountTypeAndCustomer("OPOType", "OPC OPO Customer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer categoryId = plansHelper.buildAndPersistCategoryOnePerOrder("OPO Category", true, true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("VD-01", true, categoryId, new BigDecimal("100"), api, envBuilder);
            productIds[1] = plansHelper.buildAndPersistFlatProduct("VD-02", true, categoryId, new BigDecimal("100"), api, envBuilder);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("OPC OPO Customer");

            envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .withQuantity(new BigDecimal("1"))
                                           .build())
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[1])
                                           .withQuantity(new BigDecimal("1"))
                                           .build())
                    .withCodeForTests("OPO Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodOneTime(api))
                    .build();
        });
    }

    @Test(description = "Creating order with product from one per customer category")
    public void test007OrderCreationOnePerCustomerCategory () {
        final Integer[] productIds = new Integer[1];
        initAccountTypeAndCustomer("OPOType", "OPC OPO Customer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer categoryId = plansHelper.buildAndPersistCategoryOnePerCustomer("OPO Category", true, true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("VD-03", true, categoryId, new BigDecimal("100"), api, envBuilder);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("OPC OPO Customer");
            Integer orderId = null;

            orderId = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .build())
                    .withCodeForTests("OPO Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();

            OrderWS order = api.getOrder(orderId);

            assertNotNull(orderId);
            assertEquals(order.getOrderLines().length, 1, "There should be one only item!");
            assertEquals(order.getOrderLines()[0].getQuantityAsDecimal().setScale(BigDecimal.ROUND_CEILING, 2),
                         BigDecimal.ONE.setScale(BigDecimal.ROUND_CEILING, 2), "Order should have order line with quantity one!");
        });
    }

    @Test(expectedExceptions = SessionInternalError.class,
            description = "Creating second order with product from one per customer category")
    public void test007OrderCreationWithProductFromSameCategoryOnePerCustomer () {
        final Integer[] productIds = new Integer[4];
        initAccountTypeAndCustomer("OPOType", "OPC OPO Customer").given(envBuilder -> {

            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer categoryId = plansHelper.buildAndPersistCategoryOnePerCustomer("OPO Category", true, true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("VD-03", true, categoryId, new BigDecimal("100"), api, envBuilder);
            productIds[1] = plansHelper.buildAndPersistFlatProduct("VD-04", true, categoryId, new BigDecimal("100"), api, envBuilder);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("OPC OPO Customer");
            Integer orderId = null;

            orderId = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[0])
                                           .build())
                    .withCodeForTests("OPO Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();

            OrderWS order = api.getOrder(orderId);

            assertNotNull(orderId);
            assertEquals(order.getOrderLines().length, 1, "There should be one only item!");

            orderId = envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productIds[1])
                                           .build())
                    .withCodeForTests("OPO Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();
        });
    }

    @Test(expectedExceptions = SessionInternalError.class,
            description = "Trying to create order using product with quantity higher than one. The product belong to category one per customer")
    public void test007CreationOnePerCustomerCategoryProductOrderFailed () {
        initAccountTypeAndCustomer("OPOType", "OPC OPO Customer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer categoryId = plansHelper.buildAndPersistCategoryOnePerCustomer("OPO Category", true, true, api, envBuilder);

            plansHelper.buildAndPersistFlatProduct("VD-03", true, categoryId, new BigDecimal("100"), api, envBuilder);

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode("OPC OPO Customer");
            Integer productId = env.idForCode("VD-03");

            envBuilder.orderBuilder(api)
                    .withOrderLine(envBuilder.orderBuilder(api)
                                           .orderLine()
                                           .withItemId(productId)
                                           .withQuantity(new BigDecimal("2"))
                                           .build())
                    .withCodeForTests("OPO Order")
                    .forUser(userId)
                    .withPeriod(envHelper.getOrderPeriodMonth(api))
                    .build();
        });
    }

    @Test(description = "Create plan with more than one categories")
    public void test009CreatePlanWithDifferentCategories () {
        final Integer[] productIds = new Integer[2];
        final Integer[] categoryIds = new Integer[2];
        initAccountTypeAndCustomer("OPOType", "OPC OPO Customer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            categoryIds[0] = plansHelper.buildAndPersistCategoryOnePerCustomer("OPC Category", true, true, api, envBuilder);
            categoryIds[1] = plansHelper.buildAndPersistCategoryOnePerOrder("OPO Category", true, true, api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProductMultipleCategories("VD-03", false, categoryIds[0],
                                                                                     categoryIds[1], new BigDecimal("100"), api, envBuilder);

            productIds[1] = plansHelper.buildAndPersistFlatProduct("CP-01", true, categoryIds[0], new BigDecimal("20"), api, envBuilder);

            envBuilder.planBuilder(api, "Plan")
                    .withItemId(productIds[0])
                    .withDescription("Test Plan")
                    .withPeriodId(envHelper.getOrderPeriodMonth(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[1])
                                         .withBundledQuantity("0")
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .build();

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer planId = env.idForCode("Plan");

            PlanWS plan = api.getPlanWS(planId);

            assertNotNull(plan);
        });
    }

    // TODO This test should pass with this setup, but it throws NoSuchElementException
    // on update because we are setting the global flag as 'true' while creating the product
    @Test(description = "Create plan with metafields", enabled = false)
    public void test010CreatePlanWithMetafields () {
        final Integer[] productIds = new Integer[2];
        final Integer[] categoryIds = new Integer[2];
        final List<MetaFieldWS> metaFields = new ArrayList<>();
        initAccountTypeAndCustomer("OPOType", "MetafieldsCustomer").given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            categoryIds[0] = plansHelper.buildAndPersistCategoryOnePerCustomer("OPC Category", true, true, api, envBuilder);
            categoryIds[1] = plansHelper.buildAndPersistCategoryOnePerOrder("OPO Category", true, true, api, envBuilder);

            MetaFieldWS metaFieldWS1 = ApiBuilderHelper.getMetaFieldWS("New Metafield", DataType.STRING, EntityType.ORDER_LINE, api
                    .getCallerCompanyId());

            metaFields.add(metaFieldWS1);

            // If we change this product to be "global = false" this test will pass
            productIds[0] = plansHelper.buildAndPersistFlatProductMultipleCategories("VD-03", true, categoryIds[0], categoryIds[1], new BigDecimal("100"), metaFields, api, envBuilder);

            productIds[1] = plansHelper.buildAndPersistFlatProduct("NP-01", true, categoryIds[0], new BigDecimal("100"), api, envBuilder);

            envBuilder.planBuilder(api, "Plan")
                    .withItemId(productIds[0])
                    .withDescription("Test Plan")
                    .withPeriodId(envHelper.getOrderPeriodMonth(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[1])
                                         .withBundledQuantity("0")
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .build();

        }).test((env, envBuilder) -> {
            JbillingAPI api = env.getPrancingPonyApi();

            Integer userId = env.idForCode("MetafieldsCustomer");
            Integer planId = env.idForCode("Plan");

            PlanWS plan = api.getPlanWS(planId);
            ItemDTOEx itemDTOEx = api.getItem(plan.getItemId(), userId, null);


            assertNotNull(plan, "There should be one plan!");
            assertEquals(itemDTOEx.getOrderLineMetaFields().length, 1, "There should be one meta-field!");

            MetaFieldWS metaFieldWS2 = ApiBuilderHelper.getMetaFieldWS("Extra Meta-field", DataType.STRING, EntityType.ORDER_LINE, api
                    .getCallerCompanyId());

            metaFields.add(metaFieldWS2);

            itemDTOEx.setOrderLineMetaFields(metaFields.toArray(new MetaFieldWS[metaFields.size()]));

            api.updateItem(itemDTOEx);

            assertEquals(itemDTOEx.getOrderLineMetaFields().length, 2, "There should be two meta-fields!");
            assertEquals(itemDTOEx.getEntityId(), api.getCallerCompanyId());
        });
    }

    // TODO Order creation fail when trying to add plan with product that has asset
    @Test(description = "Creating order with multiple plans; Order with plan that contains product with asset")
    public void test012CreatePlanWithDifferentDependenciesAndPrecedenceLevel () {
        initAccountTypeAndCustomer("TestAccountType", "TestCustomerAsset");
        dataPreparationForPlans()
        .test((env, envBuilder) -> {

            Integer msisdn = env.idForCode("MSISDN");
            Integer assetCode = env.idForCode("Asset");
            Integer planRate = env.idForCode("SMS Rate Plan");
            Integer planDiscount = env.idForCode("SMS Discount Plan");
            Integer planTariff = env.idForCode("Basic Tariff Plan");
            Integer planDependency = env.idForCode("Dependency Plan");
            Integer orderId = null;
            Integer userId = env.idForCode("TestCustomerAsset");

            JbillingAPI api = env.getPrancingPonyApi();

            try {

                ItemDTOEx assetManagementEnabledItem = api.getItem(msisdn, userId, null);
                AssetWS asset = api.getAsset(assetCode);

                assertNotNull(asset, "There should be asset!");

                assertNotNull(assetManagementEnabledItem, "There should be one product");
                assertTrue(null != assetManagementEnabledItem.getAssetManagementEnabled() && assetManagementEnabledItem.getAssetManagementEnabled()
                        .equals(1), "This product should have asset management enabled!");

                PlanWS smsPlanRate = api.getPlanWS(planRate);
                validatePlan(smsPlanRate, 3);

                PlanWS smsPlanDiscount = api.getPlanWS(planDiscount);
                validatePlan(smsPlanDiscount, 2);


                PlanWS smsBasicTariff = api.getPlanWS(planTariff);
                validatePlan(smsBasicTariff, 5);

                PlanWS smsPlanDependency = api.getPlanWS(planDependency);
                validatePlan(smsPlanDependency, 1);

                OrderWS order = envBuilder.orderBuilder(api)
                        .withOrderLine(envBuilder.orderBuilder(api)
                                               .orderLine()
                                               .withItemId(smsBasicTariff.getItemId())
                                               .build())
                        .withCodeForTests("Order")
                        .forUser(userId)
                        .withPeriod(envHelper.getOrderPeriodMonth(api))
                        .buildOrder();

                OrderChangeWS orderChange = OrderChangeBL.buildFromLine(order.getOrderLines()[0], order, envHelper.getOrderChangeStatusApply(api));
                OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                orderChangePlanItem.setItemId(msisdn);
                orderChangePlanItem.setAssetIds(new int[] {assetCode});
                orderChangePlanItem.setBundledQuantity(1);
                orderChangePlanItem.setId(0);
                orderChangePlanItem.setOptlock(0);
                orderChangePlanItem.setDescription("Test");
                orderChange.setOrderChangePlanItems(new OrderChangePlanItemWS[] {orderChangePlanItem});

                orderId = api.createUpdateOrder(order, new OrderChangeWS[] {orderChange});

            } finally {
                if (null != orderId) { deleteOrderHierarchy(api, orderId); }
            }
        });
    }

    // TODO Test fails because we use product with asset when we try to create order; Same problem in other tests with asset management product
    @Test (description = "Test scenario in which we create order with plans, run billing process and pay the invoice generated by the billing process run")
    public void test015PaymentSuccessful () {
        initTestDataForPayment("TestAccountType", "TestCustomerAsset");
        dataPreparationForPlans()
                .test((env, envBuilder) -> {

                    Integer msisdn = env.idForCode("MSISDN");
                    Integer assetCode = env.idForCode("Asset");
                    Integer planRate = env.idForCode("SMS Rate Plan");
                    Integer planDiscount = env.idForCode("SMS Discount Plan");
                    Integer planTariff = env.idForCode("Basic Tariff Plan");
                    Integer planDependency = env.idForCode("Dependency Plan");
                    Integer orderId = null;
                    Integer userId = env.idForCode("TestCustomerAsset");

                    InvoiceWS invoice = null;

                    JbillingAPI api = env.getPrancingPonyApi();

                    try {
                        UserWS user = api.getUserWS(userId);

                        ItemDTOEx assetManagementEnabledItem = api.getItem(msisdn, userId, null);
                        AssetWS asset = api.getAsset(assetCode);

                        assertNotNull(asset, "There should be asset!");

                        assertNotNull(assetManagementEnabledItem, "There should be one product");
                        assertTrue(null != assetManagementEnabledItem.getAssetManagementEnabled() && assetManagementEnabledItem.getAssetManagementEnabled()
                                .equals(1), "This product should have asset management enabled!");

                        PlanWS smsPlanRate = api.getPlanWS(planRate);
                        validatePlan(smsPlanRate, 3);

                        PlanWS smsPlanDiscount = api.getPlanWS(planDiscount);
                        validatePlan(smsPlanDiscount, 2);


                        PlanWS smsBasicTariff = api.getPlanWS(planTariff);
                        validatePlan(smsBasicTariff, 5);

                        PlanWS smsPlanDependency = api.getPlanWS(planDependency);
                        validatePlan(smsPlanDependency, 1);


                OrderWS order = envBuilder.orderBuilder(api)
                        .withOrderLine(envBuilder.orderBuilder(api)
                                               .orderLine()
                                               .withItemId(smsBasicTariff.getItemId())
                                               .build())
                        .withCodeForTests("Order")
                        .forUser(userId)
                        .withActiveSince(new LocalDate().toDateTimeAtCurrentTime().toDate())
                        .withPeriod(envHelper.getOrderPeriodMonth(api))
                        .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                        .buildOrder();

                OrderChangeWS orderChange = OrderChangeBL.buildFromLine(order.getOrderLines()[0], order, envHelper.getOrderChangeStatusApply(api));
                OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                orderChangePlanItem.setItemId(msisdn);
                orderChangePlanItem.setAssetIds(new int[] {assetCode});
                orderChangePlanItem.setId(0);
                orderChangePlanItem.setOptlock(0);
                orderChangePlanItem.setDescription("Test");
                orderChangePlanItem.setBundledQuantity(1);

                orderChange.setOrderLineId(order.getOrderLines()[0].getId());
                orderChange.setAssetIds(new Integer[] {assetCode});
                orderChange.setItemId(msisdn);
                orderChange.setStartDate(new LocalDate().toDateTimeAtCurrentTime().toDate());

                orderChange.setOrderChangePlanItems(new OrderChangePlanItemWS[] {orderChangePlanItem});

                orderId = api.createOrder(order, new OrderChangeWS[] {orderChange});

                api.createInvoiceFromOrder(orderId, null);
                invoice = api.getLatestInvoice(userId);
                api.payInvoice(invoice.getId());

                invoice = api.getInvoiceWS(invoice.getId());

                assertNotNull(invoice.getId(), "There should be one invoice!");
                assertTrue(invoice.getPaymentAttempts().equals(1));
                assertTrue(invoice.getOwningUserId()
                                   .equals(user.getId()), "Owning user id on the invoice should match the user id");

            } finally {
                if (null != invoice) { api.deleteInvoice(invoice.getId()); }
                if (null != orderId) { deleteOrderHierarchy(api, orderId); }
            }
        });
    }

    // TODO Test fails on order creation because there is product with asset
    @Test(description = "Test scenario in which we delete plan and try to create order to see the price results"
            , enabled = false)
    public void test016PlanPriceOfProductNotApplicableWhenPlanOrderDeleted () {
        initAccountTypeAndCustomer("TestAccountType", "TestCustomerAsset");
        dataPreparationForPlans()
                .test((env, envBuilder) -> {

                    Integer msisdn = env.idForCode("MSISDN");
                    Integer assetCode = env.idForCode("Asset");
                    Integer planRate = env.idForCode("SMS Rate Plan");
                    Integer planDiscount = env.idForCode("SMS Discount Plan");
                    Integer connectionFee = env.idForCode("Connection Fee");
                    Integer gprsService = env.idForCode("GPRS Service");
                    Integer tariffPlanItem = env.idForCode("Basic Tariff Plan Item");
                    Integer userId = env.idForCode("TestCustomerAsset");

                    Integer orderId = null;

                    JbillingAPI api = env.getPrancingPonyApi();

                    PlanWS smsPlanRate = api.getPlanWS(planRate);
                    validatePlan(smsPlanRate, 3);

                    PlanWS smsPlanDiscount = api.getPlanWS(planDiscount);
                    validatePlan(smsPlanDiscount, 2);

                    PlanItemWS[] planItem = new PlanItemWS[5];

                    planItem[0] = createPlanItem(msisdn, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                            .getCallerCurrencyId()), -1, envHelper.getOrderPeriodMonth(api), "1");
                    planItem[0].addModel(new Date(), new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                            .getCallerCurrencyId()));

                    planItem[1] = createPlanItem(connectionFee, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                            .getCallerCurrencyId()), -1, Constants.ORDER_PERIOD_ONCE, "1");
                    planItem[1].addModel(new Date(), new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                            .getCallerCurrencyId()));

                    planItem[2] = createPlanItem(gprsService, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                            .getCallerCurrencyId()), -1, envHelper.getOrderPeriodMonth(api), "1");
                    planItem[2].addModel(new Date(), new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                            .getCallerCurrencyId()));


                    planItem[3] = createPlanItem(smsPlanRate.getPlanSubscriptionItemId(),
                                                 new PriceModelWS(PriceModelStrategy.FLAT.name(),
                                                                  new BigDecimal("0"), api.getCallerCurrencyId()), -1,
                                                 envHelper.getOrderPeriodMonth(api), "1");
                    planItem[3].addModel(new Date(), new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                            .getCallerCurrencyId()));

                    planItem[4] = createPlanItem(smsPlanDiscount.getPlanSubscriptionItemId(),
                                                 new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("30"),
                                                      api.getCallerCurrencyId()), -1, Constants.ORDER_PERIOD_ONCE, "0");
                    planItem[4].addModel(new Date(), new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("30"),
                                                                                                api.getCallerCurrencyId()));

                    List<PlanItemWS> planItems = new ArrayList<>();

                    Collections.addAll(planItems, planItem);

                    PlanWS smsPlanTariff = createPlan(tariffPlanItem, envHelper.getOrderPeriodMonth(api), "Basic Tariff Plan", planItems);

                    validatePlan(smsPlanTariff, 5);

                    api.deletePlan(smsPlanTariff.getId());

                    OrderWS order = envBuilder.orderBuilder(api)
                            .withOrderLine(envBuilder.orderBuilder(api)
                                                   .orderLine()
                                                   .withItemId(connectionFee)
                                                   .withQuantity(new BigDecimal("2"))
                                                   .build())
                            .withOrderLine(envBuilder.orderBuilder(api)
                                                   .orderLine()
                                                   .withItemId(msisdn)
                                                   .withQuantity(BigDecimal.ONE)
                                                   .withAsset(assetCode)
                                                   .build())
                            .withCodeForTests("Order")
                            .withActiveSince(new LocalDate().toDateTimeAtCurrentTime().toDate())
                            .forUser(userId)
                            .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                            .withPeriod(envHelper.getOrderPeriodMonth(api))
                            .buildOrder();

                    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(order.getOrderLines()[1], order, envHelper.getOrderChangeStatusApply(api));
                    OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                    orderChangePlanItem.setItemId(msisdn);
                    orderChangePlanItem.setAssetIds(new int[] {assetCode});
                    orderChangePlanItem.setId(0);
                    orderChangePlanItem.setOptlock(0);
                    orderChangePlanItem.setDescription("Test");

                    orderChange.setStartDate(new LocalDate().toDateTimeAtCurrentTime().toDate());
                    orderChange.setQuantity(BigDecimal.ONE);
                    orderChange.setOrderChangePlanItems(new OrderChangePlanItemWS[] {orderChangePlanItem});

                    orderId = api.createUpdateOrder(order, new OrderChangeWS[] {orderChange});

                    order = api.getOrder(orderId);

                    assertNotNull(order.getId(), "There should be one order!");
                    assertEquals(order.getOrderLines()[0].getQuantityAsDecimal().setScale(BigDecimal.ROUND_CEILING, 2),
                                 new BigDecimal(2).setScale(BigDecimal.ROUND_CEILING, 2), "Quantity should be 2!");
                    assertEquals(order.getOrderLines()[0].getPriceAsDecimal().setScale(BigDecimal.ROUND_CEILING, 2),
                                 new BigDecimal(99.99).setScale(BigDecimal.ROUND_CEILING, 2), "Price of one product should be $99.99");
                    assertEquals(order.getTotalAsDecimal().setScale(BigDecimal.ROUND_CEILING, 2),
                                 order.getOrderLines()[0].getPriceAsDecimal().setScale(BigDecimal.ROUND_CEILING, 2)
                                         .multiply(new BigDecimal(2)), "Total should be 2 * quantity of the product!");
                });
    }

    @Test(expectedExceptions = SessionInternalError.class)
    public void test017OrderValidityPeriodNotWithinPlanPeriodFailed () {
        initAccountTypeAndCustomer("TestAccountType", "PlanValidity");
        dataPreparationForPlans()
                .test((env, envBuilder) -> {

                    Integer msisdn = env.idForCode("MSISDN");
                    Integer assetCode = env.idForCode("Asset");
                    Integer planRate = env.idForCode("SMS Rate Plan");
                    Integer planDiscount = env.idForCode("SMS Discount Plan");
                    Integer planTariff = env.idForCode("Basic Tariff Plan");
                    Integer planDependency = env.idForCode("Dependency Plan");
                    Integer orderId = null;
                    Integer userId = env.idForCode("TestCustomerAsset");

                    JbillingAPI api = env.getPrancingPonyApi();
            try {
                ItemDTOEx assetManagementEnabledItem = api.getItem(msisdn, userId, null);
                AssetWS asset = api.getAsset(assetCode);

                assertNotNull(asset, "There should be asset!");

                assertNotNull(assetManagementEnabledItem, "There should be one product");
                assertTrue(null != assetManagementEnabledItem.getAssetManagementEnabled() && assetManagementEnabledItem.getAssetManagementEnabled()
                        .equals(1), "This product should have asset management enabled!");

                PlanWS smsPlanRate = api.getPlanWS(planRate);
                validatePlan(smsPlanRate, 3);

                PlanWS smsPlanDiscount = api.getPlanWS(planDiscount);
                validatePlan(smsPlanDiscount, 2);

                PlanWS smsPlanTariff = api.getPlanWS(planTariff);
                validatePlan(smsPlanTariff, 5);

                PlanWS smsPlanDependency = api.getPlanWS(planDependency);
                validatePlan(smsPlanDependency, 1);

                orderId = envBuilder.orderBuilder(api)
                        .withOrderLine(envBuilder.orderBuilder(api)
                                               .orderLine()
                                               .withItemId(smsPlanTariff.getItemId())
                                               .build())
                        .withCodeForTests("Order")
                        .forUser(userId)
                        .withActiveSince(new LocalDate().toDateTimeAtCurrentTime().plusMonths(3).toDate())
                        .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                        .withPeriod(envHelper.getOrderPeriodMonth(api))
                        .build();

            } finally {
                if (null != orderId) { deleteOrderHierarchy(api, orderId); }
            }
        });
    }

    private void deleteOrderHierarchy (JbillingAPI api, Integer orderId) {
        if (null == orderId) { return; }
        OrderWS order = api.getOrder(orderId);
        if (null != order.getChildOrders()) {
            for (OrderWS child : order.getChildOrders()) {
                deleteOrderHierarchy(api, child.getId());
            }
        }
        api.deleteOrder(orderId);
    }

    private void validatePlan (PlanWS plan, int planItems) {
        assertTrue(null != plan.getPlanItems(), "This is plan!");
        assertNotNull(plan, "One plan is expected!");
        assertEquals(plan.getPlanItems().size(), planItems, "This plan should have " + planItems + " plan items!");
    }

    private Integer createProductWithMultipleDependencies (String code, boolean global, Integer categoryId, Integer minimum, Integer maximum, BigDecimal flatPrice,
                                                           JbillingAPI api, TestEnvironmentBuilder testEnvironmentBuilder, Integer dependentId, Integer dependentId2) {
        return testEnvironmentBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withDependencies(testEnvironmentBuilder.itemBuilder(api)
                                          .itemDependency()
                                          .withDependentId(dependentId)
                                          .withMinimum(minimum)
                                          .withMaximum(maximum)
                                          .withItemDependencyType(ItemDependencyType.ITEM)
                                          .build(),
                                  testEnvironmentBuilder.itemBuilder(api)
                                          .itemDependency()
                                          .withDependentId(dependentId2)
                                          .withMinimum(minimum)
                                          .withMaximum(maximum)
                                          .withItemDependencyType(ItemDependencyType.ITEM)
                                          .build())
                .withFlatPrice(String.valueOf(flatPrice))
                .global(global)
                .build();
    }

    private PlanWS createPlan (Integer itemId, Integer periodId, String description, List<PlanItemWS> planItems) {

        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();

        PlanWS plan = new PlanWS();
        plan.setItemId(itemId);
        plan.setPeriodId(periodId);
        plan.setDescription(description);
        plan.setPlanItems(planItems);

        Integer planId = api.createPlan(plan);

        return api.getPlanWS(planId);
    }

    public PlanItemWS createPlanItem (Integer itemId, PriceModelWS model, Integer precedence, Integer bundledPeriodId, String bundledQuantity) {
        SortedMap<Date, PriceModelWS> models = new TreeMap<>();

        PlanItemWS planItem = new PlanItemWS();
        planItem.setItemId(itemId);
        planItem.setModel(model);
        planItem.addModel(new Date(), model);
        planItem.setModels(models);
        planItem.setPrecedence(precedence);
        PlanItemBundleWS planItemBundle = new PlanItemBundleWS();
        planItemBundle.setPeriodId(bundledPeriodId);
        planItemBundle.setQuantity(bundledQuantity);
        planItem.setBundle(planItemBundle);

        return planItem;
    }

    private void addMetafield (String metafieldNameCcCardholderName, boolean disabled, boolean mandatory,
                               DataType string, int displayOrder, Object value, List<MetaFieldValueWS> metaFields) {
        MetaFieldValueWS metaFieldValue = new MetaFieldValueWS();

        metaFieldValue.setFieldName(metafieldNameCcCardholderName);
        metaFieldValue.getMetaField().setDisabled(disabled);
        metaFieldValue.getMetaField().setMandatory(mandatory);
        metaFieldValue.getMetaField().setDataType(string);
        metaFieldValue.getMetaField().setDisplayOrder(displayOrder);
        metaFieldValue.setValue(value);

        metaFields.add(metaFieldValue);
    }

    private TestBuilder initAccountTypeAndCustomer (String code, String userName) {
        return testBuilder.given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            AccountTypeWS accountType = envBuilder.accountTypeBuilder(api)
                    .withName(code)
                    .withCreditLimit("100")
                    .withEntityId(api.getCallerCompanyId())
                    .build();

            envBuilder.customerBuilder(api)
                    .withUsername(userName)
                    .withAccountTypeId(accountType.getId())
                    .build();
        });
    }

    private TestBuilder dataPreparationForPlans () {
        final Integer[] productIds = new Integer[8];
        final Integer[] categoryIds = new Integer[5];
        final Integer[] planIds = new Integer[4];
        return testBuilder.given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            categoryIds[0] = plansHelper.buildAndPersistCategory("Services", true, api, envBuilder);
            categoryIds[1] = plansHelper.buildAndPersistCategoryWithAssetManagementEnabled("MSISDN Repository", true, api, envBuilder);
            categoryIds[2] = plansHelper.buildAndPersistCategory("Tariff", true, api, envBuilder);
            categoryIds[3] = plansHelper.buildAndPersistCategory("Rates", true, api, envBuilder);

            planIds[0] = plansHelper.buildAndPersistFlatProduct("SMS Rate Plan Item", false, categoryIds[3], new BigDecimal("99.99"), api, envBuilder);
            planIds[1] = plansHelper.buildAndPersistFlatProduct("SMS Discount Plan Item", false, categoryIds[3], new BigDecimal("99.99"), api, envBuilder);

            planIds[2] = plansHelper.buildAndPersistFlatProductWithDate("Basic Tariff Plan Item", false, categoryIds[2], new BigDecimal("180"), new Date(),
                                                                        new LocalDate().toDateTimeAtCurrentTime().plusMonths(2).toDate(), api, envBuilder);

            planIds[3] = plansHelper.buildAndPersistFlatProductWithDate("Dependency Plan Item", false, categoryIds[3], new BigDecimal("180"), new Date(),
                                                                        new LocalDate().toDateTimeAtCurrentTime().plusMonths(2).toDate(), api, envBuilder);

            productIds[0] = plansHelper.buildAndPersistFlatProduct("SMS Service", true, categoryIds[0], new BigDecimal("10"), api, envBuilder);
            productIds[1] = plansHelper.buildAndPersistFlatProduct("GPRS Service", true, categoryIds[0], new BigDecimal("10"), api, envBuilder);

            productIds[2] = plansHelper.buildAndPersistFlatProductWithAssetManagementEnabled("MSISDN", true, categoryIds[1], new BigDecimal("99.99"), api, envBuilder);

            productIds[3] = plansHelper.buildAndPersistFlatProduct("SMS to North America", true, categoryIds[3], new BigDecimal("99.99"), api, envBuilder);
            productIds[4] = plansHelper.buildAndPersistFlatProduct("SMS to EU27", true, categoryIds[3], new BigDecimal("99.99"), api, envBuilder);
            productIds[5] = plansHelper.buildAndPersistFlatProduct("Connection Fee", true, categoryIds[3], new BigDecimal("99.99"), api, envBuilder);
            productIds[6] = plansHelper.buildAndPersistFlatProduct("SMS to Delhi", true, categoryIds[3], new BigDecimal("99.99"), api, envBuilder);
            productIds[7] = plansHelper.buildAndPersistProductWithDependency("SMS to New Delhi", true, categoryIds[3], productIds[6], 1, 1, new BigDecimal("99.99"), api, envBuilder);

            ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryIds[1]);
            Integer assetStatusId = itemTypeWS.getAssetStatuses().stream().
                    filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription()
                            .equals("Available")).collect(Collectors.toList()).get(0).getId();

            envBuilder.assetBuilder(api)
                    .withItemId(productIds[2])
                    .withAssetStatusId(assetStatusId)
                    .global(true)
                    .withCode("Asset")
                    .build();

            envBuilder.planBuilder(api, "SMS Rate Plan")
                    .withItemId(planIds[0])
                    .withDescription("Test SMS Rate Plan")
                    .withPeriodId(envHelper.getOrderPeriodOneTime(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[0])
                                         .withBundledQuantity("1")
                                         .withPrecedence(-1)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name()))
                                         .withBundledPeriodId(envHelper.getOrderPeriodMonth(api))
                                         .build())
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[3])
                                         .withBundledQuantity("0")
                                         .withPrecedence(-1)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("2"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("2"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[4])
                                         .withBundledQuantity("0")
                                         .withPrecedence(-1)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.5"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.5"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .build();

            envBuilder.planBuilder(api, "SMS Discount Plan")
                    .withItemId(planIds[1])
                    .withDescription("Test SMS Discount Plan")
                    .withPeriodId(envHelper.getOrderPeriodOneTime(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[3])
                                         .withBundledQuantity("0")
                                         .withPrecedence(-2)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("1"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("1"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[4])
                                         .withBundledQuantity("0")
                                         .withPrecedence(-2)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.025"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.025"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .build();

            envBuilder.planBuilder(api, "Basic Tariff Plan") // Plan with product and asset
                    .withItemId(planIds[2])
                    .withDescription("Test Basic Tariff Plan")
                    .withPeriodId(envHelper.getOrderPeriodMonth(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()    //Item with asset that causes the test to fail. If it's commented out this test will pass
                                         .withItemId(productIds[2])
                                         .withBundledQuantity("1")
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(envHelper.getOrderPeriodMonth(api))
                                         .build())
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[5])
                                         .withBundledQuantity("1")
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[1])
                                         .withBundledQuantity("1")
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(envHelper.getOrderPeriodMonth(api))
                                         .build())
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(planIds[0])
                                         .withBundledQuantity("1")
                                         .withPrecedence(-1)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(envHelper.getOrderPeriodMonth(api))
                                         .build())
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(planIds[1])
                                         .withBundledQuantity("0")
                                         .withPrecedence(-1)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("30"), api
                                                 .getCallerCurrencyId()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("30"), api
                                                 .getCallerCurrencyId()))
                                         .withBundledPeriodId(Constants.ORDER_PERIOD_ONCE)
                                         .build())
                    .build();

            envBuilder.planBuilder(api, "Dependency Plan")
                    .withItemId(planIds[3])
                    .withDescription("Test Dependency Plan")
                    .withPeriodId(envHelper.getOrderPeriodMonth(api))
                    .addPlanItem(PlanBuilder.PlanItemBuilder.getBuilder()
                                         .withItemId(productIds[7])
                                         .withBundledQuantity("1")
                                         .withPrecedence(-1)
                                         .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name()))
                                         .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name()))
                                         .withBundledPeriodId(envHelper.getOrderPeriodOneTime(api))
                                         .build())
                    .build();
        });
    }

    private TestBuilder initTestDataForPayment (String code, String userName) {
        return testBuilder.given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            Date expiryDate = new LocalDate().withMonthOfYear(12)
                    .withYear(Calendar.getInstance().get(Calendar.YEAR) + 1)
                    .toDate(); // expiry date of the credit card

            PaymentMethodTypeWS paymentMethodType = new PaymentMethodTypeWS();
            MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
            PaymentInformationWS paymentInformation = new PaymentInformationWS();
            List<MetaFieldValueWS> metaFields = new ArrayList<>();

            paymentMethodType = api.getPaymentMethodType(1);

            mainSubscription.setNextInvoiceDayOfPeriod(new LocalDate().getDayOfMonth());
            mainSubscription.setPeriodId(2);

            paymentInformation.setProcessingOrder(1);
            paymentInformation.setPaymentMethodTypeId(1);
            paymentInformation.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

            addMetafield(Constants.METAFIELD_NAME_CC_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, "Ashish".toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_NUMBER, false, true, DataType.CHAR, 2, "4111111111111152".toCharArray(), metaFields);
            addMetafield(Constants.METAFIELD_NAME_CC_EXPIRY_DATE, false, true, DataType.CHAR, 3, (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT)
                    .print(expiryDate.getTime())).toCharArray(), metaFields);

            paymentInformation.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

            AccountTypeWS accountType = envBuilder.accountTypeBuilder(api)
                    .withName(code)
                    .withCreditLimit("100")
                    .withEntityId(api.getCallerCompanyId())
                    .withPaymentMethodTypeIds(new Integer[] {paymentMethodType.getId()})
                    .build();

            UserWS user = envBuilder.customerBuilder(api)
                    .withUsername(userName)
                    .withAccountTypeId(accountType.getId())
                    .withMainSubscription(mainSubscription)
                    .addPaymentInstrument(paymentInformation)
                    .build();

            user.setNextInvoiceDate(new LocalDate().toDateTimeAtCurrentTime().toDate());
            api.updateUser(user);
        });
    }
}

