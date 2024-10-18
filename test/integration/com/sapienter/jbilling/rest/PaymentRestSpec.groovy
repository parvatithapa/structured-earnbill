package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.payment.PaymentWS
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Martin Kostovski
 * @author Vojislav Stanojevikj
 * @since 9/15/2016.
 */

class PaymentRestSpec extends RestBaseSpec {

    def paymentResource

    def setup() {
        init(paymentResource, 'payments')
    }

    void "get existing payment"() {

        given: 'The JSON of the payment that needs to be fetched.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)
        def payment = BuilderHelper.buildWSMock(paymentJson, PaymentWS)

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getPayment(paymentId) >> payment
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, paymentJson)
    }

    void "try to fetch non existing payment"() {

        given: 'Just the id of the non existing payment'
        def paymentId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getPayment(paymentId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText()==''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "fetch on existing payment resulted with internal error"() {

        given: 'Just the id of a existing payment'
        def paymentId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getPayment(paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "try to create invalid payment"() {

        given: 'The JSON of the payment that needs to be created.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of the createPayment, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createPayment(_ as PaymentWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "creation of new payment resulted with internal error"() {

        given: 'The JSON of the payment that needs to be created.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of the createPayment, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createPayment(_ as PaymentWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create new payment"() {

        given: 'The JSON of the payments that needs to be created.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)
        def payment = BuilderHelper.buildWSMock(paymentJson, PaymentWS)

        and: 'Mock the behaviour of the createPayment and getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.createPayment(_ as PaymentWS) >> paymentId
        1 * webServicesSessionMock.getPayment(paymentId) >> payment
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${paymentId}#")
        RestApiHelper.validateResponseJson(response, paymentJson)
    }

    void "try to update non existing payment"() {

        given: 'The JSON of the payment that needs to be updated.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.updatePayment(_ as PaymentWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '','')
    }

    void "try to update payment with invalid data"() {

        given: 'The JSON of the payment that needs to be updated.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.updatePayment(_ as PaymentWS) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '','')
    }

    void "update on existing payment resulted with internal error"() {

        given: 'The JSON of the payment that needs to be updated.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.updatePayment(_ as PaymentWS) >>
                { throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '','')
    }

    void "update payment with valid data"() {

        given: 'The JSON of the payment that needs to be updated.'
        def paymentId = 1
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)
        def payment = BuilderHelper.buildWSMock(paymentJson, PaymentWS)

        and: 'Mock the behaviour of updatePayment and getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.updatePayment(_ as PaymentWS)
        1 * webServicesSessionMock.getPayment(paymentId) >> payment
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, paymentJson)
    }

    void "try to delete payment that does not exist"() {

        given: 'The id of the payment that do not exist'
        def paymentId = 1

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.deletePayment(paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '','')
    }

    void "deletion of payment resulted with internal error"() {

        given: 'The id of the payment that do not exist'
        def paymentId = 1

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.deletePayment(paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '','')
    }

    void "delete payment"() {

        given: 'The id of the payments that needs to be deleted.'
        def paymentId = 1

        and: 'Mock the behaviour of the getPayment and deletePayment.'
        1 * webServicesSessionMock.deletePayment(paymentId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${paymentId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }
}