package com.sapienter.jbilling.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.twofactorauth.TwoFactorAuthenticationHelperService
import com.sapienter.jbilling.twofactorauth.TwoFactorRequestWS
import com.sapienter.jbilling.twofactorauth.TwoFactorResponseWS
import com.sapienter.jbilling.twofactorauth.TwoFactorVerificationRequestWS
import com.sapienter.jbilling.twofactorauth.TwoFactorVerificationResponseWS
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

@Path('/api/2FA')
@Api(value = "/api/2FA", description = "twofactor authentication")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
public class TwoFactorAuthenticationResource {

	TwoFactorAuthenticationHelperService twoFactorAuthenticationHelperService

	@Path("/generateOtp")
	@POST
	@Consumes(APPLICATION_JSON)
	@ApiOperation(value = "2FA generate otp.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "2FA opt generated.", response = TwoFactorResponseWS.class),
		@ApiResponse(code = 400, message = "Invalid 2FA request supplied.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while generating 2FA request."),
	])
	public Response generateOTP(@ApiParam(value = "TwoFactorRequest payload.", required = true) TwoFactorRequestWS twoFactorRequest) {
		try {
			TwoFactorResponseWS twoFactorResponse = twoFactorAuthenticationHelperService
					.generateOtp(UUID.randomUUID().toString(), twoFactorRequest)
			return Response.ok().entity(twoFactorResponse).build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

	@Path("/verifyOtp")
	@POST
	@Consumes(APPLICATION_JSON)
	@ApiOperation(value = "2FA verify otp.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "2FA opt verification response.", response = TwoFactorVerificationResponseWS.class),
		@ApiResponse(code = 400, message = "Invalid 2FA verification request supplied.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while verifying otp."),
	])
	public Response generateOTP(@ApiParam(value = "TwoFactorVerificationRequestWS payload.", required = true)
			TwoFactorVerificationRequestWS twoFactorVerificationRequest) {
		try {
			TwoFactorVerificationResponseWS verificationResponse = twoFactorAuthenticationHelperService
					.verifyOtp(twoFactorVerificationRequest)
			return Response.ok().entity(verificationResponse).build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}
}
