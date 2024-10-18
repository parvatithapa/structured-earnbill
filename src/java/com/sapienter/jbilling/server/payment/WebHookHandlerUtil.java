package com.sapienter.jbilling.server.payment;

import static com.sapienter.jbilling.common.CommonConstants.PAYMENT_METHOD_CREDIT;
import static com.sapienter.jbilling.common.CommonConstants.PAYMENT_METHOD_CUSTOM;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cashfree.model.UpiAdvanceResponseSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDAS;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.paymentUrl.db.Status;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.pluggableTask.IOrderChangeTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.CreateResponseWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import grails.plugin.springsecurity.SpringSecurityService;

public class WebHookHandlerUtil {
    private static final Integer ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, Object>> TYPE_REF = new TypeReference<HashMap<String, Object>>() {};
    private static final List<String> UPI_META_FIELDS = Arrays.asList("utr", "ifsc", "payerVpa");
    private static final List<String> CARD_META_FIELDS = Arrays.asList("cardNumber", "expiry date", "name");
    public static final List<String> SUCCESS_RESPONSES = Arrays.asList("PAID","SUCCESS","PAYMENT_SUCCESS");
    public static final List<String> FAILED_RESPONSES = Arrays.asList("FAILED", "PAYMENT_FAILED");
    private String gatewayName;
    private Integer accountTypeId;
    private boolean doVerification = false;

    public WebHookHandlerUtil(String gatewayName, Integer accountTypeId) {
        this.gatewayName = gatewayName;
        this.accountTypeId = accountTypeId;
    }

    private WebHookHandlerUtil() {}

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Integer getLanguageId(Integer entityId) throws PluggableTaskException {
        return executeWebServicesOperation(entityId, null, null, null, true, null, null);
    }

    private UpiAdvanceResponseSchema getVerificationDataFromPaymentGateway(Integer entityId, String payerVPA) throws PluggableTaskException {
        try {
            return executeWebServicesOperation(entityId, null, null, null, false, payerVPA, null);
        } catch (PluggableTaskException e) {
            throw new RuntimeException(e);
        }

    }

    private Integer getCurrencyId(Integer entityId, String currencyCode) {
        try {
            return executeWebServicesOperation(entityId, null, null, null, false, null, currencyCode);
        } catch (PluggableTaskException e) {
            throw new RuntimeException(e);
        }

    }

    private <T> T executeWebServicesOperation(Integer entityId, UserWS userWS, OrderWS orderWS, OrderChangeWS[] orderChanges, boolean languageId, String payerVPA, String currencyCode) throws PluggableTaskException {
        SpringSecurityService springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        if( !springSecurityService.isLoggedIn() ) {
            try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
                return getT(userWS, orderWS, orderChanges, languageId, payerVPA, currencyCode);
            }
        }
        return getT(userWS, orderWS, orderChanges, languageId, payerVPA, currencyCode);
    }

    private <T> T getT(UserWS userWS, OrderWS orderWS, OrderChangeWS[] orderChanges, boolean languageId, String payerVPA, String currencyCode) throws PluggableTaskException {
        IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        if( languageId ) {
            return (T) webServicesSessionBean.getCallerLanguageId();
        } else if( StringUtils.isNotBlank(payerVPA) ) {
            return (T) webServicesSessionBean.executePaymentTask(null, payerVPA,
                    "verifyPayerVPA");
        } else if( StringUtils.isNotBlank(currencyCode) ) {
            CurrencyWS[] currencies = webServicesSessionBean.getCurrencies();
            Optional<CurrencyWS> currencyWS = Arrays.stream(currencies).filter(c -> c.getCode().equalsIgnoreCase(currencyCode)).findFirst();
            if( currencyWS.isPresent() ) {
                return (T) currencyWS.get().getId();
            }
            return (T) webServicesSessionBean.getCallerCurrencyId();
        } else {

            if( userWS.getId() != 0 ) {
                return (T) webServicesSessionBean.createWithExistingUser(userWS.getId(), orderWS, orderChanges);
            } else {
                return (T) webServicesSessionBean.create(userWS, orderWS, orderChanges);
            }
        }
    }

    private String getProductAndQuantity(BigDecimal amount, Integer entityId) {
        String result = null;
        try {
            PluggableTaskManager<IOrderChangeTask> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_DERIVING_ORDER_CHANGE);
            IOrderChangeTask task = taskManager.getNextClass();
            while (task != null) {
                result = task.guessProductByAmount(amount, entityId);
                task = taskManager.getNextClass();
            }
        } catch (PluggableTaskException e) {
            logger.error("Problems handling order change task ", e);
            throw new SessionInternalError("Exception in OrderChange task",
                    WebHookHandlerUtil.class, e);
        }
        return result;
    }

    private UserWS createOrGetUser(String userName, Integer accountTypeId, Integer entityId) {
        userName = StringUtils.isNotBlank(userName) ? userName : "User-" + System.currentTimeMillis();
        UserBL bl = new UserBL(userName, entityId);
        UserWS newUser = bl.getUserWS();
        return null != newUser ? newUser : createNewUserObject(userName, accountTypeId);
    }

    private UserWS createNewUserObject(String userName, Integer accountTypeId) {
        UserWS newUser = new UserWS();
        newUser.setId(0);
        newUser.setUserName(userName);
        newUser.setAccountTypeId(accountTypeId);
        newUser.setMainRoleId(CommonConstants.TYPE_CUSTOMER);
        return newUser;
    }

    private OrderWS createOrder(BigDecimal totalAmount, Integer userId, Integer languageId, Integer paymentUrlId, Integer entityId, String itemCode, String quantity, Integer currencyId) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);
        order.setActiveSince(Calendar.getInstance().getTime());
        OrderLineWS orderLineWS = new OrderLineWS();
        orderLineWS.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        orderLineWS.setQuantity(quantity);
        ItemDTO item = new ItemDAS().findItemByInternalNumber(itemCode, entityId);
        orderLineWS.setDescription(item.getDescription(languageId));
        orderLineWS.setItemId(item.getId());
        BigDecimal price = totalAmount.divide(new BigDecimal(quantity), 8, RoundingMode.HALF_DOWN);
        orderLineWS.setPrice(price);
        orderLineWS.setAmount(totalAmount);
        order.setOrderLines(new OrderLineWS[]{orderLineWS});
        if (null != paymentUrlId) {
            order.setNotes("Order generated for PaymentURL with id :" + paymentUrlId);
        }
        order.setCurrencyId(currencyId);
        return order;
    }

    private void createPaymentAuthDTO(String transactionId, Integer entityId, Integer paymentId) {
        PaymentAuthorizationDTO authorizationDTO = new PaymentAuthorizationDTO();
        authorizationDTO.setProcessor(gatewayName);
        authorizationDTO.setCode1(gatewayName);
        authorizationDTO.setTransactionId(transactionId);
        authorizationDTO.setCreateDate(TimezoneHelper.companyCurrentDate(entityId));
        new PaymentAuthorizationBL().create(authorizationDTO, paymentId);
    }

    private MetaFieldValueWS getMetaFieldValueWS(MetaFieldDAS metaFieldDAS, Integer entityId, String fieldName, String fieldValue) {
        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
        com.sapienter.jbilling.server.metafields.MetaFieldWS tableTypeMfield = MetaFieldBL.getWS(metaFieldDAS.getFieldByName(entityId, new EntityType[]{EntityType.PAYMENT_METHOD_TYPE}, fieldName));
        metaFieldValueWS.setMetaField(tableTypeMfield);
        if (fieldValue.equals("null")) {
            return null;
        }
        metaFieldValueWS.setStringValue(fieldValue);
        return metaFieldValueWS;
    }

    public CreateResponseWS create(PaymentResponse response, String webhookResponse, Integer entityId) {
        try {
            Optional.ofNullable(new AccountTypeDAS().findNow(accountTypeId))
                    .orElseThrow(() -> new SessionInternalError("Account type " + accountTypeId + " not present for entity " + entityId));

            Integer languageId = getLanguageId(entityId);
            String userName = null;
            BigDecimal paymentAmount = null;
            String itemCode = null;
            String quantity = null;

            Map<String, String> metaData = response.getMetaData();
            String merchantTransactionId = response.getMerchantTransactionId();
            PaymentUrlLogDTO paymentUrlLogDTO = new PaymentUrlLogDAS().findByGatewayId(merchantTransactionId);
            if (null == paymentUrlLogDTO) {
                logger.debug("this is payment for Static QR code");
                String paymentAmountStr = metaData.get("amount");
                paymentAmount = new BigDecimal(paymentAmountStr);
                String result = getProductAndQuantity(paymentAmount, entityId);

                String[] strings = Optional.ofNullable(result)
                        .map(str -> str.split(":"))
                        .filter(segments -> segments.length > 1)
                        .get();
                itemCode = strings[0];
                quantity = strings[1];
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("entityId", entityId);
                requestMap.put("totalAmount", paymentAmountStr);
                requestMap.put("webhookResponse", webhookResponse);
                requestMap.put("gatewayId", merchantTransactionId);
                requestMap.put("paymentUrlType", "upi");
                paymentUrlLogDTO = createPaymentUrlLog(requestMap, entityId);
            } else {
                Integer invoiceId = paymentUrlLogDTO.getInvoiceId();

                if(null == paymentUrlLogDTO.getPaymentStatusResponse() || !paymentUrlLogDTO.getStatus().equals(Status.SUCCESSFUL)) {
                    if (null != invoiceId) {
                        InvoiceDTO invoiceDTO = new InvoiceBL(invoiceId).getEntity();
                        // we already have invoice generated so no need to create user and generate invoice
                        // we only need to link invoice with the payment.
                        CreateResponseWS responseWS = new CreateResponseWS();
                        responseWS.setInvoiceId(invoiceId);
                        responseWS.setUserId(invoiceDTO.getUserId());
                        responseWS.setOrderId(invoiceDTO.getOrderProcesses()
                            .stream().findFirst().get().getPurchaseOrder().getId());
                        responseWS.setPaymentId(updatePaymentStatus(paymentUrlLogDTO, response, invoiceId, invoiceDTO.getUserId(), metaData, entityId, webhookResponse));
                        return responseWS;
                    } else {
                        // get the json map from the paymentUrlLogDTOrathi
                        String mobileRequestPayload = paymentUrlLogDTO.getMobileRequestPayload();
                        Map<String, Object> map = OBJECT_MAPPER.readValue(mobileRequestPayload, TYPE_REF);
                        userName = (String) map.get("name");
                        itemCode = (String) map.get("itemCode");
                        quantity = (String) map.get("quantity");
                        paymentAmount = paymentUrlLogDTO.getPaymentAmount();
                    }
                } else {
                    // TODO check what needs to be done.
                    System.out.println("Do nothing as now, since the webhook response is already processed");
                    return null;
                }
            }
            String payerVpa = metaData.get("payerVpa");
            if (doVerification && StringUtils.isNotBlank(payerVpa)) {
                UpiAdvanceResponseSchema verificationResponse = getVerificationDataFromPaymentGateway(entityId,payerVpa);
                if (null != verificationResponse) {
                    if (verificationResponse.getStatus().equalsIgnoreCase("VALID")) {
                        String nameAtBank = verificationResponse.getNameAtBank();
                        if (StringUtils.isNotBlank(nameAtBank)) {
                            metaData.put("userName", nameAtBank);
                            userName = nameAtBank;
                        }
                        metaData.put("ifsc", verificationResponse.getIfsc());
                    }
                }
            }
            UserWS userWS = createOrGetUser(userName, accountTypeId, entityId);
            OrderWS orderWS = createOrder(paymentAmount, userWS.getId(), languageId, paymentUrlLogDTO == null ? null : paymentUrlLogDTO.getId(), entityId, itemCode, quantity, getCurrencyId(entityId, metaData.get("currency")));
            OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(orderWS, ORDER_CHANGE_STATUS_APPLY_ID);
            CreateResponseWS responseWS = executeWebServicesOperation(entityId, userWS, orderWS, orderChanges, false, null, null);
            responseWS.setPaymentId(updatePaymentStatus(paymentUrlLogDTO, response, responseWS.getInvoiceId(), responseWS.getUserId(), metaData, entityId, webhookResponse));

            return responseWS;
        } catch (Exception e) {
            throw new SessionInternalError(e, new String[]{"Error is occurring in the creation of User, Order, and Invoice"},
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

    }

    private Integer updatePaymentStatus(PaymentUrlLogDTO paymentUrlLogDTO, PaymentResponse response, Integer invoiceId,Integer userId, Map<String, String> metaData, Integer entityId, String webhookResponse) throws JsonProcessingException {
        String responseStr = OBJECT_MAPPER.writeValueAsString(response);
        PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();
        PaymentUrlLogDTO forUpdate = (null == paymentUrlLogDTO) ? paymentUrlLogDAS.findByInvoiceId(invoiceId) : paymentUrlLogDAS.findForUpdate(paymentUrlLogDTO.getId());
        forUpdate.setPaymentStatusResponse(responseStr);
        forUpdate.setInvoiceId(invoiceId);
        forUpdate.setWebhookResponse(webhookResponse);
        forUpdate.setStatus(SUCCESS_RESPONSES.contains(response.getResponseCode()) ? Status.SUCCESSFUL : Status.FAILED);
        paymentUrlLogDAS.save(forUpdate);
        if (!SUCCESS_RESPONSES.contains(response.getResponseCode())){
            return null;
        }

        String responseType = metaData.get("responseType");
        Integer paymentId = null;
        if ("UPI".equalsIgnoreCase(responseType)) {
            paymentId = createPayment(response.getTransactionId(), metaData, userId, entityId, getPaymentMethodTypeId("UPI", entityId), PAYMENT_METHOD_CUSTOM, UPI_META_FIELDS);
        } else if ("CARD".equals(responseType)) {
            paymentId = createPayment(response.getTransactionId(), metaData, userId, entityId, getPaymentMethodTypeId("Card Payment", entityId), PAYMENT_METHOD_CREDIT, CARD_META_FIELDS);
        } else if ("NETBANKING".equals(responseType)) {

        } else {
            logger.error("responseType {} not found", responseType);
        }
        return paymentId;
    }

    private int getPaymentMethodTypeId(String paymentMethodName, Integer entityId) {
        List<PaymentMethodTypeDTO> paymentMethodTypeList = new PaymentMethodTypeDAS().findByMethodName(paymentMethodName, entityId);
        logger.debug("found {} payment method type for provided paymentMethodName = {} and entityId = {}", paymentMethodTypeList.size(), paymentMethodName, entityId);
        return paymentMethodTypeList.stream().findFirst().get().getId();
    }

    private Integer createPayment(String transactionId, Map<String, String> metaData, Integer userId, Integer entityId, Integer typeId, Integer paymentMethodId, List<String> metaFields) {
        PaymentWS paymentWS = createPaymentWS(entityId, metaData, userId);
        paymentWS.setPaymentInstruments(createPaymentInstrument(metaData, entityId, typeId, paymentMethodId, metaFields));
        return savePayment(paymentWS, entityId, transactionId);
    }

    private Integer savePayment(PaymentWS paymentWS, Integer entityId, String transactionId) {
        Integer paymentId;
        IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        SpringSecurityService springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        if( !springSecurityService.isLoggedIn() ) {
            try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
                paymentId = webServicesSessionBean.createPayment(paymentWS);
            } catch (Exception e) {
                throw new SessionInternalError("Exception in create payment");
            }
        } else {
            paymentId = webServicesSessionBean.createPayment(paymentWS);
        }
        createPaymentAuthDTO(transactionId, entityId, paymentId);
        return paymentId;
    }

    private PaymentUrlLogDTO createPaymentUrlLog(Map<String, Object> requestMap, Integer entityId) {
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            return webServicesSessionBean.createPaymentUrl(requestMap);
        } catch (Exception e) {
            throw new SessionInternalError("Exception in create payment");
        }
    }

    private PaymentWS createPaymentWS(Integer entityId, Map<String, String> metaData, Integer userId) {
        PaymentWS paymentWS = new PaymentWS();
        paymentWS.setUserId(userId);
        paymentWS.setIsRefund(Boolean.TRUE.equals(Boolean.parseBoolean(metaData.get("isRefund"))) ? 1 : 0);
        paymentWS.setAmount(new BigDecimal(String.valueOf(metaData.get("amount"))));
        String paymentTime = metaData.get("paymentTime");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        Date paymentDate = new Date();
        try {
            paymentDate = sdf.parse(paymentTime);
        } catch (ParseException e) {
            logger.error("Invalid payment time format: {}", e.getMessage(), e);
        }
        paymentWS.setPaymentDate(paymentDate);
        paymentWS.setCurrencyId(getCurrencyId(entityId,metaData.get("currency")));
        return paymentWS;
    }

    private List<PaymentInformationWS> createPaymentInstrument(Map<String, String> metaData, Integer entityId, Integer paymentMethodTypeId, Integer paymentMethodId, List<String> metaFieldNames) {

        PaymentInformationWS paymentInformationWS = new PaymentInformationWS();
        paymentInformationWS.setPaymentMethodTypeId(paymentMethodTypeId);
        paymentInformationWS.setPaymentMethodId(paymentMethodId);

        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

        List<MetaFieldValueWS> metaFieldValues = metaFieldNames.stream()
                .map(fieldName -> getMetaFieldValueWS(metaFieldDAS, entityId, fieldName, String.valueOf(metaData.get(fieldName))))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        paymentInformationWS.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

        return Arrays.asList(paymentInformationWS);
    }
}
