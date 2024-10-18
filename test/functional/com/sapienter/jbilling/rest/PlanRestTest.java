package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.util.Constants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 25-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "PlanRestTest")
public class PlanRestTest extends RestTestCase{

    private static final String TESTING_ITEM_TYPE_NAME = "RestTestCategory";
    private static final String TESTING_ITEM_NAME = "RestTestItem";
    private static final String TESTING_SUBSCRIPTION_ITEM_NAME = "RestTestSubscriptionItem";
    private static final String TESTING_PLAN_NAME = "RestTestPlan";

    private Integer TESTING_ITEM_TYPE_ID;
    private Integer TESTING_ITEM_ID;
    private Integer TESTING_ORDER_PERIOD_ID;
    private RestOperationsHelper itemTypeRestHelper;
    private RestOperationsHelper itemRestHelper;
    private RestOperationsHelper orderPeriodRestHelper;

    @BeforeClass
    public void setup(){
        super.setup("plans");
        itemRestHelper = RestOperationsHelper.getInstance("items");
        itemTypeRestHelper = RestOperationsHelper.getInstance("itemtypes");
        orderPeriodRestHelper = RestOperationsHelper.getInstance("orderperiods");

        TESTING_ITEM_TYPE_ID = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemTypeMock(TESTING_ITEM_TYPE_NAME, true, true), ItemTypeWS.class)
                .getBody().getId();

        TESTING_ITEM_ID = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_ITEM_NAME, true, true, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        TESTING_ORDER_PERIOD_ID = restTemplate.sendRequest(orderPeriodRestHelper.getFullRestUrl(), HttpMethod.POST,
                RestOperationsHelper.appendHeaders(restHelper.getAuthHeaders(), RestOperationsHelper.getJSONHeaders(true, true)),
                RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "NewMonth"), OrderPeriodWS.class)
                .getBody().getId();
    }

    @AfterClass
    public void tearDown(){
        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + TESTING_ITEM_ID, HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl() + TESTING_ITEM_TYPE_ID, HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(orderPeriodRestHelper.getFullRestUrl() + TESTING_ORDER_PERIOD_ID, HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void postPlan(){

        Integer subscriptionItemId = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_SUBSCRIPTION_ITEM_NAME, false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        ResponseEntity<PlanWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPlanMock(subscriptionItemId, TESTING_PLAN_NAME, TESTING_ORDER_PERIOD_ID,
                        new RestPlanItem(TESTING_ITEM_ID, TESTING_ORDER_PERIOD_ID, BigDecimal.ONE, BigDecimal.TEN)), PlanWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        PlanWS postedPlan = postResponse.getBody();
        ResponseEntity<PlanWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedPlan.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, PlanWS.class);
        PlanWS fetchedPlan = fetchedResponse.getBody();

        assertEquals(fetchedPlan, postedPlan, "Plans do not match!");

        restTemplate.sendRequest(REST_URL + postedPlan.getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + subscriptionItemId, HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void updatePlan(){

        Integer subscriptionItemId = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_SUBSCRIPTION_ITEM_NAME, false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        ResponseEntity<PlanWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPlanMock(subscriptionItemId, TESTING_PLAN_NAME, TESTING_ORDER_PERIOD_ID,
                        new RestPlanItem(TESTING_ITEM_ID, TESTING_ORDER_PERIOD_ID, BigDecimal.ONE, BigDecimal.TEN)), PlanWS.class);

        Integer newPlanItemId = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock("NewPlanItem", false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        PlanWS updatedPlan = postResponse.getBody();
        updatedPlan.addPlanItem(RestEntitiesHelper.buildPlanItemMock(
                new RestPlanItem(newPlanItemId, TESTING_ORDER_PERIOD_ID, BigDecimal.ONE, BigDecimal.valueOf(100))));

        ResponseEntity<PlanWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedPlan.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedPlan, PlanWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<PlanWS> fetchedPlanResponse = restTemplate.sendRequest(REST_URL + updatedPlan.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, PlanWS.class);

        assertEquals(fetchedPlanResponse.getBody(), updatedResponse.getBody(), "Plans do not match!");

        restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + newPlanItemId, HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + subscriptionItemId, HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void updatePlanThatDoNotExists(){

        Integer subscriptionItemId = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_SUBSCRIPTION_ITEM_NAME, false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildPlanMock(subscriptionItemId, TESTING_PLAN_NAME, TESTING_ORDER_PERIOD_ID,
                            new RestPlanItem(TESTING_ITEM_ID, TESTING_ORDER_PERIOD_ID, BigDecimal.ONE, BigDecimal.TEN)), PlanWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        } finally {
            restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + subscriptionItemId, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getAllPlans(){

        ResponseEntity<List<PlanWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertNotNull(response, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());

        int initialNumberOfEntities = response.getBody().size();

        Integer subscriptionItemId = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_SUBSCRIPTION_ITEM_NAME, false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        ResponseEntity<PlanWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders,RestEntitiesHelper.buildPlanMock(subscriptionItemId, TESTING_PLAN_NAME, TESTING_ORDER_PERIOD_ID,
                        new RestPlanItem(TESTING_ITEM_ID, TESTING_ORDER_PERIOD_ID, BigDecimal.ONE, BigDecimal.TEN)), PlanWS.class);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertEquals(initialNumberOfEntities + 1, response.getBody().size(), "Initial number of plans did not increased!");

        restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + subscriptionItemId, HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void getPlan(){

        Integer subscriptionItemId = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_SUBSCRIPTION_ITEM_NAME, false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        ResponseEntity<PlanWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders,RestEntitiesHelper.buildPlanMock(subscriptionItemId, TESTING_PLAN_NAME, TESTING_ORDER_PERIOD_ID,
                        new RestPlanItem(TESTING_ITEM_ID, TESTING_ORDER_PERIOD_ID, BigDecimal.ONE, BigDecimal.TEN)), PlanWS.class);

        ResponseEntity<PlanWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, PlanWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        assertEquals(fetchedResponse.getBody(), postResponse.getBody(), "Plans do not match!");

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + subscriptionItemId, HttpMethod.DELETE,
                getOrDeleteHeaders, null);
    }

    @Test
    public void getPlanThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET,
                    getOrDeleteHeaders, null, PlanWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deletePlan(){

        Integer subscriptionItemId = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildItemMock(TESTING_SUBSCRIPTION_ITEM_NAME, false, false, TESTING_ITEM_TYPE_ID), ItemDTOEx.class)
                .getBody().getId();

        ResponseEntity<PlanWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPlanMock(subscriptionItemId, TESTING_PLAN_NAME, TESTING_ORDER_PERIOD_ID,
                        new RestPlanItem(TESTING_ITEM_ID, TESTING_ORDER_PERIOD_ID, BigDecimal.ONE, BigDecimal.TEN)), PlanWS.class);

        ResponseEntity<ItemDTOEx> deletedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        try {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                    getOrDeleteHeaders, null, PlanWS.class);
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        } finally {
            restTemplate.sendRequest(itemRestHelper.getFullRestUrl() + subscriptionItemId, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }

    }

    @Test
    public void deletePlanThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

}
