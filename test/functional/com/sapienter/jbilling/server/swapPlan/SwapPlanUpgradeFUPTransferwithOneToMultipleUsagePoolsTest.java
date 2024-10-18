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
@Test(groups = { "swapPlan" })
public class SwapPlanUpgradeFUPTransferwithOneToMultipleUsagePoolsTest {

	private static final Logger logger = LoggerFactory.getLogger(SwapPlanUpgradeFUPTransferwithOneToMultipleUsagePoolsTest.class);
    private JbillingAPI api;
    private Integer userId;
    private Integer orderId = null;
    private Integer onetimeOrderId = null;
    UserWS user = null;
    PlanWS downGradePlanwith100Min;
    PlanWS downGradePlanwith225Min;
    PlanWS upGradePlanwith100Min;
    PlanWS upGradePlanwith225Min;
    @BeforeClass
    protected void setUp() throws Exception {
    	api = JbillingAPIFactory.getAPI();
    	UsagePoolWS usagePoolWith100Quantity = FullCreativeUtil.populateFreeUsagePoolObject("100");
    	usagePoolWith100Quantity.setId(api.createUsagePool(usagePoolWith100Quantity));

    	UsagePoolWS usagePoolWith225Quantity = FullCreativeUtil.populateFreeUsagePoolObject("225");
    	UsagePoolWS usagePoolWith85Quantity = FullCreativeUtil.populateFreeUsagePoolObject("85");

    	usagePoolWith225Quantity.setId(api.createUsagePool(usagePoolWith225Quantity));
    	usagePoolWith85Quantity.setId(api.createUsagePool(usagePoolWith85Quantity));
    	assertNotNull("Usage Pool Creation Failed ", usagePoolWith100Quantity);
    	assertNotNull("Usage Pool Creation Failed ", usagePoolWith85Quantity);
    	assertNotNull("Usage Pool Creation Failed ", usagePoolWith225Quantity);

    	upGradePlanwith225Min = FullCreativeUtil.createPlan("225", "0.95", 
    			new Integer[]{usagePoolWith225Quantity.getId(),usagePoolWith85Quantity.getId()},
    			"Test Plan 225 Min", api,
    			TestConstants.CHAT_USAGE_PRODUCT_ID,
    			TestConstants.INBOUND_USAGE_PRODUCT_ID,
    			TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	assertNotNull("planWS should not be null", upGradePlanwith225Min);

    	upGradePlanwith100Min = FullCreativeUtil.createPlan("100", "0.95", 
    			new Integer[]{usagePoolWith100Quantity.getId()},
    			"Test Plan 100 Min", api, 
    			TestConstants.CHAT_USAGE_PRODUCT_ID, 
    			TestConstants.INBOUND_USAGE_PRODUCT_ID,
    			TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	assertNotNull("planWS should not be null", upGradePlanwith100Min);
    }	
    
    /**
   	# Plan Swap - Upgrade of plan C1 Scenario
   	# Old plan  containing one usage pool to new plan containing multiple usage pools
   	# A plan subscription is upgraded from 100 mins free to 225 mins free.
   	# Bundle Item Price for usage product remains the same on both plans. 
   	# No overage before or after the plan swap.
     */
    @Test
    public void testScenarioC1() throws Exception {
    	//Customer 'Customer c1' with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer C1 -");
    	userId = user.getId();
    	assertNotNull("user should not be null",user);
    	user = api.getUserWS(userId);

    	user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
    	api.updateUser(user);

    	user = api.getUserWS(userId);
    	logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));


    	logger.debug("##Creating Plan Subscription order...");

    	//Create subscription order with Active since date 12-15-2015

    	orderId = FullCreativeUtil.createOrder(upGradePlanwith100Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
    	OrderWS order  =  api.getOrder(orderId);
    	logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
    			TestConstants.DATE_FORMAT.format(order.getActiveSince()));

    	assertNotNull("orderId should not be null",orderId);

    	user = api.getUserWS(userId);

    	CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

    	BigDecimal expectedProratedQuatity = new BigDecimal("54.8387000000");
    	BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
    	logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
    	assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, upGradePlanwith100Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, upGradePlanwith100Min.getId())));

    	Integer existingPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(upGradePlanwith100Min.getItemId())) {
    			existingPlanId = lineWS.getItemId();
    		}
    	}
    	assertEquals("##Subscription Order's Plan ID must be ", upGradePlanwith100Min.getItemId(),existingPlanId);

    	order = api.getOrder(orderId);
    	assertNotNull("getOrder should not be null", order);

    	Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015), "13.60","16.40","12.20");

    	OrderWS usageOrder = api.getOrder(usageOrderId);
    	assertNotNull("Usage Ordeer should not be null", usageOrder);

    	BigDecimal overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
    			.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
    	customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	BigDecimal customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);

    	assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
    	assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);

    	assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("12.64"), 
    			customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("42.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "42.2000000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
    			overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
    			usageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));


    	OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId(), upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
    	assertNotNull("Swap changes should be calculated", orderChanges);
    	api.createUpdateOrder(order, orderChanges);

    	order = api.getOrder(orderId);
    	assertNotNull("### Order after swap plan should not be null", order);
    	assertEquals("## Subscription Order Amount must be 200.00", "225.0000000000",order.getTotal());

    	Integer swapedPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(upGradePlanwith225Min.getItemId())) {
    			swapedPlanId = lineWS.getItemId(); 
    		}
    	}
    	assertEquals("##Subscription Order's swaped Plan ID must be", upGradePlanwith225Min.getItemId(),swapedPlanId);

    	customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);
    	logger.debug("ActualProratedQuantity::::::::::::::::{}", actualProratedQuantity);
    	expectedProratedQuatity = new BigDecimal("170.0000").setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);

    	assertEquals("Usage pool Prorated quantity should be ", expectedProratedQuatity, actualProratedQuantity);

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, upGradePlanwith225Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, upGradePlanwith225Min.getId())));

    	usageOrder = api.getOrder(usageOrderId);
    	assertNotNull("Usage Ordeer should not be null", usageOrder);

    	overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
    			.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
    	customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);

    	assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
    	assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);

    	assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("127.80"), 
    			customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("42.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "42.2000000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
    			overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
    			usageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
    }

    /**
	# Plan Swap - Upgrade of plan C2 Scenario
	# Old plan  containing one usage pool to new plan containing multiple usage pools
	# A plan subscription is upgraded from 100 mins free to 225 mins free.
	# Bundle Item Price for usage product remains the same on both plans. 
	# Overage before the plan swap but no overage after the plan swap.
     */
    @Test
    public void testScenarioC2() throws Exception {
    	//Customer 'Swap Plan Proration' with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer C2 -");
    	userId = user.getId();
    	assertNotNull("user should not be null",user);
    	user = api.getUserWS(userId);

    	user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
    	api.updateUser(user);

    	user = api.getUserWS(userId);
    	logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));


    	logger.debug("##Creating Plan Subscription order...");

    	//Create subscription order with Active since date 12-15-2015

    	orderId = FullCreativeUtil.createOrder(upGradePlanwith100Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
    	OrderWS order  =  api.getOrder(orderId);
    	logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
    			TestConstants.DATE_FORMAT.format(order.getActiveSince()));

    	assertNotNull("orderId should not be null",orderId);

    	user = api.getUserWS(userId);

    	CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

    	BigDecimal expectedProratedQuatity = new BigDecimal("54.8387000000");
    	BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
    	logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
    	assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, upGradePlanwith100Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, upGradePlanwith100Min.getId())));

    	Integer existingPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(upGradePlanwith100Min.getItemId())) {
    			existingPlanId = lineWS.getItemId();
    		}
    	}
    	assertEquals("##Subscription Order's Plan ID must be ", upGradePlanwith100Min.getItemId(),existingPlanId);

    	order = api.getOrder(orderId);
    	assertNotNull("getOrder should not be null", order);

    	Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015), "63.60","36.40","22.20");

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

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("122.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "54.8387000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("67.36"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("63.99"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));



    	OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId(), upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
    	assertNotNull("Swap changes should be calculated", orderChanges);
    	api.createUpdateOrder(order, orderChanges);

    	order = api.getOrder(orderId);
    	assertNotNull("### Order after swap plan should not be null", order);
    	assertEquals("## Subscription Order Amount must be 200.00", "225.0000000000",order.getTotal());

    	Integer swapedPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(upGradePlanwith225Min.getItemId())) {
    			swapedPlanId = lineWS.getItemId(); 
    		}
    	}
    	assertEquals("##Subscription Order's swaped Plan ID must be", upGradePlanwith225Min.getItemId(),swapedPlanId);

    	customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);
    	logger.debug("ActualProratedQuantity::::::::::::::::{}", actualProratedQuantity);
    	expectedProratedQuatity = new BigDecimal("170.0000").setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);

    	assertEquals("Usage pool Prorated quantity should be ", expectedProratedQuatity, actualProratedQuantity);

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, upGradePlanwith225Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, upGradePlanwith225Min.getId())));

    	usageOrder = api.getOrder(usageOrderId);
    	assertNotNull("Usage Ordeer should not be null", usageOrder);

    	overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
    			.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
    	customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);

    	assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
    	assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);

    	assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("47.80"), 
    			customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("122.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "122.2000000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
    			overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
    			usageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
    }

    /**
	# Plan Swap - Upgrade of plan C3 Scenario
	# Old plan  containing one usage pool to new plan containing multiple usage pools
	# A plan subscription is upgraded from 100 mins free to 225 mins free.
	# Bundle Item Price for usage product remains the same on both plans. 
	# Overage before and after the plan swap.
     */
    @Test
    public void testScenarioC3() throws Exception {
    	//Customer 'Customer C2' with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer C2 -");
    	userId = user.getId();
    	assertNotNull("user should not be null",user);
    	user = api.getUserWS(userId);

    	user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
    	api.updateUser(user);

    	user = api.getUserWS(userId);
    	logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));


    	logger.debug("##Creating Plan Subscription order...");

    	//Create subscription order with Active since date 12-15-2015

    	orderId = FullCreativeUtil.createOrder(upGradePlanwith100Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
    	OrderWS order  =  api.getOrder(orderId);
    	logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
    			TestConstants.DATE_FORMAT.format(order.getActiveSince()));

    	assertNotNull("orderId should not be null",orderId);

    	user = api.getUserWS(userId);

    	CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

    	BigDecimal expectedProratedQuatity = new BigDecimal("54.8387000000");
    	BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
    	logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
    	assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, upGradePlanwith100Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, upGradePlanwith100Min.getId())));

    	Integer existingPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(upGradePlanwith100Min.getItemId())) {
    			existingPlanId = lineWS.getItemId();
    		}
    	}
    	assertEquals("##Subscription Order's Plan ID must be ", upGradePlanwith100Min.getItemId(),existingPlanId);

    	order = api.getOrder(orderId);
    	assertNotNull("getOrder should not be null", order);

    	Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015), "73.60","56.40","45.20");

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

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("175.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "54.8387000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("120.36"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("114.34"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));


    	OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId(), upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
    	assertNotNull("Swap changes should be calculated", orderChanges);
    	api.createUpdateOrder(order, orderChanges);

    	order = api.getOrder(orderId);
    	assertNotNull("### Order after swap plan should not be null", order);
    	assertEquals("## Subscription Order Amount must be 200.00", "225.0000000000",order.getTotal());

    	Integer swapedPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(upGradePlanwith225Min.getItemId())) {
    			swapedPlanId = lineWS.getItemId(); 
    		}
    	}
    	assertEquals("##Subscription Order's swaped Plan ID must be", upGradePlanwith225Min.getItemId(),swapedPlanId);

    	customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);
    	logger.debug("ActualProratedQuantity::::::::::::::::{}", actualProratedQuantity);
    	expectedProratedQuatity = new BigDecimal("170.0000").setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);

    	assertEquals("Usage pool Prorated quantity should be ", expectedProratedQuatity, actualProratedQuantity);

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, upGradePlanwith225Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, upGradePlanwith225Min.getId())));

    	usageOrder = api.getOrder(usageOrderId);
    	assertNotNull("Usage Ordeer should not be null", usageOrder);

    	overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
    			.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
    	customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);

    	assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
    	assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);

    	assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("0.00"), 
    			customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("175.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "170.0000000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("5.20"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("4.94"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
    }

}
