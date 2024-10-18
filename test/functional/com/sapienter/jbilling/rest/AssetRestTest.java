package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;

/**
 * @author Vojislav Stanojevikj
 * @since 24-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "AssetRestTest")
public class AssetRestTest extends RestTestCase{

    private static final String TESTING_ITEM_TYPE_NAME = "RestTestCategory";
    private static final String TESTING_ITEM_NAME = "RestTestItem";
    private static final String TESTING_ASSET_ID = "RestTestAssetID";

    private Integer TESTING_ITEM_TYPE_ID;
    private Integer TESTING_ITEM_ID;
    private Integer DEFAULT_STATUS_ID;
    private RestOperationsHelper itemTypeRestHelper;
    private RestOperationsHelper itemRestHelper;

    @BeforeClass
    public void setup(){
        super.setup("assets");
        itemRestHelper = RestOperationsHelper.getInstance("items");
        itemTypeRestHelper = RestOperationsHelper.getInstance("itemtypes");

        ItemTypeWS itemType = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME, true, true), ItemTypeWS.class)
                .getBody();

        DEFAULT_STATUS_ID = RestEntitiesHelper.findDefaultAssetStatusId(itemType);
        TESTING_ITEM_TYPE_ID = itemType.getId();

        TESTING_ITEM_ID = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();
    }

    @AfterClass
    public void tearDown(){
        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + TESTING_ITEM_ID, HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + TESTING_ITEM_TYPE_ID, HttpMethod.DELETE,
                getOrDeleteHeaders, null);

    }

    @Test
    public void postAsset(){

        ResponseEntity<AssetWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAssetMock(TESTING_ASSET_ID,
                        TESTING_ITEM_ID, DEFAULT_STATUS_ID), AssetWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        AssetWS postedAsset = postResponse.getBody();
        ResponseEntity<AssetWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedAsset.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AssetWS.class);
        AssetWS fetchedAsset = fetchedResponse.getBody();

        assertEquals(fetchedAsset, postedAsset, "Assets do not match!");

        restTemplate.sendRequest(REST_URL + postedAsset.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void postAssetForItemWithNoAssetManagement(){

        Integer notAssetManagedItem = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("NotAssetManaged", false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                    RestEntitiesHelper.buildAssetMock(TESTING_ASSET_ID, notAssetManagedItem, DEFAULT_STATUS_ID), AssetWS.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("AssetWS,itemId,asset.validation.item.not.assetmanagement"));
        } finally {
            restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + notAssetManagedItem, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getAsset(){

        ResponseEntity<AssetWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAssetMock(TESTING_ASSET_ID,
                        TESTING_ITEM_ID, DEFAULT_STATUS_ID), AssetWS.class);

        ResponseEntity<AssetWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AssetWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        AssetWS fetchedAsset = fetchedResponse.getBody();
        assertEquals(fetchedAsset, postResponse.getBody(), "Assets do not match!");

        restTemplate.sendRequest(REST_URL + fetchedAsset.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getAssetThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET,
                    getOrDeleteHeaders, null, AssetWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteAsset(){

        ResponseEntity<AssetWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock(TESTING_ASSET_ID, TESTING_ITEM_ID, DEFAULT_STATUS_ID), AssetWS.class);

        ResponseEntity<AssetWS> deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        ResponseEntity<AssetWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AssetWS.class);

        assertEquals(fetchedResponse.getBody().getDeleted(), 1, "Asset not deleted!");
    }

    @Test
    public void deleteAssetThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateAsset(){

        ResponseEntity<AssetWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock(TESTING_ASSET_ID, TESTING_ITEM_ID, DEFAULT_STATUS_ID), AssetWS.class);

        AssetWS updatedMock = postedResponse.getBody();
        updatedMock.setIdentifier("Updated-" + TESTING_ASSET_ID);

        ResponseEntity<AssetWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedMock, AssetWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<AssetWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AssetWS.class);

        assertEquals(fetchedResponse.getBody(), updatedMock, "Assets do not match!");

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void updateAssetThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildAssetMock(TESTING_ASSET_ID,
                            TESTING_ITEM_ID, DEFAULT_STATUS_ID), AssetWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateAssetInvalid(){

        ResponseEntity<AssetWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAssetMock(TESTING_ASSET_ID, TESTING_ITEM_ID, DEFAULT_STATUS_ID), AssetWS.class);

        ItemTypeWS itemType = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + TESTING_ITEM_TYPE_ID, HttpMethod.GET,
                getOrDeleteHeaders, null, ItemTypeWS.class).getBody();

        AssetWS updatedMock = postedResponse.getBody();
        updatedMock.setAssetStatusId(RestEntitiesHelper.findAssetStatusIdByName(itemType, "Three"));

        try {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                    getOrDeleteHeaders, updatedMock, AssetWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("AssetWS,assetStatus,asset.validation.status.change.toordersaved"));
        } finally {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }
}
