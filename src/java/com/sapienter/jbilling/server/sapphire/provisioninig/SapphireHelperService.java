package com.sapienter.jbilling.server.sapphire.provisioninig;

import static com.sapienter.jbilling.server.sapphire.SapphireConstants.ASSET_MF_EX_DIRECTORY_NAME;
import static com.sapienter.jbilling.server.sapphire.SapphireConstants.ASSET_MF_TALK_APP_NAME;
import static com.sapienter.jbilling.server.sapphire.SapphireConstants.ASSET_MF_WITH_HELD_NAME;
import static com.sapienter.jbilling.server.sapphire.provisioninig.OrderProvisioninigStatus.PENDING_DEVICE_SWAP;
import static com.sapienter.jbilling.server.sapphire.provisioninig.OrderProvisioninigStatus.PENDING_NEW_SALE;
import static com.sapienter.jbilling.server.sapphire.provisioninig.OrderProvisioninigStatus.PENDING_UPDATE_OF_ASSET;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelper.createProvisioningRequestFromOrder;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelper.createProvisioningRequestFromSapphireSwapEvent;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelper.mapToProvisioningRequest;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelper.updateCustomerProvisioningStatusMf;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelper.updateOrderProvisioningStatusMf;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.ACTIVATION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.ADDON_ACTIVATION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.CHANGE_CREDENTIALS;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.CHANGE_EX_DIRECTORY;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.CHANGE_PLAN;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.DEVICE_SWAP;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.DISCONNECTION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.NEW_SALE;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.REACTIVATION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.RECONNECTION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.SERVICE_TRANSFER;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.SUSPENSION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.TERMINATION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_AIT_GROUP_NAME;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_API_URL;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_ORDER_PROVISIONING_STATUS_MF_NAME;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_PASSWORD;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_SERVICE_ORDER_ID_MF_NAME;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_TIME_OUT;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningTask.PARAM_USER_NAME;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.PENDING_CHANGE_OF_CREDENTIALS;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.PENDING_DISCONNECTION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.PENDING_FOR_SERVICE_TRANSFER;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.PENDING_REACTIVATION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.PENDING_RECONNECTION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.PENDING_SUSPENSION;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.PENDING_TERMINATION;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.sapienter.jbilling.common.HandleException;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customer.event.UpdateCustomerEvent;
import com.sapienter.jbilling.server.integration.db.OutBoundInterchange;
import com.sapienter.jbilling.server.integration.db.OutBoundInterchangeDAS;
import com.sapienter.jbilling.server.integration.db.Status;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.SwapAssetWS;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDAS;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.item.event.AssetMetaFieldUpdatedEvent;
import com.sapienter.jbilling.server.item.event.SwapAssetsEvent;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.UpgradeOrderEvent;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanEvent;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanRequestWS;
import com.sapienter.jbilling.server.sapphire.NewSaleEvent;
import com.sapienter.jbilling.server.sapphire.NewSaleRequestWS;
import com.sapienter.jbilling.server.sapphire.OrderPeriod;
import com.sapienter.jbilling.server.sapphire.ProductDetailWS;
import com.sapienter.jbilling.server.sapphire.SapphireHelper;
import com.sapienter.jbilling.server.sapphire.SapphireResponseWS;
import com.sapienter.jbilling.server.sapphire.SapphireSwapAssetEvent;
import com.sapienter.jbilling.server.sapphire.SwapAssetResponse;
import com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean;

/**
 * @author Krunal Bhavsar
 * @author Ashwinkumar Patra
 */
@Transactional
public class SapphireHelperService {

    enum SapphireProvisioningStatus {
        SUSPENDED,
        DISCONNECTED,
        TERMINATED,
        REACTIVATION,
        RECONNECTION,
        NONE
    }

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Integer DEFAULT_TIME_OUT = 10000;
    private static final String INVALID_PLAN_CODE_MESSAGE = "invalid plan code [%s] passed, [%s] not found for entity [%d]";
    private static final String NOT_PLAN_MESSAGE = "invalid plan code [%s] passed, [%s] is not plan.";

    @Resource
    private OutBoundInterchangeDAS outBoundInterchangeDAS;
    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;
    @Resource
    private OrderDAS orderDAS;
    @Resource
    private AssetDAS assetDAS;
    @Resource
    private UserDAS userDAS;
    @Resource
    private OrderStatusDAS orderStatusDAS;
    @Resource
    private ItemDAS itemDAS;
    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Async("asyncTaskExecutor")
    public void postEventToProvisioningAsync(final Event event, final Map<String, String> parameters) {
        try {
            logger.debug("executing async request {} for entity {}", event.getName(), event.getEntityId());
            processEventRequest(event, parameters);
        } catch (Exception ex) {
            logger.error("error occured while sending event to sapphire orchestration layer", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private void processEventRequest(Event event, Map<String, String> parameters) {
        String eventName = event.getClass().getSimpleName();
        switch (eventName) {
        case "PaymentSuccessfulEvent":
            paymentSuccessful(event, parameters);
            break;
        case "NewUserStatusEvent":
            userStatusChanged(event, parameters);
            break;
        case "SwapAssetsEvent":
        case "SapphireSwapAssetEvent":
            deviceSwap(event, parameters);
            break;
        case "UpgradeOrderEvent":
            activateAddOns(event, parameters);
            break;
        case "AssetMetaFieldUpdatedEvent":
            updateAssetMf(event, parameters);
            break;
        case "UpdateCustomerEvent":
            changeOfCredentialsAndServiceTransfer(event, parameters);
            break;
        case "ChangeOfPlanEvent":
            changeOfPlan(event, parameters);
            break;
        case "NewSaleEvent":
            newSaleEvent(event, parameters);
            break;
        default:
            break;
        }
    }

    public void postEventToProvisioning(final Event event, final Map<String, String> parameters) {
        try {
            logger.debug("executing request {} for entity {}", event.getName(), event.getEntityId());
            processEventRequest(event, parameters);
        } catch (Exception ex) {
            logger.error("error occured while sending event to sapphire orchestration layer", ex);
        }
    }

    @Async("asyncTaskExecutor")
    public void retryOutBoundInterchangeRequest(Integer requestId, Integer pluginId, Integer entityId, Integer maxRetry) {
        logger.debug("SapphireProvisioningHelperService.retryOutBoundInterchangeRequest for request {} ", requestId);
        OutBoundInterchange outBoundInterchange = null;
        try {
            outBoundInterchange = outBoundInterchangeDAS.
                    findFailedFailedOutBoundInterchangeRequestWithLock(requestId);
            Integer userId = validateRetryAndGetUser(entityId, maxRetry, outBoundInterchange);
            if (null == userId) {
                return;
            }
            Map<String, String> parameters = collectParametersFromPlugin(pluginId, entityId);
            String aitGroupName = getAndValidateParameterValue(parameters, PARAM_AIT_GROUP_NAME.getName(), entityId);
            outBoundInterchange.incrementRetry();
            outBoundInterchange.setLastRetryDateTime(new Date());
            SapphireProvisioningRequestWS retrypaymentRequest = mapToProvisioningRequest(userId,
                    outBoundInterchange.getMethodName().toUpperCase(), aitGroupName);
            outBoundInterchange.setRequest(OBJECT_MAPPER.writeValueAsString(retrypaymentRequest));
            callRestPostAPI(outBoundInterchange, parameters);

        } catch (Exception ex) {
            logger.error("failed during retry of request {} for entity {} ", outBoundInterchange, entityId, ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @Transactional
    @HandleException
    public Integer changeOfPlan(ChangeOfPlanRequestWS changeOfPlanRequest) {
        if(null == changeOfPlanRequest.getOrderId()) {
            throw new SessionInternalError("Order parameter is null", "Please enter orderId.", HttpStatus.SC_BAD_REQUEST);
        }

        if(StringUtils.isEmpty(changeOfPlanRequest.getExistingPlanCode())) {
            throw new SessionInternalError("existingPlanCode parameter is null or empty", "Please enter existingPlanCode.", HttpStatus.SC_BAD_REQUEST);
        }

        if(StringUtils.isEmpty(changeOfPlanRequest.getNewPlanCode())) {
            throw new SessionInternalError("newPlanCode parameter is null or empty", "Please enter newPlanCode.", HttpStatus.SC_BAD_REQUEST);
        }
        long startTime = System.currentTimeMillis();
        try {
            OrderDTO order = orderDAS.findNow(changeOfPlanRequest.getOrderId());
            if(null == order || order.getDeleted() == 1) {
                logger.error("invalid order id {} passed", changeOfPlanRequest.getOrderId());
                throw new SessionInternalError("Please provide valid order id parameter",
                        new String [] { "Please enter valid order id." }, HttpStatus.SC_NOT_FOUND);
            }
            Integer entityId = api.getCallerCompanyId();
            String existingPlanCode = changeOfPlanRequest.getExistingPlanCode();
            ItemDTO oldPlan = itemDAS.findItemByInternalNumber(existingPlanCode, entityId);
            if(null == oldPlan) {
                logger.debug("no plan found for plan code {} for entity {}", existingPlanCode, entityId);
                throw new SessionInternalError("Please provide valid existingPlanCode parameter",
                        String.format(INVALID_PLAN_CODE_MESSAGE, existingPlanCode, existingPlanCode, entityId), HttpStatus.SC_NOT_FOUND);
            }

            if(!oldPlan.isPlan()) {
                logger.debug("plan code {} is not plan", existingPlanCode);
                throw new SessionInternalError("Please provide valid existingPlanCode parameter",
                        String.format(NOT_PLAN_MESSAGE, existingPlanCode, existingPlanCode), HttpStatus.SC_BAD_REQUEST);
            }

            if(null == order.getLine(oldPlan.getId())) {
                logger.debug("plan item {} not found on order {}", existingPlanCode, oldPlan.getId());
                throw new SessionInternalError("existingPlanCode not found on order",
                        "existingPlanCode not found on order "+ order.getId(), HttpStatus.SC_BAD_REQUEST);
            }

            String newPlanCode = changeOfPlanRequest.getNewPlanCode();
            ItemDTO newPlan = itemDAS.findItemByInternalNumber(newPlanCode, entityId);
            if(null == newPlan) {
                logger.debug("no plan found for plan code {} for entity {}", newPlanCode, entityId);
                throw new SessionInternalError("existing plan code is not plan", String.format(INVALID_PLAN_CODE_MESSAGE,
                        newPlanCode, newPlanCode, entityId), HttpStatus.SC_NOT_FOUND);
            }

            if(!newPlan.isPlan()) {
                logger.debug("plan code {} is not plan", existingPlanCode);
                throw new SessionInternalError("new plan code is not plan", String.format(NOT_PLAN_MESSAGE,
                        existingPlanCode, existingPlanCode), HttpStatus.SC_BAD_REQUEST);
            }
            validatePlan(oldPlan.getPlans().iterator().next(), newPlan.getPlans().iterator().next());
            ChangeOfPlanEvent changeOfPlanEvent = new ChangeOfPlanEvent(order.getId(), existingPlanCode, newPlanCode, entityId);
            logger.debug("processing event {}", changeOfPlanEvent);
            EventManager.process(changeOfPlanEvent);
            return changeOfPlanEvent.getNewOrderId();
        } catch(SessionInternalError error) {
            throw error;
        } catch (Exception e) {
            throw new SessionInternalError("error in changeOfPlan", e);
        } finally {
            logger.debug("time {} taken to finish changeOfPlan", (System.currentTimeMillis() - startTime));
        }
    }

    /**
     * Creates Map for plan Item and period
     * @param plan
     * @return
     */
    private Map<Integer, Integer> createBundleItemPeriodMap(PlanDTO plan) {
        return plan.getPlanItems()
                .stream()
                .filter(planItemDTO-> planItemDTO.getItem().isAssetEnabledItem())
                .collect(Collectors.toMap(planItemDTO-> planItemDTO.getItem().getId(),
                        planItemDTO-> {
                            OrderPeriodDTO period = planItemDTO.getBundle().getPeriod();
                            if(period.getId() == Constants.ORDER_PERIOD_ONCE) {
                                return Constants.ORDER_PERIOD_ONCE;
                            }
                            return period.getPeriodUnit().getId();
                        }));
    }

    /**
     * validates both plan's bundle time period.
     * @param oldPlan
     * @param newPlan
     */
    private void validatePlan(PlanDTO oldPlan, PlanDTO newPlan) {
        Map<Integer, Integer> oldPlanItemPeriodMap = createBundleItemPeriodMap(oldPlan);
        logger.debug("old plan bundle item period map {}", oldPlanItemPeriodMap);
        Map<Integer, Integer> newPlanItemPeriodMap = createBundleItemPeriodMap(newPlan);
        logger.debug("new plan bundle item period map {}", newPlanItemPeriodMap);
        if(!MapUtils.isEmpty(Maps.difference(oldPlanItemPeriodMap, newPlanItemPeriodMap).entriesDiffering())) {
            logger.debug("bundle item period mismatch between old plan {} and new plan {}", oldPlan.getId(), newPlan.getId());
            throw new SessionInternalError("bundle item period mismatch",
                    "bundle item period mismatch between old and new plan", HttpStatus.SC_BAD_REQUEST);
        }
    }
    /**
     * @param entityId
     * @param maxRetry
     * @param outBoundInterchange
     * @throws IOException
     */
    private Integer validateRetryAndGetUser(Integer entityId, Integer maxRetry, OutBoundInterchange outBoundInterchange) throws IOException {
        if (null == outBoundInterchange) {
            return null;
        }
        if (maxRetry == null) {
            maxRetry = Integer.MAX_VALUE;
        }
        if (maxRetry.equals(outBoundInterchange.getRetryCount())) {
            logger.debug("max retry count reached for request {} for entity {}", outBoundInterchange, entityId);
            return null;
        }
        SapphireProvisioningRequestWS failedProvisioningRequest = OBJECT_MAPPER.readValue(outBoundInterchange.getRequest(),
                SapphireProvisioningRequestWS.class);
        if (null == failedProvisioningRequest.getClientId()) {
            logger.debug("no client id found from request {}", outBoundInterchange);
            return null;
        }
        UserDTO user = userDAS.findNow(failedProvisioningRequest.getClientId());
        if (null == user) {
            logger.debug("no user found for user id {} for entity {}", failedProvisioningRequest.getClientId(), entityId);
            return null;
        }
        return user.getId();
    }

    private void paymentSuccessful(Event event, final Map<String, String> parameters) {
        if (((PaymentSuccessfulEvent) event).isEnrollment()) {
            logger.debug("Triggering activation event!");
            ((PaymentSuccessfulEvent) event).setEnrollment(false);
            activate(event, parameters);
        }
    }

    /**
     * @param event
     * @param parameters
     * @param entityId
     */
    private void activate(Event event, final Map<String, String> parameters) {
        logger.debug("Triggering activation status event!");
        PaymentDTOEx paymentDTOEx = ((PaymentSuccessfulEvent) event).getPayment();
        Map<String, Status> apiResult = triggerRestApi(parameters, event.getEntityId(), ACTIVATION.getRequestType(), paymentDTOEx.getUserId());
        logger.debug("response from activation rest call = {}", apiResult);
    }

    private SapphireProvisioningStatus findStatus(Integer entityId, Integer newStatusId, Integer oldStatusId, Map<String, String> parameters, Integer cancellationStatusId) {
        Integer suspendnedStatusId = Integer.parseInt(getAndValidateParameterValue(parameters,
                SapphireProvisioningTask.PARAM_SUSPENDED_AGEING_STEP_ID.getName(), entityId));
        Integer disconnectedStatusId = Integer.parseInt(getAndValidateParameterValue(parameters,
                SapphireProvisioningTask.PARAM_DISCONNECTED_AGEING_STEP_ID.getName(), entityId));
        Integer terminatedStatusId = Integer.parseInt(getAndValidateParameterValue(parameters,
                SapphireProvisioningTask.PARAM_TERMINATED_AGEING_STEP_ID.getName(), entityId));
        SapphireProvisioningStatus provisioningStatus = SapphireProvisioningStatus.NONE;
        if (suspendnedStatusId.equals(newStatusId)) {
            provisioningStatus = SapphireProvisioningStatus.SUSPENDED;
        } else if(disconnectedStatusId.equals(newStatusId)) {
            provisioningStatus = SapphireProvisioningStatus.DISCONNECTED;
        } else if(terminatedStatusId.equals(newStatusId) || cancellationStatusId.equals(newStatusId)) {
            provisioningStatus = SapphireProvisioningStatus.TERMINATED;
        } else if(suspendnedStatusId.equals(oldStatusId) &&
                UserDTOEx.STATUS_ACTIVE.equals(newStatusId)) {
            provisioningStatus = SapphireProvisioningStatus.REACTIVATION;
        } else if(disconnectedStatusId.equals(oldStatusId) &&
                UserDTOEx.STATUS_ACTIVE.equals(newStatusId)) {
            provisioningStatus = SapphireProvisioningStatus.RECONNECTION;
        }
        return provisioningStatus;
    }

    private void userStatusChanged(Event event, final Map<String, String> parameters) {
        NewUserStatusEvent newUserStatusEvent = (NewUserStatusEvent) event;
        UserDTO user = userDAS.find(newUserStatusEvent.getUserId());
        Integer entityId = event.getEntityId();
        UserStatusDTO cancellationStatus = new UserStatusDAS().findByDescription(Constants.CUSTOMER_CANCELLATION_STATUS_DESCRIPTION, user.getLanguage().getId());
        Integer cancellationStatusId = cancellationStatus.getId();
        SapphireProvisioningStatus status = findStatus(entityId, newUserStatusEvent.getNewStatusId(),
                newUserStatusEvent.getOldStatusId(), parameters, cancellationStatusId);
        if(status.equals(SapphireProvisioningStatus.NONE)) {
            return;
        }
        logger.debug("user {} status changed to {}", user.getId(), status);
        String customerProvisioningMfName = getAndValidateParameterValue(parameters, PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME.getName(), entityId);
        UserProvisioninigStatus userProvisioninigStatus = null;
        SapphireProvisioningRequestType requestType = null;
        if(status.equals(SapphireProvisioningStatus.DISCONNECTED)) {
            userProvisioninigStatus = PENDING_DISCONNECTION;
            requestType = DISCONNECTION;
        } else if(status.equals(SapphireProvisioningStatus.TERMINATED)) {
            userProvisioninigStatus = PENDING_TERMINATION;
            requestType = TERMINATION;
        } else if(status.equals(SapphireProvisioningStatus.SUSPENDED)) {
            userProvisioninigStatus = PENDING_SUSPENSION;
            requestType = SUSPENSION;
        } else if(status.equals(SapphireProvisioningStatus.REACTIVATION)) {
            userProvisioninigStatus = PENDING_REACTIVATION;
            requestType = REACTIVATION;
        } else if(status.equals(SapphireProvisioningStatus.RECONNECTION)) {
            userProvisioninigStatus = PENDING_RECONNECTION;
            requestType = RECONNECTION;
        }
        if(null == requestType || null == userProvisioninigStatus) {
            logger.debug("request type or user provisioning status not found for user {}", user.getId());
            return;
        }
        logger.debug("Triggering {} event!", requestType);
        // update customer provisioning meta field
        updateCustomerProvisioningStatusMf(customerProvisioningMfName, user.getId(), userProvisioninigStatus);
        Map<String, Status> apiResult = triggerRestApi(parameters, entityId, requestType.getRequestType(), user.getId());
        logger.debug("response from {} rest call = {}", requestType, apiResult);
        if(requestType.equals(DISCONNECTION) &&
                apiResult.values().iterator().next().equals(Status.PROCESSED)) {
            Integer feeProductId = Integer.parseInt(getAndValidateParameterValue(parameters,
                    SapphireProvisioningTask.PARAM_DISCONNECTION_FEE_PRODUCT_ID.getName(), entityId));
            Integer orderId = createOrderForUser(user.getId(), feeProductId, entityId);
            logger.debug("fee order {} created for user {}", orderId, user.getId());
        }
    }

    /**
     * Creates Order for given item for user
     * @param userId
     * @param itemId
     * @param entityId
     * @return
     */
    private Integer createOrderForUser(Integer userId, Integer itemId, Integer entityId) {
        try (RunAsUser adminUser = new RunAsCompanyAdmin(entityId)) {
            UserBL userBL = new UserBL(userId);
            UserDTO user = userBL.getEntity();
            // create the order
            OrderWS orderWS = new OrderWS();
            orderWS.setUserId(userId);
            orderWS.setActiveSince(TimezoneHelper.companyCurrentDate(entityId));
            orderWS.setPeriod(Constants.ORDER_PERIOD_ONCE);
            orderWS.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
            orderWS.setCreateDate(Calendar.getInstance().getTime());
            orderWS.setCurrencyId(user.getCurrency().getId());
            orderWS.setNotes("Auto generated reconnection charges order.");
            ItemDTOEx item = api.getItem(itemId, null, null);
            String description = item.getDescription();
            OrderLineWS line = new OrderLineWS();
            line.setItemId(itemId);
            line.setQuantity(BigDecimal.ONE);
            line.setUseItem(true);
            line.setTypeId(item.getOrderLineTypeId());
            line.setDescription(description);
            orderWS.setOrderLines(new OrderLineWS[] { line });
            return api.createUpdateOrder(orderWS, OrderChangeBL.buildFromOrder(orderWS,
                    findApplyOrderChangeStatusForEntity(entityId).getId()));
        }
    }

    private OrderChangeStatusDTO findApplyOrderChangeStatusForEntity(Integer entityId) {
        List<OrderChangeStatusDTO> statusDTOs = new OrderChangeStatusDAS().findOrderChangeStatuses(entityId);
        for(OrderChangeStatusDTO status : statusDTOs) {
            if(status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status;
            }
        }
        throw new SessionInternalError("No order Change Apply status found for entity id "+ entityId);
    }

    /**
     * Validates and return parameter value for given parameter name.
     * @param parameters
     * @param paramName
     * @param entityId
     * @return
     */
    private String getAndValidateParameterValue(Map<String, String> parameters, String paramName, Integer entityId) {
        Assert.hasLength(paramName, "please provide parameter name");
        String paramValue = parameters.get(paramName);
        if(StringUtils.isEmpty(paramValue)) {
            logger.error("{} parameter not configured for plugin {} for entity {}", paramName, SapphireProvisioningTask.class.getSimpleName(), entityId);
            throw new SessionInternalError("parameter "+ paramValue + " not configured for plugin "+
                    SapphireProvisioningTask.class.getSimpleName() + " for entity "+ entityId);
        }
        return paramValue;
    }

    private boolean isAssetReplacement(Map<Integer, Integer> assetMap) {
        for(Entry<Integer, Integer> oldNewAssetEntry : assetMap.entrySet()) {
            AssetDTO oldAsset = assetDAS.find(oldNewAssetEntry.getKey());
            AssetDTO newAsset = assetDAS.find(oldNewAssetEntry.getValue());
            if(!oldAsset.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX) &&
                    !newAsset.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private void deviceSwap(Event event, final Map<String, String> parameters) {
        Integer entityId = event.getEntityId();
        String orderProvisioningMfName = getAndValidateParameterValue(parameters, PARAM_ORDER_PROVISIONING_STATUS_MF_NAME.getName(), entityId);
        if(event instanceof SwapAssetsEvent) {
            SwapAssetsEvent swapAssetsEvent = (SwapAssetsEvent) event;
            logger.debug("processing {}", swapAssetsEvent);
            if(!isAssetReplacement(swapAssetsEvent.getOldNewAssetMap())) {
                return;
            }
            OrderDTO order = orderDAS.findNow(swapAssetsEvent.getOrderId());
            // update order provisioning meta field
            updateOrderProvisioningStatusMf(orderProvisioningMfName, order.getId(), PENDING_DEVICE_SWAP);
            logger.debug("sending device swap reuqest for order {}", order.getId());
            Map<String, Status> apiResult = triggerRestApi(parameters, entityId, DEVICE_SWAP.getRequestType(), order.getUserId());
            logger.debug("response from device-swap rest call = {}", apiResult);
            for(Entry<Integer, Integer> assetOldNewEntry : swapAssetsEvent.getOldNewAssetMap().entrySet()) {
                changeAssetStatusToFinished(assetOldNewEntry.getKey());
            }
        } else if(event instanceof SapphireSwapAssetEvent) {
            SapphireSwapAssetEvent sapphireSwapAssetEvent = (SapphireSwapAssetEvent) event;
            String aitGroupName = getAndValidateParameterValue(parameters, PARAM_AIT_GROUP_NAME.getName(), entityId);
            String serviceMfName = parameters.get(PARAM_SERVICE_ORDER_ID_MF_NAME.getName());
            // creating request from swap asset event.
            SapphireProvisioningRequestWS provisioningRequestWS = createProvisioningRequestFromSapphireSwapEvent(sapphireSwapAssetEvent,
                    aitGroupName, serviceMfName);
            OutBoundInterchange outBoundInterchange = getOutBoundInterchange(provisioningRequestWS,
                    DEVICE_SWAP.getRequestType().toLowerCase(), entityId);
            // sending details to sapphire.
            callRestPostAPI(outBoundInterchange, parameters);
            // update order provisioning meta field
            updateOrderProvisioningStatusMf(orderProvisioningMfName, sapphireSwapAssetEvent.getNewOrderId(), PENDING_DEVICE_SWAP);
            // linking new order to service order.
            linkNewOrderToServiceOrder(sapphireSwapAssetEvent.getOldOrderId(), sapphireSwapAssetEvent.getNewOrderId(), serviceMfName, entityId);
            // update old asset status to finish.
            changeAssetStatusToFinished(sapphireSwapAssetEvent.getOldAssetId());

        }
    }

    @SuppressWarnings("unchecked")
    private void linkNewOrderToServiceOrder(Integer oldOrderId, Integer newOrderId, String serviceMfName, Integer entityId) {
        OrderDTO oldOrder = orderDAS.findNow(oldOrderId);
        OrderDTO newOrder = orderDAS.findNow(newOrderId);
        MetaFieldValue<Integer> oldOrderServiceMfValue = oldOrder.getMetaField(serviceMfName);
        MetaField orderServiceMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, serviceMfName);
        MetaFieldValue<Integer> newOrderServiceMfValue = newOrder.getMetaField(serviceMfName);
        if(null == newOrderServiceMfValue) {
            logger.debug("creating {} on Order {}", serviceMfName, newOrderId);
            newOrderServiceMfValue = new IntegerMetaFieldValue(orderServiceMf);
            newOrder.addMetaField(newOrderServiceMfValue);
        }
        if(null != oldOrderServiceMfValue && !oldOrderServiceMfValue.isEmpty()) {
            Integer oldServiceId = oldOrderServiceMfValue.getValue();
            newOrderServiceMfValue.setValue(oldServiceId);
            logger.debug("set {} with value {} from old order {} to new order {}", serviceMfName, oldServiceId, oldOrderId, newOrderId);
        } else if(null!= oldOrder.getParentOrder()) {
            newOrderServiceMfValue.setValue(oldOrder.getParentOrder().getId());
            logger.debug("set {} parent order {} of old order {} to new order {}",
                    serviceMfName, oldOrder.getParentOrder().getId(), oldOrderId, newOrderId);
        } else {
            logger.debug("new order {} did not link, since {} and no parent order found on old order {}",
                    newOrderId, serviceMfName, oldOrderId);
        }

    }

    /**
     * updates given asset to finished status
     * @param assetId
     */
    private void changeAssetStatusToFinished(Integer assetId) {
        AssetDTO asset = assetDAS.findNow(assetId);
        AssetStatusDTO finishedStatus = new AssetStatusDAS().findOrderFinishedStatusForItem(asset.getItem().getId());
        if(null == finishedStatus) {
            logger.debug("asset finished status not found for asset {}", assetId);
            return;
        }
        asset.setAssetStatus(finishedStatus);
        assetDAS.save(asset);
        logger.debug("Asset {} status updated to finished", assetId);
    }

    private void activateAddOns(Event event, final Map<String, String> parameters) {
        logger.debug("Triggering activate add ons!");
        Integer orderId = ((UpgradeOrderEvent) event).getUpgradeOrderId();
        Map<String, Status> apiResult = triggerRestApi(parameters, event.getEntityId(), ADDON_ACTIVATION.getRequestType(),
                new OrderBL(orderId).getEntity().getUserId());
        logger.debug("response from addon-activation rest call = {}", apiResult);
    }

    private void updateAssetMf(Event event, final Map<String, String> parameters) {
        Integer entityId = event.getEntityId();
        logger.debug("processing event {} for entity {}", event, entityId);
        AssetMetaFieldUpdatedEvent assetMetaFieldUpdatedEvent = (AssetMetaFieldUpdatedEvent) event;
        AssetDTO asset = new AssetDAS().findNow(assetMetaFieldUpdatedEvent.getAssetId());
        if(null == asset.getOrderLine()) {
            logger.debug("skipping external api call since asset not assigned to any user");
            return;
        }
        OrderDTO order = asset.getOrderLine().getPurchaseOrder();
        String orderProvisioningMfName = getAndValidateParameterValue(parameters, PARAM_ORDER_PROVISIONING_STATUS_MF_NAME.getName(), entityId);
        logger.debug("updated asset meta field map {} for asset {}", assetMetaFieldUpdatedEvent.getAssetMetaFieldDiff(), asset.getId());
        ValueDifference<Object> talkAppDiff = assetMetaFieldUpdatedEvent.findMetaFieldValueDiffForName(ASSET_MF_TALK_APP_NAME);
        ValueDifference<Object> exDirectoryDiff = assetMetaFieldUpdatedEvent.findMetaFieldValueDiffForName(ASSET_MF_EX_DIRECTORY_NAME);
        ValueDifference<Object> withHeldDiff = assetMetaFieldUpdatedEvent.findMetaFieldValueDiffForName(ASSET_MF_WITH_HELD_NAME);
        if(null!= talkAppDiff || null!= exDirectoryDiff || null!= withHeldDiff) {
            updateOrderProvisioningStatusMf(orderProvisioningMfName, order.getId(), PENDING_UPDATE_OF_ASSET);
            Integer userId = order.getBaseUserByUserId().getId();
            Map<String, Status> apiResult = triggerRestApi(parameters, entityId, CHANGE_EX_DIRECTORY.getRequestType(), userId);
            logger.debug("response from updateAssetMf rest call = {}", apiResult);
        }
    }

    private void changeOfPlan(Event event, final Map<String, String> parameters) {
        ChangeOfPlanEvent changeOfPlanEvent = (ChangeOfPlanEvent) event;
        OrderDTO order = orderDAS.findNow(changeOfPlanEvent.getOrderId());
        Integer userId = order.getBaseUserByUserId().getId();
        Map<String, Status> apiResult = triggerRestApi(parameters, event.getEntityId(), CHANGE_PLAN.getRequestType(), userId);
        logger.debug("response from CHANGE_PLAN rest call = {}", apiResult);
    }

    private void newSaleEvent(Event event, final Map<String, String> parameters) {
        NewSaleEvent newSaleEvent = (NewSaleEvent) event;
        Integer entityId = newSaleEvent.getEntityId();
        String aitGroupName = getAndValidateParameterValue(parameters, PARAM_AIT_GROUP_NAME.getName(), entityId);
        String orderProvisioningMfName = getAndValidateParameterValue(parameters, PARAM_ORDER_PROVISIONING_STATUS_MF_NAME.getName(), entityId);
        // creating request from new sale event order id.
        SapphireProvisioningRequestType newSale = NEW_SALE;
        SapphireProvisioningRequestWS provisioningRequestWS = createProvisioningRequestFromOrder(newSaleEvent.getOrderId(),
                newSaleEvent.getNewSaleRequest().getPlanOrderId(), aitGroupName, newSale);
        OutBoundInterchange outBoundInterchange = getOutBoundInterchange(provisioningRequestWS, newSale.getRequestType().toLowerCase(), entityId);
        // sending details to sapphire.
        callRestPostAPI(outBoundInterchange, parameters);
        // update order provisioning meta field
        updateOrderProvisioningStatusMf(orderProvisioningMfName, newSaleEvent.getOrderId(), PENDING_NEW_SALE);
    }

    private void changeOfCredentialsAndServiceTransfer(Event event, final Map<String, String> parameters) {
        UpdateCustomerEvent updateCustomerEvent = (UpdateCustomerEvent) event;
        Integer entityId = event.getEntityId();
        logger.debug("processing event {} for customer {}", updateCustomerEvent, updateCustomerEvent.getCustomerId());
        CustomerDTO customer = new CustomerDAS().findNow(updateCustomerEvent.getCustomerId());
        if(null == customer) {
            logger.debug("customer {} not found ", updateCustomerEvent.getCustomerId());
            return;
        }
        String aitGroupName = getAndValidateParameterValue(parameters, PARAM_AIT_GROUP_NAME.getName(), entityId);
        Integer accountTypeId = customer.getAccountType().getId();
        AccountInformationTypeDTO contactInformationAIT = new AccountInformationTypeDAS().findByName(aitGroupName, entityId, accountTypeId);
        if(null == contactInformationAIT) {
            logger.debug("{} not found on account type {} for entity {}", aitGroupName, accountTypeId, entityId);
            return;
        }
        Map<Integer, Map<String, ValueDifference<Object>>> aitDiff = updateCustomerEvent.getOldNewAITValueMapByName();
        Integer groupId = contactInformationAIT.getId();
        Map<String, ValueDifference<Object>> aitMetaFieldValueMap = aitDiff.getOrDefault(groupId, Collections.emptyMap());
        Date effectiveDate = customer.getCurrentEffectiveDateByGroupId(groupId);
        CustomerAccountInfoTypeMetaField email = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.EMAIL,
                groupId, effectiveDate);
        CustomerAccountInfoTypeMetaField phoneNumber = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.PHONE_NUMBER,
                groupId, effectiveDate);
        UserDTO user = customer.getBaseUser();
        String customerProvisioningMfName = getAndValidateParameterValue(parameters, PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME.getName(), entityId);
        if((!user.getUserName().equals(updateCustomerEvent.getOldCustomer().getUserName())) ||
                (null != email && aitMetaFieldValueMap.containsKey(email.getMetaFieldValue().getFieldName())) ||
                (null != phoneNumber && aitMetaFieldValueMap.containsKey(phoneNumber.getMetaFieldValue().getFieldName())) ||
                StringUtils.isNotEmpty(updateCustomerEvent.getOldCustomer().getNewPassword())) {
            // update customer provisioning meta field
            updateCustomerProvisioningStatusMf(customerProvisioningMfName, user.getId(), PENDING_CHANGE_OF_CREDENTIALS);
            Map<String, Status> apiResult = triggerRestApi(parameters, entityId, CHANGE_CREDENTIALS.getRequestType(), user.getId());
            logger.debug("response from CHANGE_CREDENTIALS rest call = {}", apiResult);
        }

        CustomerAccountInfoTypeMetaField address = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.ADDRESS1,
                groupId, effectiveDate);

        CustomerAccountInfoTypeMetaField streetName = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.STREET_NAME,
                groupId, effectiveDate);

        CustomerAccountInfoTypeMetaField city = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.CITY,
                groupId, effectiveDate);

        CustomerAccountInfoTypeMetaField country = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.COUNTRY_NAME,
                groupId, effectiveDate);

        CustomerAccountInfoTypeMetaField postalCode = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.POSTAL_CODE,
                groupId, effectiveDate);

        if((null!= address && aitMetaFieldValueMap.containsKey(address.getMetaFieldValue().getFieldName())) ||
                (null!= streetName && aitMetaFieldValueMap.containsKey(streetName.getMetaFieldValue().getFieldName())) ||
                (null!= city && aitMetaFieldValueMap.containsKey(city.getMetaFieldValue().getFieldName())) ||
                (null!= country && aitMetaFieldValueMap.containsKey(country.getMetaFieldValue().getFieldName())) ||
                (null!= postalCode && aitMetaFieldValueMap.containsKey(postalCode.getMetaFieldValue().getFieldName()))) {
            // update customer provisioning meta field
            updateCustomerProvisioningStatusMf(customerProvisioningMfName, user.getId(), PENDING_FOR_SERVICE_TRANSFER);
            Map<String, Status> apiResult = triggerRestApi(parameters, entityId, SERVICE_TRANSFER.getRequestType(), user.getId());
            logger.debug("response from SERVICE_TRANSFER rest call = {}", apiResult);
        }



    }

    /**
     * @param parameters
     * @param entityId
     * @param methodName
     * @param userId
     * @return
     */
    private Map<String, Status> triggerRestApi(final Map<String, String> parameters, Integer entityId, String methodName, Integer userId) {
        String aitGroupName = getAndValidateParameterValue(parameters, PARAM_AIT_GROUP_NAME.getName(), entityId);
        SapphireProvisioningRequestWS requestWS = mapToProvisioningRequest(userId, methodName, aitGroupName);
        OutBoundInterchange outBoundInterchange = getOutBoundInterchange(requestWS, methodName.toLowerCase(), entityId);
        return Collections.singletonMap(callRestPostAPI(outBoundInterchange, parameters), outBoundInterchange.getStatus());
    }

    private String callRestPostAPI(OutBoundInterchange outBoundInterchange, final Map<String, String> parameters) {
        try {
            String url = parameters.get(PARAM_API_URL.getName());
            validateParameter(PARAM_API_URL.getName(), url);
            url = url.endsWith("/") ? url : (url + "/");
            RestOperations restOperations = configureRestTemplateFromParameters(parameters);
            String userName = parameters.get(PARAM_USER_NAME.getName());
            validateParameter(PARAM_USER_NAME.getName(), userName);
            String password = parameters.get(PARAM_PASSWORD.getName());
            validateParameter(PARAM_PASSWORD.getName(), password);
            HttpEntity<String> request = new HttpEntity<>(outBoundInterchange.getRequest(), createAuthHeader(userName, password));
            ResponseEntity<String> response = restOperations.exchange(url + outBoundInterchange.getMethodName(),
                    outBoundInterchange.getHttpMethod(), request, String.class);
            outBoundInterchange.setResponse(response.getBody());
            boolean hasError = hasError(outBoundInterchange.getResponse());
            outBoundInterchange.setStatus(hasError ? Status.FAILED : Status.PROCESSED);
            logger.debug("response is {}", outBoundInterchange.getResponse());
            return response.getStatusCode() + " : " + response.getBody().toUpperCase();
        } catch (IOException ex) {
            logger.error("failed during response unmarshalling", ex);
            outBoundInterchange.setStatus(Status.UNMARSHALLING_ERROR);
            String errorResponse = ExceptionUtils.getStackTrace(ex);
            outBoundInterchange.setResponse(errorResponse);
            return errorResponse;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorResponse = ExceptionUtils.getStackTrace(e);
            String httpResponse = e.getResponseBodyAsString();
            outBoundInterchange.setResponse(httpResponse);
            outBoundInterchange.setStatus(Status.FAILED);
            logger.debug("response is {}", outBoundInterchange.getResponse());
            return errorResponse + httpResponse;
        } catch (Exception e) {
            String errorResponse = ExceptionUtils.getStackTrace(e);
            outBoundInterchange.setResponse(errorResponse);
            outBoundInterchange.setStatus(Status.FAILED);
            logger.error("request sending failed for Sapphire provisioning api call ", e);
            return errorResponse;
        }
    }

    /**
     * @param entityId
     * @param provisioningRequest
     * @param methodName
     * @return OutBoundInterchange
     */
    private OutBoundInterchange getOutBoundInterchange(SapphireProvisioningRequestWS provisioningRequest, String methodName, Integer entityId) {
        OutBoundInterchange outBoundInterchange = new OutBoundInterchange();
        outBoundInterchange.setCompany(new CompanyDTO(entityId));
        outBoundInterchange.setMethodName(methodName);
        outBoundInterchange.setHttpMethod(HttpMethod.POST);
        try {
            outBoundInterchange.setRequest(OBJECT_MAPPER.writeValueAsString(provisioningRequest));
            outBoundInterchange.setUserId(provisioningRequest.getClientId());
            outBoundInterchange = outBoundInterchangeDAS.save(outBoundInterchange);
            logger.debug("Request {} saved for entity {}", outBoundInterchange, entityId);
            return outBoundInterchange;
        } catch (JsonProcessingException ex) {
            throw new SessionInternalError("provisioning request json string conversion failed ", ex);
        }
    }

    private Map<String, String> collectParametersFromPlugin(Integer pluginId, Integer entityId) {
        PluggableTaskDAS pluggableTaskDAS = Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
        PluggableTaskDTO sapphirePlugin = pluggableTaskDAS.findNow(pluginId);
        Assert.notNull(sapphirePlugin, "SapphireProvisioningTask not configured for entity " + entityId);
        Map<String, String> parameters = new HashMap<>();
        for (PluggableTaskParameterDTO parameterDTO : sapphirePlugin.getParameters()) {
            parameters.put(parameterDTO.getName(), parameterDTO.getValue());
        }
        return parameters;
    }

    private void validateParameter(String paramName, Object paramValue) {
        Assert.notNull(paramValue, String.format("Parameter [%s] is null", paramName));
    }

    /**
     * Creates Auth Header for given credential
     *
     * @return HttpHeaders
     */
    private static HttpHeaders createAuthHeader(String userName, String password) {
        byte[] key = getEncodedKey(userName + ":" + password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Basic " + new String(key));
        return HttpHeaders.readOnlyHttpHeaders(headers);
    }

    private static byte[] getEncodedKey(String key) {
        return Base64.encodeBase64(key.getBytes(StandardCharsets.US_ASCII));
    }

    private static ClientHttpRequestFactory getClientHttpRequestFactory(final int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        return new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().setDefaultRequestConfig(config).build());
    }

    private RestOperations configureRestTemplateFromParameters(final Map<String, String> parameters) {
        return new RestTemplate(getClientHttpRequestFactory(getTimeOut(parameters)));
    }

    private Integer getTimeOut(Map<String, String> parameters) {
        String value = parameters.get(PARAM_TIME_OUT.getName());
        if (StringUtils.isEmpty(value)) {
            value = DEFAULT_TIME_OUT.toString();
        }
        return Integer.parseInt(value);
    }

    private boolean hasError(String response) throws IOException {
        if (response.equalsIgnoreCase("Request processed successfully")) {
            return false;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.readTree(response);
        return responseNode.get("errors").size() != 0;
    }

    @HandleException
    public SwapAssetResponse swapAssets(Integer orderId, SwapAssetWS[] swapRequests) {
        if(null == orderId) {
            logger.error("Order parameter is null");
            throw new SessionInternalError("Please provide non null orderId.", "Order id is null", HttpStatus.SC_BAD_REQUEST);
        }
        if(ArrayUtils.isEmpty(swapRequests)) {
            logger.error("swapRequest parameter is null or empty");
            throw new SessionInternalError("swapRequest is null or empty", "Please enter swapRequest.", HttpStatus.SC_BAD_REQUEST);
        }
        long startTime = System.currentTimeMillis();
        try {
            //validates Order and swapAssetRequest
            validateSwapAssetRequest(orderId, swapRequests);
            OrderWS order = api.getOrder(orderId);
            Integer applyStatusId = getOrderChangeApplyStatus();
            List<Integer> newOrders = new ArrayList<>();
            for(SwapAssetWS swapAsset : swapRequests) {
                for(OrderLineWS line : order.getOrderLines()) {
                    AssetDTO existingAsset = assetDAS.getAssetByIdentifier(swapAsset.getExistingIdentifier());
                    Integer[] assets = line.getAssetIds();
                    if(ArrayUtils.isEmpty(assets) || !ArrayUtils.contains(assets, existingAsset.getId())) {
                        continue;
                    }
                    AssetDTO newAsset = assetDAS.getAssetByIdentifier(swapAsset.getNewIdentifier());

                    UserBL userBL = new UserBL(order.getUserId());
                    ItemDTO newAssetItem = newAsset.getItem();
                    if (!newAssetItem.isStandardAvailability() && !newAssetItem
                            .getAccountTypeAvailability()
                            .contains(userBL.getAccountType())) {
                        throw new SessionInternalError("The item is not available for the selected customer",
                                "Please provide Asset of product which is available for customer", HttpStatus.SC_BAD_REQUEST);
                    }

                    int indexOfExistingAsset = ArrayUtils.indexOf(assets, existingAsset.getId());
                    if(existingAsset.getItem().getId() == newAssetItem.getId()) {
                        logger.debug("replacing old asset {} with new asset {}", existingAsset.getId(), newAsset.getId());
                        assets[indexOfExistingAsset] = newAsset.getId();
                    } else {
                        if(assets.length > 1) {
                            ArrayUtils.remove(assets, indexOfExistingAsset); // remove existing asset.
                            line.setQuantity(line.getQuantityAsDecimal().subtract(BigDecimal.ONE)); // subtract one quantity.
                        } else {
                            line.setDeleted(1); //mark line deleted.
                        }
                        // creating new order for new asset from same product category.
                        OrderWS newAssetOrder = createOrderForAsset(order.getUserId(), newAsset, swapAsset.getAmount());
                        logger.debug("creating new order for asset {}", newAsset.getIdentifier());
                        Integer newAssetOrderId = api.createUpdateOrder(newAssetOrder,
                                OrderChangeBL.buildFromOrder(newAssetOrder, applyStatusId));
                        newOrders.add(newAssetOrderId);
                        EventManager.process(new SapphireSwapAssetEvent(order.getId(), newAssetOrderId,
                                existingAsset.getId(), newAsset.getId(), api.getCallerCompanyId()));
                        logger.debug("order {} created for asset identifer {}", newAssetOrderId, newAsset.getIdentifier());
                    }
                    line.setAssetIds(assets);
                }
            }
            WebServicesSessionSpringBean webSessionSpringBean = (WebServicesSessionSpringBean) api;
            // update existing order.
            webSessionSpringBean.createUpdateOrder(order,
                    OrderChangeBL.buildFromOrder(order, applyStatusId), true);
            return new SwapAssetResponse(orderId, newOrders.toArray(new Integer[0]));
        } catch(SessionInternalError ex) {
            throw ex;
        } catch(Exception ex) {
            throw new SessionInternalError(ex, new String[] {"error in swapAssets"},
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);

        } finally {
            logger.debug("time {} taken to finish swapAssets for user {}", (System.currentTimeMillis() - startTime), orderId);
        }
    }

    /**
     * Process newSaleRequest.
     * @param newSaleRequestWS
     * @return
     */
    @HandleException
    public OrderWS processNewSale(NewSaleRequestWS newSaleRequestWS) {
        Integer userId = newSaleRequestWS.getUserId();
        List<String> errors = new ArrayList<>();
        if(null == userId) {
            errors.add("userId parameter is null, Please enter userId.");
        } else {
            if(!api.userExistsWithId(userId)) {
                throw new SessionInternalError("user id not found for entity " + api.getCallerCompanyId(),
                        new String [] { "Please enter valid user id." }, HttpStatus.SC_NOT_FOUND);
            }
        }
        ProductDetailWS[] productDetails = newSaleRequestWS.getProductDetails();
        if(ArrayUtils.isEmpty(productDetails)) {
            errors.add("please provide productDetails parameter");
        }
        if(CollectionUtils.isNotEmpty(errors)) {
            logger.error("validation failed {}", errors);
            throw new SessionInternalError("validation error", errors.toArray(new String[0]), HttpStatus.SC_BAD_REQUEST);
        }
        errors.addAll(validateProductDetails(productDetails)); //validate product details.
        if(CollectionUtils.isNotEmpty(errors)) {
            logger.error("invalid product details passed, product details validation failed {}", errors);
            throw new SessionInternalError("product details validation failed", errors.toArray(new String[0]), HttpStatus.SC_BAD_REQUEST);
        }
        OrderWS order = createOrderWS(newSaleRequestWS);
        logger.debug("new sale order {} created for user {}", order.getId(), userId);
        // fire newSale event.
        EventManager.process(new NewSaleEvent(order.getId(), api.getCallerCompanyId(), newSaleRequestWS));
        return order;
    }


    /**
     * return true if product details contains plan.
     * @param productDetails
     * @return
     */
    private boolean isNewSaleProductDetailRequestContainsPlan(ProductDetailWS [] productDetails) {
        Integer entityId = api.getCallerCompanyId();
        for(ProductDetailWS productDetail : productDetails) {
            if(itemDAS.findItemByInternalNumber(productDetail.getProductCode(), entityId).isPlan()) {
                return true;
            }
        }
        return false;
    }

    /**
     * return orderPeriod id for given period unit.
     * @param periodUnit
     * @return
     */
    private Integer findOrderPeriodByPeriodUnti(Integer periodUnit) {
        for(OrderPeriodWS orderPeriod : api.getOrderPeriods()) {
            if(orderPeriod.getPeriodUnitId().equals(periodUnit)) {
                return orderPeriod.getId();
            }
        }
        throw new SessionInternalError(new String[] { " no order period found for period unti " + periodUnit });
    }

    /**
     * Create {@link OrderWS} from {@link NewSaleRequestWS} in system.
     * @param newSaleRequestWS
     * @return
     */
    private OrderWS createOrderWS(NewSaleRequestWS newSaleRequestWS) {
        OrderWS order = new OrderWS();
        Date activeSinceDate = newSaleRequestWS.getStartDate();
        if(null == activeSinceDate) {
            activeSinceDate = new Date();
        }
        Integer userId = newSaleRequestWS.getUserId();
        order.setActiveSince(activeSinceDate);
        order.setUserId(userId);
        Integer orderPeriodId = getOrderPeriod(newSaleRequestWS.getOrderPeriod());
        if(orderPeriodId.equals(Constants.ORDER_PERIOD_ONCE)) {
            order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        } else {
            order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        }
        if(isNewSaleProductDetailRequestContainsPlan(newSaleRequestWS.getProductDetails())) {
            orderPeriodId = findOrderPeriodByPeriodUnti(OrderPeriod.MONTHLY.getPeriodUnti());
        }
        order.setPeriod(orderPeriodId);
        if(!orderPeriodId.equals(Constants.ORDER_PERIOD_ONCE)) {
            order.setProrateFlag(Boolean.TRUE);
        }
        Integer entityId = api.getCallerCompanyId();
        UserDTO user = userDAS.find(userId);
        order.setCurrencyId(user.getCurrencyId());
        SapphireHelper.setOrderStatusAsPending(order, entityId);
        List<OrderLineWS> lines = new ArrayList<>();
        // creating orderLines from Product details.
        for(ProductDetailWS productDetail : newSaleRequestWS.getProductDetails()) {
            ItemDTO item = itemDAS.findItemByInternalNumber(productDetail.getProductCode(), entityId);
            OrderLineWS line = new OrderLineWS();
            line.setItemId(item.getId());
            line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            String[] assets = productDetail.getAssetIdentifiers();
            BigDecimal quantity;
            if(!item.isPlan()) {
                if(ArrayUtils.isEmpty(assets)) {
                    quantity = BigDecimal.ONE;
                } else {
                    quantity = new BigDecimal(assets.length);
                    line.setAssetIds(collectAssetsIdFromIdentifiers(assets));
                }
            } else {
                quantity = BigDecimal.ONE;
            }
            line.setQuantity(quantity);
            line.setUseItem(Boolean.TRUE);
            lines.add(line);
        }
        order.setOrderLines(lines.toArray(new OrderLineWS[0]));
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, getOrderChangeApplyStatus());
        for(ProductDetailWS productDetail : newSaleRequestWS.getProductDetails()) {
            ItemDTO item = itemDAS.findItemByInternalNumber(productDetail.getProductCode(), entityId);
            String[] assets = productDetail.getAssetIdentifiers();
            if(!item.isPlan() || ArrayUtils.isEmpty(assets)) {
                continue;
            }
            Map<String, List<Integer>> itemAssetIdMap = itemAssetIdMap(assets);
            logger.debug("Item code assets id map {}", itemAssetIdMap);
            for(OrderChangeWS orderChange : orderChanges) {
                orderChange.setAppliedManually(1);
                if (orderChange.getItemId().intValue() == item.getId()) {
                    List<OrderChangePlanItemWS> orderChangePlanItems = new ArrayList<>();
                    for(PlanItemDTO planItem : SapphireHelper.getAssetEnabledPlanItemsFromSubscriptionItem(item.getId())) {
                        OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                        ItemDTO planBundledItem = planItem.getItem();
                        orderChangePlanItem.setItemId(planBundledItem.getId());
                        orderChangePlanItem.setBundledQuantity(planItem.getBundle().getQuantity().intValue());
                        String bundleItemCode = planBundledItem.getInternalNumber();
                        String planItemCode = item.getInternalNumber();
                        List<Integer> assetIds = itemAssetIdMap.get(bundleItemCode);
                        if(CollectionUtils.isEmpty(assetIds)) {
                            logger.debug("assets not found for plan {}'s bundled item {}", planItemCode, bundleItemCode);
                            throw new SessionInternalError("assets not found", "no asset found for plan "+ planItemCode
                                    + "'s bundled item "+ bundleItemCode, HttpStatus.SC_BAD_REQUEST);
                        }
                        if(assetIds.size()!= orderChangePlanItem.getBundledQuantity()) {
                            logger.debug("insuffcient assets found for plan {}'s bundled item {} quantiy", planItemCode, bundleItemCode);
                            throw new SessionInternalError("insuffcient assets", "asset quantity not euqal to plan "+ planItemCode
                                    + "'s bundled item "+ bundleItemCode + " quantity", HttpStatus.SC_BAD_REQUEST);
                        }
                        logger.debug("Creating order with assets {} for user {}", assetIds, userId);
                        orderChangePlanItem.setAssetIds(ArrayUtils.toPrimitive(assetIds.toArray(new Integer[0])));
                        orderChangePlanItems.add(orderChangePlanItem);
                    }
                    orderChange.setOrderChangePlanItems(orderChangePlanItems.toArray(new OrderChangePlanItemWS[0]));
                }
            }
        }
        return api.getOrder(api.createOrder(order, orderChanges));
    }

    /**
     * creates item code and assets id map.
     * @param assetIdentifiers
     * @return
     */
    private Map<String, List<Integer>> itemAssetIdMap(String[] assetIdentifiers) {
        Map<String, List<Integer>> itemAssetIdMap = new HashMap<>();
        for(String assetIdentifier : assetIdentifiers) {
            AssetDTO asset = assetDAS.getAssetByIdentifier(assetIdentifier);
            ItemDTO assetItem = asset.getItem();
            List<Integer> assets = itemAssetIdMap.getOrDefault(assetItem.getInternalNumber(), new ArrayList<>());
            assets.add(asset.getId());
            itemAssetIdMap.putIfAbsent(assetItem.getInternalNumber(), assets);
        }
        return itemAssetIdMap;
    }

    private Integer[] collectAssetsIdFromIdentifiers(String[] assetIdentifiers) {
        return Arrays.stream(assetIdentifiers)
                .map(assetDAS::getAssetByIdentifier)
                .filter(Objects::nonNull)
                .map(AssetDTO::getId)
                .toArray(Integer[]::new);
    }

    /**
     * get db mapped {@link OrderPeriodDTO} id by {@link OrderPeriod}
     * @param orderPeriod
     * @return
     */
    private Integer getOrderPeriod(OrderPeriod orderPeriod) {
        if(null == orderPeriod || orderPeriod.equals(OrderPeriod.ONETIME)) {
            return Constants.ORDER_PERIOD_ONCE;
        }
        OrderPeriodWS[] orderPeriods = api.getOrderPeriods();
        if(ArrayUtils.isEmpty(orderPeriods)) {
            throw new SessionInternalError(new String[] { " no order period found for entity " + api.getCallerCompanyId() });
        }
        for(OrderPeriodWS orderPeriodWS : orderPeriods) {
            if(orderPeriodWS.getPeriodUnitId().equals(orderPeriod.getPeriodUnti())) {
                return orderPeriodWS.getId();
            }
        }
        throw new SessionInternalError(new String[] { " invalid  " + orderPeriod.name()+ " order period passed "});
    }

    private static final String PRODUCT_NOT_FOUND_MESSAGE =
            "product code %s not found for entity %d";

    private List<String> validateAsset(String assetIdentifier, String productCode) {
        List<String> errors = new ArrayList<>();
        if(StringUtils.isEmpty(assetIdentifier)) {
            errors.add("product code "+ productCode + " is asset enabled and no asset present in request.");
        } else {
            AssetDTO asset = assetDAS.getAssetByIdentifier(assetIdentifier);
            if(null == asset) {
                errors.add("Asset identifer " + assetIdentifier + " for product code "+ productCode + " not found in system");
            } else if(null!= asset.getOrderLine()) {
                errors.add("Asset identifer "+ assetIdentifier + " already assigned to other order");
            }
        }
        return errors;
    }

    /**
     * validates {@link ProductDetailWS}.
     * @param productDetails
     * @return
     */
    private List<String> validateProductDetails(ProductDetailWS[] productDetails) {
        List<String> errors = new ArrayList<>();
        Integer entityId = api.getCallerCompanyId();
        for(ProductDetailWS productDetail : productDetails) {
            String productCode = productDetail.getProductCode();
            if(StringUtils.isEmpty(productCode)) {
                errors.add("productCode parameter is null, Please enter productCode.");
            } else {
                ItemDTO item = itemDAS.findItemByInternalNumber(productCode, entityId);
                if(null == item) {
                    errors.add(String.format(PRODUCT_NOT_FOUND_MESSAGE, productCode, entityId));
                } else {
                    boolean assetValidationFailed = false;
                    String[] assets = productDetail.getAssetIdentifiers();
                    if(ArrayUtils.isNotEmpty(assets) && !item.isPlan() && item.isAssetEnabledItem()) {
                        for(String assetIdentifier : assets) {
                            List<String> assetErrors = validateAsset(assetIdentifier, productCode);
                            errors.addAll(assetErrors);
                            if(!assetErrors.isEmpty()) {
                                assetValidationFailed = true;
                            } else {
                                AssetDTO asset = assetDAS.getAssetByIdentifier(assetIdentifier);
                                if(!asset.belongsToItem(item.getId())) {
                                    errors.add("Asset identifer " + assetIdentifier + " not belongs to product code "+ productCode);
                                    assetValidationFailed = true;
                                }
                            }
                        }
                    }
                    if(!assetValidationFailed) {
                        if(!item.isPlan()) {
                            boolean isAssetEnabledItem = item.isAssetEnabledItem();
                            if(isAssetEnabledItem && ArrayUtils.isEmpty(assets)) {
                                errors.add("product code "+ productCode +
                                        ", is asset enabled and no asset present for it in request");
                            } else if(!isAssetEnabledItem && ArrayUtils.isNotEmpty(assets)) {
                                errors.add("product code "+ productCode +
                                        ", is not asset enabled and asset present for it in request");
                            }
                        } else {
                            PlanDTO plan = item.getPlan();
                            if(plan.doesPlanHaveAssetEnabledItem() && ArrayUtils.isEmpty(assets)) {
                                errors.add("plan "+ productCode + " is asset enabled plan, and no asset found in request");
                            } else {
                                for(String identifier : assets) {
                                    List<String> assetErrors = validateAsset(identifier, productCode);
                                    errors.addAll(assetErrors);
                                    if(assetErrors.isEmpty()) {
                                        AssetDTO asset = assetDAS.getAssetByIdentifier(identifier);
                                        ItemDTO assetItem = asset.getItem();
                                        if(!plan.doesPlanHaveItem(assetItem.getId())) {
                                            errors.add("Asset identifer " + identifier + " does not belong to plan bundle item "+ assetItem.getInternalNumber());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Creates {@link OrderWS} for given UserId and {@link AssetDTO}.
     * @param userId
     * @param asset
     * @return
     */
    private OrderWS createOrderForAsset(Integer userId, AssetDTO asset, String amount) {
        OrderWS order = new OrderWS();
        order.setActiveSince(new Date());
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);
        UserDTO user = userDAS.find(userId);
        order.setCurrencyId(user.getCurrencyId());
        order.setStatusId(orderStatusDAS.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, api.getCallerCompanyId()));
        OrderLineWS line = new OrderLineWS();
        line.setItemId(asset.getItem().getId());
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(BigDecimal.ONE);
        line.setAssetIds(new Integer[] { asset.getId() });
        logger.debug("Asset order line amount {}", amount);
        if (StringUtils.isEmpty(amount)) {
            line.setUseItem(Boolean.TRUE);    
        } else {
            line.setUseItem(Boolean.FALSE);
            line.setDescription(asset.getItem().getDescription(api.getCallerLanguageId()));
            line.setPrice(new BigDecimal(amount));
            line.setAmount(new BigDecimal(amount));
        }
        logger.debug("Line {} created for asset {}", line, asset.getId());
        order.setOrderLines(new OrderLineWS[] { line });
        return order;
    }

    /**
     * Validates {@link SwapAssetWS} request and given order
     * @param orderId
     * @param swapRequests
     */
    private void validateSwapAssetRequest(Integer orderId, SwapAssetWS[] swapRequests) {
        OrderDTO order = orderDAS.findNow(orderId);
        if(null == order) {
            logger.error("order {} not found for entity {}", order, api.getCallerCompanyId());
            throw new SessionInternalError("validation failed", "Please enter a valid order id.", HttpStatus.SC_NOT_FOUND);
        }
        if(!order.containsAssets()) {
            logger.error("order {} has no assets on it", orderId);
            throw new SessionInternalError("validation failed", "order has no assets on it.", HttpStatus.SC_BAD_REQUEST);
        }
        List<String> errors = new ArrayList<>();
        for(SwapAssetWS swapAsset : swapRequests) {
            String existingIdentifier = swapAsset.getExistingIdentifier();
            AssetDTO existingAsset = null;
            AssetDTO newAsset = null;
            if(StringUtils.isEmpty(existingIdentifier)) {
                logger.debug("existingIdentifier parameter is null or empty!");
                errors.add("existingIdentifier parameter is null or empty!");
            } else {
                existingAsset = assetDAS.getAssetByIdentifier(existingIdentifier);
                if(null == existingAsset) {
                    logger.error("Asset Identifier {} not found in system", existingIdentifier);
                    errors.add("Asset Identifier [" + existingIdentifier + "] not found in system");
                }
            }

            String newIdentifier = swapAsset.getNewIdentifier();
            if(StringUtils.isEmpty(newIdentifier)) {
                logger.debug("newIdentifier parameter is null or empty!");
                errors.add("newIdentifier parameter is null or empty!");
            } else {
                newAsset = assetDAS.getAssetByIdentifier(newIdentifier);
                if(null == newAsset) {
                    logger.error("Asset Identifier {} not found in system", newIdentifier);
                    errors.add("Asset Identifier [" + newIdentifier + "] not found in system");
                }
            }

            if(null!= existingAsset && null!= newAsset) {
                if(existingIdentifier.equals(newIdentifier)) {
                    logger.error("Both assets [{}, {}] are equal", existingIdentifier, newIdentifier);
                    String errorMessage = "Both Assets [%s, %s] are equal";
                    throw new SessionInternalError("validation failed", String.format(errorMessage, existingIdentifier, newIdentifier), HttpStatus.SC_BAD_REQUEST);

                }
                if(null!= newAsset.getOrderLine()) {
                    logger.error("Asset {} already assigned to other order", newAsset.getId());
                    throw new SessionInternalError("validation failed", "newIdentifier "+ newAsset.getIdentifier() + " already assigned to other order",
                            HttpStatus.SC_BAD_REQUEST);
                }
                if(!order.isAssetPresent(existingAsset.getId())) {
                    logger.error("Asset identifier {} not found on order {}", existingIdentifier, orderId);
                    errors.add("Asset Identifier [" + existingIdentifier + "] not found on order [" + orderId + "]");
                }
                Integer[] exisitingTypes = existingAsset.getItem().getTypes();
                Integer[] newTypes = newAsset.getItem().getTypes();
                boolean found = false;
                for(Integer newType : newTypes) {
                    if(ArrayUtils.contains(exisitingTypes, newType)) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    logger.debug("existing {} and new {} asset identfiers belongs to different categories [{}, {}]",
                            existingIdentifier, newIdentifier, exisitingTypes, newTypes);
                    String message = "Existing [%s] and New [%s]asset identfiers belongs to different categories [%s, %s]";
                    errors.add(String.format(message, existingIdentifier, newIdentifier, Arrays.toString(exisitingTypes), Arrays.toString(newTypes)));
                }
            }
        }

        if(CollectionUtils.isNotEmpty(errors)) {
            logger.error("SwapAsset Validation failed with errors {}", errors);
            throw new SessionInternalError("validation failed",
                    errors.toArray(new String[0]), HttpStatus.SC_BAD_REQUEST);
        }
    }

    private Integer getOrderChangeApplyStatus() {
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        for(OrderChangeStatusWS orderChangeStatus : list) {
            if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return orderChangeStatus.getId();
            }
        }
        throw new SessionInternalError("No order Change status found!");
    }

    private static final String FIND_ASSETS_BY_CATEGROY_SQL =
            "SELECT a.id, a.identifier,a.create_datetime, a.item_id, i.internal_number, aa.start_datetime, i_d.content, ol.order_id from asset a "
                    + "INNER JOIN item i ON a.item_id = i.id  "
                    + "INNER JOIN international_description i_d ON i_d.foreign_id = i.id  "
                    + "AND i_d.language_id = ?  AND i_d.table_id = (SELECT id FROM jbilling_table WHERE name = 'item') "
                    + "AND i_d.psudo_column = 'description' "
                    + "LEFT JOIN asset_assignment aa ON aa.asset_id = a.id "
                    + "AND aa.end_datetime IS NULL LEFT JOIN order_line ol ON ol.id = a.order_line_id "
                    + "WHERE a.status_id = ? AND i.id in (SELECT item_id FROM item_type_map WHERE type_id = ?) "
                    + "AND a.deleted = 0 ORDER BY a.id DESC LIMIT ? OFFSET ?";

    private static final String FIND_ASSET_METAFIELD_VALUE_SQL =
            "select amfm.asset_id, mfv.string_value, mfv.integer_value, mfv.boolean_value, mfv.decimal_value, mfv.date_value, mfn.name "
                    + "from asset_meta_field_map amfm, meta_field_value mfv, meta_field_name mfn where amfm.meta_field_value_id = mfv.id  "
                    + "and mfv.meta_field_name_id = mfn.id and amfm.asset_id in "
                    + "(SELECT a.id from asset a INNER JOIN item i ON a.item_id = i.id "
                    + "INNER JOIN international_description i_d ON i_d.foreign_id = i.id AND "
                    + "i_d.language_id = ? AND i_d.table_id = "
                    + "(SELECT id FROM jbilling_table WHERE name = 'item') "
                    + "AND i_d.psudo_column = 'description' LEFT JOIN asset_assignment aa "
                    + "ON aa.asset_id = a.id AND aa.end_datetime IS NULL LEFT JOIN "
                    + "order_line ol ON ol.id = a.order_line_id WHERE a.status_id = ? "
                    + "AND i.id in (SELECT item_id FROM item_type_map WHERE type_id = ?) "
                    + "AND a.deleted = 0 ORDER BY a.id DESC LIMIT ? OFFSET ? )";

    /**
     * finds assets by category and given status.
     * @param categoryId
     * @param assetStatus
     * @param limit
     * @param offset
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    @HandleException
    public SapphireResponseWS[] getAssetsByCategoryAndStatus(Integer categoryId, String assetStatus, Integer limit, Integer offset) {
        long startTime = System.currentTimeMillis();
        if(null == categoryId) {
            logger.error("categoryId {}, is null", categoryId);
            throw new SessionInternalError("Please provide categoryId id parameter", new String [] { "Please enter categoryId." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(StringUtils.isEmpty(assetStatus)) {
            logger.error("assetStatus is null");
            throw new SessionInternalError("Please provide assetStatus parameter", new String [] { "Please enter assetStatus." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        validOffsetAndLimit(offset, limit);
        SapphireResponseWS[] result = null;
        try {
            ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
            if(!itemTypeDAS.isIdPersisted(categoryId)) {
                logger.error("Invalid category id {}, passed", categoryId);
                throw new SessionInternalError("Invalid category id passed ", new String [] { "Please enter valid category id."},
                        HttpStatus.SC_NOT_FOUND);
            }
            ItemTypeDTO itemType = itemTypeDAS.find(categoryId);
            Integer languageId = api.getCallerLanguageId();
            long statusFetchTime = System.currentTimeMillis();
            Map<String, Integer> assetStatusMap = itemType
                    .getAssetStatuses()
                    .stream()
                    .collect(Collectors.toMap(status-> status.getDescription(languageId), AssetStatusDTO::getId));
            logger.debug("time {} taken to create assetStatusMap in milliseconds", (System.currentTimeMillis() - statusFetchTime));
            logger.debug("collected assetStatuses {} for Category {}", assetStatusMap, categoryId);
            if(!assetStatusMap.containsKey(assetStatus)) {
                logger.error("Invalid asset status {}, passed", assetStatus);
                throw new SessionInternalError("Invalid asset status passed ", new String [] { "Please enter valid assetStatus.",
                        "Asset status " + assetStatus + " not present on category " + categoryId},
                        HttpStatus.SC_BAD_REQUEST);
            }
            long assetFetchTime = System.currentTimeMillis();
            // fetch asset, item, asset_assignment, order_line data.
            List<SapphireResponseWS> assets = jdbcTemplate.query(FIND_ASSETS_BY_CATEGROY_SQL, (rs, rowNum) -> {
                Map<String, Object> assetFieldAndValue = new HashMap<>();
                assetFieldAndValue.put("id", rs.getInt(1));
                assetFieldAndValue.put("identifier", rs.getString(2));
                assetFieldAndValue.put("createDatetime", rs.getDate(3));
                assetFieldAndValue.put("itemId", rs.getInt(4));
                assetFieldAndValue.put("productCode", rs.getString(5));
                assetFieldAndValue.put("categoryId", categoryId);
                assetFieldAndValue.put("status", assetStatus);
                assetFieldAndValue.put("startTime", rs.getDate(6));
                assetFieldAndValue.put("itemDescription", rs.getString(7));
                assetFieldAndValue.put("orderId", rs.getObject(8));
                return new SapphireResponseWS("Asset", assetFieldAndValue);
            }, languageId, assetStatusMap.get(assetStatus), categoryId, limit, offset);
            logger.debug("time taken {} to fetch assets", (System.currentTimeMillis() - assetFetchTime));

            long assetMetaFieldFetch = System.currentTimeMillis();
            // fetch asset meta field values.
            List<Map<String, Object>> assetMetaFieldValueRows =  jdbcTemplate.queryForList(FIND_ASSET_METAFIELD_VALUE_SQL,
                    languageId, assetStatusMap.get(assetStatus), categoryId, limit, offset);

            Map<Integer, Map<String, Object>> assetMetaFieldValueMap = new HashMap<>();
            for(Map<String, Object> assetMetaFieldValueRow : assetMetaFieldValueRows) {
                Integer assetId = (Integer) assetMetaFieldValueRow.get("asset_id");
                assetMetaFieldValueRow.remove("asset_id");
                for(Entry<String, Object> assetMfRowEntry: assetMetaFieldValueRow.entrySet()) {
                    Map<String, Object> assetMfValueMap = assetMetaFieldValueMap.get(assetId);
                    if(null == assetMfValueMap) {
                        assetMfValueMap = new HashMap<>();
                        assetMetaFieldValueMap.put(assetId, assetMfValueMap);
                    }
                    if(null != assetMfRowEntry.getValue()) {
                        assetMfValueMap.put((String)assetMetaFieldValueRow.get("name"), assetMfRowEntry.getValue());
                        break;
                    }
                }
            }
            for(SapphireResponseWS asset : assets) {
                Map<String, Object> assetFields = asset.getEntityFields();
                Integer assetId = (Integer) assetFields.get("id");
                asset.getEntityFields().put("metaFieldsMap", assetMetaFieldValueMap.get(assetId));
            }
            logger.debug("time taken {} to fetch asset meta field", (System.currentTimeMillis() - assetMetaFieldFetch));
            result = assets.toArray(new SapphireResponseWS[0]);
            return result;
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            throw new SessionInternalError("Error in getAssetsByCategoryAndStatus", ex);
        } finally {
            logger.debug("time {} taken to fetch assets {} in milliseconds", (System.currentTimeMillis() - startTime),
                    null == result ? 0 : result.length);
        }
    }

    private void validOffsetAndLimit(Integer offset, Integer limit) {
        if (null != offset && offset.intValue() < 0) {
            throw new SessionInternalError("Offset value can not be negative number.", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != limit && limit.intValue() < 0) {
            throw new SessionInternalError("Limit value must be greater than zero.", HttpStatus.SC_BAD_REQUEST);
        }
    }
}
