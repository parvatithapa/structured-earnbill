package com.sapienter.jbilling.resources

import com.sapienter.jbilling.appdirect.subscription.PayloadWS

import com.sapienter.jbilling.appdirect.userCompany.CompanyPayload
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.utils.RestErrorHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured
import jbilling.DtCustomerService

import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import java.lang.invoke.MethodHandles


@Path("/api/users/dt")
@Secured('permitAll')
class DtCustomerResource {

    DtCustomerService dtCustomerService
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{entityIdentifier}")
    Response createDTCustomer(
                            @PathParam("entityIdentifier") String entityIdentifier,
                            PayloadWS payloadWS,
                            @Context UriInfo uriInfo) {
        try {
            UserWS childCustomer = dtCustomerService.createDTCustomer(entityIdentifier, payloadWS)
            return Response.created(uriInfo.getAbsolutePathBuilder()
                    .path(Integer.toString(childCustomer.getUserId())).build())
                    .build()
        } catch (Exception e){
            logger.error("Error occurred during create customer", e)
            return RestErrorHandler.mapErrorToHttpResponse(e)
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/delete/{entityIdentifier}")
    Response deleteDTCustomer(
                            @PathParam("entityIdentifier") String entityIdentifier,
                            PayloadWS payloadWS) {

        try {
            dtCustomerService.deleteDTCustomer(entityIdentifier, payloadWS)

        } catch (Exception exp) {
            logger.error("Error occurred during delete customer", exp)
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }

        return Response.status(Response.Status.OK).build()
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/update/{entityIdentifier}")
    Response updateCompany(
            @PathParam("entityIdentifier") String entityIdentifier,
            CompanyPayload companyPayload) {

        try {
            dtCustomerService.updateCompany(entityIdentifier, companyPayload)

        } catch (Exception exp) {
            logger.error("Error occurred during create customer", exp)
            return RestErrorHandler.mapErrorToHttpResponse(exp);
        }
        return Response.status(Response.Status.OK).build()
    }

}
