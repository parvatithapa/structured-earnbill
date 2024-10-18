package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS
import com.sapienter.jbilling.server.process.BillingProcessWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Bojan Dikovski
 * @since 11-OCT-2016
 */
@Path("/api/billing")
@Api(value="/api/billing", description = "Billing process.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class BillingResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the current billing process configuration.", response = BillingProcessConfigurationWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning the current billing process configuration.",
                    response = BillingProcessConfigurationWS.class),
            @ApiResponse(code = 500, message = "Fetching the billing process configuration failed.")
    ])
    Response getBillingProcessConfiguration() {

        try {
            BillingProcessConfigurationWS config = webServicesSession.getBillingProcessConfiguration();
            return Response.ok().entity(config).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @PUT
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update the billing process configuration.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Billing process configuration updated."),
            @ApiResponse(code = 500, message = "Billing process configuration update failed.")
    ])
    Response updateBillingProcessConfiguration(
            @ApiParam(value = "Billing process configuration object that will be updated.", required = true)
            BillingProcessConfigurationWS billingProcessConfig
    ) {

        BillingProcessConfigurationWS billingConfig;
        try {
            webServicesSession.createUpdateBillingProcessConfiguration(billingProcessConfig);
            billingConfig = webServicesSession.getBillingProcessConfiguration();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return Response.ok().entity(billingConfig).build();
    }

    @GET
    @Path("/processes/{processId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a billing process run by the id.", response = BillingProcessWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning the billing process run with the provided id.",
                    response = BillingProcessWS.class),
            @ApiResponse(code = 400, message = "Invalid parameters used to get a billing process run."),
            @ApiResponse(code = 404, message = "A billing process run with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the billing process run failed.")
    ])
    Response getBillingProcess(
            @ApiParam(name = "processId", value = "The id of the billing process run that needs to be fetched.", required = true)
            @PathParam("processId")
            Integer processId) {

        BillingProcessWS billingRun;
        try {
            billingRun = webServicesSession.getBillingProcess(processId);
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return null != billingRun ?
                Response.ok().entity(billingRun).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/processes/last")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the last billing process run.", response = BillingProcessWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning the last billing process run.",
                    response = BillingProcessWS.class),
            @ApiResponse(code = 404, message = "A billing process run was not found."),
            @ApiResponse(code = 500, message = "Fetching the billing process run failed.")
    ])
    Response getLastBillingProcess() {

        BillingProcessWS billingRun;
        try {
            Integer billingRunId = webServicesSession.getLastBillingProcess();
            if (null == billingRunId) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            billingRun = webServicesSession.getBillingProcess(billingRunId);
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return null != billingRun ?
                Response.ok().entity(billingRun).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/processes/review")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the last review billing process run.", response = BillingProcessWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning the last review billing process run.",
                    response = BillingProcessWS.class),
            @ApiResponse(code = 404, message = "A review billing process run was not found."),
            @ApiResponse(code = 500, message = "Fetching the billing process run failed.")
    ])
    Response getReviewBillingProcess() {

        BillingProcessWS billingRun;
        try {
            billingRun = webServicesSession.getReviewBillingProcess();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return (null != billingRun) ?
            Response.ok().entity(billingRun).build() :
            Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/processes/{runDate}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Start a billing process run.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Billing process run started."),
            @ApiResponse(code = 500, message = "Billing process run start failed.")
    ])
    Response triggerBilling(
            @ApiParam(name="runDate", value = "Date for which the billing process will be started.",
                    required = true)
            @PathParam("runDate")
            Long runDate,
            @ApiParam(name="async", value = "Flag used to start the billing process run in asynchronous mode.",
                    required = false)
            @QueryParam("async")
            Boolean async
    ) {

        Date runDateParsed = new Date(runDate);
        try {
            if (null == async || !async) {
                webServicesSession.triggerBilling(runDateParsed);
            } else {
                webServicesSession.triggerBillingAsync(runDateParsed);
            }
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/processes/review")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Approve or disapprove the last review billing process run.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Review billing process approval flag updated."),
            @ApiResponse(code = 400, message = "Invalid parameters used to update the review billing process run."),
            @ApiResponse(code = 500, message = "Review billing process approval flag update failed.")
    ])
    Response setReviewApproval(
            @ApiParam(name = "approval", value = "Approval flag value to set to the review billing process.", required = true)
            @QueryParam("approval")
            Boolean approval
    ) {

        try {
            webServicesSession.setReviewApproval(approval);
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/processes/{processId}/invoices")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the invoices generated by a billing process run.", response = InvoiceWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning the generated invoices by the specified billing process run.",
                    response = InvoiceWS.class),
            @ApiResponse(code = 404, message = "A billing process run with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the generated invoices failed.")
    ])
    Response getGeneratedInvoices(
            @ApiParam(name = "processId", value = "The id of the billing process run that generated the invoices.", required = true)
            @PathParam("processId")
            Integer processId
    ) {

        List<InvoiceWS> invoices = new ArrayList<>();
        try {
            if (null == webServicesSession.getBillingProcess(processId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            for (Integer invoiceId : webServicesSession.getBillingProcessGeneratedInvoices(processId)) {
                invoices.add(webServicesSession.getInvoiceWS(invoiceId));
            }
            return Response.ok().entity(invoices).build();
        } catch (SessionInternalError error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }
}