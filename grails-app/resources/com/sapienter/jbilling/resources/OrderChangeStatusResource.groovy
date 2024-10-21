package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.order.OrderChangeStatusWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

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
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.core.UriInfo

@Path('/api/orderchangestatuses')
@Api(value = "/api/orderchangestatuses", description = "Order change status.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class OrderChangeStatusResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all order change statuses.", response = OrderChangeStatusWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order change statuses found or empty.", response = OrderChangeStatusWS.class),
            @ApiResponse(code = 500, message = "Failure while getting order change statuses")
    ])
    Response getAllOrderChangeStatuses(){

        OrderChangeStatusWS[] orderChangeStatuses;
        try {
            orderChangeStatuses = webServicesSession.getOrderChangeStatusesForCompany();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return (null == orderChangeStatuses || orderChangeStatuses.length == 0) ?
                Response.ok().build() :
                Response.ok().entity(orderChangeStatuses).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create order change status.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Order change status created.", response = OrderChangeStatusWS.class),
            @ApiResponse(code = 400, message = "Invalid order change status. Content of the international description must be unique"),
            @ApiResponse(code = 500, message = "Failure while creating the order change status")
    ])
    Response createOrderChangeStatus(
            @ApiParam(value = "Order change status object. Content of international description must be unique!", required = true)
                    OrderChangeStatusWS orderChangeStatusWS,
            @Context
                    UriInfo uriInfo) {

        Integer orderChangeStatusId;
        try {
            orderChangeStatusId = webServicesSession.createOrderChangeStatus(orderChangeStatusWS);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return orderChangeStatusId != null ?
                Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(orderChangeStatusId)).build())
                        .entity(getOrderChangeStatusById(orderChangeStatusId)).build() :
                Response.status(Response.Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing order change status.", response = OrderChangeStatusWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order Change Status updated.", response = OrderChangeStatusWS.class),
            @ApiResponse(code = 400, message = "Invalid Order Change Status. Content of International Description must be unique"),
            @ApiResponse(code = 404, message = "Order change status not found."),
            @ApiResponse(code = 500, message = "Failure while updating the order change status")
    ])
    Response updateOrderChangeStatus(
            @ApiParam(name = "id", value = "Order Change Status Id.",required = true)@PathParam("id")Integer id,
            @ApiParam(value = "Order Change Status containing update data.", required = true)OrderChangeStatusWS orderChangeStatus){
        try {
            if (null == getOrderChangeStatusById(id)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            orderChangeStatus.setId(id);
            webServicesSession.updateOrderChangeStatus(orderChangeStatus);
            return Response.ok().entity(getOrderChangeStatusById(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes existing order change status.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Order change status with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Order change status with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "Failure while deleting the order change status")
    ])
    Response deleteOrderChangeStatus(
            @ApiParam(name = "id", value = "Order Change Status id.", required = true)
            @PathParam("id") Integer id) {
        try {
            if (null == getOrderChangeStatusById(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteOrderChangeStatus(id);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private OrderChangeStatusWS getOrderChangeStatusById(Integer orderChangeStatusId){
        OrderChangeStatusWS[] orderChangeStatuses=webServicesSession.getOrderChangeStatusesForCompany()
        for(OrderChangeStatusWS currStatus : orderChangeStatuses){
            if (currStatus.getId().equals(orderChangeStatusId)){
                return currStatus;
            }
        }
        return null;
    }
}