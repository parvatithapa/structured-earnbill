package com.sapienter.jbilling.resources

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

import com.sapienter.jbilling.auth.AuthRequestWS
import com.sapienter.jbilling.auth.JwtAuthenticationService
import com.sapienter.jbilling.auth.JwtDecodedTokenInfoWS
import com.sapienter.jbilling.auth.JwtTokenResponseWS
import com.sapienter.jbilling.auth.LoggedInUserInfoWS
import com.sapienter.jbilling.auth.TokenVerificationRequestWS
import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

@Path("/api/authentication")
@Api(value="/api/authentication", description = "Jwt Authentication.")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class JwtAuthenticationResource {

	JwtAuthenticationService jwtAuthenticationService

	@POST
	@Path("/authenticate")
	@ApiOperation(value = "Autheticating user.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "User Authenticated.", response = JwtTokenResponseWS.class),
		@ApiResponse(code = 400, message = "Invalid user data.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while authenticating.")
	])
	Response authenticateUser(
			@ApiParam(value = "JSON representation of user authentication request.",
			required = true)
			AuthRequestWS authRequest) {
		try {
			return Response.ok()
					.entity(jwtAuthenticationService.authenticateUser(authRequest))
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

	@POST
	@Path("/verifyToken")
	@ApiOperation(value = "verifying user token.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "User token verified.", response = JwtDecodedTokenInfoWS.class),
		@ApiResponse(code = 400, message = "Invalid user data.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while verifying.", response = ErrorDetails.class)
	])
	Response verifyToken(
			@ApiParam(value = "JSON representation of token request.",
			required = true)
			TokenVerificationRequestWS tokenVerificationRequest) {
		try {
			return Response.ok()
					.entity(jwtAuthenticationService.verifyToken(tokenVerificationRequest))
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

	@GET
	@Path("/userInfo")
	@ApiOperation(value = "current loggedin user info.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "user fetched.", response = LoggedInUserInfoWS.class),
		@ApiResponse(code = 404, message = "user not found.", response = ErrorDetails.class),
		@ApiResponse(code = 400, message = "Invalid user data.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failed.", response = ErrorDetails.class)
	])
	Response userInfo() {
		try {
			return Response.ok()
					.entity(jwtAuthenticationService.loggedInUserInfo())
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}
}
