package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.user.AccountInformationTypeWS
import com.sapienter.jbilling.server.user.AccountTypeWS
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import org.apache.http.HttpStatus
import javax.ws.rs.core.Response
import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import java.time.LocalDateTime
import java.time.ZoneId


class AccountInformationTypeRestSpec extends RestBaseSpec {

    private static final Date TEST_DATE = Date.from(LocalDateTime.of(2010, 4, 7, 0, 0).atZone(ZoneId.systemDefault()).toInstant())
    def accountInformationTypeResource

    def setup() {
        init(accountInformationTypeResource, 'accounttypes')
    }

    void "get all existing AITs for a account type"(){

        given: 'The JSON of the AITs that needs to be fetched.'
        def accountTypeId = Integer.valueOf(1)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, 221, 321, 421)
        def aits = buildAccountInformationTypeMockArray(accountTypeId, 221, 321, 421)

        and: 'Mock the behaviour of the getAllAccountTypes, and verify the number of calls.'
        1 * webServicesSessionMock.getInformationTypesForAccountType(accountTypeId) >> aits
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, aitJSONString)
    }

    void "get all existing AITs for a account type resulted with internal error"(){

        given: 'The id account type for which the AITs that needs to be fetched.'
        def accountTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAllAccountTypes, and verify the number of calls.'
        1 * webServicesSessionMock.getInformationTypesForAccountType(accountTypeId) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "get a specific ait by id"(){

        given: 'The JSON of the AITs that needs to be fetched.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, aitJSONString)
    }

    void "try to fetch non existing ait"() {

        given: 'The ids of the account type and AIT that needs to be fetched.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "fetch existing ait resulted with internal error"() {

        given: 'The ids of the account type and AIT that needs to be fetched.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to steal an ait from different account type"() {

        given: 'The JSON of the AITs that needs to be fetched.'
        def fakeAccountTypeId = Integer.valueOf(1)
        def accountTypeId = Integer.valueOf(2)
        def aitId = Integer.valueOf(221)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${fakeAccountTypeId}/aits/${aitId}",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to create invalid ait"(){

        given: 'The JSON of the ait that needs to be created.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)

        and: 'Mock the behaviour of the createAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.createAccountInformationType(_ as AccountInformationTypeWS) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits", HttpMethod.POST,
                RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "creation of ait resulted with internal error"(){

        given: 'The JSON of the ait that needs to be created.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)

        and: 'Mock the behaviour of the createAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        1 * webServicesSessionMock.createAccountInformationType(_ as AccountInformationTypeWS) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}

        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits", HttpMethod.POST,
                RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to create ait for different account type"(){

        given: 'The JSON of the ait that needs to be created.'
        def fakeAccountTypeId = Integer.valueOf(1)
        def accountTypeId = Integer.valueOf(2)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)

        and: 'No api calls were made..verify that.'
        1 * webServicesSessionMock.getAccountType(fakeAccountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${fakeAccountTypeId}/aits",
                HttpMethod.POST, RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == 'Account type param id invalid!'
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "create ait"(){

        given: 'The JSON of the ait that needs to be created.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the createAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.createAccountInformationType(_ as AccountInformationTypeWS) >> aitId
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits",
                HttpMethod.POST, RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${accountTypeId}/aits/${aitId}#")
        RestApiHelper.validateResponseJson(response, aitJSONString)
    }

    void "try to update non existing ait"(){

        given: 'The JSON of the ait that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to update ait from different account type"(){

        given: 'The JSON of the ait that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def fakeAccountTypeId = Integer.valueOf(2)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${fakeAccountTypeId}/aits/${aitId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "update ait"(){

        given: 'The JSON of the ait that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType and updateAccountInformationType, and verify the number of calls.'
        2 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.updateAccountInformationType(_ as AccountInformationTypeWS)
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, aitJSONString)
    }

    void "try to update ait with invalid data"(){

        given: 'The JSON of the ait that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType and updateAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.updateAccountInformationType(_ as AccountInformationTypeWS) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "update of ait resulted with internal error"(){

        given: 'The JSON of the ait that needs to be updated.'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(221)
        def aitJSONString = buildAccountInformationTypeJSONString(accountTypeId, aitId)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType and updateAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.updateAccountInformationType(_ as AccountInformationTypeWS) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), aitJSONString.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1,
                MediaType.APPLICATION_JSON)
    }

    void "try to delete ait that do not exist"(){

        given: 'The id of the ait that do not exist'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
    }

    void "try to delete ait for different account type"(){

        given: 'The id of the ait that needs to be deleted'
        def accountTypeId = Integer.valueOf(1)
        def fakeAccountTypeId = Integer.valueOf(2)
        def aitId = Integer.valueOf(221)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${fakeAccountTypeId}/aits/${aitId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
    }

    void "try to delete ait that can not be deleted"(){

        given: 'The id of the ait that needs to be deleted'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(212)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.deleteAccountInformationType(aitId) >> false
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        response.getText() == ''
    }

    void "try to delete ait that can not be deleted 2"(){

        given: 'The id of the ait that needs to be deleted'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(212)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.deleteAccountInformationType(aitId) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "try to delete ait that can not be deleted 3"(){

        given: 'The id of the ait that needs to be deleted'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(212)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.deleteAccountInformationType(aitId) >>
                {throw new SessionInternalError("Test", HttpStatus.SC_CONFLICT)}
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_CONFLICT, '', '')
    }

    void "delete ait"(){

        given: 'The id of the ait that needs to be deleted'
        def accountTypeId = Integer.valueOf(1)
        def aitId = Integer.valueOf(212)
        def ait = buildAccountInformationTypeMock(accountTypeId, aitId)

        and: 'Mock the behaviour of the getAccountInformationType, and verify the number of calls.'
        1 * webServicesSessionMock.getAccountInformationType(aitId) >> ait
        1 * webServicesSessionMock.deleteAccountInformationType(aitId) >> true
        1 * webServicesSessionMock.getAccountType(accountTypeId) >> new AccountTypeWS()
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${accountTypeId}/aits/${aitId}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    private static buildAccountInformationTypeJSONString(accountTypeId, id){
        """{
          "id": ${id},
          "dateCreated": ${RestApiHelper.DUMMY_TEST_DATE.time},
          "dateUpdated": ${TEST_DATE.time},
          "entityId": 1,
          "entityType": "ACCOUNT_TYPE",
          "displayOrder": 1,
          "metaFields": [${BuilderHelper.buildEmailMetaFieldJson(0, EntityType.ACCOUNT_INFORMATION_TYPE)}],
          "descriptions": [${BuilderHelper.buildInternationalDescriptionJson('Contact', 'description')}],
          "name": "Name-${id}",
          "accountTypeId": ${accountTypeId},
          "useForNotifications": false
        }"""
    }

    private static buildAccountInformationTypeJSONString(accountTypeId, id, int... ids){

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildAccountInformationTypeJSONString(accountTypeId, id))

        ids.each {
            stringBuilder.append(",${buildAccountInformationTypeJSONString(accountTypeId, it)}")
        }

        stringBuilder.append(']')
        stringBuilder.toString()
    }

    private static buildAccountInformationTypeMock(accountTypeId, id){

        AccountInformationTypeWS accountInformationType = new AccountInformationTypeWS()
        accountInformationType.accountTypeId = accountTypeId
        accountInformationType.id = id
        accountInformationType.name = "Name-${id}"
        accountInformationType.dateCreated = RestApiHelper.DUMMY_TEST_DATE
        accountInformationType.dateUpdated = TEST_DATE
        accountInformationType.entityId = Integer.valueOf(1)
        accountInformationType.displayOrder = Integer.valueOf(1)
        accountInformationType.metaFields = [BuilderHelper.buildEmailMetaField(0, EntityType.ACCOUNT_INFORMATION_TYPE)]
        accountInformationType.descriptions = Arrays.asList(new InternationalDescriptionWS('description', 1, 'Contact'))

        accountInformationType
    }

    private static buildAccountInformationTypeMockArray(accountTypeId, id, int... ids){

        def aits = []
        aits.add(buildAccountInformationTypeMock(accountTypeId, id))
        ids.each {
            aits.add(buildAccountInformationTypeMock(accountTypeId, it))
        }

        aits.toArray(new AccountInformationTypeWS[0])
    }

}
