package com.sapienter.jbilling.server.payment;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

@Test(groups = "external-system", testName = "PaymentPaySafeTaskTest")
public class PaymentPaySafeTaskTest {

	private static final Logger logger = LoggerFactory.getLogger(PaymentPaySafeTaskTest.class);
	private JbillingAPI api;
	private static final Integer PAY_SAFE_PAYMENT_METHOD_ID = 150;
	private Integer accountTypeId;
	private Integer contactSectionId;
	private Integer billingSectionId;
	private final static String CC_MF_CARDHOLDER_NAME = "Card Holder Name";
	private final static String CC_MF_NUMBER = "Card Number";
	private final static String CC_MF_EXPIRY_DATE = "Expiry Date";
	private final static String CC_MF_TYPE = "Card Type";
	private final static String VALID_CREDIT_CARD_NUMBER_1 = "5257279846844529";
	private final static String VALID_CREDIT_CARD_NUMBER_2 = "4111111111111111";
	private final static String INVALID_CREDIT_CARD_NUMBER = "5257279846844523";
	private final static String PAYMENT_FAKE_TASK_CLASS_NAME = "com.sapienter.jbilling.server.pluggableTask.PaymentFakeTask";
	private final static String PAYMENT_ROUTER_CCF_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentRouterCCFTask";
	private final static String PAYMENT_FILTER_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentFilterTask";
    private final static String PAYMENT_ROUTER_CURRENCY_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentRouterCurrencyTask";
	public static final String PAYMNET_PAY_SAFE_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentPaySafeTask";
	public static final String SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.SaveCreditCardExternallyTask";
	private Integer paymentPaySafePluginId ;
	private Integer saveCreditCardPluginId;
	
	@BeforeClass
	public void init() throws Exception {
		api = JbillingAPIFactory.getAPI();
		logger.debug("Creating Account Type ");
		
		AccountTypeWS accountType = new AccountTypeBuilder()
											.addDescription("Pay Safe Test Account "+System.currentTimeMillis(), api.getCallerLanguageId())
											.paymentMethodTypeIds(new Integer[]{PAY_SAFE_PAYMENT_METHOD_ID})
											.create(api);
		accountTypeId = accountType.getId();
		
		assertNotNull("Account Type Should be created", accountTypeId);
		
		Map<MetaFieldType, DataType> contactSection = new HashMap<MetaFieldType, DataType>();
		contactSection.put(MetaFieldType.FIRST_NAME, DataType.STRING);
		contactSection.put(MetaFieldType.LAST_NAME, DataType.STRING);
		contactSection.put(MetaFieldType.COUNTRY_CODE, DataType.STRING);
		contactSection.put(MetaFieldType.POSTAL_CODE, DataType.STRING);
		contactSection.put(MetaFieldType.STATE_PROVINCE, DataType.STRING);

		Map<MetaFieldType, DataType> billingSection = new HashMap<MetaFieldType, DataType>();
		billingSection.put(MetaFieldType.INITIAL, DataType.BOOLEAN);
		billingSection.put(MetaFieldType.FIRST_NAME, DataType.STRING);
		billingSection.put(MetaFieldType.LAST_NAME, DataType.STRING);
		billingSection.put(MetaFieldType.COUNTRY_CODE, DataType.STRING);
		billingSection.put(MetaFieldType.POSTAL_CODE, DataType.STRING);
		billingSection.put(MetaFieldType.STATE_PROVINCE, DataType.STRING);

		contactSectionId = buildAitWithMetaFields(accountType, contactSection, "Contact Info");
		assertNotNull("Contact Section Should be created", contactSectionId);
		
		billingSectionId = buildAitWithMetaFields(accountType, billingSection, "Billing Info");
		assertNotNull("Billing Section Should be created", billingSectionId);
		
		updateProcessingOrderOfPlugin(api, PAYMENT_FAKE_TASK_CLASS_NAME);
		updateProcessingOrderOfPlugin(api, PAYMENT_FILTER_TASK_CLASS_NAME);
		updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CCF_TASK_CLASS_NAME);
		updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CURRENCY_TASK_CLASS_NAME);
		
		PluggableTaskWS paymentPaySafePlugin = new PluggableTaskWS();
		
		paymentPaySafePlugin.setProcessingOrder(1);
		paymentPaySafePlugin.setTypeId(api.getPluginTypeWSByClassName(PAYMNET_PAY_SAFE_TASK_CLASS_NAME).getId());
		
		Hashtable<String, String> parameters = new Hashtable<String, String>();

		parameters.put("MerchantAccountNumber", "1001080520");
		parameters.put("UserName", "test_test-paysafe");
		parameters.put("Password", "B-qa2-0-587f2d0b-0-302c02144e3fc9004da98dad9c0b312c90261753689cf0d402147ecb8a182e4b1a689c580a1ba263623e72aebfbd");
		parameters.put("Url", "https://api.test.paysafe.com");
		parameters.put("CurrencyCode", "USD");
		parameters.put("Customer Contact Section Name", "Contact Info");
		parameters.put("Customer Billing Address Section Name", "Billing Info");
		
		paymentPaySafePlugin.setParameters(parameters);
		paymentPaySafePlugin.setOwningEntityId(api.getCallerCompanyId());
		paymentPaySafePluginId = api.createPlugin(paymentPaySafePlugin);

		PluggableTaskWS saveCreditCardExternalPlugin = new PluggableTaskWS();

		saveCreditCardExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
		saveCreditCardExternalPlugin.setProcessingOrder(1234);
		saveCreditCardExternalPlugin.setTypeId(api.getPluginTypeWSByClassName(SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME).getId());
		parameters.clear();

		parameters = new Hashtable<String, String>();
		parameters.put("removeOnFail", "false");
		parameters.put("externalSavingPluginId",paymentPaySafePluginId.toString());
		parameters.put("contactType", contactSectionId.toString());
		parameters.put("obscureOnFail", "false");

		saveCreditCardExternalPlugin.setParameters(parameters);
		saveCreditCardPluginId = api.createPlugin(saveCreditCardExternalPlugin);
	}
	
	@Test
	public void testSuccessfulUserCIMProfileCreationAndRecurringPayment() throws Exception {
		UserWS user = createUser(accountTypeId, VALID_CREDIT_CARD_NUMBER_1);
		assertNotNull("user Should be created", user);
		
		 List<PaymentInformationWS> paymentInstruments = user.getPaymentInstruments();
	     MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();
	        
	     char[] cardNumber = Arrays.stream(metaFields)
	        					   .filter(metaField -> metaField.getFieldName().equals(CC_MF_NUMBER))
	        					   .map(metaField -> metaField.getCharValue())
	        					   .findFirst()
	        					   .orElse(" ".toCharArray());

	     assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));
	     
	     
	     PaymentWS payment = new PaymentWS();
	        payment.setAmount(new BigDecimal("30"));
	        payment.setIsRefund(0);
	        payment.setPaymentDate(Calendar.getInstance().getTime());
	        payment.setCurrencyId(Integer.valueOf(1));
	        payment.setUserId(user.getUserId());
	        PaymentInformationWS cc = user.getPaymentInstruments().get(0);
	        payment.getPaymentInstruments().add(cc);

	        // Testing first payment through PaySafe
	        logger.debug("Processing token payment...");
	        PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
	        assertNotNull("Payment result not null", authInfo);
	        assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

	        // check payment has zero balance
	        PaymentWS lastPayment = api.getLatestPayment(user.getId());

	        assertNotNull("payment can not be null", lastPayment);
	        assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
	        assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
	        assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());
	}
	
	@Test(expectedExceptions = SessionInternalError.class,
			expectedExceptionsMessageRegExp = "(?s).*Either you submitted a request that is missing a mandatory field or "
					+ "the value of a field does not match the format expected.*")
	public void testFailedUserCIMProfileCreation() throws Exception {
		UserWS user = createUser(accountTypeId, INVALID_CREDIT_CARD_NUMBER);
		assertNotNull("user Should be created", user);
	}
	
	@Test
	public void testUpdateUsersPaymentInstrument () throws Exception {

		UserWS user = createUser(accountTypeId, VALID_CREDIT_CARD_NUMBER_1);
		assertNotNull("user Should be created", user);

		UserWS newUser = api.getUserWS(user.getId());

		List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
		MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();

		char[] cardNumber = Arrays.stream(metaFields)
				.filter(metaField -> metaField.getFieldName().equals(CC_MF_NUMBER))
				.map(metaField -> metaField.getCharValue())
				.findFirst()
				.orElse(" ".toCharArray());

		assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));
		assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).endsWith("4529"));
		newUser.getPaymentInstruments().clear();
		api.updateUser(newUser);
		newUser = api.getUserWS(newUser.getId());
		assertTrue("Payment Information of user Should be Empty ", newUser.getPaymentInstruments().isEmpty());
		
		 // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        
        PaymentInformationWS cc = createCreditCard("Frodo Baggins",
        		VALID_CREDIT_CARD_NUMBER_2, expiry.getTime());
        
        newUser.getPaymentInstruments().add(cc);
        
        api.updateUser(newUser);
        
        UserWS updatedUser = api.getUserWS(user.getId() );
    	char[] updatedCardNumber = Arrays.stream(updatedUser.getPaymentInstruments().get(0).getMetaFields())
				.filter(metaField -> metaField.getFieldName().equals(CC_MF_NUMBER))
				.map(metaField -> metaField.getCharValue())
				.findFirst()
				.orElse(" ".toCharArray());

		assertTrue("Credit Card Number should be obscure",updatedCardNumber != null && new String(updatedCardNumber).startsWith("************"));
		assertTrue("Credit Card Number should be obscure",updatedCardNumber != null && new String(updatedCardNumber).endsWith("1111"));
		
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("30"));
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(updatedUser.getUserId());
		payment.getPaymentInstruments().add(updatedUser.getPaymentInstruments().get(0));

		// Testing first payment through PaySafe
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment has zero balance
		PaymentWS lastPayment = api.getLatestPayment(updatedUser.getId());

		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());
	}
	
	@Test
	public void testRemoveUsersPaymentInstrument () throws Exception {

		UserWS user = createUser(accountTypeId, VALID_CREDIT_CARD_NUMBER_1);
		assertNotNull("user Should be created", user);

		UserWS newUser = api.getUserWS( user.getId() );

		List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
		MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();

		char[] cardNumber = Arrays.stream(metaFields)
				.filter(metaField -> metaField.getFieldName().equals(CC_MF_NUMBER))
				.map(metaField -> metaField.getCharValue())
				.findFirst()
				.orElse(" ".toCharArray());

		assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));

		newUser.getPaymentInstruments().clear();
		api.updateUser(newUser);
		newUser = api.getUserWS(newUser.getId());
		assertTrue("Payment Information of user Should be Empty ", newUser.getPaymentInstruments().isEmpty());
	}
	 
	@Test
	public void testSuccessfulOneTimePayment () throws Exception {
		UserWS user = createUser(accountTypeId, null);
		assertNotNull("user Should be created", user);
		
		 // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        
        PaymentInformationWS cc = createCreditCard("Frodo Baggins",
        		VALID_CREDIT_CARD_NUMBER_2, expiry.getTime());
        
    	
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("30"));
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(user.getUserId());
		payment.getPaymentInstruments().add(cc);

		// Testing first payment through PaySafe
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

		// check payment has zero balance
		PaymentWS lastPayment = api.getLatestPayment(user.getId());

		assertNotNull("payment can not be null", lastPayment);
		assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
		assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
		assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());
		
	}
	
	@Test
	public void testFailedOneTimePayment () throws Exception {
		UserWS user = createUser(accountTypeId, null);
		assertNotNull("user Should be created", user);
		
		 // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        
        PaymentInformationWS cc = createCreditCard("Frodo Baggins",
        		VALID_CREDIT_CARD_NUMBER_2, expiry.getTime());
        
    	
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("0.12"));
		payment.setIsRefund(0);
		payment.setPaymentDate(Calendar.getInstance().getTime());
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(user.getUserId());
		payment.getPaymentInstruments().add(cc);

		// Testing first payment through PaySafe
		logger.debug("Processing token payment...");
		PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
		assertNotNull("Payment result not null", authInfo);
		assertTrue("Payment Authorization result should be OK", !authInfo.getResult().booleanValue());
	}
	
	private UserWS createUser(Integer accountTypeId, String creditCardNumber) {
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
	        metaField1.setFieldName(MetaFieldType.COUNTRY_CODE.name());
	        metaField1.getMetaField().setDataType(DataType.STRING);
	        metaField1.setValue("IN");
	        metaField1.setGroupId(contactSectionId);
	        metaFieldValues.add(metaField1);
	        
	        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
	        metaField2.setFieldName(MetaFieldType.STATE_PROVINCE.name());
	        metaField2.getMetaField().setDataType(DataType.STRING);
	        metaField2.setValue("OR");
	        metaField2.setGroupId(contactSectionId);
	        metaFieldValues.add(metaField2);
	        
	        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
	        metaField3.setFieldName(MetaFieldType.FIRST_NAME.name());
	        metaField3.getMetaField().setDataType(DataType.STRING);
	        metaField3.setValue("Frodo");
	        metaField3.setGroupId(contactSectionId);
	        metaFieldValues.add(metaField3);
	        
	        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
	        metaField4.setFieldName(MetaFieldType.LAST_NAME.name());
	        metaField4.getMetaField().setDataType(DataType.STRING);
	        metaField4.setValue("Baggins");
	        metaField4.setGroupId(contactSectionId);
	        metaFieldValues.add(metaField4);
	        
	        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
	        metaField5.setFieldName(MetaFieldType.POSTAL_CODE.name());
	        metaField5.getMetaField().setDataType(DataType.STRING);
	        metaField5.setValue("1234");
	        metaField5.setGroupId(contactSectionId);
	        metaFieldValues.add(metaField5);
	        
	        MetaFieldValueWS metaField6 = new MetaFieldValueWS();
	        metaField6.setFieldName(MetaFieldType.INITIAL.name());
	        metaField6.getMetaField().setDataType(DataType.BOOLEAN);
	        metaField6.setValue(true);
	        metaField6.setGroupId(billingSectionId);
	        metaFieldValues.add(metaField6);
	        
	        newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
	        
	        logger.debug("Meta field values set");
	        if(null!= creditCardNumber) {
	        	// add a credit card
	        	Calendar expiry = Calendar.getInstance();
	        	expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

	        	PaymentInformationWS cc = createCreditCard("Frodo Baggins",
	        			creditCardNumber, expiry.getTime());

	        	newUser.getPaymentInstruments().add(cc);
	        }
	        
	        logger.debug("Creating user ...");
	        newUser.setUserId(api.createUser(newUser));
	        logger.debug("User created with id: {}", newUser.getUserId());
		return api.getUserWS(newUser.getId());
	}
	
	private Integer buildAitWithMetaFields(AccountTypeWS accountType, Map<MetaFieldType, DataType> metaFieldMap, String sectionName) {
		//create a valid meta field
		List<MetaFieldWS> metaFields = metaFieldMap.entrySet()
					.stream()
					.map(fieldEntry -> {
						MetaFieldWS metaField = new MetaFieldBuilder()
						.dataType(fieldEntry.getValue())
						.entityType(EntityType.ACCOUNT_TYPE)
						.name(fieldEntry.getKey().name())
						.fieldUsage(fieldEntry.getKey())
						.build();
						return metaField;
					}).collect(Collectors.toList());
		
		//create ait
		AccountInformationTypeWS ait = new AccountInformationTypeBuilder(accountType)
				.name(sectionName)
				.addMetaFields(metaFields)
				.build();

		return api.createAccountInformationType(ait);
	}
	
	public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(PAY_SAFE_PAYMENT_METHOD_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Integer.valueOf(2));
		//cc

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, new SimpleDateFormat(Constants.CC_DATE_FORMAT).format(date).toCharArray());

		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, false, false,
				DataType.STRING, 4, CreditCardType.VISA);
		addMetaField(metaFields, "cc.gateway.key" ,false, true, DataType.CHAR, 5, null );
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
	
	private void updateProcessingOrderOfPlugin(JbillingAPI api, String className) {
		PluggableTaskWS[] plugins = api.getPluginsWS(api.getCallerCompanyId(), className);
		Arrays.stream(plugins).forEach(pluggableTaskWS -> {
		    pluggableTaskWS.setProcessingOrder(pluggableTaskWS.getProcessingOrder()+10);
            pluggableTaskWS.setParameters(new Hashtable<>(pluggableTaskWS.getParameters()));
            api.updatePlugin(pluggableTaskWS);
        });
	}
	
	@AfterClass
	private void cleanUp() {
		if(null!=paymentPaySafePluginId || null!=saveCreditCardPluginId)
		api.deletePlugin(paymentPaySafePluginId);
		api.deletePlugin(saveCreditCardPluginId);
	}
}
