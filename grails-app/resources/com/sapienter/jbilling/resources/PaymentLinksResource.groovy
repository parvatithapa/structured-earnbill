package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponses
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import grails.plugin.springsecurity.annotation.Secured

@Path('/api/payments/{paymentId}/invoices')
@Api(value = "/api/payments/{paymentId}/invoices", description = "Payment links.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class PaymentLinksResource {

    IWebServicesSessionBean webServicesSession;

    @POST
    @Path("/{invoiceId}")
    @ApiOperation(value = "Create payment link.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Payment link created."),
            @ApiResponse(code = 400, message = "Invalid Payment link supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response createPaymentLink(
            @ApiParam(value = "PaymentId", required = true) @PathParam("paymentId") int paymentId,
            @ApiParam(value = "InvoiceId", required = true) @PathParam("invoiceId") int invoiceId) {
        try {
            webServicesSession.createPaymentLink(invoiceId,paymentId);
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{invoiceId}")
    @ApiOperation(value = "Deletes existing payment link with invoice.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Payment link deleted."),
            @ApiResponse(code = 404, message = "Payment or invoice not found."),
            @ApiResponse(code = 409, message = "Payment-Invoice link conflicted.",response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.",response = ErrorDetails.class)])
    public Response removePaymentLink(
            @ApiParam(value = "PaymentId", required = true) @PathParam("paymentId") int paymentId,
            @ApiParam(value = "InvoiceId", required = true) @PathParam("invoiceId") int invoiceId) {
        try {
            webServicesSession.removePaymentLink(invoiceId,paymentId)
            return Response.status(Response.Status.NO_CONTENT).build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @ApiOperation(value = "Deletes all payment links.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Payment link deleted."),
            @ApiResponse(code = 404, message = "Payment does not exist."),
            @ApiResponse(code = 409, message = "Payment link can not be deleted.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response deletePayment(@ApiParam(value = "PaymentId", required = true) @PathParam("paymentId") int paymentId) {
        try {
            webServicesSession.removeAllPaymentLinks(paymentId)
            return Response.noContent().build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}
