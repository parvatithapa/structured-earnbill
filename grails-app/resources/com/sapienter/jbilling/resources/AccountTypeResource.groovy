package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo;

/**
 * @author Vojislav Stanojevikj
 * @since 14-Jul-2016.
 */
@Path("/api/accounttypes")
@Api(value="/api/accounttypes", description = "Account types.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
public class AccountTypeResource {

    IWebServicesSessionBean webServicesSession;


    @GET
    @ApiOperation(value = "Get all account types.", response = AccountTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Account types found or empty.", response = AccountTypeWS.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAllAccountTypes(){
        try {
            return Response.ok().entity(webServicesSession.getAllAccountTypes()).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get account type by id.", response = AccountTypeWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Account type found.", response = AccountTypeWS.class),
        @ApiResponse(code = 404, message = "Account type not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAccountTypeById(
            @ApiParam(name = "id",
                      value = "The id of the account type that needs to be fetched.",
                      required = true)
            @PathParam("id") Integer id) {

        try {
            AccountTypeWS accountType = webServicesSession.getAccountType(id);
            if (null == accountType){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(accountType).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create account type.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Account type created.", response = AccountTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid account type supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response createAccountType(
            @ApiParam(value = "Created account type object.", required = true)
                    AccountTypeWS accountType,
            @Context
                    UriInfo uriInfo){
        try {
            Integer accountTypeId = webServicesSession.createAccountType(accountType);
            return Response
                    .created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(accountTypeId)).build())
                    .entity(webServicesSession.getAccountType(accountTypeId))
                    .build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing account type.", response = AccountTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Account type created."),
            @ApiResponse(code = 400, message = "Invalid account type supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 404, message = "Account type not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateAccountType(
            @ApiParam(name = "id", value = "Account type id that needs to be updated.", required = true)
            @PathParam("id")
                    Integer id,
            @ApiParam(value = "Account type object containing update data.", required = true)
                    AccountTypeWS accountType) {

        try {
            if (null == webServicesSession.getAccountType(id)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            accountType.setId(id);
            webServicesSession.updateAccountType(accountType);
            return Response.ok().entity(webServicesSession.getAccountType(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing account type.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Account type with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Account type with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Deletion of the specified account type resulted with error.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response deleteAccountType(@ApiParam(name = "id",
            value = "The id of the account type that needs to be deleted.",
            required = true)
    @PathParam("id") Integer id){

        try {
            if (null == webServicesSession.getAccountType(id)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteAccountType(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

}
