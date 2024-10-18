package com.sapienter.jbilling.resources

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET
import javax.ws.rs.POST;
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response
import javax.ws.rs.Consumes

import org.json.JSONObject

import com.sapienter.jbilling.common.ErrorDetails;
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.SwapAssetWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanRequestWS;

import com.sapienter.jbilling.server.sapphire.NewSaleRequestWS;
import com.sapienter.jbilling.server.sapphire.SapphireResponseWS;
import com.sapienter.jbilling.server.sapphire.SwapAssetResponse;
import com.sapienter.jbilling.server.sapphire.provisioninig.SapphireHelperService;
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

import static javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path("/api/sapphire")
@Api(value="/api/sapphire", description = "Sappphire.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class SapphireResource {

    private static final String RESPONSE_MESSAGE = "changeOfPlan for old order [%d] successfully, new order [%d] created with newPlanCode[%s]"

    SapphireHelperService sapphireHelperService;

    @POST
    @Path("/changeofplan")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "change of plan.", response = String.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "plan change done.", response = String.class),
            @ApiResponse(code = 404, message = "order not found."),
            @ApiResponse(code = 500, message = "plan change failed.")
    ])
    Response changeOfPlan(@ApiParam(value = "ChangeOfPlanRequest.", required = true)  ChangeOfPlanRequestWS changeOfPlanRequest) {
        try {
            def newOrderId = sapphireHelperService.changeOfPlan(changeOfPlanRequest)
            return Response.ok().entity(JSONObject.quote(String.format(RESPONSE_MESSAGE, changeOfPlanRequest.orderId, 
                newOrderId, changeOfPlanRequest.newPlanCode))).build()
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error)
        }
    }

    @POST
    @Path("/swapassets/{orderId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Swap Assets.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order successfully updated.", response = SwapAssetResponse.class),
        @ApiResponse(code = 400, message = "Invalid Asset Swap Request Passed.", response = ErrorDetails.class),
        @ApiResponse(code = 404, message = "Order not found."),
        @ApiResponse(code = 500, message = "Failure while swapping assets on the order.")
    ])
    Response swapAssets(
            @ApiParam(name = "orderId", value = "The id of the order that should be updated") @PathParam("orderId") Integer orderId,
            @ApiParam(value = "JSON represenation of SwapAsset Request.", required = true)
            SwapAssetWS[] swapAssetRequests
    ) {
        try {
            return Response.ok().entity(sapphireHelperService.swapAssets(orderId, swapAssetRequests)).build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }

    }

    @POST
    @Path("/newsale")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "New Sale Request.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order successfully Created.", response = OrderWS.class),
        @ApiResponse(code = 400, message = "Invalid NewSale Request Passed.", response = ErrorDetails.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "Failure while processing new sale request.", response = ErrorDetails.class)
    ])
    Response newSale(@ApiParam(value = "JSON represenation of NewSale Request.", required = true) NewSaleRequestWS newSaleRequestWS) {
        try {
            return Response.ok().entity(sapphireHelperService.processNewSale(newSaleRequestWS)).build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Path("/category/{categoryId}/status/{assetStatus}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Retrieve assets by category and status.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Assets found for Category.", response = SapphireResponseWS[].class),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 404, message = "Category not found."),
        @ApiResponse(code = 500, message = "Failure while retrieving assets.")
    ])
    Response getAssetsByCategoryAndStatus( @PathParam("categoryId")
            @ApiParam(name="categoryId", value=  "categoryId", required = true) Integer categoryId,
            @PathParam("assetStatus") @ApiParam(name="assetStatus", required = true) String assetStatus,
            @DefaultValue("10000")@QueryParam("limit") @ApiParam(name="limit", value = "limit") Integer limit,
            @DefaultValue("0")@QueryParam("offset") @ApiParam(name="offset", value = "offset") Integer offset) {
        try {
            return Response.ok()
                           .entity(sapphireHelperService.getAssetsByCategoryAndStatus(categoryId, assetStatus, limit, offset))
                           .build()
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }
}
