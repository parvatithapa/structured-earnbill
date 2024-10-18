package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.EnumerationWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
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
class EnumerationRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/enumerations'

    def webServicesSessionMock
    def enumerationResource
    ObjectMapper mapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        enumerationResource.webServicesSession = webServicesSessionMock
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        grailsApplication.config.org.grails.jaxrs.doreader.disable = true
        grailsApplication.config.org.grails.jaxrs.dowriter.disable = true
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

    void "get existing enumeration by id"() {

        given: 'The JSON of the enumeration that needs to be fetched.'
        def enumerationId = Integer.valueOf(1)
        def enumerationJson = buildEnumerationJSON(enumerationId)
        def enumeration = buildEnumerationMock(enumerationJson)

        and: 'Mock the behaviour of getEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >> enumeration
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, enumerationJson)
    }

    void "get enumeration by non-existing id"() {

        given: 'The id of a non-existing enumeration.'
        def enumerationId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching enumeration by id"() {

        given: 'The id of the enumeration that needs to be fetched.'
        def enumerationId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get all existing enumerations with pagination"() {

        given: 'The JSON of enumerations that needs to be fetched.'
        def enumerationsJson = buildEnumerationsJSON(1, 2, 3, 4, 5)
        def enumerations = buildEnumerationsMock(1, 2, 3, 4, 5)
        def limit = 5
        def offset = 10

        and: 'Mock the behaviour of getAllEnumerations and verify the number of calls.'
        1 * webServicesSessionMock.getAllEnumerations(limit, offset) >> enumerations
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}?limit=${limit}&offset=${offset}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, enumerationsJson)
    }

    void "fetching all existing enumerations with invalid limit parameter"() {

        given: 'The expected error message, limit and offset parameters.'
        def errorMessage = 'Test Error Message'
        def limit = 0
        def offset = 10

        and: 'Mock the behaviour of getAllEnumerations and verify the number of calls.'
        1 * webServicesSessionMock.getAllEnumerations(limit, offset) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}?limit=${limit}&offset=${offset}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "fetching all existing enumerations with invalid offset parameter"() {

        given: 'The expected error message, limit and offset parameters.'
        def errorMessage = 'Test Error Message'
        def limit = 5
        def offset = -1

        and: 'Mock the behaviour of getAllEnumerations and verify the number of calls.'
        1 * webServicesSessionMock.getAllEnumerations(limit, offset) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}?limit=${limit}&offset=${offset}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "failure while fetching all existing enumerations"() {

        given: 'The expected error message, limit and offset parameters.'
        def errorMessage = 'Test Error Message'
        def limit = 5
        def offset = 10

        and: 'Mock the behaviour of getAllEnumerations and verify the number of calls.'
        1 * webServicesSessionMock.getAllEnumerations(limit, offset) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}?limit=${limit}&offset=${offset}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "create enumeration"() {

        given: 'The JSON of the enumeration that needs to be created.'
        def enumerationId = Integer.valueOf(1)
        def enumerationJson = buildEnumerationJSON(enumerationId)
        def enumeration = buildEnumerationMock(enumerationJson)

        and: 'Mock the behaviour of createUpdateEnumeration and getEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateEnumeration(_ as EnumerationWS) >> enumerationId
        1 * webServicesSessionMock.getEnumeration(enumerationId) >> enumeration
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), enumerationJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1,
                "http://localhost/${BASE_URL}/${enumerationId}#")
        RestApiHelper.validateResponseJson(response, enumerationJson)
    }

    void "failure while creating an enumeration"() {

        given: 'The JSON of the enumeration that needs to be created.'
        def enumerationJson = buildEnumerationJSON(Integer.valueOf(1))
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of createUpdateEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateEnumeration(_ as EnumerationWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), enumerationJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update enumeration"() {

        given: 'The JSON of the enumeration that needs to be updated.'
        def enumerationId = Integer.valueOf(1)
        def enumerationJson = buildEnumerationJSON(enumerationId)
        def enumeration = buildEnumerationMock(enumerationJson)

        and: 'Mock the behaviour of getEnumeration and createUpdateEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >> enumeration
        1 * webServicesSessionMock.createUpdateEnumeration(_ as EnumerationWS)
        1 * webServicesSessionMock.getEnumeration(enumerationId) >> enumeration
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), enumerationJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, enumerationJson)
    }

    void "update enumeration with non-existing id"() {

        given: 'The id of a non-existing enumeration.'
        def enumerationId = Integer.MAX_VALUE
        def enumerationJson = buildEnumerationJSON(enumerationId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), enumerationJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while updating an enumeration"() {

        given: 'The JSON of the enumeration that needs to be updated.'
        def enumerationId = Integer.valueOf(1)
        def enumerationJson = buildEnumerationJSON(enumerationId)
        def enumeration = buildEnumerationMock(enumerationJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getEnumeration and createUpdateEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >> enumeration
        1 * webServicesSessionMock.createUpdateEnumeration(_ as EnumerationWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), enumerationJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "delete enumeration"() {

        given: 'The JSON of the enumeration that needs to be deleted.'
        def enumerationId = Integer.valueOf(1)
        def enumerationJson = buildEnumerationJSON(enumerationId)
        def enumeration = buildEnumerationMock(enumerationJson)

        and: 'Mock the behaviour of getEnumeration and deleteEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >> enumeration
        1 * webServicesSessionMock.deleteEnumeration(enumerationId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "delete enumeration with non-existing id"() {

        given: 'The id of a non-existing enumeration.'
        def enumerationId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while deleting an enumeration"() {

        given: 'The JSON of the enumeration that needs to be deleted.'
        def enumerationId = Integer.valueOf(1)
        def enumerationJson = buildEnumerationJSON(enumerationId)
        def enumeration = buildEnumerationMock(enumerationJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getEnumeration and deleteEnumeration and verify the number of calls.'
        1 * webServicesSessionMock.getEnumeration(enumerationId) >> enumeration
        1 * webServicesSessionMock.deleteEnumeration(enumerationId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${enumerationId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    public static String buildEnumerationJSON(Integer id) {

        return """{
            "id": ${id},
            "entityId": 1,
            "name": "testEnumeration",
            "values": [
                {
                    "id": 1,
                    "value": "A"
                },
                {
                    "id": 2,
                    "value": "B"
                }
            ]
        }"""
    }

    EnumerationWS buildEnumerationMock(String json) {

        return mapper.readValue(json, EnumerationWS.class)
    }

    private static buildEnumerationsJSON(int id, int... ids){

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildEnumerationJSON(id))
        ids.each {
            stringBuilder.append(',')
            stringBuilder.append(buildEnumerationJSON(it))
        }
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    List<EnumerationWS> buildEnumerationsMock(int id, int... ids) {

        def enumerations = []
        enumerations.add(buildEnumerationMock(buildEnumerationJSON(id)))
        ids.each {
            enumerations.add(buildEnumerationMock(buildEnumerationJSON(it)))
        }
        return enumerations
    }

}
