package com.sapienter.jbilling.resources

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.List;

import com.sapienter.jbilling.common.ErrorDetails;


import com.sapienter.jbilling.resources.CustomerMetaFieldValueWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.payment.PaymentInformationRestWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.ContactInformationWS;
import com.sapienter.jbilling.server.user.CreateUserRequestWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

import com.sapienter.jbilling.server.user.CustomerRestWS;

import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue;
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

import org.apache.http.HttpStatus
import org.eclipse.core.resources.IFolder;
import org.json.JSONObject

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.user.UserProfileWS
import com.sapienter.jbilling.server.user.UserResourceHelperService
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import com.sapienter.jbilling.server.payment.SecurePaymentWS;
import com.sapienter.jbilling.server.item.AssetWS
import java.util.function.Supplier;
import com.sapienter.jbilling.server.user.UserDTOEx;

@Path("/api/users")
@Api(value="/api/users", description = "Users.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class UserResource {

    IWebServicesSessionBean webServicesSession
    UserResourceHelperService userResourceHelperService

    @GET
    @Path("/{userId}")
    @ApiOperation(value = "Get user by id.", response = UserWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User found.", response = UserWS.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getUserId(
            @ApiParam(name = "userId",
            value = "The id of the user that needs to be fetched.",
            required = true)
            @PathParam("userId") Integer userId) {
        try {
            UserWS user = webServicesSession.getUserWS(userId);
            return Response.ok().entity(user).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create user.")
    @ApiResponses(value = [
        @ApiResponse(code = 201, message = "User created.", response = UserWS.class),
        @ApiResponse(code = 400, message = "Invalid user supplied."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response createUser(
            @ApiParam(value = "Created user object.", required = true)
            UserWS user,
            @Context
            UriInfo uriInfo){
        try {
            Integer userId = webServicesSession.createUser(user)
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(userId)).build())
                    .entity(webServicesSession.getUserWS(userId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
			
	@POST
	@Path("/createUser")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "MEDAAS - Create user.")
	@ApiResponses(value = [
	@ApiResponse(code = 201, message = "User created.", response = Integer.class),
	@ApiResponse(code = 400, message = "Invalid user supplied."),
	@ApiResponse(code = 500, message = "The call resulted with internal error.")])
	Response create(
		@ApiParam(value = "Created user object.", required = true)
		CreateUserRequestWS createUserRequest){
		try {
			def userResponse = userResourceHelperService.createUser(createUserRequest)
			return Response.ok()
				.entity(userResponse)
					.build();
		} catch (Exception e){
			return RestErrorHandler.mapErrorToHttpResponse(e);
		}
	}
			
	@PUT
	@Path("/{userId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Updates existing user.", response = UserWS.class)
	@ApiResponses(value = [
	@ApiResponse(code = 200, message = "User created."),
	@ApiResponse(code = 400, message = "Invalid user supplied."),
	@ApiResponse(code = 404, message = "User not found."),
	@ApiResponse(code = 500, message = "The call resulted with internal error.")])
	Response updateUser(
		@ApiParam(name = "userId", value = "User id that needs to be updated.", required = true)
			@PathParam("userId")
			Integer userId,
			@ApiParam(value = "User object containing update data.", required = true)
			UserWS user){
		
			try {
				webServicesSession.getUserWS(userId)
				user.setId(userId);
				webServicesSession.updateUser(user);
				return Response.ok().entity(webServicesSession.getUserWS(userId)).build();
			} catch (Exception e){
				return RestErrorHandler.mapErrorToHttpResponse(e);
			}
	}

    @GET
    @Path("/customerattributes/{userId}")
    @ApiOperation(value = "Get customer attributes by user id.", response = CustomerRestWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User found.", response = CustomerRestWS.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getCustomerAttributes(
            @ApiParam(name = "userId",
            value = "The id of the customer that needs to be fetched.",
            required = true)
            @PathParam("userId") Integer userId) {
        try {
            CustomerRestWS customerRestWS = userResourceHelperService.getCustomerAttributes(userId);
            return Response.ok().entity(customerRestWS).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/customerattributes/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update customer attributes.", response = CustomerRestWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Customer attributes updated."),
        @ApiResponse(code = 400, message = "Invalid request supplied."),
        @ApiResponse(code = 404, message = "Customer not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateCustomerAttributes(
            @ApiParam(name = "userId", value = "User id that needs to be updated.", required = true)
            @PathParam("userId")
            Integer userId,
            @ApiParam(value = "Customer attributes object containing data to be updated.", required = true)
            CustomerRestWS customerRestWS){
        try {
            UserWS userToBeUpdated = userResourceHelperService.updateCustomerAttributes(userId, customerRestWS);
            if (userToBeUpdated != null) {
                webServicesSession.updateUser(userToBeUpdated);
                return Response.ok().entity(userResourceHelperService.getCustomerAttributes(userId)).build();
            }
            return Response.ok().entity(JSONObject.quote("Customer attributes have not been modified since request has no changes")).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{userId}")
    @ApiOperation(value = "Deletes existing user.")
    @ApiResponses(value = [
        @ApiResponse(code = 204, message = "User with the supplied id deleted."),
        @ApiResponse(code = 404, message = "User with the supplied id does not exists."),
        @ApiResponse(code = 409, message = "Deletion of the specified user is not allowed."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response deleteUser(
            @ApiParam(name = "userId",
            value = "The id of the user that needs to be deleted.", required = true)
            @PathParam("userId") Integer userId){

        try {
            webServicesSession.getUserWS(userId)
            webServicesSession.deleteUser(userId);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{userId}/invoices")
    @ApiOperation(value = "Get all invoices by the user id.", response = InvoiceWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Returning all invoices for the provided user id.", response = InvoiceWS.class),
        @ApiResponse(code = 400, message = "Invalid parameters used to fetch invoices."),
        @ApiResponse(code = 404, message = "User with the provided id not found."),
        @ApiResponse(code = 500, message = "Fetching the invoices by user id failed.")
    ])
    Response getAllInvoicesForUser(
            @ApiParam(name = "userId", value = "User id.", required = true) @PathParam("userId") Integer userId,
            @ApiParam(value = "limit", required = false) @QueryParam("limit")  Integer limit,
            @ApiParam(value = "offset", required = false) @QueryParam("offset") Integer offset
    ) {

        InvoiceWS[] invoices;
        try {
            if (null == webServicesSession.getUserWS(userId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            // If 'limit' or 'offset' is used as a query parameter, use the API method with pagination.
            if (null != limit || null != offset) {
                invoices = webServicesSession.getUserInvoicesPage(userId, limit, offset);
            } else {
                invoices = webServicesSession.getAllInvoicesForUser(userId);
            }
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception);
        }
        return Response.ok().entity(invoices).build();
    }

    @GET
    @Path("/{userId}/invoices/last")
    @ApiOperation(value = "Get last invoices by the user id.", response = InvoiceWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Returning the last invoices for the provided user id.", response = InvoiceWS.class),
        @ApiResponse(code = 400, message = "Invalid parameters used to fetch invoices."),
        @ApiResponse(code = 404, message = "User with the provided id not found."),
        @ApiResponse(code = 500, message = "Fetching the invoices by user id failed.")
    ])
    Response getLastInvoicesForUser(
            @ApiParam(name = "userId", value = "User id.", required = true) @PathParam("userId") Integer userId,
            @ApiParam(value = "number", required = false) @QueryParam("number")  Integer number
    ) {

        try {
            if (null == webServicesSession.getUserWS(userId)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            // If 'number' is used as a query parameter, use the API method getLastInvoices(),
            // else use the method getLatestInvoice().
            if (null != number) {
                List<InvoiceWS> invoices = new ArrayList<>();
                for (Integer invoiceId : webServicesSession.getLastInvoices(userId, number)) {
                    invoices.add(webServicesSession.getInvoiceWS(invoiceId));
                }
                return Response.ok().entity(invoices).build();
            } else {
                return Response.ok().entity(webServicesSession.getLatestInvoice(userId)).build();
            }
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception);
        }
    }

    @GET
    @Path("/{userId}/payments/last")
    @ApiOperation(value = "Get last user payments.", response = PaymentWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User payment found.", response = PaymentWS.class),
        @ApiResponse(code = 400, message = "Invalid user supplied."),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response getLastUserPayment(
            @ApiParam(value = "User Id", required = true) @PathParam("userId") Integer userId,
            @DefaultValue("10000") @QueryParam("limit") @ApiParam(name="limit", required = false) Integer limit,
            @DefaultValue("0") @QueryParam("offset") @ApiParam(name="offset", required = false) Integer offset) {
        try {
            return Response.ok(webServicesSession.findPaymentsForUser(userId, offset, limit)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{userId}/payments")
    @ApiOperation(value = "Get user payments page.", response = PaymentWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User payments page found.", response = PaymentWS.class),
        @ApiResponse(code = 400, message = "Invalid user supplied."),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    public Response getUserPaymentsPage(
            @ApiParam(value = "UserId", required = true) @PathParam("userId") Integer userId,
            @ApiParam(value = "Limit") @QueryParam("limit")  Integer limit,
            @ApiParam(value = "Offset") @QueryParam("offset") Integer offset) {

        try {
            return Response.ok().entity(webServicesSession.getLastPaymentsPage(userId, limit, offset)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{metaFieldName}/{metaFieldValue}")
    @ApiOperation(value = "Get user by Customer MetaField.", response = UserWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User found with provided metaField name and value.", response = UserWS.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response getUserByCustomerMetaField(
            @ApiParam(name = "metaFieldName",
            value = "The Customer level metafield name that needs to be fetched.",
            required = true)
            @PathParam("metaFieldName") String metaFieldName,
            @ApiParam(name = "metaFieldValue",
            value = "The metaField value that needs to be used to fetched customer.",
            required = true)
            @PathParam("metaFieldValue") String metaFieldValue) {
        try {
            return Response.ok().entity(webServicesSession.getUserByCustomerMetaField(metaFieldValue,metaFieldName)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/status/in/{statusId}/{inc}")
    @ApiOperation(value = "Retrieves an array of user ids in the required status.", response = Integer[].class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User ids found with provided status Id.", response = Integer[].class),
        @ApiResponse(code = 404, message = "User ids not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response getUsersByStatus(
            @ApiParam(name = "statusId",
            value = "Users need to fetch according to provided statusId value.",
            required = true)
            @PathParam("statusId") Integer statusId,
            @ApiParam(name = "inc",
            value = "Users need to include according to provided status.",
            required = true)
            @PathParam("inc") boolean inc) {
        try {
            return Response.ok().entity(webServicesSession.getUsersByStatus(statusId,inc)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/validatelogin/{username}/{password}")
    @ApiOperation(value = "validate user credential and retrun UserWS.", response = UserWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Returning user ws for given credential.", response = UserWS.class),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 404, message = "User with the provided credential not found."),
        @ApiResponse(code = 500, message = "validating credential failed.")
    ])
    Response validateLogin(
            @ApiParam(name = "username", value = "user name.", required = true) @PathParam("username") String userName,
            @ApiParam(name = "password", value = "password", required = true) @PathParam("password")  String password
    ) {
        try {
            return Response.ok().entity(webServicesSession.validateLogin(userName, password)).build();
        } catch (Exception exception) {
            return RestErrorHandler.mapErrorToHttpResponse(exception);
        }
    }

    @POST
    @Path("/resetpassword/{userId}")
    @ApiOperation(value = "Reset the password of existing user.", response = String.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "The password reset for the supplied user.", response = String.class),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response resetpassword(
            @ApiParam(name = "userId",
            value = "The id of the user that needs to reset the password.", required = true)
            @PathParam("userId") Integer userId) {
        try {
            webServicesSession.resetPassword(userId);
            return Response.ok().entity(JSONObject.quote("Reset password request sent successfully")).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/userprofile/{userId}")
    @ApiOperation(value = "Get user profile by id.", response = UserProfileWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User profile found.", response = UserProfileWS.class),
        @ApiResponse(code = 404, message = "User profile not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getUserProfile(
            @ApiParam(name = "userId",
            value = "The id of the user that needs to be fetched.",
            required = true)
            @PathParam("userId") Integer userId) {
        try {
            UserProfileWS userProfileWS = webServicesSession.getUserProfile(userId);
            return Response.ok().entity(userProfileWS).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/removeinstrument/{instrumentId}")
    @ApiOperation(value = "Deletes existing payment instrument.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "payment instrument deleted.", response = String.class),
        @ApiResponse(code = 404, message = "payment instrument  not found."),
        @ApiResponse(code = 409, message = "payment instrument can not be deleted.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response removePaymentInstrument(
            @ApiParam(value = "instrumentId", required = true) @PathParam("instrumentId") Integer instrumentId ) {
        try {
            Boolean result = webServicesSession.removePaymentInstrument(instrumentId)
            return Response.ok().entity(JSONObject.quote("Payment instrument removed successfully")).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/paymentinstruments/{userId}")
    @ApiOperation(value = "get all the payment instruments for User.", response = PaymentInformationRestWS[].class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Returning payment instrument.", response = PaymentInformationRestWS[].class),
        @ApiResponse(code = 404, message = "payment instrument  not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    public Response getPaymentInstruments(
            @ApiParam(name = "userId"
				, value = "The user id to fetch payment instruments."
				, required = true) @PathParam("userId") Integer userId) {
        try {
            PaymentInformationWS[] paymentInformationWSs = webServicesSession.getPaymentInstruments(userId)
            PaymentInformationRestWS[] paymentInformationRestWSs = UserBL.getPaymentInformationWSForRestWS(paymentInformationWSs)
            return Response.ok().entity(paymentInformationRestWSs).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/resetpasswordbyusername/{userName}")
    @ApiOperation(value = "Reset the password of existing user.", response = String.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "The password reset for the supplied user.", response = String.class),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response resetPasswordByUserName(
            @ApiParam(name = "userName", value = "The name of the user that needs to reset the password.", required = true)
            @PathParam("userName") String userName
    ) {
        try {
            webServicesSession.resetPasswordByUserName(userName);
            return Response.ok().entity(JSONObject.quote("Reset password request sent successfully")).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/updatecustomercontactinfo")
    @ApiOperation(value = "Update user's contact information", response = UserWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "User contact iformation updated.", response = UserWS.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateCustomerContactInfo(
            @ApiParam(value = "The contact info for the user that needs to update.",
            required = true)
            ContactInformationWS contactInformation) {
        try {
            return Response.ok().entity(userResourceHelperService.updateAITMetaField(contactInformation)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/getcustomermetafields/{userId}")
    @ApiOperation(value = "get user's meta fields", response = CustomerMetaFieldValueWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Returning all meta fields of customer.", response = CustomerMetaFieldValueWS.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    Response getCustomerMetaFields(
            @ApiParam(name = "userId"
				, value = "The user id to fetch the customer meta fields."
				, required = true) @PathParam("userId") Integer userId) {
        try {
            CustomerMetaFieldValueWS customerMetaFieldValue = webServicesSession.getCustomerMetaFields(userId)
            return Response.ok().entity(customerMetaFieldValue).build()
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

	@POST
    @Path("/addpaymentinstrument")
	@Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add payment instrument to the given user", response = SecurePaymentWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Payment instrument has been added.", response = SecurePaymentWS.class),
        @ApiResponse(code = 400, message = "Invalid parameters supplied."),
		@ApiResponse(code = 402, message = "Payment required."),
		@ApiResponse(code = 404, message = "User or payment method type not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response addPaymentInstrument(
            @ApiParam(value = "Payment instrument object containing user and payment instrument's meta-field data.", required = true)
            PaymentInformationRestWS paymentInformationRestWS) {
        try {
            //Constructing PaymentInformationWS from PaymentInformationRestWS
            PaymentInformationWS paymentInformation = populatePaymentInformationWSFromRestWS(paymentInformationRestWS);
            return RestErrorHandler.mapStatusToHttpResponse(webServicesSession.addPaymentInstrument(paymentInformation));
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    //Private helper methods
    private def populatePaymentInformationWSFromRestWS(PaymentInformationRestWS piRestWS) {
        PaymentInformationWS ws = new PaymentInformationWS();
        try {
            ws.setUserId(piRestWS.getUserId());
            ws.setProcessingOrder(piRestWS.getProcessingOrder());
            ws.setPaymentMethodId(piRestWS.getPaymentMethodId());
            ws.setPaymentMethodTypeId(piRestWS.getPaymentMethodTypeId());
            if(null != piRestWS.getCvv()){
                ws.setCvv(piRestWS.getCvv());
            }
            List<MetaFieldValueWS> paymentMetaFields = new ArrayList<>();
            piRestWS.getMetaFields().each
                { key, value-> paymentMetaFields.add(populateMetaFieldValueWS(key, value, piRestWS.getPaymentMethodTypeId())) }
            ws.setMetaFields(paymentMetaFields.toArray(new MetaFieldValueWS[paymentMetaFields.size()]));
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
        return ws;
    }

    private def populateMetaFieldValueWS(String metaFieldName, Object metaFieldValue, Integer paymentMethodTypeId) {
        MetaFieldValueWS mfValue = new MetaFieldValueWS();
        mfValue.setFieldName(metaFieldName);
        mfValue.setValue(metaFieldValue);
        MetaField metaField = MetaFieldExternalHelper.findPaymentMethodMetaField(metaFieldName, paymentMethodTypeId);
        if (metaField != null) {
            if (DataType.BOOLEAN.equals(metaField.getDataType())) {
                mfValue.setValue(new Boolean(metaFieldValue.toString()));
            } else if (DataType.DECIMAL.equals(metaField.getDataType())) {
                mfValue.setValue(new BigDecimal(metaFieldValue.toString()));
            } else if (DataType.INTEGER.equals(metaField.getDataType())) {
                mfValue.setValue(Integer.parseInt(metaFieldValue.toString()));
            } else if (DataType.DATE.equals(metaField.getDataType())) {
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                mfValue.setValue(formatter.parse(metaFieldValue.toString()));
            }
        }
        return mfValue;
    }

    @POST
    @Path("/updatecustomermetafields")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update customer meta fields", response = CustomerMetaFieldValueWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Customer MetaFields updated.", response = CustomerMetaFieldValueWS.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 400, message = "Invalid parameters passed."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateCustomerMetaFields(
            @ApiParam(value = "customer MetaFields that needs to update.",
            required = true)
            CustomerMetaFieldValueWS customerMetaFieldValueWS) {
        try {
            return Response.ok().entity(userResourceHelperService.updateCustomerMetaFields(customerMetaFieldValueWS)).build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Path("/{userId}/usagepools")
    @ApiOperation(value = "get user's usage pools", response = CustomerUsagePoolWS[].class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Returning all active user usage pool.", response = CustomerUsagePoolWS[].class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    Response getCustomerUsagePoolsByUserId(@ApiParam(name = "userId",
            value = "The id of the user to fetch customer usage pools.", required = true)
            @PathParam("userId") Integer userId) {
        try {
            return Response.ok()
                    .entity(userResourceHelperService.getCustomerUsagePoolsByUserId(userId))
                    .build()
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @GET
    @Path("/getinvoicedesign/{userId}")
    @ApiOperation(value = "get user's invoice design", response = String.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Returning user's invoice design.", response = String.class),
        @ApiResponse(code = 404, message = "User not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")
    ])
    Response getCustomerInvoiceDesign(@ApiParam(name = "userId",
            value = "The id of the user to fetch customer invoice design.", required = true)
            @PathParam("userId") Integer userId) {
        try {
            userResourceHelperService.validate(userId);
            String invoiceDesign = webServicesSession.getCustomerInvoiceDesign(userId);
            return Response.ok()
                    .entity(JSONObject.quote(invoiceDesign))
                    .build();
        } catch (Exception e) {
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }

    @PUT
    @Path("/invoicedesign/{userId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Update customer invoice design.", response = String.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Customer invoice design updated."),
        @ApiResponse(code = 400, message = "Invalid request supplied."),
        @ApiResponse(code = 404, message = "Customer not found."),
        @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updateCustomerInvoiceDesign(
            @ApiParam(name = "userId", value = "User id that needs to be updated.", required = true)
            @PathParam("userId")
            Integer userId,
            @ApiParam(value = "Customer invoice design to be updated.")
            String invoiceDesign) {
        try {
            userResourceHelperService.validate(userId);
            webServicesSession.updateCustomerInvoiceDesign(userId, invoiceDesign);
            return Response.ok().entity(JSONObject.quote("Customer invoice design has been updated successfully")).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/getUserRating/{crmId}")
    @ApiOperation(value = "Get rating by CRM id.", response = String.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "rating created.", response = String.class),
            @ApiResponse(code = 404, message = "Rating not created."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getUserRating(
            @ApiParam(name = "crmId",
                    value = "The CRM id of the user that needs to be fetched.",
                    required = true)
            @PathParam("crmId") String crmId) {
        try {
            def rating = webServicesSession.getUserRating(crmId)
            return Response.ok().entity(JSONObject.quote(rating)).build()
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("userbyidentifier/{identifier}")
    @ApiOperation(value = "Get user by identifier.", response = UserWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "User found.", response = UserWS.class),
            @ApiResponse(code = 404, message = "User not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getUserByIdentifier(
            @ApiParam(name = "identifier",
                    value = "The identifier of the asset that needs to be fetched.",
                    required = true)
            @PathParam("identifier") String identifier) {
        try {
            AssetWS assetWS =Optional.ofNullable(webServicesSession.getAssetByIdentifier(identifier))
                    .orElseThrow(new RuntimeException("Asset not found for identifier: " + identifier) as Supplier<? extends Throwable>)

            UserWS user = UserBL.getWS(new UserDTOEx(new UserBL().getUserByAssetId(assetWS.getId())))
            return Response.ok().entity(user).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}
