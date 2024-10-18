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

package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.text.DateFormat;
import java.util.*;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

/**
 * @author Brian Cowdery
 * @since 06-08-2010
 */
@Test(groups = {"web-services", "pricing"}, testName = "pricing.WSTest")
public class WSTest {

	private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");
	private static Integer CURRENCY_ID = 1;

	private static Integer PP_ACCOUNT_TYPE = 1;
	private static Integer MORDOR_ACCOUNT_TYPE = 2;

	private static Integer PP_ENTITY_ID;
	private static Integer MORDOR_ENTITY_ID;

	private static Integer PP_MONTHLY_PERIOD;
	private static Integer MORDOR_MONTHLY_PERIOD;

	private static Integer PP_ORDER_CHANGE_APPLY_STATUS;

	private static JbillingAPI api = null;
	private static JbillingAPI apiMordor = null;


	@BeforeClass
	public void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		apiMordor = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_MORDOR.getName());

		PP_ENTITY_ID = api.getCallerCompanyId();
		MORDOR_ENTITY_ID = apiMordor.getCallerCompanyId();

		PP_MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);
		MORDOR_MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(apiMordor);

		PP_ORDER_CHANGE_APPLY_STATUS = PricingTestHelper.getOrCreateOrderChangeApplyStatus(api);
	}

	/**
	 * Tests that the get all plans API method only returns plans for the caller company with
	 * all of the fields and values intact.
	 */
	@Test
	public void test001GetAllPlans() {
		List<PlanWS> plans = Arrays.asList(api.getAllPlans());

		Integer initialSize = plans.size();

		//Create unique description for plan

		Integer categoryId = createItemCategory(api);
		String newPlanDes = "Test-plan-" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		Integer newPlanId = api.createPlan(planWS);

		plans = Arrays.asList(api.getAllPlans());

		assertNotNull("plans is not null", plans);
		assertTrue("new Plan is not added", initialSize + 1 == plans.size());

		PlanWS newlyAddedPlan = null;
		for (PlanWS pWs : plans) {
			if (pWs.getId().equals(newPlanId)) {
				newlyAddedPlan = pWs;
			}
		}

		assertNotNull("Newly added plan not in list", newlyAddedPlan);
		assertEquals(newPlanId, newlyAddedPlan.getId());
		assertEquals(newPlanDes, newlyAddedPlan.getDescription());
		assertEquals(PP_MONTHLY_PERIOD.intValue(), newlyAddedPlan.getPeriodId().intValue());

		PlanItemWS planItem = newlyAddedPlan.getPlanItems().get(0);
		assertEquals(planAffectedItemId, planItem.getAffectedItemId());

		PriceModelWS model = planItem.getModel();
		assertEquals("FLAT", model.getType());
		assertEquals(BigDecimal.ONE, model.getRateAsDecimal());

		//cleanup
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests subscription / un-subscription to a plan by creating and deleting an order
	 * containing the plan item.
	 */
	@Test
	public void test002CreateDeleteOrder() {
		UserWS user = PricingTestHelper.buildUser("plan-test-01-", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());

		Integer categoryId = createItemCategory(api);
		String newPlanDes = "Test-plan-" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));

		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		Integer newPlanId = api.createPlan(planWS); // Creating Plan

		//subscribed the user to plan
		OrderWS order = PricingTestHelper.buildOrder(user.getUserId(), planItemId, PP_MONTHLY_PERIOD);
		Integer orderIdCreated = api.createOrder(order, OrderChangeBL.buildFromOrder(order, PP_ORDER_CHANGE_APPLY_STATUS));
		assertNotNull("order created", orderIdCreated);
		order = api.getOrder(orderIdCreated);

		// verify customer price creation with API calls.
		assertTrue("Customer should be subscribed to plan.", api.isCustomerSubscribed(newPlanId, user.getUserId()));

		PlanItemWS price = api.getCustomerPrice(user.getUserId(), planAffectedItemId);
		assertEquals("Affected item should be discounted.", new BigDecimal("1"), price.getModel().getRateAsDecimal());

		// delete order that subscribes the user to the plan
		api.deleteOrder(order.getId());

		// verify customer price removal with API calls.
		assertFalse("Customer should no longer subscribed to plan.", api.isCustomerSubscribed(newPlanId, user.getUserId()));

		price = api.getCustomerPrice(user.getUserId(), planAffectedItemId);
		assertNull("Customer no longer subscribed to plan.", price);

		// cleanup
		api.deleteUser(user.getUserId());
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests subscription to a plan by updating an order.
	 */
	@Test
	public void test003UpdateOrderSubscribe() {
		Integer categoryId = createItemCategory(api);
		//Creating Plan
		String newPlanDes = "Test-plan-" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		Integer newPlanId = api.createPlan(planWS);
		Integer nonPlanItem = api.createItem(createItem(PP_ENTITY_ID, categoryId));

		UserWS user = PricingTestHelper.buildUser("plan-test-02-", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());

		// create order with non plan item
		OrderWS order = PricingTestHelper.buildOrder(user.getUserId(), nonPlanItem, PP_MONTHLY_PERIOD);
		OrderLineWS line = order.getOrderLines()[0];
		order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, PP_ORDER_CHANGE_APPLY_STATUS)));
		order = api.getOrder(order.getId());
		assertNotNull("order created", order.getId());

		// verify customer prices still empty with API calls.
		assertFalse("Customer should not subscribed to plan.", api.isCustomerSubscribed(newPlanId, user.getUserId()));

		PlanItemWS price = api.getCustomerPrice(user.getUserId(), planAffectedItemId);
		assertNull("Order does not subscribe the customer to a plan. No price change.", price);

		// subscribe to plan item
		OrderLineWS planLine = new OrderLineWS();
		planLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		planLine.setItemId(planItemId);
		planLine.setUseItem(true);
		planLine.setQuantity(1);
		order.setOrderLines(new OrderLineWS[]{line, planLine});
		OrderChangeWS orderChange = OrderChangeBL.buildFromLine(planLine, order, PP_ORDER_CHANGE_APPLY_STATUS);

		api.createUpdateOrder(order, new OrderChangeWS[]{orderChange}); // update order

		// verify price creation with API calls.
		assertTrue("Customer should be subscribed to plan.", api.isCustomerSubscribed(newPlanId, user.getUserId()));

		price = api.getCustomerPrice(user.getUserId(), planAffectedItemId);
		assertEquals("Affected item should be discounted.", new BigDecimal("1"), price.getModel().getRateAsDecimal());

		// cleanup
		api.deleteOrder(order.getId());
		api.deleteUser(user.getUserId());
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItem(nonPlanItem);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests un-subscription from a plan by updating an order.
	 */
	@Test
	public void test004UpdateOrderUnSubscribe() {
		Integer categoryId = createItemCategory(api);
		//Creating Plan
		String newPlanDes = "Test-plan-" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		planWS.getPlanItems().get(0).getBundle().setQuantity("0");
		Integer newPlanId = api.createPlan(planWS);

		// Item not associated with plan
		Integer nonPlanItem = api.createItem(createItem(PP_ENTITY_ID, categoryId));

		UserWS user = PricingTestHelper.buildUser("plan-test-03-", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());

		// create order
		OrderWS order = PricingTestHelper.buildOrder(user.getUserId(), planItemId, PP_MONTHLY_PERIOD);
		order.setActiveSince(DateUtils.addDays(new Date(), -1));
		order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, PP_ORDER_CHANGE_APPLY_STATUS)));
		order = api.getOrder(order.getId());
		assertNotNull("order created", order.getId());


		// verify price creation with API calls.
		assertTrue("Customer should be subscribed to plan.", api.isCustomerSubscribed(newPlanId, user.getUserId()));

		PlanItemWS price = api.getCustomerPrice(user.getUserId(), planAffectedItemId);
		assertEquals("Affected item should be discounted.", new BigDecimal("1"), price.getModel().getRateAsDecimal());

		// remove plan item
		OrderLineWS oldLine = order.getOrderLines()[0];
		oldLine.setDeleted(1);
		OrderChangeWS deleteChange = OrderChangeBL.buildFromLine(oldLine, null, PP_ORDER_CHANGE_APPLY_STATUS);
		deleteChange.setRemoval(Integer.valueOf(1));
		deleteChange.setQuantity("-1");

		// replace with non-plan junk item
		OrderLineWS line = new OrderLineWS();
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setItemId(nonPlanItem);
		line.setUseItem(true);
		line.setQuantity(1);
		OrderChangeWS newLineChange = OrderChangeBL.buildFromLine(line, order, PP_ORDER_CHANGE_APPLY_STATUS);

		order.setOrderLines(new OrderLineWS[]{oldLine, line});
		api.createUpdateOrder(order, new OrderChangeWS[]{deleteChange, newLineChange}); // update order

		// verify price removed
		assertFalse("Customer no longer subscribed to plan.", api.isCustomerSubscribed(newPlanId, user.getUserId()));

		/*  TODO The following test has to change. getCustomerPriceForDate should be done for today.
            Since the plan was active only up to yesterday, assetNull on price will be true.
            This is because customer specific price history will now be maintained for late guided usage.
            price for pricingDate needs to be fetched. Default to new Date();
        */
		price = api.getCustomerPriceForDate(user.getUserId(), planAffectedItemId, new Date(), null);
		assertNull("Order does not subscribe the customer to a plan. No price change.", price);

		// cleanup
		api.deleteOrder(order.getId());
		api.deleteUser(user.getUserId());
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItem(nonPlanItem);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that the plan can be queried by the subscription item.
	 */
	@Test
	public void test005GetPlanBySubscriptionItem() {
		Integer categoryId = createItemCategory(api);
		String newPlanDes = "Test-plan-" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));

		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		Integer newPlanId = api.createPlan(planWS);

		Integer[] planIds = api.getPlansBySubscriptionItem(planItemId);
		assertEquals("Should only be 1 plan.", 1, planIds.length);
		assertEquals("Should be 'crazy brian's discount plan'", newPlanId, planIds[0]);

		//cleanup
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that the plan can be queried by the item's it affects.
	 */
	@Test
	public void test006GetPlansByAffectedItem() {
		Integer categoryId = createItemCategory(api);

		String newPlanDes = "Test-plan" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));

		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		Integer newPlanId = api.createPlan(planWS);

		Integer[] planIds = api.getPlansByAffectedItem(planAffectedItemId);
		assertEquals("Should only be 1 plans.", 1, planIds.length);
		assertEquals("Should be 'crazy brian's discount plan'", newPlanId, planIds[0]);

		//cleanup
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests plan CRUD API calls.
	 */
	@Test
	public void test007CreateUpdateDeletePlan() {
		Integer categoryId = createItemCategory(api);

		// subscription item for plan
		ItemDTOEx item = new ItemDTOEx();
		item.setDescription("Test Long Distance Plan");
		item.setNumber("TEST-LD-PLAN-01");
		item.setPrice("10.00");
		item.setTypes(new Integer[]{categoryId});

		item.setId(api.createItem(item));
		Integer longDistanceCall = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer longDistanceCallGeneric = api.createItem(createItem(PP_ENTITY_ID, categoryId));

		// create plan
		PlanItemWS callPrice = new PlanItemWS();
		callPrice.setItemId(longDistanceCall);
		callPrice.getModels().put(CommonConstants.EPOCH_DATE,
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.10"), CURRENCY_ID));

		PlanWS plan = new PlanWS();
		plan.setItemId(item.getId());
		plan.setDescription("Discount long distance calls.");
		plan.setPeriodId(PP_MONTHLY_PERIOD);
		plan.addPlanItem(callPrice);

		plan.setId(api.createPlan(plan));

		// verify creation
		PlanWS fetchedPlan = api.getPlanWS(plan.getId());
		assertEquals(item.getId(), fetchedPlan.getItemId());
		assertEquals("Discount long distance calls.", fetchedPlan.getDescription());

		PlanItemWS fetchedPrice = fetchedPlan.getPlanItems().get(0);
		assertEquals(longDistanceCall, fetchedPrice.getItemId());
		assertEquals(PriceModelStrategy.FLAT.name(), fetchedPrice.getModel().getType());
		assertEquals(new BigDecimal("0.10"), fetchedPrice.getModel().getRateAsDecimal());
		assertEquals(1, fetchedPrice.getModel().getCurrencyId().intValue());


		// update the plan
		// update the description and add a price for the generic LD item
		PlanItemWS genericPrice = new PlanItemWS();
		genericPrice.setItemId(longDistanceCallGeneric);
		genericPrice.getModels().put(CommonConstants.EPOCH_DATE,
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.25"), CURRENCY_ID));

		fetchedPlan.setDescription("Updated description.");
		fetchedPlan.addPlanItem(genericPrice);

		api.updatePlan(fetchedPlan);

		// verify update
		fetchedPlan = api.getPlanWS(fetchedPlan.getId());
		assertEquals(item.getId(), fetchedPlan.getItemId());
		assertEquals("Updated description.", fetchedPlan.getDescription());

		PlanItemWS distanceCallItem = null;
		PlanItemWS distanceCallGenericItem = null;

		for (PlanItemWS planItem : fetchedPlan.getPlanItems()) {
			if (planItem.getItemId().equals(longDistanceCall)) {
				distanceCallItem = planItem;
				continue;
			}
			if (planItem.getItemId().equals(longDistanceCallGeneric)) {
				distanceCallGenericItem = planItem;
				continue;
			}
		}

		assertNotNull("Distance Call Item is not presented in plan", distanceCallItem);
		assertNotNull("Distance Call Generic Item is not presented in plan", distanceCallGenericItem);

		// validate long distance call item  again
		assertEquals(longDistanceCall, distanceCallItem.getItemId());
		assertEquals(PriceModelStrategy.FLAT.name(), distanceCallItem.getModel().getType());
		assertEquals(new BigDecimal("0.10"), distanceCallItem.getModel().getRateAsDecimal());
		assertEquals(1, distanceCallItem.getModel().getCurrencyId().intValue());

		// validate long distance call generic item
		assertEquals(longDistanceCallGeneric, distanceCallGenericItem.getItemId());
		assertEquals(PriceModelStrategy.FLAT.name(), distanceCallGenericItem.getModel().getType());
		assertEquals(new BigDecimal("0.25"), distanceCallGenericItem.getModel().getRateAsDecimal());
		assertEquals(1, distanceCallGenericItem.getModel().getCurrencyId().intValue());


		// delete the plan
		api.deletePlan(fetchedPlan.getId());
		api.deleteItem(item.getId());

		PlanWS deletedPlan = api.getPlanWS(fetchedPlan.getId());
		if(deletedPlan!=null){
			fail("plan has deleted, object should be null.");
		}

		//cleanup
		api.deleteItem(longDistanceCall);
		api.deleteItem(longDistanceCallGeneric);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that the customer is un-subscribed when the plan is deleted.
	 */
	@Test
	public void test008UnsubscribePlanDelete() {
		Integer categoryId = createItemCategory(api);
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));

		// subscription item for plan
		ItemDTOEx item = new ItemDTOEx();
		item.setDescription("Test Long Distance Plan");
		item.setNumber("TEST-LD-PLAN-02");
		item.setPrice("10.00");
		item.setTypes(new Integer[]{categoryId});

		item.setId(api.createItem(item));


		// create plan
		PlanItemWS callPrice = new PlanItemWS();
		callPrice.setItemId(planAffectedItemId);
		callPrice.getModels().put(CommonConstants.EPOCH_DATE,
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.10"), CURRENCY_ID));

		PlanWS plan = new PlanWS();
		plan.setItemId(item.getId());
		plan.setDescription("Discount long distance calls.");
		plan.setPeriodId(PP_MONTHLY_PERIOD);
		plan.addPlanItem(callPrice);

		plan.setId(api.createPlan(plan));
		assertNotNull("plan created", plan.getId());

		UserWS user = PricingTestHelper.buildUser("plan-test-04-", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());

		OrderWS order = PricingTestHelper.buildOrder(user.getUserId(), item.getId(), PP_MONTHLY_PERIOD);
		order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, PP_ORDER_CHANGE_APPLY_STATUS)));
		order = api.getOrder(order.getId());
		assertNotNull("order created", order.getId());


		// verify that customer is subscribed
		assertTrue("Customer should be subscribed to plan.", api.isCustomerSubscribed(plan.getId(), user.getUserId()));

		// delete plan
		api.deletePlan(plan.getId());

		// verify that customer is no longer subscribed
		assertFalse("Customer should no longer be subscribed to plan.", api.isCustomerSubscribed(plan.getId(), user.getUserId()));

		// cleanup
		api.deleteOrder(order.getId());
		api.deleteUser(user.getUserId());
		api.deleteItem(item.getId());
		api.deleteItem(planAffectedItemId);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that the price is affected by the subscription to a plan
	 */
	@Test
	public void test009RateOrder() {
		Integer categoryId = createItemCategory(api);

		String newPlanDes = "Test-plan-" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		Integer newPlanId = api.createPlan(planWS);

		UserWS user = PricingTestHelper.buildUser("plan-test-05-", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());

		OrderWS order = PricingTestHelper.buildOrder(user.getUserId(), planItemId, PP_MONTHLY_PERIOD);
		order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, PP_ORDER_CHANGE_APPLY_STATUS)));
		order = api.getOrder(order.getId());
		assertNotNull("order created", order.getId());


		// verify customer price creation with API calls.
		assertTrue("Customer should be subscribed to plan.", api.isCustomerSubscribed(newPlanId, user.getUserId()));

		PlanItemWS price = api.getCustomerPrice(user.getUserId(), planAffectedItemId);
		assertEquals("Affected item should be discounted.", new BigDecimal("1"), price.getModel().getRateAsDecimal());


		// test order using the affected plan item
		OrderWS testOrder = new OrderWS();
		testOrder.setUserId(user.getUserId());
		testOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		testOrder.setPeriod(PP_MONTHLY_PERIOD);
		testOrder.setCurrencyId(CURRENCY_ID);
		testOrder.setActiveSince(new Date());

		OrderLineWS testLine = new OrderLineWS();
		testLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		testLine.setItemId(planAffectedItemId);
		testLine.setUseItem(true);
		testLine.setQuantity(1);
		testOrder.setOrderLines(new OrderLineWS[]{testLine});


		// rate order and verify price
		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, PP_ORDER_CHANGE_APPLY_STATUS));
		assertEquals("Order line should be priced at $1.", new BigDecimal("1"), testOrder.getOrderLines()[0].getPriceAsDecimal());

		// cleanup
		api.deleteOrder(order.getId());
		api.deleteUser(user.getUserId());
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that subscribing to a plan with bundled quantity also adds the bundled items
	 * to the subscription order. Bundled items should be added at the plan price.
	 */
	@Test
	public void test010BundledQuantity() {
		Integer categoryId = createItemCategory(api);

		// subscription item for plan
		ItemDTOEx item = new ItemDTOEx();
		item.setDescription("Test Long Distance Plan");
		item.setNumber("TEST-LD-PLAN-03");
		item.setPrice("10.00");
		item.setTypes(new Integer[]{categoryId});

		item.setId(api.createItem(item));

		UserWS user = PricingTestHelper.buildUser("plan-test-10-", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());


		// create plan
		// includes 10 bundled "long distance call" items
		Integer longDistanceCallTest = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		PlanItemWS callPrice = new PlanItemWS();
		callPrice.setItemId(longDistanceCallTest);
		callPrice.getModels().put(CommonConstants.EPOCH_DATE,
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.10"), CURRENCY_ID));

		PlanItemBundleWS bundle = new PlanItemBundleWS();
		bundle.setPeriodId(Constants.ORDER_PERIOD_ONCE);
		bundle.setTargetCustomer(PlanItemBundleWS.TARGET_SELF);
		bundle.setQuantity(new BigDecimal("10"));

		callPrice.setBundle(bundle);


		PlanWS plan = new PlanWS();
		plan.setItemId(item.getId());
		plan.setDescription("Discount long distance calls.");
		plan.setPeriodId(PP_MONTHLY_PERIOD);
		plan.addPlanItem(callPrice);

		plan.setId(api.createPlan(plan));


		// subscribe to the created plan
		OrderWS order = PricingTestHelper.buildOrder(user.getUserId(), item.getId(), PP_MONTHLY_PERIOD);
		Integer orderIdCreated = api.createOrder(order, OrderChangeBL.buildFromOrder(order, PP_ORDER_CHANGE_APPLY_STATUS));
		order = api.getOrder(orderIdCreated);
		assertNotNull("order created", order.getId());


		// verify that a new one-time order was created using the original order as a template
		Integer[] orderIds = api.getLastOrders(user.getUserId(), 2);
		assertEquals("extra order created", 2, orderIds.length);

		OrderWS bundledOrder = api.getOrder(orderIds[1]);
        if (!bundledOrder.getPeriod().equals(1))
            bundledOrder = api.getOrder(orderIds[0]);
		assertNotNull("bundled order created", bundledOrder);

		assertEquals(Constants.ORDER_PERIOD_ONCE, bundledOrder.getPeriod());
		assertEquals(order.getCurrencyId(), bundledOrder.getCurrencyId());
		assertEquals(order.getActiveSince(), bundledOrder.getActiveSince());
		assertEquals(order.getActiveUntil(), bundledOrder.getActiveUntil());

		// verify bundled item quantity added to a new one-time order
		boolean found = false;
		for (OrderLineWS line : bundledOrder.getOrderLines()) {
			if (line.getItemId().equals(longDistanceCallTest)) {
				found = true;
				assertEquals("includes 10 bundled call items", new BigDecimal("10"), line.getQuantityAsDecimal());
			}
		}
		assertTrue("Found line for bundled quantity", found);


		// cleanup; first delete the leaf order
		api.deleteOrder(bundledOrder.getId());
		api.deleteOrder(order.getId());
		api.deleteUser(user.getUserId());
		api.deletePlan(plan.getId());
		api.deleteItem(item.getId());
		api.deleteItem(longDistanceCallTest);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that sub-accounts flagged with "use parent pricing" can inherit the price from
	 * a parent if they do not have a plan price for the product.
	 */
	@Test
	public void test011SubAccountPricing() {
		Integer categoryId = createItemCategory(api);

		//create plan
		String newPlanDes = "Test-plan" + dateFormat.print(new Date().getTime());
		Integer planItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		Integer planAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, categoryId));
		PlanWS planWS = createPlan(newPlanDes, planItemId, planAffectedItemId, PP_MONTHLY_PERIOD);
		Integer newPlanId = api.createPlan(planWS);

		UserWS parentUser = PricingTestHelper.buildUser("sub-account-test-01-parent", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		parentUser.setIsParent(Boolean.TRUE); // can be a parent
		Integer parentUserId = api.createUser(parentUser); // create user
		parentUser = api.getUserWS(parentUserId);
		assertNotNull("parent customer created", parentUser.getUserId());

		// create child user
		UserWS childUser = PricingTestHelper.buildUser("sub-account-test-01-child", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		childUser.setParentId(parentUser.getId()); // sub-account of parent user created above
		childUser.setUseParentPricing(true);           // inherit pricing from parent
		Integer childUserId = api.createUser(childUser); // create user
		childUser = api.getUserWS(childUserId);
		assertNotNull("child customer created", childUser.getUserId());
		assertTrue("child customer should use parent pricing", childUser.useParentPricing());

		// subscribe the parent account to the plan
		OrderWS parentOrder = PricingTestHelper.buildOrder(parentUser.getId(), planItemId, PP_MONTHLY_PERIOD);
		parentOrder.setId(api.createOrder(parentOrder, OrderChangeBL.buildFromOrder(parentOrder, PP_ORDER_CHANGE_APPLY_STATUS)));
		parentOrder = api.getOrder(parentOrder.getId());
		assertNotNull("order created", parentOrder.getId());


		// verify parent account is subscribed to plan
		assertTrue("Parent account should be subscribed to plan.", api.isCustomerSubscribed(newPlanId, parentUser.getUserId()));

		// check that the parent subscribed plan price applies to the sub-account
		OrderWS childOrder = PricingTestHelper.buildOrder(childUser.getUserId(), planAffectedItemId, PP_MONTHLY_PERIOD);
		childOrder = api.rateOrder(childOrder, OrderChangeBL.buildFromOrder(childOrder, PP_ORDER_CHANGE_APPLY_STATUS));

		// verify that price matches the parent accounts plan price
		assertEquals("Discounted price add plan", new BigDecimal("1"), childOrder.getOrderLines()[0].getPriceAsDecimal());


		// cleanup
		api.deleteOrder(parentOrder.getId());
		api.deleteUser(childUser.getUserId());
		api.deleteUser(parentUser.getUserId());
		api.deletePlan(newPlanId);
		api.deleteItem(planItemId);
		api.deleteItem(planAffectedItemId);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that a plan can have prices with different start dates, and that a purchase after
	 * that start date will use the appropriate price from the plan.
	 */
	@Test
	public void test012PricingTimeLine() {
		Integer categoryId = createItemCategory(api);

		// subscription item for plan
		ItemDTOEx item = new ItemDTOEx();
		item.setActiveSince(Constants.EPOCH_DATE);
		item.setDescription("Test Long Distance Plan");
		item.setNumber("TEST-LD-PLAN-03");
		item.setPrice("10.00");
		item.setTypes(new Integer[]{categoryId});
		item.setId(api.createItem(item));

		ItemDTOEx longDistanceItem = createItem(PP_ENTITY_ID, categoryId);
		longDistanceItem.setActiveSince(Constants.EPOCH_DATE);
		Integer longDistanceCallTest = api.createItem(longDistanceItem);
		// starting price
		PlanItemWS planItem = new PlanItemWS();
		planItem.setItemId(longDistanceCallTest);

		// price starting at epoch date - used as default when no other prices set in timeline
		planItem.addModel(CommonConstants.EPOCH_DATE,
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.10"), CURRENCY_ID));

		// price for june
		planItem.addModel(new DateMidnight(2011, 6, 1).toDate(),
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.20"), CURRENCY_ID));

		// price for august
		planItem.addModel(new DateMidnight(2011, 8, 1).toDate(),
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.30"), CURRENCY_ID));


		// create plan
		PlanWS plan = new PlanWS();
		plan.setItemId(item.getId());
		plan.setDescription("Discount long distance calls.");
		plan.setPeriodId(PP_MONTHLY_PERIOD);
		plan.addPlanItem(planItem);

		plan.setId(api.createPlan(plan)); // create plan
		assertNotNull("plan created", plan.getId());

		UserWS user = PricingTestHelper.buildUser("plan-test-12-", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
		user.setUserId(api.createUser(user)); // create user
		assertNotNull("customer created", user.getUserId());


		// subscribe to plan
		OrderWS order = PricingTestHelper.buildOrder(user.getUserId(), plan.getItemId(), PP_MONTHLY_PERIOD);
		order.setActiveSince(CommonConstants.EPOCH_DATE);
		//subscription's got to be active since this date, i.e. epoch date
		//because subsequent tests asset on price almost as old as this date
		order.setId(api.createOrder(order, OrderChangeBL.buildFromOrder(order, PP_ORDER_CHANGE_APPLY_STATUS)));
		assertNotNull("order created", order.getId());


		// verify customer price creation with API calls.
		assertTrue("Customer should be subscribed to plan.", api.isCustomerSubscribed(plan.getId(), user.getUserId()));

		PlanItemWS price = api.getCustomerPrice(user.getUserId(), longDistanceCallTest);
		assertEquals("Price for item should have 3 different dates", 3, price.getModels().size());


		// test order using the affected plan item
		OrderWS testOrder = PricingTestHelper.buildOrder(user.getUserId(), longDistanceCallTest, PP_MONTHLY_PERIOD);
		OrderLineWS testLine = testOrder.getOrderLines()[0];

		// rate order and verify starting price
		testOrder.setActiveSince(new DateMidnight(1971, 1, 1).toDate()); // after the epoch date
		testOrder.setOrderLines(new OrderLineWS[]{testLine});         // should give us the starting price

		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, PP_ORDER_CHANGE_APPLY_STATUS));
		assertEquals("Order line should use starting price at $0.10.", new BigDecimal("0.10"), testOrder.getOrderLines()[0].getPriceAsDecimal());

		// update order to june, rate order and verify new price
		testOrder.setActiveSince(new DateMidnight(2011, 6, 1).toDate()); // 1st day of june, same as june price start date
		testOrder.setOrderLines(new OrderLineWS[]{testLine});         // should give us the june price

		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, PP_ORDER_CHANGE_APPLY_STATUS));
		assertEquals("Order line should use june price at $0.20.", new BigDecimal("0.20"), testOrder.getOrderLines()[0].getPriceAsDecimal());

		// update order to august, rate order and verify new price
		testOrder.setActiveSince(new DateMidnight(2011, 8, 2).toDate()); // 2nd day of august, after august price start date
		testOrder.setOrderLines(new OrderLineWS[]{testLine});         // should give us the august price

		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, PP_ORDER_CHANGE_APPLY_STATUS));
		assertEquals("Order line should use august price at $0.30.", new BigDecimal("0.30"), testOrder.getOrderLines()[0].getPriceAsDecimal());

		// cleanup
		api.deleteOrder(order.getId());
		api.deleteUser(user.getUserId());
		api.deletePlan(plan.getId());
		api.deleteItem(item.getId());
		api.deleteItem(longDistanceCallTest);
		api.deleteItemCategory(categoryId);
	}

	/**
	 * Tests that plans and prices can only be accessed by the entity that owns the subscription/affected
	 * plan item ids.
	 */
	@Test
	public void test013WSSecurity() {
		//Creating plan for Pony
		Date date = new Date();
		String planDes = "Test-plan-1-" + dateFormat.print(date.getTime());
		Integer ponyCategoryId = createItemCategory(api);
		Integer ponyPlanItemId = api.createItem(createItem(PP_ENTITY_ID, ponyCategoryId));
		Integer ponyPlanAffectedItemId = api.createItem(createItem(PP_ENTITY_ID, ponyCategoryId));
		PlanWS ponyPlan = createPlan(planDes, ponyPlanItemId, ponyPlanAffectedItemId, PP_MONTHLY_PERIOD);
		Integer ponyPlanId = api.createPlan(ponyPlan);

		//creating plan for Mordor
		String newPlanDes = "Test-plan-2-" + dateFormat.print(date.getTime());
		Integer mordorCategoryId = createItemCategory(apiMordor);
		Integer mordorPlanItemId = apiMordor.createItem(createItem(MORDOR_ENTITY_ID, mordorCategoryId));
		Integer mordorPlanAffectedItemId = apiMordor.createItem(createItem(MORDOR_ENTITY_ID, mordorCategoryId));

		PlanWS mordorPlan = createPlan(newPlanDes, mordorPlanItemId, mordorPlanAffectedItemId, MORDOR_MONTHLY_PERIOD);
		Integer mordorPlanId = apiMordor.createPlan(mordorPlan); // Creating Plan
		Integer mordorItemId = apiMordor.createItem(createItem(MORDOR_ENTITY_ID, mordorCategoryId)); //creating item
		Integer mordorUserId = apiMordor.createUser(PricingTestHelper.buildUser("securityUser", MORDOR_MONTHLY_PERIOD, MORDOR_ACCOUNT_TYPE));

		// getPlanWS
		try {
			api.getPlanWS(mordorPlanId);
			fail("Should not be able to get plan from another entity");
		} catch (SecurityException | SessionInternalError e) {
		}

		// createPlan
		try {
			PlanWS createPlan = new PlanWS();
			createPlan.setItemId(mordorItemId);
			createPlan.setDescription("Create plan with a bad item.");
			api.createPlan(createPlan);
			fail("Should not be able to create plans using items from another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// updatePlan
		try {
			PlanWS updatePlan = api.getPlanWS(ponyPlanId);
			updatePlan.setItemId(mordorItemId);
			api.updatePlan(updatePlan);
			fail("Should not be able to update plan using items from another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// deletePlan
		try {
			api.deletePlan(mordorPlanId);
			fail("Should not be able to delete a plan using an item from another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// addPlanPrice
		PlanItemWS addPlanPrice = new PlanItemWS();
		addPlanPrice.setItemId(mordorItemId);
		addPlanPrice.getModels().put(CommonConstants.EPOCH_DATE,
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("1.00"), CURRENCY_ID));

		try {
			// cannot add to a plan we don't own
			api.addPlanPrice(mordorPlanId, addPlanPrice);
			fail("Should not be able to delete");
		} catch (SecurityException | SessionInternalError e) {
		}

		try {
			// cannot add a price for an item we don't own
			api.addPlanPrice(ponyPlanId, addPlanPrice);
			fail("Should not be able to add price for item from another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// isCustomerSubscribed
		try {
			api.isCustomerSubscribed(mordorPlanId, mordorUserId);
			fail("Should not be able to check subscription status for a plan from another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// getSubscribedCustomers
		try {
			api.getSubscribedCustomers(mordorPlanId);
			fail("Should not be able to get subscribed customers for a plan from another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// getPlansbySubscriptionItem
		try {
			api.getPlansBySubscriptionItem(mordorItemId);
			fail("Should not be able to get plans using for item belonging to another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// getPlansByAffectedItem
		try {
			api.getPlansByAffectedItem(mordorItemId);
			fail("Should not be able to get plans using for item belonging to another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		// getCustomerPrice
		try {
			api.getCustomerPrice(mordorUserId, ponyPlanAffectedItemId);
			fail("Should not be able to get price for a user belonging to another entity.");
		} catch (SecurityException | SessionInternalError e) {
		}

		//cleanup mordor
		try {
			apiMordor.deleteUser(mordorUserId);
		} catch (SessionInternalError e) {
			assertTrue(e.getMessage().contains("Notification not found for sending deleted user notification"));
		}
		
		apiMordor.deleteItem(mordorItemId);
		apiMordor.deleteItem(mordorPlanItemId);
		apiMordor.deleteItem(mordorPlanAffectedItemId);
		apiMordor.deleteItemCategory(mordorCategoryId);

		//cleanup pony
		api.deleteItem(ponyPlanItemId);
		api.deleteItem(ponyPlanAffectedItemId);
		api.deletePlan(ponyPlanId);
		api.deleteItemCategory(ponyCategoryId);
	}

	private PlanWS createPlan(String itemDescription, Integer itemId, Integer planItemId, Integer periodId) {

		PlanWS testPlan = new PlanWS();
		testPlan.setDescription(itemDescription);
		testPlan.setPeriodId(periodId);
		testPlan.setItemId(itemId);

		PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, CURRENCY_ID);
		SortedMap<Date, PriceModelWS> models = new TreeMap<Date, PriceModelWS>();
		models.put(Constants.EPOCH_DATE, priceModel);

		PlanItemBundleWS bundle1 = new PlanItemBundleWS();
		bundle1.setPeriodId(periodId);
		bundle1.setQuantity(BigDecimal.ONE);

		PlanItemWS pi1 = new PlanItemWS();
		pi1.setPrecedence(-1);
		pi1.setItemId(planItemId);
		pi1.setModels(models);
		pi1.setBundle(bundle1);
		testPlan.addPlanItem(pi1);

		return testPlan;
	}

	private ItemDTOEx createItem(Integer entityId, Integer type) {
		ItemDTOEx testItem = new ItemDTOEx();
		testItem.setDescription("item" + Short.toString((short) System.currentTimeMillis()));
		testItem.setEntityId(entityId);
		testItem.setTypes(new Integer[]{type});
		testItem.setPrice("1");
		testItem.setNumber("Number" + Short.toString((short) System.currentTimeMillis()));
		testItem.setActiveSince(Util.getDate(2010, 1, 1));
		return testItem;
	}

	private Integer createItemCategory(JbillingAPI api) {
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("category" + Short.toString((short) System.currentTimeMillis()));
		itemType.setOrderLineTypeId(1);
		return api.createItemCategory(itemType);
	}

}
