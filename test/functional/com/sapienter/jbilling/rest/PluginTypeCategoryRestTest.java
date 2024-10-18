package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryWS;
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
@Test(groups = {"rest"}, testName = "PluginTypeCategoryRestTest")
public class PluginTypeCategoryRestTest extends RestTestCase {

    private static final PluggableTaskTypeCategoryWS EXPECTED_RESULT = RestEntitiesHelper.buildPluginTypeCategory(
            Integer.valueOf(1), "com.sapienter.jbilling.server.pluggableTask.OrderProcessingTask");

    @BeforeClass
    public void setup() {
        super.setup("plugintypecategories");
    }

    @Test
    public void getPluginTypeCategoryById() {

        // Get a plugin type category by the id and verify the response
        ResponseEntity<PluggableTaskTypeCategoryWS> getResponse = restTemplate.sendRequest(
                REST_URL + "/" + EXPECTED_RESULT.getId(),
                HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeCategoryWS.class);
        assertEquals(getResponse.getBody(), EXPECTED_RESULT);
    }

    @Test
    public void getPluginTypeCategoryByNonExistingId() {

        // Try to get a plugin type category by a non-existing id and verify the response
        try {
            restTemplate.sendRequest(REST_URL + "/" + Integer.MAX_VALUE,
                    HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeCategoryWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getPluginTypeCategoryByInterfaceName() {

        // Get a plugin type category by the interface name and verify the response
        ResponseEntity<PluggableTaskTypeCategoryWS> getResponse = restTemplate.sendRequest(
                REST_URL + "/classname/" + EXPECTED_RESULT.getInterfaceName(),
                HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeCategoryWS.class);
        assertEquals(getResponse.getBody(), EXPECTED_RESULT);
    }

    @Test
    public void getPluginTypeCategoryByNonExistingInterfaceName() {

        // Try to get a plugin type category by a non-existing interface name and verify the response
        try {
            restTemplate.sendRequest(REST_URL + "/classname/invalidInterfaceName",
                    HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskTypeCategoryWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
