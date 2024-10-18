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

package com.sapienter.jbilling.server.process;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.server.item.ItemDTOEx;



/**
 * AgeingStopActivationOnPaymentTest
 *
 * @author Mahesh Shivarkar
 * @since 05/01/2017
 */
@Test(groups = { "integration", "ageing" }, testName = "ageing.AgeingStopActivationOnPaymentTest", priority = 17)
public class AgeingStopActivationOnPaymentTest {

	private static final Logger logger = LoggerFactory.getLogger(AgeingStopActivationOnPaymentTest.class);
	private final static int AGEING_STEP_PAUSE_FOR_PROVISIONING = 2500; // milliseconds
	private final static String BLACK_LIST_USER_STATUS_TASK = "com.sapienter.jbilling.server.payment.blacklist.tasks.BlacklistUserStatusTask";

	private TestBuilder testBuilder;
	private TestEnvironment environment;
	private EnvironmentHelper environmentHelper;

	private static final String CATEGORY_CODE = "TestCategory";
	private static final String PRODUCT_CODE = "TestProduct";
	private static final String ACCOUNT_TYPE_CODE = "TestAccountType";

	private static final String CUSTOMER_CODE1 = "TestCustomer1";
	private static final String CUSTOMER_CODE2 = "TestCustomer2";
	private static final String CUSTOMER_CODE3 = "TestCustomer3";
	private static final String CUSTOMER_CODE4 = "TestCustomer4";
	private static final String CUSTOMER_CODE5 = "TestCustomer5";

	private final static Integer CC_PM_ID = 5;

	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	private final static String CREDIT_CARD_NUMBER = "5257279846844529";
	public final static int ONE_TIME_ORDER_PERIOD = 1;
	public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private static final Integer postPaidOrderTypeId = Constants.ORDER_BILLING_POST_PAID;

	private Integer CATEGORY_ID;

	@BeforeClass
	public void initializeTests(){
		testBuilder = getTestEnvironment();
		environment = testBuilder.getTestEnvironment();
		final JbillingAPI api = environment.getPrancingPonyApi();
		ageingConfiguration(api,true);
	}


	@AfterClass
	public void tearDown(){
		final JbillingAPI api = environment.getPrancingPonyApi();
		// Save ageing configuration as earlier
		ageingConfiguration(api,false);

		PluggableTaskWS pluggableTask = new PluggableTaskWS();
		pluggableTask.setProcessingOrder(999);
		PluggableTaskTypeWS blackListPluginType =
				api.getPluginTypeWSByClassName(BLACK_LIST_USER_STATUS_TASK);
		pluggableTask.setTypeId(blackListPluginType.getId());
		api.createPlugin(pluggableTask);

		testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
		if (null != environmentHelper){
			environmentHelper = null;
		}
		if (null != testBuilder){
			testBuilder = null;
		}
	}


	private TestBuilder getTestEnvironment() {
		return TestBuilder.newTest(false).givenForMultiple(envCreator -> {
			final JbillingAPI api = envCreator.getPrancingPonyApi();
			environmentHelper = EnvironmentHelper.getInstance(api);

			CATEGORY_ID = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(true).build();
			envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(CATEGORY_ID)
					.withFlatPrice("5.00").build();
			envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();

		});
	}

	/**
	 * Check User Status after making full payment
	 * & Stop activation on payment is checked
	 * User next invoice date: 1st Jan 2016
	 * Order:
	 * ActiveSince: 1st Dec 2015
	 * EffectiveDate: 1st Dec 2015
	 */

	@Test(priority = 1)
	public void test001CheckUserStatusAfterFullPayment(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2015);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE1, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.debug("### userId: {}", userId);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();
			logger.debug("### order Id: {}", api.getLatestOrder(userId));
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			Date billingDate = new LocalDate(2016,01,01).toDate();
			Integer[] invoiceIdsOfUser= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE1),billingDate, null, null, false);
			assertNotNull("invoiceIdsOfUser should not be null",invoiceIdsOfUser);
			logger.debug("### invoiceIdsOfUser: {}", invoiceIdsOfUser[0]);

			Date billingDate2 = new LocalDate(2016,02,01).toDate();

			UserWS userWS = api.getUserWS(environment.idForCode(CUSTOMER_CODE1));
			userWS.setNextInvoiceDate(billingDate2);
			api.updateUser(userWS);

			Integer[] invoiceIdsOfUser2= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE1),billingDate2, null, null, false);
			assertNotNull("invoiceIdsOfUser2 should not be null",invoiceIdsOfUser2);
			logger.debug("### invoiceIdsOfUser2: {}", invoiceIdsOfUser2[0]);

			Date ageingDate = new LocalDate(2016,02,03).toDate();

			api.triggerAgeing(ageingDate);

			Integer userId = env.idForCode(CUSTOMER_CODE1);
			assertNotNull("UserId should not be null",userId);
			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 2);
			paymentDate.set(Calendar.DAY_OF_MONTH, 5);

			// Test user status after partial payment
			makePayment("2.5", paymentDate.getTime(), false,CUSTOMER_CODE1);

			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			// Test user status after full payment
			makePayment("2.5", paymentDate.getTime(), false,CUSTOMER_CODE1);

			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			updateCustomerStatusToActive(userWS.getId(), api);
		});
	}

	/**
	 *Check User Status with Credit Note & Stop activation on payment is checked
	 * User next invoice date: 1st Jan 2016
	 * ActiveSince: 1st Dec 2015
	 * EffectiveDate: 1st Dec 2015
	 */


	@Test(priority = 2)
	public void test002CheckUserStatusWithCreditNote(){
		final Date activeSince = FullCreativeUtil.getDate(11,01,2015);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE2, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.debug("### userId: {}", userId);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();
			// Check user status after creating credit note
			createOrder("OneTimeFeeOrderO1", activeSince,null, ONE_TIME_ORDER_PERIOD,postPaidOrderTypeId, ORDER_CHANGE_STATUS_APPLY_ID, false,
					Collections.singletonMap(environment.idForCode(PRODUCT_CODE), BigDecimal.ONE), null, true,CUSTOMER_CODE2);

			logger.debug("### order Id: {}", api.getLatestOrder(userId));
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			Date billingDate = new LocalDate(2016,01,01).toDate();
			Integer[] invoiceIdsOfUser= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE2),billingDate, null, null, false);
			assertNotNull("invoiceIdsOfUser should not be null",invoiceIdsOfUser);
			logger.debug("### invoiceIdsOfUser: " + invoiceIdsOfUser[0]);

			Date billingDate2 = new LocalDate(2016,02,01).toDate();

			UserWS userWS = api.getUserWS(environment.idForCode(CUSTOMER_CODE2));
			userWS.setNextInvoiceDate(billingDate2);
			api.updateUser(userWS);

			Integer[] invoiceIdsOfUser2= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE2),billingDate2, null, null, false);
			assertNotNull("invoiceIdsOfUser2 should not be null",invoiceIdsOfUser2);
			logger.debug("### invoiceIdsOfUser2: {}", invoiceIdsOfUser2[0]);

			Date ageingDate = new LocalDate(2016,02,03).toDate();

			api.triggerAgeing(ageingDate);

			Integer userId = env.idForCode(CUSTOMER_CODE2);
			assertNotNull("UserId should not be null",userId);
			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			updateCustomerStatusToActive(userWS.getId(), api);
		});
	}

	/**
	  Check User Status after unlinking/linking payment using apply payment
	 * & Stop activation on payment is checked
	 * User next invoice date: 1st Jan 2016
	 * Order:
	 * ActiveSince: 1st Dec 2015
	 * EffectiveDate: 1st Dec 2015
	 */


	@Test(priority = 3)
	public void test003CheckUserStatusAfterApplyPayment(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2015);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE3, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.debug("### userId: {}", userId);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();
			logger.debug("### order Id: {}", api.getLatestOrder(userId));
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			Date billingDate = new LocalDate(2016,01,01).toDate();
			Integer[] invoiceIdsOfUser= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE3),billingDate, null, null, false);
			assertNotNull("invoiceIdsOfUser should not be null",invoiceIdsOfUser);
			logger.debug("### invoiceIdsOfUser: " + invoiceIdsOfUser[0]);

			Date billingDate2 = new LocalDate(2016,02,01).toDate();

			UserWS userWS = api.getUserWS(environment.idForCode(CUSTOMER_CODE3));
			userWS.setNextInvoiceDate(billingDate2);
			api.updateUser(userWS);

			Integer[] invoiceIdsOfUser2= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE3),billingDate2, null, null, false);
			assertNotNull("invoiceIdsOfUser2 should not be null",invoiceIdsOfUser2);
			logger.debug("### invoiceIdsOfUser2: {}", invoiceIdsOfUser2[0]);

			Date ageingDate = new LocalDate(2016,02,03).toDate();

			api.triggerAgeing(ageingDate);

			Integer userId = env.idForCode(CUSTOMER_CODE3);
			assertNotNull("UserId should not be null",userId);
			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 2);
			paymentDate.set(Calendar.DAY_OF_MONTH, 5);

			// Test user status after full payment
			makePayment("10.00", paymentDate.getTime(), false,CUSTOMER_CODE3);

			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			// Unlink the payment from invoices & check user status
			Integer[] invoices = api.getAllInvoices(userId);

			Integer[] payments = api.getPaymentsByUserId(userId);

			api.removeAllPaymentLinks(payments[0]);

			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			// Link the payment back to invoices & check user status
			api.createPaymentLink(invoices[0], payments[0]);

			api.createPaymentLink(invoices[1], payments[0]);


			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			updateCustomerStatusToActive(userWS.getId(), api);
		});
	}

	/**
	 * Manully Suspend User and then make full Payment
	 * & check user status
	 * User next invoice date: 1st Jan 2016
	 * Order:
	 * ActiveSince: 1st Dec 2015
	 * EffectiveDate: 1st Dec 2015
	 */

	@Test(priority = 4)
	public void test004ManullySuspendUserAndThenMakeFullPayment(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2015);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE4, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.debug("### userId: {}", userId);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();
			logger.debug("### order Id: {}", api.getLatestOrder(userId));
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			Date billingDate = new LocalDate(2016,01,01).toDate();
			Integer[] invoiceIdsOfUser= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE4),billingDate, null, null, false);
			assertNotNull("invoiceIdsOfUser should not be null",invoiceIdsOfUser);
			logger.debug("### invoiceIdsOfUser: {}", invoiceIdsOfUser[0]);

			Date billingDate2 = new LocalDate(2016,02,01).toDate();

			UserWS userWS = api.getUserWS(environment.idForCode(CUSTOMER_CODE4));
			userWS.setNextInvoiceDate(billingDate2);
			api.updateUser(userWS);

			Integer[] invoiceIdsOfUser2= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE4),billingDate2, null, null, false);
			assertNotNull("invoiceIdsOfUser2 should not be null",invoiceIdsOfUser2);
			logger.debug("### invoiceIdsOfUser2: {}", invoiceIdsOfUser2[0]);

			// Manually suspend the user & then make full payment
			userWS = api.getUserWS(environment.idForCode(CUSTOMER_CODE4));
			userWS.setStatusId(Integer.valueOf(2));
			api.updateUser(userWS);

			Integer userId = env.idForCode(CUSTOMER_CODE4);
			assertNotNull("UserId should not be null",userId);
			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			userId = env.idForCode(CUSTOMER_CODE4);
			assertNotNull("UserId should not be null",userId);
			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 2);
			paymentDate.set(Calendar.DAY_OF_MONTH, 5);

			// Test user status after full payment
			makePayment("10.00", paymentDate.getTime(), false,CUSTOMER_CODE4);

			userWS = api.getUserWS(userId);
			// User status should be suspended = 2
			assertEquals("UserId status id should be",Integer.valueOf(2),userWS.getStatusId());

		});
	}


	@Test(priority = 5)
	public void test005AgeUserToLastStatusThenDoPaymentAndCheckUserStatus(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2015);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE5, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.debug("### userId: {}", userId);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();
			logger.debug("### order Id: {}", api.getLatestOrder(userId));
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			PluggableTaskWS[] pluggableTask = api.getPluginsWS(api.getCallerCompanyId(), "com.sapienter.jbilling.server.payment.blacklist.tasks.BlacklistUserStatusTask");
			logger.debug("### pluggableTaskId: {}", pluggableTask[0].getId());
			if (null != pluggableTask[0]) {api.deletePlugin(pluggableTask[0].getId());}

				Date billingDate2 = new LocalDate(2016,02,01).toDate();

				UserWS userWS = api.getUserWS(environment.idForCode(CUSTOMER_CODE5));
				userWS.setNextInvoiceDate(billingDate2);
				api.updateUser(userWS);

				Integer[] invoiceIdsOfUser2= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE5),billingDate2, null, null, false);
				assertNotNull("invoiceIdsOfUser2 should not be null",invoiceIdsOfUser2);
				logger.debug("### invoiceIdsOfUser2: {}", invoiceIdsOfUser2[0]);

				Date ageingDate2 = new LocalDate(2016,02,03).toDate();
				logger.debug("## ageingDate2: {}", ageingDate2);
				api.triggerAgeing(ageingDate2);

				Calendar calendar = Calendar.getInstance();
				Date dueDate = api.getInvoiceWS(invoiceIdsOfUser2[0]).getDueDate();
				calendar.setTime(Util.addDays(dueDate, 1));
				logger.debug("Day 1 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 1", UserDTOEx.STATUS_ACTIVE + 1, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 4));
				logger.debug("Day 4 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 4", UserDTOEx.STATUS_ACTIVE + 2, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 5));
				logger.debug("Day 5 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 5", UserDTOEx.STATUS_ACTIVE + 3, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 15));
				logger.debug("Day 15 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 15", UserDTOEx.STATUS_ACTIVE + 4, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 16));
				logger.debug("Day 16 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 16", UserDTOEx.STATUS_ACTIVE + 5, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 27));
				logger.debug("Day 27 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 27", UserDTOEx.STATUS_ACTIVE + 6, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 30));
				logger.debug("Day 30 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 30", UserDTOEx.STATUS_ACTIVE + 7, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 31));
				logger.debug("Day 31 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 31", UserDTOEx.STATUS_ACTIVE + 8, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 32));
				logger.debug("Day 32 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 32", UserDTOEx.STATUS_ACTIVE + 9, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 40));
				logger.debug("Day 40 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 40", UserDTOEx.STATUS_ACTIVE + 10, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 45));
				logger.debug("Day 45 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 45", UserDTOEx.STATUS_ACTIVE + 11, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 50));
				logger.debug("Day 50 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 50", UserDTOEx.STATUS_ACTIVE + 12, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 55));
				logger.debug("Day 55 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 55", UserDTOEx.STATUS_ACTIVE + 13, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 65));
				logger.debug("Day 65 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 65", UserDTOEx.STATUS_ACTIVE + 14, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 90));
				logger.debug("Day 90 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 90", UserDTOEx.STATUS_ACTIVE + 15, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 180));
				logger.debug("Day 180 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 180", UserDTOEx.STATUS_ACTIVE + 16, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 190));
				logger.debug("Day 190 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 190", UserDTOEx.STATUS_ACTIVE + 17, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				calendar.setTime(Util.addDays(dueDate, 200));
				logger.debug("Day 200 status on: {}", calendar.getTime());
				api.triggerAgeing(calendar.getTime());
				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 200", UserDTOEx.STATUS_ACTIVE + 18, userWS.getStatusId().intValue());

				pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH, 7);
				paymentDate.set(Calendar.DAY_OF_MONTH, 19);

				// Test user status after full payment
				makePayment("10.00", paymentDate.getTime(), false,CUSTOMER_CODE5);

				userWS = api.getUserWS(userWS.getId());
				assertEquals("User status should be Day 200", UserDTOEx.STATUS_ACTIVE + 18, userWS.getStatusId().intValue());

				updateCustomerStatusToActive(userWS.getId(), api);
		});
	}

	private Integer createCustomer(TestEnvironmentBuilder envBuilder,String code, Integer accountTypeId, Date nid){
		final JbillingAPI api = envBuilder.getPrancingPonyApi();

		CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
				.withUsername(code).withAccountTypeId(accountTypeId)
				.withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), getDay(nid)));

		UserWS user = customerBuilder.build();
		user.setNextInvoiceDate(nid);
		api.updateUser(user);
		return user.getId();
	}

	private static Integer getDay(Date inputDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(inputDate);
		return Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
	}

	private void makePayment(String amount, Date paymentDate, boolean isRefund, String customerCode) {

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal(amount));
		payment.setIsRefund(isRefund ? 1 : 0);
		payment.setPaymentDate(paymentDate);
		payment.setCreateDatetime(paymentDate);
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(testBuilder.getTestEnvironment().idForCode(customerCode));
		PaymentWS lastPayment = testBuilder.getTestEnvironment().getPrancingPonyApi().getLatestPayment(payment.getUserId());
		payment.setPaymentId(isRefund ? lastPayment.getId() : CC_PM_ID);
		payment.setResultId(Constants.RESULT_ENTERED);
		Calendar expiryDate = Calendar.getInstance();
		expiryDate.add(Calendar.YEAR, 10);

		payment.setPaymentInstruments(Arrays.asList(createCreditCard(UUID.randomUUID().toString(), CREDIT_CARD_NUMBER, expiryDate.getTime(), 2)));

		PaymentAuthorizationDTOEx authInfo = testBuilder.getTestEnvironment().getPrancingPonyApi().processPayment( payment, null);
		assertNotNull("Payment result not null", authInfo);
	}

	private PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date, Integer methodId) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Integer.valueOf(methodId));

		//cc

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, new SimpleDateFormat(Constants.CC_DATE_FORMAT).format(date).toCharArray());

		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, false, false,
				DataType.STRING, 4, CreditCardType.MASTER_CARD);
		addMetaField(metaFields, "cc.gateway.key" ,false, true, DataType.CHAR, 5, null );
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
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

	private void createOrder(String code,Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId, int statusId,
			boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, Integer> productAssetMap, boolean createNegativeOrder, String userName) {
		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			List<OrderLineWS> lines = productQuantityMap.entrySet()
					.stream()
					.map((lineItemQuatityEntry) -> {
						OrderLineWS line = new OrderLineWS();
						line.setItemId(lineItemQuatityEntry.getKey());
						line.setTypeId(Integer.valueOf(1));
						ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
						line.setDescription(item.getDescription());
						line.setQuantity(lineItemQuatityEntry.getValue());
						line.setUseItem(true);
						if(createNegativeOrder) {
							line.setUseItem(false);
							line.setPrice(item.getPriceAsDecimal().negate());
							line.setAmount(line.getQuantityAsDecimal().multiply(line.getPriceAsDecimal()));
						}
						if(null!=productAssetMap && !productAssetMap.isEmpty()
								&& productAssetMap.containsKey(line.getItemId())) {
							line.setAssetIds(new Integer[] {productAssetMap.get(line.getItemId())});
						}
						return line;
					}).collect(Collectors.toList());

			envBuilder.orderBuilder(api)
			.withCodeForTests(code)
			.forUser(envBuilder.idForCode(userName))
			.withActiveSince(activeSince)
			.withActiveUntil(activeUntil)
			.withEffectiveDate(activeSince)
			.withPeriod(orderPeriodId)
			.withBillingTypeId(billingTypeId)
			.withProrate(prorate)
			.withOrderLines(lines)
			.withOrderChangeStatus(statusId)
			.build();

		}).test((testEnv, envBuilder) -> {
			assertNotNull("Order Creation Failed", envBuilder.idForCode(code));

		});

	}

	private void pause(long t)  {
		logger.debug("pausing for {} ms...", t);
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void ageingConfiguration(JbillingAPI api,Boolean stopActivationOnPayment) {
		// Save ageing configuration as earlier
		List<AgeingWS> steps = Arrays.asList(api.getAgeingConfiguration(1));
		api.saveAgeingConfiguration(steps.stream()
			 .map(step -> {
				 if(step.getDays() == 1 || step.getDays() == 200) {
					 step.setStopActivationOnPayment(stopActivationOnPayment);
				 }
				 step.setCollectionType(CollectionType.REGULAR);
				 return step;
			 }).<AgeingWS>toArray(AgeingWS[]::new),1);

	}

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }
}
