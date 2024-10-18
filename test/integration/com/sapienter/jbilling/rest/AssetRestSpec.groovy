package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.AssetWS
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Vojislav Stanojevikj
 * @since 28-Sep-2016.
 */
class AssetRestSpec extends RestBaseSpec{

    def assetResource

    def setup() {
        init(assetResource, 'assets')
    }

    void "get existing asset"() {

        given: 'The JSON of the asset that needs to be fetched.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))
        def asset = BuilderHelper.buildWSMock(assetJson, AssetWS)

        and: 'Mock the behaviour of the getAsset, and verify the number of calls.'
        1 * webServicesSessionMock.getAsset(assetId) >> asset
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, assetJson)
    }

    void "try to fetch non existing asset"() {

        given: 'Just the id of the non existing asset'
        def assetId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getAsset, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getAsset(assetId) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to fetch asset, but the call resulted with internal error"() {

        given: 'Just the id of a existing asset'
        def assetId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAsset, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getAsset(assetId) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to create invalid asset"(){

        given: 'The JSON of the asset that needs to be created.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))

        and: 'Mock the behaviour of the createAsset, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createAsset(_ as AssetWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), assetJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to create invalid asset 2"(){

        given: 'The JSON of the asset that needs to be created.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))

        and: 'Mock the behaviour of the createAsset, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createAsset(_ as AssetWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), assetJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create asset"(){

        given: 'The JSON of the asset that needs to be created.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))
        def asset = BuilderHelper.buildWSMock(assetJson, AssetWS)

        and: 'Mock the behaviour of the createAsset and getAsset, and verify the number of calls.'
        1 * webServicesSessionMock.createAsset(_ as AssetWS) >> assetId
        1 * webServicesSessionMock.getAsset(assetId) >> asset
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), assetJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${assetId}#")
        RestApiHelper.validateResponseJson(response, assetJson)
    }

    void "try to update non existing asset"(){

        given: 'The JSON of the asset that needs to be updated.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))

        and: 'Mock the behaviour of the getAsset, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getAsset(assetId) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), assetJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to update asset with invalid data"(){

        given: 'The JSON of the asset that needs to be updated.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))
        def asset = BuilderHelper.buildWSMock(assetJson, AssetWS)

        and: 'Mock the behaviour of the getAsset and updateAsset, and verify the number of calls.'
        1 * webServicesSessionMock.getAsset(assetId) >> asset
        1 * webServicesSessionMock.updateAsset(_ as AssetWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), assetJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to update asset and internal error occurs"(){

        given: 'The JSON of the asset that needs to be updated.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))
        def asset = BuilderHelper.buildWSMock(assetJson, AssetWS)

        and: 'Mock the behaviour of the getAsset and updateAsset, and verify the number of calls.'
        1 * webServicesSessionMock.getAsset(assetId) >> asset
        1 * webServicesSessionMock.updateAsset(_ as AssetWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), assetJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update asset"(){

        given: 'The JSON of the asset that needs to be updated.'
        def assetId = Integer.valueOf(1)
        def assetJson = BuilderHelper.buildAssetJsonString(assetId.intValue(), BuilderHelper.random.nextInt(Integer.MAX_VALUE),
                BuilderHelper.random.nextInt(Integer.MAX_VALUE))
        def asset = BuilderHelper.buildWSMock(assetJson, AssetWS)

        and: 'Mock the behaviour of the getAsset and updateAsset, and verify the number of calls.'
        2 * webServicesSessionMock.getAsset(assetId) >> asset
        1 * webServicesSessionMock.updateAsset(_ as AssetWS)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), assetJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, assetJson)
    }

    void "try to delete asset that do not exist"(){

        given: 'The id of the asset that do not exist'
        def assetId = Integer.valueOf(1)

        and: 'Mock the behaviour of the deleteAsset, and verify the number of calls.'
        1 * webServicesSessionMock.deleteAsset(assetId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "try to delete asset that can not be deleted"(){

        given: 'The id of the asset that needs to be deleted.'
        def assetId = Integer.valueOf(1)

        and: 'Mock the behaviour of the deleteItem, and verify the number of calls.'
        1 * webServicesSessionMock.deleteAsset(assetId) >> {throw new SessionInternalError("Test", HttpStatus.SC_CONFLICT)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.CONFLICT.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_CONFLICT, '', '')
    }

    void "try to delete asset and internal error occurred"(){

        given: 'The id of the asset that needs to be deleted.'
        def assetId = Integer.valueOf(1)

        and: 'Mock the behaviour of the deleteItem, and verify the number of calls.'
        1 * webServicesSessionMock.deleteAsset(assetId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "delete asset"(){

        given: 'The id of the asset that needs to be deleted.'
        def assetId = Integer.valueOf(1)

        and: 'Mock the behaviour of the deleteAsset, and verify the number of calls.'
        1 * webServicesSessionMock.deleteAsset(assetId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${assetId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

}
