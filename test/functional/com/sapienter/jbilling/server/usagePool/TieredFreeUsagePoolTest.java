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
 * TieredFreeUsagePoolTest
 * Test class for Tiered with FUP scenarios.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

@Test(groups = { "usagePools" }, testName = "TieredFreeUsagePoolTest")
public class TieredFreeUsagePoolTest extends AbstractFreeUsagePoolPricingTest {

	public TieredFreeUsagePoolTest() {
		logger.debug("TieredFreeUsagePoolTest");
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

		PriceModelWS priceModel = getTestPriceModel();

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

		planId = createPlanForMultpileUsagePool(priceModel, usagePoolsId, itemTypeId);
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
		logger.debug("custUsagePoolsId :: {}", custUsagePoolsId.length);
		testMultipleUsagePoolsWithMultpileOrderLines(custUsagePoolsId, usagePoolsId);
	}

	
	protected void rateOrderAsserts(OrderWS order, String scenario, Integer[] custUsagePoolsId) {
		CustomerUsagePoolWS customerUsagePool = null;
		UsagePoolWS usagePool = null;
		
		if (scenario.equals("RATE_ORDER_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200, 
			// (100 * 1)+(100*0.75) = 100+75 = 175,  --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal("175.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_2")) {
			// line 1 = 100, line 2 = 200, FUP Quantity = 100
			// (100+200)-100 => 300-100 = 200, 
			// (100 * 1)+(100*0.75) = 100+75 = 175,  --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal("175.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_3")) {
			// line 1 = 50, line 2 = 100, FUP Quantity = 100
			// (50+100)-100 => 150-100 = 50, 
			// 50 * 1 = 50 --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal("50.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_4")) {
			// line 1 = 100, line 2 = 200, totalLineQuantity= 300; 
			// FUP Quantity 1 = 100, FUP Quantity 2 = 200, totalFUP= 300;  
			// (100+200)-(100+200) => 300-300 = 0, 
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_5")) {
			// order line qty =	50, FUP Quantity = 100
			// 50 - 100 = 50, 
			// (50 * 0) = 00.00,  --> Expected total
			
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
			// 300 - 100 = 200, (100*1)+(100*0.75) = (100+75) = 175 --> Expected Order line amount
			// 175/300 = 0.583333333 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.583333333"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("175.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_2")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200, (100*1)+(100*0.75) = (100+75) = 175 --> Expected Order line amount
			// 175/300 = 0.583333333 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.583333333"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("175.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_3")) {
			// order line qty =	300, increased by 100, total=400, FUP Quantity = 100
			// 400 - 100 = 300,  (100*1)+(100*0.75)+(100*0.5)= (100+75+50) = 225 --> Expected Order line amount
			// 225/400 = 0.5625 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.5625"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("225.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_4")) {
			// order line qty =	300, reduce upto 100, total=100,  FUP Quantity = 100
			// 100 - 100 = 0,  0 * 1.00 = 0 --> Expected Order line amount
			// 0/300 = 0  ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_5")) {
			// order line 1 qty = 300, reduce qty upto 0, add new line qty = 200, total=200,  FUP Quantity = 100
			// (300-300+200) - 100 = 100,  100 * 1.00 = 100 --> Expected Order line amount
			// 100/200 = 0.5   ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.5"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("100.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_6")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 100, total=100,  FUP Quantity = 100
			// (300-300+100) - 100 = 0,  0 * 1.00 = 0 --> Expected Order line amount
			// 0/300 = 0   ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_7")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 50, total=50, FUP Quantity = 100
			// here order line qunatity less than free usage pool, 
			// so Expected line Price = 0.00 & Order line amount = 0.00 
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_8")) {
			// Multiple order lines, we don't know which line use free usage pool & which not.
			// order line 1 = 100, line 2 = 200, total=300, FUP Quantity = 100
			// (100+200) - 100 = 200, (100*1)+(100*0.75)= (100+75) = 175 --> Expected Order line amount
			
	    	assertEquals("Expected Order total: ", new BigDecimal("175.00") , order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_9")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100
			// 600 - 100 = 500,  
			// (100*1)+(100*0.75)+(300*0.5) = (100+75+150) = 325 --> Expected Order line amount
			
	    	assertEquals("Expected Order total: ", new BigDecimal("350.00") , order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_10")) {
			// Removed order line which does not have usage pool
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100
			// 600 - 100 = 500,  totalQuantity = 500-200 = 300 
			// (100*1)+(100*0.75) = (100+75) = 175 
			BigDecimal remainingQuantity = new BigDecimal(300).subtract(removeOrderLineQuantity);
			
			BigDecimal remainingTotal = remainingQuantity.multiply(new BigDecimal(0.5), MathContext.DECIMAL128);
			BigDecimal expectedTotal = new BigDecimal("175.00").add(remainingTotal);
			assertEquals("Expected Order total: ", expectedTotal, order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_13")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200,  (100*1)(100*0.75) = (100_+75) = 175 --> Expected Order line amount
			// 175/300 = 0.583333333 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.583333333"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("175.00"), orderLine.getAmountAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_14")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, totalQuantity =600; 
			// FUP 1 = 100, FUP 2 = 200  totalFUP =300; 
			// 600 - 300 = 300, (100*1)+(100*0.75)+(100*0.5) = (100+75+50) = 225 --> Expected Order line amount
			assertEquals("Expected Order total: ", new BigDecimal("450.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_15")) {
			// order line qty =	50, increased by 200, total=250, FUP Quantity = 100
			// 250 - 100 = 150,  (100*1)+(50*0.75) = (100+37.5) = 137.5 --> Expected Order line amount
			// 137.5/250 = 0.55 ------> Expected Order line Price
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.55"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("137.5"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_16")) {
			// order line qty =	50, decrased by 30, total=220, FUP Quantity = 100
			// 220 - 100 = 120,  (100*1)+(20*0.75) = (100+37.5) = 115 --> Expected Order line amount
			// 115/250 = 0.522727273 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.522727273"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("115.00"), orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_17")) {
			// order line qty =	220, increased by 50, total=270, FUP Quantity = 100
			// 270 - 100 = 170,  (100*1)+(70*0.75) = (100+52.5) = 152.5 --> Expected Order line amount
			// 152.5/270 = 0.564814815 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.564814815"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("152.5"), orderLine.getAmountAsDecimal());
	    	
		}
	}

    @Override
    protected PriceModelWS getTestPriceModel() {
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TIERED.name(), null, Constants.PRIMARY_CURRENCY_ID);
        priceModel.addAttribute("0", "1.00");	// 0 - 100 = 100 @$1
        priceModel.addAttribute("100", "0.75");		// 101 - 200 = 200 @$0.75
        priceModel.addAttribute("200", "0.50"); 	// more than 200 = @$0.50
        return priceModel;
    }
}
