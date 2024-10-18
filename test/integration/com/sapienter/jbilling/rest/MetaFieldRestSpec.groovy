package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.MetaFieldWS
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
class MetaFieldRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/metafields'

    def webServicesSessionMock
    def metaFieldResource
    ObjectMapper mapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        metaFieldResource.webServicesSession = webServicesSessionMock
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

    void "get existing meta field by id"() {

        given: 'The JSON of the meta field that needs to be fetched.'
        def metaFieldId = Integer.valueOf(1)
        def metaFieldJson = buildMetaFieldJSON(metaFieldId)
        def metaField = buildMetaFieldMock(metaFieldJson)

        and: 'Mock the behaviour of getMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >> metaField
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, metaFieldJson)
    }

    void "get meta field by non-existing id"() {

        given: 'The id of a non-existing meta field.'
        def metaFieldId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching meta field by id"() {

        given: 'The id of the meta field that needs to be fetched.'
        def metaFieldId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get existing meta fields by entity type"() {

        given: 'The entity type of the meta fields that needs to be fetched.'
        def entityType = 'CUSTOMER'
        def metaFieldsJson = buildMetaFieldsJSON(1, 2, 3, 4, 5)
        def metaFields = buildMetaFieldsMock(1, 2, 3, 4, 5)

        and: 'Mock the behaviour of getMetaFieldsForEntity and verify the number of calls.'
        1 * webServicesSessionMock.getMetaFieldsForEntity(entityType) >> metaFields
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}?entityType=${entityType}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, metaFieldsJson)
    }

    void "failure while fetching meta fields by entity type"() {

        given: 'The entity type of the meta fields that needs to be fetched.'
        def entityType = 'CUSTOMER'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getMetaFieldsForEntity and verify the number of calls.'
        1 * webServicesSessionMock.getMetaFieldsForEntity(entityType) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}?entityType=${entityType}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "create meta field"() {

        given: 'The JSON of the meta field that needs to be created.'
        def metaFieldId = Integer.valueOf(1)
        def metaFieldJson = buildMetaFieldJSON(metaFieldId)
        def metaField = buildMetaFieldMock(metaFieldJson)

        and: 'Mock the behaviour of createMetaField and getMetaField and verify the number of calls.'
        1 * webServicesSessionMock.createMetaField(_ as MetaFieldWS) >> metaFieldId
        1 * webServicesSessionMock.getMetaField(metaFieldId) >> metaField
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), metaFieldJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1,
                "http://localhost/${BASE_URL}/${metaFieldId}#")
        RestApiHelper.validateResponseJson(response, metaFieldJson)
    }

    void "failure while creating a meta field"() {

        given: 'The JSON of the meta field that needs to be created.'
        def metaFieldJson = buildMetaFieldJSON(Integer.valueOf(1))
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of createMetaField and verify the number of calls.'
        1 * webServicesSessionMock.createMetaField(_ as MetaFieldWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), metaFieldJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update meta field"() {

        given: 'The JSON of the meta field that needs to be updated.'
        def metaFieldId = Integer.valueOf(1)
        def metaFieldJson = buildMetaFieldJSON(metaFieldId)
        def metaField = buildMetaFieldMock(metaFieldJson)

        and: 'Mock the behaviour of getMetaField and updateMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >> metaField
        1 * webServicesSessionMock.updateMetaField(_ as MetaFieldWS)
        1 * webServicesSessionMock.getMetaField(metaFieldId) >> metaField
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), metaFieldJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, metaFieldJson)
    }

    void "update meta field with non-existing id"() {

        given: 'The id of a non-existing meta field.'
        def metaFieldId = Integer.MAX_VALUE
        def metaFieldJson = buildMetaFieldJSON(metaFieldId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), metaFieldJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while updating a meta field"() {

        given: 'The JSON of the meta field that needs to be updated.'
        def metaFieldId = Integer.valueOf(1)
        def metaFieldJson = buildMetaFieldJSON(metaFieldId)
        def metaField = buildMetaFieldMock(metaFieldJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getMetaField and updateMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >> metaField
        1 * webServicesSessionMock.updateMetaField(_ as MetaFieldWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), metaFieldJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "delete meta field"() {

        given: 'The JSON of the meta field that needs to be deleted.'
        def metaFieldId = Integer.valueOf(1)
        def metaFieldJson = buildMetaFieldJSON(metaFieldId)
        def metaField = buildMetaFieldMock(metaFieldJson)

        and: 'Mock the behaviour of getMetaField and deleteMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >> metaField
        1 * webServicesSessionMock.deleteMetaField(metaFieldId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "delete meta field with non-existing id"() {

        given: 'The id of a non-existing meta field.'
        def metaFieldId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while deleting a meta field"() {

        given: 'The JSON of the meta field that needs to be deleted.'
        def metaFieldId = Integer.valueOf(1)
        def metaFieldJson = buildMetaFieldJSON(metaFieldId)
        def metaField = buildMetaFieldMock(metaFieldJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getMetaField and deleteMetaField and verify the number of calls.'
        1 * webServicesSessionMock.getMetaField(metaFieldId) >> metaField
        1 * webServicesSessionMock.deleteMetaField(metaFieldId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${metaFieldId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    public static String buildMetaFieldJSON(Integer id) {

        return """{
            "id": ${id},
            "entityId": 1,
            "name": "test.meta.field",
            "fakeId": null,
            "entityType": "CUSTOMER",
            "dataType": "STRING",
            "disabled": false,
            "mandatory": false,
            "displayOrder": 1,
            "dependentMetaFields": [],
            "dataTableId": null,
            "helpDescription": null,
            "helpContentURL": null,
            "defaultValue": null,
            "validationRule": null,
            "primary": true,
            "fieldUsage": null,
            "filename": null
        }"""
    }

    MetaFieldWS buildMetaFieldMock(String json) {

        return mapper.readValue(json, MetaFieldWS.class)
    }

    private static buildMetaFieldsJSON(int id, int... ids){

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildMetaFieldJSON(id))
        ids.each {
            stringBuilder.append(',')
            stringBuilder.append(buildMetaFieldJSON(it))
        }
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    MetaFieldWS[] buildMetaFieldsMock(int id, int... ids) {

        def metaFields = []
        metaFields.add(buildMetaFieldMock(buildMetaFieldJSON(id)))
        ids.each {
            metaFields.add(buildMetaFieldMock(buildMetaFieldJSON(it)))
        }
        return metaFields.toArray(new MetaFieldWS[metaFields.size()])
    }
}
