package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;

import static org.testng.Assert.*;

/**
 */
@Test(groups = {"rest"}, testName = "PaymentActionRestTest")
public class PaymentActionRestTest extends RestTestCase{

    private static final Integer MORDOR_ID = Integer.valueOf(12);

    @BeforeClass
    public void setup(){
        super.setup("payments/invoices");
    }


    @Test(enabled = false)
    public void applyPayment(){
        try {
            restTemplate.sendRequest(REST_URL+"-11111/apply", HttpMethod.POST, postOrPutHeaders,
                    RestEntitiesHelper.buildPaymentMock(MORDOR_ID, new Date(), 1));

            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void payInvoice(){
        try {
            restTemplate.sendRequest(REST_URL+"-11111/pay", HttpMethod.POST, postOrPutHeaders,
                    null);

            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test(enabled = false)
    public void processPayment(){
        try {
            restTemplate.sendRequest(REST_URL+"-11111/process", HttpMethod.POST, postOrPutHeaders,
                    RestEntitiesHelper.buildPaymentMock(MORDOR_ID, new Date(), 1));

            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }


}
