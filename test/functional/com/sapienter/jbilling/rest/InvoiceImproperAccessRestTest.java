package com.sapienter.jbilling.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.fail;

import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.testng.annotations.Test;

import com.sapienter.jbilling.rest.RestConfig;
import com.sapienter.jbilling.server.invoice.InvoiceWS;

/**
 * @author amey.pelapkar
 * @since 25th JUN 2021
 *
 */
@Test(groups = {"rest"}, testName = "InvoiceImproperAccessRestTest")
public class InvoiceImproperAccessRestTest extends BaseRestImproperAccessTest {

	private static final boolean ENABLED_TEST = true;
	private static final String CONTEXT_STRING = "invoices";
	
	private static final Integer INVOICE_ID_COMPANY_1_CUSTOMER_2 = Integer.valueOf(45);
	private static final Integer INVOICE_ID_COMPANY_1_CUSTOMER_53 = Integer.valueOf(55);
	private static final Integer INVOICE_ID_COMPANY_2_CUSTOMER_13 = Integer.valueOf(75);
	
	private static final Integer USER_ID_COMPANY1_CUSTOMER1= Integer.valueOf(2);
	private static final Integer USER_ID_COMPANY1_CUSTOMER2= Integer.valueOf(10750);
	private static final Integer USER_ID_COMPANY2_CUSTOMER1= Integer.valueOf(13);

    
	@Override
	@Test(enabled = ENABLED_TEST)
	public void testCreate() {
		
		// Login as admin : Cross Company, get invoice for another company  -- company2AdminApi	-- mordor;2		
		try {
			createinvoicewithdate(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }
		
		// Login as customer : get invoice for another company	-- company1Customer2Api		-- french-speaker;1			
		try {
			createinvoicewithdate(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }
		
		// Login as customer : get invoice of another customer in same company	-- company1Customer3Api	--	pendunsus1;1		
		try {
			createinvoicewithdate(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : get invoice for parent company	-- parent1Company3AdminApi	--	admin;3			
		try {
			createinvoicewithdate(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testRead() {
		
		// Login as admin : Cross Company, get invoice for another company  -- company2AdminApi	-- mordor;2		
		try {
        	getInvoiceWS(company2AdminApi, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }
		
		// Login as customer : get invoice for another company	-- company1Customer2Api		-- french-speaker;1			
		try {
        	getInvoiceWS(company1Customer2Api, INVOICE_ID_COMPANY_2_CUSTOMER_13);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }
		
		// Login as customer : get invoice of another customer in same company	-- company1Customer3Api	--	pendunsus1;1		
		try {
        	getInvoiceWS(company1Customer3Api, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : get invoice for parent company	-- parent1Company3AdminApi	--	admin;3			
		try {
        	getInvoiceWS(parent1Company3AdminApi, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testUpdate() {
		
	}

	@Override
	@Test(enabled = ENABLED_TEST)
	public void testDelete() {
		
		// Login as admin : Cross Company, delete invoice for another company  -- company2AdminApi	-- mordor;2		
		try {
        	deleteInvoiceWS(company2AdminApi, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }
		
		// Login as customer : delete invoice for another company	-- company1Customer2Api		-- french-speaker;1			
		try {
			deleteInvoiceWS(company1Customer2Api, INVOICE_ID_COMPANY_2_CUSTOMER_13);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }
		
		// Login as customer : delete invoice of another customer in same company	-- company1Customer3Api	--	pendunsus1;1		
		try {
			deleteInvoiceWS(company1Customer3Api, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : delete invoice for parent company	-- parent1Company3AdminApi	--	admin;3			
		try {
			deleteInvoiceWS(parent1Company3AdminApi, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
	}
	
	@Test(enabled = ENABLED_TEST)
	public void testGetPaperInvoicePdf() {
		
		// Login as admin : Cross Company, get invoice for another company  -- company2AdminApi	-- mordor;2		
		try {
			getPaperInvoicePdf(company2AdminApi, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }
		
		// Login as customer : get invoice for another company	-- company1Customer2Api		-- french-speaker;1			
		try {
			getPaperInvoicePdf(company1Customer2Api, INVOICE_ID_COMPANY_2_CUSTOMER_13);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }
		
		// Login as customer : get invoice of another customer in same company	-- company1Customer3Api	--	pendunsus1;1		
		try {
			getPaperInvoicePdf(company1Customer3Api, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){        	
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, 2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
		
		// Login as admin(child company) : get invoice for parent company	-- parent1Company3AdminApi	--	admin;3			
		try {
			getPaperInvoicePdf(parent1Company3AdminApi, INVOICE_ID_COMPANY_1_CUSTOMER_2);
        	fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, INVOICE_ID_COMPANY_1_CUSTOMER_2));
        } catch (RestClientResponseException responseError){
        	Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
	}
	
	
	private InvoiceWS getInvoiceWS(RestConfig restConfig,
			Integer invoiceId) {
    	ResponseEntity<InvoiceWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.toString(invoiceId)), HttpMethod.GET,
    			getAuthHeaders(restConfig, true, false), null, InvoiceWS.class);
        return response.getBody();
    }
	
	private InvoiceWS getPaperInvoicePdf(RestConfig restConfig,
			Integer invoiceId) {
		
		String path = "paperinvoicepdf/".concat(Integer.toString(invoiceId));
    	ResponseEntity<InvoiceWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, path), HttpMethod.GET,
    			getAuthHeaders(restConfig, true, false), null, InvoiceWS.class);
        return response.getBody();
    }
	
	private InvoiceWS createinvoicewithdate(RestConfig restConfig,
			Integer userId) {
		String path = "createinvoicewithdate".concat("?userId=").concat(userId.toString()).concat("&billingDate=25-06-2021&onlyRecurring=false");
    	ResponseEntity<InvoiceWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, path), HttpMethod.POST,
    			getAuthHeaders(restConfig, true, false), null, InvoiceWS.class);
        return response.getBody();
    }
	
	private void deleteInvoiceWS(RestConfig restConfig, Integer invoiceId) {
    	restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.valueOf(invoiceId).toString()), HttpMethod.DELETE, getAuthHeaders(restConfig, true, false), null, null);
    }
    
}
