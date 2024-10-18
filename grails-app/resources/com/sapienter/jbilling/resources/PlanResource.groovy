package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.item.PlanWS
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
import javax.ws.rs.QueryParam
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path("/api/plans")
@Api(value="/api/plans", description = "Plans.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class PlanResource {

    IWebServicesSessionBean webServicesSession;

    @GET
    @ApiOperation(value = "Get all plans.", response = PlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plans found or empty.", response = PlanWS.class),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getAllPlans(){
        try {
            return Response.ok().entity(webServicesSession.getAllPlans()).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get plan by id.", response = PlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plan found.", response = PlanWS.class),
            @ApiResponse(code = 404, message = "Plan not found."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response getPlanById(
            @ApiParam(name = "id",
                    value = "The id of the plan that needs to be fetched.",
                    required = true)
            @PathParam("id") Integer id) {
        try{
            PlanWS planWS = webServicesSession.getPlanWS(id);
            return null != planWS ? Response.ok().entity(planWS).build() :
            Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

     @GET
     @Path("/planNumber/{planNumber}")
     @ApiOperation(value = "Get plan by number.", response = PlanWS.class)
            @ApiResponses(value = [
                @ApiResponse(code = 200, message = "Plan found.", response = PlanWS.class),
                @ApiResponse(code = 404, message = "Plan not found."),
                @ApiResponse(code = 500, message = "The call resulted with internal error.")])
            Response getPlanByPlanNumber(
            @ApiParam(name = "planNumber",
            value = "The plan number(internal number) of the plan that needs to be fetched.",
            required = true)
            @PathParam("planNumber") String planNumber) {
                try{
                    Integer entityId = webServicesSession.getCallerCompanyId();
                    PlanWS planWS = webServicesSession.getPlanByInternalNumber(planNumber, entityId);
                    return null != planWS ? Response.ok().entity(planWS).build() :
                    Response.status(Response.Status.NOT_FOUND).build();
                } catch (Exception e){
                    return RestErrorHandler.mapErrorToHttpResponse(e);
                }
            }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create plan.")
    @ApiResponses(value = [
            @ApiResponse(code = 201, message = "Plan created.", response = PlanWS.class),
            @ApiResponse(code = 400, message = "Problem occurred while persisting plan."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response createPlan(
            @ApiParam(value = "Created plan object.", required = true)
                    PlanWS plan,
            @Context
                    UriInfo uriInfo){
        try {
            Integer planId = webServicesSession.createPlan(plan);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(Integer.toString(planId)).build())
                    .entity(webServicesSession.getPlanWS(planId)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates existing plan.", response = PlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "Plan created.", response = PlanWS.class),
            @ApiResponse(code = 404, message = "Plan with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response updatePlan(
            @ApiParam(name = "id", value = "The id of the plan that needs to be updated.", required = true)
            @PathParam("id")
                    Integer id,
            @ApiParam(value = "Plan object containing update data.", required = true)
                    PlanWS planWS){
        try {
            planWS.setId(id);
            webServicesSession.updatePlan(planWS);
            return Response.ok().entity(webServicesSession.getPlanWS(id)).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes existing plan.")
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "Plan with the supplied id deleted."),
            @ApiResponse(code = 404, message = "Plan with the supplied id does not exists."),
            @ApiResponse(code = 500, message = "Internal problem occurred.")])
    Response deletePlan(@ApiParam(name = "id",
            value = "The id of the plan that needs to be deleted.", required = true)
                               @PathParam("id") Integer id){
        try {
            webServicesSession.deletePlan(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

}
