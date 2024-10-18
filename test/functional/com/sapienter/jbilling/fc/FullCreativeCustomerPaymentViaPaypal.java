package com.sapienter.jbilling.fc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
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

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.framework.TestBuilder;

/**
 *
 * @author nazish
 *
 */
@Test(groups = "external-system", testName = "FullCreativeCustomerPaymentViaPaypal")
public class FullCreativeCustomerPaymentViaPaypal {

    private static final Logger logger = LoggerFactory.getLogger(FullCreativeCustomerPaymentViaPaypal.class);
    private JbillingAPI api;
    private final static Integer CC_PM_ID = 1;

    private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private final static String CC_MF_NUMBER = "cc.number";
    private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private final static String CC_MF_TYPE = "cc.type";
    private final static String CREDIT_CARD_NUMBER = "5257279846844529";
    private final static String CREDIT_CARD_NUMBER_NEW = "4916841912947530";
    private final static Integer migrationAcoountTypeId=60103;
    private final static Integer standardAccountTypeId=60104;
    private static final Integer PAYPAL_IPN_ID = 1;
    private static Integer paymentPayPalPluginId;
    private static Integer saveCreditCardPluginId;
    private static TestBuilder testBuilder;
    private Integer tempUserCode;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        testBuilder = getTestEnvironment();
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID1);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID2);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FILTER_TASK_ID);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CCF_TASK_ID);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CUREENCY_TASK_ID);
        
        PluggableTaskWS paymentPayPalPlugin = new PluggableTaskWS();
        paymentPayPalPlugin.setProcessingOrder(1);
        paymentPayPalPlugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.PAYMNET_PAYPAL_EXTERNAL_TASK_CLASS_NAME).getId());
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put("PaypalUserId", "paypal-facilitator_api1.answerconnect.com");
        parameters.put("PaypalPassword", "1398095235");
        parameters.put("PaypalSignature", "AT9-1shSE4saHFiVmaoZMYRy-uuGA5ct7uBfzjbeO97zJFKlDLC6BLNw");
        parameters.put("PaypalEnvironment", "sandbox");
        paymentPayPalPlugin.setParameters(parameters);
        paymentPayPalPlugin.setOwningEntityId(api.getCallerCompanyId());
        paymentPayPalPluginId = api.createPlugin(paymentPayPalPlugin);
        
        PluggableTaskWS saveCreditCardExternalPlugin = new PluggableTaskWS();
        
        saveCreditCardExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
        saveCreditCardExternalPlugin.setProcessingOrder(1234);
        saveCreditCardExternalPlugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME).getId());
        parameters.clear();
        
        parameters = new Hashtable<String, String>();
        parameters.put("removeOnFail", "false");
        parameters.put("externalSavingPluginId",paymentPayPalPluginId.toString());
        parameters.put("contactType", "14");
        parameters.put("obscureOnFail", "false");
        
        saveCreditCardExternalPlugin.setParameters(parameters);
        saveCreditCardPluginId = api.createPlugin(saveCreditCardExternalPlugin);
        
        
        
    }

    @Test(enabled = true)
    public void test001PayPalPayment() throws Exception {

       UserWS newUser1 = createUser(new Date(), true);
       logger.debug("User : {}", newUser1);

        UserWS newUser = api.getUserWS( newUser1.getId() );

        List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
        MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();
        char[] cardNumber = null;
        for(MetaFieldValueWS metafield : metaFields){
            if( (metafield.getFieldName()).equals("cc.number") )
                cardNumber = metafield.getCharValue();
        }


        assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));
        assertEquals("First 12 digits of credit Card Number should be obscure","************",new String(cardNumber).substring(0, 12));
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
        assertEquals("correct payment amount", new BigDecimal("20.0000000000"), lastPayment1.getAmountAsDecimal());
        assertEquals("correct payment balance", new BigDecimal("0E-10"), lastPayment1.getBalanceAsDecimal());

        api.updateUser(newUser);

        PaymentInformationWS paypal = null;
        metaFields = null;
        cardNumber = null;

        for(PaymentInformationWS instrument : newUser.getPaymentInstruments()) {
            if(instrument.getPaymentMethodTypeId() == CC_PM_ID) {
                paypal = instrument;
            }
        }
        logger.debug(" Updated User paypal Instrument is: {}", paypal);

        for(MetaFieldValueWS metafield : paypal.getMetaFields()){
            if( (metafield.getFieldName()).equals("cc.number") )
                cardNumber = metafield.getCharValue();
        }


        assertTrue("Updated user credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));

    }

    @Test(enabled = true)
    public void test002PayPalPaymentWithoutBillingGroup() throws Exception {
        UserWS newUser1 = null;
        AccountTypeWS accountTypeWS = createAccountType();
    	Integer groupId = accountTypeWS.getInformationTypeIds()[0];
		try {
			newUser1 = createUser(new Date(), false, true, accountTypeWS.getId(), groupId);
			logger.debug("User : {}", newUser1);
			fail(" This api call should throw field value failed validation exception, instead its passed");
		} catch (SessionInternalError e) {
			assertTrue("User Creation should fail due to Mandatory Metafield value",e.getMessage().contains("Field value failed validation.."));
		} finally {
			api.deleteAccountType(accountTypeWS.getId());
		}
    }

    @Test(enabled = true)
    public void test003RemoveUsersPaymentInstrument () throws Exception {
    	
    	 UserWS newUser1 = createUser(new Date(), true);
         logger.debug("User : {}", newUser1);

         UserWS newUser = api.getUserWS( newUser1.getId() );

         List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
         MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();
         char[] cardNumber = null;
         for(MetaFieldValueWS metafield : metaFields){
             if( (metafield.getFieldName()).equals("cc.number") )
                 cardNumber = metafield.getCharValue();
         }

         assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));
         
         newUser.getPaymentInstruments().clear();
         api.updateUser(newUser);
         newUser = api.getUserWS(newUser.getId());
         logger.debug("newUser.getPaymentInstruments() : {}", newUser.getPaymentInstruments().isEmpty());
         assertTrue("Payment Information of user Should be Empty ", newUser.getPaymentInstruments().isEmpty());
         
         Calendar expiry = Calendar.getInstance();
         expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 2);

         PaymentInformationWS cc = createCreditCard("PaypalIPN",
                 CREDIT_CARD_NUMBER,
                 expiry.getTime());
         
         PaymentWS payment = new PaymentWS();
         payment.setAmount(new BigDecimal("30"));
         payment.setIsRefund(0);
         payment.setPaymentDate(Calendar.getInstance().getTime());
         payment.setCurrencyId(Integer.valueOf(1));
         payment.setUserId(newUser.getUserId());
         payment.getPaymentInstruments().add(cc);

         // Testing first payment through Paypal
         logger.debug("Processing token payment...");
         PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
         assertNotNull("Payment result not null", authInfo);
         logger.debug("Payment Authorization result should be OK, actual result : {}", authInfo.getResult());
         assertTrue("Payment Authorization result should be OK", authInfo.getResult());
	}
    
    @Test(enabled = true)
    public void test004UpdateUsersPaymentInstrument () throws Exception {
    	
    	 UserWS newUser1 = createUser(new Date(), true);
         logger.debug("User : {}", newUser1);

         UserWS newUser = api.getUserWS( newUser1.getId() );

         List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
         MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();
         char[] cardNumber = null;
         for(MetaFieldValueWS metafield : metaFields){
             if( (metafield.getFieldName()).equals("cc.number") )
                 cardNumber = metafield.getCharValue();
         }

         assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));
         
         newUser.getPaymentInstruments().clear();
         Calendar expiry = Calendar.getInstance();
         expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 2);

         PaymentInformationWS cc = createCreditCard("PaypalIPN",
        		 CREDIT_CARD_NUMBER_NEW,
                 expiry.getTime());
         
         paymentInstruments = new ArrayList<PaymentInformationWS>();
         paymentInstruments.add(cc);
         newUser.setPaymentInstruments(paymentInstruments);
         api.updateUser(newUser);
         newUser =api.getUserWS(newUser.getId());
         logger.debug("newUser.getPaymentInstruments() : {}", newUser.getPaymentInstruments());
         
         PaymentWS payment = new PaymentWS();
         payment.setAmount(new BigDecimal("30"));
         payment.setIsRefund(0);
         payment.setPaymentDate(Calendar.getInstance().getTime());
         payment.setCurrencyId(Integer.valueOf(1));
         payment.setUserId(newUser.getUserId());
         PaymentInformationWS cc1 = newUser.getPaymentInstruments().get(0);
         payment.getPaymentInstruments().add(cc1);

         // Testing first payment through Paypal
         logger.debug("Processing token payment...");
         PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
         assertNotNull("Payment result not null", authInfo);
         logger.debug("Payment Authorization result should be OK actual result : {}", authInfo.getResult());
         assertTrue("Payment Authorization result should be OK", authInfo.getResult());
	}
    
    @Test(enabled = true)
    public void test005CreateUserWithoutMandatoryFields () throws Exception {
    	UserWS newUser = null;
    	AccountTypeWS accountTypeWS = createAccountType();
    	Integer groupId = accountTypeWS.getInformationTypeIds()[0];
		try {
			UserWS newUser1 = createUser(new Date(), false, true, accountTypeWS.getId(), groupId);
			logger.debug("User : {}", newUser1);
			newUser = api.getUserWS(newUser1.getId());
			fail(" This api call should throw field value failed validation exception, instead its passed");
		} catch (SessionInternalError e) {
			assertTrue(e.getMessage().contains("Field value failed validation.."));
		} finally {
			api.deleteAccountType(accountTypeWS.getId());
		}
	}

    @Test(enabled = true)
	public void test006ProcessSignupPaymentWithoutMandatoryFields()	throws Exception {
    	Integer userId = null;
    	AccountTypeWS accountTypeWS = createAccountType();
    	Integer groupId = accountTypeWS.getInformationTypeIds()[0];
    	UserWS newUser1 = createUserForProcessSignup(new Date(), false,	false, accountTypeWS.getId(), groupId);
		logger.debug("User : {}", newUser1);
		PaymentWS payment = createPayment(newUser1.getId(), new BigDecimal("1"), newUser1.getPaymentInstruments().get(0));
		logger.debug("Processing token payment...");
		try {
			userId = api.processSignupPayment(newUser1, payment);
			fail(" This api call should throw field value failed validation exception, instead its passed");
		} catch (SessionInternalError e) {
			assertTrue(e.getMessage().contains("Field value failed validation.."));
		} finally {
			api.deleteAccountType(accountTypeWS.getId());
		}
	}

    @Test(enabled = true)
    public void test007RemovePaymentInstrumentThroughApi() throws Exception {
        testBuilder.given(envBuilder -> {
            try {
                UserWS newUser1;
                newUser1 = createUser(new Date(), true);
                logger.debug("User : {}", newUser1);
                UserWS newUser = api.getUserWS( newUser1.getId() );
                tempUserCode = newUser.getId();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).test((env) -> {
            int tempInstruments = 0;
            try {
                UserWS userWS = api.getUserWS(tempUserCode);
                tempInstruments = userWS.getPaymentInstruments().size();
                for (PaymentInformationWS instrument : userWS.getPaymentInstruments()) {
                    if (instrument.getPaymentMethodTypeId() == PAYPAL_IPN_ID) {
                        api.removePaymentInstrument(instrument.getId());
                        tempInstruments = tempInstruments - 1;
                    }
                }
                assertEquals("Payment Instrument should've been removed",
                        tempInstruments, api.getUserWS(tempUserCode).getPaymentInstruments().size());
            } finally {
                api.deleteUser(tempUserCode);
            }
        });
    }

    public static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress) throws JbillingAPIException,
	IOException {
    	return createUserForProcessSignup(nextInvoiceDate,populateBillingGroupAddress,true,migrationAcoountTypeId,14);
    }

    public static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress, boolean useAPI, Integer accountTypeId, Integer groupID) throws JbillingAPIException,
	IOException {
    	return createUserForProcessSignup(nextInvoiceDate,populateBillingGroupAddress,useAPI,accountTypeId,groupID);
    }

    public static UserWS createUserForProcessSignup(Date nextInvoiceDate, boolean populateBillingGroupAddress, boolean useAPI, Integer accountTypeId, Integer groupID) throws JbillingAPIException,
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
        metaField7.getMetaField().setMandatory(true);
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
        metaField9.setGroupId(groupID);
        metaFieldValues.add(metaField9);
        
        MetaFieldValueWS metaField10 = new MetaFieldValueWS();
        metaField10.setFieldName("STATE_PROVINCE");
        metaField10.getMetaField().setDataType(DataType.STRING);
        metaField10.setValue("OR");
        metaField10.setGroupId(groupID);
        metaFieldValues.add(metaField10);
        
        MetaFieldValueWS metaField11 = new MetaFieldValueWS();
        metaField11.setFieldName("ORGANIZATION");
        metaField11.getMetaField().setDataType(DataType.STRING);
        metaField11.setValue("Frodo");
        metaField11.setGroupId(groupID);
        metaFieldValues.add(metaField11);

        MetaFieldValueWS metaField12 = new MetaFieldValueWS();
        metaField12.setFieldName("LAST_NAME");
        metaField12.getMetaField().setDataType(DataType.STRING);
        metaField12.setValue("Baggins");
        metaField12.setGroupId(groupID);
        metaFieldValues.add(metaField12);

        MetaFieldValueWS metaField13 = new MetaFieldValueWS();
        metaField13.setFieldName("ADDRESS1");
        metaField13.getMetaField().setDataType(DataType.STRING);
        metaField13.setValue("Baggins");
        metaField13.setGroupId(groupID);
        metaFieldValues.add(metaField13);
        
        MetaFieldValueWS metaField14 = new MetaFieldValueWS();
        metaField14.setFieldName("CITY");
        metaField14.getMetaField().setDataType(DataType.STRING);
        metaField14.setValue("Baggins");
        metaField14.setGroupId(groupID);
        metaFieldValues.add(metaField14);
        
        MetaFieldValueWS metaField15 = new MetaFieldValueWS();
        metaField15.setFieldName("BILLING_EMAIL");
        metaField15.getMetaField().setDataType(DataType.STRING);
        metaField15.setValue(newUser.getUserName() + "@shire.com");
        metaField15.setGroupId(groupID);
        metaFieldValues.add(metaField15);
        
        MetaFieldValueWS metaField16 = new MetaFieldValueWS();
        metaField16.setFieldName("POSTAL_CODE");
        metaField16.getMetaField().setDataType(DataType.STRING);
        metaField16.setValue("K0");
        metaField16.setGroupId(groupID);
        metaFieldValues.add(metaField16);
        
        }

        newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
        
        logger.debug("Meta field values set");
        
        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        
        PaymentInformationWS cc = createCreditCard("Frodo Baggins",
        		"5257279846844529", expiry.getTime());
        
        newUser.getPaymentInstruments().add(cc);
        
        logger.debug("Creating user ...");
        MainSubscriptionWS billing = new MainSubscriptionWS();
        billing.setPeriodId(2);
        billing.setNextInvoiceDayOfPeriod(1);
        newUser.setMainSubscription(billing);
        newUser.setNextInvoiceDate(nextInvoiceDate);
        if(useAPI){
        	JbillingAPI api = JbillingAPIFactory.getAPI();
        	newUser.setUserId(api.createUser(newUser));
        }
        logger.debug("User created with id : {}", newUser.getUserId());
        
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
        metaFieldWS6.setName("ADDRESS2");
        metaFieldWS6.setFieldUsage(MetaFieldType.ADDRESS2);
        metaFieldWS6.setDataType(DataType.STRING);
        metaFieldWS6.setMandatory(false);
        metaFieldWS6.setEntityId(1);
        metaFieldWS6.setEntityType(EntityType.ACCOUNT_TYPE);
        metaFields.add(metaFieldWS6);

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
    
    private void updateProcessingOrderOfPlugin(JbillingAPI api, Integer pluginId) {
	PluggableTaskWS plugIn = api.getPluginWS(pluginId);
	plugIn.setProcessingOrder(plugIn.getProcessingOrder()+10);
	plugIn.setParameters(new Hashtable<String, String>(plugIn.getParameters()));
	api.updatePlugin(plugIn);
    }
    
    private TestBuilder getTestEnvironment() {

        return TestBuilder.newTest(false).givenForMultiple(
                testEnvCreator -> {
                    EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
                });
    }

    @AfterClass
    private void cleanUp() {
	api.deletePlugin(paymentPayPalPluginId);
	api.deletePlugin(saveCreditCardPluginId);
    }
}
