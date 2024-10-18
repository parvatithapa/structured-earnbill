package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.OrderPeriodWS
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import org.apache.http.HttpStatus
import javax.ws.rs.core.Response
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

/**
 * @author Vojislav Stanojevikj
 * @since 16-Aug-2016.
 */
class OrderPeriodRestSpec extends RestBaseSpec{

    def orderPeriodResource

    def setup() {
        init(orderPeriodResource, 'orderperiods')
    }

    void "get all existing order periods"(){

        given: 'The JSON of the order periods that needs to be fetched.'
        def orderPeriodJSON = buildOrderPeriodJSONString(100, 102, 103, 104)
        def orderPeriods = buildOrderPeriodArrayMock(100, 102, 103, 104)

        and: 'Mock the behaviour of the getOrderPeriods, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriods() >> orderPeriods
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderPeriodJSON)
    }

    void "get all existing order periods resulted with internal error"(){

        given: 'Mock the behaviour of the getOrderPeriods, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriods() >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}

        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "get existing order period"() {

        given: 'The JSON of the order period that needs to be fetched.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)

        and: 'Mock the behaviour of the getOrderPeriodWS, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderPeriodJson)
    }

    void "try to fetch non existing order period"() {

        given: 'Just the id of the non existing order period'
        def orderPeriodId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getOrderPeriodWS, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "failure while getting an order period"() {

        given: 'The id of the order period that needs to be fetched.'
        def orderPeriodId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getOrderPeriodWS and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "fetch existing order period resulted with internal error"() {

        given: 'Just the id of the non existing order period'
        def orderPeriodId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getOrderPeriodWS, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to create invalid order period"(){

        given: 'The JSON of the order period that needs to be created.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId)

        and: 'Mock the behaviour of the createOrderPeriod, and verify the number of calls.'
        1 * webServicesSessionMock.createOrderPeriod(_ as OrderPeriodWS) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderPeriodJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "creation of order period resulted with internal error"(){

        given: 'The JSON of the order period that needs to be created.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId.intValue())

        and: 'Mock the behaviour of the createOrderPeriod, and verify the number of calls.'
        1 * webServicesSessionMock.createOrderPeriod(_ as OrderPeriodWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST,
                RestApiHelper.buildJsonHeaders(), orderPeriodJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "create order period"(){

        given: 'The JSON of the order period that needs to be created.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)

        and: 'Mock the behaviour of the createOrderPeriod and getOrderPeriodWS, and verify the number of calls.'
        1 * webServicesSessionMock.createOrderPeriod(_ as OrderPeriodWS) >> orderPeriodId
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderPeriodJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${orderPeriodId}#")
        RestApiHelper.validateResponseJson(response, orderPeriodJson)
    }

    void "failure while creating an order period"() {

        given: 'The JSON of the order period that needs to be created.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the createOrderPeriod and verify the number of calls.'
        1 * webServicesSessionMock.createOrderPeriod(_ as OrderPeriodWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), orderPeriodJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "try to update order period with invalid data"(){

        given: 'The JSON of the order period that needs to be updated.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)

        and: 'Mock the behaviour of the getOrderPeriodWS and updateOrCreateOrderPeriod, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        1 * webServicesSessionMock.updateOrCreateOrderPeriod(_ as OrderPeriodWS) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), orderPeriodJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update order period"(){

        given: 'The JSON of the order period that needs to be updated.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)

        and: 'Mock the behaviour of the getOrderPeriodWS and updateOrCreateOrderPeriod, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        1 * webServicesSessionMock.updateOrCreateOrderPeriod(_ as OrderPeriodWS) >> true
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), orderPeriodJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderPeriodJson)
    }

    void "failure while updating an order period"() {

        given: 'The JSON of the order period that needs to be updated.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriodJson = buildOrderPeriodJSONString(orderPeriodId)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getOrderPeriodWS and updateOrCreateOrderPeriod, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        1 * webServicesSessionMock.updateOrCreateOrderPeriod(_ as OrderPeriodWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), orderPeriodJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "try to delete order period that do not exist"(){

        given: 'The id of the order period that do not exist'
        def orderPeriodId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getOrderPeriodWS, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "try to delete order period that can not be deleted"(){

        given: 'The id of the order period that needs to be deleted.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)

        and: 'Mock the behaviour of the getOrderPeriodWS and deleteOrderPeriod, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        1 * webServicesSessionMock.deleteOrderPeriod(orderPeriodId) >> false
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        response.getText() == ''
    }

    void "delete on order period resulted with internal error"(){

        given: 'The id of the order period that do not exist'
        def orderPeriodId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getOrderPeriodWS, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "delete order period"(){

        given: 'The id of the order period that needs to be deleted.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)

        and: 'Mock the behaviour of the getOrderPeriodWS and deleteOrderPeriod, and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        1 * webServicesSessionMock.deleteOrderPeriod(orderPeriodId) >> true
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    void "failure while deleting an order period"(){

        given: 'The id of the order period that needs to be deleted.'
        def orderPeriodId = Integer.valueOf(1)
        def orderPeriod = buildOrderPeriodMock(orderPeriodId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the getOrderPeriodWS and deleteOrderPeriod and verify the number of calls.'
        1 * webServicesSessionMock.getOrderPeriodWS(orderPeriodId) >> orderPeriod
        1 * webServicesSessionMock.deleteOrderPeriod(orderPeriodId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderPeriodId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    private static buildOrderPeriodJSONString(int id){

        """{
            "id": ${id},
            "entityId": 1,
            "periodUnitId": 1,
            "value": 2,
            "descriptions": [${BuilderHelper.buildInternationalDescriptionJson('TestDesc', 'description')}]
        }"""
    }

    private static buildOrderPeriodJSONString(int id, int... ids){

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildOrderPeriodJSONString(id))
        ids.each {
            stringBuilder.append(',')
            stringBuilder.append(buildOrderPeriodJSONString(it))
        }
        stringBuilder.append("]")
        stringBuilder.toString()
    }

    private static buildOrderPeriodMock(int id){

        OrderPeriodWS orderPeriod = new OrderPeriodWS()
        orderPeriod.id = id
        orderPeriod.entityId = Integer.valueOf(1)
        orderPeriod.periodUnitId = Integer.valueOf(1)
        orderPeriod.value = Integer.valueOf(2)
        orderPeriod.descriptions = Arrays.asList(new InternationalDescriptionWS('description', Integer.valueOf(1), 'TestDesc'))
        orderPeriod
    }

    private static buildOrderPeriodArrayMock(int id, int... ids){

        def orderPeriods = []
        orderPeriods.add(buildOrderPeriodMock(id))
        ids.each {
            orderPeriods.add(buildOrderPeriodMock(it))
        }

        orderPeriods.toArray(new OrderPeriodWS[orderPeriods.size()])
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

}
