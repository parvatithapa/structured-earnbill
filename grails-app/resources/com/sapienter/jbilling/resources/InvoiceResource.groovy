package com.sapienter.jbilling.resources

import java.text.SimpleDateFormat;
import java.util.Date;

import com.sapienter.jbilling.common.ErrorDetails;
import com.sapienter.jbilling.server.invoice.InvoiceResourceHelperService;
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST;
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue;


/**
 * @author Bojan Dikovski
 * @since 11-OCT-2016
 */
@Path("/api/invoices")
@Api(value="/api/invoices", description = "Invoices.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class InvoiceResource {

    IWebServicesSessionBean webServicesSession
    InvoiceResourceHelperService invoiceResourceHelperService

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get an invoice by the id.", response = InvoiceWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Invoice with the provided id found.", response = InvoiceWS.class),
            @ApiResponse(code = 404, message = "Invoice with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the invoice failed.")
    ])
    Response getInvoice(
            @ApiParam(name = "id", value = "Invoice id.", required = true) @PathParam("id") Integer id) {

        InvoiceWS invoice;
        try {
            invoice = webServicesSession.getInvoiceWS(id);
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception);
        }
        return null != invoice ?
                Response.ok().entity(invoice).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete an invoice.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "The invoice with the provided id was deleted."),
            @ApiResponse(code = 404, message = "The invoice with the provided id does not exist."),
            @ApiResponse(code = 500, message = "The invoice deletion failed.")
    ])
    Response deleteInvoice(
            @ApiParam(name = "id", value = "Invoice id.", required = true) @PathParam("id") Integer id) {

        try {
            if (null == webServicesSession.getInvoiceWS(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteInvoice(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception);
        }
    }

    @POST
    @Path("/createinvoicewithdate")
    @ApiOperation(value = "create invoice.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Invoice created.", response = Integer[].class),
        @ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "The call resulted with internal error."),
        @ApiResponse(code = 404, message = "There is no applicable periode to generate invoice")
    ])
    public Response createInvoiceWithDate(
            @ApiParam(name = "userId", value = "create invoice against user",required = true)
            @QueryParam("userId") Integer userId,
            @ApiParam(name = "billingDate",value = "billingDate.", required = true)
            @QueryParam("billingDate") String billingDate,
            @ApiParam(name = "dueDatePeriodId",value = "dueDatePeriodId.", required = false)
            @QueryParam("dueDatePeriodId") Integer dueDatePeriodId,
            @ApiParam(name = "dueDatePeriodValue",value = "dueDatePeriodValue.", required = false)
            @QueryParam("dueDatePeriodValue") Integer dueDatePeriodValue,
            @ApiParam(name = "onlyRecurring",value = "onlyRecurring.", required = false)
            @QueryParam("onlyRecurring") Boolean onlyRecurring
    ) {
        try {
            SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
            Integer[] invoiceIds = webServicesSession.createInvoiceWithDate(userId, formatter.parse(billingDate), dueDatePeriodId, dueDatePeriodValue, onlyRecurring);
            if (Arrays.asList(invoiceIds).empty)
                return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok().entity(Arrays.asList(invoiceIds)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/createInvoiceFromOrder")
    @ApiOperation(value = "create invoice.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Invoice created.", response = Integer.class),
        @ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "The call resulted with internal error."),
        @ApiResponse(code = 404, message = "There is no applicable periode to generate invoice")
    ])
    public Response creatInvoiceFromOrder(
            @ApiParam(name = "orderId", value = "create invoice against Order",required = true)
            @QueryParam("orderId") Integer orderId,
            @ApiParam(name = "inoviceId")
            @QueryParam("inoviceId") Integer inoviceId
    ) {
        try {
            Integer invoiceId = webServicesSession.createInvoiceFromOrder(orderId,inoviceId);
            if (null == invoiceId)
                return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok().entity(invoiceId).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/paperinvoicepdf/{invoiceId}")
    @ApiOperation(value = "Generates and returns the paper invoice PDF for the given invoiceId.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Invoice PDF generated successfully for the given invoiceId."),
            @ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 404, message = "Invoice PDF with the provided Invoice ID not found."),
            @ApiResponse(code = 500, message = "Generation of the invoice PDF failed.")
    ])
    Response getPaperInvoicePDF(@ApiParam(name = "invoiceId", value = "Invoice id.", required = true)
        @PathParam("invoiceId") Integer invoiceId) {
        try {
            return invoiceResourceHelperService.generatePdfFileForInvoice(invoiceId)
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception)
        }
    }
		
		
	@GET
	@Path("/invoices/{startdate}/{enddate}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gets the invoices by the date range.")
	@ApiResponses(value = [
			@ApiResponse(code = 200, message = "Invoices with the provided period found.", response = InvoiceWS[].class),			
			@ApiResponse(code = 204, message = "Invoice with the provided period not found."),
			@ApiResponse(code = 500, message = "Fetching the invoice failed."),
			@ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class)
	])
	Response getInvoicesByDateRange(
		@ApiParam(name = "startdate",value = "The start date to fetch Invoices, Format: yyyy-MM-dd, Example: 2020-01-25", required = true) @PathParam("startdate") String startdate,
		@ApiParam(name = "enddate",value = "The end date to fetch Invoices, Format: yyyy-MM-dd, Example: 2020-01-25", required = true) @PathParam("enddate") String enddate,
		@ApiParam(name = "limit", value = "Limit") @DefaultValue("50") @QueryParam("limit") Integer limit,
        @ApiParam(name = "offset", value = "Offset") @DefaultValue("0") @QueryParam("offset") Integer offset) {
			
		try {			
			return invoiceResourceHelperService.getInvoicesByDateRange(startdate, enddate, offset, limit)
		} catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception)
		}
	}
}
