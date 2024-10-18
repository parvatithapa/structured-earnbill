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
 * 
 * @author Pranay G. Raherkar
 *
 */

@Test(groups = { "swapPlan" }, testName = "SwapPlanUpgradeFUPTransferwithMultipleToSinglePoolsWithDiffBundlePriceTest")
public class SwapPlanUpgradeFUPTransferwithMultipleToSinglePoolsWithDiffBundlePriceTest{

	private static final Logger logger = LoggerFactory.getLogger(SwapPlanUpgradeFUPTransferwithMultipleToSinglePoolsWithDiffBundlePriceTest.class);
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
		UsagePoolWS usagePoolWith85Quantity = FullCreativeUtil.populateFreeUsagePoolObject("85");
		usagePoolWith100Quantity.setId(api.createUsagePool(usagePoolWith100Quantity));
		usagePoolWith85Quantity.setId(api.createUsagePool(usagePoolWith85Quantity));

		UsagePoolWS usagePoolWith225Quantity = FullCreativeUtil.populateFreeUsagePoolObject("225");
		usagePoolWith225Quantity.setId(api.createUsagePool(usagePoolWith225Quantity));

		assertNotNull("Usage Pool Creation Failed ", usagePoolWith100Quantity);
		assertNotNull("Usage Pool Creation Failed ", usagePoolWith85Quantity);
		assertNotNull("Usage Pool Creation Failed ", usagePoolWith225Quantity);

		upGradePlanwith225Min = FullCreativeUtil.createPlan("225", "0.95",
				new Integer[]{usagePoolWith225Quantity.getId()},
				"Test Plan 225 Min",api,
				TestConstants.CHAT_USAGE_PRODUCT_ID,
				TestConstants.INBOUND_USAGE_PRODUCT_ID,
				TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
		
		assertNotNull("planWS should not be null", upGradePlanwith225Min);

		upGradePlanwith100Min = FullCreativeUtil.createPlan("100", "1.39",
				new Integer[]{usagePoolWith100Quantity.getId(),usagePoolWith85Quantity.getId()},
				"Test Plan 100 Min",api,
				TestConstants.CHAT_USAGE_PRODUCT_ID,
				TestConstants.INBOUND_USAGE_PRODUCT_ID,
				TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
		
		assertNotNull("planWS should not be null", upGradePlanwith100Min);
	}

	/**
	 # Plan Swap - Upgrade of a plan
	 # Old plan containing multiple usage pools to new plan containing single usage pools
	 # A plan subscription is Upgrade from (pool 100+ pool 85) mins with Bundle Price $1.39 free to (Pool 225) mins Bundle Price $0.95 free
	 # No Overage before and after the plan swap.
	 */
	@Test
	public void test001SwapPlanUpgradeFUPTransferwithMultipleToSinglePoolsWithDiffBundlePriceTestI1 () throws Exception{
		//Customer 'Customer I1' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Customer I1 - ");
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

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("101.45"),
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));


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
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"45.6","30.4","22.2");
		OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder).subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity before swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Available Quantity before swap plan Should be :::: {}", customerUsagePoolQuantity);

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("Mediated Quantity Should be : ",new BigDecimal("98.20"),
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("One Time Order Overage Quantity Should be : ",BigDecimal.ZERO,
				overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("One Time Order Overage Charges Should be : ",BigDecimal.ZERO,
				oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
		
        assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("3.25"),
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));

		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId()
				,upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(order.getActiveSince()));
		
		assertNotNull("Swap changes should be calculated", orderChanges);
		
		api.createUpdateOrder(order, orderChanges);

		order = api.getOrder(orderId);

		assertNotNull("### Order after swap plan should not be null", order);
		
		assertEquals("## Subscription Order Amount must be 225.00", "225.0000000000",order.getTotal());

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("123.39"),
				customerUsagePools[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));


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
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal();

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());
		
		assertNotNull("Available Quantity After swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Remaining Avialable Quantity sum of Customer usage pools After Swap Plan  ::: {}", customerUsagePoolQuantity);

		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("98.2000000000"),
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("98.20"),
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",BigDecimal.ZERO,
				overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",BigDecimal.ZERO,
				oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));

        assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("25.19"),
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
	}

	/**
	 # Plan Swap - Upgrade of a plan
	 # Old plan containing multiple usage pools to new plan containing single usage pools
	 # A plan subscription is Upgrade from (pool 100+ pool 85) mins with Bundle Price $1.39 free to (Pool 225) mins Bundle Price $0.95 free
	 # Overage before the plan swap but No overage after the plan swap.
	 */
	@Test
	public void test002SwapPlanUpgradeFUPTransferwithMultipleToSinglePoolsWithDiffBundlePriceTestI2 () throws Exception{
		//Customer 'Customer I2' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Customer I2 - ");
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

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("101.45"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));


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
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"55.6","30.4","22.2");
		OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity before swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Available Quantity before swap plan Should be :::: {}", customerUsagePoolQuantity);

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("Mediated Quantity Should be : ",new BigDecimal("108.20"),
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("6.75"),
				overageQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

		assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("9.38"),
				oneTimeUsageOrder.getTotalAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
		
		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO,
	        		customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));

		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId()
				,upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(order.getActiveSince()));
		assertNotNull("Swap changes should be calculated", orderChanges);

		api.createUpdateOrder(order, orderChanges);

		order = api.getOrder(orderId);
		assertNotNull("### Order after swap plan should not be null", order);

		assertEquals("## Subscription Order Amount must be 225.00", "225.0000000000",order.getTotal());

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("123.39"),
				customerUsagePools[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));


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
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal();

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());

		assertNotNull("Available Quantity After swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Remaining Avialable Quantity sum of Customer usage pools After Swap Plan  ::: {}", customerUsagePoolQuantity);

		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("108.2000000000"),
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("108.20"), 
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",BigDecimal.ZERO, 
				overageQuantity.setScale(0, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",BigDecimal.ZERO, 
				oneTimeUsageOrder.getTotalAsDecimal().setScale(0, BigDecimal.ROUND_HALF_UP));
		
		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",new BigDecimal("15.19"),
        		customerUsagePoolQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));
	}


	/**
	 # Plan Swap - Upgrade of a plan 
	 # Old plan containing multiple usage pools to new plan containing single usage pools
	 # A plan subscription is Upgrade from (pool 100+ pool 85) mins with Bundle Price $1.39 free to (Pool 225) mins Bundle Price $0.95 free
	 # Overage before the plan swap and after the plan swap
	 */
	@Test
	public void test003SwapPlanUpgradeFUPTransferwithMultipleToSinglePoolsWithDiffBundlePriceTestI3 () throws Exception{
		//Customer 'Customer I3' with Next Invoice Date 01-01-2016
		UserWS user  = FullCreativeUtil.createUser("Customer I3 - ");
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

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("101.45"), 
				sumOfUsagePoolsQuantities.setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));


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
		onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, FullCreativeUtil.getDate(11, 15, 2015) ,"65.6","36.4","22.2");
		OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);

		overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
				.subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal().add(customerUsagePools[1].getQuantityAsDecimal());

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());
		
		assertNotNull("Available Quantity before swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Available Quantity before swap plan Should be :::: {}", customerUsagePoolQuantity);

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());
		
		assertEquals("Mediated Quantity Should be : ",new BigDecimal("124.20"),
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));
		
		assertEquals("One Time Order Overage Quantity Should be : ",new BigDecimal("22.75"), 
				overageQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
		
		assertEquals("One Time Order Overage Charges Should be : ",new BigDecimal("31.62"),
				oneTimeUsageOrder.getTotalAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));
		
		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO,
        		customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));

		OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, upGradePlanwith100Min.getItemId()
				,upGradePlanwith225Min.getItemId(), SwapMethod.DIFF, Util.truncateDate(order.getActiveSince()));
		
		assertNotNull("Swap changes should be calculated", orderChanges);
		api.createUpdateOrder(order, orderChanges);

		order = api.getOrder(orderId);
		assertNotNull("### Order after swap plan should not be null", order);
		assertEquals("## Subscription Order Amount must be 225.00", "225.0000000000",order.getTotal());

		customerUsagePools  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId());

		logger.debug("Customer Usage Pool Prorated Quantity of CUP1:::{}", customerUsagePools[0].getInitialQuantityAsDecimal());

		assertEquals("Usage pools Prorated quantity sum should be  ",new BigDecimal("123.39"), 
				customerUsagePools[0].getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR,BigDecimal.ROUND_HALF_UP));


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
		customerUsagePoolQuantity = customerUsagePools[0].getQuantityAsDecimal();

		assertNotNull("FreeUsage Quantity Should not null", oneTimeUsageOrder.getFreeUsageQuantity());
		
		assertNotNull("Available Quantity After swap plan Should not be null", customerUsagePoolQuantity);

		logger.debug(" Remaining Avialable Quantity sum of Customer usage pools After Swap Plan  ::: {}", customerUsagePoolQuantity);

		assertNotNull("After Swap Plan one time order Free Usage Quantity Should not null",
				oneTimeUsageOrder.getFreeUsageQuantity());

		assertEquals("After Swap Plan Usage Order Used free quantity Should be :::", new BigDecimal("123.3871000000"),
				FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));

		assertEquals("After Swap Plan Mediated Quantity Should be :::",new BigDecimal("124.20"), 
				FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder));

		assertEquals("After Swap Plan One Time Order Overage Quantity Should be : ",new BigDecimal("0.81"), 
				overageQuantity.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan One Time Order Overage Charges Should be : ",new BigDecimal("0.77"), 
				oneTimeUsageOrder.getTotalAsDecimal().setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP));

		assertEquals("After Swap Plan Available Quantity of customer usage pool Should be : ",BigDecimal.ZERO,
        		customerUsagePoolQuantity.setScale(0,BigDecimal.ROUND_HALF_UP));
	}
}
