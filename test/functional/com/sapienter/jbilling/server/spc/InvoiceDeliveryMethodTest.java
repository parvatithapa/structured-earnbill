package com.sapienter.jbilling.server.spc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

@Test(groups = "agl", testName = "agl.InvoiceDeliveryMethodTest")
public class InvoiceDeliveryMethodTest extends SPCBaseConfiguration {

	private static final String  TEST_USER_1                                   = "Test-User1-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_2                                   = "Test-User2-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_3                                   = "Test-User3-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_4                                   = "Test-User4-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_5                                   = "Test-User5-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_6                                   = "Test-User6-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_7                                   = "Test-User7-"+ UUID.randomUUID().toString();
	private static final String  CUSTOMER_CREATION_FAILED                      = "Customer Creation Failed";
	private UserWS spcUserWS = null;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initialize() {
        System.out.println("InvoiceDeliveryMethodTest.initialize"+testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        
        System.out.println("InvoiceDeliveryMethodTest.initialize"+testBuilder);
    }

    @AfterClass
    public void afterTests() {
        System.out.println("InvoiceDeliveryMethodTest.afterTests");
    }

    /**
     * New Customer invoice delivery method is Email and Email Id field is blank
     */
    @Test(enabled = true, priority = 1)
    public void testNewCustomerInvoiceDeliveryMethodIsEmailAndEmailIdFieldIsBlank() {
        try {
        	testBuilder.given(envBuilder -> {
        		
        		spcUserWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_1)

        		        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
        		                .withNextInvoiceDate(new Date()).withCurrency(CURRENCY_AUD)
        		                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
        		                .withPaymentInstruments(getInstruments(new String[] {CC})).withMetaField(ENUMERATION_CUSTOMER_TYPE, CUSTOMER_TYPE_VALUE_PRE_PAID)
        		                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, getBillingAddress())
        		                .withAITGroup(CUSTOMER_DETAILS, getCustomerDetails(false))
        		                .withInvoiceDesign("")
        		                .withInvoiceDeliveryMethod(Constants.D_METHOD_EMAIL)
        		                .build();
        		logger.debug("User created, user id is : {}", spcUserWS.getId());
        		
            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_1));
                UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_1));
                assertEquals("Invoice delivery method should be Email & Paper", Constants.D_METHOD_EMAIL_AND_PAPER, user.getInvoiceDeliveryMethodId());
            });        	
        } finally {
        	clearTestDataForUser(spcUserWS.getId());
        	spcUserWS = null;
        }
    }

    /**
     * New customer with invoice delivery method as Email and Email Id field is populated
     */
    @Test(enabled = true, priority = 2)
    public void testNewCustomerInvoiceDeliveryMethodIsEmailAndEmailIdFieldIsPopulated() {
        try {
        	testBuilder.given(envBuilder -> {
                
        		spcUserWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_2)

        		        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
        		                .withNextInvoiceDate(new Date()).withCurrency(CURRENCY_AUD)
        		                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
        		                .withPaymentInstruments(getInstruments(new String[] {CC})).withMetaField(ENUMERATION_CUSTOMER_TYPE, CUSTOMER_TYPE_VALUE_PRE_PAID)
        		                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, getBillingAddress())
        		                .withAITGroup(CUSTOMER_DETAILS, getCustomerDetails(true))
        		                .withInvoiceDesign("")
        		                .withInvoiceDeliveryMethod(Constants.D_METHOD_EMAIL)
        		                .build();
        		
        		logger.debug("User created, user id is : {}", spcUserWS.getId());

            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_2));
                UserWS user2 = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_2));
                assertEquals("Invoice delivery method should be Email", Constants.D_METHOD_EMAIL, user2.getInvoiceDeliveryMethodId());
            });        	
        } finally {
        	clearTestDataForUser(spcUserWS.getId());
        	spcUserWS = null;
        }
    }
    
    /**
     * New customer with invoice delivery method as Paper and Email Id field is populated
     */
    @Test(enabled = true, priority = 3)
    public void testNewCustomerInvoiceDeliveryMethodIsPaperAndEmailIdFieldIsPopulated() {
        try {
        	testBuilder.given(envBuilder -> {
                
        		spcUserWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_3)

        		        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
        		                .withNextInvoiceDate(new Date()).withCurrency(CURRENCY_AUD)
        		                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
        		                .withPaymentInstruments(getInstruments(new String[] {CC})).withMetaField(ENUMERATION_CUSTOMER_TYPE, CUSTOMER_TYPE_VALUE_PRE_PAID)
        		                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, getBillingAddress())
        		                .withAITGroup(CUSTOMER_DETAILS, getCustomerDetails(true))
        		                .withInvoiceDesign("")
        		                .withInvoiceDeliveryMethod(Constants.D_METHOD_PAPER)
        		                .build();
        		
        		logger.debug("User created, user id is : {}", spcUserWS.getId());

            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_3));
                UserWS user2 = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_3));
                assertEquals("Invoice delivery method should be Paper", Constants.D_METHOD_PAPER, user2.getInvoiceDeliveryMethodId());
            });        	
        } finally {
        	clearTestDataForUser(spcUserWS.getId());
        	spcUserWS = null;
        }
    }

    /**
     * New customer with invoice delivery method as Paper and Email Id field is blank
     */
    @Test(enabled = true, priority = 4)
    public void testNewCustomerInvoiceDeliveryMethodIsPaperAndEmailIdFieldIsBlank() {
        try {
        	testBuilder.given(envBuilder -> {
                
        		spcUserWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_4)

        		        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
        		                .withNextInvoiceDate(new Date()).withCurrency(CURRENCY_AUD)
        		                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
        		                .withPaymentInstruments(getInstruments(new String[] {CC})).withMetaField(ENUMERATION_CUSTOMER_TYPE, CUSTOMER_TYPE_VALUE_PRE_PAID)
        		                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, getBillingAddress())
        		                .withAITGroup(CUSTOMER_DETAILS, getCustomerDetails(true))
        		                .withInvoiceDesign("")
        		                .withInvoiceDeliveryMethod(Constants.D_METHOD_PAPER)
        		                .build();
        		
        		logger.debug("User created, user id is : {}", spcUserWS.getId());

            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_4));
                UserWS user2 = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_4));
                assertEquals("Invoice delivery method should be Paper", Constants.D_METHOD_PAPER, user2.getInvoiceDeliveryMethodId());
            });        	
        } finally {
        	clearTestDataForUser(spcUserWS.getId());
        	spcUserWS = null;
        }
    }
    
    /**
     * Update customer with invoice delivery method as Email and Email Id field blank
     */
    @Test(enabled = true, priority = 5)
    public void testUpdateCustomerWithInvoiceDeliveryMethodAsEmailAndEmailIdFieldIsBlank() {
        try {
        	testBuilder.given(envBuilder -> {
                
        		spcUserWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_5)

        		        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
        		                .withNextInvoiceDate(new Date()).withCurrency(CURRENCY_AUD)
        		                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
        		                .withPaymentInstruments(getInstruments(new String[] {CC})).withMetaField(ENUMERATION_CUSTOMER_TYPE, CUSTOMER_TYPE_VALUE_PRE_PAID)
        		                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, getBillingAddress())
        		                .withAITGroup(CUSTOMER_DETAILS, getCustomerDetails(false))
        		                .withInvoiceDesign("")
        		                .withInvoiceDeliveryMethod(Constants.D_METHOD_EMAIL)
        		                .build();
        		
        		logger.debug("User created, user id is : {}", spcUserWS.getId());

            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_5));
                UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_5));
                assertEquals("Invoice delivery method should be Email", Constants.D_METHOD_EMAIL_AND_PAPER, user.getInvoiceDeliveryMethodId());
            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_5));
                UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_5));
                user.setInvoiceDeliveryMethodId(Constants.D_METHOD_PAPER);
                api.updateUser(user);
                assertEquals("Invoice delivery method should be Paper", Constants.D_METHOD_PAPER, user.getInvoiceDeliveryMethodId());
            });        	
        } finally {
        	clearTestDataForUser(spcUserWS.getId());
        	spcUserWS = null;
        }
    }
    
    /**
     * Update customer with invoice delivery method as Email and populated Email Id field
     */
    @Test(enabled = true, priority = 6)
    public void testUpdateCustomerWithInvoiceDeliveryMethodAsEmailAndEmailIdFieldIsPopulated() {
        try {
        	testBuilder.given(envBuilder -> {
                
        		spcUserWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_6)

        		        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
        		                .withNextInvoiceDate(new Date()).withCurrency(CURRENCY_AUD)
        		                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
        		                .withPaymentInstruments(getInstruments(new String[] {CC})).withMetaField(ENUMERATION_CUSTOMER_TYPE, CUSTOMER_TYPE_VALUE_PRE_PAID)
        		                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, getBillingAddress())
        		                .withAITGroup(CUSTOMER_DETAILS, getCustomerDetails(true))
        		                .withInvoiceDesign("")
        		                .withInvoiceDeliveryMethod(Constants.D_METHOD_EMAIL)
        		                .build();
        		
        		logger.debug("User created, user id is : {}", spcUserWS.getId());

            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_6));
                UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_6));
                assertEquals("Invoice delivery method should be Email", Constants.D_METHOD_EMAIL, user.getInvoiceDeliveryMethodId());
            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_6));
                UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_6));
                user.setInvoiceDeliveryMethodId(Constants.D_METHOD_EMAIL_AND_PAPER);
                api.updateUser(user);
                assertEquals("Invoice delivery method should be Paper", Constants.D_METHOD_EMAIL_AND_PAPER, user.getInvoiceDeliveryMethodId());
            });        	
        } finally {
        	clearTestDataForUser(spcUserWS.getId());
        	spcUserWS = null;
        }
    }

    /**
     * New Customer invoice delivery method is Email and Email Id field is blank and 
     * preference Force to set Invoice delivery method as Email and Paper on the customer account If Email Address not provided 
     * value set zero
     */
    @Test(enabled = true, priority = 7)
    public void testNewCustomerInvoiceDeliveryMethodIsEmailAndEmailIdFieldIsBlankAndPreferenceValueSetToZero() {
        try {
        	testBuilder.given(envBuilder -> {
        		// Update preference value to zero
        		updatePreference(Constants.PREFERENCE_SET_INVOICE_DELIVERY_METHOD_TO_EMAIL_AND_PAPER_IF_EMAIL_ID_IS_NOT_PROVIDED, "0");
        		
        		spcUserWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_7)

        		        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
        		                .withNextInvoiceDate(new Date()).withCurrency(CURRENCY_AUD)
        		                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(new Date())))
        		                .withPaymentInstruments(getInstruments(new String[] {CC})).withMetaField(ENUMERATION_CUSTOMER_TYPE, CUSTOMER_TYPE_VALUE_PRE_PAID)
        		                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, getBillingAddress())
        		                .withAITGroup(CUSTOMER_DETAILS, getCustomerDetails(true))
        		                .withInvoiceDesign("")
        		                .withInvoiceDeliveryMethod(Constants.D_METHOD_EMAIL)
        		                .build();
        		logger.debug("User created, user id is : {}", spcUserWS.getId());
        		
            }).validate((testEnv, testEnvBuilder) -> {
            	api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            	assertNotNull(CUSTOMER_CREATION_FAILED, testEnvBuilder.idForCode(TEST_USER_7));
                UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_7));
                assertEquals("Invoice delivery method should be Email & Paper", Constants.D_METHOD_EMAIL, user.getInvoiceDeliveryMethodId());
            });        	
        } finally {
        	clearTestDataForUser(spcUserWS.getId());
        	spcUserWS = null;
        }
    }
    
    public Map<String, Object> getCustomerDetails(boolean addEmail) {
        Map<String, Object> fieldsSPCCustomerDetails = new Hashtable();
		fieldsSPCCustomerDetails.put("PO Box", "1234");
		fieldsSPCCustomerDetails.put("Country", "AU");
		fieldsSPCCustomerDetails.put("Post Code", "41100");
		fieldsSPCCustomerDetails.put("State", "WA");
		fieldsSPCCustomerDetails.put("Street Name", "Test Street");
		fieldsSPCCustomerDetails.put("City", "TestCity");
		fieldsSPCCustomerDetails.put("Street Number", "11");
		fieldsSPCCustomerDetails.put("Title", "Mr");
		fieldsSPCCustomerDetails.put("First Name", "Test");
		fieldsSPCCustomerDetails.put("Last Name", "Customer");
		fieldsSPCCustomerDetails.put("Business Name", "TATA");
		fieldsSPCCustomerDetails.put("Date of Birth", "30-11-1989");
		fieldsSPCCustomerDetails.put("Contact Number", "9595959595");
		fieldsSPCCustomerDetails.put("direct_marketing", "Yes");
		if (addEmail) {
			fieldsSPCCustomerDetails.put("Email Address", "test@test.com");
		}
		return fieldsSPCCustomerDetails;
    }

    public Map<String, Object> getBillingAddress() {
        Map<String, Object> fieldsSPCBillingAddress = new Hashtable();
        fieldsSPCBillingAddress.put("PO Box", "12356");
        fieldsSPCBillingAddress.put("Country", "AU");
        fieldsSPCBillingAddress.put("Sub Premises", "TestSub");
        fieldsSPCBillingAddress.put("Street Number", "12335");
        fieldsSPCBillingAddress.put("Street Name", "Old Street");
        fieldsSPCBillingAddress.put("Street Type", "TestType");
        fieldsSPCBillingAddress.put("City", "Sydney");
        fieldsSPCBillingAddress.put("State", "WA");
        fieldsSPCBillingAddress.put("Post Code", "1231");
        return fieldsSPCBillingAddress;
    }
}
