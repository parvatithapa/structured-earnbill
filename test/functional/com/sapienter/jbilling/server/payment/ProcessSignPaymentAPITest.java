package com.sapienter.jbilling.server.payment;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.user.UserDTOEx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.fc.FullCreativeTestConstants;

import java.io.IOException;
import java.lang.Integer;
import java.lang.System;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Hashtable;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import static org.testng.AssertJUnit.assertEquals;

/**
 *
 * @author Ashok Kale
 *
 */
@Test(groups = "external-system", testName = "ProcessSignPaymentAPITest")
public class ProcessSignPaymentAPITest {

	private static final Logger logger = LoggerFactory.getLogger(ProcessSignPaymentAPITest.class);
	private JbillingAPI api;
	private final static Integer PAYPALIPN_PM_ID = 4;

	private final static Integer CC_PM_ID = 1;
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	private final static String INVALID_CREDIT_CARD_NUMBER = "37828224631000";
	private final static String CREDIT_CARD_NUMBER_PAYPAL = "5555555555554444";
	private final static String AMEX_CREDIT_CARD_NUMBER_PAYPAL = "371449635398431";

	public static final int PAYMENT_FAKE_TASK_ID1 = 20;
	public static final int PAYMENT_ROUTER_CCF_TASK_ID = 21;
	public static final int PAYMENT_FAKE_TASK_ID2 = 22;
	public static final int PAYMENT_FILTER_TASK_ID = 460;
	public static final int PAYMENT_ROUTER_CUREENCY_TASK_ID = 520;


	private final static Integer migrationAcoountTypeId=60103;
	private final static String GATEWAY_KEY = "cc.gateway.key";
	private final static String PLUGIN_PARAM_EXTERNAL_PLUGIN_ID = "externalSavingPluginId";
	private Integer userId = null;
	private static Integer saveCreditCardPluginId;
	private static Integer paymentPayPalPluginId;

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


			logger.debug("Changing processing orders of payment plugin");
			//configure plugin PaymentPaypalExternalTask 
			PluggableTaskWS paymentPayPalPlugin = new PluggableTaskWS();
			paymentPayPalPlugin.setProcessingOrder(1);
			PluggableTaskTypeWS payPalPluginType = 
					api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.payment.tasks.PaymentPaypalExternalTask");
			paymentPayPalPlugin.setTypeId(payPalPluginType.getId());
			logger.debug("payment PayPal Plugin Type Id ::: {}", payPalPluginType.getId());
			Hashtable<String, String> payPalPluginparameters = new Hashtable<String, String>();
			payPalPluginparameters.put("PaypalUserId", "paypal-facilitator_api1.answerconnect.com");
			payPalPluginparameters.put("PaypalPassword", "1398095235");
			payPalPluginparameters.put("PaypalSignature", "AT9-1shSE4saHFiVmaoZMYRy-uuGA5ct7uBfzjbeO97zJFKlDLC6BLNw");
			payPalPluginparameters.put("PaypalEnvironment", "sandbox");
			paymentPayPalPlugin.setParameters(payPalPluginparameters);
			paymentPayPalPlugin.setOwningEntityId(api.getCallerCompanyId());

			paymentPayPalPluginId = api.createPlugin(paymentPayPalPlugin);
			logger.debug("paymentPayPalPluginId ******** ::: {}", paymentPayPalPluginId );

			// configure save creditcard external task
			PluggableTaskWS saveCreditCardExternalPlugin = new PluggableTaskWS();

			saveCreditCardExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
			saveCreditCardExternalPlugin.setProcessingOrder(1234);
			saveCreditCardExternalPlugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME).getId());
			Hashtable<String, String> saveCreditCardExternalparameters = new Hashtable<String, String>();

			saveCreditCardExternalparameters = new Hashtable<String, String>();
			saveCreditCardExternalparameters.put("removeOnFail", "false");
			saveCreditCardExternalparameters.put("externalSavingPluginId",paymentPayPalPluginId.toString());
			saveCreditCardExternalparameters.put("contactType", "14");
			saveCreditCardExternalparameters.put("obscureOnFail", "false");
			saveCreditCardExternalPlugin.setParameters(saveCreditCardExternalparameters);
			saveCreditCardPluginId = api.createPlugin(saveCreditCardExternalPlugin);
			logger.debug("saveCreditCardPluginId ******** ::: {}", saveCreditCardPluginId );

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test(enabled = true)
	public void test001SignupPaymentAPIWithoutCreditCardDetails() throws Exception {
		UserWS user = createUser(new Date(),true,null);
		PaymentWS payment = createPayment(user.getId(), new BigDecimal("100"), user.getPaymentInstruments().get(0));
		user.getPaymentInstruments().clear();

		logger.debug("Processing token payment...");
		try {
			userId = api.processSignupPayment(user, payment);
		} catch (Exception e) {
		}
		assertNull("UserId should be null as user would not get created", userId);
	}

	@Test(enabled = true)
	public void test002SignupPaymentAPIWithInvalidCreditCard() throws Exception {
		UserWS user = createUser(new Date(),true,false);

		PaymentWS payment = createPayment(user.getId(), new BigDecimal("100"), user.getPaymentInstruments().get(0));
		user.getPaymentInstruments().clear();
		logger.debug("Processing token payment...");
		try {
			userId = api.processSignupPayment(user, payment);
		} catch (Exception e) {
		}
		assertNull("UserId should be null as user would not get created", userId);
	}

	@Test(enabled = true)
	public void test003SignupPaymentAPIWithInvalidUserDetails() throws Exception {
		UserWS user = createUser(new Date(),true,true);
		user.setParentId(new Integer(-1111));

		PaymentWS payment = createPayment(user.getId(), new BigDecimal("1"), user.getPaymentInstruments().get(0));
		logger.debug("Processing token payment...");
		try {
			userId = api.processSignupPayment(user, payment);
		} catch (SessionInternalError e) {
			assertEquals("UserWS,parentId,validation.error.parent.does.not.exist", e.getErrorMessages()[0]);
		}
		assertNull("UserId should be null as user would not get created", userId);
	}

	@Test
	public void test004ProcessSignupPaymentAPIWithHugeAmount() throws Exception {
		UserWS user = createUser(new Date(),true,true);

		PaymentWS payment = createPayment(user.getId(), new BigDecimal("9999999999"), user.getPaymentInstruments().get(0));
		logger.debug("Processing token payment...");
		try {
		    api.processSignupPayment(user, payment);
		} catch (SessionInternalError e) {
			assertNotNull(e);
		}
		assertNull("UserId should be null as user would not get created", userId);
	}


	@Test(enabled = true)
	public void test006ProcessSignupPaymentAPIWithPayPal() throws Exception {
		UserWS user = createUser(new Date(),true,true);
		logger.debug("user ::::::::::::: {}", user);
		PaymentWS payment = createPayment(user.getId(), new BigDecimal("1"), user.getPaymentInstruments().get(0));
		logger.debug("Processing token payment...");
		userId = api.processSignupPayment(user, payment);
		logger.debug("userId ::::::::::::: {}", userId);
		assertNotNull("User Id can not be null", userId);
		user = api.getUserWS(userId);

		assertNotNull("User can not be null", user);
		PaymentInformationWS card = user.getPaymentInstruments().iterator().next();
		assertNotNull("Credit Card can not be null", card);
		logger.debug("Updated card {}", card.getId());
		assertEquals("card type MasterCard", "MasterCard", getMetaField(card.getMetaFields(), CC_MF_TYPE).getStringValue());
		char[] cardNumber = getMetaField(card.getMetaFields(), CC_MF_NUMBER).getCharValue();
		assertEquals("First 12 digits of Card number should be.", "************",new String(cardNumber).substring(0, 12));
		assertEquals("Last 4 digits of Card number should be.", "4444",new String(cardNumber,cardNumber.length-4,4));

		payment = api.getLatestPayment(userId);
		assertNotNull("payment can not be null", payment);
		assertEquals("Payment result should be successful ", payment.getResultId(), Constants.PAYMENT_RESULT_SUCCESSFUL);
		assertNotNull("Gateway key can not be null", new String(getMetaField(card.getMetaFields(), GATEWAY_KEY).getCharValue()));
		assertEquals("Correct payment amount", "1.0000000000", payment.getAmount());


        /**
         * Below test is for hotfix/JB-1998 - test update user with CIM profile created
         * @author Manish
         */
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("Account PIN");
        metaField1.getMetaField().setDataType(DataType.STRING);
        metaField1.setValue("Test-Account-pin-test006**");
        metaFieldValues.add(metaField1);

        user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

        api.updateUser(user);
        logger.debug("User Update Must be successful ");
        assertNotNull("User should not be null ", api.getUserByCustomerMetaField("Test-Account-pin-test006**", "Account PIN"));

	}

	@Test(enabled = true)
	public void test007ProcessSignupPaymentAPICardValidation() throws Exception {
		AccountTypeWS accountTypeWS = createAccountType();
    	Integer groupId = accountTypeWS.getInformationTypeIds()[0];
		UserWS user = createUser(new Date(),true,true,false,accountTypeWS.getId(),groupId);
		PaymentWS payment = createPayment(user.getId(), new BigDecimal("1"), user.getPaymentInstruments().get(0));
		logger.debug("Processing token payment...");
		try {
			userId = api.processSignupPayment(user, payment);
			fail(" This api call should throw field value failed validation exception, instead its passed");
		} catch (SessionInternalError e) {
			assertTrue(e.getMessage().contains("Field value failed validation.."));
		} finally {
			api.deleteAccountType(accountTypeWS.getId());
		}
	}

	/**
     * Below test is for hotfix/JB-1998 - test update user with CIM profile created
     * @author Harshad Pathan
     */
	@Test(enabled = true)
    public void test008ProcessSignupPaymentAPIWithPayPalWith15DigitAmexCard() throws Exception {

        api = JbillingAPIFactory.getAPI();
        UserWS user = createUser(new Date(),true,true);

        user.getPaymentInstruments().forEach(System.out::println);

        Arrays.asList(user.getPaymentInstruments().get(0).getMetaFields())
                                                  .stream()
                                                  .filter(mf -> mf.getFieldName().equals(CC_MF_NUMBER))
                                                  .findAny().get()
                                                  .setStringValue(AMEX_CREDIT_CARD_NUMBER_PAYPAL);

        PaymentWS payment = createPayment(user.getId(), new BigDecimal("1"), user.getPaymentInstruments().get(0));
        logger.debug("Processing token payment...");
        userId = api.processSignupPayment(user, payment);
        logger.debug("userId ::::::::::::: {}", userId);

        user = api.getUserWS(userId);
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("Account PIN");
        metaField1.getMetaField().setDataType(DataType.STRING);
        metaField1.setValue("Test-Account-pin-007**");
        metaFieldValues.add(metaField1);

        user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

        api.updateUser(user);
        logger.debug("User Update Must be successful ");
        assertNotNull("User should not be null ", api.getUserByCustomerMetaField("Test-Account-pin-007**", "Account PIN"));


    }

	public static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress, Boolean isValidCreditcard) throws JbillingAPIException,
	IOException {
		return createUser(new Date(),true,true,true,migrationAcoountTypeId,14);
	}

	public static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress, Boolean isValidCreditcard, boolean isValidUser, Integer accountTypeId, Integer groupId) throws JbillingAPIException,
	IOException {
		UserWS newUser = new UserWS();
		List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
		newUser.setUserId(0);
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword12");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(accountTypeId);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));

		logger.debug("User properties set");
		MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		metaField1.setFieldName("Country");
		metaField1.getMetaField().setDataType(DataType.STRING);
		metaField1.setValue("CA");
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

			if(isValidUser) {
				MetaFieldValueWS metaField13 = new MetaFieldValueWS();
				metaField13.setFieldName("ADDRESS1");
				metaField13.setValue("Baggins");
				metaField13.getMetaField().setDataType(DataType.STRING);
				metaField13.setGroupId(groupId);
				metaFieldValues.add(metaField13);

				MetaFieldValueWS metaField14 = new MetaFieldValueWS();
				metaField14.setFieldName("CITY");
				metaField14.setValue("Baggins");
				metaField14.getMetaField().setDataType(DataType.STRING);
				metaField14.setGroupId(groupId);
				metaFieldValues.add(metaField14);

				MetaFieldValueWS metaField15 = new MetaFieldValueWS();
				metaField15.setFieldName("BILLING_EMAIL");
				metaField15.setValue(newUser.getUserName() + "@shire.com");
				metaField15.getMetaField().setDataType(DataType.STRING);
				metaField15.setGroupId(groupId);
				metaFieldValues.add(metaField15);
			}

			MetaFieldValueWS metaField16 = new MetaFieldValueWS();
			metaField16.setFieldName("POSTAL_CODE");
			metaField16.setValue("K0");
			metaField16.getMetaField().setDataType(DataType.STRING);
			metaField16.setGroupId(groupId);
			metaFieldValues.add(metaField16);

		}
		newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

		logger.debug("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		String creditCard =null;
		if(null != isValidCreditcard)
			creditCard = isValidCreditcard==Boolean.TRUE ? CREDIT_CARD_NUMBER_PAYPAL : INVALID_CREDIT_CARD_NUMBER;
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

	private AccountTypeWS createAccountType() {
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setCreditLimit(new BigDecimal(0));
        accountType.setCurrencyId(new Integer(1));
        accountType.setEntityId(1);
        accountType.setInvoiceDeliveryMethodId(1);
        accountType.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        accountType.setMainSubscription(new MainSubscriptionWS(
                Constants.PERIOD_UNIT_DAY, 1));
        accountType.setCreditNotificationLimit1("0");
        accountType.setCreditNotificationLimit2("0");
        accountType.setCreditLimit("0");

        accountType.setName("Account Type_" + System.currentTimeMillis(),
                Constants.LANGUAGE_ENGLISH_ID);
        Integer[] paymentMethodIds = {1,2};
        accountType.setPaymentMethodTypeIds(paymentMethodIds);
        Integer newAccountTypeId = api.createAccountType(accountType);
        accountType = api.getAccountType(newAccountTypeId);
        List<MetaFieldWS> metaFields = new ArrayList<MetaFieldWS>(5);

        MetaFieldWS metaFieldWS1 = new MetaFieldWS();
        metaFieldWS1.setName("ORGANIZATION");
        metaFieldWS1.setFieldUsage(MetaFieldType.ORGANIZATION);
        metaFieldWS1.setDataType(DataType.STRING);
        metaFieldWS1.setMandatory(true);
        metaFieldWS1.setEntityId(1);
        metaFieldWS1.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS1);

        MetaFieldWS metaFieldWS2 = new MetaFieldWS();
        metaFieldWS2.setName("BILLING_EMAIL");
        metaFieldWS2.setFieldUsage(MetaFieldType.BILLING_EMAIL);
        metaFieldWS2.setDataType(DataType.STRING);
        metaFieldWS2.setMandatory(true);
        metaFieldWS2.setEntityId(1);
        metaFieldWS2.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS2);

        MetaFieldWS metaFieldWS3 = new MetaFieldWS();
        metaFieldWS3.setName("LAST_NAME");
        metaFieldWS3.setFieldUsage(MetaFieldType.LAST_NAME);
        metaFieldWS3.setDataType(DataType.STRING);
        metaFieldWS3.setMandatory(false);
        metaFieldWS3.setEntityId(1);
        metaFieldWS3.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS3);

        MetaFieldWS metaFieldWS4 = new MetaFieldWS();
        metaFieldWS4.setName("ADDRESS1");
        metaFieldWS4.setFieldUsage(MetaFieldType.ADDRESS1);
        metaFieldWS4.setDataType(DataType.STRING);
        metaFieldWS4.setMandatory(true);
        metaFieldWS4.setEntityId(1);
        metaFieldWS4.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS4);

        MetaFieldWS metaFieldWS5 = new MetaFieldWS();
        metaFieldWS5.setName("POSTAL_CODE");
        metaFieldWS5.setFieldUsage(MetaFieldType.POSTAL_CODE);
        metaFieldWS5.setDataType(DataType.STRING);
        metaFieldWS5.setMandatory(true);
        metaFieldWS5.setEntityId(1);
        metaFieldWS5.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS5);

        MetaFieldWS metaFieldWS6 = new MetaFieldWS();
        metaFieldWS6.setName("STATE_PROVINCE");
        metaFieldWS6.setFieldUsage(MetaFieldType.STATE_PROVINCE);
        metaFieldWS6.setDataType(DataType.STRING);
        metaFieldWS6.setMandatory(false);
        metaFieldWS6.setEntityId(1);
        metaFieldWS6.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS6);

        MetaFieldWS metaFieldWS7 = new MetaFieldWS();
        metaFieldWS7.setName("COUNTRY_CODE");
        metaFieldWS7.setFieldUsage(MetaFieldType.COUNTRY_CODE);
        metaFieldWS7.setDataType(DataType.STRING);
        metaFieldWS7.setMandatory(false);
        metaFieldWS7.setEntityId(1);
        metaFieldWS7.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS7);

        MetaFieldWS metaFieldWS8 = new MetaFieldWS();
        metaFieldWS8.setName("CITY");
        metaFieldWS8.setFieldUsage(MetaFieldType.CITY);
        metaFieldWS8.setDataType(DataType.STRING);
        metaFieldWS8.setMandatory(false);
        metaFieldWS8.setEntityId(1);
        metaFieldWS8.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS8);

        createInformationType(accountType, "BILLING_ADDRESS_", metaFields);

        return accountType;
    }

    private void createInformationType(AccountTypeWS accountTypeCreated, String accountInformationTypeName, List<MetaFieldWS> metaFields) {
        AccountInformationTypeWS accountInformationTypes = new AccountInformationTypeWS();
        accountInformationTypes.setEntityId(api.getCallerCompanyId());
        accountInformationTypes.setName(accountInformationTypeName);
        accountInformationTypes.setAccountTypeId(accountTypeCreated.getId());
        accountInformationTypes.setMetaFields(metaFields.toArray(new MetaFieldWS[0]));
        Integer accountInformationTypeId = api.createAccountInformationType(accountInformationTypes);
        accountTypeCreated.setInformationTypeIds(Arrays.asList(accountInformationTypeId).toArray(new Integer[0]));
        api.updateAccountType(accountTypeCreated);
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
			 cardNumber!=null ? cardNumber.toCharArray(): cardNumber);
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, (new SimpleDateFormat(
						Constants.CC_DATE_FORMAT).format(date)).toCharArray());

		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, false, false,
				DataType.STRING, 4, CreditCardType.MASTER_CARD);
		addMetaField(metaFields, "cc.gateway.key" ,false, true, DataType.CHAR, 5, null );
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

	public static PaymentWS createPayment(Integer userId, BigDecimal amount, PaymentInformationWS paymentInformation) {

		PaymentWS payment = new PaymentWS();
		payment.setAmount(amount);
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(userId);
		payment.getPaymentInstruments().add(paymentInformation);

		return payment;

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

	public static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
			String fieldName) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				return ws;
			}
		}
		return null;
	}

	private void updateProcessingOrderOfPlugin(JbillingAPI api, Integer pluginId) {
		PluggableTaskWS plugIn = api.getPluginWS(pluginId);
		plugIn.setProcessingOrder(plugIn.getProcessingOrder()+10);
		plugIn.setParameters(new Hashtable<String, String>(plugIn.getParameters()));
		api.updatePlugin(plugIn);
	}

	@AfterClass
	private void cleanUp() {
		api.deletePlugin(paymentPayPalPluginId);
		api.deletePlugin(saveCreditCardPluginId);
	}
}
