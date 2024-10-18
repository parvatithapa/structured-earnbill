package com.sapienter.jbilling.server.fupProrating;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
/**
 * Here is link of FUP proration scenarios sheet.
 * https://docs.google.com/spreadsheets/d/1Ky14XZ7gfgwdDPgJcitGogCyysR_8S1mOHb2dwkQTRA/edit#gid=0
 * @author Ashok Kale
 *
 */
@Test(groups = { "fupProrating" }, testName = "FUPProratingActiveSinceChangeTest")
public class FUPProratingActiveSinceChangeTest {

	private static final Logger logger = LoggerFactory.getLogger(FUPProratingActiveSinceChangeTest.class);
	public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private Integer afIntro100MinPlanId;
	JbillingAPI api;
	private	Integer orderId = null;
	private Integer userId = null;
	private Integer onetimeOrderId = null;
	private BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	private BigDecimal overageQuantity = BigDecimal.ZERO;
	private OrderWS[] childOrders = null;

	@org.testng.annotations.BeforeClass
	protected void setUp() throws Exception {

		api = JbillingAPIFactory.getAPI();
		afIntro100MinPlanId = TestConstants.AF_INTRO_100_MIN_PLAN_ID;
	}

	/**
	 * FUP Proration A1 Scenario
	 */
	@Test
	public void test001FupProratingActiveSinceChangeScenarioA1() {

		try {
			logger.debug("### FUP Proration A1 Scenario - Active Since date change of Monthly order - Overage before but no overage after the date change ###");

			logger.debug("FUP Proration Scenarios Sheet : https://docs.google.com/spreadsheets/d/1Ky14XZ7gfgwdDPgJcitGogCyysR_8S1mOHb2dwkQTRA/edit#gid=0");

			//Customer A1 with Next Invoice Date 01-01-2016
			UserWS user  = FullCreativeUtil.createUser("Customer A1");
			userId = user.getId();
			assertNotNull("user should not be null",user);
			user = api.getUserWS(userId);

			user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
			api.updateUser(user);

			user = api.getUserWS(userId);
			logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

			PlanWS planWS = api.getPlanWS(afIntro100MinPlanId); // AF INTRO 100 MIN Plan

			assertNotNull("planWS should not be null", planWS);

			logger.debug("Creating Plan Subscription order...");

			//Create subscription order with Active since date 12-15-2015

			orderId = FullCreativeUtil.createOrder(planWS,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
			OrderWS order  =  api.getOrder(orderId);
			logger.debug("Subscription order created with Id ::: {} And Active Since Date ::: {}", orderId,
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			assertNotNull("orderId should not be null",orderId);

			user = api.getUserWS(userId);

			CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

			logger.debug("Customer Usage Pool Prorated Quantity ::: {}", customerUsagePoolWS.getInitialQuantityAsDecimal());
			
			assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("54.8387"), customerUsagePoolWS.
					getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			assertEquals("Expected Cycle start date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

			// One Time Usage Order 
			onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"23.6","16.4","17.2");
			OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();

			assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO, 
					customerUsagePoolQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("57.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("54.8387000000"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("2.36"), 
					overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("2.24"), 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

			// Change SubScription order Active since date to 12-11-2015
			order  =  api.getOrder(orderId);
			order.setActiveSince(FullCreativeUtil.getDate(11, 11, 2015));
			childOrders =  order.getChildOrders();
			for(OrderWS childOrder : childOrders) {
				childOrder.setActiveSince(FullCreativeUtil.getDate(11, 11, 2015));
			}
			api.updateOrder(order, null);

			order = api.getOrder(order.getId());
			assertNotNull("Order Updation Failed ",order);
			logger.debug("Changed Subscription order Active Since Date to ::: {}",
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

			logger.debug("After Active Since date change Customer Usage Pool Prorated Quantity :::{}",
					customerUsagePoolWS.getInitialQuantityAsDecimal());

			assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("67.74"), customerUsagePoolWS.
					getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			assertEquals("Expected Cycle start date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 11, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

			oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();

			assertNotNull("After Active since date change Free Usage Quantity Should not null", 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("57.2000000000"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("57.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
					overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("10.54"), 
					api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0]
							.getQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			logger.debug("#### Scenario A1 DONE ####");

		} catch(Exception ex) {
			ex.printStackTrace();
		} 
	}
	
	
	/**
	 * FUP Proration A2 Scenario
	 */
	@Test
	public void test002FupProratingActiveSinceChangeScenarioA2() {

		try {
			logger.debug("### FUP Proration A2 Scenario - Active Since date change of Monthly order - No overage before but overage after the date change ###");

			//Customer A1 with Next Invoice Date 01-01-2016
			UserWS user  = FullCreativeUtil.createUser("Customer A2");
			userId = user.getId();
			assertNotNull("user should not be null",user);
			user = api.getUserWS(userId);

			user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
			api.updateUser(user);

			user = api.getUserWS(userId);
			logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

			PlanWS planWS = api.getPlanWS(afIntro100MinPlanId); // AF INTRO 100 MIN Plan

			assertNotNull("planWS should not be null", planWS);

			logger.debug("Creating Plan Subscription order...");

			//Create subscription order for AF INTRO 100 MIN Plan (100 min free usage Pool) with Active since date 12-11-2015 

			orderId = FullCreativeUtil.createOrder(planWS,user.getUserId(),FullCreativeUtil.getDate(11, 11, 2015),null);
			OrderWS order  =  api.getOrder(orderId);
			logger.debug("Subscription order created with Id ::: {} And Active Since Date ::: {}", orderId,
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			assertNotNull("orderId should not be null",orderId);

			user = api.getUserWS(userId);

			CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
			logger.debug("Customer Usage Pool Prorated Quantity ::: {}", customerUsagePoolWS.getInitialQuantityAsDecimal());

			assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("67.7419"), customerUsagePoolWS.
					getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			assertEquals("Expected Cycle start date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 11, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

			// One Time Usage Order 
			onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 11, 2015) ,"23.6","16.4","17.2");
			OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();

			assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("10.5419"), 
					customerUsagePoolQuantity.setScale(4, BigDecimal.ROUND_HALF_UP));

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("57.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("57.2000000000"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
					overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("10.54"), 
					api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0]
							.getQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			// Change SubScription order Active since date to 12-15-2015
			order  =  api.getOrder(orderId);
			order.setActiveSince(FullCreativeUtil.getDate(11, 15, 2015));
			childOrders =  order.getChildOrders();
			for(OrderWS childOrder : childOrders) {
				childOrder.setActiveSince(FullCreativeUtil.getDate(11, 15, 2015));
			}
			api.updateOrder(order, null);

			order = api.getOrder(order.getId());
			assertNotNull("Order Updation Failed ",order);
			logger.debug("Changed Subscription order Active Since Date to :::{}",
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

			oneTimeUsageOrder = api.getOrder(onetimeOrderId);
			oneTimeUsageOrder.setActiveSince(FullCreativeUtil.getDate(11, 15, 2015));
			//Removed 0.0001 quantity from usage order to call Quantity change event.
			OrderChangeWS change1 = OrderChangeBL.buildFromLine(oneTimeUsageOrder.getOrderLines()[0], null, ORDER_CHANGE_STATUS_APPLY_ID);
			OrderChangeWS change2 = OrderChangeBL.buildFromLine(oneTimeUsageOrder.getOrderLines()[1], null, ORDER_CHANGE_STATUS_APPLY_ID);
			OrderChangeWS change3 = OrderChangeBL.buildFromLine(oneTimeUsageOrder.getOrderLines()[2], null, ORDER_CHANGE_STATUS_APPLY_ID);
			change1.setQuantity(change1.getQuantityAsDecimal().subtract(new BigDecimal("0.0001")));
			change2.setQuantity(change1.getQuantityAsDecimal().subtract(new BigDecimal("0.0001")));
			change3.setQuantity(change1.getQuantityAsDecimal().subtract(new BigDecimal("0.0001")));

			api.updateOrder(oneTimeUsageOrder, new OrderChangeWS[]{change1, change2, change3});

			oneTimeUsageOrder = api.getOrder(onetimeOrderId);
			assertNotNull("One Time Usage Order Updation Failed ",oneTimeUsageOrder);
			logger.debug("Changed One Time order Active Since Date to :::{}",
					TestConstants.DATE_FORMAT.format(oneTimeUsageOrder.getActiveSince()));

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();

			logger.debug("After Active Since date change Customer Usage Pool Prorated Quantity :::{}",
					customerUsagePoolWS.getInitialQuantityAsDecimal());

			assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("54.8387"), customerUsagePoolWS.
					getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			assertEquals("Expected Cycle start date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

			assertNotNull("After Active since date change Free Usage Quantity Should not null", 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("54.8387000000"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("57.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("2.36"), 
					overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("2.24"), 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO, 
					api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0]
							.getQuantityAsDecimal().setScale(0,BigDecimal.ROUND_HALF_UP));

			logger.debug("#### Scenario A2 DONE ####");

		} catch(Exception ex) {
			ex.printStackTrace();
		} 
	}
	
	/**
	 * FUP Proration A3 Scenario
	 */
	@Test
	public void test003FupProratingActiveSinceChangeScenarioA3() {

		try {
			logger.debug("#### FUP Proration A3 Scenario - Active Since date change of Monthly order - No overage before and after the date change ###");

			//Customer A1 with Next Invoice Date 01-01-2016
			UserWS user  = FullCreativeUtil.createUser("Customer A3");
			userId = user.getId();
			assertNotNull("user should not be null",user);
			user = api.getUserWS(userId);

			user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
			api.updateUser(user);

			user = api.getUserWS(userId);
			logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

			PlanWS planWS = api.getPlanWS(afIntro100MinPlanId); // AF INTRO 100 MIN Plan

			assertNotNull("planWS should not be null", planWS);

			logger.debug("##Creating Plan Subscription order...");

			//Create subscription order for AF INTRO 100 MIN Plan (100 min free usage Pool) with Active since date 12-11-2015 

			orderId = FullCreativeUtil.createOrder(planWS,user.getUserId(),FullCreativeUtil.getDate(11, 11, 2015),null);
			OrderWS order  =  api.getOrder(orderId);
			logger.debug("Subscription order created with Id ::: {} And Active Since Date ::: {}", orderId,
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			assertNotNull("orderId should not be null",orderId);

			user = api.getUserWS(userId);

			CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];
			logger.debug("Customer Usage Pool Prorated Quantity :::{}", customerUsagePoolWS.getInitialQuantityAsDecimal());

			assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("67.7419"), customerUsagePoolWS.
					getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			assertEquals("Expected Cycle start date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 11, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

			// One Time Usage Order 
			onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 11, 2015) ,"19.6","16.4","17.2");
			OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();

			assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("14.5419"), 
					customerUsagePoolQuantity.setScale(4, BigDecimal.ROUND_HALF_UP));

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("53.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("53.20"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder)
					.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
					overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("14.54"), 
					api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0]
							.getQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			// Change SubScription order Active since date to 12-15-2015
			order  =  api.getOrder(orderId);
			order.setActiveSince(FullCreativeUtil.getDate(11, 15, 2015));
			childOrders =  order.getChildOrders();
			for(OrderWS childOrder : childOrders) {
				childOrder.setActiveSince(FullCreativeUtil.getDate(11, 15, 2015));
			}
			api.updateOrder(order, null);

			order = api.getOrder(order.getId());
			assertNotNull("Order Updation Failed ",order);
			logger.debug("Changed Subscription order Active Since Date to :::{}",
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

			oneTimeUsageOrder = api.getOrder(onetimeOrderId);
			oneTimeUsageOrder.setActiveSince(FullCreativeUtil.getDate(11, 15, 2015));
			//Removed 0.0001 quantity from usage order to call Quantity change event.
			OrderChangeWS change1 = OrderChangeBL.buildFromLine(oneTimeUsageOrder.getOrderLines()[0], null, ORDER_CHANGE_STATUS_APPLY_ID);
			OrderChangeWS change2 = OrderChangeBL.buildFromLine(oneTimeUsageOrder.getOrderLines()[1], null, ORDER_CHANGE_STATUS_APPLY_ID);
			OrderChangeWS change3 = OrderChangeBL.buildFromLine(oneTimeUsageOrder.getOrderLines()[2], null, ORDER_CHANGE_STATUS_APPLY_ID);
			change1.setQuantity(change1.getQuantityAsDecimal().subtract(new BigDecimal("0.0001")));
			change2.setQuantity(change1.getQuantityAsDecimal().subtract(new BigDecimal("0.0001")));
			change3.setQuantity(change1.getQuantityAsDecimal().subtract(new BigDecimal("0.0001")));

			api.updateOrder(oneTimeUsageOrder, new OrderChangeWS[]{change1, change2, change3});

			oneTimeUsageOrder = api.getOrder(onetimeOrderId);
			assertNotNull("One Time Usage Order Updation Failed ",oneTimeUsageOrder);
			logger.debug("Changed One Time order Active Since Date to :::{}",
					TestConstants.DATE_FORMAT.format(oneTimeUsageOrder.getActiveSince()));

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();

			logger.debug("After Active Since date change Customer Usage Pool Prorated Quantity :::{}",
					customerUsagePoolWS.getInitialQuantityAsDecimal());

			assertEquals("Usage pool Prorated quantity should be ",new BigDecimal("54.8387"), customerUsagePoolWS.
					getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			assertEquals("Expected Cycle start date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

			assertNotNull("After Active since date change Free Usage Quantity Should not null", 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should not null", new BigDecimal("53.20"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder)
					.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("53.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
					overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("1.6392"), 
					api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0]
							.getQuantityAsDecimal().setScale(4,BigDecimal.ROUND_HALF_UP));

			logger.debug("#### Scenario A3 DONE ####");

		} catch(Exception ex) {
			ex.printStackTrace();
		} 
	}
}
