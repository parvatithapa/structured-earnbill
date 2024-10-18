package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.discount.DiscountWS
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS
import com.sapienter.jbilling.server.user.partner.CommissionProcessRunWS
import com.sapienter.jbilling.server.user.partner.CommissionWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.*
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path('/api/commissions')
@Api(value = "/api/commissions", description = "Agents and Commission Methods.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class CommissionResource {

    IWebServicesSessionBean webServicesSession;

    @POST
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create or Update Commission Process Configuration.")
    @ApiResponses(value = [
    @ApiResponse(code = 201, message = "Commission created/updated."),
    @ApiResponse(code = 400, message = "Configuration object failed validation.", response = ErrorDetails.class),
    @ApiResponse(code = 500, message = "Failure while creating/updating the configuration.")
    ])
    Response createUpdateCommissionProcessConfiguration(
            @ApiParam(value = "Configuration Object.", required = true) CommissionProcessConfigurationWS conf,
            @Context UriInfo uriInfo
    ) {
        try {
            webServicesSession.createUpdateCommissionProcessConfiguration(conf)
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }


    @GET
    @Path("/processes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Return all commission runs")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "All commission runs.", response = CommissionProcessRunWS.class)
    ])
    Response getAllCommissionRuns() {

        List<CommissionProcessRunWS> processes;
        try {
            processes = Arrays.asList(webServicesSession.getAllCommissionRuns());
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(processes).build()
    }

    @GET
    @Path("/processes/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get commissions for process.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Commissions found.", response = CommissionWS.class),
            @ApiResponse(code = 404, message = "Commission run with the provided id not found."),
            @ApiResponse(code = 500, message = "Failure while getting commissions.")
    ])
    Response getCommissionsByProcessRunId(
            @ApiParam(name = "id", value = "Commission run id.", required = true) @PathParam("id") Integer id,
            @Context UriInfo uriInfo
    ) {
        CommissionWS[] commissions
        try {
            commissions = webServicesSession.getCommissionsByProcessRunId(id)
            if (null == commissions) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().entity(Arrays.asList(commissions)).build();
    }

    @POST
    @Path("/processes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Trigger partner commission process")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Commission process triggered.")
    ])
    Response calculatePartnerCommissions(@QueryParam("async") @ApiParam(value = "Trigger commission process async.", required = false) Boolean async,
                                         @Context UriInfo uriInfo) {
        try {
            if(async == null || !async.booleanValue()) {
                webServicesSession.calculatePartnerCommissions();
            } else {
                webServicesSession.calculatePartnerCommissionsAsync();
            }
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.ok().build()
    }

    @GET
    @Path("/processes/run")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Is commission process running")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "True or false.", response = String.class)
    ])
    Response isPartnerCommissionRunning(@Context UriInfo uriInfo) {
        try {
            Boolean result = webServicesSession.isPartnerCommissionRunning();
            return Response.ok().entity(result.toString()).build()
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }
}
