package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 20-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "OrderPeriodRestTest")
public class OrderPeriodRestTest extends RestTestCase{

    @BeforeClass
    public void setup(){
        super.setup("orderperiods");
    }

    @Test
    public void postOrderPeriod(){

        ResponseEntity<OrderPeriodWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "NewMonth"), OrderPeriodWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        OrderPeriodWS postedOrderPeriod = postResponse.getBody();
        ResponseEntity<OrderPeriodWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedOrderPeriod.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, OrderPeriodWS.class);
        OrderPeriodWS fetchedPeriod = fetchedResponse.getBody();

        assertEquals(fetchedPeriod, postedOrderPeriod, "Periods do not match!");

        restTemplate.sendRequest(REST_URL + postedOrderPeriod.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void postBadOrderPeriod(){
        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), ""), OrderPeriodWS.class);
            fail("No no");
        } catch (HttpClientErrorException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void getOrderPeriod(){

        ResponseEntity<OrderPeriodWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "NewMonth"), OrderPeriodWS.class);
        OrderPeriodWS postedOrderPeriod = postResponse.getBody();

        ResponseEntity<OrderPeriodWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedOrderPeriod.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, OrderPeriodWS.class);
        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
        OrderPeriodWS fetchedOrderPeriod = fetchedResponse.getBody();

        assertEquals(fetchedOrderPeriod, postedOrderPeriod, "Order periods do not match!!");

        restTemplate.sendRequest(REST_URL + postedOrderPeriod.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getOrderPeriodThatDoNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders, null, OrderPeriodWS.class);
        } catch (HttpClientErrorException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getAllOrderPeriods(){

        ResponseEntity<List<OrderPeriodWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertNotNull(response, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());

        int initialNumberOfEntities = response.getBody().size();

        ResponseEntity<OrderPeriodWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "NewMonth"), OrderPeriodWS.class);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertEquals(initialNumberOfEntities + 1, response.getBody().size(), "Initial number of order periods did not increased!");

        restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertEquals(initialNumberOfEntities, response.getBody().size(), "Current number of order periods did not decreased!");
    }

    @Test
    public void deleteOrderPeriod(){

        ResponseEntity<OrderPeriodWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "NewMonth"), OrderPeriodWS.class);

        ResponseEntity<List<OrderPeriodWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        int currentNumberOfEntities = response.getBody().size();

        ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertEquals(currentNumberOfEntities - 1, response.getBody().size(), "Current number of account types did not decreased!");
    }

    @Test
    public void deleteOrderPeriodThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    // ToDo when order rest end points are added, create an order for new order period and try to delete that one
    // For now this test tries to delete one time order period, probably there are orders with this order period.
    @Test
    public void deleteOrderPeriodThatCanNotBeDeleted(){

        try {
            restTemplate.sendRequest(REST_URL + Constants.ORDER_PERIOD_ONCE, HttpMethod.DELETE, getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.CONFLICT.getStatusCode());
        }
    }

    @Test
    public void updateOrderPeriod(){

        OrderPeriodWS mock = RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "NewMonth");

        ResponseEntity<OrderPeriodWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, mock, OrderPeriodWS.class);

        OrderPeriodWS updatedMock = postedResponse.getBody();
        updatedMock.setDescriptions(Arrays.asList(new InternationalDescriptionWS("description", RestEntitiesHelper.TEST_LANGUAGE_ID, "UpdatedNewMonth")));

        ResponseEntity<OrderPeriodWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedMock, OrderPeriodWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<OrderPeriodWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, OrderPeriodWS.class);

        assertEquals(fetchedResponse.getBody(), updatedMock, "Order periods do not match!");

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void updateOrderPeriodWithInvalidData(){

        OrderPeriodWS mock = RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "NewMonth");

        ResponseEntity<OrderPeriodWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, mock, OrderPeriodWS.class);

        OrderPeriodWS updatedMock = postedResponse.getBody();
        updatedMock.setDescriptions(Arrays.asList(new InternationalDescriptionWS("description", RestEntitiesHelper.TEST_LANGUAGE_ID, "")));

        try {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT, postOrPutHeaders, updatedMock, OrderPeriodWS.class);
        } catch (HttpClientErrorException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }

    }

}
