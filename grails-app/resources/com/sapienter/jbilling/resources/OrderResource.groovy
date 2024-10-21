package com.sapienter.jbilling.resources

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.order.OrderChangeWS
import com.sapienter.jbilling.server.order.OrderResourceHelperService;
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.order.SwapMethod
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.SwapAssetWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import org.json.JSONObject

import java.text.SimpleDateFormat

import static javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path('/api/orders')
@Api(value = "/api/orders", description = "Orders")
@Produces(MediaType.APPLICATION_JSON)
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class OrderResource {

    IWebServicesSessionBean webServicesSession;
    OrderResourceHelperService orderResourceHelperService;

    @POST
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Create new order.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Order successfully created.", response = OrderWS.class),
            @ApiResponse(code = 400, message = "Invalid order data.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "Failure while creating the order.")
    ])
    Response createOrder(
            @ApiParam(value = "JSON representation of the order that should be created.", required = true)
                    OrderInfo orderInfo,
            @Context UriInfo uriInfo
    ) {
        try {
            Integer orderId = webServicesSession.createOrder(orderInfo.order, orderInfo.orderChanges);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(orderId)).build())
                    .entity(webServicesSession.getOrder(orderId)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }

    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get order by ID.", response = OrderWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order successfully returned.", response = OrderWS.class),
            @ApiResponse(code = 404, message = "Order not found."),
            @ApiResponse(code = 500, message = "Failure while getting the order.")
    ])
    Response getOrder(
            @ApiParam(name = "id",
                    value = "The ID of the order that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer orderId) {
        OrderWS order;
        try {
            order = webServicesSession.getOrder(orderId);
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
        return (null != order) ?
                Response.ok().entity(order).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update order.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order successfully updated.", response = OrderWS.class),
            @ApiResponse(code = 400, message = "Invalid order data.", response = ErrorDetails.class),
            @ApiResponse(code = 404, message = "Order not found."),
            @ApiResponse(code = 500, message = "Failure while updating the order.")
    ])
    Response updateOrder(
            @ApiParam(name = "id", value = "The id of the order that should be updated") @PathParam("id") Integer id,
            @ApiParam(value = "JSON represenation of the updated order.", required = true)
                    OrderInfo orderInfo
    ) {
        try {
            if (null == webServicesSession.getOrder(id)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            orderInfo.order.setId(id);
            webServicesSession.updateOrder(orderInfo.order, orderInfo.orderChanges);
            return Response.ok().entity(webServicesSession.getOrder(id)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }

    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete the order with the specified ID.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Order successfully deleted."),
            @ApiResponse(code = 404, message = "Order with the specified ID not found."),
            @ApiResponse(code = 500, message = "Failure while deleting the order.")
    ])
    Response deleteOrder(
            @ApiParam(name = "id",
                    value = "The ID of the order that needs to be deleted.",
                    required = true)
            @PathParam("id") Integer orderId
    ) {
        try {
            if (null == webServicesSession.getOrder(orderId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            webServicesSession.deleteOrder(orderId);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/last/{userId}")
    @ApiOperation(value = "Get latest orders for the given user.", response = OrderWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Latest order successfully returned.", response = OrderWS.class),
            @ApiResponse(code = 400, message = "Trying to get the latest orders with invalid parameters."),
            @ApiResponse(code = 404, message = "Latest order not found."),
            @ApiResponse(code = 500, message = "Failure while getting the last order.")
    ])
    Response getLatestOrder(
            @ApiParam(name = "userId", value = "The ID of the user whose last order needs to be fetched.", required = true)
            @PathParam("userId")
                    Integer userId,
            @ApiParam(name = "number", value = "The number of the last orders to be returned")
            @QueryParam("number")
                    Integer number
    ) {
        try {
            if (null == number) {
                OrderWS order = webServicesSession.getLatestOrder(userId);
                return (null != order) ?
                        Response.ok().entity(order).build() :
                        Response.status(Response.Status.NOT_FOUND).build();
            } else {
                List<OrderWS> orders = new ArrayList<>();
                for (Integer orderId : webServicesSession.getLastOrders(userId, number)) {
                    orders.add(webServicesSession.getOrder(orderId));
                }
                return (orders.size() > 0) ?
                        Response.ok().entity(orders).build() :
                        Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/page/{userId}")
    @ApiOperation(value = "Get a page of orders for the given user with specified limit and offset.", response = OrderWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Orders page successfully returned.", response = OrderWS.class),
            @ApiResponse(code = 400, message = "Trying to get user orders with invalid parameters."),
            @ApiResponse(code = 404, message = "Orders page can't be retrieved."),
            @ApiResponse(code = 500, message = "Failure while getting the orders.")
    ])
    Response getUserOrdersPage(
            @ApiParam(name = "userId", value = "The ID of the user whose orders need to be fetched.", required = true)
                    @PathParam("userId") Integer userId,
            @ApiParam(name = "limit", value = "Limit of the orders page.") @QueryParam("limit") Integer limit,
            @ApiParam(name = "offset", value = "Offset of the orders page.") @QueryParam("offset") Integer offset
    ) {
        OrderWS[] orders;
        try {
            orders = webServicesSession.getUserOrdersPage(userId, limit, offset);
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
        return (null != orders && orders.length > 0) ?
                Response.ok().entity(orders).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/calculateSwapPlanChanges")
    @ApiOperation(value = "Calculate orderChangeWs from calculateSwapPlanChanges ")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order Changes successfully returned.", response = OrderChangeWS[].class),
            @ApiResponse(code = 404, message = "Order Changes not found."),
            @ApiResponse(code = 500, message = "Failure while getting the order changes.")
    ])
    Response calculateSwapPlanChanges(
           @ApiParam(name = "existingPlanItemId", value = "Existing plan item id", required = true)
            @QueryParam("existingPlanItemId") Integer existingPlanItemId,
            @ApiParam(name = "swapPlanItemId", value = "Swap plan item id", required = true)
            @QueryParam("swapPlanItemId") Integer swapPlanItemId,
            @ApiParam(name = "effectiveDate", value = "Effective date in format yyyy-mm-dd", required = true)
            @QueryParam("effectiveDate") String effectiveDate,
            @ApiParam(name = "swapMethod", value = "Swap Method", required = true)
            @QueryParam("swapMethod") SwapMethod method,
           @ApiParam(name = "order", value = "Order", required = true) OrderWS orderWS,
           @Context
                    UriInfo uriInfo){
        try {
            SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
            def changes = webServicesSession.calculateSwapPlanChanges(orderWS, existingPlanItemId, swapPlanItemId,
                    method, formatter.parse(effectiveDate));
            if(changes) {
                return Response.ok().entity(changes).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build()
            }
        } catch(Exception ex) {
            return RestErrorHandler.mapErrorToHttpResponse(ex)
        }
    }

    @PUT
    @Path("/swapPlan")
    @ApiOperation(value = "Swap Plan for provided OrderId.", response = boolean.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Swap Plan successfully returned.", response = boolean.class),
        @ApiResponse(code = 404, message = "Order not found."),
        @ApiResponse(code = 500, message = "Failure during Swap Plan.")
    ])
    Response swapPlan(
            @ApiParam(name = "orderId", value = "The ID of the order that needs to be updated.", required = true)
            @QueryParam("orderId") Integer orderId,
            @ApiParam(name = "existingPlanCode", value = "Existing plan code", required = true)
            @QueryParam("existingPlanCode") String existingPlanCode,
            @ApiParam(name = "swapPlanCode", value = "Swap plan code", required = true)
            @QueryParam("swapPlanCode") String swapPlanCode,
            @ApiParam(name = "swapMethod", value = "Swap Method", required = true)
            @QueryParam("swapMethod") SwapMethod swapMethod) {
        try {
            def returnValue = webServicesSession.swapPlan(orderId, existingPlanCode, swapPlanCode, swapMethod);
            return Response.ok().entity(returnValue).build();
        } catch(Exception ex) {
            return RestErrorHandler.mapErrorToHttpResponse(ex)
        }
    }

    @GET
    @Path("/rateorder")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "rate order.")
    @ApiResponses(value = [
        @ApiResponse(code = 201, message = "Order Rated successfully.", response = OrderWS.class),
        @ApiResponse(code = 400, message = "Invalid order data.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "Failure while rating the order.")
    ])
    Response rateOrder(
            @ApiParam(value = "JSON representation of the order that should be rated.", required = true)
            OrderInfo orderInfo) {
        try {
            return Response.ok()
                    .entity(webServicesSession.rateOrder(orderInfo.order, orderInfo.orderChanges))
                    .build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/createorderandinvoice")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "create order and generate order's invoice and return invoice id.")
    @ApiResponses(value = [
        @ApiResponse(code = 201, message = "Order and Invoice created successfully.", response = Integer.class),
        @ApiResponse(code = 400, message = "Invalid order data.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "Failure while creating the order and invoice.")
    ])
    Response createOrderAndInvoice(
            @ApiParam(value = "JSON representation of the order that should be created.", required = true)
            OrderInfo orderInfo) {
        try {
            return Response.ok()
                    .entity(webServicesSession.createOrderAndInvoice(orderInfo.order, orderInfo.orderChanges))
                    .build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/user/{userId}")
    @ApiOperation(value = "Get all subscription orders for the given user.", response = OrderWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Subscription orders are successfully returned.", response = OrderWS.class),
            @ApiResponse(code = 400, message = "Trying to get user subscription orders with invalid parameters."),
            @ApiResponse(code = 404, message = "Subscription order not found."),
            @ApiResponse(code = 500, message = "Failure while getting the subscription orders.")
    ])
    Response getAllOrdersByUser(
            @ApiParam(name = "userId", value = "The ID of the user whose orders needs to be fetched.", required = true)
            @PathParam("userId")
                    Integer userId
    ) {
        OrderWS[] orders;
        try {
            orders = webServicesSession.getUsersAllSubscriptions(userId);
            orders = webServicesSession.getOrderMetaFieldMap(orders);
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
        return (null != orders && orders.length > 0) ?
                Response.ok().entity(orders).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/createorderwithassets")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Create new order with asset.")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Order successfully created.", response = OrderWS.class),
            @ApiResponse(code = 400, message = "Invalid order data.", response = ErrorDetails.class),
            @ApiResponse(code = 500, message = "Failure while creating the order.")
    ])
    Response createOrderWithAssets(
            @ApiParam(value = "JSON representation of the order that should be created.", required = true)
                    OrderInfo orderInfo,
            @Context UriInfo uriInfo
    ) {
        try {
            Integer orderId = webServicesSession.createOrderWithAssets(orderInfo.order, orderInfo.orderChanges, orderInfo.assets);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(orderId)).build())
                    .entity(webServicesSession.getOrder(orderId)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }

    }

    @POST
    @Path("/updateordermetafields")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update Order meta fields", response = OrderMetaFieldValueWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order MetaFields updated.", response = OrderMetaFieldValueWS.class),
        @ApiResponse(code = 404, message = "Order not found."),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateOrderMetaFields(
            @ApiParam(value = "Order MetaFields that needs to update.",
            required = true)
            OrderMetaFieldValueWS orderMetaFieldValueWS) {
        try {
            return Response.ok().entity(orderResourceHelperService.updateOrderMetaFields(orderMetaFieldValueWS)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }
            
    @POST
    @Path("/swapassets/{orderId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Swap Assets.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order successfully updated.", response = OrderWS.class),
        @ApiResponse(code = 400, message = "Invalid Asset Swap Request Passed.", response = ErrorDetails.class),
        @ApiResponse(code = 404, message = "Order not found."),
        @ApiResponse(code = 500, message = "Failure while swapping assets on the order.")
    ])
    Response swapAssets(
            @ApiParam(name = "orderId", value = "The id of the order that should be updated") @PathParam("orderId") Integer orderId,
            @ApiParam(value = "JSON represenation of SwapAsset Request.", required = true)
            SwapAssetWS[] swapAssetRequests
    ) {
        try {
            webServicesSession.swapAssets(orderId, swapAssetRequests)
            return Response.ok().entity(webServicesSession.getOrder(orderId)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }

    }

    @GET
    @Path("/ordermetafields/{orderId}")
    @ApiOperation(value = "get Order level metafields.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order level metafields returned.", response = OrderMetaFieldValueWS.class),
        @ApiResponse(code = 400, message = "Invalid parameters Passed.", response = ErrorDetails.class),
        @ApiResponse(code = 404, message = "Order not found."),
        @ApiResponse(code = 500, message = "Failure fetching order level meta fields.")
    ])
    Response getOrderMetaFieldValueWS(
            @ApiParam(name = "orderId", value = "The id of the order") 
            @PathParam("orderId") Integer orderId) {
        try {
            return Response.ok().entity(webServicesSession.getOrderMetaFieldValueWS(orderId)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }

    }

    @POST
    @Path("/cancelserviceorder")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Cancel order.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Order successfully updated.", response = OrderWS.class),
        @ApiResponse(code = 400, message = "Invalid order data.", response = ErrorDetails.class),
        @ApiResponse(code = 404, message = "Order not found."),
        @ApiResponse(code = 500, message = "Failure while updating the order.", response = ErrorDetails.class)
    ])
    Response cancelServiceOrder(
            @ApiParam(value="JSON representation of cancel order", required= true)
            CancelOrderInfo cancelOrderInfo) {
        try {
            webServicesSession.cancelServiceOrder(cancelOrderInfo);
            return Response.ok().entity(JSONObject.quote("Order cancelled successfully.")).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}