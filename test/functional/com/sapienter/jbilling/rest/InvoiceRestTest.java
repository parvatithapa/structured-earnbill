package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 07-Nov-2016.
 */
@Test(groups = {"rest"}, testName = "InvoiceRestTest")
public class InvoiceRestTest extends RestTestCase {

    private static final Integer GANDALF_INVOICE_ID = Integer.valueOf(1);
    private static final Integer GANDALF_ID = Integer.valueOf(2);

    @BeforeClass
    public void setup(){
        super.setup("invoices");
    }

    @Test(priority = 1)
    public void getInvoice(){

        ResponseEntity<InvoiceWS> fetchedResponse = restTemplate.sendRequest(REST_URL + GANDALF_INVOICE_ID, HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        InvoiceWS fetchedInvoice = fetchedResponse.getBody();
        assertNotNull(fetchedInvoice, "Invoice can not be null!");
        assertEquals(fetchedInvoice.getUserId(), GANDALF_ID, "User id invalid");
        assertEquals(fetchedInvoice.getTotalAsDecimal().setScale(4, BigDecimal.ROUND_CEILING),
                BigDecimal.valueOf(20).setScale(4, BigDecimal.ROUND_CEILING), "User id invalid");
        assertEquals(fetchedInvoice.getStatusDescr(), "Paid");
        assertEquals(fetchedInvoice.getStatusId(), CommonConstants.INVOICE_STATUS_PAID);
    }

    @Test(priority = 2)
    public void getInvoiceThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET,
                    getOrDeleteHeaders, null, InvoiceWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test(priority = 3)
    public void deleteInvoice(){

        ResponseEntity<InvoiceWS> deletedResponse = restTemplate.sendRequest(REST_URL + GANDALF_INVOICE_ID, HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        try {
            restTemplate.sendRequest(REST_URL + GANDALF_INVOICE_ID, HttpMethod.DELETE,
                    getOrDeleteHeaders, null, InvoiceWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test(priority = 4)
    public void deleteInvoiceThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE,
                    getOrDeleteHeaders, null, InvoiceWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
