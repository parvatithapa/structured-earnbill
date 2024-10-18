package com.sapienter.jbilling.resources

import static javax.ws.rs.core.MediaType.APPLICATION_JSON

import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

import org.springframework.security.access.annotation.Secured

import com.sapienter.jbilling.common.ErrorDetails
import com.sapienter.jbilling.server.process.signup.SignupRequestWS
import com.sapienter.jbilling.server.process.signup.SignupResponseWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses

@Path('/api/processsignup')
@Api(value = "/api/processsignup", description = "Signup Process.")
@Produces(APPLICATION_JSON)
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class SignupProcessResource {

    IWebServicesSessionBean webServicesSession;

    @POST
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Signup Process.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Signup process done.", response = SignupResponseWS.class),
        @ApiResponse(code = 400, message = "Invalid signup request supplied.", response = ErrorDetails.class),
        @ApiResponse(code = 500, message = "Failure while processing signup request."),
        @ApiResponse(code = 404, message = "Signup request not found.")
    ])
    Response processSignupRequest(SignupRequestWS requestWS) {
        try {
            if(null == requestWS) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            SignupResponseWS responseWS = webServicesSession.processSignupRequest(requestWS)
            if (responseWS.hasError()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseWS).build();
            }
            return  Response.ok().entity(responseWS).build()
        } catch(Exception ex) {
            return RestErrorHandler.mapErrorToHttpResponse(ex);
        }
    }
}
