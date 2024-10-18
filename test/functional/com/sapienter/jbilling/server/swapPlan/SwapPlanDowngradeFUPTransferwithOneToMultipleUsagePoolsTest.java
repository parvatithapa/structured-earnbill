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
@Test(groups = { "swapPlan" }, testName = "SwapPlanDowngradeFUPTransferwithOneToMultipleUsagePoolsTest")
public class SwapPlanDowngradeFUPTransferwithOneToMultipleUsagePoolsTest {

	private static final Logger logger = LoggerFactory.getLogger(SwapPlanDowngradeFUPTransferwithOneToMultipleUsagePoolsTest.class);
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

    	downGradePlanwith100Min = FullCreativeUtil.createPlan("100", "0.95",
    			new Integer[]{usagePoolWith100Quantity.getId(),
    			usagePoolWith85Quantity.getId()},
    			"Test Plan 100 Min", api,
    			TestConstants.CHAT_USAGE_PRODUCT_ID,
    			TestConstants.INBOUND_USAGE_PRODUCT_ID,
    			TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	assertNotNull("planWS should not be null", downGradePlanwith100Min);

    	downGradePlanwith225Min = FullCreativeUtil.createPlan("225", "0.95",
    			new Integer[]{usagePoolWith225Quantity.getId()},
    			"Test Plan 225 Min", api, 
    			TestConstants.CHAT_USAGE_PRODUCT_ID,
    			TestConstants.INBOUND_USAGE_PRODUCT_ID,
    			TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
    	assertNotNull("planWS should not be null", downGradePlanwith225Min);

    }	

    /**
	# Plan Swap - DownGrade of plan C4 Scenario
	# Old plan containing one usage pool to new plan also containing one usage pool
	# A plan subscription is downgraded from 225 mins free to 100 mins free. 
	# Bundle Item Price for usage product remains the same on both plans. 
	# No overage before and after the plan swap.
     */
    @Test
    public void testScenarioC4() throws Exception {
    	//Customer 'Customer C4' with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer C4 -");
    	userId = user.getId();
    	assertNotNull("user should not be null",user);
    	user = api.getUserWS(userId);

    	user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
    	api.updateUser(user);

    	user = api.getUserWS(userId);
    	logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));


    	logger.debug("##Creating Plan Subscription order...");

    	//Create subscription order with Active since date 12-15-2015

    	orderId = FullCreativeUtil.createOrder(downGradePlanwith225Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
    	OrderWS order  =  api.getOrder(orderId);
    	logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
    			TestConstants.DATE_FORMAT.format(order.getActiveSince()));

    	assertNotNull("orderId should not be null",orderId);

    	user = api.getUserWS(userId);

    	CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

    	BigDecimal expectedProratedQuatity = new BigDecimal("123.3871");
    	BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
    	logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
    	assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, downGradePlanwith225Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, downGradePlanwith225Min.getId())));

    	Integer existingPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(downGradePlanwith225Min.getItemId())) {
    			existingPlanId = lineWS.getItemId();
    		}
    	}
    	assertEquals("##Subscription Order's Plan ID must be ", downGradePlanwith225Min.getItemId(),existingPlanId);

    	order = api.getOrder(orderId);
    	assertNotNull("getOrder should not be null", order);

    	Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015),"43.60","26.40"	,"22.20");

    	OrderWS usageOrder = api.getOrder(usageOrderId);
    	assertNotNull("Usage Ordeer should not be null", usageOrder);

    	BigDecimal overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
    			.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
    	customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	BigDecimal customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);

    	assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
    	assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);

    	assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("31.19"), 
    			customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("92.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "92.2000000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("0.00"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("0.00"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));



    	OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, downGradePlanwith225Min.getItemId(), downGradePlanwith100Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
    	assertNotNull("Swap changes should be calculated", orderChanges);
    	api.createUpdateOrder(order, orderChanges);

    	order = api.getOrder(orderId);
    	assertNotNull("### Order after swap plan should not be null", order);

    	Integer swapedPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(downGradePlanwith100Min.getItemId())) {
    			swapedPlanId = lineWS.getItemId(); 
    		}
    	}
    	assertEquals("##Subscription Order's swaped Plan ID must be", downGradePlanwith100Min.getItemId(),swapedPlanId);

    	customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);
    	logger.debug("ActualProratedQuantity::::::::::::::::{}", actualProratedQuantity);
    	expectedProratedQuatity = new BigDecimal("101.4516").setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);

    	assertEquals("Usage pool Prorated quantity should be ", expectedProratedQuatity, actualProratedQuantity);

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, downGradePlanwith100Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, downGradePlanwith100Min.getId())));

    	usageOrder = api.getOrder(usageOrderId);
    	assertNotNull("Usage Ordeer should not be null", usageOrder);

    	overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
    			.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
    	customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);

    	assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
    	assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);

    	assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("9.25"), 
    			customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("92.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "92.2000000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("0.00"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("0.00"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    /**
   	# Plan Swap - DownGrade of plan C5 Scenario
   	# Old plan containing one usage pool to new plan also containing one usage pool
   	# A plan subscription is downgraded from 225 mins free to 100 mins free. 
   	# Bundle Item Price for usage product remains the same on both plans. 
   	# Overage before and after the plan swap.
     */
    @Test
    public void testScenarioC5() throws Exception {
    	//Customer 'Customer C5' with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer C5 -");
    	userId = user.getId();
    	assertNotNull("user should not be null",user);
    	user = api.getUserWS(userId);

    	user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
    	api.updateUser(user);

    	user = api.getUserWS(userId);
    	logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));


    	logger.debug("##Creating Plan Subscription order...");

    	//Create subscription order with Active since date 12-15-2015

    	orderId = FullCreativeUtil.createOrder(downGradePlanwith225Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
    	OrderWS order  =  api.getOrder(orderId);
    	logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
    			TestConstants.DATE_FORMAT.format(order.getActiveSince()));

    	assertNotNull("orderId should not be null",orderId);

    	user = api.getUserWS(userId);

    	CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

    	BigDecimal expectedProratedQuatity = new BigDecimal("123.3871");
    	BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
    	logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
    	assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, downGradePlanwith225Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, downGradePlanwith225Min.getId())));

    	Integer existingPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(downGradePlanwith225Min.getItemId())) {
    			existingPlanId = lineWS.getItemId();
    		}
    	}
    	assertEquals("##Subscription Order's Plan ID must be ", downGradePlanwith225Min.getItemId(),existingPlanId);

    	order = api.getOrder(orderId);
    	assertNotNull("getOrder should not be null", order);

    	Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015),"84.60","46.40","22.20");

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

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("153.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "123.3871000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("29.81"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("28.32"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));



    	OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, downGradePlanwith225Min.getItemId(), downGradePlanwith100Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
    	assertNotNull("Swap changes should be calculated", orderChanges);
    	api.createUpdateOrder(order, orderChanges);

    	order = api.getOrder(orderId);
    	assertNotNull("### Order after swap plan should not be null", order);

    	Integer swapedPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(downGradePlanwith100Min.getItemId())) {
    			swapedPlanId = lineWS.getItemId(); 
    		}
    	}
    	assertEquals("##Subscription Order's swaped Plan ID must be", downGradePlanwith100Min.getItemId(),swapedPlanId);

    	customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);
    	logger.debug("ActualProratedQuantity::::::::::::::::{}", actualProratedQuantity);
    	expectedProratedQuatity = new BigDecimal("101.4516").setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);

    	assertEquals("Usage pool Prorated quantity should be ", expectedProratedQuatity, actualProratedQuantity);

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, downGradePlanwith100Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, downGradePlanwith100Min.getId())));

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

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("153.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "101.4516000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("51.75"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("49.16"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
    } 

    /**
	# Plan Swap - DownGrade of plan C6 Scenario
	# Old plan containing one usage pool to new plan also containing one usage pool
	# A plan subscription is downgraded from 225 mins free to 100 mins free. 
	# Bundle Item Price for usage product remains the same on both plans. 
	# No overage before but overage after the plan swap.
     */
    @Test
    public void testScenarioC6() throws Exception {
    	//Customer 'Customer C6 -' with Next Invoice Date 01-01-2016
    	UserWS user  = FullCreativeUtil.createUser("Customer C6 -");
    	userId = user.getId();
    	assertNotNull("user should not be null",user);
    	user = api.getUserWS(userId);

    	user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
    	api.updateUser(user);

    	user = api.getUserWS(userId);
    	logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));


    	logger.debug("##Creating Plan Subscription order...");

    	//Create subscription order with Active since date 12-15-2015

    	orderId = FullCreativeUtil.createOrder(downGradePlanwith225Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
    	OrderWS order  =  api.getOrder(orderId);
    	logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
    			TestConstants.DATE_FORMAT.format(order.getActiveSince()));

    	assertNotNull("orderId should not be null",orderId);

    	user = api.getUserWS(userId);

    	CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

    	BigDecimal expectedProratedQuatity = new BigDecimal("123.3871");
    	BigDecimal actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools);
    	logger.debug("ActualProratedQuantity:::::::::::::::{}", actualProratedQuantity);
    	assertEquals("Usage pool Prorated quantity should be ",expectedProratedQuatity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP), actualProratedQuantity.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, downGradePlanwith225Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, downGradePlanwith225Min.getId())));

    	Integer existingPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(downGradePlanwith225Min.getItemId())) {
    			existingPlanId = lineWS.getItemId();
    		}
    	}
    	assertEquals("##Subscription Order's Plan ID must be ", downGradePlanwith225Min.getItemId(),existingPlanId);

    	order = api.getOrder(orderId);
    	assertNotNull("getOrder should not be null", order);

    	Integer usageOrderId = FullCreativeUtil.createOneTimeOrder(user.getId(), FullCreativeUtil.getDate(11, 15, 2015),"60.60","36.40","22.20");

    	OrderWS usageOrder = api.getOrder(usageOrderId);
    	assertNotNull("Usage Ordeer should not be null", usageOrder);

    	BigDecimal overageQuantity = FullCreativeUtil.getOrderTotalQuantity(usageOrder)
    			.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder));
    	customerUsagePools = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	BigDecimal customerAvailableUsagePoolQuantity = FullCreativeUtil.getCustomerAvailableQuantity(customerUsagePools);

    	assertNotNull("FreeUsage Quantity Should not null", usageOrder.getFreeUsageQuantity());
    	assertNotNull("Available Quantity Should not be null", customerAvailableUsagePoolQuantity);

    	assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("4.19"), 
    			customerAvailableUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("119.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "119.2000000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("0.00"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("0.00"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));



    	OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, downGradePlanwith225Min.getItemId(), downGradePlanwith100Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
    	assertNotNull("Swap changes should be calculated", orderChanges);
    	api.createUpdateOrder(order, orderChanges);

    	order = api.getOrder(orderId);
    	assertNotNull("### Order after swap plan should not be null", order);

    	Integer swapedPlanId = 0;
    	for (OrderLineWS lineWS:order.getOrderLines()) {
    		if(lineWS.getItemId().equals(downGradePlanwith100Min.getItemId())) {
    			swapedPlanId = lineWS.getItemId(); 
    		}
    	}
    	assertEquals("##Subscription Order's swaped Plan ID must be", downGradePlanwith100Min.getItemId(),swapedPlanId);

    	customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
    	actualProratedQuantity = FullCreativeUtil.getTotalFreeUsageQuantity(customerUsagePools).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);
    	logger.debug("ActualProratedQuantity::::::::::::::::{}", actualProratedQuantity);
    	expectedProratedQuatity = new BigDecimal("101.4516").setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP);

    	assertEquals("Usage pool Prorated quantity should be ", expectedProratedQuatity, actualProratedQuantity);

    	assertEquals("Expected Cycle start date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleStartDateByPlanId(customerUsagePools, downGradePlanwith100Min.getId())));

    	assertEquals("Expected Cycle end date of customer usage pool: ", TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
    			,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getCustomerUsagePoolCycleEndDateByPlanId(customerUsagePools, downGradePlanwith100Min.getId())));

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

    	assertEquals("Mediated Quantity Should be : ",new BigDecimal("119.20"), 
    			FullCreativeUtil.getOrderTotalQuantity(usageOrder));

    	assertEquals("Usage Order Used free quantity Should not null", "101.4516000000", 
    			FullCreativeUtil.getTotalFreeUsageQuantityByOrder(usageOrder).toString());

    	assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("17.75"), 
    			overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

    	assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("16.86"), 
    			usageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
    } 
}
