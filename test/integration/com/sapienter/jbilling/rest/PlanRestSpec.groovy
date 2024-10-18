package com.sapienter.jbilling.rest

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.PlanWS
import org.apache.http.HttpStatus

import javax.ws.rs.HttpMethod
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Vojislav Stanojevikj
 * @since 30-Sep-2016.
 */
class PlanRestSpec extends RestBaseSpec{

    def planResource

    def setup() {
        init(planResource, 'plans')
    }

    void "get all existing plans"(){

        given: 'The JSON of the plan that needs to be fetched.'
        def plansJSON = buildPlansJsonString(100, 102, 103, 104)
        def plans = BuilderHelper.buildWSMocks(plansJSON, PlanWS).toArray(new PlanWS[0])

        and: 'Mock the behaviour of the getAllPlans, and verify the number of calls.'
        1 * webServicesSessionMock.getAllPlans() >> plans
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, plansJSON)
    }

    void "get all existing plans empty"(){

        given: 'Mock the behaviour of the getAllPlans, and verify the number of calls.'
        1 * webServicesSessionMock.getAllPlans() >> new PlanWS[0]
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get all existing plans resulted with internal error"(){

        given: 'Mock the behaviour of the getAllPlans, and verify the number of calls.'
        1 * webServicesSessionMock.getAllPlans() >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get existing plan"() {

        given: 'The JSON of the plan that needs to be fetched.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())
        def plan = BuilderHelper.buildWSMock(planJson, PlanWS)

        and: 'Mock the behaviour of the getPlanWS, and verify the number of calls.'
        1 * webServicesSessionMock.getPlanWS(planId) >> plan
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, planJson)
    }

    void "try to fetch non existing plan"() {

        given: 'Just the id of the non existing plan'
        def planId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getPlanWS, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.getPlanWS(planId) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "get existing plan resulted with internal error"() {

        given: 'Just the id of the non existing plan'
        def planId = Integer.MAX_VALUE

        and: 'Mock the behaviour of the getPlanWS, and verify the number of calls.'
        1 * webServicesSessionMock.getPlanWS(planId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.GET, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create plan resulted with internal error"(){

        given: 'The JSON of the plan that needs to be created.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())

        and: 'Mock the behaviour of the createPlan, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createPlan(_ as PlanWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), planJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to create plan with invalid data"(){

        given: 'The JSON of the plan that needs to be created.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())

        and: 'Mock the behaviour of the createPlan, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.createPlan(_ as PlanWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), planJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, errorMessage,'')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "create plan"(){

        given: 'The JSON of the plan that needs to be created.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())
        def plan = BuilderHelper.buildWSMock(planJson, PlanWS)

        and: 'Mock the behaviour of the createPlan and getPlanWS, and verify the number of calls.'
        1 * webServicesSessionMock.createPlan(_ as PlanWS) >> planId
        1 * webServicesSessionMock.getPlanWS(planId) >> plan
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest(BASE_URL, HttpMethod.POST, RestApiHelper.buildJsonHeaders(), planJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.CREATED.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.LOCATION, 1, "http://localhost/${BASE_URL}/${planId}#")
        RestApiHelper.validateResponseJson(response, planJson)
    }

    void "try to update non existing plan"(){

        given: 'The JSON of the plan that needs to be updated.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())

        and: 'Mock the behaviour of the updatePlan, and verify the number of calls.'
        def errorMessage = 'Test Error Message'
        1 * webServicesSessionMock.updatePlan(_ as PlanWS) >> {throw new SessionInternalError("Test",
                [errorMessage] as String[], HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), planJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, errorMessage, '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "try to update plan with invalid data"(){

        given: 'The JSON of the plan that needs to be updated.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())

        and: 'Mock the behaviour of the updatePlan, and verify the number of calls.'
        1 * webServicesSessionMock.updatePlan(_ as PlanWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_BAD_REQUEST)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), planJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.BAD_REQUEST.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_BAD_REQUEST, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update plan resulted with internal error"(){

        given: 'The JSON of the plan that needs to be updated.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())

        and: 'Mock the behaviour of the updatePlan, and verify the number of calls.'
        1 * webServicesSessionMock.updatePlan(_ as PlanWS) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), planJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
    }

    void "update plan"(){

        given: 'The JSON of the plan that needs to be updated.'
        def planId = Integer.valueOf(1)
        def planJson = buildPlanJsonString(planId.intValue())
        def plan = BuilderHelper.buildWSMock(planJson, PlanWS)

        and: 'Mock the behaviour of the updatePlan and getPlanWS, and verify the number of calls.'
        1 * webServicesSessionMock.updatePlan(_ as PlanWS)
        1 * webServicesSessionMock.getPlanWS(planId) >> plan
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.PUT,
                RestApiHelper.buildJsonHeaders(), planJson.bytes)

        then: 'Validate the response.'
        response.status == Response.Status.OK.statusCode
        RestApiHelper.validateResponseHeaders(response, HttpHeaders.CONTENT_TYPE, 1, MediaType.APPLICATION_JSON)
        RestApiHelper.validateResponseJson(response, planJson)
    }

    void "try to delete plan that do not exist"(){

        given: 'The id of the plan that do not exist'
        def planId = Integer.valueOf(1)

        and: 'Mock the behaviour of the deletePlan, and verify the number of calls.'
        1 * webServicesSessionMock.deletePlan(planId) >> {throw new SessionInternalError("Test", HttpStatus.SC_NOT_FOUND)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.NOT_FOUND.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_NOT_FOUND, '', '')
    }

    void "try to delete plan that can not be deleted"(){

        given: 'The id of the item that needs to be deleted.'
        def planId = Integer.valueOf(1)

        and: 'Mock the behaviour of the deletePlan, and verify the number of calls.'
        1 * webServicesSessionMock.deletePlan(planId) >> {throw new SessionInternalError("Test", HttpStatus.SC_INTERNAL_SERVER_ERROR)}
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}",
                HttpMethod.DELETE, RestApiHelper.buildJsonHeaders(false))

        then: 'Validate the response.'
        response.status == Response.Status.INTERNAL_SERVER_ERROR.statusCode
        RestApiHelper.validateResponseErrorJSONBody(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, '', '')
    }

    void "delete plan"(){

        given: 'The id of the plan that needs to be deleted.'
        def planId = Integer.valueOf(1)

        and: 'Mock the behaviour of the deletePlan, and verify the number of calls.'
        1 * webServicesSessionMock.deletePlan(planId)
        0 * webServicesSessionMock._

        when: 'Initialize the http request and parameters.'
        sendRequest("${BASE_URL}/${planId.intValue()}", HttpMethod.DELETE)

        then: 'Validate the response.'
        response.status == Response.Status.NO_CONTENT.statusCode
        response.getText() == ''
    }

    private static buildPlanJsonString(id){

        """
           {
              "id": ${id},
              "itemId": ${BuilderHelper.random.nextInt(Integer.MAX_VALUE)},
              "periodId": 1,
              "description": "TestPlan-${id}",
              "planItems": [
                {
                  "itemId": ${BuilderHelper.random.nextInt(Integer.MAX_VALUE)},
                  "models": {
                      "${new Date().getTime()}":${BuilderHelper.buildFlatPriceJson(Integer.valueOf(100))}
                  },
                  "model": ${BuilderHelper.buildFlatPriceJson(Integer.valueOf(100))},
                  "bundle": {
                    "quantity": 1,
                    "periodId": 1,
                    "targetCustomer": "SELF",
                    "addIfExists": false
                  },
                  "precedence": -1
                }
              ],
              "metaFieldsMap": {}
            }
        """
    }

    private static buildPlansJsonString(id, int... ids){
        StringBuilder sb = new StringBuilder("[${buildPlanJsonString(id)}")
        ids.each {
            sb.append(",${buildPlanJsonString(it)}")
        }
        sb.append(']').toString()
    }

}
