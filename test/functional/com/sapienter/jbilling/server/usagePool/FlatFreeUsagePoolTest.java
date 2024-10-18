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

import java.math.MathContext;
import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;

/**
 * FlatFreeUsagePoolTest
 * Test class for ZERO pricing with FUP scenarios.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

@Test(groups = { "usagePools" }, testName = "FlatFreeUsagePoolTest")
public class FlatFreeUsagePoolTest extends AbstractFreeUsagePoolPricingTest {

	public FlatFreeUsagePoolTest() {
		logger.debug("FlatFreeUsagePoolTest");
	}

	@Test
	public void test001CreateOrderForFreeUsagePool() throws Exception {
		super.testCreateOrderForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test002EditOrderWithoutChangeQuantityForFreeUsagePool() throws Exception {
		super.testEditOrderWithoutChangeQuantityForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test003EditOrderAndIncreaseQuantityForFreeUsagePool() throws Exception {
		super.testEditOrderAndIncreaseQuantityForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test004ReducingQuantityUptoFreeUsagePool() throws Exception {
		super.testReducingQuantityUptoFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test005OrderLineQuantityZeroForFreeUsagePool() throws Exception {
		super.testOrderLineQuantityZeroForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test006AddNewLineWithQuantityGreaterThanFreeUsagePool() throws Exception {
		super.testAddNewLineWithQuantityGreaterThanFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test007AddNewLineWithQuantityEqualToFreeUsagePool() throws Exception {
		super.testAddNewLineWithQuantityEqualToFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test008AddNewLineWithQuantityLessThanFreeUsagePool() throws Exception {
		super.testAddNewLineWithQuantityLessThanFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test009CreateOrderWithTwoLinesForFreeUsagePool() throws Exception {
		super.testCreateOrderWithTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test010EditOrderAddTwoLinesForFreeUsagePool() throws Exception {
		super.testEditOrderAddTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test011EditOrderRemoveLineWhichDoesNotUseFreeUsagePool() throws Exception {
		super.testEditOrderRemoveLineWhichDoesNotUseFreeUsagePool(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test012DeleteOrder() throws Exception {
		super.testDeleteOrder(customerUsagePoolId, usagePoolId);
	}

	@Test
	public void test013CleanUp() {
		logger.debug("Clean up");
		cleanUp(api);
	}

	@Test
	public void test014MultipleUsagePoolsWithMultpileOrderLines() throws Exception {

		PriceModelWS flatPrice = getTestPriceModel();

		userId = createUser("MultipleFUPCustomer");
		UserWS user = api.getUserWS(userId);
		customerId = user.getCustomerId();

		Integer[] usagePoolsId = new Integer[2];
		Set<Integer> customerUsagePoolsId = new HashSet<Integer>();

		usagePoolsId[0] = usagePoolId = createFreeUsagePool("100 Units Free", new BigDecimal(100), new Integer[]{itemTypeId}, new Integer[]{itemId});
		usagePoolsId[1] = usagePoolId2 = createFreeUsagePool("200 Units Free", new BigDecimal(200), new Integer[]{itemTypeId}, new Integer[]{itemId});

		BigDecimal totalFreeUsageQuantity = BigDecimal.ZERO;
		for (Integer usagePoolID: usagePoolsId) {
			UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolID);
			totalFreeUsageQuantity = totalFreeUsageQuantity.add(usagePool.getQuantityAsDecimal());
		}

		planId = createPlanForMultpileUsagePool(flatPrice, usagePoolsId, itemTypeId);
		PlanWS plan = api.getPlanWS(planId);
		Integer plansItemId = plan.getItemId();

		planOrderId = createPlanItemBasedOrder(userId, plansItemId, customerId);

		List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
		for (CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
			if (customerUsagePool.getUsagePoolId().intValue() == usagePoolsId[0].intValue() ||
					customerUsagePool.getUsagePoolId().intValue() == usagePoolsId[1].intValue())
				customerUsagePoolsId.add(customerUsagePool.getId());
		}
		Integer[] custUsagePoolsId = customerUsagePoolsId.toArray(new Integer[customerUsagePoolsId.size()]);
		testMultipleUsagePoolsWithMultpileOrderLines(custUsagePoolsId, usagePoolsId);
	}

	protected void rateOrderAsserts(OrderWS order, String scenario, Integer[] custUsagePoolsId) {
		CustomerUsagePoolWS customerUsagePool = null;
		UsagePoolWS usagePool = null;
		// Free usage pool quantity not consider for Flat pricing strategy.
		if (scenario.equals("RATE_ORDER_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 * 0.0 = 0.00 --> Expected total

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_2")) {
			// line 1 = 100, line 2 = 200, FUP Quantity = 100
			// (100+200) => 300, 
			// 300 * 0.00 = 100 --> Expected total

			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_3")) {
			// line 1 = 50, line 2 = 100, FUP Quantity = 100
			// (50+100) => 150, 
			// 150 * 0.00 = 00 --> Expected total

			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_4")) {
			// line 1 = 100, line 2 = 200, totalLineQuantity= 300; 
			// FUP Quantity 1 = 100, FUP Quantity 2 = 200, totalFUP= 300;  
			// (100+200) => 300,  300 * 0.00= 0.00, 
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
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
			// 300 * 0.00 = 0.00 --> Expected Order line amount

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_2")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 * 0.00 = 0.00 --> Expected Order line amount

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_3")) {
			// order line qty =	300, increased by 100, total=400, FUP Quantity = 100
			// 400  * 0.00 = 0.00 --> Expected Order line amount

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_4")) {
			// order line qty =	300, reduce upto 100, total=100,  FUP Quantity = 100
			// 100 * 0.00 = 0.00 --> Expected Order line amount

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_5")) {
			// order line 1 qty = 300, reduce qty upto 0, add new line qty = 200, total=200,  FUP Quantity = 100
			// (300-300+200) = 200,  200 * 0.00 = 0.00 --> Expected Order line amount

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_6")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 100, total=100,  FUP Quantity = 100
			// (300-300+100) = 100,  100 * 0.00 = 0 --> Expected Order line amount

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_7")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 50, total=50, FUP Quantity = 100
			// 50 * 0.00 = 0.00 

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_8")) {
			// Multiple order lines, we don't know which line use free usage pool & which not.
			// order line 1 = 100, line 2 = 200, total=300, FUP Quantity = 100

			for (OrderLineWS orderLine: order.getOrderLines()) {
				assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
				assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			}
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_9")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100
			// 600 * 0.00 = 0.00 --> Expected Order line amount

			for (OrderLineWS orderLine: order.getOrderLines()) {
				assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
				assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			}
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_10")) {
			// Removed order line which does not have usage pool
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100

			for (OrderLineWS orderLine: order.getOrderLines()) {
				assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
				assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			}
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_13")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 * 0.00 = 0.00 --> Expected Order line amount

			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
			assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_14")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, totalQuantity =600; 
			// FUP 1 = 100, FUP 2 = 200  totalFUP =300; 
			// 600 * 0.00 = 0.00 --> Expected Order line amount
			for (OrderLineWS orderLine: order.getOrderLines()) {
				assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
				assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
			}
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		}
	}

	@Override
	protected PriceModelWS getTestPriceModel(){
		return new PriceModelWS(PriceModelStrategy.ZERO.name(), null, Constants.PRIMARY_CURRENCY_ID);
	}

}
