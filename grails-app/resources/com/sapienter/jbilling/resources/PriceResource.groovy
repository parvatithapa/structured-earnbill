package com.sapienter.jbilling.resources

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.item.PriceRequestWS
import com.sapienter.jbilling.server.item.PriceResponseWS
import com.sapienter.jbilling.server.item.PriceService
import com.sapienter.jbilling.server.item.UserPlanSubcriptionRequestWS
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

@Path("/api/price")
@Api(value="/api/price", description = "price service")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class PriceResource {


	PriceService priceService

	@POST
	@Path("/resolvePrice")
	@ApiOperation(value = "resolving price for user.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "Price resolved.", response = PriceResponseWS.class),
		@ApiResponse(code = 400, message = "Invalid price request.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while resolving price.", response = ErrorDetails.class)
	])
	public Response resolvePrice(@ApiParam(value = "JSON representation of price request.", required = true)
			PriceRequestWS priceRequest) {
		try {
			return Response.ok()
					.entity(priceService.resolvePrice(priceRequest))
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

	@POST
	@Path("/subscribe")
	@ApiOperation(value = "user subscribe to plan.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "User Subscribed to plan.", response = UserPlanSubcriptionRequestWS.class),
		@ApiResponse(code = 400, message = "Invalid Subscription request.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while subscription.", response = ErrorDetails.class)
	])
	public Response subscribe(@ApiParam(value = "JSON representation of User PlanSubcription Request.", required = true)
			UserPlanSubcriptionRequestWS userPlanSubcriptionRequest) {
		try {
			priceService.subscribe(userPlanSubcriptionRequest)
			return Response.ok().build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}
}