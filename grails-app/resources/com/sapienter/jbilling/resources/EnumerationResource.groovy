package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.EnumerationWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.*
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * @author Martin Kostovski
 * @author Bojan Dikovski
 * @since 03-OCT-2016
 */
@Path('/api/enumerations')
@Api(value = "/api/enumerations", description = "Enumerations.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class EnumerationResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Find all enumerations.", response = EnumerationWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Enumerations found.", response = EnumerationWS.class),
            @ApiResponse(code = 400, message = "Trying to fetch enumerations with invalid parameters."),
            @ApiResponse(code = 500, message = "Fetching the enumerations failed.")
    ])
    Response findAllEnumerations(@ApiParam(value =  "Limit", required = true) @QueryParam("limit")  Integer limit,
                                        @ApiParam(value = "Offset", required = true) @QueryParam("offset") Integer offset) {
        try {
            List<EnumerationWS> enumerations = webServicesSession.getAllEnumerations(limit, offset);
            return Response.ok().entity(enumerations).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Find enumeration by id.", response = EnumerationWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Enumeration found or null.", response = EnumerationWS.class),
            @ApiResponse(code = 404, message = "Enumeration not found."),
            @ApiResponse(code = 500, message = "Fetching the enumeration failed.")
    ])
    Response findEnumerationById(@ApiParam(name = "id", value = "Enumeration Id.", required = true)
                                        @PathParam("id") Integer enumerationId) {
        try {
            EnumerationWS enumerationWS = webServicesSession.getEnumeration(enumerationId);
            return enumerationWS != null ?
                    Response.ok().entity(enumerationWS).build() :
                    Response.status(Response.Status.NOT_FOUND).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create enumeration.", response = EnumerationWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Enumeration created.", response = EnumerationWS.class),
            @ApiResponse(code = 500, message = "Creation of the enumeration failed.")
    ])
    Response createEnumeration(@ApiParam(value = "Enumeration object.", required = true) EnumerationWS enumerationWS,
                               @Context UriInfo uriInfo) {
        try {
            Integer enumerationId = webServicesSession.createUpdateEnumeration(enumerationWS);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(enumerationId)).build())
                    .entity(webServicesSession.getEnumeration(enumerationId)).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update enumeration.", response = EnumerationWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Updated Enumeration.", response = EnumerationWS.class),
            @ApiResponse(code = 404, message = "Enumeration with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "Updating the enumeration failed.")
    ])
    Response updateEnumeration(@PathParam("id") Integer id,
                                      @ApiParam(value = "Enumeration object.", required = true) EnumerationWS enumerationWS) {
        try {
            if (null == webServicesSession.getEnumeration(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            enumerationWS.setId(id);
            webServicesSession.createUpdateEnumeration(enumerationWS);
            return Response.ok().entity(webServicesSession.getEnumeration(id)).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes existing enumeration.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Enumeration with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Enumeration with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "Deletion of the enumeration failed.")
    ])
    Response deleteEnumeration(@ApiParam(name = "id", value = "Enumeration id.", required = true)
                                      @PathParam("id") Integer id) {
        try {
            if (null == webServicesSession.getEnumeration(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteEnumeration(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }
}