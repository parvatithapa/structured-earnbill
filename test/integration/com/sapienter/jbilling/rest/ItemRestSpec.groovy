package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.AssetWS
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.metafields.EntityType
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Vojislav Stanojevikj
 * @since 27-Sep-2016.
 */
class ItemRestSpec extends RestBaseSpec{

    def itemResource

    def setup() {
        init(itemResource, 'items')
    }

    void "get all existing items."(){

        given: 'The JSON of the items that needs to be fetched.'
        def itemsJSON = buildItemJsonString(100, 102, 103, 104)
        def items = BuilderHelper.buildWSMocks(itemsJSON, ItemDTOEx).toArray(new ItemDTOEx[0])

        and: 'Mock the behaviour of the getAllItems, and verify the number of calls.'
        1 * webServicesSessionMock.getAllItems() >> items
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, itemsJSON)
    }

    void "get all existing items empty"(){

        given: 'Mock the behaviour of the getAllItems, and verify the number of calls.'
        1 * webServicesSessionMock.getAllItems() >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get all existing items resulted with internal error "(){

        given: 'Mock the behaviour of the getAllItems, and verify the number of calls.'
        1 * webServicesSessionMock.getAllItems() >> new ItemDTOEx[0]
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get existing item"() {

        given: 'The JSON of the item that needs to be fetched.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())
        def item = BuilderHelper.buildWSMock(itemJson, ItemDTOEx)

        and: 'Mock the behaviour of the getItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> item
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.GET)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, itemJson)
    }

    void "try to fetch non existing item"() {

        given: 'Just the id of the non existing item'
        def itemId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getItem, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "fetch item resulted with internal error"() {

        given: 'Just the id of a existing item'
        def itemId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to create invalid item"(){

        given: 'The JSON of the item that needs to be created.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())

        and: 'Mock the behaviour of the createItem, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createItem(_ as ItemDTOEx) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), itemJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to create invalid item 2"(){

        given: 'The JSON of the item that needs to be created.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())

        and: 'Mock the behaviour of the createItem, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createItem(_ as ItemDTOEx) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), itemJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create item"(){

        given: 'The JSON of the item that needs to be created.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())
        def item = BuilderHelper.buildWSMock(itemJson, ItemDTOEx)

        and: 'Mock the behaviour of the createItem and getItem, and verify the number of calls.'
        1 * webServicesSessionMock.createItem(_ as ItemDTOEx) >> itemId
        1 * webServicesSessionMock.getItem(itemId, null, null) >> item
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), itemJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${itemId}#")
        RestApiHelper.validateResponseJson(response, itemJson)
    }

    void "try to update non existing item"(){

        given: 'The JSON of the item that needs to be updated.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())

        and: 'Mock the behaviour of the getItem, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to update item with invalid data"(){

        given: 'The JSON of the item that needs to be updated.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())
        def item = BuilderHelper.buildWSMock(itemJson, ItemDTOEx)

        and: 'Mock the behaviour of the getItem and updateItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> item
        1 * webServicesSessionMock.updateItem(_ as ItemDTOEx) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update item resulted with internal error"(){

        given: 'The JSON of the item that needs to be updated.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())
        def item = BuilderHelper.buildWSMock(itemJson, ItemDTOEx)

        and: 'Mock the behaviour of the getItem and updateItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> item
        1 * webServicesSessionMock.updateItem(_ as ItemDTOEx) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update item"(){

        given: 'The JSON of the item that needs to be updated.'
        def itemId = Integer.valueOf(1)
        def itemJson = buildItemJsonString(itemId.intValue())
        def item = BuilderHelper.buildWSMock(itemJson, ItemDTOEx)

        and: 'Mock the behaviour of the getItem and updateItem, and verify the number of calls.'
        2 * webServicesSessionMock.getItem(itemId, null, null) >> item
        1 * webServicesSessionMock.updateItem(_ as ItemDTOEx)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, itemJson)
    }

    void "try to delete item that do not exist"(){

        given: 'The id of the item that do not exist'
        def itemId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "try to delete item that can not be deleted"(){

        given: 'The id of the item that needs to be deleted.'
        def itemId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItem and deleteItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> new ItemDTOEx()
        1 * webServicesSessionMock.deleteItem(itemId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders())

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "delete item"(){

        given: 'The id of the item that needs to be deleted.'
        def itemId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItem and deleteItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> new ItemDTOEx()
        1 * webServicesSessionMock.deleteItem(itemId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    void "get all assets for an item"(){
        given: 'The id of the item for which assets needs to be fetched.'
        def itemId = Integer.valueOf(1)
        def assetsJson = BuilderHelper.buildAssetJsonStringArray(Integer.valueOf(100), Integer.valueOf(101))
        def assets = BuilderHelper.buildWSMocks(assetsJson, AssetWS).toArray(new AssetWS[0])

        and: 'Mock the behaviour of the getAssetsForItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> new ItemDTOEx()
        1 * webServicesSessionMock.getAssetsForItemId(itemId, null, null) >> assets
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId}/assets", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, assetsJson)

    }

    void "get all assets for an item that do not exist"(){
        given: 'The id of the item for which assets needs to be fetched.'
        def itemId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getAssetsForItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId}/assets", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get all assets for an item resulted with internal error"(){
        given: 'The id of the item for which assets needs to be fetched.'
        def itemId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getAssetsForItem, and verify the number of calls.'
        1 * webServicesSessionMock.getItem(itemId, null, null) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemId}/assets", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }


    private static buildItemJsonString(id){
        """
            {
              "id": ${id},
              "number": "testProduct",
              "hasDecimals": 0,
              "deleted": 0,
              "assetManagementEnabled": 0,
              "entityId": 1,
              "defaultPrices": {
                  "${new Date().getTime()}": ${BuilderHelper.buildFlatPriceJson(Integer.valueOf(10))}
              },
              "defaultPrice": ${BuilderHelper.buildFlatPriceJson(Integer.valueOf(10))},
              "types": [${BuilderHelper.random.nextInt(Integer.MAX_VALUE - 1)}],
              "currencyId": 1,
              "orderLineTypeId": 1,
              "descriptions": [${BuilderHelper.buildInternationalDescriptionJson('testProduct', 'description')}],
              "orderLineMetaFields": [${BuilderHelper.buildEmailMetaFieldJson(Integer.valueOf(0), EntityType.ORDER_LINE)}],
              "standardAvailability": false,
              "global": false,
              "accountTypes": [1],
              "entities": [1],
              "priceModelCompanyId": 1,
              "isPlan": false,
              "metaFieldsMap": {}
            }
        """
    }

    private static buildItemJsonString(id, int... ids){
        StringBuilder sb = new StringBuilder("[${buildItemJsonString(id)}")
        ids.each {
            sb.append(",${buildItemJsonString(it)}")
        }
        sb.append(']').toString()
    }
}
