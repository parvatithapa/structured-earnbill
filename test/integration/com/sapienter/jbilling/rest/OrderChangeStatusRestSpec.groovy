package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.server.order.OrderChangeStatusWS

import com.sapienter.jbilling.common.SessionInternalError
import org.apache.http.HttpStatus
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import org.grails.jaxrs.itest.IntegrationTestSpec

import javax.ws.rs.core.Response
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

/**
 * Created by Martin on 9/2/2016.
 */
class OrderChangeStatusRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/orderchangestatuses'

    def webServicesSessionMock
    def orderChangeStatusResource
    ObjectMapper objectMapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        orderChangeStatusResource.webServicesSession = webServicesSessionMock
        objectMapper = new ObjectMapper()
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        grailsApplication.config.org.grails.jaxrs.doreader.disable = true
        grailsApplication.config.org.grails.jaxrs.dowriter.disable = true
    }

    def cleanup() {
    }

    void "get all existing order change statuses"() {

        given: 'The JSON of the order change statuses that needs to be fetched.'
        String orderChangeStatusesJson = buildAllOrderChangeStatusesJSON()
        List<OrderChangeStatusWS> allOrderChangeStatuses = buildAllOrderChangeStatusesWS()

        and: 'Mock the behaviour of the getOrderChangeStatusesForCompany(), and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> allOrderChangeStatuses
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJsonString(response.getContentAsString(), orderChangeStatusesJson)
    }

    void "failure while trying to get all existing order change statuses"() {

        given: 'Error message that will be received.'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getOrderChangeStatusesForCompany and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "try to create invalid order change status"() {

        given: 'The JSON of the order change statuses that needs to be created.'
        def orderChangeStatusId = 1
        def orderChangeStatusJson = buildOrderChangeStatusJSON(orderChangeStatusId)

        and: 'Mock the behaviour of the createOrderChangeStatus, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createOrderChangeStatus(_ as OrderChangeStatusWS) >>
                {throw new SessionInternalError("Test",[errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderChangeStatusJson.bytes)

        then: 'Validate the response.'
        response.status==HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "create order change status"() {

        given: 'The JSON of the order change statuses that needs to be created.'
        def orderChangeStatusId = 1
        def orderChangeStatusJson = buildOrderChangeStatusJSON(orderChangeStatusId)
        def orderChangeStatus = buildOrderChangeStatusMock(orderChangeStatusJson)
        List<OrderChangeStatusWS> resList = new ArrayList<>()
        resList.add(orderChangeStatus)

        and: 'Mock the behaviour of the createUpdateOrderChangeStatus and findOrderChangeStatusById, and verify the number of calls.'
        1 * webServicesSessionMock.createOrderChangeStatus(_ as OrderChangeStatusWS) >> orderChangeStatusId
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> resList
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderChangeStatusJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1,
                "http://localhost/${BASE_URL}/${orderChangeStatusId}#")
        RestApiHelper.validateResponseJson(response, orderChangeStatusJson)
    }

    void "failure while creating an order change status"() {

        given: 'The JSON of the order change status that needs to be created.'
        def orderChangeStatusId = Integer.valueOf(1)
        def orderChangeStatusJson = buildOrderChangeStatusJSON(orderChangeStatusId)

        and: 'Mock the behaviour of the createOrderChangeStatus and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createOrderChangeStatus(_ as OrderChangeStatusWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderChangeStatusJson.bytes)

        then: 'Validate the response.'
        response.status==HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "try to update non existing order change status"(){

        given: 'The JSON of the order change status that needs to be updated.'
        def orderChangeStatusId = Integer.valueOf(1)
        def orderChangeStatusJson = buildOrderChangeStatusJSON(orderChangeStatusId.intValue())

        and: 'Mock the behaviour of the getOrderChangeStatusesForCompany, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderChangeStatusId.intValue()}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderChangeStatusJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "try to update order change status with invalid data"(){

        given: 'The JSON of the order change status that needs to be updated.'
        def orderChangeStatusJson = buildOrderChangeStatusJSON(1)
        def errorMessage = 'Invalid data'
        List<OrderChangeStatusWS> resList = buildAllOrderChangeStatusesWS()

        and: 'Mock the behaviour of getOrderChangeStatusesForCompany, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> resList
        1 * webServicesSessionMock.updateOrderChangeStatus(_ as OrderChangeStatusWS) >>
                {throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/2", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderChangeStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "update order change status"(){

        given: 'The JSON of the order change status that needs to be updated.'
        def orderChangeStatusJson = buildOrderChangeStatusJSON(1,false)
        def updatedChangeStatusJson = buildOrderChangeStatusJSON(2,false)
        OrderChangeStatusWS updatedChangeStatusWSMock = buildOrderChangeStatusMock(updatedChangeStatusJson)
        List<OrderChangeStatusWS> resList = buildAllOrderChangeStatusesWS()
        List<OrderChangeStatusWS> updatedList = new ArrayList<>()
        updatedList.add(updatedChangeStatusWSMock)

        and: 'Mock the behaviour of getOrderChangeStatusesForCompany, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> resList
        1 * webServicesSessionMock.updateOrderChangeStatus(_ as OrderChangeStatusWS)
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> updatedList
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/2", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderChangeStatusJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, updatedChangeStatusJson)
    }

    void "failure while updating an order change status"(){

        given: 'The JSON of the order change status that needs to be updated.'
        def orderChangeStatusId = Integer.valueOf(1)
        def orderChangeStatusJson = buildOrderChangeStatusJSON(orderChangeStatusId)
        List<OrderChangeStatusWS> resList = buildAllOrderChangeStatusesWS()

        and: 'Mock the behaviour of getOrderChangeStatusesForCompany and updateOrderChangeStatus' +
                ' and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> resList
        1 * webServicesSessionMock.updateOrderChangeStatus(_ as OrderChangeStatusWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderChangeStatusId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), orderChangeStatusJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "try to delete order change status that does not exist"() {

        given: 'The id of the order change status that do not exist'
        def orderChangeStatusId = Integer.valueOf(1)

        and: 'Mock the behaviour of the findOrderChangeStatusById, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderChangeStatusId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
    }

    void "delete order change status"() {

        given: 'The id of the order change statuses that needs to be deleted.'
        def orderChangeStatusId = 1
        List<OrderChangeStatusWS> resList = buildAllOrderChangeStatusesWS()

        and: 'Mock the behaviour of the getOrderChangeStatusesForCompany and deleteOrderChangeStatus.'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> resList
        1 * webServicesSessionMock.deleteOrderChangeStatus(orderChangeStatusId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderChangeStatusId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    void "failure while deleting an order change status"() {

        given: 'The id of the order change status that needs to be deleted.'
        def orderChangeStatusId = Integer.valueOf(1)
        List<OrderChangeStatusWS> resList = buildAllOrderChangeStatusesWS()

        and: 'Mock the behaviour of the getOrderChangeStatusesForCompany and deleteOrderChangeStatus' +
                ' and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getOrderChangeStatusesForCompany() >> resList
        1 * webServicesSessionMock.deleteOrderChangeStatus(orderChangeStatusId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderChangeStatusId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    private List<OrderChangeStatusWS> buildAllOrderChangeStatusesWS(){
        String allStatusesJson = buildAllOrderChangeStatusesJSON()
        List<OrderChangeStatusWS> resp= Arrays.asList(objectMapper.readValue(allStatusesJson, OrderChangeStatusWS[].class))
        return resp
    }

    private def buildOrderChangeStatusMock(String statusJson) {
        return objectMapper.readValue(statusJson, OrderChangeStatusWS.class);
    }

    private def buildOrderChangeStatusJSON(int id) {
        return buildOrderChangeStatusJSON(id, true)
    }

    private def buildOrderChangeStatusJSON(int id, boolean create) {
        //Create or Update content
        String content = create?"APPLY":"TEST"
        StringBuilder sb = new StringBuilder("\"").append(content).append("\"")
        """{
            "id": ${id},
            "order": 1,
            "entityId": 1,
            "deleted": 0,
            "applyToOrder": "YES",
            "descriptions": [
                {
                    "psudoColumn": "description",
                    "languageId": 1,
                    "content": ${sb.toString()},
                    "deleted": false,
                    "label": "description"
                }
            ]
        }"""
    }

    private static String buildAllOrderChangeStatusesJSON() {
        """[
                {
                    "id": 1,
                    "order": 0,
                    "entityId": null,
                    "deleted": 0,
                    "applyToOrder": "NO",
                    "descriptions": [
                        {
                            "psudoColumn": "description",
                            "languageId": 1,
                            "content": "PENDING",
                            "deleted": false,
                            "label": "description"
                        }
                    ]
                },
                {
                    "id": 2,
                    "order": 0,
                    "entityId": null,
                    "deleted": 0,
                    "applyToOrder": "NO",
                    "descriptions": [
                        {
                            "psudoColumn": "description",
                            "languageId": 1,
                            "content": "APPLY ERROR",
                            "deleted": false,
                            "label": "description"
                        }
                    ]
                },
                {
                    "id": 3,
                    "order": 1,
                    "entityId": 1,
                    "deleted": 0,
                    "applyToOrder": "YES",
                    "descriptions": [
                        {
                            "psudoColumn": "description",
                            "languageId": 1,
                            "content": "APPLY",
                            "deleted": false,
                            "label": "description"
                        }
                    ]
                }
        ]"""
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }
}
