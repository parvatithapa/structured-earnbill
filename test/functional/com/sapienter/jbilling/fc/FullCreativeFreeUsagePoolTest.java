package com.sapienter.jbilling.fc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
/**
 * 
 * @author manish
 *
 */
@Test(groups = { "fullcreative" }, testName = "FullCreativeFreeUsagePoolTest")
public class FullCreativeFreeUsagePoolTest {

	private static final Logger logger = LoggerFactory.getLogger(FullCreativeFreeUsagePoolTest.class);
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private final BigDecimal ORDER_LINE_PRICE = new BigDecimal(0.50);
	protected final Integer SYSTEM_CURRENCY_ID = 1;
	private Integer inboundUsageId ;
	private Integer planId ;
	private JbillingAPI api;
    private Integer basicItemManagerPlugInId;
	
	@org.testng.annotations.BeforeClass
	 protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        inboundUsageId = FullCreativeTestConstants.INBOUND_USAGE_PRODUCT_ID;
        planId = FullCreativeTestConstants.AF_BEST_VALUE_PLAN_ID;

        basicItemManagerPlugInId = FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID;
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);
    }

	@Test
	public void testACFreeUsagePoolTest() throws Exception {
		
		final int LINES = 1;
		logger.debug("Creating user...");
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		UserWS user = FullCreativeUtil.createUser(calendar.getTime());
		logger.debug("User {} created", user.getId());
		
		logger.debug("Creating order...");
		OrderWS order = new OrderWS();
		order.setUserId(user.getId());
                order.setActiveSince(new Date());
                order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
                order.setPeriod(new Integer(2)); // monthly
		order.setCurrencyId(new Integer(1));
		
		PlanWS planWS = api.getPlanWS(planId); // AF Best Value Plan
		
		assertNotNull("Plan Not Found for id 603.", planWS);
		
		OrderLineWS plLine = new OrderLineWS();
		plLine.setItemId(planWS.getItemId());
		plLine.setAmount("225.00");
		plLine.setPrice("225.00");
		plLine.setTypeId(Integer.valueOf(1));
		plLine.setDescription("AF Best Value Plan");
		plLine.setQuantity("0");
		plLine.setUseItem(true);
		order.setOrderLines(new OrderLineWS[]{plLine});

		Integer orderId	= api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
		assertNotNull("Plan Order creation failed", orderId);
		logger.debug("Plan Order created : {}", orderId);
		
		OrderWS order2 = createMockOrder(user.getUserId(), inboundUsageId.intValue(), LINES, ORDER_LINE_PRICE, new BigDecimal(100));
		
		Integer orderId2 = api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));
		assertNotNull("One Time Usage Order 2 creation failed", orderId2);
		logger.debug("One Time Usage Order 2 created : {}", orderId2);
		
		order2 = api.getOrder(orderId2);
		
		OrderLineWS orderLine = order2.getOrderLines()[0];
		// order line qty =	100, FUP Quantity = 300
		// Remaining Customer Usage Pool Quantity 300 - 100 = 200, 
		// After use FUP remaining quantity  Zero 
		// 0.00 * 0.95 = 0.00 --> Expected total
		assertEquals("Expected Order line Price: ", new BigDecimal("0E-10"), orderLine.getPriceAsDecimal());
            	assertEquals("Expected Order line Amount: ", new BigDecimal("0E-10"), orderLine.getAmountAsDecimal());
            	assertEquals("Expected Order line  usage pool quantity : ", new BigDecimal("100.0000000000"), 
    			            orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
    	
        	OrderWS order3 = createMockOrder(user.getUserId(), inboundUsageId.intValue(), LINES, ORDER_LINE_PRICE, new BigDecimal(300));
    
        	Integer orderId3 = api.createOrder(order3, OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID));
        	assertNotNull("One Time Usage Order 3 creation failed", orderId3);
    		logger.debug("One Time Usage Order 2 created : {}", orderId3);
        	
        	order3 = api.getOrder(orderId3);
		
		orderLine = order3.getOrderLines()[0];
		// order line qty =	300, FUP Quantity = 200 (Remaining FUP quantity)
		// Remaining Customer Usage Pool Quantity 300 - 200 = 100, 
		// After use FUP remaining quantity  Zero 
		// 100 * 0.95 = 95.00 --> Expected total
		assertEquals("Expected Order line Price: ", new BigDecimal("0.3166666667"), orderLine.getPriceAsDecimal());
        	assertEquals("Expected Order line Amount: ", new BigDecimal("95.0000000100"), orderLine.getAmountAsDecimal());
        	assertEquals("Expected Order line  usage pool quantity: ", new BigDecimal("200.0000000000"), 
    						orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
		
	}
	
	protected OrderWS createMockOrder(int userId, int itemId, int orderLinesCount, 
			BigDecimal linePrice, BigDecimal quantity) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(1); // once
		order.setCurrencyId(SYSTEM_CURRENCY_ID);
		order.setActiveSince(new Date());
		
		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(orderLinesCount);
		for (int i = 0; i < orderLinesCount; i++){
			OrderLineWS nextLine = new OrderLineWS();
			
			nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(itemId);
			nextLine.setQuantity(quantity.multiply(new BigDecimal(i+1)));
			nextLine.setPrice(linePrice);
			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));
			nextLine.setUseItem(true);
			nextLine.setCreateDatetime(new Date());
			
			lines.add(nextLine);
		}
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

	@AfterClass
	public void cleanUp(){
		FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
	}
}

