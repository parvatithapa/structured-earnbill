package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.util.Hashtable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 07-NOV-2016
 */
@Test(groups = {"rest"}, testName = "PluginRestTest")
public class PluginRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);
    private static final Integer PLUGIN_TYPE_ID = Integer.valueOf(1);
    private static final Integer PROCESSING_ORDER = Integer.valueOf(10);
    private Hashtable<String, String> parameters;

    @BeforeClass
    public void setup(){

        super.setup("plugins");

        parameters = new Hashtable<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        parameters.put("key3", "value3");
    }

    @Test
    public void getPlugin() {

        // Build a plugin
        PluggableTaskWS plugin = RestEntitiesHelper.buildPlugin(
                null, ENTITY_ID, PLUGIN_TYPE_ID, "Test Notes", PROCESSING_ORDER, parameters);

        ResponseEntity<PluggableTaskWS> postResponse = null;

        try {
            // Create the plugin
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, plugin, PluggableTaskWS.class);

            // Get the plugin and verify the response
            ResponseEntity<PluggableTaskWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
            assertEquals(getResponse.getBody(), postResponse.getBody(), "Plugins do not match.");
        } finally {
            // Delete the plugin
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExistingPlugin() {

        // Try to get a non-existing plugin and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, PluggableTaskWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void createPlugin() {

        // Build a plugin
        PluggableTaskWS plugin = RestEntitiesHelper.buildPlugin(
                null, ENTITY_ID, PLUGIN_TYPE_ID, "Test Notes", PROCESSING_ORDER, parameters);

        ResponseEntity<PluggableTaskWS> postResponse = null;

        try {
            // Create the plugin
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, plugin, PluggableTaskWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the plugin is generated
            ResponseEntity<PluggableTaskWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the plugin
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void createPluginError() {

        // Build a plugin with invalid type id
        PluggableTaskWS plugin = RestEntitiesHelper.buildPlugin(
                null, ENTITY_ID, Integer.MAX_VALUE, "Test Notes", PROCESSING_ORDER, parameters);

        try {
            // Try to create the plugin
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, plugin, PluggableTaskWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void updatePlugin() {

        // Build a plugin
        PluggableTaskWS plugin = RestEntitiesHelper.buildPlugin(
                null, ENTITY_ID, PLUGIN_TYPE_ID, "Test Notes", PROCESSING_ORDER, parameters);


        ResponseEntity<PluggableTaskWS> postResponse = null;

        try {
            // Create the plugin
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, plugin, PluggableTaskWS.class);

            // Update the plugin and verify the response
            plugin = postResponse.getBody();
            plugin.setNotes("Updated Test Notes");
            ResponseEntity<PluggableTaskWS> putResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, plugin, PluggableTaskWS.class);
            RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
            assertEquals(plugin, putResponse.getBody(), "Plugins do not match.");
        } finally {
            // Delete the plugin
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateNonExistingPlugin() {

        // Build a plugin
        PluggableTaskWS plugin = RestEntitiesHelper.buildPlugin(
                null, ENTITY_ID, PLUGIN_TYPE_ID, "Test Notes", PROCESSING_ORDER, parameters);

        // Try to update a non-existing plugin and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    plugin, PluggableTaskWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updatePluginError() {

        // Build a plugin
        PluggableTaskWS plugin = RestEntitiesHelper.buildPlugin(
                null, ENTITY_ID, PLUGIN_TYPE_ID, "Test Notes", PROCESSING_ORDER, parameters);

        ResponseEntity<PluggableTaskWS> postResponse = null;

        try {
            // Create the plugin
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, plugin, PluggableTaskWS.class);

            // Try to update the plugin with an invalid type id
            plugin = postResponse.getBody();
            plugin.setTypeId(Integer.MAX_VALUE);
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, plugin, PluggableTaskWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } finally {
            // Delete the plugin
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void deletePlugin() {

        // Build a plugin
        PluggableTaskWS plugin = RestEntitiesHelper.buildPlugin(
                null, ENTITY_ID, PLUGIN_TYPE_ID, "Test Notes", PROCESSING_ORDER, parameters);

        ResponseEntity<PluggableTaskWS> postResponse = null;
        try {
            // Create the plugin
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, plugin, PluggableTaskWS.class);

            // Verify that the plugin is generated
            ResponseEntity<PluggableTaskWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, PluggableTaskWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the plugin
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Verify that the plugin does not exist any more
        try {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET, getOrDeleteHeaders,
                    null, PluggableTaskWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteNonExistingPlugin() {

        // Try to delete a non-existing plugin and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders,
                    null, PluggableTaskWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
