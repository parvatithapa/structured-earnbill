/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.item;

import java.io.IOException;
import java.util.*;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.JBillingTestUtils;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import static org.testng.AssertJUnit.*;
/**
 * @author Khobab
 */
@Test(groups = { "web-services", "item" }, testName = "PlansEnhancementTest")
public class PlansEnhancementTest {

    private static final Logger logger = LoggerFactory.getLogger(PlansEnhancementTest.class);
    private static final Integer PRANCING_PONY = new Integer(1);
    private static final Integer US_DOLLAR = new Integer(1);

    private static final Integer ENABLED = Integer.valueOf(1);
    private static final Integer DISABLED = Integer.valueOf(0);

    private static Integer MONTHLY_PERIOD;
	private static Integer ORDER_CHANGE_STATUS_APPLY_ID;

    private static Integer TEST_USER_ID;
    private static Integer TEST_ITEM_TYPE_ID;

    private static JbillingAPI api;

    @BeforeClass
    public void initializeTests() throws IOException, JbillingAPIException {
        if(null == api){
            api = JbillingAPIFactory.getAPI();
        }

        // Create And Persist User
        UserWS customer = null;
        try {
            customer = com.sapienter.jbilling.server.user.WSTest
		            .createUser(true, true, null, US_DOLLAR, true);
        } catch (Exception e) {
            fail("Error creating customer!!!");
        }
        TEST_USER_ID = customer.getUserId();

        // Create Item Type
        ItemTypeWS itemType = getItemType();
        // Persist
        TEST_ITEM_TYPE_ID = api.createItemCategory(itemType);
	    ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(api);
	    MONTHLY_PERIOD = getOrCreateMonthlyOrderPeriod(api);
    }

    @AfterClass
    public void tearDown(){
        if(null != TEST_ITEM_TYPE_ID){
            api.deleteItemCategory(TEST_ITEM_TYPE_ID);
            TEST_ITEM_TYPE_ID = null;
        }
        if(null != TEST_USER_ID){
            api.deleteUser(TEST_USER_ID);
            TEST_USER_ID = null;
        }
        if(null != api){
            api = null;
        }
    }

    @Test
    public void test001ItemValidityPeriod() {

        //creating an item
        ItemDTOEx newItem = getItem(TEST_ITEM_TYPE_ID);

        logger.debug("Creating item {}", newItem);
        Integer ret = api.createItem(newItem);
        assertNotNull("The item was not created", ret);

        ItemDTOEx returned = api.getItem(ret, TEST_USER_ID, null);
        assertNull("Active since date should be null", returned.getActiveSince());
        assertNull("Active until date should be null", returned.getActiveUntil());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 10);
        returned.setActiveUntil(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 10);
        Date activeSince = cal.getTime();
        returned.setActiveSince(activeSince);

        try {
            api.updateItem(returned);
            fail("Active since date can not be after active until");
        } catch(SessionInternalError e) {
            // ok
        }

        cal.add(Calendar.DAY_OF_MONTH, 10);
        Date activeUntil = cal.getTime();
        returned.setActiveUntil(activeUntil);

        api.updateItem(returned);

        returned = api.getItem(ret, TEST_USER_ID, null);

        assertTrue(DateUtils.isSameDay(activeSince, returned.getActiveSince()));
        assertTrue(DateUtils.isSameDay(activeUntil, returned.getActiveUntil()));

        // create order
        List<Integer> items = new ArrayList<Integer>(1);
        items.add(returned.getId());

        OrderWS order = getOrder(items);
	    OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
	    for(OrderChangeWS change : changes) change.setStartDate(order.getActiveSince());

        try {
            order.setId(api.createOrder(order, changes));
            fail("Order should not have been created");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "validation.order.line.not.added.valdidity.period");
        }

        // match order and products validity period
        order.setActiveSince(activeSince);
        order.setActiveUntil(activeUntil);

	    changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
	    for(OrderChangeWS change : changes) change.setStartDate(order.getActiveSince());

        Integer orderCreatedId = api.createOrder(order, changes);

        assertNotNull("order was not created", orderCreatedId);

        //cleanup
        api.deleteOrder(orderCreatedId);
        api.deleteItem(ret);
    }
    
    @Test
    public void test003CreateCategoryExclusiveValidation() {
        ItemTypeWS type = getItemType();
        type.setOnePerCustomer(true);
        type.setOnePerOrder(true);
        type.setId(api.createItemCategory(type));

        assertNotNull("Item type was not created", type.getId());

        ItemTypeWS[] all = api.getAllItemCategories();
        ItemTypeWS returned = null;
        for(ItemTypeWS single : all) {
            if(single.getId().intValue() == type.getId().intValue()) {
                returned = single;
                break;
            }
        }

        if(null == returned){
            fail(String.format("Item Type %d not found!!", type.getId()));
        }

        // verify that both boolean validation values are not true
        assertTrue(!(returned.isOnePerCustomer() && returned.isOnePerOrder()));

        //clean up
        api.deleteItemCategory(type.getId());
    }

    @Test
    public void test004OnePerOrderCategoryValidation() {
        ItemTypeWS type = getItemType();
        type.setOnePerOrder(true);
        type.setId(api.createItemCategory(type));

        ItemDTOEx item1 = getItem(type.getId());
        item1.setId(api.createItem(item1));

        ItemDTOEx item2 = getItem(type.getId());
        item2.setId(api.createItem(item2));

        List<Integer> items = new ArrayList<Integer>();
        items.add(item1.getId());
        items.add(item2.getId());
        OrderWS order = getOrder(items);

        // try to create order with two items from a category
        try {
            order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID))); // create order
            // two tariff plans can not be active at the same time
            fail("Order should not have been created");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "validation.order.line.not.added.not.compatible");
        }

        // try to create order with items in different order
        items = new ArrayList<Integer>();
        items.add(item1.getId());
        OrderWS order1 = getOrder(items);

        order1.setId(api.createOrder(order1, OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID))); // create order

        assertNotNull("Order was not created", order1.getId());

        items = new ArrayList<Integer>();
        items.add(item2.getId());
        OrderWS order2 = getOrder(items);

        order2.setId(api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID))); // create order

        assertNotNull("Order was not created", order2.getId());

        //cleanup
        api.deleteOrder(order1.getId());
        api.deleteOrder(order2.getId());
        api.deleteItem(item1.getId());
        api.deleteItem(item2.getId());
        api.deleteItemCategory(type.getId());
    }

    @Test
    public void test005OnePerCustomerCategoryValidation() {
        ItemTypeWS type = getItemType();
        type.setOnePerCustomer(true);
        type.setId(api.createItemCategory(type));

        ItemDTOEx item1 = getItem(type.getId());
        item1.setId(api.createItem(item1));

        ItemDTOEx item2 = getItem(type.getId());
        item2.setId(api.createItem(item2));

        List<Integer> items = new ArrayList<Integer>();
        items.add(item1.getId());
        items.add(item2.getId());
        OrderWS order = getOrder(items);

	    OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
	    for(OrderChangeWS change : changes) change.setStartDate(order.getActiveSince());

        // try to create order with two items from a category
        try {
            order.setId(api.createOrder(order, changes)); // create order
            // two tariff plans can not be active at the same time
            fail("Order should not have been created");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "validation.order.line.not.added.not.compatible");
        }

        // try to create order with only one item in category
        items = new ArrayList<Integer>();
        items.add(item1.getId());
        OrderWS order1 = getOrder(items);

	    changes = OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID);
	    for(OrderChangeWS change : changes) change.setStartDate(order1.getActiveSince());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 10);
        order1.setActiveUntil(cal.getTime());
        order1.setId(api.createOrder(order1, changes)); // create order

        assertNotNull("Order was not created", order1.getId());

        items = new ArrayList<Integer>();
        items.add(item2.getId());
        OrderWS order2 = getOrder(items);

	    changes = OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID);
	    for(OrderChangeWS change : changes) change.setStartDate(order2.getActiveSince());

        // try to create another order with item from the one per customer category
        try {
            order2.setId(api.createOrder(order2, changes)); // create order
            // two tariff plans can not be active at the same time
            fail("Order should not have been created");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "validation.order.line.not.added.not.compatible");
        }

        // not create order with active since after order1 has expired
        cal.add(Calendar.DAY_OF_MONTH, 1);
        order2.setActiveSince(cal.getTime());
	    for(OrderChangeWS change : changes) change.setStartDate(order2.getActiveSince());

        order2.setId(api.createOrder(order2, changes)); // create order

        assertNotNull("Order was not created", order2.getId());

        //cleanup
        api.deleteOrder(order1.getId());
        api.deleteOrder(order2.getId());
        api.deleteItem(item1.getId());
        api.deleteItem(item2.getId());
        api.deleteItemCategory(type.getId());
    }

    private ItemDTOEx getItem(Integer... types){
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription("TestItem: " + System.currentTimeMillis());
        item.setNumber("TestWS-" + System.currentTimeMillis());
        item.setTypes(types);
        item.setAssetManagementEnabled(DISABLED);
        item.setExcludedTypes(new Integer[]{});
        item.setHasDecimals(DISABLED);
        item.setGlobal(false);
        item.setDeleted(DISABLED);
        item.setEntityId(PRANCING_PONY);
        ArrayList<Integer> entities = new ArrayList<Integer>();
        entities.add(PRANCING_PONY);
        item.setEntities(entities);
        return item;
    }
    
    private OrderWS getOrder(List<Integer> items) {
    	OrderWS order = new OrderWS();
    	order.setUserId(TEST_USER_ID);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(MONTHLY_PERIOD);
        order.setCurrencyId(US_DOLLAR);
        order.setActiveSince(Util.truncateDate(new Date()));
        order.setNotify(new Integer(0));
        order.setDueDateUnitId(new Integer(3));
        order.setDfFm(new Integer(0));
        order.setOwnInvoice(new Integer(0));
        order.setNotesInInvoice(new Integer(0));
        
        OrderLineWS[] lines = new OrderLineWS[items.size()];
        int index = 0;
        for(Integer itemId : items) {
        	OrderLineWS line = new OrderLineWS();
        	line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        	line.setItemId(itemId);
        	line.setUseItem(true);
        	line.setQuantity(1);
        	
        	lines[index] = line;
        	
        	index++;
        }
        order.setOrderLines(lines);
        
        return order;
    }

	private PlanWS getPlan(String description, Integer itemId) {
		PlanWS plan = new PlanWS();
		plan.setItemId(itemId);
		plan.setDescription(description);
		plan.setPeriodId(MONTHLY_PERIOD);
		return plan;
	}

	private ItemTypeWS getItemType() {
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("TestCategory: " + System.currentTimeMillis());
		itemType.setEntityId(PRANCING_PONY);
		itemType.setEntities(new ArrayList<Integer>(PRANCING_PONY));
		itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		itemType.setAllowAssetManagement(DISABLED);

		return itemType;
	}

	private static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api) {
		OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
		Integer statusId = null;
		for (OrderChangeStatusWS orderChangeStatus : list) {
			if (orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
				statusId = orderChangeStatus.getId();
				break;
			}
		}
		if (statusId != null) {
			return statusId;
		} else {
			OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
			newStatus.setApplyToOrder(ApplyToOrder.YES);
			newStatus.setDeleted(0);
			newStatus.setOrder(1);
			newStatus.addDescription(new InternationalDescriptionWS(
					com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
			return api.createOrderChangeStatus(newStatus);
		}
	}

	private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api) {
		OrderPeriodWS[] periodsList = api.getOrderPeriods();
		Integer periodId = getOrderPeriodFromList(periodsList);
		if (periodId != null) {
			return periodId;
		} else {
			OrderPeriodWS period = new OrderPeriodWS();
			period.setPeriodUnitId(PeriodUnitDTO.MONTH);
			period.setValue(1);
			period.getDescriptions().add(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "Monthly Period"));
			api.updateOrCreateOrderPeriod(period);
			periodsList = api.getOrderPeriods();
			return getOrderPeriodFromList(periodsList);
		}
	}

	private static Integer getOrderPeriodFromList(OrderPeriodWS[] list) {
		Integer periodId = null;
		for (OrderPeriodWS period : list) {
			if (period.getPeriodUnitId() == PeriodUnitDTO.MONTH) {
				periodId = period.getId();
				break;
			}
		}
		if (periodId != null) {
			return periodId;
		} else {
			return null;
		}
	}
 }
