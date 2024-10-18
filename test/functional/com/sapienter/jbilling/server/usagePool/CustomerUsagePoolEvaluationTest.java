/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.usagePool;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

/**
 * CustomerUsagePoolEvaluationTest
 * Test cases to trigger cron plugin CustomerUsagePoolEvaluationTask
 * and reset to initial value of expired customer usage pools
 * @author Mahesh Shivarkar
 * @since 23-Nov-2016
 */

@Test(groups = { "usagePools" }, testName = "CustomerUsagePoolEvaluationTest")
public class CustomerUsagePoolEvaluationTest {

    private static final Logger logger = LoggerFactory.getLogger(CustomerUsagePoolEvaluationTest.class);

	public CustomerUsagePoolEvaluationTest() {
		logger.debug("CustomerUsagePoolEvaluationTest");
	}

	private static final Integer PRANCING_PONY = 1;
    private static final Integer DISABLED = Integer.valueOf(0);
    private static final BigDecimal PLAN_BUNDLE_QUANTITY = new BigDecimal(1);
    private Integer userId;
    private Integer itemTypeId;
    private Integer itemId;
    private Integer ORDER_CHANGE_STATUS_APPLY_ID;
    private Integer ORDER_PERIOD_MONTHLY;
    long today = new Date().getTime();
    protected static final BigDecimal ORDER_LINE_PRICE = new BigDecimal(0.50);
    final int LINES = 1;
	private JbillingAPI api;
    private static Integer DYNAMIC_BALANCE_MANAGER_PLUGIN_ID;


    @BeforeClass
    public void initializeTests(){
		try {
			api = JbillingAPIFactory.getAPI();
		} catch (Exception e) {
			logger.error("Error while getting API");
		}

        // Create and persist Test User
        userId = createUser("UsagePoolsTestUser");

        // Create and persist Test Item Category
        itemTypeId = createItemType("PlanSubscriptionTestItemType");

        // Create and persist Test item
        itemId = createItem(itemTypeId);

        ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        BillingProcessConfigurationWS bpcw = api.getBillingProcessConfiguration();
        bpcw.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());
        api.createUpdateBillingProcessConfiguration(bpcw);
        DYNAMIC_BALANCE_MANAGER_PLUGIN_ID = getOrCreatePluginWithoutParams(
                "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask", 10009);
	}

    @AfterClass
    public void tearDown(){

        if(null != itemId){
            api.deleteItem(itemId);
        }

        if(null != itemTypeId){
            ItemDTOEx[] items = api.getItemByCategory(itemTypeId);
            if(null != items){
                for (ItemDTOEx item : items){
                    api.deleteItem(item.getId());
                }
            }
            api.deleteItemCategory(itemTypeId);
            itemTypeId = null;
            itemId = null;
        }

        if(null != userId){
            api.deleteUser(userId);
            userId = null;
        }

        if(null != ORDER_CHANGE_STATUS_APPLY_ID){
            ORDER_CHANGE_STATUS_APPLY_ID = null;
        }

        if(null != ORDER_PERIOD_MONTHLY){
            ORDER_PERIOD_MONTHLY = null;
        }

        BillingProcessConfigurationWS bpcw = api.getBillingProcessConfiguration();
        bpcw.setProratingType(ProratingType.PRORATING_AUTO_ON.getProratingType());
        api.createUpdateBillingProcessConfiguration(bpcw);

        if(null != DYNAMIC_BALANCE_MANAGER_PLUGIN_ID) {
            api.deletePlugin(DYNAMIC_BALANCE_MANAGER_PLUGIN_ID);
        }

        if(null != api){
            api = null;
        }
    }

	@Test
	public void test001CustomerUsagePoolEvaluation() throws Exception {

		PriceModelWS flatPrice = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID);

		UserWS user = api.getUserWS(userId);
		Calendar nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.setTime(user.getNextInvoiceDate());
		nextInvoiceDate.add(Calendar.MONTH, -1);
		user.setNextInvoiceDate(nextInvoiceDate.getTime());
		api.updateUser(user);
		user = api.getUserWS(userId);
		logger.debug("user next Invoice date :::::{}", user.getNextInvoiceDate());

		Integer customerId = user.getCustomerId();
		Integer[] usagePoolsId = new Integer[1];

		usagePoolsId[0] = createFreeUsagePool("100 Local Calls Mins", "100", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Reset To Initial Value");

		UsagePoolWS usagePoolWS = api.getUsagePoolWS(usagePoolsId[0]);
		Integer planId = createPlanForUsagePool(flatPrice, usagePoolsId, itemTypeId);

		PlanWS plan = api.getPlanWS(planId);
		UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planId);
    	assertNotNull("Usage Pool should not be null", usagePools[0]);

    	Integer[] planUsagePoolsId = new Integer[1];
    	BigDecimal[] planUsagePoolsQuantity = new BigDecimal[1];
        int i = 0;
		for (UsagePoolWS usagePool: usagePools) {
			planUsagePoolsId[i]=usagePool.getId();
			planUsagePoolsQuantity[i]=usagePool.getQuantityAsDecimal();
			i++;
		}
		i = 0;
		BigDecimal[] usagePoolsQuantity = new BigDecimal[1];
		for (Integer usagePoolID: usagePoolsId) {
			UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolID);
			usagePoolsQuantity[i] = usagePool.getQuantityAsDecimal();
			i++;
		}
    	assertSameElementsInArray(planUsagePoolsQuantity, usagePoolsQuantity);
    	assertSameElementsInArray(usagePoolsId, planUsagePoolsId);
		Integer plansItemId = plan.getItemId();

		Integer planOrderId = createPlanItemBasedOrder(userId, plansItemId, customerId);

		OrderWS planItemBasedOrder = api.getOrder(planOrderId);

		List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
		assertNotNull("Customer Usage Pool should not be null", customerUsagePools);
		//assertEquals("Customer Usage Pools should contain one pool!", Integer.valueOf("1.00"), Integer.valueOf(customerUsagePools.size()));
    	Integer[] customerUsagePoolsId = new Integer[1];
    	BigDecimal[] customerUsagePoolsQuantity = new BigDecimal[1];
    	i = 0;
		for (CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
			customerUsagePoolsId[i] = customerUsagePool.getUsagePoolId();
			customerUsagePoolsQuantity[i] = customerUsagePool.getQuantityAsDecimal();
			i++;
		}
        assertSameElementsInArray(customerUsagePoolsQuantity, usagePoolsQuantity);
        assertSameElementsInArray(customerUsagePoolsId, planUsagePoolsId);

		//Create order with line quantity 5. Customer Usage Pool Quantity 100.
	    OrderWS order = createUsageOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("5"));
	    order.setActiveSince(planItemBasedOrder.getActiveSince());

	    OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    int orderId1 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
	    order = api.getOrder(orderId1);

	    BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
	    BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;

	    for (OrderLineWS orderLine: order.getOrderLines()) {
	    	if (orderLine.getOrderLineUsagePools().length > 0)
	    		orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
	    }
	    customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
		UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolsId[0]);

		customerUsagePoolQuantity = customerUsagePools.get(0).getQuantityAsDecimal();
		freeUsagePoolQuantity = usagePool.getQuantityAsDecimal();

    	BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);

    	//assert condition for check order line Price & Amount
    	OrderLineWS orderLine = order.getOrderLines()[0];
		assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
		assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
		assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

	    //to check assert condition
    	usagePoolAssert(freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);

    	user = api.getUserWS(userId);
		nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.setTime(user.getNextInvoiceDate());
		nextInvoiceDate.add(Calendar.MONTH, 1);
		user.setNextInvoiceDate(nextInvoiceDate.getTime());
		api.updateUser(user);
		user = api.getUserWS(userId);
		logger.debug("### user next Invoice date :::::{}", user.getNextInvoiceDate());

        api.triggerCustomerUsagePoolEvaluation(1 , nextInvoiceDate.getTime());
    	logger.debug("## customerId: {}", customerId);
    	Thread.sleep(3000);
    	customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
    	logger.debug("## customerUsagePools: {}", customerUsagePools);
    	assertEquals("Expected Customer Usage Pool Quantity: ", new BigDecimal("100"), customerUsagePools.get(0).getQuantityAsDecimal());

    	api.deleteOrder(planOrderId);
		api.deletePlan(planId);
	}

    @Test
    public void test002CustomerReservedUsagePoolEvaluation() throws Exception {

        PriceModelWS flatPrice = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID);

        Integer userId = createUser("test002-user");
        UserWS user = api.getUserWS(userId);
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, -1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
        user = api.getUserWS(userId);
        logger.debug("user next Invoice date :::::{}", user.getNextInvoiceDate());

        Integer customerId = user.getCustomerId();
        Integer usagePoolId = createFreeUsagePool("Reserved VM dynamic pool", "0", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Hours Per Calendar Month");

        UsagePoolWS usagePoolWS = api.getUsagePoolWS(usagePoolId);
        Integer planId = createPlanForUsagePool(flatPrice, new Integer[] { usagePoolId }, itemTypeId);

        PlanWS plan = api.getPlanWS(planId);
        UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planId);
        assertNotNull("Usage Pool should not be null", usagePools);
        equalCheck("Expected UsagePool Count on plan ", 1, usagePools.length);

        UsagePoolWS planUsagePool = usagePools[0];

        equalCheck("Usage Pool Quantity Should be equal to plan's usage pool quantity ", usagePoolWS.getQuantityAsDecimal(),
                planUsagePool.getQuantityAsDecimal());

        equalCheck("Usage Pool Id Should be equal to plan's usage pool id ", usagePoolWS.getId(), planUsagePool.getId());

        Integer plansItemId = plan.getItemId();

        Integer planOrderId = createPlanItemBasedOrder(userId, plansItemId, customerId);

        OrderWS planItemBasedOrder = api.getOrder(planOrderId);


        List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertNotNull("Customer Usage Pool should not be null", customerUsagePools);
        //assertEquals("Customer Usage Pools should contain one pool!", Integer.valueOf("1.00"), Integer.valueOf(customerUsagePools.size()));

        equalCheck("Customer Free UsagePool Size should be", 1, customerUsagePools.size());

        Calendar relMonth = Calendar.getInstance();
        relMonth.setTime(user.getNextInvoiceDate());
        relMonth.add(Calendar.MONTH,-1);
        BigDecimal dynamicQuantity = new BigDecimal(relMonth.getActualMaximum(Calendar.DAY_OF_MONTH) * 24).setScale(2, RoundingMode.FLOOR);

        BigDecimal customerUsagePoolQuantity = customerUsagePools.get(0).getQuantityAsDecimal().setScale(2, RoundingMode.FLOOR);

        equalCheck("Customer Usage Pool Quantity Should be euqal to dynamic generated quantity ", dynamicQuantity, customerUsagePoolQuantity);


        OrderWS order = createUsageOrder(userId, itemId, LINES, ORDER_LINE_PRICE, new BigDecimal("5"));
        order.setActiveSince(planItemBasedOrder.getActiveSince());

        OrderWS orderWs = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        int orderId1 = api.createOrder(orderWs, OrderChangeBL.buildFromOrder(orderWs, ORDER_CHANGE_STATUS_APPLY_ID));
        order = api.getOrder(orderId1);

        BigDecimal orderLineUsagePoolQuantity = BigDecimal.ZERO;

        for (OrderLineWS orderLine: order.getOrderLines()) {
            if (orderLine.getOrderLineUsagePools().length > 0)
                orderLineUsagePoolQuantity = orderLineUsagePoolQuantity.add(orderLine.getOrderLineUsagePools()[0].getQuantityAsDecimal());
        }

        BigDecimal expectedUsagePoolQuantity = (customerUsagePoolQuantity.subtract(new BigDecimal("5")))
                .setScale(2, RoundingMode.FLOOR);

        customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertNotNull("Customer Usage Pool should not be null", customerUsagePools);
        equalCheck("Customer Free UsagePool Size should be", 1, customerUsagePools.size());

        customerUsagePoolQuantity = customerUsagePools.get(0).getQuantityAsDecimal();

        equalCheck("Customer Usage Pool Quantity Update failed! ", expectedUsagePoolQuantity,
                customerUsagePoolQuantity.setScale(2, RoundingMode.FLOOR));

        BigDecimal olUsagePoolAndCustUsagePoolQuantitySum = orderLineUsagePoolQuantity.add(customerUsagePoolQuantity);

        //assert condition for check order line Price & Amount
        OrderLineWS orderLine = order.getOrderLines()[0];
        assertEquals("Expected Order line Price: ", BigDecimal.ZERO, orderLine.getPriceAsDecimal());
        assertEquals("Expected Order line Amount: ", BigDecimal.ZERO, orderLine.getAmountAsDecimal());
        assertEquals("Expected Order total: ", BigDecimal.ZERO , order.getTotalAsDecimal());

        //to check assert condition
        usagePoolAssert(dynamicQuantity, olUsagePoolAndCustUsagePoolQuantitySum, customerUsagePoolQuantity);

        user = api.getUserWS(userId);
        nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
        user = api.getUserWS(userId);
        logger.debug("### user next Invoice date :::::{}", user.getNextInvoiceDate());

        api.triggerCustomerUsagePoolEvaluation(1 , nextInvoiceDate.getTime());
        logger.debug("## customerId: {}", customerId);
        Thread.sleep(3000);
        customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        logger.debug("## customerUsagePools: {}", customerUsagePools);
        Calendar updatedQuantityMonth = Calendar.getInstance();
        updatedQuantityMonth.setTime(user.getNextInvoiceDate());
        updatedQuantityMonth.add(Calendar.MONTH,-1);

        assertEquals("Expected Customer Usage Pool Quantity: ",new BigDecimal(updatedQuantityMonth.getActualMaximum(Calendar.DAY_OF_MONTH) * 24).setScale(2, RoundingMode.FLOOR) , customerUsagePools.get(0).getQuantityAsDecimal().setScale(2, RoundingMode.FLOOR));

        api.deleteOrder(planOrderId);
        api.deletePlan(planId);
    }

    private void equalCheck(String message, Object expected, Object actual) {
        org.testng.AssertJUnit.assertEquals(message, expected, actual);
    }

	protected OrderWS createUsageOrder(int userId, int itemId, int orderLinesCount,
			BigDecimal linePrice, BigDecimal quantity) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(Constants.ORDER_PERIOD_ONCE); // once
		order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
		order.setActiveSince(new Date());
		ArrayList<OrderLineWS> lines = createOrderLines(userId, itemId, orderLinesCount, linePrice, quantity);
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

	private ArrayList<OrderLineWS> createOrderLines(Integer userId, Integer itemId,
                                int orderLinesCount, BigDecimal linePrice, BigDecimal quantity) {
		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(orderLinesCount);
		for (int i = 0; i < orderLinesCount; i++){
			OrderLineWS nextLine = new OrderLineWS();
			nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(itemId);
			nextLine.setQuantity(quantity.multiply(new BigDecimal(i+1)));
			nextLine.setUseItem(true);
			nextLine.setPrice(linePrice);
			lines.add(nextLine);
		}
		return lines;
	}

	private void usagePoolAssert(BigDecimal freeUsagePoolQuantity,
			BigDecimal olUsagePoolAndCustUsagePoolQuantitySum,
			BigDecimal customerUsagePoolQuantity) {
		assertTrue("Customer usage Pool Quantity not negative.", customerUsagePoolQuantity.compareTo(BigDecimal.ZERO) >= 0);
		assertEquals("The free usage pool quantity & sum of customer usage pool, order line usage pool quantity must be same",
				freeUsagePoolQuantity, olUsagePoolAndCustUsagePoolQuantitySum);
	}

    private void assertSameElementsInArray(Object[] actualValues, Object[] expectedValues) {
        for (Object actualValue: actualValues) {
            boolean found = false;
            for (Object expectedValue: expectedValues) {
                if (actualValue.equals(expectedValue)) found = true;
            }
            assertTrue("Elements are not the same in the two arrays", found);
        }
    }

    private Integer createItem(Integer... itemTypes) {

        ItemDTOEx item = buildItem("FUP-Item", "Free Usage Test Item-" + today, itemTypes);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID));
        itemId = api.createItem(item);
        assertNotNull("Item was not created", itemId);
        return itemId;

    }

    private ItemDTOEx buildItem(String number, String desc, Integer... itemTypesId) {
        ItemDTOEx item = new ItemDTOEx();
        Long entitySuffix = System.currentTimeMillis();
        item.setNumber(String.format("%s-%s", number, entitySuffix));
        item.setDescription(String.format("%s-%s", desc, entitySuffix));
        item.setTypes(itemTypesId);
        item.setEntityId(PRANCING_PONY);
        item.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        item.setPriceModelCompanyId(PRANCING_PONY);
        return item;
    }

	// create User
	private Integer createUser(String userName) {
		Date today = new Date();
		UserWS customer = CreateObjectUtil.createCustomer(1, userName + today.getTime(),
				"P@ssword1", Constants.LANGUAGE_ENGLISH_ID, 5, false, 1, null,
				CreateObjectUtil.createCustomerContact("test@gmail.com"));
		userId = api.createUser(customer);
		assertNotNull("Customer/User ID should not be null", userId);
		updateCustomerNextInvoiceDate(userId, api);
	    return userId;
	}

  //create Plan
	private Integer createPlan(Integer usagePoolId, Integer itemTypeId) {
		PriceModelWS flatPrice = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID);
		PlanWS plan = CreateObjectUtil.createPlanBundledItems(PRANCING_PONY, new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID,
                                                              itemTypeId, ORDER_PERIOD_MONTHLY, new BigDecimal(1),
                                                              0, api, usagePoolId);
		plan.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE, flatPrice);
		Integer planId = api.createPlan(plan);
		assertNotNull("Plan ID should not be null", planId);
		return planId;
	}


	// create Free Usage Pool
	private Integer createFreeUsagePool(String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue) {
		UsagePoolWS usagePool = populateFreeUsagePoolObject(usagePoolName, quantity, cyclePeriodUnit, resetValue);
		Integer poolId = api.createUsagePool(usagePool);
        assertNotNull("Free usage pool should not be null ", poolId);
        return poolId;
	}

    private UsagePoolWS populateFreeUsagePoolObject(String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue) {

        UsagePoolWS usagePool = new UsagePoolWS();
        usagePool.setName(usagePoolName + today);
        usagePool.setQuantity(quantity);
        usagePool.setPrecedence(new Integer(1));
        usagePool.setCyclePeriodUnit(cyclePeriodUnit);
        usagePool.setCyclePeriodValue(new Integer(1));
        usagePool.setItemTypes(new Integer[]{itemTypeId});
        usagePool.setItems(new Integer[]{itemId});
        usagePool.setEntityId(PRANCING_PONY);
        usagePool.setUsagePoolResetValue(resetValue);

        return usagePool;
    }

	// create Plan For Usage Pools
	private Integer createPlanForUsagePool(PriceModelWS priceModel, Integer[] usagePoolId, Integer itemType) {

        PlanWS plan = CreateObjectUtil.createPlanBundledItems(PRANCING_PONY, new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID, itemType, ORDER_PERIOD_MONTHLY, PLAN_BUNDLE_QUANTITY, 0, api);
        plan.setUsagePoolIds(usagePoolId);
        plan.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE, priceModel);
        Integer planId = api.createPlan(plan);
        assertNotNull("Plan ID should not be null", planId);
        return planId;
	}

	//create Plan Item Based Order
	private Integer createPlanItemBasedOrder(Integer userId, Integer plansItemId, Integer customerId) {

		List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool created", customerUsagePools.size() == 0);
        UserWS user = api.getUserWS(userId);
		Calendar activeSince = Calendar.getInstance();
		activeSince.setTime(user.getNextInvoiceDate());
		activeSince.add(Calendar.MONTH, -1);
		OrderWS planItemBasedOrder = getUserSubscriptionToPlan(activeSince.getTime(), userId, Constants.ORDER_BILLING_PRE_PAID, ORDER_PERIOD_MONTHLY, plansItemId, 1);
		Integer orderId = api.createOrder(planItemBasedOrder, OrderChangeBL.buildFromOrder(planItemBasedOrder, ORDER_CHANGE_STATUS_APPLY_ID));
		assertNotNull("Order Id cannot be null.", orderId);

		customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool not created", customerUsagePools.size() > 0);
        return orderId;
	}

    private OrderWS getUserSubscriptionToPlan(Date since, Integer userId,
                                              Integer billingType, Integer orderPeriodID,
                                              Integer plansItemId, Integer planQuantity) {
        logger.debug("since :::::::::" + since);
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingType);
        order.setPeriod(orderPeriodID);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        order.setActiveSince(since);
        order.setProrateFlag(Boolean.TRUE);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(planQuantity);
        line.setDescription("Order line for plan subscription");
        line.setItemId(plansItemId);
        line.setUseItem(true);
        line.setPrice(BigDecimal.ZERO);

        order.setOrderLines(new OrderLineWS[]{line});

        return order;
    }

	private String getName(List<InternationalDescriptionWS> names,int langId) {
        for (InternationalDescriptionWS name : names) {
            if (name.getLanguageId() == langId) {
                return name.getContent();
            }
        }
        return "";
    }

    // create Item category
    private Integer createItemType(String description) {
        ItemTypeWS itemType = buildItemType(description);
        Integer itemTypeId = api.createItemCategory(itemType);
        assertNotNull(itemTypeId);
        ItemTypeWS[] types = api.getAllItemCategories();

        boolean addedFound = false;
        for (int i = 0; i < types.length; ++i) {
            if (itemType.getDescription().equals(types[i].getDescription())) {
                logger.debug("Test category was found. Creation was completed successfully.");
                addedFound = true;
                break;
            }
        }
        assertTrue(itemType.getDescription() + " not found.", addedFound);
        return itemTypeId;
    }

    private ItemTypeWS buildItemType(String desc){
        ItemTypeWS itemType = new ItemTypeWS();

        itemType.setDescription(desc);
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        itemType.setEntityId(PRANCING_PONY);
        List<Integer> entities = new ArrayList<Integer>(1);
        entities.add(PRANCING_PONY);
        itemType.setEntities(entities);
        itemType.setAllowAssetManagement(DISABLED);

        return itemType;
    }

    private Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
        OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
        for (OrderChangeStatusWS status : statuses) {
            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status.getId();
            }
        }
        //there is no APPLY status in db so create one
        OrderChangeStatusWS apply = new OrderChangeStatusWS();
        String status1Name = "APPLY: " + System.currentTimeMillis();
        OrderChangeStatusWS status = new OrderChangeStatusWS();
        status.setApplyToOrder(ApplyToOrder.YES);
        status.setDeleted(0);
        status.setOrder(1);
        status.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
        return api.createOrderChangeStatus(apply);
    }

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "INV:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    private UserWS updateCustomerNextInvoiceDate(Integer userId, JbillingAPI api) {
        UserWS user = api.getUserWS(userId);
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
        return api.getUserWS(userId);
    }

    private Integer getOrCreatePluginWithoutParams(String className, int processingOrder) {
        PluggableTaskWS[] taskWSs = api.getPluginsWS(api.getCallerCompanyId(), className);
        if(taskWSs.length != 0){
            return taskWSs[0].getId();
        }
        PluggableTaskWS pluggableTaskWS = new PluggableTaskWS();
        pluggableTaskWS.setTypeId(api.getPluginTypeWSByClassName(className).getId());
        pluggableTaskWS.setProcessingOrder(processingOrder);
        pluggableTaskWS.setOwningEntityId(api.getCallerCompanyId());
        return api.createPlugin(pluggableTaskWS);
    }
}
