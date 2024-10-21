package com.sapienter.jbilling.server.discounts;

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateMidnight;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.FullCreativeCustomInvoiceFieldsTokenTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.CreateObjectUtil;


@Test(groups = { "billing-and-discounts", "discounts" }, testName = "PercentageDiscountOrderTest")
public class PercentageDiscountOrderTest extends BaseDiscountApiTest {
	private Map<String, String> parameters = new HashMap<>();
	private Integer processingOrder = 777777;

	@Override
	protected void beforeTestClass() throws Exception {
		super.beforeTestClass();
		JbillingAPI api = JbillingAPIFactory.getAPI();
		setITGInvoiceNotification(api, "0");
		deleteFullCreativeCustomInvoiceFieldsTokenPlugin(api);
	}

	@Override
	protected void afterTestClass() throws Exception {
		super.afterTestClass();
		JbillingAPI api = JbillingAPIFactory.getAPI();
		setITGInvoiceNotification(api, "1");
		configureFullCreativeCustomInvoiceFieldsTokenPlugin(api);
	}

	private void deleteFullCreativeCustomInvoiceFieldsTokenPlugin(JbillingAPI api) {
		PluggableTaskWS[] pluginsWS = api.getPluginsWS(1, FullCreativeCustomInvoiceFieldsTokenTask.class.getName());
		if(ArrayUtils.isNotEmpty(pluginsWS) && pluginsWS.length >= 1) {
			PluggableTaskWS fullCreativeCustomInvoiceFieldsTokenTask = pluginsWS[0];
			parameters = fullCreativeCustomInvoiceFieldsTokenTask.getParameters();
			processingOrder = fullCreativeCustomInvoiceFieldsTokenTask.getProcessingOrder();
			api.deletePlugin(fullCreativeCustomInvoiceFieldsTokenTask.getId());
		}
	}

	private void setITGInvoiceNotification(JbillingAPI api, String value) {
		PreferenceWS preference = api.getPreference(Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION);
		preference.setValue(value);
		api.updatePreference(preference);
	}

	private void configureFullCreativeCustomInvoiceFieldsTokenPlugin(JbillingAPI api) {
		PluggableTaskWS[] pluginsWS = api.getPluginsWS(1, FullCreativeCustomInvoiceFieldsTokenTask.class.getName());
		if(ArrayUtils.isEmpty(pluginsWS)) {
			PluggableTaskWS fullCreativeCustomInvoiceFieldsTokenTask = new PluggableTaskWS();
			fullCreativeCustomInvoiceFieldsTokenTask.setProcessingOrder(processingOrder);
			PluggableTaskTypeWS fullCreativeCustomInvoiceFieldsTokenTaskType = api.getPluginTypeWSByClassName(FullCreativeCustomInvoiceFieldsTokenTask.class.getName());
			fullCreativeCustomInvoiceFieldsTokenTask.setTypeId(fullCreativeCustomInvoiceFieldsTokenTaskType.getId());
			Hashtable<String, String> newParams = new Hashtable<>();
			String date = Util.formatDate(new Date(), "yyyyMMdd");
			if(parameters.isEmpty()) {
				newParams.put("new_invoice_cut_over_date", date);
			} else {
				for (Map.Entry<String, String> e : parameters.entrySet()) {
					newParams.put(e.getKey(), StringUtils.isNotBlank(e.getValue()) ? e.getValue() : date);
				}
			}
			fullCreativeCustomInvoiceFieldsTokenTask.setParameters(newParams);
			api.createPlugin(fullCreativeCustomInvoiceFieldsTokenTask);
		}
	}

	@Test(priority=1)
	public void test100PercentageDiscountOrderTotalWithThreeDecimal() {

		//create User
		UserWS user = CreateObjectUtil.createCustomer(
				CURRENCY_USD, "H786-1-54.545",
				"newPa$$word1", LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
				CreateObjectUtil.createCustomerContact("test@gmail.com"));

		Integer customerId = api.createUser(user);
		logger.debug("customerId : {}", customerId);
		assertNotNull("Customer/User ID should not be null", customerId);
		updateCustomerNextInvoiceDate(customerId, api);
		// create item
		Integer itemId = createItem("Item1-54.545 Description", "54.545", "IT-000-54.545", TEST_ITEM_CATEGORY);

		logger.debug("itemId : {}", itemId);
		assertNotNull("Item ID should not be null", itemId);

		// create order object
		OrderWS mainOrder = CreateObjectUtil.createOrderObject(
				customerId, CURRENCY_USD, 2,
				ONE_TIME_ORDER_PERIOD, new Date());

		mainOrder.setUserId(customerId);
		// set discount lines with percentage discount at order level
		DiscountWS discount = createPercentageDiscount(3, null,new BigDecimal(100));
		mainOrder.setDiscountLines(createDiscountLinesOnOrder(mainOrder, discount));
		//mainOrder.setActiveSince(activeSince);
		mainOrder.setOrderLines(new OrderLineWS[0]);
		mainOrder = CreateObjectUtil.addLine(
				mainOrder,
				new Integer(1),
				Constants.ORDER_LINE_TYPE_ITEM,
				itemId,
				new BigDecimal("54.545"),
				"Order Line 1"
				);

		// call api to create order
		Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
		mainOrder = api.getOrder(orderId);
		assertNotNull("mainOrder is null.", orderId);
		// fetch the discount order from linked orders

		OrderWS primaryOrder= api.getOrder(orderId);
		OrderWS[] linkedOrders = primaryOrder.getChildOrders();

		logger.debug("Primary Order ID is {}", primaryOrder.getId());
		logger.debug("No. of linked orders: {}", linkedOrders.length);

		assertNotNull("linkedOrders is null.", linkedOrders);
		assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);

		OrderWS discountOrderWS = null;

		for (OrderWS orderWS : linkedOrders) {
			if (orderWS.getId() != orderId) {
				discountOrderWS = orderWS;
				break;
			}
		}
		// various asserts to test discount order and its order line
		assertNotNull("Discount Order is null", discountOrderWS);
		assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
		assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
		assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT);
		assertNull("Discount Order line item is not null", discountOrderWS.getOrderLines()[0].getItemId());


		BigDecimal expectedDiscountAmount = mainOrder.getTotalAsDecimal().negate();
		//Both lines should match and offset each other exactly.
		assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
		assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());


		Integer[] invoiceIds = api.createInvoiceWithDate(
				customerId,
				new Date(),
				PeriodUnitDTO.DAY,
				Integer.valueOf(1),
				false);

		InvoiceWS invoiceWS = api.getInvoiceWS(invoiceIds[0]);

		assertEquals("Dynamic Balance of user must be zero after 100% discount ", BigDecimal.ZERO,api.getUserWS(customerId).getDynamicBalanceAsDecimal());
		assertEquals("Invoice amount must be zero after 100% discount ", BigDecimal.ZERO,invoiceWS.getTotalAsDecimal());
		assertEquals("Invoice balance must be zero after 100% discount ", BigDecimal.ZERO,invoiceWS.getBalanceAsDecimal());		 
		//cleanup
		api.deleteInvoice(invoiceWS.getId());
		api.deleteOrder(discountOrderWS.getId());
		api.deleteOrder(orderId);
		api.deleteItem(itemId);
		api.deleteUser(customerId);
	}

	 @Test(priority=2)
	 public void test100PercentageDiscountOrderTotalWithTwoDecimal() {

		 //create User
		 UserWS user = CreateObjectUtil.createCustomer(
				 CURRENCY_USD, "H786-2-54.54",
				 "newPa$$word1", LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
				 CreateObjectUtil.createCustomerContact("test@gmail.com"));

		 Integer customerId = api.createUser(user);
		 logger.debug("customerId : {}", customerId);
		 assertNotNull("Customer/User ID should not be null", customerId);
		 updateCustomerNextInvoiceDate(customerId, api);
		 // create item
		 Integer itemId = createItem("Item1-54.54 Description", "54.54", "IT-000-54.54", TEST_ITEM_CATEGORY);

		 logger.debug("itemId : {}", itemId);
		 assertNotNull("Item ID should not be null", itemId);

		 // create order object
		 OrderWS mainOrder = CreateObjectUtil.createOrderObject(
				 customerId, CURRENCY_USD, 2,
				 ONE_TIME_ORDER_PERIOD, new Date());

		 DiscountWS discount  = createPercentageDiscount(23, null,new BigDecimal(100));
		 mainOrder.setUserId(customerId);
		 // set discount lines with percentage discount at order level
		 mainOrder.setDiscountLines(createDiscountLinesOnOrder(mainOrder, discount));
		 mainOrder.setOrderLines(new OrderLineWS[0]);
		 mainOrder = CreateObjectUtil.addLine(
				 mainOrder,
				 new Integer(1),
				 Constants.ORDER_LINE_TYPE_ITEM,
				 itemId,
				 new BigDecimal("54.54"),
				 "Order Line 1"
				 );

		 // call api to create order
		 Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
		 mainOrder = api.getOrder(orderId);
		 assertNotNull("mainOrder is null.", orderId);
		 // fetch the discount order from linked orders

		 OrderWS primaryOrder= api.getOrder(orderId);
		 OrderWS[] linkedOrders = primaryOrder.getChildOrders();

		 logger.debug("Primary Order ID is {}", primaryOrder.getId());
		 logger.debug("No. of linked orders: {}", linkedOrders.length);

		 assertNotNull("linkedOrders is null.", linkedOrders);
		 assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);

		 OrderWS discountOrderWS = null;

		 for (OrderWS orderWS : linkedOrders) {
			 if (orderWS.getId() != orderId) {
				 discountOrderWS = orderWS;
				 break;
			 }
		 }
		 // various asserts to test discount order and its order line
		 assertNotNull("Discount Order is null", discountOrderWS);
		 assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
		 assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
		 assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT);
		 assertNull("Discount Order line item is not null", discountOrderWS.getOrderLines()[0].getItemId());


		 BigDecimal expectedDiscountAmount = mainOrder.getTotalAsDecimal().negate();
		 //Both lines should match and offset each other exactly.
		 assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
		 assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		 Integer[] invoiceIds = api.createInvoiceWithDate(
				 customerId,
				 new Date(),
				 PeriodUnitDTO.DAY,
				 Integer.valueOf(1),
				 false);

		 InvoiceWS invoiceWS = api.getInvoiceWS(invoiceIds[0]);

		 assertEquals("Dynamic Balance of user must be zero after 100% discount ", BigDecimal.ZERO,api.getUserWS(customerId).getDynamicBalanceAsDecimal());
		 assertEquals("Invoice amount must be zero after 100% discount ", BigDecimal.ZERO,invoiceWS.getTotalAsDecimal());
		 assertEquals("Invoice balance must be zero after 100% discount ", BigDecimal.ZERO,invoiceWS.getBalanceAsDecimal());
		 //cleanup
		 api.deleteInvoice(invoiceWS.getId());
		 api.deleteOrder(discountOrderWS.getId());
		 api.deleteOrder(orderId);
		 api.deleteItem(itemId);
		 api.deleteUser(customerId);
	 }
	 
	@Test
	public void testPercentageDiscountOrderTotal() {
		//create User
        UserWS user = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testPercentageDiscountItemOrder.310",
                "newPa$$word1", LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
                CreateObjectUtil.createCustomerContact("test@gmail.com"));

        Integer customerId = api.createUser(user);
        logger.debug("customerId : {}", customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        // create item
        Integer itemId = createItem("Item1.417 Description", "400", "IT-0001.417", TEST_ITEM_CATEGORY);

        logger.debug("itemId : {}", itemId);
        assertNotNull("Item ID should not be null", itemId);

        // create order object
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(
		        customerId, CURRENCY_USD, Constants.ORDER_BILLING_PRE_PAID,
		        MONTHLY_ORDER_PERIOD, new DateMidnight(2013, 01, 21).toDate());

        mainOrder.setUserId(customerId);
        // set discount lines with percentage discount at order level
		DiscountWS discount = createPercentageDiscount(1, null);
        mainOrder.setDiscountLines(createDiscountLinesOnOrder(mainOrder, discount));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
                mainOrder,
                new Integer(1),
                Constants.ORDER_LINE_TYPE_ITEM,
                itemId,
                new BigDecimal("100.00"),
                "Order Line 1"
                );
        // call api to create order
        Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        mainOrder = api.getOrder(orderId);
        assertNotNull("mainOrder is null.", orderId);
        // fetch the discount order from linked orders

        OrderWS primaryOrder= api.getOrder(orderId);
        OrderWS[] linkedOrders = primaryOrder.getChildOrders();

        logger.debug("Primary Order ID is {}", primaryOrder.getId());
        logger.debug("No. of linked orders: {}", linkedOrders.length);

        assertNotNull("linkedOrders is null.", linkedOrders);
        assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);

        OrderWS discountOrderWS = null;
        for (OrderWS orderWS : linkedOrders) {
            if (orderWS.getId() != orderId) {
                discountOrderWS = orderWS;
                break;
            }
        }
        // various asserts to test discount order and its order line
        assertNotNull("Discount Order is null", discountOrderWS);
        assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
        assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
        assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT);
        assertNull("Discount Order line item is not null", discountOrderWS.getOrderLines()[0].getItemId());

        BigDecimal expectedDiscountAmount = TEN.negate().divide(new BigDecimal(100)).multiply(mainOrder.getTotalAsDecimal());
        assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
        assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		//cleanup
		api.deleteOrder(discountOrderWS.getId());
		api.deleteItem(itemId);
		api.deleteUser(customerId);
	}
	
	@Test
	public void testPercentageItemLevelDiscount() throws Exception {
		//create User
        UserWS user = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testPercentageDiscountItem.310", "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
		        CreateObjectUtil.createCustomerContact("test@gmail.com"));

		Integer customerId = api.createUser(user);
        logger.debug("customerId : {}", customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        // create 2 items
        Integer itemId1 = createItem("Item1.518 Description", "500", "IT-0001.518", TEST_ITEM_CATEGORY);
        Integer itemId2 = createItem("Item2.518 Description", "500", "IT-0002.518", TEST_ITEM_CATEGORY);

        logger.debug("itemId1 : {}", itemId1);
        assertNotNull("Item ID 1 should not be null", itemId1);

        logger.debug("itemId2 : {}", itemId2);
        assertNotNull("Item ID 2 should not be null", itemId2);
        // create order object
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(
		        customerId, CURRENCY_USD, Constants.ORDER_BILLING_PRE_PAID,
		        MONTHLY_ORDER_PERIOD, new DateMidnight(2013, 01, 21).toDate());

        mainOrder.setUserId(customerId);
        // set discount lines with percentage discount at item level (only on item 1)
		DiscountWS discount = createPercentageDiscount(10, null);
        mainOrder.setDiscountLines(createDiscountLinesOnOrder(mainOrder, discount, itemId1, "Item Level"));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
                mainOrder,
                new Integer(1),
                Constants.ORDER_LINE_TYPE_ITEM,
                itemId1,
                new BigDecimal("100.00"),
                "Order Line 1"
                );
        mainOrder = CreateObjectUtil.addLine(
                mainOrder,
                new Integer(1),
                Constants.ORDER_LINE_TYPE_ITEM,
                itemId2,
                new BigDecimal("100.00"),
                "Order Line 2"
                );
        // call api to create order
        Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        mainOrder = api.getOrder(orderId);
        assertNotNull("mainOrder is null.", orderId);
        // fetch the discount order from linked orders
        OrderWS primaryOrder= api.getOrder(orderId);
        OrderWS[] linkedOrders = primaryOrder.getChildOrders();

        logger.debug("Primary Order ID is {}", primaryOrder.getId());
        logger.debug("No. of linked orders: {}", linkedOrders.length);

        assertNotNull("linkedOrders is null.", linkedOrders);
        assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);

        OrderWS discountOrderWS = null;
        for (OrderWS orderWS : linkedOrders) {
            if (orderWS.getId() != orderId) {
                discountOrderWS = orderWS;
                break;
            }
        }
        // various asserts to test discount order and its order line
        assertNotNull("Discount Order is null", discountOrderWS);
        assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
        assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
        assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT);
		// if discount is apply on the product then discount order's line contains product
		// GET THE discountable amount as matching order line's amount (match by item id).
		BigDecimal discountableAmount = BigDecimal.ZERO;
        for (OrderLineWS line : mainOrder.getOrderLines()) {
            if (line.getItemId().intValue() == itemId1.intValue()) {
                discountableAmount = line.getAmountAsDecimal();
                break;
            }
        }
        BigDecimal expectedDiscountAmount = TEN.negate().divide(new BigDecimal(100)).multiply(discountableAmount);
        assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
        assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		//cleanup
		api.deleteOrder(discountOrderWS.getId());
//		api.deleteDiscount(discount.getId());
		api.deleteItem(itemId1);
		api.deleteItem(itemId2);
		api.deleteUser(customerId);
	}

	@Test
	public void testPercentagePlanItemLevelDiscount() {
		//create plan with bundled items
		ItemDTOEx subscriptionItem = CreateObjectUtil.createItem(
				TEST_ENTITY_ID, new BigDecimal(100), CURRENCY_USD,
				TEST_ITEM_CATEGORY, "Test Plan Subscription Item");
		Integer subscriptionItemId = api.createItem(subscriptionItem);

		List<Integer> bundleItems = new ArrayList<Integer>();
		ItemDTOEx bundleItem = CreateObjectUtil.createItem(
				TEST_ENTITY_ID, BigDecimal.ONE, CURRENCY_USD,
				TEST_ITEM_CATEGORY, "Test Plan Item");
		bundleItems.add(api.createItem(bundleItem));

		//create quarterly plan
		PlanWS plan = CreateObjectUtil.createPlanWithBundledItems(
				THREE_MONTHLY_ORDER_PERIOD, BigDecimal.ONE, subscriptionItemId, bundleItems );
		plan.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE,
				new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("100.10"), CURRENCY_USD));

		plan.getPlanItems().get(0).getBundle().setPeriodId(THREE_MONTHLY_ORDER_PERIOD);
		Integer planId = api.createPlan(plan);
		assertNotNull("Plan ID should not be null", planId);

		// load back the plan
		PlanWS planWS = api.getPlanWS(planId);

		//create User
		UserWS customer = CreateObjectUtil.createCustomer(
				CURRENCY_USD, "testPercentagePlanItemLevel.310."+random, "newPa$$word1",
				LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
				CreateObjectUtil.createCustomerContact("test@gmail.com"));

		Integer customerId = api.createUser(customer);
		assertNotNull("Customer/User ID should not be null", customerId);

		//create order
		Integer plansItemId = plan.getItemId();
		OrderWS mainOrder = getUserSubscriptionToPlan(new Date(), BigDecimal.TEN, customerId, 1, THREE_MONTHLY_ORDER_PERIOD, plansItemId);

		mainOrder.setUserId(customerId);
		// set discount lines with percentage discount at plan item level
		Integer planItemId = planWS.getPlanItems().get(0).getId();
		Integer planItemsItemId = planWS.getPlanItems().get(0).getItemId();
		DiscountWS discount = createPercentageDiscount(20, null);
		DiscountLineWS[] discountLines = createDiscountLinesOnOrder(mainOrder, discount, planItemId, "Plan Item Level");
		mainOrder.setDiscountLines(discountLines);

		// call api to create order
		Integer orderId = api.createOrder(mainOrder, OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID));
		mainOrder = api.getOrder(orderId);
		assertNotNull("mainOrder is null.", orderId);
		// fetch the discount order from linked orders

		OrderWS primaryOrder = api.getOrder(orderId);
		OrderWS[] linkedOrders = primaryOrder.getChildOrders();

		logger.debug("Primary Order ID is {}", primaryOrder.getId());
		logger.debug("No. of linked orders: {}", linkedOrders.length);

		assertNotNull("linkedOrders is null.", linkedOrders);
		assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);

		OrderWS discountOrderWS = null;
		for (OrderWS orderWS : linkedOrders) {
			if (orderWS.getId() != orderId) {
				discountOrderWS = orderWS;
				break;
			}
		}

		// various asserts to test discount order and its order line
		assertNotNull("Discount Order is null", discountOrderWS);
		assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
		assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
		assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT);
		// if discount is apply on the product then discount order's line contains product
		// GET THE discountable amount as matching order line's amount (match by item id).
		BigDecimal discountableAmount = planWS.getPlanItems().get(0).getModel().getRateAsDecimal().
				multiply(planWS.getPlanItems().get(0).getBundle().getQuantityAsDecimal());

		logger.debug("plan item level discount, discountableAmount : {}", discountableAmount);
		BigDecimal expectedDiscountAmount = TEN.negate().divide(new BigDecimal(100)).multiply(discountableAmount);
		assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
		assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		//cleanup
		api.deleteOrder(discountOrderWS.getId());
//		api.deleteDiscount(discount.getId());
		api.deleteItem(subscriptionItemId);
//		api.deletePlan(planId);
		for(Integer bItem : bundleItems) api.deleteItem(bItem);
		api.deleteUser(customerId);
	}
	
	/**
	 * An Order Active Since in past (1/1/2012), add Percentage Discount (Start Date: 1/1/2011) 
	 * Order creation should be successful (Order backdating), 
	 * Invoices should have correct values for both the main Order and the Discount One-time Order
	 */
	@Test
	public void testBackdatedDiscountOrder() throws Exception {

        Date fixedDate = new GregorianCalendar(2011, 7, 5).getTime();
		// reset the billing configuration to 3 months 2 days back
		today.setTime(fixedDate);
		today.add(Calendar.MONTH, -3);

		//create User
        UserWS customer = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testPercentageDiscountItemOrder.311."+random, "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
		        CreateObjectUtil.createCustomerContact("test@gmail.com"));

        Integer customerId = api.createUser(customer);
        updateCustomerNextInvoiceDate(customerId, api);
        logger.debug("customerId : {}", customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        
        customer = api.getUserWS(customerId);
		customer.setPassword(null);
        // update the user main subscription and next invoice date to match with the date of billing run below
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(today.getTime());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
    	mainSubscription.setPeriodId(MONTHLY_ORDER_PERIOD); //monthly
    	mainSubscription.setNextInvoiceDayOfPeriod(gcal.get(Calendar.DAY_OF_MONTH));
    	customer.setMainSubscription(mainSubscription);
    	customer.setNextInvoiceDate(gcal.getTime());
    	api.updateUser(customer);

    	// update the user once again this time for setting back the invoice date to date of billing run below
    	customer = api.getUserWS(customer.getId());
		customer.setPassword(null);
    	customer.setNextInvoiceDate(gcal.getTime());
    	api.updateUser(customer);
        
        // create item
        Integer itemId = createItem("Item1.418 Description", "401", "IT-0001.418", TEST_ITEM_CATEGORY);
        
        logger.debug("itemId : {}", itemId);
        assertNotNull("Item ID should not be null", itemId);
        
        // create order object
        // setting active since to 1 year before
        Calendar activeSince = Calendar.getInstance();
        today.setTime(fixedDate);
        activeSince.set(Calendar.YEAR, today.get(Calendar.YEAR)-1);
        activeSince.set(Calendar.MONTH, today.get(Calendar.MONTH));	// current month, but 1 year back as year is set to -1 in the above line. 
        activeSince.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month a year before.
        OrderWS mainOrder = CreateObjectUtil.createOrderObject(customerId, CURRENCY_USD,
        		Constants.ORDER_BILLING_PRE_PAID, MONTHLY_ORDER_PERIOD, activeSince.getTime());
        mainOrder.setUserId(customerId);

		//create discount
		Calendar backDatedDiscountStartDate = Calendar.getInstance();
		backDatedDiscountStartDate.set(Calendar.YEAR, today.get(Calendar.YEAR)-2);
		backDatedDiscountStartDate.set(Calendar.MONTH, today.get(Calendar.MONTH));	// current month, but 2 years back as -2 in above line for year.
		backDatedDiscountStartDate.set(Calendar.DAY_OF_MONTH, 1); // lets take first of the month 2 years back
		DiscountWS discount = createPercentageDiscount(2, backDatedDiscountStartDate.getTime());

		// set discount lines with percentage discount at order level
		mainOrder.setDiscountLines(createBackDatedDiscountLinesOnOrder(mainOrder, discount));
        mainOrder.setOrderLines(new OrderLineWS[0]);
        mainOrder = CreateObjectUtil.addLine(
        		mainOrder,
        		Integer.valueOf(1),
        		Constants.ORDER_LINE_TYPE_ITEM,
        		itemId,
        		new BigDecimal("100.00"),
        		"Backdated Order Line 1"
        		);

        OrderChangeWS[] mainOrderChanges = OrderChangeBL.buildFromOrder(mainOrder, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: mainOrderChanges) {
            change.setStartDate(mainOrder.getActiveSince());
        }

        Integer orderId = api.createOrder(mainOrder, mainOrderChanges);
       	mainOrder = api.getOrder(orderId);
        assertNotNull("mainOrder is null.", mainOrder);
        
        // fetch the discount order from linked orders        
        OrderWS primaryOrder= api.getOrder(orderId);
        OrderWS[] linkedOrders = primaryOrder.getChildOrders();
        
        logger.debug("Primary Order ID is {}", primaryOrder.getId());
        logger.debug("No. of linked orders: {}", linkedOrders.length);
        
        assertNotNull("linkedOrders is null.", linkedOrders);
        assertTrue("No. of linkedOrders is not equal to 1", linkedOrders.length == 1);
                
        OrderWS discountOrderWS = null;
        for (OrderWS orderWS : linkedOrders) {
        	if (orderWS.getId() != orderId) {
        		discountOrderWS = orderWS;
        		break;
        	}
        }
        // various asserts to test discount order and its order line
        assertNotNull("Discount Order is null", discountOrderWS);
        assertTrue("Discount Order Period is not One Time.", discountOrderWS.getPeriod().intValue() == 1);
        assertTrue("No. of lines on Discount Order not equal to One", discountOrderWS.getOrderLines().length == 1);
        assertTrue("Discount Order line Type not Discount Line Type.", discountOrderWS.getOrderLines()[0].getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT);
        assertNull("Discount Order line item is not null", discountOrderWS.getOrderLines()[0].getItemId());
        BigDecimal expectedDiscountAmount = TEN.negate().divide(new BigDecimal(100)).multiply(mainOrder.getTotalAsDecimal());
        assertEquals("Discount Order line Amount not equal to Discount Amount.", expectedDiscountAmount, discountOrderWS.getOrderLines()[0].getAmountAsDecimal());
        assertEquals("Discount amount is not equal to Discount Order Total", expectedDiscountAmount, discountOrderWS.getTotalAsDecimal());

		//instead of running the entire billing process we use this
		//API method to only generate invoices for one customer
		Integer[] invoiceIds = api.createInvoiceWithDate(
				customerId,
				new DateMidnight(today.get(Calendar.YEAR), today.get(Calendar.MONTH)+1, today.get(Calendar.DAY_OF_MONTH)).toDate(),
				PeriodUnitDTO.MONTH,
				Integer.valueOf(1),
				false);
		assertTrue("More than one invoice should be generated", invoiceIds.length > 0);

        InvoiceWS[] invoices = api.getAllInvoicesForUser(mainOrder.getUserId());
        InvoiceWS invoice1 = invoices != null && invoices.length > 0 ? invoices[0] : null;
        assertNotNull("Invoice was not generated", invoice1);
        Arrays.sort(invoices, new Comparator<InvoiceWS>() {
            public int compare(InvoiceWS invoice1, InvoiceWS invoice2) {
                return invoice1.getId() - invoice2.getId();
            }
        });
        for (InvoiceWS invoiceWs : invoices) {
        	// make sure the invoice amount matches total amount on main order minus the discount amount
            BigDecimal carriedBalance = invoiceWs.getCarriedBalanceAsDecimal();
            if (carriedBalance !=null && carriedBalance.compareTo(BigDecimal.ZERO) > 0) {
            	BigDecimal invoiceWsTotal = carriedBalance.add(mainOrder.getTotalAsDecimal());
                assertEquals("Actual and Expected Invoice amounts are not equal.", invoiceWsTotal, invoiceWs.getTotalAsDecimal());
            } else {
            	// because amount discount is one-time discount, subtracting it from main order total only first time for the expected invoice amount.
            	BigDecimal invoiceWsTotal = mainOrder.getTotalAsDecimal().add(discountOrderWS.getTotalAsDecimal());
                assertEquals("Actual and Expected Invoice amounts are not equal.", invoiceWsTotal, invoiceWs.getTotalAsDecimal());
                try {
                    assertNotNull("paper invoice pdf should not be null" + api.getPaperInvoicePDF(invoiceWs.getId()));
                } catch (Exception e) {
                    fail("get paper invoice pdf should not throw exception : "+e.getMessage());
                }
            }
        }

		//cleanup
		for (InvoiceWS invoice : invoices) {
			api.deleteInvoice(invoice.getId());
		}
		api.deleteOrder(discountOrderWS.getId());
//		api.deleteDiscount(discount.getId());
		api.deleteItem(itemId);
		api.deleteUser(customerId);
	}
	
	private DiscountLineWS[] createBackDatedDiscountLinesOnOrder(OrderWS order, DiscountWS discount) {
		
		// Percentage Discount applied at Order level
		DiscountLineWS percentageDiscountOrderLevel = new DiscountLineWS();

	    percentageDiscountOrderLevel.setDiscountId(discount.getId());
		percentageDiscountOrderLevel.setOrderId(order.getId());
		percentageDiscountOrderLevel.setDescription(discount.getDescription() + " Discount On Order Level");
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = percentageDiscountOrderLevel;
		
		// return discount lines
		return discountLines;
	}
	

	private DiscountLineWS[] createDiscountLinesOnOrder(OrderWS order, DiscountWS discount) {
		
		// Percentage based Discount applied at Order level
		DiscountLineWS percentageDiscountOrderLevel = new DiscountLineWS();
		percentageDiscountOrderLevel.setDiscountId(discount.getId());
		percentageDiscountOrderLevel.setOrderId(order.getId());
		percentageDiscountOrderLevel.setDescription(discount.getDescription() + " Discount On Order Level");
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = percentageDiscountOrderLevel;		// Percentage Discount applied on Order level
		
		// return discount lines
		return discountLines;
	}
	
	/** Percentage based Discount applied at Item Or Plan Item level.
	 * Pass the discountLevel param as "Item Level" Or "Plan Item Level".	
	 */
	private DiscountLineWS[] createDiscountLinesOnOrder(
			OrderWS order, DiscountWS discount, Integer itemId,
			String discountLevel) {
		
		DiscountLineWS discountLine = new DiscountLineWS();
		discountLine.setDiscountId(discount.getId());
		discountLine.setOrderId(order.getId());
		if ("Item Level".equalsIgnoreCase(discountLevel)) {
			discountLine.setItemId(itemId);
		} else if ("Plan Item Level".equalsIgnoreCase(discountLevel)) {
			discountLine.setPlanItemId(itemId);
		}
		discountLine.setDescription(discount.getDescription() + " Discount " + discountLevel);
		
		DiscountLineWS discountLines[] = new DiscountLineWS[1];
		discountLines[0] = discountLine;
		
		// return discount lines
		return discountLines;
	}
	
	private DiscountWS createPercentageDiscount(Integer callCounter, Date discountStartDate) {
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DSC-PRC-" + random + callCounter);
		discountWs.setDescription("Discount-" + random + callCounter + " %" + TEN);
		discountWs.setRate(TEN);	// 10% Discount Rate
		discountWs.setType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name());
		
		if (discountStartDate != null) {
			discountWs.setStartDate(discountStartDate);
		}
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}

	private OrderWS getUserSubscriptionToPlan(Date since, BigDecimal cost, Integer userId,
	                                          Integer billingType, Integer orderPeriodID, Integer plansItemId) {

        logger.debug("Got plan Item Id as {}", plansItemId);
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingType);
        order.setPeriod(orderPeriodID);
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(since);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setDescription("Order line for plan subscription");
        line.setItemId(plansItemId);
        line.setUseItem(true);

        order.setOrderLines(new OrderLineWS[]{line});

        logger.debug("User subscription...");
        return order;
    }
	
	private DiscountWS createPercentageDiscount(Integer callCounter, Date discountStartDate,BigDecimal rate) {
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DSC-PRC-" + random + callCounter);
		discountWs.setDescription("Discount-" + random + callCounter + " %" + rate);
		discountWs.setRate(rate);    // 100%
		discountWs.setType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name());

		if (discountStartDate != null) {
			discountWs.setStartDate(discountStartDate);
		}

		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
}
