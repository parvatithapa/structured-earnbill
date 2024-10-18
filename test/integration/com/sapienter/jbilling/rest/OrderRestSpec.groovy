package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.OrderChangeWS
import com.sapienter.jbilling.server.order.OrderWS
import org.apache.http.HttpStatus

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

import static javax.ws.rs.HttpMethod.*
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.Response.Status.*

class OrderRestSpec extends RestBaseSpec {

    def orderResource

    def setup() {
        init(orderResource, 'orders')
    }

    void "get existing order by ID"() {

        given: "The JSON of the order that needs to be fetched and the corresponding order mock."
        def orderId = Integer.valueOf(1)
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)

        and: "Mock the behavior of getOrder and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> orderMock
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/${orderId}", GET, RestApiHelper.buildJsonHeaders(false))

        then: "Validate the response."
        response.status == OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderJson)
    }

    void "try to get a non-existing order"() {

        given: "The invalid id used to get an order."
        def orderId = Integer.MAX_VALUE

        and: "Mock the behavior of getOrder and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> null
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/${orderId}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_NOT_FOUND
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "failure while trying to get an order"() {

        given: "The id of the order that needs to be fetched"
        def orderId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getOrder and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/${orderId}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "create a new order"() {

        given: "The JSON of the order that needs to be created"
        def orderId = Integer.valueOf(1)
        def orderInfoJson = buildOrderInfoJSON()
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)

        and: "Mock the behavior of createOrder, getOrder, and verify the number of calls"
        1 * webServicesSessionMock.createOrder(_ as OrderWS, _ as OrderChangeWS[]) >> orderId
        1 * webServicesSessionMock.getOrder(orderId) >> orderMock
        0 * webServicesSessionMock._

        when: "Initialize the HTTP request and parameters."
        sendRequest(BASE_URL, POST, RestApiHelper.buildJsonHeaders(), orderInfoJson.bytes)

        then: "Validate the response"
        response.status == CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${orderId}#")
        RestApiHelper.validateResponseJson(response, orderJson)
    }

    void "try to create order with invalid data"() {

        given: "The JSON of the order that needs to be created"
        def orderInfoJson = buildOrderInfoJSON()
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of createOrder and verify the number of calls"
        1 * webServicesSessionMock.createOrder(_ as OrderWS, _ as OrderChangeWS[]) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: "Initialize the HTTP request and parameters."
        sendRequest(BASE_URL, POST, RestApiHelper.buildJsonHeaders(), orderInfoJson.bytes)

        then: "Validate the response"
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "failure while trying to create an order"() {

        given: "The JSON of the order that needs to be created"
        def orderInfoJson = buildOrderInfoJSON()
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of createOrder and verify the number of calls"
        1 * webServicesSessionMock.createOrder(_ as OrderWS, _ as OrderChangeWS[]) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the HTTP request and parameters."
        sendRequest(BASE_URL, POST, RestApiHelper.buildJsonHeaders(), orderInfoJson.bytes)

        then: "Validate the response"
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get the last order for a user"() {

        given: "The JSON of the order that needs to be fetched and the corresponding order mock."
        def orderId = Integer.valueOf(1)
        def userId = Integer.valueOf(2)
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)

        and: "Mock the behavior of getLatestOrder and verify the number of calls"
        1 * webServicesSessionMock.getLatestOrder(userId) >> orderMock
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/last/${userId}", GET)

        then: "Validate the response."
        response.status ==  HttpStatus.SC_OK
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderJson)
    }

    void "try to get the last order for a user when a order does not exist"() {

        given: "The id of the user for whom the last order should be fetched"
        def userId = Integer.valueOf(2)

        and: "Mock the behavior of getLatestOrder and verify the number of calls"
        1 * webServicesSessionMock.getLatestOrder(userId) >> null
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/last/${userId}", GET)

        then: "Validate the response."
        response.status ==  HttpStatus.SC_NOT_FOUND
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        response.getText() == ''
    }

    void "failure while trying to get the last order for a user"() {

        given: "The id of the user for whom the last order should be fetched"
        def userId = Integer.valueOf(2)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getLatestOrder and verify the number of calls"
        1 * webServicesSessionMock.getLatestOrder(userId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/last/${userId}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get last orders for a user"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def number = Integer.valueOf(5)
        def orderIds = [1, 2, 3, 4, 5]
        def ordersJson = buildOrdersJson(1, 2, 3, 4, 5)
        def ordersMock = buildOrdersMock(1, 2, 3, 4, 5)

        and: "Mock the behavior of getLastOrders, getOrder and verify the number of calls"
        1 * webServicesSessionMock.getLastOrders(userId, number) >> orderIds
        1 * webServicesSessionMock.getOrder(1) >> ordersMock[0]
        1 * webServicesSessionMock.getOrder(2) >> ordersMock[1]
        1 * webServicesSessionMock.getOrder(3) >> ordersMock[2]
        1 * webServicesSessionMock.getOrder(4) >> ordersMock[3]
        1 * webServicesSessionMock.getOrder(5) >> ordersMock[4]
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/last/${userId}?number=${number}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_OK
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, ordersJson)
    }

    void "try to get last orders for a user when no orders exist"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def number = Integer.valueOf(5)

        and: "Mock the behavior of getLastOrders and verify the number of calls"
        1 * webServicesSessionMock.getLastOrders(userId, number) >> new OrderWS[0]
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/last/${userId}?number=${number}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_NOT_FOUND
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        response.getText() == ''
    }

    void "try to get last orders for a user with invalid parameters"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def number = Integer.valueOf(-10)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getLastOrders and verify the number of calls"
        1 * webServicesSessionMock.getLastOrders(userId, number) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/last/${userId}?number=${number}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "failure while trying to get last orders for a user"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def number = Integer.valueOf(5)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getLastOrders and verify the number of calls"
        1 * webServicesSessionMock.getLastOrders(userId, number) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/last/${userId}?number=${number}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }


    void "get user orders page"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def limit = Integer.valueOf(5)
        def offset = Integer.valueOf(10)
        def ordersJson = buildOrdersJson(1, 2, 3, 4, 5)
        def ordersMock = buildOrdersMock(1, 2, 3, 4, 5)

        and: "Mock the behavior of getUserOrdersPage and verify the number of calls"
        1 * webServicesSessionMock.getUserOrdersPage(userId, limit, offset) >> ordersMock
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/page/${userId}?limit=${limit}&offset=${offset}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_OK
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, ordersJson)
    }

    void "try to get paged orders for a user when no orders exist"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def limit = Integer.valueOf(5)
        def offset = Integer.valueOf(10)

        and: "Mock the behavior of getUserOrdersPage and verify the number of calls"
        1 * webServicesSessionMock.getUserOrdersPage(userId, limit, offset) >> new OrderWS[0]
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/page/${userId}?limit=${limit}&offset=${offset}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_NOT_FOUND
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        response.getText() == ''
    }

    void "try to get paged orders for a user with invalid parameters"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def limit = Integer.valueOf(0)
        def offset = Integer.valueOf(-10)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getUserOrdersPage and verify the number of calls"
        1 * webServicesSessionMock.getUserOrdersPage(userId, limit, offset) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/page/${userId}?limit=${limit}&offset=${offset}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "failure while trying to get paged orders for a user"() {

        given: "The JSON of the orders that need to be fetched"
        def userId = Integer.valueOf(2)
        def limit = Integer.valueOf(5)
        def offset = Integer.valueOf(10)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getUserOrdersPage and verify the number of calls"
        1 * webServicesSessionMock.getUserOrdersPage(userId, limit, offset) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the http request and parameters."
        sendRequest("${BASE_URL}/page/${userId}?limit=${limit}&offset=${offset}", GET)

        then: "Validate the response."
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "test update order"() {

        given: "The JSON of the order that needs to be updated"
        def orderId = Integer.valueOf(1)
        def orderInfoJson = buildOrderInfoJSON()
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)

        and: "Mock the behavior of updateOrder, getOrder, and verify the number of calls"
        1 * webServicesSessionMock.updateOrder(_ as OrderWS, _ as OrderChangeWS[])
        2 * webServicesSessionMock.getOrder(orderId) >> orderMock
        0 * webServicesSessionMock._

        when: "Initialize the HTTP request and parameters."
        sendRequest("${BASE_URL}/${orderId}", PUT, RestApiHelper.buildJsonHeaders(), orderInfoJson.bytes)

        then: "Validate the response"
        response.status == OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, orderJson)
    }

    void "try to update an order with invalid data"() {

        given: "The JSON of the order that needs to be updated"
        def orderId = Integer.valueOf(1)
        def orderInfoJson = buildOrderInfoJSON()
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getOrder, updateOrder and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> orderMock
        1 * webServicesSessionMock.updateOrder(_ as OrderWS, _ as OrderChangeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: "Initialize the HTTP request and parameters."
        sendRequest("${BASE_URL}/${orderId}", PUT, RestApiHelper.buildJsonHeaders(), orderInfoJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_BAD_REQUEST
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "try to update non existing order"() {

        given: "The JSON of the order that needs to be updated"
        def orderId = Integer.valueOf(1)
        def orderInfoJson = buildOrderInfoJSON()

        and: "Mock the behavior of getOrder and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> null
        0 * webServicesSessionMock._

        when: "Initialize the HTTP request and parameters."
        sendRequest("${BASE_URL}/${orderId}", PUT, RestApiHelper.buildJsonHeaders(), orderInfoJson.bytes)

        then: 'Validate the response.'
        response.status == NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, APPLICATION_JSON)
        response.getText() == ''
    }

    void "failure while trying to update an order"() {

        given: "The JSON of the order that needs to be updated"
        def orderId = Integer.valueOf(1)
        def orderInfoJson = buildOrderInfoJSON()
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of getOrder, updateOrder and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> orderMock
        1 * webServicesSessionMock.updateOrder(_ as OrderWS, _ as OrderChangeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: "Initialize the HTTP request and parameters."
        sendRequest("${BASE_URL}/${orderId}", PUT, RestApiHelper.buildJsonHeaders(), orderInfoJson.bytes)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "test delete order"() {

        given: "The id of the order that needs to be deleted."
        def orderId = Integer.valueOf(1)
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)

        and: "Mock the behavior of deleteOrder and getOrder, and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> orderMock
        1 * webServicesSessionMock.deleteOrder(orderId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderId}", DELETE)

        then: 'Validate the response.'
        response.status == NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "try to delete an order that doesn't exist"() {

        given: "The id of non existing order"
        def orderId = Integer.MAX_VALUE

        and: "Mock the behavior of getOrder, and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderId}", DELETE)

        then: 'Validate the response.'
        response.status == NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "failure while trying to delete an order"() {

        given: "The id of the order that needs to be deleted."
        def orderId = Integer.valueOf(1)
        def orderJson = buildOrderJSON(orderId)
        def orderMock = buildOrderMock(orderJson)
        def errorMessage = 'Test Error Message'

        and: "Mock the behavior of deleteOrder and getOrder, and verify the number of calls"
        1 * webServicesSessionMock.getOrder(orderId) >> orderMock
        1 * webServicesSessionMock.deleteOrder(orderId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${orderId}", DELETE)

        then: 'Validate the response.'
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    static String buildOrderJSON(Integer id) {

        return """{
            "id": ${id},
            "statusId": 2,
            "userId": 2,
            "currencyId": 1,
            "billingTypeId": 1,
            "period": 1,
            "createDate": 1153899819793,
            "createdBy": 1,
            "activeSince": 1153864800000,
            "activeUntil": null,
            "nextBillableDay": null,
            "deleted": 0,
            "isMediated": false,
            "prorateAdjustmentFlag": false,
            "notify": 0,
            "lastNotified": null,
            "notificationStep": null,
            "dueDateUnitId": 3,
            "dueDateValue": null,
            "dfFm": 0,
            "anticipatePeriods": null,
            "ownInvoice": 0,
            "notes": "",
            "notesInInvoice": 0,
            "orderLines": [{
                "id": 1,
                "orderId": 1,
                "createDatetime": 1153899819873,
                "deleted": 0,
                "description": "Lemonade",
                "editable": true,
                "assetIds": [],
                "assetAssignmentIds": [],
                "sipUri": null,
                "metaFields": [],
                "provisioningStatusId": null,
                "provisioningRequestId": null,
                "typeId": 1,
                "useItem": false,
                "itemId": 2,
                "orderLineUsagePools": [],
                "productCode": "DP-2",
                "parentLine": null,
                "childLines": [],
                "isPercentage": false,
                "provisioningCommands": [],
                "percentage": false,
                "isSwapPlanCondition": false,
                "amount": 20,
                "quantity": 1,
                "price": 20,
                "adjustedPrice": null
            }],
            "discountLines": [],
            "pricingFields": null,
            "generatedInvoices": [{
                "delegatedInvoiceId": null,
                "payments": [],
                "accessEntities": [],
                "userId": 2,
                "invoiceLines": [{
                    "id": 1,
                    "description": "Lemonade",
                    "amount": 20,
                    "price": 20,
                    "quantity": 1,
                    "deleted": 0,
                    "itemId": 2,
                    "sourceUserId": 2,
                    "callCounter":0,
                    "percentage": 0
                }],
                "orders": [1],
                "billingProcess": null,
                "id": 1,
                "createDatetime": 1153864800000,
                "createTimeStamp": 1153899823428,
                "lastReminder": null,
                "dueDate": 1156543200000,
                "total": 20.0,
                "toProcess": 0,
                "statusId": 1,
                "balance": 0E-10,
                "carriedBalance": 0E-10,
                "inProcessPayment": 1,
                "deleted": 0,
                "paymentAttempts": 1,
                "isReview": 0,
                "currencyId": 1,
                "customerNotes": null,
                "number": "1",
                "overdueStep": null,
                "metaFields": [{
                    "fieldName": "suretaxResponseTransId",
                    "groupId": null,
                    "dataType": "STRING",
                    "defaultValue": null,
                    "displayOrder": 1,
                    "id": null,
                    "stringValue": null,
                    "dateValue": null,
                    "booleanValue": null,
                    "integerValue": null,
                    "listValue": null,
                    "disabled": false,
                    "mandatory": false,
                    "decimalValue": null,
                    "charValue":null
                }],
                "statusDescr": null
            }],
            "metaFields": [{
                "fieldName": "salesTypeCode",
                "groupId": null,
                "dataType": "ENUMERATION",
                "defaultValue": null,
                "displayOrder": 1,
                "id": null,
                "stringValue": null,
                "dateValue": null,
                "booleanValue": null,
                "integerValue": null,
                "listValue": null,
                "disabled": false,
                "mandatory": false,
                "decimalValue": null,
                "charValue":null
            }],
            "parentOrder": null,
            "childOrders": [],
            "userCode": null,
            "provisioningCommands": [],
            "statusStr": "Finished",
            "timeUnitStr": null,
            "periodStr": "oneTime",
            "billingTypeStr": "prePaid",
            "prorateFlag": false,
            "customerBillingCycleUnit": null,
            "customerBillingCycleValue": null,
            "proratingOption": null,
            "cancellationFeeType": null,
            "cancellationFee": null,
            "cancellationFeePercentage": null,
            "cancellationMaximumFee": null,
            "cancellationMinimumPeriod": null,
            "freeUsageQuantity": "0",
            "primaryOrderId": null,
            "orderStatusWS": {
                "id": 2,
                "orderStatusFlag": "FINISHED",
                "description": "Finished",
                "descriptions": []
            },
            "parentOrderId": null,
            "planBundledItems": null,
            "adjustedTotal": null,
            "total": 20,
            "disable": false
        }"""
    }

    static String buildOrderInfoJSON() {

        return """{
            "order": {
                "activeSince": 1153864800000,
                "billingTypeId": "1",
                "billingTypeStr": "prePaid",
                "createDate": "1153899819793",
                "createdBy": "1",
                "currencyId": "1",
                "deleted": "0",
                "dfFm": "0",
                "disable": "false",
                "dueDateUnitId": "3",
                "freeUsageQuantity": "0",
                "metaFields": [{
                    "dataType": "ENUMERATION",
                    "disabled": "false",
                    "displayOrder": "1",
                    "fieldName": "salesTypeCode",
                    "mandatory": "false"
                }],
                "notes": "",
                "notesInInvoice": "0",
                "notify": "0",
                "orderLines": [],
                "orderStatusWS": {
                    "id": 2,
                    "orderStatusFlag": "FINISHED",
                    "description": "Finished",
                    "descriptions": []
                },
                "ownInvoice": "0",
                "period": "1",
                "periodStr": "oneTime",
                "prorateFlag": "false",
                "statusId": "2",
                "statusStr": "Finished",
                "total": 20,
                "userId": "2",
                "versionNum": "1"
            },
            "orderChanges": [{
                    "id": null,
                    "orderId": null,
                    "orderLineId": null,
                    "itemId": 2,
                    "parentOrderChangeId": null,
                    "parentOrderLineId": null,
                    "description": "Lemonade",
                    "useItem": 1,
                    "startDate": 1474840800000,
                    "applicationDate": null,
                    "assetIds": [],
                    "userAssignedStatusId": 3,
                    "userAssignedStatus": "APPLY",
                    "statusId": null,
                    "status": null,
                    "errorMessage": null,
                    "errorCodes": null,
                    "optLock": 1,
                    "delete": 0,
                    "orderStatusIdToApply": null,
                    "orderChangeTypeId": 1,
                    "type": null,
                    "metaFields": [],
                    "orderChangePlanItems": null,
                    "appliedManually": 0,
                    "removal": 0,
                    "nextBillableDate": 1474840800000,
                    "endDate": null,
                    "parentOrderChange": null,
                    "quantity": 1,
                    "price": 3.5,
                    "percentage": false
            }]
        }"""
    }

    OrderWS buildOrderMock(String json) {

        return BuilderHelper.mapper.readValue(json, OrderWS.class)
    }

    static buildOrdersJson(int id, int... ids) {

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildOrderJSON(id))
        ids.each {
            stringBuilder.append(',')
            stringBuilder.append(buildOrderJSON(it))
        }
        stringBuilder.append(']')
        return stringBuilder.toString()
    }

    List<OrderWS> buildOrdersMock(int id, int... ids) {

        def orders = []
        orders.add(buildOrderMock(buildOrderJSON(id)))
        ids.each {
            orders.add(buildOrderMock(buildOrderJSON(it)))
        }
        return orders
    }


}
