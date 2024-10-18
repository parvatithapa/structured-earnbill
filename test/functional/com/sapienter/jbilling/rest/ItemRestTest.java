package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.server.item.AssetSearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 23-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "ItemRestTest")
public class ItemRestTest extends RestTestCase{

    private static final Logger logger = LoggerFactory.getLogger(ItemRestTest.class);

    private static final String TESTING_ITEM_TYPE_NAME = "RestTestCategory";
    private static final String TESTING_ITEM_NAME = "RestTestItem";

    private Integer TESTING_ITEM_TYPE_ID;
    private RestOperationsHelper itemTypeRestHelper;
    private static EnvironmentHelper environmentHelper;
    private TestBuilder testBuilder;
    private ResponseEntity<ItemTypeWS> itemTypeResponse;
    private ResponseEntity<ItemDTOEx> postedItemResponse;

    @BeforeClass
    public void setup(){
        super.setup("items");
        testBuilder = getTestEnvironment();
        itemTypeRestHelper = RestOperationsHelper.getInstance("itemtypes");
        TESTING_ITEM_TYPE_ID = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST,
                RestOperationsHelper.appendHeaders(restHelper.getAuthHeaders(), RestOperationsHelper.getJSONHeaders(true, true)),
                RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME, true, true), ItemTypeWS.class).getBody().getId();
    }

    @AfterClass
    public void tearDown(){
        restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + TESTING_ITEM_TYPE_ID, HttpMethod.DELETE,
                RestOperationsHelper.appendHeaders(restHelper.getAuthHeaders(),
                        RestOperationsHelper.getJSONHeaders(true, false)), null);
    }

    @Test
    public void postItem(){

        ResponseEntity<ItemDTOEx> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(
                        TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        ItemDTOEx postedItem = postResponse.getBody();
        ResponseEntity<ItemDTOEx> fetchedResponse = restTemplate.sendRequest(REST_URL + postedItem.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, ItemDTOEx.class);
        ItemDTOEx fetchedItem = fetchedResponse.getBody();

        assertEquals(fetchedItem, postedItem, "Items do not match!");

        restTemplate.sendRequest(REST_URL + postedItem.getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void postItemAssetManagementFailed(){

        Integer testCategory = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(
                        "AnotherCategory", false, false), ItemTypeWS.class).getBody().getId();

        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                    RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME, true, false, testCategory), ItemDTOEx.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("ItemDTOEx,types,product.validation.no.assetmanagement.type.error"));
        } finally {
            restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + testCategory, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getItem(){

        ResponseEntity<ItemDTOEx> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(
                        TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);

        ResponseEntity<ItemDTOEx> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, ItemDTOEx.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        ItemDTOEx fetchedItem = fetchedResponse.getBody();
        assertEquals(fetchedItem, postResponse.getBody(), "Items do not match!");

        restTemplate.sendRequest(REST_URL + fetchedItem.getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    /**
     * C34749871
     */
    @Test
    public void getItemThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET,
                    getOrDeleteHeaders, null, ItemDTOEx.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

/*
    / * Too manu items. Request times out * /
    @Test
    public void getAllItems(){

        ResponseEntity<List<ItemDTOEx>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertNotNull(response, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());

        int initialNumberOfEntities = response.getBody().size();

        ResponseEntity<ItemDTOEx> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(
                        TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertEquals(initialNumberOfEntities + 1, response.getBody().size(), "Initial number of items did not increased!");

        restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }*/

    /**
     * C34749887
     */
    @Test
    public void deleteItem(){

        ResponseEntity<ItemDTOEx> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(
                        TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);

        ResponseEntity<ItemDTOEx> deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        ResponseEntity<ItemDTOEx> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, ItemDTOEx.class);

        assertEquals(fetchedResponse.getBody().getDeleted(), Integer.valueOf(1), "Item not deleted!");
    }

    /**
     * C34749888
     */
    @Test
    public void deleteItemThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteItemThatCanNotBeDeleted(){

        RestOperationsHelper assetRestHelper = RestOperationsHelper.getInstance("assets");

        ResponseEntity<ItemTypeWS> itemTypeResponse = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock("AnotherTestCategory", true, true), ItemTypeWS.class);
        Integer defaultAssetStatusId = RestEntitiesHelper.findDefaultAssetStatusId(itemTypeResponse.getBody());

        ResponseEntity<ItemDTOEx> itemResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("testRestItem", true, false, itemTypeResponse.getBody().getId()), ItemDTOEx.class);

        ResponseEntity<AssetWS> assetResponse = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId", itemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        try {
            restTemplate.sendRequest(REST_URL + itemResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.CONFLICT.getStatusCode());
            assertTrue(errorMsg.contains("validation.item.no.delete.assets.linked"));
        } finally {
            restTemplate.sendRequest(assetRestHelper.getFullRestUrl() + assetResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(REST_URL + itemResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void updateItem(){

        ResponseEntity<ItemDTOEx> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);

        ItemDTOEx updatedMock = postedResponse.getBody();
        updatedMock.setDescription("Updated-" + TESTING_ITEM_TYPE_NAME);

        ResponseEntity<ItemDTOEx> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedMock, ItemDTOEx.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<ItemDTOEx> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders,
                null, ItemDTOEx.class);

        assertEquals(fetchedResponse.getBody(), updatedMock, "Items do not match!");

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void updateItemThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME,
                            true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateItemWithNoDescription(){

        ResponseEntity<ItemDTOEx> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);

        ItemDTOEx updatedMock = postedResponse.getBody();
        updatedMock.setDescription("");

        try {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                    postOrPutHeaders, updatedMock, ItemDTOEx.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("ItemDTOEx,descriptions,validation.error.is.required"));
        } finally {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getAssetsByItemId(){

        RestOperationsHelper assetRestHelper = RestOperationsHelper.getInstance("assets");

        ResponseEntity<ItemTypeWS> itemTypeResponse = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock("AnotherTestCategory", true, true), ItemTypeWS.class);
        Integer defaultAssetStatusId = RestEntitiesHelper.findDefaultAssetStatusId(itemTypeResponse.getBody());

        ResponseEntity<ItemDTOEx> postedItemResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME, true, true, itemTypeResponse.getBody().getId()), ItemDTOEx.class);

        ResponseEntity<AssetWS> assetResponse1 = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId1", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        ResponseEntity<AssetWS> assetResponse2 = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId2", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        ResponseEntity<AssetWS> assetResponse3 = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId3", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        ResponseEntity<AssetWS[]> assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + postedItemResponse.getBody().getId() + "/assets"), HttpMethod.GET,
                getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 3, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse1.getBody(),
                assetResponse2.getBody(), assetResponse3.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + postedItemResponse.getBody().getId() + "/assets",
                        new RestQueryParameter<>("max", Integer.valueOf(1))), HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 1, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse1.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + postedItemResponse.getBody().getId() + "/assets",
                new RestQueryParameter<>("max", Integer.valueOf(2))), HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 2, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse1.getBody(), assetResponse2.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + postedItemResponse.getBody().getId() + "/assets",
                new RestQueryParameter<>("max", Integer.valueOf(10)), new RestQueryParameter<>("offset", Integer.valueOf(1))),
                HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 2, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse2.getBody(), assetResponse3.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + postedItemResponse.getBody().getId() + "/assets",
                        new RestQueryParameter<>("max", Integer.valueOf(10)), new RestQueryParameter<>("offset", Integer.valueOf(2))),
                HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 1, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse3.getBody());

        restTemplate.sendRequest(assetRestHelper.getFullRestUrl() + assetResponse3.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(assetRestHelper.getFullRestUrl() + assetResponse2.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(assetRestHelper.getFullRestUrl() + assetResponse1.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(REST_URL + postedItemResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

    }

    @Test
    public void getAssetsByItemThatDoNotExist(){

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            REST_URL + Integer.MAX_VALUE + "/assets"), HttpMethod.GET,
                    getOrDeleteHeaders, null, AssetWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * Test for new restful api FindAssets
     *
     */
    @Test
    public void testFindAssets() {
        testBuilder.given(envBuilder -> {
                itemTypeResponse = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                        RestEntitiesHelper.buildItemTypeMock("AnotherTestCategory-" + System.currentTimeMillis(), true, true), ItemTypeWS.class);

                postedItemResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                        RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME + System.currentTimeMillis(), true, true, itemTypeResponse.getBody().getId()),
                        ItemDTOEx.class);

        }).test(env -> {
            try {
                RestOperationsHelper assetRestHelper = RestOperationsHelper.getInstance("assets");
                Integer defaultAssetStatusId = RestEntitiesHelper.findDefaultAssetStatusId(itemTypeResponse.getBody());

                restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                        RestEntitiesHelper.buildAssetMock("TestAssetId1", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

                restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                        RestEntitiesHelper.buildAssetMock("TestAssetId2", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

                restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                        RestEntitiesHelper.buildAssetMock("TestAssetId3", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

                Integer wrongItemId = 1234;
                ResponseEntity<AssetSearchResult> assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + "assets/" + wrongItemId + "/" + "One" + "?offset=0&max=0"), HttpMethod.GET,
                        getOrDeleteHeaders, null, AssetSearchResult.class);
                assertEquals(assetsResponse.getStatusCode(), 404, "Item not found code should be matched!");

                assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                                REST_URL + "assets/" + postedItemResponse.getBody().getId() + "/" + "One" + "?offset=0&max=0"), HttpMethod.GET,
                                getOrDeleteHeaders, null, AssetSearchResult.class);
                assertEquals(assetsResponse.getBody().getObjects().length, 3, "Invalid number of fetched assets!");

                assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + "assets/" + postedItemResponse.getBody().getId() + "/" + "One" + "?offset=0&max=2"), HttpMethod.GET,
                        getOrDeleteHeaders, null, AssetSearchResult.class);
                        assertEquals(assetsResponse.getBody().getObjects().length, 2, "Invalid number of fetched assets!");

                assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + "assets/" + postedItemResponse.getBody().getId() + "/" + "One" + "?offset=2&max=0"), HttpMethod.GET,
                        getOrDeleteHeaders, null, AssetSearchResult.class);
                assertEquals(assetsResponse.getBody().getObjects().length, 1, "Invalid number of fetched assets!");

                restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            } catch(Exception e) {
                logger.error(e.getMessage());
            }
        });
    }

    /**
     * C34749869
     */
    /* Too many items. Request times out */
/*    @Test
    public void getAllProductItems() {
        ResponseEntity<ItemDTOEx> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(
                        TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class);

        ResponseEntity<ItemDTOEx[]> fetchedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null, ItemDTOEx[].class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        assertTrue(RestValidationHelper.<ItemDTOEx>arrayContainsAllElements(fetchedResponse.getBody(), postResponse.getBody()));

        restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }*/

    /**
     * C34749874
     */

    @Test
    public void getAssetsByItemIDWithNegativeOffload() {

        RestOperationsHelper itemRestHelper = RestOperationsHelper.getInstance("items");
        RestOperationsHelper assetRestHelper = RestOperationsHelper.getInstance("assets");

        ResponseEntity<ItemTypeWS> itemTypeResponse = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), true, true), ItemTypeWS.class);
        Integer defaultAssetStatusId = RestEntitiesHelper.findDefaultAssetStatusId(itemTypeResponse.getBody());

        ResponseEntity<ItemDTOEx> postedItemResponse = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("TestItem" + RestTestUtils.getRandomString(3), true, true, itemTypeResponse.getBody().getId()), ItemDTOEx.class);

        restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId" + RestTestUtils.getRandomString(3), postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    itemRestHelper.getFullRestUrl() + postedItemResponse.getBody().getId() + "/assets",
                    new RestQueryParameter<>("max", Integer.valueOf(-1)), new RestQueryParameter<>("offset", Integer.valueOf(-5))),
                    HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    private static TestBuilder getTestEnvironment() {
        return TestBuilder.newTest().givenForMultiple(envCreator -> {
            environmentHelper = EnvironmentHelper.getInstance(envCreator.getPrancingPonyApi());
        });
    }
}
