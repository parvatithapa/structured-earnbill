package com.sapienter.jbilling.resources

import java.lang.invoke.MethodHandles

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sapienter.jbilling.server.payment.PaymentWebHookService
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses


@Path("/api/paymentWebHook")
@Api(value = "/api/paymentWebHook", description = "PaymentWebHook.")
class PaymentWebHookResource {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

	PaymentWebHookService paymentWebHookService

	@POST
	@Path("/{entityId}/{gatewayName}")
	@ApiOperation(value = "PaymentWebHook Event handler")
	@ApiResponses(value = [
			@ApiResponse(code = 200, message = "PaymentWebHook Event Processed"),
			@ApiResponse(code = 500, message = "error occured while processing PaymentWebHook Event.")])
	Response handleWebHookEvent(
			@ApiParam(value = "entityId", required = true) @PathParam("entityId") Integer entityId,
			@ApiParam(value = "gatewayName", required = true) @PathParam("gatewayName") String gatewayName,
			@Context HttpServletRequest request) {
		try {
			return paymentWebHookService.handleWebHookEvent(gatewayName, entityId, request)
		} catch (Exception exception) {
			logger.error("handleWebHookEvent failed for entity={}, gatewayName={}", entityId, gatewayName, exception)
			def errorBody = Collections.singletonMap("error", ExceptionUtils.getStackTrace(exception))
			return Response.status(500)
					.entity(errorBody.toString())
					.build()
		}

	}
}
