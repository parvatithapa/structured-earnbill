package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertNotNull;

/**
 * @author Vojislav Stanojevikj
 * @since 21-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "ItemTypeRestTest")
public class ItemTypeRestTest extends RestTestCase{

    private String TESTING_ITEM_TYPE_NAME = "RestTestCategory";
    private Integer TESTING_ITEM_TYPE_ID;

    @BeforeClass
    public void setup(){
        super.setup("itemtypes");
    }

    /**
     * C34749877
     */
    @Test
    public void postItemType(){

        ResponseEntity<List<ItemTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        int initialNumberOfEntities = response.getBody().size();

        ResponseEntity<ItemTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), false, true), ItemTypeWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        ItemTypeWS postedItemType = postResponse.getBody();
        ResponseEntity<ItemTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedItemType.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, ItemTypeWS.class);
        ItemTypeWS fetchedItemType = fetchedResponse.getBody();

        assertEquals(fetchedItemType, postedItemType, "Item categories do not match!");

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertEquals(initialNumberOfEntities + 1, response.getBody().size(), "Initial number of item types did not increased!");

        restTemplate.sendRequest(REST_URL + postedItemType.getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void postDuplicateItemType(){
        TESTING_ITEM_TYPE_NAME = "RestTestCategory"+RestTestUtils.getRandomString(3);
        ResponseEntity<ItemTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME, false, true), ItemTypeWS.class);

        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME, false, true), ItemTypeWS.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("ItemTypeWS,name,validation.error.category.already.exists"));
        }finally {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getItemType(){

        ResponseEntity<ItemTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), false, true), ItemTypeWS.class);

        ResponseEntity<ItemTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, ItemTypeWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        ItemTypeWS fetchedItemType = fetchedResponse.getBody();
        assertEquals(fetchedItemType, postResponse.getBody(), "Item types do not match!");

        restTemplate.sendRequest(REST_URL + fetchedItemType.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    /**
     * C34749865
     */
    @Test
    public void getItemTypeThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders, null, ItemTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getAllItemTypes(){

        ResponseEntity<List<ItemTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertNotNull(response, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());

        int initialNumberOfEntities = response.getBody().size();

        ResponseEntity<ItemTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), false, true), ItemTypeWS.class);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertEquals(initialNumberOfEntities + 1, response.getBody().size(), "Initial number of item types did not increased!");

        restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertEquals(initialNumberOfEntities, response.getBody().size(), "Current number of item types did not decreased!");
    }

    /**
     * C34749889
     */
    @Test
    public void deleteItemType(){

        ResponseEntity<ItemTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), false, true), ItemTypeWS.class);

        ResponseEntity<List<ItemTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        int currentNumberOfEntities = response.getBody().size();

        ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertEquals(currentNumberOfEntities - 1, response.getBody().size(), "Current number of item types did not decreased!");
    }

    /**
     * C34749890
     */
    @Test
    public void deleteItemTypeThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteItemTypeThatCanNotBeDeleted(){

        RestOperationsHelper itemRestHelper = RestOperationsHelper.getInstance("items");

        ResponseEntity<ItemTypeWS> itemTypeResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), false, true), ItemTypeWS.class);

        ResponseEntity<ItemDTOEx> itemResponse = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("testRestItem", true, false, itemTypeResponse.getBody().getId()), ItemDTOEx.class);

        try {
            restTemplate.sendRequest(REST_URL + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.CONFLICT.getStatusCode());
            assertTrue(errorMsg.contains("validation.error.category.is.not.empty"));
        } finally {
            restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + itemResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(REST_URL + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    /**
     * C34749878
     */
    @Test
    public void updateItemType(){
        TESTING_ITEM_TYPE_NAME = "RestTestCategory" + RestTestUtils.getRandomString(3);
        ResponseEntity<ItemTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME, false, true), ItemTypeWS.class);

        ItemTypeWS updatedMock = postedResponse.getBody();
        updatedMock.setDescription("Up-" + TESTING_ITEM_TYPE_NAME);

        ResponseEntity<ItemTypeWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedMock, ItemTypeWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<ItemTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, ItemTypeWS.class);

        assertEquals(fetchedResponse.getBody(), updatedMock, "Item types do not match!");

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void updateItemTypeThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), false, true));
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateItemTypeAssetManagementInvalid(){

        RestOperationsHelper itemRestHelper = RestOperationsHelper.getInstance("items");

        ResponseEntity<ItemTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), false, true), ItemTypeWS.class);

        ResponseEntity<ItemDTOEx> itemResponse = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("testRestItem", true, false, postedResponse.getBody().getId()), ItemDTOEx.class);

        ItemTypeWS updatedMock = postedResponse.getBody();
        updatedMock.setAllowAssetManagement(Integer.valueOf(0));

        try {
            restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.PUT,
                    postOrPutHeaders, updatedMock, ItemTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("ItemTypeWS,allowAssetManagement,product.category.validation.product.assetmanagement.enabled"));
        } finally {
            restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + itemResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    /**
     * C34749866 , C34749879
     */
    @Test
    public void getAssetsByItemTypeId(){

        RestOperationsHelper itemRestHelper = RestOperationsHelper.getInstance("items");
        RestOperationsHelper assetRestHelper = RestOperationsHelper.getInstance("assets");

        ResponseEntity<ItemTypeWS> itemTypeResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), true, true), ItemTypeWS.class);
        Integer defaultAssetStatusId = RestEntitiesHelper.findDefaultAssetStatusId(itemTypeResponse.getBody());

        ResponseEntity<ItemDTOEx> postedItemResponse = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("AnotherTestItem", true, true, itemTypeResponse.getBody().getId()), ItemDTOEx.class);

        ResponseEntity<AssetWS> assetResponse1 = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId1", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        ResponseEntity<AssetWS> assetResponse2 = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId2", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        ResponseEntity<AssetWS> assetResponse3 = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId3", postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        ResponseEntity<AssetWS[]> assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + itemTypeResponse.getBody().getId() + "/assets"), HttpMethod.GET,
                getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 3, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse1.getBody(),
                assetResponse2.getBody(), assetResponse3.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                REST_URL + itemTypeResponse.getBody().getId() + "/assets",
                new RestQueryParameter<>("max", Integer.valueOf(1))), HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 1, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse1.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                REST_URL + itemTypeResponse.getBody().getId() + "/assets",
                new RestQueryParameter<>("max", Integer.valueOf(2))), HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 2, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse1.getBody(), assetResponse2.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + itemTypeResponse.getBody().getId() + "/assets",
                        new RestQueryParameter<>("max", Integer.valueOf(10)), new RestQueryParameter<>("offset", Integer.valueOf(1))),
                HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);

        assertEquals(assetsResponse.getBody().length, 2, "Invalid number of fetched assets!");
        RestValidationHelper.<AssetWS>arrayContainsAllElements(assetsResponse.getBody(), assetResponse2.getBody(), assetResponse3.getBody());

        assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + itemTypeResponse.getBody().getId() + "/assets",
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

        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + postedItemResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(REST_URL + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

    }

    @Test
    public void getAssetsByItemTypeThatDoNotExist(){

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            REST_URL + Integer.MAX_VALUE + "/assets"), HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C34749863
     */
    @Test
    public void getProductCategories(){
        TESTING_ITEM_TYPE_ID = restTemplate.sendRequest(restHelper.getFullRestUrl(), HttpMethod.POST,
                RestOperationsHelper.appendHeaders(restHelper.getAuthHeaders(), RestOperationsHelper.getJSONHeaders(true, true)),
                RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), true, true), ItemTypeWS.class).getBody().getId();
        ParameterizedTypeReference<List<ItemTypeWS>> typeRef = new ParameterizedTypeReference<List<ItemTypeWS>>() {};

        ResponseEntity<List<ItemTypeWS>> responseEntity = restTemplate.getRestOperations().exchange(restHelper.getFullRestUrl(), HttpMethod.GET, new HttpEntity<>(null, getOrDeleteHeaders), typeRef);
        List<ItemTypeWS> productCategories = responseEntity.getBody()
                .stream().filter(t -> t.getId().equals(TESTING_ITEM_TYPE_ID)).collect(Collectors.toList());
        Assert.assertTrue(!productCategories.isEmpty(),"Product Category Not available.");
        restTemplate.sendRequest(REST_URL + TESTING_ITEM_TYPE_ID, HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    /**
     * C34749868
     */
    @Test
    public void getAssetsByItemTypeWithNegativeOffload() {

        RestOperationsHelper itemRestHelper = RestOperationsHelper.getInstance("items");
        RestOperationsHelper assetRestHelper = RestOperationsHelper.getInstance("assets");

        ResponseEntity<ItemTypeWS> itemTypeResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), true, true), ItemTypeWS.class);
        Integer defaultAssetStatusId = RestEntitiesHelper.findDefaultAssetStatusId(itemTypeResponse.getBody());

        ResponseEntity<ItemDTOEx> postedItemResponse = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("AnotherTestItem" + RestTestUtils.getRandomString(3), true, true, itemTypeResponse.getBody().getId()), ItemDTOEx.class);

        ResponseEntity<AssetWS> assetResponse1 = restTemplate.sendRequest(assetRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock("TestAssetId1" + RestTestUtils.getRandomString(3), postedItemResponse.getBody().getId(), defaultAssetStatusId), AssetWS.class);

        try {
            ResponseEntity<AssetWS[]> assetsResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + itemTypeResponse.getBody().getId() + "/assets",
                    new RestQueryParameter<>("max", Integer.valueOf(-1)), new RestQueryParameter<>("offset", Integer.valueOf(-2))),
                    HttpMethod.GET, getOrDeleteHeaders, null, AssetWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            restTemplate.sendRequest(assetRestHelper.getFullRestUrl() + assetResponse1.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + postedItemResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(REST_URL + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    /**
     * C34749886
     */
    @Test
    public void deletePopulatedProductCategory() {
        RestOperationsHelper itemRestHelper = RestOperationsHelper.getInstance("items");
        ResponseEntity<ItemTypeWS> itemTypeResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME + RestTestUtils.getRandomString(3), true, true), ItemTypeWS.class);

        ResponseEntity<ItemDTOEx> postedItemResponse = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("AnotherTestItem", true, true, itemTypeResponse.getBody().getId()), ItemDTOEx.class);
        try {
            restTemplate.sendRequest(REST_URL + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.CONFLICT.getStatusCode());
            assertTrue(errorMsg.contains("validation.error.category.is.not.empty"), "[FALSE] Actual: "+errorMsg);
        } finally {
            restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + postedItemResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            restTemplate.sendRequest(REST_URL + itemTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }
}