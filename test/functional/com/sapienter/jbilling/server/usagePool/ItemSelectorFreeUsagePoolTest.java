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
import static junit.framework.TestCase.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;

/**
 * ItemSelectorFreeUsagePoolTest
 * Test class for Item Selector with FUP scenarios.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

@Test(groups = { "usagePools" }, testName = "ItemSelectorFreeUsagePoolTest")
public class ItemSelectorFreeUsagePoolTest extends AbstractFreeUsagePoolPricingTest {

	public ItemSelectorFreeUsagePoolTest() {
		logger.debug("ItemSelectorFreeUsagePoolTest");
	}

	private Integer categoryId = null;
	private Integer mailBoxItemId_1 = null;
	private Integer mailBoxItemId_2 = null;
	private Integer mailBoxItemId_3 = null;
	
	private SortedMap<Integer, BigDecimal> prices = new TreeMap<Integer, BigDecimal>();

    private void initialize() {

        // products we want to include in third caregory
        if(null == categoryId){
            categoryId = createItemType("Category_1_");
        }

		// category 3 product
        if(null == mailBoxItemId_1){
            mailBoxItemId_1 = createItem("up to 10 Mailboxes", "up to 10 Mailboxes", new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("25.00"), Constants.PRIMARY_CURRENCY_ID), categoryId);
        }
        if(null == mailBoxItemId_2){
            mailBoxItemId_2 = createItem("up to 25 Mailboxes", "up to 25 Mailboxes", new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("50.00"), Constants.PRIMARY_CURRENCY_ID), categoryId);
        }
        if(null == mailBoxItemId_3){
            mailBoxItemId_3 = createItem("unlimited Mailboxes", "unlimited Mailboxes", new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("100.00"), Constants.PRIMARY_CURRENCY_ID), categoryId);
        }
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
		
		PriceModelWS selector = getTestPriceModel();

		planId = createPlanForMultpileUsagePool(selector, usagePoolsId, itemTypeId);
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
		
		testMultipleUsagePoolsWithMultpileOrderLines(custUsagePoolsId, usagePoolsId);
	}
	
	@Test
	public void test016CleanUp() {
		logger.debug("Clean up");
		api.deleteItem(mailBoxItemId_1);
		api.deleteItem(mailBoxItemId_2);
		api.deleteItem(mailBoxItemId_3);
        api.deleteItemCategory(categoryId);
	}
	
	protected void rateOrderAsserts(OrderWS order, String scenario, Integer[] custUsagePoolsId) {
		CustomerUsagePoolWS customerUsagePool = null;
		UsagePoolWS usagePool = null;
		
		if (scenario.equals("RATE_ORDER_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100, total quantity = 300, import up to 25 Mailboxes Item
			// 300 - 100 = 200,  200 * 10 = 2000
			// 2000+50 = 2050  --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal(2050) , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_2")) {
			// line 1 = 100, line 2 = 200, FUP Quantity = 100,  total quantity = 300, import up to 25 Mailboxes Item
			// (100+200)-100 => 300-100 = 200, 
			// 200 * 10 = 2000,  2000+50 = 2050 --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal(2050) , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_3")) {
			// line 1 = 50, line 2 = 100, FUP Quantity = 100, total quantity = 150, import up to 10 Mailboxes Item
			// (50+100)-100 => 150-100 = 50, 
			// 50 * 10 = 500, 500+25= 525 --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal(525) , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_4")) {
			// line 1 = 100, line 2 = 200, totalLineQuantity= 300; total quantity = 300, import up to 25 Mailboxes Item 
			// FUP Quantity 1 = 100, FUP Quantity 2 = 200, totalFUP= 300;  
			// (100+200)-(100+200) => 300-300 = 0, 0+50 = 50 
			assertEquals("Expected Order total: ", new BigDecimal(50), order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_5")) {
			// order line qty =	50, FUP Quantity = 100, total quantity = 50, import up to 10 Mailboxes Item
			// 50 - 100 = -50,  -50 * 00 = 00.00
			// 00.00+25 = 25  --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal(25) , order.getTotalAsDecimal());
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

		OrderLineWS orderLine = null;
		if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100, total quantity = 300, import up to 25 Mailboxes Item
			// 300 - 100 = 200,  200 * 10 = 2000 --> Expected Order line amount
			// 2000/300 = 6.666666667 ------> Expected Order line Price
			// 2000+50 = 2050  --> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("6.666666667"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("2000.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("2050.00"), order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_2")) {
			// order line qty =	300, FUP Quantity = 100, total quantity = 300, import up to 25 Mailboxes Item
			// 300 - 100 = 200,  200 * 10 = 2000 --> Expected Order line amount
			// 2000/300 = 6.666666667 ------> Expected Order line Price
			// 2000+50 = 2050  --> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("6.666666667"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("2000.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("2050.00"), order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_3")) {
			// order line qty =	300, increased by 100, total=400, FUP Quantity = 100, , import up to 25 Mailboxes Item
			// 400 - 100 = 300,  300 * 10 = 3000   --> Expected Order line amount 
			// 3000/400 = 7.5  ------> Expected Order line Price
			// 3000+50 = 3050  -----> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("7.5"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("3000.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("3050.00"), order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_4")) {
			// order line qty =	300, reduce upto 100, total=100,  FUP Quantity = 100, import up to 10 Mailboxes Item
			// 100 - 100 = 0,  0 * 10 = 0 --> Expected Order line amount
			// 0/300 = 0  ------> Expected Order line Price
			// 0+25 = 25  -----> Expected Order total
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("25.00"), order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_5")) {
			// order line 1 qty = 300, reduce qty upto 0, add new line qty = 200, total=200, import up to 10 Mailboxes Item  
			// FUP Quantity = 100
			// (300-300+200) - 100 = 100,  100 * 10 = 1000  --> Expected Order line amount
			// 1000/200 = 5.00   ------> Expected Order line Price
			// 1000+25 = 1025  -----> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("5.00"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("1000.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("1025.00"), order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_6")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 100, total=100, import up to 10 Mailboxes Item  
			// FUP Quantity = 100
			// (300-300+100) - 100 = 0,  0 * 10 = 0 --> Expected Order line amount
			// 0/300 = 0   ------> Expected Order line Price
			// 0+25 = 25  -----> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("25.00"), order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_7")) {
			// order line qty =	300, reduce qty upto 0, add new line qty= 50, total=50, import up to 10 Mailboxes Item 
			// FUP Quantity = 100
			// here order line qunatity less than free usage pool, 
			// so Expected line Price = 0.00 & Order line amount = 0.00 
			// 0+25 = 25   -----> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("25.00"), order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_8")) {
			// Multiple order lines, we don't know which line use free usage pool & which not.
			// order line 1 = 100, line 2 = 200, total=300, FUP Quantity = 100, import up to 25 Mailboxes Item
			// (100+200) - 100 = 200,  200 * 10 = 2000 
			// 2000+50 = 2050    -----> Expected Order total 
	    	assertEquals("Expected Order total: ", new BigDecimal("2050.00") , order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_9")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, 
			// FUP Quantity = 100,  import unlimited Mailboxes Item
			// 600 - 100 = 500,  500 * 10 = 5000, 5000+100 = 5100   -----> Expected Order total
			
	    	assertEquals("Expected Order total: ", new BigDecimal("5100.00") , order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_10")) {
			// Removed order line which does not have usage pool
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, total=600, 
			// FUP Quantity = 100,  import unlimited Mailboxes Item
			// 600 - 100 = 500,  totalQuantity = 500   
			
			BigDecimal remainingQuantity = new BigDecimal(500).subtract(removeOrderLineQuantity);
			BigDecimal expectedTotal = remainingQuantity.multiply(new BigDecimal("10.00"), MathContext.DECIMAL128);
			if (removeOrderLineQuantity.compareTo(new BigDecimal(100)) > 0)
				expectedTotal = expectedTotal.add(new BigDecimal("50.00"));
			else
				expectedTotal = expectedTotal.add(new BigDecimal("100.00"));
			assertEquals("Expected Order total: ", expectedTotal, order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_13")) {
			// order line qty =	300, FUP Quantity = 100,  import up to 25 Mailboxes Item
			// 300 - 100 = 200,  200 * 10 = 2000 --> Expected Order line amount
			// 2000/300 = 6.666666667 ------> Expected Order line Price
			// 2000+50 = 2050   -----> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("6.666666667"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("2000.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("2050.00") , order.getTotalAsDecimal());
		}  else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_14")) {
			// order line 1 = 100, line 2 = 200, line 3 = 100, line 4 = 200, totalQuantity =600; 
			// FUP 1 = 100, FUP 2 = 200  totalFUP =300;   import unlimited Mailboxes Item 
			// 600 - 300 = 300,  300 * 10 = 3000,  3000+100 = 3100 -----> Expected Order total
			
			assertEquals("Expected Order total: ", new BigDecimal("3100.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_15")) {
			// order line qty =	50, increased by 200, FUP Quantity = 100, total quantity = 250, import up to 10 Mailboxes Item
			// 250 - 100 = 150,  150 * 10 = 1500 --> Expected Order line amount
			// 1500/250 = 6.00 ------> Expected Order line Price
			// 1500+25 = 1525  --> Expected Order total
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("6.00"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("1500.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("1525.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_16")) {
			// order line qty =	250, descresed by 30, FUP Quantity = 100, total quantity = 220, import up to 10 Mailboxes Item
			// 220 - 100 = 150,  120 * 10 = 1200 --> Expected Order line amount
			// 1200/220 = 5.454545455 ------> Expected Order line Price
			// 1200+25 = 1225  --> Expected Order total
						
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			}
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("5.454545455"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("1200.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("1225.00"), order.getTotalAsDecimal());
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_17")) {
			// order line qty =	220, increased by 50, FUP Quantity = 100, total quantity = 270, import up to 10 Mailboxes Item
			// 270 - 100 = 170,  170 * 10 = 1700 --> Expected Order line amount
			// 1700/270 = 6.296296296 ------> Expected Order line Price
			// 1700+25 = 1725  --> Expected Order total
						
			
			for (OrderLineWS line: order.getOrderLines()) {
				if (line.getQuantityAsDecimal().compareTo(BigDecimal.ONE) > 0)
					orderLine = line;
			} 
			assertNotNull("Order line should not be null", orderLine);
			assertEquals("Expected Order line Price: ", new BigDecimal("6.296296296"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("1700.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("1725.00"), order.getTotalAsDecimal());
		}
	}

    //create Plan Item Based Order
    protected Integer createPlanItemBasedOrderForMultpileUsagePool(Integer userId, Integer plansItemId, Integer customerId) {

        List<CustomerUsagePoolWS> customerUsagePools = null;

        OrderWS planItemBasedOrder = super.getUserSubscriptionToPlan(new Date(), userId, Constants.ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY, plansItemId, 1);
        Integer orderId = api.createOrder(planItemBasedOrder, OrderChangeBL.buildFromOrder(planItemBasedOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order Id cannot be null.", orderId);

        customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool not created", customerUsagePools.size() > 0);
        return orderId;
    }

    @Override
    protected PriceModelWS getTestPriceModel() {
        initialize();
        PriceModelWS selector = new PriceModelWS(PriceModelStrategy.ITEM_SELECTOR.name(), new BigDecimal("10.00"), Constants.PRIMARY_CURRENCY_ID);
        selector.addAttribute("typeId", itemTypeId.toString());	// call category
        selector.addAttribute("1", mailBoxItemId_1.toString());	// add item mailBoxItemId_1 when 1 purchased
        selector.addAttribute("300", mailBoxItemId_2.toString());	// add item mailBoxItemId_2 when > 296 purchased
        selector.addAttribute("500", mailBoxItemId_3.toString());	// add item mailBoxItemId_3 when > 496 purchased

        SortedMap<String, String> attributes = new TreeMap<String, String>();
        attributes.putAll(selector.getAttributes());
        attributes.remove("typeId");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().contains(":")) {
                prices.put(AttributeUtils.parseInteger(entry.getKey()), AttributeUtils.parseDecimal(entry.getValue()));
            }
        }

        return selector;

    }
}
