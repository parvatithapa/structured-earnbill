package com.sapienter.jbilling.server.payment;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.ProcessSignPaymentAPITest;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

/**
 * 
 * @author Harshad Pathan
 * This class is for testing process signup via BT plugin 
 * To test this class, Please add its enrty in testng-functional.xml
 * under "web-services" group.
 * you also need to uncomment a changeSet in test-data-features.xml.
 * That will configure payment method for BT 
 */
@Test(groups = {"web-services", "test-sure-address"}, testName = "ProcessSignPaymentAPIWithBrainTreeTest")
public class ProcessSignPaymentAPIWithBrainTreeTest {

	private static final Logger logger = LoggerFactory.getLogger(ProcessSignPaymentAPIWithBrainTreeTest.class);
	private JbillingAPI api;
	public static final int PAYMENT_FAKE_TASK_ID1 = 20;
	public static final int PAYMENT_ROUTER_CCF_TASK_ID = 21;
	public static final int PAYMENT_FAKE_TASK_ID2 = 22;
	public static final int PAYMENT_FILTER_TASK_ID = 460;
	public static final int PAYMENT_ROUTER_CUREENCY_TASK_ID = 520;
	private final static String CREDIT_CARD_NUMBER = "378282246310005";
	private final static String INVALID_CREDIT_CARD_NUMBER = "37828224631000";
	private final static String BT_CUSTOMER_ID = "BT Customer Id";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_TYPE = "cc.type";
	private final static String OBSCURED_CREDIT_CARD= "***********0005";
	private final static String PLUGIN_PARAM_BUSSINESS_ID = "Bussiness Id";
	private final static String PLUGIN_PARAM_BT_ENVIRONMENT = "BT Environment";
	private final static String PLUGIN_PARAM_ALLOW_PAYMENT_ID = "Allowed Payment Method Ids";
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static Integer CC_PM_ID = 15;
	private final static Integer migrationAcoountTypeId=60103;
	private Integer userId = null;
	private Integer brainTreePluginId = null;
	private static Integer saveCreditCardPluginId;

	@BeforeClass
	protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		PluggableTaskWS plugin1 = new PluggableTaskWS();
		try {
			logger.debug("Changing processing orders of payment plugin");
			updateProcessingOrderOfPlugin(api, PAYMENT_FAKE_TASK_ID1);
			updateProcessingOrderOfPlugin(api, PAYMENT_FAKE_TASK_ID2);
			updateProcessingOrderOfPlugin(api, PAYMENT_FILTER_TASK_ID);
			updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CCF_TASK_ID);
			updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CUREENCY_TASK_ID);

			//configure plugin BrainTreePaymentExternalTask
			logger.debug("Configured BrainTree payment plugin");
			PluggableTaskWS brainTreePlugin = new PluggableTaskWS();
			PluggableTaskTypeWS pluginType = 
					api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.payment.tasks.braintree.BrainTreePaymentExternalTask");
			Hashtable<String, String> brainTreeparameters = new Hashtable<String, String>();
			brainTreeparameters.put(PLUGIN_PARAM_BUSSINESS_ID, "0dd89c5e-fa33-4940-a51e-8eb0658b2b6c");
			brainTreeparameters.put(PLUGIN_PARAM_BT_ENVIRONMENT, "sandbox");
			brainTreeparameters.put(PLUGIN_PARAM_ALLOW_PAYMENT_ID, "15");

			brainTreePlugin.setTypeId(pluginType.getId());
			brainTreePlugin.setProcessingOrder(1);
			brainTreePlugin.setParameters(brainTreeparameters);

			brainTreePluginId = api.createPlugin(brainTreePlugin);

			PluggableTaskWS saveCreditCardExternalPlugin = new PluggableTaskWS();

			saveCreditCardExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
			saveCreditCardExternalPlugin.setProcessingOrder(1234);
			saveCreditCardExternalPlugin.setTypeId(api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.payment.tasks.SaveCreditCardExternallyTask").getId());
			Hashtable<String, String> saveCreditCardExternalparameters = new Hashtable<String, String>();
			saveCreditCardExternalparameters.clear();

			saveCreditCardExternalparameters.put("removeOnFail", "false");
			saveCreditCardExternalparameters.put("externalSavingPluginId",brainTreePluginId.toString());
			saveCreditCardExternalparameters.put("contactType", "14");
			saveCreditCardExternalparameters.put("obscureOnFail", "true");

			saveCreditCardExternalPlugin.setParameters(saveCreditCardExternalparameters);
			saveCreditCardPluginId = api.createPlugin(saveCreditCardExternalPlugin);


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test001SignupPaymentAPIWithoutCreditCardDetails() throws Exception {
		UserWS user = createUser(new Date(), true,null);
		PaymentWS payment = ProcessSignPaymentAPITest.createPayment(user.getId(), new BigDecimal("100"), user.getPaymentInstruments().get(0));
		user.getPaymentInstruments().clear();

		logger.debug("Processing token payment...");
		try {
			userId = api.processSignupPayment(user, payment);
		} catch (Exception e) {
		}
		assertNull("UserId should be null as user would not get created", userId);
	}

	@Test
	public void test002SignupPaymentAPIWithInvalidCreditCard() throws Exception {
		UserWS user = createUser(new Date(), true,false);
		PaymentWS payment = ProcessSignPaymentAPITest.createPayment(user.getId(), new BigDecimal("100"), user.getPaymentInstruments().get(0));
		user.getPaymentInstruments().clear();
		logger.debug("Processing token payment...");
		try {
			userId = api.processSignupPayment(user, payment);
		} catch (Exception e) {
		}
		assertNull("UserId should be null as user would not get created", userId);
	}

	@Test
	public void test003SignupPaymentAPIWithInvalidUserDetails() throws Exception {
		Integer userId = null;
		UserWS user = createUser(new Date(),true,true);
		user.setParentId(new Integer(-1111));
		PaymentWS payment = ProcessSignPaymentAPITest.createPayment(user.getId(), new BigDecimal("100"), user.getPaymentInstruments().get(0));
		try {
			userId = api.processSignupPayment(user, payment);
		} catch (Exception e) {
			// TODO: handle exception
		}
		logger.debug("userId ::::::: {}", userId);
		assertNull("UserId should be null as user would not get created", userId);
	}

	@Test
	public void test004ProcessSignupPaymentAPIWithHugeAmount() throws Exception {
		Integer userId = null;
		UserWS user = createUser(new Date(),true,true);
		PaymentWS payment = ProcessSignPaymentAPITest.createPayment(user.getId(), new BigDecimal("9999999999"), user.getPaymentInstruments().get(0));
		try {
			userId = api.processSignupPayment(user, payment);
		} catch (Exception e) {
			// TODO: handle exception
		}
		logger.debug("userId ::::::: {}", userId);
		assertNull("UserId should be null as user would not get created", userId);
	}

	@Test
	public void test005ProcessSignupPaymentAPIWithBrainTree() throws Exception {
		UserWS user = createUser(new Date(),true,true);
		PaymentWS payment = ProcessSignPaymentAPITest.createPayment(user.getId(), new BigDecimal("100"), user.getPaymentInstruments().get(0));
		logger.debug("Processing token payment...");
		userId = api.processSignupPayment(user, payment);
		logger.debug("userId ::::: {}", userId);
		assertNotNull("User Id can not be null", userId);
		user = api.getUserWS(userId);
		assertNotNull("User can not be null", user);
		PaymentInformationWS card = user.getPaymentInstruments().iterator().next();
		assertNotNull("Credit Card can not be null", card);
		logger.debug("Updated card {}", card.getId());
		assertEquals("card type AMEX", "Amex", ProcessSignPaymentAPITest.getMetaField(card.getMetaFields(), CC_MF_TYPE).getStringValue());
		assertEquals("Card number should be obscured", OBSCURED_CREDIT_CARD, new String(ProcessSignPaymentAPITest.getMetaField(card.getMetaFields(), CC_MF_NUMBER).getCharValue()));
		assertEquals("First 11 digits of Card number should be obscured", "***********", ProcessSignPaymentAPITest.getMetaField(card.getMetaFields(), CC_MF_NUMBER).toString().substring(0, 11));
		payment = api.getLatestPayment(userId);
		assertNotNull("payment can not be null", payment);
		assertEquals("Payment result should be successful ", payment.getResultId(), Constants.PAYMENT_RESULT_SUCCESSFUL);
		assertNotNull("Brain Tree customer Id can not be null", ProcessSignPaymentAPITest.getMetaField(card.getMetaFields(), BT_CUSTOMER_ID).getStringValue());
		assertEquals("correct payment amount", "100.0000000000", payment.getAmount());
	}

	private void updateProcessingOrderOfPlugin(JbillingAPI api, Integer pluginId) {
		PluggableTaskWS plugIn = api.getPluginWS(pluginId);
		plugIn.setProcessingOrder(plugIn.getProcessingOrder()+10);
		plugIn.setParameters(new Hashtable<String, String>(plugIn.getParameters()));
		api.updatePlugin(plugIn);
	}

	public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Integer.valueOf(2));
		//cc

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
				DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2,
				cardNumber!= null? cardNumber.toCharArray(): cardNumber);
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, (new SimpleDateFormat(
						Constants.CC_DATE_FORMAT).format(date)).toCharArray());
		addMetaField(metaFields, "Country", false, false,
				DataType.STRING, 3, "US");
		addMetaField(metaFields, CC_MF_TYPE, false, false,
				DataType.STRING, 4, CreditCardType.AMEX); 

		addMetaField(metaFields, "BT Customer Id" ,false, true, DataType.STRING, 5, null );
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
	public static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress, Boolean isValidCreditcard) throws JbillingAPIException,
	IOException {
		UserWS newUser = new UserWS();
		List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
		newUser.setUserId(0);
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword12");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(migrationAcoountTypeId);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));

		logger.debug("User properties set");
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
		metaField7.setValue(newUser.getUserName() + "@shire.com");
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
			metaField9.setGroupId(14);
			metaFieldValues.add(metaField9);

			MetaFieldValueWS metaField10 = new MetaFieldValueWS();
			metaField10.setFieldName("STATE_PROVINCE");
			metaField10.getMetaField().setDataType(DataType.STRING);
			metaField10.setValue("OR");
			metaField10.setGroupId(14);
			metaFieldValues.add(metaField10);

			MetaFieldValueWS metaField11 = new MetaFieldValueWS();
			metaField11.setFieldName("ORGANIZATION");
			metaField11.getMetaField().setDataType(DataType.STRING);
			metaField11.setValue("Frodo");
			metaField11.setGroupId(14);
			metaFieldValues.add(metaField11);

			MetaFieldValueWS metaField12 = new MetaFieldValueWS();
			metaField12.setFieldName("LAST_NAME");
			metaField12.getMetaField().setDataType(DataType.STRING);
			metaField12.setValue("Baggins");
			metaField12.setGroupId(14);
			metaFieldValues.add(metaField12);

			MetaFieldValueWS metaField13 = new MetaFieldValueWS();
			metaField13.setFieldName("ADDRESS1");
			metaField13.getMetaField().setDataType(DataType.STRING);
			metaField13.setValue("Baggins");
			metaField13.setGroupId(14);
			metaFieldValues.add(metaField13);

			MetaFieldValueWS metaField14 = new MetaFieldValueWS();
			metaField14.setFieldName("CITY");
			metaField14.getMetaField().setDataType(DataType.STRING);
			metaField14.setValue("Baggins");
			metaField14.setGroupId(14);
			metaFieldValues.add(metaField14);

			MetaFieldValueWS metaField15 = new MetaFieldValueWS();
			metaField15.setFieldName("BILLING_EMAIL");
			metaField15.getMetaField().setDataType(DataType.STRING);
			metaField15.setValue(newUser.getUserName() + "@shire.com");
			metaField15.setGroupId(14);
			metaFieldValues.add(metaField15);

			MetaFieldValueWS metaField16 = new MetaFieldValueWS();
			metaField16.setFieldName("POSTAL_CODE");
			metaField16.getMetaField().setDataType(DataType.STRING);
			metaField16.setValue("K0");
			metaField16.setGroupId(14);
			metaFieldValues.add(metaField16);

		}
		newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

		logger.debug("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
		String creditCard =null;
		if(null != isValidCreditcard)
			creditCard = isValidCreditcard==Boolean.TRUE ? CREDIT_CARD_NUMBER : INVALID_CREDIT_CARD_NUMBER;
		logger.debug("creditCard :::::::: {}", creditCard);
		PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				creditCard, expiry.getTime());

		newUser.getPaymentInstruments().add(cc);

		logger.debug("Creating user ...");
		MainSubscriptionWS billing = new MainSubscriptionWS();
		billing.setPeriodId(2);
		billing.setNextInvoiceDayOfPeriod(1);
		newUser.setMainSubscription(billing);
		newUser.setNextInvoiceDate(nextInvoiceDate);
		logger.debug("User created with id: {}", newUser.getUserId());

		return newUser;
	}

	public static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
			String fieldName) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				return ws;
			}
		}
		return null;
	}
	@AfterClass
	private void cleanUp() {
		api.deletePlugin(brainTreePluginId);
		api.deletePlugin(saveCreditCardPluginId);
	}
}
