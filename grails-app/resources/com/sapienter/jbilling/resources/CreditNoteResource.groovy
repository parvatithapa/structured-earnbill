package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.payment.PaymentWS

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import org.apache.commons.lang3.ArrayUtils
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.creditnote.CreditNoteInvoiceMapWS
import com.sapienter.jbilling.server.creditnote.CreditNoteWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.sapienter.jbilling.common.ErrorDetails

import javax.ws.rs.core.UriInfo


@Path("/api/credits")
@Api(value = "/api/credits", description = "CreditNote")
@Produces(MediaType.APPLICATION_JSON)
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class CreditNoteResource {

    private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

    IWebServicesSessionBean webServicesSession

    @GET
    @Path("/invoicemaps/{invoiceCreationStartDate}/{invoiceCreationEndDate}")
    @ApiOperation(value = "Get CreditNoteInvoiceMaps by invoiceCreationStartDate and invoiceCreationEndDate.", response = CreditNoteInvoiceMapWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "CreditNoteInvoiceMapWS successfully returned.", response = CreditNoteInvoiceMapWS.class),
        @ApiResponse(code = 404, message = "CreditNoteInvoiceMapWS not found."),
        @ApiResponse(code = 500, message = "Failure while getting the CreditNoteInvoiceMapWS.")
    ])
    Response getCreditNoteInvoiceMaps(
            @ApiParam(name = "invoiceCreationStartDate",
            value = "invoiceCreationStartDate in format yyyy-mm-dd.",
            required = true)
            @PathParam("invoiceCreationStartDate") String invoiceCreationStartDate,
            @ApiParam(name = "invoiceCreationEndDate",
            value = "invoiceCreationEndDate in format yyyy-mm-dd.",
            required = true)
            @PathParam("invoiceCreationEndDate") String invoiceCreationEndDate) {
        try {
            Date startDate = dateFormat.parseLocalDate(invoiceCreationStartDate).toDate()
            Date endDate = dateFormat.parseLocalDate(invoiceCreationEndDate).toDate()
            return Response.ok()
                    .entity(webServicesSession.getCreditNoteInvoiceMaps(startDate, endDate))
                    .build()
        } catch(UnsupportedOperationException | IllegalArgumentException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Date parsing failed, invalid invoiceCreationStartDate or invoiceCreationEndDate passed!")
        } catch(Exception ex) {
            return RestErrorHandler.mapErrorToHttpResponse(ex)
        }
    }

    @GET
    @Path("/{creditNoteId}")
    @ApiOperation(value = "Get creditNote by id.", response = CreditNoteWS.class)
    @ApiResponses(value = [
    @ApiResponse(code = 200, message = "Credit Note found.", response = CreditNoteWS.class),
    @ApiResponse(code = 404, message = "Credit Note not found."),
    @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getCreditNote(
        @ApiParam(name = "creditNoteId",
        value = "The id of the creditNote that needs to be fetched.",
        required = true)
        @PathParam("creditNoteId") Integer creditNoteId) {
        try {
            CreditNoteWS creditNote = webServicesSession.getCreditNote(creditNoteId);
            if (null == creditNote || creditNote.deleted == 1){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(creditNote).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Path("/getallcreditnotes")
    @ApiOperation(value = "Get creditNote by id.", response = CreditNoteWS[].class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Credit Notes found.", response = CreditNoteWS[].class),
        @ApiResponse(code = 404, message = "Credit Note not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    Response getAllCreditNotes() {
        try {
            Integer entityId = webServicesSession.getCallerCompanyId()
            CreditNoteWS[] creditNotes = webServicesSession.getAllCreditNotes(entityId)
            return Response.ok()
                    .entity(creditNotes)
                    .build()
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve Credit notes for user.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Find credit notes for user.", response = CreditNoteWS[].class),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "Failure while retrieving credit notes.")
    ])
    Response getCreditNotesByUser( @PathParam("userId") @ApiParam(name="userId", required = true) Integer userId,
            @DefaultValue("10000")@QueryParam("limit") @ApiParam(name="limit") Integer limit,
            @DefaultValue("0")@QueryParam("offset") @ApiParam(name="offset") Integer offset) {
        try {
            return Response.ok()
                    .entity(webServicesSession.getCreditNotesByUser(userId, offset, limit))
                    .build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

	@GET
	@Path("/creditnotes/{startDate}/{endDate}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gets the credit notes by the date range.")
	@ApiResponses(value = [
			@ApiResponse(code = 200, message = "Credit notes with the provided period found.", response = CreditNoteWS[].class),
			@ApiResponse(code = 204, message = "Credit notes with the provided period not found."),
			@ApiResponse(code = 500, message = "Fetching the credit notes failed."),
			@ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class)
	])
	Response getCreditNotesByDateRange(
		@ApiParam(name = "startDate",value = "The start date to fetch Credit notes, Format: yyyy-MM-dd, Example: 2020-01-25", required = true) @PathParam("startDate") String startDate,
		@ApiParam(name = "endDate",value = "The end date to fetch Credit notes, Format: yyyy-MM-dd, Example: 2020-01-25", required = true) @PathParam("endDate") String endDate,
		@ApiParam(name="limit", value = "Limit") @DefaultValue("50") @QueryParam("limit") Integer limit,
		@ApiParam(name="offset", value = "Offset") @DefaultValue("0") @QueryParam("offset") Integer offset) {

		try {
			CreditNoteWS[] creditNote = webServicesSession.getCreditNotesByDateRange(startDate, endDate, offset, limit)
			if(ArrayUtils.isEmpty(creditNote)) {
				return Response.noContent().build()
			}
			return Response.ok()
					.entity(creditNote)
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete existing credit note")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Credit note with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Credit note with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Credit note can not be deleted.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    Response deleteAdHocCreditNote(@ApiParam(name = "id", value = "Credit Note Id.", required = true)
                           @PathParam("id") Integer id) {
        try {
            webServicesSession.deleteCreditNote(id)
            return Response.status(Response.Status.NO_CONTENT).build()
        } catch (SessionInternalError sie) {
            return RestErrorHandler.mapErrorToHttpResponse(sie)
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create Ad Hoc Credit Note.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Credit Note created.", response = CreditNoteWS.class),
            @ApiResponse(code = 400, message = "Invalid Credit Note supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    Response createAdHocCreditNote(@ApiParam(value = "Created Credit Note object.", required = true)
                                           CreditNoteWS creditNoteWS,
                                   @Context UriInfo uriInfo) {
        try {
            Integer creditNoteId = webServicesSession.createAdhocCreditNote(creditNoteWS)
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(creditNoteId)).build())
                    .entity(webServicesSession.getCreditNote(creditNoteId)).build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

}

