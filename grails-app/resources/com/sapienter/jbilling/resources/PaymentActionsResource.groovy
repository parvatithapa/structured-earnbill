package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponses
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiParam

import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.POST
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Context
import javax.ws.rs.core.UriInfo
import grails.plugin.springsecurity.annotation.Secured

@Path("/api/payments/invoices")
@Api(value = "/api/payments/invoices", description = "Payment actions.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class PaymentActionsResource {

    IWebServicesSessionBean webServicesSession;

    @POST
    @Path("/{invoiceId}/apply")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Apply payment.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Payment is created."),
            @ApiResponse(code = 404, message = "Invoice not found."),
            @ApiResponse(code = 400, message = "Invalid payment supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response applyPayment(
            @ApiParam(name = "invoiceId", value = "Invoice ID is optional." ,required = true)
            @PathParam("invoiceId")Integer invoiceId,
            @ApiParam(value = "PaymentWS data.", required = true)PaymentWS paymentWs,
            @Context UriInfo uriInfo) {
        try {
            Integer paymentId = webServicesSession.applyPayment(paymentWs,invoiceId);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(paymentId)).build())
            .build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/{invoiceId}/pay")
    @ApiOperation(value = "Pay invoice.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Invoice is payed.", response = PaymentAuthorizationDTOEx.class),
            @ApiResponse(code = 400, message = "Invalid invoice supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 404, message = "Invoice not found.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response payInvoice(
            @ApiParam(name = "invoiceId", value = "Invoice Id.",required = true)
            @PathParam("invoiceId")Integer invoiceId,
            @Context UriInfo uriInfo) {
        try {
            PaymentAuthorizationDTOEx paymentAuthorization = webServicesSession.payInvoice(invoiceId);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(paymentAuthorization.getId())).build())
                    .build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/{invoiceId}/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Payment is processed.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Payment is processed.", response = PaymentAuthorizationDTOEx.class),
            @ApiResponse(code = 400, message = "Payment is NOT processed.", response = ErrorDetails.class),
            @ApiResponse(code = 404, message = "Invoice not found.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response processPayment(
            @ApiParam(name = "invoiceId", value = "Invoice Id.",required = true)@PathParam("invoiceId")Integer invoiceId,
            @ApiParam(value = "PaymentWS data.", required = true)PaymentWS paymentWs,
            @Context UriInfo uriInfo) {
        try {
            PaymentAuthorizationDTOEx  paymentAuth = webServicesSession.processPayment(paymentWs,invoiceId);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(paymentAuth.getId())).build())
                    .build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}
