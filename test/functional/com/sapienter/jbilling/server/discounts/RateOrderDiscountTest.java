package com.sapienter.jbilling.server.discounts;

import java.math.BigDecimal;
import java.util.Calendar;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import org.joda.time.DateMidnight;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.JBillingTestUtils;

@Test(groups = { "billing-and-discounts", "discounts" }, testName = "RateOrderDiscountTest")
public class RateOrderDiscountTest extends BaseDiscountApiTest {

    private static final BigDecimal PLAN_QUANTITY = new BigDecimal(2);
    private static final BigDecimal PLAN_BUNDLE_QUANTITY = new BigDecimal(2);
    private static final BigDecimal PLAN_ITEM_RATE = new BigDecimal(100.10).setScale(Constants.BIGDECIMAL_SCALE_STR, Constants.BIGDECIMAL_ROUND);
	private static final BigDecimal DISCOUNT_RATE  = new BigDecimal(5);

	private OrderWS order;
	private PlanWS planWS;
	private UserWS customer;
	private DiscountWS amountBasedDiscount;
	private DiscountWS percentageBasedDiscount;
	
	private OrderWS planQuantityTestOrder;
	private UserWS planQuantityTestCustomer;
	
	@BeforeMethod
    protected void setUp() {
        // create one time plan, the second last parameter says there are 2 bundled items in the plan.
        PlanWS plan= CreateObjectUtil.createPlanBundledItems(
		        TEST_ENTITY_ID, BigDecimal.TEN, CURRENCY_USD,
		        TEST_ITEM_CATEGORY, ONE_TIME_ORDER_PERIOD,
		        PLAN_BUNDLE_QUANTITY, 2, api);
        
        // as we are saying there are 2 bundled items that go with plan, so set the pricing for both.
        plan.getPlanItems().get(0).addModel(CommonConstants.EPOCH_DATE,
        		new PriceModelWS(PriceModelStrategy.FLAT.name(), PLAN_ITEM_RATE, CURRENCY_USD));
        plan.getPlanItems().get(1).addModel(CommonConstants.EPOCH_DATE,
        		new PriceModelWS(PriceModelStrategy.FLAT.name(), PLAN_ITEM_RATE.add(BigDecimal.ONE), CURRENCY_USD));
        
        Integer planId= api.createPlan(plan);
        assertNotNull("Plan ID should not be null", planId);
          
        // load back the plan
        this.planWS = api.getPlanWS(planId);
          
        //create User
        this.customer = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testRateOrderApi-New-"+ System.currentTimeMillis(), "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
          		CreateObjectUtil.createCustomerContact("test@gmail.com"));
        Integer customerId = api.createUser(this.customer);
        this.customer.setUserId(customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        
        // create order and line for plan's item
        this.order = CreateObjectUtil.createOrderObject(this.customer.getUserId(), CURRENCY_USD,
        		Constants.ORDER_BILLING_PRE_PAID, ONE_TIME_ORDER_PERIOD, new DateMidnight(2012, 6, 1).toDate());
        this.order = CreateObjectUtil.addLine(this.order, 1, Constants.ORDER_LINE_TYPE_ITEM, 
        		this.planWS.getItemId(), new BigDecimal("100.00"), "Test 1st Order line");
        assertNotNull("Order should not be null", this.order);
        
        // New test added for testing plan quantity used in rate Order api or not.
        // create Plan Quantity Test Case User
        this.planQuantityTestCustomer = CreateObjectUtil.createCustomer(
		        CURRENCY_USD, "testPlanQuantityRateOrderApi103."+ System.currentTimeMillis(), "newPa$$word1",
		        LANGUAGE_US, CUSTOMER_MAIN_ROLE, false, CUSTOMER_ACTIVE, null,
          		CreateObjectUtil.createCustomerContact("test@gmail.com"));
        customerId = api.createUser(this.planQuantityTestCustomer);
        this.planQuantityTestCustomer.setUserId(customerId);
        assertNotNull("Customer/User ID should not be null", customerId);
        
        // create plan quantity test case order and line for plan's item
        this.planQuantityTestOrder = CreateObjectUtil.createOrderObject(this.planQuantityTestCustomer.getUserId(), CURRENCY_USD,
        								Constants.ORDER_BILLING_PRE_PAID, ONE_TIME_ORDER_PERIOD, new DateMidnight(2012, 6, 1).toDate());
        this.planQuantityTestOrder = CreateObjectUtil.addLine(this.planQuantityTestOrder, PLAN_QUANTITY.intValue(), Constants.ORDER_LINE_TYPE_ITEM, 
        								this.planWS.getItemId(), new BigDecimal("100.00"), "Test 1st Order line");
        assertNotNull("Order should not be null", this.planQuantityTestOrder);
    }

	/**
	 * This is a test case to test list prices on planBundledItems after rating an order.
	 */
	@Test
	public void testRateOrderApiWithPlanBundleItems() {
        this.order = api.rateOrder(this.order, OrderChangeBL.buildFromOrder(this.order, ORDER_CHANGE_STATUS_APPLY_ID));
        
        for (OrderLineWS lineWs : this.order.getPlanBundledItems()) {
        	logger.debug("1.1 List Price: {}", lineWs.getAmount());
        	logger.debug("1.2 Adjusted Price: {}", lineWs.getAdjustedPrice());
        	logger.debug("1.3 Line Price: {}", lineWs.getPrice());
            // adjusted price should be null on planBundledItems as there is no discount.
            assertNull("Adjusted Price should be null as there is no discount.", lineWs.getAdjustedPriceAsDecimal());
        }
        
        // list price test: make sure the rate added on price model above is returned as list price on planBundleItems
        BigDecimal expectedLine1ListPrice = PLAN_ITEM_RATE.multiply(PLAN_BUNDLE_QUANTITY);
        assertEquals(expectedLine1ListPrice.compareTo(this.order.getPlanBundledItems()[0].getAmountAsDecimal()), 0);
        
        BigDecimal expectedLine2ListPrice = PLAN_ITEM_RATE.add(BigDecimal.ONE);
        expectedLine2ListPrice = expectedLine2ListPrice.multiply(PLAN_BUNDLE_QUANTITY);
        assertEquals(expectedLine2ListPrice.compareTo(this.order.getPlanBundledItems()[1].getAmountAsDecimal()), 0);

        // Check that price returned for percentage bundle item = percentage (set as 10% in CreateObjectUtil)
        BigDecimal expectedLine3ListPrice = BigDecimal.TEN;
        assertEquals(expectedLine3ListPrice.compareTo(this.order.getPlanBundledItems()[2].getPriceAsDecimal()), 0);
	}
	
	/**
	 * This test case rates an order that has 2 discounted plan bundle items, then checks 
	 * if the list price and adjusted price for each bundle item is matching the expected
	 * prices post rating.
	 */
	@Test
	public void testRateOrderApiWithDiscountedPlanBundleItems() {
		this.amountBasedDiscount = createAmountBasedDiscount(119);
		this.percentageBasedDiscount = createPercentageBasedDiscount(119);
		
		// Amount based Discount applied at Plan Item level
		DiscountLineWS amountBasedPlanItemLevel = new DiscountLineWS();
  		amountBasedPlanItemLevel.setDiscountId(this.amountBasedDiscount.getId());
  		amountBasedPlanItemLevel.setOrderId(this.order.getId());
  		amountBasedPlanItemLevel.setPlanItemId(this.planWS.getPlanItems().get(0).getId());
  		amountBasedPlanItemLevel.setDescription("Test Amount based Discount applied at Plan Item level");
  		
  		// Percentage based Discount applied at Plan Item level
  		DiscountLineWS percentageBasedPlanItemLevel = new DiscountLineWS();
		percentageBasedPlanItemLevel.setDiscountId(this.percentageBasedDiscount.getId());
		percentageBasedPlanItemLevel.setOrderId(this.order.getId());
		percentageBasedPlanItemLevel.setPlanItemId(this.planWS.getPlanItems().get(1).getId());
		percentageBasedPlanItemLevel.setDescription("Test Percentage based Discount applied at Plan Item level");
        
		// Create discount lines array and attach to the order
  		DiscountLineWS []discountLines = new DiscountLineWS[2];
  		discountLines[0] = amountBasedPlanItemLevel;
  		discountLines[1] = percentageBasedPlanItemLevel;
  		this.order.setDiscountLines(discountLines);

  		// rate the order with discounted plan bundle items
  		this.order = api.rateOrder(this.order, OrderChangeBL.buildFromOrder(this.order, ORDER_CHANGE_STATUS_APPLY_ID));
        
        for (OrderLineWS lineWs : this.order.getPlanBundledItems()) {
			logger.debug("1.1 List Price: {}", lineWs.getAmount());
			logger.debug("1.2 Adjusted Price: {}", lineWs.getAdjustedPrice());
			logger.debug("1.3 Line Price: {}", lineWs.getPrice());
        }
        
        // plan item 1: list price test: make sure the rate added on price model above is returned as list price on planBundleItems
        BigDecimal expectedLine1ListPrice = PLAN_ITEM_RATE.multiply(PLAN_BUNDLE_QUANTITY); 
        assertEquals(expectedLine1ListPrice.compareTo(this.order.getPlanBundledItems()[0].getAmountAsDecimal()), 0);
        
        // plan item 1: adjusted price test: make sure the amount discount is deducted from the list price on planBundleItems
        BigDecimal expectedLine1DiscountAmount = DISCOUNT_RATE.multiply(PLAN_BUNDLE_QUANTITY);
        BigDecimal expectedLine1AdjustedPrice = expectedLine1ListPrice.subtract(expectedLine1DiscountAmount);
        assertEquals(expectedLine1AdjustedPrice.compareTo(this.order.getPlanBundledItems()[0].getAdjustedPriceAsDecimal()), 0);
        
        // plan item 2: list price test: make sure the rate added on price model above is returned as list price on planBundleItems
        BigDecimal expectedLine2ListPrice = PLAN_ITEM_RATE.add(BigDecimal.ONE);
        expectedLine2ListPrice = expectedLine2ListPrice.multiply(PLAN_BUNDLE_QUANTITY);
        assertEquals(expectedLine2ListPrice.compareTo(this.order.getPlanBundledItems()[1].getAmountAsDecimal()), 0);
        
        // plan item 2: adjusted price test: make sure the % discount is deducted from the list price on planBundleItems
        BigDecimal expectedLine2DiscountAmount = expectedLine2ListPrice.multiply(DISCOUNT_RATE).divide(new BigDecimal(100));
        BigDecimal expectedLine2AdjustedPrice = expectedLine2ListPrice.subtract(expectedLine2DiscountAmount);
        expectedLine2AdjustedPrice = expectedLine2AdjustedPrice.setScale(Constants.BIGDECIMAL_SCALE_STR, Constants.BIGDECIMAL_ROUND);
        assertEquals(expectedLine2AdjustedPrice.compareTo(this.order.getPlanBundledItems()[1].getAdjustedPriceAsDecimal()), 0);
        
        // Check that price returned for percentage bundle item = percentage (set as 10% in CreateObjectUtil)
        BigDecimal expectedLine3ListPrice = BigDecimal.TEN;
        assertEquals(expectedLine3ListPrice.compareTo(this.order.getPlanBundledItems()[2].getPriceAsDecimal()), 0);
	}
	
	/**
	 * This is a test case to rate list prices on planBundledItems containing plan quantity.
	 */
	@Test
	public void testRateOrderApiWithPlanQuantityOnPlanBundleItems() {
        this.planQuantityTestOrder = api.rateOrder(this.planQuantityTestOrder, OrderChangeBL.buildFromOrder(this.planQuantityTestOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        
        for (OrderLineWS lineWs : this.planQuantityTestOrder.getPlanBundledItems()) {
			logger.debug("1.1 List Price: {}", lineWs.getAmount());
			logger.debug("1.2 Adjusted Price: {}", lineWs.getAdjustedPrice());
			logger.debug("1.3 Line Price: {}", lineWs.getPrice());
            // adjusted price should be null on planBundledItems as there is no discount.
            assertNull("Adjusted Price should be null as there is no discount.", lineWs.getAdjustedPriceAsDecimal());
        }
        
        BigDecimal planBundleQuantity = PLAN_BUNDLE_QUANTITY.multiply(PLAN_QUANTITY);
        // list price test: make sure the rate added on price model above is returned as list price on planBundleItems
        BigDecimal expectedLine1ListPrice = PLAN_ITEM_RATE.multiply(planBundleQuantity);
        assertEquals(expectedLine1ListPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[0].getAmountAsDecimal()), 0);
        
        BigDecimal expectedLine2ListPrice = PLAN_ITEM_RATE.add(BigDecimal.ONE);
        expectedLine2ListPrice = expectedLine2ListPrice.multiply(planBundleQuantity);
        assertEquals(expectedLine2ListPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[1].getAmountAsDecimal()), 0);
        
        // Check that price returned for percentage bundle item = percentage (set as 10% in CreateObjectUtil)
        BigDecimal expectedLine3ListPrice = BigDecimal.TEN; 
        assertEquals(expectedLine3ListPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[2].getPriceAsDecimal()), 0);
	}
	
	/**
	 * This test case rates an order that has 2 discounted plan bundle items, then checks 
	 * if the list price and adjusted price for each bundle item is matching the expected prices post rating.
	 * This is similar to testRateOrderApiWithDiscountedPlanBundleItems, except that it takes into account
	 * the PLAN_QUANTITY as 2.
	 */
	@Test
	public void testRateOrderApiWithPlanQuantityOnDiscountedPlanBundleItems() {
		this.amountBasedDiscount = createAmountBasedDiscount(122);
		this.percentageBasedDiscount = createPercentageBasedDiscount(122);
		
		// Amount based Discount applied at Plan Item level
		DiscountLineWS amountBasedPlanItemLevel = new DiscountLineWS();
  		amountBasedPlanItemLevel.setDiscountId(this.amountBasedDiscount.getId());
  		amountBasedPlanItemLevel.setOrderId(this.planQuantityTestOrder.getId());
  		amountBasedPlanItemLevel.setPlanItemId(this.planWS.getPlanItems().get(0).getId());
  		amountBasedPlanItemLevel.setDescription("Test Amount based Discount applied at Plan Item level");
  		
  		// Percentage based Discount applied at Plan Item level
  		DiscountLineWS percentageBasedPlanItemLevel = new DiscountLineWS();
		percentageBasedPlanItemLevel.setDiscountId(this.percentageBasedDiscount.getId());
		percentageBasedPlanItemLevel.setOrderId(this.planQuantityTestOrder.getId());
		percentageBasedPlanItemLevel.setPlanItemId(this.planWS.getPlanItems().get(1).getId());
		percentageBasedPlanItemLevel.setDescription("Test Percentage based Discount applied at Plan Item level");
        
		// Create discount lines array and attach to the order
  		DiscountLineWS []discountLines = new DiscountLineWS[2];
  		discountLines[0] = amountBasedPlanItemLevel;
  		discountLines[1] = percentageBasedPlanItemLevel;
  		this.planQuantityTestOrder.setDiscountLines(discountLines);

  		// rate the order with discounted plan bundle items
  		this.planQuantityTestOrder = api.rateOrder(this.planQuantityTestOrder, OrderChangeBL.buildFromOrder(this.planQuantityTestOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        
        for (OrderLineWS lineWs : this.planQuantityTestOrder.getPlanBundledItems()) {
			logger.debug("1.1 List Price: {}", lineWs.getAmount());
			logger.debug("1.2 Adjusted Price: {}", lineWs.getAdjustedPrice());
			logger.debug("1.3 Line Price: {}", lineWs.getPrice());
        }
        
        BigDecimal planBundleQuantity = PLAN_BUNDLE_QUANTITY.multiply(PLAN_QUANTITY);
        
        // plan item 1: list price test: make sure the rate added on price model above is returned as list price on planBundleItems
        BigDecimal expectedLine1ListPrice = PLAN_ITEM_RATE.multiply(planBundleQuantity); 
        assertEquals(expectedLine1ListPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[0].getAmountAsDecimal()), 0);
        
        // plan item 1: adjusted price test: make sure the amount discount is deducted from the list price on planBundleItems
        BigDecimal expectedLine1DiscountAmount = DISCOUNT_RATE.multiply(planBundleQuantity);
        BigDecimal expectedLine1AdjustedPrice = expectedLine1ListPrice.subtract(expectedLine1DiscountAmount);
        assertEquals(expectedLine1AdjustedPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[0].getAdjustedPriceAsDecimal()), 0);
        
        // plan item 2: list price test: make sure the rate added on price model above is returned as list price on planBundleItems
        BigDecimal expectedLine2ListPrice = PLAN_ITEM_RATE.add(BigDecimal.ONE);
        expectedLine2ListPrice = expectedLine2ListPrice.multiply(planBundleQuantity);
        assertEquals(expectedLine2ListPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[1].getAmountAsDecimal()), 0);
        
        // plan item 2: adjusted price test: make sure the % discount is deducted from the list price on planBundleItems
        BigDecimal expectedLine2DiscountAmount = expectedLine2ListPrice.multiply(DISCOUNT_RATE).divide(new BigDecimal(100));
        BigDecimal expectedLine2AdjustedPrice = expectedLine2ListPrice.subtract(expectedLine2DiscountAmount);
        expectedLine2AdjustedPrice = expectedLine2AdjustedPrice.setScale(Constants.BIGDECIMAL_SCALE_STR, Constants.BIGDECIMAL_ROUND);
        assertEquals(expectedLine2AdjustedPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[1].getAdjustedPriceAsDecimal()), 0);
        
        // Check that price returned for percentage bundle item = percentage (set as 10% in CreateObjectUtil)
        BigDecimal expectedLine3ListPrice = BigDecimal.TEN;
        assertEquals(expectedLine3ListPrice.compareTo(this.planQuantityTestOrder.getPlanBundledItems()[2].getPriceAsDecimal()), 0);
	}
	
	/**
	 * This test case is for testing rate order api to check if discounted order lines
	 * and the order are rated properly. The fields to check for would be: 
	 * 1. orderLine.adjustedPrice
	 * 2. order.adjustedTotal
	 * 
	 * Also the test case checks if order lines are created for given set of discount lines on an order.
	 */
	@Test
	public void testRateOrderApiDiscountedProducts() {
		this.amountBasedDiscount = createAmountBasedDiscount(120);
		this.percentageBasedDiscount = createPercentageBasedDiscount(120);
		
		ItemDTOEx testItem1 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN, CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 1");
		Integer testItem1Id = api.createItem(testItem1);
		ItemDTOEx testItem2 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN.add(BigDecimal.ONE), CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 2");
		Integer testItem2Id = api.createItem(testItem2);
		
		logger.debug("Item Ids 1 : {} & 2 : {}", testItem1Id, testItem2Id);
		
		// create order and lines with items
        OrderWS testOrder = CreateObjectUtil.createOrderObject(this.customer.getUserId(), CURRENCY_USD,
        		Constants.ORDER_BILLING_PRE_PAID, ONE_TIME_ORDER_PERIOD, new DateMidnight(2012, 6, 1).toDate());
        OrderLineWS lines[] = new OrderLineWS[2];

        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
        				testItem1Id, BigDecimal.TEN, "Test Item 1 Order line");
        
        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
						testItem2Id, BigDecimal.TEN.add(BigDecimal.ONE), "Test Item 2 Order line");
        
        assertNotNull("Order should not be null", testOrder);
        
        // lets first rate our test order without discounts
        OrderWS ratedTestOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_STATUS_APPLY_ID));
        
        BigDecimal linesTotal = BigDecimal.ZERO;
        
        for (OrderLineWS line : ratedTestOrder.getOrderLines()) {
        	logger.debug("line amount: {}", line.getAmount());
        	logger.debug("line adjusted price: {}", line.getAdjustedPrice());
        	
        	assertNull("Adjusted Price for the line should be null as there is no line discount.", line.getAdjustedPrice());
        	assertNotNull("Line Amount should be not null after rating an order.", line.getAmount());
        
        	linesTotal = linesTotal.add(line.getAmountAsDecimal());
        }
        
        logger.debug("Order Total: {}", ratedTestOrder.getTotal());
        logger.debug("Order Adjusted Total: {}", ratedTestOrder.getAdjustedTotal());
        
        assertNull("Adjusted Total for the order should be null as there is no order level discount.", ratedTestOrder.getAdjustedTotal());
        assertNotNull("Order Total should be not null post rating.", ratedTestOrder.getTotal());
        assertEquals(linesTotal, ratedTestOrder.getTotalAsDecimal()); 
        
        // apply discounts on order lines - both percentage and amount
        
        // Amount based Discount applied at Item level
 		DiscountLineWS amountBasedItemLevel = new DiscountLineWS();
 		amountBasedItemLevel.setDiscountId(this.amountBasedDiscount.getId());
 		amountBasedItemLevel.setOrderId(ratedTestOrder.getId());
 		amountBasedItemLevel.setItemId(testOrder.getOrderLines()[0].getItemId());
 		amountBasedItemLevel.setDescription("Test Amount based Discount applied at Item level");
   		
   		// Percentage based Discount applied at Item level
   		DiscountLineWS percentageBasedItemLevel = new DiscountLineWS();
   		percentageBasedItemLevel.setDiscountId(this.percentageBasedDiscount.getId());
   		percentageBasedItemLevel.setOrderId(ratedTestOrder.getId());
   		percentageBasedItemLevel.setItemId(testOrder.getOrderLines()[1].getItemId());
   		percentageBasedItemLevel.setDescription("Test Percentage based Discount applied at Item level");
         
 		// Create discount lines array and attach to the test order
   		DiscountLineWS []discountLines = new DiscountLineWS[2];
   		discountLines[0] = amountBasedItemLevel;
   		discountLines[1] = percentageBasedItemLevel;
        
   		testOrder.setDiscountLines(discountLines);
        
        // rate order again post discounts
   		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_STATUS_APPLY_ID));
   		
        // check adjusted prices on lines
   		BigDecimal discountedLinesTotal = BigDecimal.ZERO;
   		
   		for (OrderLineWS line : testOrder.getOrderLines()) {
   			
        	logger.debug("line amount: {}", line.getAmount());
        	logger.debug("line adjusted price: {}", line.getAdjustedPrice());
        	
        	assertNotNull("Line Amount should be not null after rating an order.", line.getAmount());
        	
        	if (line.getTypeId().intValue() == Constants.ORDER_LINE_TYPE_DISCOUNT) {
        		
        		logger.debug("ORDER_LINE_TYPE_DISCOUNT");
        		// adjusted price should be null for discount type order line
        		assertNull("Adjusted Price for the line should be null as this is discount line.", line.getAdjustedPrice());
        		
        		// the line amount for discount type order line should be negative
        		assertTrue(line.getAmountAsDecimal().compareTo(BigDecimal.ZERO) < 0);
        		
        	} else if (line.getTypeId().intValue() == Constants.ORDER_LINE_TYPE_ITEM) {
        		
        		logger.debug("ORDER_LINE_TYPE_ITEM");
        		
        		// adjusted price should be not null and positive/zero (but not negative)
        		assertNotNull("Adjusted Price for the line should be not null as there is line discount.", line.getAdjustedPrice());
        		assertTrue(line.getAdjustedPriceAsDecimal().compareTo(BigDecimal.ZERO) >= 0);
        	}
        
        	discountedLinesTotal = discountedLinesTotal.add(line.getAmountAsDecimal());
        }
   		
   		// check if adjusted price on line 1 = line amount - discount
   		OrderLineWS line1Ws = findOrderLineWithItem(testOrder.getOrderLines(), testItem1Id);
   		OrderLineWS line2Ws = findOrderLineWithItem(testOrder.getOrderLines(), testItem2Id);   	
   		   		
   		JBillingTestUtils.assertEquals("Line1: ", line1Ws.getAmountAsDecimal().subtract(DISCOUNT_RATE), line1Ws.getAdjustedPriceAsDecimal());
        assertEquals((line2Ws.getAmountAsDecimal().
        				subtract((DISCOUNT_RATE.multiply(line2Ws.getAmountAsDecimal().divide(new BigDecimal(100)))))).
        				compareTo(line2Ws.getAdjustedPriceAsDecimal()), 0);
   		
   		logger.debug("Order Total: {}", testOrder.getTotal());
        logger.debug("Order Adjusted Total: {}", testOrder.getAdjustedTotal());
        
        assertNotNull("Adjusted Total for the order should be not null as there are product level discounts.", testOrder.getAdjustedTotal());
        assertNotNull("Order Total should be not null post rating.", testOrder.getTotal());

        // check adjusted total on order is less than order total
   		assertTrue(testOrder.getAdjustedTotalAsDecimal().compareTo(testOrder.getTotalAsDecimal()) < 0);
   		
   		// calculate the expected adjusted total by subtracting amount based discount from order total
   		BigDecimal line1DiscountAmount = DISCOUNT_RATE;	// directly the amount based discount amount
   		BigDecimal line2DiscountAmount = DISCOUNT_RATE.multiply(line2Ws.getAmountAsDecimal().divide(new BigDecimal(100))); // percentage discount
   		BigDecimal expectedAdjustedTotal = testOrder.getTotalAsDecimal().subtract((line1DiscountAmount.add(line2DiscountAmount)));
   		
        assertEquals(expectedAdjustedTotal.compareTo(testOrder.getAdjustedTotalAsDecimal()), 0);

		//cleanup
		api.deleteItem(testItem2Id);
		api.deleteItem(testItem1Id);
	}
	
	/**
	 * This test case applies amount and percentage based discounts at order level
	 * and rates the order to verify if the order's adjusted total has been updated correctly.
	 */
	@Test
	public void testRateOrderApiDiscountedOrder() {
		this.amountBasedDiscount = createAmountBasedDiscount(121);
		this.percentageBasedDiscount = createPercentageBasedDiscount(121);
		
		ItemDTOEx testItem1 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN, CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 1");
		Integer testItem1Id = api.createItem(testItem1);
		ItemDTOEx testItem2 = CreateObjectUtil.createItem(TEST_ENTITY_ID, BigDecimal.TEN.add(BigDecimal.ONE), CURRENCY_USD, TEST_ITEM_CATEGORY, "Test Item 2");
		Integer testItem2Id = api.createItem(testItem2);
		
		// create order and lines with items
        OrderWS testOrder = CreateObjectUtil.createOrderObject(this.customer.getUserId(), CURRENCY_USD,
        		Constants.ORDER_BILLING_PRE_PAID, ONE_TIME_ORDER_PERIOD, new DateMidnight(2012, 6, 1).toDate());
        OrderLineWS lines[] = new OrderLineWS[2];

        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
        				testItem1Id, BigDecimal.TEN, "Test Item 1 Order line");
        
        testOrder = CreateObjectUtil.addLine(testOrder, 1, Constants.ORDER_LINE_TYPE_ITEM, 
						testItem2Id, BigDecimal.TEN.add(BigDecimal.ONE), "Test Item 2 Order line");
        
        assertNotNull("Order should not be null", testOrder);
		
        // Amount based Discount applied at Order level
  		DiscountLineWS amountBasedOrderLevel = new DiscountLineWS();
  		amountBasedOrderLevel.setDiscountId(this.amountBasedDiscount.getId());
  		amountBasedOrderLevel.setOrderId(testOrder.getId());
  		amountBasedOrderLevel.setDescription("Test Amount based Discount applied at Order level");
    		
		// Percentage based Discount applied at Order level
		DiscountLineWS percentageBasedOrderLevel = new DiscountLineWS();
		percentageBasedOrderLevel.setDiscountId(this.percentageBasedDiscount.getId());
		percentageBasedOrderLevel.setOrderId(testOrder.getId());
		percentageBasedOrderLevel.setDescription("Test Percentage based Discount applied at Order level");
        
		DiscountLineWS []orderDiscountLines = new DiscountLineWS[2];
		orderDiscountLines[0] = amountBasedOrderLevel;
		orderDiscountLines[1] = percentageBasedOrderLevel;
        
		logger.debug("Amount Based Discount @ {}", amountBasedDiscount.getRateAsDecimal());
		logger.debug("Percentage Based Discount @ {}", percentageBasedDiscount.getRateAsDecimal());
		
   		testOrder.setDiscountLines(orderDiscountLines);
        
        // rate order again post discounts
   		testOrder = api.rateOrder(testOrder, OrderChangeBL.buildFromOrder(testOrder, ORDER_CHANGE_STATUS_APPLY_ID));
   		
   		//discount lines to contain discount amounts
        for (DiscountLineWS discountLIne: testOrder.getDiscountLines()) {
            logger.debug("Discount amounts {}", discountLIne.getDiscountAmountAsDecimal());
            assertNotNull("Discount Amount must be set for Order Level Discount lines.", 
                    discountLIne.getDiscountAmount());
            if (discountLIne.getDiscountId() == amountBasedDiscount.getId() ) 
                assertEquals("Discount Amount set right", discountLIne.getDiscountAmountAsDecimal(), new BigDecimal("5.0").negate());
        }
        // check adjusted total on order
   		logger.debug("Adjusted Total: {}", testOrder.getAdjustedTotal());
   		logger.debug("Total: {}", testOrder.getTotal());
   		
   		assertNotNull("Adjusted total on order should be not null", testOrder.getAdjustedTotal());
   		assertNotNull("Order Total should be not null", testOrder.getTotal());
   		
   		assertTrue(testOrder.getAdjustedTotalAsDecimal().compareTo(testOrder.getTotalAsDecimal()) < 0);
   		
   		// calculate the expected adjusted total by subtracting amount based discount from order total
   		BigDecimal expectedAdjustedTotal = testOrder.getTotalAsDecimal().subtract(DISCOUNT_RATE);
   		
   		// now deduct the percentage based discount
   		expectedAdjustedTotal = expectedAdjustedTotal.
   			subtract(testOrder.getTotalAsDecimal().multiply(DISCOUNT_RATE.divide(new BigDecimal(100))));
   		
        assertEquals(expectedAdjustedTotal.compareTo(testOrder.getAdjustedTotalAsDecimal()), 0);

		//cleanup
		api.deleteItem(testItem2Id);
		api.deleteItem(testItem1Id);
	}
	
	private DiscountWS createAmountBasedDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DISC-AMT-310-" + callCounter);
		discountWs.setDescription("Flat Discount (Code 310-" + callCounter + ") of $5");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(DISCOUNT_RATE);
		discountWs.setType(DiscountStrategyType.ONE_TIME_AMOUNT.name());
		discountWs.setEntityId(TEST_ENTITY_ID);
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	private DiscountWS createPercentageBasedDiscount(Integer callCounter) {
		Calendar startOfThisMonth = Calendar.getInstance();
		startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);
				
		Calendar oneYearLater = Calendar.getInstance();
		oneYearLater.set(oneYearLater.get(Calendar.YEAR) + 1, oneYearLater.get(Calendar.MONTH), oneYearLater.get(Calendar.DAY_OF_MONTH));
		
		DiscountWS discountWs = new DiscountWS();
		discountWs.setCode("DISC-PERCENT-310-" + callCounter);
		discountWs.setDescription("Discount (Code 310-" + callCounter + ") of 5%");
		discountWs.setStartDate(startOfThisMonth.getTime());
		discountWs.setEndDate(oneYearLater.getTime());
		discountWs.setRate(DISCOUNT_RATE);
		discountWs.setType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name());
		
		Integer discountId = api.createOrUpdateDiscount(discountWs);
		return api.getDiscountWS(discountId);
	}
	
	@AfterMethod
    protected void tearDown() throws Exception {
        try{
	        if (this.amountBasedDiscount != null && this.amountBasedDiscount.getId() > 0) {
	        	api.deleteDiscount(this.amountBasedDiscount.getId());
	        }
	        
	        if (this.percentageBasedDiscount != null && this.percentageBasedDiscount.getId() > 0) {
	        	api.deleteDiscount(this.percentageBasedDiscount.getId());
	        }
        } catch (Exception e) {
        	logger.error("Exception trying to delete discounts on tear down method", e);
        }
        
        if (this.planWS != null) {
        	api.deletePlan(this.planWS.getId());
	        api.deleteItem(this.planWS.getItemId());
	        for(PlanItemWS planItem : this.planWS.getPlanItems()){
		        api.deleteItem(planItem.getItemId());
	        }
        }
        
        if (this.customer != null) {
        	api.deleteUser(this.customer.getUserId());
        }
        
        if (this.planQuantityTestCustomer != null) {
        	api.deleteUser(this.planQuantityTestCustomer.getUserId());
        }
    }

    private OrderLineWS findOrderLineWithItem(OrderLineWS[] lines, Integer itemId) {
        for (OrderLineWS line : lines) {
            if (line.getItemId().equals(itemId)) return line;
        }
        return null;
    }
}
