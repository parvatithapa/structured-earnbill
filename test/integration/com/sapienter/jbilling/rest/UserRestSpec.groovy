package com.sapienter.jbilling.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.user.MainSubscriptionWS
import com.sapienter.jbilling.server.user.UserWS
import org.apache.http.HttpStatus
import javax.ws.rs.core.Response
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

class UserRestSpec extends RestBaseSpec {

    def userResource

    def setup() {
        init(userResource, 'users')
    }

    void "get existing user"() {

        given: 'The JSON of the user that needs to be fetched.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)
        def user = buildUserMock(userId.intValue(), username)

        and: 'Mock the behaviour of the getUserWS, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, userJson)
    }

    void "get existing user resulted with internal error"() {

        given: 'The JSON of the user that needs to be fetched.'
        def userId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getUserWS, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to fetch non existing user"() {

        given: 'Just the id of the non existing user'
        def userId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to create invalid user"(){

        given: 'The JSON of the user that needs to be created.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)

        and: 'Mock the behaviour of the createUser, and verify the number of calls.'
        1 * webServicesSessionMock.createUser(_ as UserWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), userJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "creation of user resulted with internal error"(){

        given: 'The JSON of the user that needs to be created.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)

        and: 'Mock the behaviour of the createUser, and verify the number of calls.'
        1 * webServicesSessionMock.createUser(_ as UserWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), userJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "create user"(){

        given: 'The JSON of the user that needs to be created.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)
        def user = buildUserMock(userId.intValue(), username)

        and: 'Mock the behaviour of the createUser and getUserWS, and verify the number of calls.'
        1 * webServicesSessionMock.createUser(_ as UserWS) >> userId
        1 * webServicesSessionMock.getUserWS(userId) >> user
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), userJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${userId}#")
        RestApiHelper.validateResponseJson(response, userJson)
    }

    void "try to update non existing user"(){

        given: 'The JSON of the user that needs to be updated.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)

        and: 'Mock the behaviour of the getUserWS, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), userJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to update user with invalid data"(){

        given: 'The JSON of the user that needs to be updated.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)
        def user = buildUserMock(userId.intValue(), username)

        and: 'Mock the behaviour of the getUserWS and updateUser, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.updateUser(_ as UserWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), userJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "update on user resulted with internal error"(){

        given: 'The JSON of the user that needs to be updated.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)
        def user = buildUserMock(userId.intValue(), username)

        and: 'Mock the behaviour of the getUserWS and updateUser, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.updateUser(_ as UserWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), userJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "update user"(){

        given: 'The JSON of the user that needs to be updated.'
        def userId = Integer.valueOf(1)
        def username = 'TestCustomer'
        def userJson = buildUserJSONString(userId.intValue(), username)
        def user = buildUserMock(userId.intValue(), username)

        and: 'Mock the behaviour of the getUserWS and updateUser, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> new UserWS()
        1 * webServicesSessionMock.updateUser(_ as UserWS)
        1 * webServicesSessionMock.getUserWS(userId) >> user
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), userJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, userJson)
    }

    void "try to delete user that do not exist"(){

        given: 'The id of the user that do not exist'
        def userId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getUserWS, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "try to delete user that can not be deleted"(){

        given: 'The id of the user that needs to be deleted.'
        def userId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getUserWS and deleteUser, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> new UserWS()
        1 * webServicesSessionMock.deleteUser(userId) >> {throw new SessionInternalError("Test", HttpStatus.SC_CONFLICT)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_CONFLICT, '', '')
    }

    void "deleting user resulted with internal error"(){

        given: 'The id of the user that needs to be deleted.'
        def userId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getUserWS and deleteUser, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> new UserWS()
        1 * webServicesSessionMock.deleteUser(userId) >> {throw new SessionInternalError("Test", HttpStatus.SC_CONFLICT)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_CONFLICT, '', '')
    }

    void "delete user"(){

        given: 'The id of the user that needs to be deleted.'
        def userId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getUserWS and deleteUser, and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> new UserWS()
        1 * webServicesSessionMock.deleteUser(userId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    void "get invoices generated for a user"() {

        given: 'The JSON of the invoices that need to be fetched.'
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def invoicesJson = InvoiceRestSpec.buildInvoicesJSON(1, 2, 3, 4, 5)
        def invoices = buildInvoicesMock(1, 2, 3, 4, 5)

        and: 'Mock the behaviour of getUserWS and getAllInvoicesForUser and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getAllInvoicesForUser(userId) >> invoices
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, invoicesJson)
    }

    void "get invoices generated for a user with pagination"() {

        given: 'The JSON of the invoices that need to be fetched.'
        def limit = Integer.valueOf(5)
        def offset = Integer.valueOf(10)
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def invoicesJson = InvoiceRestSpec.buildInvoicesJSON(1, 2, 3, 4, 5)
        def invoices = buildInvoicesMock(1, 2, 3, 4, 5)

        and: 'Mock the behaviour of getUserWS and getUserInvoicesPage and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getUserInvoicesPage(userId, limit, offset) >> invoices
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices?limit=${limit}&offset=${offset}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, invoicesJson)
    }

    void "try to get invoices generated for a user with invalid pagination parameters"() {

        given: 'The JSON of the invoices that need to be fetched.'
        def limit = Integer.valueOf(0)
        def offset = Integer.valueOf(-10)
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getUserWS and getUserInvoicesPage and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getUserInvoicesPage(userId, limit, offset) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices?limit=${limit}&offset=${offset}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "try to fetch generated invoices for a non-existing user id"() {

        given: 'The id of a non-existing user.'
        def userId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getUserWS and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching generated invoices for a user"() {

        given: 'The error message that will be received.'
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getUserWS and getAllInvoicesForUser and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getAllInvoicesForUser(userId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get last invoice generated for a user"() {

        given: 'The JSON of the invoice that needs to be fetched.'
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def invoiceJson = InvoiceRestSpec.buildInvoiceJSON(1)
        def invoice = buildInvoiceMock(invoiceJson)

        and: 'Mock the behaviour of getUserWS and getAllInvoicesForUser and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getLatestInvoice(userId) >> invoice
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices/last", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, invoiceJson)
    }

    void "get a number of the last invoices generated for a user"() {

        given: 'The JSON of the invoices that need to be fetched.'
        def number = Integer.valueOf(5)
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def invoiceIds = [1, 2, 3, 4, 5]
        def invoicesJson = InvoiceRestSpec.buildInvoicesJSON(1, 2, 3, 4, 5)
        def invoices = buildInvoicesMock(1, 2, 3, 4, 5)

        and: 'Mock the behaviour of getUserWS and getLastInvoices and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getLastInvoices(userId, number) >> invoiceIds
        1 * webServicesSessionMock.getInvoiceWS(1) >> invoices[0]
        1 * webServicesSessionMock.getInvoiceWS(2) >> invoices[1]
        1 * webServicesSessionMock.getInvoiceWS(3) >> invoices[2]
        1 * webServicesSessionMock.getInvoiceWS(4) >> invoices[3]
        1 * webServicesSessionMock.getInvoiceWS(5) >> invoices[4]
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices/last?number=${number}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, invoicesJson)
    }

    void "try to get an invalid number of the last invoices generated for a user"() {

        given: 'The error message that will be received.'
        def number = Integer.valueOf(-5)
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getUserWS and getLastInvoices and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getLastInvoices(userId, number) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices/last?number=${number}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage, '')
    }

    void "try to fetch the last invoice for a non-existing user id"() {

        given: 'The id of a non-existing user.'
        def userId = Integer.MAX_VALUE
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getUserWS and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_NOT_FOUND) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices/last", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
    }

    void "failure while fetching the last invoice for a user"() {

        given: 'The error message that will be received.'
        def userId = Integer.valueOf(2)
        def username = 'TestCustomer'
        def user = buildUserMock(userId.intValue(), username)
        def errorMessage = 'Test Error Message'

        and: 'Mock the behaviour of getUserWS and getLatestInvoice and verify the number of calls.'
        1 * webServicesSessionMock.getUserWS(userId) >> user
        1 * webServicesSessionMock.getLatestInvoice(userId) >>
                { throw new SessionInternalError("Test", [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR) }
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${userId}/invoices/last", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage, '')
    }

    void "get last payments for userId"() {

        given: 'The JSON array of the payments that needs to be fetched.'
        def userId = 2
        def numberOfLastPayments = 3
        def paymentJson = BuilderHelper.buildPaymentJsonStringArray(100, userId, 101, 102)
        def payments = BuilderHelper.buildWSMocks(paymentJson, PaymentWS)

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getLastPaymentsWS(userId, numberOfLastPayments) >> payments
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${getBaseUrlForUser(userId)}/last?number=${numberOfLastPayments}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, paymentJson)
    }

    void "get last payment for non existing user"() {

        given: 'Just the id of the non user'
        def userId = Integer.MAX_VALUE
        def numberOfLastPayments = 3

        and: 'Mock the behaviour of the getPaymentMethodTemplate, and verify the number of calls.'
        1 * webServicesSessionMock.getLastPaymentsWS(userId, numberOfLastPayments) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${getBaseUrlForUser(userId)}/last?number=${numberOfLastPayments}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "get last payment for user resulted with internal error"() {

        given: 'Just the id of a existing user'
        def userId = Integer.valueOf(1)
        def numberOfLastPayments = 3

        and: 'Mock the behaviour of the getPaymentMethodTemplate, and verify the number of calls.'
        1 * webServicesSessionMock.getLastPaymentsWS(userId, numberOfLastPayments) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${getBaseUrlForUser(userId)}/last?number=${numberOfLastPayments}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "get paged payments for userId"() {

        given: 'The JSON of the payment that needs to be fetched.'
        def userId = 2
        def numberOfPayments = 1
        def offset = 0
        def paymentsList = [6]
        String expectedJsonListOfPayments="[6]"

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getLastPaymentsPage(userId, numberOfPayments, offset) >> paymentsList
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${getBaseUrlForUser(userId)}?limit=${numberOfPayments}&offset=${offset}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, expectedJsonListOfPayments)
    }

    void "get paged payments for non existing user"() {

        given: 'The JSON of the payment that needs to be fetched.'
        def userId = 2
        def numberOfPayments = 1
        def offset = 0

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getLastPaymentsPage(userId, numberOfPayments, offset) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${getBaseUrlForUser(userId)}?limit=${numberOfPayments}&offset=${offset}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "get paged payments for existing user invalid"() {

        given: 'The JSON of the payment that needs to be fetched.'
        def userId = 2
        def numberOfPayments = 1
        def offset = 0

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getLastPaymentsPage(userId, numberOfPayments, offset) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${getBaseUrlForUser(userId)}?limit=${numberOfPayments}&offset=${offset}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
    }

    void "get paged payments for existing user resulted with internal error"() {

        given: 'The JSON of the payment that needs to be fetched.'
        def userId = 2
        def numberOfPayments = 1
        def offset = 0

        and: 'Mock the behaviour of the getPayment, and verify the number of calls.'
        1 * webServicesSessionMock.getLastPaymentsPage(userId, numberOfPayments, offset) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${getBaseUrlForUser(userId)}?limit=${numberOfPayments}&offset=${offset}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    private String getBaseUrlForUser(int userId) {
        "${BASE_URL}/${userId}/payments"
    }

    private static buildUserJSONString(id, username){
        """
            {
              "id": ${id},                                                           \
              "currencyId": 1,
              "password": "P@ssw0rd1",
              "deleted": 0,
              "userName": "${username}",
              "failedAttempts": 0,
              "languageId": 1,
              "mainRoleId": 5,
              "accountTypeId": 1,
              "lowBalanceThreshold": -1,
              "statusId": 1,
              "entityId": 1,
              "invoiceTemplateId": 1,
              "createCredentials":false,
              "accountExpired":false,
              "rechargeThreshold":-1,
              "mainSubscription": ${BuilderHelper.buildMainSubscriptionJson()},
              "accountInfoTypeFieldsMap":{},
              "timelineDatesMap":{},
              "effectiveDateMap":{},
              "removedDatesMap":{},
              "paymentInstruments":[]
            }
        """
    }

    public static buildUserMock(id, username) {

        UserWS user = new UserWS();
        user.id = id
        user.currencyId = Integer.valueOf(1) // USD
        user.password = 'P@ssw0rd1'
        user.userName = username
        user.languageId = Integer.valueOf(1) // US
        user.mainRoleId = Integer.valueOf(5) // Role Customer
        user.accountTypeId = Integer.valueOf(1)
        user.statusId = Integer.valueOf(1) // Status active
        user.entityId = Integer.valueOf(1)
        user.invoiceTemplateId = Integer.valueOf(1)
        user.mainSubscription = new MainSubscriptionWS(Integer.valueOf(1), Integer.valueOf(1))

        user
    }

    InvoiceWS buildInvoiceMock(String json) {

        return BuilderHelper.mapper.readValue(json, InvoiceWS.class)
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
