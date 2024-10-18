package com.sapienter.jbilling.server.swapPlan;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessTestCase;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;

/**
 * JB-3255 : Customer price expires Test Case
 * 
 * @author Swapnil
 *
 */

@Test(groups = { "swapPlan" }, testName = "CustomerPriceExpiresTest")
public class SwapPlanCustomerPriceExpiryTest extends BillingProcessTestCase {

	private Logger logger = LoggerFactory.getLogger(SwapPlanCustomerPriceExpiryTest.class);

    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final String CUSTOMER_CODE = "Test-customer-" + Calendar.getInstance().getTimeInMillis();
    public static final Integer ACCOUNT_TYPE_ID = 60103;

    private TestBuilder testBuilder;
    private JbillingAPI api;
    private PlanWS planwith100Min;
    private PlanWS planWith225Min;
    private Integer userId;
    private Integer orderId;
    private BigDecimal overageQuantity = BigDecimal.ZERO;
    private BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
    private Integer onetimeOrderId = null;
    private Calendar activeSince;
    private Calendar activeUntil;
    private Calendar nextInvoiceDate;
    private static int product8XXTollFreeId;
    private static Date originalBillingProcessRunDate;
    private EnvironmentHelper environmentHelper;
    private static final String SWAP_CHANGES_DONE = "Swap changes done";
    private static final String _380_0000000000 = "380.0000000000";
    private static final String SWAP_PLAN_FROM_225_MIN_PLAN_TO_100_MIN_PLAN = "Swap plan from 225 min plan to 100 min plan !";
    private static final String _380_000 = "380.000";
    private static final String SWAP_PLAN = "Swap Plan - ";
    private static final String _380_00 = "380.00";
	private static final String _225_0000 = "225.0000";
	private static final String SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID = "##Subscription Order's swapped Plan ID ::: {}";
	private static final String _225_0000000000 = "225.0000000000";
	private static final String ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL = "### Order after swap plan should not be null";
	private static final String ONE_TIME_ORDER = "One time order: ";
	private static final String SWAP_CHANGES_SHOULD_BE_CALCULATED = "Swap changes should be calculated";
	private static final String SWAP_PLAN_FROM_100_MIN_PLAN_TO_225_MIN_PLAN = "Swap plan from 100 min plan to 225 min plan !";
	private static final String SUBSCRIPTION_ORDER_AMOUNT_MUST_BE = "## Subscription Order Amount must be";
	private static final String ORDER_TOTAL_QUANTITY_SHOULD_BE_1 = "Order total quantity should be 1 !";
	private static final String ORDER_TOTAL_IS = "orderTotal is {}";
	private static final String AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL = "Available Quantity Should not be null";
	private static final String FREE_USAGE_QUANTITY_SHOULD_NOT_NULL = "FreeUsage Quantity Should not null";
	private static final String OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL = "OverageQuantity should not be null!";
	private static final String ORDER_TOTAL_QUANTITY_SHOULD_BE = "Order total quantity should be";
	private static final String ONE_TIME_ORDER_CREATED_WITH_ID = "one time order created with id {}";
	private static final String GET_ORDER_SHOULD_NOT_BE_NULL = "getOrder should not be null";
	private static final String EXPECTED_CYCLE_END_DATE_OF_CUSTOMER_USAGE_POOL = "Expected Cycle end date of customer usage pool: ";
	private static final String DELETING_USER = "deleting user {}";
	private static final String EXPECTED_CYCLE_START_DATE_OF_CUSTOMER_USAGE_POOL = "Expected Cycle start date of customer usage pool: ";
	private static final String DELETING_INVOICES = "deleting invoices !";
	private static final String _100_0000 = "100.0000";
	private static final String USAGE_POOL_PRORATED_QUANTITY_SHOULD_BE = "Usage pool Prorated quantity should be ";
	private static final String USAGE_POOL_QUANTITY_SHOULD_BE = "Usage pool quantity should be ";
	private static final String CUSTOMER_USAGE_POOL_QUANTITY = "Customer Usage Pool Quantity :::{}";
	private static final String CUSTOMER_USAGE_POOL_PRORATED_QUANTITY = "Customer Usage Pool Prorated Quantity :::{}";
	private static final String ORDER_ID_SHOULD_NOT_BE_NULL = "orderId should not be null";
	private static final String SUBSCRIPTION_ORDER_CREATED_WITH_ID_AND_ACTIVE_SINCE_DATE = "Subscription order created with Id :::{} And Active Since Date :::{}";
	private static final String CREATING_PLAN_SUBSCRIPTION_ORDER = "##Creating Plan Subscription order...";
	private static final String PLAN_WS_GET_USAGE_POOL_IDS_0 = "planWS.getUsagePoolIds()[0] ::::::::;;{}";
	private static final String USER_NEXT_INVOICE_DATE = "user Next Invoice Date ::: {}";
	private static final String USER_SHOULD_NOT_BE_NULL = "user should not be null";
	private static final String USER_CREATED_WITH_ID = "user created with id {}";

    @BeforeClass
    public void initializeTests(){
        logger.debug("inside initializeTests()");
        testBuilder = getTestEnvironment();
        environmentHelper = EnvironmentHelper.getInstance(api);
        originalBillingProcessRunDate = api.getBillingProcessConfiguration().getNextRunDate();
    }

    private TestBuilder getTestEnvironment()  {
        logger.debug("getting test environment!");
        return TestBuilder.newTest(false).givenForMultiple(envBuilder -> {
            try {
                api = JbillingAPIFactory.getAPI();
                UsagePoolWS usagePoolWith100Quantity = FullCreativeUtil.populateFreeUsagePoolObject("100");
                usagePoolWith100Quantity.setId(api.createUsagePool(usagePoolWith100Quantity));

                UsagePoolWS usagePoolWith225Quantity = FullCreativeUtil.populateFreeUsagePoolObject("225");
                usagePoolWith225Quantity.setId(api.createUsagePool(usagePoolWith225Quantity));

                assertNotNull("Usage Pool Creation Failed ", usagePoolWith100Quantity);
                assertNotNull("Usage Pool Creation Failed ", usagePoolWith225Quantity);

                planWith225Min = FullCreativeUtil.createPlan("225", "0.95", 
                        new Integer[]{usagePoolWith225Quantity.getId()},
                        "Test Plan 225 Min", api, 
                        TestConstants.CHAT_USAGE_PRODUCT_ID, 
                        TestConstants.INBOUND_USAGE_PRODUCT_ID, 
                        TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);

                assertNotNull("planWS should not be null", planWith225Min);
                logger.debug("plan -{} created", planWith225Min.getId());

                planwith100Min = FullCreativeUtil.createPlan("100", "1.00", 
                        new Integer[]{usagePoolWith100Quantity.getId()},
                        "Test Plan 100 Min", api, 
                        TestConstants.CHAT_USAGE_PRODUCT_ID, 
                        TestConstants.INBOUND_USAGE_PRODUCT_ID, 
                        TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
                assertNotNull("planWS should not be null", planwith100Min);
                logger.debug("plan -{} created", planwith100Min.getId());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        });
    }

    /**
     * Test case for prepaid order
     */
    @Test(priority = 1, enabled = true)
    public void testScenario01() {
        logger.debug("test scenario no 1");
        testBuilder.given(envBuilder -> {
            try {
                activeSince = Calendar.getInstance();
                activeSince.setTime(FullCreativeUtil.getDate(0, 1, 2009));

                activeUntil = (Calendar) activeSince.clone();
                activeUntil.set(Calendar.DATE, activeSince.getActualMaximum(Calendar.DAY_OF_MONTH));

                nextInvoiceDate = (Calendar) activeSince.clone();
                nextInvoiceDate.add(Calendar.MONTH, 1);

                userId = buildAndPersistCustomer(envBuilder, CUSTOMER_CODE, ACCOUNT_TYPE_ID, nextInvoiceDate.getTime(),
                        nextInvoiceDate.get(Calendar.DAY_OF_WEEK));
                UserWS user = api.getUserWS(userId);
                userId = user.getId();
                logger.debug(USER_CREATED_WITH_ID, user.getId());
                assertNotNull(USER_SHOULD_NOT_BE_NULL,user);

                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);

                user = api.getUserWS(userId);
                logger.debug(USER_NEXT_INVOICE_DATE, TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

                logger.debug(PLAN_WS_GET_USAGE_POOL_IDS_0, planwith100Min.getUsagePoolIds()[0]);

                updateBillingProcessConfiguration(nextInvoiceDate.getTime(), 21);
            } catch(Exception e) {
                logger.error(e.getMessage());
            }
        }).test(env -> {
            try {
                InvoiceWS invoiceWS = null;

                UserWS user = api.getUserWS(userId);
                logger.debug(CREATING_PLAN_SUBSCRIPTION_ORDER);
                orderId = createOrder(planwith100Min, user.getUserId(), activeSince.getTime(), null, 1);
                OrderWS order  =  api.getOrder(orderId);
                logger.debug(SUBSCRIPTION_ORDER_CREATED_WITH_ID_AND_ACTIVE_SINCE_DATE, orderId,
                        TestConstants.DATE_FORMAT.format(order.getActiveSince()));

                assertNotNull(ORDER_ID_SHOULD_NOT_BE_NULL,orderId);

                logger.debug(CUSTOMER_USAGE_POOL_PRORATED_QUANTITY,
                        api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
                CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_PRORATED_QUANTITY_SHOULD_BE,new BigDecimal(_100_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                assertEquals(EXPECTED_CYCLE_START_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeSince.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

                assertEquals(EXPECTED_CYCLE_END_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeUntil.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

                order = api.getOrder(orderId);
                assertNotNull(GET_ORDER_SHOULD_NOT_BE_NULL, order);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","101");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                String orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE_1, "1.00", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                order = api.getOrder(orderId);
                //swap plan upgrade
                logger.debug("##Swapping existing planwith100Min Plan (100 Min Free usage Pool) With planwith225Min (225 Min Free usage Pool)...");
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        planwith100Min.getItemId(),
                        planWith225Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_100_MIN_PLAN_TO_225_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                order = api.getOrder(orderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, order);
                assertEquals("## Subscription Order Amount must be 225.00", _225_0000000000,order.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planWith225Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_PRORATED_QUANTITY_SHOULD_BE,new BigDecimal(_225_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                assertEquals(EXPECTED_CYCLE_START_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeSince.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

                assertEquals(EXPECTED_CYCLE_END_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeUntil.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","125");
                oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                orderTotal = api.getOrder(onetimeOrderId).getTotal();
                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE_1, "0.95", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan downgrade
                logger.debug("##Swapping existing planwith225Min Plan (225 Min Free usage Pool) With planwith100Min (100 Min Free usage Pool)...");
                order = api.getOrder(orderId);
                OrderChangeWS[] orderChanges2 = api.calculateSwapPlanChanges(order,
                        planWith225Min.getItemId(),
                        planwith100Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_225_MIN_PLAN_TO_100_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges2);

                api.createUpdateOrder(order, orderChanges2);

                order = api.getOrder(orderId);
                logger.debug("Swapped to Plan Id :::{}", planwith100Min.getId());

                assertNotNull("After swap plan order should not be null", order);
                assertEquals("Subscription Order Amount must be", "100.0000000000",order.getTotal());

                user = api.getUserWS(user.getId());

                logger.debug(CUSTOMER_USAGE_POOL_PRORATED_QUANTITY,
                        api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals("After Swap Plan Usage pool Prorated quantity should be ",new BigDecimal(_100_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
                assertEquals("After Swap Plan Expected Cycle start date of new customer usage pool: "
                        ,TestConstants.DATE_FORMAT.format(activeSince.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

                assertEquals("After Swap Plan Expected Cycle end date of new customer usage pool: "
                        ,TestConstants.DATE_FORMAT.format(activeUntil.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planwith100Min.getId());

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","1");
                oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                orderTotal = api.getOrder(onetimeOrderId).getTotal();
                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE_1, "1.00", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                logger.debug("Generating invoices!");
                Integer[] invoiceIds = api.createInvoiceWithDate(userId, nextInvoiceDate.getTime(), 3, 21, false);
                logger.debug("invoice {} is generated!", invoiceIds[0]);
                invoiceWS = api.getInvoiceWS(invoiceIds[0]);
                assertNotNull("Invoice should be generated!", invoiceWS);
            } catch(Exception e) {
                logger.error(e.getMessage());
            } finally {
                logger.debug(DELETING_INVOICES);
                Arrays.stream(api.getUserInvoicesPage(userId, 10, 0))
                      .forEach(invoice -> api.deleteInvoice(invoice.getId()));

                logger.debug(DELETING_USER, userId);
                api.deleteUser(userId);
            }
        });
    }

    /**
     * Test case for postpaid order
     */
    @Test(priority = 2, enabled = true)
    public void testScenario02() {
        logger.debug("test scenario no 1");
        testBuilder.given(envBuilder -> {
            try {
                activeSince = Calendar.getInstance();
                activeSince.setTime(FullCreativeUtil.getDate(0, 1, 2009));

                activeUntil = (Calendar) activeSince.clone();
                activeUntil.set(Calendar.DATE, activeSince.getActualMaximum(Calendar.DAY_OF_MONTH));

                nextInvoiceDate = (Calendar) activeSince.clone();
                nextInvoiceDate.add(Calendar.MONTH, 1);

                UserWS user  = FullCreativeUtil.createUser("Swap Plan Proration");
                userId = user.getId();
                logger.debug(USER_CREATED_WITH_ID, user.getId());
                assertNotNull(USER_SHOULD_NOT_BE_NULL,user);
                user = api.getUserWS(userId);

                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);

                user = api.getUserWS(userId);
                logger.debug(USER_NEXT_INVOICE_DATE, TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

                logger.debug(PLAN_WS_GET_USAGE_POOL_IDS_0, planwith100Min.getUsagePoolIds()[0]);
            } catch(Exception e) {
                logger.error(e.getMessage());
            }
        }).test(env -> {
            try {
                UserWS user = api.getUserWS(userId);

                logger.debug(CREATING_PLAN_SUBSCRIPTION_ORDER);
                orderId = createOrder(planwith100Min, user.getUserId(), activeSince.getTime(), null, 2);
                OrderWS order  =  api.getOrder(orderId);
                logger.debug(SUBSCRIPTION_ORDER_CREATED_WITH_ID_AND_ACTIVE_SINCE_DATE, orderId,
                        TestConstants.DATE_FORMAT.format(order.getActiveSince()));

                assertNotNull(ORDER_ID_SHOULD_NOT_BE_NULL,orderId);

                logger.debug(CUSTOMER_USAGE_POOL_PRORATED_QUANTITY,
                        api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
                CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_PRORATED_QUANTITY_SHOULD_BE,new BigDecimal(_100_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                assertEquals(EXPECTED_CYCLE_START_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeSince.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

                assertEquals(EXPECTED_CYCLE_END_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeUntil.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

                order = api.getOrder(orderId);
                assertNotNull(GET_ORDER_SHOULD_NOT_BE_NULL, order);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","101");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                String orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE_1, "1.00", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan upgrade
                logger.debug("##Swapping existing planwith100Min Plan (100 Min Free usage Pool) With planwith225Min (225 Min Free usage Pool)...");
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        planwith100Min.getItemId(),
                        planWith225Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_100_MIN_PLAN_TO_225_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                order = api.getOrder(orderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, order);
                assertEquals("## Subscription Order Amount must be 225.00", _225_0000000000,order.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planWith225Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_PRORATED_QUANTITY_SHOULD_BE,new BigDecimal(_225_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                assertEquals(EXPECTED_CYCLE_START_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeSince.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

                assertEquals(EXPECTED_CYCLE_END_DATE_OF_CUSTOMER_USAGE_POOL
                        ,TestConstants.DATE_FORMAT.format(activeUntil.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","125");
                oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                orderTotal = api.getOrder(onetimeOrderId).getTotal();
                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE_1, "0.95", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan downgrade
                logger.debug("##Swapping existing planwith225Min Plan (225 Min Free usage Pool) With planwith100Min (100 Min Free usage Pool)...");
                OrderWS retOrder = api.getOrder(orderId);
                OrderChangeWS[] orderChanges2 = api.calculateSwapPlanChanges(retOrder, 
                        planWith225Min.getItemId(), 
                        planwith100Min.getItemId(), 
                        SwapMethod.DIFF,  
                        Util.truncateDate(retOrder.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_225_MIN_PLAN_TO_100_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges2);

                api.createUpdateOrder(retOrder, orderChanges2);

                retOrder = api.getOrder(orderId);
                logger.debug("Swapped to Plan Id :::{}", planwith100Min.getId());

                assertNotNull("After swap plan order should not be null", retOrder);
                assertEquals("Subscription Order Amount must be", "100.0000000000",retOrder.getTotal());

                user = api.getUserWS(user.getId());

                logger.debug(CUSTOMER_USAGE_POOL_PRORATED_QUANTITY,
                        api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals("After Swap Plan Usage pool Prorated quantity should be ",new BigDecimal(_100_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));
                assertEquals("After Swap Plan Expected Cycle start date of new customer usage pool: "
                        ,TestConstants.DATE_FORMAT.format(activeSince.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleStartDate()));

                assertEquals("After Swap Plan Expected Cycle end date of new customer usage pool: "
                        ,TestConstants.DATE_FORMAT.format(activeUntil.getTime())
                        ,TestConstants.DATE_FORMAT.format(customerUsagePoolWS.getCycleEndDate()));

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planwith100Min.getId());

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","1");
                oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                orderTotal = api.getOrder(onetimeOrderId).getTotal();
                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE_1, "1.00", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //running billing process
                logger.debug("generating invoice!");
                Integer[] invoiceIds = api.createInvoiceWithDate(userId, nextInvoiceDate.getTime(), 3, 21, false);
                while (api.isBillingRunning(api.getCallerCompanyId())) {
                    logger.debug("waiting:::::");
                    sleep(2000);
                }
                logger.debug("invoice {} generated after billing run!", invoiceIds[0]);
                assertNotNull("Invoice should be generated !", invoiceIds);
            } catch(Exception e) {
                logger.error(e.getMessage());
            } finally {
                logger.debug(DELETING_INVOICES);
                Arrays.stream(api.getUserInvoicesPage(userId, 10, 0))
                      .forEach(item ->   logger.debug(":::::::::::::::"+item.getId()));
                Arrays.stream(api.getUserInvoicesPage(userId, 10, 0))
                      .forEach(invoice -> api.deleteInvoice(invoice.getId()));

                logger.debug(DELETING_USER, userId);
                api.deleteUser(userId);
            }
        });
    }

    @Test(priority = 1, enabled = true)
    public void testCustomerPriceAfterPlanSwap() {
        logger.debug("test customer price after planswap");
        testBuilder.given(envBuilder -> {
            try {
                activeSince = Calendar.getInstance();
                activeSince.set(Calendar.DATE, 1);
                activeSince.add(Calendar.MONTH, -2);

                activeUntil = (Calendar) activeSince.clone();
                activeUntil.set(Calendar.DATE, activeSince.getActualMaximum(Calendar.DAY_OF_MONTH));

                nextInvoiceDate = (Calendar) activeSince.clone();
                nextInvoiceDate.add(Calendar.MONTH, 1);

                UserWS user  = FullCreativeUtil.createUser(SWAP_PLAN+ new Date().getTime());
                userId = user.getId();
                logger.debug(USER_CREATED_WITH_ID, user.getId());
                assertNotNull(USER_SHOULD_NOT_BE_NULL,user);
                user = api.getUserWS(userId);

                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);

                user = api.getUserWS(userId);
                logger.debug(USER_NEXT_INVOICE_DATE, TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

                logger.debug(PLAN_WS_GET_USAGE_POOL_IDS_0, planwith100Min.getUsagePoolIds()[0]);
            } catch(Exception e) {
                logger.error(e.getMessage());
            }
        }).test(env -> {
            try {
                UserWS user = api.getUserWS(userId);

                logger.debug(CREATING_PLAN_SUBSCRIPTION_ORDER);
                orderId = FullCreativeUtil.createOrder(planwith100Min, user.getUserId(), activeSince.getTime(), null);
                OrderWS order  =  api.getOrder(orderId);
                logger.debug(SUBSCRIPTION_ORDER_CREATED_WITH_ID_AND_ACTIVE_SINCE_DATE, orderId,
                        TestConstants.DATE_FORMAT.format(order.getActiveSince()));

                assertNotNull(ORDER_ID_SHOULD_NOT_BE_NULL,orderId);

                logger.debug(CUSTOMER_USAGE_POOL_QUANTITY,
                        api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
                CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal(_100_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                order = api.getOrder(orderId);
                assertNotNull(GET_ORDER_SHOULD_NOT_BE_NULL, order);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","325");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                String orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, "224.99", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan upgrade
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        planwith100Min.getItemId(),
                        planWith225Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_100_MIN_PLAN_TO_225_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                OrderWS oneTimeOrder = api.getOrder(onetimeOrderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, oneTimeOrder);
                assertEquals(SUBSCRIPTION_ORDER_AMOUNT_MUST_BE, "94.9999999975",oneTimeOrder.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planWith225Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal(_225_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                logger.debug(ONE_TIME_ORDER + oneTimeOrder);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, "94.9999", oneTimeOrder.getTotal().substring(0, orderTotal.indexOf('.')+4));

                api.createInvoice(userId, false);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","400");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, _380_00, orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan upgrade
                orderChanges = api.calculateSwapPlanChanges(order,
                        planWith225Min.getItemId(),
                        planwith100Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_225_MIN_PLAN_TO_100_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                logger.debug(SWAP_CHANGES_DONE);

                order = api.getOrder(onetimeOrderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, order);
                assertEquals(SUBSCRIPTION_ORDER_AMOUNT_MUST_BE, _380_0000000000,order.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planWith225Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal(_225_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                logger.debug(ONE_TIME_ORDER + order);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, _380_000, order.getTotal().substring(0, orderTotal.indexOf('.')+4));


            } catch(Exception e) {
                logger.error(e.getMessage());
                fail();
            } finally {
                logger.debug(DELETING_INVOICES);
                Arrays.stream(api.getUserInvoicesPage(userId, 10, 0))
                        .forEach(invoice -> api.deleteInvoice(invoice.getId()));

                logger.debug(DELETING_USER, userId);
                api.deleteUser(userId);
            }
        });
    }

    @Test(priority = 1, enabled = true)
    public void testCustomerPriceForPrePaidOrderAfterPlanSwap() {
        logger.debug("test customer price for prepaid order after planswap");
        testBuilder.given(envBuilder -> {
            try {
                activeSince = Calendar.getInstance();
                activeSince.set(Calendar.DATE, 1);
                activeSince.add(Calendar.MONTH, -2);

                activeUntil = (Calendar) activeSince.clone();
                activeUntil.set(Calendar.DATE, activeSince.getActualMaximum(Calendar.DAY_OF_MONTH));

                nextInvoiceDate = (Calendar) activeSince.clone();
                nextInvoiceDate.add(Calendar.MONTH, 1);

                UserWS user  = FullCreativeUtil.createUser(SWAP_PLAN+ new Date().getTime());
                userId = user.getId();
                logger.debug(USER_CREATED_WITH_ID, user.getId());
                assertNotNull(USER_SHOULD_NOT_BE_NULL,user);
                user = api.getUserWS(userId);

                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);

                user = api.getUserWS(userId);
                logger.debug(USER_NEXT_INVOICE_DATE, TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

                logger.debug(PLAN_WS_GET_USAGE_POOL_IDS_0, planwith100Min.getUsagePoolIds()[0]);
            } catch(Exception e) {
                logger.error(e.getMessage());
            }
        }).test(env -> {
            try {
                UserWS user = api.getUserWS(userId);

                logger.debug(CREATING_PLAN_SUBSCRIPTION_ORDER);
                orderId = FullCreativeUtil.createPrepaidOrder(planwith100Min, user.getUserId(), activeSince.getTime(), null);
                OrderWS order  =  api.getOrder(orderId);
                logger.debug(SUBSCRIPTION_ORDER_CREATED_WITH_ID_AND_ACTIVE_SINCE_DATE, orderId,
                        TestConstants.DATE_FORMAT.format(order.getActiveSince()));

                assertNotNull(ORDER_ID_SHOULD_NOT_BE_NULL,orderId);

                logger.debug(CUSTOMER_USAGE_POOL_QUANTITY,
                        api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
                CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal(_100_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                order = api.getOrder(orderId);
                assertNotNull(GET_ORDER_SHOULD_NOT_BE_NULL, order);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","325");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                String orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, "224.99", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan upgrade
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        planwith100Min.getItemId(),
                        planWith225Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_100_MIN_PLAN_TO_225_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                OrderWS oneTimeOrder = api.getOrder(onetimeOrderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, oneTimeOrder);
                assertEquals(SUBSCRIPTION_ORDER_AMOUNT_MUST_BE, "94.9999999975",oneTimeOrder.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planwith100Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal(_225_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                logger.debug(ONE_TIME_ORDER + oneTimeOrder);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, "94.9999", oneTimeOrder.getTotal().substring(0, orderTotal.indexOf('.')+4));
                api.createInvoice(userId, false);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, activeSince.getTime() ,"0","0","400");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, _380_00, orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan upgrade
                orderChanges = api.calculateSwapPlanChanges(order,
                        planWith225Min.getItemId(),
                        planwith100Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_225_MIN_PLAN_TO_100_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                logger.debug(SWAP_CHANGES_DONE);

                order = api.getOrder(onetimeOrderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, order);
                assertEquals(SUBSCRIPTION_ORDER_AMOUNT_MUST_BE, _380_0000000000,order.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planWith225Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal(_225_0000), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                logger.debug(ONE_TIME_ORDER + order);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, _380_000, order.getTotal().substring(0, orderTotal.indexOf('.')+4));


            } catch(Exception e) {
                logger.error(e.getMessage());
                fail();
            } finally {
                logger.debug(DELETING_INVOICES);
                Arrays.stream(api.getUserInvoicesPage(userId, 10, 0))
                        .forEach(invoice -> api.deleteInvoice(invoice.getId()));

                logger.debug(DELETING_USER, userId);
                api.deleteUser(userId);
            }
        });
    }

    @Test(priority = 1, enabled = true)
    public void testCustomerPriceAfterPlanSwapWithActiveSinceDate() {
        logger.debug("test customer price after plan swap with active since date");
        testBuilder.given(envBuilder -> {
            try {
                activeSince = Calendar.getInstance();
                activeSince.set(Calendar.DATE, 1);
                activeSince.set(Calendar.MONTH,3);
                activeSince.set(Calendar.YEAR,2018);

                activeUntil = (Calendar) activeSince.clone();
                activeUntil.set(Calendar.DATE, activeSince.getActualMaximum(Calendar.DAY_OF_MONTH));

                nextInvoiceDate = (Calendar) activeSince.clone();
                nextInvoiceDate.add(Calendar.MONTH, 1);

                UserWS user  = FullCreativeUtil.createUser(SWAP_PLAN+ new Date().getTime());
                userId = user.getId();
                logger.debug(USER_CREATED_WITH_ID, user.getId());
                assertNotNull(USER_SHOULD_NOT_BE_NULL,user);
                user = api.getUserWS(userId);

                user.setNextInvoiceDate(nextInvoiceDate.getTime());
                api.updateUser(user);

                user = api.getUserWS(userId);
                logger.debug(USER_NEXT_INVOICE_DATE, TestConstants.DATE_FORMAT.format(user.getNextInvoiceDate()));

                logger.debug(PLAN_WS_GET_USAGE_POOL_IDS_0, planwith100Min.getUsagePoolIds()[0]);
            } catch(Exception e) {
                logger.error(e.getMessage());
            }
        }).test(env -> {
            try {
                UserWS user = api.getUserWS(userId);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DATE, 16);
                calendar.set(Calendar.MONTH,3);
                calendar.set(Calendar.YEAR,2018);

                logger.debug("Date: " + calendar.getTime());
                logger.debug(CREATING_PLAN_SUBSCRIPTION_ORDER);
                orderId = FullCreativeUtil.createOrder(planwith100Min, user.getUserId(), calendar.getTime(), null);
                OrderWS order  =  api.getOrder(orderId);
                logger.debug(SUBSCRIPTION_ORDER_CREATED_WITH_ID_AND_ACTIVE_SINCE_DATE, orderId,
                        TestConstants.DATE_FORMAT.format(order.getActiveSince()));

                assertNotNull(ORDER_ID_SHOULD_NOT_BE_NULL,orderId);

                logger.debug(CUSTOMER_USAGE_POOL_QUANTITY,
                        api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getInitialQuantityAsDecimal());
                CustomerUsagePoolWS customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal("50.0000"), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                order = api.getOrder(orderId);
                assertNotNull(GET_ORDER_SHOULD_NOT_BE_NULL, order);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, calendar.getTime() ,"0","0","325");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                OrderWS oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                String orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals("Order total quantity should be ", "275.00", orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan upgrade
                OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order,
                        planwith100Min.getItemId(),
                        planWith225Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_100_MIN_PLAN_TO_225_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                order = api.getOrder(orderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, order);
                assertEquals(SUBSCRIPTION_ORDER_AMOUNT_MUST_BE, _225_0000000000,order.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planWith225Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals(USAGE_POOL_QUANTITY_SHOULD_BE,new BigDecimal("112.5000"), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                order = api.getOrder(onetimeOrderId);

                logger.debug(ONE_TIME_ORDER + order);

                assertEquals("Order total quantity", "201.875", order.getTotal().substring(0, orderTotal.indexOf('.')+4));

                api.createInvoice(userId, false);

                onetimeOrderId = FullCreativeUtil.createOneTimeOrder(userId, calendar.getTime() ,"0","0","400");
                logger.debug(ONE_TIME_ORDER_CREATED_WITH_ID, onetimeOrderId);

                oneTimeUsageOrder = api.getOrder(onetimeOrderId);
                overageQuantity = FullCreativeUtil.getOrderTotalQuantity(oneTimeUsageOrder)
                        .subtract(FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(OVERAGE_QUANTITY_SHOULD_NOT_BE_NULL, overageQuantity);
                customerUsagePoolQuantity = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0].getQuantityAsDecimal();
                assertNotNull(FREE_USAGE_QUANTITY_SHOULD_NOT_NULL, FullCreativeUtil.getTotalFreeUsageQuantityByOrder(oneTimeUsageOrder));
                assertNotNull(AVAILABLE_QUANTITY_SHOULD_NOT_BE_NULL, customerUsagePoolQuantity);
                orderTotal = api.getOrder(onetimeOrderId).getTotal();
                logger.debug(ORDER_TOTAL_IS, orderTotal);

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, _380_00, orderTotal.substring(0, orderTotal.indexOf('.')+3));

                //swap plan upgrade
                orderChanges = api.calculateSwapPlanChanges(order,
                        planWith225Min.getItemId(),
                        planwith100Min.getItemId(),
                        SwapMethod.DIFF,
                        Util.truncateDate(order.getActiveSince()));
                logger.debug(SWAP_PLAN_FROM_225_MIN_PLAN_TO_100_MIN_PLAN);

                assertNotNull(SWAP_CHANGES_SHOULD_BE_CALCULATED, orderChanges);
                api.createUpdateOrder(order, orderChanges);

                logger.debug(SWAP_CHANGES_DONE);

                order = api.getOrder(onetimeOrderId);
                assertNotNull(ORDER_AFTER_SWAP_PLAN_SHOULD_NOT_BE_NULL, order);
                assertEquals(SUBSCRIPTION_ORDER_AMOUNT_MUST_BE, _380_0000000000,order.getTotal());

                logger.debug(SUBSCRIPTION_ORDER_S_SWAPPED_PLAN_ID, planWith225Min.getId());

                customerUsagePoolWS  = api.getCustomerUsagePoolsByCustomerId(user.getCustomerId())[0];

                assertEquals("Usage pool quantity",new BigDecimal("112.5000"), customerUsagePoolWS.
                        getInitialQuantityAsDecimal().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,BigDecimal.ROUND_HALF_UP));

                assertEquals(ORDER_TOTAL_QUANTITY_SHOULD_BE, _380_000, order.getTotal().substring(0, orderTotal.indexOf('.')+4));

            } catch(Exception e) {
                logger.error(e.getMessage());
                fail();
            } finally {
                logger.debug(DELETING_INVOICES);
                Arrays.stream(api.getUserInvoicesPage(userId, 10, 0))
                        .forEach(invoice -> api.deleteInvoice(invoice.getId()));

                logger.debug(DELETING_USER, userId);
                api.deleteUser(userId);
            }
        });
    }

    private Integer buildAndPersistCustomer(TestEnvironmentBuilder envBuilder, String userName,
            Integer accountTypeId, Date nextInvoiceDate, Integer nextInvoiceDay) {
        logger.debug("getting customer {}", userName);
        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(userName)
                .withAccountTypeId(accountTypeId)
                .addTimeToUsername(false)
                .withNextInvoiceDate(nextInvoiceDate)
                .withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), Integer.valueOf(1)))
                .build();
        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);
        return userWS.getId();
    }

    public Integer createOrder(PlanWS planWS, Integer userId, Date activeSinceDate, Date activeUntilDate, Integer orderTypeId)
            throws JbillingAPIException, IOException {
       JbillingAPI Jb_api = JbillingAPIFactory.getAPI();
       product8XXTollFreeId = TestConstants.PRODUCT_8XX_TOLL_FREE_ID;

       BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
       SearchCriteria criteria = new SearchCriteria();
       criteria.setMax(3);
       criteria.setOffset(3);
       criteria.setSort("id");
       criteria.setTotal(-1);
       criteria.setFilters(new BasicFilter[]{basicFilter});

       // get an available asset's id for plan subscription item (id = 320104)
       AssetSearchResult assetsResult320104 = Jb_api.findProductAssetsByStatus(product8XXTollFreeId, criteria);

       assertNotNull("No available asset found for product 320104", assetsResult320104);
       AssetWS[] available320104Assets = assetsResult320104.getObjects();
       assertTrue("No assets found for product 320104.", null != available320104Assets && available320104Assets.length != 0);
       Integer assetIdProduct320104_1 = available320104Assets[0].getId();
       logger.debug("Asset Available for product id 320104 = {}", assetIdProduct320104_1);
       Integer assetIdProduct320104_2 = available320104Assets[1].getId();
       logger.debug("Asset Available for product id 320104 = {}", assetIdProduct320104_2);

       logger.debug("Creating subscription order...");
       OrderWS order = new OrderWS();
       order.setUserId(userId);
       order.setActiveSince(activeSinceDate);
       order.setActiveUntil(activeUntilDate);
       order.setBillingTypeId(orderTypeId);
       order.setPeriod(2); // monthly
       order.setCurrencyId(1);
       order.setProrateFlag(true);

       OrderLineWS line = new OrderLineWS();
       line.setItemId(planWS.getItemId());
       line.setAmount("225.00");
       line.setPrice("225.00");
       line.setTypeId(Integer.valueOf(1));
       line.setDescription(planWS.getDescription());
       line.setQuantity("1");
       line.setUseItem(true);

       order.setOrderLines(new OrderLineWS[]{line});
       OrderChangeWS [] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
       for (OrderChangeWS ws : orderChanges) {
           if (ws.getItemId().intValue() == Integer.valueOf(planWS.getItemId())) {

               OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
               orderChangePlanItem.setItemId(product8XXTollFreeId);
               orderChangePlanItem.setId(0);
               orderChangePlanItem.setOptlock(0);
               orderChangePlanItem.setBundledQuantity(1);
               orderChangePlanItem.setDescription("DID-8XX");
               orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);

               orderChangePlanItem.setAssetIds(new int[]{assetIdProduct320104_2});

               ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
               ws.setStartDate(activeSinceDate);
           }
       }

       Integer order_Id =  Jb_api.createOrder(order, orderChanges);
       assertNotNull(ORDER_ID_SHOULD_NOT_BE_NULL,order_Id);

       return order_Id;
    }

    private void updateBillingProcessConfiguration(Date nextRunDate, int dueDays){

        BillingProcessConfigurationWS billingProcessConfiguration = api.getBillingProcessConfiguration();
        billingProcessConfiguration.setMaximumPeriods(100);
        billingProcessConfiguration.setNextRunDate(nextRunDate);
        billingProcessConfiguration.setPeriodUnitId(Constants.PERIOD_UNIT_MONTH);
        billingProcessConfiguration.setReviewStatus(Integer.valueOf(0));
        billingProcessConfiguration.setGenerateReport(Integer.valueOf(0));
        billingProcessConfiguration.setOnlyRecurring(Integer.valueOf(0));
        billingProcessConfiguration.setDueDateUnitId(3);
        billingProcessConfiguration.setDueDateValue(dueDays);
        api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
    }

    private void sleep(long miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            logger.error("Inside sleep:::::",e.getMessage());
        }
    }

    @AfterClass
    public void cleanUp() {
        logger.debug("cleaning up !");
        api.deletePlan(planwith100Min.getId());
        api.deletePlan(planWith225Min.getId());
        updateBillingProcessConfiguration(originalBillingProcessRunDate, 1);
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder = null;
    }

}
