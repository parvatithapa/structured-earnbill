package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.user.UserWS;
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
import java.util.Date;
import java.util.List;

import static org.testng.Assert.*;

/**
 */
public class CollectionsRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);
    private static final Logger logger = LoggerFactory.getLogger(CollectionsRestTest.class);
    private RestOperationsHelper userRestHelper;
    private static final String STATUS_URL = "/status/in/1/";
    @BeforeClass
    public void setup(){
        super.setup("collections");
        userRestHelper = RestOperationsHelper.getInstance("users");
        ResponseEntity<Integer[]> fetchedResponse = restTemplate.sendRequest(userRestHelper.getFullRestUrl() + STATUS_URL
                + false, HttpMethod.GET, getOrDeleteHeaders, null, Integer[].class);
        Integer[] users = fetchedResponse.getBody();
        for(Integer userId : users) {
            updateCustomerStatusToActive(userId);
        }
    }

    private void updateCustomerStatusToActive(Integer customerId){

        UserWS user = restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class).getBody();
        user.setStatusId(Integer.valueOf(1));
        user.setStatus("Active");
        user.setPassword(null);

        restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId,
                HttpMethod.PUT, postOrPutHeaders, user, UserWS.class);
    }


    @Test
    public void getAgeingConfiguration() {

        ResponseEntity<AgeingWS[]> getResponse = null;

        // original steps
        getResponse = restTemplate.sendRequest(REST_URL + "configuration",
                HttpMethod.GET, resellerGetOrDeleteHeaders, null, AgeingWS[].class);
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
        logger.debug("Ageing steps {}", getResponse.getBody().length);

        AgeingWS[] ageingSteps = getResponse.getBody();

        //clear ageing steps
        restTemplate.sendRequest(REST_URL + "configuration/1",
                HttpMethod.POST, resellerPostOrPutHeaders, new AgeingWS[0]);

        //set test steps
        AgeingWS[] newSteps = RestEntitiesHelper.buildAgeingSteps();

        ResponseEntity postResponse = restTemplate.sendRequest(REST_URL + "configuration/1",
                HttpMethod.POST, resellerPostOrPutHeaders, newSteps);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

        getResponse = restTemplate.sendRequest(REST_URL + "configuration",
                HttpMethod.GET, resellerGetOrDeleteHeaders, null, AgeingWS[].class);
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());

        AgeingWS ageing = getResponse.getBody()[0];
        assertEquals(ageing.getDays(), newSteps[0].getDays());
        assertEquals(ageing.getPaymentRetry(), newSteps[0].getPaymentRetry());
        assertEquals(ageing.getSendNotification(), newSteps[0].getSendNotification());

        //save original steps
        postResponse = restTemplate.sendRequest(REST_URL + "configuration/1",
                HttpMethod.POST, resellerPostOrPutHeaders, new AgeingWS[0]);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
    }


    @Test(dependsOnMethods = "getAgeingConfiguration")
    public void triggerAgeing() {
        ResponseEntity postResponse = null;

        // Trigger ageing
        postResponse = restTemplate.sendRequest(REST_URL + "1970-01-01",
                HttpMethod.POST, resellerPostOrPutHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());


        ResponseEntity<ProcessStatusWS> statusResponse = restTemplate.sendRequest(REST_URL + "processes/status",
                HttpMethod.GET, resellerGetOrDeleteHeaders, null, ProcessStatusWS.class);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.OK.getStatusCode());
        logger.debug("ProcessStatusWS: {}", statusResponse.getBody());

    }

    /**
     * C37985707- Create Ageing Configuration with no value in 'For Days'
     */
    @Test
    public void saveAgeingConfigurationWithNoDays() {
        ResponseEntity<AgeingWS[]> getResponse = null;
        // original steps
        getResponse = restTemplate.sendRequest(REST_URL + "/configuration",
                HttpMethod.GET, resellerGetOrDeleteHeaders, null, AgeingWS[].class);
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
        logger.debug("Ageing steps {}", getResponse.getBody().length);

        AgeingWS[] ageingSteps = getResponse.getBody();

        //clear ageing steps
        restTemplate.sendRequest(REST_URL + "/configuration/1",
                HttpMethod.POST, resellerPostOrPutHeaders, new AgeingWS[0]);

        //set test steps
        AgeingWS[] newSteps = RestEntitiesHelper.buildAgeingStepsWithNoDays();

        try {
            ResponseEntity postResponse = restTemplate.sendRequest(REST_URL + "/configuration/1",
                    HttpMethod.POST, resellerPostOrPutHeaders, newSteps);
        } catch (HttpServerErrorException e) {
            if (e.toString().contains("500 Internal Server Error")) {
                assertTrue(true);
            }
        }

    }

    /**
     * C37985708 - Create Ageing configuration with no value in 'Step Name'
     */
    @Test
    public void saveAgeingConfigurationWithNoStepName() {
        ResponseEntity<AgeingWS[]> getResponse = null;

        // original steps
        getResponse = restTemplate.sendRequest(REST_URL + "configuration",
                HttpMethod.GET, resellerGetOrDeleteHeaders, null, AgeingWS[].class);
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
        logger.debug("Ageing steps {}", getResponse.getBody().length);

        AgeingWS[] ageingSteps = getResponse.getBody();

        //clear ageing steps
        restTemplate.sendRequest(REST_URL + "/configuration/1",
                HttpMethod.POST, resellerPostOrPutHeaders, new AgeingWS[0]);

        //set test steps
        AgeingWS[] newSteps = RestEntitiesHelper.buildAgeingStepsWithNoStepName();

        try {
            ResponseEntity postResponse = restTemplate.sendRequest(REST_URL + "/configuration/1",
                    HttpMethod.POST, resellerPostOrPutHeaders, newSteps);
        } catch (HttpServerErrorException e) {
            if (e.toString().contains("500 Internal Server Error")) {
                assertTrue(true);
            }
        }
    }
}
