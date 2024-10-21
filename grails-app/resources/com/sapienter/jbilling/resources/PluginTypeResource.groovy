package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Bojan Dikovski
 * @since 03-OCT-2016
 */
@Path("/api/plugintypes")
@Api(value="/api/plugintypes", description = "Plugin types.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class PluginTypeResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a plugin type by id.", response = PluggableTaskTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plugin type with the provided id found.",
                    response = PluggableTaskTypeWS.class),
            @ApiResponse(code = 404, message = "Plugin type with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the plugin type failed.")
    ])
    Response getPluginTypeCategory(
            @ApiParam(name = "id", value = "Plugin type id.", required = true) @PathParam("id") Integer id) {

        PluggableTaskTypeWS pluginType;
        try {
            pluginType = webServicesSession.getPluginTypeWS(id);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(pluginType).build();
    }

    @GET
    @Path("/classname/{className}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a plugin type by class name.", response = PluggableTaskTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plugin type with the provided class name found.",
                    response = PluggableTaskTypeWS.class),
            @ApiResponse(code = 404, message = "Plugin type with the provided class name not found."),
            @ApiResponse(code = 500, message = "Fetching the plugin type failed.")
    ])
    Response getPluginTypeCategoryByInterfaceName(
            @ApiParam(name = "className", value = "Plugin type class name.", required = true)
            @PathParam("className") String className) {

        PluggableTaskTypeWS pluginType;
        try {
            pluginType = webServicesSession.getPluginTypeWSByClassName(className);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(pluginType).build();
    }
}