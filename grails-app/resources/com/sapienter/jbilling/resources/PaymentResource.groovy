package com.sapienter.jbilling.resources

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpStatus;

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentResourceWS
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponses
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiParam

import java.text.ParseException;
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId;
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone;

import javassist.bytecode.stackmap.BasicBlock.Catch;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.core.UriInfo

import paypal.payflow.Invoice;

import com.sapienter.jbilling.server.payment.SecurePaymentWS;

@Path('/api/payments')
@Api(value = "/api/payments", description = "Payments.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class PaymentResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get payment by id.", response = PaymentWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Payment found.", response = PaymentWS.class),
        @ApiResponse(code = 404, message = "Payment not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response getPaymentById(
            @ApiParam(name = "id",value = "The id of the payment that needs to be fetched.",required = true)
            @PathParam("id") Integer id) {

        try {
            PaymentWS payment = webServicesSession.getPayment(id);
            if (null == payment){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(payment).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/payments/{startDate}/{endDate}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the payments by the date range.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Payments with the provided period found.", response = PaymentResourceWS[].class),
            @ApiResponse(code = 204, message = "Payments with the provided period not found."),
            @ApiResponse(code = 500, message = "Fetching the payments failed."),
            @ApiResponse(code = 400, message = "Invalid parameters supplied.", response = ErrorDetails.class)
    ])
    Response getPaymentsByDateRange(
        @ApiParam(name = "startDate",value = "The start date to fetch Payments, Format: yyyy-MM-dd, Example: 2020-01-25", required = true) @PathParam("startDate") String startDate,
        @ApiParam(name = "endDate",value = "The end date to fetch Payments, Format: yyyy-MM-dd, Example: 2020-01-25", required = true) @PathParam("endDate") String endDate,
        @ApiParam(name="limit", value = "Limit") @DefaultValue("50") @QueryParam("limit") Integer limit,
        @ApiParam(name="offset", value = "Offset") @DefaultValue("0") @QueryParam("offset") Integer offset) {

        try {
            PaymentResourceWS[] payments = webServicesSession.getPaymentsByDateRange(
                                DateConvertUtils.getZonedDateTime(startDate + " 00:00", Constants.DATE_FORMAT_YYYY_MM_DD_HH_MM_SS,
                                    TimezoneHelper.getCompanyLevelTimeZone(webServicesSession.getCallerCompanyId()), Constants.DEFAULT_TIMEZONE),
                                DateConvertUtils.getZonedDateTime(endDate + " 00:00", Constants.DATE_FORMAT_YYYY_MM_DD_HH_MM_SS,
                                    TimezoneHelper.getCompanyLevelTimeZone(webServicesSession.getCallerCompanyId()), Constants.DEFAULT_TIMEZONE),
                                offset, limit)
            if(ArrayUtils.isEmpty(payments)) {
                return Response.noContent().build();
            }
            return Response.ok()
                    .entity(payments)
                    .build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create payment.")
    @ApiResponses(value = [
        @ApiResponse(code = 201, message = "Payment created.", response = PaymentWS.class),
        @ApiResponse(code = 400, message = "Invalid Payment supplied.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response createPayment(@ApiParam(value = "Created payment object.",required = true)
            PaymentWS paymentWS,
            @Context UriInfo uriInfo) {
        try {
            Integer paymentId = webServicesSession.createPayment(paymentWS);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(paymentId)).build())
                    .entity(webServicesSession.getPayment(paymentId)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/processpayment")
    @ApiOperation(value = "process payment.", response = SecurePaymentWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Payment created.", response = SecurePaymentWS.class),
        @ApiResponse(code = 400, message = "Invalid Payment supplied.", response = ErrorDetails.class),
		@ApiResponse(code = 402, message = "Payment required."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response processPayment(
            @ApiParam(name = "paymentWS", value = "Created payment on gateway",required = true)
            PaymentWS paymentWS,
            @ApiParam(name = "invoiceId",value = "User object containing update data.", required = false) 
            @QueryParam("invoiceId") Integer invoiceId
            ) {
        try {
            PaymentAuthorizationDTOEx paymentAuthorizationDTOEx = webServicesSession.processPayment(paymentWS,invoiceId);
			
            return RestErrorHandler.mapStatusToHttpResponse(paymentAuthorizationDTOEx.getSecurePaymentWS());
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }


    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing payment.", response = PaymentWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Payment updated."),
        @ApiResponse(code = 400, message = "Invalid payment supplied.", response = ErrorDetails.class),
        @ApiResponse(code = 404, message = "Payment not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response updatePayment(@ApiParam(name = "id", value = "Payment Id.",required = true)
            @PathParam("id")Integer id,
            @ApiParam(value = "Payment containing update data.", required = true)
            PaymentWS payment){
        try {
            payment.setId(id);
            webServicesSession.updatePayment(payment);
            payment=webServicesSession.getPayment(id)
            return Response.ok().entity(payment).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing payment.")
    @ApiResponses(value = [
        @ApiResponse(code = 204, message = "Payment with the supplied id deleted."),
        @ApiResponse(code = 404, message = "Payment with the supplied id does not exists."),
        @ApiResponse(code = 409, message = "Payment can not be deleted.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response deletePayment(@ApiParam(name = "id", value = "Payment id.", required = true)
            @PathParam("id") Integer id) {
        try {
            webServicesSession.deletePayment(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception);
        }
    }
}
