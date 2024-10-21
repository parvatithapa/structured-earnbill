/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.resources


import com.sapienter.jbilling.server.adennet.AdennetHelperService
import com.sapienter.jbilling.server.adennet.ws.AdennetBankPlanWS
import com.sapienter.jbilling.server.adennet.ws.AdennetPlanWS
import com.sapienter.jbilling.server.adennet.ws.ConsumptionUsageDetailsWS
import com.sapienter.jbilling.server.adennet.ws.PlanChangeRequestWS
import com.sapienter.jbilling.server.adennet.ws.RechargeRequestWS
import com.sapienter.jbilling.server.adennet.ws.RechargeResponseWS
import com.sapienter.jbilling.server.adennet.ws.RechargeWS
import com.sapienter.jbilling.server.adennet.ws.UserAndAssetAssociationResponseWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import org.springframework.security.access.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Response
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.ORDER_LEVEL_SUBSCRIPTION_ORDER_ID_MF_NAME
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import org.apache.commons.lang.StringUtils


@Path("/api/adennet")
@Api(value = "/api/adennet", description = "adennet")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class AdennetResource {

    public static final String CRM_ORDER_ID = "crmOrderID"
    IWebServicesSessionBean webServicesSession

    AdennetHelperService adennetHelperService

    @GET
    @Path("/users/asset/{subscriber-number}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve user by subscriber number.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Fetch user by subscriber number.", response = UserAndAssetAssociationResponseWS.class),
            @ApiResponse(code = 404, message = "Record not found."),
            @ApiResponse(code = 500, message = "Failure while retrieving record.")
    ])
    Response getUserIdForAssetIdentifier(
            @ApiParam(value = "subscriberNumber", required = true)
            @PathParam("subscriber-number") String subscriberNumber) {
        try {
            UserAndAssetAssociationResponseWS response = adennetHelperService.getUserAssetAssociationsBySubscriberNumber(subscriberNumber)
            log.info("Successfully retrieved user asset association for subscriber number: ${subscriberNumber}")
            return Response.ok().entity(response).build()
        } catch (Exception exception) {
            log.error("Failed to retrieve user asset association for subscriber number: ${subscriberNumber}", exception)
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }

    @GET
    @Path("/users/{userId}/plan")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve plan details by userId", response = AdennetPlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "User plan details per subscriber number", response = AdennetPlanWS.class),
            @ApiResponse(code = 400, message = "Invalid input supplied."),
            @ApiResponse(code = 404, message = "User with the provided id not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getPlansByUserId(
            @ApiParam(value = "userId", required = true) @PathParam("userId") Integer userId
    ) {
        try {
            def planResponseWS = fetchPlanByUserIdAndSubscriberNumber(userId, null)

            return planResponseWS != null
                    ? (Response.ok().entity(planResponseWS).build())
                    : Response.status(Response.Status.NOT_FOUND).build()
        } catch (Exception exception) {
            log.error("Failed to retrieve plan details for userId: ${userId}, exception: ${exception.message}", exception)
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }

    @GET
    @Path("/users/{userId}/plan/{subscriber-number}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve plan details by userId and subscriber number", response = AdennetPlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "User plan details per subscriber number", response = AdennetPlanWS.class),
            @ApiResponse(code = 400, message = "Invalid input supplied."),
            @ApiResponse(code = 404, message = "User with the provided id not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getPlanByUserIdAndSubscriberNumber(
            @ApiParam(value = "userId", required = true) @PathParam("userId") Integer userId,
            @ApiParam(value = "subscriberNumber", required = true) @PathParam("subscriber-number") String subscriberNumber
    ) {
        try {
            def planResponseWS = fetchPlanByUserIdAndSubscriberNumber(userId, subscriberNumber)

            return planResponseWS != null
                    ? (Response.ok().entity(planResponseWS).build())
                    : Response.status(Response.Status.NOT_FOUND).build()
        } catch (Exception exception) {
            log.error("Failed to retrieve plan details for userId: ${userId}, subscriberNumber: ${subscriberNumber}, exception: ${exception.message}", exception)
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }

    private AdennetPlanWS fetchPlanByUserIdAndSubscriberNumber(int userId, String subscriberNumber) {
        return adennetHelperService.getActivePlanByAssetIdentifierAndUserID(userId, subscriberNumber)
    }

    @PUT
    @Path("/users/plan")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation("Update plan")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plan updated successfully"),
            @ApiResponse(code = 404, message = "Plan not found"),
            @ApiResponse(code = 500, message = "Failure while changing the plan")
    ])
    Response updatePlan(@ApiParam(name = "PlanChangeRequest", required = true) PlanChangeRequestWS planChangeRequestWS) {
        String subscriberNumber = null
        Integer orderId = null
        RechargeWS rechargeWS = new RechargeWS()
        try {
            subscriberNumber = planChangeRequestWS?.subscriptions?.get(0)?.number

            log.info("Change plan request received for subscriberNumber=${subscriberNumber}, userId=${planChangeRequestWS?.userId}")
            if (adennetHelperService.isSubscriberNumberActive(subscriberNumber)) {
                String userName = adennetHelperService.getUserNameByUserId(planChangeRequestWS?.userId)

                rechargeWS.with {
                    entityId = planChangeRequestWS?.entityId
                    userId = planChangeRequestWS?.userId
                    planId = planChangeRequestWS?.subscriptions?.get(0)?.plan?.id
                    identifier = userName
                    activeSince = convertStringToDate(planChangeRequestWS?.startDate)
                    activeUntil = convertStringToDate(planChangeRequestWS?.endDate)
                }

                orderId = webServicesSession.associateAssetAndPlanWithCustomer(rechargeWS, planChangeRequestWS.previousOrderId)
                log.info("Plan changed for subscriberNumber=${subscriberNumber}, orderId=${orderId}")
            }
            if (orderId) {
                // Create payment
                RechargeRequestWS rechargeRequestWS = RechargeRequestWS.builder()
                        .entityId(rechargeWS.entityId)
                        .userId(rechargeWS.userId)
                        .primaryPlan(adennetHelperService.getPrimaryPlanWS(rechargeWS.planId))
                        .build()

                adennetHelperService.createPayment(rechargeRequestWS)
                log.info("Payment recived for userID=${planChangeRequestWS.userId}, with amount=${rechargeRequestWS.primaryPlan.price}")

                return Response.ok(orderId, APPLICATION_JSON).build()
            } else {
                log.warn("Order ID is null for subscriberNumber=${subscriberNumber}, userId=${planChangeRequestWS?.userId}")
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Order ID is null").build()
            }
        } catch (Exception exception) {
            log.error("Change plan failed for userId: ${planChangeRequestWS.userId}, subscriberNumber: ${subscriberNumber}, exception: ${exception.message}", exception)
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }

    private static Date convertStringToDate(String stringDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        LocalDateTime localDateTime = LocalDateTime.parse(stringDate, formatter)
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
    }

    @POST
    @Path("/recharge")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Do recharge")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "RechargeRequestWS created.", response = String.class),
            @ApiResponse(code = 400, message = "Invalid input supplied."),
            @ApiResponse(code = 500, message = "The call resulted in an internal error.")])

    Response recharge(@ApiParam(value = "Created RechargeWS object.", required = true) RechargeRequestWS rechargeRequestWS) {
        RechargeResponseWS rechargeResponse = null
        try {
            // Perform recharge
            rechargeResponse = adennetHelperService.recharge(rechargeRequestWS)
            def orderId = rechargeResponse?.planOrderId
            log.info("Order ${orderId} created for user=${rechargeRequestWS?.userId} with subscriberNumber=${rechargeRequestWS?.subscriberNumber}")
            Map<String, Object> additionalFields = rechargeRequestWS?.additionalFields
            log.info("Additional Fields: " + additionalFields)
            String crmOrderID = (String) additionalFields.get(CRM_ORDER_ID);
            if (StringUtils.isNotBlank(crmOrderID)) {
                MetaFieldValueWS mfValue = new MetaFieldValueWS();
                mfValue.setFieldName(CRM_ORDER_ID);
                mfValue.setValue(crmOrderID);
                if (orderId) {
                    webServicesSession.updateOrderMetaFields(orderId, [mfValue] as MetaFieldValueWS[]);
                }
            }

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("transactionId", rechargeResponse.transactionId);
            responseMap.put("orderId", orderId);

            return Response.ok().entity(rechargeResponse).build()
        } catch (Exception exception) {
            log.error("Failed to create recharge for user=${rechargeRequestWS?.userId}, exception=${exception.message}", exception)
            if (rechargeResponse) {
                adennetHelperService.rollbackRechargeResponse(rechargeResponse)
                log.info("Rollback the order=${rechargeResponse?.orderIds?.get(0)} for user=${rechargeRequestWS?.userId}")
            }
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }

    @GET
    @Path("/plans")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get all plans.", response = AdennetBankPlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plans found or empty.", response = AdennetBankPlanWS.class),
            @ApiResponse(code = 500, message = "The call resulted in an internal error.")])
    Response getAllPlans() {
        try {
            List<AdennetBankPlanWS> plans = adennetHelperService.getAllPlans()
            log.info("Retrieved ${plans.size()} plans from the service.")
            return Response.ok().entity(plans).build()
        } catch (Exception exception) {
            log.error("Failed to retrieve plans due to an exception: ${exception.message}", exception)
            return RestErrorHandler.mapErrorToHttpResponse(exception);
        }
    }
}
