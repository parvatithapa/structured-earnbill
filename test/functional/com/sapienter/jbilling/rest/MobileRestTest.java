package com.sapienter.jbilling.rest;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
@Test(groups =  { "test-earnbill", "earnbill" }, testName = "MobileRestTest")
public class MobileRestTest extends RestTestCase{
    private static final Integer INVOICE_ID = 70;
    private static final Integer USER_ID = 73;
    private static final Integer ORDER_ID= 2;
    private static final Integer PAYMENT_ID= 2;
    private static final Integer CURRENCY_ID= 2;
    private static final Integer ENTITY_ID= 1;

    @BeforeClass
    public void setup(){
        super.setup("mobile");
    }

    @Test
    public void testGetInvoiceById(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "invoice/" + INVOICE_ID, HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        Map fetchedInvoice = fetchedResponse.getBody();
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetUserById(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "user/" + USER_ID, HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetOrderById(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "order/" + ORDER_ID, HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetPaymentById(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "payment/" + PAYMENT_ID, HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetCurrencyById(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "currency/" + CURRENCY_ID, HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetCompanyById(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "company/" + ENTITY_ID, HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetInvoiceByUserId(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + USER_ID + "/invoices", HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetUnpaidInvoiceByUserId(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + USER_ID + "/invoices/unpaid", HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetOrdersByUserId(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "orders/" +USER_ID, HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetPaymentsByUserId(){
        ResponseEntity<Map> fetchedResponse = restTemplate.sendRequest(REST_URL + "users/" + USER_ID + "/payments/last", HttpMethod.GET,
                earnbillGetOrDeleteHeaders, null);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
    }

    @AfterClass
    public void tearDown(){
    }
}