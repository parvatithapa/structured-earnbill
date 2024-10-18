package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 07-NOV-2016
 */
@Test(groups = {"rest"}, testName = "MetaFieldRestTest")
public class MetaFieldRestTest extends RestTestCase {

    @BeforeClass
    public void setup(){

        super.setup("metafields");
    }

    /**
     * C35160446
     */
    @Test
    public void getAllMetaFields() {

        // Get all meta fields and verify the response
        ResponseEntity<List<MetaFieldWS>> getResponse = restTemplate.sendRequest(
                REST_URL + "?entityType=ORDER", HttpMethod.GET, getOrDeleteHeaders, null);
        assertNotNull(getResponse, "Response should not be null");
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());

        // Get the number of existing meta fields
        int number = getResponse.getBody().size();

        // Build a meta field
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.STRING, null, "testMetaField");
        metaField.setPrimary(true);

        ResponseEntity<MetaFieldWS> postResponse = null;

        try {
            // Create the meta field
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, metaField, MetaFieldWS.class);

            // Try to get all meta field again, and verify the returned number
            getResponse = restTemplate.sendRequest(
                    REST_URL + "?entityType=ORDER", HttpMethod.GET, getOrDeleteHeaders, null);
            assertEquals(number + 1, getResponse.getBody().size(), "Returned number does not match");
        } finally {
            // Delete the meta field
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Try to get all meta field again, and verify the returned number
        getResponse = restTemplate.sendRequest(
                REST_URL + "?entityType=ORDER", HttpMethod.GET, getOrDeleteHeaders, null);
        assertEquals(number, getResponse.getBody().size(), "Returned number does not match");
    }

    @Test
    public void getMetaField() {

        // Build a meta field
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.STRING, null, "testMetaField");

        ResponseEntity<MetaFieldWS> postResponse = null;

        try {
            // Create the meta field
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, metaField, MetaFieldWS.class);

            // Get the meta field and verify the response
            ResponseEntity<MetaFieldWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, MetaFieldWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
            assertEquals(getResponse.getBody(), postResponse.getBody(), "Meta fields do not match.");
        } finally {
            // Delete the created meta field
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExistingMetaField() {

        // Try to get a non-existing meta field and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, MetaFieldWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void createMetaField() {

        // Build a meta field
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.STRING, null, "testMetaField");

        ResponseEntity<MetaFieldWS> postResponse = null;

        try {
            // Create the meta field
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, metaField, MetaFieldWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the meta field is generated
            ResponseEntity<MetaFieldWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, MetaFieldWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the created meta field
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void createMetaFieldError() {

        // Build a meta field of type script with null file name
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.SCRIPT, null, "testMetaField");

        try {
            // Try to create the meta field
            restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, metaField, MetaFieldWS.class);
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void updateMetaField() {

        // Build a meta field
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.STRING, null, "testMetaField");

        ResponseEntity<MetaFieldWS> postResponse = null;

        try {
            // Create the meta field
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, metaField, MetaFieldWS.class);

            // Update the meta field and verify the response
            metaField = postResponse.getBody();
            metaField.setName("updatedMetaField");
            ResponseEntity<MetaFieldWS> putResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, metaField, MetaFieldWS.class);
            RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
            assertEquals(metaField, putResponse.getBody(), "Meta fields do not match.");
        } finally {
            // Delete the meta field
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + metaField.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateNonExistingMetaField() {

        // Build a meta field
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.STRING, null, "testMetaField");

        // Try to update a non-existing meta field and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    metaField, MetaFieldWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateMetaFieldError() {

        // Build a meta field
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.STRING, null, "testMetaField");

        ResponseEntity<MetaFieldWS> postResponse = null;

        try {
            // Create the meta field
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, metaField, MetaFieldWS.class);

            // Try to update the meta field with type script and null file name
            metaField.setDataType(DataType.SCRIPT);
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, metaField, MetaFieldWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            // Delete the meta field
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void deleteMetaField() {

        // Build a meta field
        MetaFieldWS metaField = RestEntitiesHelper.buildMetaField(
                Integer.valueOf(0), EntityType.ORDER, DataType.STRING, null, "testMetaField");

        ResponseEntity<MetaFieldWS> postResponse = null;

        try {
            // Create the meta field
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, metaField, MetaFieldWS.class);

            // Verify that the meta field is generated
            ResponseEntity<MetaFieldWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, MetaFieldWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the meta field
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Verify that the meta field does not exist any more
        try {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET, getOrDeleteHeaders,
                    null, MetaFieldWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteNonExistingMetaField() {

        // Try to delete a non-existing meta field and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders,
                    null, MetaFieldWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
