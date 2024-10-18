package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.item.AssetWS
import com.sapienter.jbilling.server.item.ItemTypeWS
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
import javax.ws.rs.DefaultValue;
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
 * @author Vojislav Stanojevikj
 * @since 06-Sep-2016.
 */

@Path("/api/itemtypes")
@Api(value="/api/itemtypes", description = "Item categories.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class ItemTypeResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @ApiOperation(value = "Get all item categories.", response = ItemTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Item categories found or empty.", response = ItemTypeWS.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAllItemCategories(){
        try {
            return Response.ok().entity(webServicesSession.getAllItemCategories()).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get item category by id.", response = ItemTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Item category found.", response = ItemTypeWS.class),
            @ApiResponse(code = 404, message = "Item category not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getItemCategoryId(
            @ApiParam(name = "id",
                    value = "The id of the item category that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer id) {

        try{
            ItemTypeWS itemTypeWS = webServicesSession.getItemCategoryById(id);
            if (null == itemTypeWS){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(itemTypeWS).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create item category.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Item category created.", response = ItemTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid item category supplied."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response createItemCategory(
            @ApiParam(value = "Created item category object.", required = true)
                    ItemTypeWS itemType,
            @Context
                    UriInfo uriInfo){

        try {
            Integer itemTypeId = webServicesSession.createItemCategory(itemType);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(itemTypeId)).build())
                    .entity(webServicesSession.getItemCategoryById(itemTypeId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing item category.", response = ItemTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Item category created.", response = ItemTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid item category supplied."),
            @ApiResponse(code = 404, message = "Item category with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateItemCategory(
            @ApiParam(name = "id", value = "The id of Item category that needs to be updated.", required = true)
            @PathParam("id")
                    Integer id,
            @ApiParam(value = "Item category object containing update data.", required = true)
                    ItemTypeWS itemType){
        try {
            ItemTypeWS itemTypeWS = webServicesSession.getItemCategoryById(id);
            if (null == itemTypeWS){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            itemType.setId(id);
            webServicesSession.updateItemCategory(itemType);
            return Response.ok().entity(webServicesSession.getItemCategoryById(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing item category.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Item category with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Item category with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Deletion of the specified item category resulted with conflict."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response deleteItemCategory(@ApiParam(name = "id",
            value = "The id of the item category that needs to be deleted.", required = true)
                                      @PathParam("id") Integer id){

        try {
            ItemTypeWS itemTypeWS = webServicesSession.getItemCategoryById(id);
            if (null == itemTypeWS){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteItemCategory(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{id}/assets")
    @ApiOperation(value = "Get all assets by item type id.", response = AssetWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Assets found.", response = AssetWS.class),
            @ApiResponse(code = 404, message = "Item category not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAssetsByItemTypeId(
            @ApiParam(name = "id", value = "The id of the item type assets that needs to be fetched.", required = true)
            @PathParam("id") Integer id,
            @ApiParam(name = "offset", value = "Read all values starting from this value.")
            @DefaultValue("0")@QueryParam("offset") Integer offset,
            @ApiParam(name = "max", value = "Limit the number of fetched values.")
            @DefaultValue("100")@QueryParam("max") Integer max){

        try {
            if (null == webServicesSession.getItemCategoryById(id)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(webServicesSession.getAssetsForCategoryId(id, offset, max)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}
