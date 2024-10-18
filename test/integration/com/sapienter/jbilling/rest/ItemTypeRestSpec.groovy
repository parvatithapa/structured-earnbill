package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.AssetWS
import com.sapienter.jbilling.server.item.ItemTypeWS
import com.sapienter.jbilling.server.metafields.EntityType
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Vojislav Stanojevikj
 * @since 21-Sep-2016.
 */

class ItemTypeRestSpec extends RestBaseSpec{

    private static final String CATEGORY_NAME = 'TestCategory'
    private static final String ASSET_ID_LABEL = 'TestAssetLabelId'
    private static final String STATUS_DEFAULT = 'Default'
    private static final String STATUS_ORDER_SAVED = 'OrderSaved'

    def itemTypeResource

    def setup() {
        init(itemTypeResource, 'itemtypes')
    }

    void "get all existing item types"(){

        given: 'The JSON of the item types that needs to be fetched.'
        def itemTypeJSON = buildItemTypeJSONString(100, 102, 103, 104)
        def itemTypes = BuilderHelper.buildWSMocks(itemTypeJSON, ItemTypeWS).toArray(new ItemTypeWS[0])

        and: 'Mock the behaviour of the getAllItemCategories, and verify the number of calls.'
        1 * webServicesSessionMock.getAllItemCategories() >> itemTypes
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, itemTypeJSON)
    }

    void "get all existing item types resulted with internal error"(){

        given: 'Mock the behaviour of the getAllItemCategories, and verify the number of calls.'
        1 * webServicesSessionMock.getAllItemCategories() >> new ItemTypeWS[0]
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get all existing item types empty."(){

        given: 'Mock the behaviour of the getAllItemCategories, and verify the number of calls.'
        1 * webServicesSessionMock.getAllItemCategories() >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get existing item type"() {

        given: 'The JSON of the item type that needs to be fetched.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())
        def itemType = BuilderHelper.buildWSMock(itemTypeJson, ItemTypeWS)

        and: 'Mock the behaviour of the getItemCategoryById, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> itemType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, itemTypeJson)
    }

    void "try to fetch non existing item type"() {

        given: 'Just the id of the non existing item type'
        def itemTypeId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getItemCategoryById, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "fetch existing item type resulted with internal error"() {

        given: 'Just the id of the a existing item type'
        def itemTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItemCategoryById, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to create invalid item type"(){

        given: 'The JSON of the item type that needs to be created.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())

        and: 'Mock the behaviour of the createItemCategory, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createItemCategory(_ as ItemTypeWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST,
                RestApiHelper.buildJsonHeaders(), itemTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create item type resulted with internal error"(){

        given: 'The JSON of the item type that needs to be created.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())

        and: 'Mock the behaviour of the createItemCategory, and verify the number of calls.'
        1 * webServicesSessionMock.createItemCategory(_ as ItemTypeWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), itemTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create item type"(){

        given: 'The JSON of the item type that needs to be created.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())
        def itemType = BuilderHelper.buildWSMock(itemTypeJson, ItemTypeWS)

        and: 'Mock the behaviour of the createItemCategory and getItemCategoryById, and verify the number of calls.'
        1 * webServicesSessionMock.createItemCategory(_ as ItemTypeWS) >> itemTypeId
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> itemType
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST,
                RestApiHelper.buildJsonHeaders(), itemTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${itemTypeId}#")
        RestApiHelper.validateResponseJson(response, itemTypeJson)
    }

    void "try to update non existing item type"(){

        given: 'The JSON of the item type that needs to be updated.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())

        and: 'Mock the behaviour of the getItemCategoryById, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to update item type with invalid data"(){

        given: 'The JSON of the item type that needs to be updated.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())
        def itemType = BuilderHelper.buildWSMock(itemTypeJson, ItemTypeWS)

        and: 'Mock the behaviour of the getItemCategoryById and updateItemCategory, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> itemType
        1 * webServicesSessionMock.updateItemCategory(_ as ItemTypeWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update item type resulted with internal error"(){

        given: 'The JSON of the item type that needs to be updated.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())
        def itemType = BuilderHelper.buildWSMock(itemTypeJson, ItemTypeWS)

        and: 'Mock the behaviour of the getItemCategoryById and updateItemCategory, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> itemType
        1 * webServicesSessionMock.updateItemCategory(_ as ItemTypeWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update item type"(){

        given: 'The JSON of the item type that needs to be updated.'
        def itemTypeId = Integer.valueOf(1)
        def itemTypeJson = buildItemTypeJSONString(itemTypeId.intValue())
        def itemType = BuilderHelper.buildWSMock(itemTypeJson, ItemTypeWS)

        and: 'Mock the behaviour of the getItemCategoryById and updateItemCategory, and verify the number of calls.'
        2 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> itemType
        1 * webServicesSessionMock.updateItemCategory(_ as ItemTypeWS)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), itemTypeJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, itemTypeJson)
    }

    void "try to delete item type that do not exist"(){

        given: 'The id of the item type that do not exist'
        def itemTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItemCategoryById, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        response.getText() == ''
    }

    void "try to delete item type that can not be deleted"(){

        given: 'The id of the item type that needs to be deleted.'
        def itemTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItemCategoryById and deleteItemCategory, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> new ItemTypeWS()
        1 * webServicesSessionMock.deleteItemCategory(itemTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "delete item type resulted with internal error"(){

        given: 'The id of the item type that do not exist'
        def itemTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItemCategoryById, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "delete item type"(){

        given: 'The id of the item type that needs to be deleted.'
        def itemTypeId = Integer.valueOf(1)

        and: 'Mock the behaviour of the getItemCategoryById and deleteItemCategory, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> new ItemTypeWS()
        1 * webServicesSessionMock.deleteItemCategory(itemTypeId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    void "get all assets for an item category"(){
        given: 'The id of the item category for which assets needs to be fetched.'
        def itemTypeId = Integer.valueOf(1)
        def assetsJson = BuilderHelper.buildAssetJsonStringArray(Integer.valueOf(100), Integer.valueOf(101))
        def assets = BuilderHelper.buildWSMocks(assetsJson, AssetWS).toArray(new AssetWS[0])

        and: 'Mock the behaviour of the getAssetsForCategoryId, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> new ItemTypeWS()
        1 * webServicesSessionMock.getAssetsForCategoryId(itemTypeId, null, null) >> assets
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId}/assets",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, assetsJson)

    }

    void "get all assets for an item category that do not exist"(){
        given: 'The id of the item category for which assets needs to be fetched.'
        def itemTypeId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getAssetsForCategoryId, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> null
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId}/assets",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)

    }

    void "get all assets for an item category resulted with internal error"(){
        given: 'The id of the item category for which assets needs to be fetched.'
        def itemTypeId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getAssetsForCategoryId, and verify the number of calls.'
        1 * webServicesSessionMock.getItemCategoryById(itemTypeId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${itemTypeId}/assets",
                HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)

    }

    private static buildItemTypeJSONString(int id){
    StringBuilder stringBuilder = new StringBuilder("""{"id":${id},""")
        stringBuilder.append(""""description":"${CATEGORY_NAME}-${id}",""")
        stringBuilder.append(""""orderLineTypeId":1,""")
        stringBuilder.append(""""global":false,""")
        stringBuilder.append(""""entityId":1,""")
        stringBuilder.append(""""entities":[1],""")
        stringBuilder.append(""""allowAssetManagement":1,""")
        stringBuilder.append(""""assetIdentifierLabel":"${ASSET_ID_LABEL}",""")
        stringBuilder.append(""""assetStatuses":[""")
        stringBuilder.append("""{"id":0,"description":"${STATUS_DEFAULT}","isDefault":1,"isAvailable":1,"isOrderSaved":0,"isInternal":0},""")
        stringBuilder.append("""{"id":0,"description":"${STATUS_ORDER_SAVED}","isDefault":0,"isAvailable":0,"isOrderSaved":1,"isInternal":0}],""")
        stringBuilder.append(""""assetMetaFields":[${BuilderHelper.buildEmailMetaFieldJson(0, EntityType.PRODUCT_CATEGORY)}],""")
        stringBuilder.append(""""metaFields":[],""")
        stringBuilder.append(""""metaFieldsMap":{},""")
        stringBuilder.append(""""onePerCustomer":false,""")
        stringBuilder.append(""""onePerOrder":false}""")

        stringBuilder.toString()
    }

    private static buildItemTypeJSONString(int id, int... ids){

        StringBuilder stringBuilder = new StringBuilder('[')
        stringBuilder.append(buildItemTypeJSONString(id))
        ids.each {
            stringBuilder.append(',')
            stringBuilder.append(buildItemTypeJSONString(it))
        }
        stringBuilder.append("]")
        stringBuilder.toString()
    }
}
