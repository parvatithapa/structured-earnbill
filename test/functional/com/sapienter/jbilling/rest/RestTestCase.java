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
    protected HttpHeaders earnbillGetOrDeleteHeaders;
    protected HttpHeaders earnbillPostOrPutHeaders;
    protected HttpHeaders mobileApiGetOrDeleteHeaders;
    protected HttpHeaders webApiGetOrDeleteHeaders;
    protected HttpHeaders bothApiGetOrDeleteHeaders;
    protected HttpHeaders noneApiGetOrDeleteHeaders;
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
        earnbillGetOrDeleteHeaders = RestOperationsHelper.appendHeaders(restHelper.getEarnbillAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, false));
        earnbillPostOrPutHeaders = RestOperationsHelper.appendHeaders(restHelper.getEarnbillAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, true));
        mobileApiGetOrDeleteHeaders = RestOperationsHelper.appendHeaders(restHelper.getMobileApiAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, false));
        webApiGetOrDeleteHeaders = RestOperationsHelper.appendHeaders(restHelper.getWebApiAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, false));
        bothApiGetOrDeleteHeaders = RestOperationsHelper.appendHeaders(restHelper.getBothApiAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, false));
        noneApiGetOrDeleteHeaders = RestOperationsHelper.appendHeaders(restHelper.getNoneApiAuthHeaders(),
                RestOperationsHelper.getJSONHeaders(true, false));
        REST_URL = restHelper.getFullRestUrl();
    }

}
