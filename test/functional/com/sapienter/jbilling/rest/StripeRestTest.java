package com.sapienter.jbilling.rest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInformationRestWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.SecurePaymentWS;
import com.sapienter.jbilling.server.user.UserWS;


/**
 * @author amey.pelapkar
 * Stripe, payment gateway, specific test cases  
 */
@Test(groups = {"rest"}, testName = "StripeRestTest")
public class StripeRestTest extends RestTestCase{
	
	private static final Integer BASIC_ACCOUNT_TYPE_ID = Integer.valueOf(1);
	private static final Integer AIT_CONTACT_ID = Integer.valueOf(1);
	
	private static final boolean ENABLED_ADD_INSTRUMENT = true;	
	private static final boolean ENABLED_PROCESS_PAYMENT = true;
	
	private static Integer methodTypeId = Integer.valueOf(1);
	private static Integer currencyId  = Integer.valueOf(1);
	
	/* 4242424242424242
	 * 3D Secure is supported for this card, but this card is not enrolled in 3D Secure.
	*/
	private static final String CARD_3DS_NOT_REQUIRED = "4242424242424242";
	
	/* 4000002500003155
	 * This card requires authentication for one-time payments. 
	 * However, if you set up this card and use the saved card for subsequent off-session payments, no further authentication is needed.
	*/
	private static final String CARD_3DS_REQUIRED = "4000002500003155";
	
	/* 4242424242424241
	 * Charge is declined with an incorrect_number code as the card number fails the Luhn check.
	*/
	private static final String CARD_INVALID = "4242424242424241";
	
	/* 4000000000009995
	 * Charge is declined with a card_declined code. The decline_code attribute is insufficient_funds
	*/	
	private static final String CARD_INSUFFICIENT_FUNDS = "4000000000009995";
	
	/* 4000000000009979
	 * Charge is declined with a card_declined code. The decline_code attribute is stolen_card..
	*/
	private static final String CARD_STOLEN = "4000000000009979";
	
    @BeforeClass
    public void setup(){
        super.setup(null);
    }

    @AfterClass()
    public void tearDown(){
    }
    
    @Test(enabled=ENABLED_ADD_INSTRUMENT)
    public void testAddPaymentInstrumentWith3DSNotRequiredCard(){
    	
    	ResponseEntity<SecurePaymentWS> responseInstrument = addPaymentInstrument(null, CARD_3DS_NOT_REQUIRED, "03/2029", 1);
    	
    	RestValidationHelper.validateStatusCode(responseInstrument, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = responseInstrument.getBody();
    	
    	assertSuccess(securePaymentWS,true);
   }
    

	@Test(enabled=ENABLED_ADD_INSTRUMENT)
    public void testAddPaymentInstrumentWith3DSRequiredCard(){
    	
    	ResponseEntity<SecurePaymentWS> responseInstrument = addPaymentInstrument(null, CARD_3DS_REQUIRED, "02/2030", 1);
    	
    	RestValidationHelper.validateStatusCode(responseInstrument, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = responseInstrument.getBody();
    	
    	assert3DSRequired(securePaymentWS, true);
   }

	@Test(enabled=ENABLED_ADD_INSTRUMENT)
    public void testAddPaymentInstrumentWithInvalidCardNumber(){
    	/* 0000002500003155
    	 * Invalid credit card number
    	 */
    	ResponseEntity<SecurePaymentWS> responseInstrument = addPaymentInstrument(null, CARD_INVALID, "04/2000", 1);
    	
    	RestValidationHelper.validateStatusCode(responseInstrument, Response.Status.PAYMENT_REQUIRED.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = responseInstrument.getBody();
    	
    	assertFail(securePaymentWS, "incorrect_number");
    	
   }
   
 
    @Test(enabled=ENABLED_ADD_INSTRUMENT)
    public void testAddPaymentInstrumentWithInvalidExpiryDate(){
	
    	/* 4000002500003155, Valid card number but invalid expire date
    	 * This card requires authentication for one-time payments. 
    	 * However, if you set up this card and use the saved card for subsequent off-session payments, no further authentication is needed.
    	*/
    	ResponseEntity<SecurePaymentWS> responseInstrument = addPaymentInstrument(null, CARD_3DS_REQUIRED, "09/2000", 1);
    	
    	RestValidationHelper.validateStatusCode(responseInstrument, Response.Status.PAYMENT_REQUIRED.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = responseInstrument.getBody();
    	
    	assertFail(securePaymentWS, "invalid_expiry_year");
   }
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessOneTimePaymentWith3DSNotRequiredCard(){
	
    	/* 4242424242424242
    	 * 3D Secure is supported for this card, but this card is not enrolled in 3D Secure.
    	*/
    	
    	UserWS newUserWS = createUser();
    	
        PaymentInformationWS paymentInformationWS = RestEntitiesHelper.buildCCPaymentInstrumentMock(methodTypeId, "OneTimePaymentWith3DSNotRequired",  CARD_3DS_NOT_REQUIRED, getDateAfterYears(20));
    	
    	PaymentWS paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), null, currencyId, BigDecimal.valueOf(3.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	ResponseEntity<SecurePaymentWS> response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, true);
   }
    
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessOneTimePaymentWith3DSRequiredCard(){
	
    	/* 4000002500003155
    	 * This card requires authentication for one-time payments. 
    	 * However, if you set up this card and use the saved card for subsequent off-session payments, no further authentication is needed.
    	*/
    	
    	UserWS newUserWS = createUser();
    	
        PaymentInformationWS paymentInformationWS = RestEntitiesHelper.buildCCPaymentInstrumentMock(methodTypeId, "ProcessOneTimePaymentWith3DSRequired",  CARD_3DS_REQUIRED, getDateAfterYears(20));
    	
    	PaymentWS paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), null, currencyId, BigDecimal.valueOf(3.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	ResponseEntity<SecurePaymentWS> response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assert3DSRequired(securePaymentWS, true);
   }
    
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessOffSessionPaymentWith3DSNotRequiredCard(){
    	
    	
    	/* 4242424242424242
    	 * 3D Secure is supported for this card, but this card is not enrolled in 3D Secure.
    	*/
    	ResponseEntity<SecurePaymentWS> response = addPaymentInstrument(null, CARD_3DS_NOT_REQUIRED, "01/2027", 1);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, false);
    	
    	PaymentWS paymentWS = createPayment(securePaymentWS.getUserId(), null, currencyId, BigDecimal.valueOf(54.42), Constants.PAYMENT_METHOD_VISA, null);
    	
    	response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, true);
   }    
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessOneTimePaymentWithInvalidCard(){

    	UserWS newUserWS = createUser();
    	
        PaymentInformationWS paymentInformationWS = RestEntitiesHelper.buildCCPaymentInstrumentMock(methodTypeId, "ProcessOneTimePaymentWithInvalid",  CARD_INVALID, getDateAfterYears(20));
    	
    	PaymentWS paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), null, currencyId, BigDecimal.valueOf(3.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	ResponseEntity<SecurePaymentWS> response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.PAYMENT_REQUIRED.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assertFail(securePaymentWS, "incorrect_number");
   }
    
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessOneTimePaymentWithInsufficientFunds(){

    	UserWS newUserWS = createUser();
    	
        PaymentInformationWS paymentInformationWS = RestEntitiesHelper.buildCCPaymentInstrumentMock(methodTypeId, "InsufficientFunds",  CARD_INSUFFICIENT_FUNDS, getDateAfterYears(20));
    	
    	PaymentWS paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), null, currencyId, BigDecimal.valueOf(3.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	ResponseEntity<SecurePaymentWS> response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.PAYMENT_REQUIRED.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assertFail(securePaymentWS, "card_declined");
   }
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessOneTimePaymentWithStolenCard(){

    	UserWS newUserWS = createUser();
    	
        PaymentInformationWS paymentInformationWS = RestEntitiesHelper.buildCCPaymentInstrumentMock(methodTypeId, "StolenCard",  CARD_STOLEN, getDateAfterYears(20));
    	
    	PaymentWS paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), null, currencyId, BigDecimal.valueOf(3.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	ResponseEntity<SecurePaymentWS> response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.PAYMENT_REQUIRED.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assertFail(securePaymentWS, "card_declined");
   }

    /* Process payment with mutiple cards.
     * Add 3DS auth require card
     * Add 3DS auth NOT require card
     * 
     * Result : Payment should be processed, 3DS auth require card requires customer to authenticate the payment hence card will not be attached to customer profile 
    */
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessPaymentWithMultipleCards(){
    	
    	ResponseEntity<SecurePaymentWS> response = addPaymentInstrument(null, CARD_3DS_REQUIRED, "08/2038", 1);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assert3DSRequired(securePaymentWS,false);
    	
    	
    	response = addPaymentInstrument(securePaymentWS.getUserId(), CARD_3DS_NOT_REQUIRED, "08/2025", 2);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS,false);
    	
    	
    	PaymentWS paymentWS = createPayment(securePaymentWS.getUserId(), null, currencyId, BigDecimal.valueOf(54.42), Constants.PAYMENT_METHOD_VISA, null);
    	
    	response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, true);
   }
    
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessRefundFull(){

    	UserWS newUserWS = createUser();
    	
        PaymentInformationWS paymentInformationWS = RestEntitiesHelper.buildCCPaymentInstrumentMock(methodTypeId, "ProcessOneTimePaymentWithInvalid",  CARD_3DS_NOT_REQUIRED, getDateAfterYears(20));
    	
    	PaymentWS paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), null, currencyId, BigDecimal.valueOf(13.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	ResponseEntity<SecurePaymentWS> response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, false);
    	
    	paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), securePaymentWS.getBillingHubRefId(), currencyId, BigDecimal.valueOf(13.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, true);
   }
    
    
    @Test(enabled=ENABLED_PROCESS_PAYMENT)
    public void testProcessRefundPartial(){

    	UserWS newUserWS = createUser();
    	
        PaymentInformationWS paymentInformationWS = RestEntitiesHelper.buildCCPaymentInstrumentMock(methodTypeId, "ProcessOneTimePaymentWithInvalid",  CARD_3DS_NOT_REQUIRED, getDateAfterYears(20));
    	
    	PaymentWS paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), null, currencyId, BigDecimal.valueOf(13.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	ResponseEntity<SecurePaymentWS> response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	SecurePaymentWS securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, false);
    	
    	
    	paymentWS = createPayment(Integer.valueOf(newUserWS.getUserId()), securePaymentWS.getBillingHubRefId(), currencyId, BigDecimal.valueOf(3.63), Constants.PAYMENT_METHOD_VISA, paymentInformationWS);
    	
    	response = processPayment(paymentWS);
    	
    	RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());
        
    	securePaymentWS = response.getBody();
    	
    	assertSuccess(securePaymentWS, true);
   }
    
    private void assert3DSRequired(SecurePaymentWS securePaymentWS, boolean deleteUser) {
    	
    	assertNotNull(securePaymentWS, "Response, SecurePaymentWS, should not be null");
    	
    	assertEquals(securePaymentWS.getNextAction(), securePaymentWS.getNextAction());
    	
    	assertEquals(securePaymentWS.getError(), null);
    	
    	assertEquals(securePaymentWS.getBillingHubRefId(), Integer.valueOf(0)); // assert Payment instrument id is null
    	
    	if(deleteUser){
	    	deleteUser(securePaymentWS.getUserId());
	    }
		
	}    
    
    private void assertSuccess(SecurePaymentWS securePaymentWS, boolean deleteUser) {
 	   
	   	assertNotNull(securePaymentWS, "Response, SecurePaymentWS, should not be null");
	   	
	   	assertEquals(securePaymentWS.getNextAction(), null); // assert next action is null
	   	
	   	assertEquals(securePaymentWS.getError(), null); // assert error is null
	   	
	    assertNotNull(securePaymentWS.getBillingHubRefId(), "Paymen intrument id should not be null"); // assert Payment instrument id is not null
	    
	    if(deleteUser){
	    	deleteUser(securePaymentWS.getUserId());
	    }
   }
    
   private void assertFail(SecurePaymentWS securePaymentWS , String expectedErrorCode){
	   
		assertNotNull(securePaymentWS, "Response, SecurePaymentWS, should not be null");
	   	
		assertEquals(securePaymentWS.getBillingHubRefId(), Integer.valueOf(0)); // assert Payment instrument id is null
		   	
		assertEquals(securePaymentWS.getNextAction(), null); // assert next action is null
		   	
		assertEquals(securePaymentWS.getStatus(), "failed");
		   	
		assertNotNull(securePaymentWS.getError(), "Response, error, should not be null");
		   	
		assertEquals(securePaymentWS.getError().getCode(), expectedErrorCode);
		
		deleteUser(securePaymentWS.getUserId());
   }
    
    
    /** Add payment instrument, generic method
     * 
     * @param cardNumber
     * @param expiryDate
     * @return
     */
    private ResponseEntity<SecurePaymentWS> addPaymentInstrument(Integer userId,  String cardNumber, String expiryDate, Integer processingOrder){
    	/*
		 * Creating dummy customer to attach payment method with his profile 
		*/
    	if(userId == null){
    		UserWS newUserWS = createUser();
    		userId = newUserWS.getUserId();
    	}
    	
        PaymentInformationRestWS instrumentWS = RestEntitiesHelper.buildPaymentInformationWSMock(userId, 1, cardNumber, expiryDate, null);
        
        instrumentWS.setProcessingOrder(processingOrder);
        ResponseEntity<SecurePaymentWS> responseInstrument = null; 
        try{
        	responseInstrument = restTemplate.sendRequest(restHelper.getFullRestUrl()+"users/addpaymentinstrument", HttpMethod.POST,
                postOrPutHeaders, instrumentWS, SecurePaymentWS.class);
        
        	assertNotNull(responseInstrument, "Response, SecurePaymentWS, should not be null");        
        
        }catch(org.springframework.web.client.HttpClientErrorException exp){
        	try{
	        	ObjectMapper mapper = new ObjectMapper();
	        	SecurePaymentWS securePaymentWS =   mapper.readValue(exp.getResponseBodyAsString(), SecurePaymentWS.class) ;
	        	
	        	responseInstrument = new ResponseEntity(securePaymentWS, exp.getStatusCode());
	        	
	        	System.out.print(exp.getResponseBodyAsString());
	        }catch(Exception ex){
	        	ex.printStackTrace();
			}
        }
        return responseInstrument;
    }
    
    
    private ResponseEntity<SecurePaymentWS> processPayment(PaymentWS paymentWS ){
    	ResponseEntity<SecurePaymentWS> responseProcessPayment = null; 
    	
    	try{
    		
    		responseProcessPayment = restTemplate.sendRequest(restHelper.getFullRestUrl()+"payments/processpayment", HttpMethod.POST, postOrPutHeaders, paymentWS, SecurePaymentWS.class);        
        	assertNotNull(responseProcessPayment, "Response, SecurePaymentWS, should not be null");
        	
    	}catch(org.springframework.web.client.HttpClientErrorException exp){
    		try{
	        	ObjectMapper mapper = new ObjectMapper();    		
	    		SecurePaymentWS securePaymentWS =   mapper.readValue(exp.getResponseBodyAsString(), SecurePaymentWS.class);
	        	responseProcessPayment = new ResponseEntity(securePaymentWS, exp.getStatusCode());
	        	System.out.print(exp.getResponseBodyAsString());
    		}catch(Exception ex){
    			ex.printStackTrace();
    		}
        }
        return responseProcessPayment;
    } 
    /*
     * Get future year date
    */
    private static Date getDateAfterYears(int years) {
    	Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.YEAR, years); // +years
    	return cal.getTime();
    	}
    
    private PaymentWS createPayment(Integer userId, Integer paymentIdToRefund, Integer currencyId, BigDecimal amount, Integer paymentMethodId, PaymentInformationWS paymentInstrument){
    	PaymentWS paymentWS = new PaymentWS();
    	
    	paymentWS.setUserId(userId);
    	paymentWS.setIsRefund(paymentIdToRefund == null ? 0 : 1);
    	paymentWS.setPaymentId(paymentIdToRefund);
    	paymentWS.setPaymentDate(Calendar.getInstance().getTime());
    	paymentWS.setCurrencyId(currencyId);
    	paymentWS.setAmount(amount);
    	paymentWS.setMethodId(paymentMethodId);
    	
    	if(paymentInstrument!=null){
    		List<PaymentInformationWS> paymentInstruments = new ArrayList<PaymentInformationWS>();
    		paymentInstruments.add(paymentInstrument);
    		
    		paymentWS.setPaymentInstruments(paymentInstruments);
    	}
    	
    	return paymentWS;
    }
    
    /** Generic method to create a test user
     * @return
     */
    private UserWS createUser(){
		String dummyEmailAddress = "testCaseStripe".concat(""+Calendar.getInstance().getTimeInMillis()).concat("@gmail.com");
		UserWS userWS = RestEntitiesHelper.buildUserMock("testCaseStripe", BASIC_ACCOUNT_TYPE_ID, true);
		
		setContactDetails(userWS, dummyEmailAddress);
		
		ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(restHelper.getFullRestUrl()+"users", HttpMethod.POST, postOrPutHeaders, userWS, UserWS.class);
		
        assertNotNull(postResponse, "User can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        return postResponse.getBody();
    }
    
    private void deleteUser(Integer userId){
		ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(restHelper.getFullRestUrl()+"users/"+userId, HttpMethod.DELETE, getOrDeleteHeaders, userId, null);
		assertNotNull(postResponse, "Response should not be null.");
        RestValidationHelper.validateStatusCode(postResponse, 204);
    }
    
    private void setContactDetails(UserWS userWS, String dummyEmailAddress){
    	setAITMetaFields(AIT_CONTACT_ID, userWS, new Date(), new String[]{"contact.email"
			, "contact.address1"
			, "contact.address2"
			, "contact.city"
			, "contact.state.province"
			, "contact.postal.code"
			, "contact.country.code"
			, "contact.first.name"
			, "contact.last.name" }
    	, new String[]{dummyEmailAddress
			, "777"
			, "Brockton Avenue"
			, "Abington"
			, "MA"
			, "2351"
			, "US"
			, "Stripe first"
			, "Stripe last" 
    	});
    }
    
    private void setAITMetaFields(Integer aitId, UserWS user, Date date, String metaFieldName[], String metaFieldValue[]){
        Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> aitMetaFields = new HashMap<>();

        HashMap<Date, ArrayList<MetaFieldValueWS>> timeLineMetaFields = new HashMap<>();
        ArrayList<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
        
        for(int i=0; i < metaFieldValue.length; i++){
        	MetaFieldValueWS metaFieldValueWS = RestEntitiesHelper.buildMetaFieldValue(metaFieldName[i], metaFieldValue[i], aitId);
            metaFieldValues.add(metaFieldValueWS);
        }
        
        user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[]{}));
        timeLineMetaFields.put(date, metaFieldValues);
        aitMetaFields.put(aitId, timeLineMetaFields);
        user.setAccountInfoTypeFieldsMap(aitMetaFields);
        ArrayList<Date> dates = new ArrayList<>();
        dates.add(date);
        Map<Integer, ArrayList<Date>> datesMap = new HashMap<>();
        datesMap.put(aitId, dates);
        user.setTimelineDatesMap(datesMap);
        Map<Integer, Date> effectiveDateMap = new HashMap<>();
        effectiveDateMap.put(aitId, date);
        user.setEffectiveDateMap(effectiveDateMap);
    }
}
