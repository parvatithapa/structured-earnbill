package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
/**
 * @author Martin Kostovski
 * @author Vojislav Stanojevikj
 * @since 9/16/2016.
 */
class PaymentLinksRestSpec extends RestBaseSpec {

    def paymentLinksResource

    def setup() {
        init(paymentLinksResource, 'payments/')
    }

    void "remove payment link"() {

        given: 'PaymentId and InvoiceId'
        def paymentId =6
        def invoiceId=35

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removePaymentLink(invoiceId,paymentId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/"+invoiceId, HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "remove payment link for non existing link"() {

        given: 'PaymentId and InvoiceId'
        def paymentId =6
        def invoiceId=35

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removePaymentLink(invoiceId,paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/"+invoiceId, HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "try remove payment link for a link that can not be deleted"() {

        given: 'PaymentId and InvoiceId'
        def paymentId =6
        def invoiceId=35

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removePaymentLink(invoiceId,paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_CONFLICT)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/"+invoiceId, HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_CONFLICT, '', '')
    }

    void "remove payment link resulted with internal error"() {

        given: 'PaymentId and InvoiceId'
        def paymentId =6
        def invoiceId=35

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removePaymentLink(invoiceId,paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/"+invoiceId, HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "remove all payment links"() {

        given: 'PaymentId'
        def paymentId =6

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removeAllPaymentLinks(paymentId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "remove all payment links for non existing payment"() {

        given: 'PaymentId'
        def paymentId =6

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removeAllPaymentLinks(paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "try remove all payment links for a payment failed"() {

        given: 'PaymentId'
        def paymentId =6

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removeAllPaymentLinks(paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_CONFLICT)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_CONFLICT, '', '')
    }

    void "remove all payment links for a payment resulted with internal error"() {

        given: 'PaymentId'
        def paymentId =6

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.removeAllPaymentLinks(paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "create payment link"() {

        given: 'PaymentId and InvoiceId'
        def paymentId =6
        def invoiceId=35

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.createPaymentLink(invoiceId,paymentId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/"+invoiceId, HttpMethod.POST)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "create payment link invalid"() {

        given: 'PaymentId and InvoiceId'
        def paymentId =6
        def invoiceId=35

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.createPaymentLink(invoiceId,paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/"+invoiceId, HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
    }

    void "create payment link resulted with internal error"() {

        given: 'PaymentId and InvoiceId'
        def paymentId =6
        def invoiceId=35

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.createPaymentLink(invoiceId,paymentId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(getBasicPaymentUrl(paymentId)+"/"+invoiceId, HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    private String getBasicPaymentUrl(int paymentId){
        return BASE_URL + paymentId + '/invoices'
    }
}
