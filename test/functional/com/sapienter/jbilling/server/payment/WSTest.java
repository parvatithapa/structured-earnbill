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

package com.sapienter.jbilling.server.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Emil
 */
@Test(groups = {"web-services", "payment"}, testName = "payment.WSTest")
public class WSTest {

	private static final Logger logger = LoggerFactory.getLogger(WSTest.class);
	private static Integer STATUS_SUSPENDED;

	private static Integer CC_PAYMENT_TYPE;
	private static Integer ACH_PAYMENT_TYPE;
	private static Integer CHEQUE_PAYMENT_TYPE;

	private static Integer CURRENCY_USD;
	private static Integer CURRENCY_AUD;
	private static Integer PRANCING_PONY_ACCOUNT_TYPE;
	private static Integer MORDOR_ACCOUNT_TYPE;
	private static Integer LANGUAGE_ID;
	private static Integer ORDER_CHANGE_STATUS_APPLY;
	private static Integer PAYMENT_PERIOD;
	private static Integer ORDER_PERIOD_ONCE;

	private static JbillingAPI api;
	private static JbillingAPI mordorApi;

	private final static String CC_HOLDER = "Frodo Baggins";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_NUMBER = "4111111111111152";
	private final static String CC_ALTERNATIVE_NUMBER = "4532055035096596";
	private final static String CC_OBSCURE_CREDIT_CARD = "************6596";
	private final static LocalDate CC_EXPIRE_DATE = LocalDate.of(2020, 12, 01);
	private final static String CHEQUE_MF_BANK_NAME = "cheque.bank.name";
	private final static String CHEQUE_MF_DATE = "cheque.date";
	private final static String CHEQUE_MF_NUMBER = "cheque.number";
	private final static int CHEQUE_PM_ID = 3;


	@BeforeClass
	protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		mordorApi = JbillingAPIFactory.getAPI("apiClientMordor");
		CURRENCY_USD = Constants.PRIMARY_CURRENCY_ID;
		CURRENCY_AUD = Integer.valueOf(11);
		LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
		MORDOR_ACCOUNT_TYPE = Integer.valueOf(2);
		PAYMENT_PERIOD = Integer.valueOf(1);
		ORDER_PERIOD_ONCE = Integer.valueOf(1);
		ORDER_CHANGE_STATUS_APPLY = getOrCreateOrderChangeStatusApply(api);
		STATUS_SUSPENDED = getOrCreateSuspendedStatus(api);

		CC_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildCCTemplateMethod(api));
		ACH_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildACHTemplateMethod(api));
		CHEQUE_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildChequeTemplateMethod(api));
	}

	@AfterClass
	protected void tearDown() {
		//TODO: should we be able to (soft) delete payment method type if all customers are soft deleted???
		api.deletePaymentMethodType(CC_PAYMENT_TYPE);
		api.deletePaymentMethodType(ACH_PAYMENT_TYPE);
		api.deletePaymentMethodType(CHEQUE_PAYMENT_TYPE);
	}

	/**
	 * Tests payment apply and retrieve.
	 */
	@Test
	public void testApplyGet() {
		//setup
		UserWS mordorUser = buildUser(MORDOR_ACCOUNT_TYPE);
		mordorUser.setId(mordorApi.createUser(mordorUser));

		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		//testing
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());
		payment.setPaymentNotes("Notes");
		payment.setPaymentPeriod(PAYMENT_PERIOD);

		PaymentInformationWS cheque = PaymentMethodHelper.createCheque(
				CHEQUE_PAYMENT_TYPE, "ws bank", "2232-2323-2323", Calendar.getInstance().getTime());
		payment.getPaymentInstruments().add(cheque);

		logger.debug("Applying payment");
		Integer paymentId = api.applyPayment(payment, invoice.getId());
		logger.debug("Created payment {}", paymentId);
		assertNotNull("Didn't get the payment id", paymentId);


		//  get

		//verify the created payment
		logger.debug("Getting created payment");
		PaymentWS retPayment = api.getPayment(paymentId);
		assertNotNull("didn't get payment ", retPayment);

		assertEquals("created payment result", retPayment.getResultId(), payment.getResultId());
		logger.debug("Instruments are: {}", retPayment.getPaymentInstruments());

		assertEquals("created payment cheque ",
				getMetaField(retPayment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue(),
				getMetaField(payment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue());

		assertEquals("created payment user ", retPayment.getUserId(), payment.getUserId());
		assertEquals("notes", retPayment.getPaymentNotes(), payment.getPaymentNotes());
		assertEquals("period", retPayment.getPaymentPeriod(), payment.getPaymentPeriod());


		logger.debug("Validated created payment and paid invoice");
		assertNotNull("payment not related to invoice", retPayment.getInvoiceIds());
		assertTrue("payment not related to invoice", retPayment.getInvoiceIds().length == 1);
		assertEquals("payment not related to invoice", retPayment.getInvoiceIds()[0], invoice.getId());

		InvoiceWS retInvoice = api.getInvoiceWS(retPayment.getInvoiceIds()[0]);
		assertNotNull("New invoice not present", retInvoice);
		assertEquals("Balance of invoice should be total of order", BigDecimal.ZERO, retInvoice.getBalanceAsDecimal());
		assertEquals("Total of invoice should be total of order", new BigDecimal("15"), retInvoice.getTotalAsDecimal());
		assertEquals("New invoice not paid", retInvoice.getToProcess(), new Integer(0));
		assertNotNull("invoice not related to payment", retInvoice.getPayments());
		assertTrue("invoice not related to payment", retInvoice.getPayments().length == 1);
		assertEquals("invoice not related to payment", retInvoice.getPayments()[0].intValue(), retPayment.getId());


		//  get latest

		//verify the created payment
		logger.debug("Getting latest");
		retPayment = api.getLatestPayment(user.getId());
		assertNotNull("didn't get payment ", retPayment);
		assertEquals("latest id", paymentId.intValue(), retPayment.getId());
		assertEquals("created payment result", retPayment.getResultId(), payment.getResultId());

		assertEquals("created payment cheque ",
			getMetaField(retPayment.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue(),
			getMetaField(payment.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue());

		assertEquals("created payment user ", retPayment.getUserId(), payment.getUserId());

		try {
			logger.debug("Getting latest - invalid");
			api.getLatestPayment(mordorUser.getId());
			fail("User belongs to entity Mordor");
		} catch (Exception e) {
		}

		//  get last

		logger.debug("Getting last");
		Integer retPayments[] = api.getLastPayments(user.getId(), Integer.valueOf(2));
		assertNotNull("didn't get payment ", retPayments);
		// fetch the payment


		retPayment = api.getPayment(retPayments[0]);

		assertEquals("created payment result", retPayment.getResultId(), payment.getResultId());

		assertEquals("created payment cheque ",
				getMetaField(retPayment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue(),
				getMetaField(payment.getPaymentInstruments().iterator().next().getMetaFields(),
						PaymentMethodHelper.CHEQUE_MF_NUMBER).getStringValue());

		assertEquals("created payment user ", retPayment.getUserId(), payment.getUserId());
		assertTrue("No more than two records", retPayments.length <= 2);

		try {
			logger.debug("Getting last - invalid");
			api.getLastPayments(mordorUser.getId(), Integer.valueOf(2));
			fail("User belongs to entity Mordor");
		} catch (Exception e) {
		}

		//cleanup
		api.deletePayment(paymentId);
		api.deleteInvoice(invoice.getId());
		api.deleteUser(user.getId());
		try{
			mordorApi.deleteUser(mordorUser.getId());
		} catch (SessionInternalError e) {
			assertTrue(e.getMessage().contains("Notification not found for sending deleted user notification"));
		}
	}

	/**
	 * Test for: NameFilter. For now it uses a value already in DB
	 * for the blacklisted Name. In prepare-test db the name Bilbo Baggins
	 * is blacklisted.
	 * <p/>
	 * TODO: Here we only test two blacklist filters and both of them test
	 * against data already present in the db from prepare test. Due to the
	 * lack of control for modifying the black list from outside for now we
	 * do not test the rest of the filters: UserId, Address, Phone Number,
	 * Ip Address.
	 */
	@Test
	public void testBlacklistNameFilter() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "Bilbo", "Baggins");
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		// get invoice id
		invoice = api.getLatestInvoice(user.getId());
		assertNotNull("Couldn't get last invoice", invoice);
		Integer invoiceId = invoice.getId();
		assertNotNull("Invoice id was null", invoiceId);

		// try paying the invoice
		logger.debug("Trying to pay invoice for blacklisted user ...");
		PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceId);
		assertNotNull("Payment result empty", authInfo);

		// check that it was failed by the test blacklist filter
		assertFalse("Payment wasn't failed for user: " + user.getId(), authInfo.getResult().booleanValue());
		assertEquals("Processor response", "Name is blacklisted.", authInfo.getResponseMessage());

		//cleanup
		api.deleteInvoice(invoiceId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Test for: CreditCardFilter.For now it uses a value already in DB
	 * for the blacklisted cc number. In prepare-test db the cc number
	 * 5555555555554444 is blacklisted.
	 */
	@Test
	public void testBlacklistCreditCardFilter() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "5555555555554444");
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setId(api.saveLegacyInvoice(invoice));

		// get invoice id
		invoice = api.getLatestInvoice(user.getId());
		assertNotNull("Couldn't get last invoice", invoice);
		Integer invoiceId = invoice.getId();
		assertNotNull("Invoice id was null", invoiceId);

		// try paying the invoice
		logger.debug("Trying to pay invoice for blacklisted user ...");
		PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceId);
		assertNotNull("Payment result empty", authInfo);

		// check that it was failed by the test blacklist filter
		assertFalse("Payment wasn't failed for user: " + user.getId(), authInfo.getResult().booleanValue());
		assertEquals("Processor response", "Credit card number is blacklisted.", authInfo.getResponseMessage());

		//cleanup
		api.deleteInvoice(invoiceId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Removing pre-authorization when the CC number is changed.
	 */
	@Test
	public void testRemoveOnCCChange() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		// put a pre-auth record on this user
		OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("3.45"));

		PaymentAuthorizationDTOEx auth = api.createOrderPreAuthorize(
				order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderId = api.getLatestOrder(user.getId()).getId();

		PaymentWS preAuthPayment = api.getPayment(auth.getPaymentId());
		assertThat(preAuthPayment, is(not(nullValue())));
		assertThat(preAuthPayment.getIsPreauth(), is(1));
		assertThat(preAuthPayment.getDeleted(), is(0)); // NOT deleted

		// update the user's credit card, this should remove the old card
		// and delete any associated pre-authorizations
		DateTimeFormatter format = DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT);
		user = api.getUserWS(user.getId());
		com.sapienter.jbilling.server.user.WSTest.updateMetaField(
				user.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CC_MF_EXPIRY_DATE, format.print(new DateMidnight().plusYears(4).withDayOfMonth(1).toDate().getTime()).toCharArray());
		api.updateUser(user);
		logger.debug("User instruments are: {}", user.getPaymentInstruments());
		// validate that the pre-auth payment is no longer available
		preAuthPayment = api.getPayment(auth.getPaymentId());
		assertThat(preAuthPayment, is(not(nullValue())));
		assertThat(preAuthPayment.getIsPreauth(), is(1));
		assertThat(preAuthPayment.getDeleted(), is(1)); // is now deleted

		// cleanup
		api.deleteOrder(orderId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Test for BlacklistUserStatusTask. When a user's status moves to
	 * suspended or higher, the user and all their information is
	 * added to the blacklist.
	 */
	@Test(enabled = false)
	public void testBlacklistUserStatus() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE
				, "BlackListFirst", "BlackListSecond", "4916347258194745");
		user.setId(api.createUser(user));

		// expected filter response messages
		String[] messages = new String[3];
		messages[0] = "User id is blacklisted.";
		messages[1] = "Name is blacklisted.";
		messages[2] = "Credit card number is blacklisted.";

//	    TODO: for now we do not test for these three
//        messages[3] = "Address is blacklisted.";
//        messages[4] = "IP address is blacklisted.";
//        messages[5] = "Phone number is blacklisted.";


		// check that a user isn't blacklisted
		user = api.getUserWS(user.getId());
		// CXF returns null
		if (user.getBlacklistMatches() != null) {
			assertTrue("User shouldn't be blacklisted yet",
					user.getBlacklistMatches().length == 0);
		}

		// change their status to suspended
		user.setStatusId(STATUS_SUSPENDED);
		user.setPassword(null);
		api.updateUser(user);

		// check all their records are now blacklisted
		user = api.getUserWS(user.getId());
		assertEquals("User records should be blacklisted.",
				Arrays.toString(messages),
				Arrays.toString(user.getBlacklistMatches()));


		//cleanup
		api.deleteUser(user.getId());
	}

	/**
	 * Tests the PaymentRouterCurrencyTask.
	 */
	@Test
	public void testPaymentRouterCurrencyTask() {
		//prepare
		UserWS userUSD = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		userUSD.setCurrencyId(CURRENCY_USD);
		userUSD.setId(api.createUser(userUSD));

		updateCustomerNextInvoiceDate(userUSD.getId(), api);

		UserWS userAUD = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		userAUD.setCurrencyId(CURRENCY_AUD);
		userAUD.setId(api.createUser(userAUD));

		updateCustomerNextInvoiceDate(userAUD.getId(), api);

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		//testing
		OrderWS order = buildOrder(userUSD.getId(), Arrays.asList(item.getId()), new BigDecimal("10"));
		order.setCurrencyId(userUSD.getCurrencyId());

		// create the order and invoice it
		logger.debug("Creating and invoicing order ...");
		Integer invoiceIdUSD = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderIdUSD = api.getLastOrders(userUSD.getId(), 1)[0];

		// try paying the invoice in USD
		logger.debug("Making payment in USD...");
		PaymentAuthorizationDTOEx authInfo = api.payInvoice(invoiceIdUSD);

		assertTrue("USD Payment should be successful", authInfo.getResult().booleanValue());
		assertEquals("Should be processed by 'first_fake_processor'", authInfo.getProcessor(), "first_fake_processor");

		// create a new order in AUD and invoice it
		order.setUserId(userAUD.getId());
		order.setCurrencyId(userAUD.getCurrencyId());

		logger.debug("Creating and invoicing order ...");
		Integer invoiceIdAUD = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderIdAUD = api.getLastOrders(userAUD.getId(), 1)[0];

		// try paying the invoice in AUD
		logger.debug("Making payment in AUD...");
		authInfo = api.payInvoice(invoiceIdAUD);

		assertTrue("AUD Payment should be successful", authInfo.getResult().booleanValue());
		assertEquals("Should be processed by 'second_fake_processor'", authInfo.getProcessor(), "second_fake_processor");

		// remove invoices and orders
		logger.debug("Deleting invoices and orders.");
		api.deleteInvoice(invoiceIdUSD);
		api.deleteInvoice(invoiceIdAUD);
		api.deleteOrder(orderIdUSD);
		api.deleteOrder(orderIdAUD);
		api.deleteUser(userUSD.getId());
		api.deleteUser(userAUD.getId());
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
	}

	/**
	 * Test payInvoice(invoice) API call.
	 */
	@Test
	public void testPayInvoice() {
		//setup
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));
		updateCustomerNextInvoiceDate(user.getId(), api);

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		//testing
		logger.debug("Getting an invoice paid, and validating the payment.");
		OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("3.45"));
		Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer orderId = api.getInvoiceWS(invoiceId).getOrders()[0];
		PaymentAuthorizationDTOEx auth = api.payInvoice(invoiceId);
		assertNotNull("auth can not be null", auth);
		PaymentWS payment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", payment);
		assertNotNull("auth in payment can not be null", payment.getAuthorizationId());

		//cleanup
		api.deletePayment(auth.getPaymentId());
		api.deleteInvoice(invoiceId);
		api.deleteOrder(orderId);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	/**
	 * Tests processPayment API call.
	 */
	@Test
	public void testProcessPayment() {
		//setup
		// Set the company timezone to Aus/Sydney to test if last payment date is as per company timezone
		CompanyWS company = api.getCompany();
		company.setTimezone("Australia/Sydney");
		api.updateCompany(company);
		int companyId = company.getId();
				
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "4111111111111111");
		user.setId(api.createUser(user));
		updateCustomerNextInvoiceDate(user.getId(), api);

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		// first, create two unpaid invoices
		OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("10.00"));
		Integer invoiceId1 = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
		Integer invoiceId2 = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));

		// create the payment
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("5.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_VISA);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());

		//  try a credit card number that fails
		// note that creating a payment with a NEW credit card will save it and associate
		// it with the user who made the payment.
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 5);

		PaymentInformationWS cc = PaymentMethodHelper.createCreditCard(
				CC_PAYMENT_TYPE, CC_HOLDER, "4111111111111111", cal.getTime());
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
		payment.getPaymentInstruments().add(cc);

		logger.debug("processing payment.");
		PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);
		// Expected payment date as per company timezone of Aus/Sydney (without the timestamp)
		// as payment date does not store timestamp
		Date expectedPaymentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(companyId));
		
		// check payment failed
		assertNotNull("Payment result not null", authInfo);
		assertFalse("Payment Authorization result should be FAILED", authInfo.getResult().booleanValue());

		// check payment has zero balance
		PaymentWS lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("5"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment.getBalanceAsDecimal());
		
		// Make sure the last payment date is getting saved as per company timezone
		assertEquals("Payment Date is not matching per company timezone", expectedPaymentDate, lastPayment.getPaymentDate());

		// check invoices still have balance
		InvoiceWS invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10.0"), invoice1.getBalanceAsDecimal());
		InvoiceWS invoice2 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10.0"), invoice2.getBalanceAsDecimal());

		// do it again, but using the credit card on file
		// which is also 4111111111111111
		payment.getPaymentInstruments().clear();
		logger.debug("processing payment.");
		authInfo = api.processPayment(payment, null);
		expectedPaymentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(companyId));
		
		// check payment has zero balance
		PaymentWS lastPayment2 = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment2);
		assertNotNull("auth in payment can not be null", lastPayment2.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("5"), lastPayment2.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment2.getBalanceAsDecimal());
		assertFalse("Payment is not the same as preiouvs", lastPayment2.getId() == lastPayment.getId());

		// Make sure the last payment date is getting saved as per company timezone
		assertEquals("Payment Date is not matching per company timezone", expectedPaymentDate, lastPayment2.getPaymentDate());

		// check invoices still have balance
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10"), invoice1.getBalanceAsDecimal());
		invoice2 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("10"), invoice2.getBalanceAsDecimal());


		//  do a successful payment of $5
		cc = PaymentMethodHelper.createCreditCard(
				CC_PAYMENT_TYPE, CC_HOLDER, CC_NUMBER, cal.getTime());
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
		payment.getPaymentInstruments().add(cc);
		logger.debug("processing payment.");
		authInfo = api.processPayment(payment, null);
		expectedPaymentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(companyId));

		// check payment successful
		assertNotNull("Payment result not null", authInfo);
		assertNotNull("Auth id not null", authInfo.getId());
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment was made
		lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("payment ids match", lastPayment.getId(), authInfo.getPaymentId().intValue());
		assertEquals("correct payment amount", new BigDecimal("5"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment.getBalanceAsDecimal());
		
		// Make sure the last payment date is getting saved as per company timezone
		assertEquals("Payment Date is not matching per company timezone", expectedPaymentDate, lastPayment.getPaymentDate());

		// check invoice 1 was partially paid (balance 5)
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", new BigDecimal("5.0"), invoice1.getBalanceAsDecimal());

		// check invoice 2 wan't paid at all
		invoice2 = api.getInvoiceWS(invoiceId2);
		assertEquals("correct invoice balance", new BigDecimal("10.0"), invoice2.getBalanceAsDecimal());


		//  another payment for $10, this time with the user's credit card

		// update the credit card to the one that is good
		user = api.getUserWS(user.getId());
		com.sapienter.jbilling.server.user.WSTest.updateMetaField(
				user.getPaymentInstruments().iterator().next().getMetaFields(),
				PaymentMethodHelper.CC_MF_NUMBER, CC_NUMBER.toCharArray());
		api.updateUser(user);

		// process a payment without an attached credit card
		// should try and use the user's saved credit card
		payment.getPaymentInstruments().clear();
		payment.setAmount(new BigDecimal("10.00"));
		logger.debug("processing payment.");
		authInfo = api.processPayment(payment, null);
		expectedPaymentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(companyId));

		// check payment successful
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment was made
		lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("10"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance", BigDecimal.ZERO, lastPayment.getBalanceAsDecimal());
		
		// Make sure the last payment date is getting saved as per company timezone
		assertEquals("Payment Date is not matching per company timezone", expectedPaymentDate, lastPayment.getPaymentDate());

		// check invoice 1 is fully paid (balance 0)
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", BigDecimal.ZERO, invoice1.getBalanceAsDecimal());

		// check invoice 2 was partially paid (balance 5)
		invoice2 = api.getInvoiceWS(invoiceId2);
		assertEquals("correct invoice balance", new BigDecimal("5"), invoice2.getBalanceAsDecimal());


		// another payment for $10

		payment.getPaymentInstruments().add(cc);
		payment.setAmount(new BigDecimal("10.00"));
		logger.debug("processing payment.");
		authInfo = api.processPayment(payment, null);
		expectedPaymentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(companyId));

		// check payment successful
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment was made
		lastPayment = api.getLatestPayment(user.getId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct  payment amount", new BigDecimal("10"), lastPayment.getAmountAsDecimal());
		assertEquals("correct  payment balance", new BigDecimal("5"), lastPayment.getBalanceAsDecimal());
		
		// Make sure the last payment date is getting saved as per company timezone
		assertEquals("Payment Date is not matching per company timezone", expectedPaymentDate, lastPayment.getPaymentDate());

		// check invoice 1 balance is unchanged
		invoice1 = api.getInvoiceWS(invoiceId1);
		assertEquals("correct invoice balance", BigDecimal.ZERO, invoice1.getBalanceAsDecimal());

		// check invoice 2 is fully paid (balance 0)
		invoice2 = api.getInvoiceWS(invoiceId2);
		assertEquals("correct invoice balance", BigDecimal.ZERO, invoice2.getBalanceAsDecimal());
		//cleanup
		logger.debug("Deleting invoices and orders.");
		api.deleteInvoice(invoice1.getId());
		api.deleteInvoice(invoice2.getId());
		api.deleteOrder(invoice1.getOrders()[0]);
		api.deleteOrder(invoice2.getOrders()[0]);
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
		
		// Revert back the company timezone to default UTC
		company.setTimezone("UTC");
		api.updateCompany(company);
	}

	/**
	 * Tests failed and successful payment for ACH
	 */
	@Test
	public void testAchFakePayments() {
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);

		//remove payment instruments and add only ACH payment instrument
		user.getPaymentInstruments().clear();
		user.getPaymentInstruments().add(
				PaymentMethodHelper.createACH(ACH_PAYMENT_TYPE, CC_HOLDER,
						"Shire Financial Bank", "123456789", "123456789", PRANCING_PONY_ACCOUNT_TYPE));

		logger.debug("Creating user with ACH record and no CC...");
		user.setId(api.createUser(user));

		// get ach
		try (PaymentInformationWS ach = user.getPaymentInstruments().get(0)) {

			logger.debug("Testing ACH payment with even amount (should pass)");
			PaymentWS payment = new PaymentWS();
			payment.setAmount(new BigDecimal("15.00"));
			payment.setIsRefund(new Integer(0));
			payment.setMethodId(Constants.PAYMENT_METHOD_ACH);
			payment.setPaymentDate(Calendar.getInstance().getTime());
			payment.setResultId(Constants.RESULT_ENTERED);
			payment.setCurrencyId(CURRENCY_USD);
			payment.setUserId(user.getId());
			payment.setPaymentNotes("Notes");
			payment.setPaymentPeriod(PAYMENT_PERIOD);
			payment.getPaymentInstruments().add(ach);

			PaymentAuthorizationDTOEx resultOne = api.processPayment(payment, null);
			assertEquals("ACH payment with even amount should pass",
					Constants.RESULT_OK, api.getPayment(resultOne.getPaymentId()).getResultId());

			logger.debug("Testing ACH payment with odd amount (should fail)");
			payment = new PaymentWS();
			payment.setAmount(new BigDecimal("15.01"));
			payment.setIsRefund(new Integer(0));
			payment.setMethodId(Constants.PAYMENT_METHOD_ACH);
			payment.setPaymentDate(Calendar.getInstance().getTime());
			payment.setResultId(Constants.RESULT_ENTERED);
			payment.setCurrencyId(CURRENCY_USD);
			payment.setUserId(user.getId());
			payment.setPaymentNotes("Notes");
			payment.setPaymentPeriod(PAYMENT_PERIOD);
			payment.getPaymentInstruments().add(ach);

			PaymentAuthorizationDTOEx resultTwo = api.processPayment(payment, null);
			assertEquals("ACH payment with odd amount should fail",
					Constants.RESULT_FAIL, api.getPayment(resultTwo.getPaymentId()).getResultId());

			//cleanup

			api.deletePayment(resultTwo.getPaymentId());
			api.deletePayment(resultOne.getPaymentId());
			api.deleteUser(user.getId());
		}catch (Exception exception){
			logger.error("Error during clean up", exception);
		}
	}

	/**
	 * Tries to create payment against review invoice. Here,
	 * instead of using the billing process to generate a review
	 * invoice we are creating a review invoice with the help
	 * of saveLegacyInvoice call.
	 */
	@Test
	public void testPayReviewInvoice() {
		//creating new user
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		InvoiceWS invoice = buildInvoice(user.getId(), item.getId());
		invoice.setIsReview(Integer.valueOf(1));
		invoice.setId(api.saveLegacyInvoice(invoice));

		//check if invoice is a review invoice
		logger.debug("Invoice is review : {}", invoice.getIsReview());
		assertEquals("Invoice is a review invoice", Integer.valueOf(1), invoice.getIsReview());

		try {
			//pay for a review invoice
			api.payInvoice(invoice.getId());
			fail("We should not be able to issue a payment against review invoice");
		} catch (SessionInternalError e) {
			logger.error("Error while doing payment", e);
		}

		//clean up
		api.deleteInvoice(invoice.getId());
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getId());
	}

	@Test
	public void testNewGetPaymentsApiMethods() throws Exception{
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// Create a user with balance $1.00
		UserWS user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);

		List<PaymentWS> payments = new ArrayList<PaymentWS>();

		for(int i=0; i<5; i++){
			payments.add(createPaymentWS(user.getUserId(), new DateTime().plusMonths(i).toDate(), String.valueOf(i)));
		}

		//get two latest payments except the latest one.
		Integer[] paymentsId = api.getLastPaymentsPage(user.getUserId(), 2, 1) ;

		assertEquals(2, paymentsId.length);

        onePaymentHasNPaymentNotes(paymentsId, "3");
        onePaymentHasNPaymentNotes(paymentsId, "2");

		//get the payments between next month and four months from now.
		Integer[] paymentsId2 = api.getPaymentsByDate(user.getUserId(), new DateTime().plusDays(1).toDate() , new DateTime().plusMonths(3).plusDays(1).toDate()) ;

		assertEquals(3, paymentsId2.length);

        onePaymentHasNPaymentNotes(paymentsId2, "3");
        onePaymentHasNPaymentNotes(paymentsId2, "2");
        onePaymentHasNPaymentNotes(paymentsId2, "1");

		//Delete orders
		for(PaymentWS payment : payments){
			api.deletePayment(payment.getId()) ;
		}
		//Delete user
		api.deleteUser(user.getUserId());
	}

	@Test
	public void testPaymentWithCcNumberChanged() throws Exception {
		UserWS user = api.getUserWS(api.createUser(buildUser(PRANCING_PONY_ACCOUNT_TYPE)));

		ItemTypeWS itemType = buildItemType();
		itemType.setId(api.createItemCategory(itemType));

		ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
		item.setId(api.createItem(item));

		// first, create two unpaid invoices
		OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), BigDecimal.TEN);
		InvoiceWS invoice = api.getInvoiceWS(api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY)));
		order = api.getLatestOrder(user.getUserId());
		assertNotNull("Order is empty", order);
		assertNotNull("Invoices is empty", invoice);

		PaymentInformationWS paymentInformation = PaymentMethodHelper.createCreditCard(CC_PAYMENT_TYPE, CC_HOLDER, CC_ALTERNATIVE_NUMBER, DateConvertUtils.asUtilDate(CC_EXPIRE_DATE));
		PaymentWS payment = ProcessSignPaymentAPITest.createPayment(user.getUserId(), BigDecimal.TEN, paymentInformation);
		PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);
		assertNotNull("Payment result is empty", authInfo);
		assertTrue("Payment was failed for user: " + user.getId(), authInfo.getResult());

		payment = api.getLatestPayment(user.getId());
		assertNotNull("Payment is empty", payment);

		assertNotNull("Payment is empty", payment);
		assertEquals("Payment with Credit Card Number", CC_OBSCURE_CREDIT_CARD,
			new String(getMetaField(payment.getPaymentInstruments().get(0).getMetaFields(), CC_MF_NUMBER).getCharValue()));

		/*
		 * Delete Payment
		 * Delete Order
		 * Delete Invoice
		 * Delete Item
		 * Delete ItemCategory
		 * Delete User
		 */
		api.deletePayment(payment.getId());
		api.deleteInvoice(invoice.getId());
		api.deleteOrder(order.getId());
		api.deleteItem(item.getId());
		api.deleteItemCategory(itemType.getId());
		api.deleteUser(user.getUserId());
	}

	@Test
	public void testEditPayment() throws Exception {
        BillingProcessConfigurationWS billingConfig = api.getBillingProcessConfiguration();
        int autoApply = billingConfig.getAutoPaymentApplication();
        billingConfig.setAutoPaymentApplication(0);
        api.createUpdateBillingProcessConfiguration(billingConfig);
        billingConfig.setAutoPaymentApplication(autoApply);

		Integer userId = api.createUser(buildUser(PRANCING_PONY_ACCOUNT_TYPE));
		Integer itemTypeId = api.createItemCategory(buildItemType());
		Integer itemId = api.createItem(buildItem(itemTypeId, api.getCallerCompanyId()));

		// create the payment
		PaymentWS payment = createPaymentWS(userId, new Date(), "Test Edit Payment");
		PaymentInformationWS cc = PaymentMethodHelper.createCreditCard(
				CC_PAYMENT_TYPE, CC_HOLDER, CC_NUMBER, new Date());
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
		payment.getPaymentInstruments().clear();
		payment.getPaymentInstruments().add(cc);
		logger.debug("Processing payment.");

		PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);

		// check payment failed
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result shouldn't be FAILED", authInfo.getResult().booleanValue());

		// check payment
		payment = api.getLatestPayment(userId);
		assertNotNull("Payment can not be null", payment);
		assertNotNull("Auth in payment can not be null", payment.getAuthorizationId());

		payment.setAmount(BigDecimal.TEN);
		payment.setAuthorization(null);
		api.updatePayment(payment);

		// create unpaid invoices
		OrderWS order = buildOrder(userId, Arrays.asList(itemId), BigDecimal.TEN);
		InvoiceWS invoice = api.getInvoiceWS(api.createOrderAndInvoice(buildOrder(userId, Arrays.asList(itemId), BigDecimal.TEN), OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY)));
		order = api.getLatestOrder(userId);
		assertNotNull("Order is empty", order);
		assertNotNull("Invoices is empty", invoice);

		api.createPaymentLink(invoice.getId(), payment.getId());
		payment = api.getPayment(payment.getId());

		logger.debug("Validated created payment and paid invoice, {}",payment);
		assertNotNull("Payment not related to invoice", payment.getInvoiceIds());
		assertTrue("Payment not related to invoice", payment.getInvoiceIds().length == 1);
		assertEquals("Payment not related to invoice", payment.getInvoiceIds()[0], invoice.getId());
		assertTrue("Payment Authorization result shouldn't be FAILED", authInfo.getResult().booleanValue());

		/*
		 * Delete Payment
		 * Delete Order
		 * Delete Invoice
		 * Delete Item
		 * Delete ItemCategory
		 * Delete User
		 */
        api.createUpdateBillingProcessConfiguration(billingConfig);
		api.deletePayment(payment.getId());
		api.deleteInvoice(invoice.getId());
		api.deleteOrder(order.getId());
		api.deleteItem(itemId);
		api.deleteItemCategory(itemTypeId);
		api.deleteUser(userId);

	}

    @Test
    public void testEditRefund() throws Exception {
        Integer userId = api.createUser(buildUser(PRANCING_PONY_ACCOUNT_TYPE));

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, userId, null, cheque);

        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull("Payment result not null", paymentId);

        payment.setId(paymentId);
        Integer refundId = createPayment(api, cheque.getPaymentMethodId(), "40.00", true, userId, paymentId, cheque);
        PaymentWS refund = api.getPayment(refundId);
        refund.setPaymentId(refund.getId());
        assertNotNull("Didn't get the payment id", refundId);

        refund.setId(refundId);

        refund = api.getPayment(refund.getId());
        assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
        assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("40")) == 0);

        payment = api.getPayment(payment.getId());
        assertTrue("Payments remaining balance should be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("60")) == 0);

        refund.setAmount(new BigDecimal("20"));
        api.updatePayment(refund);

        //As per the new changes we are not allowing to update amount of refunds/payment.(You can update rest of fields except amount)
        payment = api.getPayment(payment.getId());
        assertTrue("Payments remaining balance should not be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("60")) == 0);

        refund.setAmount(new BigDecimal("120"));
        try {
            api.updatePayment(refund);
        } catch (Exception e) {
            assertTrue("Update payment should through validation error", e.getMessage().contains("validation.error.apply.without.payment.or.different.linked.payment.amount"));
        }

        api.deletePayment(refund.getId());
        api.deletePayment(payment.getId());
        api.deleteUser(userId);
    }

    @Test
    public void testEditMultipleRefundsWithEnteredPayment() throws Exception {
        Integer userId = api.createUser(buildUser(PRANCING_PONY_ACCOUNT_TYPE));

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //Creating payment with amount 100
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, userId, null, cheque);

        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull("Payment result not null", paymentId);
        assertEquals("Payment is Entered payment", payment.getResultId(),new Integer(4));

        payment.setId(paymentId);
        //Creating refund with amount 40
        Integer refundId_01 = createPayment(api, cheque.getPaymentMethodId(), "40.00", true, userId, paymentId, cheque);
        PaymentWS refund = api.getPayment(refundId_01);
        refund.setPaymentId(refund.getId());
        assertNotNull("Didn't get the payment id", refundId_01);
        assertEquals("Payment is Entered payment", refund.getResultId(),new Integer(4));

        refund.setId(refundId_01);

        refund = api.getPayment(refundId_01);
        assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
        assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("40")) == 0);

        //Creating refund with amount 20
        Integer refundId_02 = createPayment(api, cheque.getPaymentMethodId(), "20.00", true, userId, paymentId, cheque);
        refund = api.getPayment(refundId_02);
        refund.setPaymentId(refund.getId());
        assertNotNull("Didn't get the payment id", refundId_02);
        assertEquals("Payment is Entered payment", refund.getResultId(),new Integer(4));

        refund.setId(refundId_02);

        refund = api.getPayment(refundId_02);
        assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
        assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("20")) == 0);

        payment = api.getPayment(payment.getId());
        assertTrue("Payments remaining balance should be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("40")) == 0);

        //Updating second refund with amount 10
        refund.setAmount(new BigDecimal("10"));
        api.updatePayment(refund);

        //As per the new changes we are not allowing to update amount of refunds/payment.(You can update rest of fields except amount)
        payment = api.getPayment(payment.getId());
        assertTrue("Payments remaining balance should not be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("40")) == 0);

        //Creating refund with amount 10
        Integer refundId_03 = createPayment(api, cheque.getPaymentMethodId(), "10.00", true, userId, paymentId, cheque);
        refund = api.getPayment(refundId_03);
        refund.setPaymentId(refund.getId());
        assertNotNull("Didn't get the payment id", refundId_03);
        assertEquals("Payment is Entered payment", refund.getResultId(),new Integer(4));

        refund.setId(refundId_03);

        refund = api.getPayment(refundId_03);
        assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
        assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("10")) == 0);

        payment = api.getPayment(payment.getId());
        assertTrue("Payments remaining balance should be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("30")) == 0);

        refund = api.getPayment(refundId_01);
        assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
        assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("40")) == 0);

        //Updating first refund with amount 30
        refund.setAmount(new BigDecimal("30"));
        api.updatePayment(refund);

        //As per the new changes we are not allowing to update amount of refunds/payment.(You can update rest of fields except amount)
        payment = api.getPayment(payment.getId());
        assertTrue("Payments remaining balance should not be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("30")) == 0);

        api.deletePayment(refundId_01);
        api.deletePayment(refundId_02);
        api.deletePayment(refundId_03);
        api.deletePayment(payment.getId());
        api.deleteUser(userId);
    }

     @Test
        public void testEditMultipleRefundsWithUnlinkedInvoice() throws Exception {
            Integer userId = api.createUser(buildUser(PRANCING_PONY_ACCOUNT_TYPE));

            // Creating payment with amount 100
            PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

            Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, userId, null, cheque);

            PaymentWS payment = api.getPayment(paymentId);
            assertNotNull("Payment result not null", paymentId);
            assertEquals("Payment is Entered payment", payment.getResultId(),new Integer(4));

            payment.setId(paymentId);
            //Creating refund with amount 40
            Integer refundId_01 = createPayment(api, cheque.getPaymentMethodId(), "40.00", true, userId, paymentId, cheque);
            PaymentWS refund = api.getPayment(refundId_01);
            refund.setPaymentId(refund.getId());
            assertNotNull("Didn't get the payment id", refundId_01);
            assertEquals("Payment is Entered payment", refund.getResultId(),new Integer(4));

            refund.setId(refundId_01);

            refund = api.getPayment(refundId_01);
            assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
            assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("40")) == 0);
            //Creating refund with amount 20
            Integer refundId_02 = createPayment(api, cheque.getPaymentMethodId(), "20.00", true, userId, paymentId, cheque);
            refund = api.getPayment(refundId_02);
            refund.setPaymentId(refund.getId());
            assertNotNull("Didn't get the payment id", refundId_02);
            assertEquals("Payment is Entered payment", refund.getResultId(),new Integer(4));

            refund.setId(refundId_02);

            refund = api.getPayment(refundId_02);
            assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
            assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("20")) == 0);

            payment = api.getPayment(payment.getId());
            assertTrue("Payments remaining balance should be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("40")) == 0);

            refund.setAmount(new BigDecimal("10"));
            //Updating second refund with amount 10
            api.updatePayment(refund);

            //As per the new changes we are not allowing to update amount of refunds/payment.(You can update rest of fields except amount)
            payment = api.getPayment(payment.getId());
            assertTrue("Payments remaining balance should not be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("40")) == 0);

            //Creating order with amount 10
            ItemTypeWS itemType = buildItemType();
            itemType.setId(api.createItemCategory(itemType));

            ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
            item.setId(api.createItem(item));

            OrderWS order = buildOrder(userId, Arrays.asList(item.getId()), new BigDecimal("10"));
            order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
            order.setPeriod(Integer.valueOf(2));

            //Creating the order and invoice
            logger.debug("Creating and invoicing order ...");
            Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY));
            payment = api.getPayment(payment.getId());
            assertTrue("Payments remaining balance should be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("30")) == 0);
            //Creating refund with amount 10
            Integer refundId_03 = createPayment(api, cheque.getPaymentMethodId(), "10.00", true, userId, paymentId, cheque);
            refund = api.getPayment(refundId_03);
            refund.setPaymentId(refund.getId());
            assertNotNull("Didn't get the payment id", refundId_03);
            assertEquals("Payment is Entered payment", refund.getResultId(),new Integer(4));

            refund.setId(refundId_03);

            refund = api.getPayment(refundId_03);
            assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
            assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("10")) == 0);

            payment = api.getPayment(payment.getId());
            assertTrue("Payments remaining balance should be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("20")) == 0);

            //Updating first refund with amount 30
            refund = api.getPayment(refundId_01);
            assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
            assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("40")) == 0);

            refund.setAmount(new BigDecimal("30"));
            //update refund
            api.updatePayment(refund);

            //As per the new changes we are not allowing to update amount of refunds/payment.(You can update rest of fields except amount)
            payment = api.getPayment(payment.getId());
            assertTrue("Payments remaining balance should not be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("20")) == 0);

            //Unlink the invoice from paymnet
            api.removePaymentLink(invoiceId, payment.getId());

            payment = api.getPayment(payment.getId());
            assertTrue("Payments remaining balance should be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("30")) == 0);

            //Updating third refund with amount 40
            refund = api.getPayment(refundId_03);
            assertTrue("Refunds remaining balance should be zero", refund.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) == 0);
            assertTrue("Refunds amount should be ", refund.getAmountAsDecimal().compareTo(new BigDecimal("10")) == 0);

            refund.setAmount(new BigDecimal("20"));
            //update refund
            api.updatePayment(refund);

            //As per the new changes we are not allowing to update amount of refunds/payment.(You can update rest of fields except amount)
            payment = api.getPayment(payment.getId());
            assertTrue("Payments remaining balance should not be updated", payment.getBalanceAsDecimal().compareTo(new BigDecimal("30")) == 0);

            api.deletePayment(refundId_01);
            api.deletePayment(refundId_02);
            api.deletePayment(refundId_03);
            api.deleteInvoice(invoiceId);
            api.deletePayment(payment.getId());
            api.deleteUser(userId);
        }

    private void onePaymentHasNPaymentNotes(Integer[] paymentsIds, String paymentNotesExpected) {
        boolean found = false;
        for (Integer paymentId: paymentsIds) {
            if (paymentNotesExpected.equals(api.getPayment(paymentId).getPaymentNotes()))
                found = true;
        }
        assertTrue(found);
    }

    private PaymentWS createPaymentWS(Integer userId, Date date, String note) throws Exception{
		JbillingAPI api = JbillingAPIFactory.getAPI();

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(date);
		payment.setCreateDatetime(date);
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(userId);
		payment.setPaymentNotes(note);
		payment.setPaymentPeriod(new Integer(1));

		PaymentInformationWS cheque = com.sapienter.jbilling.server.user.WSTest.
				createCheque("ws bank", "2232-2323-2323", date);

		payment.getPaymentInstruments().add(cheque);

		logger.debug("Applying payment");
		Integer ret = api.applyPayment(payment, new Integer(35));
		logger.debug("Created payment {}", ret);
		assertNotNull("Didn't get the payment id", ret);

		payment.setId(ret);
		return payment;
	}
	
	/**
	 * Testing the saveLegacyPayment API call
	 */
	@Test
	public void testSaveLegacyPayment() {
		//setup
		UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);
		user.setId(api.createUser(user));

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("15.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_CREDIT);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(CURRENCY_USD);
		payment.setUserId(user.getId());
		payment.setPaymentPeriod(PAYMENT_PERIOD);
		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		// add credit card
		payment.getPaymentInstruments().add(PaymentMethodHelper
				.createCreditCard(CC_PAYMENT_TYPE, CC_HOLDER, CC_NUMBER, expiry.getTime()));

		Integer paymentId = api.saveLegacyPayment(payment);
		assertNotNull("Payment should be saved", paymentId);

		PaymentWS retPayment = api.getPayment(paymentId);
		assertNotNull(retPayment);
		assertEquals(retPayment.getAmountAsDecimal(), payment.getAmountAsDecimal());
		assertEquals(retPayment.getIsRefund(), payment.getIsRefund());
		assertEquals(retPayment.getMethodId(), payment.getMethodId());
		assertEquals(retPayment.getResultId(), payment.getResultId());
		assertEquals(retPayment.getCurrencyId(), payment.getCurrencyId());
		assertEquals(retPayment.getUserId(), payment.getUserId());
		assertEquals(retPayment.getPaymentNotes(), "This payment is migrated from legacy system.");
		assertEquals(retPayment.getPaymentPeriod(), payment.getPaymentPeriod());

		//cleanup
		api.deletePayment(retPayment.getId());
		api.deleteUser(user.getId());
	}

	private UserWS buildUser(Integer accountType) {
		return buildUser(accountType, "Frodo", "Baggins", CC_NUMBER);
	}

	private UserWS buildUser(Integer accountType, String firstName, String lastName) {
		return buildUser(accountType, firstName, lastName, CC_NUMBER);
	}

	private UserWS buildUser(Integer accountType, String ccNumber) {
		return buildUser(accountType, "Frodo", "Baggins", ccNumber);
	}

	private UserWS buildUser(Integer accountTypeId, String firstName, String lastName, String ccNumber) {
		UserWS newUser = new UserWS();
		newUser.setUserName("payment-test-" + Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("Admin123@");
		newUser.setLanguageId(LANGUAGE_ID);
		newUser.setMainRoleId(new Integer(5));
		newUser.setAccountTypeId(accountTypeId);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(CURRENCY_USD);
		
		MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		metaField1.setFieldName("contact.email");
		metaField1.setValue(newUser.getUserName() + "@shire.com");
		metaField1.setGroupId(accountTypeId);

		MetaFieldValueWS metaField2 = new MetaFieldValueWS();
		metaField2.setFieldName("contact.first.name");
		metaField2.setValue(firstName);
		metaField2.setGroupId(accountTypeId);

		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("contact.last.name");
		metaField3.setValue(lastName);
		metaField3.setGroupId(accountTypeId);

		newUser.setMetaFields(new MetaFieldValueWS[]{
				metaField1,
				metaField2,
				metaField3
		});

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		// add credit card
		newUser.getPaymentInstruments().add(PaymentMethodHelper
				.createCreditCard(CC_PAYMENT_TYPE, CC_HOLDER, ccNumber, expiry.getTime()));

		return newUser;
	}

	private OrderWS buildOrder(int userId, List<Integer> itemIds, BigDecimal linePrice) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(ORDER_PERIOD_ONCE); // once
		order.setCurrencyId(CURRENCY_USD);
		order.setActiveSince(new Date());
		order.setProrateFlag(Boolean.FALSE);

		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(itemIds.size());
		for (int i = 0; i < itemIds.size(); i++) {
			OrderLineWS nextLine = new OrderLineWS();
			nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(itemIds.get(i));
			nextLine.setQuantity(1);
			nextLine.setPrice(linePrice);
			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));

			lines.add(nextLine);
		}
		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
		return order;
	}

	private InvoiceWS buildInvoice(Integer userId, Integer itemId) {
		InvoiceWS invoice = new InvoiceWS();
		invoice.setUserId(userId);
		invoice.setNumber("800" + System.currentTimeMillis());
		invoice.setTotal("15");
		invoice.setToProcess(1);
		invoice.setBalance("15");
		invoice.setCurrencyId(CURRENCY_USD);
		invoice.setDueDate(new Date());
		invoice.setPaymentAttempts(1);
		invoice.setInProcessPayment(1);
		invoice.setCarriedBalance("0");

		InvoiceLineDTO invoiceLineDTO = new InvoiceLineDTO();
		invoiceLineDTO.setAmount("15");
		invoiceLineDTO.setDescription("line desc");
		invoiceLineDTO.setItemId(itemId);
		invoiceLineDTO.setPercentage(1);
		invoiceLineDTO.setPrice("15");
		invoiceLineDTO.setQuantity("1");
		invoiceLineDTO.setSourceUserId(userId);
		invoiceLineDTO.setTypeId(3);

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

	private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId) {
		ItemDTOEx item = new ItemDTOEx();
		long millis = System.currentTimeMillis();
		String name = String.valueOf(millis) + new Random().nextInt(10000);
		item.setDescription("Payment, Product:" + name);
		item.setPriceModelCompanyId(priceModelCompanyId);
		item.setPrice(new BigDecimal("10"));
		item.setNumber("PYM-PROD-" + name);
		item.setAssetManagementEnabled(0);
		Integer typeIds[] = new Integer[]{itemTypeId};
		item.setTypes(typeIds);
		return item;
	}

	private MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields, String fieldName) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				return ws;
			}
		}
		return null;
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
		OrderChangeStatusWS status1 = new OrderChangeStatusWS();
		status1.setApplyToOrder(ApplyToOrder.YES);
		status1.setDeleted(0);
		status1.setOrder(1);
		status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

	private Integer getOrCreateSuspendedStatus(JbillingAPI api) {
		List<AgeingWS> steps = Arrays.asList(api.getAgeingConfiguration(LANGUAGE_ID));
		for (AgeingWS step : steps) {
			if (step.getSuspended().booleanValue()) {
				return step.getStatusId();
			}
		}

		AgeingWS suspendStep = new AgeingWS();
		suspendStep.setSuspended(Boolean.TRUE);
		suspendStep.setDays(Integer.valueOf(180));
		suspendStep.setStatusStr("Ageing Step 180");
		suspendStep.setCollectionType(CollectionType.REGULAR);
        suspendStep.setSendNotification(Boolean.FALSE);
        suspendStep.setPaymentRetry(Boolean.FALSE);
        suspendStep.setInUse(Boolean.FALSE);
        suspendStep.setStopActivationOnPayment(Boolean.FALSE);
		steps.add(suspendStep);
		api.saveAgeingConfiguration(steps.toArray(new AgeingWS[steps.size()]), LANGUAGE_ID);
		return getOrCreateOrderChangeStatusApply(api);
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

    private Integer createPayment(JbillingAPI api, Integer paymentMethodId, String amount, boolean isRefund,
            Integer userId, Integer linkedPaymentId, PaymentInformationWS paymentInformationWS) {
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal(amount));
        payment.setIsRefund(isRefund ? new Integer(1) : new Integer(0));
        payment.setMethodId(paymentMethodId);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setResultId(Constants.RESULT_ENTERED);
        payment.setCurrencyId(CURRENCY_USD);
        payment.setUserId(userId);
        payment.setPaymentNotes("Notes");
        payment.setPaymentPeriod(new Integer(1));
        payment.setPaymentId(linkedPaymentId);

        payment.getPaymentInstruments().add(paymentInformationWS);

        logger.debug("Creating {}", isRefund ? " refund." : " payment.");
        return api.createPayment(payment);
    }

    private PaymentInformationWS createCheque(String bankName, String chequeNumber, Date date) {
        PaymentInformationWS cheque = new PaymentInformationWS();
        cheque.setPaymentMethodTypeId(CHEQUE_PM_ID);
        cheque.setPaymentMethodId(Constants.PAYMENT_METHOD_CHEQUE);
        cheque.setProcessingOrder(new Integer(3));

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CHEQUE_MF_BANK_NAME, false, true,
                DataType.STRING, 1, bankName);
        addMetaField(metaFields, CHEQUE_MF_NUMBER, false, true,
                DataType.STRING, 2, chequeNumber);
        addMetaField(metaFields, CHEQUE_MF_DATE, false, true,
            DataType.DATE, 3, date);
        cheque.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cheque;
    }

    private void addMetaField(List<MetaFieldValueWS> metaFields,
        String fieldName, boolean disabled, boolean mandatory,
        DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }
}
