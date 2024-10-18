package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import com.wordnik.swagger.annotations.*
import javax.ws.rs.*
import grails.plugin.springsecurity.annotation.Secured

@Path('/api/paymentMethodTemplates')
@Api(value = "/api/paymentMethodTemplates", description = "Payment method templates.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class PaymentMethodTemplatesResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get payment method template by id.", response = PaymentMethodTemplateWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Payment method template found.", response = PaymentMethodTemplateWS.class),
            @ApiResponse(code = 404, message = "Payment method template not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response getPaymentMethodTemplateById(
            @ApiParam(name = "id",value = "The id of the payment method template that needs to be fetched.",required = true)
            @PathParam("id") Integer id) {

        try {
            PaymentMethodTemplateWS paymentMethodTemplate = webServicesSession.getPaymentMethodTemplate(id);
            if (null == paymentMethodTemplate){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(paymentMethodTemplate).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}
