package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS
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
 * @author Bojan Dikovski
 * @since 03-OCT-2016
 */
@Path("/api/plugins")
@Api(value="/api/plugins", description = "Plugins.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class PluginResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a plugin by the id.", response = PluggableTaskWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plugin with the provided id found.", response = PluggableTaskWS.class),
            @ApiResponse(code = 404, message = "Plugin with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the plugin failed.")
    ])
    Response getPlugin(@ApiParam(value = "Plugin id.", required = true) @PathParam("id") Integer id) {

        PluggableTaskWS pluginWS;
        try {
            pluginWS = webServicesSession.getPluginWS(id);
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return Response.ok().entity(pluginWS).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a plugin.", response = PluggableTaskWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Plugin created.", response = PluggableTaskWS.class),
            @ApiResponse(code = 500, message = "Plugin creation failed.")
    ])
    Response createPlugin(
            @ApiParam(value = "Plugin object that will be persisted.", required = true) PluggableTaskWS pluginWS,
            @Context UriInfo uriInfo) {

        try {
            Integer pluginId = webServicesSession.createPlugin(pluginWS);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(pluginId)).build())
                    .entity(webServicesSession.getPluginWS(pluginId)).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a plugin.", response = PluggableTaskWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plugin updated.", response = PluggableTaskWS.class),
            @ApiResponse(code = 404, message = "Plugin with the provided id not found."),
            @ApiResponse(code = 500, message = "Plugin update failed.")
    ])
    Response updatePlugin(@ApiParam(value = "Plugin id.", required = true) @PathParam("id") Integer id,
                                 @ApiParam(value = "Plugin object that will be persisted", required = true)
                                    PluggableTaskWS pluginWS) {

        try {
            if (null == webServicesSession.getPluginWS(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            pluginWS.setId(id);
            webServicesSession.updatePlugin(pluginWS);
            return Response.ok().entity(webServicesSession.getPluginWS(id)).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a plugin.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Plugin deleted."),
            @ApiResponse(code = 404, message = "Plugin with the provided id not found."),
            @ApiResponse(code = 500, message = "Plugin deletion failed.")
    ])
    Response deletePlugin(@ApiParam(value = "Plugin id.", required = true) @PathParam("id") Integer id) {

        try {
            if (null == webServicesSession.getPluginWS(id)) {
                return Response.status(Response.Status.NOT_FOUND).build()
            }
            webServicesSession.deletePlugin(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }
}