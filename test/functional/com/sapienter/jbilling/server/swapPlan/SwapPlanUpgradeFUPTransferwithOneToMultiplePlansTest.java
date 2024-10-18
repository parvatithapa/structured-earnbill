package com.sapienter.jbilling.server.swapPlan;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
/**
 * Here is link of Swap Plan proration scenarios sheet.
 * https://docs.google.com/spreadsheets/d/178y1MPMRAcG-d_S6XismdLccG62lKq3B33xJb5p_-Xo/edit#gid=2059461249
 * @author Krunal Bhavsar
 *
 */
@Test(groups = { "swapPlan" }, testName = "SwapPlanUpgradeFUPTransferwithOneToMultiplePlansTest")
public class SwapPlanUpgradeFUPTransferwithOneToMultiplePlansTest {

	private static final Logger logger = LoggerFactory.getLogger(SwapPlanUpgradeFUPTransferwithOneToMultiplePlansTest.class);
    private JbillingAPI api;
    private Integer userId;
    private Integer subscriptionOrderIdWith100MinFreePlan = null;
    private Integer subscriptionOrderIdWith255MinFreePlan = null;
    private Integer onetimeOrderId = null;
    UserWS user = null;
    PlanWS planWith100FreeMin;
    PlanWS planWith225FreeMin;
    PlanWS planWith450FreeMin;
    
    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        UsagePoolWS usagePoolWith100Quantity = FullCreativeUtil.populateFreeUsagePoolObject("100");
    	usagePoolWith100Quantity.setId(api.createUsagePool(usagePoolWith100Quantity));
    	
    	UsagePoolWS usagePoolWith225Quantity = FullCreativeUtil.populateFreeUsagePoolObject("225");
	UsagePoolWS usagePoolWith450Quantity = FullCreativeUtil.populateFreeUsagePoolObject("450");
    	
	usagePoolWith225Quantity.setId(api.createUsagePool(usagePoolWith225Quantity));
	usagePoolWith450Quantity.setId(api.createUsagePool(usagePoolWith450Quantity));
	assertNotNull("Usage Pool Creation Failed ", usagePoolWith100Quantity);
	assertNotNull("Usage Pool Creation Failed ", usagePoolWith450Quantity);
	assertNotNull("Usage Pool Creation Failed ", usagePoolWith225Quantity);
	
	planWith100FreeMin = FullCreativeUtil.createPlan("100", "0.95", new Integer[]{usagePoolWith100Quantity.getId()},"Test Plan 100 Min", api, TestConstants.CHAT_USAGE_PRODUCT_ID, TestConstants.INBOUND_USAGE_PRODUCT_ID, TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	assertNotNull("planWS should not be null", planWith100FreeMin);
    	
    	planWith225FreeMin = FullCreativeUtil.createPlan("225", "0.95", new Integer[]{usagePoolWith225Quantity.getId()},"Test Plan 225 Min", api, TestConstants.CHAT_USAGE_PRODUCT_ID, TestConstants.INBOUND_USAGE_PRODUCT_ID, TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	assertNotNull("planWS should not be null", planWith225FreeMin);
    	
    	planWith450FreeMin = FullCreativeUtil.createPlan("450", "0.95", new Integer[]{usagePoolWith450Quantity.getId()},"Test Plan 450 Min", api, TestConstants.CHAT_USAGE_PRODUCT_ID, TestConstants.INBOUND_USAGE_PRODUCT_ID, TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	assertNotNull("planWS should not be null", planWith450FreeMin);
    	
    }	
    
    	@Test
	public void testScenarioE1() throws Exception {
    	//Customer 'Swap Plan Proration' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Swap Plan Proration");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);
		
		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));
		
		
		logger.debug("##Creating Plan Subscription order...");
		
		//Create subscription order with Active since date 12-15-2015
		
		subscriptionOrderIdWith100MinFreePlan = FullCreativeUtil.createOrder(planWith100FreeMin,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS subscriptionOrderWith100Min  =  api.getOrder(subscriptionOrderIdWith100MinFreePlan);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", subscriptionOrderIdWith100MinFreePlan,
				TestConstants.DATE_FORMAT.format(subscriptionOrderWith100Min.getActiveSince()));
		
		assertNotNull("orderId should not be null",subscriptionOrderIdWith100MinFreePlan);
		
		user = api.getUserWS(userId);
		
		
		Integer existingPlanId = 0;
		for (OrderLineWS lineWS:subscriptionOrderWith100Min.getOrderLines()) {
			if(lineWS.getItemId().equals(planWith100FreeMin.getItemId())) {
				existingPlanId = lineWS.getItemId();
			}
		}
		
		assertEquals("##Subscription Order's Plan ID must be ", planWith100FreeMin.getItemId(),existingPlanId);
		
		// 2 Subscription order
		
		//Create subscription order with Active since date 12-15-2015
		
		subscriptionOrderIdWith255MinFreePlan = FullCreativeUtil.createOrder(planWith225FreeMin,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS subscriptionOrderWith225Min  =  api.getOrder(subscriptionOrderIdWith255MinFreePlan);
		logger.debug("Subscription order created with Id :::{}  And Active Since Date :::{}", subscriptionOrderIdWith255MinFreePlan,
			TestConstants.DATE_FORMAT.format(subscriptionOrderWith225Min.getActiveSince()));
        				
		assertNotNull("orderId should not be null",subscriptionOrderIdWith255MinFreePlan);
        				
        				
		existingPlanId = 0;
		for (OrderLineWS lineWS:subscriptionOrderWith225Min.getOrderLines()) {
		    if(lineWS.getItemId().equals(planWith225FreeMin.getItemId())) {
			existingPlanId = lineWS.getItemId();
		    }
		}
		assertEquals("##Subscription Order's Plan ID must be ", planWith225FreeMin.getItemId(),existingPlanId);
		
		CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		
		BigDecimal expectedProratedQuatity = new BigDecimal("178.2258");
		BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
		logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
		assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
		
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith225FreeMin.getId())));
	
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith225FreeMin.getId())));
		
		// Creating Usage Order
		
		Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015),"71.60","56.40","45.20");
		
		OrderWS usageOrder = api.getOrder(usageOrderId);
		assertNotNull("Usage Ordeer should not be null", usageOrder);
		
		BigDecimal overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
	    		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
		customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			BigDecimal customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);
			
	            assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
	            assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);
	            
	            assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("5.03"), 
	            	customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("Mediated Quantity Should be : ",new BigDecimal("173.20"), 
	            		FullCreativeUtil.getOrderTotalQuantity(usageOrder));
	            
	            assertEquals("Usage Order Used free quantity Should not null", "173.2000000000", 
	            	FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());
	            
	            assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("0.00"), 
	            		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("0.00"), 
	            	usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
		
	            OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(subscriptionOrderWith225Min, planWith225FreeMin.getItemId(), planWith450FreeMin.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
	            assertNotNull("Swap changes should be calculated", orderChanges);
	            Integer subscriptionOrderIdWith450Min = api.createUpdateOrder(subscriptionOrderWith225Min, orderChanges);
	    
	            OrderWS subscriptionOrderWith450Min = api.getOrder(subscriptionOrderIdWith450Min);
			assertNotNull("### Order after swap plan should not be null", subscriptionOrderWith450Min);
			
			Integer swapedPlanId = 0;
			for (OrderLineWS lineWS:subscriptionOrderWith450Min.getOrderLines()) {
				if(lineWS.getItemId().equals(planWith450FreeMin.getItemId())) {
					swapedPlanId = lineWS.getItemId(); 
				}
			}
			
		assertEquals("##Subscription Order's swaped Plan ID must be", planWith450FreeMin.getItemId(),swapedPlanId);
		
		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		
		expectedProratedQuatity = new BigDecimal("301.6129");
		actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
		logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
		assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith450FreeMin.getId())));
		
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith450FreeMin.getId())));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
	
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
	
		usageOrder = api.getOrder(usageOrderId);
		
		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
	    		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
		customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);
			
	            assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
	            assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);
	            
	            assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("128.41"), 
	            	customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("Mediated Quantity Should be : ",new BigDecimal("173.20"), 
	            		FullCreativeUtil.getOrderTotalQuantity(usageOrder));
	            
	            assertEquals("Usage Order Used free quantity Should not null", "173.2000000000", 
	            	FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());
	            
	            assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("0.00"), 
	            		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("0.00"), 
	            	usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
    	}
    	
    	@Test
	public void testScenarioE2() throws Exception {
    	//Customer 'Swap Plan Proration' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Swap Plan Proration");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);
		
		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));
		
		
		logger.debug("##Creating Plan Subscription order...");
		
		//Create subscription order with Active since date 12-15-2015
		
		subscriptionOrderIdWith100MinFreePlan = FullCreativeUtil.createOrder(planWith100FreeMin,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS subscriptionOrderWith100Min  =  api.getOrder(subscriptionOrderIdWith100MinFreePlan);
		logger.debug("Subscription order created with Id :::{}  And Active Since Date :::{}", subscriptionOrderIdWith100MinFreePlan,
				TestConstants.DATE_FORMAT.format(subscriptionOrderWith100Min.getActiveSince()));
		
		assertNotNull("orderId should not be null",subscriptionOrderIdWith100MinFreePlan);
		
		user = api.getUserWS(userId);
		
		
		Integer existingPlanId = 0;
		for (OrderLineWS lineWS:subscriptionOrderWith100Min.getOrderLines()) {
			if(lineWS.getItemId().equals(planWith100FreeMin.getItemId())) {
				existingPlanId = lineWS.getItemId();
			}
		}
		
		assertEquals("##Subscription Order's Plan ID must be ", planWith100FreeMin.getItemId(),existingPlanId);
		
		// 2 Subscription order
		
		//Create subscription order with Active since date 12-15-2015
		
		subscriptionOrderIdWith255MinFreePlan = FullCreativeUtil.createOrder(planWith225FreeMin,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS subscriptionOrderWith225Min  =  api.getOrder(subscriptionOrderIdWith255MinFreePlan);
		logger.debug("Subscription order created with Id :::{}  And Active Since Date :::{}", subscriptionOrderIdWith255MinFreePlan,
			TestConstants.DATE_FORMAT.format(subscriptionOrderWith225Min.getActiveSince()));
        				
		assertNotNull("orderId should not be null",subscriptionOrderIdWith255MinFreePlan);
        				
        				
		existingPlanId = 0;
		for (OrderLineWS lineWS:subscriptionOrderWith225Min.getOrderLines()) {
		    if(lineWS.getItemId().equals(planWith225FreeMin.getItemId())) {
			existingPlanId = lineWS.getItemId();
		    }
		}
		assertEquals("##Subscription Order's Plan ID must be ", planWith225FreeMin.getItemId(),existingPlanId);
		
		CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		
		BigDecimal expectedProratedQuatity = new BigDecimal("178.2258");
		BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
		logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
		assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
		
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith225FreeMin.getId())));
	
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith225FreeMin.getId())));
		
		// Creating Usage Order
		
		Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015),"79.60","56.40","45.20");
		
		OrderWS usageOrder = api.getOrder(usageOrderId);
		assertNotNull("Usage Ordeer should not be null", usageOrder);
		
		BigDecimal overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
	    		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
		customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			BigDecimal customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);
			
	            assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
	            assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);
	            
	            assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("0.00"), 
	            	customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("Mediated Quantity Should be : ",new BigDecimal("181.20"), 
	            		FullCreativeUtil.getOrderTotalQuantity(usageOrder));
	            
	            assertEquals("Usage Order Used free quantity Should not null", "178.2258000000", 
	            	FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());
	            
	            assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("2.97"), 
	            		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("2.83"), 
	            	usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
		
	            OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(subscriptionOrderWith225Min, planWith225FreeMin.getItemId(), planWith450FreeMin.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
	            assertNotNull("Swap changes should be calculated", orderChanges);
	            Integer subscriptionOrderIdWith450Min = api.createUpdateOrder(subscriptionOrderWith225Min, orderChanges);
	    
	            OrderWS subscriptionOrderWith450Min = api.getOrder(subscriptionOrderIdWith450Min);
			assertNotNull("### Order after swap plan should not be null", subscriptionOrderWith450Min);
			
			Integer swapedPlanId = 0;
			for (OrderLineWS lineWS:subscriptionOrderWith450Min.getOrderLines()) {
				if(lineWS.getItemId().equals(planWith450FreeMin.getItemId())) {
					swapedPlanId = lineWS.getItemId(); 
				}
			}
			
		assertEquals("##Subscription Order's swaped Plan ID must be", planWith450FreeMin.getItemId(),swapedPlanId);
		
		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		
		expectedProratedQuatity = new BigDecimal("301.6129");
		actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
		logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
		assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith450FreeMin.getId())));
		
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith450FreeMin.getId())));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
	
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
	
		usageOrder = api.getOrder(usageOrderId);
		
		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
	    		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
		customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);
			
	            assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
	            assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);
	            
	            assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("120.41"), 
	            	customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("Mediated Quantity Should be : ",new BigDecimal("181.20"), 
	            		FullCreativeUtil.getOrderTotalQuantity(usageOrder));
	            
	            assertEquals("Usage Order Used free quantity Should not null", "181.2000000000", 
	            	FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());
	            
	            assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("0.00"), 
	            		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("0.00"), 
	            	usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
    	} 
    
    	@Test
	public void testScenarioE3() throws Exception {
		//Customer 'Swap Plan Proration' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Swap Plan Proration");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);
		
		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));
		
		
		logger.debug("##Creating Plan Subscription order...");
		
		//Create subscription order with Active since date 12-15-2015
		
		subscriptionOrderIdWith100MinFreePlan = FullCreativeUtil.createOrder(planWith100FreeMin,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS subscriptionOrderWith100Min  =  api.getOrder(subscriptionOrderIdWith100MinFreePlan);
		logger.debug("Subscription order created with Id :::{}  And Active Since Date :::{}", subscriptionOrderIdWith100MinFreePlan,
				TestConstants.DATE_FORMAT.format(subscriptionOrderWith100Min.getActiveSince()));
		
		assertNotNull("orderId should not be null",subscriptionOrderIdWith100MinFreePlan);
		
		user = api.getUserWS(userId);
		
		
		Integer existingPlanId = 0;
		for (OrderLineWS lineWS:subscriptionOrderWith100Min.getOrderLines()) {
			if(lineWS.getItemId().equals(planWith100FreeMin.getItemId())) {
				existingPlanId = lineWS.getItemId();
			}
		}
		
		assertEquals("##Subscription Order's Plan ID must be ", planWith100FreeMin.getItemId(),existingPlanId);
		
		// 2 Subscription order
		
		//Create subscription order with Active since date 12-15-2015
		
		subscriptionOrderIdWith255MinFreePlan = FullCreativeUtil.createOrder(planWith225FreeMin,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS subscriptionOrderWith225Min  =  api.getOrder(subscriptionOrderIdWith255MinFreePlan);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", subscriptionOrderIdWith255MinFreePlan,
			TestConstants.DATE_FORMAT.format(subscriptionOrderWith225Min.getActiveSince()));
    				
		assertNotNull("orderId should not be null",subscriptionOrderIdWith255MinFreePlan);
    				
    				
		existingPlanId = 0;
		for (OrderLineWS lineWS:subscriptionOrderWith225Min.getOrderLines()) {
		    if(lineWS.getItemId().equals(planWith225FreeMin.getItemId())) {
			existingPlanId = lineWS.getItemId();
		    }
		}
		assertEquals("##Subscription Order's Plan ID must be ", planWith225FreeMin.getItemId(),existingPlanId);
		
		CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		
		BigDecimal expectedProratedQuatity = new BigDecimal("178.2258");
		BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
		logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
		assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
		
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith225FreeMin.getId())));
	
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith225FreeMin.getId())));
		
		// Creating Usage Order
		
		Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015),"140.60","81.40","74.20");
		
		OrderWS usageOrder = api.getOrder(usageOrderId);
		assertNotNull("Usage Ordeer should not be null", usageOrder);
		
		BigDecimal overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
	    		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
		customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			BigDecimal customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);
			
	            assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
	            assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);
	            
	            assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("0.00"), 
	            	customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("Mediated Quantity Should be : ",new BigDecimal("296.20"), 
	            		FullCreativeUtil.getOrderTotalQuantity(usageOrder));
	            
	            assertEquals("Usage Order Used free quantity Should not null", "178.2258000000", 
	            	FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());
	            
	            assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("117.97"), 
	            		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("112.08"), 
	            	usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
		
	            OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(subscriptionOrderWith225Min, planWith225FreeMin.getItemId(), planWith450FreeMin.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
	            assertNotNull("Swap changes should be calculated", orderChanges);
	            Integer subscriptionOrderIdWith450Min = api.createUpdateOrder(subscriptionOrderWith225Min, orderChanges);
	    
	            OrderWS subscriptionOrderWith450Min = api.getOrder(subscriptionOrderIdWith450Min);
			assertNotNull("### Order after swap plan should not be null", subscriptionOrderWith450Min);
			
			Integer swapedPlanId = 0;
			for (OrderLineWS lineWS:subscriptionOrderWith450Min.getOrderLines()) {
				if(lineWS.getItemId().equals(planWith450FreeMin.getItemId())) {
					swapedPlanId = lineWS.getItemId(); 
				}
			}
			
		assertEquals("##Subscription Order's swaped Plan ID must be", planWith450FreeMin.getItemId(),swapedPlanId);
		
		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		
		expectedProratedQuatity = new BigDecimal("301.6129");
		actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
		logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
		assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith450FreeMin.getId())));
		
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith450FreeMin.getId())));
		
		assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
	
		assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, planWith100FreeMin.getId())));
	
		usageOrder = api.getOrder(usageOrderId);
		
		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
	    		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
		customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);
			
	            assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
	            assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);
	            
	            assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("5.41"), 
	            	customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("Mediated Quantity Should be : ",new BigDecimal("296.20"), 
	            		FullCreativeUtil.getOrderTotalQuantity(usageOrder));
	            
	            assertEquals("Usage Order Used free quantity Should not null", "296.2000000000", 
	            	FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());
	            
	            assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("0.00"), 
	            		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
	            
	            assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("0.00"), 
	            	usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
	}
}
