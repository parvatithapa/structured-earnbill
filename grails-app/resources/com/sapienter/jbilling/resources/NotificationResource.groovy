package com.sapienter.jbilling.resources

import com.sapienter.jbilling.server.notification.MessageDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.utils.RestErrorHandler
import com.wordnik.swagger.annotations.*
import grails.plugin.springsecurity.annotation.Secured

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Path("/api/notifications")
@Api(value="/api/notifications", description = "Notification.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
@Produces(MediaType.APPLICATION_JSON)
class NotificationResource {

    IWebServicesSessionBean webServicesSession;

    @POST
    @Path("/notifyUserByEmail")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Notify user by email")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "True or false.", response = String.class),
            @ApiResponse(code = 400, message = "Invalid user supplied."),
            @ApiResponse(code = 500, message = "The call resulted with internal error.")])
    Response notifyUserByEmail(
            @ApiParam(name="userId", value = "User Id", required = true) Integer userId,
            @ApiParam(name="notificationId", value = "Notification Id", required = true)
            @QueryParam("notificationId") Integer notificationId,
            @Context
                    UriInfo uriInfo){
        try {

            Boolean result = webServicesSession.notifyUserByEmail(userId, notificationId);
            return Response.ok().entity(result.toString()).build()
        }catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/createNotificationMessageType")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new notification message type")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "True or false.", response = String.class),
            @ApiResponse(code = 400, message = "Invalid notification message type."),
            @ApiResponse(code = 500, message = "Failure while creating the notification message type.")
    ])
    Response createNotificationMessageType(
            @ApiParam(name="description", value = "Description", required = true)
                    String description,
            @ApiParam(name="notificationCategoryId", value = "Notification Category Id", required = true)
            @QueryParam("notificationCategoryId") Integer notificationCategoryId,
            @ApiParam(name="languageId", value = "Notification Id", required = true)
            @QueryParam("languageId") Integer languageId,
            @Context
                    UriInfo uriInfo){
        try {

            Integer result = webServicesSession.createMessageNotificationType(notificationCategoryId,
                    description, languageId);
            return Response.ok().entity(result.toString()).build()
        }catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }

    @POST
    @Path("/getIdFromCreateUpdateNotification")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new notification message")
    @ApiResponses(value = [
            @ApiResponse(code = 200, message = "True or false.", response = String.class),
            @ApiResponse(code = 400, message = "Invalid notification message."),
            @ApiResponse(code = 500, message = "Failure while creating the notification message .")
    ])
    Response getIdFromCreateUpdateNotification(
            @ApiParam(name="messageDTO", value = "Message DTO", required = true)
                    MessageDTO messageDTO,
            @ApiParam(name="messageId", value = "Message Id")
            @QueryParam("messageId") Integer messageId,
            @Context
                    UriInfo uriInfo){
        try {

            Integer result = webServicesSession.getIdFromCreateUpdateNotification(messageId, messageDTO);
            return Response.ok().entity(result.toString()).build()
        }catch (Exception e){
            return RestErrorHandler.mapErrorToHttpResponse(e);
        }
    }
}
