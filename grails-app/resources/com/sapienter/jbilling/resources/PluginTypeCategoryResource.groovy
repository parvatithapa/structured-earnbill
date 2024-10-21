package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryWS
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
@Path("/api/plugintypecategories")
@Api(value="/api/plugintypecategories", description = "Plugin type categories.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class PluginTypeCategoryResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a plugin type category by id.", response = PluggableTaskTypeCategoryWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plugin type category with the provided id found.",
                    response = PluggableTaskTypeCategoryWS.class),
            @ApiResponse(code = 404, message = "Plugin type category with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetiching the plugin type category failed.")
    ])
    Response getPluginTypeCategory(
            @ApiParam(name = "id", value = "Plugin type category id.", required = true) @PathParam("id") Integer id) {

        PluggableTaskTypeCategoryWS pluginTypeCategory;
        try {
            pluginTypeCategory = webServicesSession.getPluginTypeCategory(id);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(pluginTypeCategory).build();
    }

    @GET
    @Path("/classname/{interfaceName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a plugin type category by interface name.", response = PluggableTaskTypeCategoryWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plugin type category with the provided interface name found.",
                    response = PluggableTaskTypeCategoryWS.class),
            @ApiResponse(code = 404, message = "Plugin type category with the provided interface name not found."),
            @ApiResponse(code = 500, message = "Fetching the plugin type category failed.")
    ])
    Response getPluginTypeCategoryByInterfaceName(
            @ApiParam(name = "interfaceName", value = "Plugin type category interface name.", required = true)
            @PathParam("interfaceName") String interfaceName) {

        PluggableTaskTypeCategoryWS pluginTypeCategory;
        try {
            pluginTypeCategory = webServicesSession.getPluginTypeCategoryByInterfaceName(interfaceName);
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(pluginTypeCategory).build();
    }
}