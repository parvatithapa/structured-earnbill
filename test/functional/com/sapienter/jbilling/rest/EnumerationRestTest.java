package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 07-NOV-2016
 */
@Test(groups = {"rest"}, testName = "EnumerationRestTest")
public class EnumerationRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);

    @BeforeClass
    public void setup(){

        super.setup("enumerations");
    }

    @Test
    public void getAllEnumerations() {

        // Get all enumerations and verify the response
        ResponseEntity<List<EnumerationWS>> getResponse = restTemplate.sendRequest(
                REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertNotNull(getResponse, "Response should not be null");
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());

        // Get the number of existing enumerations
        int number = getResponse.getBody().size();

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));
        EnumerationWS enumeration = new EnumerationWS(null, ENTITY_ID, "testEnumeration", listValues);

        ResponseEntity<EnumerationWS> postResponse = null;

        try {
            // Create the enumeration
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumeration, EnumerationWS.class);

            // Try to get all enumerations again, and verify the returned number
            getResponse = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
            assertEquals(number + 1, getResponse.getBody().size(), "Returned number does not match");
        } finally {
            // Delete the enumeration
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Try to get all enumerations again, and verify the returned number
        getResponse = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertEquals(number, getResponse.getBody().size(), "Returned number does not match");
    }

    @Test
    public void getEnumeration() {

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));
        EnumerationWS enumeration = new EnumerationWS(null, ENTITY_ID, "testEnumeration", listValues);

        ResponseEntity<EnumerationWS> postResponse = null;

        try {
            // Create the enumeration
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumeration, EnumerationWS.class);

            // Get the enumeration and verify the response
            ResponseEntity<EnumerationWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, EnumerationWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
            assertEquals(getResponse.getBody(), postResponse.getBody(), "Enumerations do not match.");
        } finally {
            // Delete the created enumeration
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExistingEnumeration() {

        // Try to get a non-existing enumeration and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, EnumerationWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void createEnumeration() {

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));
        EnumerationWS enumeration = new EnumerationWS(null, ENTITY_ID, "testEnumeration", listValues);

        ResponseEntity<EnumerationWS> postResponse = null;

        try {
            // Create the enumeration
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumeration, EnumerationWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the enumeration is generated
            ResponseEntity<EnumerationWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, EnumerationWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the created enumeration
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    /**
     * C35160441
     */
    @Test
    public void createEnumerationError() {

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));
        EnumerationWS enumeration = new EnumerationWS(null, ENTITY_ID, "testEnumeration", listValues);

        ResponseEntity<EnumerationWS> postResponse = null;

        try {
            // Create the enumeration
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumeration, EnumerationWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Try to create another enumeration with the same name
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, enumeration, EnumerationWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } finally {
            // Delete the created enumeration
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateEnumeration() {

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));
        EnumerationWS enumeration = new EnumerationWS(null, ENTITY_ID, "testEnumeration", listValues);

        ResponseEntity<EnumerationWS> postResponse = null;

        try {
            // Create the enumeration
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumeration, EnumerationWS.class);

            // Update the enumeration and verify the response
            enumeration = postResponse.getBody();
            enumeration.setName("updatedEnumeration");
            ResponseEntity<EnumerationWS> putResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, enumeration, EnumerationWS.class);
            RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
            assertEquals(enumeration, putResponse.getBody(), "Enumerations do not match.");
        } finally {
            // Delete the enumeration
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + enumeration.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateNonExistingEnumeration() {

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));
        EnumerationWS enumeration = new EnumerationWS(null, ENTITY_ID, "testEnumeration", listValues);

        // Try to update a non-existing enumeration and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    enumeration, EnumerationWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateEnumerationError() {

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));

        EnumerationWS enumerationOne = new EnumerationWS(null, ENTITY_ID, "firstEnumeration", listValues);
        EnumerationWS enumerationTwo = new EnumerationWS(null, ENTITY_ID, "twoEnumeration", listValues);

        ResponseEntity<EnumerationWS> firstPostResponse = null;
        ResponseEntity<EnumerationWS> secondPostResponse = null;

        try {
            // Create the enumerations
            firstPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumerationOne, EnumerationWS.class);
            secondPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumerationTwo, EnumerationWS.class);

            // Try to update the second enumeration with the same name as the first one
            enumerationTwo.setName(enumerationOne.getName());
            restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, enumerationTwo, EnumerationWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } finally {
            // Delete the enumerations
            if (null != firstPostResponse) {
                restTemplate.sendRequest(REST_URL + firstPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
            if (null != secondPostResponse) {
                restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void deleteEnumeration() {

        // Build an enumeration
        List<EnumerationValueWS> listValues = new ArrayList<>();
        listValues.add(new EnumerationValueWS("value1"));
        listValues.add(new EnumerationValueWS("value2"));
        listValues.add(new EnumerationValueWS("value3"));
        EnumerationWS enumeration = new EnumerationWS(null, ENTITY_ID, "testEnumeration", listValues);

        ResponseEntity<EnumerationWS> postResponse = null;

        try {
            // Create the enumeration
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, enumeration, EnumerationWS.class);

            // Verify that the enumeration is generated
            ResponseEntity<EnumerationWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, EnumerationWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the enumeration
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Verify that the enumeration does not exist any more
        try {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET, getOrDeleteHeaders,
                    null, EnumerationWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteNonExistingEnumeration() {

        // Try to delete a non-existing enumeration and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders,
                    null, EnumerationWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C35160436
     */
    @Test
    public void getEnumerationWithInvalidLimit() {
        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL,
                    new RestQueryParameter<>("limit", "4k"), new RestQueryParameter<>("offset", "4i")), HttpMethod.GET, getOrDeleteHeaders,
                    null, EnumerationWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
