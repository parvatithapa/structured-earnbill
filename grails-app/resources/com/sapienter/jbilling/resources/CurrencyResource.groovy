package com.sapienter.jbilling.resources

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import com.sapienter.jbilling.server.util.CurrencyWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiParam
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured

@Path("/api/currencies")
@Api(value="/api/currencies", description = "Currency.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class CurrencyResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @ApiOperation(value = "Get all currencies.", response = CurrencyWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Currency found or empty.", response = CurrencyWS.class),
        @ApiResponse(code = 500, message = "Internal error occurred.")
    ])
    Response getCurrencies(){
        try {
            return Response.ok().entity(webServicesSession.getCurrencies()).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

	@GET
	@Path("/{code}")
	@ApiOperation(value = "Get currency by code.", response = CurrencyWS.class)
	@ApiResponses(value = [
		@ApiResponse(code = 200, message = "Currency found or empty.", response = CurrencyWS.class),
		@ApiResponse(code = 500, message = "Internal error occurred.")
	])
	Response getCurrencyByCode(@ApiParam(name = "code", value = "The code of the currency that needs to be fetched.", required = true)
            @PathParam("code")
            String code){
		try {
			CurrencyWS[] currencies =webServicesSession.getCurrencies();
			CurrencyWS currencyByCode = null;
			for (CurrencyWS currency : currencies) {
				if(code.equalsIgnoreCase(currency.getCode()))
				{ 
					currencyByCode = currency;
					break;
				}
			}
			return Response.ok().entity(currencyByCode).build();
		} catch (Exception e){
			return RestErrorHandler.mapErrorToHttpResponse(e);
		}
	}
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create currency.")
    @ApiResponses(value = [
        @ApiResponse(code = 201, message = "Currency created.", response = Integer.class),
        @ApiResponse(code = 400, message = "Invalid currency supplied."),
        @ApiResponse(code = 500, message = "Internal problem occurred.")
    ])
    Response createCurrency(
            @ApiParam(value = "Created currency object.", required = true)
            CurrencyWS currencyWs,
            @Context
            UriInfo uriInfo){
        try {
            Integer currencyId = webServicesSession.createCurrency(currencyWs);
            return Response.ok().entity(currencyId).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing currency.", response = CurrencyWS.class)
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Currency updated.", response = Integer.class),
        @ApiResponse(code = 400, message = "Invalid currency supplied."),
        @ApiResponse(code = 404, message = "currency with the supplied id does not exists."),
        @ApiResponse(code = 500, message = "Internal problem occurred.")
    ])
    Response updateCurrency(
            @ApiParam(name = "id", value = "The id of the currency that needs to be updated.", required = true)
            @PathParam("id")
            Integer id,
            @ApiParam(value = "Currency object containing update data.", required = true)
            CurrencyWS currencyWS){
        try {
            if(id.equals(currencyWS.getId())) {
                webServicesSession.updateCurrency(currencyWS);
                return Response.ok().entity(currencyWS.getId()).build();
            } else {
                return Response.status(404).entity("Invalid currency id").build()
            }
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing currencies.")
    @ApiResponses(value = [
        @ApiResponse(code = 200, message = "Currencies updated."),
        @ApiResponse(code = 400, message = "Invalid currencies supplied."),
        @ApiResponse(code = 404, message = "Ids for the supplied currencies does not exists."),
        @ApiResponse(code = 500, message = "Internal problem occurred.")
    ])
    Response updateCurrencies(
            @ApiParam(value = "Currencies object containing update data.", required = true)
            CurrencyWS[] currencies){
        try {
            webServicesSession.updateCurrencies(currencies);
            return Response.ok().build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing currency.")
    @ApiResponses(value = [
        @ApiResponse(code = 204, message = "Currency with the supplied id deleted."),
        @ApiResponse(code = 404, message = "Currency with the supplied id does not exists."),
        @ApiResponse(code = 409, message = "Deletion resulted with conflict.")
    ])
    Response deleteCurrency(@ApiParam(name = "id",
            value = "The id of the currency that needs to be deleted.", required = true)
            @PathParam("id") Integer id){
        try {
            webServicesSession.deleteCurrency(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

}
