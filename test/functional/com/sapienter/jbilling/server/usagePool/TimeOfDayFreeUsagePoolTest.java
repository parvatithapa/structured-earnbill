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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.*;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import org.joda.time.LocalTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;

/**
 * TimeOfDayFreeUsagePoolTest
 * Test class for Time of day with FUP scenarios.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

@Test(groups = { "usagePools" }, testName = "TimeOfDayFreeUsagePoolTest")
public class TimeOfDayFreeUsagePoolTest extends AbstractFreeUsagePoolPricingTest {

	public TimeOfDayFreeUsagePoolTest() {
		logger.debug("TimeOfDayFreeUsagePoolTest");
	}

	private SortedMap<LocalTime, BigDecimal> prices = new TreeMap<LocalTime, BigDecimal>();
	private LocalTime now = LocalTime.fromDateFields(new Date());
	
	@Test
	public void test001CreateOrderForFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testCreateOrderForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test002EditOrderWithoutChangeQuantityForFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testEditOrderWithoutChangeQuantityForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test003EditOrderAndIncreaseQuantityForFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testEditOrderAndIncreaseQuantityForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test004ReducingQuantityUptoFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testReducingQuantityUptoFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test005OrderLineQuantityZeroForFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testOrderLineQuantityZeroForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test006AddNewLineWithQuantityGreaterThanFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testAddNewLineWithQuantityGreaterThanFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test007AddNewLineWithQuantityEqualToFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testAddNewLineWithQuantityEqualToFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test008AddNewLineWithQuantityLessThanFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testAddNewLineWithQuantityLessThanFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test009CreateOrderWithTwoLinesForFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testCreateOrderWithTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test010EditOrderAddTwoLinesForFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testEditOrderAddTwoLinesForFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test011EditOrderRemoveLineWhichDoesNotUseFreeUsagePool() throws Exception {
		now = LocalTime.fromDateFields(new Date());
		super.testEditOrderRemoveLineWhichDoesNotUseFreeUsagePool(customerUsagePoolId, usagePoolId);
	}
	
	@Test
	public void test012DeleteOrder() throws Exception {
		now = LocalTime.fromDateFields(new Date());
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
	public void test015MultipleUsagePoolsWithMultpileOrderLines() throws Exception {
		
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
		now = LocalTime.fromDateFields(new Date());
		testMultipleUsagePoolsWithMultpileOrderLines(custUsagePoolsId, usagePoolsId);
	}
	
	protected void rateOrderAsserts(OrderWS order, String scenario, Integer[] custUsagePoolsId) {
		CustomerUsagePoolWS customerUsagePool = null;
		UsagePoolWS usagePool = null;
		BigDecimal orderLinePrice = BigDecimal.ZERO;
		BigDecimal orderTotal = BigDecimal.ZERO;
		
		for (LocalTime time : prices.keySet()) {
            if (now.isEqual(time) || 
        		now.isAfter(time)) {
            	orderLinePrice = prices.get(time);
            }
        }
		
		if (scenario.equals("RATE_ORDER_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200,
			
			orderTotal = new BigDecimal("200.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", orderTotal , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_2")) {
			// line 1 = 100, line 2 = 200, FUP Quantity = 100
			// (100+200)-100 => 300-100 = 200,
			
			orderTotal = new BigDecimal("200.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", orderTotal , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_3")) {
			// line 1 = 50, line 2 = 100, FUP Quantity = 100
			// (50+100)-100 => 150-100 = 50, 
			
			orderTotal = new BigDecimal("50.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", orderTotal , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_4")) {
			// line 1 = 100, line 2 = 200, totalLineQuantity= 300; 
			// FUP Quantity 1 = 100, FUP Quantity 2 = 200, totalFUP= 300;  
			// (100+200)-(100+200) => 300-300 = 0,
			orderTotal = new BigDecimal("0.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_5")) {
			// order line qty =	50, FUP Quantity = 100
			// 50 - 100 = 00,
			
			orderTotal = new BigDecimal("00.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", orderTotal , order.getTotalAsDecimal());
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
		
		BigDecimal orderLinePrice = BigDecimal.ZERO;
		BigDecimal orderTotal = BigDecimal.ZERO;
		BigDecimal averagePrice = BigDecimal.ZERO;
		
		for (LocalTime time : prices.keySet()) {
            if (now.isEqual(time) || 
        		now.isAfter(time)) {
            	orderLinePrice = prices.get(time);
            }
        }
		
		if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200,  remaining quantity = 200, total quantity = 300
			
			orderTotal = new BigDecimal("200.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("300.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_2")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200,  remaining quantity = 200, total quantity = 300
			
			orderTotal = new BigDecimal("200.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("300.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_3")) {
			// order line qty =	300, increased by 100, total=400, FUP Quantity = 100
			// 400 - 100 = 300,  remaining quantity = 300, total quantity = 400
			
			orderTotal = new BigDecimal("300.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("400.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_4")) {
			// order line qty =	300, reduce upto 100, total=100,  FUP Quantity = 100
			// 100 - 100 = 0,  remaining quantity = 0, total = 100
			
			orderTotal = new BigDecimal("0.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("100.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_5")) {
			// order line 1 qty = 300, reduce qty upto 0, add new line qty = 200, total=200,  FUP Quantity = 100
			// (300-300+200) - 100 = 100,  remaining quantity = 100, total quantity = 200
			
			orderTotal = new BigDecimal("100.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("200.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_6")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 100, total=100,  FUP Quantity = 100
			// (300-300+100) - 100 = 0,  remaining quantity = 0, total quantity = 100
			
			orderTotal = new BigDecimal("0.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("100.00"), MathContext.DECIMAL128);
			
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
			// order line 1 = 100, line 2 = 200, total=300, FUP Quantity = 100
			// (100+200) - 100 = 200,  remaining quantity = 200, total quantity = 300
			
			orderTotal = new BigDecimal("200.00").multiply(orderLinePrice, MathContext.DECIMAL128);
	    	assertEquals("Expected Order total: ", orderTotal, order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_9")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100
			// 600 - 100 = 500,  remaining quantity = 500, total quantity = 600
			
			orderTotal = new BigDecimal("500.00").multiply(orderLinePrice, MathContext.DECIMAL128);
	    	assertEquals("Expected Order total: ", orderTotal, order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_10")) {
			// Removed order line which does not have usage pool
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, FUP Quantity = 100
			// 600 - 100 = 500,  totalQuantity = 500
			
			BigDecimal remainingQuantity = new BigDecimal(500).subtract(removeOrderLineQuantity);
			orderTotal = remainingQuantity.multiply(orderLinePrice, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", orderTotal, order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_13")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200,  remaining quantity = 200, total quantity = 300 
			
			orderTotal = new BigDecimal("200.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("300.00"), MathContext.DECIMAL128);
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_14")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, totalQuantity =600; 
			// FUP 1 = 100, FUP 2 = 200  totalFUP =300; 
			// 600 - 300 = 300,  remaining quantity = 300, total quantity = 600
			
			orderTotal = new BigDecimal("300.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			assertEquals("Expected Order total: ", orderTotal, order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_15")) {
			// order line qty =	50, increased by 200,  FUP Quantity = 100
			// 250 - 100 = 150,  remaining quantity = 150, total quantity = 250
			
			orderTotal = new BigDecimal("150.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("250.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_16")) {
			// order line qty =	250, decreased by 30,  FUP Quantity = 100
			// 220 - 100 = 120,  remaining quantity = 120, total quantity = 220
			
			orderTotal = new BigDecimal("120.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("220.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_17")) {
			// order line qty =	220, increased by 50,  FUP Quantity = 100
			// 270 - 100 = 170,  remaining quantity = 170, total quantity = 270
			
			orderTotal = new BigDecimal("170.00").multiply(orderLinePrice, MathContext.DECIMAL128);
			averagePrice = orderTotal.divide(new BigDecimal("270.00"), MathContext.DECIMAL128);
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", averagePrice, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", orderTotal, orderLine.getAmountAsDecimal());
	    	
		}
	}

    @Override
    protected PriceModelWS getTestPriceModel() {
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TIME_OF_DAY.name(), new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID);
        priceModel.addAttribute("date_field", "event_date");
        priceModel.addAttribute("00:00", "10.00");
        priceModel.addAttribute("12:00", "20.00");
        priceModel.addAttribute("18:00", "25.00");

        for (Map.Entry<String, String> entry : priceModel.getAttributes().entrySet()) {
            if (entry.getKey().contains(":")) {
                prices.put(AttributeUtils.parseTime(entry.getKey()), AttributeUtils.parseDecimal(entry.getValue()));
            }
        }

        return priceModel;
    }
}
