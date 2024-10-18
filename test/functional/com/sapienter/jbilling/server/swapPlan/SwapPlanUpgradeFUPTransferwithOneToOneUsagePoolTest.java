package com.sapienter.jbilling.server.swapPlan;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
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
 * 
 * Scenario A1, A2, A3
 * 
 * @author Ashok Kale
 * 
 */
@Test(groups = { "swapPlan" }, testName = "SwapPlanUpgradeFUPTransferwithOneToOneUsagePoolTest")
public class SwapPlanUpgradeFUPTransferwithOneToOneUsagePoolTest {

	private static final Logger logger = LoggerFactory.getLogger(SwapPlanUpgradeFUPTransferwithOneToOneUsagePoolTest.class);
    private JbillingAPI api;
    private PlanWS afbestvaluePlanWith225Min;
    private PlanWS afIntroPlanwith100Min;
    private Integer userId;
    private	Integer orderId = null;
    private Integer onetimeOrderId = null;
    private BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	private BigDecimal overageQuantity = BigDecimal.ZERO;
    UserWS user = null;
    
    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        UsagePoolWS usagePoolWith100Quantity = FullCreativeUtil.populateFreeUsagePoolObject("100");
    	usagePoolWith100Quantity.setId(api.createUsagePool(usagePoolWith100Quantity));
    	
    	UsagePoolWS usagePoolWith225Quantity = FullCreativeUtil.populateFreeUsagePoolObject("225");
    	usagePoolWith225Quantity.setId(api.createUsagePool(usagePoolWith225Quantity));
    	
    	assertNotNull("Usage Pool Creation Failed ", usagePoolWith100Quantity);
    	assertNotNull("Usage Pool Creation Failed ", usagePoolWith225Quantity);
	
    	afbestvaluePlanWith225Min = FullCreativeUtil.createPlan("225", "0.95", 
    			new Integer[]{usagePoolWith225Quantity.getId()},
    			"Test Plan 225 Min", api, 
    			TestConstants.CHAT_USAGE_PRODUCT_ID, 
    			TestConstants.INBOUND_USAGE_PRODUCT_ID, 
    			TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	
    	assertNotNull("planWS should not be null", afbestvaluePlanWith225Min);
    	
    	afIntroPlanwith100Min = FullCreativeUtil.createPlan("100", "0.95", 
    			new Integer[]{usagePoolWith100Quantity.getId()},
    			"Test Plan 100 Min", api, 
    			TestConstants.CHAT_USAGE_PRODUCT_ID, 
    			TestConstants.INBOUND_USAGE_PRODUCT_ID, 
    			TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	
    	assertNotNull("planWS should not be null", afIntroPlanwith100Min);
    }
    
    @Test
	public void test001SwapPlanProration() throws Exception {
    	//Customer 'Swap Plan Proration' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Swap Plan Proration");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);
		
		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));
		
		logger.debug("planWS.getUsagePoolIds()[0] ::::::::;;{}", afIntroPlanwith100Min.getUsagePoolIds()[0]);
		logger.debug("##Creating Plan Subscription order...");
		
		//Create subscription order with Active since date 12-12-2015
		orderId = FullCreativeUtil.createOrder(afIntroPlanwith100Min,user.getUserId(),FullCreativeUtil.getDate(11, 12, 2015),null);
		OrderWS order  =  api.getOrder(orderId);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
				TestConstants.DATE_FORMAT.format(order.getActiveSince()));
		
		assertNotNull("orderId should not be null",orderId);
		
		user = api.getUserWS(userId);
		
		logger.debug("Customer Usage Pool Prorated Quantity :::{}",
				api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
		
		CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("64.5161"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 12, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("Expected Cycle end date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		
        order = api.getOrder(orderId);
		assertNotNull("getOrder should not be null", order);
		
		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, 
				afIntroPlanwith100Min.getItemId(), 
				afbestvaluePlanWith225Min.getItemId(), 
				SwapMethod.DIFF, 
				Util.truncateDate(order.getActiveSince()));
		
        assertNotNull("Swap changes should be calculated", orderChanges);
        api.createUpdateOrder(order, orderChanges);
        
        order = api.getOrder(orderId);
		assertNotNull("### Order after swap plan should not be null", order);
		assertEquals("## Subscription Order Amount must be 225.00", "225.0000000000",order.getTotal());
		
		logger.debug("##Subscription Order's swapped Plan ID ::: {}", afbestvaluePlanWith225Min.getId());
		
		customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("145.1613"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("Expected Cycle start date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 12, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("Expected Cycle end date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
	}
    
    /**
      # Plan Swap - Upgrade of plan A1 Scenario
	  # Old plan containing one usage pool to new plan also containing one usage pool
      # A plan subscription is upgraded from 100 mins free to 225 mins free
      # Bundle Item Price for usage product remains the same on both plans. 
      # No overage before or after the plan swap.
     */
    @Test
	public void test002SwapPlanFUPTransferScenarioA1() throws Exception {
    	//Customer A1 with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer A1 - ");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);
		
		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));
		
		logger.debug("##Creating Plan Subscription order with AF Intro Plan (100 Min Free usage Pool)...");
		
		//Create subscription order with Active since date 12-12-2015
		orderId = FullCreativeUtil.createOrder(afIntroPlanwith100Min, user.getUserId(),FullCreativeUtil.getDate(11, 12, 2015),null);
		OrderWS order  =  api.getOrder(orderId);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
				TestConstants.DATE_FORMAT.format(order.getActiveSince()));
		
		assertNotNull("orderId should not be null",orderId);
		
		user = api.getUserWS(userId);
		
		logger.debug("Customer Usage Pool Prorated Quantity :::{}",
				api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
		
		CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("64.5161"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

		assertEquals("Expected Cycle start date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 12, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("Expected Cycle end date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		
		// Create One Time Usage Order 
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 12, 2015) ,"13.6","16.4","12.2");
        OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
        
        overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
        		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
        
        logger.debug("Available Quantity of customer usage pool :::{}",
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
        
        assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);
        
        assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("22.32"), 
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("Mediated Quantity Should be : ",new BigDecimal("42.20"), 
        		FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
        
        assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("42.2000000000"), 
        		FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
        		overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
        		oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
		
        OrderWS retOrder = api.getOrder(orderId);
		assertNotNull("getOrder should not be null", retOrder);
		
		// Swap old plan - AF Intro Plan (100 min FUP) TO New Plan AF Best Value Plan (225 min FUP)
		logger.debug("##Swapping existing AF Intro Plan (100 Min Free usage Pool) With AF Best Value Plan (225 Min Free usage Pool)...");
		
		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(retOrder, 
				afIntroPlanwith100Min.getItemId(), 
				afbestvaluePlanWith225Min.getItemId(), 
				SwapMethod.DIFF,  
				Util.truncateDate(retOrder.getActiveSince()));
		
        assertNotNull("Swap changes should be calculated", orderChanges);
        
        api.createUpdateOrder(retOrder, orderChanges);
		
        retOrder = api.getOrder(orderId);
        logger.debug("Swapped to Plan Id :::{}", afbestvaluePlanWith225Min.getId());
        
		assertNotNull("After swap plan order should not be null", retOrder);
		assertEquals("Subscription Order Amount must be", "225.0000000000",retOrder.getTotal());
		
		user = api.getUserWS(user.getId());
		
		logger.debug("Customer Usage Pool Prorated Quantity :::{}",
				api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
		
		customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("After Swap Plan Usage pool Prorated quantity should be ",new BigDecimal("145.1613"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("After Swap Plan Expected Cycle start date of new customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 12, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("After Swap Plan Expected Cycle end date of new customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		
		logger.debug("##Subscription Order's swapped Plan ID ::: {}", afbestvaluePlanWith225Min.getId());
		
		oneTimeUsageOrder = api.getOrder(onetimeOrderId);
        
        overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
        		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
		
        logger.debug("After Swap Plan Available Quantity of customer usage pool :::{}",
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
        
		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("42.2000000000"),
        		FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("42.20"), 
        		FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
        		overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
        		oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("102.96"), 
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
	}
    
    /**
     # Plan Swap - Upgrade of plan A2 Scenario
	 # Old plan containing one usage pool to new plan also containing one usage pool
	 # A plan subscription is upgraded from 100 mins free to 225 mins free
	 # Bundle Item Price for usage product remains the same on both plans. 
	 # Overage before the plan swap but no overage after the plan swap.	
     */
    @Test
	public void test003SwapPlanFUPTransferScenarioA2() throws Exception {
    	//Customer A2 with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer A2 - ");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);
		
		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));
		
		logger.debug("##Creating Plan Subscription order with AF Intro Plan (100 Min Free usage Pool)...");
		
		//Create subscription order with Active since date 12-15-2015
		orderId = FullCreativeUtil.createOrder(afIntroPlanwith100Min, user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS order  =  api.getOrder(orderId);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
				TestConstants.DATE_FORMAT.format(order.getActiveSince()));
		
		assertNotNull("orderId should not be null",orderId);
		
		user = api.getUserWS(userId);
		
		logger.debug("Customer Usage Pool Prorated Quantity :::{}",
				api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
		
		CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("54.8387"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

		assertEquals("Expected Cycle start date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("Expected Cycle end date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		
		// Create One Time Usage Order 
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"33.6","16.4","12.2");
        OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
        
        overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
        		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
        
        logger.debug("Available Quantity of customer usage pool :::{}",
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
        
        assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);
        
        assertEquals("Mediated Quantity Should be : ",new BigDecimal("62.20"), 
        		FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
        
        assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("54.8387000000"), 
        		FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("7.36"), 
        		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("6.99"), 
        		oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO, 
        		customerUsagePoolQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));
		
        OrderWS retOrder = api.getOrder(orderId);
		assertNotNull("getOrder should not be null", retOrder);
		
		// Swap old plan - AF Intro Plan (100 min FUP) TO New Plan AF Best Value Plan (225 min FUP)
		logger.debug("##Swapping existing AF Intro Plan (100 Min Free usage Pool) With AF Best Value Plan (225 Min Free usage Pool)...");
		
		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(retOrder, 
				afIntroPlanwith100Min.getItemId(), 
				afbestvaluePlanWith225Min.getItemId(), 
				SwapMethod.DIFF,  
				Util.truncateDate(retOrder.getActiveSince()));
		
        assertNotNull("Swap changes should be calculated", orderChanges);
        
        api.createUpdateOrder(retOrder, orderChanges);
		
        retOrder = api.getOrder(orderId);
        logger.debug("Swapped to Plan Id :::{}", afbestvaluePlanWith225Min.getId());
        
		assertNotNull("After swap plan order should not be null", retOrder);
		assertEquals("Subscription Order Amount must be", "225.0000000000",retOrder.getTotal());
		
		user = api.getUserWS(user.getId());
		logger.debug("Customer Usage Pool Prorated Quantity :::{}",
				api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
		
		customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("After Swap Plan Usage pool Prorated quantity should be ",new BigDecimal("123.3871"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("After Swap Plan Expected Cycle start date of new customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("After Swap Plan Expected Cycle end date of new customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		
		logger.debug("##Subscription Order's swapped Plan ID ::: {}", afbestvaluePlanWith225Min.getId());
		
		oneTimeUsageOrder = api.getOrder(onetimeOrderId);
        
        overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
        		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
		
        logger.debug("After Swap Plan Available Quantity of customer usage pool :::{}",
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
        
		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("62.2000000000"),
        		FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("62.20"), 
        		FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
        		overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
        		oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("61.19"), 
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
	}
    
    /**
     # Plan Swap - Upgrade of plan A3 Scenario
	 # Old plan containing one usage pool to new plan also containing one usage pool
	 # A plan subscription is upgraded from 100 mins free to 225 mins free.
	 # Bundle Item Price for usage product remains the same on both plans.
	 # Overage before the plan swap but no overage after the plan swap.
     */
    @Test
	public void test004SwapPlanFUPTransferScenarioA3() throws Exception {
    	//Customer A3 with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer A3 - ");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);
		
		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));
		
		logger.debug("##Creating Plan Subscription order with AF Intro Plan (100 Min Free usage Pool)...");
		
		//Create subscription order with Active since date 12-15-2015
		orderId = FullCreativeUtil.createOrder(afIntroPlanwith100Min, user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS order  =  api.getOrder(orderId);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
				TestConstants.DATE_FORMAT.format(order.getActiveSince()));
		
		assertNotNull("orderId should not be null",orderId);
		
		user = api.getUserWS(userId);
		
		logger.debug("Customer Usage Pool Prorated Quantity :::{}",
				api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
		
		CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("54.8387"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

		assertEquals("Expected Cycle start date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("Expected Cycle end date of customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		
		// Create One Time Usage Order 
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"63.6","36.4","22.2");
        OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
        logger.debug("One time Mediated usage order Total Quantity :::{}", FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
        
        overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
        		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
        
        logger.debug("Available Quantity of customer usage pool :::{}",
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
        
        assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);
        
        assertEquals("Mediated Quantity Should be : ",new BigDecimal("122.20"), 
        		FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
        
        assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("54.8387000000"), 
        		FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("67.36"), 
        		overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("63.99"), 
        		oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO, 
        		customerUsagePoolQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));
		
        OrderWS retOrder = api.getOrder(orderId);
		assertNotNull("getOrder should not be null", retOrder);
		
		// Swap old plan - AF Intro Plan (100 min FUP) TO New Plan AF Best Value Plan (225 min FUP)
		logger.debug("##Swapping existing AF Intro Plan (100 Min Free usage Pool) With AF Best Value Plan (225 Min Free usage Pool)...");
		
		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(retOrder, 
				afIntroPlanwith100Min.getItemId(), 
				afbestvaluePlanWith225Min.getItemId(), 
				SwapMethod.DIFF,  
				Util.truncateDate(retOrder.getActiveSince()));
		
        assertNotNull("Swap changes should be calculated", orderChanges);
        
        api.createUpdateOrder(retOrder, orderChanges);
		
        retOrder = api.getOrder(orderId);
        logger.debug("Swapped to Plan Id :::{}", afbestvaluePlanWith225Min.getId());
        
		assertNotNull("After swap plan order should not be null", retOrder);
		assertEquals("Subscription Order Amount must be", "225.0000000000",retOrder.getTotal());
		
		user = api.getUserWS(user.getId());
		
		logger.debug("Customer Usage Pool Prorated Quantity :::{}",
				api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
		
		customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
		
		assertEquals("After Swap Plan Usage pool Prorated quantity should be ",new BigDecimal("123.3871"), customerUsagePoolWS.
				getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
		
		assertEquals("After Swap Plan Expected Cycle start date of new customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));
		
		assertEquals("After Swap Plan Expected Cycle end date of new customer usage pool: "
				,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
				,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		
		logger.debug("##Subscription Order's swapped Plan ID ::: {}", afbestvaluePlanWith225Min.getId());
		
		oneTimeUsageOrder = api.getOrder(onetimeOrderId);
        
        overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
        		.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
        
        logger.debug("After Swap Plan Available Quantity of customer usage pool :::{}",
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
		
		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("122.2000000000"),
        		FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("122.20"), 
        		FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
        
        assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
        		overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
        		oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
        
        assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("1.19"), 
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
	}
}
