package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path("/api/usageratingscheme")
@Api(value="/api/usageratingscheme", description = "Usage Rating Scheme.")
@Secured(["isAuthenticated()", "MENU_99"])
class UsageRatingSchemeResource {

    WebServicesSessionSpringBean webServicesSession

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create usage rating scheme.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "scheme created."),
            @ApiResponse(code = 500, message = "Internal server error")
    ])
    @Path("/save")
    Response save( UsageRatingSchemeWS ws,
                   @Context UriInfo uriInfo) {

        try {
            webServicesSession.createUsageRatingScheme(ws)
            return Response.status(Response.Status.CREATED).build()

        } catch(SessionInternalError e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get usage rating scheme by id.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "rating scheme found.", response = UsageRatingSchemeWS.class),
            @ApiResponse(code = 404, message = "rating scheme not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    ])
    @Path("/{usageRatingSchemeId}")
    Response getUsageRatingSchemeById(@PathParam("usageRatingSchemeId") Integer usageRatingSchemeId) {

       try{
           UsageRatingSchemeWS usageRatingScheme = webServicesSession.getUsageRatingScheme(usageRatingSchemeId)
            return null != usageRatingScheme ? Response.ok().entity(usageRatingScheme).build() :
                    Response.status(Response.Status.NOT_FOUND).build()
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "delete usage rating scheme")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "rating scheme deleted successfully."),
            @ApiResponse(code = 500, message = "Internal server error while deletion")
    ])
    @Path("/{usageRatingSchemeId}")
    Response deleteUsageRatingScheme(@PathParam("usageRatingSchemeId") Integer usageRatingSchemeId) {
        try {
            webServicesSession.deleteUsageRatingScheme(usageRatingSchemeId)
        } catch (SessionInternalError e){
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
        return Response.status(Response.Status.OK).build()
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "fetch usage rating scheme")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "usage rating scheme fetched successfully." , response = List.class),
            @ApiResponse(code = 500, message = "Internal server error while getting list")
    ])
    @Path("/list")
    Response getAllUsageRatingScheme() {
        try {
            return Response.ok().entity(webServicesSession.findAllUsageRatingSchemes()).build()
        } catch (SessionInternalError e){
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "fetch usage rating scheme types")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "usage rating scheme types fetched successfully." ,
                    response = List.class),
            @ApiResponse(code = 500, message = "Internal server error while getting data")
    ])
    @Path("/types")
    Response getAllUsageRatingSchemeTypes() {
        try {
            return Response.ok().entity(webServicesSession.findAllRatingSchemeTypeValues()).build()
        } catch (SessionInternalError e){
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }
}


