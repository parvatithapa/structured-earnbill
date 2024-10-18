package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessRunWS;
import com.sapienter.jbilling.server.user.partner.CommissionWS;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.testng.Assert.*;

/**
 */
public class CommissionRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);
    private static final Logger logger = LoggerFactory.getLogger(CommissionRestTest.class);

    @BeforeClass
    public void setup(){
        super.setup("commissions");
    }


    @Test
    public void createUpdateCommissionProcessConfiguration() {

        // Build an order status
        CommissionProcessConfigurationWS conf = CreateObjectUtil.createCommissionProcessConfig(ENTITY_ID, new Date(), 1, 1);

        ResponseEntity<Integer> postResponse = null;

        // Create the discount
        postResponse = restTemplate.sendRequest(REST_URL + "configuration",
                HttpMethod.POST, postOrPutHeaders, conf, Integer.class);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        logger.debug("Created CommissionProcessConfigurationWS with id " + postResponse.getBody());
        System.out.println("Created CommissionProcessConfigurationWS with id " + postResponse.getBody());

    }

    @Test(dependsOnMethods = "createUpdateCommissionProcessConfiguration")
    public void calculatePartnerCommissions() {
        // Build an order status
        ResponseEntity<DiscountWS> postResponse = null;

        // Create the discount
        postResponse = restTemplate.sendRequest(REST_URL + "processes?async=false",
                HttpMethod.POST, postOrPutHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<String> commissionsResponse = restTemplate.sendRequest(REST_URL + "processes/run",
                HttpMethod.GET, getOrDeleteHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());

    }

    /**
     * C37972611
     */
    @Test(dependsOnMethods = "calculatePartnerCommissions")
    public void getAllCommissionRuns() throws Exception {
        // Build an order status

        ResponseEntity<List<Map>> postResponse = null;

        postResponse = restTemplate.sendRequest(REST_URL + "processes",
                HttpMethod.GET, getOrDeleteHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());
        List<Map> runs = postResponse.getBody();
        assertTrue(runs.size() > 0, "There must be at least one run");

        ResponseEntity<List<CommissionWS>> commissionsResponse = restTemplate.sendRequest(REST_URL + "processes/" + runs.get(0).get("id"),
                HttpMethod.GET, getOrDeleteHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());

        try {
            restTemplate.sendRequest(REST_URL + "processes/99999999",
                    HttpMethod.GET, getOrDeleteHeaders, null);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C37972607
     */
    @Test(dependsOnMethods = "createUpdateCommissionProcessConfiguration")
    public void createConfigurationWithPastDate() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, -2);

        // Build an configuration with past run date
        CommissionProcessConfigurationWS conf = CreateObjectUtil.createCommissionProcessConfig(ENTITY_ID, cal.getTime(), 1, 1);
        ResponseEntity<Integer> postResponse = null;
        try {
            // Create the configuration
            postResponse = restTemplate.sendRequest(REST_URL + "configuration",
                    HttpMethod.POST, postOrPutHeaders, conf, Integer.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
            fail("Configuration shouldn't be created for past run");
        } catch (HttpStatusCodeException e) {
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("partner.error.commissionProcess.invalidDate"));
        }
    }

    /**
     * C37972608
     */
    @Test(dependsOnMethods = "createUpdateCommissionProcessConfiguration")
    public void createConfigurationWithFutureDate() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, 2);

        // Build an configuration with future run date
        CommissionProcessConfigurationWS conf = CreateObjectUtil.createCommissionProcessConfig(ENTITY_ID, cal.getTime(), 1, 1);
        ResponseEntity<Integer> postResponse = null;
        // Create the configuration
        postResponse = restTemplate.sendRequest(REST_URL + "configuration",
                HttpMethod.POST, postOrPutHeaders, conf, Integer.class);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        // Run the configuration
        postResponse = restTemplate.sendRequest(REST_URL + "processes?async=false",
                HttpMethod.POST, postOrPutHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());

        restTemplate.sendRequest(REST_URL + "processes/run",
                HttpMethod.GET, getOrDeleteHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());
    }

    /**
     * C37972609
     */
    @Test
    public void createConfigurationWithInvalidPeriodId() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, 4);
        try {
            // Build an configuration with invalid periodId
            CommissionProcessConfigurationWS conf = CreateObjectUtil.createCommissionProcessConfig(ENTITY_ID, cal.getTime(), 9, 1);
            ResponseEntity<Integer> postResponse = null;

            postResponse = restTemplate.sendRequest(REST_URL + "configuration",
                    HttpMethod.POST, postOrPutHeaders, conf, Integer.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            postResponse = restTemplate.sendRequest(REST_URL + "processes?async=false",
                    HttpMethod.POST, postOrPutHeaders, null);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());

            restTemplate.sendRequest(REST_URL + "processes/run",
                    HttpMethod.GET, getOrDeleteHeaders, null);
            fail("This configuration should not run with period Unit as 9");
        } catch (HttpServerErrorException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    /**
     * C37972610
     */
    @Test
    public void createConfigurationWithInvalidPeriodValue() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, 4);
        try {
            // Build an configuration with invalid periodValue
            CommissionProcessConfigurationWS conf = CreateObjectUtil.createCommissionProcessConfig(ENTITY_ID, cal.getTime(), 1, -1);
            ResponseEntity<Integer> postResponse = null;

            postResponse = restTemplate.sendRequest(REST_URL + "configuration",
                    HttpMethod.POST, postOrPutHeaders, conf, Integer.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            fail("This configuration should not run with period Value -1");
        } catch (HttpStatusCodeException e) {
            String errorMsg = e.getResponseBodyAsString();
            assertTrue(errorMsg.contains("partner.error.commissionProcess.invalidPeriodValue.negative.or.zero"));
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }
}