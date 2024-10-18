package com.sapienter.jbilling.server.fupProrating;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
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
@Test(groups = { "fupProrating" }, testName = "FUPProratingMultiplePlansActiveUntilChangeTest")
public class FUPProratingMultiplePlansActiveUntilChangeTest {

	private static final Logger logger = LoggerFactory.getLogger(FUPProratingMultiplePlansActiveUntilChangeTest.class);
	private Integer afIntro100MinPlanId;
	JbillingAPI api;
	private	Integer orderId = null;
	private Integer userId = null;
	private Integer onetimeOrderId = null;
	private BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	private BigDecimal overageQuantity = BigDecimal.ZERO;

	@org.testng.annotations.BeforeClass
	protected void setUp() throws Exception {

		api = JbillingAPIFactory.getAPI();
		afIntro100MinPlanId = TestConstants.AF_INTRO_100_MIN_PLAN_ID;
	}
	

	/**
	 * FUP Proration C4 Scenario
	 */
	@Test
	public void test004FupProratingActiveUntilDateChangeScenarioB4() {

		try {
			logger.debug("### FUP Proration C4 Scenario - Active Until date change of One of the Monthly order - TWO subscription orders for customer - each containing separate plan - each plan has single usage pool - No overage before but  overage after the date change ###");

			//Customer C4 with Next Invoice Date 01-01-2016
			UserWS user  = FullCreativeUtil.createUser("Customer C4");
			userId = user.getId();
			assertNotNull("user should not be null",user);
			user = api.getUserWS(userId);

			user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
			api.updateUser(user);

			user = api.getUserWS(userId);
			logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

			PlanWS planWS = api.getPlanWS(afIntro100MinPlanId); // AF INTRO 100 MIN Plan
			//Creating 135 min usage pool
			UsagePoolWS usagePool = FullCreativeUtil.populateFreeUsagePoolObject("135.00");
			Integer usagePoolId = api.createUsagePool(usagePool);
			logger.debug("planWS.getUsagePoolIds()[0] :::::::: {}", planWS.getUsagePoolIds()[0]);
			planWS.setUsagePoolIds(new Integer[] {102 ,usagePoolId});
			api.updatePlan(planWS);

			planWS = api.getPlanWS(afIntro100MinPlanId);
			assertNotNull("planWS should not be null", planWS);

			logger.debug("Plan containing FUP ::: {}, {}", planWS.getUsagePoolIds()[0], planWS.getUsagePoolIds()[1]);

			logger.debug("Creating Plan Subscription order...");

			//Create subscription order for AF INTRO 100 MIN Plan (100 min & 135 min free usage Pool) with Active since date 12-11-2015 

			orderId = FullCreativeUtil.createOrder(planWS,user.getUserId(),FullCreativeUtil.getDate(11, 11, 2015),null);
			OrderWS order  =  api.getOrder(orderId);
			logger.debug("Subscription order created with Id ::: {} and Active Since Date ::: {}", orderId,
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			assertNotNull("orderId should not be null",orderId);

			user = api.getUserWS(userId);


			CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			logger.debug("Customer Usage Pool Prorated Quantity ::: {}",
					customerUsagePools[0].getInitialQuantityAsDecimal());
			
			BigDecimal sumOfUsagePoolsQuantities = customerUsagePools[0].getQuantityAsDecimal()
					.add(customerUsagePools[1].getQuantityAsDecimal());

			logger.debug("Customer Usage Pool Prorated Quantity of CUP1 ::: {}", customerUsagePools[0].getInitialQuantityAsDecimal());

			logger.debug("Customer Usage Pool Prorated Quantity of CUP2 ::: {}", customerUsagePools[1].getInitialQuantityAsDecimal());

			logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum ::: {}", sumOfUsagePoolsQuantities);

			assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("159.1935"), 
					sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			for (CustomerUsagePoolWS customerUsagePool : customerUsagePools) {
				assertEquals("Expected Cycle start date of customer usage pool: " 
						,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 11, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleStartDate()));

				assertEquals("Expected Cycle end date of customer usage pool: "
						, TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleEndDate()));
			}

			// One Time Usage Order 
			onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"79.6","46.4","23.2");
			OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

			assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("149.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should be ", new BigDecimal("149.2000000000"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
					overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("9.99"), 
					customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			// Set SubScription order Active Until date to 12-25-2015
			order  =  api.getOrder(orderId);
			order.setActiveUntil(FullCreativeUtil.getDate(11, 25, 2015));
			api.updateOrder(order, null);

			order = api.getOrder(order.getId());
			assertNotNull("Order Updation Failed ",order);
			logger.debug("Set Subscription order Active Until Date to ::: {}", TestConstants.DATE_FORMAT.format(order.getActiveUntil()));

			customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			sumOfUsagePoolsQuantities = customerUsagePools[0].getInitialQuantityAsDecimal()
					.add(customerUsagePools[1].getInitialQuantityAsDecimal());

			oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

			logger.debug("After Active until date set Customer Usage Pools Prorated Quantity First ::: {} - Second ::: {}",
					customerUsagePools[0].getInitialQuantityAsDecimal(), customerUsagePools[1].getInitialQuantityAsDecimal());

			logger.debug("After Active Until date set Customer Usage Pool Prorated Quantity sum is ::: {}",
					sumOfUsagePoolsQuantities);

			assertEquals("After Active until date set Usage pools Prorated quantity sum should be ",new BigDecimal("113.7097"), 
					sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			for (CustomerUsagePoolWS customerUsagePool : customerUsagePools) {
				assertEquals("Expected Cycle start date of customer usage pool: "
						, TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 11, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleStartDate()));

				assertEquals("Expected Cycle end date of customer usage pool: "
						, TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 25, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleEndDate()));
			}

			assertNotNull("After Active Until date set Free Usage Quantity Should not null",
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should be :::", new BigDecimal("113.7097000000"),
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("Mediated Quantity Should be :::",new BigDecimal("149.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("35.49"), 
					overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("33.72"), 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO, 
					customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));

			logger.debug("#### Scenario B4 DONE! ####");
		} catch(Exception ex) {
			ex.printStackTrace();
		} 
	}

	/**
	 * FUP Proration B5 Scenario
	 */
	@Test
	public void test005FupProratingActiveUntilDateChangeScenarioB5() {

		try {
			logger.debug("### FUP Proration B5 Scenario - 	Active Until date change of Monthly order - Plan containing two usage pools - No overage before and after the date change ###");

			//Customer B5 with Next Invoice Date 01-01-2016
			UserWS user  = FullCreativeUtil.createUser("Customer B5");
			userId = user.getId();
			assertNotNull("user should not be null",user);
			user = api.getUserWS(userId);

			user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
			api.updateUser(user);

			user = api.getUserWS(userId);
			logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

			PlanWS planWS = api.getPlanWS(afIntro100MinPlanId); // AF INTRO 100 MIN Plan

			assertNotNull("planWS should not be null", planWS);

			logger.debug("Plan containing FUP ::: {}, {}", planWS.getUsagePoolIds()[0], planWS.getUsagePoolIds()[1]);

			logger.debug("Creating Plan Subscription order...");

			//Create subscription order for AF INTRO 100 MIN Plan (100 min & 135 min free usage Pool) with Active since date 12-11-2015 

			orderId = FullCreativeUtil.createOrder(planWS,user.getUserId(),FullCreativeUtil.getDate(11, 11, 2015),null);
			OrderWS order  =  api.getOrder(orderId);
			logger.debug("Subscription order created with Id ::: {} and Active Since Date ::: {}", orderId,
					TestConstants.DATE_FORMAT.format(order.getActiveSince()));

			assertNotNull("orderId should not be null",orderId);

			user = api.getUserWS(userId);

			CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			BigDecimal sumOfUsagePoolsQuantities = customerUsagePools[0].getQuantityAsDecimal()
					.add(customerUsagePools[1].getQuantityAsDecimal());

			logger.debug("Customer Usage Pool Prorated Quantity of CUP1 ::: {}", customerUsagePools[0].getInitialQuantityAsDecimal());

			logger.debug("Customer Usage Pool Prorated Quantity of CUP2 ::: {}", customerUsagePools[1].getInitialQuantityAsDecimal());

			logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum ::: {}", sumOfUsagePoolsQuantities);

			assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("159.1935"), 
					sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			for (CustomerUsagePoolWS customerUsagePool : customerUsagePools) {
				assertEquals("Expected Cycle start date of customer usage pool: " 
						,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 11, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleStartDate()));

				assertEquals("Expected Cycle end date of customer usage pool: "
						, TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleEndDate()));
			}

			// One Time Usage Order 
			onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"56.6","46.4","23.2");
			OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

			assertNotNull("FreeUsage Quantity Should not null", FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertNotNull("Available Quantity Should not be null", customerUsagePoolQuantity);

			assertEquals("Mediated Quantity Should be : ",new BigDecimal("126.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should be ", new BigDecimal("126.2000000000"), 
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
					overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",new BigDecimal("32.99"), 
					customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

			// Set SubScription order Active Until date to 12-25-2015
			order  =  api.getOrder(orderId);
			order.setActiveUntil(FullCreativeUtil.getDate(11, 25, 2015));
			api.updateOrder(order, null);

			order = api.getOrder(order.getId());
			assertNotNull("Order Updation Failed ",order);
			logger.debug("Set Subscription order Active Until Date to ::: {}", TestConstants.DATE_FORMAT.format(order.getActiveUntil()));

			customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
			sumOfUsagePoolsQuantities = customerUsagePools[0].getInitialQuantityAsDecimal()
					.add(customerUsagePools[1].getInitialQuantityAsDecimal());

			oneTimeUsageOrder = api.getOrder(onetimeOrderId);

			overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
					.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
			customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

			logger.debug("After Active until date set Customer Usage Pools Prorated Quantity First ::: {} - Second ::: {}",
					customerUsagePools[0].getInitialQuantityAsDecimal(), customerUsagePools[1].getInitialQuantityAsDecimal());

			logger.debug("After Active Until date set Customer Usage Pool Prorated Quantity sum is ::: {}",
					sumOfUsagePoolsQuantities);

			assertEquals("After Active until date set Usage pools Prorated quantity sum should be ",new BigDecimal("113.7097"), 
					sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

			for (CustomerUsagePoolWS customerUsagePool : customerUsagePools) {
				assertEquals("Expected Cycle start date of customer usage pool: "
						, TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 11, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleStartDate()));

				assertEquals("Expected Cycle end date of customer usage pool: "
						, TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 25, 2015))
						,TestConstants.DATE_FORMAT.format(customerUsagePool.getCycleEndDate()));
			}

			assertNotNull("After Active Until date set Free Usage Quantity Should not null",
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("Usage Order Used free quantity Should be :::", new BigDecimal("113.7097000000"),
					FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

			assertEquals("Mediated Quantity Should be :::",new BigDecimal("126.20"), 
					FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

			assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("12.49"), 
					overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

			assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("11.87"), 
					oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

			assertEquals("Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO, 
					customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));

			planWS = api.getPlanWS(afIntro100MinPlanId);
			planWS.setUsagePoolIds(new Integer[] {102});
			api.updatePlan(planWS);
			logger.debug("#### Scenario B5 DONE! ####");
		} catch(Exception ex) {
			ex.printStackTrace();
		} 
	}

}
