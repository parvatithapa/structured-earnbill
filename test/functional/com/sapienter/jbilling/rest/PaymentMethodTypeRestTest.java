package com.sapienter.jbilling.rest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import javax.ws.rs.core.Response;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;

/**
 * @author Vojislav Stanojevikj
 * @since 31-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "PaymentMethodTypeRestTest")
public class PaymentMethodTypeRestTest extends RestTestCase {

    private static final Integer PC_PAYMENT_METHOD_TEMPLATE_ID = Integer.valueOf(1);

    private RestOperationsHelper paymentMethodTemplateRestHelper;
    private PaymentMethodTemplateWS PC_PAYMENT_METHOD_TEMPLATE;

    @BeforeClass
    public void setup(){
        super.setup("paymentMethodTypes");
        paymentMethodTemplateRestHelper = RestOperationsHelper.getInstance("paymentMethodTemplates");

        PC_PAYMENT_METHOD_TEMPLATE = restTemplate.sendRequest(paymentMethodTemplateRestHelper.getFullRestUrl() +
                PC_PAYMENT_METHOD_TEMPLATE_ID, HttpMethod.GET, getOrDeleteHeaders, null, PaymentMethodTemplateWS.class).getBody();
    }


    @Test
    public void postPaymentMethodType(){

        ResponseEntity<PaymentMethodTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE), PaymentMethodTypeWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        PaymentMethodTypeWS postedPaymentMethodType = postResponse.getBody();
        ResponseEntity<PaymentMethodTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedPaymentMethodType.getId(),
                HttpMethod.GET, getOrDeleteHeaders, null, PaymentMethodTypeWS.class);
        PaymentMethodTypeWS fetchedPaymentMethodType = fetchedResponse.getBody();

        assertEquals(fetchedPaymentMethodType, postedPaymentMethodType, "Payment method types do not match!");

        restTemplate.sendRequest(REST_URL + postedPaymentMethodType.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void postPaymentMethodTypeWithDuplicateMethodName(){

        PaymentMethodTypeWS paymentMethodType = RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE);

        ResponseEntity<PaymentMethodTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, paymentMethodType, PaymentMethodTypeWS.class);
        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, paymentMethodType, PaymentMethodTypeWS.class);
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("PaymentMethodTypeWS,methodName,validation.error.methodname.already.exists"));
        } finally {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }

    }

    @Test
    public void getPaymentMethodType(){

        ResponseEntity<PaymentMethodTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE), PaymentMethodTypeWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        PaymentMethodTypeWS postedPaymentMethodType = postResponse.getBody();
        ResponseEntity<PaymentMethodTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedPaymentMethodType.getId(),
                HttpMethod.GET, getOrDeleteHeaders, null, PaymentMethodTypeWS.class);
        PaymentMethodTypeWS fetchedPaymentMethodType = fetchedResponse.getBody();

        assertEquals(fetchedPaymentMethodType, postedPaymentMethodType, "Payment method types do not match!");

        restTemplate.sendRequest(REST_URL + postedPaymentMethodType.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getPaymentMethodTypeThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET,
                    getOrDeleteHeaders, null, PaymentMethodTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deletePaymentMethodType(){

        ResponseEntity<PaymentMethodTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE), PaymentMethodTypeWS.class);

        ResponseEntity<PaymentMethodTypeWS> deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        try {
            restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                    getOrDeleteHeaders, null, PaymentMethodTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deletePaymentMethodTypeThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updatePaymentMethodType(){

        ResponseEntity<PaymentMethodTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE), PaymentMethodTypeWS.class);

        PaymentMethodTypeWS updatedMock = postedResponse.getBody();
        updatedMock.setMethodName("UD-" + updatedMock.getMethodName());

        ResponseEntity<PaymentMethodTypeWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedMock, PaymentMethodTypeWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<PaymentMethodTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, PaymentMethodTypeWS.class);

        assertEquals(fetchedResponse.getBody().getId(), updatedMock.getId(), "Payment method type id do not match!");

        assertEquals(fetchedResponse.getBody().getMethodName(), updatedMock.getMethodName(),
                "Payment method types do not match!");

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void updatePaymentTypeThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE), PaymentMethodTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updatePaymentMethodTypeInvalid(){

        ResponseEntity<PaymentMethodTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE), PaymentMethodTypeWS.class);

        ResponseEntity<PaymentMethodTypeWS> postedResponse2 = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(PC_PAYMENT_METHOD_TEMPLATE), PaymentMethodTypeWS.class);

        PaymentMethodTypeWS updatedMock = postedResponse2.getBody();
        updatedMock.setMethodName(postedResponse.getBody().getMethodName());

        try {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                    postOrPutHeaders, updatedMock, PaymentMethodTypeWS.class);
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("PaymentMethodTypeWS,methodName,validation.error.methodname.already.exists"));
        } finally {
            restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            restTemplate.sendRequest(REST_URL + postedResponse2.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

}
