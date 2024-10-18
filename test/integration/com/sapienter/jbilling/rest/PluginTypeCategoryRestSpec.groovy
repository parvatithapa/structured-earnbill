package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryWS
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
class PluginTypeCategoryRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/plugintypecategories'

    def webServicesSessionMock
    def pluginTypeCategoryResource
    ObjectMapper mapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        pluginTypeCategoryResource.webServicesSession = webServicesSessionMock
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

    void "get existing plugin type category by id"() {

        given: 'The JSON of the plugin type category that needs to be fetched.'
        def pluginTypeCategoryId = Integer.valueOf(1)
        def pluginTypeCategoryJson = buildPluginTypeCategoryJSON(pluginTypeCategoryId)
        def pluginTypeCategory = buildPluginTypeCategoryMock(pluginTypeCategoryJson)

        and: 'Mock the behaviour of the getPluginTypeCategory and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeCategory(pluginTypeCategoryId) >> pluginTypeCategory
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginTypeCategoryId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, pluginTypeCategoryJson)
    }

    void "get non-existing plugin type category by id"() {

        given: 'The id of a non-existing plugin type category.'
        def pluginTypeCategoryId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeCategory and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeCategory(pluginTypeCategoryId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginTypeCategoryId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage,'')
    }

    void "failure while fetching plugin type category by id"() {

        given: 'The id of the plugin type category that needs to be fetched.'
        def pluginTypeCategoryId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeCategory and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeCategory(pluginTypeCategoryId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${pluginTypeCategoryId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
    }

    void "get existing plugin type category by interface name"() {

        given: 'The JSON of the plugin type category that needs to be fetched.'
        def interfaceName = 'com.sapienter.jbilling.server.pluggableTask.TestPluggableTask'
        def pluginTypeCategoryJson = buildPluginTypeCategoryJSON(interfaceName)
        def pluginTypeCategory = buildPluginTypeCategoryMock(pluginTypeCategoryJson)

        and: 'Mock the behaviour of the getPluginTypeCategoryByInterfaceName and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeCategoryByInterfaceName(interfaceName) >> pluginTypeCategory
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/classname/${interfaceName}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, pluginTypeCategoryJson)
    }

    void "get non-existing plugin type category by interface name"() {

        given: 'The interface name of a non-existing plugin type category.'
        def interfaceName = 'com.sapienter.jbilling.server.pluggableTask.NonExistentPluggableTask'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeCategoryByInterfaceName and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeCategoryByInterfaceName(interfaceName) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/classname/${interfaceName}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage,'')
    }

    void "failure while fetching plugin type category by interface name"() {

        given: 'The interface name of the plugin type category that needs to be fetched.'
        def interfaceName = 'com.sapienter.jbilling.server.pluggableTask.TestPluggableTask'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getPluginTypeCategoryByInterfaceName and verify the number of calls.'
        1 * webServicesSessionMock.getPluginTypeCategoryByInterfaceName(interfaceName) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/classname/${interfaceName}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
    }

    public static String buildPluginTypeCategoryJSON(Integer id) {

        return """{
            "id": ${id},
            "interfaceName": "com.sapienter.jbilling.server.pluggableTask.TestPluggableTask"
        }"""
    }

    public static String buildPluginTypeCategoryJSON(String interfaceName) {

        return """{
            "id": 1,
            "interfaceName": "${interfaceName}"
        }"""
    }

    PluggableTaskTypeCategoryWS buildPluginTypeCategoryMock(String json) {

        return mapper.readValue(json, PluggableTaskTypeCategoryWS.class)
    }
}
