package com.sapienter.jbilling.resources

import com.sapienter.jbilling.DtOrderPlanWS
import com.sapienter.jbilling.ErrorResponse
import com.sapienter.jbilling.PaginatedRecordWS
import com.sapienter.jbilling.catalogue.DtPlanWS
import com.sapienter.jbilling.exception.DtReserveInstanceException
import com.sapienter.jbilling.server.dt.reserve.validator.DtReserveInstanceValidator
import com.sapienter.jbilling.server.util.search.SearchCriteria
import com.sapienter.jbilling.subscribe.DtCancelValidationStatusResponse
import com.sapienter.jbilling.subscribe.DtSubscribeRequestPayload
import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.plugin.springsecurity.annotation.Secured
import grails.util.Holders
import jbilling.DtReserveInstanceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.UnexpectedRollbackException

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import java.lang.invoke.MethodHandles

@Path("/api/v1/reserve/dt")
@Api(value="/api/v1/reserve/dt", description = "DT Reserve Instance.")
@Secured(["IS_AUTHENTICATED_FULLY", "API_120"])
class DtReserveInstanceResource {
    DtReserveInstanceValidator dtReserveInstanceValidator
    DtReserveInstanceService dtReserveInstanceService
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    def messageSource = Holders.getGrailsApplication().getMainContext().getBean("messageSource")

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetch list of all reserve instance.", response = DtOrderPlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "No Content found for the Reserved Instance Plan Catalogue"),
            @ApiResponse(code = 500, message = "Failure while retrieving reserved plan catalogue."),
            @ApiResponse(code = 400, message = "Invalid parameter received in request.")
    ])
    @Path("/catalogue")
    Response listReservePlanCatalogue(@Context UriInfo uriInfo,
                                      @QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize,
                                      @QueryParam("sortField") String sortField, @QueryParam("sortOrder") String sortOrder,
                                      @QueryParam("productCategory") String filter, @QueryParam("ram") String ram, @QueryParam("os") String os, @QueryParam("cpu") String cpu, @HeaderParam("Accept-Language") String localeStr) {
        Locale locale = dtReserveInstanceValidator.validateLocale(localeStr)
        try {
            SearchCriteria criteria = dtReserveInstanceValidator.validateSearchCriteria(pageSize, pageNumber, sortField, sortOrder, filter, locale, os, ram, cpu)
            PaginatedRecordWS<DtPlanWS> dtPlanWSlist = dtReserveInstanceService.listReservePlanCatalogue(criteria, locale)

            if (!dtPlanWSlist.getRecords().isEmpty()) {
                return Response.ok().entity(dtPlanWSlist).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
            } else {
                String errorMesssage = messageSource.getMessage('validation.error.catalogue.not.found', null, messageSource.getMessage(
                        'validation.error.catalogue.not.found', null, 'No Content found for the Reserved Instance Plan Catalogue', locale), locale)
                ErrorResponse errorResponse = new ErrorResponse(204, errorMesssage)

                return Response.noContent().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
            }
        } catch(DtReserveInstanceException e){
            ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getErrorMessage())

            return Response.status(e.getErrorCode()).entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        } catch (Exception e){
            logger.error("Error occurred during listing catalogue for reserve plan", e)
            String errorMesssage = messageSource.getMessage('validation.error.catalogue.failure', null, messageSource.getMessage(
                    'validation.error.catalogue.failure', null, 'Failure while retrieving reserved plan catalogue.', locale), locale)
            ErrorResponse errorResponse = new ErrorResponse(500, errorMesssage)

            return Response.serverError().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetch list of reserve instance order's of a user.", response = DtOrderPlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "No Reserved subscription found for this user"),
            @ApiResponse(code = 500, message = "Failure while retrieving reserve plan for user."),
            @ApiResponse(code = 400, message = "Invalid parameter received in request."),
            @ApiResponse(code = 404, message = "Invalid subscription id  received.")
    ])
    @Path("/{externalAccountId}")
    Response listReservedSubscription(@PathParam("externalAccountId") String externalAccountId, @Context UriInfo uriInfo,
                                      @QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize,
                                      @QueryParam("sortField") String sortField, @QueryParam("sortOrder") String sortOrder,
                                      @QueryParam("productCategory") String filter, @QueryParam("ram") String ram, @QueryParam("os") String os, @QueryParam("cpu") String cpu, @HeaderParam("Accept-Language") String localeStr) {
        Locale locale = dtReserveInstanceValidator.validateLocale(localeStr)
        try {
            SearchCriteria criteria = dtReserveInstanceValidator.validateSearchCriteria(pageSize, pageNumber, sortField, sortOrder, filter, locale, os, ram, cpu)
            PaginatedRecordWS<DtOrderPlanWS> subscriptionPlanlist = dtReserveInstanceService.listReserveSubscription(externalAccountId, criteria, locale)

            if (!subscriptionPlanlist.getRecords().isEmpty()) {
                return Response.ok().entity(subscriptionPlanlist).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
            } else {
                String errorMesssage = messageSource.getMessage('validation.error.subscription.reserve.list.not.found',
                        null, messageSource.getMessage('validation.error.subscription.reserve.list.not.found', null,
                        'No Reserved subscription found for this user', locale), locale)
                ErrorResponse errorResponse = new ErrorResponse(204, errorMesssage)

                return Response.noContent().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
            }
        } catch(DtReserveInstanceException e){
            ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getErrorMessage())

            return Response.status(e.getErrorCode()).entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        } catch (Exception e){
            String errorMesssage = messageSource.getMessage('validation.error.subscription.reserve.list.failure', null,
                    messageSource.getMessage('validation.error.subscription.reserve.list.failure', null,
                            'Failure while retrieving reserve plan for user.', locale), locale)

            logger.error("Error occurred during listing reserve plan of a user", e)

            ErrorResponse errorResponse = new ErrorResponse(500, errorMesssage)

            return Response.serverError().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Upgrade a plan to another plan in a order", response = DtOrderPlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 500, message = "Failure while upgrading plan."),
            @ApiResponse(code = 500, message = "Order level meta-field may be missing."),
            @ApiResponse(code = 400, message = "Invalid parameter received in request. Cannot upgrade"),
            @ApiResponse(code = 404, message = "Invalid subscription id  received."),
            @ApiResponse(code = 404, message = "Invalid plan id received."),
            @ApiResponse(code = 404, message = "The order ID does not exist.")
    ])
    @Path("/{orderId}")
    Response upgradePlan(
            @PathParam("orderId") String orderId,
            DtSubscribeRequestPayload upgradeInfo,
            @Context UriInfo uriInfo, @HeaderParam("Accept-Language") String localeStr) {
        Locale locale = dtReserveInstanceValidator.validateLocale(localeStr)
        try {
            DtOrderPlanWS order = dtReserveInstanceService.upgradeReservedPlan(orderId, upgradeInfo, locale)

            return Response.ok().entity(order).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        } catch (DtReserveInstanceException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getErrorMessage())

            return Response.status(e.getErrorCode()).entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()

        }catch (Exception e){
            String errorMesssage = messageSource.getMessage('validation.error.upgrade.failure', null, messageSource.getMessage(
                    'validation.error.upgrade.failure', null, 'Failure while upgrading plan.', locale), locale)

            logger.error("Error occurred during upgrade order", e)

            ErrorResponse errorResponse = new ErrorResponse(500, errorMesssage)

            return Response.serverError().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Purchase a reserve instance", response = DtOrderPlanWS.class)
    @ApiResponses(value = [
            @ApiResponse(code = 500, message = "Failure while subscribing plan."),
            @ApiResponse(code = 500, message = "Order level meta-field may be missing."),
            @ApiResponse(code = 404, message = "Invalid subscription id  received."),
            @ApiResponse(code = 204, message = "No reserved subscription found for this user"),
            @ApiResponse(code = 204, message = "Invalid parameter received in request")
    ])
    @Path("/")
    Response subscribePlan(
            DtSubscribeRequestPayload upgradeInfo,
            @Context UriInfo uriInfo,  @HeaderParam("Accept-Language") String localeStr) {
        Locale locale = dtReserveInstanceValidator.validateLocale(localeStr)
        try {
            DtOrderPlanWS order = dtReserveInstanceService.subscribeReservedPlan(upgradeInfo, null, locale)

            return Response.ok().entity(order).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }catch (DtReserveInstanceException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getErrorMessage())

            return Response.status(e.getErrorCode()).entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()

        }catch (Exception e){
            String errorMesssage = messageSource.getMessage('validation.error.reserve.purchase.failure', null, messageSource.getMessage(
                    'validation.error.reserve.purchase.failure', null, 'Failure while subscribing plan.', locale), locale)

            logger.error("Error occurred during subscription order", e)
            ErrorResponse errorResponse = new ErrorResponse(500, errorMesssage)

            return Response.serverError().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if subscription can be cancelled", response = DtCancelValidationStatusResponse.class)
    @ApiResponses(value = [
            @ApiResponse(code = 500, message = "Failure while validating cancellation."),
            @ApiResponse(code = 404, message = "Invalid subscription id  received.")
    ])
    @Path("/cancelSubscription/{subscriptionId}")
    Response cancelStatus(
            @PathParam("subscriptionId") String subscriptionId,
            @Context UriInfo uriInfo, @HeaderParam("Accept-Language") String localeStr) {
        Locale locale = dtReserveInstanceValidator.validateLocale(localeStr)
        try {
            DtCancelValidationStatusResponse status = dtReserveInstanceService.validateCancellation(subscriptionId, locale)

            return Response.ok().entity(status).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }catch (DtReserveInstanceException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getErrorMessage())

            return Response.status(e.getErrorCode()).entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        } catch (Exception e){
            logger.error("Error occurred during validating subscription", e)
            String errorMesssage = messageSource.getMessage('validation.error.cancellation.failure', null, messageSource.getMessage(
                    'validation.error.cancellation.failure', null, 'Failure while validating cancellation.', locale), locale)
            ErrorResponse errorResponse = new ErrorResponse(500, errorMesssage)

            return Response.serverError().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetch list of all reserve categories.", response = Map.class)
    @ApiResponses(value = [
            @ApiResponse(code = 204, message = "No category found"),
            @ApiResponse(code = 500, message = "Failure while retrieving reserved plan category.")
    ])
    @Path("/categories")
    Response listReservePlanCategories(@Context UriInfo uriInfo, @HeaderParam("Accept-Language") String localeStr) {
        Locale locale = dtReserveInstanceValidator.validateLocale(localeStr)
        try {
            Map<String,Set<String>> categories = dtReserveInstanceService.getCategories()
            if (categories.get(DtReserveInstanceService.categoryKey).size() != 0) {
                return Response.ok().entity(categories).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
            } else {
                String errorMesssage = messageSource.getMessage('validation.error.category.not.found', null, messageSource.getMessage(
                        'validation.error.category.not.found', null, 'No category found', locale), locale)
                ErrorResponse errorResponse = new ErrorResponse(204, errorMesssage)

                return Response.noContent().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
            }
        } catch(DtReserveInstanceException e){
            ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode(), e.getErrorMessage())

            return Response.status(e.getErrorCode()).entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        } catch (Exception e){
            logger.error("Error occurred during listing reserved plan categories", e)
            String errorMesssage = messageSource.getMessage('validation.error.category.failure', null, messageSource.getMessage(
                    'validation.error.category.failure', null, 'Failure while retrieving reserved plan category.', locale), locale)
            ErrorResponse errorResponse = new ErrorResponse(500, errorMesssage)

            return Response.serverError().entity(errorResponse).type(MediaType.APPLICATION_JSON + "; charset=utf-8").build()
        }

    }



}
