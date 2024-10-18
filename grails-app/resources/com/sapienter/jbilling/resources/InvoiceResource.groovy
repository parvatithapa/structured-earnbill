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
        } catch (Exception error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
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
        } catch (Exception error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
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
        } catch (Exception error) {
            return RestErrorHandler.mapErrorToHttpResponse(error)
        }
    }

	@POST
	@Path("/createEInvoice/{invoiceId}")
	@ApiOperation(value = "create Einvoice for the given invoiceId.")
	@ApiResponses(value = [
			@ApiResponse(code = 200, message = "EInvoie generated successfully for the given invoiceId."),
			@ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class),
			@ApiResponse(code = 404, message = "Invoice ID not found."),
			@ApiResponse(code = 500, message = "createEInvoice failed.")
	])
	Response createEInvoice(
		@ApiParam(name = "invoiceId", value = "Invoice id.", required = true)
		@PathParam("invoiceId") Integer invoiceId) {
		try {
			return Response.ok().entity(invoiceResourceHelperService.createEInvoice(invoiceId)).build()
		} catch(Exception exception) {
			return RestErrorHandler.mapErrorToHttpResponse(exception)
		}
	}

	@POST
	@Path("/eInvoiceDetails/{invoiceId}")
	@ApiOperation(value = "fetch eInvocie details for given invoiceId.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "fetch eInvocie details for given invoiceId."),
		@ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class),
		@ApiResponse(code = 404, message = "Invoice ID not found."),
		@ApiResponse(code = 500, message = "createEInvoice failed.")
	])
	Response getEInvoiceResponse(@ApiParam(name = "invoiceId", value = "Invoice id.", required = true)
			@PathParam("invoiceId") Integer invoiceId) {
		try {
			return Response.ok().entity(invoiceResourceHelperService.findEInvoiceDetailsByInvoiceId(invoiceId)).build()
		} catch(Exception exception) {
			return RestErrorHandler.mapErrorToHttpResponse(exception)
		}
	}
}
