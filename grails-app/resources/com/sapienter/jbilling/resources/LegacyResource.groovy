package com.sapienter.jbilling.resources

import java.text.SimpleDateFormat;
import java.util.Date;

import com.sapienter.jbilling.common.ErrorDetails;
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.user.UserWS;
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
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context
import javax.ws.rs.Consumes



/**
 * @author Ashish Srivastava
 * @since 14-JUN-2019
 */
@Path("/api/legacy")
@Api(value="/api/legacy", description = "Legacy.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class LegacyResource {

    IWebServicesSessionBean webServicesSession;

     @POST
    @Path("/invoice")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Save legacy invoice")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Legacy Invoice saved."),
            @ApiResponse(code = 500, message = "Failure while saving the Legacy Invoice")
    ])
    Response saveLegacyInvoice(
            @ApiParam(value = "Created Legacy Invoice object.", required = true)
            InvoiceWS invoice,
            @Context UriInfo uriInfo
            ) {
        try {
            Integer invoiceId = webServicesSession.saveLegacyInvoice(invoice);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(invoiceId)).build())
            .entity(webServicesSession.getInvoiceWS(invoiceId)).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

            @POST
            @Path("/payment")
            @Consumes(MediaType.APPLICATION_JSON)
            @Produces(MediaType.APPLICATION_JSON)
            @ApiOperation(value = "Save legacy Payment")
            @ApiResponses(value = [
                    @ApiResponse(code = 201, message = "Legacy Payment saved."),
                    @ApiResponse(code = 500, message = "Failure while saving  the Legacy Payment")
            ])
            Response saveLegacyPayment(
                    @ApiParam(value = "Created Legacy payment object.", required = true)
                    PaymentWS payment,
                    @Context UriInfo uriInfo
                    ) {
                try {
                    Integer paymentId = webServicesSession.saveLegacyPayment(payment);
                    return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(paymentId)).build())
                    .entity(webServicesSession.getPayment(paymentId)).build();
                } catch (Exception exp) {
                    return RestErrorHandler.mapErrorToHttpResponse(exp);
                }
            }

}