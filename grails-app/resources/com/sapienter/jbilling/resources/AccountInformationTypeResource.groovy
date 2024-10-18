package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.user.AccountInformationTypeWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured
import org.hibernate.SessionFactory

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

@Path("/api/accounttypes/{accountTypeId}/aits")
@Api(value="/api/accounttypes/{accountTypeId}/aits", description = "Account information types")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class AccountInformationTypeResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @ApiOperation(value = "Returns all AITs for a specific account type", response = AccountInformationTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "AITs found or empty.", response = AccountInformationTypeWS.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getInformationTypesForAccountType(
            @ApiParam(name = "accountTypeId",
                    value = "The AITs for a account type defined with this id",
                    required = true)
            @PathParam("accountTypeId")
                    Integer accountTypeId){

        try {
            return Response.ok().entity(webServicesSession.getInformationTypesForAccountType(accountTypeId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{accountInformationTypeId}")
    @ApiOperation(value = "Returns a specific AIT for a specific account type", response = AccountInformationTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "AITs found", response = AccountInformationTypeWS.class),
            @ApiResponse(code = 404, message = "AITs not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getInformationTypeForAccountType(
            @ApiParam(name = "accountTypeId", value = "The AITs for a account type defined with this id", required = true)
            @PathParam("accountTypeId")
                    Integer accountTypeId,
            @ApiParam(name = "accountInformationTypeId", value = "The id of the specific AIT that is requested", required = true)
            @PathParam("accountInformationTypeId")
                    Integer aitId){

        try {
            AccountInformationTypeWS ait = webServicesSession.getAccountInformationType(aitId);
            if (null == ait || !validAccountType(ait, accountTypeId)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(ait).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create AIT for specific account type.", response = AccountInformationTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "AIT type created.", response = AccountInformationTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid AIT or supplied."),
            @ApiResponse(code = 404, message = "Account type with the supplied id not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response createAIT(
            @ApiParam(name = "accountTypeId", value = "The id of the account type for which this AIT will be created for.", required = true)
            @PathParam("accountTypeId")
                    Integer accountTypeId,
            @ApiParam(value = "AIT object that needs to be created.", required = true)
                    AccountInformationTypeWS accountInformationType,
            @Context
                    UriInfo uriInfo){

        try {
            if (null != accountInformationType.getAccountTypeId() && !validAccountType(accountInformationType, accountTypeId)){
                return Response.status(Response.Status.NOT_FOUND).entity('Account type param id invalid!').build();
            }
            Integer aitId = webServicesSession.createAccountInformationType(accountInformationType);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(aitId)).build())
                    .entity(webServicesSession.getAccountInformationType(aitId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{accountInformationTypeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing AIT.", response = AccountInformationTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "AIT created.", response = AccountInformationTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid AIT supplied."),
            @ApiResponse(code = 404, message = "AIT or account type not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateAIT(
            @ApiParam(name = "accountTypeId", value = "Account type id for which a AIT needs to be updated.", required = true)
            @PathParam("accountTypeId")
                    Integer id,
            @ApiParam(name = "accountInformationTypeId", value = "Id of the AIT needs to be updated.", required = true)
            @PathParam("accountInformationTypeId")
                    Integer aitId,
            @ApiParam(value = "AIT object containing update data.", required = true)
                    AccountInformationTypeWS accountInformationType){

        try {
            AccountInformationTypeWS ait = webServicesSession.getAccountInformationType(aitId)
            if (null == ait || !validAccountType(ait, id)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            accountInformationType.setId(aitId)
            webServicesSession.updateAccountInformationType(accountInformationType);
            return Response.ok().entity(webServicesSession.getAccountInformationType(aitId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{accountInformationTypeId}")
    @ApiOperation(value = "Deletes existing AIT.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Account type with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Account type with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Deletion of the specified account type failed."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response deleteAIT(
            @ApiParam(name = "accountTypeId", value = "The id of the account type for which an AIT will be deleted.", required = true)
            @PathParam("accountTypeId") Integer id,
            @ApiParam(name = "accountInformationTypeId", value = "The id of the AIT that will be deleted.", required = true)
            @PathParam("accountInformationTypeId") Integer aitId){

        try {
            AccountInformationTypeWS ait = webServicesSession.getAccountInformationType(aitId)
            if (null == ait || !validAccountType(ait, id)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            cleanupHibernateSession()
            boolean deleted = webServicesSession.deleteAccountInformationType(aitId);
            if (!deleted){
                return Response.status(Response.Status.CONFLICT).build();
            }
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    private void cleanupHibernateSession () {
        SessionFactory hibernateSessionFactory = com.sapienter.jbilling.server.util.Context.getBean(com.sapienter.jbilling.server.util.Context.Name.HIBERNATE_SESSION);
        hibernateSessionFactory.getCurrentSession().flush();
        hibernateSessionFactory.getCurrentSession().clear();
    }

    private boolean validAccountType(AccountInformationTypeWS accountInformationType, Integer accountTypeId){
        return null != webServicesSession.getAccountType(accountTypeId) &&
                null != accountInformationType.getAccountTypeId() &&
                accountInformationType.getAccountTypeId().equals(accountTypeId);
    }
}
