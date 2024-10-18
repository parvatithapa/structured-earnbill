package com.sapienter.jbilling.rest;

import org.springframework.http.HttpHeaders;
import org.testng.annotations.Test;

/**
 * @author Vojislav Stanojevikj
 * @since 29-Oct-2016.
 */
@Test(groups = {"rest"})
abstract class RestTestCase {

    protected RestOperationsHelper restHelper;
    protected final JBillingRestTemplate restTemplate = JBillingRestTemplate.getInstance();
    protected HttpHeaders getOrDeleteHeaders;
    protected HttpHeaders postOrPutHeaders;
    protected HttpHeaders resellerGetOrDeleteHeaders;
    protected HttpHeaders resellerPostOrPutHeaders;
    protected String REST_URL;


    protected void setup(String restEntity){
        restHelper = RestOperationsHelper.getInstance(restEntity);
        getOrDeleteHeaders = RestOperationsHelper.appendHeaders(restHelper.getAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, false));
        postOrPutHeaders = RestOperationsHelper.appendHeaders(restHelper.getAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, true));
        resellerGetOrDeleteHeaders = RestOperationsHelper.appendHeaders(restHelper.getResellerAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, false));
        resellerPostOrPutHeaders = RestOperationsHelper.appendHeaders(restHelper.getResellerAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, true));
        REST_URL = restHelper.getFullRestUrl();
    }

}
