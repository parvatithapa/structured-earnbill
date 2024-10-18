package com.sapienter.jbilling.server.sapphire.provisioninig;

import static com.sapienter.jbilling.server.sapphire.SapphireConstants.ASSET_MF_EX_DIRECTORY_NAME;
import static com.sapienter.jbilling.server.sapphire.SapphireConstants.ASSET_MF_TALK_APP_NAME;
import static com.sapienter.jbilling.server.sapphire.SapphireConstants.ASSET_MF_WITH_HELD_NAME;
import static com.sapienter.jbilling.server.sapphire.provisioninig.OrderProvisioninigStatus.PENDING_DEVICE_SWAP;
import static com.sapienter.jbilling.server.sapphire.provisioninig.OrderProvisioninigStatus.PENDING_UPDATE_OF_ASSET;
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
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
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
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDAS;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.event.AssetMetaFieldUpdatedEvent;
import com.sapienter.jbilling.server.item.event.SwapAssetsEvent;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.event.OrderMetaFieldUpdateEvent;
import com.sapienter.jbilling.server.order.event.UpgradeOrderEvent;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanEvent;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanRequestWS;
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
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * @author Krunal Bhavsar
 * @author Ashwinkumar Patra
 */
@Transactional
public class SapphireProvisioningHelperService {

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

    @Autowired
    private OutBoundInterchangeDAS outBoundInterchangeDAS;
    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;
    @Resource
    private OrderDAS orderDAS;

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
        case "OrderMetaFieldUpdateEvent":
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

        try {
            OrderDTO order = orderDAS.findNow(changeOfPlanRequest.getOrderId());
            if(null == order || order.getDeleted() == 1) {
                logger.error("invalid order id {} passed", changeOfPlanRequest.getOrderId());
                throw new SessionInternalError("Please provide valid order id parameter",
                        new String [] { "Please enter valid order id." }, HttpStatus.SC_NOT_FOUND);
            }
            ItemDAS itemDAS = new ItemDAS();
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
        UserDTO user = new UserDAS().findNow(failedProvisioningRequest.getClientId());
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

    private SapphireProvisioningStatus findStatus(Integer entityId, Integer newStatusId, Integer oldStatusId, Map<String, String> parameters) {
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
        } else if(terminatedStatusId.equals(newStatusId)) {
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
        UserDTO user = new UserDAS().find(newUserStatusEvent.getUserId());
        Integer entityId = event.getEntityId();
        SapphireProvisioningStatus status = findStatus(entityId, newUserStatusEvent.getNewStatusId(),
                newUserStatusEvent.getOldStatusId(), parameters);
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
        AssetDAS assetDAS = new AssetDAS();
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
            for(Entry<Integer, Integer> assetOldNewEntry : swapAssetsEvent.getOldNewAssetMap().entrySet()) {
                changeAssetStatusToFinished(assetOldNewEntry.getKey());
            }
        } else if(event instanceof OrderMetaFieldUpdateEvent) {
            OrderMetaFieldUpdateEvent orderMetaFieldUpdateEvent = (OrderMetaFieldUpdateEvent) event;
            logger.debug("processing {}", orderMetaFieldUpdateEvent);
            OrderDTO order = orderDAS.findNow(orderMetaFieldUpdateEvent.getOrderId());
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> provisioningStatus = order.getMetaField(orderProvisioningMfName);
            if(null == provisioningStatus) {
                logger.debug("skipping external system call since meta field {} not configured on order level for entity {}",
                        orderProvisioningMfName, entityId);
                return;
            }
            if(PENDING_DEVICE_SWAP.getStatus().equals(provisioningStatus.getValue())) {
                logger.debug("sending device swap reuqest for order {}", order.getId());
                Map<String, Status> apiResult = triggerRestApi(parameters, entityId, DEVICE_SWAP.getRequestType(), order.getUserId());
                logger.debug("response from device-swap rest call = {}", apiResult);
            }
        }
    }

    /**
     * updates given asset to finished status
     * @param assetId
     */
    private void changeAssetStatusToFinished(Integer assetId) {
        AssetDAS assetDAS = new AssetDAS();
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
        } catch (JsonProcessingException ex) {
            logger.error("provisioning request json string conversion failed ", ex);
            throw new SessionInternalError("provisioning request json string conversion failed ", ex);
        }
        return outBoundInterchange;
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
        return Base64.encodeBase64(key.getBytes(Charset.forName("US-ASCII")));
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
}
