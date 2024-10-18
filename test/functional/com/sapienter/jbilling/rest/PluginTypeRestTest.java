package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 07-NOV-2016
 */
@Test(groups = {"rest"}, testName = "PluginTypeRestTest")
public class PluginTypeRestTest extends RestTestCase {

    private static final PluggableTaskTypeWS EXPECTED_RESULT = RestEntitiesHelper.buildPluginType(
            Integer.valueOf(1), Integer.valueOf(1),
            "com.sapienter.jbilling.server.pluggableTask.BasicLineTotalTask", Integer.valueOf(0));

    @BeforeClass
    public void setup() {
        super.setup("plugintypes");
    }

    @Test
    public void getPluginTypeById() {

        // Get a plugin type by the id and verify the response
        ResponseEntity<PluggableTaskTypeWS> getResponse = restTemplate.sendRequest(
                REST_URL + "/" + EXPECTED_RESULT.getId(),
                HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeWS.class);
        assertEquals(getResponse.getBody(), EXPECTED_RESULT);
    }

    @Test
    public void getPluginTypeByNonExistingId() {

        // Try to get a plugin type by a non-existing id and verify the response
        try {
            restTemplate.sendRequest(REST_URL + "/" + Integer.MAX_VALUE,
                    HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getPluginTypeByClassName() {

        // Get a plugin type by the class name and verify the response
        ResponseEntity<PluggableTaskTypeWS> getResponse = restTemplate.sendRequest(
                REST_URL + "/classname/" + EXPECTED_RESULT.getClassName(),
                HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeWS.class);
        assertEquals(getResponse.getBody(), EXPECTED_RESULT);
    }

    @Test
    public void getPluginTypeCategoryByNonExistingInterfaceName() {

        // Try to get a plugin type by a non-existing class name and verify the response
        try {
            restTemplate.sendRequest(REST_URL + "/classname/invalidClassName",
                    HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
