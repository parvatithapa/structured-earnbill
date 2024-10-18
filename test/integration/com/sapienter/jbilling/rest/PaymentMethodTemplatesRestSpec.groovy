package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Martin Kostovski
 * @author Vojislav Stanojevikj
 * @since 9/15/2016
 */
class PaymentMethodTemplatesRestSpec extends RestBaseSpec {

    def paymentMethodTemplatesResource

    def setup() {
        init(paymentMethodTemplatesResource, 'paymentMethodTemplates')
    }

    void "get existing payment method template"() {

        given: 'The JSON of the payment method template that needs to be fetched.'
        def paymentMethodTemplateId = 1
        def paymentMethodTemplateJson = buildPaymentMethodTemplateJSON(paymentMethodTemplateId)
        def paymentMethodTemplate = BuilderHelper.buildWSMock(paymentMethodTemplateJson, PaymentMethodTemplateWS)

        and: 'Mock the behaviour of the getPaymentMethodTemplate, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodTemplate(paymentMethodTemplateId) >> paymentMethodTemplate
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTemplateId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, paymentMethodTemplateJson)
    }

    void "try to fetch non existing payment method template"() {

        given: 'Just the id of the non existing payment method template'
        def paymentMethodTemplateId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getPaymentMethodTemplate, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodTemplate(paymentMethodTemplateId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTemplateId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText()==''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "fetch existing payment method template resulted with internal error"() {

        given: 'Just the id of the non existing payment method template'
        def paymentMethodTemplateId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getPaymentMethodTemplate, and verify the number of calls.'
        1 * webServicesSessionMock.getPaymentMethodTemplate(paymentMethodTemplateId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentMethodTemplateId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    private buildPaymentMethodTemplateJSON(int id) {
        """{
            "id": ${id},
            "templateName": "Payment Card",
            "metaFields":
            [
                {
                    "id": 82,
                    "entityId": 1,
                    "name": "cc.expiry.date",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TEMPLATE",
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
                    "id": 7,
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
                    "id": 84,
                    "entityId": 1,
                    "name": "cc.type",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TEMPLATE",
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
                },
                {
                    "id": 80,
                    "entityId": 1,
                    "name": "cc.cardholder.name",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TEMPLATE",
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
                    "id": 81,
                    "entityId": 1,
                    "name": "cc.number",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TEMPLATE",
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
                    "id": 6,
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
                    "id": 83,
                    "entityId": 1,
                    "name": "cc.gateway.key",
                    "fakeId": null,
                    "entityType": "PAYMENT_METHOD_TEMPLATE",
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
                }
            ]
        }"""
    }

}
