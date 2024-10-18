package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;

import static com.sapienter.jbilling.test.Asserts.assertEquals;

/**
 * Created by vivekmaster146 on 21/8/14.
 */
@Test(groups = {"rating-late-usage", "plan-enhancements"}, testName = "PlanRelatedEnhancementsTest")
public class PlanRelatedEnhancementsTest {

    private static final Logger logger = LoggerFactory.getLogger(PlanRelatedEnhancementsTest.class);
    private static final Integer PRANCING_PONY = new Integer(1);
    private static final Integer MONTHLY_PERIOD = 2;
    private static final Integer ONE_TIME = 1;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    JbillingAPI api = null;

    @BeforeClass
    protected void getAPI() throws Exception {
        api = JbillingAPIFactory.getAPI();
    }

    @Test
    public void priceDeterminationTest() {
        final Integer LEMONADE = 2602;

        final Integer LEMONADE_MODEL_MAP_ID = 1602;

        ItemDTOEx newItem1 = getItem("TEST PLAN", "TP-01");
        newItem1.setId(api.createItem(newItem1));
        PlanWS plan = getPlan("TEST PLAN", newItem1.getId());
        //plan.setTariff(true);

        PlanItemWS planItem = new PlanItemWS();
        planItem.setItemId(LEMONADE);
        planItem.getModels().put(CommonConstants.EPOCH_DATE,
                new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.10"), 1));

        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(Constants.ORDER_PERIOD_ONCE);
        bundle.setTargetCustomer(PlanItemBundleWS.TARGET_SELF);
        bundle.setQuantity(new BigDecimal("10"));

        planItem.setBundle(bundle);
        plan.addPlanItem(planItem);

        plan.setId(api.createPlan(plan));

        PriceModelWS priceModelWS = new PriceModelWS();
        priceModelWS.setCurrencyId(1);
        priceModelWS.setRate(new BigDecimal("1.35"));
        priceModelWS.setType("FLAT");

        AccountTypeWS accountType = createAccountType();
        Integer accountTypeId = api.createAccountType(accountType);

        PlanItemWS price = new PlanItemWS();
        price.setItemId(LEMONADE);
        price.setPrecedence(1);
        price.getModels().put(new Date(), priceModelWS);
        price = api.createAccountTypePrice(accountTypeId, price, null);
        logger.debug("TEST OUT: Created Account Type Price as {}", price);

        UserWS customer = CreateObjectUtil.createCustomer(1, "CustomerPriceTest",
                "AAaa$$11", 1, 5, false, 1, null,
                CreateObjectUtil.createCustomerContact("test@gmail.com"));
        customer.setAccountTypeId(accountTypeId);
        Integer userId = api.createUser(customer);
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(MONTHLY_PERIOD);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        // subscribe to plan item
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(newItem1.getId());
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[]{line});

        api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        PriceModelWS priceModelWS2 = new PriceModelWS();
        priceModelWS2.setCurrencyId(1);
        priceModelWS2.setRate(new BigDecimal("57.55"));
        priceModelWS2.setType("FLAT");

        PlanItemWS customerPrice = new PlanItemWS();
        customerPrice.setItemId(LEMONADE);
        customerPrice.setPrecedence(1);
        customerPrice.getModels().put(new Date(), priceModelWS2);
        customerPrice = api.createCustomerPrice(userId, customerPrice, null);
        logger.debug("TEST OUT: Created Customer Price as {}", customerPrice);

        OrderWS productOrder = new OrderWS();
        productOrder.setUserId(userId);
        productOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        productOrder.setPeriod(ONE_TIME);
        productOrder.setCurrencyId(1);
        productOrder.setActiveSince(new Date());

        OrderLineWS line2 = new OrderLineWS();
        line2.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line2.setItemId(LEMONADE);
        line2.setUseItem(true);
        line2.setQuantity(1);
        productOrder.setOrderLines(new OrderLineWS[]{line2});
        Integer productOrderId = api.createOrder(productOrder, OrderChangeBL.buildFromOrder(productOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        // Fetch the order saved in the database
        productOrder = api.getOrder(productOrderId);
        Integer[] orderIds = api.getLastOrders(userId, 3);
        OrderWS bundledOrder = api.getOrder(orderIds[2]);
        OrderWS parentOrder = api.getOrder(orderIds[1]);

        assertEquals("Customer Price should be picked", new BigDecimal("57.55"), productOrder.getOrderLines()[0].getPriceAsDecimal());

        // delete the child order first
        api.deleteOrder(bundledOrder.getId());
        // delete the parent order
        api.deleteOrder(parentOrder.getId());
        // delete the product order
        api.deleteOrder(productOrderId);
        // Customer Price will be delete with user. So no need to explicitly delete customer prices
        api.deleteUser(userId);
        api.deleteAccountTypePrice(accountTypeId, price.getId());
        api.deleteAccountType(accountTypeId);
        api.deletePlan(plan.getId());
        api.deleteItem(newItem1.getId());
    }

    @Test
    public void planUpdate() {
        final Integer LEMONADE = 2602;
        final Integer TESTITEM = 240;
        final Integer COFEE = 3;
        final Integer LOOP_UNTIL = 100;

        ItemDTOEx newItem1 = getItem("Test PLan2", "TP-01");
        newItem1.setId(api.createItem(newItem1));
        PlanWS plan = getPlan("TEST PLAN", newItem1.getId());
        //plan.setTariff(true);

        plan = addPlanItems(plan);

        plan.setId(api.createPlan(plan));
        Integer[] orderIds = new Integer[LOOP_UNTIL];
        Integer[] userIds = new Integer[LOOP_UNTIL];

        // Create 100 customers, subscribe them to the plan
        for (int i = 0; i < LOOP_UNTIL; i++) {

            UserWS customer = CreateObjectUtil.createCustomer(1, "TestPlanCustomer" + i,
                    "newPa$$word1", 1, 5, false, 1, null,
                    CreateObjectUtil.createCustomerContact("test@gmail.com"));
            Integer userId = api.createUser(customer);
            userIds[i] = userId;

            OrderWS order = new OrderWS();
            order.setUserId(userId);
            order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
            order.setPeriod(MONTHLY_PERIOD);
            order.setCurrencyId(1);
            order.setActiveSince(new Date());

            // subscribe to plan item
            OrderLineWS line = new OrderLineWS();
            line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            line.setItemId(newItem1.getId());
            line.setUseItem(true);
            line.setQuantity(1);
            order.setOrderLines(new OrderLineWS[]{line});
            orderIds[i] = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        }
        StopWatch watch = new StopWatch("Stop Watch");
        watch.start();
        // Chane price of a bundled plan item and update the plan
        plan.getPlanItems().get(0).getModels().put(CommonConstants.EPOCH_DATE,
                new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("305.20"), 1));
        api.updatePlan(plan);
        watch.stop();
        logger.debug("TEST OUT: Total time taken for updating plan having {} subscriptions is {}",LOOP_UNTIL, watch.getTotalTimeMillis());


        // Unsusbcribe the customers to the plan

        watch.start();

        for (Integer orderId : orderIds) {
            logger.debug("TEST OUT: Deleting Parent Order {} and its child {}", orderId, (orderId - 1));
            // Delete child order before deleting parent order
            api.deleteOrder(orderId - 1);
            api.deleteOrder(orderId);
        }
        // Re-subscribe the customer to the plan
        for (Integer userId : userIds) {
            OrderWS order = new OrderWS();
            order.setUserId(userId);
            order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
            order.setPeriod(MONTHLY_PERIOD);
            order.setCurrencyId(1);
            order.setActiveSince(new Date());

            // subscribe to plan item
            OrderLineWS line = new OrderLineWS();
            line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            line.setItemId(newItem1.getId());
            line.setUseItem(true);
            line.setQuantity(1);
            order.setOrderLines(new OrderLineWS[]{line});
            api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        }

        watch.stop();

        logger.debug("TEST OUT: Total time taken for re-subscription is {}", watch.getTotalTimeMillis());

        // Delete Created Users. All orders associated would also get deleted.
        for (Integer userId : userIds) {
            logger.debug("TEST OUT: Deleting User:{}", userId);
            api.deleteUser(userId);
        }
       /* api.deletePlan(plan.getId());*/
    }

    public PlanWS addPlanItems(PlanWS plan) {
        logger.debug("TEST OUT: PlanRelatedEnhancementsTest.addPlanItems");
        for (Integer i = 0; i < 50; i++) {
            PlanItemWS planItem = new PlanItemWS();
            PlanItemBundleWS bundle = new PlanItemBundleWS();

            ItemDTOEx newItem = getItem("Item" + i, "TP-01" + i);
            planItem.setItemId(api.createItem(newItem));
            planItem.getModels().put(CommonConstants.EPOCH_DATE,
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(i), 1));
            bundle.setPeriodId(Constants.ORDER_PERIOD_ONCE);
            bundle.setTargetCustomer(PlanItemBundleWS.TARGET_SELF);
            bundle.setQuantity(new BigDecimal("1"));
            planItem.setBundle(bundle);
            plan.addPlanItem(planItem);
        }
        return plan;
    }


    private PlanWS getPlan(String description, Integer itemId) {
        PlanWS plan = new PlanWS();
        plan.setItemId(itemId);
        plan.setDescription(description);
        plan.setPeriodId(MONTHLY_PERIOD);
        return plan;
    }

    private ItemDTOEx getItem(String description, String number) {
        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription(description);
        newItem.setPriceModelCompanyId(PRANCING_PONY);
        newItem.setPrice(new BigDecimal("5.00"));
        newItem.setNumber(number);
        Integer types[] = new Integer[1];
        types[0] = new Integer(1);
        newItem.setTypes(types);
        return newItem;
    }

    private AccountTypeWS createAccountType() {
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setCreditLimit(new BigDecimal(0));
        accountType.setCurrencyId(new Integer(1));
        accountType.setEntityId(1);
        accountType.setInvoiceDeliveryMethodId(1);
        accountType.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        accountType.setMainSubscription(new MainSubscriptionWS(
                Constants.PERIOD_UNIT_DAY, 1));
        accountType.setCreditNotificationLimit1("0");
        accountType.setCreditNotificationLimit2("0");
        accountType.setCreditLimit("0");

        accountType.setName("Test account type_" + System.currentTimeMillis(),
                Constants.LANGUAGE_ENGLISH_ID);
        return accountType;
    }
}
