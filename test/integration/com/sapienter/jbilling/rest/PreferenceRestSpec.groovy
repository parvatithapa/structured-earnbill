package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceWS
import grails.test.mixin.integration.Integration
import org.apache.http.HttpStatus
import org.grails.jaxrs.itest.IntegrationTestSpec

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Bojan Dikovski
 * @since 05-OCT-2016
 */
@Integration
class PreferenceRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/preferences'

    def webServicesSessionMock
    def preferenceResource
    ObjectMapper mapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        preferenceResource.webServicesSession = webServicesSessionMock
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

    void "get existing preference by type id"() {

        given: 'The JSON of the preference that needs to be fetched.'
        def preferenceTypeId = Integer.valueOf(1)
        def preferenceJson = buildPreferenceJSON(preferenceTypeId)
        def preference = buildPreferenceMock(preferenceJson)

        and: 'Mock the behaviour of getPreference and verify the number of calls.'
        1 * webServicesSessionMock.getPreference(preferenceTypeId) >> preference
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${preferenceTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, preferenceJson)
    }

    void "get preference by non-existing type id"() {

        given: 'The id of a non-existing preference type.'
        def preferenceTypeId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPreference and verify the number of calls.'
        1 * webServicesSessionMock.getPreference(preferenceTypeId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${preferenceTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage,'')
    }

    void "failure while fetching preference by type id"() {

        given: 'The id of the preference type that needs to be fetched.'
        def preferenceTypeId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPreference and verify the number of calls.'
        1 * webServicesSessionMock.getPreference(preferenceTypeId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${preferenceTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
    }

    void "update preference"() {

        given: 'The JSON of the preference that needs to be updated.'
        def preferenceTypeId = Integer.valueOf(1)
        def preferenceJson = buildPreferenceJSON(preferenceTypeId)
        def preference = buildPreferenceMock(preferenceJson)

        and: 'Mock the behaviour of getPreference and updatePreference and verify the number of calls.'
        1 * webServicesSessionMock.getPreference(preferenceTypeId) >> preference
        1 * webServicesSessionMock.updatePreference(_ as PreferenceWS)
        1 * webServicesSessionMock.getPreference(preferenceTypeId) >> preference
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${preferenceTypeId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), preferenceJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, preferenceJson)
    }

    void "update preference with non-existing type id"() {

        given: 'The id of a non-existing preference type.'
        def preferenceTypeId = Integer.MAX_VALUE
        def preferenceJson = buildPreferenceJSON(preferenceTypeId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPreference and verify the number of calls.'
        1 * webServicesSessionMock.getPreference(preferenceTypeId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${preferenceTypeId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), preferenceJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage,'')
    }

    void "failure while updating preference"() {

        given: 'The JSON of the preference that needs to be updated.'
        def preferenceTypeId = Integer.valueOf(1)
        def preferenceJson = buildPreferenceJSON(preferenceTypeId)
        def preference = buildPreferenceMock(preferenceJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPreference and updatePreference and verify the number of calls.'
        1 * webServicesSessionMock.getPreference(preferenceTypeId) >> preference
        1 * webServicesSessionMock.updatePreference(_ as PreferenceWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${preferenceTypeId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), preferenceJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
    }

    public static String buildPreferenceJSON(Integer typeId) {

        return """{
            "id": 10,
            "preferenceType": {
                "id": ${typeId},
                "description": "Test_preference_type.",
                "defaultValue": null,
                "validationRule": null
            },
            "value": "Test_preference_value."
        }"""
    }

    PreferenceWS buildPreferenceMock(String json) {

        return mapper.readValue(json, PreferenceWS.class)
    }
}
