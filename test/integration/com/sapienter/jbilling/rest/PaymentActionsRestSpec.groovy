package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx
import com.sapienter.jbilling.server.payment.PaymentWS
import org.apache.commons.httpclient.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Martin Kostovski
 * @author Vojislav Stanojevikj
 * @since 9/19/2016.
 */
class PaymentActionsRestSpec extends RestBaseSpec{

    def paymentActionsResource

    def setup() {
        init(paymentActionsResource, 'payments/invoices/')
    }

    void "apply payment"() {

        given: 'The JSON of the payment that needs to be applied to an invoice.'
        def invoiceId = 6
        def paymentId = 100
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.applyPayment(_ as PaymentWS,invoiceId) >> paymentId
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/apply", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${getBasicPaymentUrl(invoiceId)+"/apply"}/${paymentId}#")
    }

    void "apply payment not successful"() {

        given: 'The JSON of the payment that needs to be applied to an invoice.'
        def invoiceId = 6
        def paymentId = 100
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.applyPayment(_ as PaymentWS,invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/apply", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '','')
    }

    void "apply payment resulted with internal error"() {

        given: 'The JSON of the payment that needs to be applied to an invoice.'
        def invoiceId = 6
        def paymentId = 100
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.applyPayment(_ as PaymentWS,invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/apply", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '','')
    }

    void "apply payment on non existing invoice"() {

        given: 'The JSON of the payment that needs to be applied to an invoice.'
        def invoiceId = 6
        def paymentId = 100
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.applyPayment(_ as PaymentWS,invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/apply", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '','')
    }

    void "pay invoice"() {

        given: 'Mock ids of invoice and auth feedback.'
        def invoiceId = 6
        def authId = 10

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.payInvoice(invoiceId) >> new PaymentAuthorizationDTOEx(id: authId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/pay", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${getBasicPaymentUrl(invoiceId)+"/pay"}/${authId}#")
    }

    void "pay invoice not successful"() {

        given: 'Mock id of invoice.'
        def invoiceId = 6

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.payInvoice(invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/pay", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '','')
    }

    void "try to pay invoice that is not found"() {

        given: 'Mock id of invoice.'
        def invoiceId = 6

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.payInvoice(invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/pay", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '','')
    }

    void "try to pay invoice resulted with internal error"() {

        given: 'Mock id of invoice.'
        def invoiceId = 6

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.payInvoice(invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/pay", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '','')
    }

    void "process payment"() {

        given: 'The JSON of the payment that needs to be processed.'
        def invoiceId = 6
        def authId = 10
        def paymentId = 100
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.processPayment(_ as PaymentWS, invoiceId) >> new PaymentAuthorizationDTOEx(id: authId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/process", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${getBasicPaymentUrl(invoiceId)+"/process"}/${authId}#")
    }

    void "process payment not successful"() {

        given: 'The JSON of the payment that needs to be processed.'
        def invoiceId = 10
        def paymentId = 6
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.processPayment(_ as PaymentWS,invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/process", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '','')
    }

    void "process payment for non existing invoice"() {

        given: 'The JSON of the payment that needs to be processed.'
        def invoiceId = 10
        def paymentId = 6
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.processPayment(_ as PaymentWS,invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/process", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '','')
    }

    void "process payment resulted with internal error"() {

        given: 'The JSON of the payment that needs to be processed.'
        def invoiceId = 10
        def paymentId = 6
        def paymentJson = BuilderHelper.buildPaymentJson(paymentId, 0)

        and: 'Mock the behaviour of applyPayment, and verify the number of calls.'
        1 * webServicesSessionMock.processPayment(_ as PaymentWS,invoiceId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(invoiceId)+"/process", HttpMethod.POST, RestApiHelper.buildJsonHeaders(), paymentJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '','')
    }

    private String getBasicPaymentUrl(int paymentId){
        return BASE_URL + paymentId
    }
}
