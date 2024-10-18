/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.payment.tasks;

import static com.sapienter.jbilling.paymentUrl.util.PaymentUrlConstants.*;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;

import com.cashfree.model.UpiAdvanceResponseSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sapienter.jbilling.paymentUrl.client.PaymentGatewayClient;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDAS;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlType;
import com.sapienter.jbilling.paymentUrl.db.Status;
import com.sapienter.jbilling.paymentUrl.domain.PaymentConfiguration;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.IPaymentWebHookHandler;
import com.sapienter.jbilling.server.payment.WebHookHandlerUtil;
import com.sapienter.jbilling.server.payment.event.PaymentUrlInitiatedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUrlRegenerateEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import io.jsonwebtoken.lang.Collections;

/**
 * This Task will be invoked in case of 3 events
 * - InvoicesGeneratedEvent
 * - PaymentUrlInitiatedEvent
 * - InvoiceDeletedEvent
 * - PaymentUrlRegenerateEvent
 * This task is responsible for Generating the Payment URL Link based on above events,
 * and the payment url links will be used by Customers for performing payments.
 *
 * @author Ashwinkumar Patra
 * @since 30/11/2023
 */
public class GeneratePaymentURLTask extends PluggableTask
    implements IInternalEventsTask {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String WEBHOOK_HANDLER_INTERFACE_NAME = "com.sapienter.jbilling.server.payment.IPaymentWebHookHandler";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, Object>> TYPE_REF = new TypeReference<HashMap<String, Object>>() {};

    public static final ParameterDescription PARAMETER_PROVIDER_URL =
        new ParameterDescription(PROVIDER_URL, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_CALLBACK_URL =
        new ParameterDescription(CALLBACK_URL, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_REDIRECT_URL =
        new ParameterDescription(REDIRECT_URL, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_SALT_INDEX =
        new ParameterDescription(SALT_INDEX, true, ParameterDescription.Type.INT);
    public static final ParameterDescription PARAMETER_SALT_KEY =
        new ParameterDescription(SALT_KEY, true, ParameterDescription.Type.STR, true);
    public static final ParameterDescription PARAMETER_SHOULD_PUBLISH_EVENTS =
        new ParameterDescription(SHOULD_PUBLISH_EVENTS, true, ParameterDescription.Type.BOOLEAN);
    public static final ParameterDescription PARAMETER_MERCHANT_ID =
        new ParameterDescription(MERCHANT_ID, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_VERIFICATION_CLIENT_ID =
        new ParameterDescription(VERIFICATION_CLIENT_ID, false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_VERIFICATION_CLIENT_SECRET =
        new ParameterDescription(VERIFICATION_CLIENT_SECRET, false, ParameterDescription.Type.STR, true);
    public static final ParameterDescription PARAMETER_ENVIRONMENT =
        new ParameterDescription(ENVIRONMENT, true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_PROVIDER_NAME =
        new ParameterDescription(PROVIDER_NAME, true, ParameterDescription.Type.STR);

    public static final ParameterDescription PARAMETER_SHOULD_NOTIFY_BY_GATEWAY =
        new ParameterDescription(SHOULD_NOTIFY_BY_GATEWAY, true, ParameterDescription.Type.BOOLEAN, "true");
    public static final ParameterDescription PARAMETER_API_KEY =
        new ParameterDescription(API_KEY, true, ParameterDescription.Type.STR, true);
    public static final List<Status> FAILED_STATUSES = Arrays.asList(Status.FAILED, Status.EXPIRED, Status.CANCELLED, Status.TIMEOUT);

    {
        descriptions.add(PARAMETER_PROVIDER_URL);
        descriptions.add(PARAMETER_CALLBACK_URL);
        descriptions.add(PARAMETER_REDIRECT_URL);
        descriptions.add(PARAMETER_SALT_INDEX);
        descriptions.add(PARAMETER_SALT_KEY);
        descriptions.add(PARAMETER_SHOULD_PUBLISH_EVENTS);
        descriptions.add(PARAMETER_MERCHANT_ID);
        descriptions.add(PARAMETER_ENVIRONMENT);
        descriptions.add(PARAMETER_PROVIDER_NAME);
        descriptions.add(PARAMETER_SHOULD_NOTIFY_BY_GATEWAY);
        descriptions.add(PARAMETER_API_KEY);
        descriptions.add(PARAMETER_VERIFICATION_CLIENT_ID);
        descriptions.add(PARAMETER_VERIFICATION_CLIENT_SECRET);
    }

    private String url;
    private String callbackUrl;
    private String redirectUrl;
    private String saltIndex;
    private String saltKey;
    private String shouldPublishEvents;
    private String merchantId;
    private String environment;
    private String providerName;
    private String apiKey;

    private static final Class<Event> events[] = new Class[]{
        InvoicesGeneratedEvent.class,
        PaymentUrlInitiatedEvent.class,
        InvoiceDeletedEvent.class,
        PaymentUrlRegenerateEvent.class
    };

    private final PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();

    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

    /**
     * This method will process the events and based on the type of events further processing will be done.
     *
     * @param event
     * @throws PluggableTaskException
     */
    public void process (Event event) throws PluggableTaskException {
        logger.debug("GeneratePaymentURLTask.process");
        if (event instanceof InvoiceDeletedEvent) {
            InvoiceDeletedEvent instantiatedEvent = (InvoiceDeletedEvent) event;
            deletePaymentUrl(instantiatedEvent.getInvoice());
        } else if (event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent instantiatedEvent = (InvoicesGeneratedEvent) event;
            List<Integer> invoiceIds = instantiatedEvent.getInvoiceIds();
            Integer entityId = instantiatedEvent.getEntityId();
            if(!Collections.isEmpty(invoiceIds)) {
                invoiceIds.stream().forEach(invoiceId -> {
                    InvoiceDTO invoiceDTO = new InvoiceDAS().findNow(invoiceId);
                    String orderIds = invoiceDTO.getOrderProcesses()
                        .stream()
                        .map(orderProcessDTO -> orderProcessDTO.getPurchaseOrder().getId())
                        .collect(Collectors.toSet()).toString();
                    Set<Integer> paymentUrlIds = invoiceDTO.getOrderProcesses()
                        .stream()
                        .map(orderProcessDTO -> {
                            String notes = orderProcessDTO.getPurchaseOrder().getNotes();
                            if (null != notes && notes.contains("Order generated for PaymentURL with id :")) {
                                String[] split = notes.split(":");
                                if (split.length > 1) {
                                    return Integer.valueOf(split[1]);
                                }
                            }
                            return null;
                        }).filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                    if (paymentUrlIds.isEmpty()) {
                        createPaymentUrlForInvoice(invoiceDTO, entityId, orderIds, false);
                    }
                });
            }
        } else if (event instanceof PaymentUrlInitiatedEvent) {
            PaymentUrlInitiatedEvent instantiatedEvent = (PaymentUrlInitiatedEvent) event;
            Integer paymentUrlLogId = instantiatedEvent.getPaymentUrlLogId();
            generateLinkForPaymentUrl(paymentUrlLogId);
        } else if (event instanceof PaymentUrlRegenerateEvent) {
            PaymentUrlRegenerateEvent instantiatedEvent = (PaymentUrlRegenerateEvent) event;
            Integer invoiceId = instantiatedEvent.getInvoiceId();
            InvoiceDTO invoiceDTO = new InvoiceDAS().findNow(invoiceId);
            String orderIds = invoiceDTO.getOrderProcesses()
                    .stream()
                    .map(orderProcessDTO -> orderProcessDTO.getPurchaseOrder().getId())
                    .collect(Collectors.toSet()).toString();
            createPaymentUrlForInvoice(invoiceDTO, instantiatedEvent.getEntityId(), orderIds, true);
        } else {
            throw new PluggableTaskException("Unknown event: " + event.getClass());
        }
    }

    /**
     * This method is responsible for providing the Provider URL stored in the Configured Plugin
     * @return payment provider url
     */
    public String getPaymentProviderUrl() {
        if (url == null) {
            url = parameters.get(PARAMETER_PROVIDER_URL.getName());
        }
        return url;
    }

    /**
     * This method is responsible to check the payment status of the provided merchantTransactionId
     * @param paymentLogUrlLogId
     * @return
     */
    public PaymentResponse checkPaymentStatus(Integer paymentLogUrlLogId) {
        try {
            PaymentUrlLogDTO paymentUrlLogDTO = paymentUrlLogDAS.findNow(paymentLogUrlLogId);
            if(null == paymentUrlLogDTO) {
                logger.debug("payment url for paymentLogUrlLogId={}, not generated ", paymentLogUrlLogId);
                return null;
            }
            logger.debug("check if we have already received the payment status");
            if (Status.SUCCESSFUL == paymentUrlLogDTO.getStatus()) {
                String paymentStatusResponse = paymentUrlLogDTO.getPaymentStatusResponse();
                return new Gson().fromJson(paymentStatusResponse, PaymentResponse.class);
            }
            logger.debug("Checking Payment Status for paymentLogUrlLogId = {}", paymentLogUrlLogId);
            PaymentConfiguration paymentConfiguration = buildPaymentConfiguration();
            Map<String, Object> paymentStatusMetaData = buildPaymentMetaData(paymentUrlLogDTO.getGatewayId());
            paymentConfiguration.setPaymentData(paymentStatusMetaData);

            PaymentGatewayClient paymentGatewayClient = new PaymentGatewayClient(getPaymentProviderUrl(), getParameter(API_KEY), getEntityId());
            PaymentResponse response = paymentGatewayClient.checkPaymentStatus(paymentConfiguration);
            String responseStr = null != response ? new Gson().toJson(response) : "no response";
            logger.debug("Response received from the checkPaymentStatus API call {} ",responseStr);

            if (WebHookHandlerUtil.SUCCESS_RESPONSES.contains(response.getResponseCode())) {
                Integer categoryId = new PluggableTaskTypeCategoryDAS().findByInterfaceName(WEBHOOK_HANDLER_INTERFACE_NAME).getId();
                PluggableTaskManager<IPaymentWebHookHandler> taskManager = new PluggableTaskManager<>(getEntityId(), categoryId);
                IPaymentWebHookHandler task = taskManager.getNextClass();
                Integer accountTypeId = null;
                while (null != task) {
                    if (task.gatewayName().equalsIgnoreCase(getParameter(PROVIDER_NAME))) {
                        accountTypeId = task.getAccountTypeId();
                    }
                    task = taskManager.getNextClass(); // fetch next task.
                }
                new WebHookHandlerUtil(getParameter(PROVIDER_NAME).toUpperCase(), accountTypeId).create(response, null, getEntityId());
            } else {
                PaymentUrlLogDTO paymentUrlLogToUpdate = paymentUrlLogDAS.findForUpdate(paymentLogUrlLogId);
                paymentUrlLogToUpdate.setStatus(Status.valueOf(response.getResponseCode()));
                paymentUrlLogDAS.save(paymentUrlLogToUpdate);
            }
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * This method is responsible to fetch the verification data based on UPI ID
     *
     * @param payerVPA
     * @return
     */
    public UpiAdvanceResponseSchema getVerificationData(String payerVPA) {
        try {
            PaymentConfiguration paymentConfiguration = buildVerificationConfiguration();
            Map<String, Object> authorizationMetaData = new HashMap<>();
            authorizationMetaData.put("payerVpa", payerVPA);
            paymentConfiguration.setPaymentData(authorizationMetaData);

            PaymentGatewayClient paymentGatewayClient = new PaymentGatewayClient(getPaymentProviderUrl(), getParameter(API_KEY), getEntityId());
            UpiAdvanceResponseSchema response = paymentGatewayClient.getVerificationData(paymentConfiguration);
            String responseStr = null != response ? new Gson().toJson(response) : "no response";
            logger.debug("Response received from the checkPaymentStatus API call {} ", responseStr);
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * This method is responsible for generating Payment URL based on various scenarios for Invoice.
     * This method will check if payment url is already generated for the invoice,
     * if not then it will create a new payment method url and trigger the payment gateway to generate the payment url
     * if payment url is already present then it will check the status of the payment url dto object and if regenerate payment url event is triggered
     * if regenerate event is triggered then it will generate a new payment url if the earlier payment url status is in failed status
     * @param invoice
     * @param entityId
     * @param orderIds
     * @param isRegenerate
     */
    private void createPaymentUrlForInvoice(InvoiceDTO invoice, Integer entityId, String orderIds, boolean isRegenerate) {
        try {
            int invoiceId = invoice.getId();
            if (null == invoice.getTotal() || BigDecimal.ZERO.equals(invoice.getTotal())) {
                logger.debug("Invoice total is null or ZERO for invoiceId={}", invoiceId);
                return;
            }
            PaymentUrlLogDTO paymentUrlLogDTO = null;
            List<PaymentUrlLogDTO> paymentUrlLogDTOS = paymentUrlLogDAS.findAllByInvoiceId(invoiceId);
            UserDTO baseUser = invoice.getBaseUser();
            if (CollectionUtils.isNotEmpty(paymentUrlLogDTOS)) {
                int numberOfPaymentUrls = paymentUrlLogDTOS.size();
                if (numberOfPaymentUrls == 1) {
                    paymentUrlLogDTO = paymentUrlLogDTOS.get(0);
                    if (!isRegenerate) {
                        logger.debug("payment url for merchantTransactionId={}, has been generated and is in {} status ", paymentUrlLogDTO.getGatewayId(), paymentUrlLogDTO.getStatus());
                        return;
                    }
                }
                if (numberOfPaymentUrls > 1) {
                    paymentUrlLogDTO = paymentUrlLogDTOS.get(numberOfPaymentUrls - 1);
                    PaymentUrlLogDTO previousPaymentUrlLogDTO = paymentUrlLogDTOS.get(numberOfPaymentUrls - 2);
                    if (isRegenerate) {
                        Status status = previousPaymentUrlLogDTO.getStatus();
                        if (!FAILED_STATUSES.contains(status)) {
                            logger.debug("payment url for merchantTransactionId={}, has been generated and the previous payment url is in {} status ", paymentUrlLogDTO.getGatewayId(), status);
                            return;
                        }
                    }
                }
            }
            if (null == paymentUrlLogDTO) {
                paymentUrlLogDTO = new PaymentUrlLogDTO();
                paymentUrlLogDTO.setCreatedAt(new Date());
                paymentUrlLogDTO.setStatus(Status.INITIATED);
                paymentUrlLogDTO.setEntityId(entityId);
                paymentUrlLogDTO.setGatewayId(entityId + "-" + System.currentTimeMillis());
            }
            paymentUrlLogDTO.setPaymentAmount(invoice.getTotal());
            paymentUrlLogDTO.setInvoiceId(invoiceId);
            boolean shouldNotifyByGateway = Boolean.parseBoolean(getParameter(SHOULD_NOTIFY_BY_GATEWAY));
            paymentUrlLogDTO.setPaymentUrlType(shouldNotifyByGateway ? PaymentUrlType.UPI : PaymentUrlType.LINK);

            logger.debug("Generating Payment URL for merchantTransactionId = {}", invoiceId);
            PaymentUrlLogDTO dto = paymentUrlLogDAS.save(paymentUrlLogDTO);
            Map<String, Object> paymentRequestMetaData = buildPaymentMetaData(paymentUrlLogDTO.getGatewayId(), invoice.getTotal(), baseUser.getUserName(), orderIds, baseUser.getId(), paymentUrlLogDTO.getPaymentUrlType(), "Payment for Invoice id - " + invoiceId);
            paymentRequestMetaData.put(SHOULD_NOTIFY_BY_GATEWAY, shouldNotifyByGateway);
            MetaFieldValueWS[] metaFields = new UserBL(baseUser.getId()).getUserWS().getMetaFields();
                for(MetaFieldValueWS mf :metaFields)
                {
                if(mf.getMetaField().getFieldUsage().equals(MetaFieldType.PHONE_NUMBER)) {
                    paymentRequestMetaData.put("customerPhone", mf.getStringValue());
                }
                if(mf.getMetaField().getFieldUsage().equals(MetaFieldType.EMAIL)) {
                    paymentRequestMetaData.put("customerEmail", mf.getStringValue());
                }
            }
            generatePaymentUrl(paymentRequestMetaData, dto);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * this method is responsible to trigger the payment gateway to generate the payment url
     */
    private void generateLinkForPaymentUrl(Integer paymentUrlLogId) {
        try {
            PaymentUrlLogDTO paymentUrlLogDTO = paymentUrlLogDAS.findNow(paymentUrlLogId);
            if (null == paymentUrlLogDTO) {
                logger.debug("payment url for paymentUrlLogId={}, not found ", paymentUrlLogId);
                return;
            }
            logger.debug("Generating Payment URL for paymentUrlLogId = {}", paymentUrlLogId);
            String mobileRequestPayload = paymentUrlLogDTO.getMobileRequestPayload();
            Map<String, Object> map = OBJECT_MAPPER.readValue(mobileRequestPayload, TYPE_REF);
            Map<String, Object> paymentRequestMetaData = buildPaymentMetaData(paymentUrlLogDTO.getGatewayId(), paymentUrlLogDTO.getPaymentAmount(), (String) map.get("name"), null, null, paymentUrlLogDTO.getPaymentUrlType(), "Payment for - " + paymentUrlLogDTO.getGatewayId());
            generatePaymentUrl(paymentRequestMetaData, paymentUrlLogDTO);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Helper method to trigger the payment gateway client to generate payment url link
     * @param paymentRequestMetaData
     * @param paymentUrlLogDTO
     * @throws JobExecutionException
     */
    private void generatePaymentUrl(Map<String, Object> paymentRequestMetaData, PaymentUrlLogDTO paymentUrlLogDTO) throws JobExecutionException {
        PaymentConfiguration paymentConfiguration = buildPaymentConfiguration();
        paymentConfiguration.setPaymentData(paymentRequestMetaData);
        PaymentGatewayClient paymentGatewayClient = new PaymentGatewayClient(getPaymentProviderUrl(), getParameter(API_KEY), getEntityId());
        try {
            PaymentResponse response = paymentGatewayClient.generatePaymentLink(paymentConfiguration, paymentUrlLogDTO.getPaymentUrlType());
            if( null != response ) {
                String responseStr = new Gson().toJson(response);
                logger.debug("Response received from the generateUPILink API call {} ", responseStr);
                Map<String, String> metaData = response.getMetaData();
                Boolean responseStatus = response.getResponseStatus();
                if( null != metaData ) {
                    paymentUrlLogDTO.setPaymentUrl(responseStatus ? metaData.get(INTENT_URL) : null);
                    paymentUrlLogDTO.setPaymentUrlRequestPayload(paymentRequestMetaData.toString());
                    paymentUrlLogDTO.setPaymentUrlResponse(responseStr);
                    paymentUrlLogDTO.setPaymentProvider(paymentConfiguration.getPaymentConfig().get(PROVIDER_NAME));
                    paymentUrlLogDTO.setStatus(responseStatus ? Status.GENERATED : Status.FAILED);
                    PaymentUrlLogDTO dto = paymentUrlLogDAS.save(paymentUrlLogDTO);
                    if( null != dto ) {
                        logger.debug("Payment URL generated successfully for the merchantTransactionId {}", dto.getId());
                    }
                }
            }
        } catch (Exception e) {
            paymentUrlLogDTO.setStatus(Status.FAILED);
            paymentUrlLogDTO.setPaymentStatusResponse(e.getMessage());
            paymentUrlLogDAS.save(paymentUrlLogDTO);
        }
    }

    /**
     * Helper method to build payment metadata which will be passed to payment gateway to generate the payment url link
     * @param merchantTransactionId
     * @param paymentAmount
     * @param orderIds
     * @param userId
     * @return
     */
    private static Map<String, Object> buildPaymentMetaData(String merchantTransactionId, BigDecimal paymentAmount, String userName, String orderIds, Integer userId, PaymentUrlType paymentUrlType, String purpose) {
        Map<String, Object> paymentRequestMetaData = new HashMap<>();
        paymentRequestMetaData.put(MERCHANT_TRANSACTION_ID, merchantTransactionId);
        paymentRequestMetaData.put(PAYMENT_AMOUNT, paymentAmount);
        if (null != orderIds) {
            paymentRequestMetaData.put(MERCHANT_ORDER_ID, orderIds);
        }
        if (null != userId) {
            paymentRequestMetaData.put(MERCHANT_USER_ID, String.valueOf(userId));
        }
        if (StringUtils.isNotBlank(userName)) {
            userName = userName.replace("@", "-");
            paymentRequestMetaData.put(USER_NAME, userName);
        }
        paymentRequestMetaData.put("linkPurpose", purpose);
        if (paymentUrlType.equals(PaymentUrlType.UPI)) {
            paymentRequestMetaData.put("paymentUrlType", paymentUrlType.name());
        }
        return paymentRequestMetaData;
    }

    private static Map<String, Object> buildPaymentMetaData(String merchantTransactionId) {
        Map<String, Object> paymentRequestMetaData = new HashMap<>();
        paymentRequestMetaData.put(MERCHANT_TRANSACTION_ID, merchantTransactionId);
        return paymentRequestMetaData;
    }

    /**
     * Helper method to build payment gateway configuration object based on the plugin parameters
     * @return
     * @throws JobExecutionException
     */
    private PaymentConfiguration buildPaymentConfiguration() throws JobExecutionException {
        return getPaymentConfiguration(MERCHANT_ID, SALT_KEY);
    }

    /**
     * Helper method to build payment gateway configuration object based on the plugin parameters
     *
     * @return
     * @throws JobExecutionException
     */
    private PaymentConfiguration buildVerificationConfiguration() throws JobExecutionException {
        return getPaymentConfiguration(VERIFICATION_CLIENT_ID, VERIFICATION_CLIENT_SECRET);
    }

    private PaymentConfiguration getPaymentConfiguration(String merchantId, String clientSecret) throws JobExecutionException {
        PaymentConfiguration paymentConfiguration = new PaymentConfiguration();
        Map<String, String> paymentConfig = new HashMap<>();
        paymentConfig.put(MERCHANT_ID, getParameter(merchantId));
        paymentConfig.put(ENVIRONMENT, getParameter(ENVIRONMENT));
        paymentConfig.put(PROVIDER_NAME, getParameter(PROVIDER_NAME));
        paymentConfig.put(SHOULD_PUBLISH_EVENTS, getParameter(SHOULD_PUBLISH_EVENTS));
        paymentConfig.put(SALT_INDEX, getParameter(SALT_INDEX));
        paymentConfig.put(SALT_KEY, getParameter(clientSecret));
        paymentConfig.put(CALLBACK_URL, getParameter(CALLBACK_URL));
        paymentConfig.put(REDIRECT_URL, getParameter(REDIRECT_URL));
        paymentConfig.put("ENTITY_ID", String.valueOf(getEntityId()));
        paymentConfiguration.setPaymentConfig(paymentConfig);
        return paymentConfiguration;
    }

    /**
     * Helper method to delete payment url when invoice is deleted.
     * @param invoice
     */
    private void deletePaymentUrl(InvoiceDTO invoice){
        int invoiceId = invoice.getId();
        logger.debug("Deleting the payment url for invoiceId = {}", invoiceId);
        PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();
        List<PaymentUrlLogDTO> paymentUrlLogDTOs = paymentUrlLogDAS.findAllByInvoiceId(invoiceId);
        for( PaymentUrlLogDTO dto : paymentUrlLogDTOs ) {
            if( null != dto ) {
                invoice.getOrderProcesses()
                        .stream()
                        .map(orderProcessDTO -> {
                            OrderDTO order = orderProcessDTO.getPurchaseOrder();
                            String notes = order.getNotes();
                            if( null != notes && notes.contains("Order generated for PaymentURL with id :") ) {
                                order.setNotes(null);
                                new OrderDAS().save(order);
                            }
                            return null;
                        });
                paymentUrlLogDAS.delete(dto);
            }
        }
    }

    /**
     * Helper method to fetch the plugin parameter values
     * @param key
     * @return
     * @throws JobExecutionException
     */
    private String getParameter(String key) throws JobExecutionException {
        String value = String.valueOf(parameters.get(key));
        AtomicReference<String> logValue = new AtomicReference<>(value);
        super.getParameterDescriptions().stream().forEach(pd -> {
            if (pd.getName().equalsIgnoreCase(key) && pd.getIsPassword()){
                logValue.set("***************");
                return;
            }
        });
        logger.info("In getParameter with key= {} and value={}", key, logValue);
        if (value == null || value.trim().equals("")) {
            throw new JobExecutionException("parameter '" + key + "' cannot be blank!");
        }
        return value;
    }

    public PaymentResponse cancelPaymentUrl(Integer paymentLogUrlLogId) {
        try {
            PaymentUrlLogDTO paymentUrlLogDTO = paymentUrlLogDAS.findNow(paymentLogUrlLogId);
            if( null == paymentUrlLogDTO ) {
                logger.debug("payment url for paymentLogUrlLogId={}, not generated ", paymentLogUrlLogId);
                return null;
            }
            logger.debug("check if we have already cancelled the payment status");
            if( Status.CANCELLED == paymentUrlLogDTO.getStatus() ) {
                String paymentStatusResponse = paymentUrlLogDTO.getPaymentStatusResponse();
                return new Gson().fromJson(paymentStatusResponse, PaymentResponse.class);
            }
            logger.debug("requesting cancellation for paymentLogUrlLogId = {}", paymentLogUrlLogId);
            PaymentConfiguration paymentConfiguration = buildPaymentConfiguration();
            Map<String, Object> paymentStatusMetaData = buildPaymentMetaData(paymentUrlLogDTO.getGatewayId());
            paymentConfiguration.setPaymentData(paymentStatusMetaData);

            PaymentGatewayClient paymentGatewayClient = new PaymentGatewayClient(getPaymentProviderUrl(), getParameter(API_KEY), getEntityId());
            PaymentResponse response = paymentGatewayClient.cancelPaymentUrl(paymentConfiguration);
            String responseStr = null != response ? new Gson().toJson(response) : "no response";
            logger.debug("Response received from the checkPaymentStatus API call {} ", responseStr);

            PaymentUrlLogDTO paymentUrlLogToUpdate = paymentUrlLogDAS.findForUpdate(paymentLogUrlLogId);
            paymentUrlLogToUpdate.setStatus(Status.valueOf(response.getResponseCode()));
            paymentUrlLogDAS.save(paymentUrlLogToUpdate);
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

}
