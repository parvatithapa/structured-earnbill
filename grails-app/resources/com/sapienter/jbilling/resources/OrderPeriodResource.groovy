package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.OrderPeriodWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * @author Vojislav Stanojevikj
 * @since 16-Aug-2016.
 */
@Path("/api/orderperiods")
@Api(value="/api/orderperiods", description = "Order periods.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class OrderPeriodResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @ApiOperation(value = "Get all order periods.", response = OrderPeriodWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order periods found or empty.", response = OrderPeriodWS.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAllOrderPeriods(){
        try {
            return Response.ok().entity(webServicesSession.getOrderPeriods()).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get order period by id.", response = OrderPeriodWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order period type found.", response = OrderPeriodWS.class),
            @ApiResponse(code = 404, message = "Order period not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getOrderPeriodById(
            @ApiParam(name = "id",
                    value = "The id of the order period that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer id) {
        try{
            return Response.ok().entity(webServicesSession.getOrderPeriodWS(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create order period.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Order period created.", response = OrderPeriodWS.class),
            @ApiResponse(code = 400, message = "Invalid order period supplied."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response createOrderPeriod(
            @ApiParam(value = "Created order period object.", required = true)
                    OrderPeriodWS orderPeriod,
            @Context
                    UriInfo uriInfo){
        try {
            Integer orderPeriodId = webServicesSession.createOrderPeriod(orderPeriod);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(orderPeriodId)).build())
                    .entity(webServicesSession.getOrderPeriodWS(orderPeriodId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing order period.", response = OrderPeriodWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order period created.", response = OrderPeriodWS.class),
            @ApiResponse(code = 400, message = "Invalid order period supplied."),
            @ApiResponse(code = 404, message = "Order period with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateOrderPeriod(
            @ApiParam(name = "id", value = "Order period id that needs to be updated.", required = true)
            @PathParam("id")
                    Integer id,
            @ApiParam(value = "Order period object containing update data.", required = true)
                    OrderPeriodWS orderPeriod){

        try {
            if (null == webServicesSession.getOrderPeriodWS(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            orderPeriod.setId(id);
            webServicesSession.updateOrCreateOrderPeriod(orderPeriod);
            return Response.ok().entity(webServicesSession.getOrderPeriodWS(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes existing order period.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Order period with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Order period with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Deletion of the specified order period resulted with a conflict."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response deleteOrderPeriod(@ApiParam(name = "id",
           value = "The id of the order period that needs to be deleted.", required = true)
          @PathParam("id") Integer id){

        try {
            if (null == webServicesSession.getOrderPeriodWS(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            boolean deleted = webServicesSession.deleteOrderPeriod(id);
            if (!deleted){
                return Response.status(Response.Status.CONFLICT).build();
            }
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

}
