package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Martin Kostovski
 * @author Vojislav Stanojevikj
 * @since 9/12/2016
 */

class PaymentMethodTypesRestSpec extends RestBaseSpec {

    def paymentMethodTypesResource

    def setup() {
        init(paymentMethodTypesResource, 'paymentMethodTypes')
    }

    void "get existing payment method type"() {

        given: 'The JSON of the payment methodType that needs to be fetched.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)
        def paymentMethodType = BuilderHelper.buildWSMock(paymentMethodTypeJson, PaymentMethodTypeWS)

        and: 'Mock the behaviour of the getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> paymentMethodType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, paymentMethodTypeJson)
    }

    void "try to fetch non existing payment method type"() {

        given: 'Just the id of the non existing payment method type'
        def paymentMethodTypeId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText()==''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "fetch existing payment method type resulted with internal error"() {

        given: 'Just the id of the non existing payment method type'
        def paymentMethodTypeId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "try to create invalid payment method type"() {

        given: 'The JSON of the payment method types that needs to be created.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)

        and: 'Mock the behaviour of the createPaymentMethodType, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createPaymentMethodType(_ as PaymentMethodTypeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentMethodTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "creation of payment method type resulted with internal error"() {

        given: 'The JSON of the payment method types that needs to be created.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)

        and: 'Mock the behaviour of the createPaymentMethodType, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createPaymentMethodType(_ as PaymentMethodTypeWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentMethodTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create payment method type"() {

        given: 'The JSON of the payment method types that needs to be created.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)
        def paymentMethodType = BuilderHelper.buildWSMock(paymentMethodTypeJson, PaymentMethodTypeWS)

        and: 'Mock the behaviour of the createPaymentMethodType and getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.createPaymentMethodType(_ as PaymentMethodTypeWS) >> paymentMethodTypeId
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> paymentMethodType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentMethodTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${paymentMethodTypeId}#")
        RestApiHelper.validateResponseJson(response, paymentMethodTypeJson)
    }

    void "try to update non existing payment method type"() {

        given: 'The JSON of the payment method type that needs to be updated.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)

        and: 'Mock the behaviour of the getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentMethodTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to update payment method type with invalid data"() {

        given: 'The JSON of the payment method type that needs to be updated.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)
        def paymentMethodType = BuilderHelper.buildWSMock(paymentMethodTypeJson, PaymentMethodTypeWS)

        and: 'Mock the behaviour of getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> paymentMethodType
        1 * webServicesSessionMock.updatePaymentMethodType(_ as PaymentMethodTypeWS) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentMethodTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update payment method type resulted with internal error"() {

        given: 'The JSON of the payment method type that needs to be updated.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)
        def paymentMethodType = BuilderHelper.buildWSMock(paymentMethodTypeJson, PaymentMethodTypeWS)

        and: 'Mock the behaviour of getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> paymentMethodType
        1 * webServicesSessionMock.updatePaymentMethodType(_ as PaymentMethodTypeWS) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentMethodTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update payment method type with valid data"() {

        given: 'The JSON of the payment method type that needs to be updated.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)
        def paymentMethodType = BuilderHelper.buildWSMock(paymentMethodTypeJson, PaymentMethodTypeWS)

        and: 'Mock the behaviour of getPaymentMethodType, and verify the number of calls.'
        2 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> paymentMethodType
        1 * webServicesSessionMock.updatePaymentMethodType(_ as PaymentMethodTypeWS)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentMethodTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, paymentMethodTypeJson)
    }

    void "try to delete payment method type that does not exist"() {

        given: 'The id of the payment method type that do not exist'
        def paymentMethodTypeId = 1

        and: 'Mock the behaviour of the getPaymentMethodType, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
    }

    void "delete payment method type"() {

        given: 'The id of the payment method types that needs to be deleted.'
        def paymentMethodTypeId = 1
        def paymentMethodTypeJson = buildPaymentMethodTypeJSON(paymentMethodTypeId)
        def paymentMethodType = BuilderHelper.buildWSMock(paymentMethodTypeJson, PaymentMethodTypeWS)

        and: 'Mock the behaviour of the getPaymentMethodType and deletePaymentMethodType.'
        1 * webServicesSessionMock.getPaymentMethodType(paymentMethodTypeId) >> paymentMethodType
        1 * webServicesSessionMock.deletePaymentMethodType(paymentMethodTypeId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTypeId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    private static def buildPaymentMethodTypeJSON(int id) {
        """{
            "id": ${id},
            "methodName": "Credit Card",
            "isRecurring": true,
            "allAccountType": false,
            "templateId": 1,
            "accountTypes": [],
            "metaFields": [
                {
                    "id": 94,
                    "entityId": 1,
                    "name": "cc.cardholder.name",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TYPE",
                    "dataType": "STRING",
                    "disabled": false,
                    "mandatory": true,
                    "displayOrder": 1,
                    "dependentMetaFields": [],
                    "dataTableId": null,
                    "helpDescription": null,
                    "helpContentURL": null,
                    "defaultValue": null,
                    "validationRule": null,
                    "primary": true,
                    "fieldUsage": "TITLE",
                    "filename": null
                },
                {
                    "id": 95,
                    "entityId": 1,
                    "name": "cc.number",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TYPE",
                    "dataType": "STRING",
                    "disabled": false,
                    "mandatory": true,
                    "displayOrder": 2,
                    "dependentMetaFields": [],
                    "dataTableId": null,
                    "helpDescription": null,
                    "helpContentURL": null,
                    "defaultValue": null,
                    "validationRule": {
                    "id": 9,
                    "ruleType": "PAYMENT_CARD",
                    "ruleAttributes": {},
                    "errorMessages": [
                            {
                                "psudoColumn": "errorMessage",
                                "languageId": 1,
                                "content": "Payment card number is not valid",
                                "deleted": false,
                                "label": "errorMessage"
                            }
                    ],
                    "enabled": true
                },
                    "primary": true,
                    "fieldUsage": "PAYMENT_CARD_NUMBER",
                    "filename": null
                },
                {
                    "id": 96,
                    "entityId": 1,
                    "name": "cc.expiry.date",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TYPE",
                    "dataType": "STRING",
                    "disabled": false,
                    "mandatory": true,
                    "displayOrder": 3,
                    "dependentMetaFields": [],
                    "dataTableId": null,
                    "helpDescription": null,
                    "helpContentURL": null,
                    "defaultValue": null,
                    "validationRule": {
                    "id": 10,
                    "ruleType": "REGEX",
                    "ruleAttributes": {
                        "regularExpression": "(?:0[1-9]|1[0-2])/[0-9]{4}"
                    },
                    "errorMessages": [
                            {
                                "psudoColumn": "errorMessage",
                                "languageId": 1,
                                "content": "Expiry date should be in format MM/yyyy",
                                "deleted": false,
                                "label": "errorMessage"
                            }
                    ],
                    "enabled": true
                },
                    "primary": true,
                    "fieldUsage": "DATE",
                    "filename": null
                },
                {
                    "id": 97,
                    "entityId": 1,
                    "name": "cc.gateway.key",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TYPE",
                    "dataType": "STRING",
                    "disabled": true,
                    "mandatory": false,
                    "displayOrder": 4,
                    "dependentMetaFields": [],
                    "dataTableId": null,
                    "helpDescription": null,
                    "helpContentURL": null,
                    "defaultValue": null,
                    "validationRule": null,
                    "primary": true,
                    "fieldUsage": "GATEWAY_KEY",
                    "filename": null
                },
                {
                    "id": 98,
                    "entityId": 1,
                    "name": "cc.type",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TYPE",
                    "dataType": "INTEGER",
                    "disabled": true,
                    "mandatory": false,
                    "displayOrder": 5,
                    "dependentMetaFields": [],
                    "dataTableId": null,
                    "helpDescription": null,
                    "helpContentURL": null,
                    "defaultValue": null,
                    "validationRule": null,
                    "primary": true,
                    "fieldUsage": "CC_TYPE",
                    "filename": null
                }
        ]}"""
    }
}