package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS
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
class PluginTypeRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/plugintypes'

    def webServicesSessionMock
    def pluginTypeResource
    ObjectMapper mapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        pluginTypeResource.webServicesSession = webServicesSessionMock
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

    void "get existing plugin type by id"() {

        given: 'The JSON of the plugin type that needs to be fetched.'
        def pluginTypeId = Integer.valueOf(1)
        def pluginTypeJson = buildPluginTypeJSON(pluginTypeId)
        def pluginType = buildPluginTypeMock(pluginTypeJson)

        and: 'Mock the behaviour of the getPluginTypeWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeWS(pluginTypeId) >> pluginType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, pluginTypeJson)
    }

    void "get non-existing plugin type by id"() {

        given: 'The id of a non-existing plugin type.'
        def pluginTypeId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeWS(pluginTypeId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching plugin type by id"() {

        given: 'The id of the plugin type that needs to be fetched.'
        def pluginTypeId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeWS(pluginTypeId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get existing plugin type by class name"() {

        given: 'The JSON of the plugin type that needs to be fetched.'
        def className = 'com.sapienter.jbilling.server.pluggableTask.TestTask'
        def pluginTypeJson = buildPluginTypeJSON(className)
        def pluginType = buildPluginTypeMock(pluginTypeJson)

        and: 'Mock the behaviour of the getPluginTypeWSByClassName and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeWSByClassName(className) >> pluginType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/classname/${className}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, pluginTypeJson)
    }

    void "get non-existing plugin type by class name"() {

        given: 'The class name of a non-existing plugin type.'
        def className = 'com.sapienter.jbilling.server.pluggableTask.NonExistentTask'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeWSByClassName and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeWSByClassName(className) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/classname/${className}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching plugin type by class name"() {

        given: 'The class name of the plugin type that needs to be fetched.'
        def className = 'com.sapienter.jbilling.server.pluggableTask.TestTask'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeWSByClassName and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeWSByClassName(className) >>
                {throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/classname/${className}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    public static String buildPluginTypeJSON(Integer id) {

        return """{
            "id": ${id},
            "className": "com.sapienter.jbilling.server.pluggableTask.TestTask",
            "minParameters": 0,
            "categoryId": 1
        }"""
    }

    public static String buildPluginTypeJSON(String className) {

        return """{
            "id": 1,
            "className": "${className}",
            "minParameters": 0,
            "categoryId": 1
        }"""
    }

    PluggableTaskTypeWS buildPluginTypeMock(String json) {

        return mapper.readValue(json, PluggableTaskTypeWS.class)
    }
}