package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.util.CurrencyWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;
@Test(groups =  { "test-earnbill", "earnbill" }, testName = "PermissionRestTest")
public class PermissionRestTest extends RestTestCase{
    private static final Integer INVOICE_ID = 70;

    @BeforeClass
    public void setup(){
    }

    @Test
    public void testWebApiPermission_web() {
        super.setup("currencies");
        ResponseEntity<CurrencyWS[]> currenciesList = restTemplate
                .sendRequest(REST_URL, HttpMethod.GET, webApiGetOrDeleteHeaders, null, CurrencyWS[].class);
        assertNotNull(currenciesList, "Currencies list can not be null!!");
        RestValidationHelper.validateStatusCode(currenciesList,
                Response.Status.OK.getStatusCode());
    }

    @Test
    public void testWebApiPermission_mobile(){
        try {
            super.setup("mobile");
            ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "invoice/" + INVOICE_ID, HttpMethod.GET,
                    webApiGetOrDeleteHeaders, null);
            fail("Data Fetch Without Permission");
        }catch(Exception e){
            assertEquals("403 Forbidden","403 Forbidden",e.getMessage());
        }
    }

    @Test
    public void testMobileApiPermission_web() {
        try {
            super.setup("currencies");
            ResponseEntity<CurrencyWS[]> currenciesList = restTemplate
                    .sendRequest(REST_URL, HttpMethod.GET, mobileApiGetOrDeleteHeaders, null, CurrencyWS[].class);
            fail("Data Fetch Without Permission");
        }catch(Exception e){
            assertEquals("403 Forbidden","403 Forbidden",e.getMessage());
         }
    }

    @Test
    public void testMobileApiPermission_mobile(){
            super.setup("mobile");
            ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "invoice/" + INVOICE_ID, HttpMethod.GET,
                    mobileApiGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

    }

    @Test
    public void testBothApiPermission_web() {
        super.setup("currencies");
        ResponseEntity<CurrencyWS[]> currenciesList = restTemplate
                .sendRequest(REST_URL, HttpMethod.GET, webApiGetOrDeleteHeaders, null, CurrencyWS[].class);
        assertNotNull(currenciesList, "Currencies list can not be null!!");
        RestValidationHelper.validateStatusCode(currenciesList,
                Response.Status.OK.getStatusCode());
    }

    @Test
    public void testBothApiPermission_mobile(){
        super.setup("mobile");
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "invoice/" + INVOICE_ID, HttpMethod.GET,
                bothApiGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

    }

    @Test
    public void testNoPermission_mobile(){
        try {
            super.setup("mobile");
            ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "invoice/" + INVOICE_ID, HttpMethod.GET,
                    noneApiGetOrDeleteHeaders, null);
            fail("Data Fetch Without Permission");
        }catch(Exception e){
            assertEquals("403 Forbidden","403 Forbidden",e.getMessage());
        }
    }

    @Test
    public void testNoPermission_web() {
        try {
            super.setup("currencies");
            ResponseEntity<CurrencyWS[]> currenciesList = restTemplate
                    .sendRequest(REST_URL, HttpMethod.GET, noneApiGetOrDeleteHeaders, null, CurrencyWS[].class);
            fail("Data Fetch Without Permission");
        }catch(Exception e){
            assertEquals("403 Forbidden","403 Forbidden",e.getMessage());
        }
    }

    @AfterClass
    public void tearDown(){
    }

}