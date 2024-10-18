package com.sapienter.jbilling.resources

import com.sapienter.jbilling.resources.OrderChangeUpdateRequest
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PathParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import static javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path('/api/orderchange')
@Api(value = "/api/orderchange", description = "Order Changes")
@Produces(MediaType.APPLICATION_JSON)
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class OrderChangeResource {

    IWebServicesSessionBean webServicesSession;
    
    // Render Response of Update Order change api call.
    private static final String JSON_RESPONSE_FORMAT = "{ \"message\" : \"Order Change successfully updated for user  %s\"}"


    @PUT
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update OrderChange .")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order Change successfully updated."),
        @ApiResponse(code = 500, message = "Failure while updating the order change.")
    ])
    Response createUpdateOrderChange(
            @ApiParam(name = "OrderChangeUpdateRequest",
            value = "OrderChangeUpdateRequest to be applied on order.",
            required = true)
            OrderChangeUpdateRequest changeRquest) {
        try {
            webServicesSession.createUpdateOrderChange(changeRquest.getUserId(), changeRquest.getProductCode(),
                    changeRquest.getNewPrice(), changeRquest.getNewQuantity(), changeRquest.getChangeEffectiveDate())
            return Response.ok()
                    .entity(String.format(JSON_RESPONSE_FORMAT, changeRquest.getUserId()))
                    .build()
        } catch(Exception ex) {
            return RestErrorHandler.mapErrorToHttpResponse(ex)
        }
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get order chnages by OrderId.", response = OrderChangeWS[].class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order Changes successfully returned.", response = OrderChangeWS[].class),
            @ApiResponse(code = 404, message = "Order Changes not found."),
            @ApiResponse(code = 500, message = "Failure while getting the order changes.")
    ])
    Response getOrderOrderChanges(
            @ApiParam(name = "id",
                    value = "The ID of the order that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer orderId) {
            try {
               def changes = webServicesSession.getOrderChanges(orderId)
               if(changes) {
                   return Response.ok().entity(changes).build();
               } else {
                   return Response.status(Response.Status.NOT_FOUND).build()
               }

            } catch(Exception ex) {
                return RestErrorHandler.mapErrorToHttpResponse(ex)
            }
    }
}
