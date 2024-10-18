package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.invoice.InvoiceWS
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Bojan Dikovski
 * @since 13-OCT-2016
 */
class InvoiceRestSpec extends RestBaseSpec {

    def invoiceResource

    def setup() {
        init(invoiceResource, 'invoices')
    }

    void "get an existing invoice by id"() {

        given: 'The JSON of the invoice that needs to be fetched.'
        def invoiceId = Integer.valueOf(1)
        def invoiceJson = buildInvoiceJSON(invoiceId)
        def invoice = buildInvoiceMock(invoiceJson)

        and: 'Mock the behaviour of getInvoiceWS and verify the number of calls.'
        1 * webServicesSessionMock.getInvoiceWS(invoiceId) >> invoice
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${invoiceId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, invoiceJson)
    }

    void "get an invoice with non-existing id"() {

        given: 'The id of a non-existing invoice.'
        def invoiceId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getInvoiceWS and verify the number of calls.'
        1 * webServicesSessionMock.getInvoiceWS(invoiceId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${invoiceId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching an invoice by id"() {

        given: 'The id of the invoice that needs to be fetched.'
        def invoiceId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getInvoiceWS and verify the number of calls.'
        1 * webServicesSessionMock.getInvoiceWS(invoiceId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${invoiceId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "delete an invoice"() {

        given: 'The JSON of the invoice that needs to be deleted.'
        def invoiceId = Integer.valueOf(1)
        def invoiceJson = buildInvoiceJSON(invoiceId)
        def invoice = buildInvoiceMock(invoiceJson)

        and: 'Mock the behaviour of getInvoiceWS and deleteInvoice and verify the number of calls.'
        1 * webServicesSessionMock.getInvoiceWS(invoiceId) >> invoice
        1 * webServicesSessionMock.deleteInvoice(invoiceId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${invoiceId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "delete an invoice with non-existing id"() {

        given: 'The id of a non-existing invoice.'
        def invoiceId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getInvoiceWS and verify the number of calls.'
        1 * webServicesSessionMock.getInvoiceWS(invoiceId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${invoiceId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while deleting an invoice"() {

        given: 'The JSON of the invoice that needs to be deleted.'
        def invoiceId = Integer.valueOf(1)
        def invoiceJson = buildInvoiceJSON(invoiceId)
        def invoice = buildInvoiceMock(invoiceJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getInvoiceWS and deleteInvoice and verify the number of calls.'
        1 * webServicesSessionMock.getInvoiceWS(invoiceId) >> invoice
        1 * webServicesSessionMock.deleteInvoice(invoiceId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${invoiceId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    static String buildInvoiceJSON(Integer id) {

        return """{
            "delegatedInvoiceId": null,
            "payments": [
                6
            ],
            "userId": 2,
            "invoiceLines": [{
                "id": 17,
                "description": "due_date_09/26/2006",
                "amount": 63.0,
                "price": null,
                "quantity": null,
                "deleted": 0,
                "itemId": null,
                "callIdentifier":null,
                "usagePlanId":null,
                "callCounter":0,
                "sourceUserId": null,
                "percentage": 0
            },
            {
                "id": 16,
                "description": "Lemonade",
                "amount": 20.0,
                "price": 20.0,
                "quantity": 1.0,
                "deleted": 0,
                "itemId": 2,
                "callIdentifier":null,
                "usagePlanId":null,
                "callCounter":0,
                "sourceUserId": 2,
                "percentage": 0
            }],
            "orders": [
                4
            ],
            "billingProcess": {
                "id": 12,
                "entityId": 1,
                "periodUnitId": 1,
                "periodValue": 1,
                "billingDate": 1159221600000,
                "billingDateEnd": null,
                "retries": null,
                "retriesToDo": 0,
                "invoiceIds": [
                    15
                ],
                "orderProcesses": [{
                    "id": 15,
                    "billingProcessId": 12,
                    "orderId": 4,
                    "invoiceId": 15,
                    "periodsIncluded": 1,
                    "periodStart": 1159221600000,
                    "periodEnd": 1161813600000,
                    "origin": 1,
                    "review": 0
                }],
                "processRuns": [{
                    "id": 12,
                    "billingProcessId": 12,
                    "runDate": 1159221600000,
                    "started": 1166541000116,
                    "finished": 1166541002870,
                    "invoicesGenerated": 1,
                    "paymentFinished": null,
                    "processRunTotals": [{
                        "id": 11,
                        "processRunId": 12,
                        "currencyId": 1,
                        "totalInvoiced": 83.0,
                        "totalPaid": 0,
                        "totalNotPaid": 0
                    }],
                    "statusId": 2,
                    "statusStr": null
                }],
                "review": 0
            },
            "id": ${id},
            "createDatetime": 1159221600000,
            "createTimeStamp": 1166541000587,
            "lastReminder": null,
            "dueDate": 1161813600000,
            "total": 95.0,
            "toProcess": 0,
            "statusId": 1,
            "balance": 20.0,
            "carriedBalance": 75.0,
            "inProcessPayment": 1,
            "deleted": 0,
            "paymentAttempts": 1,
            "isReview": 0,
            "currencyId": 1,
            "customerNotes": null,
            "number": "6",
            "overdueStep": null,
            "metaFields": [{
                "fieldName": "suretax_response_trans_id",
                "groupId": null,
                "dataType": "STRING",
                "defaultValue": null,
                "displayOrder": 1,
                "id": null,
                "stringValue": null,
                "dateValue": null,
                "booleanValue": null,
                "integerValue": null,
                "charValue":null,
                "listValue": null,
                "disabled": false,
                "mandatory": false,
                "decimalValue": null
            }],
            "statusDescr": "Paid"
        }"""
    }

    InvoiceWS buildInvoiceMock(String json) {

        return BuilderHelper.mapper.readValue(json, InvoiceWS.class)
    }

    static buildInvoicesJSON(int id, int... ids){

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildInvoiceJSON(id))
        ids.each {
            stringBuilder.append(',')
            stringBuilder.append(buildInvoiceJSON(it))
        }
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    InvoiceWS[] buildInvoicesMock(int id, int... ids) {

        def invoices = []
        invoices.add(buildInvoiceMock(buildInvoiceJSON(id)))
        ids.each {
            invoices.add(buildInvoiceMock(buildInvoiceJSON(it)))
        }
        return invoices.toArray(new InvoiceWS[invoices.size()])
    }
}