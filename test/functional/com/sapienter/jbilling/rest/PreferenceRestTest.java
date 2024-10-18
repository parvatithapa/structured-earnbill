package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.util.PreferenceWS;
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
 * @author Bojan Dikovski
 * @since 07-NOV-2016
 */
@Test(groups = {"rest"}, testName = "PreferenceRestTest")
public class PreferenceRestTest extends RestTestCase {

    private static final Integer PREF_TYPE_ID = Integer.valueOf(13);

    @BeforeClass
    public void setup(){

        super.setup("preferences");
    }

    @Test
    public void getPreferenceByTypeId() {

        // Build the expected result
        PreferenceWS expectedPreference = RestEntitiesHelper.buildPreference(Integer.valueOf(13), null, null,
                RestEntitiesHelper.buildPreferenceType(PREF_TYPE_ID, "Self delivery of paper invoices", null, null), "1");

        // Get the preference and verify the response
        ResponseEntity<PreferenceWS> getResponse = restTemplate.sendRequest(REST_URL + PREF_TYPE_ID,
                HttpMethod.GET, getOrDeleteHeaders, null, PreferenceWS.class);
        assertNotNull(getResponse, "GET response should not be null.");
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
        assertEquals(getResponse.getBody(), expectedPreference, "Preferences do not match.");
    }

    @Test
    public void getPreferenceByNonExistingTypeId() {

        // Try to get a non-existing preference and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, PreferenceWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updatePreference() {

        // Build the expected result
        PreferenceWS preference = RestEntitiesHelper.buildPreference(Integer.valueOf(13), null, null,
                RestEntitiesHelper.buildPreferenceType(PREF_TYPE_ID, "Self delivery of paper invoices", null, null), "1");

        // Get the preference and verify the response
        ResponseEntity<PreferenceWS> getResponse = restTemplate.sendRequest(REST_URL + PREF_TYPE_ID,
                HttpMethod.GET, getOrDeleteHeaders, null, PreferenceWS.class);
        assertNotNull(getResponse, "GET response should not be null.");
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
        assertEquals(getResponse.getBody(), preference, "Preferences do not match.");

        // Update the preference and verify the result
        preference.setValue("0");
        ResponseEntity<PreferenceWS> putResponse = restTemplate.sendRequest(REST_URL + PREF_TYPE_ID,
                HttpMethod.PUT, postOrPutHeaders, preference, PreferenceWS.class);
        RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
        assertEquals(preference, putResponse.getBody(), "Preferences do not match.");
    }

    @Test
    public void updateNonExistingPreference() {

        // Build a preference
        PreferenceWS preference = RestEntitiesHelper.buildPreference(Integer.valueOf(13), null, null,
                RestEntitiesHelper.buildPreferenceType(Integer.MAX_VALUE, "Self delivery of paper invoices", null, null), "1");

        // Try to update a non-existing preference and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    preference, PreferenceWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
