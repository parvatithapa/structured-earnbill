package com.sapienter.jbilling.server.usagePool;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

@Test(groups = { "usagePools" }, testName = "public class CustomerUsagePoolCyclePeriodExpiryTest extends AbstractFreeUsagePoolPricingTest {\n")
public class CustomerUsagePoolCyclePeriodExpiryTest extends AbstractFreeUsagePoolPricingTest {

	private static final BigDecimal ORDER_LINE_PRICE = new BigDecimal("0.50");
	
	@Test
	public void test001CreateOrderForFreeUsagePool() throws Exception {
		super.testCreateOrderForFreeUsagePool(customerUsagePoolId, usagePoolId);
		api.deleteOrder(planOrderId);
		OrderWS order = api.getOrder(planOrderId);
		assertTrue("Plan Order should be deleted", order.getDeleted() == 1);
		CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolById(customerUsagePoolId);
		assertEquals("Expiry Date of Customer Usage Pool : ", customerUsagePool.getCycleEndDate(), Util.getEpochDate());
	}
	
	@Test
	public void test002FreeUsagePoolExpiry() throws Exception {
		final int LINES = 1;
		OrderWS order = createMockOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("300"));
	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    rateOrderAsserts(orderWs, "RATE_ORDER_SCENARIO_2", new Integer[]{customerUsagePoolId});
	    
	    int orderId1 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId1);
	    assertNotNull(order);
        assertEquals(LINES, order.getOrderLines().length);
        OrderLineWS orderLine = order.getOrderLines()[0];
        assertTrue(orderLine.getOrderLineUsagePools().length == 0);
      //assert condition for check order line Price & Amount
    	orderLinePriceAssert(order, "ORDER_LINE_PRICE_SCENARIO_2", null);
        
	    api.deleteOrder(orderId1);
	    order = api.getOrder(orderId1);
	    assertTrue("Order should be deleted", order.getDeleted() == 1);
	}
	
	protected void rateOrderAsserts(OrderWS order, String scenario, Integer[] custUsagePoolsId) {
		CustomerUsagePoolWS customerUsagePool = null;
		UsagePoolWS usagePool = null;
		
		if (scenario.equals("RATE_ORDER_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200,  200 * 0.50 = 100 --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal("100.00") , order.getTotalAsDecimal());
		} else if (scenario.equals("RATE_ORDER_SCENARIO_2")) {
			// line 1 = 100, line 2 = 200, FUP Quantity = 0
			// (100+200) => 300, 
			// 300 * 0.50 = 150 --> Expected total
			
			assertEquals("Expected Order total: ", new BigDecimal("150.00") , order.getTotalAsDecimal());
		} 
	}
	
	protected void orderLinePriceAssert(OrderWS order, String scenario, BigDecimal removeOrderLineQuantity) {

		if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_1")) {
			// order line qty =	300, FUP Quantity = 100
			// 300 - 100 = 200,  200 * 0.50 = 100 --> Expected Order line amount
			// 100/300 = 0.333333333 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.333333333"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("100.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("100.00") , order.getTotalAsDecimal());
	    	
		} else if (scenario.equals("ORDER_LINE_PRICE_SCENARIO_2")) {
			// order line qty =	300, FUP Quantity = 0
			// 300 * 0.50 = 150 --> Expected Order line amount
			// 150/300 = 0.5 ------> Expected Order line Price
			
			OrderLineWS orderLine = order.getOrderLines()[0];
			assertEquals("Expected Order line Price: ", new BigDecimal("0.5"), orderLine.getPriceAsDecimal());
	    	assertEquals("Expected Order line Amount: ", new BigDecimal("150.00"), orderLine.getAmountAsDecimal());
	    	assertEquals("Expected Order total: ", new BigDecimal("150.00") , order.getTotalAsDecimal());
		} 
	}

    @Override
    protected PriceModelWS getTestPriceModel() {
        return new PriceModelWS(PriceModelStrategy.FLAT.name(), ORDER_LINE_PRICE, Constants.PRIMARY_CURRENCY_ID);
    }
}
