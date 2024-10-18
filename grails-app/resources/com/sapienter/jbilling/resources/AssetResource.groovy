package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.item.AssetResourceHelperService;
import com.sapienter.jbilling.server.item.AssetRestWS;
import com.sapienter.jbilling.server.item.AssetWS
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path("/api/assets")
@Api(value="/api/assets", description = "Assets.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class AssetResource {

    IWebServicesSessionBean webServicesSession
    AssetResourceHelperService assetResourceHelperService

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get asset by id.", response = AssetWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Asset found.", response = AssetWS.class),
            @ApiResponse(code = 404, message = "Asset not found."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response getAssetById(
            @ApiParam(name = "id",
                    value = "The id of the asset that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer id) {

        try{
            return Response.ok().entity(webServicesSession.getAsset(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/user/{userId}")
    @ApiOperation(value = "Get all assets for user id.", response = AssetRestWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Assets found.", response = AssetRestWS.class),
        @ApiResponse(code = 404, message = "Assets not found."),
        @ApiResponse(code = 500, message = "Internal server error occurred.")
    ])
    Response getAllAssetsForUser(
            @ApiParam(name = "userId",value = "User Id", required = true)
            @PathParam("userId") Integer userId) {

        try{
            return Response.ok().entity(webServicesSession.getAllAssetsForUser(userId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create asset.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Asset created.", response = AssetWS.class),
            @ApiResponse(code = 400, message = "Invalid asset supplied."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
   Response createAsset(
            @ApiParam(value = "Created asset object.", required = true)
                    AssetWS assetWS,
            @Context
                    UriInfo uriInfo){
        try {
            Integer assetId = webServicesSession.createAsset(assetWS);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(assetId)).build())
                    .entity(webServicesSession.getAsset(assetId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing asset.", response = AssetWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Asset created.", response = AssetWS.class),
            @ApiResponse(code = 400, message = "Invalid asset supplied."),
            @ApiResponse(code = 404, message = "Asset with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response updateAsset(
            @ApiParam(name = "id", value = "The id of the asset that needs to be updated.", required = true)
            @PathParam("id")
                    Integer id,
            @ApiParam(value = "Asset object containing update data.", required = true)
                    AssetWS assetWS){
        try {
            webServicesSession.getAsset(id);
            assetWS.setId(id);
            webServicesSession.updateAsset(assetWS);
            return Response.ok().entity(webServicesSession.getAsset(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing asset.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Asset with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Asset with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response deleteItem(@ApiParam(name = "id",
            value = "The id of the asset that needs to be deleted.", required = true)
                               @PathParam("id") Integer id){

        try {
            webServicesSession.deleteAsset(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/updatemetafields")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing asset's meta fields.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Asset metafields updated.", response = AssetWS.class),
        @ApiResponse(code = 400, message = "Invalid parameter supplied."),
        @ApiResponse(code = 404, message = "Asset with the supplied id does not exists."),
        @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response updateAssetMetaFields(@ApiParam(value = "Asset MetaFields that needs to update.",
            required = true)
            AssetMetaFieldValueWS assetMetaFieldValueWS) {
        try {
            assetResourceHelperService.updateAssetMetaFields(assetMetaFieldValueWS)
            return Response.ok().entity(webServicesSession.getAsset(assetMetaFieldValueWS.getAssetId())).build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
            
    @GET
    @Path("/{itemId}/{assetStatus}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve assets by item and status.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Assets found for item.", response = AssetRestWS[].class),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 404, message = "item not found."),
        @ApiResponse(code = 500, message = "Failure while retrieving assets.")
    ])
    Response getAssetsByItemAndStatus( @PathParam("itemId") @ApiParam(name="itemId", required = true) Integer itemId,
            @PathParam("assetStatus") @ApiParam(name="assetStatus", required = true) String assetStatus,
            @DefaultValue("10000")@QueryParam("limit") @ApiParam(name="limit") Integer limit,
            @DefaultValue("0")@QueryParam("offset") @ApiParam(name="offset") Integer offset) {
        try {
            return Response.ok().entity(assetResourceHelperService.getAssetsByItemAndStatus(itemId, assetStatus, limit, offset)).build();
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }

    @POST
    @Path("/release/{assetId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Release asset by Asset Id")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Asset released.", response = AssetRestWS[].class),
    @ApiResponse(code = 400, message = "Asset release failed."),
    @ApiResponse(code = 404, message = "Asset not found."),
    @ApiResponse(code = 500, message = "Internal problem occurred.")
    ])
    Response releaseAsset(@PathParam("assetId") @ApiParam(name="assetId", value = "The id of the asset that needs to be released.", required = true) String assetId) {
        try {
             return Response.ok().entity(webServicesSession.removeAssetFromActiveOrder(assetId)).build();
        } catch (Exception exception) {
             return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }

    @GET
    @Path("/identifier/{identifier}")
    @ApiOperation(value = "Get asset by identifier/mobile number.", response = AssetWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Asset found.", response = AssetWS.class),
            @ApiResponse(code = 404, message = "Asset not found."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response getAssetByIdentifier(
            @ApiParam(name = "identifier",
                    value = "The identifier of the asset that needs to be fetched.",
                    required = true)
            @PathParam("identifier") String identifier) {

        try{
            return Response.ok().entity(webServicesSession.getAssetByIdentifier(identifier)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

}
