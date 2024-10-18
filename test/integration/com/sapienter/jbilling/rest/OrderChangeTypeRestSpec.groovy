package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.OrderChangeTypeWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import org.grails.jaxrs.itest.IntegrationTestSpec
import org.apache.http.HttpStatus
import javax.ws.rs.core.Response
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

/**
 * Created by nenad on 8/26/16.
 */
class OrderChangeTypeRestSpec extends IntegrationTestSpec {

    public static final String BASE_URL = 'api/orderchangetypes'

    IWebServicesSessionBean webServicesSessionMock
    def orderChangeTypeResource

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        orderChangeTypeResource.webServicesSession = webServicesSessionMock
    }

    void "get all existing order change types for company"() {
        given: "The order change types, and the corresponding JSON"
        def orderChangeTypesJson = buildOrderChangeTypesJson(1, 2, 3, 4)
        def orderChangeTypes = buildOrderChangeTypes(1, 2, 3, 4)

        and: "Mock the behavior of webServicesSessionMock methods"
        1 * webServicesSessionMock.getOrderChangeTypesForCompany() >> orderChangeTypes
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest(BASE_URL, HttpMethod.GET)

        then: "Validate the response."
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderChangeTypesJson)
    }

    void "failure while getting existing order change types for company"() {

        given: "The expected error message"
        def errorMessage = "Test Error Message"

        and: "Mock the behavior of webServicesSessionMock methods"
        1 * webServicesSessionMock.getOrderChangeTypesForCompany() >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest(BASE_URL, HttpMethod.GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get existing order change type by ID"() {
        given: "The order change type that needs to be fetched."
        def orderChangeTypeId = Integer.valueOf(1)
        def orderChangeTypeJson = buildSampleOrderChangeTypeJson(orderChangeTypeId)
        def orderChangeType = buildOrderChangeType(orderChangeTypeId)

        and: "Mock the behavior of webServicesSessionMock methods"
        1 * webServicesSessionMock.getOrderChangeTypeById(orderChangeTypeId) >> orderChangeType
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest(BASE_URL + "/" + orderChangeTypeId, HttpMethod.GET)

        then: "Validate the response."
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderChangeTypeJson)
    }

    void "try to fetch non existing order change type"() {
        given: "The id of the non existing order change type"
        def ocTypeId = Integer.valueOf(2)

        and: "Mock the behavior of webServicesSessionMock methods"
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> null
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest(BASE_URL + "/" + ocTypeId, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "failure while getting existing order change type by id"() {

        given: "The id of the order change type that needs to be fetched."
        def orderChangeTypeId = Integer.valueOf(1)
        def errorMessage = "Test Error Message"

        and: "Mock the behavior of webServicesSessionMock methods"
        1 * webServicesSessionMock.getOrderChangeTypeById(orderChangeTypeId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest("${BASE_URL}/${orderChangeTypeId}", HttpMethod.GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "create new order change type"() {
        given: "The JSON of the order change type that needs to be created"
        def ocTypeId = Integer.valueOf(1)
        def ocTypeJson = buildSampleOrderChangeTypeJson(ocTypeId)
        def ocType = buildOrderChangeType(ocTypeId)

        and: 'Mock the behaviour of the createUpdateOrderChangeType and getOrderChangeTypeById, and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateOrderChangeType(_ as OrderChangeTypeWS) >> ocTypeId
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> ocType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), ocTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${ocTypeId}#")
        RestApiHelper.validateResponseJson(response, ocTypeJson)
    }

    void "try to create invalid order change type"() {
        given: "The JSON for the invalid order change type"
        int ocTypeId = Integer.valueOf(1)
        def ocTypeJson = buildSampleOrderChangeTypeJson(ocTypeId)

        and: "Mock the behaviour of the createUpdateOrderChangeType and verify the number of calls."
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createUpdateOrderChangeType(_ as OrderChangeTypeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), ocTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "failure while creating an order change type"() {

        given: "The JSON of the order change type that needs to be created"
        def ocTypeId = Integer.valueOf(1)
        def ocTypeJson = buildSampleOrderChangeTypeJson(ocTypeId)
        def errorMessage = "Test Error Message"

        and: 'Mock the behaviour of the createUpdateOrderChangeType and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateOrderChangeType(_ as OrderChangeTypeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), ocTypeJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "update an order change type"() {

        given: "The JSON of the order change type that needs to be updated"
        def ocTypeId = Integer.valueOf(1)
        def ocTypeJson = buildSampleOrderChangeTypeJson(ocTypeId)
        def ocType = buildOrderChangeType(ocTypeId)

        and: 'Mock the behaviour of the createUpdateOrderChangeType, getOrderChangeTypeById, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> ocType
        1 * webServicesSessionMock.createUpdateOrderChangeType(_ as OrderChangeTypeWS) >> ocTypeId
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> ocType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${ocTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), ocTypeJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_OK
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, ocTypeJson)
    }

    void "try to update an order change type that does not exist"() {

        given: "The invalid order change type id"
        int ocTypeId = Integer.MAX_VALUE
        def ocTypeJson = buildSampleOrderChangeTypeJson(ocTypeId)

        and: "Mock the behaviour of getOrderChangeTypeById and verify the number of calls."
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${ocTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), ocTypeJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_NOT_FOUND
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "try to update an invalid order change type"() {

        given: "The JSON for the invalid order change type"
        int ocTypeId = Integer.valueOf(1)
        def ocTypeJson = buildSampleOrderChangeTypeJson(ocTypeId)
        def ocType = buildOrderChangeType(ocTypeId)

        and: "Mock the behaviour of the createUpdateOrderChangeType, getOrderChangeTypeById and verify the number of calls."
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> ocType
        1 * webServicesSessionMock.createUpdateOrderChangeType(_ as OrderChangeTypeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${ocTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), ocTypeJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "failure while updating an order change type"() {

        given: "The JSON of the order change type that needs to be updated"
        def ocTypeId = Integer.valueOf(1)
        def ocTypeJson = buildSampleOrderChangeTypeJson(ocTypeId)
        def ocType = buildOrderChangeType(ocTypeId)
        def errorMessage = "Test Error Message"

        and: 'Mock the behaviour of the createUpdateOrderChangeType and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> ocType
        1 * webServicesSessionMock.createUpdateOrderChangeType(_ as OrderChangeTypeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${ocTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), ocTypeJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "delete order change type"() {
        given: "The ID of the order change type that needs to be deleted"
        int ocTypeId = 1

        and: "Mock the behaviour of the getOrderChangeTypeById and deleteOrderChangeType, and verify the number of calls."
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> new OrderChangeTypeWS()
        1 * webServicesSessionMock.deleteOrderChangeType(ocTypeId)
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest(BASE_URL + "/" + ocTypeId, HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    void "try to delete non existing order change type"() {

        given: "The ID of the order change type that needs to be deleted"
        def ocTypeId = Integer.valueOf(1)

        and: "Mock the behaviour of the getOrderChangeTypeById and verify the number of calls."
        1 * webServicesSessionMock.getOrderChangeTypeById(_ as Integer) >> null
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest("${BASE_URL}/${ocTypeId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_NOT_FOUND
    }

    void "try to delete order change type, that can not be deleted"() {
        given: "The ID of the order change type that needs to be deleted"
        int ocTypeId = 1

        and: "Mock the behaviour of the getOrderChangeTypeById and deleteOrderChangeType, and verify the number of calls."
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> new OrderChangeTypeWS()
        1 * webServicesSessionMock.deleteOrderChangeType(ocTypeId) >> {
            throw new SessionInternalError("Test", HttpStatus.SC_METHOD_NOT_ALLOWED)
        }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest(BASE_URL + "/" + ocTypeId, HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == HttpStatus.SC_METHOD_NOT_ALLOWED
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_METHOD_NOT_ALLOWED, '', '')
    }

    void "failure while deleting an order change type"() {
        given: "The id of the order change type that needs to be deleted"
        def ocTypeId = Integer.valueOf(1)
        def errorMessage = "Test Error Message"

        and: "Mock the behaviour of the getOrderChangeTypeById and deleteOrderChangeType, and verify the number of calls."
        1 * webServicesSessionMock.getOrderChangeTypeById(ocTypeId) >> new OrderChangeTypeWS()
        1 * webServicesSessionMock.deleteOrderChangeType(ocTypeId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters"
        sendRequest("${BASE_URL}/${ocTypeId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    private String buildOrderChangeTypesJson(int ... ids) {
        StringBuffer restult = '' << '['
        ids.each {
            restult << """
            {
                "id": ${it},
                "entityId": 1,
                "name": "Default",
                "defaultType": true,
                "allowOrderStatusChange": false,
                "itemTypes": [],
                "orderChangeTypeMetaFields": []
            },
            """
        }

        int li = restult.lastIndexOf(',')
        restult.replace(li, li + 1, '')

        restult << "]"
        restult.toString()
    }

    private String buildSampleOrderChangeTypeJson(int id) {
        """
        {
            "id": ${id},
            "entityId": 1,
            "name": "Default",
            "defaultType": true,
            "allowOrderStatusChange": false,
            "itemTypes": [],
            "orderChangeTypeMetaFields": []
        }
        """
    }

    private OrderChangeTypeWS buildOrderChangeType(int id) {
        new OrderChangeTypeWS(
                id: id,
                entityId: 1,
                name: 'Default',
                defaultType: true,
                allowOrderStatusChange: false,
                itemTypes: [],
                orderChangeTypeMetaFields: []
        )
    }

    private OrderChangeTypeWS[] buildOrderChangeTypes(int ... ids) {
        List<OrderChangeTypeWS> ocTypes = []
        ids.each {
            ocTypes << buildOrderChangeType(it)
        }
        ocTypes.toArray()
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

}
