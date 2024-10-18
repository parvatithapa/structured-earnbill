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
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * AbstractFreeUsagePoolPricingTest
 * This is the base class for all pricing strategy test classes that have 
 * been written to test Free Usage Pools functionality. It has all the methods 
 * that are reusable across all pricing strategies. Most of the test scenarios 
 * are also written in this class (not as annotated test methods) and are invoked
 * from derived classes specific for each pricing strategy from the annotated test methods.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

public abstract class AbstractFreeUsagePoolPricingTest {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractFreeUsagePoolPricingTest.class);
	protected static final Integer PRANCING_PONY = 1;
    protected static final Integer ENABLED = Integer.valueOf(1);
    protected static final Integer DISABLED = Integer.valueOf(0);
    protected static final BigDecimal ORDER_LINE_PRICE = new BigDecimal(0.50);
    protected static final BigDecimal PLAN_BUNDLE_QUANTITY = new BigDecimal(1);

    protected Integer userId = null;
    protected Integer customerId = null;
    protected Integer itemTypeId = null;
    protected Integer itemId = null;
    protected Integer usagePoolId = null;
    protected Integer usagePoolId2 = null;
    protected Integer customerUsagePoolId = null;
    protected Integer planId = null;
    protected Integer planOrderId = null;
    protected static Integer ORDER_PERIOD_MONTHLY;
    protected static Integer ORDER_CHANGE_STATUS_APPLY_ID;

	protected JbillingAPI api;

    @BeforeClass(alwaysRun = true)
    public void initializeTests() {
		try {
			api = JbillingAPIFactory.getAPI();
		} catch (Exception e) {
			logger.error("Error while getting API");
		}

        ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);

        // Create and persist test customer
        userId = createUser("FUPTestCustomer");
        // Get customer id
        UserWS user = api.getUserWS(userId);
        customerId = user.getCustomerId();

        // Create and persist test items category
        itemTypeId = createItemType("FUPItemType");

        // Create and persist test item
        PriceModelWS testPriceModel = getTestPriceModel();
        itemId = createItem("FUP", "TestItem", testPriceModel, itemTypeId);
        // Create and persist test FUP
        usagePoolId = createFreeUsagePool("100 Units Free", new BigDecimal(100), new Integer[]{itemTypeId}, new Integer[]{itemId});

        // Create and persist test plan
        planId = createPlan(testPriceModel, itemTypeId, PLAN_BUNDLE_QUANTITY);

        // Create and persist plan based order
        PlanWS plan = api.getPlanWS(planId);
        planOrderId = createPlanItemBasedOrder(userId, plan.getItemId(), customerId);

        List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        for (CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
            if (customerUsagePool.getUsagePoolId().intValue() == usagePoolId.intValue())
                customerUsagePoolId = customerUsagePool.getId();
        }
        assertNotNull("Customer Usage Pool not created", customerUsagePoolId);

	}

    @AfterClass(alwaysRun = true)
    public void cleanUpTests(){

        cleanUp(api);

        if(null != itemTypeId){
            ItemDTOEx[] items = api.getItemByCategory(itemTypeId);
            if(null != items){
                for (ItemDTOEx item : items){
                    api.deleteItem(item.getId());
                }
            }
            api.deleteItemCategory(itemTypeId);
            itemTypeId = null;
            itemId = null;
        }
        if(null != ORDER_CHANGE_STATUS_APPLY_ID){
            ORDER_CHANGE_STATUS_APPLY_ID = null;
        }
        if(null != ORDER_PERIOD_MONTHLY){
            ORDER_PERIOD_MONTHLY = null;
        }

        if(null != api){
            api = null;
        }
    }
	
	
	protected abstract void rateOrderAsserts(OrderWS order, String scenario, Integer[] custUsagePoolsId);
	
	protected abstract void orderLinePriceAssert(OrderWS order, String scenario, 
											BigDecimal removeOrderLineQuantity);
	
	protected void orderAssert(OrderWS order, int orderLineCount, 
							BigDecimal orderLinesUsagePoolQuantity, 
							BigDecimal freeUsageQuantity) {
    	assertNotNull(order);
        assertEquals(orderLineCount, order.getOrderLines().length);
        if (orderLinesUsagePoolQuantity.compareTo(BigDecimal.ZERO) > 0) {
        	OrderLineWS orderLine = null;
        	if (orderLineCount == 1) {
        		orderLine = order.getOrderLines()[0];
                if(orderLine.getOrderLineUsagePools().length > 0){
                    OrderLineUsagePoolWS orderLineUsagePool = orderLine.getOrderLineUsagePools()[0];
                    BigDecimal orderLineUsagePoolQuantity = orderLineUsagePool.getQuantityAsDecimal();
                    assertTrue("Order line should have Order line Usage pool", orderLine.getOrderLineUsagePools().length > 0);
                    assertTrue("Order line usage pool quantity is less than or equal to Free usage pool quantity",
                            orderLineUsagePoolQuantity.compareTo(freeUsageQuantity) <= 0);
                }
        	}
        	else {
        		BigDecimal totalOLUsagePoolQuantity = BigDecimal.ZERO;
        		int countOrderLineUsagePool = 0;
        		for (OrderLineWS orderLinews: order.getOrderLines()) {
        			for (OrderLineUsagePoolWS olUsagePool: orderLinews.getOrderLineUsagePools()) {
        				countOrderLineUsagePool++;
        				totalOLUsagePoolQuantity = totalOLUsagePoolQuantity.add(olUsagePool.getQuantityAsDecimal());
        			}
        		}
        		assertTrue("Order line should have Order line Usage pool",  countOrderLineUsagePool > 0);
        		assertTrue("Total of Order line usage pool quantity is less than or equal to Free usage pool quantity", 
        					totalOLUsagePoolQuantity.compareTo(freeUsageQuantity) <= 0);
        	}
        }
    }
	
	protected void orderAssert(OrderWS order, int orderLineCount, Integer customerUsagePoolId, Integer usagePoolId) {
		assertNotNull(order);
		assertEquals(orderLineCount, order.getOrderLines().length);
		
		for (OrderLineWS orderLine: order.getOrderLines()) {
			assertTrue("Order line should not have order line usage pools", (orderLine.getOrderLineUsagePools().length) == 0);
		}
		
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
				usagePool.getQuantityAsDecimal(), customerUsagePool.getQuantityAsDecimal());
	}
	
	protected void deleteOrderAssert(OrderWS order, BigDecimal freeUsagePoolQuantity, BigDecimal customerUsagePoolQuantity) {
    	BigDecimal orderedFreeUsageQauntity = new BigDecimal(order.getFreeUsageQuantity());
	    
	    assertTrue("Order should be deleted", order.getDeleted() == 1);
	    assertTrue("Order line should be null", (order.getOrderLines().length) == 0);
	    assertTrue("Order free usage quantity should be zero", orderedFreeUsageQauntity.compareTo(BigDecimal.ZERO) == 0);
		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
						freeUsagePoolQuantity, customerUsagePoolQuantity);
    }
	
	protected void usagePoolAssert(BigDecimal freeUsagePoolQuantity, 
									BigDecimal olUsagePoolAndCustUsagePoolQuantitySum, 
									BigDecimal customerUsagePoolQuantity) {
		assertTrue("Customer usage Pool Quantity not negative.", customerUsagePoolQuantity.compareTo(BigDecimal.ZERO) >= 0);
		assertEquals("The free usage pool quantity & sum of customer usage pool, order line usage pool quantity must be same", 
					freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum);
	}
	
	public void testCreateOrderForFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		logger.debug("testCreateOrderForFreeUsagePool");
		final int LINES = 1;
		
		//Create order with line quantity 300. Customer Usage Pool Quantity 100.
	    OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));
	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    
	    int orderId1 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId1);
	    if (this instanceof ItemSelectorFreeUsagePoolTest || 
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)	
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
		    orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
	    
		CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
			
    	BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
    	
    	//assert condition for check order line Price & Amount
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_1", null);
    	
	    //to check assert condition
    	if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else 
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
    	
	    api.deleteOrder(orderId1);
	    
	    order = api.getOrder(orderId1);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
	    
	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testEditOrderWithoutChangeQuantityForFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order line, save with no change in quantity. 
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    
	    int orderId2 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    //edit order without changing order line quantity. Quantity remains 300
	    OrderWS retOrder = api.getOrder(orderId2);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    api.updateOrder(retOrder, OrderChangeBL.buildFromOrder(retOrder, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId2);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
    	
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	       
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_2", null);
	       
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId2);
	    
	    order = api.getOrder(orderId2);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testEditOrderAndIncreaseQuantityForFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order line, save with increase in quantity.
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    
	    int orderId3 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    //edit and increase the Order Line Quantity 300 to 400.
	    OrderWS retOrder = api.getOrder(orderId3);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    OrderLineWS orderLinews = null;
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    	for (OrderLineWS line: retOrder.getOrderLines()) {
	    		if (line.getQuantityAsDecimal().compareTo(new BigDecimal(300)) >= 0)
	    			orderLinews = line;
	    	}
	    }
	    else
	    	orderLinews = retOrder.getOrderLines()[0];
	    
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLinews, retOrder, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(100));
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId3);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
	    
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	       
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_3", null);
	       
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId3);
	    
	    order = api.getOrder(orderId3);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	} 
	
	public void testReducingQuantityUptoFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order line, save with reducing quantity upto FUP quantity.
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    int orderId4 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    //edit and reduce the Order Line Quantity 300 to 100.
	    OrderWS retOrder = api.getOrder(orderId4);
	    if (this instanceof ItemSelectorFreeUsagePoolTest || 
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    OrderLineWS orderLinews = null;
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    	for (OrderLineWS line: retOrder.getOrderLines()) {
	    		if (line.getQuantityAsDecimal().compareTo(new BigDecimal(300)) >= 0)
	    			orderLinews = line;
	    	}
	    }
	    else
	    	orderLinews = retOrder.getOrderLines()[0];
	    
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLinews, retOrder, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(200).negate());
	    		
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId4);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
	    
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	       
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_4", null);
	       
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId4);
	    
	    order = api.getOrder(orderId4);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
    
	public void testOrderLineQuantityZeroForFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order line, remove all quantity, make quantity zero.
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    int orderId5 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    //edit and reduce the Order Line Quantity 300 to 0.
	    OrderWS retOrder = api.getOrder(orderId5);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(retOrder, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO; 
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    
	    OrderLineWS orderLinews = null;
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    	for (OrderLineWS line: retOrder.getOrderLines()) {
	    		if (line.getQuantityAsDecimal().compareTo(new BigDecimal(300)) >= 0)
	    			orderLinews = line;
	    	}
	    }
	    else
	    	orderLinews = retOrder.getOrderLines()[0];
	    
	    if (orderLinews.getOrderLineUsagePools().length > 0) {
	    	OrderLineUsagePoolWS orderLineUsagePool = orderLinews.getOrderLineUsagePools()[0];
		    orderLineUsagePoolQuantity = orderLineUsagePool.getQuantityAsDecimal();
	    }
	    
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLinews, retOrder, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(300).negate());
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId5);
	   
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES, new BigDecimal(0), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES-1, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES-1, new BigDecimal(0), new BigDecimal(100));
        
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
		
		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
				freeUsagePoolQuantity, customerUsagePoolQuantity);
		
	    api.deleteOrder(orderId5);
	    
	    order = api.getOrder(orderId5);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testAddNewLineWithQuantityGreaterThanFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order, add new order line, quantity greater than FUP quantity (FUP=100).
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    int orderId6 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    OrderWS retOrder = api.getOrder(orderId6);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(retOrder, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO; 
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    
	    OrderLineWS orderLine = null;
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    	for (OrderLineWS line: retOrder.getOrderLines()) {
	    		if (line.getQuantityAsDecimal().compareTo(new BigDecimal(300)) >= 0)
	    			orderLine = line;
	    	}
	    }
	    else
	    	orderLine = retOrder.getOrderLines()[0];
	    
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLine, retOrder, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(300).negate());
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId6);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES, new BigDecimal(0), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES-1, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES-1, new BigDecimal(0), new BigDecimal(100));
	    
        //let's add a line with quantity 200
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("add new line");
        line.setItemId(itemId);
        line.setQuantity(new Integer(200));
        line.setPrice(ORDER_LINE_PRICE);
        line.setUseItem(new Boolean(true));
        orderChange = OrderChangeBL.buildFromLine(line, order, ORDER_CHANGE_STATUS_APPLY_ID);

        // call the update
        api.updateOrder(order, new OrderChangeWS[]{orderChange});
        
	    order = api.getOrder(orderId6);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    for (OrderLineWS orderLinews: order.getOrderLines()) {
	    	if (orderLinews.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLinews.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
	    
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	       
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_5", null);
	       
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId6);
	    
	    order = api.getOrder(orderId6);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	
	public void testAddNewLineWithQuantityEqualToFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order, add new order line, quantity equal FUP quantity. (FUP=100).
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    int orderId6 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    OrderWS retOrder = api.getOrder(orderId6);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(retOrder, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    OrderLineWS orderLinews = null;
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    	for (OrderLineWS line: retOrder.getOrderLines()) {
	    		if (line.getQuantityAsDecimal().compareTo(new BigDecimal(300)) >= 0)
	    			orderLinews = line;
	    	}
	    }
	    else
	    	orderLinews = retOrder.getOrderLines()[0];
	    
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLinews, retOrder, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(300).negate());
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId6);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES, new BigDecimal(0), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES-1, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES-1, new BigDecimal(0), new BigDecimal(100));
        
        //let's add a line with quantity 100
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("add new line");
        line.setItemId(itemId);
        line.setQuantity(new Integer(100));
        line.setPrice(ORDER_LINE_PRICE);
        line.setUseItem(new Boolean(true));
        orderChange = OrderChangeBL.buildFromLine(line, order, ORDER_CHANGE_STATUS_APPLY_ID);

        // call the update
        api.updateOrder(order, new OrderChangeWS[]{orderChange});
        
	    order = api.getOrder(orderId6);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
    	
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	       
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_6", null);
	       
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId6);
	    
	    order = api.getOrder(orderId6);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testAddNewLineWithQuantityLessThanFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order, add new order line, quantity less than FUP quantity. (FUP=100).
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    int orderId7 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    OrderWS retOrder = api.getOrder(orderId7);
	    if (this instanceof ItemSelectorFreeUsagePoolTest || 
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(retOrder, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    OrderLineWS orderLine = null;
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    	for (OrderLineWS line: retOrder.getOrderLines()) {
	    		if (line.getQuantityAsDecimal().compareTo(new BigDecimal(300)) >= 0)
	    			orderLine = line;
	    	}
	    }
	    else
	    	orderLine = retOrder.getOrderLines()[0];
	    
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLine, retOrder, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(300).negate());
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});
	    
	    order = api.getOrder(orderId7);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES, new BigDecimal(0), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES-1, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES-1, new BigDecimal(0), new BigDecimal(100));
        
	  	//let's add a line with quantity 50
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("add new line");
        line.setItemId(itemId);
        line.setQuantity(new Integer(50));
        line.setPrice(ORDER_LINE_PRICE);
        line.setUseItem(new Boolean(true));
        orderChange = OrderChangeBL.buildFromLine(line, order, ORDER_CHANGE_STATUS_APPLY_ID);

        // call the update
        api.updateOrder(order, new OrderChangeWS[]{orderChange});
        
	    order = api.getOrder(orderId7);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(50), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(50), new BigDecimal(100));
        
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLinews: order.getOrderLines()) {
	    	if (orderLinews.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLinews.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
    	
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	       
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_7", null);
	       
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId7);
	    
	    order = api.getOrder(orderId7);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testCreateOrderWithTwoLinesForFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 2;
		
		//Create Order, add two or more order lines of the same product.
	    OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("100"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_2", new Integer[]{customerUsagePoolId});
	    int orderId8 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId8);
	    if (this instanceof ItemSelectorFreeUsagePoolTest || 
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
        
	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantitySum = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
    	
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
    	
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_8", null);
    	
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId8);
	    
	    order = api.getOrder(orderId8);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testEditOrderAddTwoLinesForFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 2;
		
		//Edit order, add two or more order lines of the same product.
	    OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("100"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_2", new Integer[]{customerUsagePoolId});
	    int orderId9 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId9);
	    if (this instanceof ItemSelectorFreeUsagePoolTest || 
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
        //let's add a 2 line with quantity 100
        ArrayList<OrderLineWS> lines = createOrderLines(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal(100));
        order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        OrderLineWS[] orderLines = lines.toArray(new OrderLineWS[lines.size()]);
        for (OrderLineWS orderLine : orderLines) {
        	OrderChangeWS change = OrderChangeBL.buildFromLine(orderLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
        	orderChanges.add(change);
		}
        // call the update
        api.updateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));
        order = api.getOrder(orderId9);
        if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+3, new BigDecimal(100), new BigDecimal(100));
        else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES+2, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES+2, new BigDecimal(100), new BigDecimal(100));
        
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
        BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
        BigDecimal orderLineUsagePoolQuantitySum = BigDecimal.ZERO;
        
        CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	    
		for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
    	
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_9", null);
    	
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId9);
	    
	    order = api.getOrder(orderId9);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testEditOrderRemoveLineWhichDoesNotUseFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 2;
		
		//Edit order, add two or more order lines of the same product, remove one without FUP usage.
	    OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("100"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_2", new Integer[]{customerUsagePoolId});
	    int orderId10 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId10);
	    if (this instanceof ItemSelectorFreeUsagePoolTest || 
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
        //let's add a 2 line with quantity 100
        ArrayList<OrderLineWS> lines = createOrderLines(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal(100));
        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        OrderLineWS[] orderLines = lines.toArray(new OrderLineWS[lines.size()]);
        for (OrderLineWS orderLine : orderLines) {
        	OrderChangeWS change = OrderChangeBL.buildFromLine(orderLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
        	orderChanges.add(change);
		}
        // call the update
        api.updateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));
        order = api.getOrder(orderId10);
        if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+3, new BigDecimal(100), new BigDecimal(100));
        else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES+2, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES+2, new BigDecimal(100), new BigDecimal(100));
        
        orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_9", null);
        
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantitySum = BigDecimal.ZERO;
	    BigDecimal removeOrderLineQuantity = BigDecimal.ZERO;
        OrderLineWS removeOrderLine = null; 
        for (OrderLineWS orderLine: order.getOrderLines()) {
        	if (orderLine.getQuantityAsDecimal().compareTo(new BigDecimal("100")) >= 0 && 
    			null != orderLine.getOrderLineUsagePools() && 
    			orderLine.getOrderLineUsagePools().length == 0) {
        		removeOrderLine = orderLine;
        		break;
        	}
        }
        assertNotNull("Order line without Free usage pool should not null", removeOrderLine);
        removeOrderLineQuantity = removeOrderLine.getQuantityAsDecimal();
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(removeOrderLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.ZERO.subtract(removeOrderLine.getQuantityAsDecimal()));
	    
	    api.updateOrder(order, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId10);
	    
	    if (this instanceof ItemSelectorFreeUsagePoolTest || 
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+2, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES+1, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
        
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
    	
    	CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
		
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_10", removeOrderLineQuantity);
    	
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId10);
	    
	    order = api.getOrder(orderId10);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	/*public void testEditOrderRemoveLineWhichUsesFreeUsagePool(Integer customerUsagePoolId, Integer usagePoolId)
	throws Exception {
		System.out.println("testEditOrderRemoveLineWhichUseFreeUsagePool");
		final int LINES = 2;

		//Edit order, add two or more order lines of the same product, remove one with FUP usage.
	    OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("50"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
//	    rateOrderAsserts(orderWs, usagePoolId, userId, new BigDecimal(100));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_3", customerUsagePoolId);
	    int orderId11 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId11);
	    orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));

	    //let's add a 2 line with quantity 100
        ArrayList<OrderLineWS> lines = createOrdeLines(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal(100));
        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        OrderLineWS[] orderLines = lines.toArray(new OrderLineWS[lines.size()]);
        for (OrderLineWS orderLine : orderLines) {
        	OrderChangeWS change = OrderChangeBL.buildFromLine(orderLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
        	orderChanges.add(change);
		}
        // call the update
        api.updateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));
        order = api.getOrder(orderId11);
        orderAssert(order, LINES+2, new BigDecimal(100), new BigDecimal(100));
        orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_11", null);

        BigDecimal removeOrderLineQuantity = BigDecimal.ZERO;

        OrderLineWS removeOrderLine = null;
        for (OrderLineWS orderLine: order.getOrderLines()) {
        	if (null != orderLine.getOrderLineUsagePools() &&
    			orderLine.getOrderLineUsagePools().length >= 1 &&
    			orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal().compareTo(BigDecimal.ZERO) > 0) {
        		removeOrderLine = orderLine;
        		break;
        	}
        }
        assertNotNull("Order line with Free usage pool should not null", removeOrderLine);
        removeOrderLineQuantity = removeOrderLine.getQuantityAsDecimal();
        System.out.println("removeOrderLineQuantity :: "+removeOrderLineQuantity);
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(removeOrderLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.ZERO.subtract(removeOrderLine.getQuantityAsDecimal()));

	    BigDecimal removedOLUsagePoolsQuantity = new BigDecimal(0);
	    for (OrderLineUsagePoolWS olUsagePool: removeOrderLine.getOrderLineUsagePools()) {
	    	removedOLUsagePoolsQuantity = removedOLUsagePoolsQuantity.add(olUsagePool.getQuantityAsDecimal());
	    }
	    //System.out.println("****************removedOLUsagePoolsQuantity :::"+removedOLUsagePoolsQuantity);
	    assertTrue("Order line usage pool quantity should be less than or equal to Free usage pool quantity",
	    				removedOLUsagePoolsQuantity.compareTo(new BigDecimal(100)) <= 0);
	    api.updateOrder(order, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId11);

	    orderAssert(order, LINES+1, new BigDecimal(100).subtract(removedOLUsagePoolsQuantity), new BigDecimal(100));

	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantitySum = BigDecimal.ZERO;

    	for (OrderLineWS orderLine: order.getOrderLines()) {
	    	for(OrderLineUsagePoolWS olUsagePool : orderLine.getOrderLineUsagePools()) {
	    		orderLineUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(olUsagePool.getQuantityAsDecimal());
	    	}
	    }

    	CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();

    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_12", removeOrderLineQuantity);

	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantitySum.add(customerUsagePoolQuantity);

	    //to check assert condition
	    usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    api.deleteOrder(orderId11);

	    order = api.getOrder(orderId11);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}*/
	
	public void testEditOrderLineWithFUPQuantity(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		
		//Edit order line, save with increase in quantity.
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("50"));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_5", new Integer[]{customerUsagePoolId});
	    
	    int orderId3 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    
	    //edit and increase the Order Line Quantity 50 to 250.
	    OrderWS retOrder = api.getOrder(orderId3);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES+1, new BigDecimal(50), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(retOrder, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(retOrder, LINES, new BigDecimal(50), new BigDecimal(100));
	    
	    OrderLineWS orderLinews = null;
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    	for (OrderLineWS line: retOrder.getOrderLines()) {
	    		if (line.getQuantityAsDecimal().compareTo(new BigDecimal(50)) >= 0)
	    			orderLinews = line;
	    	}
	    }
	    else
	    	orderLinews = retOrder.getOrderLines()[0];
	    
	    OrderChangeWS orderChange = OrderChangeBL.buildFromLine(orderLinews, retOrder, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange.setQuantity(BigDecimal.valueOf(200));
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange});
	    order = api.getOrder(orderId3);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;
	    
	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)  
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
	    
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
	       
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_15", null);
    	
    	OrderChangeWS orderChange1 = OrderChangeBL.buildFromLine(orderLinews, order, ORDER_CHANGE_STATUS_APPLY_ID);
	    orderChange1.setQuantity(BigDecimal.valueOf(-30));
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange1});
	    order = api.getOrder(orderId3);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_16", null);
	    
	    OrderChangeWS orderChange2 = OrderChangeBL.buildFromLine(orderLinews, order, ORDER_CHANGE_STATUS_APPLY_ID);
	    if (this instanceof CappedGraduatedFreeUsagePoolTest) {
	    	orderChange2.setQuantity(BigDecimal.valueOf(300));
	    } else {
	    	orderChange2.setQuantity(BigDecimal.valueOf(50));
	    }
	    
	    
	    api.updateOrder(retOrder, new OrderChangeWS[]{orderChange2});
	    order = api.getOrder(orderId3);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest)
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	    
	    orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_17", null);
	       
	    BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);
	       
	    //to check assert condition
	    if (this instanceof FlatFreeUsagePoolTest){
    		assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
    				freeUsagePoolQuantity, customerUsagePoolQuantity);
    	} else
    		usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);
	    
	    api.deleteOrder(orderId3);
	    
	    order = api.getOrder(orderId3);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();

	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testDeleteOrder(Integer customerUsagePoolId, Integer usagePoolId) 
	throws Exception {
		final int LINES = 1;
		//here quantity is 300 & customer usage pool quantity is 100.
		//price = ((300-100)*1)/300 => 200/3000 = 0.666666667
		BigDecimal orderLineQuantity = new BigDecimal(300);
	    OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, orderLineQuantity);

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_1", new Integer[]{customerUsagePoolId});
	    
	    int orderId12 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId12);
	    if (this instanceof ItemSelectorFreeUsagePoolTest ||
    		this instanceof ItemPercentageSelectorFreeUsagePoolTest) 
	    	orderAssert(order, LINES+1, new BigDecimal(100), new BigDecimal(100));
	    else if (this instanceof FlatFreeUsagePoolTest)
	    	orderAssert(order, LINES, customerUsagePoolId, usagePoolId);
	    else
	    	orderAssert(order, LINES, new BigDecimal(100), new BigDecimal(100));
	       
	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    
	    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertNotNull("Customer Usage Pool should not be null", customerUsagePool);
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);
		customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();
		
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_13", null);
    	api.deleteOrder(orderId12);
	    
    	order = api.getOrder(orderId12);
	    customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
	    customerUsagePoolQuantity = customerUsagePool.getQuantityAsDecimal();
	    
	    deleteOrderAssert(order, freeUsagePoolQuantity, customerUsagePoolQuantity);
	}
	
	public void testMultipleUsagePoolsWithMultpileOrderLines(Integer[] custUsagePoolsId, Integer[] usagePoolsId) 
	throws Exception {
		final int LINES = 2;
		UsagePoolWS usagePool = null;
		CustomerUsagePoolWS customerUsagePool = null;
		
		BigDecimal totalFreeUsageQuantity = BigDecimal.ZERO;
		
		for (int i=0; i < usagePoolsId.length; i++) {
			usagePool = api.getUsagePoolWS(usagePoolsId[i]);
			totalFreeUsageQuantity = totalFreeUsageQuantity.add(usagePool.getQuantityAsDecimal());
		}
		
	    OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal(100));

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_4", custUsagePoolsId);
	    int orderId13 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId13);
	    if (this instanceof FlatFreeUsagePoolTest) {
	    	assertNotNull(order);
			assertEquals(LINES, order.getOrderLines().length);
		
			for (OrderLineWS orderLine: order.getOrderLines()) {
				assertTrue("Order line should not have order line usage pools", (orderLine.getOrderLineUsagePools().length) == 0);
			}
			for (int i=0; i < custUsagePoolsId.length; i++) {
				customerUsagePool = api.getCustomerUsagePoolById(custUsagePoolsId[i]);
				usagePool = api.getUsagePoolWS(customerUsagePool.getUsagePoolId());
				assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
						usagePool.getQuantityAsDecimal(), customerUsagePool.getQuantityAsDecimal());
			}
	    } else if (this instanceof ItemSelectorFreeUsagePoolTest ||
	    			this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    		orderAssert(order, LINES+1, new BigDecimal(300), totalFreeUsageQuantity);
	    }
	    else 
	    	orderAssert(order, LINES, new BigDecimal(300), totalFreeUsageQuantity);
	    
        //let's add a 2 line with quantity 100
        ArrayList<OrderLineWS> lines = createOrderLines(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal(100));
        
        List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
        
        OrderLineWS[] orderLines = lines.toArray(new OrderLineWS[lines.size()]);
        for (OrderLineWS orderLine : orderLines) {
        	OrderChangeWS change = OrderChangeBL.buildFromLine(orderLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
        	orderChanges.add(change);
		}
        // call the update
        api.updateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));
        order = api.getOrder(orderId13);
        if (this instanceof FlatFreeUsagePoolTest) {
	    	assertNotNull(order);
			assertEquals(LINES+2, order.getOrderLines().length);
		
			for (OrderLineWS orderLine: order.getOrderLines()) {
				assertTrue("Order line should not have order line usage pools", (orderLine.getOrderLineUsagePools().length) == 0);
			}
			for (int i=0; i < custUsagePoolsId.length; i++) {
				customerUsagePool = api.getCustomerUsagePoolById(custUsagePoolsId[i]);
				usagePool = api.getUsagePoolWS(customerUsagePool.getUsagePoolId());
				assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
						usagePool.getQuantityAsDecimal(), customerUsagePool.getQuantityAsDecimal());
			}
	    } else if (this instanceof ItemSelectorFreeUsagePoolTest ||
	    			this instanceof ItemPercentageSelectorFreeUsagePoolTest) {
	    		orderAssert(order, LINES+3, new BigDecimal(300), totalFreeUsageQuantity);
	    }
        else
        	orderAssert(order, LINES+2, new BigDecimal(300), totalFreeUsageQuantity);
        
        /*Map<Integer, BigDecimal> customerUsagePoolMap = new HashMap<Integer, BigDecimal>();
        Map<Integer, BigDecimal> usagePoolMap = new HashMap<Integer, BigDecimal>();*/
        Map<Integer, BigDecimal> olUsagePoolMap = new HashMap<Integer, BigDecimal>();
	    
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_14", null);
    	
    	Integer customerUsagePoolId = null;
    	if (! (this instanceof FlatFreeUsagePoolTest)) {
	    	for (OrderLineWS orderLine: order.getOrderLines()) {
		    	for(OrderLineUsagePoolWS olUsagePool : orderLine.getOrderLineUsagePools()) {
		    		BigDecimal olUsagePoolQuantity = olUsagePool.getQuantityAsDecimal();
		    		customerUsagePoolId = olUsagePool.getCustomerUsagePoolId();
		    		
		    		if (olUsagePoolMap.containsKey(customerUsagePoolId)) {
		    			BigDecimal customerOrderLinePoolQuantity = olUsagePoolMap.get(customerUsagePoolId);
		    			olUsagePoolMap.put(customerUsagePoolId, customerOrderLinePoolQuantity.add(olUsagePoolQuantity));
		    		}
		    		else {
		    			olUsagePoolMap.put(customerUsagePoolId, olUsagePoolQuantity);
		    		}
		    	}
		    }
	    	
	    	for (int i=0; i < custUsagePoolsId.length; i++) {
				customerUsagePool = api.getCustomerUsagePoolById(custUsagePoolsId[i]);
				usagePool = api.getUsagePoolWS(customerUsagePool.getUsagePoolId());
				BigDecimal olUsageQuantity = olUsagePoolMap.get(customerUsagePool.getId());
				BigDecimal totalCustAndOlPoolQuantity =  customerUsagePool.getQuantityAsDecimal().add(olUsageQuantity);
	    		BigDecimal usagePoolQuantity = usagePool.getQuantityAsDecimal();
	    		assertEquals("The free usage pool quantity & sum of customer usage pool, order line usage pool quantity must be same", 
	    				usagePoolQuantity, totalCustAndOlPoolQuantity);
			}
	    }
    	
	    api.deleteOrder(orderId13);
	    
	    order = api.getOrder(orderId13);
	    
	    for (int i=0; i < custUsagePoolsId.length; i++) {
			customerUsagePool = api.getCustomerUsagePoolById(custUsagePoolsId[i]);
			usagePool = api.getUsagePoolWS(customerUsagePool.getUsagePoolId());
			assertEquals("The free usage pool quantity & customer usage pool quantity must be same", 
					usagePool.getQuantityAsDecimal(), customerUsagePool.getQuantityAsDecimal());
		}
	    
	    BigDecimal orderedFreeUsageQauntity = new BigDecimal(order.getFreeUsageQuantity());
	    assertTrue("Order should be deleted", order.getDeleted() == 1);
	    assertTrue("Order line should be null", (order.getOrderLines().length) == 0);
	    assertTrue("Order free usage quantity should be zero", orderedFreeUsageQauntity.compareTo(BigDecimal.ZERO) == 0);
	}

    // create User
    protected Integer createUser(String userName) {
        Date today = new Date();
        UserWS customer = CreateObjectUtil.createCustomer(Constants.PRIMARY_CURRENCY_ID, userName + today.getTime(),
                "P@ssword1", Constants.LANGUAGE_ENGLISH_ID, 5, false, 1, null,
                CreateObjectUtil.createCustomerContact("test@gmail.com"));
        Integer userId = api.createUser(customer);
        assertNotNull("Customer/User ID should not be null", userId);
        return userId;
    }

    // create Item category
    protected Integer createItemType(String description) {
        ItemTypeWS itemType = buildItemType(description);
        Integer itemTypeId = api.createItemCategory(itemType);
        assertNotNull(itemTypeId);
        ItemTypeWS[] types = api.getAllItemCategories();

        boolean addedFound = false;
        for (int i = 0; i < types.length; ++i) {
            if (itemType.getDescription().equals(types[i].getDescription())) {
                logger.debug("Test category was found. Creation was completed successfully.");
                addedFound = true;
                break;
            }
        }
        assertTrue( itemType.getDescription()+" not found.", addedFound);
        return itemTypeId;
    }

    protected abstract PriceModelWS getTestPriceModel();

    // create Product
    protected Integer createItem(String number, String description, PriceModelWS priceModel, Integer... itemTypeId) {

        ItemDTOEx item = buildItem(number, description, itemTypeId);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        Integer itemId = api.createItem(item);
        assertNotNull("Item was not created", itemId);
        return itemId;

    }

    // create Free Usage Pool
    protected Integer createFreeUsagePool(String usagePoolName, BigDecimal quantity, Integer[] itemTypes, Integer[] items) {
        UsagePoolWS usagePool = populateFreeUsagePoolObject(usagePoolName, quantity, itemTypes, items);
        Integer poolId = api.createUsagePool(usagePool);
        logger.debug("usagePoolId ::{}", poolId);
        assertNotNull("Free usage pool should not be null ", poolId);
        return poolId;
    }


    //create Plan
    protected Integer createPlan(PriceModelWS priceModel, Integer itemType, BigDecimal planBundleQuantity) {

        PlanWS plan = CreateObjectUtil.createPlanBundledItems(PRANCING_PONY, new BigDecimal(1),
                                                              Constants.PRIMARY_CURRENCY_ID, itemType, ORDER_PERIOD_MONTHLY,
                                                              planBundleQuantity, 0, api, usagePoolId);
        plan.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE, priceModel);
        Integer planId = api.createPlan(plan);
        assertNotNull("Plan ID should not be null", planId);
        return planId;
    }

    protected Integer createPlanForMultpileUsagePool(PriceModelWS priceModel, Integer[] usagePoolsId, Integer itemType) {

        PlanWS plan = CreateObjectUtil.createPlanBundledItems(PRANCING_PONY, new BigDecimal(1),
                                                              Constants.PRIMARY_CURRENCY_ID, itemType, ORDER_PERIOD_MONTHLY,
                                                              PLAN_BUNDLE_QUANTITY, 0, api);
        plan.setUsagePoolIds(usagePoolsId);
        plan.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE, priceModel);
        Integer planId = api.createPlan(plan);
        assertNotNull("Plan ID should not be null", planId);
        return planId;
    }

    //create Plan Item Based Order
    protected Integer createPlanItemBasedOrder(Integer userId, Integer plansItemId, Integer customerId) {

        List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool created", customerUsagePools.size() == 0);

        OrderWS planItemBasedOrder = getUserSubscriptionToPlan(new Date(), userId, Constants.ORDER_BILLING_PRE_PAID, ORDER_PERIOD_MONTHLY, plansItemId, 1);
        Integer orderId = api.createOrder(planItemBasedOrder, OrderChangeBL.buildFromOrder(planItemBasedOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order Id cannot be null.", orderId);

        customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool not created", customerUsagePools.size() > 0);
        return orderId;
    }
	
	protected OrderWS createMockOrder(int userId, int itemId, int orderLinesCount, 
								BigDecimal linePrice, BigDecimal quantity) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(Constants.ORDER_PERIOD_ONCE); // once
		order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
		order.setActiveSince(new Date());
		
		ArrayList<OrderLineWS> lines = createOrderLines(userId, itemId, orderLinesCount, linePrice, quantity);
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

	private ArrayList<OrderLineWS> createOrderLines(Integer userId, Integer itemId,
                                                    int orderLinesCount, BigDecimal linePrice, BigDecimal quantity) {
		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(orderLinesCount);
		for (int i = 0; i < orderLinesCount; i++){
			OrderLineWS nextLine = new OrderLineWS();
			nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(itemId);
			nextLine.setQuantity(quantity.multiply(new BigDecimal(i+1)));
//			nextLine.setPrice(linePrice);
//			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));
			nextLine.setUseItem(true);
			
			lines.add(nextLine);
		}
		return lines;
	}
	
	
	protected UsagePoolWS populateFreeUsagePoolObject(String usagePoolName, BigDecimal quantity, Integer[] itemTypesId, Integer[] items)
	{
		Date today = new Date();
        UsagePoolWS usagePool = new UsagePoolWS();
        
		usagePool.setName(usagePoolName + today);
		usagePool.setQuantity(quantity.toString());
		usagePool.setPrecedence(new Integer(1));
		usagePool.setCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS);
		usagePool.setCyclePeriodValue(new Integer(1));
        usagePool.setItemTypes(itemTypesId);
        usagePool.setItems(items);
        usagePool.setEntityId(PRANCING_PONY);
        usagePool.setUsagePoolResetValue("Zero");
        
        return usagePool;
	}
	
	protected ItemDTOEx buildItem(String number, String desc, Integer... itemTypesId) {
        ItemDTOEx item = new ItemDTOEx();
        Long entitySuffix = System.currentTimeMillis();
        item.setNumber(String.format("%s-%s", number, entitySuffix));
        item.setDescription(String.format("%s-%s", desc, entitySuffix));
        item.setTypes(itemTypesId);
        item.setEntityId(PRANCING_PONY);
        item.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        item.setPriceModelCompanyId(PRANCING_PONY);
        return item;
    }

    protected ItemTypeWS buildItemType(String desc){
        ItemTypeWS itemType = new ItemTypeWS();
        Long entitySuffix = System.currentTimeMillis();
        itemType.setDescription(String.format("%s-%s", desc, entitySuffix));
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        itemType.setEntityId(PRANCING_PONY);
        List<Integer> entities = new ArrayList<Integer>(1);
        entities.add(PRANCING_PONY);
        itemType.setEntities(entities);
        itemType.setAllowAssetManagement(DISABLED);

        return itemType;
    }
	
	protected OrderWS getUserSubscriptionToPlan(Date since, Integer userId, 
												Integer billingType, Integer orderPeriodID, 
												Integer plansItemId, Integer planQuantity) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(billingType);
		order.setPeriod(orderPeriodID);
		order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
		order.setActiveSince(since);
		
		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(planQuantity);
		line.setDescription("Order line for plan subscription");
		line.setItemId(plansItemId);
		line.setUseItem(true);
		
		order.setOrderLines(new OrderLineWS[]{line});
		
		return order;
	}

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "INV:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    private Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
        OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
        for (OrderChangeStatusWS status : statuses) {
            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status.getId();
            }
        }
        //there is no APPLY status in db so create one
        OrderChangeStatusWS apply = new OrderChangeStatusWS();
        String status1Name = "APPLY: " + System.currentTimeMillis();
        OrderChangeStatusWS status = new OrderChangeStatusWS();
        status.setApplyToOrder(ApplyToOrder.YES);
        status.setDeleted(0);
        status.setOrder(1);
        status.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
        return api.createOrderChangeStatus(apply);
    }

    protected void cleanUp(JbillingAPI api){
        if(null != planOrderId){
            api.deleteOrder(planOrderId);
            planOrderId = null;
        }
        if(null != planId){
            api.deletePlan(planId);
            planId = null;
        }
//        if(null != usagePoolId){
//            api.deleteUsagePool(usagePoolId);
//            usagePoolId = null;
//        }
//        if(null != usagePoolId2){
//            api.deleteUsagePool(usagePoolId2);
//            usagePoolId2 = null;
//        }

        if(null != userId){
            api.deleteUser(userId);
            userId = null;
        }

    }
}
