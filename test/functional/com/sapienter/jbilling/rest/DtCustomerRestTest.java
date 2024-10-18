package com.sapienter.jbilling.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.sapienter.jbilling.appdirect.subscription.PayloadWS;
import com.sapienter.jbilling.appdirect.userCompany.CompanyPayload;
import com.sapienter.jbilling.appdirect.userCompany.ContentWS;
import com.sapienter.jbilling.appdirect.userCompany.ResourceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.apache.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.sapienter.jbilling.server.util.Constants.DeutscheTelekom.*;
import static org.testng.Assert.*;

@Test(groups = {"dt-rest"}, testName = "DtCustomerRestTest")
public class DtCustomerRestTest extends RestTestCase {

    private RestOperationsHelper restOperationsHelperForUser;

    private Integer PARENT_CUSTOMER_ID;
    private Integer CHILD_CUSTOMER_ID;


    private String ENTITY_IDENTIFIER;

    private HttpHeaders dtHeadersGetOrDelete;
    private HttpHeaders dtHeadersPostOrPut;

    private UUID childExtAccntNo;
    private UUID childSubscriptionId;

    private WireMockServer wireMockServer = new WireMockServer(8090);

    @BeforeClass
    public void setUp() {

        // set up the default parameters to point to DtCustomerResource
        super.setup("users/dt");
        restOperationsHelperForUser = RestOperationsHelper.getInstance("users");


        // Value is same as the one given in test-data-features.xml for company_unique_identifier metafield
        ENTITY_IDENTIFIER = "b9ab449a-9f5a-4a69-a738-99ba6fd84d49";

        // Extra header is needed to access users/dt api
        dtHeadersGetOrDelete = new HttpHeaders();
        dtHeadersGetOrDelete.add("appdirect-webhook-token", "api-user-token-dt");
        dtHeadersGetOrDelete.putAll(getOrDeleteHeaders);


        dtHeadersPostOrPut = new HttpHeaders();
        dtHeadersPostOrPut.add("appdirect-webhook-token", "api-user-token-dt");
        dtHeadersPostOrPut.putAll(postOrPutHeaders);

        childExtAccntNo = UUID.randomUUID();
        childSubscriptionId = UUID.randomUUID();

        // Stub appdirect marketplace url
        String COMPANY_BASE_URL = "/api/account/v2/companies/6d07f0e5-13c5-4ac8-b481-47ebf9c0b07e";
        // this uuid is the parent ext account identifier resource --> content --> company --> id

        wireMockServer.start();

        wireMockServer.stubFor(get(urlMatching(COMPANY_BASE_URL))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("OAuth.*"))
                .willReturn(
                        aResponse()
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                .withStatus(200)
                                    .withBody("{\"name\":\"DT Company\"}")
                ));
    }

    @AfterClass
    public void tearDown() {

        wireMockServer.stop();

        // delete child customer first, then parent
        restTemplate.sendRequest(restOperationsHelperForUser.getFullRestUrl() + CHILD_CUSTOMER_ID, HttpMethod.DELETE, getOrDeleteHeaders,
                null);

        restTemplate.sendRequest(restOperationsHelperForUser.getFullRestUrl() + PARENT_CUSTOMER_ID, HttpMethod.DELETE, getOrDeleteHeaders,
                null);
    }


    /**
     * This test case will create a parent as well as a child customer
     */
    @Test(priority = 1)
    public void testCreateCustomerWithParentAndChild() {

        PayloadWS requestPayload = RestEntitiesHelper.buildPayloadMockFromJsonString(childExtAccntNo.toString(), childSubscriptionId.toString());

        ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + ENTITY_IDENTIFIER, HttpMethod.POST,
                dtHeadersPostOrPut, requestPayload);


        RestValidationHelper.validateStatusCode(response, Response.Status.CREATED.getStatusCode());

        String responsePath = response.getHeaders().getLocation().getPath();
        Integer childCustomerId = Integer.parseInt(responsePath.substring(responsePath.lastIndexOf('/') + 1, responsePath.length()));


        UserWS fetchedChildCustomer = restTemplate.sendRequest(restOperationsHelperForUser.getFullRestUrl() + childCustomerId, HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class).getBody();

        UserWS fetchedParentCustomer = restTemplate.sendRequest(restOperationsHelperForUser.getFullRestUrl() + fetchedChildCustomer.getParentId(), HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class).getBody();

        // These values will be required to delete the created customers after all test cases have been executed
        PARENT_CUSTOMER_ID = fetchedParentCustomer.getId();
        CHILD_CUSTOMER_ID = fetchedChildCustomer.getId();

        String parentExtAccntIdentifier = null;
        String childExtAccntIdentifier = null;

        MetaFieldValueWS[] metaFields = fetchedParentCustomer.getMetaFields();
        for(MetaFieldValueWS metaFieldValueWS: metaFields) {
            if(metaFieldValueWS.getFieldName().equals(EXTERNAL_ACCOUNT_IDENTIFIER)) {
                parentExtAccntIdentifier = metaFieldValueWS.getStringValue();
                break;
            }
        }

        metaFields = fetchedChildCustomer.getMetaFields();
        for(MetaFieldValueWS metaFieldValueWS: metaFields) {
            if(metaFieldValueWS.getFieldName().equals(EXTERNAL_ACCOUNT_IDENTIFIER)) {
                childExtAccntIdentifier = metaFieldValueWS.getStringValue();
                break;
            }
        }

        // Check User name - should be same as the one we get from stubbed marketplace instance
        assertEquals(fetchedParentCustomer.getUserName(), "DT Company");

        // Check the external account identifiers as given in the request for parent and child
        assertNotNull(parentExtAccntIdentifier, "Parent customer external account identifier is null");
        assertEquals(parentExtAccntIdentifier, requestPayload.getResource().getContent().getCompany().getId(), "External account identifier for parent does not match");

        assertNotNull(childExtAccntIdentifier, "Child customer external account identifier is null");
        assertEquals(childExtAccntIdentifier, requestPayload.getResource().getContent().getExternalAccountId(), "External account identifier for child does not match");
    }

    /**
     * Test create customer having invalid product id
     */
    @Test(priority = 2)
    public void testCreateCustomerForInvalidProduct() {

        ResponseEntity<UserWS> response = null;
        try {
            PayloadWS requestPayload = RestEntitiesHelper.buildPayloadMockFromJsonString(childExtAccntNo.toString(), childSubscriptionId.toString());
            requestPayload.getResource().getContent().getProduct().setId("1221");
            // this id is not present in the metafield value appdirectProductDetails

            response = restTemplate.sendRequest(REST_URL + ENTITY_IDENTIFIER, HttpMethod.POST,
                    dtHeadersPostOrPut, requestPayload, UserWS.class);
            fail("Test case for invalid product failed");

        } catch (Exception e) {
            assertEquals(e.getMessage(), "500 Internal Server Error");
        }

    }

    /**
     * Test delete customer having Valid subscription id
     */
    @Test(priority = 3)
    public void testDeleteCustomerForValidSubscriptionId() {

        ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + "delete/" + ENTITY_IDENTIFIER, HttpMethod.POST, dtHeadersPostOrPut,
                RestEntitiesHelper.buildPayloadMockFromJsonString(childExtAccntNo.toString(), childSubscriptionId.toString()));

        assertNotNull(deletedResponse, "Response cannot be null");

        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<UserWS> fetchedResponse = restTemplate.sendRequest(restOperationsHelperForUser.getFullRestUrl() + CHILD_CUSTOMER_ID, HttpMethod.GET,
                dtHeadersGetOrDelete, null, UserWS.class);

        assertTrue(fetchedResponse.getBody().getDeleted() == 1, "Child customer not deleted!");

    }

    /**
     * Test delete customer having Invalid subscription id
     */
    @Test(priority = 4)
    public void testDeleteCustomerForInvalidSubscriptionId() {

        try {

            PayloadWS payloadWS = RestEntitiesHelper.buildPayloadMockFromJsonString(childExtAccntNo.toString(), childSubscriptionId.toString());
            payloadWS.getResource().setUuid("wrong-uuid-for-this-customer");
            ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + "delete/" + ENTITY_IDENTIFIER, HttpMethod.POST,
                    dtHeadersPostOrPut, payloadWS);

        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Test delete customer having Invalid entity identifier
     */
    @Test(priority = 5)
    public void testDeleteCustomerForInvalidEntityIdentifier() {

        try {

            ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + "delete/some-invalid-identifier", HttpMethod.POST,
                    dtHeadersPostOrPut, RestEntitiesHelper.buildPayloadMockFromJsonString(childExtAccntNo.toString(), childSubscriptionId.toString()));

        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Test update company having Valid subscription id
     */
    @Test(priority = 6)
    public void testUpdateCompanyForValidSubscriptionId() {

        ResourceWS resourceWS = new ResourceWS();
        resourceWS.setType(RESOURCE_TYPE_COMPANY);
        ContentWS contentWS = new ContentWS();
        contentWS.setName("Test Company");
        resourceWS.setContent(contentWS);

        CompanyPayload companyPayload = new CompanyPayload();
        companyPayload.setUuid("6d07f0e5-13c5-4ac8-b481-47ebf9c0b07e"); // this uuid is same from the payload - resource --> content --> company --> id
        companyPayload.setResource(resourceWS);
        companyPayload.setResourceAction(PAYLOAD_ACTION_CHANGED);

        ResponseEntity updateResponse = restTemplate.sendRequest(REST_URL + "update/" + ENTITY_IDENTIFIER, HttpMethod.POST,
                dtHeadersPostOrPut, companyPayload);

        assertNotNull(updateResponse, "Response cannot be null");

        RestValidationHelper.validateStatusCode(updateResponse, Response.Status.OK.getStatusCode());
    }

    /**
     * Test update company having Invalid subscription id
     */
    @Test(priority = 7)
    public void testUpdateCustomerForInvalidSubscriptionId() {

        try {

            PayloadWS payloadWS = RestEntitiesHelper.buildPayloadMockFromJsonString(childExtAccntNo.toString(), childSubscriptionId.toString());
            payloadWS.getResource().setUuid("wrong-uuid-for-this-customer");
            ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + "update/" + ENTITY_IDENTIFIER, HttpMethod.POST,
                    dtHeadersPostOrPut, payloadWS);

        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), HttpStatus.SC_NOT_FOUND);
        }

    }


    /**
     * Test update company having Invalid entity identifier
     */
    @Test(priority = 8)
    public void testUpdateCompanyForInvalidEntityIdentifier() {

        try {

            ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + "update/some-invalid-identifier", HttpMethod.POST,
                    dtHeadersPostOrPut, RestEntitiesHelper.buildPayloadMockFromJsonString(childExtAccntNo.toString(), childSubscriptionId.toString()));

        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
