package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 31-OCT-2016
 */
@Test(groups = {"rest"}, testName = "OrderChangeTypeRestTest")
public class OrderChangeTypeRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);

    @BeforeClass
    public void setup(){
        super.setup("orderchangetypes");
    }

    @Test
    public void getAllOrderChangeTypes() {

        // Get all order change types and verify the response
        ResponseEntity<List<OrderChangeTypeWS>> getResponse = restTemplate.sendRequest(
                REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertNotNull(getResponse, "Response should not be null");
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());

        // Get the number of existing order change types
        int number = getResponse.getBody().size();

        // Build an order change type
        OrderChangeTypeWS ocType = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "testOrderChangeType");

        ResponseEntity<OrderChangeTypeWS> postResponse = null;

        try {
            // Create the order change type
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocType, OrderChangeTypeWS.class);

            // Try to get all order change types again, and verify the returned number
            getResponse = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
            assertEquals(number + 1, getResponse.getBody().size(), "Returned number does not match");
        } finally {
            // Delete the created order change type
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Try to get all order change types again, and verify the returned number
        getResponse = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertEquals(number, getResponse.getBody().size(), "Returned number does not match");
    }

    @Test
    public void getOrderChangeType() {

        // Build an order change type
        OrderChangeTypeWS ocType = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "testOrderChangeType");

        ResponseEntity<OrderChangeTypeWS> postResponse = null;

        try {
            // Create the order change type
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocType, OrderChangeTypeWS.class);

            // Get the order change type and verify the response
            ResponseEntity<OrderChangeTypeWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderChangeTypeWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
            assertEquals(getResponse.getBody(), postResponse.getBody(), "Order change types do not match.");
        } finally {
            // Delete the created order change type
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExistingOrderChangeType() {

        // Try to get a non-existing order change type and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderChangeTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void createOrderChangeType() {

        // Build an order change type
        OrderChangeTypeWS ocType = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "testOrderChangeType");

        ResponseEntity<OrderChangeTypeWS> postResponse = null;

        try {
            // Create the order change
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocType, OrderChangeTypeWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the order change type is generated
            ResponseEntity<OrderChangeTypeWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderChangeTypeWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the created order change type
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void createOrderChangeTypeError() {

        // Build an order change type
        OrderChangeTypeWS ocType = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "testOrderChangeType");

        ResponseEntity<OrderChangeTypeWS> postResponse = null;

        try {
            // Create the order change
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocType, OrderChangeTypeWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Try to create another order change type with the same name
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, ocType, OrderChangeTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            // Delete the created order change type
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateOrderChangeType() {

        // Build an order change type
        OrderChangeTypeWS ocType = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "testOrderChangeType");

        ResponseEntity<OrderChangeTypeWS> postResponse = null;

        try {
            // Create the order change
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocType, OrderChangeTypeWS.class);

            // Update the order change type and verify the response
            ocType = postResponse.getBody();
            ocType.setName("updatedOrderChangeType");
            ResponseEntity<OrderChangeTypeWS> putResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, ocType, OrderChangeTypeWS.class);
            RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
            assertEquals(ocType, putResponse.getBody(), "Order change types do not match.");
        } finally {
            // Delete the order change type
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + ocType.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateNonExistingOrderChangeType() {

        // Build an order change type
        OrderChangeTypeWS ocType = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "testOrderChangeType");

        // Try to update a non-existing order change type and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    ocType, OrderChangeTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateOrderChangeTypeError() {

        // Build two order change types
        OrderChangeTypeWS ocTypeOne = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "firstOCType");
        OrderChangeTypeWS ocTypeTwo = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "secondOCType");

        ResponseEntity<OrderChangeTypeWS> firstPostResponse = null;
        ResponseEntity<OrderChangeTypeWS> secondPostResponse = null;

        try {
            // Create the order change types
            firstPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocTypeOne, OrderChangeTypeWS.class);
            secondPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocTypeTwo, OrderChangeTypeWS.class);

            // Try to update the second order change type with the same name as the first one
            ocTypeTwo.setName(ocTypeOne.getName());
            restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, ocTypeTwo, OrderChangeTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            // Delete the order change types
            if (null != firstPostResponse) {
                restTemplate.sendRequest(REST_URL + firstPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
            if (null != secondPostResponse) {
                restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void deleteOrderChangeType() {

        // Build an order change type
        OrderChangeTypeWS ocType = RestEntitiesHelper.buildOrderChangeType(null, ENTITY_ID, "testOrderChangeType");

        ResponseEntity<OrderChangeTypeWS> postResponse = null;

        try {
            // Create the order change
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, ocType, OrderChangeTypeWS.class);

            // Verify that the order change type is generated
            ResponseEntity<OrderChangeTypeWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderChangeTypeWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the order change type
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Verify that the order change type does not exist any more
        try {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderChangeTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteNonExistingOrderChangeType() {


        // Try to delete a non-existing order change type and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders,
                    null, OrderChangeTypeWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
