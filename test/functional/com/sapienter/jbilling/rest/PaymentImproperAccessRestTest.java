package com.sapienter.jbilling.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.fail;

import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.testng.annotations.Test;

import com.sapienter.jbilling.rest.RestConfig;
import com.sapienter.jbilling.server.payment.PaymentWS;


/**
 * @author amey.pelapkar
 * @since 25th JUN 2021
 *
 */
@Test(groups = {"rest"}, testName = "PaymentImproperAccessRestTest")
public class PaymentImproperAccessRestTest extends BaseRestImproperAccessTest {

	private static final boolean ENABLED_TEST = true;
	private static final String CONTEXT_STRING = "payments";
	
	private static final Integer PAYMENT_ID_COMPANY_1_CUSTOMER_2 = Integer.valueOf(5);
	private static final Integer PAYMENT_ID_COMPANY_1_CUSTOMER_10750 = Integer.valueOf(1802);
	private static final Integer PAYMENT_ID_COMPANY_2_CUSTOMER_13 = Integer.valueOf(1801);
    
	@Override
	@Test(enabled = ENABLED_TEST)
	public void testCreate() {
		
		// Login as admin : Cross Company, create payment for another company  -- company2AdminApi	-- mordor;2
		try {
        	PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
        	paymentWS.setId(0);
        	createPaymentWS(company2AdminApi, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }		
		
		// Login as customer : create payment for another company	-- company1Customer2Api		-- french-speaker;1
		try {
        	PaymentWS paymentWS = getPaymentWS(company2AdminApi , PAYMENT_ID_COMPANY_2_CUSTOMER_13);
        	paymentWS.setId(0);
        	createPaymentWS(company1Customer2Api, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }		
		
		// Login as customer : create payment in same company	-- company1Customer3Api	--	pendunsus1;1
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			paymentWS.setId(0);
        	createPaymentWS(company1Customer3Api, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(INVALID_REFUND_AMOUNT));
        }
		
		// Login as admin(child company) : create payment for parent company	-- parent1Company3AdminApi	--	admin;3
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			paymentWS.setId(0);
        	createPaymentWS(parent1Company3AdminApi, paymentWS);
	        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
		   } catch (RestClientResponseException responseError){
		       	
		       	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
		   }		
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testRead() {
		
		// Login as admin : Cross Company, get payment for another company  -- company2AdminApi	-- mordor;2		
		try {
        	getPaymentWS(company2AdminApi, PAYMENT_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }
		
		// Login as customer : get payment for another company	-- company1Customer2Api		-- french-speaker;1			
		try {
        	getPaymentWS(company1Customer2Api, PAYMENT_ID_COMPANY_2_CUSTOMER_13);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }
		
		// Login as customer : get payment of another customer in same company	-- company1Customer3Api	--	pendunsus1;1		
		try {
        	getPaymentWS(company1Customer3Api, PAYMENT_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : get payment for parent company	-- parent1Company3AdminApi	--	admin;3			
		try {
        	getPaymentWS(parent1Company3AdminApi, PAYMENT_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testUpdate() {
		
		// Login as admin : Cross Company, update payment for another company  -- company2AdminApi	-- mordor;2
		try {
        	PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
        	updatePaymentWS(company2AdminApi, paymentWS);        	
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }		
		
		// Login as customer : update payment for another company	-- company1Customer2Api		-- french-speaker;1
		try {
        	PaymentWS paymentWS = getPaymentWS(company2AdminApi , PAYMENT_ID_COMPANY_2_CUSTOMER_13);
        	updatePaymentWS(company1Customer2Api, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }		
		
		// Login as customer : update payment in same company	-- company1Customer3Api	--	pendunsus1;1
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			updatePaymentWS(company1Customer3Api, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : update payment for parent company	-- parent1Company3AdminApi	--	admin;3
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			updatePaymentWS(parent1Company3AdminApi, paymentWS);
	        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
		   } catch (RestClientResponseException responseError){
		       	
		       	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
		   }		
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testDelete() {
		
		// Login as admin : Cross Company, delete payment for another company  -- company2AdminApi	-- mordor;2
		try {
        	PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
        	deletePaymentWS(company2AdminApi, paymentWS.getId());        	
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }		
		
		// Login as customer : delete payment for another company	-- company1Customer2Api		-- french-speaker;1
		try {
        	PaymentWS paymentWS = getPaymentWS(company2AdminApi , PAYMENT_ID_COMPANY_2_CUSTOMER_13);
        	deletePaymentWS(company1Customer2Api, paymentWS.getId());
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }		
		
		// Login as customer : delete payment in same company	-- company1Customer3Api	--	pendunsus1;1
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			deletePaymentWS(company1Customer3Api, paymentWS.getId());
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : delete payment for parent company	-- parent1Company3AdminApi	--	admin;3
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			deletePaymentWS(parent1Company3AdminApi, paymentWS.getId());
	        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
		   } catch (RestClientResponseException responseError){
			   Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
		   }		
	}
	
	@Test(enabled = ENABLED_TEST)
	public void testProcessPayment() {
		
		// Login as admin : Cross Company, create payment for another company  -- company2AdminApi	-- mordor;2
		try {
        	PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
        	paymentWS.setId(0);
        	processPaymentWS(company2AdminApi, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }		
		
		// Login as customer : create payment for another company	-- company1Customer2Api		-- french-speaker;1
		try {
        	PaymentWS paymentWS = getPaymentWS(company2AdminApi , PAYMENT_ID_COMPANY_2_CUSTOMER_13);
        	paymentWS.setId(0);
        	processPaymentWS(company1Customer2Api, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }		
		
		// Login as customer : create payment in same company	-- company1Customer3Api	--	pendunsus1;1
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			paymentWS.setId(0);
			processPaymentWS(company1Customer3Api, paymentWS);
        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(responseError.getResponseBodyAsString(), containsString(INVALID_REFUND_AMOUNT));
        }
		
		// Login as admin(child company) : create payment for parent company	-- parent1Company3AdminApi	--	admin;3
		try {
			PaymentWS paymentWS = getPaymentWS(company1AdminApi , PAYMENT_ID_COMPANY_1_CUSTOMER_2);
			paymentWS.setId(0);
			processPaymentWS(parent1Company3AdminApi, paymentWS);
	        	
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PAYMENT_ID_COMPANY_1_CUSTOMER_2));
		   } catch (RestClientResponseException responseError){
		       	
		       	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
		   }		
	}
	
	
	private PaymentWS getPaymentWS(RestConfig restConfig,
			Integer paymentId) {
    	ResponseEntity<PaymentWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.toString(paymentId)), HttpMethod.GET,
    			getAuthHeaders(restConfig, true, false), null, PaymentWS.class);
        return response.getBody();
    }
	
	
	private void createPaymentWS(RestConfig restConfig, PaymentWS paymentWS) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, null), HttpMethod.POST, getAuthHeaders(restConfig, true, false), paymentWS, Object.class);
    }
	
	private void processPaymentWS(RestConfig restConfig, PaymentWS paymentWS) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, "processpayment"), HttpMethod.POST, getAuthHeaders(restConfig, true, false), paymentWS, Object.class);
    }
	
	private void updatePaymentWS(RestConfig restConfig, PaymentWS paymentWS) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.valueOf(paymentWS.getId()).toString()), HttpMethod.PUT, getAuthHeaders(restConfig, true, false), paymentWS, Object.class);
    }
	
	
	private void deletePaymentWS(RestConfig restConfig, Integer paymentId) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.valueOf(paymentId).toString()), HttpMethod.DELETE, getAuthHeaders(restConfig, true, false), null, Object.class);
    }
	
	
}
