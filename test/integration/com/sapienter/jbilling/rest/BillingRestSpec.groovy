package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS
import com.sapienter.jbilling.server.process.BillingProcessWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.test.mixin.integration.Integration
import org.apache.http.HttpStatus
import org.grails.jaxrs.itest.IntegrationTestSpec

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Bojan Dikovski
 * @since 13-OCT-2016
 */
class BillingRestSpec extends IntegrationTestSpec {

    private static final String BASE_URL = 'api/billing'

    def webServicesSessionMock
    def billingResource
    ObjectMapper mapper

    def setup() {
        webServicesSessionMock = Mock(IWebServicesSessionBean)
        billingResource.webServicesSession = webServicesSessionMock
        mapper = new ObjectMapper()
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        grailsApplication.config.org.grails.jaxrs.doreader.disable = true
        grailsApplication.config.org.grails.jaxrs.dowriter.disable = true
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

    void "get the current billing process configuration"() {

        given: 'The JSON of the billing process configuration that needs to be fetched.'
        def billingConfigId = Integer.valueOf(1)
        def billingConfigJson = buildBillingConfigJSON(billingConfigId)
        def billingConfig = buildBillingConfigMock(billingConfigJson)

        and: 'Mock the behaviour of getBillingProcessConfiguration and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcessConfiguration() >> billingConfig
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/configuration", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, billingConfigJson)
    }

    void "failure while fetching the billing process configuration"() {

        given: 'The error message that will be received.'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getBillingProcessConfiguration and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcessConfiguration() >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/configuration", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "update the billing process configuration"() {

        given: 'The JSON of the billing process configuration that needs to be updated.'
        def billingConfigId = Integer.valueOf(1)
        def billingConfigJson = buildBillingConfigJSON(billingConfigId)
        def billingConfig = buildBillingConfigMock(billingConfigJson)

        and: 'Mock the behaviour of createUpdateBillingProcessConfiguration and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateBillingProcessConfiguration(_ as BillingProcessConfigurationWS)
        1 * webServicesSessionMock.getBillingProcessConfiguration() >> billingConfig
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/configuration", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), billingConfigJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, billingConfigJson)
    }

    void "failure while updating the billing process configuration"() {

        given: 'The JSON of the meta field that needs to be updated.'
        def billingConfigId = Integer.valueOf(1)
        def billingConfigJson = buildBillingConfigJSON(billingConfigId)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of createUpdateBillingProcessConfiguration and verify the number of calls.'
        1 * webServicesSessionMock.createUpdateBillingProcessConfiguration(_ as BillingProcessConfigurationWS) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/configuration", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(), billingConfigJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get an existing billing process by the id"() {

        given: 'The JSON of the billing process that needs to be fetched.'
        def billingProcessId = Integer.valueOf(1)
        def billingProcessJson = buildBillingProcessJSON(billingProcessId)
        def billingProcess = buildBillingProcessMock(billingProcessJson)

        and: 'Mock the behaviour of getBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcess(billingProcessId) >> billingProcess
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${billingProcessId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, billingProcessJson)
    }

    void "get a billing process with non-existing id"() {

        given: 'The id of a non-existing billing process.'
        def billingProcessId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcess(billingProcessId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${billingProcessId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching the billing process"() {

        given: 'The error message that will be received.'
        def billingProcessId = Integer.valueOf(1)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcess(billingProcessId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${billingProcessId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get the last billing process"() {

        given: 'The JSON of the billing process that needs to be fetched.'
        def billingProcessId = Integer.valueOf(1)
        def billingProcessJson = buildBillingProcessJSON(billingProcessId)
        def billingProcess = buildBillingProcessMock(billingProcessJson)

        and: 'Mock the behaviour of getLastBillingProcess and getBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getLastBillingProcess() >> billingProcessId
        1 * webServicesSessionMock.getBillingProcess(billingProcessId) >> billingProcess
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/last", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, billingProcessJson)
    }

    void "try to get the last billing process when one does not exist"() {

        given: 'The error message that will be received.'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getLastBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getLastBillingProcess() >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/last", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching the last billing process"() {

        given: 'The error message that will be received.'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getLastBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getLastBillingProcess() >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/last", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get the review billing process"() {

        given: 'The JSON of the billing process that needs to be fetched.'
        def billingProcessId = Integer.valueOf(1)
        def billingProcessJson = buildBillingProcessJSON(billingProcessId)
        def billingProcess = buildBillingProcessMock(billingProcessJson)

        and: 'Mock the behaviour of getReviewBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getReviewBillingProcess() >> billingProcess
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/review", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, billingProcessJson)
    }

    void "try to get the review billing process when one does not exist"() {

        given: 'The error message that will be received.'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getReviewBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getReviewBillingProcess() >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/review", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching the review billing process"() {

        given: 'The error message that will be received.'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getReviewBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getReviewBillingProcess() >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/review", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "start a billing process run"() {

        given: 'The run date for the billing process run.'
        def runDateLong = 1159221600000L
        def runDate = new Date(runDateLong)

        and: 'Mock the behaviour of the triggerBilling and verify the number of calls.'
        1 * webServicesSessionMock.triggerBilling(runDate)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${runDateLong}", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "failure while starting a billing process run"() {

        given: 'The run date for the billing process run.'
        def runDateLong = 1159221600000L
        def runDate = new Date(runDateLong)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the createUser, and verify the number of calls.'
        1 * webServicesSessionMock.triggerBilling(runDate) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${runDateLong}", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "start a billing process run in async mode"() {

        given: 'The run date for the billing process run.'
        def runDateLong = 1159221600000L
        def runDate = new Date(runDateLong)

        and: 'Mock the behaviour of the triggerBilling and verify the number of calls.'
        1 * webServicesSessionMock.triggerBillingAsync(runDate)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${runDateLong}?async=true", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "failure while starting a billing process run in async mode"() {

        given: 'The run date for the billing process run.'
        def runDateLong = 1159221600000L
        def runDate = new Date(runDateLong)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of the createUser, and verify the number of calls.'
        1 * webServicesSessionMock.triggerBillingAsync(runDate) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${runDateLong}?async=true", HttpMethod.POST, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "approve the review billing process"() {

        given: 'The review billing process run approval status.'
        def reviewApproved = true

        and: 'Mock the behaviour of setReviewApproval and verify the number of calls.'
        1 * webServicesSessionMock.setReviewApproval(reviewApproved)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/review?approval=${reviewApproved}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        response.getText() == ''
    }

    void "approve the review billing process with null value"() {

        given: 'The error message that will be received.'
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of setReviewApproval and verify the number of calls.'
        1 * webServicesSessionMock.setReviewApproval(null) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/review", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "failure while approving the review billing process"() {

        given: 'The error message that will be received.'
        def reviewApproved = true
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of setReviewApproval and verify the number of calls.'
        1 * webServicesSessionMock.setReviewApproval(reviewApproved) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/review?approval=${reviewApproved}", HttpMethod.PUT, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get invoices generated by a billing process"() {

        given: 'The JSON of the invoices that need to be fetched.'
        def billingProcessId = Integer.valueOf(12)
        def billingProcessJson = buildBillingProcessJSON(billingProcessId)
        def billingProcess = buildBillingProcessMock(billingProcessJson)
        def invoiceIds = [1]
        def invoicesJson = InvoiceRestSpec.buildInvoicesJSON(1)
        def invoices = buildInvoicesMock(1)

        and: 'Mock the behaviour of getBillingProcess, getBillingProcessGeneratedInvoices and getInvoiceWS, ' +
                'and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcess(billingProcessId) >> billingProcess
        1 * webServicesSessionMock.getBillingProcessGeneratedInvoices(billingProcessId) >> invoiceIds
        1 * webServicesSessionMock.getInvoiceWS(1) >> invoices[0]
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${billingProcessId}/invoices", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, invoicesJson)
    }

    void "try to fetch generated invoices by a non-existing billing process run"() {

        given: 'The id of a non-existing billing process.'
        def billingProcessId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getBillingProcess and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcess(billingProcessId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${billingProcessId}/invoices", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching the billing process generated invoices"() {

        given: 'The error message that will be received.'
        def billingProcessId = Integer.valueOf(1)
        def billingProcessJson = buildBillingProcessJSON(billingProcessId)
        def billingProcess = buildBillingProcessMock(billingProcessJson)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getBillingProcess and getBillingProcessGeneratedInvoices ' +
                'and verify the number of calls.'
        1 * webServicesSessionMock.getBillingProcess(billingProcessId) >> billingProcess
        1 * webServicesSessionMock.getBillingProcessGeneratedInvoices(billingProcessId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/processes/${billingProcessId}/invoices", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    static String buildBillingConfigJSON(Integer id) {

        return """{
            "id": ${id},
            "periodUnitId": 1,
            "entityId": 1,
            "nextRunDate": 1161813600000,
            "generateReport": 1,
            "retries": null,
            "daysForReport": 3,
            "reviewStatus": 1,
            "dueDateUnitId": 1,
            "dueDateValue": 1,
            "applyCreditNotesBeforePayments": 1,
            "autoCreditNoteApplication": 1,
            "autoPayment": 1,
            "dfFm": 0,
            "onlyRecurring": 1,
            "invoiceDateProcess": 0,
            "maximumPeriods": 1,
            "autoPaymentApplication": 1,
            "lastDayOfMonth": false,
            "proratingType": "PRORATING_AUTO_OFF"
        }"""
    }

    static String buildBillingProcessJSON(Integer id) {

        return """{
            "id": ${id},
            "entityId": 1,
            "periodUnitId": 1,
            "periodValue": 1,
            "billingDate": 1159221600000,
            "billingDateEnd": 1161813600000,
            "retries": 1,
            "retriesToDo": 0,
            "invoiceIds": [],
            "orderProcesses": [],
            "processRuns": [{
                "id": 12,
                "billingProcessId": null,
                "runDate": 1159221600000,
                "started": 1166541000116,
                "finished": 1166541002870,
                "invoicesGenerated": 2,
                "paymentFinished": null,
                "processRunTotals": [{
                    "id": 11,
                    "processRunId": null,
                    "currencyId": 1,
                    "totalInvoiced": 83,
                    "totalPaid": 0,
                    "totalNotPaid": 0
                },
                {
                    "id": -1,
                    "processRunId": 12,
                    "currencyId": 1,
                    "totalInvoiced": 95,
                    "totalPaid": 95,
                    "totalNotPaid": 0
                }],
                "statusId": null,
                "statusStr": "Finished:successful"
            }],
            "review": 0
        }"""
    }

    BillingProcessConfigurationWS buildBillingConfigMock(String json) {

        return mapper.readValue(json, BillingProcessConfigurationWS.class)
    }

    BillingProcessWS buildBillingProcessMock(String json) {

        return mapper.readValue(json, BillingProcessWS.class)
    }

    InvoiceWS buildInvoiceMock(String json) {

        return mapper.readValue(json, InvoiceWS.class)
    }

    InvoiceWS[] buildInvoicesMock(int id, int... ids) {

        def invoices = []
        invoices.add(buildInvoiceMock(InvoiceRestSpec.buildInvoiceJSON(id)))
        ids.each {
            invoices.add(buildInvoiceMock(InvoiceRestSpec.buildInvoiceJSON(it)))
        }
        return invoices.toArray(new InvoiceWS[invoices.size()])
    }
}
