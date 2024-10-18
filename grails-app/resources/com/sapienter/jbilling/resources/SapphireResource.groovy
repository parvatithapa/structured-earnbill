package com.sapienter.jbilling.resources

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.GET
import javax.ws.rs.POST;
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType

import org.json.JSONObject
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanRequestWS;
import com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelperService;
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

@Path("/api/sapphire")
@Api(value="/api/sapphire", description = "Sappphire.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class SapphireResource {

    private static final String RESPONSE_MESSAGE = "changeOfPlan for old order [%d] successfully, new order [%d] created with newPlanCode[%s]"

    SapphireProvisioningHelperService sapphireProvisioningHelperService;

    @POST
    @Path("/changeofplan")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "change of plan.", response = String.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "plan change done.", response = String.class),
            @ApiResponse(code = 404, message = "order not found."),
            @ApiResponse(code = 500, message = "plan change failed.")
    ])
    Response changeOfPlan(@ApiParam(value = "ChangeOfPlanRequest.", required = true)  ChangeOfPlanRequestWS changeOfPlanRequest) {
        try {
            def newOrderId = sapphireProvisioningHelperService.changeOfPlan(changeOfPlanRequest)
            return Response.ok().entity(JSONObject.quote(String.format(RESPONSE_MESSAGE, changeOfPlanRequest.orderId, 
                newOrderId, changeOfPlanRequest.newPlanCode))).build()
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }
}
