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

package com.sapienter.jbilling.server.invoice;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.strategy.TeaserPricingStrategy.UseOrderPeriod;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * @author Emil
 */
@Test(groups = { "web-services", "invoice" }, testName = "invoice.WSTest")
public class WSTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer PRANCING_PONY = 1;
	private static Integer PRANCING_PONY_BASIC_ACCOUNT_TYPE = 1;
	private static Integer MORDOR_BASIC_ACCOUNT_TYPE = 2;
    private static Integer CURRENCY_ID;

	private static JbillingAPI api = null;
	private static JbillingAPI mordorApi = null;
	private static int ORDER_CHANGE_STATUS_APPLY_ID;
	private static int ORDER_PERIOD_MONTHLY_ID;
	private static int ORDER_PERIOD_ONE_TIME_ID = 1;
    private static final String TEST_ACCOUNT = "Account Type";
    private static final String TEST_CATEGORY = "MediatedUsageCategory";
    private static final String SUBSCRIPTION_PROD_01 = "testPlanSubscriptionItem_01";
    private static final String SUBSCRIPTION_PROD_02 = "testPlanSubscriptionItem_02";
    private static final Integer CC_PM_ID = 5;
    private static final String USER_01 = "Test-1-"+System.currentTimeMillis();
    private static final String ORDER_01 = "testSubScriptionOrderO1";
    private static final String ORDER_02 = "testSubScriptionOrderO2";
    private static final Integer NEXT_INVOICE_DAY = 1;
    private static final int ONE_TIME_ORDER_PERIOD = 1;
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;

    private static void sortInvoiceLines(InvoiceWS invoice) {
        Arrays.sort(invoice.getInvoiceLines(),
                (i1, i2) -> new BigDecimal(i1.getAmount()).compareTo(new BigDecimal(i2.getAmount())));
    }

	@BeforeClass
	public void setupClass() throws Exception {
		api = JbillingAPIFactory.getAPI();
		mordorApi = JbillingAPIFactory.getAPI("apiClientMordor");
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		ORDER_PERIOD_MONTHLY_ID = getOrCreateMonthlyOrderPeriod(api);
        CURRENCY_ID = PricingTestHelper.CURRENCY_USD;

        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            //Create account type
            buildAndPersistAccountType(envBuilder, api, TEST_ACCOUNT, CC_PM_ID);
            //Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, TEST_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

          //Create usage products
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_01, false, envBuilder.idForCode(TEST_CATEGORY), "10", true);

          //Create usage products
            buildAndPersistFlatProduct(envBuilder, api, SUBSCRIPTION_PROD_02, false, envBuilder.idForCode(TEST_CATEGORY), "0", true);
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(TEST_ACCOUNT));
            assertNotNull("Category Creation Failed", testEnvBuilder.idForCode(TEST_CATEGORY));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_01));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_02));
        });
	}

    @Test(enabled = false)
    public void test001Get() {
	    //create data in Mordor
	    UserWS user = buildUser(MORDOR_BASIC_ACCOUNT_TYPE);
	    user.setId(mordorApi.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(mordorApi.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), mordorApi.getCallerCompanyId());
	    item.setId(mordorApi.createItem(item));

	    InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setId(mordorApi.saveLegacyInvoice(invoice));

        // get
        // try getting one that doesn't belong to us
        try {
            logger.debug("Getting invalid invoice");
            api.getInvoiceWS(invoice.getId());
            fail("Invoice belongs to entity 2");
        } catch (Exception e) {
        }

	    // latest
	    // first, from a guy that is not mine
	    try {
		    api.getLatestInvoice(user.getId());
		    fail("User belongs to entity 2");
	    } catch (Exception e) {
	    }

	    // List of last
	    // first, from a guy that is not mine
	    try {
		    api.getLastInvoices(user.getId(), 5);
		    fail("User belongs to entity 2");
	    } catch (Exception e) {
	    }

	    //delete data from Mordor
	    mordorApi.deleteInvoice(invoice.getId());
	    mordorApi.deleteItem(item.getId());
	    mordorApi.deleteItemCategory(itemType.getId());
	    mordorApi.deleteUser(user.getId());

	    logger.debug("Done with Mordor");

	    //setup data in Prancing Pony
	    user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    item = buildItem(itemType.getId(), mordorApi.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setCreateDatetime(newDate(2006, 7, 26));
	    invoice.setId(api.saveLegacyInvoice(invoice));

	    InvoiceWS invoice2 = buildInvoice(user.getId(), item.getId());
	    invoice2.setCreateDatetime(newDate(2006, 8, 26));
	    invoice2.setId(api.saveLegacyInvoice(invoice2));

	    //start testing Prancing Pony
        logger.debug("Getting invoice");
        InvoiceWS retInvoice = api.getInvoiceWS(invoice.getId());
        assertNotNull("invoice not returned", retInvoice);
        assertEquals("invoice id", retInvoice.getId(), new Integer(invoice.getId()));
        logger.debug("Got Invoice With Id = {}", retInvoice.getId());

        logger.debug("Getting latest invoice");
        retInvoice = api.getLatestInvoice(user.getId());
        assertNotNull("invoice not returned", retInvoice);
        assertEquals("invoice's user id", user.getId(), retInvoice.getUserId().intValue());
        logger.debug("Got = {}", retInvoice);
        Integer lastInvoice = retInvoice.getId();

        logger.debug("Getting last 5 invoices");
        Integer invoices[] = api.getLastInvoices(user.getId(), 5);
        assertNotNull("invoice not returned", invoices);

        retInvoice = api.getInvoiceWS(invoices[0]);
        assertEquals("invoice's user id", user.getId(), retInvoice.getUserId().intValue());
        logger.debug("Got = {} invoices", invoices.length);
        for (int f = 0; f < invoices.length; f++) {
            logger.debug(" Invoice {}", (f + 1) + invoices[f]);
        }

        // now I want just the two latest
        logger.debug("Getting last 2 invoices");
        invoices = api.getLastInvoices(user.getId(), 2);
        assertNotNull("invoice not returned", invoices);
        retInvoice = api.getInvoiceWS(invoices[0]);
        assertEquals("invoice's user id", user.getId(), retInvoice.getUserId().intValue());
        assertEquals("invoice's has to be latest", lastInvoice, retInvoice.getId());
        assertEquals("there should be only 2", 2, invoices.length);

        // get some by date
        logger.debug("Getting by date (empty)");
        Integer invoices2[] = api.getInvoicesByDate("2000-01-01", "2005-01-01");
        // CXF returns null instead of empty arrays
        // assertNotNull("invoice not returned", invoices2);
        if (invoices2 != null) {
            assertTrue("array not empty", invoices2.length == 0);
        }

        logger.debug("Getting by date");
        invoices2 = api.getInvoicesByDate("2006-01-01", "2007-01-01");
        assertNotNull("invoice not returned", invoices2);
        assertFalse("array not empty", invoices2.length == 0);
        logger.debug("Got array with size: {}", invoices2.length);
        retInvoice = api.getInvoiceWS(invoices2[0]);
        assertNotNull("invoice not there", retInvoice);
        logger.debug("Got invoice {}", retInvoice);

	    //cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteInvoice(invoice2.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    @Test
    public void test002Delete() {
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		assertNotNull("Invoice ID should not be Null", invoice.getId());
        assertNotNull("Invoice should not be Null:", api.getInvoiceWS(invoice.getId()));

        api.deleteInvoice(invoice.getId());
        try {
            api.getInvoiceWS(invoice.getId());
            fail("Invoice should not have been deleted");
        } catch(Exception e) {
            //ok
        }

		//cleanup
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());

	    /*creates an invoice for Mordor company and tries to delete it with PrancingPony api */

	    user = buildUser(MORDOR_BASIC_ACCOUNT_TYPE);
	    user.setId(mordorApi.createUser(user));

	    itemType = buildItemType();
	    itemType.setId(mordorApi.createItemCategory(itemType));

	    item = buildItem(itemType.getId(), mordorApi.getCallerCompanyId());
	    item.setId(mordorApi.createItem(item));

	    invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setId(mordorApi.saveLegacyInvoice(invoice));

	    assertNotNull("Invoice ID should not be Null", invoice.getId());
	    assertNotNull("Invoice should not be Null:", mordorApi.getInvoiceWS(invoice.getId()));

        // try to delete an invoice that is not mine
        try {
            api.deleteInvoice(invoice.getId());
            fail("Not my invoice. It should not have been deleted");
        } catch(Exception e) {
            //ok
        }

	    //cleanup Mordor data
	    mordorApi.deleteInvoice(invoice.getId());
	    mordorApi.deleteItem(item.getId());
	    mordorApi.deleteItemCategory(itemType.getId());
        try {
	        mordorApi.deleteUser(user.getId());
        } catch (SessionInternalError e) {
            assertTrue(e.getMessage().contains("Notification not found for sending deleted user notification"));
        }
    }

    @Test
    public void test003CreateInvoice() {
	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        final Integer userId = api.createUser(user);

        //Update Next invoice date and billing cycle period.
        user = api.getUserWS(userId);
	    user.setPassword(null);
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        user.setMainSubscription(createUserMainSubscription(day));
        user.setPassword(null);
        
        api.updateUser(user);
        user = api.getUserWS(userId);
	    user.setPassword(null);
        user.setNextInvoiceDate(new Date());
        user.setPassword(null);
        api.updateUser(user);

        // setup order
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setProrateFlag(false);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[] { line });


        //  Test invoicing of one-time and recurring orders


        // create 1st order
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create 2nd order
        line.setPrice(new BigDecimal("20.00"));
        line.setAmount(new BigDecimal("20.00"));
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create invoice
        Integer[] invoices = api.createInvoice(userId, false);

        assertEquals("Number of invoices returned", 1, invoices.length);
        InvoiceWS invoice = api.getInvoiceWS(invoices[0]);

        assertNull("Invoice is not delegated.", invoice.getDelegatedInvoiceId());
        assertEquals("Invoice does not have a carried balance.", BigDecimal.ZERO, invoice.getCarriedBalanceAsDecimal());

        Integer[] invoicedOrderIds = invoice.getOrders();
        assertEquals("Number of orders invoiced", 2, invoicedOrderIds.length);
        Arrays.sort(invoicedOrderIds);
        assertTrue("Order 1 invoiced", arrayContains(invoicedOrderIds, orderId1));
        assertTrue("Order 2 invoiced", arrayContains(invoicedOrderIds, orderId2));
        assertEquals("Total is 30.0", new BigDecimal("30.00"), invoice.getTotalAsDecimal());

        // clean up
        api.deleteInvoice(invoices[0]);
        api.deleteOrder(orderId1);
        api.deleteOrder(orderId2);

        //  Test only recurring order can generate invoice.

        // one-time order
        line.setPrice(new BigDecimal("2.00"));
        line.setAmount(new BigDecimal("2.00"));
        orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // try to create invoice, but none should be returned
        invoices = api.createInvoice(userId, true);

        // Note: CXF returns null for empty array
        if (invoices != null) {
            assertEquals("Number of invoices returned", 0, invoices.length);
        }

        // recurring order
        order.setPeriod(ORDER_PERIOD_MONTHLY_ID); // monthly
        line.setPrice(new BigDecimal("3.00"));
        line.setAmount(new BigDecimal("3.00"));
        orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create invoice
        invoices = api.createInvoice(userId, true);

        assertEquals("Number of invoices returned", 1, invoices.length);
        invoice = api.getInvoiceWS(invoices[0]);
        invoicedOrderIds = invoice.getOrders();
        assertEquals("Number of orders invoiced", 2, invoicedOrderIds.length);
        Arrays.sort(invoicedOrderIds);
        assertTrue("Order 1 invoiced", arrayContains(invoicedOrderIds, orderId1));
        assertTrue("Order 2 invoiced", arrayContains(invoicedOrderIds, orderId2));
        assertEquals("Total is 5.0", new BigDecimal("5.00"), invoice.getTotalAsDecimal());

        // clean up
        api.deleteInvoice(invoices[0]);
        api.deleteOrder(orderId1);
        api.deleteOrder(orderId2);

	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(userId);
    }

    public boolean arrayContains(Integer[] ids, Integer idExpected) {
        for (Integer id: ids) {
            if (id.equals(idExpected))
                return true;
        }
        return false;
    }

    @Test
    public void test004CreateInvoiceFromOrder() {
	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    final Integer userId = api.createUser(user);
	    
	    updateCustomerNextInvoiceDate(userId);
	    
        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Order 2 created", orderId2);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);        

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total is $10.00", new BigDecimal("10.00"), invoice.getTotalAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // add second order to invoice
        Integer invoiceId2 = api.createInvoiceFromOrder(orderId2, invoiceId);
        assertEquals("Order added to the same invoice", invoiceId, invoiceId2);

        invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Still only 1 invoice generated", 1, invoiceIds.length);

        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total is $20.00", new BigDecimal("20.00"), invoice.getTotalAsDecimal());
        assertEquals("2 orders invoiced", 2, invoice.getOrders().length);

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId1);
        api.deleteOrder(orderId2);

	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(userId);
    }

    @Test
    public void test005CreateInvoiceSecurity() {
	    //create mordor user
	    UserWS user = buildUser(MORDOR_BASIC_ACCOUNT_TYPE);
	    user.setId(mordorApi.createUser(user));

        try {
            api.createInvoice(user.getId(), false);
            fail("User belongs to entity 2");
        } catch (SecurityException | SessionInternalError e) {
        }

	    //cleanup
        try {
            mordorApi.deleteUser(user.getUserId());
        } catch (Exception e) {
        }
    }


    /**
     * Tests that when a past due invoice is processed it will generate a new invoice for the
     * current period that contains all previously un-paid balances as the carried balance.
     *
     * Invoices that have been carried still show the original balance for reporting/paper-trail
     * purposes, but will not be re-processed by the system as part of the normal billing process.
     *
     * @throws Exception
     */
    @Test
    public void test006CreateWithCarryOver() {
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    InvoiceWS overDueInvoice = buildInvoice(user.getId(), item.getId());
	    overDueInvoice.setDueDate(newDate(2007, 8, 26));
	    overDueInvoice.setCreateDatetime(newDate(2007, 7, 26));
	    overDueInvoice.setCreateTimeStamp(newDate(2007, 7, 26));
	    overDueInvoice.setId(api.saveLegacyInvoice(overDueInvoice));

        final Integer USER_ID = user.getId();          // user has one past-due invoice to be carried forward
        final Integer OVERDUE_INVOICE_ID = overDueInvoice.getId();  // holds a $20 balance

        //Update Next invoice date and billing cycle period.
        user = api.getUserWS(USER_ID);
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        user.setMainSubscription(createUserMainSubscription(day));
        user.setPassword(null);
        api.updateUser(user);
        user = api.getUserWS(USER_ID);
	    user.setPassword(null);
        user.setNextInvoiceDate(new Date());
        user.setPassword(null);
        api.updateUser(user);
        
        // new order with a single line item
        OrderWS order = new OrderWS();
        order.setUserId(USER_ID);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setProrateFlag(false);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create order
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // create invoice
        Integer invoiceId = api.createInvoice(USER_ID, false)[0];

        // validate that the overdue invoice has been carried forward to the newly created invoice
        InvoiceWS overdue = api.getInvoiceWS(OVERDUE_INVOICE_ID);

        assertEquals("Status updated to 'unpaid and carried'",
                     Constants.INVOICE_STATUS_UNPAID_AND_CARRIED, overdue.getStatusId());
        assertEquals("Carried invoice will not be re-processed",
                     0, overdue.getToProcess().intValue());
        assertEquals("Overdue invoice holds original balance",
                     new BigDecimal("20.00"), overdue.getBalanceAsDecimal());

        assertEquals("Overdue invoice delegated to the newly created invoice",
                     invoiceId, overdue.getDelegatedInvoiceId());

        // validate that the newly created invoice contains the carried balance
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertEquals("New invoice balance is equal to the current period charges",
                     new BigDecimal("10.00"), invoice.getBalanceAsDecimal());
        assertEquals("New invoice holds the carried balance equal to the old invoice balance",
                     overdue.getBalanceAsDecimal(), invoice.getCarriedBalanceAsDecimal());
        assertEquals("New invoice total is equal to the current charges plus the carried total",
                     new BigDecimal("30.00"), invoice.getTotalAsDecimal());

	    //cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteInvoice(overDueInvoice.getId());
	    api.deleteOrder(orderId);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    @Test
    public void test007GetUserInvoicesByDate() {
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    InvoiceWS invoiceOne = buildInvoice(user.getId(), item.getId());
	    invoiceOne.setCreateDatetime(newDate(2006, 7, 26));
	    invoiceOne.setId(api.saveLegacyInvoice(invoiceOne));

	    InvoiceWS invoiceTwo = buildInvoice(user.getId(), item.getId());
	    invoiceTwo.setCreateDatetime(newDate(2006, 7, 27));
	    invoiceTwo.setId(api.saveLegacyInvoice(invoiceTwo));

	    InvoiceWS invoiceThree = buildInvoice(user.getId(), item.getId());
	    invoiceThree.setCreateDatetime(newDate(2006, 7, 28));
	    invoiceThree.setId(api.saveLegacyInvoice(invoiceThree));

        // invoice dates: 2006-07-26
        // select the week
        Integer[] result = api.getUserInvoicesByDate(user.getId(), "2006-07-23", "2006-07-29");
        // note: invoice 1 gets deleted
        assertEquals("Number of invoices returned", 3, result.length);
        assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
        assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());
        assertEquals("Invoice One",   invoiceOne.getId().intValue(),    result[2].intValue());

        // test since date inclusive
        result = api.getUserInvoicesByDate(user.getId(), "2006-07-26", "2006-07-29");
        assertEquals("Number of invoices returned", 3, result.length);
	    assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
	    assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());
	    assertEquals("Invoice One",   invoiceOne.getId().intValue(),    result[2].intValue());

        // test until date inclusive
        result = api.getUserInvoicesByDate(user.getId(), "2006-07-23", "2006-07-28");
        assertEquals("Number of invoices returned", 3, result.length);
	    assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
	    assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());
	    assertEquals("Invoice One",   invoiceOne.getId().intValue(),    result[2].intValue());

	    result = api.getUserInvoicesByDate(user.getId(), "2006-07-27", "2006-07-28");
	    assertEquals("Number of invoices returned", 2, result.length);
	    assertEquals("Invoice Three", invoiceThree.getId().intValue(),  result[0].intValue());
	    assertEquals("Invoice Two",   invoiceTwo.getId().intValue(),    result[1].intValue());

        // test date with no invoices
        result = api.getUserInvoicesByDate(user.getId(), "2005-07-23", "2005-07-29");
        // Note: CXF returns null for empty array
        if (result != null) {
            assertEquals("Number of invoices returned", 0, result.length);
        }

	    //clean up
	    api.deleteInvoice(invoiceThree.getId());
	    api.deleteInvoice(invoiceTwo.getId());
	    api.deleteInvoice(invoiceOne.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    @Test
    public void test008GetTotalAsDecimal() {
	    //prepare
        List<Integer> invoiceIds = new ArrayList<Integer>();
        List<Integer> orderIds = new ArrayList<Integer>();
	    List<Integer> itemIds = new ArrayList<Integer>();
	    UserWS user = null;
	    ItemTypeWS itemType = null;

        itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        itemIds.add(api.createItem(buildItem(itemType.getId(), api.getCallerCompanyId())));
        itemIds.add(api.createItem(buildItem(itemType.getId(), api.getCallerCompanyId())));
        itemIds.add(api.createItem(buildItem(itemType.getId(), api.getCallerCompanyId())));

        user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        user.setId(api.createUser(user));
        updateCustomerNextInvoiceDate(user.getId());

        // test BigDecimal behavior
        assertFalse(new BigDecimal("1.1").equals(new BigDecimal("1.10")));
        assertTrue(new BigDecimal("1.1").compareTo(new BigDecimal("1.10")) == 0);

        OrderWS order = createMockOrder(user.getId(), itemIds, new BigDecimal("0.32"));

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        orderIds.add(api.createOrder(order, orderChanges));
        order = createMockOrder(user.getId(), itemIds, new BigDecimal("0.32"));

        orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        orderIds.add(api.createOrder(order, orderChanges));

        invoiceIds.addAll(Arrays.asList(api.createInvoice(user.getId(), false)));
        assertEquals("There should be one invoice created", 1, invoiceIds.size());

        InvoiceWS invoice = api.getInvoiceWS(invoiceIds.get(0));
        assertEquals("1.9200000000", invoice.getTotal());
        assertEquals(new BigDecimal("1.920"), invoice.getTotalAsDecimal());

	    //cleanup
        for (Integer integer : invoiceIds) {
            api.deleteInvoice(integer);
        }
        logger.debug("Successfully deleted invoices: {}", invoiceIds.size());
        for (Integer integer : orderIds) {
            api.deleteOrder(integer);
        }
        logger.debug("Successfully deleted orders: {}", orderIds.size());
        for(Integer itemId : itemIds){
            api.deleteItem(itemId);
        }
        logger.debug("Successfully deleted items: {}", itemIds.size());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());
    }

    @Test
    public void test009GetPaperInvoicePDF() {
	    //setup
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
	    invoice.setId(api.saveLegacyInvoice(invoice));

	    //test
        Integer[] invoiceIds = api.getLastInvoices(user.getId(), 1);
        assertEquals("Invoice found for user", 1, invoiceIds.length);

        byte[] pdf = api.getPaperInvoicePDF(invoiceIds[0]);
        assertTrue("PDF invoice bytes returned", pdf.length > 0);

	    //cleanup
	    api.deleteInvoice(invoice.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }
    
    @Test
    public void test010OverDueInvoices() {
	    final Integer ACTIVE = Integer.valueOf(1);
	    final Integer OVERDUE = Integer.valueOf(2);

	    Date date = new Date();
	    date.setDate(date.getDate()+80);

	    {
	    UserWS userOne = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    userOne.setId(api.createUser(userOne));
	    
		userOne = api.getUserWS(userOne.getId());
		updateCustomerNextInvoiceDate(userOne.getId());

        //setting user status as ACTIVE
		userOne.setStatusId(ACTIVE);
        userOne.setPassword(null);
        api.updateUser(userOne);
        logger.debug("user initial status : {}", api.getUserWS(userOne.getId()).getStatus());

        //creating order having balance less than min balance to ignore ageing i.e, 0.00
        OrderWS order = setUpOrder(userOne.getId(), new BigDecimal("-2.123450"));
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order 1 created", orderId1);
        logger.debug("Order 1 created {}", orderId1);
        Integer invoiceId1 = api.createInvoiceFromOrder(orderId1, null);
        assertNotNull("Invoice created", invoiceId1);
        logger.debug("Invoice created {}", invoiceId1);
        api.triggerAgeing(date);
        logger.debug("user status : {}", api.getUserWS(userOne.getId()).getStatus());
        //checking if user status is ACTIVE
        assertEquals("Expected ACTIVE user", ACTIVE, api.getUserWS(userOne.getId()).getStatusId());

		//cleanup
	    api.deleteInvoice(invoiceId1);
	    api.deleteOrder(orderId1);
	    updateCustomerStatusToActive(userOne.getId(), api);
	    api.deleteUser(userOne.getId());
	    }

	    {
		UserWS userTwo = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
		userTwo.setId(api.createUser(userTwo));
		
		updateCustomerNextInvoiceDate(userTwo.getId());
		userTwo = api.getUserWS(userTwo.getId());

        //setting user status as ACTIVE
		userTwo.setStatusId(ACTIVE);
        api.updateUser(userTwo);
        logger.debug("user initial status : {}", api.getUserWS(userTwo.getId()).getStatus());

        //creating order having balance equal to  min balalance to ignore ageing i.e, 0.00
        OrderWS order2 = setUpOrder(userTwo.getId(), new BigDecimal("0.00"));
        Integer orderId2 = api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order 2 created", orderId2);
        logger.debug("Order 2 created {}", orderId2);
        Integer invoiceId2 = api.createInvoiceFromOrder(orderId2, null);
        assertNotNull("Invoice created", invoiceId2);
        logger.debug("Invoice created {}", invoiceId2);
        api.triggerAgeing(date);
        logger.debug("user status : {}", api.getUserWS(userTwo.getId()).getStatus());
        //checking if user status is ACTIVE
        assertEquals("Expected ACTIVE user", ACTIVE, api.getUserWS(userTwo.getId()).getStatusId());

	    api.deleteInvoice(invoiceId2);
	    api.deleteOrder(orderId2);
	    updateCustomerStatusToActive(userTwo.getId(), api);
	    api.deleteUser(userTwo.getId());
	    }

	    {
		UserWS userThree = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
		userThree.setId(api.createUser(userThree));

		updateCustomerNextInvoiceDate(userThree.getId());
		userThree = api.getUserWS(userThree.getId());

        //setting user status as ACTIVE
		userThree.setStatusId(ACTIVE);
        api.updateUser(userThree);
        logger.debug("user status again changed to : {}", api.getUserWS(userThree.getId()).getStatus());

        //creating order having balance more than min balalance to ignore ageing i.e, 0.00
        OrderWS order3 = setUpOrder(userThree.getId(), new BigDecimal("21.00"));
        Integer orderId3 = api.createOrder(order3, OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID));
        assertNotNull("Order 3 created", orderId3);
        logger.debug("Order 3 created {}", orderId3);
        Integer invoiceId3 = api.createInvoiceFromOrder(orderId3, null);
        assertNotNull("Invoice created", invoiceId3);
        logger.debug("Invoice created {}", invoiceId3);
        api.triggerAgeing(date);
        logger.debug("user status : {}", api.getUserWS(userThree.getId()).getStatus());
        //checking if user status is OVERDUE
        assertEquals("Expected OVERDUE user", OVERDUE, api.getUserWS(userThree.getId()).getStatusId());

		//cleanup
	    api.deleteInvoice(invoiceId3);
	    api.deleteOrder(orderId3);
	    updateCustomerStatusToActive(userThree.getId(), api);
	    api.deleteUser(userThree.getId());
	    }
    }

    @Test
    public void test011CreditGeneratedFromNegativeInvoice() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
        Integer userId = user.getUserId();
        updateCustomerNextInvoiceDate(userId);
	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("-100.00"));
        line.setAmount(new BigDecimal("-100.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $0.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        //Validate that a credit note should have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteIds[0]);
        assertEquals("The credit note amount should be $100.00", new BigDecimal("100.00"), creditNoteWS.getAmountAsDecimal());

        // cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId1);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
        api.deleteUser(userId);
    }

    @Test
    public void test012SaveLegacyInvoice() throws Exception {
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        InvoiceWS invoiceWS = new InvoiceWS();
        invoiceWS.setUserId(userId);
        invoiceWS.setNumber("800");
        invoiceWS.setTotal("4500");
        invoiceWS.setToProcess(1);
        invoiceWS.setBalance("4500");
        invoiceWS.setCurrencyId(1);
        invoiceWS.setDueDate(new Date());
        invoiceWS.setPaymentAttempts(2);
        invoiceWS.setInProcessPayment(1);
        invoiceWS.setCarriedBalance("0");

        InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
        invoiceLineDTO.setAmount("4500");
        invoiceLineDTO.setDescription("line desc");
        invoiceLineDTO.setItemId(1);
        invoiceLineDTO.setPercentage(1);
        invoiceLineDTO.setPrice("4500");
        invoiceLineDTO.setQuantity("1");
        invoiceLineDTO.setSourceUserId(userId);

        invoiceWS.setInvoiceLines(new InvoiceLineDTO[] {invoiceLineDTO});

        invoiceWS.setId(api.saveLegacyInvoice(invoiceWS));

        InvoiceWS lastInvoiceWS = api.getLatestInvoice(userId);

        assertNotNull(lastInvoiceWS);
        assertTrue(lastInvoiceWS.getNumber().equals(invoiceWS.getNumber()));
        assertEquals(lastInvoiceWS.getTotalAsDecimal(), invoiceWS.getTotalAsDecimal());
        assertEquals(lastInvoiceWS.getBalanceAsDecimal(), invoiceWS.getBalanceAsDecimal());
        assertTrue(lastInvoiceWS.getCurrencyId().equals(invoiceWS.getCurrencyId()));
        assertTrue("This invoice is migrated from legacy system.".equals(lastInvoiceWS.getCustomerNotes()));

        InvoiceLineDTO lastInvoiceLineDTO = lastInvoiceWS.getInvoiceLines()[0];
        assertNotNull(lastInvoiceLineDTO);
        assertEquals(lastInvoiceLineDTO.getAmountAsDecimal(), invoiceLineDTO.getAmountAsDecimal());
        assertTrue(lastInvoiceLineDTO.getDescription().equals(invoiceLineDTO.getDescription()));
        assertTrue(lastInvoiceLineDTO.getItemId().equals(invoiceLineDTO.getItemId()));
        assertEquals(lastInvoiceLineDTO.getPriceAsDecimal(), invoiceLineDTO.getPriceAsDecimal());
        assertEquals(lastInvoiceLineDTO.getQuantityAsDecimal(), invoiceLineDTO.getQuantityAsDecimal());
        assertTrue(lastInvoiceLineDTO.getSourceUserId().equals(invoiceLineDTO.getSourceUserId()));

	    api.deleteInvoice(invoiceWS.getId());
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(user.getId());
    }

    @Test
    public void test013OrderWithTeaserPricing() throws Exception {
        BillingProcessConfigurationWS process = api.getBillingProcessConfiguration();
        process.setMaximumPeriods(99);
        api.createUpdateBillingProcessConfiguration(process);

        UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        user.setId(api.createUser(user));
        Integer userId = user.getUserId();

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TEASER_PRICING.name(), BigDecimal.ONE, CURRENCY_ID);
        priceModel.addAttribute("period", null);
        priceModel.addAttribute("use_order_period", UseOrderPeriod.YES.name());
        priceModel.addAttribute("1", "50");
        priceModel.addAttribute("3", "75");
        priceModel.addAttribute("5", "100");

        ItemDTOEx item = PricingTestHelper.buildItem("TEST-ITEM-SEL-" + System.currentTimeMillis(),
                                                     "Item selector test",
                                                     itemType.getId());

        item.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        item.setId(api.createItem(item));
        assertNotNull("Item created", item.getId());

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_MONTHLY_ID);
        order.setCurrencyId(CURRENCY_ID);
        order.setActiveSince(DateConvertUtils.asUtilDate(LocalDate.now().minusMonths(5)));

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        String itemPrice = item.getDefaultPrices().get(CommonConstants.EPOCH_DATE).getAttributes().get("1");
        line.setPrice(itemPrice);
        line.setAmount(itemPrice);
        line.setUseItem(true);

        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        order = api.getOrder(api.createOrder(order, orderChanges));
        assertNotNull("Order created", item.getId());
        InvoiceWS invoiceWS = api.getInvoiceWS(api.createInvoiceFromOrder(order.getId(), null));
        assertNotNull("Invoice created", invoiceWS.getId());
        sortInvoiceLines(invoiceWS);
        assertEquals("Invoices Lines not match", invoiceWS.getInvoiceLines().length, 6);
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[0].getAmount(), "50.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[1].getAmount(), "50.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[2].getAmount(), "75.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[3].getAmount(), "75.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[4].getAmount(), "100.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[5].getAmount(), "100.0000000000");

        /**
         * Delete invoice
         * Delete order
         * Delete item
         * Delete item type
         * Delete user
         **/
        api.deleteInvoice(invoiceWS.getId());
        api.deleteOrder(order.getId());
        api.deleteItem(item.getId());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(userId);
    }

    @Test
    public void test014OrderWithTeaserPricingAndFixOrderPeriod() throws Exception {
        UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        user.setId(api.createUser(user));
        Integer userId = user.getUserId();

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        OrderPeriodWS quarterly = new OrderPeriodWS();
        quarterly.setEntityId(PRANCING_PONY);
        quarterly.setPeriodUnitId(PeriodUnit.MONTHLY.getId());
        quarterly.setValue(3);
        quarterly.setDescriptions(Collections.singletonList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "INV:QUARTERLY")));
        quarterly = api.getOrderPeriodWS(api.createOrderPeriod(quarterly));
        assertNotNull("Order Period created", quarterly.getId());

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TEASER_PRICING.name(), BigDecimal.ONE, CURRENCY_ID);
        priceModel.addAttribute("period", quarterly.getId().toString());
        priceModel.addAttribute("use_order_period", UseOrderPeriod.NO.name());
        priceModel.addAttribute("1", "50");
        priceModel.addAttribute("2", "75");

        ItemDTOEx item = PricingTestHelper.buildItem("TEST-ITEM-SEL-" + System.currentTimeMillis(),
                                                     "Item selector test",
                                                     itemType.getId());

        item.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        item.setId(api.createItem(item));
        assertNotNull("Item created", item.getId());

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_MONTHLY_ID);
        order.setCurrencyId(CURRENCY_ID);
        order.setActiveSince(DateConvertUtils.asUtilDate(LocalDate.now().minusMonths(4)));

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        String itemPrice = item.getDefaultPrices().get(CommonConstants.EPOCH_DATE).getAttributes().get("1");
        line.setPrice(itemPrice);
        line.setAmount(itemPrice);
        line.setUseItem(true);

        order.setOrderLines(new OrderLineWS[] { line });

        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS change: orderChanges) {
            change.setStartDate(order.getActiveSince());
        }

        order = api.getOrder(api.createOrder(order, orderChanges));
        assertNotNull("Order created", item.getId());
        InvoiceWS invoiceWS = api.getInvoiceWS(api.createInvoiceFromOrder(order.getId(), null));
        assertNotNull("Invoice created", invoiceWS.getId());
        sortInvoiceLines(invoiceWS);
        assertEquals("Invoices Lines not match", invoiceWS.getInvoiceLines().length, 5);
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[0].getAmount(), "50.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[1].getAmount(), "50.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[2].getAmount(), "50.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[3].getAmount(), "75.0000000000");
        assertEquals("Invoices Lines Amount not match", invoiceWS.getInvoiceLines()[4].getAmount(), "75.0000000000");

        /**
         * Delete invoice
         * Delete order
         * Delete item
         * Delete item type
         * Delete user
         **/
        api.deleteInvoice(invoiceWS.getId());
        api.deleteOrder(order.getId());
        api.deleteItem(item.getId());
        api.deleteItemCategory(itemType.getId());
        api.deleteOrderPeriod(quarterly.getId());
        api.deleteUser(userId);
    }

    @Test
    public void test015InvoiceStutusPaidForZeroBalance() throws Exception {
        TestEnvironment environment = testBuilder.getTestEnvironment();
        InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
        try {
            testBuilder.given(envBuilder -> {
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 1);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);

                scenario01.createUser(USER_01,environment.idForCode(TEST_ACCOUNT),nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                //Creating subscription order on 1st ofJan 2018
                .createOrder(ORDER_01, activeSince.getTime(),null, ONE_TIME_ORDER_PERIOD,Constants.ORDER_BILLING_POST_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, null, false)
                        .generateInvoice(nextInvoiceDate.getTime(), false);
            }).test((testEnv, envBuilder) -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                InvoiceWS invoice = api.getLatestInvoice(envBuilder.idForCode(USER_01));

                assertEquals("Invoice status should be unpaid", invoice.getStatusId(),Constants.INVOICE_STATUS_UNPAID);
                assertEquals("Invoice balance should be 10", invoice.getBalanceAsDecimal().compareTo(new BigDecimal("10")),0);
                assertEquals("Invoice total should be 10", invoice.getTotalAsDecimal().compareTo(new BigDecimal("10")),0);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 1);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 2);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Integer lastInvoiceId = invoice.getId();

                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_02), BigDecimal.ONE);
                scenario01.createOrder(ORDER_02, activeSince.getTime(),null, ONE_TIME_ORDER_PERIOD,Constants.ORDER_BILLING_POST_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, null, false)
                        .generateInvoice(nextInvoiceDate.getTime(), false);

                invoice = api.getLatestInvoice(envBuilder.idForCode(USER_01));

                assertEquals("Invoice status should be paid", invoice.getStatusId(),Constants.INVOICE_STATUS_PAID);
                assertEquals("Invoice balance should be 0", invoice.getBalanceAsDecimal().compareTo(new BigDecimal("0")),0);
                assertEquals("Invoice total should be 10", invoice.getTotalAsDecimal().compareTo(new BigDecimal("10")),0);

                invoice = api.getInvoiceWS(lastInvoiceId);

                assertEquals("Invoice status should be unpaid and carried", invoice.getStatusId(),Constants.INVOICE_STATUS_UNPAID_AND_CARRIED);
                assertEquals("Invoice total should be 10", invoice.getTotalAsDecimal().compareTo(new BigDecimal("10")),0);
            });
        } finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(USER_01), 10, 0))
            .forEach(invoice -> api.deleteInvoice(invoice.getId()));
            api.deleteUser(testBuilder.getTestEnvironment().idForCode(USER_01));
        }
    }

	private OrderWS createMockOrder(int userId, List<Integer> items, BigDecimal linePrice) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
		order.setCurrencyId(1);
		order.setActiveSince(new Date());
		order.setProrateFlag(Boolean.FALSE);

		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(items.size());
		for (int i = 0; i < items.size(); i++){
			OrderLineWS nextLine = new OrderLineWS();
			nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(items.get(i));
			nextLine.setQuantity(1);
			nextLine.setPrice(linePrice);
			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));

			lines.add(nextLine);
		}
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

    private OrderWS setUpOrder(Integer userId,BigDecimal price){
        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        Date date = new Date();
        date.setDate(date.getDate()-20);
        order.setActiveSince(date);

        //setup orderLines
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(1);
        line.setQuantity(1);
        line.setPrice(price);
        line.setAmount(price);

        order.setOrderLines(new OrderLineWS[] { line });

        return order;
    }

    private static UserWS buildUser(Integer accountTypeId) {
		UserWS newUser = new UserWS();
		newUser.setUserId(0);
		newUser.setUserName("testInvoiceUser-" + System.currentTimeMillis());
		newUser.setPassword("Admin123@");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(accountTypeId);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(Integer.valueOf(1));
		
		newUser.setInvoiceChild(false);

		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("contact.email");
		metaField3.setValue(newUser.getUserName() + "@shire.com");
		metaField3.setGroupId(accountTypeId);

		MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("contact.first.name");
		metaField4.setValue("Frodo");
		metaField4.setGroupId(accountTypeId);

		MetaFieldValueWS metaField5 = new MetaFieldValueWS();
		metaField5.setFieldName("contact.last.name");
		metaField5.setValue("Baggins");
		metaField5.setGroupId(accountTypeId);

		newUser.setMetaFields(new MetaFieldValueWS[] { metaField3, metaField4, metaField5 });
		return newUser;
	}

	private InvoiceWS buildInvoice(Integer userId, Integer itemId) {
		InvoiceWS invoice = new InvoiceWS();
		invoice.setUserId(userId);
		invoice.setNumber("800");
		invoice.setTotal("20");
		invoice.setToProcess(1);
		invoice.setBalance("20");
		invoice.setCurrencyId(1);
		invoice.setDueDate(new Date());
		invoice.setPaymentAttempts(1);
		invoice.setInProcessPayment(1);
		invoice.setCarriedBalance("0");

		InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
		invoiceLineDTO.setAmount("20");
		invoiceLineDTO.setDescription("line desc");
		invoiceLineDTO.setItemId(itemId);
		invoiceLineDTO.setPercentage(1);
		invoiceLineDTO.setPrice("20");
		invoiceLineDTO.setQuantity("1");
		invoiceLineDTO.setSourceUserId(userId);

		invoice.setInvoiceLines(new InvoiceLineDTO[]{invoiceLineDTO});
		return invoice;
	}

	private ItemTypeWS buildItemType() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Invoice, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(1);//items
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId){
		ItemDTOEx item = new ItemDTOEx();
		long millis = System.currentTimeMillis();
		String name = String.valueOf(millis) + new Random().nextInt(10000);
		item.setDescription("Invoice, Product:" + name);
		item.setPriceModelCompanyId(priceModelCompanyId);
		item.setPrice(new BigDecimal("10"));
		item.setNumber("INV-PRD-"+name);
		item.setAssetManagementEnabled(0);
		Integer typeIds[] = new Integer[] {itemTypeId};
		item.setTypes(typeIds);
		return item;
	}

	private static MainSubscriptionWS createUserMainSubscription(int day) {
    	MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
    	mainSubscription.setPeriodId(ORDER_PERIOD_MONTHLY_ID); //monthly
    	mainSubscription.setNextInvoiceDayOfPeriod(day); // 1st of the month
    	return mainSubscription;
    }

	private static Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
		OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
		for (OrderChangeStatusWS status : statuses) {
			if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
				return status.getId();
			}
		}
		//there is no APPLY status in db so create one
		OrderChangeStatusWS apply = new OrderChangeStatusWS();
		String status1Name = "APPLY: " + System.currentTimeMillis();
		OrderChangeStatusWS status1 = new OrderChangeStatusWS();
		status1.setApplyToOrder(ApplyToOrder.YES);
		status1.setDeleted(0);
		status1.setOrder(1);
		status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

	private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
		OrderPeriodWS[] periods = api.getOrderPeriods();
		for(OrderPeriodWS period : periods){
			if(1 == period.getValue().intValue() &&
					PeriodUnitDTO.MONTH == period.getPeriodUnitId().intValue()){
				return period.getId();
			}
		}
		//there is no monthly order period so create one
		OrderPeriodWS monthly = new OrderPeriodWS();
		monthly.setEntityId(api.getCallerCompanyId());
		monthly.setPeriodUnitId(1);//monthly
		monthly.setValue(1);
		monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "INV:MONTHLY")));
		return api.createOrderPeriod(monthly);
	}

	private static Date newDate(int year, int month, int day){
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month-1, day);
		return cal.getTime();
	}
	
	private UserWS updateCustomerNextInvoiceDate(Integer userId) {
        logger.debug("Updating Customer Next Invoice Date for user id {}", userId);
        UserWS user = api.getUserWS(userId);
        logger.debug("Old Next Invoice Date is {}", user.getNextInvoiceDate());
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        logger.debug("New Next Invoice Date is {}", nextInvoiceDate.getTime());
        api.updateUser(user);
        return api.getUserWS(userId);
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi()));
    }

    public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    public Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    public Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, String flatPrice, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(flatPrice)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }
}
