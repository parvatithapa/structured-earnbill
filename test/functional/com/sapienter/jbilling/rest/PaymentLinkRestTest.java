package com.sapienter.jbilling.rest;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 */
@Test(groups = {"rest"}, testName = "PaymentLinkRestTest")
public class PaymentLinkRestTest extends RestTestCase{

    private static final Integer PC_PAYMENT_METHOD_TEMPLATE_ID = Integer.valueOf(1);
    private static final Integer MORDOR_ID = Integer.valueOf(12);

    @BeforeClass
    public void setup(){
        super.setup("payments/-111111/invoices");

    }


    @Test
    public void createPaymentLink(){
        try {
            restTemplate.sendRequest(REST_URL+"-222222", HttpMethod.POST, postOrPutHeaders,
                    null);

            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void deletePaymentLink(){
        try {
            restTemplate.sendRequest(REST_URL+"-222222", HttpMethod.DELETE, postOrPutHeaders,
                    null);

            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void removeAllPaymentLinks(){
        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.DELETE, postOrPutHeaders,null);

            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }


}
