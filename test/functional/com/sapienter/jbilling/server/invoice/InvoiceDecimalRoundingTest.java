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

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;


/**
 * Invoice Decimal Rounding Test
 * @author jBilling
 */
@Test(groups = { "web-services", "invoicerounding" }, testName = "invoice.InvoiceDecimalRoundingTest")
public class InvoiceDecimalRoundingTest {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceDecimalRoundingTest.class);
	private static Integer PRANCING_PONY_BASIC_ACCOUNT_TYPE = Integer.valueOf(1);

	private static JbillingAPI api = null;
	private static int ORDER_CHANGE_STATUS_APPLY_ID;
	private static int ORDER_PERIOD_MONTHLY_ID;
	private static int ORDER_PERIOD_ONE_TIME_ID = 1;

	@BeforeClass
	public void setupClass() throws Exception {
		api = JbillingAPIFactory.getAPI();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api).intValue();
		ORDER_PERIOD_MONTHLY_ID = getOrCreateMonthlyOrderPeriod(api).intValue();
	}

    @Test
    public void test01ApplyOrderToInvoice_Decimal_2() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");
	    
        // setup orders
	    OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);
        
        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82.81", new BigDecimal(82.81), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82.81", new BigDecimal(82.81), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);
        
        // update invoice decimal rounding preference to 2
        updateInvoiceDecimalRoundingPreference("2");
        
        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-82.8118");
        
        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);
        
        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        
        api.applyOrderToInvoice(orderId2, invoice);
        
        // Test invoice after applyOrderToInvoice Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should not have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("No credit notes should be generated as the invoice balance is Zero", 0, creditNoteIds.length);

        // cleanup
        cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }
    
    @Test
    public void test02ApplyOrderToInvoice_Decimal_4() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");
	    
        // setup orders
        OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82.81", new BigDecimal(82.81), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82.81", new BigDecimal(82.81), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // update invoice decimal rounding preference to 4
        updateInvoiceDecimalRoundingPreference("4");
        
        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-82.8118");

        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);
        
        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        
        api.applyOrderToInvoice(orderId2, invoice);
        
        // Test invoice after applyOrderToInvoice Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("1 credit note is generated for negative invoice balance", 1, creditNoteIds.length);
       	CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteIds[0]);
       	assertEquals("The credit note amount should be $0.0018", new BigDecimal("0.0018"), creditNoteWS.getAmountAsDecimal());

        // cleanup
       	cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }
    
    @Test
    public void test03ApplyOrderToInvoice_Decimal_null() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));
	    
        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");

        // setup orders
		OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82.81", new BigDecimal(82.81), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82.81", new BigDecimal(82.81), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);
        
        // update invoice decimal rounding preference to null
        // Note: Null means it will take default value from preference_type table which is '2'.
        updateInvoiceDecimalRoundingPreference(null);
        
        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-82.8118");
        
        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);

        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        
        api.applyOrderToInvoice(orderId2, invoice);
        
        // Test invoice after applyOrderToInvoice Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should not have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("No credit notes should be generated as the invoice balance is Zero", 0, creditNoteIds.length);

        // cleanup
        cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }
    
    @Test
    public void test04ApplyOrderToInvoice_Decimal_2() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");
	    
        // setup orders
        OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82.81", new BigDecimal(82.81), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82.81", new BigDecimal(82.81), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // update invoice decimal rounding preference to 4
        updateInvoiceDecimalRoundingPreference("2");
        
        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-83.61");

        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);
        
        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        
        api.applyOrderToInvoice(orderId2, invoice);
        
        // Test invoice after applyOrderToInvoice Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("1 credit note is generated for negative invoice balance", 1, creditNoteIds.length);
       	CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteIds[0]);
       	assertEquals("The credit note amount should be $0.80", new BigDecimal("0.80"), creditNoteWS.getAmountAsDecimal());

        // cleanup
       	cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }

    @Test
    public void test05ApplyOrderToInvoice_NonDecimalValues() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.00");

        // setup orders
        OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82", new BigDecimal(82.00), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82", new BigDecimal(82.00), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // update invoice decimal rounding preference to 2
        updateInvoiceDecimalRoundingPreference("2");

        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-83");

        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);

        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        api.applyOrderToInvoice(orderId2, invoice);

        // Test invoice after applyOrderToInvoice Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("1 credit note is generated for negative invoice balance", 1, creditNoteIds.length);
        CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteIds[0]);
        assertEquals("The credit note amount should be $1.00", new BigDecimal("1.00"), creditNoteWS.getAmountAsDecimal());

        // cleanup
        cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }

    @Test
    public void test06CreateInvoiceFromOrder_Decimal_2() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");

        // setup orders
	    OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82.81", new BigDecimal(82.81), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82.81", new BigDecimal(82.81), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // update invoice decimal rounding preference to 2
        updateInvoiceDecimalRoundingPreference("2");

        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-82.8118");

        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);

        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        api.createInvoiceFromOrder(orderId2, invoice.getId());

        // Test invoice after CreateInvoiceFromOrder Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should not have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("No credit notes should be generated as the invoice balance is Zero", 0, creditNoteIds.length);

        // cleanup
        cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }

    @Test
    public void test07CreateInvoiceFromOrder_Decimal_4() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");

        // setup orders
        OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82.81", new BigDecimal(82.81), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82.81", new BigDecimal(82.81), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // update invoice decimal rounding preference to 4
        updateInvoiceDecimalRoundingPreference("4");

        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-82.8118");

        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);

        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        api.createInvoiceFromOrder(orderId2, invoice.getId());

        // Test invoice after CreateInvoiceFromOrder Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("1 credit note is generated for negative invoice balance", 1, creditNoteIds.length);
        CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteIds[0]);
        assertEquals("The credit note amount should be $0.0018", new BigDecimal("0.0018"), creditNoteWS.getAmountAsDecimal());

        // cleanup
        cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }

    @Test
    public void test08CreateInvoiceFromOrder_Decimal_2() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");

        // setup orders
        OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82.81", new BigDecimal(82.81), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82.81", new BigDecimal(82.81), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // update invoice decimal rounding preference to 4
        updateInvoiceDecimalRoundingPreference("2");

        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-83.61");

        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);

        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        api.createInvoiceFromOrder(orderId2, invoice.getId());

        // Test invoice after CreateInvoiceFromOrder Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("1 credit note is generated for negative invoice balance", 1, creditNoteIds.length);
        CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteIds[0]);
        assertEquals("The credit note amount should be $0.80", new BigDecimal("0.80"), creditNoteWS.getAmountAsDecimal());

        // cleanup
        cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }

    @Test
    public void test09CreateInvoiceFromOrder_NonDecimalValues() throws Exception {
        // user for tests
	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
	    user.setId(api.createUser(user));
	    updateCustomerNextInvoiceDate(user.getId(), api);
        Integer userId = user.getUserId();

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.00");

        // setup orders
        OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        Integer[] invoiceIds = api.getLastInvoices(userId, 2);
        assertEquals("Only 1 invoice was generated", 1, invoiceIds.length);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $82", new BigDecimal(82.00), invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $82", new BigDecimal(82.00), invoice.getBalanceAsDecimal());
        assertEquals("Only 1 order invoiced", 1, invoice.getOrders().length);
        assertEquals("Invoice generated from 1st order", orderId1, invoice.getOrders()[0]);

        // update invoice decimal rounding preference to 4
        updateInvoiceDecimalRoundingPreference("2");

        // negative price for the one time order with credit adjustment.
        price = new BigDecimal("-83");

        // setup orders
        order = setupOrder(userId, item, price, ORDER_PERIOD_ONE_TIME_ID);

        // create orders
        Integer orderId2 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        api.createInvoiceFromOrder(orderId2, invoice.getId());

        // Test invoice after CreateInvoiceFromOrder Call
        invoice = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice total should be $00.00", BigDecimal.ZERO, invoice.getTotalAsDecimal());
        assertEquals("Invoice balance should be $00.00", BigDecimal.ZERO, invoice.getBalanceAsDecimal());
        assertEquals("2 order invoiced", 2, invoice.getOrders().length);

        //Validate that a credit note should have been created.
        Integer[] creditNoteIds = api.getLastCreditNotes(userId, 1);
        assertEquals("1 credit note is generated for negative invoice balance", 1, creditNoteIds.length);
        CreditNoteWS creditNoteWS = api.getCreditNote(creditNoteIds[0]);
        assertEquals("The credit note amount should be $1.00", new BigDecimal("1.00"), creditNoteWS.getAmountAsDecimal());

        // cleanup
        cleanUp(userId, itemType, item, invoiceId, orderId1, orderId2);
    }

	private OrderWS setupOrder(Integer userId, ItemDTOEx item,
			BigDecimal price, int period) {
		// setup orders
		OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(period);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
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

	private void updateInvoiceDecimalRoundingPreference(String preferenceValue) {
		PreferenceWS preferenceWS =  api.getPreference(Constants.PREFERENCE_INVOICE_DECIMALS);
        preferenceWS.setValue(preferenceValue);
        api.updatePreference(preferenceWS);
        logger.debug("Preference for Invoice Decimal Rounding updated to : {}",preferenceValue);
	}

	private void cleanUp(Integer userId, ItemTypeWS itemType, ItemDTOEx item,
			 Integer invoiceId, Integer... orderIds) {
		api.deleteInvoice(invoiceId);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
        api.deleteUser(userId);
        for(Integer orderId : orderIds){
        	api.deleteOrder(orderId);
        }
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
}
