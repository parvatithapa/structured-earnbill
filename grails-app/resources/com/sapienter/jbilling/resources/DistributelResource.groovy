package com.sapienter.jbilling.resources

import grails.plugin.springsecurity.annotation.Secured;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sapienter.jbilling.server.spa.SpaHappyFox;
import com.sapienter.jbilling.server.spa.SpaImportWS;
import com.sapienter.jbilling.server.util.DistributelWebServicesSessionSpringBean;
import com.sapienter.jbilling.server.util.api.JbillingDistributelAPI;
import com.sapienter.jbilling.utils.RestErrorHandler;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("/api/distributel")
@Api(value="/api/distributel", description = "distributel.")
@Secured('permitAll')
@Produces(MediaType.APPLICATION_JSON)
class DistributelResource{

    JbillingDistributelAPI distributelWebServicesSession;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add Staff Private Note")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Added Staff Private Note.", response = SpaHappyFox.class),
            @ApiResponse(code = 400, message = "Invalid details supplied."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getCustomerTicketInfoResult(
            @ApiParam(value = "Add Staff Private Note and add custom field values", required = true)
                    SpaHappyFox spaHappyFox){
        try {
            return Response.ok().entity(distributelWebServicesSession.getCustomerTicketInfoResult(spaHappyFox)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
					
	@POST
	@Path("/processSpaImport")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Customer creation using processSpaImport")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "Customer added successfully.", response = SpaImportWS.class),
		@ApiResponse(code = 400, message = "Invalid details provided"),
		@ApiResponse(code = 500, message = "The call resulted with internal error.")
	])
	Response processSpaImport(
			@ApiParam(value = "JSON represenation of SpaImportWS Request", required = true)SpaImportWS spaImportWS){
		try {
			return Response.ok().entity(distributelWebServicesSession.processSpaImport(spaImportWS)).build();
		} catch (Exception e){
			return RestErrorHandler.mapErrorToHttpResponse(e);
		}
	}
}