package com.sapienter.jbilling.resources


import com.sapienter.jbilling.auth.AdennetJwtTokenResponseWS

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import grails.plugin.springsecurity.annotation.Secured

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

import com.sapienter.jbilling.auth.AuthRequestWS
import com.sapienter.jbilling.auth.GetSecurityUserRequestWS
import com.sapienter.jbilling.auth.GetSecurityUserResponseWS
import com.sapienter.jbilling.auth.JwtAuthenticationService
import com.sapienter.jbilling.auth.JwtDecodedTokenInfoWS
import com.sapienter.jbilling.auth.JwtTokenResponseWS
import com.sapienter.jbilling.auth.RefreshTokenResponseWS
import com.sapienter.jbilling.auth.TokenVerificationRequestWS
import com.sapienter.jbilling.auth.LoggedInUserInfoWS
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
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class JwtAuthenticationResource {

	final static String COOKIE_REFRESH_TOKEN = "_msrt"

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
			AuthRequestWS authRequest,
			@Context HttpServletResponse httpServletResponse) {
		try {

			JwtTokenResponseWS jwtToken = jwtAuthenticationService.authenticateUser(authRequest)

			httpServletResponse.addCookie(getRefreshTokenCookie(jwtToken.getRefreshToken()))

			return Response.ok()
					.entity(jwtToken)
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

	@GET
	@Path("/refreshToken")
	@ApiOperation(value = "refresh jwt token.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "refreshToken Response.", response = RefreshTokenResponseWS.class),
		@ApiResponse(code = 400, message = "Invalid refreshToken.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while refreshToken.", response = ErrorDetails.class)
	])
	Response refreshToken(@QueryParam("refreshToken") @ApiParam(name = "refreshToken") String refreshToken,
			@Context HttpServletRequest httpServletRequest) {
		try {
			Cookie ckRefreshToken = httpServletRequest.getCookies().find{ cookie -> cookie.getName().equals(COOKIE_REFRESH_TOKEN )}

			return Response.ok()
					.entity(jwtAuthenticationService.refreshToken( (ckRefreshToken != null) ? ckRefreshToken.getValue() : refreshToken))
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

	@POST
	@Path("/security-user")
	@ApiOperation(value = "getSecurityUser.")
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "User response.", response = GetSecurityUserResponseWS.class),
		@ApiResponse(code = 400, message = "Invalid getSecurityUserWS.", response = ErrorDetails.class),
		@ApiResponse(code = 404, message = "user not found.", response = ErrorDetails.class),
		@ApiResponse(code = 500, message = "Failure while getSecurityUser.", response = ErrorDetails.class)
	])
	Response getSecurityUser(@ApiParam( value = "JSON representation of getSecurityUserWS request.", required = true) GetSecurityUserRequestWS getSecurityUserRequestWS) {
		try {
			return Response.ok()
					.entity(jwtAuthenticationService.getSecurityUser(getSecurityUserRequestWS))
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

	private Cookie getRefreshTokenCookie(String refreshToken) {
		Cookie ckRefreshToken =  new Cookie(COOKIE_REFRESH_TOKEN, refreshToken);
		ckRefreshToken.setPath("/")
		ckRefreshToken.setHttpOnly(true);

		return ckRefreshToken;
	}

	@POST
	@Path("/authenticate/{subscriberNumber}")
	@ApiOperation(value = "Create JWT token for adennet specific subscriber", response = AdennetJwtTokenResponseWS.class)
	@ApiResponses(value = [
			@ApiResponse(code = 200, message = "Token created successfully", response = AdennetJwtTokenResponseWS.class),
			@ApiResponse(code = 403, message = "Invalid request", response = ErrorDetails.class),
			@ApiResponse(code = 500, message = "Failure while generating token.")
	])
	Response authenticateAdennetSubscriber(
			@PathParam("subscriberNumber") @ApiParam(name="subscriberNumber", value = "Create a JWT token specific to the subscriber number", required = true) String subscriberNumber,
			@Context HttpServletResponse httpServletResponse) {
		try {
			AdennetJwtTokenResponseWS jwtToken = jwtAuthenticationService.authenticateAdennetSubscriber(subscriberNumber);
			httpServletResponse.addCookie(getRefreshTokenCookie(jwtToken.getRefreshToken()))
			return Response.ok()
					.entity(jwtToken)
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}

	@GET
	@Path("/subscriber/refreshToken")
	@ApiOperation(value = "Refresh adnnent specific subscriber jwt token.")
	@ApiResponses(value = [
			@ApiResponse(code = 200, message = "RefreshToken Response.", response = RefreshTokenResponseWS.class),
			@ApiResponse(code = 403, message = "Invalid refreshToken.", response = ErrorDetails.class),
			@ApiResponse(code = 500, message = "Failure while refreshing token.", response = ErrorDetails.class)
	])
	Response refreshAdennetToken(@QueryParam("refreshToken") @ApiParam(name = "refreshToken") String refreshToken,
						  @Context HttpServletRequest httpServletRequest) {
		try {
			Cookie ckRefreshToken = httpServletRequest.getCookies().find{ cookie -> cookie.getName().equals(COOKIE_REFRESH_TOKEN )}

			return Response.ok()
					.entity(jwtAuthenticationService.adennetRefreshToken( (ckRefreshToken != null) ? ckRefreshToken.getValue() : refreshToken))
					.build()
		} catch (Exception e) {
			return RestErrorHandler.mapErrorToHttpResponse(e)
		}
	}
}
