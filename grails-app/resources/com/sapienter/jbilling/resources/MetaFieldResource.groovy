package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.MetaFieldWS
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
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * @author Bojan Dikovski
 * @since 03-OCT-2016
 */
@Path("/api/metafields")
@Api(value="/api/metafields", description = "Meta fields.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class MetaFieldResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a meta field by the id.", response = MetaFieldWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Meta field with the provided id found.", response = MetaFieldWS.class),
            @ApiResponse(code = 404, message = "Meta field with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the meta field failed.")
    ])
    Response getMetaField(
            @ApiParam(name = "id", value = "Meta field id.", required = true) @PathParam("id") Integer id) {

        MetaFieldWS metaField;
        try {
            metaField = webServicesSession.getMetaField(id);
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return Response.ok().entity(metaField).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all meta fields by an entity type.", response = MetaFieldWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning found meta fields for the provided entity type.",
                    response = MetaFieldWS.class),
            @ApiResponse(code = 500, message = "Fetching the meta fields failed.")
    ])
    Response getAllMetaFieldsByEntityType(
            @ApiParam(value = "Entity type", required = true) @QueryParam("entityType") String entityType) {

        try {
            MetaFieldWS[] metaFields = webServicesSession.getMetaFieldsForEntity(entityType);
            return (null == metaFields || 0 == metaFields.length) ?
                    Response.ok().build() :
                    Response.ok().entity(metaFields).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a meta field.", response = MetaFieldWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Meta field created.", response = MetaFieldWS.class),
            @ApiResponse(code = 500, message = "Meta field creation failed.")
    ])
    Response createMetaField(
            @ApiParam(value = "Meta field object that will be persisted.", required = true) MetaFieldWS metaFieldWS,
            @Context UriInfo uriInfo
    ) {

        try {
            Integer metaFieldId = webServicesSession.createMetaField(metaFieldWS)
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(metaFieldId)).build())
                    .entity(webServicesSession.getMetaField(metaFieldId)).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a meta field.", response = MetaFieldWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Meta field updated.", response = MetaFieldWS.class),
            @ApiResponse(code = 404, message = "Meta field with the provided id not found."),
            @ApiResponse(code = 500, message = "Meta field update failed.")
    ])
    Response updateMetaField(
            @ApiParam(name = "id", value = "Meta field id.", required = true) @PathParam("id") Integer id,
            @ApiParam(value = "Meta field object hat will be updated.", required = true) MetaFieldWS metaFieldWS
    ) {

        try {
            if (null == webServicesSession.getMetaField(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            metaFieldWS.setId(id);
            webServicesSession.updateMetaField(metaFieldWS);
            return Response.ok().entity(webServicesSession.getMetaField(id)).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a meta field.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Meta field with the provided id was deleted."),
            @ApiResponse(code = 404, message = "Meta field with the provided id does not exists."),
            @ApiResponse(code = 500, message = "Meta field deletion failed.")
    ])
    Response deleteMetaFields(
            @ApiParam(name = "id", value = "Meta field id.", required = true) @PathParam("id") Integer id) {

        try {
            if (null == webServicesSession.getMetaField(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteMetaField(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }
}