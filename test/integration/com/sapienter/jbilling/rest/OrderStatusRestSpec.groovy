package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.server.order.OrderStatusWS
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import org.grails.jaxrs.itest.IntegrationTestSpec
import org.apache.http.HttpStatus
import javax.ws.rs.core.Response
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

class OrderStatusRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/orderstatuses'

    def webServicesSessionMock
    def orderStatusResource
    ObjectMapper objectMapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        orderStatusResource.webServicesSession = webServicesSessionMock
        objectMapper = new ObjectMapper()
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }

    void "get existing order status"() {

        given: 'The JSON of the order status that needs to be fetched.'
        def orderStatusId = 1
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def orderStatus = buildOrderStatusMock(orderStatusJson)

        and: 'Mock the behaviour of the findOrderStatusById, and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> orderStatus
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJsonString(response.getContentAsString(), orderStatusJson)
    }

    void "try to fetch non existing order status"() {

        given: 'Just the id of the non existing order status'
        def orderStatusId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the findOrderStatusById, and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "failure while getting an existing order status"() {

        given: 'The id of the order status that needs to be fetched.'
        def orderStatusId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the findOrderStatusById and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "try to create invalid order status"(){

        given: 'The JSON of the order statuses that needs to be created.'
        def orderStatusId = Integer.valueOf(1)
        def orderStatusJson = buildOrderStatusJSON(orderStatusId.intValue())

        and: 'Mock the behaviour of the createUpdateOrderStatus, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createUpdateOrderStatus(_ as OrderStatusWS) >>
                {throw new SessionInternalError("Test",[errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "create order status"(){

        given: 'The JSON of the order statuses that needs to be created.'
        def orderStatusId = 1
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def orderStatus = buildOrderStatusMock(orderStatusJson)

        and: 'Mock the behaviour of the createUpdateOrderStatus and findOrderStatusById, and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateOrderStatus(_ as OrderStatusWS) >> orderStatusId
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> orderStatus
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1,
                "http://localhost/${BASE_URL}/${orderStatusId}#")
        RestApiHelper.validateResponseJsonString(response.getContentAsString(), orderStatusJson)
    }

    void "failure while creating an order status"() {

        given: 'The JSON of the order statuses that needs to be created.'
        def orderStatusId = Integer.valueOf(1)
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the createUpdateOrderStatus and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateOrderStatus(_ as OrderStatusWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "create an order status with an invalid description"() {

        given: 'The JSON of the order statuses that needs to be created.'
        def orderStatusId = Integer.valueOf(1)
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the createUpdateOrderStatus and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateOrderStatus(_ as OrderStatusWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "try to update non-existing order status"(){

        given: 'The invalid order status id.'
        def orderStatusId = Integer.MAX_VALUE
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the createUpdateOrderStatus, and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_NOT_FOUND
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "update order status"(){

        given: 'The JSON of the order status that needs to be updated.'
        def orderStatusId = 1
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def orderStatus = buildOrderStatusMock(orderStatusJson)

        and: 'Mock the behaviour of the findOrderStatusById, createUpdateOrderStatus and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> orderStatus
        1 * webServicesSessionMock.createUpdateOrderStatus(_ as OrderStatusWS) >> orderStatusId
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> orderStatus
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_OK
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJsonString(response.getContentAsString(), orderStatusJson)
    }

    void "update an order status with an invalid description"() {

        given: 'The JSON of the order status that needs to be updated.'
        def orderStatusId = Integer.valueOf(1)
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def orderStatus = buildOrderStatusMock(orderStatusJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the findOrderStatusById, createUpdateOrderStatus and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> orderStatus
        1 * webServicesSessionMock.createUpdateOrderStatus(_ as OrderStatusWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "failure while updating an order status"() {

        given: 'The JSON of the order status that needs to be updated.'
        def orderStatusId = Integer.valueOf(1)
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def orderStatus = buildOrderStatusMock(orderStatusJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the findOrderStatusById, createUpdateOrderStatus and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> orderStatus
        1 * webServicesSessionMock.createUpdateOrderStatus(_ as OrderStatusWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "try to delete order status that does not exist"(){

        given: 'The id of the order status that do not exist'
        def orderStatusId = 1

        and: 'Mock the behaviour of the findOrderStatusById, and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
    }

    void "try to delete order status that is in use"(){

        given: 'The id of the order status that do not exist'
        def orderStatusId = 1
        String errorMessage="Cannot Delete. Order Status currently in use.";

        and: 'Mock the behaviour of the findOrderStatusById, and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >>
                {throw new SessionInternalError(errorMessage, HttpStatus.SC_CONFLICT)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_CONFLICT
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "delete order status"(){

        given: 'The id of the order statuses that needs to be deleted.'
        def orderStatusId = 1
        def orderStatusJson = buildOrderStatusJSON(orderStatusId)
        def orderStatus = buildOrderStatusMock(orderStatusJson)

        and: 'Mock the behaviour of the findOrderStatusById and deleteOrderStatus, and verify the number of calls.'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >> orderStatus
        1 * webServicesSessionMock.deleteOrderStatus(orderStatus)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    void "failure while deleting an order status"() {

        given: 'The id of the order statuses that needs to be deleted.'
        def orderStatusId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the findOrderStatusById'
        1 * webServicesSessionMock.findOrderStatusById(orderStatusId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderStatusId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    private static String buildOrderStatusJSON(int id) {
        """{
            "id": ${id},
            "orderStatusFlag": "INVOICE",
            "description": "Active",
            "descriptions": []
        }"""
    }

    private OrderStatusWS buildOrderStatusMock(String json){
        return objectMapper.readValue(json,OrderStatusWS.class)
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }
}
