package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.user.AccountTypeWS
import com.sapienter.jbilling.server.user.MainSubscriptionWS
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import org.apache.http.HttpStatus
import javax.ws.rs.core.Response
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

class AccountTypeRestSpec extends RestBaseSpec {

    def accountTypeResource

    def setup() {
        init(accountTypeResource, 'accounttypes')
    }

    void "get all existing account types"(){

        given: 'The JSON of the account types that needs to be fetched.'
        def accountTypesJSON = buildAccountTypeJSONString(1, 2, 3, 4)
        def accountTypes = buildAccountTypeArrayMock(1, 2, 3, 4)

        and: 'Mock the behaviour of the getAllAccountTypes, and verify the number of calls.'
        1 * webServicesSessionMock.getAllAccountTypes() >> accountTypes
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, accountTypesJSON)
    }

    void "get all existing account types resulted with internal error"(){

        given: 'Mock the behaviour of the getAllAccountTypes, and verify the number of calls.'
        1 * webServicesSessionMock.getAllAccountTypes() >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "get existing account type"() {

        given: 'The JSON of the account type that needs to be fetched.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())
        def accountType = buildAccountTypeMock(accountTypeId.intValue())

        and: 'Mock the behaviour of the getAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> accountType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, accountTypeJson)
    }

    void "try to fetch non existing account type "() {

        given: 'Just the id of the non existing account type'
        def accountTypeId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "fetch existing account type resulted with internal error"() {

        given: 'Just the id of a existing account type'
        def accountTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to create invalid account type"(){

        given: 'The JSON of the account types that needs to be created.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())

        and: 'Mock the behaviour of the createAccountType, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createAccountType(_ as AccountTypeWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), accountTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "creation of account type resulted with internal error"(){

        given: 'The JSON of the account types that needs to be created.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())

        and: 'Mock the behaviour of the createAccountType, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createAccountType(_ as AccountTypeWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), accountTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,MediaType.APPLICATION_JSON)
    }

    void "create account type"(){

        given: 'The JSON of the account types that needs to be created.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())
        def accountType = buildAccountTypeMock(accountTypeId.intValue())

        and: 'Mock the behaviour of the createAccountType and getAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.createAccountType(_ as AccountTypeWS) >> accountTypeId
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> accountType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), accountTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${accountTypeId}#")
        RestApiHelper.validateResponseJson(response, accountTypeJson)
    }

    void "try to update non existing account type"(){

        given: 'The JSON of the account types that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())

        and: 'Mock the behaviour of the getAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), accountTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to update account type with invalid data"(){

        given: 'The JSON of the account types that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())
        def accountType = buildAccountTypeMock(accountTypeId.intValue())

        and: 'Mock the behaviour of the getAccountType and updateAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> accountType
        1 * webServicesSessionMock.updateAccountType(_ as AccountTypeWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), accountTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update of account type resulted with internal error"(){

        given: 'The JSON of the account types that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())
        def accountType = buildAccountTypeMock(accountTypeId.intValue())

        and: 'Mock the behaviour of the getAccountType and updateAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> accountType
        1 * webServicesSessionMock.updateAccountType(_ as AccountTypeWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), accountTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update account type"(){

        given: 'The JSON of the account types that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def accountTypeJson = buildAccountTypeJSONString(accountTypeId.intValue())
        def accountType = buildAccountTypeMock(accountTypeId.intValue())

        and: 'Mock the behaviour of the getAccountType and updateAccountType, and verify the number of calls.'
        2 * webServicesSessionMock.getAccountType(accountTypeId) >> accountType
        1 * webServicesSessionMock.updateAccountType(_ as AccountTypeWS) >> true
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), accountTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, accountTypeJson)
    }

    void "try to delete account type that do not exist"(){

        given: 'The id of the account type that do not exist'
        def accountTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
    }

    void "try to delete account type that can not be deleted"(){

        given: 'The id of the account types that needs to be deleted.'
        def accountTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAccountType and deleteAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        1 * webServicesSessionMock.deleteAccountType(accountTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_CONFLICT)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_CONFLICT, '', '')
    }

    void "delete of account type resulted with internal error"(){

        given: 'The id of the account types that needs to be deleted.'
        def accountTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAccountType and deleteAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        1 * webServicesSessionMock.deleteAccountType(accountTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "delete account type"(){

        given: 'The id of the account types that needs to be deleted.'
        def accountTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAccountType and deleteAccountType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        1 * webServicesSessionMock.deleteAccountType(accountTypeId) >> true
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    private static buildAccountTypeJSONString(int id){

        """
            {
              "id": ${id},
              "entityId": 1,
              "invoiceTemplateId": 1,
              "dateCreated": ${RestApiHelper.DUMMY_TEST_DATE.time},
              "invoiceDeliveryMethodId": 1,
              "invoiceDesign": null,
              "currencyId": 1,
              "languageId": 1,
              "descriptions": [${BuilderHelper.buildInternationalDescriptionJson('TestDesc', 'description')}],
              "mainSubscription": ${BuilderHelper.buildMainSubscriptionJson()},
              "preferredNotificationAitId": 0,
              "creditLimit": 0,
              "creditNotificationLimit1": 0,
              "creditNotificationLimit2": 0,
              "informationTypeIds": [],
              "paymentMethodTypeIds": []
            }
        """

    }

    private static buildAccountTypeJSONString(int id, int... ids){

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildAccountTypeJSONString(id))
        ids.each {
            stringBuilder.append(',')
            stringBuilder.append(buildAccountTypeJSONString(it))
        }
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    private static buildAccountTypeMock(int id){

        AccountTypeWS accountType = new AccountTypeWS(id, '')
        accountType.entityId = Integer.valueOf(1)
        accountType.invoiceTemplateId = Integer.valueOf(1)
        accountType.dateCreated = RestApiHelper.DUMMY_TEST_DATE
        accountType.invoiceDeliveryMethodId = Integer.valueOf(1)
        accountType.currencyId = Integer.valueOf(1)
        accountType.languageId = Integer.valueOf(1)
        accountType.addDescription(new InternationalDescriptionWS('description', Integer.valueOf(1), 'TestDesc'))
        accountType.mainSubscription = new MainSubscriptionWS(Integer.valueOf(1), Integer.valueOf(1))
        accountType.setpreferredNotificationAitId(Integer.valueOf(0))
        accountType.creditLimitAsDecimal = BigDecimal.ZERO
        accountType.creditNotificationLimit1AsDecimal = BigDecimal.ZERO
        accountType.creditNotificationLimit2AsDecimal = BigDecimal.ZERO
        accountType.informationTypeIds = new Integer[0]
        accountType.paymentMethodTypeIds = new Integer[0]

        return accountType

    }

    private static buildAccountTypeArrayMock(int id, int... ids){

        def accountTypes = []
        accountTypes.add(buildAccountTypeMock(id))
        ids.each {
            accountTypes.add(buildAccountTypeMock(it))
        }
        return accountTypes.toArray(new AccountTypeWS[accountTypes.size()])
    }
}
