package com.sapienter.jbilling.server.swapPlan;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import com.sapienter.jbilling.fc.FullCreativeUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.TestConstants;

/**
 * Here is link of Swap Plan proration scenarios sheet.
 * https://docs.google.com/spreadsheets/d/178y1MPMRAcG-d_S6XismdLccG62lKq3B33xJb5p_-Xo/edit#gid=2059461249
 * 
 * Scenario B1, B2, B3
 *
 * @author Pranay G. Raherkar
 *
 */

@Test(groups = { "swapPlan" }, testName = "SwapPlanUpgradeFUPTransferwithMultiplePoolsTest")
public class SwapPlanUpgradeFUPTransferwithMultiplePoolsTest{

	private static final Logger logger = LoggerFactory.getLogger(SwapPlanUpgradeFUPTransferwithMultiplePoolsTest.class);
	private JbillingAPI api;
	private Integer userId;
	private	Integer orderId = null;
	private Integer onetimeOrderId = null;
	private BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	private BigDecimal overageQuantity = BigDecimal.ZERO;
	UserWS user = null;
	PlanWS upGradePlanwith100Min;
	PlanWS upGradePlanwith225Min;

	@BeforeClass
	protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();

		UsagePoolWS usagePoolWith100Quantity = FullCreativeUtil.populateFreeUsagePoolObject("100");
		UsagePoolWS usagePoolWith135Quantity = FullCreativeUtil.populateFreeUsagePoolObject("135");
		usagePoolWith100Quantity.setId(api.createUsagePool(usagePoolWith100Quantity));
		usagePoolWith135Quantity.setId(api.createUsagePool(usagePoolWith135Quantity));

		UsagePoolWS usagePoolWith225Quantity = FullCreativeUtil.populateFreeUsagePoolObject("225");
		UsagePoolWS usagePoolWith85Quantity = FullCreativeUtil.populateFreeUsagePoolObject("85");

		usagePoolWith225Quantity.setId(api.createUsagePool(usagePoolWith225Quantity));
		usagePoolWith85Quantity.setId(api.createUsagePool(usagePoolWith85Quantity));

		assertNotNull("Usage Pool Creation Failed ", usagePoolWith100Quantity);
		assertNotNull("Usage Pool Creation Failed ", usagePoolWith135Quantity);
		assertNotNull("Usage Pool Creation Failed ", usagePoolWith85Quantity);
		assertNotNull("Usage Pool Creation Failed ", usagePoolWith225Quantity);

		upGradePlanwith225Min = FullCreativeUtil.createPlan("225", "0.95",
				new Integer[]{usagePoolWith225Quantity.getId(),usagePoolWith85Quantity.getId()},
				"Test Plan 225 Min" ,api,
				TestConstants.CHAT_USAGE_PRODUCT_ID,
				TestConstants.INBOUND_USAGE_PRODUCT_ID,
				TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
		
		assertNotNull("planWS should not be null", upGradePlanwith225Min);

		upGradePlanwith100Min = FullCreativeUtil.createPlan("100", "0.95",
				new Integer[]{usagePoolWith100Quantity.getId(),usagePoolWith135Quantity.getId()},
				"Test Plan 100 Min",api,
				TestConstants.CHAT_USAGE_PRODUCT_ID,
				TestConstants.INBOUND_USAGE_PRODUCT_ID,
				TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
		
		assertNotNull("planWS should not be null", upGradePlanwith100Min);
	}

	/**
	 # Plan Swap - Upgrade of a plan
	 # Old plan containing multiple usage pools to new plan also containing multiple usage pools
	 # A plan subscription is Upgrade from (pool 100+ pool 135) mins free to (Pool 225+ Pool 85) mins free
	 # NO Overage before and after the plan swap.
	 */
	@Test
	public void test001SwapPlanUpgradeFUPTransferwithMultiplePoolsTestB1 () throws Exception{
		//Customer 'Customer B1' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Customer B1 - ");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);

		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);

		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

		//Create subscription order with Active since date 12-15-2015

		orderId = FullCreativeUtil.createOrder(upGradePlanwith100Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS order  =  api.getOrder(orderId);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
				TestConstants.DATE_FORMAT.format(order.getActiveSince()));

		assertNotNull("orderId should not be null",orderId);

		user = api.getUserWS(userId);

		CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		BigDecimal sumOfUsagePoolsQuantities = customerUsagePools[0].getQuantityAsDecimal()
				.add(customerUsagePools[1].getQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP2:::{}", customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum :::{}", sumOfUsagePoolsQuantities);

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("128.8710"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));


		for (CustomerUsagePoolWS customerUsagePoolWS : api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())) {

			assertEquals("Expected Cycle start date of customer usage pool  :::: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		}

		order = api.getOrder(orderId);
		assertNotNull("getOrder should not be null", order);

		// Create One Time Usage Order
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"73.6","26.4","22.2");
		OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity before swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Available Quantity before swap plan Should be :::: {}", customerUsagePoolQuantity);

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("Mediated Quantity Should be : ",new BigDecimal("122.20"),
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO,
				overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO,
				oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("6.67"),
				customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId()
				,upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
		assertNotNull("Swap changes should be calculated", orderChanges);

		api.createUpdateOrder(order, orderChanges);

		order = api.getOrder(orderId);
		assertNotNull("### Order after swap plan should not be null", order);

		assertEquals("## Subscription Order Amount must be 225.00", "225.0000000000",order.getTotal());

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		sumOfUsagePoolsQuantities = customerUsagePools[0].getInitialQuantityAsDecimal()
				.add(customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP2:::{}", customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum :::{}", sumOfUsagePoolsQuantities);

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("170.0000"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

		for (CustomerUsagePoolWS customerUsagePoolWS : api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())) {

			assertEquals(" After swap plan Expected Cycle start date of customer usage pool  :::: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals(" After swap plan Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		}

		oneTimeUsageOrder = api.getOrder(onetimeOrderId);
		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity After swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Remaining Avialable Quantity sum of Customer usage pools After Swap Plan  ::: {}", customerUsagePoolQuantity);

		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("122.2000000000"),
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("122.20"), 
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
				overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
				oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("47.80"),
				customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
	}

	/**
	 # Plan Swap - Upgrade of a plan
	 # Old plan containing multiple usage pools to new plan also containing multiple usage pools
	 # A plan subscription is Upgrade from (pool 100+ pool 135) mins free to (Pool 225+ Pool 85) mins free
	 # Overage before but no overage after the plan swap.
	 */

	@Test
	public void test002SwapPlanUpgradeFUPTransferwithMultiplePoolsTestB2 () throws Exception{
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

		orderId = FullCreativeUtil.createOrder(upGradePlanwith100Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS order  =  api.getOrder(orderId);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
				TestConstants.DATE_FORMAT.format(order.getActiveSince()));

		assertNotNull("orderId should not be null",orderId);

		user = api.getUserWS(userId);

		CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		BigDecimal sumOfUsagePoolsQuantities = customerUsagePools[0].getQuantityAsDecimal()
				.add(customerUsagePools[1].getQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP2:::{}", customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum :::{}", sumOfUsagePoolsQuantities);

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("128.8710"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));


		for (CustomerUsagePoolWS customerUsagePoolWS : api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())) {

			assertEquals("Expected Cycle start date of customer usage pool  :::: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		}

		order = api.getOrder(orderId);
		assertNotNull("getOrder should not be null", order);

		// Create One Time Usage Order 
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"73.6","46.4","22.2");
		OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity before swap plan Should not be null", customerUsagePoolQuantity);        

		logger.debug(" Available Quantity before swap plan Should be :::: {}", customerUsagePoolQuantity);

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("Mediated Quantity Should be : ",new BigDecimal("142.20"),
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("13.33"),
				overageQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

		assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("12.66"),
				oneTimeUsageOrder.getTotalAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO,
				customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));

		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,upGradePlanwith100Min.getItemId()
				,upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
		assertNotNull("Swap changes should be calculated", orderChanges);

		api.createUpdateOrder(order, orderChanges);

		order = api.getOrder(orderId);
		assertNotNull("### Order after swap plan should not be null", order);

		assertEquals("## Subscription Order Amount must be 225.00", "225.0000000000",order.getTotal());

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		sumOfUsagePoolsQuantities = customerUsagePools[0].getInitialQuantityAsDecimal()
				.add(customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1::: {}", customerUsagePools[0].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP2:::{} ", customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum :::{}", sumOfUsagePoolsQuantities);

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("170.0000"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));


		for (CustomerUsagePoolWS customerUsagePoolWS : api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())) {

			assertEquals(" After swap plan Expected Cycle start date of customer usage pool  :::: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals(" After swap plan Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		}

		oneTimeUsageOrder = api.getOrder(onetimeOrderId);
		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity After swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Remaining Avialable Quantity sum of Customer usage pools After Swap Plan  ::: {}", customerUsagePoolQuantity);

		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("142.2000000000"),
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("142.20"), 
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
				overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
				oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("27.80"),
				customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
	}

	/**
	 # Plan Swap - Upgrade of a plan
	 # Old plan containing multiple usage pools to new plan also containing multiple usage pools
	 # A plan subscription is Upgrade from (pool 100+ pool 135) mins free to (Pool 225+ Pool 85) mins free
	 # Overage before and after the plan swap.
	 */
	@Test
	public void test003SwapPlanUpgradeFUPTransferwithMultiplePoolsTestB3 () throws Exception{
		//Customer 'Customer B3' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Customer B3 - ");
		userId = user.getId();
		assertNotNull("user should not be null",user);
		user = api.getUserWS(userId);

		user.setNextInvoiceDate(FullCreativeUtil.getDate(0, 1, 2016));
		api.updateUser(user);

		user = api.getUserWS(userId);
		logger.debug("user Next Invoice Date ::: {}", TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

		//Create subscription order with Active since date 12-15-2015

		orderId = FullCreativeUtil.createOrder(upGradePlanwith100Min,user.getUserId(),FullCreativeUtil.getDate(11, 15, 2015),null);
		OrderWS order  =  api.getOrder(orderId);
		logger.debug("Subscription order created with Id :::{} And Active Since Date :::{}", orderId,
				TestConstants.DATE_FORMAT.format(order.getActiveSince()));

		assertNotNull("orderId should not be null",orderId);

		user = api.getUserWS(userId);

		CustomerUsagePoolWS[] customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		BigDecimal sumOfUsagePoolsQuantities = customerUsagePools[0].getQuantityAsDecimal()
				.add(customerUsagePools[1].getQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP2:::{}", customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum :::{}", sumOfUsagePoolsQuantities);

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("128.8710"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));


		for (CustomerUsagePoolWS customerUsagePoolWS : api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())) {

			assertEquals("Expected Cycle start date of customer usage pool  :::: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals("Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		}

		order = api.getOrder(orderId);
		assertNotNull("getOrder should not be null", order);

		// Create One Time Usage Order 
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"73.6","56.4","45.2");
		OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity before swap plan Should not be null", customerUsagePoolQuantity);        

		logger.debug(" Available Quantity before swap plan Should be :::: {}", customerUsagePoolQuantity);

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("Mediated Quantity Should be : ",new BigDecimal("175.20"),
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("46.33"),
				overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

		assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("44.01"),
				oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO,
				customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));

		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId()
				,upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(new Date()));
		assertNotNull("Swap changes should be calculated", orderChanges);

		api.createUpdateOrder(order, orderChanges);

		order = api.getOrder(orderId);
		assertNotNull("### Order after swap plan should not be null", order);

		assertEquals("## Subscription Order Amount must be 225.00", "225.0000000000",order.getTotal());

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		sumOfUsagePoolsQuantities = customerUsagePools[0].getInitialQuantityAsDecimal()
				.add(customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP2:::{}", customerUsagePools[1].getInitialQuantityAsDecimal());

		logger.debug("Multiple Customer Usage Pools Prorated Quantity Sum :::{}", sumOfUsagePoolsQuantities);

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("170.0000"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));


		for (CustomerUsagePoolWS customerUsagePoolWS : api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())) {

			assertEquals(" After swap plan Expected Cycle start date of customer usage pool  :::: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 15, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

			assertEquals(" After swap plan Expected Cycle end date of customer usage pool: "
					,TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11, 31, 2015))
					,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));
		}

		oneTimeUsageOrder = api.getOrder(onetimeOrderId);
		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity After swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Remaining Avialable Quantity sum of Customer usage pools After Swap Plan  ::: {}", customerUsagePoolQuantity);

		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("170.0000000000"),
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("175.20"), 
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",new BigDecimal("5.20"), 
				overageQuantity.setScale(2, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",new BigDecimal("4.94"), 
				oneTimeUsageOrder.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO,
				customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));
	}
}
