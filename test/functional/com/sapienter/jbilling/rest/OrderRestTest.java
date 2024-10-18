package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.resources.OrderInfo;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Bojan Dikovski
 * @since 31-OCT-2016
 */
@Test(groups = {"rest"}, testName = "OrderRestTest")
public class OrderRestTest extends RestTestCase {

    private static Integer ENTITY_ID = Integer.valueOf(1);
    private String random = String.valueOf(new Random().nextInt(100));

    private RestOperationsHelper accTypeRestHelper;
    private RestOperationsHelper userRestHelper;
    private RestOperationsHelper itemTypeRestHelper;
    private RestOperationsHelper itemRestHelper;
    private RestOperationsHelper periodRestHelper;
    private RestOperationsHelper ocStatusRestHelper;

    private Integer ACC_TYPE_ID;
    private Integer USER_ID;
    private static final String USER_NAME = "orderTestUser";
    private Integer ITEM_TYPE_ID;
    private static final String ITEM_TYPE = "orderTestItemType";
    private Integer ITEM_ONE_ID;
    private static final String ITEM_ONE = "orderTestItemOne";
    private Integer ITEM_TWO_ID;
    private static final String ITEM_TWO = "orderTestItemTwo";
    private Integer PERIOD_ID;
    private Integer OC_STATUS_ID;
    private static final String OC_STATUS = "orderTestChangeStatus_" + System.currentTimeMillis();

    private Date ACTIVE_SINCE = new GregorianCalendar(2010, 1, 1).getTime();
    private Date ACTIVE_UNTIL = new GregorianCalendar(2020, 1, 1).getTime();

    @BeforeClass
    public void setup() {

        super.setup("orders");

        // Create an account type
        accTypeRestHelper = RestOperationsHelper.getInstance("accounttypes");
        ACC_TYPE_ID = restTemplate.sendRequest(accTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAccountTypeMock(0, "orderTestAccType1" + random), AccountTypeWS.class).getBody().getId();
        // Create a user
        userRestHelper = RestOperationsHelper.getInstance("users");
        USER_ID = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildUserMock(USER_NAME  + random, ACC_TYPE_ID), UserWS.class).getBody().getId();
        // Create an item type
        itemTypeRestHelper = RestOperationsHelper.getInstance("itemtypes");
        ITEM_TYPE_ID = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock(ITEM_TYPE  + random, true, true), ItemTypeWS.class).getBody().getId();
        // Create two items
        itemRestHelper = RestOperationsHelper.getInstance("items");
        ITEM_ONE_ID = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock(ITEM_ONE, false, true, ITEM_TYPE_ID), ItemDTOEx.class).getBody().getId();
        ITEM_TWO_ID = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock(ITEM_TWO, false, true, ITEM_TYPE_ID), ItemDTOEx.class).getBody().getId();
        // Create an order period
        periodRestHelper = RestOperationsHelper.getInstance("orderperiods");
        PERIOD_ID = restTemplate.sendRequest(periodRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "orderTestPeriod"),
                OrderPeriodWS.class).getBody().getId();
        // Create an order change status
        ocStatusRestHelper = RestOperationsHelper.getInstance("orderchangestatuses");
        OC_STATUS_ID = restTemplate.sendRequest(ocStatusRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildOrderChangeStatus(null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), OC_STATUS),
                OrderChangeStatusWS.class).getBody().getId();
    }

    @AfterClass()
    public void teardown() {

        // Delete the two created items
        if (null != ITEM_ONE_ID) {
            restTemplate.sendRequest(
                    itemRestHelper.getFullRestUrl() + ITEM_ONE_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        if (null != ITEM_TWO_ID) {
            restTemplate.sendRequest(
                    itemRestHelper.getFullRestUrl() + ITEM_TWO_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        // Delete the created item type
        if (null != ITEM_TYPE_ID) {
            restTemplate.sendRequest(
                    itemTypeRestHelper.getFullRestUrl() + ITEM_TYPE_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        // Delete the created user
        if (null != USER_ID) {
            restTemplate.sendRequest(
                    userRestHelper.getFullRestUrl() + USER_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        // Delete the created account type
        if (null != ACC_TYPE_ID) {
            restTemplate.sendRequest(
                    accTypeRestHelper.getFullRestUrl() + ACC_TYPE_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getOrder() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Get the order and verify the response
            ResponseEntity<OrderWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
            assertEquals(getResponse.getBody(), postResponse.getBody(), "Orders do not match.");
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExistingOrder() {

        // Try to get a non-existing order and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void createOrder() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the order is generated
            ResponseEntity<OrderWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void createOrderError() {

        // Build an order and order changes with invalid user id
        OrderWS order = buildOrder();
        order.setUserId(Integer.MAX_VALUE);
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        // Try to create another order with the same name
        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Test
    public void updateOrder() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the order is generated
            ResponseEntity<OrderWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            assertNotNull(getResponse, "GET response should not be null.");

            // Update the order and verify the response
            order = getResponse.getBody();
            order.setActiveUntil(new GregorianCalendar(2025, 1, 1).getTime());
            orderInfo.setOrder(getResponse.getBody());
            orderInfo.setOrderChanges(new OrderChangeWS[0]);
            ResponseEntity<OrderWS> putResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
            assertEquals(putResponse.getBody(), order, "Orders do not match.");
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateNonExistingOrder() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        // Try to update a non-existing order and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT, postOrPutHeaders,
                    orderInfo, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateOrderError() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Try to update the order with invalid data
            order = postResponse.getBody();
            order.setUserId(Integer.MAX_VALUE);
            orderInfo.setOrder(order);
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, orderInfo, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void deleteOrder() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;
        ResponseEntity<OrderWS> getResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the order is generated
            getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }

        // Verify that the order is deleted
        getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
        assertEquals(getResponse.getBody().getDeleted(), 1);
    }

    @Test
    public void deleteNonExistingOrder() {

        // Try to delete a non-existing order and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders,
                    null, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getLatestOrder() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> firstPostResponse = null;
        ResponseEntity<OrderWS> secondPostResponse = null;

        try {
            // Create two orders
            firstPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            secondPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);

            // Try to get the latest order for the customer, and verify that it is the correct one
            ResponseEntity<OrderWS> getResponse = restTemplate.sendRequest(REST_URL + "/last/" + USER_ID,
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            assertEquals(getResponse.getBody().getId(), secondPostResponse.getBody().getId());
        } finally {
            // Delete the created orders
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
    public void getNonExsistingLatestOrder() {

        // Try to get the latest order for a user when one does not exist
        try {
            restTemplate.sendRequest(REST_URL + "/last/" + USER_ID, HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C35085642
     */
    @Test
    public void getNumberOfLastOrders() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> firstPostResponse = null;
        ResponseEntity<OrderWS> secondPostResponse = null;
        ResponseEntity<OrderWS> thirdPostResponse = null;

        try {
            // Create three orders
            firstPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            secondPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            thirdPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);

            // Try to get the last two orders, verify that the correct ones are returned
            ResponseEntity<OrderWS[]> getResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/last/" + USER_ID, new RestQueryParameter<>("number", Integer.valueOf(2))),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS[].class);
            assertEquals(getResponse.getBody().length, 2);
            List<Integer> orderIds = Arrays.stream(getResponse.getBody())
                    .map(OrderWS::getId)
                    .collect(Collectors.toList());
            assertTrue(orderIds.contains(secondPostResponse.getBody().getId()));
            assertTrue(orderIds.contains(thirdPostResponse.getBody().getId()));

            // Try to get a number of last orders greater then the number of orders existing for the user,
            // verify that all user orders are returned
            getResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/last/" + USER_ID, new RestQueryParameter<>("number", Integer.valueOf(10))),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS[].class);
            assertEquals(getResponse.getBody().length, 3);
            orderIds = Arrays.stream(getResponse.getBody())
                    .map(OrderWS::getId)
                    .collect(Collectors.toList());
            assertTrue(orderIds.contains(firstPostResponse.getBody().getId()));
            assertTrue(orderIds.contains(secondPostResponse.getBody().getId()));
            assertTrue(orderIds.contains(thirdPostResponse.getBody().getId()));
        } finally {
            // Delete the created orders
            if (null != firstPostResponse) {
                restTemplate.sendRequest(REST_URL + firstPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
            if (null != secondPostResponse) {
                restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
            if (null != thirdPostResponse) {
                restTemplate.sendRequest(REST_URL + thirdPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExsistingNumberOfLastOrders() {

        // Try to get the latest order for a user when one does not exist
        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/last/" + USER_ID, new RestQueryParameter<>("number", Integer.valueOf(10))),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS[].class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getNumberOfLastOrdersError() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Try to get the last orders for a user with invalid parameter
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/last/" + USER_ID, new RestQueryParameter<>("number", Integer.valueOf(-10))),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS[].class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }


    }

    @Test
    public void getPageOfUserOrders() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> firstPostResponse = null;
        ResponseEntity<OrderWS> secondPostResponse = null;
        ResponseEntity<OrderWS> thirdPostResponse = null;

        try {
            // Create three orders
            firstPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            secondPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            thirdPostResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);

            // Try to get user orders with pagination, limit = 1, offset = 1, verify the result
            ResponseEntity<OrderWS[]> getResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/page/" + USER_ID,
                    new RestQueryParameter<>("limit", Integer.valueOf(1)),
                    new RestQueryParameter<>("offset", Integer.valueOf(1))
                    ),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS[].class);
            assertEquals(getResponse.getBody().length, 1);
            assertEquals(getResponse.getBody()[0].getId(), secondPostResponse.getBody().getId());

            // Try to get user orders with pagination, limit = 10, offset = 1, verify the result
            getResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/page/" + USER_ID,
                    new RestQueryParameter<>("limit", Integer.valueOf(10)),
                    new RestQueryParameter<>("offset", Integer.valueOf(1))
                    ),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS[].class);
            assertEquals(getResponse.getBody().length, 2);
            List<Integer> orderIds = Arrays.stream(getResponse.getBody())
                    .map(OrderWS::getId)
                    .collect(Collectors.toList());
            assertTrue(orderIds.contains(firstPostResponse.getBody().getId()));
            assertTrue(orderIds.contains(secondPostResponse.getBody().getId()));

            // Try to get user orders with pagination, limit = 2, offset = 0, verify the result
            getResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/page/" + USER_ID,
                    new RestQueryParameter<>("limit", Integer.valueOf(2)),
                    new RestQueryParameter<>("offset", Integer.valueOf(0))
                    ),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS[].class);
            assertEquals(getResponse.getBody().length, 2);
            orderIds = Arrays.stream(getResponse.getBody())
                    .map(OrderWS::getId)
                    .collect(Collectors.toList());
            assertTrue(orderIds.contains(secondPostResponse.getBody().getId()));
            assertTrue(orderIds.contains(thirdPostResponse.getBody().getId()));
        } finally {
            // Delete the created orders
            if (null != firstPostResponse) {
                restTemplate.sendRequest(REST_URL + firstPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
            if (null != secondPostResponse) {
                restTemplate.sendRequest(REST_URL + secondPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
            if (null != thirdPostResponse) {
                restTemplate.sendRequest(REST_URL + thirdPostResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getNonExsistingPageOfUserOrders() {

        // Try to get orders for a user with pagination when none exist
        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/page/" + USER_ID,
                    new RestQueryParameter<>("limit", Integer.valueOf(10)),
                    new RestQueryParameter<>("offset", Integer.valueOf(10))
                    ),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getPageOfUserOrdersInalidLimit() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Try to get orders for a user with pagination with invalid parameters
            try {
                restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + "/page/" + USER_ID, new RestQueryParameter<>("limit", Integer.valueOf(0))),
                        HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
                fail("The test should fail before this line.");
            } catch (HttpStatusCodeException e) {
                assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            }
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getPageOfUserOrdersInvalidOffset() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Try to get orders for a user with pagination with invalid parameters
            try {
                restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + "/page/" + USER_ID, new RestQueryParameter<>("offset", Integer.valueOf(-10))),
                        HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
                fail("The test should fail before this line.");
            } catch (HttpStatusCodeException e) {
                assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            }
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void getPageOfUserOrdersInvalidLimitAndOffset() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            postResponse = restTemplate.sendRequest(
                    REST_URL, HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Try to get orders for a user with pagination with invalid parameters
            try {
                restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + "/page/" + USER_ID,
                        new RestQueryParameter<>("limit", Integer.valueOf(-10)),
                        new RestQueryParameter<>("offset", Integer.valueOf(-10))
                        ),
                        HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
                fail("The test should fail before this line.");
            } catch (HttpStatusCodeException e) {
                assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            }
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    private OrderWS buildOrder() {

        OrderBuilder orderBuilder = OrderBuilder.getBuilderWithoutEnv()
                .forUser(USER_ID)
                .withActiveSince(ACTIVE_SINCE)
                .withActiveUntil(ACTIVE_UNTIL)
                .withEffectiveDate(ACTIVE_SINCE)
                .withPeriod(PERIOD_ID)
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withProrate(Boolean.FALSE);

        orderBuilder.withOrderLine(orderBuilder.orderLine()
                .withItemId(ITEM_ONE_ID)
                .withQuantity(BigDecimal.ONE)
                .build());

        orderBuilder.withOrderLine(orderBuilder.orderLine()
                .withItemId(ITEM_ONE_ID)
                .withQuantity(BigDecimal.TEN)
                .build());

        return orderBuilder.buildOrder();
    }

    /**
     * C35085640
     */
    @Test
    public void getOrdersForUserThatDoNotExist() {
        // Try to get the latest order for a user which does not exist
        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "last/" + Integer.MAX_VALUE, new RestQueryParameter<>("number", Integer.valueOf(2))), HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    /**
     * C35085643
     */
    @Test
    public void getOrdersLimitForUserThatDoNotExist() {
        try {
            // Try to get invalid user orders with pagination, limit = 2, offset = 0,
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                    REST_URL + "/page/" + Integer.MAX_VALUE,
                    new RestQueryParameter<>("limit", Integer.valueOf(2)),
                    new RestQueryParameter<>("offset", Integer.valueOf(0))
                    ), HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderWS[].class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C35085638
     */
    @Test
    public void getOrdersForUserThatDoNtExist() {
        // Try to get the latest order for a user which does not exist without param
        try {
            restTemplate.sendRequest(REST_URL + "last/" + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders,
                    null, OrderWS.class);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }
}
