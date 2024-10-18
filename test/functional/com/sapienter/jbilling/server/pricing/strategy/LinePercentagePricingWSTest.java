package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Created by aman on 18/12/14.
 */
@Test(groups = {"web-services", "pricing", "linePercentage"}, testName = "LinePercentagePricingWSTest")
public class LinePercentagePricingWSTest {

    JbillingAPI api;
    private static Integer productId = null;
    private static Integer customerId = null;
    private static Integer ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private final static int BASIC_ACCOUNT_TYPE = 1;
    private static Integer ACCOUNT_TYPE;
	private static Integer MONTHLY_PERIOD;
    private final static int TEST_ITEM_ID = 2602;

    private final static String strategy = PriceModelStrategy.LINE_PERCENTAGE.name();

    @BeforeTest
    public void getAPI() throws Exception {
        api = JbillingAPIFactory.getAPI();
        ACCOUNT_TYPE = PricingTestHelper.PRANCING_PONY_BASIC_ACCOUNT_TYPE;
	    ORDER_CHANGE_STATUS_APPLY_ID= PricingTestHelper.getOrCreateOrderChangeApplyStatus(api);
	    MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);
    }

    @Test
    public void test001LinePercentagePricing() {
        OrderWS order = null;
        try {
            // Test the price calculated
            createProduct();
            assertNotNull("Item created", productId);

            // create user to test pricing with
            UserWS user = PricingTestHelper.buildUser("linePercentage-pricing", MONTHLY_PERIOD, ACCOUNT_TYPE);
            user.setUserId(api.createUser(user));
            customerId = user.getUserId();
            assertNotNull("Customer created", customerId);

            order = createOrder();
            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

            assertEquals("Two Order line created", 2, order.getOrderLines().length);
            OrderLineWS tax = getTaxLine(order);

            assertNotNull("Tax order line exist", tax);
            assertEquals("Tax Amount", new BigDecimal(0.35), tax.getAmountAsDecimal());
            assertEquals("Total Amount", new BigDecimal(3.85), order.getTotalAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occurred : " + e);
        }
    }

    @Test
    public void test002HistoricalPricing() {
        OrderWS order = null;
        try {
            updateItem("20.00");
            order = createOrder();

            // rate order and verify starting price
            order.setActiveSince(new DateMidnight(1971, 1, 1).toDate()); // before new epoch date
            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertEquals("Two Order line created", 2, order.getOrderLines().length);
            OrderLineWS tax = getTaxLine(order);

            assertNotNull("Tax order line exist", tax);
            assertEquals("Tax Amount", new BigDecimal(0.35), tax.getAmountAsDecimal());
            assertEquals("Total Amount", new BigDecimal(3.85), order.getTotalAsDecimal());

            order.setActiveSince(new DateMidnight(2014, 12, 16).toDate()); // after epoch date
            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

            assertEquals("Two Order line created", 2, order.getOrderLines().length);
            tax = getTaxLine(order);

            assertNotNull("Tax order line exist", tax);
            assertEquals("Tax Amount", new BigDecimal(0.70), tax.getAmountAsDecimal());
            assertEquals("Total Amount", new BigDecimal(4.20), order.getTotalAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occurred : " + e);
        }
    }

    @Test
    public void test003CustomerSpecificPricing() {
        OrderWS order = null;
        PlanItemWS planItemWS = null;
        try {
            PlanItemWS price = new PlanItemWS();
            price.setItemId(productId);
            PriceModelWS newLinePercentagePrice = new PriceModelWS();
            newLinePercentagePrice.setType(strategy);
            newLinePercentagePrice.setRate("25.00");
            ItemDTOEx item = api.getItem(productId, customerId, null);
            price.getModels().put(CommonConstants.EPOCH_DATE, newLinePercentagePrice);
            planItemWS = api.createCustomerPrice(customerId, price, null);

            order = createOrder();
            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertEquals("Two Order line created", 2, order.getOrderLines().length);
            OrderLineWS tax = getTaxLine(order);

            assertNotNull("Tax order line exist", tax);
            assertEquals("Tax Amount", new BigDecimal(0.875), tax.getAmountAsDecimal());
            assertEquals("Total Amount", new BigDecimal(4.375), order.getTotalAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occurred : " + e);
        } finally {
            if (planItemWS != null) api.deleteCustomerPrice(customerId, planItemWS.getId());
        }
    }

    @Test
    public void test004AccountSpecificPricing() {
        OrderWS order = null;
        PlanItemWS planItemWS = null;
        try {
            PlanItemWS price = new PlanItemWS();
            price.setItemId(productId);
            PriceModelWS newLinePercentagePrice = new PriceModelWS();
            newLinePercentagePrice.setType(strategy);
            newLinePercentagePrice.setRate("30.00");
            ItemDTOEx item = api.getItem(productId, customerId, null);
            price.getModels().put(CommonConstants.EPOCH_DATE, newLinePercentagePrice);
            planItemWS = api.createAccountTypePrice(BASIC_ACCOUNT_TYPE, price, null);

            order = createOrder();
            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertEquals("Two Order line created", 2, order.getOrderLines().length);
            OrderLineWS tax = getTaxLine(order);

            assertNotNull("Tax order line exist", tax);
            assertEquals("Tax Amount", new BigDecimal(1.05), tax.getAmountAsDecimal());
            assertEquals("Total Amount", new BigDecimal(4.55), order.getTotalAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occurred : " + e);
        } finally {
            if (planItemWS != null) api.deleteAccountTypePrice(BASIC_ACCOUNT_TYPE, planItemWS.getId());
        }
    }

    @Test
    public void test005InvoiceTest() {
        OrderWS order = null;
        InvoiceWS invoice = null;
        try {
            order = createOrder();
            Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID)); // create order
            assertNotNull("Invoice created", invoiceId);
            invoice = api.getInvoiceWS(invoiceId);
            InvoiceLineDTO tax = getTaxLine(invoice);

            assertNotNull("Tax Invoice line exist", tax);
            assertEquals("Tax Amount", new BigDecimal(0.70), tax.getAmountAsDecimal());
            assertEquals("Total Amount", new BigDecimal(4.20), invoice.getTotalAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occurred : " + e);
        } finally {
            if (invoice != null && invoice.getId() != null) api.deleteInvoice(invoice.getId());
            if (invoice != null && invoice.getOrders().length == 1) {
                Integer[] orders = invoice.getOrders();
                api.deleteOrder(orders[0]);
            }
        }
    }

    @Test
    public void test006PlanTest() {
        final Integer MONTHLY_PERIOD = 2;
        PlanWS plan = null;
        ItemDTOEx item = null;
        InvoiceWS invoice = null;
        OrderWS order = null;
        try {
            // subscription item for plan
            item = new ItemDTOEx();
            item.setDescription("Test Monthly Vat With Lemonade");
            item.setNumber("TEST-M-VT-L");
            item.setPrice("10.00");
            item.setTypes(new Integer[]{1});

            item.setId(api.createItem(item));
            assertNotNull("Plan created", item.getId());
            // create plan
            PlanItemWS callPrice = new PlanItemWS();
            callPrice.setItemId(TEST_ITEM_ID);
            callPrice.getModels().put(CommonConstants.EPOCH_DATE,
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10.00"), 1));

            PlanItemWS tax = new PlanItemWS();
            tax.setItemId(productId);
            tax.getModels().put(CommonConstants.EPOCH_DATE,
                    new PriceModelWS(strategy, new BigDecimal("5.00"), 1));

            plan = new PlanWS();
            plan.setItemId(item.getId());
            plan.setDescription("Test Monthly Vat With Lemonade Plan");
            plan.setPeriodId(MONTHLY_PERIOD);
            plan.addPlanItem(callPrice);
            plan.addPlanItem(tax);
            plan.setId(api.createPlan(plan));
            assertNotNull("Plan created", plan.getId());

            //Subscribe to plan
            order = PricingTestHelper.buildMonthlyOrder(customerId, MONTHLY_PERIOD);
            OrderLineWS planLine = PricingTestHelper.buildOrderLine(item.getId(), 1);
            order.setOrderLines(new OrderLineWS[]{planLine});
            Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertNotNull("Order created", orderId);
            order = api.getOrder(orderId);
            assertNotNull("Order found", order);

            //Purchase product from plan
            OrderWS orderWithProduct = createOrder();
            Integer invoiceId = api.createOrderAndInvoice(orderWithProduct, OrderChangeBL.buildFromOrder(orderWithProduct, ORDER_CHANGE_STATUS_APPLY_ID)); // create order
            assertNotNull("Invoice created", invoiceId);
            invoice = api.getInvoiceWS(invoiceId);
            InvoiceLineDTO taxLine = getTaxLine(invoice);

            assertNotNull("Tax Invoice line exist", tax);
            assertEquals("Tax Amount", new BigDecimal(0.50), taxLine.getAmountAsDecimal());
            assertEquals("Total Amount", new BigDecimal(10.50), invoice.getTotalAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error occurred : " + e);
        } finally {
            if (invoice != null && invoice.getId() != null) api.deleteInvoice(invoice.getId());
            if (invoice != null && invoice.getOrders().length == 1) {
                Integer[] orders = invoice.getOrders();
                api.deleteOrder(orders[0]);
            }
            if (order != null && order.getId() != null) api.deleteOrder(order.getId());
            if (plan != null && plan.getId() != null) api.deletePlan(plan.getId());
            if (item != null && item.getId() != null) api.deleteItem(item.getId());
        }
    }

    @AfterTest
    public void destroy() {
        // delete the customer "LP_User_Test" and product "Vat Tax 2014"
        if (productId != null) api.deleteItem(productId);
        if (customerId != null) api.deleteUser(customerId);
    }

    private void createProduct() {
        // new item with Line Percentage pricing
        PriceModelWS linePercentagePrice = new PriceModelWS();
        linePercentagePrice.setType(strategy);
        linePercentagePrice.setRate(new BigDecimal("10.00"));
        linePercentagePrice.setCurrencyId(1);

        Integer categoryId = PricingTestHelper.createItemCategory(api);
        ItemDTOEx item = PricingTestHelper.buildItem("VT 2014", "Vat Tax 2014", categoryId);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, linePercentagePrice);

        productId = api.createItem(item);
    }

    private void updateItem(String rate) {
        // Update item with Line Percentage pricing
        PriceModelWS newLinePercentagePrice = new PriceModelWS();
        newLinePercentagePrice.setType(strategy);
        newLinePercentagePrice.setRate(rate);
        ItemDTOEx item = api.getItem(productId, customerId, null);
        item.getDefaultPrices().put(new DateTime(2014, 12, 15, 0, 0, 0, 0).withTime(0, 0, 0, 0).toDate(), newLinePercentagePrice);
        api.updateItem(item);
    }

    private OrderWS createOrder() {
        // order to be rated to test pricing
        OrderWS order = PricingTestHelper.buildOneTimeOrder(customerId);
        OrderLineWS lemonade = PricingTestHelper.buildOrderLine(TEST_ITEM_ID, 1);
        OrderLineWS tax = PricingTestHelper.buildOrderLine(productId, 1);
        tax.setPercentage(true);
        order.setOrderLines(new OrderLineWS[]{lemonade, tax});
        return order;
    }

    private OrderLineWS getTaxLine(OrderWS order) {
        for (OrderLineWS line : order.getOrderLines()) {
            if (line.getItemId().equals(productId)) {
                return line;
            }
        }
        return null;
    }

    private InvoiceLineDTO getTaxLine(InvoiceWS invoiceWS) {
        for (InvoiceLineDTO line : invoiceWS.getInvoiceLines()) {
            if (line.getItemId().equals(productId)) {
                return line;
            }
        }
        return null;
    }
}
