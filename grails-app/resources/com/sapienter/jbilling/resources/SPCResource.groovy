package com.sapienter.jbilling.resources

import grails.plugin.springsecurity.annotation.Secured

import javax.annotation.Resource;
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT;
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import javax.ws.rs.core.MediaType
import javax.ws.rs.DefaultValue;

import java.time.ZonedDateTime;

import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.spc.wookie.crm.SpcOutBoundInterchangeHelperService;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Constants;

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.groovy.grails.web.json.JSONObject;

import com.sapienter.jbilling.server.integration.OutBoundInterchangeWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord
import com.sapienter.jbilling.server.mediation.JbillingMediationRecordRestWS
import com.sapienter.jbilling.server.mediation.MediationRestHelperService
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecordRestWS;
import com.sapienter.jbilling.server.mediation.MediationRestHelperService;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

@Path("/api/spc")
@Api(value="/api/spc", description = "spc")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class SPCResource {
    IWebServicesSessionBean webServicesSession
	
    SPCMediationHelperService spcMediationHelperService
	
    MediationRestHelperService mediationRestHelperService
	
	SpcOutBoundInterchangeHelperService spcOutBoundInterchangeHelperService
	
    private DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
	
    @GET
    @Path("/process/events/unbilled/service/{serviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve mediation records for all active mediated orders by service Id.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Fetch all unbilled mediation records by service Id.", response = JbillingMediationRecordRestWS[].class),
        @ApiResponse(code = 204, message = "Records not found."),
        @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
	Response getUnBilledMediationEventsByServiceId( @PathParam("serviceId")
        @ApiParam(name="serviceId", required = true, value="Id of service subscription.") String serviceId) {
        try {
             Integer entityId = webServicesSession.getCallerCompanyId();
             JbillingMediationRecord[] mediationRecords =  spcMediationHelperService.getUnBilledMediationEventsByServiceId(entityId, serviceId);
             JbillingMediationRecordRestWS[] mediationRecordWss = mediationRestHelperService.convertPricingFields(mediationRecords); 
             if(ArrayUtils.isEmpty(mediationRecordWss)) {
                return Response.noContent().build();
             }
			 return Response.ok().entity(mediationRecordWss).build();
        } catch (Exception exp) {
             return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }
	
    @GET
    @Path("/process/events/service/daterange/{serviceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve all mediation records for Service Id for a date range.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Found mediation records for Service Id.", response = JbillingMediationRecord[].class),
		@ApiResponse(code = 204, message = "Records not found."),
		@ApiResponse(code = 400, message = "Invalid parameters passed"),
        @ApiResponse(code = 500, message = "Failure while retrieving records.")
    ])
    Response getMediationEventsByServiceIdAndDateRange(
        @PathParam("serviceId")  @ApiParam(name="serviceId", required = true, value="Id of service subscription.") String serviceId,
        @QueryParam("limit")     @ApiParam(name="limit", required = false, value ="Max result for the events fetch.") @DefaultValue("10000") Integer limit,
        @QueryParam("offset")    @ApiParam(name="offset", required = false, value ="Records next to the offset will be returned.") @DefaultValue("0") Integer offset,
        @QueryParam("startDate") @ApiParam(name="startDate", required = true, value = "yyyy-MM-dd") String startDate,
        @QueryParam("endDate")   @ApiParam(name="endDate",   required = true, value = "yyyy-MM-dd") String endDate,
        @Context UriInfo uriInfo) {
        try {
             Integer entityId = webServicesSession.getCallerCompanyId();
             JbillingMediationRecord[] mediationRecords = spcMediationHelperService.getMediationEventsByServiceIdAndDateRange(entityId, serviceId,offset, limit, startDate, endDate);
             JbillingMediationRecordRestWS[] mediationRecordWss = mediationRestHelperService.convertPricingFields(mediationRecords);
             if(ArrayUtils.isEmpty(mediationRecordWss)) {
                return Response.noContent().build();
             }
             return Response.ok().entity(mediationRecordWss).build();
        } catch (Exception exp) {
             return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }
		
	@GET
	@Path("/process/outbound/getUnprocessedOutboundMessages")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Retrieve all the unprocessed outbound messages.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "Fetch all the unprocessed outbound messages.", response = OutBoundInterchangeWS[].class),
		@ApiResponse(code = 204, message = "Records not found."),
		@ApiResponse(code = 500, message = "Failure while retrieving records.")
	])
	Response getUnprocessedOutboundMessages() {
		try {
			Integer entityId = webServicesSession.getCallerCompanyId();
			OutBoundInterchangeWS[] unprocessedOutboundMsgs =  spcOutBoundInterchangeHelperService.findUnprocessedOutBoundMessages(entityId);
			if(ArrayUtils.isEmpty(unprocessedOutboundMsgs)) {
				return Response.noContent().build();
			}
			return Response.ok().entity(unprocessedOutboundMsgs).build();
		} catch (Exception exp) {
			return RestErrorHandler.mapErrorToHttpResponse(exp);
		}
	}
	
	@PUT
	@Path("/process/outbound/markOutboundMessageAsSent/{outboundRequestId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Update unprocessed outbound requset status to sent for the outbound request Id.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "Updated unprocessed outbound requset status to sent for the outbound request Id.", response = String.class),
		@ApiResponse(code = 404, message = "Record not found."),
		@ApiResponse(code = 400, message = "Status of outbound message is not unprocessed for the provided id."),
		@ApiResponse(code = 500, message = "Failure to update outbound status.")
	])
	
	Response markOutboundMessageAsSent(@ApiParam(name="outboundRequestId", required = true, value="Id of outbound request.") @PathParam("outboundRequestId")  Integer outboundRequestId) {
		try {
			Integer updatedOutboundReqId =  spcOutBoundInterchangeHelperService.updateOutboundMessageStatusToSent(outboundRequestId);
			return Response.ok().entity(JSONObject.quote(String.format("Outbound message status is updated to SENT successfully for the provided Outbound Request Id [%d]", updatedOutboundReqId))).build()
		} catch (Exception exp) {
			return RestErrorHandler.mapErrorToHttpResponse(exp);
		}
	}
		
}
