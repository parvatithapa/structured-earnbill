package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS
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
class PluginRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/plugins'

    def webServicesSessionMock
    def pluginResource
    ObjectMapper mapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        pluginResource.webServicesSession = webServicesSessionMock
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

    void "get existing plugin by id"() {

        given: 'The JSON of the plugin that needs to be fetched.'
        def pluginId = Integer.valueOf(1)
        def pluginJson = buildPluginJSON(pluginId)
        def plugin = buildPluginMock(pluginJson)

        and: 'Mock the behaviour of getPluginWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >> plugin
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, pluginJson)
    }

    void "get plugin by non-existing id"() {

        given: 'The id of a non-existing plugin.'
        def pluginId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPluginWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching plugin by id"() {

        given: 'The id of the plugin that needs to be fetched.'
        def pluginId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPluginWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "create plugin"() {

        given: 'The JSON of the plugin that needs to be created.'
        def pluginId = Integer.valueOf(1)
        def pluginJson = buildPluginJSON(pluginId)
        def plugin =  buildPluginMock(pluginJson)

        and: 'Mock the behaviour of createPlugin and getPluginWS and verify the number of calls.'
        1 * webServicesSessionMock.createPlugin(_ as PluggableTaskWS) >> pluginId
        1 * webServicesSessionMock.getPluginWS(pluginId) >> plugin
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), pluginJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1,
                "http://localhost/${BASE_URL}/${pluginId}#")
        RestApiHelper.validateResponseJson(response, pluginJson)
    }

    void "failure while creating a plugin"() {

        given: 'The JSON of the plugin that needs to be created.'
        def pluginJson = buildPluginJSON(Integer.valueOf(1))
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of createPlugin and verify the number of calls.'
        1 * webServicesSessionMock.createPlugin(_ as PluggableTaskWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), pluginJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update plugin"() {

        given: 'The JSON of the plugin that needs to be updated.'
        def pluginId = Integer.valueOf(1)
        def pluginJson = buildPluginJSON(pluginId)
        def plugin = buildPluginMock(pluginJson)

        and: 'Mock the behaviour of getPluginWS and updatePlugin and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >> plugin
        1 * webServicesSessionMock.updatePlugin(_ as PluggableTaskWS)
        1 * webServicesSessionMock.getPluginWS(pluginId) >> plugin
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), pluginJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, pluginJson)
    }

    void "update plugin with non-existing id"() {

        given: 'The id of a non-existing plugin.'
        def pluginId = Integer.MAX_VALUE
        def pluginJson = buildPluginJSON(pluginId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPluginWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), pluginJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while updating a plugin"() {

        given: 'The JSON of the plugin that needs to be updated.'
        def pluginId = Integer.valueOf(1)
        def pluginJson = buildPluginJSON(pluginId)
        def plugin = buildPluginMock(pluginJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPluginWS and updatePlugin and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >> plugin
        1 * webServicesSessionMock.updatePlugin(_ as PluggableTaskWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), pluginJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "delete plugin"() {

        given: 'The JSON of the plugin that needs to be deleted.'
        def pluginId = Integer.valueOf(1)
        def pluginJson = buildPluginJSON(pluginId)
        def plugin = buildPluginMock(pluginJson)

        and: 'Mock the behaviour of getPluginWS and updatePlugin and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >> plugin
        1 * webServicesSessionMock.deletePlugin(pluginId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "delete plugin with non-existing id"() {

        given: 'The id of a non-existing plugin.'
        def pluginId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPluginWS and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while deleting a plugin"() {

        given: 'The JSON of the plugin that needs to be deleted.'
        def pluginId = Integer.valueOf(1)
        def pluginJson = buildPluginJSON(pluginId)
        def plugin = buildPluginMock(pluginJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getPluginWS and deletePlugin and verify the number of calls.'
        1 * webServicesSessionMock.getPluginWS(pluginId) >> plugin
        1 * webServicesSessionMock.deletePlugin(pluginId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    public static String buildPluginJSON(Integer id) {

        return """{
            "id": ${id},
            "processingOrder": 1,
            "notes": null,
            "typeId": 10,
            "parameters": {
                "all": "yes",
                "processor_name": "second_fake_processor",
                "accept-ach": "true"
            }
        }"""
    }

    PluggableTaskWS buildPluginMock(String json) {

        return mapper.readValue(json, PluggableTaskWS.class)
    }
}