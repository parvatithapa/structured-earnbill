package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.order.OrderChangeTypeWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponses
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiParam
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.PathParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path('/api/orderchangetypes')
@Api(value = '/api/orderchangetypes', description = 'Order change types.')
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class OrderChangeTypeResource {

    IWebServicesSessionBean webServicesSession

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all order change types for the company.", response = OrderChangeTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order change types or empty.", response = OrderChangeTypeWS.class),
            @ApiResponse(code = 500, message = "Failure while getting the order change types")
    ])
    Response getOrderChangeTypesForCompany() {

        OrderChangeTypeWS[] orderChangeTypes;
        try {
            orderChangeTypes = webServicesSession.getOrderChangeTypesForCompany();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return (orderChangeTypes == null || orderChangeTypes.length == 0) ?
                Response.ok().build() :
                Response.ok().entity(orderChangeTypes).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get order change type by ID.", response = OrderChangeTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order change type found.", response = OrderChangeTypeWS.class),
            @ApiResponse(code = 404, message = "Order change type not found."),
            @ApiResponse(code = 500, message = "Failure while getting the order change type")
    ])
    Response getOrderChangeTypeById(
            @ApiParam(name = "id",
                    value = "The id of the order change type that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer orderChangeTypeId) {

        OrderChangeTypeWS orderChangeType;
        try {
            orderChangeType = webServicesSession.getOrderChangeTypeById(orderChangeTypeId);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return (orderChangeType != null) ?
                Response.ok().entity(orderChangeType).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new order change type.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Order change type created.", response = OrderChangeTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid order change type supplied."),
            @ApiResponse(code = 500, message = "Failure while creating the order change type")
    ])
    Response createOrderChangeType(
            @ApiParam(value = "Order change type that needs to be created") OrderChangeTypeWS orderChangeTypeWS,
            @Context UriInfo uriInfo) {
        try {
            Integer orderChangeTypeId = webServicesSession.createUpdateOrderChangeType(orderChangeTypeWS);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(orderChangeTypeId)).build())
                    .entity(webServicesSession.getOrderChangeTypeById(orderChangeTypeId)).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an order change type.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order change type updated.", response = OrderChangeTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid order change type supplied."),
            @ApiResponse(code = 404, message = "Order change type with the provided id does not exist."),
            @ApiResponse(code = 500, message = "Failure while updating the order change type")
    ])
    Response updateOrderChangeType(
            @ApiParam(name = "id", value = "Order change type id.", required = true) @PathParam("id") Integer id,
            @ApiParam(value = "Order change type that needs to be updated") OrderChangeTypeWS orderChangeTypeWS,
            @Context UriInfo uriInfo) {

        try {
            if (null == webServicesSession.getOrderChangeTypeById(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            orderChangeTypeWS.setId(id);
            webServicesSession.createUpdateOrderChangeType(orderChangeTypeWS);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(webServicesSession.getOrderChangeTypeById(id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes existing order change type.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Order change type with the supplied id is deleted."),
            @ApiResponse(code = 404, message = "Order change type with the supplied id does not exists."),
            @ApiResponse(code = 405, message = "Deletion of the specified order change type is not allowed.",
                    response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "Failure while deleting the order change type")
    ])
    Response deleteOrderChangeType(
            @ApiParam(name = "id",
                    value = "The id of the order change type that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer orderChangeTypeId) {

        try {
            if (webServicesSession.getOrderChangeTypeById(orderChangeTypeId) == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteOrderChangeType(orderChangeTypeId);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }


}
