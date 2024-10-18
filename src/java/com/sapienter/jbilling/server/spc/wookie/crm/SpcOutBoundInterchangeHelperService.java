package com.sapienter.jbilling.server.spc.wookie.crm;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hibernate.SessionFactory;
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
import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.integration.db.OutBoundInterchange;
import com.sapienter.jbilling.server.integration.db.OutBoundInterchangeDAS;
import com.sapienter.jbilling.server.integration.db.Status;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.event.AbstractPaymentEvent;
import com.sapienter.jbilling.server.payment.event.PaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;

/**
 *
 * @author Krunal bhavsar
 *
 */
@Transactional
public class SpcOutBoundInterchangeHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String VENDOR_NAME = "JBILLING";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final Integer DEFAULT_TIME_OUT = 10000;
    private static final String API_RESPONSE = "api_response";
    private static final String RESULT = "result";
    private static final String INVOICE_NO = "invoice_no";
    private static final String DELEMETER = " ";

    @Autowired
    private OutBoundInterchangeDAS outBoundInterchangeDAS;

    @Autowired
    private SessionFactory sessionFactory;

    @Async("asyncTaskExecutor")
    public void postEventToWookieAsync(final SpcOutBoundInterchangeRequest request) {
        try {
            Event event = request.getEvent();
            logger.debug("exeucting request {} for entity {}", event.getName(), event.getEntityId());
            processEventRequest(request);
        } catch(Exception ex) {
            logger.error("error during sending event to wookie crm ", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private boolean isReviewRun(Integer billingprocessId) {
        BillingProcessDTO processDTO = new BillingProcessDAS().findNow(billingprocessId);
        return processDTO.getIsReview() == 1;
    }

    /**
     * Sets InvoiceNo of wookie crm on invoice level meta field
     * @param crmInvoiceIdMfName
     * @param request
     */
    private void setInvoiceNoOnInvoiceLevelMetaField(String crmInvoiceIdMfName, OutBoundInterchange request) {
        try {
            Integer entityId = request.getCompany().getId();
            String responseJson = request.getResponse();
            MetaField crmInvoiceId = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.INVOICE }, crmInvoiceIdMfName);
            if(null == crmInvoiceId) {
                logger.debug("{} not configured on invoice level for entity {}", crmInvoiceIdMfName, entityId);
                return;
            }
            JsonNode response = OBJECT_MAPPER.readTree(responseJson);
            if(!response.has(API_RESPONSE)) {
                logger.debug("api_response property not found in json {}", responseJson);
                return;
            }
            JsonNode apiResponse = response.get(API_RESPONSE);
            if(!apiResponse.has(RESULT)) {
                logger.debug("result property not found in json {}", apiResponse);
                return;
            }
            JsonNode result = apiResponse.get(RESULT);
            if(!result.has(INVOICE_NO)) {
                logger.debug("invoice_no field not found in json {}", result);
                return;
            }
            String invoiceNo = result.get(INVOICE_NO).asText();
            Integer invoiceId = request.getSource();
            logger.debug("adding wookie invoice no {} on invoice id {}", invoiceNo, invoiceId);
            InvoiceDAS invoiceDAS = new InvoiceDAS();
            InvoiceDTO invoice = invoiceDAS.findNow(invoiceId);
            StringMetaFieldValue invoiceNoMetaFieldValue = new StringMetaFieldValue(crmInvoiceId);
            invoiceNoMetaFieldValue.setValue(invoiceNo);
            logger.debug("metaFieldValue {} created with invoice no {}", invoiceNoMetaFieldValue, invoiceNo);
            invoice.getMetaFields().add(invoiceNoMetaFieldValue);
            logger.debug("metaFieldValue {} added on invoice {}", invoiceNoMetaFieldValue, invoiceId);
            invoiceDAS.save(invoice);
            logger.debug("invoice {} saved with invoice no {}", invoiceId, invoiceNo);
        } catch(IOException ioException) {
            logger.error("json string to jsonNode conversion failed ", ioException);
        } catch (Exception ex) {
            logger.error("failed during setting invoice no on invoice {}", request.getSource(), ex);
        }
    }

    private void processEventRequest(final SpcOutBoundInterchangeRequest request) {
        Event event = request.getEvent();
        Map<String, String> parameters = request.getParameters();
        Integer entityId = request.getEvent().getEntityId();
        if(event instanceof InvoicesGeneratedEvent) {
            InvoicesGeneratedEvent invoicesGeneratedEvent = (InvoicesGeneratedEvent) event;
            if(null!= invoicesGeneratedEvent.getBillingProcessId() &&
                    isReviewRun(invoicesGeneratedEvent.getBillingProcessId())) {
                logger.debug("skipping invoice push to crm for for process {} review for entity {}", invoicesGeneratedEvent.getBillingProcessId(),
                        entityId);
                return ;
            }
            for(OutBoundInterchange invoiceRequest : generateOutBoundInterchangeRequestForInvoice(request)) {
                postToWookieCRM(invoiceRequest, parameters, entityId);
                if(invoiceRequest.getStatus().equals(Status.PROCESSED)) {
                    String invoiceNoMfName = request.getParameters().get(SpcOutBoundInterchangeTask.PARAM_CRM_INVOICE_ID.getName());
                    if(StringUtils.isEmpty(invoiceNoMfName)) {
                        logger.debug("Parameter {} not configured for entity {}", SpcOutBoundInterchangeTask.PARAM_CRM_INVOICE_ID.getName(), entityId);
                    } else {
                        setInvoiceNoOnInvoiceLevelMetaField(invoiceNoMfName, invoiceRequest);
                    }
                }
            }
        } else if(event instanceof PaymentSuccessfulEvent ||
                event instanceof PaymentFailedEvent ) {
            AbstractPaymentEvent paymentEvent = (AbstractPaymentEvent) event;
            PaymentDTOEx payment = paymentEvent.getPayment();
            WookiePaymentRequest paymentRequest = generateWookiePaymentRequest(payment.getId(),
                    isPaymentFailed(paymentEvent), payment.getInvoiceIds());
            OutBoundInterchange outBoundInterchange = new OutBoundInterchange();
            outBoundInterchange.setCompany(new CompanyDTO(entityId));
            outBoundInterchange.setUserId(payment.getUserId());
            String methodName = parameters.get(SpcOutBoundInterchangeTask.PARAM_CREATE_PAYMENT_METHOD_NAME.getName());
            validateParameter(SpcOutBoundInterchangeTask.PARAM_CREATE_PAYMENT_METHOD_NAME.getName(), methodName);
            outBoundInterchange.setMethodName(methodName);
            outBoundInterchange.setHttpMethod(HttpMethod.POST);
            try {
                outBoundInterchange.setRequest(OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(paymentRequest));
                outBoundInterchange  = outBoundInterchangeDAS.save(outBoundInterchange);
                logger.debug("Request {} saved for entity {}", outBoundInterchange, entityId);
            } catch (JsonProcessingException ex) {
                logger.error("payment json string conversion failed ", ex);
                throw new SessionInternalError("payment json string conversion failed ", ex);
            }
            postToWookieCRM(outBoundInterchange, parameters, entityId);
        }
    }

    public void postEventToWookie(final SpcOutBoundInterchangeRequest request) {
        processEventRequest(request);
    }

    @Async("asyncTaskExecutor")
    public void retryOutBoundInterchangeRequest(Integer requestId, Integer pluginId, Integer entityId, Integer maxRetry) {
        OutBoundInterchange outBoundInterchange = null;
        try {
            outBoundInterchange = outBoundInterchangeDAS.
                    findFailedFailedOutBoundInterchangeRequestWithLock(requestId);
            if(null == outBoundInterchange) {
                return;
            }
            Map<String, String> parameters = collectParametersFromPlugin(pluginId, entityId);
            if(maxRetry == null) {
                maxRetry = Integer.MAX_VALUE;
            }
            if(maxRetry.equals(outBoundInterchange.getRetryCount())) {
                logger.debug("max retry count reached for request {} for entity {}", outBoundInterchange, entityId);
                return;
            }
            outBoundInterchange.incrementRetry();
            String invoiceMethodName = parameters.get(SpcOutBoundInterchangeTask.PARAM_CREATE_INVOICE_METHOD_NAME.getName());
            String paymentMethodName = parameters.get(SpcOutBoundInterchangeTask.PARAM_CREATE_PAYMENT_METHOD_NAME.getName());
            if(outBoundInterchange.getMethodName().equals(invoiceMethodName)) {
                WookieInvoiceRequest failedInvoiceRequest = OBJECT_MAPPER.readValue(outBoundInterchange.getRequest(), WookieInvoiceRequest.class);
                if(null == failedInvoiceRequest.getVendorInvoiceId()) {
                    logger.debug("no invoice id found from request {}", outBoundInterchange);
                    return;
                }
                InvoiceDTO invoice = new InvoiceDAS().findNow(failedInvoiceRequest.getVendorInvoiceId());
                if(null == invoice) {
                    logger.debug("no invoice found with id {} for entity {}", failedInvoiceRequest.getVendorInvoiceId(), entityId);
                    return;
                }
                BillingProcessDTO process = invoice.getBillingProcess();
                InvoicesGeneratedEvent invoiceEvent = new InvoicesGeneratedEvent(entityId, process !=null ? process.getId() : null);
                invoiceEvent.addInvoiceIds(new Integer[] { invoice.getId() });
                List<WookieInvoiceRequest> retryInvoiceRequests = generateWookieInvoiceRequestFromInvoiceEvent(new SpcOutBoundInterchangeRequest(invoiceEvent, parameters));
                if(retryInvoiceRequests.isEmpty()) {
                    return;
                }
                outBoundInterchange.setRequest(OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(retryInvoiceRequests.get(0)));
                outBoundInterchange.setLastRetryDateTime(new Date());
                postToWookieCRM(outBoundInterchange, parameters, entityId);
                outBoundInterchange.setSource(invoice.getId());
                if(outBoundInterchange.getStatus().equals(Status.PROCESSED)) {
                    String invoiceNoMfName = parameters.get(SpcOutBoundInterchangeTask.PARAM_CRM_INVOICE_ID.getName());
                    if(StringUtils.isEmpty(invoiceNoMfName)) {
                        logger.debug("Parameter {} not configured for entity {}", SpcOutBoundInterchangeTask.PARAM_CRM_INVOICE_ID.getName(), entityId);
                    } else {
                        setInvoiceNoOnInvoiceLevelMetaField(invoiceNoMfName, outBoundInterchange);
                    }
                }
            } else if(outBoundInterchange.getMethodName().equals(paymentMethodName)) {
                WookiePaymentRequest failedPaymentRequest = OBJECT_MAPPER.readValue(outBoundInterchange.getRequest(), WookiePaymentRequest.class);
                if(null == failedPaymentRequest.getPaymentId()) {
                    logger.debug("no payment id found from request {}", outBoundInterchange);
                    return;
                }
                PaymentDTO payment = new PaymentDAS().findNow(failedPaymentRequest.getPaymentId());
                if(null == payment) {
                    logger.debug("no payment found for payment id {} for entity {}", failedPaymentRequest.getPaymentId(), entityId);
                    return;
                }
                WookiePaymentRequest retrypaymentRequest = generateWookiePaymentRequest(payment.getId(), isPaymentFailed(payment), null);
                outBoundInterchange.setRequest(OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(retrypaymentRequest));
                outBoundInterchange.setLastRetryDateTime(new Date());
                postToWookieCRM(outBoundInterchange, parameters, entityId);

            } else {
                logger.error("unknow method name {} found", outBoundInterchange.getMethodName());
            }
        } catch(Exception ex) {
            logger.error("failed during retry of request {} for entity {} ", outBoundInterchange, entityId, ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private Map<String, String> collectParametersFromPlugin(Integer pluginId, Integer entityId) {
        PluggableTaskDAS pluggableTaskDAS = Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
        PluggableTaskDTO spcPlugin = pluggableTaskDAS.findNow(pluginId);
        Assert.notNull(spcPlugin, "SpcOutBoundInterchangeTask not configured for entity " + entityId);
        Map<String, String> parameters = new HashMap<>();
        for(PluggableTaskParameterDTO parameterDTO : spcPlugin.getParameters()) {
            parameters.put(parameterDTO.getName(), parameterDTO.getValue());
        }
        return parameters;
    }

    private String formatDate(final Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        return simpleDateFormat.format(date);
    }

    private void validateParameter(String paramName, Object paramValue) {
        Assert.notNull(paramValue, String.format("Parameter [%s] is null", paramName));
    }

    private WookiePaymentRequest generateWookiePaymentRequest(Integer paymentId,  boolean isPaymentFailed, List<Integer> invoicesLinkToPayment) {
        PaymentDTO payment = (PaymentDTO) sessionFactory.getCurrentSession()
                .get(PaymentDTO.class, paymentId);
        WookiePaymentRequest paymentRequest = new WookiePaymentRequest();
        paymentRequest.setAmountPaid(payment.getAmount().setScale(Constants.BIGDECIMAL_SCALE_STR, Constants.BIGDECIMAL_ROUND));
        if(CollectionUtils.isNotEmpty(invoicesLinkToPayment)) {
            paymentRequest.setInvoiceId(invoicesLinkToPayment.get(0));
        } else if(CollectionUtils.isNotEmpty(payment.getInvoicesMap())) {
            paymentRequest.setInvoiceId(payment.getInvoicesMap().iterator().next().getId());
        }
        paymentRequest.setParentPaymentId(payment.getIsRefund() == 1 ? payment.getPayment().getId() : null);
        paymentRequest.setPaymentDate(payment.getPaymentDate());
        paymentRequest.setPaymentId(paymentId);
        UserDTO user = payment.getBaseUser();
        paymentRequest.setPaymentMethod(findPaymentMethod(payment.getPaymentMethod().getDescription(user.getLanguageIdField())));
        paymentRequest.setPaymentStatus(isPaymentFailed ? WookiePaymentStatus.FAILED : WookiePaymentStatus.PAID);
        paymentRequest.setVendor(VENDOR_NAME);
        paymentRequest.setPaymentType(payment.getIsRefund() == 1 ? WookiePaymentType.REFUND : WookiePaymentType.PAYMENT);
        return paymentRequest;
    }

    private WookiePaymentMethod findPaymentMethod(String methodName) {
        for(WookiePaymentMethod method : WookiePaymentMethod.values()) {
            if (method.getMethodName().equalsIgnoreCase(methodName) ) {
                return method;
            }
        }
        return WookiePaymentMethod.OTHER;
    }

    private boolean isPaymentFailed(AbstractPaymentEvent paymentEvent) {
        return paymentEvent instanceof PaymentFailedEvent;
    }

    private boolean isPaymentFailed(PaymentDTO payment) {
        Integer result = payment.getPaymentResult().getId();
        return Constants.RESULT_FAIL.equals(result);
    }

    private List<OutBoundInterchange> generateOutBoundInterchangeRequestForInvoice(final SpcOutBoundInterchangeRequest request) {
        try {
            List<OutBoundInterchange> requests = new ArrayList<>();
            List<WookieInvoiceRequest> invoiceRequests = generateWookieInvoiceRequestFromInvoiceEvent(request);
            Integer entityId = request.getEvent().getEntityId();
            for(WookieInvoiceRequest wookieInvoiceRequest : invoiceRequests) {
                OutBoundInterchange outBoundInterchange = new OutBoundInterchange();
                outBoundInterchange.setCompany(new CompanyDTO(entityId));
                outBoundInterchange.setUserId(wookieInvoiceRequest.getAccountId());
                String methodName = request.getParameters().get(SpcOutBoundInterchangeTask.PARAM_CREATE_INVOICE_METHOD_NAME.getName());
                validateParameter(SpcOutBoundInterchangeTask.PARAM_CREATE_INVOICE_METHOD_NAME.getName(), methodName);
                outBoundInterchange.setMethodName(methodName);
                outBoundInterchange.setHttpMethod(HttpMethod.POST);
                outBoundInterchange.setRequest(OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(wookieInvoiceRequest));
                outBoundInterchange  = outBoundInterchangeDAS.save(outBoundInterchange);
                outBoundInterchange.setSource(wookieInvoiceRequest.getVendorInvoiceId());
                logger.debug("Request {} saved for entity {}", outBoundInterchange, entityId);
                requests.add(outBoundInterchange);
            }
            return requests;
        } catch(JsonProcessingException ex) {
            logger.error("invoice json string conversion failed ", ex);
            throw new SessionInternalError("invoice json string conversion failed ", ex);
        }
    }

    private void postToWookieCRM(final OutBoundInterchange interchange, final Map<String, String> parameters, final Integer entityId) {
        try {
            logger.debug("posting request {} for entity {}", interchange, entityId);
            RestOperations restOperations = configureRestTemplateFromParameters(parameters);
            String url = parameters.get(SpcOutBoundInterchangeTask.PARAM_WOOKIE_URL.getName());
            validateParameter(SpcOutBoundInterchangeTask.PARAM_WOOKIE_URL.getName(), url);
            url = url.endsWith("/") ? url : (url + "/");
            String userName = parameters.get(SpcOutBoundInterchangeTask.PARAM_USER_NAME.getName());
            validateParameter(SpcOutBoundInterchangeTask.PARAM_USER_NAME.getName(), userName);
            String password = parameters.get(SpcOutBoundInterchangeTask.PARAM_PASSWORD.getName());
            validateParameter(SpcOutBoundInterchangeTask.PARAM_PASSWORD.getName(), password);
            HttpEntity<String> request = new HttpEntity<>(interchange.getRequest(), createAuthHeader(userName, password));
            ResponseEntity<String> response = restOperations.exchange(url + interchange.getMethodName(),
                    interchange.getHttpMethod(), request, String.class);
            interchange.setResponse(response.getBody());
            try {
                boolean hasError = hasError(interchange.getResponse());
                interchange.setStatus(hasError ? Status.FAILED : Status.PROCESSED);
            } catch (IOException ex) {
                logger.error("failed during response unmarshalling", ex);
                interchange.setStatus(Status.UNMARSHALLING_ERROR);
                String errorResponse = ExceptionUtils.getStackTrace(ex);
                interchange.setResponse(errorResponse);
            }
            logger.debug("response is {}", interchange.getResponse());
            return ;
        } catch (HttpClientErrorException | HttpServerErrorException error) {
            interchange.setResponse(error.getResponseBodyAsString());
            interchange.setStatus(Status.FAILED);
            logger.debug("response is {}", interchange.getResponse());
        } catch (Exception e) {
            String errorResponse = ExceptionUtils.getStackTrace(e);
            interchange.setResponse(errorResponse);
            interchange.setStatus(Status.FAILED);
            logger.error("request sending failed to crm ", e);
        }
    }

    /**
     * Creates Auth Header for given credential
     *
     * @return
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

    /**
     * Collects order ids from invoice lines for given {@link InvoiceDTO}
     * @param invoice
     * @return
     */
    private Integer[] collectOrdersFromInvoice(final InvoiceDTO invoice) {
        return invoice.getInvoiceLines()
                .stream()
                .map(InvoiceLineDTO::getOrder)
                .filter(Objects::nonNull)
                .map(OrderDTO::getId)
                .distinct()
                .toArray(Integer[]::new);
    }

    private WookieProductWS createWookieProduct(final InvoiceLineDTO invoiceLine, String discountProductCode) {
        BigDecimal price = invoiceLine.isTaxLine() ? invoiceLine.getAmount() : invoiceLine.getPrice();
        return new WookieProductWS(invoiceLine.isDiscountLine() ? discountProductCode :
            invoiceLine.getItem().getInternalNumber(), invoiceLine.getQuantity(),
            price.setScale(Constants.BIGDECIMAL_SCALE_STR, Constants.BIGDECIMAL_ROUND),
            invoiceLine.getTaxRate(), invoiceLine.getTaxAmount());
    }

    /**
     * Creates {@link WookieProductWS} from invoice lines for given {@link InvoiceDTO}
     * @param invoice
     * @return
     */
    private WookieProductWS[] collectProductsFromInvoice(final InvoiceDTO invoice, String discountProductCode) {
        return invoice.getInvoiceLines()
                .stream()
                .filter(invoiceLine -> !invoiceLine.dueInvoiceLine())
                .map(invoiceLine -> createWookieProduct(invoiceLine, discountProductCode))
                .toArray(WookieProductWS[]::new);
    }

    private Optional<Integer> getKeyByValue(Map<Integer, String> map, String value) {
        for(Entry<Integer, String> entry : map.entrySet()) {
            if(entry.getValue().equals(value)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private List<WookieInvoiceRequest> generateWookieInvoiceRequestFromInvoiceEvent(final SpcOutBoundInterchangeRequest request) {
        InvoicesGeneratedEvent invoicesGeneratedEvent = (InvoicesGeneratedEvent) request.getEvent();
        final InvoiceDAS invoiceDAS = new InvoiceDAS();
        final List<WookieInvoiceRequest> requests = new ArrayList<>();
        for(final Integer invoiceId : invoicesGeneratedEvent.getInvoiceIds()) {
            final InvoiceDTO invoice = invoiceDAS.findNow(invoiceId);
            if(null == invoice) {
                logger.debug("invoice {} not found", invoiceId);
                continue;
            }
            final CustomerDTO customer = invoice.getBaseUser().getCustomer();
            final WookieInvoiceRequest wookieInvoiceRequest = new WookieInvoiceRequest();
            wookieInvoiceRequest.setVendor(VENDOR_NAME);
            wookieInvoiceRequest.setSubject("Invoice for user " + invoice.getBaseUser().getId() +
                    " For Date "+ formatDate(invoice.getCreateDatetime()));
            wookieInvoiceRequest.setAccountId(invoice.getBaseUser().getId());
            wookieInvoiceRequest.setDueDate(invoice.getDueDate());
            wookieInvoiceRequest.setInvoiceDate(invoice.getCreateDatetime());
            wookieInvoiceRequest.setInvoiceStatus(invoicesGeneratedEvent.isGeneratedByAPICall() ?
                    WookieInvoiceStatus.CREATED : WookieInvoiceStatus.AUTO_CREATED);
            wookieInvoiceRequest.setVendorInvoiceId(invoice.getId());

            Collection<InvoiceLineDTO> lines= invoice.getInvoiceLines();
            BigDecimal total = BigDecimal.ZERO;
            if (CollectionUtils.isNotEmpty(lines)) {
                total =  lines.stream().filter(line -> !line.dueInvoiceLine())
                        .map(InvoiceLineDTO::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            wookieInvoiceRequest.setTotal(total);

            Map<String, String> pluginParameters = request.getParameters();
            Integer entityId = invoicesGeneratedEvent.getEntityId();
            String billingGroupName = pluginParameters.get(SpcOutBoundInterchangeTask.PARAM_BILLING_AIT_GROUP_NAME.getName());
            String shippingGroupName = pluginParameters.get(SpcOutBoundInterchangeTask.PARAM_SHIPPING_AIT_GROUP_NAME.getName());
            Map<Integer, String> accountAITSectionIdAndNameMap = new AccountInformationTypeDAS().getInformationTypeIdAndNameMapForAccountType(customer.getAccountType().getId());
            logger.debug("AIt section for account {} is {}", customer.getAccountType().getId(), accountAITSectionIdAndNameMap);
            Optional<Integer> billingGroupId = getKeyByValue(accountAITSectionIdAndNameMap, billingGroupName);
            Optional<Integer> shippingGroupId = getKeyByValue(accountAITSectionIdAndNameMap, shippingGroupName);
            logger.debug("billing ait section id {} for entity {}", billingGroupId, entityId);
            logger.debug("shipping ait section id {} for entity {}", shippingGroupId, entityId);

            if(billingGroupId.isPresent()) {
                final Map<String, String> billingAddressAIT = getCustomerAITMetaFields(customer, billingGroupId.get(), TimezoneHelper.companyCurrentDate(entityId));
                logger.debug("Billing ait section values {}", billingAddressAIT);
                // setting billing ait section meta fields
                String streetType = billingAddressAIT.getOrDefault(MetaFieldType.STREET_TYPE.name(), StringUtils.EMPTY);
                String streetName = billingAddressAIT.getOrDefault(MetaFieldType.STREET_NAME.name(), StringUtils.EMPTY);
                String streetNumber = billingAddressAIT.getOrDefault(MetaFieldType.STREET_NUMBER.name(), StringUtils.EMPTY);
                String billStreet = buildStreet(DELEMETER, streetName, streetNumber, streetType);
                logger.debug("constructed bill street {}", billStreet);
                wookieInvoiceRequest.setBillStreet(billStreet);
                for(final WookieInvoiceField wookieInvoiceField : WookieInvoiceField.getBillingAITFields()) {
                    wookieInvoiceRequest.setOtherProperty(wookieInvoiceField.name().toLowerCase(),
                            billingAddressAIT.getOrDefault(wookieInvoiceField.getMetaFieldType().name(), StringUtils.EMPTY));
                }
            }

            if(shippingGroupId.isPresent()) {
                final Map<String, String> shippingAddressAIT = getCustomerAITMetaFields(customer, shippingGroupId.get(), TimezoneHelper.companyCurrentDate(entityId));
                logger.debug("Shipping ait section values {}", shippingAddressAIT);
                // setting shipping ait section meta fields
                String streetName = shippingAddressAIT.getOrDefault(MetaFieldType.STREET_NAME.name(), StringUtils.EMPTY);
                String streetNumber = shippingAddressAIT.getOrDefault(MetaFieldType.STREET_NUMBER.name(), StringUtils.EMPTY);
                String streetType = shippingAddressAIT.getOrDefault(MetaFieldType.STREET_TYPE.name(), StringUtils.EMPTY);
                String shipStreet = buildStreet(DELEMETER, streetName, streetNumber, streetType);
                logger.debug("constructed ship street {}", shipStreet);
                wookieInvoiceRequest.setShipStreet(shipStreet);
                for (final WookieInvoiceField wookieInvoiceField : WookieInvoiceField.getShippingAITFields()) {
                    wookieInvoiceRequest.setOtherProperty(wookieInvoiceField.name().toLowerCase(),
                            shippingAddressAIT.getOrDefault(wookieInvoiceField.getMetaFieldType().name(),
                                    StringUtils.EMPTY));
                }
            }
            // adding order id from invoice
            wookieInvoiceRequest.setSalesOrders(collectOrdersFromInvoice(invoice));
            String discountParamName = SpcOutBoundInterchangeTask.PARAM_DISCOUNT_PRODUCT_CODE.getName();
            String discountProductCode = pluginParameters.get(discountParamName);
            if(StringUtils.isEmpty(discountProductCode)) {
                logger.debug("param {} not configured for entity {}", discountParamName, entityId);
                throw new SessionInternalError(String.format("Param [%s] not configured for entity [%s] ", discountParamName, entityId));
            }
            if(null == new ItemDAS().findItemByInternalNumber(discountProductCode, entityId)) {
                logger.debug("no discount product {} found for entity {}", discountProductCode, entityId);
                throw new SessionInternalError(String.format("discount prodduct [%s] not found for entity [%s]", discountProductCode, entityId));
            }
            // adding products from invoice
            wookieInvoiceRequest.setProducts(collectProductsFromInvoice(invoice, discountProductCode));
            requests.add(wookieInvoiceRequest);
        }
        return requests;
    }

    private String buildStreet(String delimiter, String... values) {
        StringJoiner strJoiner = new StringJoiner(delimiter);
        for (String value : values) {
            strJoiner.add(value);
        }
        return strJoiner.toString();
    }

    @SuppressWarnings("rawtypes")
    private Map<String,String> getCustomerAITMetaFields(final CustomerDTO customer, final Integer groupId, final Date effectiveDate) {
        final Map<Date, List<MetaFieldValue>> aitMetaFields = customer.getAitTimelineMetaFieldsMap().get(groupId);
        if(MapUtils.isEmpty(aitMetaFields)) {
            return Collections.emptyMap();
        }
        final Map<String, String> aitFieldNameValueMap = new HashMap<>();
        for(final Entry<Date, List<MetaFieldValue>> aitEntry : aitMetaFields.entrySet()) {
            if(aitEntry.getKey().compareTo(effectiveDate)<=0) {
                for(final MetaFieldValue aitFieldValue: aitEntry.getValue()) {
                    final Object value = aitFieldValue.getValue();
                    final MetaFieldType usage = aitFieldValue.getField().getFieldUsage();
                    if(null!= usage) {
                        aitFieldNameValueMap.put(usage.name(), value!=null ? value.toString() : "");
                    }
                }
                return aitFieldNameValueMap;
            }
        }
        return Collections.emptyMap();
    }

    private static ClientHttpRequestFactory getClientHttpRequestFactory(final int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        return new HttpComponentsClientHttpRequestFactory( HttpClientBuilder.create().setDefaultRequestConfig(config).build() );
    }

    private RestOperations configureRestTemplateFromParameters(final Map<String, String> parameters) {
        return new RestTemplate(getClientHttpRequestFactory(getTimeOut(parameters)));
    }

    private Integer getTimeOut(Map<String, String> parameters) {
        String value = parameters.get(SpcOutBoundInterchangeTask.PARAM_TIMEOUT.getName());
        if(StringUtils.isEmpty(value)) {
            value = DEFAULT_TIME_OUT.toString();
        }
        return Integer.parseInt(value);
    }

    private boolean hasError(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.readTree(response);
        return responseNode.get("errors").size() != 0 && !responseNode.get("api_success").asBoolean();
    }

}
