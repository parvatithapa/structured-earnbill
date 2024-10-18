package com.sapienter.jbilling.resources

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.search.BasicFilter
import com.sapienter.jbilling.server.util.search.SearchCriteria
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.item.AssetWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.item.ItemDTOEx
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path("/api/items")
@Api(value="/api/items", description = "Items.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class ItemResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @ApiOperation(value = "Get all items.", response = ItemDTOEx.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Item found or empty.", response = ItemDTOEx.class),
            @ApiResponse(code = 500, message = "Internal error occurred.")])
    Response getAllItems(){
        try {
            return Response.ok().entity(webServicesSession.getAllItems()).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get item by id.", response = ItemDTOEx.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Item found.", response = ItemDTOEx.class),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response getItemById(
            @ApiParam(name = "id",
                    value = "The id of the item that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer id) {

        try{
            return Response.ok().entity(webServicesSession.getItem(id, null, null)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create item.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Item created.", response = ItemDTOEx.class),
            @ApiResponse(code = 400, message = "Invalid item supplied."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response createItem(
            @ApiParam(value = "Created item object.", required = true)
                    ItemDTOEx itemDTOEx,
            @Context
                    UriInfo uriInfo){
        try {
            Integer itemId = webServicesSession.createItem(itemDTOEx);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(itemId)).build())
                    .entity(webServicesSession.getItem(itemId, null, null)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing item.", response = ItemDTOEx.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Item created.", response = ItemDTOEx.class),
            @ApiResponse(code = 400, message = "Invalid item supplied."),
            @ApiResponse(code = 404, message = "Item with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response updateItem(
            @ApiParam(name = "id", value = "The id of the item that needs to be updated.", required = true)
            @PathParam("id")
                    Integer id,
            @ApiParam(value = "Item object containing update data.", required = true)
                    ItemDTOEx itemDTOEx){
        try {
            webServicesSession.getItem(id, null, null);
            itemDTOEx.setId(id);
            webServicesSession.updateItem(itemDTOEx);
            return Response.ok().entity(webServicesSession.getItem(id, null, null)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing item.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Item with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Item with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Deletion resulted with conflict.")])
    Response deleteItem(@ApiParam(name = "id",
            value = "The id of the item that needs to be deleted.", required = true)
                                       @PathParam("id") Integer id){

        try {
            webServicesSession.getItem(id, null, null);
            webServicesSession.deleteItem(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{id}/assets")
    @ApiOperation(value = "Get all assets by item id.", response = AssetWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Assets found.", response = AssetWS.class),
            @ApiResponse(code = 404, message = "Item do not exist."),
            @ApiResponse(code = 400, message = "Invalid query parameters."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAssetsByItemId(
            @ApiParam(name = "id", value = "The id of the item assets that needs to be fetched.", required = true)
            @PathParam("id") Integer id,
            @ApiParam(name = "offset", value = "Read all values starting from this value.")
            @QueryParam("offset") Integer offset,
            @ApiParam(name = "max", value = "Limit the number of fetched values.")
            @QueryParam("max") Integer max){

        try{
            webServicesSession.getItem(id, null, null);
            AssetWS[] assets = webServicesSession.getAssetsForItemId(id, offset, max);
            return Response.ok().entity(assets).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/assets/{itemId}/{status}")
    @ApiOperation(value = "Get all assets by itemId and asset status.", response = AssetSearchResult.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Assets found.", response = AssetSearchResult.class),
        @ApiResponse(code = 404, message = "Item do not exist."),
        @ApiResponse(code = 400, message = "Invalid query parameters."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    Response getAssetsByItemIdAndStatus(
            @ApiParam(name = "itemId", value = "The id of the item whose assets need to be fetched.", required = true)
            @PathParam("itemId") Integer itemId,
            @ApiParam(name = "status", value = "The status of the asset.", required = true)
            @PathParam("status") String status,
            @ApiParam(name = "offset", value = "Read all values starting from this value.")
            @DefaultValue("0")
            @QueryParam("offset") Integer offset,
            @ApiParam(name = "max", value = "Limit the number of fetched values.")
            @DefaultValue("0")
            @QueryParam("max") Integer max) {
        try {
            webServicesSession.getItem(itemId, null, null);
            return Response.ok(webServicesSession.findAssets(itemId, getCriteria(status, offset, max))).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    private SearchCriteria getCriteria(String status, Integer offset, Integer max) {
        SearchCriteria criteria = new SearchCriteria();
        BasicFilter[] filters = [
            new BasicFilter("status", FilterConstraint.EQ, status)
        ];
        criteria.setFilters(filters);
        criteria.setOffset(offset);
        criteria.setMax(max);
        criteria.setSort("id");
        criteria.setDirection(SearchCriteria.SortDirection.ASC);
        return criteria;
    }
}
