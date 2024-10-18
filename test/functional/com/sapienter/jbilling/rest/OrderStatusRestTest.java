package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 31-OCT-2016
 */
@Test(groups = {"rest"}, testName = "OrderStatusRestTest")
public class OrderStatusRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);

    @BeforeClass
    public void setup(){
        super.setup("orderstatuses");
    }

    @Test
    public void getOrderStatus() {

        // Build an order status
        OrderStatusWS orderStatus = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "testInvoiceFlag");

        ResponseEntity<OrderStatusWS> postResponse = null;

        try {
            // Create the order status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderStatus, OrderStatusWS.class);

            // Get the order status and verify the response
            ResponseEntity<OrderStatusWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderStatusWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
            assertEquals(getResponse.getBody(), postResponse.getBody(), "Order statuses do not match.");
        } finally {
            // Delete the order status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExistingOrderStatus() {

        // Try to get a non-existing order status and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void createOrderStatus() {

        // Build an order status
        OrderStatusWS orderStatus = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "testInvoiceFlag");

        ResponseEntity<OrderStatusWS> postResponse = null;

        try {
            // Create the order status order status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderStatus, OrderStatusWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the order status is generated
            ResponseEntity<OrderStatusWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderStatusWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the order status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void createOrderStatusError() {

        // Build an order status
        OrderStatusWS orderStatus = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "testInvoiceFlag");

        ResponseEntity<OrderStatusWS> postResponse = null;

        try {
            // Create the order status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderStatus, OrderStatusWS.class);

            // Try to create another order status with the same name
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, orderStatus, OrderStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            // Delete the created order status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateOrderStatus() {

        // Build an order status
        OrderStatusWS orderStatus = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "testInvoiceFlag");

        ResponseEntity<OrderStatusWS> postResponse = null;

        try {
            // Create the order status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderStatus, OrderStatusWS.class);

            // Update the order status and verify the response
            orderStatus = postResponse.getBody();
            orderStatus.setOrderStatusFlag(OrderStatusFlag.NOT_INVOICE);
            ResponseEntity<OrderStatusWS> putResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, orderStatus, OrderStatusWS.class);
            RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
            assertEquals(orderStatus, putResponse.getBody(), "Order statuses do not match.");
        } finally {
            // Delete the order status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateNonExistingOrderStatus() {

        // Build an order status
        OrderStatusWS orderStatus = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "testInvoiceFlag");

        // Try to update a non-existing order status and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    orderStatus, OrderStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateOrderStatusError() {

        // Build two order statuses
        OrderStatusWS orderStatusOne = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "firstInvoiceFlag");
        OrderStatusWS orderStatusTwo = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "secondInvoiceFlag");

        ResponseEntity<OrderStatusWS> firstPostResponse = null;
        ResponseEntity<OrderStatusWS> secondPostResponse = null;

        try {
            // Create the order statuses
            firstPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderStatusOne, OrderStatusWS.class);
            secondPostResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderStatusTwo, OrderStatusWS.class);

            // Try to update the second order status with the same description (name) as the first one
            orderStatusTwo.setDescription(orderStatusOne.getDescription());
            restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, orderStatusTwo, OrderStatusWS.class);
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
    public void deleteOrderStatus() {

        // Build an order status
        OrderStatusWS orderStatus = RestEntitiesHelper.buildOrderStatus(
                null, ENTITY_ID, OrderStatusFlag.INVOICE, "testInvoiceFlag");

        ResponseEntity<OrderStatusWS> postResponse = null;
        try {
            // Create the order status
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, orderStatus, OrderStatusWS.class);

            // Verify that the order status is generated
            ResponseEntity<OrderStatusWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderStatusWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the order status
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Verify that the order status does not exist any more
        try {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteNonExistingOrderStatus() {

        // Try to delete a non-existing order status and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders,
                    null, OrderStatusWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
