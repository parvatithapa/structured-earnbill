package com.sapienter.jbilling.fc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeBL;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.PaymentMethodTypeBuilder;

/**
 * 
 * @author Harshad Pathan
 *This class is for testing payment Via BrainTree
 * To test this class, Please add its enrty in testng-functional.xml
 * under "fullcreative" group.
 * you also need to uncommenet a changeSet in test-data-features.xml.
 * That will configure payment method for BT
 */
@Test(groups = "external-system", testName = "FullCreativeCustomerPaymentViaBrainTree")
public class FullCreativeCustomerPaymentViaBrainTree{

	private static final Logger logger = LoggerFactory.getLogger(FullCreativeCustomerPaymentViaBrainTree.class);
	private JbillingAPI api;
	private static Integer CC_PM_ID = 15;
	private EnvironmentHelper envHelper;
	private static TestBuilder testBuilder;
	private TestEnvironment environment;
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	private final static String CREDIT_CARD_NUMBER = "378282246310005";
	private final static Integer migrationAcoountTypeId=60103;
	private final static Integer standardAcoountTypeId=60102;
	private final static String PLUGIN_PARAM_BT_ENVIRONMENT = "BT Environment";
	private final static String PLUGIN_PARAM_ALLOW_PAYMENT_ID = "Allowed Payment Method Ids";
	private final static String PLUGIN_PARAM_BUSSINESS_ID = "Bussiness Id";
	private static Integer brainTreePluginId;
	private static Integer saveCreditCardPluginId;
	private final static String BT_PAYMENT_METHOD_NAME = "BT Credit Card";
	private final static String UESR_WITH_STANDARD_ACCOUNT_TYPE = "userWithStandardAcoountTypeId"+Calendar.getInstance().getTimeInMillis();
	public final static int MONTHLY_ORDER_PERIOD = 2;
	private static final Integer nextInvoiceDay = 1;
	private static final Integer paymentMethodId = 1;


	@BeforeClass
	protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID1);
		updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID2);
		updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FILTER_TASK_ID);
		updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CCF_TASK_ID);
		updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CUREENCY_TASK_ID);


		testBuilder = getTestEnvironment();
		environment = testBuilder.getTestEnvironment();
		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			buildAndPersistPaymentMethodType(environment,api,BT_PAYMENT_METHOD_NAME);
		}).test((testEnv, testEnvBuilder) -> {
			assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(BT_PAYMENT_METHOD_NAME));
			CC_PM_ID =testEnvBuilder.idForCode(BT_PAYMENT_METHOD_NAME);
		});

		//configure plugin BrainTreePaymentExternalTask
		logger.debug("Configured BrainTree payment plugin");
		PluggableTaskWS brainTreePlugin = new PluggableTaskWS();
		PluggableTaskTypeWS pluginType = 
				api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.payment.tasks.braintree.BrainTreePaymentExternalTask");
		Hashtable<String, String> brainTreeparameters = new Hashtable<String, String>();
		brainTreeparameters.put(PLUGIN_PARAM_BUSSINESS_ID, "bf4d2b3b-e4f8-4066-90f1-c5d7941bbc33");
		brainTreeparameters.put(PLUGIN_PARAM_BT_ENVIRONMENT, "sandbox");
		brainTreeparameters.put(PLUGIN_PARAM_ALLOW_PAYMENT_ID, String.valueOf(environment.idForCode(BT_PAYMENT_METHOD_NAME)));

		brainTreePlugin.setTypeId(pluginType.getId());
		brainTreePlugin.setProcessingOrder(1);
		brainTreePlugin.setParameters(brainTreeparameters);

		brainTreePluginId = api.createPlugin(brainTreePlugin);

		PluggableTaskWS saveCreditCardExternalPlugin = new PluggableTaskWS();

		saveCreditCardExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
		saveCreditCardExternalPlugin.setProcessingOrder(1234);
		saveCreditCardExternalPlugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME).getId());
		Hashtable<String, String> saveCreditCardExternalparameters = new Hashtable<String, String>();
		saveCreditCardExternalparameters.clear();

		saveCreditCardExternalparameters.put("removeOnFail", "false");
		saveCreditCardExternalparameters.put("externalSavingPluginId",brainTreePluginId.toString());
		saveCreditCardExternalparameters.put("contactType", "14");
		saveCreditCardExternalparameters.put("obscureOnFail", "false");

		saveCreditCardExternalPlugin.setParameters(saveCreditCardExternalparameters);
		saveCreditCardPluginId = api.createPlugin(saveCreditCardExternalPlugin);

	}

	private TestBuilder getTestEnvironment() {

		return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {

			this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
		});
	}

	private Integer buildAndPersistPaymentMethodType(TestEnvironment envBuilder, JbillingAPI api, String code){

		List<Integer> accountTypeIds = new ArrayList<Integer>();
		accountTypeIds.add(migrationAcoountTypeId);
		accountTypeIds.add(standardAcoountTypeId);

		 PaymentMethodTypeWS paymentMethod =PaymentMethodTypeBuilder.getBuilder(api, envBuilder, code)
			.withMethodName(code)
			.withAccountTypes(accountTypeIds)
			.withTemplateId(1)
			.withMetaFields(getMetafieldList())
			.isRecurring(true)
			.build();
		 List<MetaFieldWS> metaFields = getMetafieldList();
		 paymentMethod.setMetaFields(metaFields.toArray(new MetaFieldWS[metaFields.size()]));
		 api.updatePaymentMethodType(paymentMethod);
		 return paymentMethod.getId();

	}

	private List<MetaFieldWS> getMetafieldList() {
		List<MetaFieldWS> list = new ArrayList<MetaFieldWS>();
		list.add(buildAndPersistMetafield(CC_MF_CARDHOLDER_NAME, DataType.CHAR,MetaFieldType.TITLE, Integer.valueOf(1)));
		list.add(buildAndPersistMetafield(CC_MF_NUMBER, DataType.CHAR,MetaFieldType.PAYMENT_CARD_NUMBER, Integer.valueOf(2)));
		list.add(buildAndPersistMetafield(CC_MF_EXPIRY_DATE, DataType.CHAR,MetaFieldType.DATE, Integer.valueOf(3)));
		list.add(buildAndPersistMetafield(CC_MF_TYPE, DataType.STRING,MetaFieldType.CC_TYPE, Integer.valueOf(4)));
		list.add(buildAndPersistMetafield("autopayment.authorization", DataType.BOOLEAN,MetaFieldType.AUTO_PAYMENT_AUTHORIZATION, Integer.valueOf(5)));
		list.add(buildAndPersistMetafield("Country", DataType.STRING,MetaFieldType.COUNTRY_CODE, Integer.valueOf(6)));
		list.add(buildAndPersistMetafield("BT Customer Id", DataType.CHAR,MetaFieldType.GATEWAY_KEY, Integer.valueOf(7)));
		return list;
	}

	private MetaFieldWS buildAndPersistMetafield(String name, DataType dataType, MetaFieldType fieldUsage,Integer displayOrder ){
		return new MetaFieldBuilder().name(name)
				.dataType(dataType)
				.entityType(EntityType.PAYMENT_METHOD_TYPE)
				.fieldUsage(fieldUsage)
				.displayOrder(displayOrder)
				.build();
	}

	@Test
	public void test001BrainTreePayment() throws Exception {

		UserWS newUser1 = createUser(new Date(), true,migrationAcoountTypeId,Integer.valueOf(14));
		logger.debug("User : {}", newUser1);

		UserWS newUser = api.getUserWS( newUser1.getId() );

		List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
		MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();
		String cardNumber = null;
		for(MetaFieldValueWS metafield : metaFields){
			if( (metafield.getFieldName()).equals("cc.number") )
				cardNumber =String.valueOf(metafield.getCharValue());
		}
		logger.debug("cardNumber : {}", cardNumber);
		assertTrue("Credit Card Number should be obscure",cardNumber != null && cardNumber.startsWith("************"));

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("30"));
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(newUser.getUserId());
		PaymentInformationWS cc = newUser.getPaymentInstruments().get(0);
		payment.getPaymentInstruments().add(cc);

		// Testing first payment through BT
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment has zero balance
		PaymentWS lastPayment = api.getLatestPayment(newUser.getId());

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
		secondPayment.setUserId(newUser.getUserId());
		PaymentInformationWS cc1 = newUser.getPaymentInstruments().get(0);
		secondPayment.getPaymentInstruments().add(cc1);

		//  Testing second payment through Paypal
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo1 = api.processPayment(secondPayment, null);
		assertNotNull("Payment result not null", authInfo1);
		assertTrue("Payment Authorization result should be OK", authInfo1.getResult().booleanValue());

		// check payment has zero balance
		PaymentWS lastPayment1 = api.getLatestPayment(newUser.getUserId());
		assertNotNull("payment can not be null", lastPayment1);
		assertNotNull("auth in payment can not be null", lastPayment1.getAuthorizationId());
		//	      assertEquals("correct payment amount", new BigDecimal("20.0000000000"), lastPayment1.getAmountAsDecimal());
		assertEquals("correct payment balance", new BigDecimal("0E-10"), lastPayment1.getBalanceAsDecimal());

		api.updateUser(newUser);

		PaymentInformationWS paypal = null;
		metaFields = null;
		cardNumber = null;

		for(PaymentInformationWS instrument : newUser.getPaymentInstruments()) {
			if(instrument.getPaymentMethodTypeId().equals(CC_PM_ID)) {
				paypal = instrument;
			}
		}
		logger.debug("Updated User paypal Instrument is: {}", paypal);
		for(MetaFieldValueWS metafield : paypal.getMetaFields()){
			if( (metafield.getFieldName()).equals("cc.number") )
				cardNumber =String.valueOf(metafield.getCharValue());
		}

		assertTrue("Updated user credit Card Number should be obscure",cardNumber != null && cardNumber.startsWith("************"));
	}

	@Test
	public void test002BrainTreePaymentWithoutBillingGroup() throws Exception {
		UserWS newUser1 = createUser(new Date(), false, migrationAcoountTypeId,Integer.valueOf(14));
		logger.debug("User : {}", newUser1);

		UserWS newUser = api.getUserWS( newUser1.getId() );

		List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
		MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();
		String cardNumber = null;
		for(MetaFieldValueWS metafield : metaFields){
			if( (metafield.getFieldName()).equals("cc.number") )
				cardNumber =String.valueOf(metafield.getCharValue());
		}

		//This assert is changed because to obscure credit cart it required BillingGroup info.
		assertFalse("Credit Card Number should not be obscure",cardNumber != null && cardNumber.startsWith("************"));

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("30"));
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(newUser.getUserId());
		PaymentInformationWS cc = newUser.getPaymentInstruments().get(0);
		payment.getPaymentInstruments().add(cc);
		// Testing first payment through Paypal
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
		logger.debug("authInfo : {}", authInfo);
		assertNotNull("Payment result not null", authInfo);
		assertFalse("Payment Authorization result should be false ", authInfo.getResult().booleanValue());
		logger.debug("authInfo : {}", authInfo.getResult().booleanValue());

		PaymentWS paymentWS= api.getPayment(authInfo.getPaymentId());

		assertEquals("Result should be 'Payer Billing Information not found'." ,paymentWS.getResultId(),Integer.valueOf(5));

		// adding new credit card info
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 2);

		cc = createCreditCard("PaypalIPN",
				CREDIT_CARD_NUMBER,
				expiry.getTime());

		newUser.getPaymentInstruments().add(cc);

		api.updateUser(newUser);
		assertEquals("The number of payment instruments should have been 2." ,newUser.getPaymentInstruments().size(),2);

	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void test003BrainTreePaymentwithstandardAcoountType() throws Exception {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		try {
			testBuilder.given(envBuilder -> {
				logger.debug("Scenario #1 - User created with standard account type");

				Calendar nextInvoiceDate = Calendar.getInstance();
				nextInvoiceDate.set(Calendar.YEAR, 2016);
				nextInvoiceDate.set(Calendar.MONTH, 8);
				nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

				Calendar activeSince = Calendar.getInstance();
				activeSince.set(Calendar.YEAR, 2016);
				activeSince.set(Calendar.MONTH, 7);
				activeSince.set(Calendar.DAY_OF_MONTH, 1);

				Calendar paymentDate = Calendar.getInstance();
				paymentDate.set(Calendar.YEAR, 2016);
				paymentDate.set(Calendar.MONTH, 6);
				paymentDate.set(Calendar.DAY_OF_MONTH, 1);

				try {
					UserWS user = createUser(nextInvoiceDate.getTime(), true, standardAcoountTypeId, 10);
					 InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
					 scenario01.createUser(UESR_WITH_STANDARD_ACCOUNT_TYPE, standardAcoountTypeId, nextInvoiceDate.getTime(),MONTHLY_ORDER_PERIOD, nextInvoiceDay,Integer.valueOf(9));
					 UserWS newUser = api.getUserWS(environment.idForCode(UESR_WITH_STANDARD_ACCOUNT_TYPE));
					 System.out.println("User:::::: "+user);
					 newUser.setMetaFields(getMetafieldValues(true, 10).toArray(new MetaFieldValueWS[0]));
					 // add a credit card
					 Calendar expiry = Calendar.getInstance();
					 expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

					 PaymentInformationWS cc = createCreditCard("Frodo Baggins",
							 "4012000077777777", expiry.getTime());
					 newUser.getPaymentInstruments().add(cc);
					 api.updateUser(newUser);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// Next Invoice Date 1st of Sep 2016
			}).validate((testEnv, envBuilder) -> {
				UserWS newUser = api.getUserWS(environment.idForCode(UESR_WITH_STANDARD_ACCOUNT_TYPE));
				System.out.println("Checking payment for standered account type user:::::: "+newUser);
				List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
				MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();
				String cardNumber = null;
				for(MetaFieldValueWS metafield : metaFields){
					if( (metafield.getFieldName()).equals("cc.number") )
						cardNumber =String.valueOf(metafield.getCharValue());
				}
				logger.debug("cardNumber : {}", cardNumber);
				assertTrue("Credit Card Number should be obscure",cardNumber != null && cardNumber.startsWith("************"));

				PaymentWS payment = new PaymentWS();
				payment.setAmount(new BigDecimal("30"));
				payment.setIsRefund(0);
				payment.setPaymentDate(Calendar.getInstance().getTime());
				payment.setCurrencyId(Integer.valueOf(1));
				payment.setUserId(environment.idForCode(UESR_WITH_STANDARD_ACCOUNT_TYPE));
				PaymentInformationWS cc = newUser.getPaymentInstruments().get(0);
				payment.getPaymentInstruments().add(cc);

				// Testing first payment through BT
				logger.debug("Processing token payment...");
				PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
				assertNotNull("Payment result not null", authInfo);
				assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

				// check payment has zero balance
				PaymentWS lastPayment = api.getLatestPayment(newUser.getId());

				Integer paymentID = lastPayment.getId();
				assertNotNull("payment can not be null", lastPayment);
				assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
				assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
				assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());

			});
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress,Integer accountTypeID, Integer groupId) throws JbillingAPIException,
	IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS newUser = new UserWS();
		List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
		newUser.setUserId(0);
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword12");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(accountTypeID);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));

		logger.debug("User properties set");
		metaFieldValues = getMetafieldValues(populateBillingGroupAddress, groupId);
		newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

		logger.debug("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				"4012000077777777", expiry.getTime());

		newUser.getPaymentInstruments().add(cc);

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

	public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		TestEnvironment environment = testBuilder.getTestEnvironment();
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(environment.idForCode(BT_PAYMENT_METHOD_NAME));
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Integer.valueOf(2));
		//cc

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, new SimpleDateFormat(Constants.CC_DATE_FORMAT).format(date).toCharArray());
		addMetaField(metaFields, "Country", false, false,
				DataType.STRING, 4, "US");

		// have to pass meta field card type for it to be set
	        addMetaField(metaFields, CC_MF_TYPE, false, false,
	                DataType.STRING, 5, CreditCardType.VISA);
		addMetaField(metaFields, "BT Customer Id" ,false, true, DataType.STRING, 6, null );
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

	private void updateProcessingOrderOfPlugin(JbillingAPI api, Integer pluginId) {
		PluggableTaskWS plugIn = api.getPluginWS(pluginId);
		plugIn.setProcessingOrder(plugIn.getProcessingOrder()+10);
		plugIn.setParameters(new Hashtable<String, String>(plugIn.getParameters()));
		api.updatePlugin(plugIn);
	}

	private static List<MetaFieldValueWS> getMetafieldValues(boolean populateBillingGroupAddress,Integer groupId) {
		List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();

		MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		metaField1.setFieldName("Country");
		metaField1.getMetaField().setDataType(DataType.STRING);
		metaField1.setValue("US");
		metaFieldValues.add(metaField1);

		MetaFieldValueWS metaField2 = new MetaFieldValueWS();
		metaField2.setFieldName("State/Province");
		metaField2.getMetaField().setDataType(DataType.STRING);
		metaField2.setValue("OR");
		metaFieldValues.add(metaField2);

		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("First Name");
		metaField3.getMetaField().setDataType(DataType.STRING);
		metaField3.setValue("Frodo");
		metaFieldValues.add(metaField3);

		MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("Last Name");
		metaField4.getMetaField().setDataType(DataType.STRING);
		metaField4.setValue("Baggins");
		metaFieldValues.add(metaField4);

		MetaFieldValueWS metaField5 = new MetaFieldValueWS();
		metaField5.setFieldName("Address 1");
		metaField5.getMetaField().setDataType(DataType.STRING);
		metaField5.setValue("Baggins");
		metaFieldValues.add(metaField5);

		MetaFieldValueWS metaField6 = new MetaFieldValueWS();
		metaField6.setFieldName("City");
		metaField6.getMetaField().setDataType(DataType.STRING);
		metaField6.setValue("Baggins");
		metaFieldValues.add(metaField6);

		MetaFieldValueWS metaField7 = new MetaFieldValueWS();
		metaField7.setFieldName("Email Address");
		metaField7.getMetaField().setDataType(DataType.STRING);
        metaField7.setValue("test@shire.com");
        metaFieldValues.add(metaField7);

		MetaFieldValueWS metaField8 = new MetaFieldValueWS();
		metaField8.setFieldName("Postal Code");
		metaField8.getMetaField().setDataType(DataType.STRING);
		metaField8.setValue("K0");
		metaFieldValues.add(metaField8);

		if(populateBillingGroupAddress) {

			MetaFieldValueWS metaField9 = new MetaFieldValueWS();
			metaField9.setFieldName("COUNTRY_CODE");
			metaField9.getMetaField().setDataType(DataType.STRING);
			metaField9.setValue("CA");
			metaField9.setGroupId(groupId);
			metaFieldValues.add(metaField9);

			MetaFieldValueWS metaField10 = new MetaFieldValueWS();
			metaField10.setFieldName("STATE_PROVINCE");
			metaField10.getMetaField().setDataType(DataType.STRING);
			metaField10.setValue("OR");
			metaField10.setGroupId(groupId);
			metaFieldValues.add(metaField10);

			MetaFieldValueWS metaField11 = new MetaFieldValueWS();
			metaField11.setFieldName("ORGANIZATION");
			metaField11.getMetaField().setDataType(DataType.STRING);
			metaField11.setValue("Frodo");
			metaField11.setGroupId(groupId);
			metaFieldValues.add(metaField11);

			MetaFieldValueWS metaField12 = new MetaFieldValueWS();
			metaField12.setFieldName("LAST_NAME");
			metaField12.getMetaField().setDataType(DataType.STRING);
			metaField12.setValue("Baggins");
			metaField12.setGroupId(groupId);
			metaFieldValues.add(metaField12);

			MetaFieldValueWS metaField13 = new MetaFieldValueWS();
			metaField13.setFieldName("ADDRESS1");
			metaField13.getMetaField().setDataType(DataType.STRING);
			metaField13.setValue("Baggins");
			metaField13.setGroupId(groupId);
			metaFieldValues.add(metaField13);

			MetaFieldValueWS metaField14 = new MetaFieldValueWS();
			metaField14.setFieldName("CITY");
			metaField14.getMetaField().setDataType(DataType.STRING);
			metaField14.setValue("Baggins");
			metaField14.setGroupId(groupId);
			metaFieldValues.add(metaField14);

			MetaFieldValueWS metaField15 = new MetaFieldValueWS();
			metaField15.setFieldName("BILLING_EMAIL");
			metaField15.getMetaField().setDataType(DataType.STRING);
			metaField15.setValue("test@shire.com");
			metaField15.setGroupId(groupId);
			metaFieldValues.add(metaField15);

			MetaFieldValueWS metaField16 = new MetaFieldValueWS();
			metaField16.setFieldName("POSTAL_CODE");
			metaField16.getMetaField().setDataType(DataType.STRING);
			metaField16.setValue("K0");
			metaField16.setGroupId(groupId);
			metaFieldValues.add(metaField16);

		}
		return metaFieldValues;
	}
	@AfterClass
	private void cleanUp() {
		api.deletePlugin(brainTreePluginId);
		api.deletePlugin(saveCreditCardPluginId);
	}
}
