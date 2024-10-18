package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.order.OrderStatusWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponses
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiParam
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.PathParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path('/api/orderstatuses')
@Api(value = "/api/orderstatuses", description = "Order statuses.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class OrderStatusResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Find order status by id.", response = OrderStatusWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order status found or null.", response = OrderStatusWS.class),
            @ApiResponse(code = 404, message = "Order status not found."),
            @ApiResponse(code = 500, message = "Failure while getting the order status.")
    ])
    Response findOrderStatusById(
            @ApiParam(name = "id", value = "Order Status Id.", required = true)
            @PathParam("id") Integer orderStatusId) {

        OrderStatusWS orderStatusWS;
        try {
            orderStatusWS = webServicesSession.findOrderStatusById(orderStatusId);
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
        return orderStatusWS != null ?
                Response.ok().entity(orderStatusWS).build():
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Find all order status ids by.", response = Integer.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order status ids found or null.", response = OrderStatusWS.class),
        @ApiResponse(code = 404, message = "Order status ids not found."),
        @ApiResponse(code = 500, message = "Internal error occurred.")])
    Response findAllOrderStatusIds() {
        try {
            return Response.ok().entity(webServicesSession.findAllOrderStatusIds()).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create order status.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Order status created.", response = OrderStatusWS.class),
            @ApiResponse(code = 400, message = "Order status already exists with the same description."),
            @ApiResponse(code = 500, message = "Failure while creating the order status.")
    ])
    Response createOrderStatus(
            @ApiParam(value = "Order status object that needs to be created.", required = true) OrderStatusWS orderStatusWS,
            @Context UriInfo uriInfo
    ) {
        Integer orderStatusId;
        try {
            orderStatusId = webServicesSession.createUpdateOrderStatus(orderStatusWS);
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
        return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(orderStatusId)).build())
                .entity(webServicesSession.findOrderStatusById(orderStatusId)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update order status.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order status updated.", response = OrderStatusWS.class),
            @ApiResponse(code = 400, message = "Order status already exists with the same description."),
            @ApiResponse(code = 404, message = "Order status with the provided id not found."),
            @ApiResponse(code = 500, message = "Failure while updating the order status.")
    ])
    Response updateOrderStatus(
            @ApiParam(name = "id", value = "Order status id.", required = true) @PathParam("id") Integer id,
            @ApiParam(value = "Order status object that needs to be updated.", required = true) OrderStatusWS orderStatusWS,
            @Context UriInfo uriInfo
    ) {
        try {
            if (null == webServicesSession.findOrderStatusById(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            orderStatusWS.setId(id);
            webServicesSession.createUpdateOrderStatus(orderStatusWS);
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
        return Response.ok().entity(webServicesSession.findOrderStatusById(id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes existing order status.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Order status with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Order status with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Can not delete. Order status currently in use."),
            @ApiResponse(code = 500, message = "Failure while deleting the order status.")
    ])
    Response deleteOrderStatus(@ApiParam(name = "id", value = "Order Status id.", required = true)
                                      @PathParam("id") Integer id) {
        try {
            OrderStatusWS orderStatusWS = webServicesSession.findOrderStatusById(id)
            if (null == orderStatusWS) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteOrderStatus(orderStatusWS);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }
}
