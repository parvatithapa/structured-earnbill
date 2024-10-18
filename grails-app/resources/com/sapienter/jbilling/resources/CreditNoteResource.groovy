package com.sapienter.jbilling.resources

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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
            return Response.ok()
                    .entity(webServicesSession.getCreditNote(creditNoteId))
                    .build()
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

            Integer entityId = webServicesSession.getCallerCompanyId();
            CreditNoteWS[]  creditNotes = webServicesSession.getAllCreditNotes(entityId);
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

}

