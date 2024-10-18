package com.sapienter.jbilling.resources

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
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

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.mediation.IMediationSessionBean
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord
import com.sapienter.jbilling.server.mediation.JbillingMediationRecordRestWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationBL
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS
import com.sapienter.jbilling.server.mediation.MediationProcess
import com.sapienter.jbilling.server.mediation.MediationRestHelperService;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

@Path('/api/mediation')
@Api(value = "/api/mediation", description = "Mediation Methods.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class MediationResource {

    IWebServicesSessionBean webServicesSession
    MediationRestHelperService mediationRestHelperService

    private DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");

    @POST
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create mediation configuration.")
    @ApiResponses(value = [
    @ApiResponse(code = 201, message = "Configuration saved.", response = MediationConfigurationWS.class),
    @ApiResponse(code = 400, message = "Invalid configuration."),
    @ApiResponse(code = 500, message = "Failure while saving the configuration.")
    ])
    Response createMediationConfiguration(
            @ApiParam(name = "conf", value = "Mediation Configuration", required = true) MediationConfigurationWS conf,
            @Context UriInfo uriInfo
    ) {
        IMediationSessionBean mediationBean = com.sapienter.jbilling.server.util.Context
                .getBean(com.sapienter.jbilling.server.util.Context.Name.MEDIATION_SESSION);
        try {
            Integer id = webServicesSession.createMediationConfiguration(conf);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(id)).build())
                    .entity(mediationBean.getMediationConfiguration(id)).build()
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve all the mediation configuration.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "All mediation configurations.", response = MediationConfigurationWS[].class),
    @ApiResponse(code = 500, message = "Failure while retrieving configurations.")
    ])
    Response getAllMediationConfigurations(
            @Context UriInfo uriInfo
    ) {
        try {
            MediationConfigurationWS[] configs = webServicesSession.getAllMediationConfigurations();
            return Response.ok().entity(configs).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @PUT
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update mediation configurations.")
    @ApiResponses(value = [
    @ApiResponse(code = 201, message = "Successful. Ids of created configurations.", response = MediationConfigurationWS[].class),
    @ApiResponse(code = 500, message = "Failure while updating.")
    ])
    Response updateAllMediationConfigurations(@ApiParam(name = "conf", value = "Mediation Configurations", required = true) MediationConfigurationWS[] conf ) {
        IMediationSessionBean mediationBean = com.sapienter.jbilling.server.util.Context
                .getBean(com.sapienter.jbilling.server.util.Context.Name.MEDIATION_SESSION);

        try {

            Integer[] ids= webServicesSession.updateAllMediationConfigurations(Arrays.asList(conf))

            List<MediationConfigurationWS> confList = new ArrayList<>(ids.length);
            for(Integer id : ids) {
                confList.add(MediationConfigurationBL.getWS(mediationBean.getMediationConfiguration(id)))
            }
            return Response.ok().entity(confList.toArray(new MediationConfigurationWS[confList.size()])).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @DELETE
    @Path("/configuration/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes mediation configuration.")
    @ApiResponses(value = [
    @ApiResponse(code = 204, message = "Configuration deleted."),
    @ApiResponse(code = 500, message = "Failure while trying to delete.")
    ])
    Response deleteMediationConfiguration(@ApiParam(name = "id", value = "Id of configuration to delete", required = true)
                                          @PathParam("id") Integer id) {
        try {
            webServicesSession.deleteMediationConfiguration(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @POST
    @Path("/process")
    @ApiOperation(value = "Trigger all mediation processes.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Mediation triggered."),
    @ApiResponse(code = 500, message = "Failure while triggering mediation.")
    ])
    Response triggerMediation(
            @Context UriInfo uriInfo
    ) {

        try {
            webServicesSession.triggerMediation();
            return Response.status(Response.Status.OK).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @POST
    @Path("/process/configuration/{configId}")
    @ApiOperation(value = "Trigger a specific mediation processes.")
    @ApiResponses(value = [
    @ApiResponse(code = 201, message = "Mediation triggered."),
    @ApiResponse(code = 500, message = "Failure while triggering mediation.")
    ])
    Response triggerMediationByConfiguration(@ApiParam(name = "configId", value = "Id of configuration to trigger", required = true)
                                             @PathParam("configId") Integer id,
                                             @Context UriInfo uriInfo
    ) {

        try {
            UUID uuid = webServicesSession.triggerMediationByConfiguration(id);
            return Response.created(uriInfo.getBaseUriBuilder().path("/api/mediation/process/"+uuid.toString()).build())
                    .build()
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve all the mediation processes.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "All processes returned.", response = MediationProcess[].class),
    @ApiResponse(code = 500, message = "Failure while retrieving processes.")
    ])
    Response getAllMediationProcesses(
            @Context UriInfo uriInfo
    ) {
        try {
            MediationProcess[] processes = webServicesSession.getAllMediationProcesses();
            return Response.ok().entity(processes).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process/{processId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve a mediation process.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "All a processes.", response = MediationProcess.class),
    @ApiResponse(code = 500, message = "Failure while retrieving process.")
    ])
    Response getMediationProcess( @ApiParam(name = "processId", value = "Id of mediation process", required = true)
                                  @PathParam("processId") String id,
                                  @Context UriInfo uriInfo
    ) {
        try {
            MediationProcess process = webServicesSession.getMediationProcess(UUID.fromString(id));
            return Response.ok().entity(process).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @DELETE
    @Path("/process/{processId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Undo mediation process.")
    @ApiResponses(value = [
    @ApiResponse(code = 204, message = "Mediation undone."),
    @ApiResponse(code = 500, message = "Failure trying to undo.")
    ])
    Response undoMediation(@ApiParam(name = "processId", value = "Id of configuration to delete", required = true)
                           @PathParam("processId") String id) {
        try {
            webServicesSession.undoMediation(UUID.fromString(id));
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process/{processId}/events")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve mediation records for process.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Find mediation records for process.", response = JbillingMediationRecord[].class),
    @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
    Response getMediationRecordsByMediationProcess( @PathParam("processId") @ApiParam(name="processId", required = true) String processId,
                                                    @QueryParam("limit") @ApiParam(name="limit", required = true) Integer limit,
                                                    @QueryParam("offset") @ApiParam(name="offset", required = true) Integer offset,
                                                    @QueryParam("startDate") @ApiParam("startDate") String startDateStr,
                                                    @QueryParam("endDate") @ApiParam("endDate") String endDateStr,
                                                    @Context UriInfo uriInfo
    ) {
        try {
            Date startDate = (startDateStr == null || startDateStr.length() == 0 ) ? null : dateFormat.parseLocalDate(startDateStr).toDate();
            Date endDate = (endDateStr == null || endDateStr.length() == 0 ) ? null : dateFormat.parseLocalDate(endDateStr).toDate();

            JbillingMediationRecord[] records = webServicesSession.getMediationRecordsByMediationProcess(UUID.fromString(processId),
                    offset, limit, startDate, endDate);

            return Response.ok().entity(records).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process/events/order/{orderId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve mediation records for order.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Find mediation records for process.", response = JbillingMediationRecord[].class),
    @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
    Response getMediationEventsForOrder( @PathParam("orderId") @ApiParam(name="uriInfo", required = true) Integer orderId,
                                         @Context UriInfo uriInfo
    ) {
        try {
            JbillingMediationRecord[] records = webServicesSession.getMediationEventsForOrder(orderId);

            return Response.ok().entity(records).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process/events/invoice/{invoiceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve mediation records for invoice.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Find mediation records for process.", response = JbillingMediationRecord[].class),
    @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
    Response getMediationEventsForInvoice( @PathParam("invoiceId") @ApiParam(name="invoiceId", required = true) Integer invoiceId,
                                         @Context UriInfo uriInfo
    ) {
        try {
            JbillingMediationRecord[] records = webServicesSession.getMediationEventsForInvoice(invoiceId);

            return Response.ok().entity(records).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process/eventSearch/{orderId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve mediation records for order.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Find mediation records for process.", response = JbillingMediationRecord[].class),
    @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
    Response getMediationEventsForOrderDateRange( @PathParam("orderId") @ApiParam(name="orderId", required = true) Integer orderId,
                                                  @QueryParam("limit") @ApiParam(name="limit", required = true) Integer limit,
                                                  @QueryParam("offset") @ApiParam(name="offset", required = true) Integer offset,
                                                  @QueryParam("startDate") @ApiParam(name="startDate", required = true) String startDateStr,
                                                  @QueryParam("endDate") @ApiParam(name="endDate", required = true) String endDateStr,
                                                  @Context UriInfo uriInfo
    ) {
        try {
            Date startDate = (startDateStr == null || startDateStr.length() == 0 ) ? null : dateFormat.parseLocalDate(startDateStr).toDate();
            Date endDate = (endDateStr == null || endDateStr.length() == 0 ) ? null : dateFormat.parseLocalDate(endDateStr).toDate();

            JbillingMediationRecord[] records = webServicesSession.getMediationEventsForOrderDateRange(orderId,
                    startDate, endDate, offset, limit);

            return Response.ok().entity(records).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process/events/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve mediation records for user.")
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Find mediation records for process.", response = JbillingMediationRecordRestWS[].class),
    @ApiResponse(code = 400, message = "Invalid parameters passed."),
    @ApiResponse(code = 404, message = "User not found."),
    @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
    Response getMediationEventsForUser( @PathParam("userId") @ApiParam(name="userId", required = true) Integer userId,
                                                  @DefaultValue("10000")@QueryParam("limit") @ApiParam(name="limit") Integer limit,
                                                  @DefaultValue("0")@QueryParam("offset") @ApiParam(name="offset") Integer offset,
                                                  @QueryParam("startDate") @ApiParam(name="startDate") String startDateStr,
                                                  @QueryParam("endDate") @ApiParam(name="endDate") String endDateStr) {
        try {
            Date startDate = (startDateStr == null || startDateStr.length() == 0 ) ? null : dateFormat.parseLocalDate(startDateStr).toDate();
            Date endDate = (endDateStr == null || endDateStr.length() == 0 ) ? null : dateFormat.parseLocalDate(endDateStr).toDate();
            JbillingMediationRecord[] records = webServicesSession.getMediationEventsForUserDateRange(userId, startDate, endDate, offset, limit);
            JbillingMediationRecordRestWS[] mediationRecords = mediationRestHelperService.convertPricingFields(records);
            return Response.ok().entity(mediationRecords).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/process/events/unbilled/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve mediation records for all active mediated orders by user.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Fetch all unbilled mediation records by user.", response = JbillingMediationRecordRestWS[].class),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
    Response getUnBilledMediationEventsByUser( @PathParam("userId")
            @ApiParam(name="userId", required = true) Integer userId) {
        try {
            JbillingMediationRecord[] records = webServicesSession.getUnBilledMediationEventsByUser(userId);
            JbillingMediationRecordRestWS[] mediationRecords = mediationRestHelperService.convertPricingFields(records);
            return Response.ok().entity(mediationRecords).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }
}
