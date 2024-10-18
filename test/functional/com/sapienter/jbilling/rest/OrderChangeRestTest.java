package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.resources.OrderChangeUpdateRequest;
import com.sapienter.jbilling.resources.OrderInfo;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test(groups = {"rest"}, testName = "OrderChangeRestTest")
public class OrderChangeRestTest extends RestTestCase {
    
    private static Integer ENTITY_ID = Integer.valueOf(1);
    private static final Logger LOG = LoggerFactory.getLogger(OrderChangeRestTest.class);
    
    private RestOperationsHelper accTypeRestHelper;
    private RestOperationsHelper userRestHelper;
    private RestOperationsHelper itemTypeRestHelper;
    private RestOperationsHelper itemRestHelper;
    private RestOperationsHelper periodRestHelper;
    private RestOperationsHelper ocStatusRestHelper;
    private RestOperationsHelper orderRestHelper;

    private Integer ACC_TYPE_ID;
    private Integer USER_ID;
    private static final String USER_NAME = "OrderChangeUpdateTestUser-" + System.currentTimeMillis();
    private Integer ITEM_TYPE_ID;
    private static final String ITEM_TYPE = "orderTestItemType-" + System.currentTimeMillis();
    private Integer ITEM_ONE_ID;
    private static final String ITEM_ONE = "orderTestItemOne-" + System.currentTimeMillis();
    private Integer PERIOD_ID;
    private Integer OC_STATUS_ID;
    private static final String OC_STATUS = "orderTestChangeStatus_" + System.currentTimeMillis();

    private Date ACTIVE_SINCE = new GregorianCalendar(2010, 1, 1).getTime();
    private Date ACTIVE_UNTIL = new GregorianCalendar(2020, 1, 1).getTime();
    
    @BeforeClass
    public void setup() {

        super.setup("orderchange");

        // Create an account type
        accTypeRestHelper = RestOperationsHelper.getInstance("accounttypes");
        ACC_TYPE_ID = restTemplate.sendRequest(accTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAccountTypeMock(0, "orderTestAccType1"+System.currentTimeMillis()), AccountTypeWS.class).getBody().getId();
        // Create a user
        userRestHelper = RestOperationsHelper.getInstance("users");
        USER_ID = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildUserMock(USER_NAME, ACC_TYPE_ID), UserWS.class).getBody().getId();
        // Create an item type
        itemTypeRestHelper = RestOperationsHelper.getInstance("itemtypes");
        ITEM_TYPE_ID = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock(ITEM_TYPE, true, true), ItemTypeWS.class).getBody().getId();
        // Create two items
        itemRestHelper = RestOperationsHelper.getInstance("items");
        ITEM_ONE_ID = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock(ITEM_ONE, false, true, ITEM_TYPE_ID), ItemDTOEx.class).getBody().getId();
        // Create an order period
        periodRestHelper = RestOperationsHelper.getInstance("orderperiods");
        PERIOD_ID = restTemplate.sendRequest(periodRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "orderTestPeriod"),
                OrderPeriodWS.class).getBody().getId();
        // Create an order change status
        ocStatusRestHelper = RestOperationsHelper.getInstance("orderchangestatuses");
        OC_STATUS_ID = restTemplate.sendRequest(ocStatusRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildOrderChangeStatus(
                        null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), OC_STATUS),
                OrderChangeStatusWS.class).getBody().getId();
        orderRestHelper = RestOperationsHelper.getInstance("orders");
    }
    
    @AfterClass()
    public void teardown() {

        // Delete the two created items
        if (null != ITEM_ONE_ID) {
            restTemplate.sendRequest(
                    itemRestHelper.getFullRestUrl() + ITEM_ONE_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
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
    public void createOrderAndUpdateOrderChange() {

        // Build an order and order changes
        OrderWS order = buildOrder();
        LOG.debug("Build Order {}", order);
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID, ACTIVE_SINCE);
        LOG.debug("Build Order Changes {}", orderChanges);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);
        LOG.debug("Constructed OrderInfo {}", orderInfo);
        ResponseEntity<OrderWS> postResponse = null;

        try {
            // Create the order
            LOG.debug("Sending Order Creation Reequest to URL {}", REST_URL);
            postResponse = restTemplate.sendRequest(
                    orderRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
            LOG.debug("Created Order {}", postResponse.getBody());
            // Verify that the order is generated
            ResponseEntity<OrderWS> getResponse = restTemplate.sendRequest(orderRestHelper.getFullRestUrl() + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            
            LOG.debug("Creating OrderChangeUpdate Request ");
            OrderChangeUpdateRequest changeRequest = new OrderChangeUpdateRequest();
            changeRequest.setNewQuantity(BigDecimal.TEN);
            Calendar effectiveDate = Calendar.getInstance();
            effectiveDate.setTime(ACTIVE_SINCE);
            effectiveDate.add(Calendar.DATE, 1);
            changeRequest.setChangeEffectiveDate(effectiveDate.getTime());
            changeRequest.setUserId(USER_ID);
            changeRequest.setNewPrice(BigDecimal.TEN);
            changeRequest.setProductCode(ITEM_ONE);
            
            LOG.debug("OrderChange Created {}", changeRequest);
            
            ResponseEntity<String> putResponse = restTemplate.sendRequest(REST_URL, HttpMethod.PUT, 
                    postOrPutHeaders, changeRequest, String.class);
            LOG.debug("OrderChangeUpdate Response {}", putResponse.getBody());
            assertNotNull(putResponse, "GET response should not be null.");
            
            ResponseEntity<OrderChangeWS[]>  orderChangesResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderChangeWS[].class);
            assertNotNull(orderChangesResponse, "GET response should not be null.");
            
            OrderChangeWS changes[] = orderChangesResponse.getBody();
            assertNotNull(changes, "Order Changes should not be null.");
            assertEquals(changes.length, 2, "Order Should have two order changes");
            
            OrderChangeWS expiredChange = findChange(changes, orderChange -> orderChange.getEndDate()!=null);
            assertNotNull(expiredChange, "Old Order change should be expired!");
            
            OrderChangeWS newChange = findChange(changes, orderChange -> orderChange.getEndDate()==null);
            assertNotNull(newChange, "New Change should be created.");
            
            getResponse = restTemplate.sendRequest(orderRestHelper.getFullRestUrl() + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, OrderWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            
            OrderLineWS[] lines = getResponse.getBody().getOrderLines();
            assertNotNull(lines, "OrderLines should not be null.");
            assertEquals(lines.length, 1, "Order Should have one order line");
            OrderLineWS line = lines[0];
            assertNotNull(line, "OrderLine should not be null.");
            
            assertEquals(line.getQuantity(), "10.0000000000", "Line Quantity Should be 10");
            
            
        } finally {
            // Delete the order
            if (null != postResponse) {
                restTemplate.sendRequest(orderRestHelper.getFullRestUrl() + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }
    
    private OrderChangeWS findChange(OrderChangeWS[] changes, Predicate<OrderChangeWS> filter) {
        return Arrays.stream(changes)
                     .filter(filter)
                     .findFirst()
                     .orElse(null);
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

        return orderBuilder.buildOrder();
    }

}
