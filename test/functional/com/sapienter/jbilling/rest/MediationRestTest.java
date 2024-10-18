package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 */
public class MediationRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);
    private static final Logger logger = LoggerFactory.getLogger(MediationRestTest.class);

    @BeforeClass
    public void setup(){
        super.setup("mediation");
    }

    @Test
    public void testSearchEvents() {
        String processId = "173b583e-347c-4037-b59d-b10873685056";
        logger.debug("Events for processId 1");
        ResponseEntity<JbillingMediationRecord[]> processResponse = null;


        processResponse = restTemplate.sendRequest(REST_URL + "/process/"+processId+"/events?offset=0&limit=5",
                HttpMethod.GET, getOrDeleteHeaders, null, JbillingMediationRecord[].class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());
        assertEquals(2, processResponse.getBody().length);

        logger.debug("Events for processId 2");
        processResponse = restTemplate.sendRequest(REST_URL + "/process/"+processId+"/events?offset=0&limit=1",
                HttpMethod.GET, getOrDeleteHeaders, null, JbillingMediationRecord[].class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());
        assertEquals(1, processResponse.getBody().length);

        logger.debug("Events for processId 3");
        processResponse = restTemplate.sendRequest(REST_URL + "/process/"+processId+"/events?offset=1&limit=3",
                HttpMethod.GET, getOrDeleteHeaders, null, JbillingMediationRecord[].class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());
        assertEquals(1, processResponse.getBody().length);



        logger.debug("Events for order 1");
        processResponse = restTemplate.sendRequest(REST_URL + "/process/events/order/4",
                HttpMethod.GET, getOrDeleteHeaders, null, JbillingMediationRecord[].class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());
        assertEquals(2, processResponse.getBody().length);



        logger.debug("Events for invoice 1");
        processResponse = restTemplate.sendRequest(REST_URL + "/process/events/invoice/5",
                HttpMethod.GET, getOrDeleteHeaders, null, JbillingMediationRecord[].class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());
        assertEquals(2, processResponse.getBody().length);


        logger.debug("Event search for order 1");
        processResponse = restTemplate.sendRequest(REST_URL + "/process/eventSearch/4?offset=1&limit=3",
                HttpMethod.GET, getOrDeleteHeaders, null, JbillingMediationRecord[].class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());
        assertEquals(1, processResponse.getBody().length);

        logger.debug("Event search for order 2");
        processResponse = restTemplate.sendRequest(REST_URL + "/process/eventSearch/4?offset=0&limit=3",
                HttpMethod.GET, getOrDeleteHeaders, null, JbillingMediationRecord[].class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());
        assertEquals(2, processResponse.getBody().length);
    }


    @Test
    public void testProcess() {

        ResponseEntity<MediationConfigurationWS> createResponse = null;
        MediationConfigurationWS conf = RestEntitiesHelper.buildMediationConfiguration();
        createResponse = restTemplate.sendRequest(REST_URL + "/configuration",
                HttpMethod.POST, postOrPutHeaders, conf, MediationConfigurationWS.class);
        RestValidationHelper.validateStatusCode(createResponse, Response.Status.CREATED.getStatusCode());
        conf = createResponse.getBody();


        //trigger mediation
        ResponseEntity triggerResponse = null;
        triggerResponse = restTemplate.sendRequest(REST_URL + "/process/configuration/"+conf.getId(),
                HttpMethod.POST, postOrPutHeaders, null);
        System.out.println("Headers: " + triggerResponse.getHeaders());
        RestValidationHelper.validateStatusCode(triggerResponse, Response.Status.CREATED.getStatusCode());
        String location = triggerResponse.getHeaders().getLocation().toString();
        assertTrue(location.contains("/api/mediation/process/"));
        String uuid = location.substring(location.lastIndexOf('/')+1);


        //get process
        ResponseEntity<MediationProcess> processResponse = null;
        processResponse = restTemplate.sendRequest(REST_URL + "/process/"+uuid,
                HttpMethod.GET, getOrDeleteHeaders, null, MediationProcess.class);
        RestValidationHelper.validateStatusCode(processResponse, Response.Status.OK.getStatusCode());


        //get all process
        ResponseEntity<MediationProcess[]> processesResponse = null;
        processesResponse = restTemplate.sendRequest(REST_URL + "/process",
                HttpMethod.GET, getOrDeleteHeaders, null, MediationProcess[].class);
        RestValidationHelper.validateStatusCode(processesResponse, Response.Status.OK.getStatusCode());
        assertTrue(processesResponse.getBody().length > 0);

        //undo mediation
        ResponseEntity postResponse = restTemplate.sendRequest(REST_URL + "/process/"+uuid,
                HttpMethod.DELETE, postOrPutHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.NO_CONTENT.getStatusCode());

        //delete configuration
        postResponse = restTemplate.sendRequest(REST_URL + "/configuration/"+conf.getId(),
                HttpMethod.DELETE, postOrPutHeaders, null);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.NO_CONTENT.getStatusCode());
    }

    /**
     * C37929591
     */
    @Test
    public void createMediationWithNoName() {
        MediationConfigurationWS conf = RestEntitiesHelper.buildMediationConfiguration();
        conf.setName("");
        try {
            restTemplate.sendRequest(REST_URL + "/configuration",
                    HttpMethod.POST, postOrPutHeaders, conf, MediationConfigurationWS.class);
            fail("No no");
        }catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertTrue(errorMsg.contains("MediationConfigurationWS,name,validation.error.notnull"));
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    /**
     * C37929592
     */
    @Test
    public void createMediationWithNoOrderId() {
        MediationConfigurationWS conf = RestEntitiesHelper.buildMediationConfiguration();
        conf.setOrderValue("");
        try {
            restTemplate.sendRequest(REST_URL + "/configuration",
                    HttpMethod.POST, postOrPutHeaders, conf, MediationConfigurationWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    /**
     * C37929593
     */
    @Test
    public void createMediationConfigurationAsGlobal() {
        MediationConfigurationWS conf = RestEntitiesHelper.buildMediationConfiguration();
        conf.setGlobal(true);
        ResponseEntity<MediationConfigurationWS> createResponse = null;
        try {
            createResponse = restTemplate.sendRequest(REST_URL + "/configuration",
                    HttpMethod.POST, postOrPutHeaders, conf, MediationConfigurationWS.class);
            RestValidationHelper.validateStatusCode(createResponse, Response.Status.CREATED.getStatusCode());
            assertTrue(createResponse.getBody().getGlobal(), "Mediation configuration is not set as Global");
        } catch (HttpStatusCodeException e) {
            fail("No no");
        }
    }
}