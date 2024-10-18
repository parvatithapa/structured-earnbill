package com.sapienter.jbilling.rest;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.rest.RestConfig;
import com.sapienter.jbilling.rest.TestCrudOperation;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.rest.JBillingRestTemplate;

/**
 * @author amey.pelapkar
 * @since 23rd JUN 2021
 *
 */
@Test(groups = {"rest"}, testName = "BaseRestImproperAccessTest")
public abstract class BaseRestImproperAccessTest implements TestCrudOperation {
	
	protected final JBillingRestTemplate restTemplate = JBillingRestTemplate.getInstance();
	
	private HttpHeaders authHeaders;
	
	public static final Integer ENTITY_ID_COMPANY_ONE = Integer.valueOf(1);
	
	public static final Integer ENTITY_ID_COMPANY_TWO = Integer.valueOf(2);
		
    protected RestConfig company1AdminApi;
    protected RestConfig company1Customer1Api;
    protected RestConfig company1Customer2Api;
    protected RestConfig company1Customer3Api;

    protected RestConfig parent1Company3AdminApi;
    protected RestConfig parent1Company3Customer1Api;

    protected RestConfig parent1Company10AdminApi;
    protected RestConfig parent1Company10Customer1Api;

    protected RestConfig company2AdminApi;
    protected RestConfig company2Customer1Api;

    protected RestConfig parent2Company11AdminApi;
    protected RestConfig parent2Company11Customer1Api;
    
    public static final String UNAUTHORIZED_ACCESS_TO_ID = "Unauthorized access to user ID %d";
    public static final String UNAUTHORIZED_ACCESS_TO_ENTITY = "Unauthorized access to entity ID %d";
    public static final String UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT = "Unauthorized access to payment instrument of user ID %d";
    
    public static final String INVALID_ERROR_MESSAGE = "Invalid error message!";
    protected static final String CROSS_CUSTOMER_ERROR_MSG = "Unauthorized access to entity %d for customer %d data by caller '%s'";
    protected static final String CROSS_COMPANY_ERROR_MSG = "Unauthorized access to entity %d by caller '%s'";
    protected static final String INVALID_USER_ERROR_MSG = "Please enter a valid user id";
    protected static final String INVALID_REFUND_AMOUNT = "PaymentWS,paymentId,validation.error.apply.without.payment.or.different.linked.payment.amount";
    
    protected static final String LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER = "french-speaker;1";
    protected static final String LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS = "pendunsus1;1";
    protected static final String LOGIN_USER_COMPANY2_ADMIN_MORDOR = "mordor;2";
    protected static final String LOGIN_USER_COMPANY1_ADMIN = "admin;1";
    
    protected static final String LOGIN_USER_PARENT1_COMPANY3_ADMIN = "admin;3";
    
    public static final String ERROR_404 = "404 Not Found";
	
	@BeforeClass
    protected void initialize() throws Exception {
        company1AdminApi = RemoteContext.getBean("restConfigCompany1Admin");
        company1Customer1Api = RemoteContext.getBean("restConfigCompany1Customer1");
        
        company1Customer2Api = RemoteContext.getBean("restConfigCompany1Customer2");
        company1Customer3Api = RemoteContext.getBean("restConfigCompany1Customer3");

        parent1Company3AdminApi = RemoteContext.getBean("restConfigParent1Company3Admin");
        parent1Company3Customer1Api = RemoteContext.getBean("restConfigParent1Company3Customer1");

        parent1Company10AdminApi = RemoteContext.getBean("restConfigParent1Company10Admin");
        parent1Company10Customer1Api = RemoteContext.getBean("restConfigParent1Company10Customer1");

        company2AdminApi = RemoteContext.getBean("restConfigCompany2Admin");
        company2Customer1Api = RemoteContext.getBean("restConfigCompany2Customer1");

        parent2Company11AdminApi = RemoteContext.getBean("restConfigParent2Company11Admin");
        parent2Company11Customer1Api = RemoteContext.getBean("restConfigParent2Company11Customer1");
    }

    @AfterClass
    protected void cleanup() {
        company1AdminApi = null;
        company1Customer1Api = null;
        company1Customer2Api = null;
        company1Customer3Api = null;

        parent1Company3AdminApi = null;
        parent1Company3Customer1Api = null;

        parent1Company10AdminApi = null;
        parent1Company10Customer1Api = null;

        company2AdminApi = null;
        company2Customer1Api = null;

        parent2Company11AdminApi = null;
        parent2Company11Customer1Api = null;
    }
    
    
    protected String getFullUrl(RestConfig restConfig, String context,  String path){    	
    	String fullPath = null;
    	if(path == null){
    		fullPath = restConfig.getRestUrl().concat(context).concat("/");
    	}else {
    		fullPath = restConfig.getRestUrl().concat(context).concat("/").concat(path);
    	}
    	//System.out.printf("%n#####################%n%s%n#####################%n", fullPath);
    	return fullPath;
    }
    
    protected void printResponse(RestClientResponseException responseError, String callerClass, String CallerMethod){
		System.out.printf("%n#####################%nCaller : %s.%s  %n%s%n#####################%n", callerClass, CallerMethod, responseError.getResponseBodyAsString());
		
	}
    
    public HttpHeaders getAuthHeaders(RestConfig restConfig, boolean accept, boolean contentType) {
		this.authHeaders =  createAuthHeaders(restConfig.getAuthUsername(), restConfig.getAuthPassword());
		return appendHeaders(getJSONHeaders(accept, contentType));
	}
    
    private static HttpHeaders getJSONHeaders(boolean accept, boolean contentType){
        HttpHeaders jsonHeaders = new HttpHeaders();
        if (accept){
        	jsonHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        }
        if (contentType){
        	jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        return HttpHeaders.readOnlyHttpHeaders(jsonHeaders);
    }

    private HttpHeaders appendHeaders(HttpHeaders requestHeaders){
        if (null == this.authHeaders){
            return null;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(this.authHeaders);
        if (null != requestHeaders && !requestHeaders.isEmpty()){
            httpHeaders.putAll(requestHeaders);
        }
        return HttpHeaders.readOnlyHttpHeaders(httpHeaders);
    }
	
	private HttpHeaders createAuthHeaders(String username, String password){
        return new HttpHeaders(){
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")));
                String authHeader = "Basic " + new String(encodedAuth);
                set("Authorization", authHeader);
            }
        };
    }
}