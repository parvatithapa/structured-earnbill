package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceWS
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Bojan Dikovski
 * @since 03-OCT-2016
 */
@Path("/api/preferences")
@Api(value="/api/preferences", description = "Preferences.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class PreferenceResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{typeId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a preference by the id.", response = PreferenceWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Preference with the provided type id found.", response = PreferenceWS.class),
            @ApiResponse(code = 404, message = "Preference with the provided type id not found."),
            @ApiResponse(code = 500, message = "Fetching the preference failed.")
    ])
    Response getPreference(
            @ApiParam(name = "typeId", value = "Preference type id.", required = true) @PathParam("typeId") Integer typeId) {

        PreferenceWS preferenceWS;
        try {
            preferenceWS = webServicesSession.getPreference(typeId);
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return Response.ok().entity(preferenceWS).build();
    }

    @PUT
    @Path("/{typeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a preference.", response = PreferenceWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Preference updated.", response = PreferenceWS.class),
            @ApiResponse(code = 404, message = "Preference with the provided type id not found."),
            @ApiResponse(code = 500, message = "Preference update failed.")
    ])
    Response updatePreference(
            @ApiParam(name = "typeId", value = "Preference type id.", required = true) @PathParam("typeId") Integer typeId,
            @ApiParam(value = "Preference object that will be updated.", required = true) PreferenceWS preferenceWS) {

        try {
            if (null == webServicesSession.getPreference(typeId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            preferenceWS.getPreferenceType().setId(typeId);
            webServicesSession.updatePreference(preferenceWS);
            return Response.ok().entity(webServicesSession.getPreference(typeId)).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }
}