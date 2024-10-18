package com.sapienter.jbilling.amaysim;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;

/**
 * 
 * @author Mahesh Shivarkar
 * @since 25-July-2017
 */
@Test(groups = { "amaysim" }, testName = "DeleteEnteredRefundPaymentTest")
public class DeleteEnteredRefundPaymentTest {

	private static final Logger logger = LoggerFactory.getLogger(DeleteEnteredRefundPaymentTest.class);
	

	private TestBuilder testBuilder;
	private TestEnvironment environment;
	private EnvironmentHelper environmentHelper;

	private static final String ACCOUNT_TYPE_CODE = "TestAccountType";

	private static final String CUSTOMER_CODE1 = "TestCustomer1";
	private static final String CUSTOMER_CODE3 = "TestCustomer3";


	private final static Integer CC_PM_ID = 5;

	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	private final static String CREDIT_CARD_NUMBER = "5257279846844529";

	@BeforeClass
	public void initializeTests(){
		testBuilder = getTestEnvironment();
		environment = testBuilder.getTestEnvironment();
	}


	@AfterClass
	public void tearDown(){
		final JbillingAPI api = environment.getPrancingPonyApi();
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
			envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();

		});
	}

	/**
	 * Delete full Entered refund payment 
	 */

	@Test(priority = 1)
	public void test001DeleteFullEntereRefundPayment(){

		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE1, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.info("### userId: {}",userId);
			assertNotNull("UserId should not be null",userId);
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 2);
			paymentDate.set(Calendar.DAY_OF_MONTH, 5);

			// Test user status after partial payment
			makePayment("100.00", paymentDate.getTime(), false,CUSTOMER_CODE1,false);
			PaymentWS payment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE1));
			assertEquals("Payment balance should be 100", "100.0000000000", payment.getBalance());
			// Create entered refund payment
			makePayment("100.0000000000", paymentDate.getTime(), true,CUSTOMER_CODE1,true);

			PaymentWS refundPayment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE1));
			assertEquals("Payment should be refund", Integer.valueOf(1), refundPayment.getIsRefund());
			assertEquals("Status should be Entered", Integer.valueOf(4), refundPayment.getResultId());
			payment = api.getPayment(refundPayment.getPaymentId());
			assertEquals("Original Payment balance should be zero as it is refunded", "0E-10", payment.getBalance());
			// Delete refund payment
			api.deletePayment(refundPayment.getId());
			payment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE1));
			assertEquals("Original Payment balance should be 100", "100.0000000000", payment.getBalance());
		});
	}

	/**
	 * Delete partial Entered refund payment 
	 */

	@Test(priority = 2)
	public void test002DeletePartialEntereRefundPayment(){

		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE1, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.info("### userId: {}",userId);
			assertNotNull("UserId should not be null",userId);
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 2);
			paymentDate.set(Calendar.DAY_OF_MONTH, 5);

			// Test user status after partial payment
			makePayment("100.00", paymentDate.getTime(), false,CUSTOMER_CODE1,false);
			PaymentWS payment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE1));
			assertEquals("Payment balance should be 100", "100.0000000000", payment.getBalance());
			// Create partial entered refund payment
			makePayment("50.0000000000", paymentDate.getTime(), true,CUSTOMER_CODE1,true);

			PaymentWS refundPayment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE1));
			assertEquals("Payment should be refund", Integer.valueOf(1), refundPayment.getIsRefund());
			assertEquals("Status should be Entered", Integer.valueOf(4), refundPayment.getResultId());
			assertEquals("Partial Refund Payment amount should be 50", "50.0000000000", refundPayment.getAmount());
			payment = api.getPayment(refundPayment.getPaymentId());
			assertEquals("Original Payment balance should be 50", "50.0000000000", payment.getBalance());
			// Delete partial refund payment
			api.deletePayment(refundPayment.getId());
			payment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE1));
			assertEquals("Original Payment balance should be 100", "100.0000000000", payment.getBalance());
		});
	}


	/**
	 * Delete Successful Refund refund payment. This should through validation.
	 */
	@Test(priority = 3)
	public void test003DeleteSuccessfulRefundPayment(){

		final Date nextInvoiceDate = FullCreativeUtil.getDate(0,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE3, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
			logger.info("### userId: {}",userId);
			assertNotNull("UserId should not be null",userId);
		}).test((env)-> {
			JbillingAPI api = env.getPrancingPonyApi();

			Calendar paymentDate = Calendar.getInstance();
			paymentDate.set(Calendar.YEAR, 2016);
			paymentDate.set(Calendar.MONTH, 2);
			paymentDate.set(Calendar.DAY_OF_MONTH, 5);

			// Test user status after partial payment
			makePayment("100.00", paymentDate.getTime(), false,CUSTOMER_CODE3,false);
			PaymentWS payment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE3));
			assertEquals("Payment balance should be 100", "100.0000000000", payment.getBalance());
			// Create entered refund payment
			makePayment("100.0000000000", paymentDate.getTime(), true,CUSTOMER_CODE3,false);

			PaymentWS refundPayment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE3));
			assertEquals("Payment should be refund", Integer.valueOf(1), refundPayment.getIsRefund());
			assertEquals("Status should be Succesfull", Integer.valueOf(1), refundPayment.getResultId());
			payment = api.getPayment(refundPayment.getPaymentId());
			assertEquals("Original Payment balance should be zero as it is refunded", "0E-10", payment.getBalance());
			// Delete successful refund payment. 
			try {
				api.deletePayment(refundPayment.getId());
			} catch (SessionInternalError e) {
				assertEquals("successful refunds can not be deleted", "validation.error.delete.refund.payment", e.getErrorMessages()[0]);
			}


			payment = api.getLatestPayment(environment.idForCode(CUSTOMER_CODE3));
			assertEquals("Original Payment balance should be zero", "0E-10", payment.getBalance());
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

	private void makePayment(String amount, Date paymentDate, boolean isRefund, String customerCode, boolean isEntered) {

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
		if (isEntered){
			testBuilder.getTestEnvironment().getPrancingPonyApi().createPayment(payment);
		} else {
			PaymentAuthorizationDTOEx authInfo = testBuilder.getTestEnvironment().getPrancingPonyApi().processPayment( payment, null);
			assertNotNull("Payment result not null", authInfo);
		}
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
}