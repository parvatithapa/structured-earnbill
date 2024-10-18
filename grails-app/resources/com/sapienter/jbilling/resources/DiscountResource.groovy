package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.discount.DiscountWS
import com.sapienter.jbilling.server.order.OrderStatusWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.*

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path('/api/discounts')
@Api(value = "/api/discounts", description = "Discount Methods.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class DiscountResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Find discount by id.", response = DiscountWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Discount found or null.", response = DiscountWS.class),
            @ApiResponse(code = 404, message = "Discount not found."),
            @ApiResponse(code = 500, message = "Failure while getting the discount.")
    ])
    Response findDiscountById(
            @ApiParam(name = "id", value = "Discount Id.", required = true)
            @PathParam("id") Integer discountId) {

        DiscountWS discountWS;
        try {
            discountWS = webServicesSession.getDiscountWS(discountId);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return discountWS != null ?
                Response.ok().entity(discountWS).build():
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create Discount.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Discount created.", response = DiscountWS.class),
            @ApiResponse(code = 400, message = "Discount already exists with the same description."),
            @ApiResponse(code = 500, message = "Failure while creating the discount.")
    ])
    Response createDiscount(
            @ApiParam(value = "Discount object that needs to be created.", required = true) DiscountWS discountWS,
            @Context UriInfo uriInfo
    ) {
        Integer discountId;
        try {
            discountId = webServicesSession.createOrUpdateDiscount(discountWS);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(discountId)).build())
                .entity(webServicesSession.getDiscountWS(discountId)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update discount.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Discount updated.", response = DiscountWS.class),
            @ApiResponse(code = 400, message = "Discount failed validation e.g.  duplicate code/description/invalid rate."),
            @ApiResponse(code = 404, message = "Discount with the provided id not found."),
            @ApiResponse(code = 500, message = "Failure while updating the Discount.")
    ])
    Response updateDiscount(
            @ApiParam(name = "id", value = "Discount id.", required = true) @PathParam("id") Integer id,
            @ApiParam(value = "Discount object that needs to be updated.", required = true) DiscountWS discountWS,
            @Context UriInfo uriInfo
    ) {
        try {
            if (null == webServicesSession.getDiscountWS(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            discountWS.setId(id);
            webServicesSession.createOrUpdateDiscount(discountWS);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(webServicesSession.getDiscountWS(id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes existing discount.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Discount with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Discount with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Can not delete. Discount currently in use."),
            @ApiResponse(code = 500, message = "Failure while deleting the discount.")
    ])
    Response deleteDiscount(@ApiParam(name = "id", value = "Discount id.", required = true)
                                      @PathParam("id") Integer id) {
        try {
            DiscountWS discountWS = webServicesSession.getDiscountWS(id)
            if (null == discountWS) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteDiscount(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }
}
