/*
* JBILLING CONFIDENTIAL
* _____________________
*
* [2003] - [2013] Enterprise jBilling Software Ltd.
* All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Enterprise jBilling Software.
* The intellectual and technical concepts contained
* herein are proprietary to Enterprise jBilling Software
* and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden.
*/

package com.sapienter.jbilling.server.usagePool;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.*;

import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.user.UserWS;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.util.Constants;

/**
* ItemPercentageSelectorFreeUsagePoolTest
* Test class for Item Percentage Selector with FUP scenarios.
* @author Amol Gadre
* @since 15-Dec-2013
*/

@Test(groups = { "usagePools" }, testName = "ItemPercentageSelectorFreeUsagePoolTest")
public class ItemPercentageSelectorFreeUsagePoolTest extends AbstractFreeUsagePoolPricingTest {

	public ItemPercentageSelectorFreeUsagePoolTest() {
		logger.debug("ItemPercentageSelectorFreeUsagePoolTest");
	}

	private static final BigDecimal ORDER_LINE_PRICE = new BigDecimal("10.00");

	private Integer itemTypeId_1 = null;
	private Integer itemTypeId_2 = null;
	private Integer itemTypeId_3 = null;

	private Integer metered_product_1 = null;
	private Integer metered_product_2 = null;
	private Integer metered_product_3 = null;

	private Integer product_1 = null;
	private Integer product_2 = null;
	private Integer product_3 = null;

    private void initialize() {

		PriceModelWS flatPrice = new PriceModelWS(PriceModelStrategy.FLAT.name(), ORDER_LINE_PRICE, Constants.PRIMARY_CURRENCY_ID);

		// Category 1 is our Percentage of Category
        if(null == itemTypeId_1){
            itemTypeId_1 = createItemType("Category_1_ ");
        }
		// Category 2 is our Selection of Category
        if(null == itemTypeId_2){
            itemTypeId_2 = createItemType("Category_2_ ");
        }
		// products we want to include in third category
        if(null == itemTypeId_3){
            itemTypeId_3 = createItemType("Category_3_ ");
        }

        // category 1 product
        if(null == metered_product_1){
            metered_product_1 = createItem("Metered Product 1", "Metered Product 1", flatPrice, itemTypeId_1);
        }
        // category 1 product
        if(null == metered_product_2){
            metered_product_2 = createItem("Metered Product 2", "Metered Product 2", flatPrice, itemTypeId_1);
        }

        // category 2 product
        if(null == metered_product_3){
            metered_product_3 = createItem("Metered Product 3", "Metered Product 3", flatPrice, itemTypeId_2);
        }

        // category 3 product
        if(null == product_1){
            product_1 = createItem("Product 1", "Product 1", new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("20.00"), Constants.PRIMARY_CURRENCY_ID), itemTypeId_3);
        }
        if(null == product_2){
            product_2 = createItem("Product 2", "Product 2", new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("40.00"), Constants.PRIMARY_CURRENCY_ID), itemTypeId_3);
        }
        if(null == product_3){
            product_3 = createItem("Product 3", "Product 3", new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("50.00"), Constants.PRIMARY_CURRENCY_ID), itemTypeId_3);
        }

	}

	//  Percentage = (Selection Category Prod qty) / (Percentage Category Prod qty) * 100
	@Test
	public void test001CreateOrderForFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testCreateOrderForFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test002EditOrderWithoutChangeQuantityForFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testEditOrderWithoutChangeQuantityForFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test003EditOrderAndIncreaseQuantityForFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testEditOrderAndIncreaseQuantityForFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test004ReducingQuantityUptoFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testReducingQuantityUptoFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);

	}

	@Test
	public void test005OrderLineQuantityZeroForFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testOrderLineQuantityZeroForFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test006AddNewLineWithQuantityGreaterThanFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testAddNewLineWithQuantityGreaterThanFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test007AddNewLineWithQuantityEqualToFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testAddNewLineWithQuantityEqualToFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test008AddNewLineWithQuantityLessThanFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testAddNewLineWithQuantityLessThanFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test009CreateOrderWithTwoLinesForFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 4, metered_product_2, 3);
		// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
		super.testCreateOrderWithTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test010EditOrderAddTwoLinesForFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 4, metered_product_2, 3);
		// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
		super.testEditOrderAddTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test011EditOrderRemoveLineWhichDoesNotUseFreeUsagePool() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 4, metered_product_2, 3);
		// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
		super.testEditOrderRemoveLineWhichDoesNotUseFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);

	}

	@Test
	public void test012DeleteOrder() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 12, metered_product_2, 10);
		// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
		super.testDeleteOrder(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test013EditOrderLineWithFUPQuantity() throws Exception {
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 4, metered_product_2, 3);
		// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
		super.testEditOrderLineWithFUPQuantity(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test014EditOrderLineWithFUPQuantityWhenBillingCycleEqualsToday() throws Exception {
		UserWS user = api.getUserWS(userId);
		user.getMainSubscription().setNextInvoiceDayOfPeriod(GregorianCalendar.getInstance().get(Calendar.DAY_OF_MONTH));
		api.updateUser(user);
		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 4, metered_product_2, 3);
		// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
		super.testEditOrderLineWithFUPQuantity(customerUsagePoolId, usagePoolId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test015MultipleUsagePoolsWithMultpileOrderLines() throws Exception {

		Integer[] usagePoolsId = new Integer[2];
		Set<Integer> customerUsagePoolsId = new HashSet<Integer>();

        userId = createUser("MultipleFUPCustomer");
        UserWS user = api.getUserWS(userId);
        customerId = user.getCustomerId();

		usagePoolsId[0] = usagePoolId = createFreeUsagePool("100 Units Free", new BigDecimal(100), new Integer[]{itemTypeId}, new Integer[]{itemId});
		usagePoolsId[1] = usagePoolId2 = createFreeUsagePool("200 Units Free", new BigDecimal(200), new Integer[]{itemTypeId}, new Integer[]{itemId});

		BigDecimal totalFreeUsageQuantity = BigDecimal.ZERO;
		for (Integer usagePoolID: usagePoolsId) {
			UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolID);
			totalFreeUsageQuantity = totalFreeUsageQuantity.add(usagePool.getQuantityAsDecimal());
		}

        PriceModelWS percentageSelector = getTestPriceModel();

		planId = createPlanForMultpileUsagePool(percentageSelector, usagePoolsId, itemTypeId);

		PlanWS plan = api.getPlanWS(planId);
		Integer plansItemId = plan.getItemId();

		planOrderId = createPlanItemBasedOrderForMultpileUsagePool(userId, plansItemId, customerId);

		List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
		for (CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
			if (customerUsagePool.getUsagePoolId().intValue() == usagePoolsId[0].intValue() ||
				customerUsagePool.getUsagePoolId().intValue() == usagePoolsId[1].intValue())
				customerUsagePoolsId.add(customerUsagePool.getId());
		}
		Integer[] custUsagePoolsId = customerUsagePoolsId.toArray(new Integer[customerUsagePoolsId.size()]);

		Integer percentageOrderId = createPooledItemOrder(userId, metered_product_3, 2, metered_product_1, 4, metered_product_2, 3);
		// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
		testMultipleUsagePoolsWithMultpileOrderLines(custUsagePoolsId, usagePoolsId);
		api.deleteOrder(percentageOrderId);
	}

	@Test
	public void test016CleanUp() {
		logger.debug("Clean up");

		api.deleteItem(product_1);
		api.deleteItem(product_2);
		api.deleteItem(product_3);
		api.deleteItemCategory(itemTypeId_3);

		api.deleteItem(metered_product_3);
		api.deleteItemCategory(itemTypeId_2);

		api.deleteItem(metered_product_1);
		api.deleteItem(metered_product_2);
		api.deleteItemCategory(itemTypeId_1);
	}


	//create Plan Item Based Order
	protected Integer createPlanItemBasedOrderForMultpileUsagePool(Integer userId, Integer plansItemId, Integer customerId) {

		List<CustomerUsagePoolWS> customerUsagePools = null;

		OrderWS planItemBasedOrder = super.getUserSubscriptionToPlan(new Date(), userId, Constants.ORDER_BILLING_PRE_PAID, ORDER_PERIOD_MONTHLY, plansItemId, 1);
		Integer orderId = api.createOrder(planItemBasedOrder, OrderChangeBL.buildFromOrder(planItemBasedOrder, ORDER_CHANGE_STATUS_APPLY_ID));
		assertNotNull("Order Id cannot be null.", orderId);

		customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool not created", customerUsagePools.size() > 0);
        return orderId;
	}

	private Integer createPooledItemOrder(Integer userId, Integer MP_3, Integer MP_3_quantity,
										Integer MP_1, Integer MP_1_quantity,
										Integer MP_2, Integer MP_2_quantity) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(Constants.ORDER_PERIOD_ONCE); // once
		order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
		order.setActiveSince(new Date());

		OrderLineWS[] lines = new OrderLineWS[3];
		lines[0] = createOrderLine(MP_3, MP_3_quantity);
		lines[1] = createOrderLine(MP_1, MP_1_quantity);
		lines[2] = createOrderLine(MP_2, MP_2_quantity);

		order.setOrderLines(lines);

		Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID)); // create order
		assertNotNull("order created", orderId);
		return orderId;
	}

	private OrderLineWS createOrderLine(Integer itemId, Integer quantity) {
		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setItemId(itemId);
		line.setUseItem(true);
		line.setQuantity(quantity);

		return line;
	}

	protected void rateOrderAsserts(OrderWS order, String scenario, Integer[] custUsagePoolsId) {
		CustomerUsagePoolWS customerUsagePool = null;
		UsagePoolWS usagePool = null;

		if (scenario.equals("RATE_ORDER_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100,
			// per = (2/(12+10))*100 = 9.090909091; import Product 1, price => 20
			// 300 - 100 = 200,  200 * 5 = 1000, 1000+20 = 1020      --> Expected total

			assertEquals("Expected Order total: ", new BigDecimal("1020.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_2")) {
			// line 1 = 100, line 2 = 200, FUP Quantity = 100
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// (100+200)-100 => 300-100 = 200,
			// 200 * 5 = 1000, 1000+50 = 1050 --> Expected total

			assertEquals("Expected Order total: ", new BigDecimal("1050.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_4")) {
			// line 1 = 100, line 2 = 200, totalLineQuantity= 300;
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// FUP Quantity 1 = 100, FUP Quantity 2 = 200, totalFUP= 300;
			// (100+200)-(100+200) => 300-300 = 0, 0+50 = 50
			assertEquals("Expected Order total: ", new BigDecimal("50.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_5")) {
			// line= 50, totalLineQuantity= 50;
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// FUP Quantity = 100
			// (50-100) => 00 = 0, 0+50 = 50
			assertEquals("Expected Order total: ", new BigDecimal("50.00"), order.getTotalAsDecimal());
		}

		if (custUsagePoolsId.length == 1) {
			customerUsagePool = api.getCustomerUsagePoolById(custUsagePoolsId[0]);
			usagePool = api.getUsagePoolWS(customerUsagePool.getUsagePoolId());

			assertEquals("The free usage pool quantity & customer usage pool quantity must be same",
					usagePool.getQuantityAsDecimal(), customerUsagePool.getQuantityAsDecimal());
		} else {
			for (int i=0; i < custUsagePoolsId.length; i++) {
				customerUsagePool = api.getCustomerUsagePoolById(custUsagePoolsId[i]);
				usagePool = api.getUsagePoolWS(customerUsagePool.getUsagePoolId());

				assertEquals("The free usage pool quantity & customer usage pool quantity must be same",
						usagePool.getQuantityAsDecimal(), customerUsagePool.getQuantityAsDecimal());
			}
		}
	}

	protected void orderLinePriceAssert(OrderWS order, String scenario, BigDecimal removeOrderLineQuantity) {

		if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// 300 - 100 = 200,  200 * 5 = 1000, 1000+20 = 1020 --> Expected Order line amount

	    	assertEquals("Expected Order total: ", new BigDecimal("1020.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_2")) {
			// order line qty =	300, FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// 300 - 100 = 200,  200 * 5 = 1000, 1000+20 = 1020 --> Expected Order line amount

	    	assertEquals("Expected Order total: ", new BigDecimal("1020.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_3")) {
			// order line qty =	300, increased by 100, total=400, FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// 400 - 100 = 300,  300 * 5 = 1500, 1500+20 = 1520 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("1520.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_4")) {
			// order line qty =	300, reduce upto 100, total=100,  FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// 100 - 100 = 0,  0 * 5 = 0, 0+20 = 20 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("20.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_5")) {
			// order line 1 qty = 300, reduce qty upto 0, add new line qty = 200, total=200,  FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// (300-300+200) - 100 = 100,  100 * 5 = 500, 500+20 = 520 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("520.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_6")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 100, total=100,  FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// (300-300+100) - 100 = 0,  0 * 5 = 0, 0+20 = 20 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("20.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_7")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 50, total=50, FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// here order line qunatity less than free usage pool,
			// 0 * 5 = 0, 0+20 = 20 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("20.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_8")) {
			// Multiple order lines, we don't know which line use free usage pool & which not.
			// order line 1 = 100, line 2 = 200, total=300, FUP Quantity = 100
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// (100+200) - 100 = 200,  200 * 5 = 1000, 1000+50 = 1050 --> Expected Order line amount

	    	assertEquals("Expected Order total: ", new BigDecimal("1050.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_9")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// 600 - 100 = 500,  500 * 5 = 2500, 2500+50 = 2550 --> Expected Order line amount

	    	assertEquals("Expected Order total: ", new BigDecimal("2550.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_10")) {
			// Removed order line which does not have usage pool
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// 600 - 100 = 500,  totalQuantity = 500

			BigDecimal remainingQuantity = new BigDecimal(500).subtract(removeOrderLineQuantity);
			BigDecimal expectedTotal = remainingQuantity.multiply(new BigDecimal("5.00"), MathContext.DECIMAL128);
			expectedTotal = expectedTotal.add(new BigDecimal(50));
			assertEquals("Expected Order total: ", expectedTotal, order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_13")) {
			// order line qty =	300, FUP Quantity = 100
			// per = (2/(12+10))*100 = 9.090909091; import Product_1, price => 20
			// 300 - 100 = 200,  200 * 5 = 1000, 1000+20 = 1020 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("1020.00") , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_14")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, totalQuantity =600;
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// FUP 1 = 100, FUP 2 = 200  totalFUP =300;
			// 600 - 300 = 300,  300 * 5 = 1500,  1500+50 = 1550 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("1550.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_15")) {
			// order line qty =	50, increased by 200, total=250, FUP Quantity = 100
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// 250 - 100 = 150,  150*5 = 750,  750+50 = 800 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("800.00"), order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_16")) {
			// order line qty =	50, decresed by 30, total=230, FUP Quantity = 100
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// 220 - 100 = 120,  120*5 = 600, 600+50=650 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("650.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_17")) {
			// order line qty =	220, increased by 50, total=270, FUP Quantity = 100
			// per = (2/(4+3))*100 = 28.571428571; import Product_3, price => 50
			// 270 - 100 = 170,  170*5 = 850, 850+50=900 --> Expected Order line amount

			assertEquals("Expected Order total: ", new BigDecimal("900.00"), order.getTotalAsDecimal());
		}
	}

    @Override
    protected PriceModelWS getTestPriceModel() {
        initialize();
        PriceModelWS percentageSelector = new PriceModelWS(PriceModelStrategy.ITEM_PERCENTAGE_SELECTOR.name(), new BigDecimal("5.00"), Constants.PRIMARY_CURRENCY_ID);
        percentageSelector.addAttribute("typeId", itemTypeId_2.toString());	// Selection category
        percentageSelector.addAttribute("percentOfTypeId", itemTypeId_1.toString());	// Percentage category
        percentageSelector.addAttribute("0", product_1.toString());	// add item product_1 when >= 0 & < 10 percentage
        percentageSelector.addAttribute("10", product_2.toString());	// add item product_2 when >= 10 & < 20 percentage
        percentageSelector.addAttribute("20", product_3.toString());	// add item product_3 when >= 20 percentage

        return percentageSelector;
    }
}
