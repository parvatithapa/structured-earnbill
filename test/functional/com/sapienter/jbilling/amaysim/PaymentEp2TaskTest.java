package com.sapienter.jbilling.amaysim;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
@Test(groups = { "external-system" }, testName = "PaymentEp2TaskTest")
public class PaymentEp2TaskTest {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private JbillingAPI api;
	private static final Integer EP2_GATE_WAY_TOKEN_METHOD_ID = 147;
	private static final int PAYMENT_FAKE_TASK_ID1 = 20;
	private static final int PAYMENT_ROUTER_CCF_TASK_ID = 21;
	private static final int PAYMENT_FAKE_TASK_ID2 = 22;
	private static final int PAYMENT_FILTER_TASK_ID = 460;
	private static final int PAYMENT_ROUTER_CUREENCY_TASK_ID = 520;
	private static final String CC_DATE_FORMAT = "MM/yyyy";
	private static final String CC_MF_TOKEN_ID = "cc.token.id";
	private static final String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private static final String CC_MF_AUTOPAYMENT_AUTHORIZATION = "autopayment.authorization";
	private static final String TEST_TOKEN_ID = "4250440969996089";
	private static final String PAYMNET_EP2_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentEP2Task";
	private static final String PAYMNET_PROCESS_TASK_CLASS_NAME = "com.sapienter.jbilling.server.billing.task.PaymentProcessTask";
	private static Integer PAYMENT_PROCESS_TASK_ID = 831734;
	private static AccountTypeWS account = null;
	private static Integer paymentEP2PluginId = null;
	private static Integer PRODUCT_CALL_LD_GEN_ID = 2900;

	@BeforeClass
	protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		account = new AccountTypeBuilder()
		.addDescription("Test Account Type For EP2 Payment Testing " + System.currentTimeMillis(), api.getCallerLanguageId())
		.paymentMethodTypeIds(new Integer[]{EP2_GATE_WAY_TOKEN_METHOD_ID})
		.create(api);
		assertNotNull("Account Creation  Failed", account);

		updateProcessingOrderOfPlugin(api, PAYMENT_FAKE_TASK_ID1);
		updateProcessingOrderOfPlugin(api, PAYMENT_FAKE_TASK_ID2);
		updateProcessingOrderOfPlugin(api, PAYMENT_FILTER_TASK_ID);
		updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CCF_TASK_ID);
		updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CUREENCY_TASK_ID);

		PluggableTaskWS paymentEP2Plugin = new PluggableTaskWS();
		paymentEP2Plugin.setProcessingOrder(1);
		paymentEP2Plugin.setTypeId(api.getPluginTypeWSByClassName(PAYMNET_EP2_TASK_CLASS_NAME).getId());
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters.put("MerchantId", "266d089c-798d-4a93-b2a5-42207a608756");
		parameters.put("UserName", "amaysim_test5");
		parameters.put("Password", "Tomcat123");
		parameters.put("url", "https://testapi.ep2-global.com/engine/rest/payments/");
		paymentEP2Plugin.setParameters(parameters);
		paymentEP2Plugin.setOwningEntityId(api.getCallerCompanyId());
		paymentEP2PluginId = api.createPlugin(paymentEP2Plugin);
		parameters.clear();
		parameters.put("cron_exp", "0 0 12 * * ?");
		try {
			PluggableTaskWS paymentProcessTask = api.getPluginWS(PAYMENT_PROCESS_TASK_ID);
			paymentProcessTask.setParameters(parameters);
			paymentProcessTask.setProcessingOrder(1234);
			api.updatePlugin(paymentProcessTask);
		} catch(Exception ex) {

			PluggableTaskWS paymentProcessTask = new PluggableTaskWS();
			paymentProcessTask.setTypeId(api.getPluginTypeWSByClassName(PAYMNET_PROCESS_TASK_CLASS_NAME).getId());
			paymentProcessTask.setProcessingOrder(1234);
			paymentProcessTask.setParameters(parameters);
			PAYMENT_PROCESS_TASK_ID = api.createPlugin(paymentProcessTask);
		}
        /**
         * Below change is make ep2 payment method same as that of on production.
         *
         */
		PaymentMethodTypeWS methodTypeWS = api.getPaymentMethodType(EP2_GATE_WAY_TOKEN_METHOD_ID);
        List<MetaFieldWS> list = new ArrayList<>(Arrays.asList(methodTypeWS.getMetaFields()));

        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setName("cc.number");
        metaField.setDisplayOrder(1);
        metaField.setFieldUsage(null);
        metaField.setValidationRule(null);
        metaField.setDataType(DataType.CHAR);
        metaField.setEntityType(EntityType.PAYMENT_METHOD_TYPE);

        list.add(metaField);
        methodTypeWS.setMetaFields(list.toArray(new MetaFieldWS[list.size()]));
        list.stream().forEach(metafield -> {
        if (CC_MF_TOKEN_ID.equals(metafield.getName())) {
                metafield.setDisabled(false);
			}
        });
        api.updatePaymentMethodType(methodTypeWS);
	}

	@Test
	public void test001Ep2Payment() throws Exception {
		UserWS user = createUser(new Date(), true);
		assertNotNull("User Creation  Failed", user);

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("30"));
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(user.getUserId());
		PaymentInformationWS cc = user.getPaymentInstruments().get(0);
		payment.getPaymentInstruments().add(cc);

		// Testing first payment through EP2 GateWay
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
		assertNotNull("Payment result not null", authInfo);
		assertNotNull("Transaction id not null", authInfo.getTransactionId());
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());
		assertNotNull("Approval Code Should not be null", authInfo.getApprovalCode());
		assertNotNull("Response Code Should not be null", authInfo.getCode1());
		// check payment has zero balance
		PaymentWS lastPayment = api.getLatestPayment(user.getId());

		Integer paymentID = lastPayment.getId();
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());

		  // Refund Test
        PaymentWS secondPayment = new PaymentWS();
        secondPayment.setPaymentId( paymentID );
        secondPayment.setAmount(new BigDecimal("20"));
        secondPayment.setIsRefund(1);
        secondPayment.setPaymentDate(Calendar.getInstance().getTime());
        secondPayment.setCurrencyId(Integer.valueOf(1));
        secondPayment.setUserId(user.getUserId());
        secondPayment.getPaymentInstruments().add(cc);

        //  Testing second payment through  EP2 GateWay
        logger.debug("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo1 = api.processPayment(secondPayment, null);
        assertNotNull("Payment result not null", authInfo1);
        assertTrue("Payment Authorization result should be OK", authInfo1.getResult().booleanValue());
        assertNotNull("Approval Code Should not be null", authInfo1.getApprovalCode());
        assertNotNull("Response Code Should not be null", authInfo1.getCode1());
        // check payment has zero balance
        PaymentWS lastPayment1 = api.getLatestPayment(user.getUserId());
        assertNotNull("payment can not be null", lastPayment1);
        assertNotNull("auth in payment can not be null", lastPayment1.getAuthorizationId());
        assertEquals("correct payment amount", new BigDecimal("20.0000000000"), lastPayment1.getAmountAsDecimal());
        assertEquals("correct payment balance", new BigDecimal("0E-10"), lastPayment1.getBalanceAsDecimal());

	}

	@Test
	public void test002OneTimeEp2Payment() throws Exception {
		UserWS user = createUser(new Date(), false);
		assertNotNull("User Creation  Failed", user);

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("30"));
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(user.getUserId());

		// Creating Payment Instrument For One Time Payment.
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
		PaymentInformationWS cc = createCreditCard(TEST_TOKEN_ID, expiry.getTime());

		payment.getPaymentInstruments().add(cc);

		// Testing first payment through EP2 GateWay
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
		assertNotNull("Payment result not null", authInfo);
		assertNotNull("Transaction id not null", authInfo.getTransactionId());
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());
		assertNotNull("Approval Code Should not be null", authInfo.getApprovalCode());
		assertNotNull("Response Code Should not be null", authInfo.getCode1());
		// check payment has zero balance
		PaymentWS lastPayment = api.getLatestPayment(user.getId());

		Integer paymentID = lastPayment.getId();
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());

		  // Refund Test
        PaymentWS secondPayment = new PaymentWS();
        secondPayment.setPaymentId( paymentID );
        secondPayment.setAmount(new BigDecimal("20"));
        secondPayment.setIsRefund(1);
        secondPayment.setPaymentDate(Calendar.getInstance().getTime());
        secondPayment.setCurrencyId(Integer.valueOf(1));
        secondPayment.setUserId(user.getUserId());
        secondPayment.getPaymentInstruments().add(cc);

        //  Testing second payment through  EP2 GateWay
        logger.debug("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo1 = api.processPayment(secondPayment, null);
        assertNotNull("Payment result not null", authInfo1);
        assertTrue("Payment Authorization result should be OK", authInfo1.getResult().booleanValue());
        assertNotNull("Approval Code Should not be null", authInfo1.getApprovalCode());
        assertNotNull("Response Code Should not be null", authInfo1.getCode1());

        // check payment has zero balance
        PaymentWS lastPayment1 = api.getLatestPayment(user.getUserId());
        assertNotNull("payment can not be null", lastPayment1);
        assertNotNull("auth in payment can not be null", lastPayment1.getAuthorizationId());
        assertEquals("correct payment amount", new BigDecimal("20.0000000000"), lastPayment1.getAmountAsDecimal());
        assertEquals("correct payment balance", new BigDecimal("0E-10"), lastPayment1.getBalanceAsDecimal());

	}

	@Test
	public void test003RecurringEp2Payment() throws Exception { 

		enableAutoPaymentProcessInBillingProcessConfiguration();
		UserWS user = createUser(new Date(), true);
		assertNotNull("User Creation  Failed", user);

		updateCustomerNextInvoiceDate(user.getId(), api);

		Integer invoiceId = createOrder(user.getId(), new Date());
		assertNotNull("Order and Invoice Creation  Failed", invoiceId);

		api.triggerScheduledTask(PAYMENT_PROCESS_TASK_ID, new Date());

		logger.debug("Waiting to complete Execution on PaymentProcessTask....");
		
		sleep(15000);

		PaymentWS lastPayment = api.getLatestPayment(user.getUserId());
		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("100.0000000000"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance", new BigDecimal("0E-10"), lastPayment.getBalanceAsDecimal());
	}

	@Test
	public void test004EnteredEp2Payment() throws Exception {
		UserWS user = createUser(new Date(), true);
		assertNotNull("User Creation  Failed", user);
		Integer pmId = makePayment("10", new Date(), false, user, true, null);
		assertNotNull("Payment Id can not be null", pmId);
		PaymentWS paymentWS = api.getPayment(pmId);
		assertEquals("payment result value should be ENTERED ",Constants.RESULT_ENTERED,paymentWS.getResultId());
	}

	/**
	 * test for ep2 refund payment having entered status
	 * @throws Exception
	 */
	@Test
	public void test005EnteredRefundEp2Payment() throws JbillingAPIException, IOException {
		UserWS user = createUser(new Date(), true);
		assertNotNull("User Creation  Failed", user);
		Integer pmId = makePayment("10", new Date(), false, user, true, null);
		assertNotNull("Payment Creation  Failed", pmId);
		Integer pmId2 = makePayment("10", new Date(), true, user, true, pmId);
		assertNotNull("Payment Id can not null", pmId2);
		PaymentWS paymentWS = api.getPayment(pmId2);
		assertEquals("payment result value should be ENTERED ",Constants.RESULT_ENTERED,paymentWS.getResultId());
	}

	/**
	 * test for ep2 refund payment having successful status (not Entered)
	 * @throws Exception
	 */
	@Test
	public void test006SuccessfulRefundEp2Payment() throws JbillingAPIException, IOException {
			UserWS user = createUser(new Date(), true);
			assertNotNull("User Creation  Failed", user);
			Integer pmId = makePayment("10", new Date(), false, user, false, null);
			assertNotNull("Payment Creation  Failed", pmId);
			Integer pmId2 = makePayment("10", new Date(), true, user, false, pmId);
			assertNotNull("Payment Id can not null", pmId2);
			PaymentWS paymentWS = api.getPayment(pmId2);
			assertEquals("payment result value should be SUCCESSFUL", Constants.PAYMENT_RESULT_SUCCESSFUL,paymentWS.getResultId());
			assertEquals("payment result should be REFUND and ENTERED", Constants.PAYMENT_RESULT_SUCCESSFUL,paymentWS.getResultId());
	}

	private Integer makePayment(String amount, Date paymentDate, boolean isRefund, UserWS user, boolean isEntered, Integer paymentId) {

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal(amount));
		payment.setIsRefund(isRefund ? 1 : 0);
		payment.setPaymentDate(paymentDate);
		payment.setCreateDatetime(paymentDate);
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(user.getId());
		payment.setPaymentId(paymentId);
		payment.setResultId(Constants.RESULT_ENTERED);

		payment.setPaymentInstruments(user.getPaymentInstruments());

		if(isEntered) {
			api.createPayment(payment);
		} else {
			api.processPayment(payment, null);
		}

		PaymentWS lastPayment = api.getLatestPayment(user.getId());

		return lastPayment.getId();

	}

	private void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch(InterruptedException ex) {

		}
	}

	private Integer createOrder(Integer userId, Date activeSinceDate) {
		// create an order for $10,
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(new Integer(1)); // One Time Order Period
		order.setCurrencyId(new Integer(1));
		order.setActiveSince(activeSinceDate);

		OrderLineWS line = new OrderLineWS();
		line.setPrice(new BigDecimal("100.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setItemId(PRODUCT_CALL_LD_GEN_ID);
		line.setDescription("Order created by test-case");

		order.setOrderLines(new OrderLineWS [] {line});

		OrderChangeWS orderChanges [] = OrderChangeBL.buildFromOrder(order, 3);

		for(OrderChangeWS orderChange : orderChanges) {
			orderChange.setStartDate(activeSinceDate);
			orderChange.setApplicationDate(activeSinceDate);
		}

		Integer invoiceId = api.createOrderAndInvoice(order, orderChanges);
		return invoiceId;
	}

	private void enableAutoPaymentProcessInBillingProcessConfiguration() {
		BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
		config.setNextRunDate(config.getNextRunDate());
		config.setRetries(new Integer(10));
		config.setDaysForRetry(new Integer(5));
		config.setGenerateReport(new Integer(0));
		config.setAutoPaymentApplication(new Integer(1));
		config.setAutoPayment(new Integer(1));
		//config.setStartTime("1100");
		config.setDfFm(new Integer(0));
		config.setRetryCount(12);
		//config.setInterval(1);
		config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
		config.setDueDateValue(new Integer(0));
		config.setInvoiceDateProcess(new Integer(0));
		config.setMaximumPeriods(new Integer(99));
		config.setOnlyRecurring(new Integer(0));

		logger.debug("B - Setting config to: {}", config);
		api.createUpdateBillingProcessConfiguration(config);

	}

	public static UserWS createUser(Date nextInvoiceDate, boolean addPaymentInstument) throws JbillingAPIException, IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS newUser = new UserWS();
		List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
		newUser.setUserId(0);
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword12");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(account.getId());
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));

		logger.debug("User properties set");
		MetaFieldValueWS metaField7 = new MetaFieldValueWS();
		metaField7.setFieldName("Email Address");
		metaField7.setValue(newUser.getUserName() + "@shire.com");
		metaField7.getMetaField().setDataType(DataType.STRING);
		metaFieldValues.add(metaField7);

		newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

		logger.debug("Meta field values set");
		if(addPaymentInstument) {
			// add a credit card
			Calendar expiry = Calendar.getInstance();
			expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

			PaymentInformationWS cc = createCreditCard(TEST_TOKEN_ID, expiry.getTime());

			newUser.getPaymentInstruments().add(cc);

		}

		logger.debug("Creating user ...");
		MainSubscriptionWS billing = new MainSubscriptionWS();
		billing.setPeriodId(2);
		billing.setNextInvoiceDayOfPeriod(1);
		newUser.setMainSubscription(billing);
		newUser.setNextInvoiceDate(nextInvoiceDate);
		newUser.setUserId(api.createUser(newUser));
		logger.debug("User created with id: {}", newUser.getUserId());

		return newUser;
	}

	public static PaymentInformationWS createCreditCard(String tokenId, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(EP2_GATE_WAY_TOKEN_METHOD_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Integer.valueOf(2));
		//cc

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 2, new SimpleDateFormat(
						Constants.CC_DATE_FORMAT).format(date).toCharArray());

		addMetaField(metaFields, CC_MF_TOKEN_ID ,false, true, DataType.CHAR, 1, tokenId.toCharArray());
		addMetaField(metaFields, CC_MF_AUTOPAYMENT_AUTHORIZATION ,false, true, DataType.BOOLEAN, 1, true);
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

	private static void addMetaField(List<MetaFieldValueWS> metaFields,
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

	@AfterClass
	protected void cleanUp() throws Exception {
		if(null!=paymentEP2PluginId) {
			api.deletePlugin(paymentEP2PluginId);
		}
		if(!PAYMENT_PROCESS_TASK_ID.equals(831734)) {
			api.deletePlugin(PAYMENT_PROCESS_TASK_ID);
		} else {
			PluggableTaskWS paymentProcessTask = api.getPluginWS(PAYMENT_PROCESS_TASK_ID);
			paymentProcessTask.setParameters(new Hashtable<String, String>());
			api.updatePlugin(paymentProcessTask);
		}

	}

	private void updateProcessingOrderOfPlugin(JbillingAPI api, Integer pluginId) {
		PluggableTaskWS plugIn = api.getPluginWS(pluginId);
		plugIn.setProcessingOrder(plugIn.getProcessingOrder()+10);
		plugIn.setParameters(new Hashtable<String, String>(plugIn.getParameters()));
		api.updatePlugin(plugIn);
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
