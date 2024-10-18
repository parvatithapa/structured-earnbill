package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 31-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "PaymentMethodTemplateRestTest")
public class PaymentMethodTemplateRestTest extends RestTestCase{

    @BeforeClass
    public void setup(){
        super.setup("paymentMethodTemplates");
    }

    @Test
    public void getPaymentMethodTemplate(){

        ResponseEntity<PaymentMethodTemplateWS> fetchedResponse = restTemplate.sendRequest(REST_URL + Integer.valueOf(1), HttpMethod.GET,
                getOrDeleteHeaders, null, PaymentMethodTemplateWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        PaymentMethodTemplateWS fetchedTemplate = fetchedResponse.getBody();
        assertEquals(fetchedTemplate.getTemplateName(), "Payment Card", "Invalid template name!!");
    }

    @Test
    public void getPaymentMethodTemplateThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET,
                    getOrDeleteHeaders, null, PaymentMethodTemplateWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

}
