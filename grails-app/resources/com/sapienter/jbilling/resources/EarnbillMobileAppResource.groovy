package com.sapienter.jbilling.resources


import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.*
import jbilling.EarnbillMobileService

import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/api/mobile")
@Api(value="/api/mobile", description = "Earnbill Mobile App.")
class EarnbillMobileAppResource {

    EarnbillMobileService earnbillMobileService;
    IWebServicesSessionBean webServicesSession

    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get user by id.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "User found.", response = Map),
            @ApiResponse(code = 404, message = "User not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getUserIdForMobile(
            @ApiParam(name = "userId",
                    value = "The id of the user that needs to be fetched.",
                    required = true)
            @PathParam("userId") Integer userId) {
        try {
            return Response.ok().entity(earnbillMobileService.getUserDetails(userId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{userId}/invoices")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all invoices by the user id.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning all invoices for the provided user id.", response = Map),
            @ApiResponse(code = 400, message = "Invalid parameters used to fetch invoices."),
            @ApiResponse(code = 404, message = "User with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the invoices by user id failed.")
    ])
    Response getAllInvoicesForMobile(
            @ApiParam(name = "userId", value = "User id.", required = true) @PathParam("userId") Integer userId)
    {
        try {
            if (null == webServicesSession.getUserWS(userId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(earnbillMobileService.getInvoicesByUserId(userId)).build();
        } catch (Exception error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @GET
    @Path("/{userId}/invoices/unpaid")
    @ApiOperation(value = "Get unpaid invoices by the user id.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Returning the unpaid invoices for the provided user id.", response = Map),
            @ApiResponse(code = 400, message = "Invalid parameters used to fetch unpaid invoices."),
            @ApiResponse(code = 404, message = "User with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the unpaid invoices by user id failed.")
    ])
    Response getUnpaidInvoicesForMobile(
            @ApiParam(name = "userId", value = "User id.", required = true) @PathParam("userId") Integer userId
    ) {
        try {
            return Response.ok().entity(earnbillMobileService.getUnpaidInvoices(userId)).build();
        } catch (Exception error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @GET
    @Path("/orders/{userId}")
    @ApiOperation(value = "Get all subscription orders for the given user.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Subscription orders are successfully returned.", response = Map),
            @ApiResponse(code = 400, message = "Trying to get user subscription orders with invalid parameters."),
            @ApiResponse(code = 404, message = "Subscription order not found."),
            @ApiResponse(code = 500, message = "Failure while getting the subscription orders.")
    ])
    Response getAllOrdersByUserForMobile(
            @ApiParam(name = "userId", value = "The ID of the user whose orders needs to be fetched.", required = true)
            @PathParam("userId")
                    Integer userId
    ) {
        try {
            if (null == webServicesSession.getUserWS(userId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
           return Response.ok().entity(earnbillMobileService.getOrdersByUserId(userId)).build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/invoice/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get an invoice by the id.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Invoice with the provided id found.", response = Map),
            @ApiResponse(code = 404, message = "Invoice with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the invoice failed.")
    ])
    Response getInvoiceByIdForMobile(
            @ApiParam(name = "id", value = "Invoice id.", required = true) @PathParam("id") Integer id) {
         def invoice
        try {
            invoice = earnbillMobileService.getInvoiceById(id)
        } catch (Exception error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
        return invoice ?
                 Response.ok().entity(earnbillMobileService.getInvoiceById(id)).build() :
                Response.status(Response.Status.NOT_FOUND).build()
    }

    @GET
    @Path("/order/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get order by ID.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order successfully returned.", response = Map),
            @ApiResponse(code = 404, message = "Order not found."),
            @ApiResponse(code = 500, message = "Failure while getting the order.")
    ])
    Response getOrder(
            @ApiParam(name = "id",
                    value = "The ID of the order that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer orderId) {
        def order;
        try {
            order = earnbillMobileService.getOrderById(orderId)
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
        return order?
                Response.ok().entity(earnbillMobileService.getOrderById(orderId)).build():
                Response.status(Response.Status.NOT_FOUND).build()
    }

    @GET
    @Path("/payment/{id}")
    @ApiOperation(value = "Get payment by id.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Payment found.", response = Map),
            @ApiResponse(code = 404, message = "Payment not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response getPaymentById(
            @ApiParam(name = "id",value = "The id of the payment that needs to be fetched.",required = true)
            @PathParam("id") Integer id) {
        try {
            Map payment = earnbillMobileService.getPaymentById(id);
            if (null == payment){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().entity(payment).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/users/{userId}/payments/last")
    @ApiOperation(value = "Get last user payments.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "User payment found.", response = Map),
            @ApiResponse(code = 400, message = "Invalid user supplied."),
            @ApiResponse(code = 404, message = "User not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response getLastUserPaymentForMobile(
            @ApiParam(value = "User Id", required = true) @PathParam("userId") Integer userId) {
        try {
            if (null == webServicesSession.getUserWS(userId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(earnbillMobileService.getPaymentsByUserId(userId)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/currency/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get an currency by the id.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "currency with the provided id found.", response = Map),
            @ApiResponse(code = 404, message = "currency with the provided id not found."),
            @ApiResponse(code = 500, message = "Fetching the currency failed.")
    ])
    Response getCurrencyByIdForMobile(
            @ApiParam(name = "id", value = "currency id.", required = true) @PathParam("id") Integer id) {
        try {
            return Response.ok(earnbillMobileService.getCurrencyById(id)).build();
        } catch (Exception error) {
            return RestErrorHandler.mapErrorToHttpResponse(error);
        }
    }

    @GET
    @Path("/company/{entityId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get company by entityId.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Company found.", response = Map),
            @ApiResponse(code = 404, message = "Company not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getCompanyInfoForMobile(
            @ApiParam(name = "entityId",
                    value = "The entityId of the company that needs to be fetched.",
                    required = true)
            @PathParam("entityId") Integer entityId) {
        try {
            return Response.ok().entity(earnbillMobileService.getCompanyInfo(entityId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/products")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all items.", response = Map)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Item found or empty.", response = Map),
            @ApiResponse(code = 500, message = "Internal error occurred.")])
    Response getAllItems(){
        try {
            return Response.ok().entity(earnbillMobileService.getAllProducts()).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/generateqr")
    @ApiOperation(value = "Generate QR code.")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Qr Code Generated.", response = Map.class),
            @ApiResponse(code = 400, message = "Invalid request data.", response = Map.class),
            @ApiResponse(code = 500, message = "Failure while generating QR.")
    ])
    Response generateQR(
            @ApiParam(value = "JSON representation of QR generation request.",
                    required = true)
                    Map map) {
        try {
            return Response.ok()
                    .entity(earnbillMobileService.createPaymentUrl(map))
                    .build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Path("checkpaymentstatus/{id}")
    @ApiOperation(value = "Check payment status by merchant transaction id.", response = Map.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "merchant transaction found.", response = Map.class),
            @ApiResponse(code = 404, message = "merchant transaction not found."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response checkPaymentStatus(
            @ApiParam(name = "id",
                    value = "The id of the merchant transaction that needs to be checked payment status.",
                    required = true)
            @PathParam("id") Integer id) {

        try{
            return Response.ok().entity(earnbillMobileService.checkPaymentStatus(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

}
