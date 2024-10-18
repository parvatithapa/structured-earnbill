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

package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

/**
 * @author Brian Cowdery
 * @since 28/03/11
 */
@Test(groups = { "web-services", "pricing", "pooled" }, testName = "PooledPricingWSTest")
public class PooledPricingWSTest {

	private static JbillingAPI api;

	private static Integer CURRENCY_ID;
	private static Integer ACCOUNT_TYPE;
	private static Integer ORDER_CHANGE_APPLY_STATUS;
	private static Integer MONTHLY_PERIOD;
	private static Integer LANGUAGE_ID;

	@BeforeTest
	public void getAPI() throws Exception {
		api = JbillingAPIFactory.getAPI();
		MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);
		ORDER_CHANGE_APPLY_STATUS = PricingTestHelper.getOrCreateOrderChangeApplyStatus(api);
		CURRENCY_ID = PricingTestHelper.CURRENCY_USD;
		ACCOUNT_TYPE = PricingTestHelper.PRANCING_PONY_BASIC_ACCOUNT_TYPE;
		LANGUAGE_ID = PricingTestHelper.LANGUAGE_US;
	}

    @Test
    public void testPooledPricing() throws Exception {
	    String random = String.valueOf(System.currentTimeMillis());
        UserWS user = new UserWS();
        user.setUserName("pooled-pricing-test-"+random);
        user.setPassword("P@ssword1");
        user.setLanguageId(LANGUAGE_ID);
        user.setCurrencyId(CURRENCY_ID);
        user.setMainRoleId(5);
        user.setAccountTypeId(ACCOUNT_TYPE);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        user.setMainSubscription(new MainSubscriptionWS(MONTHLY_PERIOD, new Date().getDate()));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@test.com");
        metaField1.setGroupId(1);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Pricing Test");
        metaField2.setGroupId(1);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Pooled Pricing");
        metaField3.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
        });

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());
        
        Integer categoryId = PricingTestHelper.createItemCategory(api);
        ItemDTOEx newItem = createItem("1", categoryId);
        Integer newItemId = api.createItem(newItem);
        
        // subscription item for plan
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription("Test Long Distance Plan-"+random);
        item.setNumber("TEST-LD-PLAN-01-"+random);
        item.setPrice("10.00");
        item.setTypes(new Integer[] {categoryId});

        item.setId(api.createItem(item));
        

        // create a pooled plan
        // overage is charged at $1.00/unit
        PriceModelWS pooledPrice = new PriceModelWS(PriceModelStrategy.POOLED.name(), new BigDecimal("1.00"), CURRENCY_ID);
        pooledPrice.addAttribute("pool_item_id", item.getId().toString());
        pooledPrice.addAttribute("multiplier", "5");

        PlanItemWS callPrice = new PlanItemWS();
        callPrice.setItemId(newItemId);
        callPrice.getModels().put(CommonConstants.EPOCH_DATE, pooledPrice);

        PlanWS plan = new PlanWS();
        plan.setItemId(item.getId());
        plan.setDescription("Pooled long distance calls." + random);
        plan.setPeriodId(MONTHLY_PERIOD);
        plan.addPlanItem(callPrice);

        plan.setId(api.createPlan(plan)); // create plan
        assertNotNull("plan created", plan.getId());



        // subscribe the customer to the pooled pricing plan
        // because we count usage of the pooled item, this order should be a main subscription
        // to define the period within which to look for usage of the LONG_DISTANCE_PLAN_ITEM
        OrderWS order = new OrderWS();
    	order.setUserId(user.getUserId());
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(MONTHLY_PERIOD);
        order.setCurrencyId(CURRENCY_ID);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(item.getId());
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

	    // create order
        order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_APPLY_STATUS)));
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());


        // rate an order to test the pool
        OrderWS testOrder = new OrderWS();
    	testOrder.setUserId(user.getUserId());
        testOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        testOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        testOrder.setCurrencyId(CURRENCY_ID);
        testOrder.setActiveSince(new Date());

        // test that a purchase of 5 units (equal to the included pool size)
        // rates at zero
        OrderLineWS testLine = new OrderLineWS();
        testLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        testLine.setItemId(newItemId);
        testLine.setUseItem(true);
        testLine.setQuantity(5); // equal to pool size
        testOrder.setOrderLines(new OrderLineWS[] { testLine });

        testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_APPLY_STATUS));

        assertEquals(testOrder.getOrderLines().length, 1);
        assertEquals(new BigDecimal("5"), testOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertEquals(new BigDecimal("0.00"), testOrder.getOrderLines()[0].getPriceAsDecimal());
        assertEquals(new BigDecimal("0.00"), testOrder.getOrderLines()[0].getAmountAsDecimal());

        // test that a purchase over the 5 included units
        // rates at 1.00 per extra unit
        testLine = new OrderLineWS();
        testLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        testLine.setItemId(newItemId);
        testLine.setUseItem(true);
        testLine.setQuantity(7); // 2 units over pool size
        testOrder.setOrderLines(new OrderLineWS[] { testLine });

        testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_APPLY_STATUS));

        assertEquals(testOrder.getOrderLines().length, 1);
        assertEquals(new BigDecimal("7"), testOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertEquals(new BigDecimal("0.29"), testOrder.getOrderLines()[0].getPriceAsDecimal());  // $0.29 * 7
        assertEquals(new BigDecimal("2.00"), testOrder.getOrderLines()[0].getAmountAsDecimal()); // = 1.999999

        // cleanup
        api.deletePlan(plan.getId());
        api.deleteOrder(order.getId());
        api.deleteItem(item.getId());
        api.deleteUser(user.getUserId());
        api.deleteItem(newItemId);
        api.deleteItemCategory(categoryId);
    }

    //Test issue #3665
    @Test
    public void testCreateTieredPlan() throws Exception {
	    String random = String.valueOf(System.currentTimeMillis());
        Integer categoryId = PricingTestHelper.createItemCategory(api);
    	Integer planItemId = api.createItem(createItem("1", categoryId));
        Integer planAffectedItemId = api.createItem(createItem("1", categoryId));

        PriceModelWS tieredPrice = new PriceModelWS(PriceModelStrategy.TIERED.name(), null, CURRENCY_ID);
        tieredPrice.addAttribute("0", "*breakme&");

        PlanItemWS callPrice = new PlanItemWS();
        callPrice.setItemId(planAffectedItemId);
        callPrice.getModels().put(CommonConstants.EPOCH_DATE, tieredPrice);

        PlanWS plan = new PlanWS();
        plan.setItemId(planItemId);
        plan.setDescription("Tiered calls." + random);
        plan.setPeriodId(MONTHLY_PERIOD);
        plan.addPlanItem(callPrice);

        try {
            plan.setId(api.createPlan(plan)); // create plan
            fail("Validation error expected");
        } catch (SessionInternalError e) {
            assertContainsError(e, "TieredPricingStrategy,0,validation.error.not.a.number", null);
            //assertEquals("Expected TieredPricingStrategy,0,validation.error.not.a.number, "+e.getMessage() + e.getCause(), "TieredPricingStrategy,0,validation.error.not.a.number", e.getErrorMessages()[0]);
        }

        tieredPrice.addAttribute("0", "4");

        plan.setId(api.createPlan(plan)); // create plan
        assertNotNull("plan created", plan.getId());

        // cleanup
        api.deletePlan(plan.getId());
        api.deleteItem(planItemId);
        api.deleteItem(planAffectedItemId);
        api.deleteItemCategory(categoryId);
    }
    
    private ItemDTOEx createItem(String price, Integer type){
    	ItemDTOEx testItem = new ItemDTOEx();
        testItem.setDescription("item"+Short.toString((short)System.currentTimeMillis()));
        testItem.setEntityId(api.getCallerCompanyId());
        testItem.setTypes(new Integer[]{type});
        testItem.setPrice(price);
        testItem.setNumber("Number"+Short.toString((short)System.currentTimeMillis()));
        testItem.setActiveSince(Util.getDate(2010, 1, 1));
        return testItem;
    }

}
