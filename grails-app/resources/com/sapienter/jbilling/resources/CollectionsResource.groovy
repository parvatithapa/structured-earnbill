package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.discount.DiscountWS
import com.sapienter.jbilling.server.process.AgeingWS
import com.sapienter.jbilling.server.process.ProcessStatusWS
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.*
import grails.plugin.springsecurity.annotation.Secured
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path('/api/collections')
@Api(value = "/api/collections", description = "Collections Methods.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class CollectionsResource {

    IWebServicesSessionBean webServicesSession;

    private DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");

    @POST
    @Path("/configuration/{languageId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Save ageing configuration for company.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Configuration saved."),
            @ApiResponse(code = 500, message = "Failure while getting the configuration.")
    ])
    Response saveAgeingConfiguration(
            @ApiParam(name = "languageId", value = "Language", required = true) @PathParam("languageId") Integer languageId,
            @ApiParam(name = "steps", value = "Ageing Steps", required = true) AgeingWS[] steps,
            @Context UriInfo uriInfo
            ) {

        try {
            webServicesSession.saveAgeingConfiguration(steps, languageId);
            return Response.status(Response.Status.CREATED).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }

    @GET
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve ageing configuration for company.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Ageing configuration.", response = AgeingWS.class),
            @ApiResponse(code = 500, message = "Failure while retrieving Ageing.")
    ])
    Response getAgeingConfiguration(
            @Context UriInfo uriInfo
    ) {
        AgeingWS[] ageingSteps;
        try {
            ageingSteps = webServicesSession.getAgeingConfiguration(Constants.LANGUAGE_ENGLISH_ID);
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
        return Response.ok().entity(ageingSteps).build();
    }

    @GET
    @Path("/processes/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Getting ageing process status.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "ProcessStatusWS.", response = ProcessStatusWS.class),
            @ApiResponse(code = 500, message = "Failure while getting ProcessStatusWS.")
    ])
    Response getAgeingProcessStatus( ) {
        try {

            ProcessStatusWS status = webServicesSession.getAgeingProcessStatus()
            return Response.ok().entity(status).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }

    @POST
    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes existing discount.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Process triggered."),
            @ApiResponse(code = 500, message = "Failure while triggering ageing process.")
    ])
    Response triggerAgeing(@ApiParam(name = "date", value = "Date in format yyyy-mm-dd.", required = true)
                                      @PathParam("date") String date) {
        try {
            webServicesSession.triggerAgeing(dateFormat.parseLocalDate(date).toDate());
            return Response.status(Response.Status.OK).build();
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie);
        }
    }
}
