package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 31-OCT-2016
 */
@Test(groups = {"rest"}, testName = "OrderChangeStatusRestTest")
public class OrderChangeStatusRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);

    @BeforeClass
    public void setup(){
        super.setup("orderchangestatuses");
    }

    @Test
    public void getOrderChangeStatus() {

        // Build an order change status
        OrderChangeStatusWS orderChangeStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "testOrderChangeStatus");

        ResponseEntity<OrderChangeStatusWS> postResponse = null;

        try {
            // Create the order change status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderChangeStatus, OrderChangeStatusWS.class);
            // Get the order change status
            ResponseEntity<OrderChangeStatusWS[]> getResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderChangeStatusWS[].class);
            orderChangeStatus = getOrderChangeStatusById(postResponse.getBody().getId(), getResponse.getBody());
            assertNotNull(getResponse, "GET response should not be null.");
            RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
            assertEquals(orderChangeStatus, postResponse.getBody(), "Order change statuses do not match.");
        } finally {
            // Delete the order change status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void createOrderChangeStatus() {

        // Build an order change status
        OrderChangeStatusWS orderChangeStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "testOrderChangeStatus");

        ResponseEntity<OrderChangeStatusWS> postResponse = null;

        try {
            // Create the order change status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderChangeStatus, OrderChangeStatusWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the order change status is generated
            ResponseEntity<OrderChangeStatusWS[]> getResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderChangeStatusWS[].class);
            assertNotNull(getOrderChangeStatusById(postResponse.getBody().getId(), getResponse.getBody()),
                    "The order change status is not present in the GET response.");
        } finally {
            if (null != postResponse) {
                // Delete the order change status
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void createOrderChangeStatusError() {

        // Build an order change status
        OrderChangeStatusWS orderChangeStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "testOrderChangeStatus");

        ResponseEntity<OrderChangeStatusWS> postResponse = null;

        try {
            // Create the order change status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderChangeStatus, OrderChangeStatusWS.class);
            // Try to create another order change status with the same name
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, orderChangeStatus, OrderChangeStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            // Delete the created order change status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateOrderChangeStatus() {

        // Build an order change status
        OrderChangeStatusWS orderChangeStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "testOrderChangeStatus");

        ResponseEntity<OrderChangeStatusWS> postResponse = null;

        try {
            // Create the order change status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderChangeStatus, OrderChangeStatusWS.class);

            // Update the order change status and verify the response
            orderChangeStatus = postResponse.getBody();
            orderChangeStatus.setApplyToOrder(ApplyToOrder.NO);
            ResponseEntity<OrderChangeStatusWS> putResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, orderChangeStatus, OrderChangeStatusWS.class);
            RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
            assertEquals(orderChangeStatus, putResponse.getBody(), "Order change statuses do not match.");
        } finally {
            // Delete the created order change status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateNonExistingOrderChangeStatus() {

        // Build an order change status
        OrderChangeStatusWS orderChangeStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "testOrderChangeStatus");

        // Try to update a non-existing order change status and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    orderChangeStatus, OrderChangeStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateOrderChangeStatusError() {

        // Build two order change statuses
        OrderChangeStatusWS firstOCStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "firstOrderChangeStatus");
        OrderChangeStatusWS secondOCStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "secondOrderChangeStatus");

        ResponseEntity<OrderChangeStatusWS> firstPostResponse = null;
        ResponseEntity<OrderChangeStatusWS> secondPostResponse = null;

        try {
            // Create the order change statuses
            firstPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, firstOCStatus, OrderChangeStatusWS.class);
            secondPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, secondOCStatus, OrderChangeStatusWS.class);

            // Try to update the second order change status with the same description (name) as the first one
            secondOCStatus.setDescriptions(firstOCStatus.getDescriptions());
            restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, secondOCStatus, OrderChangeStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            // Delete the order statuses
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
    public void deleteOrderChangeStatus() {

        // Build an order change status
        OrderChangeStatusWS orderChangeStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "testOrderChangeStatus");

        ResponseEntity<OrderChangeStatusWS> postResponse = null;
        ResponseEntity<OrderChangeStatusWS[]> getResponse = null;

        try {
            // Create the order change status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderChangeStatus, OrderChangeStatusWS.class);

            // Verify that the order change status is generated
            getResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderChangeStatusWS[].class);
            assertNotNull(getOrderChangeStatusById(postResponse.getBody().getId(), getResponse.getBody()),
                    "The order change status is not present in the GET response.");
        } finally {
            // Delete the order change status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Verify that the order change status does not exist any more
        getResponse = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders,
                null, OrderChangeStatusWS[].class);
        assertNull(getOrderChangeStatusById(postResponse.getBody().getId(), getResponse.getBody()),
                "The order change status should not be present GET response.");
    }

    @Test
    public void deleteNonExistingOrderChangeStatus() {

        // Build an order change status
        OrderChangeStatusWS orderChangeStatus = RestEntitiesHelper.buildOrderChangeStatus(
                null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), "testOrderChangeStatus");

        // Try to delete a non-existing order change status and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders,
                    orderChangeStatus, OrderChangeStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    private OrderChangeStatusWS getOrderChangeStatusById(Integer id, OrderChangeStatusWS[] statuses) {

        for(OrderChangeStatusWS status : statuses) {
            if (status.getId().equals(id)){
                return status;
            }
        }
        return null;
    }
}
