package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler

import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import com.wordnik.swagger.annotations.*

import javax.ws.rs.*

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.core.UriInfo

@Path('/api/paymentMethodTypes')
@Api(value = "/api/paymentMethodTypes", description = "Payment method types.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class PaymentMethodTypesResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get payment method type by id.", response = PaymentMethodTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Payment method type found.", response = PaymentMethodTypeWS.class),
            @ApiResponse(code = 404, message = "Payment method type not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response getPaymentMethodTypeById(
            @ApiParam(name = "id",value = "The id of the payment method type that needs to be fetched.",required = true)
            @PathParam("id") Integer id) {

        try {
            PaymentMethodTypeWS paymentMethodType = webServicesSession.getPaymentMethodType(id);
            if (null == paymentMethodType){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(paymentMethodType).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create payment method type.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Payment Method Type created.", response = PaymentMethodTypeWS.class),
            @ApiResponse(code = 400, message = "Invalid Payment Method Type supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response createPaymentMethodType(@ApiParam(value = "Created payment method type object.",required = true)
                                            PaymentMethodTypeWS paymentMethodTypeWS,
                                            @Context UriInfo uriInfo) {
        try {
            Integer paymentMethodTypeId = webServicesSession.createPaymentMethodType(paymentMethodTypeWS);
            PaymentMethodTypeWS currStatus = webServicesSession.getPaymentMethodType(paymentMethodTypeId)
            if (null == currStatus){
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(paymentMethodTypeId)).build())
                    .entity(currStatus).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing payment method type.", response = PaymentMethodTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Payment Method Type updated."),
            @ApiResponse(code = 400, message = "Invalid payment method type supplied.", response = ErrorDetails.class),
            @ApiResponse(code = 404, message = "Payment method type not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response updatePaymentMethodType(@ApiParam(name = "id", value = "Payment Method Type Id.",required = true)
                                            @PathParam("id")Integer id,
                                            @ApiParam(value = "Payment Method Type containing update data.", required = true)
                                            PaymentMethodTypeWS paymentMethodType){
        try {
            if (null == webServicesSession.getPaymentMethodType(id)){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            paymentMethodType.setId(id);
            webServicesSession.updatePaymentMethodType(paymentMethodType);
            paymentMethodType=webServicesSession.getPaymentMethodType(id)
            return Response.ok().entity(paymentMethodType).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing payment method type.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Payment method type with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Payment method type with the supplied id does not exists."),
            @ApiResponse(code = 409, message = "Payment method type can not be deleted.", response = ErrorDetails.class)])
    public Response deletePaymentMethodType(@ApiParam(name = "id", value = "Payment Method Type id.", required = true)
                                            @PathParam("id") Integer id) {
        try {
            PaymentMethodTypeWS paymentMethodTypeWS = webServicesSession.getPaymentMethodType(id)
            if (null == paymentMethodTypeWS) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deletePaymentMethodType(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception exp) {
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
    }

    @GET
    @Path("/list")
    @ApiOperation(value = "Get all payment method types.", response = PaymentMethodTypeWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Payment method types found.", response = PaymentMethodTypeWS.class),
            @ApiResponse(code = 404, message = "No payment method is configured for the given company."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAllPaymentMethodTypes(){
        try {
            List<PaymentMethodTypeWS> PaymentMethodTypeWSs = webServicesSession.getAllPaymentMethodTypes();
            if (null == PaymentMethodTypeWSs || PaymentMethodTypeWSs.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(PaymentMethodTypeWSs).build();

        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}