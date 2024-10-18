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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * PlanSubcriptionUsagePoolTest
 * Test cases for Plan creation and Customer Usage Pool association 
 * upon successful subscription of user to plan.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

@Test(groups = { "usagePools" }, testName = "PlanSubcriptionUsagePoolTest")
public class PlanSubcriptionUsagePoolTest {

    private static final Logger logger = LoggerFactory.getLogger(PlanSubcriptionUsagePoolTest.class);

	public PlanSubcriptionUsagePoolTest() {
		logger.debug("PlanSubcriptionUsagePoolTest");
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

	private JbillingAPI api;

    @BeforeClass
    public void initializeTests(){
		try {
			api = JbillingAPIFactory.getAPI();
		} catch (Exception e) {
			logger.error("Error while getting API", e);
		}

        // Create and persist Test User
        userId = createUser("UsagePoolsTestUser");

        // Create and persist Test Item Category
        itemTypeId = createItemType("PlanSubscriptionTestItemType");

        // Create and persist Test item
        itemId = createItem(itemTypeId);

        ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);

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

        if(null != api){
            api = null;
        }
    }
	
	@Test
	public void test001UsagePoolsCreation() throws Exception {
        try {
				UsagePoolWS usagePool = populateFreeUsagePoolObject("100 National Calls", BigDecimal.ONE.toString(), Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Zero");

		        logger.debug("Creating usagePool ... {}", usagePool);
	            Integer usagePoolId = api.createUsagePool(usagePool);
	            assertNotNull("The item was not created", usagePoolId);
	            logger.debug("Done!");
	            api.deleteUsagePool(usagePoolId);
	        } catch (Exception e) {
	            e.printStackTrace();
	            fail("Exception caught:" + e);
	        }
	}
	
	@Test
	public void test002CreatePlanWithUsagePool() throws Exception {
    	Integer usagePoolId = createFreeUsagePool("130 National Calls", "100", Constants.USAGE_POOL_CYCLE_PERIOD_DAYS, "Reset To Initial Value");
    	Integer planId = createPlan(usagePoolId, itemTypeId);
		UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planId);
    	assertNotNull("Usage Pool should not be null", usagePools);
    	assertTrue("Usage Pools should contain one pool", Integer.valueOf(1).equals(usagePools.length));
    	assertEquals(usagePoolId.intValue(), usagePools[0].getId());
    	api.deletePlan(planId);
    	api.deleteUsagePool(usagePoolId);
    }	
	
	@Test
	public void test003CreatePlanWithMultpileUsagePools() throws Exception {
		
		PriceModelWS flatPrice = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID);
		
		UserWS user = api.getUserWS(userId);
		Integer customerId = user.getCustomerId();
		Integer[] usagePoolsId = new Integer[2];
		
		usagePoolsId[0] = createFreeUsagePool("100 Local Calls Mins", "100", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Zero");
		usagePoolsId[1] = createFreeUsagePool("200 Local Calls Mins", "200", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Zero");
		
		Integer planId = createPlanForMultpileUsagePool(flatPrice, usagePoolsId, itemTypeId);

		PlanWS plan = api.getPlanWS(planId);
		UsagePoolWS[] usagePools = api.getUsagePoolsByPlanId(planId);
    	assertNotNull("Usage Pool should not be null", usagePools[0]);
    	Integer[] planUsagePoolsId = new Integer[2];
    	BigDecimal[] planUsagePoolsQuantity = new BigDecimal[2];
        int i = 0;
		for (UsagePoolWS usagePool: usagePools) {
			planUsagePoolsId[i]=usagePool.getId();
			planUsagePoolsQuantity[i]=usagePool.getQuantityAsDecimal();
			i++;
		}
		i = 0;
		BigDecimal[] usagePoolsQuantity = new BigDecimal[2];
		for (Integer usagePoolID: usagePoolsId) {
			UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolID);
			usagePoolsQuantity[i] = usagePool.getQuantityAsDecimal();
			i++;
		}
    	assertSameElementsInArray(planUsagePoolsQuantity, usagePoolsQuantity);
    	assertSameElementsInArray(usagePoolsId, planUsagePoolsId);
		Integer plansItemId = plan.getItemId();
		
		Integer planOrderId = createPlanItemBasedOrder(userId, plansItemId, customerId);
		List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
		assertNotNull("Customer Usage Pool should not be null", customerUsagePools);
		assertEquals("Customer Usage Pools should contain two pools!", Integer.valueOf(2), Integer.valueOf(customerUsagePools.size()));
    	Integer[] customerUsagePoolsId = new Integer[2];
    	BigDecimal[] customerUsagePoolsQuantity = new BigDecimal[2];
    	//Sorting
    	i = 0;
		for (CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
			customerUsagePoolsId[i] = customerUsagePool.getUsagePoolId();
			customerUsagePoolsQuantity[i] = customerUsagePool.getQuantityAsDecimal();
			i++;
		}
        assertSameElementsInArray(customerUsagePoolsQuantity, usagePoolsQuantity);
        assertSameElementsInArray(customerUsagePoolsId, planUsagePoolsId);


		for (CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
			UsagePoolWS usagePool = api.getUsagePoolWS(customerUsagePool.getUsagePoolId());
			OrderWS order = api.getOrder(planOrderId);
			Calendar cal = Calendar.getInstance();
			cal.setTime(order.getActiveSince());
			cal.add(Calendar.MONTH, usagePool.getCyclePeriodValue());

			cal.add(Calendar.DATE, -1);
            cal.set(Calendar.MILLISECOND, 999);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.HOUR_OF_DAY,23);

			Date calculatedCycleEndDate = cal.getTime();

            assertEquals(calculatedCycleEndDate, customerUsagePool.getCycleEndDate());
		}
		
		api.deleteOrder(planOrderId);
		api.deletePlan(planId);
//        api.deleteUsagePool(usagePoolsId[0]);
//        api.deleteUsagePool(usagePoolsId[1]);
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
        logger.debug("usagePoolId :: {}", poolId);
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

	// create Plan For Multiple Usage Pools
	private Integer createPlanForMultpileUsagePool(PriceModelWS priceModel, Integer[] usagePoolsId, Integer itemType) {

        PlanWS plan = CreateObjectUtil.createPlanBundledItems(PRANCING_PONY, new BigDecimal(1), Constants.PRIMARY_CURRENCY_ID, itemType, ORDER_PERIOD_MONTHLY, PLAN_BUNDLE_QUANTITY, 0, api);
        plan.setUsagePoolIds(usagePoolsId);
        plan.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE, priceModel);
        Integer planId = api.createPlan(plan);
        assertNotNull("Plan ID should not be null", planId);
        return planId;
	}

	//create Plan Item Based Order
	private Integer createPlanItemBasedOrder(Integer userId, Integer plansItemId, Integer customerId) {
	
		List<CustomerUsagePoolWS> customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool created", customerUsagePools.size() == 0);
        
		OrderWS planItemBasedOrder = getUserSubscriptionToPlan(new Date(), userId, Constants.ORDER_BILLING_PRE_PAID, ORDER_PERIOD_MONTHLY, plansItemId, 1);
		Integer orderId = api.createOrder(planItemBasedOrder, OrderChangeBL.buildFromOrder(planItemBasedOrder, ORDER_CHANGE_STATUS_APPLY_ID));
		assertNotNull("Order Id cannot be null.", orderId);
		
		customerUsagePools = Arrays.asList(api.getCustomerUsagePoolsByCustomerId(customerId));
        assertTrue("Customer Usage Pool not created", customerUsagePools.size() > 0);
        return orderId;
	}
	
	private OrderWS getUserSubscriptionToPlan(Date since, Integer userId, 
			Integer billingType, Integer orderPeriodID, 
			Integer plansItemId, Integer planQuantity) {
			OrderWS order = new OrderWS();
			order.setUserId(userId);
			order.setBillingTypeId(billingType);
			order.setPeriod(orderPeriodID);
			order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
			order.setActiveSince(since);
			
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
	
    private UsagePoolWS createUsagePoolWithMultipleNames() {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();

            UsagePoolWS newUsagePool= new UsagePoolWS();

            List<InternationalDescriptionWS> names = new java.util.ArrayList<InternationalDescriptionWS>();
            InternationalDescriptionWS enName = new InternationalDescriptionWS(1, "104 National Calls"+today);
            InternationalDescriptionWS frName = new InternationalDescriptionWS(2, "104 National calls Fr");
            names.add(enName);
            names.add(frName);

            newUsagePool.setEntityId(new Integer(1));
            newUsagePool.setNames(names);
            newUsagePool.setQuantity(BigDecimal.TEN.toString());
            newUsagePool.setPrecedence(new Integer(1));
            newUsagePool.setCyclePeriodUnit("Months");
            newUsagePool.setCyclePeriodValue(new Integer(1));
            newUsagePool.setUsagePoolResetValue("Zero");
            Integer itemTypes[] = new Integer[1];
            itemTypes[0] = new Integer(1);
            newUsagePool.setItemTypes(itemTypes);
            Integer items[] = new Integer[1];
            items[0] = new Integer(1);
            newUsagePool.setItems(items);

            logger.debug("Creating Usage Pools ...{}", newUsagePool);
            Integer ret = api.createUsagePool(newUsagePool);
            assertNotNull("The usage pool was not created", ret);
            logger.debug("Done!");
            newUsagePool = api.getUsagePoolWS(ret);

            return newUsagePool;
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
            return null;
        }
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

}
