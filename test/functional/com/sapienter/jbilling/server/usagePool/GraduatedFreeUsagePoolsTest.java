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

import java.math.BigDecimal;
import java.math.MathContext;
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
 * GraduatedFreeUsagePoolsTest
 * Test class for Graduated with FUP scenarios.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

@Test(groups = { "usagePools" }, testName = "GraduatedFreeUsagePoolsTest")
public class GraduatedFreeUsagePoolsTest extends AbstractFreeUsagePoolPricingTest {

	public GraduatedFreeUsagePoolsTest() {
		logger.debug("GraduatedFreeUsagePoolsTest");
	}

	private static final BigDecimal ORDER_LINE_PRICE = new BigDecimal("0.50");
	
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
		//super.testCreateOrderWithTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test010EditOrderAddTwoLinesForFreeUsagePool() throws Exception {
		//super.testEditOrderAddTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test011EditOrderRemoveLineWhichDoesNotUseFreeUsagePool() throws Exception {
		//super.testEditOrderRemoveLineWhichDoesNotUseFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test012DeleteOrder() throws Exception {
		super.testDeleteOrder(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test013EditOrderLineWithFUPQuantity() throws Exception {
		super.testEditOrderLineWithFUPQuantity(customerUsagePoolId, usagePoolId);
	}

    @Test
    public void test014CleanUp() {
        logger.debug("Clean up");
        cleanUp(api);
    }
	
	@Test
	public void test014MultipleUsagePoolsWithMultpileOrderLines() throws Exception {

		PriceModelWS graduatedPrice = getTestPriceModel();

        userId = createUser("MultipleFUPCustomer");
        UserWS user = api.getUserWS(userId);
        customerId = user.getCustomerId();

		Integer[] usagePoolsId = new Integer[2];
		Set<Integer> customerUsagePoolsId = new HashSet<Integer>();

        usagePoolsId[0] = usagePoolId = createFreeUsagePool("100 Units Free", new BigDecimal(100), new Integer[]{itemTypeId}, new Integer[]{itemId});
		usagePoolsId[1] = usagePoolId2 = createFreeUsagePool("200 Local Calls Mins ", new BigDecimal(200), new Integer[]{itemTypeId}, new Integer[] {itemId});


		BigDecimal totalFreeUsageQuantity = BigDecimal.ZERO;
		for (Integer usagePoolID: usagePoolsId) {
			UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolID);
			totalFreeUsageQuantity = totalFreeUsageQuantity.add(usagePool.getQuantityAsDecimal());
		}

		planId = createPlanForMultpileUsagePool(graduatedPrice, usagePoolsId, itemTypeId);
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
		
		if (scenario.equals("RATE_ORDER_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100, Included Quantity=100
			// (300-100)-100 = 100,  100 * 0.50 = 50 --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal("50.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_2")) {
			// line 1 = 100, line 2 = 200, FUP Quantity = 100, Included Quantity=100
			// (((100+200) -100) -100)  => 200-100 = 100, 
			// 100 * 0.50 = 50 --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal("50.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_3")) {
			// line 1 = 50, line 2 = 100, FUP Quantity = 100, Included Quantity=100
			// (((50+100) -100) -100) => ((150-100) -100) = -50 
			// 0 * 0.50 = 0.00 --> Expected total
			
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_4")) {
			// line 1 = 100, line 2 = 200, totalLineQuantity= 300; 
			// FUP Quantity 1 = 100, FUP Quantity 2 = 200, totalFUP= 300; Included Quantity=100  
			// ((100+200)-(100+200) -100)=> ((300-300) -100)= -100, 
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_5")) {
			// order line qty =	50, FUP Quantity = 100, Included Quantity=100
			// (50-100)-100 = 00,  50 * 0.50 = 00 --> Expected total
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
			// order line qty =	300, FUP Quantity = 100, Included Quantity=100
			// ((300-100) -100) = 100,  100 * 0.50 = 50 --> Expected Order line amount
			// 50/300 = 0.166666667 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.166666667"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("50.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_2")) {
			// order line qty =	300, FUP Quantity = 100, Included Quantity=100
			// ((300-100) -100) = 100,  100 * 0.50 = 50 --> Expected Order line amount
			// 50/300 = 0.166666667 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.166666667"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("50.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_3")) {
			// order line qty =	300, increased by 100, total=400, FUP Quantity = 100, Included Quantity=100
			// ((400 -100) -100) = 200,  200 * 0.50 = 100 --> Expected Order line amount
			// 100/400 = 0.25 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.25"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("100.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_4")) {
			// order line qty =	300, reduce upto 100, total=100,  FUP Quantity = 100, Included Quantity=100
			// ((100 -100) -100) = -100,  0 * 0.50 = 0 --> Expected Order line amount
			// 0/300 = 0  ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_5")) {
			// order line 1 qty = 300, reduce qty upto 0, add new line qty = 200, total=200,  
			// FUP Quantity = 100, Included Quantity=100
			// (((300-300+200) -100) -100) = 0,  0 * 0.50 = 0.00 --> Expected Order line amount
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_6")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 100, total=100,  
			// FUP Quantity = 100, Included Quantity=100
			// (((300-300+100) -100) -100)= -100,  0 * 0.50 = 0 --> Expected Order line amount
			// 0/300 = 0   ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_7")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 50, total=50, 
			// FUP Quantity = 100, Included Quantity=100
			// here order line qunatity less than free usage pool, 
			// so Expected line Price = 0.00 & Order line amount = 0.00 
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_8")) {
			// Multiple order lines, we don't know which line use free usage pool & which not.
			// order line 1 = 100, line 2 = 200, total=300, FUP Quantity = 100, Included Quantity=100
			// (((100+200) -100) -100) = 100,  100 * 0.50 = 50 --> Expected Order line amount
			
	    	assertEquals("Expected Order total: ", new BigDecimal("50.00") , order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_9")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, 
			// FUP Quantity = 100, Included Quantity=100
			// ((600 -100) -100) = 400,  400 * 0.50 = 200 --> Expected Order line amount
			// 200/600 = 0.333333333   ------> Expected Order line Price
			
	    	assertEquals("Expected Order total: ", new BigDecimal("200.00") , order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_10")) {
			// Removed order line which does not have usage pool
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, 
			// FUP Quantity = 100, Included Quantity=100
			// ((600 -100) -100) = 400,  totalQuantity = 400
			
			BigDecimal remainingQuantity = new BigDecimal("400.00").subtract(removeOrderLineQuantity);
			BigDecimal expectedTotal = remainingQuantity.multiply(ORDER_LINE_PRICE, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", expectedTotal, order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_13")) {
			// order line qty =	300, FUP Quantity = 100, Included Quantity=100
			// ((300 -100) -100) = 100,  100 * 0.50 = 50 --> Expected Order line amount
			// 50/300 = 0.166666667 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.166666667"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("50.00"), orderLine.getAmountAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_14")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, totalQuantity =600; 
			// FUP 1 = 100, FUP 2 = 200  totalFUP =300; Included Quantity=100
			// ((600 -300) -100) = 200,  200 * 0.50 = 100 --> Expected Order line amount
			assertEquals("Expected Order total: ", new BigDecimal("200.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_15")) {
			// order line qty =	50, increased by 200, total=250, FUP Quantity = 100, Included Quantity=100
			// (250 - 100)-100 = 50,  50 * 0.50 = 25 --> Expected Order line amount
			// 25/250 = 0.1 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.1"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("25.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_16")) {
			// order line qty =	250, decrased by 30, total=220, FUP Quantity = 100, Included Quantity=100
			// (220 - 100)-100 = 20,  20 * 0.50 = 10 --> Expected Order line amount
			// 10/220 = 0.0454545455 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.0454545455"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("10"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_17")) {
			// order line qty =	220, increased by 50, total=270, FUP Quantity = 100, Included Quantity=100
						// (270 - 100)-100 = 70,  70 * 0.50 = 35 --> Expected Order line amount
						// 35/270 = 0.1296296297 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.1296296297"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("35.00"), orderLine.getAmountAsDecimal());
	    	
		}
	}

    @Override
    protected PriceModelWS getTestPriceModel() {
        PriceModelWS graduatedPrice = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), ORDER_LINE_PRICE, Constants.PRIMARY_CURRENCY_ID);
        graduatedPrice.addAttribute("included", "100");  // 100 units included;
        return graduatedPrice;
    }
}
